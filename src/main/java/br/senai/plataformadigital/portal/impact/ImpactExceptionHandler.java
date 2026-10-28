package br.senai.plataformadigital.portal.impact;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
        ProblemDetail body = ProblemDetail.forStatusAndDetail(org.springframework.http.HttpStatusCode.valueOf(status), detail);
        body.setProperty("correlationId", request.getAttribute("impact.correlation"));
        var builder = ResponseEntity.status(status);
        if (status == 401) builder.header("WWW-Authenticate", "Bearer");
        return builder.body(body);
    }
}
