package com.backup.iDRAC.Service;

import com.backup.iDRAC.Entity.*;
import com.backup.iDRAC.Repostiory.BackupHostLogRepository;
import com.backup.iDRAC.Repostiory.BackupJobRepository;
import com.backup.iDRAC.Repostiory.IdracServerRepository;
import com.idrac.backup.api.RedfishClient;
import com.idrac.backup.client.RedfishClientBuilder;
import com.idrac.backup.config.RedfishConnection;
import com.idrac.backup.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BackupAsyncService {
    @Autowired
    private IdracServerRepository idracServerRepository;
    @Autowired
    private BackupJobRepository backupJobRepository;
    @Autowired
    private BackupHostLogRepository backupHostLogRepository;
    @Autowired
    private VaultService vaultService;

    @Async
    public void startBackupAsync(BackupJob job, List<IdracServer> servers) {

        int success = 0;
        int failure = 0;

        for (IdracServer server : servers) {

            long start = System.currentTimeMillis();

            BackupHostLog log = backupHostLogRepository.save(
                    BackupHostLog.builder()
                            .backupJob(job)
                            .host(server.getHost())
                            .status("RUNNING")
                            .createdAt(Instant.now())
                            .build()
            );

            try {

                String filePath = backupSingleServer(server, log);

                log.setStatus("COMPLETED");
                log.setFilePath(filePath);
                log.setDurationMillis(System.currentTimeMillis() - start);

                backupHostLogRepository.save(log);

                success++;

            } catch (Exception e) {

                log.setStatus("FAILED");
                log.setErrorMessage(e.getMessage());
                log.setDurationMillis(System.currentTimeMillis() - start);

                backupHostLogRepository.save(log);

                failure++;
            }
        }

        job.setFinishedAt(Instant.now());
        job.setSuccessCount(success);
        job.setFailureCount(failure);
        job.setStatus(failure > 0 ? BackupJobStatus.FAILED : BackupJobStatus.COMPLETED);

        backupJobRepository.save(job);
    }

    private String backupSingleServer(IdracServer server, BackupHostLog log) throws Exception {

        Credentials creds = vaultService.getCredentials(server.getVaultPath());

        RedfishConnection connection = RedfishConnection.builder()
                .host(server.getHost())
                .username(creds.getUsername())
                .password(creds.getPassword())
                .build();

        RedfishClient client = RedfishClientBuilder.build(connection);

        String jobId = client.triggerExport(
                ExportTarget.ALL,
                ExportFormat.XML,
                ExportUse.REPLACE,
                IncludeInExport.INCLUDE_READ_ONLY,
                IncludeInExport.INCLUDE_PASSWORD_HASH_VALUES
        );

        log.setRedfishJobId(jobId);
        log.setStatus("RUNNING");
        log.setPercent(0);
        backupHostLogRepository.save(log);

        client.pollJob(jobId, 300, job -> updateBackupProgress(job, log));

        String scp = client.fetchScp(jobId, ExportFormat.XML);

        return saveBackupToDisk(server.getHost(), scp);
    }

    private void updateBackupProgress(JobResponse job, BackupHostLog log) {

        boolean changed = false;

        String newState = job.getJobState();

        if (newState != null && !newState.equalsIgnoreCase(log.getStatus())) {

            log.setStatus(newState);
            changed = true;
        }

        Integer newPercent = job.getPercentComplete();

        if (newPercent != null &&
                (log.getPercent() == null || !newPercent.equals(log.getPercent()))) {

            log.setPercent(newPercent);
            changed = true;
        }

        if (changed) {
            backupHostLogRepository.save(log);
        }
    }


    private String saveBackupToDisk(String host, String content) throws Exception {

        String baseDir = System.getenv().getOrDefault("BACKUP_DIR", "/data/idrac-backups/");
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path dir = Path.of(baseDir, host);
        Files.createDirectories(dir);
        Path file = dir.resolve("idrac_" + timestamp + ".xml");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toAbsolutePath().toString();
    }
}
