package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubject;
import br.senai.plataformadigital.auth.api.authorization.AuthorizationPolicyContribution;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.datasource.url")
public class InstitutionalDirectoryAuthorizationPolicy implements AuthorizationPolicyContribution {
    private final JdbcTemplate jdbc;

    public InstitutionalDirectoryAuthorizationPolicy(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean supports(String action) {
        return "LIST_INSTITUTIONAL_DIRECTORY".equals(action);
    }

    @Override
    public boolean permits(AuthenticatedSubject subject, br.senai.plataformadigital.auth.api.authorization.AuthorizationRequest request) {
        UUID user = UUID.fromString(subject.id());
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                    WHERE ur.user_id = ? AND ur.scope_type = 'GLOBAL'
                      AND r.name IN ('DOCENTE', 'COORDENACAO')
                )
                """, Boolean.class, user));
    }
}
