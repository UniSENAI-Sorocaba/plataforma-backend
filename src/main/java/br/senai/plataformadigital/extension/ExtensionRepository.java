package br.senai.plataformadigital.extension;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class ExtensionRepository {
    private final JdbcTemplate jdbc;

    public ExtensionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean isActiveStudent(UUID userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM institutional.academic_link
                    WHERE user_id = ? AND role = 'student' AND status = 'active')
                """, Boolean.class, userId));
    }

    public boolean hasRole(UUID userId, String role) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                    WHERE ur.user_id = ? AND r.name = ?)
                OR EXISTS (SELECT 1 FROM institutional.academic_link
                    WHERE user_id = ? AND role = ? AND status = 'active')
                """, Boolean.class, userId, role,
                userId, role.equals("COORDENACAO") ? "coordinator" : "professor"));
    }

    public UUID insertOpportunity(OpportunityRequest request, UUID actor) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO extension.opportunity
                    (id, project_id, title, description, starts_at, ends_at, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, request.projectId(), request.title().trim(), request.description().trim(),
                Timestamp.from(request.startsAt()), Timestamp.from(request.endsAt()), actor);
        return id;
    }

    public int updateOpportunity(UUID id, OpportunityRequest request) {
        return jdbc.update("""
                UPDATE extension.opportunity
                SET project_id = ?, title = ?, description = ?, starts_at = ?, ends_at = ?, updated_at = now()
                WHERE id = ? AND status = 'DRAFT'
                """, request.projectId(), request.title().trim(), request.description().trim(),
                Timestamp.from(request.startsAt()), Timestamp.from(request.endsAt()), id);
    }

    public int deleteOpportunity(UUID id) {
        return jdbc.update("DELETE FROM extension.opportunity WHERE id = ? AND status = 'DRAFT'", id);
    }

    public OpportunityResponse findOpportunity(UUID id) {
        return jdbc.queryForObject("""
                SELECT id, project_id, title, description, status, starts_at, ends_at, created_at, updated_at
                FROM extension.opportunity WHERE id = ?
                """, (rs, row) -> mapOpportunity(rs), id);
    }

    public List<OpportunityResponse> findPublicOpportunities() {
        return jdbc.query("""
                SELECT id, project_id, title, description, status, starts_at, ends_at, created_at, updated_at
                FROM extension.opportunity WHERE status IN ('PUBLISHED', 'CLOSED')
                ORDER BY starts_at DESC, id
                """, (rs, row) -> mapOpportunity(rs));
    }

    public List<OpportunityResponse> findAllOpportunities() {
        return jdbc.query("""
                SELECT id, project_id, title, description, status, starts_at, ends_at, created_at, updated_at
                FROM extension.opportunity ORDER BY updated_at DESC, id
                """, (rs, row) -> mapOpportunity(rs));
    }

    public int updateStatus(UUID id, OpportunityStatus from, OpportunityStatus to, InstantHolder now) {
        return jdbc.update("""
                UPDATE extension.opportunity
                SET status = ?, published_at = CASE WHEN ? = 'PUBLISHED' THEN COALESCE(published_at, ?) ELSE published_at END,
                    closed_at = CASE WHEN ? = 'CLOSED' THEN ? ELSE closed_at END, updated_at = ?
                WHERE id = ? AND status = ?
                """, to.name(), to.name(), Timestamp.from(now.value()), to.name(), Timestamp.from(now.value()),
                Timestamp.from(now.value()), id, from.name());
    }

    public ApplicationResponse insertApplication(UUID opportunityId, UUID studentId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO extension.application (id, opportunity_id, student_id)
                VALUES (?, ?, ?)
                """, id, opportunityId, studentId);
        return findApplication(id);
    }

    public ApplicationResponse findApplication(UUID id) {
        return jdbc.queryForObject("""
                SELECT id, opportunity_id, student_id, status, submitted_at, withdrawn_at
                FROM extension.application WHERE id = ?
                """, ExtensionRepository::mapApplication, id);
    }

    public List<ApplicationResponse> findStudentApplications(UUID studentId) {
        return jdbc.query("""
                SELECT id, opportunity_id, student_id, status, submitted_at, withdrawn_at
                FROM extension.application WHERE student_id = ?
                ORDER BY submitted_at DESC, id
                """, ExtensionRepository::mapApplication, studentId);
    }

    public List<ApplicationResponse> findOpportunityApplications(UUID opportunityId) {
        return jdbc.query("""
                SELECT id, opportunity_id, student_id, status, submitted_at, withdrawn_at
                FROM extension.application WHERE opportunity_id = ?
                ORDER BY submitted_at, id
                """, ExtensionRepository::mapApplication, opportunityId);
    }

    public int withdrawApplication(UUID id, UUID studentId, InstantHolder now) {
        return jdbc.update("""
                UPDATE extension.application SET status = 'WITHDRAWN', withdrawn_at = ?
                WHERE id = ? AND student_id = ? AND status = 'SUBMITTED'
                """, Timestamp.from(now.value()), id, studentId);
    }

    public boolean projectResponsible(UUID opportunityId, UUID userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM extension.opportunity o
                    JOIN academic_pi.project_responsible r ON r.project_id = o.project_id
                    WHERE o.id = ? AND r.user_id = ?)
                """, Boolean.class, opportunityId, userId));
    }

    private static OpportunityResponse mapOpportunity(ResultSet rs) throws SQLException {
        return new OpportunityResponse(rs.getObject("id", UUID.class), rs.getObject("project_id", UUID.class),
                rs.getString("title"), rs.getString("description"), OpportunityStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("starts_at").toInstant(), rs.getTimestamp("ends_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private static ApplicationResponse mapApplication(ResultSet rs, int row) throws SQLException {
        return new ApplicationResponse(rs.getObject("id", UUID.class), rs.getObject("opportunity_id", UUID.class),
                rs.getObject("student_id", UUID.class), ApplicationStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("submitted_at").toInstant(),
                rs.getTimestamp("withdrawn_at") == null ? null : rs.getTimestamp("withdrawn_at").toInstant());
    }

    public record InstantHolder(java.time.Instant value) { }
}
