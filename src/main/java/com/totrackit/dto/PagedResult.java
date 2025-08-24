package com.totrackit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/**
 * Generic paged result wrapper for API responses.
 */
@Introspected
@Serdeable
public class PagedResult<T> {
    
    @JsonProperty("data")
    private List<T> data;
    
    @JsonProperty("total")
    private long total;
    
    @JsonProperty("limit")
    private int limit;
    
    @JsonProperty("offset")
    private int offset;
    
    @JsonProperty("has_more")
    private boolean hasMore;
    
    // Default constructor
    public PagedResult() {}
    
    // Constructor
    public PagedResult(List<T> data, long total, int limit, int offset) {
        this.data = data;
        this.total = total;
        this.limit = limit;
        this.offset = offset;
        this.hasMore = (offset + data.size()) < total;
    }
    
    // Getters and setters
    public List<T> getData() {
        return data;
    }
    
    public void setData(List<T> data) {
        this.data = data;
    }
    
    public long getTotal() {
        return total;
    }
    
    public void setTotal(long total) {
        this.total = total;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public void setOffset(int offset) {
        this.offset = offset;
    }
    
    public boolean isHasMore() {
        return hasMore;
    }
    
    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
    
    @Override
    public String toString() {
        return "PagedResult{" +
                "data=" + data +
                ", total=" + total +
                ", limit=" + limit +
                ", offset=" + offset +
                ", hasMore=" + hasMore +
                '}';
    }
}