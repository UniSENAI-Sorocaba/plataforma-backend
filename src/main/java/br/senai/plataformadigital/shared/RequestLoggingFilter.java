package br.senai.plataformadigital.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Logs low-risk request metadata using the correlation ID established by CorrelationIdFilter. */
@Component
// Run immediately after correlation setup and wrap downstream filters.
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final String MDC_KEY = "correlationId";
    private static final Set<String> SAFE_METHODS = Set.of(
            "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = MDC.get(MDC_KEY);
        long startedAt = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
            logger.info("http_request_completed correlationId={} method={} status={} durationMs={}",
                    correlationId, safeMethod(request.getMethod()), response.getStatus(), elapsedMillis(startedAt));
        } catch (IOException | ServletException | RuntimeException exception) {
            // Do not include exception messages or stack traces: they can contain request data or secrets.
            logger.warn("http_request_failed correlationId={} method={} durationMs={}",
                    correlationId, safeMethod(request.getMethod()), elapsedMillis(startedAt));
            throw exception;
        }
    }

    private static String safeMethod(String method) {
        return SAFE_METHODS.contains(method) ? method : "OTHER";
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
