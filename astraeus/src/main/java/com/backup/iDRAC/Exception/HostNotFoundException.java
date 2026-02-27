package com.backup.iDRAC.Exception;

public class HostNotFoundException extends RuntimeException {
    public HostNotFoundException(String host) {
        super("Host not found: " + host);
    }
}
