package com.idrac.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExportFormat {
    XML("XML"),
    JSON("JSON");

    @JsonValue
    private final String value;

}
