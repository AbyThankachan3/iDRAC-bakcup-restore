package com.idrac.restore.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreFailure {

    private String message;
    private String messageId;
    private String severity;
    private String attribute;
    private String fqdd;
    private String errCode;
}
