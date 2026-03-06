package com.backup.iDRAC.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BulkRegisterFailure {
    private String host;
    private String error;
}
