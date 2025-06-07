package com.example.worklogui.exceptions;

public class WorkLogNotFoundException extends Exception {
    
    public WorkLogNotFoundException(String message) {
        super(message);
    }
    
    public WorkLogNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}