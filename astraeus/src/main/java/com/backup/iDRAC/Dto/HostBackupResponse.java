package com.backup.iDRAC.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class HostBackupResponse {

    private Long logId;
    private String host;
    private String fileName;
    private Long durationMillis;
    private Instant createdAt;
}