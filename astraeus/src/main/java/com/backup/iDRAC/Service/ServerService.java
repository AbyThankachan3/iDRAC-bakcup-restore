package com.backup.iDRAC.Service;

import com.backup.iDRAC.Dto.RegisterServerRequest;
import com.backup.iDRAC.Dto.RegisterServerResponse;
import com.backup.iDRAC.Entity.IdracServer;
import com.backup.iDRAC.Exception.ServerConnectionException;
import com.backup.iDRAC.Exception.ServerNotFoundException;
import com.backup.iDRAC.Repostiory.IdracServerRepository;
import com.idrac.api.RedfishClient;
import com.idrac.client.RedfishClientBuilder;
import com.idrac.config.RedfishConnection;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServerService {
    private final IdracServerRepository idracServerRepository;
    private final VaultService vaultService;

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

}
