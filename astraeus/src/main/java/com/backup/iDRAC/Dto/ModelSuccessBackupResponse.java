package com.backup.iDRAC.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ModelSuccessBackupResponse {

    private String model;
    private long totalServers;
    private long successCount;
    private List<HostBackupResponse> backups;

    public static ModelSuccessBackupResponse empty(String model, long totalServers) {
        return ModelSuccessBackupResponse.builder()
                .model(model)
                .totalServers(totalServers)
                .successCount(0)
                .backups(List.of())
                .build();
    }
}
