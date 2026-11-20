package br.senai.plataformadigital.extension;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubject;
import br.senai.plataformadigital.auth.api.authorization.AuthorizationPolicyContribution;
import br.senai.plataformadigital.auth.api.authorization.AuthorizationRequest;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.datasource.url")
public class ExtensionAuthorizationPolicy implements AuthorizationPolicyContribution {
    private final JdbcTemplate jdbc;

    public ExtensionAuthorizationPolicy(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean supports(String action) {
        return switch (action) {
            case "CREATE_EXTENSION_OPPORTUNITY", "PUBLISH_EXTENSION_OPPORTUNITY", "CLOSE_EXTENSION_OPPORTUNITY",
                    "LIST_EXTENSION_APPLICATIONS" -> true;
            case "APPLY_EXTENSION_OPPORTUNITY", "LIST_OWN_EXTENSION_APPLICATIONS", "WITHDRAW_EXTENSION_APPLICATION" -> true;
            default -> false;
        };
    }

    @Override
    public boolean permits(AuthenticatedSubject subject, AuthorizationRequest request) {
        UUID user = UUID.fromString(subject.id());
        return switch (request.action()) {
            case "CREATE_EXTENSION_OPPORTUNITY", "PUBLISH_EXTENSION_OPPORTUNITY", "CLOSE_EXTENSION_OPPORTUNITY" -> hasRole(user, "COORDENACAO");
            case "APPLY_EXTENSION_OPPORTUNITY", "LIST_OWN_EXTENSION_APPLICATIONS" -> activeStudent(user);
            case "WITHDRAW_EXTENSION_APPLICATION" -> activeStudent(user) && ownsApplication(request.scope().orElseThrow(), user);
            case "LIST_EXTENSION_APPLICATIONS" -> coordinator(user) || responsible(request.scope().orElseThrow(), user);
            default -> false;
        };
    }

    private boolean hasRole(UUID user, String role) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                    WHERE ur.user_id = ? AND r.name = ?)
                OR EXISTS (SELECT 1 FROM institutional.academic_link
                    WHERE user_id = ? AND role = 'coordinator' AND status = 'active')
                """, Boolean.class, user, role, user));
    }

    private boolean coordinator(UUID user) { return hasRole(user, "COORDENACAO"); }

    private boolean activeStudent(UUID user) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM institutional.academic_link
                    WHERE user_id = ? AND role = 'student' AND status = 'active')
                """, Boolean.class, user));
    }

    private boolean ownsApplication(String applicationId, UUID user) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM extension.application WHERE id = ?::uuid AND student_id = ?)
                """, Boolean.class, applicationId, user));
    }

    private boolean responsible(String opportunityId, UUID user) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM extension.opportunity o
                    JOIN academic_pi.project_responsible r ON r.project_id = o.project_id
                    WHERE o.id = ?::uuid AND r.user_id = ?)
                """, Boolean.class, opportunityId, user));
    }
}
