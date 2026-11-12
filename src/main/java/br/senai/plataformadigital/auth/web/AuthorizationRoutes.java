package br.senai.plataformadigital.auth.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Catálogo técnico de rotas; não substitui a matriz institucional de papéis/permissões. */
public final class AuthorizationRoutes {
    public record Rule(String method, String path, boolean publicAccess, String action,
                       String resource, String resourceVariable, String scopeVariable) { }
    private final Map<String, Rule> rules = new HashMap<>();

    public AuthorizationRoutes(List<Rule> definitions) {
        for (Rule rule : definitions) {
            for (String optional : new String[]{rule.action(), rule.resource(), rule.resourceVariable(), rule.scopeVariable()}) {
                if (optional != null && optional.isBlank()) invalid();
            }
            if (blank(rule.method()) || blank(rule.path()) || !rule.path().startsWith("/")) invalid();
            if (!Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE").contains(rule.method())) invalid();
            if (rule.publicAccess()) {
                if (!blank(rule.action()) || !blank(rule.resource()) || !blank(rule.resourceVariable()) || !blank(rule.scopeVariable())) invalid();
            } else {
                if (blank(rule.action()) || (blank(rule.resource()) == blank(rule.resourceVariable()))) invalid();
                for (String variable : new String[]{rule.resourceVariable(), rule.scopeVariable()}) {
                    if (!blank(variable) && !rule.path().contains("{" + variable + "}")) invalid();
                }
            }
            if (rules.putIfAbsent(key(rule.method(), rule.path()), rule) != null) invalid();
        }
    }

    public static AuthorizationRoutes load(Resource[] resources, ObjectMapper mapper) throws IOException {
        var definitions = new java.util.ArrayList<Rule>();
        Set<String> allowed = Set.of("method", "path", "publicAccess", "action", "resource", "resourceVariable", "scopeVariable");
        for (Resource resource : resources) {
            try (var input = resource.getInputStream()) {
                JsonNode root = mapper.readTree(input);
                if (!root.isArray()) invalid();
                for (JsonNode node : root) {
                    if (!node.path("publicAccess").isBoolean()) invalid();
                    node.fieldNames().forEachRemaining(name -> { if (!allowed.contains(name)) invalid(); });
                    definitions.add(mapper.treeToValue(node, Rule.class));
                }
            }
        }
        return new AuthorizationRoutes(definitions);
    }

    public Rule find(String method, String path) {
        Rule rule = rules.get(key(method, path));
        return rule == null && method.equals("HEAD") ? rules.get(key("GET", path)) : rule;
    }

    public void verify(RequestMappingHandlerMapping mappings) {
        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            if (frameworkController(handler.getBeanType())) return;
            if (mapping.getMethodsCondition().getMethods().isEmpty()) invalid();
            mapping.getPatternValues().forEach(path -> mapping.getMethodsCondition().getMethods().forEach(method -> {
                if (find(method.name(), path) == null) {
                    throw new IllegalStateException("Rota sem classificação de autorização: " + method + " " + path);
                }
            }));
        });
    }

    public List<Rule> definitions() { return List.copyOf(rules.values()); }
    private static boolean frameworkController(Class<?> type) {
        return Set.of(
                "org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController",
                "org.springdoc.webmvc.api.OpenApiWebMvcResource",
                "org.springdoc.webmvc.api.MultipleOpenApiWebMvcResource",
                "org.springdoc.webmvc.ui.SwaggerWelcomeWebMvc",
                "org.springdoc.webmvc.ui.SwaggerConfigResource").contains(type.getName());
    }
    private static String key(String method, String path) { return method + " " + path; }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void invalid() { throw new IllegalStateException("Catálogo de autorização inválido ou ambíguo"); }
}
