package com.backup.iDRAC.Entity;

import com.backup.iDRAC.Entity.BackupJobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "backup_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant startedAt;
    private Instant finishedAt;
    private int totalServers;
    private int successCount;
    private int failureCount;
    @Enumerated(EnumType.STRING)
    private BackupJobStatus status;
    @OneToMany(mappedBy = "backupJob", cascade = CascadeType.ALL)
    private List<BackupHostLog> hostLogs;
}
