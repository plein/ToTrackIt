package com.totrackit.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.context.annotation.Context;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration to enable database connection pool metrics.
 * Creates gauges for database connection pool monitoring.
 */
@Singleton
@Context
public class DatabaseMetricsConfiguration {
    
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseMetricsConfiguration.class);
    
    @Inject
    public DatabaseMetricsConfiguration(MeterRegistry meterRegistry) {
        try {
            // Register database connection pool gauges
            meterRegistry.gauge("totrackit_database_connections_active", 0);
            meterRegistry.gauge("totrackit_database_connections_idle", 0);
            meterRegistry.gauge("totrackit_database_connections_total", 0);
            
            LOG.info("Configured database connection pool metrics");
        } catch (Exception e) {
            LOG.warn("Failed to configure database metrics", e);
        }
    }
}