package com.backup.iDRAC.Repostiory;

import com.backup.iDRAC.Entity.BackupHostLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BackupHostLogRepository extends JpaRepository<BackupHostLog, Long> {
    List<BackupHostLog> findByBackupJobId(Long jobId);
    Optional<BackupHostLog> findTopByHostAndStatusOrderByIdDesc(String host, String status);
    List<BackupHostLog> findByHostAndStatus(String host, String status);
    List<BackupHostLog> findByHostInAndStatus(List<String> hosts, String status);
    List<BackupHostLog> findByHostAndStatusAndCreatedAtBetween(String host, String status, Instant from, Instant to);
    List<BackupHostLog> findByHostAndStatusAndCreatedAtAfter(String host, String status, Instant from);
    List<BackupHostLog> findByHostAndStatusAndCreatedAtBefore(String host, String status, Instant to);
    Optional<BackupHostLog> findFirstByBackupJobIdOrderByCreatedAtDesc(Long backupId);
}