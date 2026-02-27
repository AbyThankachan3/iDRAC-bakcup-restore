package com.backup.iDRAC.Exception;

public class BackupJobNotFoundException extends RuntimeException {
    public BackupJobNotFoundException(Long id) {
        super("Backup job not found with id: " + id);
    }
}