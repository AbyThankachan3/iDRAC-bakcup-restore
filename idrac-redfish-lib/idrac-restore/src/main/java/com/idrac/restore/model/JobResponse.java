package com.idrac.restore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class JobResponse {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("JobState")
    private String jobState;

    @JsonProperty("PercentComplete")
    private Integer percentComplete;

    @JsonProperty("Message")
    private String message;

    @JsonProperty("Messages")
    private List<JobMessage> messages;


}
