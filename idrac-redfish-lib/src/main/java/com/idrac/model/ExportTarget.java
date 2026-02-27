package com.idrac.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExportTarget {
    ALL("ALL"),
    RAID("RAID"),
    BIOS("BIOS"),
    IDRAC("iDRAC"),
    NIC("NIC"),
    FIBRE_CHANNEL("Fibre Channel"),
    INFINI_BAND("InfiniBand");

    @JsonValue
    private final String value;

}
