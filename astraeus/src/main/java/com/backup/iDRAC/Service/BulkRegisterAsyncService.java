package com.backup.iDRAC.Service;

import com.backup.iDRAC.Dto.RegisterServerRequest;
import com.backup.iDRAC.Entity.BulkRegisterFailureLog;
import com.backup.iDRAC.Entity.BulkRegisterJob;
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
public class BulkRegisterAsyncService {

    @Autowired
    private BulkRegisterJobRepository jobRepository;
    @Autowired
    private BulkRegisterFailureRepository failureRepository;
    @Autowired
    private VaultService vaultService;
    @Autowired
    private IdracServerRepository idracServerRepository;

    @Async
    public void processCsvAsync(BulkRegisterJob job, MultipartFile file) {

        List<BulkRegisterFailureLog> failures = new ArrayList<>();
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
                    registerServer(new RegisterServerRequest(host, username, password));
                    success++;
                } catch (Exception ex) {
                    failures.add(
                            BulkRegisterFailureLog.builder()
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
}
