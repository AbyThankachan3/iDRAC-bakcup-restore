package com.idrac.restore.model;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NetworkConfig {

    private boolean dhcpEnabled;
    private String staticAddress;
    private String staticNetmask;
    private String staticGateway;
    private String staticDns1;
    private String staticDns2;

}
