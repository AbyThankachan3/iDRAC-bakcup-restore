package com.idrac.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IncludeInExport {
    DEFAULT("Default"),
    INCLUDE_READ_ONLY("IncludeReadOnly"),
    INCLUDE_PASSWORD_HASH_VALUES("IncludePasswordHashValues"),
    INCLUDE_CUSTOM_TELEMETRY("IncludeCustomTelemetry");

    @JsonValue
    private final String value;
}
