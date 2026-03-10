package com.backup.iDRAC.Repostiory;

import com.backup.iDRAC.Entity.RestoreLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestoreLogRepository extends JpaRepository<RestoreLog, Long> {
    List<RestoreLog> findByStatus(String status);
}
