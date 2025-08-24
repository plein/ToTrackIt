package com.totrackit.dto;

import io.micronaut.core.annotation.Introspected;

/**
 * Pagination parameters for listing operations.
 */
@Introspected
public class Pageable {
    
    private int limit = 20;
    private int offset = 0;
    
    // Default constructor
    public Pageable() {}
    
    // Constructor
    public Pageable(int limit, int offset) {
        this.limit = Math.max(1, Math.min(limit, 100)); // Limit between 1 and 100
        this.offset = Math.max(0, offset);
    }
    
    // Getters and setters
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = Math.max(1, Math.min(limit, 100)); // Limit between 1 and 100
    }
    
    public int getOffset() {
        return offset;
    }
    
    public void setOffset(int offset) {
        this.offset = Math.max(0, offset);
    }
    
    @Override
    public String toString() {
        return "Pageable{" +
                "limit=" + limit +
                ", offset=" + offset +
                '}';
    }
}