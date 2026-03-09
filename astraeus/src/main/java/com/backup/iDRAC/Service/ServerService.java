package com.backup.iDRAC.Service;

import com.backup.iDRAC.Dto.*;
import com.backup.iDRAC.Entity.ServerRegistrationJobs;
import com.backup.iDRAC.Entity.IdracServer;
import com.backup.iDRAC.Exception.ServerNotFoundException;
import com.backup.iDRAC.Repostiory.BulkRegisterFailureRepository;
import com.backup.iDRAC.Repostiory.BulkRegisterJobRepository;
import com.backup.iDRAC.Repostiory.IdracServerRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service

public class ServerService {
    @Autowired
    private IdracServerRepository idracServerRepository;
    @Autowired
    private VaultService vaultService;
    @Autowired
    private BulkRegisterJobRepository jobRepository;
    @Autowired
    private ServerRegisterAsyncService serverRegisterAsyncService;
    @Autowired
    private BulkRegisterFailureRepository failureRepository;

    public List<RegisterServerResponse> getAllServers(){
        return idracServerRepository.findAll().stream().map(
                server -> new RegisterServerResponse(
                        server.getId(), server.getHost(), server.getModel(), server.getJob().getId())).toList();
    }

    public RegisterServerResponse getServerByHost(String host){
        IdracServer server = idracServerRepository.findByHost(host).orElseThrow(() ->
                new RuntimeException("Server not found with host: " + host)
        );

        return new RegisterServerResponse(server.getId(), server.getHost(), server.getModel(), server.getJob().getId());
    }

    @Transactional
    public void deleteServer(String host) {

        IdracServer server = idracServerRepository
                .findByHost(host)
                .orElseThrow(() ->
                        new ServerNotFoundException("ID: " + host)
                );
        // Delete credentials from Vault first
        vaultService.deleteCredentials(server.getVaultPath());
        // Then delete from DB
        idracServerRepository.delete(server);
    }

    public Long startBulkRegister(MultipartFile file) {

        ServerRegistrationJobs job = ServerRegistrationJobs.builder()
                .startedAt(Instant.now())
                .status("RUNNING")
                .build();

        job = jobRepository.save(job);
        serverRegisterAsyncService.processCsvAsync(job, file);

        return job.getId();
    }

    public ServerRegistrationJobStatusResponse getServerRegisterJob(Long jobId) {

        ServerRegistrationJobs job = jobRepository.findById(jobId).orElseThrow();

        List<ServerRegistrationFailures> failures = new ArrayList<>();
        if (job.getFailures() != null) {
            failures = job.getFailures().stream()
                    .map(f -> new ServerRegistrationFailures(f.getHost(), f.getError()))
                    .toList();
        }

        List<RegisterServerResponse> successes = new ArrayList<>();
        if (job.getSuccessfulServers() != null) {
            successes = job.getSuccessfulServers().stream()
                    .map(server -> new RegisterServerResponse(
                            server.getId(),
                            server.getHost(),
                            server.getModel(),
                            job.getId() // Link the job ID
                    ))
                    .toList();
        }

        return ServerRegistrationJobStatusResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .total(job.getTotal())
                .successCount(job.getSuccessCount())
                .failureCount(job.getFailureCount())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .failures(failures)
                .successfulServers(successes)
                .build();
    }

    public Long startSingleRegisterJob(RegisterServerRequest request) {
        ServerRegistrationJobs job = ServerRegistrationJobs.builder()
                .startedAt(Instant.now())
                .status("RUNNING") // or "PENDING"
                .total(1)          // Hardcoded to 1 for a single request
                .successCount(0)
                .failureCount(0)
                .build();

        job = jobRepository.save(job);

        // 2. Pass the job and request to the @Async service
        serverRegisterAsyncService.processSingleAsync(job, request);

        // 3. Return the ID immediately
        return job.getId();
    }

}
