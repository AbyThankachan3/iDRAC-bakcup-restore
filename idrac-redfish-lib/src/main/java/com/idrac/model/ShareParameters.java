package com.idrac.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShareParameters {

    @JsonProperty("Target")
    private ExportTarget target;

    @JsonProperty("FileName")
    private String fileName;
}