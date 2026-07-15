package com.totrackit.controller;

import com.totrackit.dto.TagImpactResponse;
import com.totrackit.service.AnalyticsService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/analytics")
@Validated
@Tag(name = "Analytics", description = "Aggregated statistics across processes")
public class AnalyticsController {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;

    @Inject
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Get("/tags")
    @Operation(
        summary = "Tag impact breakdown",
        description = "Aggregates deadline outcomes (overdue, late, on-time, failed) per tag key/value, "
                + "most problematic tags first. Use it to see which segment (country, locale, provider, ...) "
                + "a problem is concentrated in."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Per-tag impact breakdown",
        content = @Content(schema = @Schema(implementation = TagImpactResponse.class))
    )
    public HttpResponse<TagImpactResponse> tagImpact(
            @Parameter(description = "Filter by process name")
            @QueryValue @Nullable String name,

            @Parameter(description = "Include processes finished within the last N hours (1-720, default 24). "
                    + "Active processes are always included.")
            @QueryValue(value = "window_hours", defaultValue = "24") @Min(1) @Max(720) int windowHours) {

        LOG.debug("Computing tag impact: name={}, windowHours={}", name, windowHours);
        return HttpResponse.ok(analyticsService.getTagImpact(name, windowHours));
    }
}
