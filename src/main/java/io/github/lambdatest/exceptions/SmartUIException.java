package io.github.lambdatest.exceptions;

/**
 * Custom exception for SmartUI operations
 */
public class SmartUIException extends Exception {
    
    public SmartUIException(String message) {
        super(message);
    }
    
    public SmartUIException(String message, Throwable cause) {
        super(message, cause);
    }
}
