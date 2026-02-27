package com.backup.iDRAC.Entity;

import com.backup.iDRAC.Entity.BackupHostStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "backup_host_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupHostLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private BackupJob backupJob;

    @Column(nullable = false)
    private Instant createdAt;

    private String host;

    @Enumerated(EnumType.STRING)
    private BackupHostStatus status;

    private String filePath;

    @Column(length = 2000)
    private String errorMessage;

    private Long durationMillis;
}
