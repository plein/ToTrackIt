package com.totrackit.model;

/**
 * Enumeration representing the deadline status of a process.
 */
public enum DeadlineStatus {
    /**
     * Process is on track to meet its deadline
     */
    ON_TRACK,
    
    /**
     * Process has missed its deadline (still active)
     */
    MISSED,
    
    /**
     * Process completed on time
     */
    COMPLETED_ON_TIME,
    
    /**
     * Process completed after the deadline
     */
    COMPLETED_LATE
}