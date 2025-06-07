package com.example.worklogui.exceptions;

public class WorkLogValidationException extends Exception {
    
    public WorkLogValidationException(String message) {
        super(message);
    }
    
    public WorkLogValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}