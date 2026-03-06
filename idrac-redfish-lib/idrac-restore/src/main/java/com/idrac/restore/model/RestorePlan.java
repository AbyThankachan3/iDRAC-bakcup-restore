package com.idrac.restore.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RestorePlan {

    private NetworkConfig networkConfig;
    private String currentHost;
    private String targetHost; // new ip assigned to current host
    private boolean rebootRequired;
    private boolean ipChange;

}
