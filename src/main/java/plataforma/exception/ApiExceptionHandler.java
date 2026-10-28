package plataforma.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.LinkedHashMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import plataforma.config.CorrelationIdFilter;

/** RFC 9457 errors that preserve Spring's status for framework failures. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(ResourceNotFoundException ex, HttpServletRequest request) { return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", request); }
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> validation(ConstraintViolationException ex, HttpServletRequest request) { return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request); }
    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<ProblemDetail> invalidSort(PropertyReferenceException ex, HttpServletRequest request) { return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request is invalid", request); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integrity(DataIntegrityViolationException ex, HttpServletRequest request) { return response(HttpStatus.CONFLICT, "CONFLICT", "Operation conflicts with existing data", request); }
    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ProblemDetail> conflict(ConflictException ex, HttpServletRequest request) { return response(HttpStatus.CONFLICT, "CONFLICT", "Operation conflicts with current state", request); }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> authentication(AuthenticationException ex, HttpServletRequest request) { return response(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required", request); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> authorization(AccessDeniedException ex, HttpServletRequest request) { return response(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not allowed to perform this action", request); }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(status, "VALIDATION_ERROR", "Request validation failed", request);
        var fields = new LinkedHashMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        problem.setProperty("fields", fields);
        return new ResponseEntity<>(problem, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return new ResponseEntity<>(problem(status, code(status), message(status), request), headers, status);
    }

    private ResponseEntity<ProblemDetail> response(HttpStatus status, String code, String message, HttpServletRequest request) { return ResponseEntity.status(status).body(problem(status, code, message, request)); }
    private ProblemDetail problem(HttpStatusCode status, String code, String message, WebRequest request) { return problem(status, code, message, request instanceof ServletWebRequest servlet ? servlet.getRequest() : null); }
    private ProblemDetail problem(HttpStatusCode status, String code, String message, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setType(URI.create("https://errors.plataforma.senai.br/" + code.toLowerCase()));
        problem.setProperty("code", code);
        problem.setProperty("message", message);
        problem.setProperty("correlation_id", CorrelationIdFilter.correlationId(request));
        return problem;
    }
    private String code(HttpStatusCode status) { return switch (status.value()) { case 404 -> "NOT_FOUND"; case 405 -> "METHOD_NOT_ALLOWED"; case 415 -> "UNSUPPORTED_MEDIA_TYPE"; default -> "REQUEST_ERROR"; }; }
    private String message(HttpStatusCode status) { return switch (status.value()) { case 404 -> "Resource not found"; case 405 -> "HTTP method is not allowed for this resource"; case 415 -> "Content type is not supported"; default -> "Request could not be processed"; }; }
}
