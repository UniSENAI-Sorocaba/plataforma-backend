package br.senai.plataformadigital.portal.impact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingRequestHeaderException;

class ImpactExceptionHandlerTests {
    private final ImpactExceptionHandler handler = new ImpactExceptionHandler();

    @Test
    void mapsDomainStatusesToStableProblemCodesAndChallengeHeader() {
        var request = request();
        assertCode(400, "VALIDATION_ERROR", null, request);
        assertCode(401, "UNAUTHENTICATED", "Bearer", request);
        assertCode(403, "FORBIDDEN", null, request);
        assertCode(404, "NOT_FOUND", null, request);
        assertCode(409, "CONFLICT", null, request);
        assertCode(412, "PRECONDITION_FAILED", null, request);
        assertCode(428, "PRECONDITION_REQUIRED", null, request);
        assertCode(503, "IMPACT_UNAVAILABLE", null, request);
    }

    @Test
    void missingRevisionHeaderAndMalformedInputReturnSafeClientErrors() throws Exception {
        var request = request();
        var missing = handler.missingHeader(
                new MissingRequestHeaderException("If-Match", null), request);
        assertEquals(428, missing.getStatusCode().value());
        assertEquals("PRECONDITION_REQUIRED", code(missing.getBody()));

        var invalid = handler.invalid(new IllegalArgumentException("secret payload"), request);
        assertEquals(400, invalid.getStatusCode().value());
        assertEquals("VALIDATION_ERROR", code(invalid.getBody()));
        assertEquals("Requisição inválida; confira os campos e formatos", invalid.getBody().getDetail());
    }

    private void assertCode(int status, String expected, String challenge, HttpServletRequest request) {
        var response = handler.failure(new ImpactFailure(status, "safe detail"), request);
        assertEquals(status, response.getStatusCode().value());
        assertEquals(expected, code(response.getBody()));
        assertNotNull(response.getBody().getProperties().get("correlation_id"));
        assertEquals(challenge, response.getHeaders().getFirst("WWW-Authenticate"));
    }

    private String code(ProblemDetail problem) {
        return (String) problem.getProperties().get("code");
    }

    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/impacts/1");
        return request;
    }
}
