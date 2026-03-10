package com.backup.iDRAC.Service;

import com.backup.iDRAC.Entity.*;
import com.backup.iDRAC.Exception.HostNotFoundException;
import com.backup.iDRAC.Repostiory.BackupHostLogRepository;
import com.backup.iDRAC.Repostiory.BackupJobRepository;
import com.backup.iDRAC.Repostiory.IdracServerRepository;
import com.idrac.backup.api.RedfishClient;
import com.idrac.backup.client.RedfishClientBuilder;
import com.idrac.backup.config.RedfishConnection;
import com.idrac.backup.model.*;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
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

            BackupHostLog backupHostLog = backupHostLogRepository.save(
                    BackupHostLog.builder()
                            .backupJob(job)
                            .host(server.getHost())
                            .status("RUNNING")
                            .createdAt(Instant.now())
                            .build()
            );

            try {

                String filePath = backupSingleServer(server, backupHostLog);

                backupHostLog.setStatus("COMPLETED");
                backupHostLog.setFilePath(filePath);
                backupHostLog.setDurationMillis(System.currentTimeMillis() - start);

                backupHostLogRepository.save(backupHostLog);

                success++;

            } catch (Exception e) {

                backupHostLog.setStatus("FAILED");
                backupHostLog.setErrorMessage(e.getMessage());
                backupHostLog.setDurationMillis(System.currentTimeMillis() - start);

                backupHostLogRepository.save(backupHostLog);

                failure++;
            }
        }

        job.setFinishedAt(Instant.now());
        job.setSuccessCount(success);
        job.setFailureCount(failure);
        job.setStatus(failure > 0 ? BackupJobStatus.FAILED : BackupJobStatus.COMPLETED);

        backupJobRepository.save(job);
    }

    @Async
    public void resumePolling(BackupHostLog hostLog) {
        long start = System.currentTimeMillis();
        try {
            IdracServer server = idracServerRepository
                    .findByHost(hostLog.getHost())
                    .orElseThrow(() -> new HostNotFoundException(hostLog.getHost()));

            Credentials creds = vaultService.getCredentials(server.getVaultPath());
            RedfishConnection connection = RedfishConnection.builder()
                    .host(server.getHost())
                    .username(creds.getUsername())
                    .password(creds.getPassword())
                    .build();
            RedfishClient client = RedfishClientBuilder.build(connection);

            // Resume polling the existing redfish job — don't trigger a new one
            client.pollJob(hostLog.getRedfishJobId(), 300, job -> updateBackupProgress(job, hostLog));

            try {
                String scp = client.fetchScp(hostLog.getRedfishJobId(), ExportFormat.XML);
                String filePath = saveBackupToDisk(hostLog.getHost(), scp);

                hostLog.setStatus(BackupJobStatus.COMPLETED.name());
                hostLog.setFilePath(filePath);
                hostLog.setDurationMillis(System.currentTimeMillis() - start);
                backupHostLogRepository.save(hostLog);
            } catch (Exception e) {
                JobResponse job = client.getJob(hostLog.getRedfishJobId());
                String actualState = job.getJobState();

                if ("Completed".equalsIgnoreCase(actualState)) {
                    log.warn("SCP expired for job {}", hostLog.getRedfishJobId());
                    hostLog.setStatus(BackupJobStatus.COMPLETED.name());
                    hostLog.setErrorMessage("SCP/File expired on iDRAC before retrieval. " +
                            "Check iDRAC Lifecycle Controller logs for details.");
                    hostLog.setDurationMillis(System.currentTimeMillis() - start);
                    hostLog.setFilePath(null);
                    backupHostLogRepository.save(hostLog);

                } else if ("Failed".equalsIgnoreCase(actualState)
                        || "Exception".equalsIgnoreCase(actualState)) {
                    hostLog.setStatus(BackupJobStatus.FAILED.name());
                    hostLog.setErrorMessage("Confirmed failed on iDRAC: " + actualState);
                    hostLog.setDurationMillis(System.currentTimeMillis() - start);
                    hostLog.setFilePath(null);
                    backupHostLogRepository.save(hostLog);

                }
            }

            // Update parent job counts
            updateParentJob(hostLog.getBackupJob());

        } catch (Exception e) {
            hostLog.setStatus(BackupJobStatus.FAILED.name());
            hostLog.setErrorMessage("Recovered after crash: " + e.getMessage());
            hostLog.setDurationMillis(System.currentTimeMillis() - start);
            backupHostLogRepository.save(hostLog);
            updateParentJob(hostLog.getBackupJob());
        }
    }

    private void updateParentJob(BackupJob job) {
        List<BackupHostLog> allLogs = backupHostLogRepository.findByBackupJobId(job.getId());
        boolean anyRunning = allLogs.stream()
                .anyMatch(l -> l.getStatus().equalsIgnoreCase(BackupJobStatus.RUNNING.name()));
        if (!anyRunning) {
            long failures = allLogs.stream()
                    .filter(l -> l.getStatus().equalsIgnoreCase(BackupJobStatus.FAILED.name()))
                    .count();
            job.setFinishedAt(Instant.now());
            job.setSuccessCount((int) allLogs.stream()
                    .filter(l -> l.getStatus().equalsIgnoreCase(BackupJobStatus.COMPLETED.name()))
                    .count());
            job.setFailureCount((int) failures);
            job.setStatus(failures > 0 ? BackupJobStatus.FAILED : BackupJobStatus.COMPLETED);
            backupJobRepository.save(job);
        }
    }

    private String backupSingleServer(IdracServer server, BackupHostLog backupHostLog) throws Exception {

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

        backupHostLog.setRedfishJobId(jobId);
        backupHostLog.setStatus(BackupJobStatus.RUNNING.name());
        backupHostLog.setPercent(0);
        backupHostLogRepository.save(backupHostLog);

        client.pollJob(jobId, 300, job -> updateBackupProgress(job, backupHostLog));

        String scp = client.fetchScp(jobId, ExportFormat.XML);

        return saveBackupToDisk(server.getHost(), scp);
    }

    private void updateBackupProgress(JobResponse job, BackupHostLog backupHostLog) {

        boolean changed = false;

        String newState = job.getJobState();

        if (newState != null && !newState.equalsIgnoreCase(backupHostLog.getStatus())) {

            backupHostLog.setStatus(newState);
            changed = true;
        }

        Integer newPercent = job.getPercentComplete();

        if (newPercent != null &&
                (backupHostLog.getPercent() == null || !newPercent.equals(backupHostLog.getPercent()))) {

            backupHostLog.setPercent(newPercent);
            changed = true;
        }

        if (changed) {
            backupHostLogRepository.save(backupHostLog);
        }
    }


    private String saveBackupToDisk(String host, String content) throws Exception {

        String baseDir = System.getenv().getOrDefault("BACKUP_DIR", "/data/idrac-backups/");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path dir = Path.of(baseDir, host);
        Files.createDirectories(dir);
        Path file = dir.resolve("idrac_" + timestamp + ".xml");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toAbsolutePath().toString();
    }
}
