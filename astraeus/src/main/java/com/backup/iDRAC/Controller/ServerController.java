package com.backup.iDRAC.Controller;

import com.backup.iDRAC.Dto.RegisterServerRequest;
import com.backup.iDRAC.Dto.RegisterServerResponse;
import com.backup.iDRAC.Entity.IdracServer;
import com.backup.iDRAC.Service.ServerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Server registered successfully",
                    content = @Content(schema = @Schema(implementation = RegisterServerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "502", description = "Unable to connect to iDRAC server")
    })
    @PostMapping
    public RegisterServerResponse registerServer(@RequestBody RegisterServerRequest serverDetails){
        IdracServer server = serverService.registerServer(serverDetails);
        return new RegisterServerResponse(server.getId(), server.getHost(), server.getModel());
    }

    @Operation(
            summary = "Get all registered servers",
            description = "Returns a list of all servers stored in the system."
    )
    @ApiResponse(responseCode = "200", description = "List of registered servers")
    @GetMapping
    public List<RegisterServerResponse> getServers(){
        return serverService.getAllServers();
    }

    @Operation(
            summary = "Get server by host IP",
            description = "Fetches server details using its host IP address."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Server found",
                    content = @Content(schema = @Schema(implementation = RegisterServerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Server not found")
    })
    @GetMapping("/{host}")
    public RegisterServerResponse getServerByIP(@PathVariable String host){
        return serverService.getServerByHost(host);
    }

    @Operation(
            summary = "Delete server by host IP",
            description = "Deletes a server from the system and removes its stored credentials."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Server deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Server not found")
    })
    @DeleteMapping("/{host}")
    public ResponseEntity<Void> deleteServer(@PathVariable String host) {
        serverService.deleteServer(host);
        return ResponseEntity.noContent().build();
    }

}
