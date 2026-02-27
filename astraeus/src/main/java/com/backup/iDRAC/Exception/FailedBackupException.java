package com.backup.iDRAC.Exception;

public class FailedBackupException extends RuntimeException {
    public FailedBackupException(String host) {
        super("Backup failed for host: " + host);
    }
}
