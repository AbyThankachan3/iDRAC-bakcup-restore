package com.backup.iDRAC.Repostiory;

import com.backup.iDRAC.Entity.ServerRegFailureLogs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BulkRegisterFailureRepository extends JpaRepository<ServerRegFailureLogs, Long> {

    List<ServerRegFailureLogs> findByJobId(Long jobId);

}