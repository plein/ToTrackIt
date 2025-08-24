package com.totrackit.exception;

/**
 * Exception thrown when attempting to create a process that already exists.
 */
public class ProcessAlreadyExistsException extends RuntimeException {
    
    public ProcessAlreadyExistsException(String message) {
        super(message);
    }
    
    public ProcessAlreadyExistsException(String name, String processId) {
        super(String.format("Active process already exists: name='%s', id='%s'", name, processId));
    }
}