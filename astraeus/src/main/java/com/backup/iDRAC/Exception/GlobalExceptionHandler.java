package com.backup.iDRAC.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.backup.iDRAC.Dto.ErrorResponse;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(ServerConnectionException.class)
    public ResponseEntity<ErrorResponse> handleConnectionFailure(ServerConnectionException ex) {
        ErrorResponse response = ErrorResponse.builder().error("Server connection failed").message(ex.getMessage()).timestamp(Instant.now().toString()).build();
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(InvalidServerCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidServerCredentialsException ex) {
        ErrorResponse response = ErrorResponse.builder().error("Invalid credentials").message(ex.getMessage()).timestamp(Instant.now().toString()).build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        ErrorResponse response = ErrorResponse.builder().error("Internal server error").message(ex.getMessage()).timestamp(Instant.now().toString()).build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(ServerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleServerNotFound(ServerNotFoundException ex) {
        ErrorResponse response = ErrorResponse.builder().error("Server not found").message(ex.getMessage()).timestamp(Instant.now().toString()).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(BackupJobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBackupJobNotFound(BackupJobNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder().error("Backup Job Not Found").message(ex.getMessage()).timestamp(Instant.now().toString()).build());
    }

    @ExceptionHandler(HostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleHostNotFound(HostNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder().error("Host Not Found").message(ex.getMessage()).timestamp(Instant.now().toString()).build());
    }

    @ExceptionHandler(ModelNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleModelNotFound(ModelNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder().error("Model Not Found").message(ex.getMessage()).timestamp(Instant.now().toString()).build());
    }

    @ExceptionHandler(FailedBackupException.class)
    public ResponseEntity<ErrorResponse> handleFailedBackup(FailedBackupException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder().error("Backup failed").message(ex.getMessage()).timestamp(Instant.now().toString()).build());
    }

    @ExceptionHandler(FileReadException.class)
    public ResponseEntity<ErrorResponse> handleFileRead(FileReadException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder().error("File Error").message(ex.getMessage()).timestamp(Instant.now().toString()).build());
    }

    @ExceptionHandler(RestoreFailedException.class)
    public ResponseEntity<ErrorResponse> handleRestoreFailed(RestoreFailedException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder().error("Restore Failed").message(ex.getMessage()).timestamp(Instant.now().toString()).build()
        );
    }

    @ExceptionHandler(RestoreIdNotFound.class)
    public ResponseEntity<ErrorResponse> handleRestoreIdNotFound(RestoreIdNotFound ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder().error("Not Found").message(ex.getMessage()).timestamp(Instant.now().toString()).build()
        );
    }

}