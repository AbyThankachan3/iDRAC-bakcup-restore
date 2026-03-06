package com.backup.iDRAC.Exception;

public class RestoreFailedException extends RuntimeException {
    public RestoreFailedException(String message) {
        super(message);
    }
}
