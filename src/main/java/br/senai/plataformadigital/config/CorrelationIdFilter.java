package br.senai.plataformadigital.config;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    static final String HEADER = "X-Correlation-Id";
    static final String MDC_KEY = "correlationId";
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Object established = request.getAttribute("request.correlationId");
        String correlationId = established instanceof String value && isSafeCorrelationId(value)
                ? value
                : request.getHeader(HEADER);
        if (!isSafeCorrelationId(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        request.setAttribute("request.correlationId", correlationId);
        response.setHeader(HEADER, correlationId);
        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, correlationId)) {
            filterChain.doFilter(request, response);
        }
    }

    private static boolean isSafeCorrelationId(String correlationId) {
        return correlationId != null && SAFE_CORRELATION_ID.matcher(correlationId).matches();
    }
}
