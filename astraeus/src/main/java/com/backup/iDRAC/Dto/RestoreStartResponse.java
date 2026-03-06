package com.backup.iDRAC.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RestoreStartResponse {

    private Long restoreId;
    private String status;

}
