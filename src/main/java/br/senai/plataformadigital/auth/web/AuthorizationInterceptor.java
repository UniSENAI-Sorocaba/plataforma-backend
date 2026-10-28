package br.senai.plataformadigital.auth.web;

import br.senai.plataformadigital.auth.api.authorization.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

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
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method) || !AuthorizationRoutes.applicationController(method.getBeanType())) return true;
        String path = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        AuthorizationRoutes.Rule rule = routes.find(request.getMethod(), path);
        if (rule != null && rule.publicAccess()) return true;
        Object established = request.getAttribute("request.correlationId");
        String correlation = established instanceof String value ? value : request.getHeader("X-Correlation-Id");
        if (correlation == null || !correlation.matches("[A-Za-z0-9._-]{1,64}")) correlation = UUID.randomUUID().toString();
        request.setAttribute("request.correlationId", correlation);
        response.setHeader("X-Correlation-Id", correlation);
        response.setHeader("Cache-Control", "no-store");
        AuthorizationDecision decision;
        String actor = null;
        if (rule == null) {
            decision = AuthorizationDecision.DENIED;
        } else {
            Map<String, String> variables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (variables == null) variables = Map.of();
            String resource = rule.resourceVariable() == null ? rule.resource() : variables.get(rule.resourceVariable());
            String scope = rule.scopeVariable() == null ? null : variables.get(rule.scopeVariable());
            try {
                var evaluation = service.evaluate(new AuthorizationRequest(rule.action(), resource, Optional.ofNullable(scope)));
                decision = evaluation.decision();
                actor = evaluation.subject().map(AuthenticatedSubject::id).orElse(null);
            }
            catch (RuntimeException invalidContext) { decision = AuthorizationDecision.UNAVAILABLE; }
        }
        try {
            audit.record(new AuthorizationAudit.Event(request.getMethod(), path, rule == null ? null : rule.action(), actor, decision, correlation));
        } catch (RuntimeException unavailable) { decision = AuthorizationDecision.UNAVAILABLE; }
        metrics.counter("authorization.decisions", "outcome", decision.name()).increment();
        if (decision.allowsAccess()) return true;
        int status = decision == AuthorizationDecision.UNAUTHENTICATED ? 401 : decision == AuthorizationDecision.UNAVAILABLE ? 503 : 403;
        response.setStatus(status);
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        response.setContentType("application/problem+json");
        json.writeValue(response.getOutputStream(), Map.of("type", "about:blank", "status", status,
                "title", org.springframework.http.HttpStatus.valueOf(status).getReasonPhrase(),
                "detail", status == 401 ? "Autenticação necessária" : status == 403 ? "Acesso negado" : "Autorização indisponível",
                "correlationId", correlation));
        return false;
    }
}
