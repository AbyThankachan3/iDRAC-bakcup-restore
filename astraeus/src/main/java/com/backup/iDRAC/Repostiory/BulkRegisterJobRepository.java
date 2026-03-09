package com.backup.iDRAC.Repostiory;

import com.backup.iDRAC.Entity.ServerRegistrationJobs;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BulkRegisterJobRepository extends JpaRepository<ServerRegistrationJobs, Long> {
}
