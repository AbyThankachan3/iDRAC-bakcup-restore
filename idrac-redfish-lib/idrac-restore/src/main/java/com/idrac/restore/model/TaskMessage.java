package com.idrac.restore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskMessage {

    @JsonProperty("Message")
    private String message;

    @JsonProperty("Severity")
    private String severity;

    @JsonProperty("MessageID")
    private String messageId;

    @JsonProperty("Oem")
    private JsonNode oem;
}
