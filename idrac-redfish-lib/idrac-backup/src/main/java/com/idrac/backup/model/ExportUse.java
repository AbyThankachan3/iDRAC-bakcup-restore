package com.idrac.backup.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExportUse {
    DEFAULT("Default"),
    CLONE("Clone"),
    REPLACE("Replace");

    @JsonValue
    private final String value;
}
