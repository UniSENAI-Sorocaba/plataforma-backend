package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubject;
import br.senai.plataformadigital.auth.api.authorization.AuthorizationPolicyContribution;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.datasource.url")
public class AcademicClassAuthorizationPolicy implements AuthorizationPolicyContribution {
    private final JdbcTemplate jdbc;
    public AcademicClassAuthorizationPolicy(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public boolean supports(String action) { return "MANAGE_INSTITUTIONAL_CLASSES".equals(action); }
    @Override public boolean permits(AuthenticatedSubject subject, br.senai.plataformadigital.auth.api.authorization.AuthorizationRequest request) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ? AND r.name = 'COORDENACAO' AND ur.scope_type = 'GLOBAL')", Boolean.class, UUID.fromString(subject.id())));
    }
}
