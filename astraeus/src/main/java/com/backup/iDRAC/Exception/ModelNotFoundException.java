package com.backup.iDRAC.Exception;

public class ModelNotFoundException extends RuntimeException {
    public ModelNotFoundException(String model) {
        super("Model not found: " + model);
    }
}