package com.backup.iDRAC.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BackupStartResponse {

    private Long jobId;
    private String status;

}
