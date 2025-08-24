package com.totrackit.exception;

/**
 * Exception thrown when a requested process is not found.
 */
public class ProcessNotFoundException extends RuntimeException {
    
    public ProcessNotFoundException(String message) {
        super(message);
    }
    
    public ProcessNotFoundException(String name, String processId) {
        super(String.format("Process not found: name='%s', id='%s'", name, processId));
    }
}