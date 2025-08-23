package com.totrackit.model;

/**
 * Enumeration representing the status of a process.
 */
public enum ProcessStatus {
    /**
     * Process is currently running/active
     */
    ACTIVE,
    
    /**
     * Process has completed successfully
     */
    COMPLETED,
    
    /**
     * Process has failed
     */
    FAILED
}