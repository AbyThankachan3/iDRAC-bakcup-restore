package com.backup.iDRAC.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Response returned after successful server registration")
public class RegisterServerResponse {
    @Schema(description = "Unique database ID of the server", example = "1")
    private Long id;
    @Schema(description = "Server IP address", example = "192.168.64.4")
    private String host;
    @Schema(description = "Detected Server model", example = "PowerEdge R450")
    private String model;
    private Long registrationJobId;
}
