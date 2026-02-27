package com.backup.iDRAC.Service;

import com.backup.iDRAC.Dto.*;
import com.backup.iDRAC.Entity.*;
import com.backup.iDRAC.Exception.BackupJobNotFoundException;
import com.backup.iDRAC.Exception.HostNotFoundException;
import com.backup.iDRAC.Exception.ModelNotFoundException;
import com.backup.iDRAC.Repostiory.BackupHostLogRepository;
import com.backup.iDRAC.Repostiory.BackupJobRepository;
import com.backup.iDRAC.Repostiory.IdracServerRepository;
import com.idrac.api.RedfishClient;
import com.idrac.client.RedfishClientBuilder;
import com.idrac.config.RedfishConnection;
import com.idrac.model.ExportFormat;
import com.idrac.model.ExportTarget;
import com.idrac.model.ExportUse;
import com.idrac.model.IncludeInExport;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class BackupService {

    @Autowired
    private IdracServerRepository idracServerRepository;
    @Autowired
    private BackupJobRepository backupJobRepository;
    @Autowired
    private BackupHostLogRepository backupHostLogRepository;
    @Autowired
    private VaultService vaultService;

    @Transactional
    public BackupResponse backupAllServers() {

        List<IdracServer> servers = idracServerRepository.findAll();
        BackupJob job = BackupJob.builder()
                .startedAt(Instant.now())
                .totalServers(servers.size())
                .status(BackupJobStatus.RUNNING)
                .build();
        job = backupJobRepository.save(job);

        int success = 0;
        int failure = 0;

        for (IdracServer server : servers) {
            long start = System.currentTimeMillis();
            try {
                String filePath = backupSingleServer(server);
                backupHostLogRepository.save(
                        BackupHostLog.builder()
                                .backupJob(job)
                                .host(server.getHost())
                                .status(BackupHostStatus.SUCCESS)
                                .filePath(filePath)
                                .durationMillis(System.currentTimeMillis() - start)
                                .createdAt(Instant.now())
                                .build()
                );
                success++;

            } catch (Exception e) {
                backupHostLogRepository.save(
                        BackupHostLog.builder()
                                .backupJob(job)
                                .createdAt(Instant.now())
                                .host(server.getHost())
                                .status(BackupHostStatus.FAILED)
                                .errorMessage(e.getMessage())
                                .durationMillis(System.currentTimeMillis() - start)
                                .createdAt(Instant.now())
                                .build()
                );
                failure++;
            }
        }

        job.setFinishedAt(Instant.now());
        job.setSuccessCount(success);
        job.setFailureCount(failure);
        job.setStatus(failure > 0 ? BackupJobStatus.FAILED : BackupJobStatus.COMPLETED);
        backupJobRepository.save(job);

        return BackupResponse.builder()
                .jobId(job.getId())
                .totalServers(job.getTotalServers())
                .successCount(success)
                .failureCount(failure)
                .build();
    }

    public HostBackupResponse backupSingleHost(String host) {

        IdracServer server = idracServerRepository.findByHost(host).orElseThrow(() -> new HostNotFoundException(host));
        BackupJob job = BackupJob.builder()
                .startedAt(Instant.now())
                .totalServers(1)
                .status(BackupJobStatus.RUNNING)
                .build();
        job = backupJobRepository.save(job);
        int success = 0;
        int failure = 0;
        long start = System.currentTimeMillis();
        String filePath = null;
        long duration;
        BackupHostLog response;
        try {
            filePath = backupSingleServer(server);
            duration = System.currentTimeMillis() - start;
            response = backupHostLogRepository.save(
                    BackupHostLog.builder()
                            .host(host)
                            .backupJob(job)
                            .status(BackupHostStatus.SUCCESS)
                            .filePath(filePath)
                            .durationMillis(duration)
                            .createdAt(Instant.now())
                            .build()
            );
            success++;

        } catch (Exception ex) {

            duration = System.currentTimeMillis() - start;
            response = backupHostLogRepository.save(
                    BackupHostLog.builder()
                            .host(host)
                            .backupJob(job)
                            .status(BackupHostStatus.FAILED)
                            .filePath(null)
                            .durationMillis(duration)
                            .createdAt(Instant.now())
                            .build()
            );
            failure++;
        }
        job.setFinishedAt(Instant.now());
        job.setSuccessCount(success);
        job.setFailureCount(failure);
        job.setStatus(failure > 0 ? BackupJobStatus.FAILED : BackupJobStatus.COMPLETED);
        backupJobRepository.save(job);
        return HostBackupResponse.builder()
                .logId(response.getId())
                .host(host)
                .fileName(extractFileName(response.getFilePath()))
                .durationMillis(duration)
                .createdAt(Instant.now())
                .build();
    }

    private String backupSingleServer(IdracServer server) throws Exception {

        Credentials creds = vaultService.getCredentials(server.getVaultPath());
        RedfishConnection connection = RedfishConnection.builder()
                .host(server.getHost())
                .username(creds.getUsername())
                .password(creds.getPassword())
                .build();
        RedfishClient client = RedfishClientBuilder.build(connection);
        String jobId = client.triggerExport(ExportTarget.ALL, ExportFormat.XML, ExportUse.CLONE, IncludeInExport.INCLUDE_READ_ONLY, IncludeInExport.INCLUDE_PASSWORD_HASH_VALUES);
        client.pollJob(jobId, 300000);
        String scp = client.fetchScp(jobId, ExportFormat.XML);
        return saveBackupToDisk(server.getHost(), scp);
    }

    private String saveBackupToDisk(String host, String content) throws Exception {

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        Path dir = Path.of("astraeus-backups", host);
        Files.createDirectories(dir);
        Path file = dir.resolve("idrac_" + timestamp + ".xml");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toAbsolutePath().toString();
    }

    public List<BackupJobSummaryResponse> getAllJobs() {

        return backupJobRepository.findAll()
                .stream()
                .map(job -> BackupJobSummaryResponse.builder()
                        .jobId(job.getId())
                        .startedAt(job.getStartedAt())
                        .finishedAt(job.getFinishedAt())
                        .totalServers(job.getTotalServers())
                        .successCount(job.getSuccessCount())
                        .failureCount(job.getFailureCount())
                        .status(job.getStatus().name())
                        .build())
                .toList();
    }

    public BackupJobDetailResponse getJobById(Long jobId) {

        BackupJob job = backupJobRepository.findById(jobId).orElseThrow(() -> new BackupJobNotFoundException(jobId));

        List<BackupHostLogResponse> logs = backupHostLogRepository.findByBackupJobId(jobId).stream()
                .map(log -> BackupHostLogResponse.builder()
                                .logId(log.getId())
                                .host(log.getHost())
                                .status(log.getStatus().name())
                                .fileName(extractFileName(log.getFilePath()))
                                .errorMessage(log.getErrorMessage())
                                .durationMillis(log.getDurationMillis())
                                .createdAt(log.getCreatedAt())
                                .build())
                        .toList();

        return BackupJobDetailResponse.builder()
                .jobId(job.getId())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .totalServers(job.getTotalServers())
                .successCount(job.getSuccessCount())
                .failureCount(job.getFailureCount())
                .status(job.getStatus().name())
                .hostLogs(logs)
                .build();
    }

    public HostSuccessBackupResponse getSuccessfulBackupsByHostAndDate(String host, Instant from, Instant to) {

        if (!idracServerRepository.existsByHost(host)) {
            throw new HostNotFoundException(host);
        }
        List<BackupHostLog> logs;
        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("From date must be before To date");
            }
            logs = backupHostLogRepository.findByHostAndStatusAndCreatedAtBetween(host, BackupHostStatus.SUCCESS, from, to);
        } else if (from != null) {
            logs = backupHostLogRepository.findByHostAndStatusAndCreatedAtAfter(host, BackupHostStatus.SUCCESS, from);
        } else if (to != null) {
            logs = backupHostLogRepository.findByHostAndStatusAndCreatedAtBefore(host, BackupHostStatus.SUCCESS, to);
        } else {
            logs = backupHostLogRepository.findByHostAndStatus(host, BackupHostStatus.SUCCESS);
        }
        if (logs.isEmpty()) {
            return HostSuccessBackupResponse.builder().host(host).successCount(0).backups(List.of()).build();
        }
        logs.sort(Comparator.comparing(BackupHostLog::getCreatedAt).reversed());
        List<BackupFileInfoResponse> backupInfos = logs.stream()
                .map(log -> BackupFileInfoResponse.builder()
                        .logId(log.getId())
                        .fileName(extractFileName(log.getFilePath()))
                        .createdAt(log.getCreatedAt())
                        .durationMillis(log.getDurationMillis())
                        .build()).toList();
        return HostSuccessBackupResponse.builder()
                .host(host)
                .successCount(logs.size())
                .backups(backupInfos)
                .build();
    }

    public HostBackupResponse getLatestSuccessfulBackup(String host) {

        if (!idracServerRepository.existsByHost(host)) {
            throw new HostNotFoundException(host);
        }

        Optional<BackupHostLog> logOptional = backupHostLogRepository.findTopByHostAndStatusOrderByIdDesc(host, BackupHostStatus.SUCCESS);

        if (logOptional.isEmpty()) {
            return HostBackupResponse.builder().host(host).fileName(null).durationMillis(null).createdAt(null).build();
        }

        BackupHostLog latest = logOptional.get();

        return HostBackupResponse.builder()
                .logId(latest.getId())
                .host(host)
                .createdAt(latest.getCreatedAt())
                .fileName(extractFileName(latest.getFilePath()))
                .durationMillis(latest.getDurationMillis())
                .createdAt(latest.getCreatedAt())
                .build();
    }

    public ModelSuccessBackupResponse getSuccessfulBackupsByModel(String model) {
        if (!idracServerRepository.existsByModel(model)) {
            throw new ModelNotFoundException(model);
        }
        List<IdracServer> servers = idracServerRepository.findByModel(model);
        List<String> hosts = servers.stream().map(IdracServer::getHost).toList();
        List<BackupHostLog> logs = backupHostLogRepository.findByHostInAndStatus(hosts, BackupHostStatus.SUCCESS);
        if (logs.isEmpty()) {
            return ModelSuccessBackupResponse.empty(model, servers.size());
        }
        List<HostBackupResponse> entries = logs.stream().map(log -> HostBackupResponse.builder()
                .logId(log.getId())
                .host(log.getHost())
                .fileName(extractFileName(log.getFilePath()))
                .durationMillis(log.getDurationMillis()).createdAt(log.getCreatedAt()).build()).toList();
        return ModelSuccessBackupResponse.builder().model(model).totalServers(servers.size()).successCount(logs.size()).backups(entries).build();
    }

    public ResponseEntity<Resource> downloadBackupFile(Long logId) {

        BackupHostLog log = backupHostLogRepository
                .findById(logId)
                .orElseThrow(() -> new RuntimeException("Backup log not found"));

        if (log.getStatus() != BackupHostStatus.SUCCESS) {
            throw new RuntimeException("Backup not successful. File not available.");
        }

        Path path = Paths.get(log.getFilePath());

        if (!Files.exists(path)) {
            throw new RuntimeException("Backup file not found on disk");
        }

        Resource resource;
        try {
            resource = new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file path");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + path.getFileName().toString() + "\"")
                .body(resource);
    }

    private String extractFileName(String path) {
        if (path == null || path.isBlank()) return null;
        return Paths.get(path).getFileName().toString();
    }
}
