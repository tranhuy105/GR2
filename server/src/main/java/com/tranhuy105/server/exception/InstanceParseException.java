package com.tranhuy105.server.exception;

/**
 * Exception thrown when instance file parsing fails
 */
public class InstanceParseException extends RuntimeException {
    
    public InstanceParseException(String message) {
        super(message);
    }
    
    public InstanceParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
