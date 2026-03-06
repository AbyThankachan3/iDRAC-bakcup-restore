package com.idrac.restore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RestoreRequest {

    @JsonProperty("ImportBuffer")
    private String importBuffer;

    @JsonProperty("ShutdownType")
    private String shutdownType;

    @JsonProperty("HostPowerState")
    private String hostPowerState;

    @JsonProperty("ShareParameters")
    private Map<String, String> shareParameters;

}