package br.senai.plataformadigital.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.senai.plataformadigital.config.CorrelationIdFilter;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {
    private final CorrelationIdFilter correlationIdFilter = new CorrelationIdFilter();
    private final RequestLoggingFilter loggingFilter = new RequestLoggingFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Test
    void usesEstablishedCorrelationIdAndLogsOnlySafeRequestMetadata() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/private@example.test");
        request.setQueryString("token=query-secret");
        request.addHeader("X-Correlation-Id", "client.safe-01");
        request.addHeader("Authorization", "Bearer header-secret");
        request.setContent("body-secret".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> idInMdc = new AtomicReference<>();
        ListAppender<ILoggingEvent> appender = captureLogs();

        try {
            runFilters(request, response, (servletRequest, servletResponse) -> {
                idInMdc.set(MDC.get("correlationId"));
                ((MockHttpServletResponse) servletResponse).setStatus(204);
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertEquals("client.safe-01", response.getHeader("X-Correlation-Id"));
        assertNull(response.getHeader("X-Request-ID"));
        assertEquals("client.safe-01", idInMdc.get());
        assertNull(MDC.get("correlationId"));

        assertEquals(1, appender.list.size());
        String log = appender.list.getFirst().getFormattedMessage();
        assertTrue(log.contains("http_request_completed"));
        assertTrue(log.contains("correlationId=client.safe-01"));
        assertTrue(log.contains("method=GET"));
        assertTrue(log.contains("status=204"));
        assertTrue(log.contains("durationMs="));
        assertFalse(log.contains("private@example.test"));
        assertFalse(log.contains("query-secret"));
        assertFalse(log.contains("header-secret"));
        assertFalse(log.contains("body-secret"));
    }

    @Test
    void replacesInvalidClientCorrelationIdAndUsesGeneratedIdInLog() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        request.addHeader("X-Correlation-Id", "invalid private value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> idInMdc = new AtomicReference<>();
        ListAppender<ILoggingEvent> appender = captureLogs();

        try {
            runFilters(request, response, (servletRequest, servletResponse) ->
                    idInMdc.set(MDC.get("correlationId")));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        String correlationId = response.getHeader("X-Correlation-Id");
        assertNotNull(correlationId);
        assertEquals(UUID.fromString(correlationId).toString(), correlationId);
        assertEquals(correlationId, idInMdc.get());
        assertNull(MDC.get("correlationId"));
        assertEquals(1, appender.list.size());
        String log = appender.list.getFirst().getFormattedMessage();
        assertTrue(log.contains("correlationId=" + correlationId));
        assertFalse(log.contains("invalid private value"));
    }

    @Test
    void preservesServerEstablishedCorrelationIdAcrossResponseAndLog() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        request.setAttribute("request.correlationId", "server-established-id");
        request.addHeader("X-Correlation-Id", "client-supplied-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ListAppender<ILoggingEvent> appender = captureLogs();

        try {
            runFilters(request, response, (servletRequest, servletResponse) -> { });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertEquals("server-established-id", response.getHeader("X-Correlation-Id"));
        assertNull(MDC.get("correlationId"));
        assertEquals(1, appender.list.size());
        String log = appender.list.getFirst().getFormattedMessage();
        assertTrue(log.contains("correlationId=server-established-id"));
        assertFalse(log.contains("client-supplied-id"));
    }

    @Test
    void failureLogOmitsExceptionDetailsAndCorrelationMdcIsCleared() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/failure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RuntimeException failure = new RuntimeException("sensitive-exception-detail");
        ListAppender<ILoggingEvent> appender = captureLogs();

        try {
            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> runFilters(request, response, (servletRequest, servletResponse) -> {
                        assertNotNull(MDC.get("correlationId"));
                        throw failure;
                    }));
            assertSame(failure, thrown);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertNotNull(response.getHeader("X-Correlation-Id"));
        assertNull(MDC.get("correlationId"));
        assertEquals(1, appender.list.size());
        String log = appender.list.getFirst().getFormattedMessage();
        assertTrue(log.contains("http_request_failed"));
        assertTrue(log.contains("correlationId="));
        assertTrue(log.contains("method=GET"));
        assertTrue(log.contains("durationMs="));
        assertFalse(log.contains("sensitive-exception-detail"));
        assertNull(appender.list.getFirst().getThrowableProxy());
    }

    @Test
    void unrecognizedMethodIsNormalizedInLog() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("CUSTOM SECRET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Correlation-Id", "safe-correlation");
        ListAppender<ILoggingEvent> appender = captureLogs();

        try {
            runFilters(request, response, (servletRequest, servletResponse) -> { });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertEquals(1, appender.list.size());
        String log = appender.list.getFirst().getFormattedMessage();
        assertTrue(log.contains("method=OTHER"));
        assertFalse(log.contains("CUSTOM SECRET"));
    }

    private void runFilters(MockHttpServletRequest request, MockHttpServletResponse response, FilterChain endpoint)
            throws ServletException, IOException {
        correlationIdFilter.doFilter(request, response,
                (servletRequest, servletResponse) -> loggingFilter.doFilter(
                        (HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, endpoint));
    }

    private ListAppender<ILoggingEvent> captureLogs() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }
}
