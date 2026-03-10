package com.backup.iDRAC.Service;

import com.backup.iDRAC.Entity.BackupHostLog;
import com.backup.iDRAC.Entity.BackupJobStatus;
import com.backup.iDRAC.Entity.RestoreJobStatus;
import com.backup.iDRAC.Entity.RestoreLog;
import com.backup.iDRAC.Repostiory.BackupHostLogRepository;
import com.backup.iDRAC.Repostiory.RestoreLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class JobRecoveryService {

    @Autowired
    private BackupHostLogRepository backupHostLogRepository;
    @Autowired
    private RestoreLogRepository restoreLogRepository;
    @Autowired
    private BackupAsyncService backupAsyncService;
    @Autowired
    private RestoreAsyncService restoreAsyncService;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInFlightJobs() {
        recoverBackupJobs();
        recoverRestoreJobs();
    }

    private void recoverBackupJobs() {
        List<BackupHostLog> stuck = backupHostLogRepository
                .findByStatus(BackupJobStatus.RUNNING.name());

        if (stuck.isEmpty()) {
            log.info("No stuck backup jobs found.");
            return;
        }

        log.warn("Found {} stuck backup job(s), recovering...", stuck.size());
        for (BackupHostLog hostLog : stuck) {
            if (hostLog.getRedfishJobId() == null) {
                log.warn("Backup log {} has no redfishJobId — marking failed", hostLog.getId());
                hostLog.setStatus(BackupJobStatus.FAILED.name());
                hostLog.setErrorMessage("Job lost before iDRAC job was created");
                backupHostLogRepository.save(hostLog);
            } else {
                backupAsyncService.resumePolling(hostLog);
            }
        }
    }

    private void recoverRestoreJobs() {
        List<RestoreLog> stuck = restoreLogRepository.findByStatus(RestoreJobStatus.RUNNING.name());

        if (stuck.isEmpty()) {
            log.info("No stuck restore jobs found.");
            return;
        }

        log.warn("Found {} stuck restore job(s), recovering...", stuck.size());
        for (RestoreLog restoreLog : stuck) {
            if (restoreLog.getRedfishJobId() == null) {
                log.warn("Restore log {} has no redfishJobId — marking failed", restoreLog.getId());
                restoreLog.setStatus(RestoreJobStatus.FAILED.name());
                restoreLog.setCompletedAt(LocalDateTime.now());
                restoreLogRepository.save(restoreLog);
            } else {
                restoreAsyncService.resumeRestorePolling(restoreLog);
            }
        }

        // Jobs stuck at CREATED — async never started
        List<RestoreLog> createdStuck = restoreLogRepository
                .findByStatus(RestoreJobStatus.CREATED.getValue());

        for (RestoreLog restoreLog : createdStuck) {
            log.warn("Restore log {} stuck at CREATED — marking failed", restoreLog.getId());
            restoreLog.setStatus(RestoreJobStatus.FAILED.name());
            restoreLog.setCompletedAt(LocalDateTime.now());
            restoreLogRepository.save(restoreLog);
        }
    }
}