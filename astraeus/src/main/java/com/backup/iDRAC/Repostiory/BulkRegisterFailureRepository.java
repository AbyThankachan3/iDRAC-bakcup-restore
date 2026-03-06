package com.backup.iDRAC.Repostiory;

import com.backup.iDRAC.Entity.BulkRegisterFailureLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BulkRegisterFailureRepository extends JpaRepository<BulkRegisterFailureLog, Long> {

    List<BulkRegisterFailureLog> findByJobId(Long jobId);

}