package com.idrac.backup.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JobResponse {

    @JsonProperty("JobState")
    private String jobState;

    @JsonProperty("PercentComplete")
    private Integer percentComplete;

    @JsonProperty("Message")
    private String message;
}