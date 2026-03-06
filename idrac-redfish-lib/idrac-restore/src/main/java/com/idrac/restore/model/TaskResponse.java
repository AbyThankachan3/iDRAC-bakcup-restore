package com.idrac.restore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponse {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("TaskState")
    private String taskState;

    @JsonProperty("TaskStatus")
    private String taskStatus;

    @JsonProperty("PercentComplete")
    private Integer percentComplete;

    @JsonProperty("Messages")
    private List<TaskMessage> messages;
}