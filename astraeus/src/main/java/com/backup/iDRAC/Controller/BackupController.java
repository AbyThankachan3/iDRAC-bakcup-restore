package com.backup.iDRAC.Controller;

import com.backup.iDRAC.Dto.*;
import com.backup.iDRAC.Service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/backup")
@Tag(name = "Backup API", description = "Backup management endpoints")
public class BackupController {

    @Autowired
    private BackupService backupService;

    @Operation(
            summary = "Trigger backup for all servers",
            description = "Creates a backup job for all registered iDRAC servers."
    )
    @PostMapping
    public BackupStartResponse backupAll(){
        Long jobId = backupService.startBackupAllServers();
        return BackupStartResponse.builder()
                .jobId(jobId)
                .status("STARTED")
                .build();
    }


    @Operation(
            summary = "Trigger backup for a single host",
            description = "Creates a backup job for a specific server."
    )
    @PostMapping("/{host}")
    public BackupStartResponse backup(@PathVariable String host){
        Long jobId = backupService.startBackupSingleHost(host);
        return BackupStartResponse.builder()
                .jobId(jobId)
                .status("STARTED")
                .build();
    }

    @Operation(summary = "Get all the created jobs",
            description = "Get all the created jobs.")
    @GetMapping("/jobs")
    public List<BackupJobSummaryResponse> getAllJobs() {
        return backupService.getAllJobs();
    }

    @Operation(summary = "Get job details for a particular job id",
            description = "Get all details of a particular job id.")
    @GetMapping("/jobs/{id}")
    public BackupJobDetailResponse getJob(@PathVariable Long id) {
        return backupService.getJobById(id);
    }

    @Operation(summary = "Get details of completed backups for a host.",
            description = "Get details of completed backups for a particular host ip.")
    @GetMapping("/hosts/{host}/success")
    public HostSuccessBackupResponse getHostSuccessBackupsByDate(@PathVariable String host, @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        return backupService.getSuccessfulBackupsByHostAndDate(host, from, to);
    }

    @Operation(summary = "Get latest backup details for a particular host ip.",
            description = "Get details of latest backup completed for a particular host ip.")
    @GetMapping("/hosts/{host}/latest")
    public ResponseEntity<HostBackupResponse> getLatestHostBackup(@PathVariable String host) {
        HostBackupResponse response = backupService.getLatestSuccessfulBackup(host);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get backup details for a particular server model.",
            description = "Get backup related details for a server model (Eg. PowerEdge R450).")
    @GetMapping("/models/success")
    public ModelSuccessBackupResponse getModelSuccessBackups(@RequestParam String model) {
        return backupService.getSuccessfulBackupsByModel(model);
    }

    @Operation(
            summary = "Download backup file",
            description = "Downloads the backup file associated with a successful backup log."
    )
    @Parameter(description = "Backup host log ID", example = "5")
    @GetMapping("/download/{logId}")
    public ResponseEntity<Resource> downloadBackup(@PathVariable Long logId) {
        return backupService.downloadBackupFile(logId);
    }
}
