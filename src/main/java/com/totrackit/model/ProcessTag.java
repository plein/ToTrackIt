package com.totrackit.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Represents a tag associated with a process for categorization and filtering.
 */
@Introspected
@Serdeable
public class ProcessTag {
    
    @NotBlank
    @Size(min = 1, max = 50)
    private final String key;
    
    @NotBlank
    @Size(min = 1, max = 100)
    private final String value;
    
    @JsonCreator
    public ProcessTag(@JsonProperty("key") String key, @JsonProperty("value") String value) {
        this.key = key;
        this.value = value;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessTag that = (ProcessTag) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
    
    @Override
    public String toString() {
        return "ProcessTag{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}