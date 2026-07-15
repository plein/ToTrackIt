package com.totrackit.filter;

import com.totrackit.dto.ErrorResponse;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Optional static API key enforcement for the process API.
 *
 * Only active when the {@code totrackit.api-key} property (or the
 * {@code TOTRACKIT_API_KEY} environment variable) is set to a non-blank value.
 * When active, all /processes routes require a matching {@code X-API-KEY}
 * header. Health, metrics, and API documentation endpoints remain open.
 *
 * This is intentionally a single shared key for self-hosted deployments, not a
 * user/tenant management system. Request-level auth lives in this filter layer
 * so richer schemes can replace it without touching business logic.
 */
// NOTE: @Requires(property=...) without notEquals — notEquals is also satisfied
// when the property is absent, which would activate this bean with an
// unresolvable @Value placeholder. Blank values are handled in doFilter instead.
@Filter("/processes/**")
@Requires(property = ApiKeyFilter.API_KEY_PROPERTY)
public class ApiKeyFilter implements HttpServerFilter {

    public static final String API_KEY_PROPERTY = "totrackit.api-key";
    public static final String API_KEY_HEADER = "X-API-KEY";

    private static final Logger LOG = LoggerFactory.getLogger(ApiKeyFilter.class);

    private final String apiKey;

    public ApiKeyFilter(@Value("${" + API_KEY_PROPERTY + "}") String apiKey) {
        this.apiKey = apiKey;
        if (apiKey == null || apiKey.isBlank()) {
            LOG.warn("{} is set but blank - API key enforcement is DISABLED", API_KEY_PROPERTY);
        } else {
            LOG.info("API key enforcement enabled for /processes routes");
        }
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        if (apiKey == null || apiKey.isBlank()) {
            return chain.proceed(request);
        }

        String provided = request.getHeaders().get(API_KEY_HEADER);
        if (provided != null && MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8))) {
            return chain.proceed(request);
        }

        ErrorResponse error = new ErrorResponse(
                "UNAUTHORIZED",
                "Missing or invalid " + API_KEY_HEADER + " header",
                request.getPath());
        return Publishers.just(HttpResponse.unauthorized().body(error));
    }

    @Override
    public int getOrder() {
        return ServerFilterPhase.SECURITY.order();
    }
}
