package com.backup.iDRAC.Exception;

public class InvalidServerCredentialsException extends RuntimeException {
    public InvalidServerCredentialsException(String host) {
        super("Invalid credentials for host: " + host);
    }
}