package com.backup.iDRAC.Service;

import com.backup.iDRAC.Dto.*;
import com.backup.iDRAC.Entity.BulkRegisterJob;
import com.backup.iDRAC.Entity.IdracServer;
import com.backup.iDRAC.Exception.ServerConnectionException;
import com.backup.iDRAC.Exception.ServerNotFoundException;
import com.backup.iDRAC.Repostiory.BulkRegisterFailureRepository;
import com.backup.iDRAC.Repostiory.BulkRegisterJobRepository;
import com.backup.iDRAC.Repostiory.IdracServerRepository;
import com.idrac.backup.api.RedfishClient;
import com.idrac.backup.client.RedfishClientBuilder;
import com.idrac.backup.config.RedfishConnection;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
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
    private BulkRegisterAsyncService bulkRegisterAsyncService;
    @Autowired
    private BulkRegisterFailureRepository failureRepository;

    @Transactional
    public IdracServer registerServer(RegisterServerRequest request){
        //check connection
        RedfishConnection connection = RedfishConnection.builder().host(request.getHost()).username(request.getUsername()).password(request.getPassword()).build();
        try{
            RedfishClient client = RedfishClientBuilder.build(connection);
            String systemModel = client.getSystemModel();
            //save values to db and vault
            String vaultPath = vaultService.storeCredentials(request.getHost(), request.getUsername(), request.getPassword());
            IdracServer server = IdracServer.builder().host(request.getHost()).model(systemModel).vaultPath(vaultPath).build();
            idracServerRepository.save(server);
            return server;
        } catch (Exception e) {
            throw new ServerConnectionException(request.getHost(), e.getMessage());
        }
    }

    public List<RegisterServerResponse> getAllServers(){
        return idracServerRepository.findAll().stream().map(
                server -> new RegisterServerResponse(
                        server.getId(), server.getHost(), server.getModel())).toList();
    }

    public RegisterServerResponse getServerByHost(String host){
        IdracServer server = idracServerRepository.findByHost(host).orElseThrow(() ->
                new RuntimeException("Server not found with host: " + host)
        );

        return new RegisterServerResponse(server.getId(), server.getHost(), server.getModel());
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

        BulkRegisterJob job = BulkRegisterJob.builder()
                .startedAt(Instant.now())
                .status("RUNNING")
                .build();

        job = jobRepository.save(job);
        bulkRegisterAsyncService.processCsvAsync(job, file);

        return job.getId();
    }

    public BulkRegisterJobResponse getBulkRegisterJob(Long jobId) {

        BulkRegisterJob job = jobRepository.findById(jobId).orElseThrow();

        List<BulkRegisterFailure> failures = failureRepository.findByJobId(jobId)
                        .stream()
                        .map(f -> new BulkRegisterFailure(
                                f.getHost(),
                                f.getError()))
                        .toList();

        return BulkRegisterJobResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .total(job.getTotal())
                .successCount(job.getSuccessCount())
                .failureCount(job.getFailureCount())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .failures(failures)
                .build();
    }

}
