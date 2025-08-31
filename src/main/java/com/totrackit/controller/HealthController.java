package com.totrackit.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.totrackit.service.HealthService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller providing comprehensive health status endpoints
 * for monitoring and observability.
 */
@Controller
@Tag(name = "Health", description = "Health check and monitoring endpoints")
public class HealthController {
    
    private static final Logger LOG = LoggerFactory.getLogger(HealthController.class);
    
    private final HealthService healthService;
    
    @Inject
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }
    
    /**
     * Basic health check endpoint providing overall application status
     */
    @Get("/health/status")
    @Operation(
        summary = "Basic health check",
        description = "Returns basic application health status with component details"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Application is healthy",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Application is unhealthy",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public HttpResponse<Map<String, Object>> health() {
        LOG.debug("Health check requested");
        
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> components = new HashMap<>();
        
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("version", getClass().getPackage().getImplementationVersion() != null ? 
                   getClass().getPackage().getImplementationVersion() : "development");
        
        // Check database health
        Map<String, Object> dbHealth = healthService.checkDatabaseHealth();
        components.put("database", dbHealth);
        
        // Check application health
        Map<String, Object> appHealth = checkApplicationHealth();
        components.put("application", appHealth);
        
        health.put("components", components);
        
        // Determine overall status
        boolean isHealthy = "UP".equals(dbHealth.get("status")) && "UP".equals(appHealth.get("status"));
        health.put("status", isHealthy ? "UP" : "DOWN");
        
        LOG.info("Health check completed: status={}", health.get("status"));
        
        return isHealthy ? HttpResponse.ok(health) : HttpResponse.serverError(health);
    }
    
    /**
     * Readiness probe endpoint for Kubernetes readiness checks
     */
    @Get("/health/ready")
    @Operation(
        summary = "Readiness probe",
        description = "Returns readiness status including database connectivity and migration status"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Application is ready to serve requests",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Application is not ready",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public HttpResponse<Map<String, Object>> ready() {
        LOG.debug("Readiness check requested");
        
        Map<String, Object> readiness = new HashMap<>();
        readiness.put("timestamp", Instant.now().toString());
        
        // Check database connectivity
        Map<String, Object> dbHealth = healthService.checkDatabaseHealth();
        boolean dbReady = "UP".equals(dbHealth.get("status"));
        
        // Check migration status
        Map<String, Object> migrationStatus = healthService.checkMigrationStatus();
        boolean migrationsReady = "UP".equals(migrationStatus.get("status"));
        
        // Application is ready if database is connected and migrations are up to date
        boolean isReady = dbReady && migrationsReady;
        
        readiness.put("status", isReady ? "UP" : "DOWN");
        readiness.put("database", dbHealth);
        readiness.put("migrations", migrationStatus);
        
        LOG.info("Readiness check completed: status={}, dbReady={}, migrationsReady={}", 
                readiness.get("status"), dbReady, migrationsReady);
        
        return isReady ? HttpResponse.ok(readiness) : HttpResponse.serverError(readiness);
    }
    
    /**
     * Liveness probe endpoint for Kubernetes liveness checks
     */
    @Get("/health/live")
    @Operation(
        summary = "Liveness probe",
        description = "Returns liveness status indicating if the application is responsive"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Application is alive and responsive",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Application is not responsive",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public HttpResponse<Map<String, Object>> live() {
        LOG.debug("Liveness check requested");
        
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("timestamp", Instant.now().toString());
        
        // Check application responsiveness
        Map<String, Object> appHealth = checkApplicationHealth();
        boolean isAlive = "UP".equals(appHealth.get("status"));
        
        liveness.put("status", isAlive ? "UP" : "DOWN");
        liveness.put("application", appHealth);
        
        LOG.info("Liveness check completed: status={}", liveness.get("status"));
        
        return isAlive ? HttpResponse.ok(liveness) : HttpResponse.serverError(liveness);
    }
    

    
    /**
     * Checks application health and responsiveness
     */
    private Map<String, Object> checkApplicationHealth() {
        Map<String, Object> appHealth = new HashMap<>();
        
        try {
            // Get runtime information
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            // Calculate uptime (approximate)
            long uptime = System.currentTimeMillis() - getStartTime();
            
            appHealth.put("status", "UP");
            appHealth.put("uptime", formatUptime(uptime));
            appHealth.put("memoryUsage", String.format("%dMB/%dMB", 
                         usedMemory / (1024 * 1024), maxMemory / (1024 * 1024)));
            appHealth.put("availableProcessors", runtime.availableProcessors());
            
        } catch (Exception e) {
            LOG.warn("Application health check failed", e);
            appHealth.put("status", "DOWN");
            appHealth.put("error", e.getMessage());
        }
        
        return appHealth;
    }
    

    
    /**
     * Gets approximate application start time
     */
    private long getStartTime() {
        // This is a simple approximation - in a real application you might want to
        // track the actual start time more precisely
        return System.currentTimeMillis() - java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
    }
    
    /**
     * Formats uptime in a human-readable format
     */
    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}