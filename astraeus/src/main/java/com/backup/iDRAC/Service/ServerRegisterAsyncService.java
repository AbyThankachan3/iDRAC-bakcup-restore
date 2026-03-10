package com.backup.iDRAC.Service;

import com.backup.iDRAC.Dto.RegisterServerRequest;
import com.backup.iDRAC.Entity.ServerRegFailureLogs;
import com.backup.iDRAC.Entity.ServerRegistrationJobs;
import com.backup.iDRAC.Entity.IdracServer;
import com.backup.iDRAC.Exception.ServerConnectionException;
import com.backup.iDRAC.Repostiory.BulkRegisterFailureRepository;
import com.backup.iDRAC.Repostiory.BulkRegisterJobRepository;
import com.backup.iDRAC.Repostiory.IdracServerRepository;
import com.idrac.backup.api.RedfishClient;
import com.idrac.backup.client.RedfishClientBuilder;
import com.idrac.backup.config.RedfishConnection;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServerRegisterAsyncService {

    @Autowired
    private BulkRegisterJobRepository jobRepository;
    @Autowired
    private BulkRegisterFailureRepository failureRepository;
    @Autowired
    private VaultService vaultService;
    @Autowired
    private IdracServerRepository idracServerRepository;

    @Async
    public void processCsvAsync(ServerRegistrationJobs job, MultipartFile file) {

        List<ServerRegFailureLogs> failures = new ArrayList<>();
        int total = 0;
        int success = 0;

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(file.getInputStream()))) {

            String line;
            boolean header = true;

            while ((line = reader.readLine()) != null) {

                if (header) {
                    header = false;
                    continue;
                }
                total++;
                String[] parts = line.split(",");
                String host = parts[0].trim();
                String username = parts[1].trim();
                String password = parts[2].trim();

                try {
                    registerServer(new RegisterServerRequest(host, username, password), job);
                    success++;
                } catch (Exception ex) {
                    failures.add(
                            ServerRegFailureLogs.builder()
                                    .job(job)
                                    .host(host)
                                    .error(ex.getMessage())
                                    .build()
                    );
                }
            }

        } catch (Exception e) {
            job.setStatus("FAILED");

        }
        job.setTotal(total);
        job.setSuccessCount(success);
        job.setFailureCount(total - success);
        job.setFinishedAt(Instant.now());
        if (success < total){
            job.setStatus("COMPLETED WITH FAILURES");
        }else {
            job.setStatus("COMPLETED");
        }
        jobRepository.save(job);
        failureRepository.saveAll(failures);
    }

    @Async
    public void processSingleAsync(ServerRegistrationJobs job, RegisterServerRequest request) {
        try {
            IdracServer server = registerServer(request, job);
            job.setSuccessCount(1);
            job.setStatus("COMPLETED");

        } catch (Exception ex) {
            job.setFailureCount(1);
            job.setStatus("FAILED");
            ServerRegFailureLogs failure = ServerRegFailureLogs.builder()
                    .job(job)
                    .host(request.getHost())
                    .error(ex.getMessage())
                    .build();
            failureRepository.save(failure);

        } finally {
            job.setFinishedAt(Instant.now());
            jobRepository.save(job);
        }
    }

    @Transactional
    public IdracServer registerServer(RegisterServerRequest request, ServerRegistrationJobs job){
        //check connection
        RedfishConnection connection = RedfishConnection.builder().host(request.getHost()).username(request.getUsername()).password(request.getPassword()).build();
        try{
            RedfishClient client = RedfishClientBuilder.build(connection);
            String systemModel = client.getSystemModel();
            //save values to db and vault
            String vaultPath = vaultService.storeCredentials(request.getHost(), request.getUsername(), request.getPassword());
            IdracServer server = IdracServer.builder().host(request.getHost()).model(systemModel).vaultPath(vaultPath).job(job).build();
            idracServerRepository.save(server);
            return server;
        } catch (Exception e) {
            throw new ServerConnectionException(request.getHost(), e.getMessage());
        }
    }
}
