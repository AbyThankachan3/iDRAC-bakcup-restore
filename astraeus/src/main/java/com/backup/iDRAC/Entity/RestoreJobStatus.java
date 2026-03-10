package com.backup.iDRAC.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RestoreJobStatus {
    CREATED ("Created"),
    RUNNING("Running"),
    COMPLETED ("Completed"),
    COMPLETED_WITH_ERRORS ("CompletedWithErrors"),
    FAILED("Failed");

    public final String value;
}
