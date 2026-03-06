package com.backup.iDRAC.Controller;


import com.backup.iDRAC.Dto.*;
import com.backup.iDRAC.Entity.BulkRegisterJob;
import com.backup.iDRAC.Entity.IdracServer;
import com.backup.iDRAC.Repostiory.BulkRegisterJobRepository;
import com.backup.iDRAC.Service.ServerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/servers")
@Tag(name = "Server Management", description = "APIs for managing iDRAC servers")
public class ServerController {

    @Autowired
    private ServerService serverService;

    @Operation(
            summary = "Register a new iDRAC server",
            description = "Validates the server connectivity, stores credentials in Vault, and saves server metadata in the database."
    )
    @PostMapping
    public RegisterServerResponse registerServer(@RequestBody RegisterServerRequest serverDetails){
        IdracServer server = serverService.registerServer(serverDetails);
        return new RegisterServerResponse(server.getId(), server.getHost(), server.getModel());
    }

    @PostMapping("/file")
    public BulkRegisterStartResponse registerBulk(@RequestParam MultipartFile file) {

        Long jobId = serverService.startBulkRegister(file);

        return BulkRegisterStartResponse.builder()
                .jobId(jobId)
                .status("STARTED")
                .build();
    }

    @GetMapping("/bulk/{jobId}")
    public BulkRegisterJobResponse getJob(@PathVariable Long jobId){
        return serverService.getBulkRegisterJob(jobId);
    }


    @Operation(
            summary = "Get all registered servers",
            description = "Returns a list of all servers stored in the system."
    )
    @GetMapping
    public List<RegisterServerResponse> getServers(){
        return serverService.getAllServers();
    }

    @Operation(
            summary = "Get server by host IP",
            description = "Fetches server details using its host IP address."
    )
    @GetMapping("/{host}")
    public RegisterServerResponse getServerByIP(@PathVariable String host){
        return serverService.getServerByHost(host);
    }

    @Operation(
            summary = "Delete server by host IP",
            description = "Deletes a server from the system and removes its stored credentials."
    )
    @DeleteMapping("/{host}")
    public ResponseEntity<Void> deleteServer(@PathVariable String host) {
        serverService.deleteServer(host);
        return ResponseEntity.noContent().build();
    }

}
