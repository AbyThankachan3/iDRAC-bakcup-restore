package com.backup.iDRAC.Dto;

import lombok.Data;

@Data
public class RestoreRequest {

    private String host;      // current IP of replacement iDRAC
    private String username;
    private String password;

}
