package br.senai.plataformadigital.extension;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class ActivityRepository {
    private final JdbcTemplate jdbc;

    public ActivityRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean isActiveParticipant(UUID projectId, UUID periodId, UUID studentId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM academic_pi.project_participant
                    WHERE project_id = ? AND period_id = ? AND student_id = ? AND status = 'ACTIVE')
                """, Boolean.class, projectId, periodId, studentId));
    }

    public boolean isResponsible(UUID projectId, UUID teacherId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM academic_pi.project_responsible
                    WHERE project_id = ? AND user_id = ?)
                """, Boolean.class, projectId, teacherId));
    }

    public boolean isCoordinator(UUID userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ? AND r.name = 'COORDENACAO')
                OR EXISTS (SELECT 1 FROM institutional.academic_link WHERE user_id = ? AND role = 'coordinator' AND status = 'active')
                """, Boolean.class, userId, userId));
    }

    public Instant periodEndsAt(UUID projectId, UUID periodId) {
        return jdbc.queryForObject("""
                SELECT ends_at FROM academic_pi.enrollment_period WHERE project_id = ? AND id = ?
                """, (rs, row) -> rs.getTimestamp("ends_at").toInstant(), projectId, periodId);
    }

    public UUID insert(UUID projectId, UUID periodId, UUID studentId, ActivityRequest request) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO extension.activity
                    (id, project_id, period_id, student_id, title, description, requested_hours)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, projectId, periodId, studentId, request.title().trim(), request.description().trim(),
                request.requestedHours());
        return id;
    }

    public ActivityResponse find(UUID id) {
        return jdbc.queryForObject("""
                SELECT a.id, a.project_id, a.period_id, a.student_id, u.full_name AS student_name, u.email AS student_email,
                       a.title, a.description, a.requested_hours, a.approved_hours, a.status, a.rejection_reason,
                       a.submitted_at, a.reviewed_at, a.reviewed_by, a.updated_at
                FROM extension.activity a JOIN auth."user" u ON u.id = a.student_id WHERE a.id = ?
                """, ActivityRepository::map, id);
    }

    public List<ActivityResponse> findMine(UUID projectId, UUID periodId, UUID studentId) {
        return findList("WHERE project_id = ? AND period_id = ? AND student_id = ?", projectId, periodId, studentId);
    }

    public List<ActivityResponse> findProject(UUID projectId, UUID periodId) {
        return findList("WHERE project_id = ? AND period_id = ?", projectId, periodId);
    }

    private List<ActivityResponse> findList(String where, Object... args) {
        return jdbc.query("""
                SELECT a.id, a.project_id, a.period_id, a.student_id, u.full_name AS student_name, u.email AS student_email,
                       a.title, a.description, a.requested_hours, a.approved_hours, a.status, a.rejection_reason,
                       a.submitted_at, a.reviewed_at, a.reviewed_by, a.updated_at
                FROM extension.activity a JOIN auth."user" u ON u.id = a.student_id""" + " " + where.replace("WHERE ", "WHERE a.") + " ORDER BY a.submitted_at, a.id", ActivityRepository::map, args);
    }

    public int updateSubmission(UUID id, UUID studentId, ActivityRequest request) {
        return jdbc.update("""
                UPDATE extension.activity
                SET title = ?, description = ?, requested_hours = ?, status = 'SUBMITTED',
                    approved_hours = NULL, rejection_reason = NULL, reviewed_at = NULL,
                    reviewed_by = NULL, submitted_at = now(), updated_at = now()
                WHERE id = ? AND student_id = ? AND status IN ('SUBMITTED', 'REJECTED')
                """, request.title().trim(), request.description().trim(), request.requestedHours(), id, studentId);
    }

    public int decide(UUID id, ActivityDecisionRequest request, UUID reviewer) {
        return jdbc.update("""
                UPDATE extension.activity
                SET status = ?, approved_hours = ?, rejection_reason = ?, reviewed_at = now(),
                    reviewed_by = ?, updated_at = now()
                WHERE id = ?
                """, request.status().name(), request.approvedHours(),
                request.rejectionReason() == null ? null : request.rejectionReason().trim(), reviewer, id);
    }

    public HoursTotalResponse total(UUID projectId, UUID periodId, UUID studentId) {
        BigDecimal total = jdbc.queryForObject("""
                SELECT COALESCE(SUM(approved_hours), 0) FROM extension.activity
                WHERE project_id = ? AND period_id = ? AND student_id = ? AND status = 'APPROVED'
                """, BigDecimal.class, projectId, periodId, studentId);
        return new HoursTotalResponse(projectId, periodId, studentId, total);
    }

    private static ActivityResponse map(ResultSet rs, int row) throws SQLException {
        return new ActivityResponse(rs.getObject("id", UUID.class), rs.getObject("project_id", UUID.class),
                rs.getObject("period_id", UUID.class), rs.getObject("student_id", UUID.class),
                rs.getString("student_name"), rs.getString("student_email"),
                rs.getString("title"), rs.getString("description"), rs.getBigDecimal("requested_hours"),
                rs.getBigDecimal("approved_hours"), ActivityStatus.valueOf(rs.getString("status")),
                rs.getString("rejection_reason"), rs.getTimestamp("submitted_at").toInstant(),
                instant(rs, "reviewed_at"), rs.getObject("reviewed_by", UUID.class),
                rs.getTimestamp("updated_at").toInstant());
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
