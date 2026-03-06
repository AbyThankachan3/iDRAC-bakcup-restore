package com.backup.iDRAC.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Credentials {
    private final String username;
    private final String password;
}
