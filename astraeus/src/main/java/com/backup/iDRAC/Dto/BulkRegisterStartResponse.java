package com.backup.iDRAC.Dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BulkRegisterStartResponse {

    private Long jobId;

    private String status;

}