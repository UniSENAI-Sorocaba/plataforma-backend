package br.senai.plataformadigital.auth.web;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubject;
import br.senai.plataformadigital.auth.api.authorization.AuthorizationAudit;
import br.senai.plataformadigital.auth.api.authorization.AuthorizationDecision;
import br.senai.plataformadigital.auth.api.authorization.AuthorizationRequest;
import br.senai.plataformadigital.auth.api.authorization.AuthorizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import plataforma.config.CorrelationIdFilter;
import plataforma.exception.ApiExceptionHandler;

public final class AuthorizationInterceptor implements HandlerInterceptor {
    private final AuthorizationRoutes routes;
    private final AuthorizationService service;
    private final AuthorizationAudit audit;
    private final MeterRegistry metrics;
    private final ObjectMapper json;

    public AuthorizationInterceptor(AuthorizationRoutes routes, AuthorizationService service,
                                    AuthorizationAudit audit, MeterRegistry metrics, ObjectMapper json) {
        this.routes = routes;
        this.service = service;
        this.audit = audit;
        this.metrics = metrics;
        this.json = json;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = null;
        if (handler instanceof HandlerMethod method) {
            if (method.getBeanType() == BasicErrorController.class && request.getDispatcherType() == DispatcherType.ERROR) {
                return true;
            }
            path = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        }
        AuthorizationRoutes.Rule rule = path == null ? null : routes.find(request.getMethod(), path);
        if (rule != null && rule.publicAccess()) return true;

        String correlation = CorrelationIdFilter.correlationId(request);
        response.setHeader("Cache-Control", "no-store");
        var evaluation = service.evaluateRoute(() -> rule == null ? null : context(request, rule));
        AuthorizationDecision decision = evaluation.decision();
        String actor = evaluation.subject().map(AuthenticatedSubject::id).orElse(null);
        try {
            audit.record(new AuthorizationAudit.Event(request.getMethod(), path, rule == null ? null : rule.action(),
                    actor, decision, correlation));
        } catch (RuntimeException unavailable) {
            metrics.counter("rbac.audit.unavailable").increment();
            if (decision.allowsAccess()) decision = AuthorizationDecision.UNAVAILABLE;
        }
        metrics.counter("rbac.decisions", "outcome", decision.name()).increment();
        if (decision.allowsAccess()) return true;

        HttpStatus status;
        String code;
        String message;
        switch (decision) {
            case UNAUTHENTICATED -> {
                status = HttpStatus.UNAUTHORIZED;
                code = "UNAUTHENTICATED";
                message = "Authentication is required";
                response.setHeader("WWW-Authenticate", "Bearer");
            }
            case DENIED -> {
                status = HttpStatus.FORBIDDEN;
                code = "FORBIDDEN";
                message = "You are not allowed to perform this action";
            }
            default -> {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                code = "AUTHORIZATION_UNAVAILABLE";
                message = "Autorização indisponível";
            }
        }
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        json.writeValue(response.getOutputStream(), ApiExceptionHandler.problem(status, code, message, request));
        return false;
    }

    @SuppressWarnings("unchecked")
    private AuthorizationRequest context(HttpServletRequest request, AuthorizationRoutes.Rule rule) {
        Map<String, String> variables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables == null) variables = Map.of();
        String resource = rule.resourceVariable() == null ? rule.resource() : variables.get(rule.resourceVariable());
        String scope = rule.scopeVariable() == null ? null : variables.get(rule.scopeVariable());
        return new AuthorizationRequest(rule.action(), resource, Optional.ofNullable(scope));
    }
}
