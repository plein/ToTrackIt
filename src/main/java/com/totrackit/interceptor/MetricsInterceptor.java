package com.totrackit.interceptor;

import com.totrackit.service.MetricsService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/**
 * HTTP filter to record request metrics for all API endpoints.
 * Captures request method, path, status code, and duration.
 */
@Filter("/**")
public class MetricsInterceptor implements HttpServerFilter {
    
    private static final Logger LOG = LoggerFactory.getLogger(MetricsInterceptor.class);
    
    private final MetricsService metricsService;
    
    @Inject
    public MetricsInterceptor(MetricsService metricsService) {
        this.metricsService = metricsService;
    }
    
    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        long startTime = System.currentTimeMillis();
        
        return Flux.from(chain.proceed(request))
                .doOnNext(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    recordMetrics(request, response, duration);
                })
                .doOnError(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    recordErrorMetrics(request, throwable, duration);
                });
    }
    
    private void recordMetrics(HttpRequest<?> request, MutableHttpResponse<?> response, long duration) {
        try {
            String method = request.getMethod().name();
            String path = sanitizePath(request.getPath());
            int statusCode = response.getStatus().getCode();
            
            metricsService.recordHttpRequest(method, path, statusCode, duration);
        } catch (Exception e) {
            LOG.warn("Failed to record HTTP request metrics", e);
        }
    }
    
    private void recordErrorMetrics(HttpRequest<?> request, Throwable throwable, long duration) {
        try {
            String method = request.getMethod().name();
            String path = sanitizePath(request.getPath());
            int statusCode = 500; // Default to 500 for unhandled errors
            
            metricsService.recordHttpRequest(method, path, statusCode, duration);
        } catch (Exception e) {
            LOG.warn("Failed to record HTTP error metrics", e);
        }
    }
    
    /**
     * Sanitizes the request path to avoid high cardinality metrics.
     * Replaces dynamic path segments with placeholders.
     */
    private String sanitizePath(String path) {
        if (path == null) {
            return "unknown";
        }
        
        // Replace common dynamic segments with placeholders
        String sanitized = path
                .replaceAll("/v1/processes/[^/]+/[^/]+", "/v1/processes/{name}/{id}")
                .replaceAll("/v1/processes/[^/]+", "/v1/processes/{name}")
                .replaceAll("/swagger-ui/.*", "/swagger-ui/**")
                .replaceAll("/swagger/.*", "/swagger/**");
        
        // Limit path length to prevent extremely long paths
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100) + "...";
        }
        
        return sanitized;
    }
}