package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubject;
import br.senai.plataformadigital.auth.api.authorization.AuthorizationPolicyContribution;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.datasource.url")
public class ProjectAuthorizationPolicy implements AuthorizationPolicyContribution {
    private final JdbcTemplate jdbc;

    public ProjectAuthorizationPolicy(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean supports(String action) {
        return switch (action) {
            case "CREATE_PROJECT", "READ_PROJECT", "REQUEST_PARTICIPATION",
                    "MANAGE_PROJECT", "DECIDE_PARTICIPATION", "REMOVE_PARTICIPANT",
                    "CREATE_ENROLLMENT_PERIOD", "CHANGE_PROJECT_LIFECYCLE", "WITHDRAW_PARTICIPATION",
                    "CREATE_DELIVERABLE" -> true;
            case "LIST_PROJECT_PERIODS", "LIST_PROJECT_PARTICIPATION", "LIST_PROJECT_PARTICIPANTS",
                    "LIST_OWN_PARTICIPATION", "LIST_MANAGED_PROJECTS" -> true;
            default -> false;
        };
    }

    @Override
    public boolean permits(AuthenticatedSubject subject, br.senai.plataformadigital.auth.api.authorization.AuthorizationRequest request) {
        UUID user = UUID.fromString(subject.id());
        return switch (request.action()) {
            case "CREATE_PROJECT" -> hasRole(user, "DOCENTE") || hasRole(user, "COORDENACAO");
            case "READ_PROJECT" -> activeUser(user);
            case "REQUEST_PARTICIPATION" -> activeStudent(user);
            case "MANAGE_PROJECT", "REMOVE_PARTICIPANT" -> responsible(request.scope().orElseThrow(), user);
            case "DECIDE_PARTICIPATION" -> requestResponsible(request.scope().orElseThrow(), user);
            case "CREATE_ENROLLMENT_PERIOD" -> hasRole(user, "COORDENACAO");
            case "CHANGE_PROJECT_LIFECYCLE" -> responsible(request.scope().orElseThrow(), user);
            case "WITHDRAW_PARTICIPATION" -> activeStudent(user) && requestStudent(request.scope().orElseThrow(), user);
            case "CREATE_DELIVERABLE" -> activeStudent(user) && activeParticipant(request.scope().orElseThrow(), user);
            case "LIST_PROJECT_PERIODS", "LIST_PROJECT_PARTICIPATION", "LIST_PROJECT_PARTICIPANTS" ->
                    responsible(request.scope().orElseThrow(), user);
            case "LIST_OWN_PARTICIPATION" -> activeStudent(user);
            case "LIST_MANAGED_PROJECTS" -> hasManagedProject(user);
            default -> false;
        };
    }

    private boolean hasRole(UUID user, String role) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                    WHERE ur.user_id = ? AND r.name = ?)
                """, Boolean.class, user, role));
    }

    private boolean activeUser(UUID user) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM auth.\"user\" WHERE id = ? AND status = 'active')",
                Boolean.class, user));
    }

    private boolean activeStudent(UUID user) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM institutional.academic_link
                    WHERE user_id = ? AND role = 'student' AND status = 'active')
                """, Boolean.class, user));
    }

    private boolean responsible(String projectId, UUID user) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM academic_pi.project_responsible
                    WHERE project_id = ?::uuid AND user_id = ?)
                """, Boolean.class, projectId, user));
    }

    private boolean requestResponsible(String requestId, UUID user) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM academic_pi.participation_request pr
                    JOIN academic_pi.project_responsible r ON r.project_id = pr.project_id
                    WHERE pr.id = ?::uuid AND r.user_id = ?
                )
                """, Boolean.class, requestId, user));
    }

    private boolean requestStudent(String requestId, UUID user) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM academic_pi.participation_request
                    WHERE id = ?::uuid AND student_id = ?)
                """, Boolean.class, requestId, user));
    }

    private boolean hasManagedProject(UUID user) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM academic_pi.project_responsible WHERE user_id = ?)
                """, Boolean.class, user));
    }

    private boolean activeParticipant(String projectId, UUID user) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM academic_pi.project_participant pp
                    JOIN academic_pi.enrollment_period ep ON ep.id = pp.period_id AND ep.project_id = pp.project_id
                    WHERE pp.project_id = ?::uuid AND pp.student_id = ? AND pp.status = 'ACTIVE')
                """, Boolean.class, projectId, user));
    }
}
