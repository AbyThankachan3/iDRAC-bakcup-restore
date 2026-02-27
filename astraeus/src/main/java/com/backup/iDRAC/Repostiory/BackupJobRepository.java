package com.backup.iDRAC.Repostiory;

import com.backup.iDRAC.Entity.BackupJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackupJobRepository extends JpaRepository<BackupJob, Long> {
}