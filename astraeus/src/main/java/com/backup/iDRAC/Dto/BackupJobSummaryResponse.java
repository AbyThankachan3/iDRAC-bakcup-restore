package com.backup.iDRAC.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class BackupJobSummaryResponse {

    private Long jobId;
    private Instant startedAt;
    private Instant finishedAt;
    private int totalServers;
    private int successCount;
    private int failureCount;
    private String status;
}