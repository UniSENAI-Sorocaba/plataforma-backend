package br.senai.plataformadigital.portal.impact;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import plataforma.exception.ApiExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ImpactController.class)
public class ImpactExceptionHandler {
    @ExceptionHandler(ImpactFailure.class)
    ResponseEntity<ProblemDetail> failure(ImpactFailure failure, HttpServletRequest request) {
        return problem(failure.status(), failure.getMessage(), request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ProblemDetail> missingHeader(MissingRequestHeaderException failure, HttpServletRequest request) {
        return problem(428, "If-Match é obrigatório para alterar o impacto", request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> invalid(Exception failure, HttpServletRequest request) {
        return problem(400, "Requisição inválida; confira os campos e formatos", request);
    }

    private ResponseEntity<ProblemDetail> problem(int status, String detail, HttpServletRequest request) {
        String code = switch (status) {
            case 400 -> "VALIDATION_ERROR";
            case 401 -> "UNAUTHENTICATED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 412 -> "PRECONDITION_FAILED";
            case 428 -> "PRECONDITION_REQUIRED";
            default -> "IMPACT_UNAVAILABLE";
        };
        ProblemDetail body = ApiExceptionHandler.problem(HttpStatusCode.valueOf(status), code, detail, request);
        var builder = ResponseEntity.status(status);
        if (status == 401) builder.header("WWW-Authenticate", "Bearer");
        return builder.body(body);
    }
}
