package br.senai.plataformadigital.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
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
    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Test
    void generatesCorrelationIdAndLogsOnlySafeRequestMetadata() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/private@example.test");
        request.setQueryString("token=query-secret");
        request.addHeader(RequestLoggingFilter.CORRELATION_HEADER, "client-supplied-id");
        request.addHeader("Authorization", "Bearer header-secret");
        request.setContent("body-secret".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> idInMdc = new AtomicReference<>();
        ListAppender<ILoggingEvent> appender = captureLogs();

        try {
            filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                idInMdc.set(MDC.get("requestId"));
                ((MockHttpServletResponse) servletResponse).setStatus(204);
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        String requestId = response.getHeader(RequestLoggingFilter.CORRELATION_HEADER);
        assertNotNull(requestId);
        assertNotEquals("client-supplied-id", requestId);
        assertEquals(UUID.fromString(requestId).toString(), requestId);
        assertEquals(requestId, idInMdc.get());
        assertNull(MDC.get("requestId"));

        assertEquals(1, appender.list.size());
        String log = appender.list.getFirst().getFormattedMessage();
        assertTrue(log.contains("http_request_completed"));
        assertTrue(log.contains("requestId=" + requestId));
        assertTrue(log.contains("method=GET"));
        assertTrue(log.contains("status=204"));
        assertTrue(log.contains("durationMs="));
        assertFalse(log.contains("private@example.test"));
        assertFalse(log.contains("query-secret"));
        assertFalse(log.contains("header-secret"));
        assertFalse(log.contains("body-secret"));
    }

    @Test
    void failureLogOmitsExceptionDetailsAndClearsMdc() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/failure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RuntimeException failure = new RuntimeException("sensitive-exception-detail");
        ListAppender<ILoggingEvent> appender = captureLogs();

        try {
            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                        assertNotNull(MDC.get("requestId"));
                        throw failure;
                    }));
            assertSame(failure, thrown);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertNotNull(response.getHeader(RequestLoggingFilter.CORRELATION_HEADER));
        assertNull(MDC.get("requestId"));
        assertEquals(1, appender.list.size());
        String log = appender.list.getFirst().getFormattedMessage();
        assertTrue(log.contains("http_request_failed"));
        assertTrue(log.contains("method=GET"));
        assertTrue(log.contains("durationMs="));
        assertFalse(log.contains("sensitive-exception-detail"));
        assertNull(appender.list.getFirst().getThrowableProxy());
    }

    @Test
    void unrecognizedMethodIsNormalizedInLog() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("CUSTOM SECRET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ListAppender<ILoggingEvent> appender = captureLogs();

        try {
            filter.doFilter(request, response, (servletRequest, servletResponse) -> { });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertEquals(1, appender.list.size());
        String log = appender.list.getFirst().getFormattedMessage();
        assertTrue(log.contains("method=OTHER"));
        assertFalse(log.contains("CUSTOM SECRET"));
    }

    private ListAppender<ILoggingEvent> captureLogs() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }
}
