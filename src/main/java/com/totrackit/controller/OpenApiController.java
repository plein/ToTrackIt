package com.totrackit.controller;

import io.micronaut.core.io.ResourceResolver;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class OpenApiController {
    
    private static final Logger LOG = LoggerFactory.getLogger(OpenApiController.class);
    
    private final ResourceResolver resourceResolver;
    
    @Inject
    public OpenApiController(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }
    
    @Get("/openapi.yml")
    @Produces(MediaType.TEXT_PLAIN)
    public HttpResponse<String> getOpenApiSpec() {
        try {
            Optional<InputStream> resource = resourceResolver.getResourceAsStream("classpath:openapi.yml");
            
            if (resource.isPresent()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.get()))) {
                    String content = reader.lines().collect(Collectors.joining("\n"));
                    LOG.info("Serving OpenAPI specification");
                    return HttpResponse.ok(content);
                }
            } else {
                LOG.warn("OpenAPI specification file not found");
                return HttpResponse.notFound("OpenAPI specification not found");
            }
        } catch (IOException e) {
            LOG.error("Error reading OpenAPI specification", e);
            return HttpResponse.serverError("Error reading OpenAPI specification");
        }
    }
    
    @Get("/swagger-ui")
    public HttpResponse<String> getSwaggerUi() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>ToTrackIt API Documentation</title>
                <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui.css" />
                <style>
                    html {
                        box-sizing: border-box;
                        overflow: -moz-scrollbars-vertical;
                        overflow-y: scroll;
                    }
                    *, *:before, *:after {
                        box-sizing: inherit;
                    }
                    body {
                        margin:0;
                        background: #fafafa;
                    }
                </style>
            </head>
            <body>
                <div id="swagger-ui"></div>
                <script src="https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui-bundle.js"></script>
                <script src="https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui-standalone-preset.js"></script>
                <script>
                    window.onload = function() {
                        const ui = SwaggerUIBundle({
                            url: '/openapi.yml',
                            dom_id: '#swagger-ui',
                            deepLinking: true,
                            presets: [
                                SwaggerUIBundle.presets.apis,
                                SwaggerUIStandalonePreset
                            ],
                            plugins: [
                                SwaggerUIBundle.plugins.DownloadUrl
                            ],
                            layout: "StandaloneLayout"
                        });
                    };
                </script>
            </body>
            </html>
            """;
        
        return HttpResponse.ok(html).contentType(MediaType.TEXT_HTML);
    }
}