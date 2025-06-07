package com.example.worklogui.exceptions;

public class CompanyOperationException extends RuntimeException {
    
    public CompanyOperationException(String message) {
        super(message);
    }
    
    public CompanyOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}