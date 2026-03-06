package com.idrac.restore.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreTaskResult {

    private String jobId;
    private String taskState;
    private String taskStatus;
    private Integer percentComplete;
    private int failureCount;
    private int warningCount;
    private List<RestoreFailure> failures;
    private List<TaskMessage> warnings;
    private List<JsonNode> rawMessages;
}
