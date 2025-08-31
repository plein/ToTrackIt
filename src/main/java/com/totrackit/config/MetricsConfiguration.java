package com.totrackit.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.context.annotation.Context;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration for custom application metrics.
 * Initializes metric registrations on startup.
 */
@Singleton
@Context
public class MetricsConfiguration {
    
    private static final Logger LOG = LoggerFactory.getLogger(MetricsConfiguration.class);

    @Inject
    public MetricsConfiguration(MeterRegistry meterRegistry) {
        try {
            // Initialize base metrics - actual metrics will be created dynamically with tags
            LOG.info("Metrics configuration initialized");
        } catch (Exception e) {
            LOG.warn("Failed to initialize metrics configuration", e);
        }
    }
}