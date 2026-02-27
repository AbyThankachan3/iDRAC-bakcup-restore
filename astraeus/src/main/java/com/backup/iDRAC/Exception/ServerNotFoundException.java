package com.backup.iDRAC.Exception;

public class ServerNotFoundException extends RuntimeException {

    public ServerNotFoundException(String identifier) {
        super("Server not found: " + identifier);
    }
}
