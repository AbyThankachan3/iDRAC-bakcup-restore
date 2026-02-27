package com.backup.iDRAC.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class BackupHostLogResponse {

    private Long logId;
    private String host;
    private String status;
    private String fileName;
    private String errorMessage;
    private Long durationMillis;
    private Instant createdAt;
}