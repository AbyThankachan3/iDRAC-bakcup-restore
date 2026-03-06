package com.backup.iDRAC.Repostiory;

import com.backup.iDRAC.Entity.BulkRegisterJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BulkRegisterJobRepository extends JpaRepository<BulkRegisterJob, Long> {
}
