package com.backup.iDRAC.Service;

import com.backup.iDRAC.Entity.BackupHostLog;
import com.backup.iDRAC.Entity.Credentials;
import com.backup.iDRAC.Entity.IdracServer;
import com.backup.iDRAC.Entity.RestoreLog;
import com.backup.iDRAC.Exception.HostNotFoundException;
import com.backup.iDRAC.Exception.RestoreFailedException;
import com.backup.iDRAC.Repostiory.BackupHostLogRepository;
import com.backup.iDRAC.Repostiory.BackupJobRepository;
import com.backup.iDRAC.Repostiory.IdracServerRepository;
import com.backup.iDRAC.Repostiory.RestoreLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idrac.restore.client.RestoreClient;
import com.idrac.restore.client.RestoreRedfishClientBuilder;
import com.idrac.restore.model.JobResponse;
import com.idrac.restore.model.RedfishConnection;
import com.idrac.restore.model.RestorePlan;
import com.idrac.restore.model.RestoreTaskResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
public class RestoreAsyncService {

    @Autowired
    private BackupJobRepository backupJobRepository;
    @Autowired
    private BackupHostLogRepository backupHostLogRepository;
    @Autowired
    private IdracServerRepository idracServerRepository;
    @Autowired
    private VaultService vaultService;
    @Autowired
    private RestoreLogRepository restoreLogRepository;

    @Async
    public void runRestoreAsync(
            Long restoreId,
            BackupHostLog hostLog,
            String host,
            String username,
            String password) {

        try {

            RestoreLog log = restoreLogRepository.findById(restoreId).orElseThrow();

            String xml = Files.readString(Path.of(hostLog.getFilePath()));
            RestoreClient client = createClient(host, username, password);
            RestorePlan plan = client.analyseXml(xml, host);

            log.setRebootRequired(plan.isRebootRequired());
            log.setFinalHost(plan.getTargetHost());
            restoreLogRepository.save(log);
//            JobResponse preview = client.preview(xml);
//            client.pollJob(preview.getId(), 300, job -> updateRestoreLog(job, log));
            JobResponse importJob = client.importConfig(xml);

            log.setRedfishJobId(importJob.getId());
            log.setStatus("IMPORT_RUNNING");

            restoreLogRepository.save(log);

            pollWithHostSwitch(importJob.getId(), plan, username, password, log);
            RestoreTaskResult result = client.fetchTaskResult(importJob.getId());
            updateRestoreResult(log, result);
        } catch (Exception e) {

            RestoreLog log = restoreLogRepository.findById(restoreId).orElseThrow();

            log.setStatus("FAILED");
            log.setCompletedAt(LocalDateTime.now());

            restoreLogRepository.save(log);
        }
    }

    private void pollWithHostSwitch(
            String jobId,
            RestorePlan plan,
            String username,
            String password,
            RestoreLog log) throws Exception {

        RestoreClient client = createClient(plan.getCurrentHost(), username, password);

        try {

            client.pollJob(jobId, 600, job -> updateRestoreLog(job, log));

        } catch (Exception ex) {

            IdracServer server =
                    idracServerRepository
                            .findByHost(plan.getTargetHost())
                            .orElseThrow(() -> new HostNotFoundException(plan.getTargetHost()));

            Credentials creds = vaultService.getCredentials(server.getVaultPath());

            RestoreClient newClient =
                    createClient(plan.getTargetHost(), creds.getUsername(), creds.getPassword());

            newClient.pollJob(jobId, 600, job -> updateRestoreLog(job, log));
        }
    }

    private void updateRestoreLog(JobResponse job, RestoreLog log) {

        boolean changed = false;
        if (!job.getJobState().equalsIgnoreCase(log.getStatus())) {
            log.setStatus(job.getJobState());
            changed = true;
        }
        if (job.getPercentComplete() != null &&
                !job.getPercentComplete().equals(log.getPercent())) {

            log.setPercent(job.getPercentComplete());
            changed = true;
        }
        if (changed) {
            if ("Completed".equalsIgnoreCase(job.getJobState())
                    || "CompletedWithErrors".equalsIgnoreCase(job.getJobState())
                    || "Failed".equalsIgnoreCase(job.getJobState())) {
                log.setCompletedAt(LocalDateTime.now());
            }
            restoreLogRepository.save(log);
        }
    }

    private void updateRestoreResult(RestoreLog log, RestoreTaskResult result) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            log.setFailures(mapper.valueToTree(result.getFailures()));
            log.setWarnings(mapper.valueToTree(result.getWarnings()));
            log.setCompletedAt(LocalDateTime.now());
            restoreLogRepository.save(log);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private RestoreClient createClient(
            String host,
            String username,
            String password) {

        RedfishConnection connection =
                RedfishConnection.builder()
                        .host(host)
                        .username(username)
                        .password(password)
                        .build();

        return RestoreRedfishClientBuilder.build(connection);
    }
}
