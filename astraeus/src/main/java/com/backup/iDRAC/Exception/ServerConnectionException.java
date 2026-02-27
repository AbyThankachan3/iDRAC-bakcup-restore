package com.backup.iDRAC.Exception;

public class ServerConnectionException extends RuntimeException {
    public ServerConnectionException(String host, String reason) {
        super("Could not connect to host " + host + ": " + reason);
    }
}
