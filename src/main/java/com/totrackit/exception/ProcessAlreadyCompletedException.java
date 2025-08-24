package com.totrackit.exception;

/**
 * Exception thrown when attempting to complete a process that is already completed.
 */
public class ProcessAlreadyCompletedException extends RuntimeException {
    
    public ProcessAlreadyCompletedException(String message) {
        super(message);
    }
    
    public ProcessAlreadyCompletedException(String name, String processId) {
        super(String.format("Process is already completed: name='%s', id='%s'", name, processId));
    }
}