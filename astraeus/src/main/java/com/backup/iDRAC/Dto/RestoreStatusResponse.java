package com.backup.iDRAC.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreStatusResponse {

    private Long restoreId;
    private String status;
    private Integer percent;
    private String redfishJobId;
    private String initialHost;
    private String finalHost;
    private Boolean rebootRequired;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

}