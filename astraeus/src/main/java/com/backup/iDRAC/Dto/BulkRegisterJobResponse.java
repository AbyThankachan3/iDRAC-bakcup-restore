package com.backup.iDRAC.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class BulkRegisterJobResponse {

    private Long jobId;
    private String status;
    private int total;
    private int successCount;
    private int failureCount;
    private Instant startedAt;
    private Instant finishedAt;
    private List<BulkRegisterFailure> failures;

}