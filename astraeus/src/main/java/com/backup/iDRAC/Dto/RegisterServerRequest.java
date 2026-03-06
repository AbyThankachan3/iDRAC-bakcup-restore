package com.backup.iDRAC.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterServerRequest {

    private String host;
    private String username;
    private String password;
}
