package com.backup.iDRAC.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HostSuccessBackupResponse {

    private String host;
    private long successCount;
    private List<BackupFileInfoResponse> backups;
}