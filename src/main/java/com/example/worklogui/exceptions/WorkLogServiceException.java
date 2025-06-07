package com.example.worklogui.exceptions;

public class WorkLogServiceException extends Exception {
    
    public WorkLogServiceException(String message) {
        super(message);
    }
    
    public WorkLogServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}