package com.backup.iDRAC.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BackupFileInfoResponse {

    private Long logId;
    private String fileName;
    private Instant createdAt;
    private Long durationMillis;
}
