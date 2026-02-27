package com.backup.iDRAC.Dto;

import lombok.Data;

@Data
public class RegisterServerRequest {

    private String host;
    private String username;
    private String password;
}
