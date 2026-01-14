package com.tranhuy105.server.exception;

/**
 * Exception thrown when optimization fails
 */
public class OptimizationException extends RuntimeException {
    
    public OptimizationException(String message) {
        super(message);
    }
    
    public OptimizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
