package com.backup.iDRAC.Repostiory;

import com.backup.iDRAC.Entity.RestoreLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestoreLogRepository extends JpaRepository<RestoreLog, Long> {
}
