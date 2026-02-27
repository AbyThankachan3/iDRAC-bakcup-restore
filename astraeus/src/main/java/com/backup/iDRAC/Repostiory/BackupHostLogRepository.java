package com.backup.iDRAC.Repostiory;

import com.backup.iDRAC.Entity.BackupHostLog;
import com.backup.iDRAC.Entity.BackupHostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BackupHostLogRepository extends JpaRepository<BackupHostLog, Long> {
    List<BackupHostLog> findByBackupJobId(Long jobId);
    Optional<BackupHostLog> findTopByHostAndStatusOrderByIdDesc(String host, BackupHostStatus status);
    long countByHostAndStatus(String host, BackupHostStatus status);
    List<BackupHostLog> findByHostAndStatus(String host, BackupHostStatus status);
    List<BackupHostLog> findByStatus(BackupHostStatus status);
    List<BackupHostLog> findByHostInAndStatus(List<String> hosts, BackupHostStatus status);
    List<BackupHostLog> findByHostAndStatusAndCreatedAtBetween(String host, BackupHostStatus status, Instant from, Instant to);
    List<BackupHostLog> findByHostAndStatusAndCreatedAtAfter(String host, BackupHostStatus status, Instant from);
    List<BackupHostLog> findByHostAndStatusAndCreatedAtBefore(String host, BackupHostStatus status, Instant to);
}