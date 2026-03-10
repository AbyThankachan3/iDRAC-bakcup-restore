package com.backup.iDRAC.Service;

import com.backup.iDRAC.Dto.RestoreDiagnosticsResponse;
import com.backup.iDRAC.Entity.*;
import com.backup.iDRAC.Exception.RestoreFailedException;
import com.backup.iDRAC.Exception.RestoreIdNotFound;
import com.backup.iDRAC.Repostiory.BackupHostLogRepository;
import com.backup.iDRAC.Repostiory.RestoreLogRepository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idrac.restore.model.RestoreFailure;
import com.idrac.restore.model.TaskMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RestoreService {

    @Autowired
    private RestoreLogRepository restoreLogRepository;
    @Autowired
    private RestoreAsyncService restoreAsyncService;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private BackupHostLogRepository backupHostLogRepository;

    public Long startRestore(Long backupId, String host, String username, String password) {
        BackupHostLog hostLog = backupHostLogRepository
                .findFirstByBackupJobIdOrderByCreatedAtDesc(backupId)
                .orElseThrow(() -> new RestoreFailedException("Backup not found"));
        RestoreLog log = RestoreLog.builder()
                .backupJobId(backupId)
                .initialHost(host)
                .status(RestoreJobStatus.CREATED.getValue())
                .percent(0)
                .startedAt(LocalDateTime.now())
                .build();
        restoreLogRepository.save(log);

        restoreAsyncService.runRestoreAsync(log.getId(), hostLog, host, username, password);
        return log.getId();
    }


    public RestoreDiagnosticsResponse getDiagnostics(Long restoreId) {

        RestoreLog log = restoreLogRepository.findById(restoreId).orElseThrow(() -> new RestoreIdNotFound(restoreId));

        try {
            List<RestoreFailure> failures =
                    log.getFailures() != null
                            ? mapper.convertValue(
                            log.getFailures(),
                            new TypeReference<List<RestoreFailure>>() {})
                            : List.of();

            List<TaskMessage> warnings =
                    log.getWarnings() != null
                            ? mapper.convertValue(
                            log.getWarnings(),
                            new TypeReference<List<TaskMessage>>() {})
                            : List.of();

            return RestoreDiagnosticsResponse.builder()
                    .restoreId(restoreId)
                    .failureCount(failures.size())
                    .warningCount(warnings.size())
                    .failures(failures)
                    .warnings(warnings)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse diagnostics", e);
        }

    }
}