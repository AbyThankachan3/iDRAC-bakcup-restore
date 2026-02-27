package com.idrac.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExportRequest {

    @JsonProperty("ExportFormat")
    private ExportFormat exportFormat;

    @JsonProperty("ExportUse")
    private ExportUse exportUse;

    @JsonProperty("IncludeInExport")
    private String includeInExport;

    @JsonProperty("ShareParameters")
    private ShareParameters shareParameters;
}