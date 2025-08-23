package com.totrackit.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessTagTest {

    @Test
    void testProcessTagCreation() {
        ProcessTag tag = new ProcessTag("environment", "production");
        
        assertEquals("environment", tag.getKey());
        assertEquals("production", tag.getValue());
    }

    @Test
    void testProcessTagEquality() {
        ProcessTag tag1 = new ProcessTag("environment", "production");
        ProcessTag tag2 = new ProcessTag("environment", "production");
        ProcessTag tag3 = new ProcessTag("environment", "staging");
        
        assertEquals(tag1, tag2);
        assertNotEquals(tag1, tag3);
        assertEquals(tag1.hashCode(), tag2.hashCode());
    }

    @Test
    void testProcessTagToString() {
        ProcessTag tag = new ProcessTag("team", "backend");
        String expected = "ProcessTag{key='team', value='backend'}";
        
        assertEquals(expected, tag.toString());
    }
}