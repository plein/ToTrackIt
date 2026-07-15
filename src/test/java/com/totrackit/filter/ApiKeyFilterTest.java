package com.totrackit.filter;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration test for ApiKeyFilter with an API key configured.
 * Verifies /processes routes require the X-API-KEY header while
 * health endpoints remain open.
 */
@MicronautTest
@Property(name = ApiKeyFilter.API_KEY_PROPERTY, value = "test-key-123")
public class ApiKeyFilterTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    public void testProcessesRejectedWithoutKey() {
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(HttpRequest.GET("/processes"), String.class));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    public void testProcessesRejectedWithWrongKey() {
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(
                        HttpRequest.GET("/processes").header(ApiKeyFilter.API_KEY_HEADER, "wrong-key"),
                        String.class));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    public void testProcessesAcceptedWithCorrectKey() {
        var response = client.toBlocking().exchange(
                HttpRequest.GET("/processes").header(ApiKeyFilter.API_KEY_HEADER, "test-key-123"),
                String.class);

        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Test
    public void testHealthRemainsOpenWithoutKey() {
        var response = client.toBlocking().exchange(HttpRequest.GET("/health/live"), String.class);

        assertEquals(HttpStatus.OK, response.getStatus());
    }
}
