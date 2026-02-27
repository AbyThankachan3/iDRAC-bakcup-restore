package com.backup.iDRAC.Repostiory;

import com.backup.iDRAC.Entity.IdracServer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IdracServerRepository extends JpaRepository<IdracServer, Long> {
    Optional<IdracServer> findByHost(String host);
    boolean existsByHost(String host);
    List<IdracServer> findByModel(String model);
    boolean existsByModel(String model);
    long countByModel(String model);
}