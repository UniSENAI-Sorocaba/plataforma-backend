package br.senai.plataformadigital.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Logs only low-risk request metadata and assigns a server-generated correlation ID. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String CORRELATION_HEADER = "X-Request-ID";
    private static final String MDC_KEY = "requestId";
    private static final Set<String> SAFE_METHODS = Set.of(
            "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        MDC.put(MDC_KEY, requestId);
        response.setHeader(CORRELATION_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
            logger.info("http_request_completed requestId={} method={} status={} durationMs={}",
                    requestId, safeMethod(request.getMethod()), response.getStatus(), elapsedMillis(startedAt));
        } catch (IOException | ServletException | RuntimeException exception) {
            // Do not include exception messages or stack traces: they can contain request data or secrets.
            logger.warn("http_request_failed requestId={} method={} durationMs={}",
                    requestId, safeMethod(request.getMethod()), elapsedMillis(startedAt));
            throw exception;
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private static String safeMethod(String method) {
        return SAFE_METHODS.contains(method) ? method : "OTHER";
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
