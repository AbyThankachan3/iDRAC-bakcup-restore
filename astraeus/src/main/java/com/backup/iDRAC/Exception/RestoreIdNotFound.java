package com.backup.iDRAC.Exception;

public class RestoreIdNotFound extends RuntimeException {
    public RestoreIdNotFound(Long restoreId) {
        super("Restore Id: " + restoreId + " not found.");
    }
}
