package com.idrac.backup.config;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
public class RedfishConnection {

    private final String host;
    private final String username;
    private final String password;

    public String getBaseUrl() {
        return "https://" + host;
    }
}