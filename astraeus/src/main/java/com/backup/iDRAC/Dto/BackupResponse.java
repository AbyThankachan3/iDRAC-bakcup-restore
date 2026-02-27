package com.backup.iDRAC.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BackupResponse {

    private Long jobId;
    private int totalServers;
    private int successCount;
    private int failureCount;
}
