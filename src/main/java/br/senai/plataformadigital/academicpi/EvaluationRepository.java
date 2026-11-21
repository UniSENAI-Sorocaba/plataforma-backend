package br.senai.plataformadigital.academicpi;

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
public class EvaluationRepository {
    private final JdbcTemplate jdbc;

    public EvaluationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean isCoordinator(UUID userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                    WHERE ur.user_id = ? AND r.name = 'COORDENACAO' AND ur.scope_type = 'GLOBAL'
                ) OR EXISTS (
                    SELECT 1 FROM institutional.academic_link
                    WHERE user_id = ? AND role = 'coordinator' AND status = 'active'
                )
                """, Boolean.class, userId, userId));
    }

    public boolean isResponsible(UUID projectId, UUID userId) {
        return exists("SELECT 1 FROM academic_pi.project_responsible WHERE project_id = ? AND user_id = ?", projectId, userId);
    }

    public boolean isActiveTarget(UUID projectId, UUID periodId, UUID studentId) {
        return exists("""
                SELECT 1 FROM academic_pi.project_participant
                WHERE project_id = ? AND period_id = ? AND student_id = ? AND status = 'ACTIVE'
                """, projectId, periodId, studentId);
    }

    public boolean isEligibleEvaluator(UUID projectId, UUID periodId, UUID evaluatorId) {
        return exists("""
                SELECT 1 FROM auth."user" u
                WHERE u.id = ? AND u.status = 'active' AND (
                    EXISTS (SELECT 1 FROM academic_pi.project_responsible r
                            WHERE r.project_id = ? AND r.user_id = u.id)
                    OR EXISTS (SELECT 1 FROM academic_pi.project_participant p
                            WHERE p.project_id = ? AND p.period_id = ?
                              AND p.student_id = u.id AND p.status = 'ACTIVE')
                )
                """, evaluatorId, projectId, projectId, periodId);
    }

    public UUID insert(UUID projectId, UUID periodId, EvaluationRequest request, UUID actor) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO academic_pi.evaluation
                    (id, project_id, period_id, evaluated_student_id, evaluation_date, created_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, projectId, periodId, request.evaluatedStudentId(), Timestamp.from(request.evaluationDate()), actor);
        return id;
    }

    public void assign(UUID evaluationId, UUID evaluatorId, UUID actor) {
        jdbc.update("""
                INSERT INTO academic_pi.evaluation_evaluator (evaluation_id, evaluator_id, assigned_by)
                VALUES (?, ?, ?)
                """, evaluationId, evaluatorId, actor);
    }

    public List<EvaluationResponse> findAll(UUID projectId, UUID periodId) {
        return jdbc.query("""
                SELECT id, project_id, period_id, evaluated_student_id, evaluation_date,
                       created_by, created_at, updated_at
                FROM academic_pi.evaluation
                WHERE project_id = ? AND period_id = ?
                ORDER BY evaluation_date, id
                """, (rs, row) -> map(rs, findEvaluators(rs.getObject("id", UUID.class))), projectId, periodId);
    }

    private List<UUID> findEvaluators(UUID evaluationId) {
        return jdbc.query("""
                SELECT evaluator_id FROM academic_pi.evaluation_evaluator
                WHERE evaluation_id = ? ORDER BY evaluator_id
                """, (rs, row) -> rs.getObject("evaluator_id", UUID.class), evaluationId);
    }

    private static EvaluationResponse map(ResultSet rs, List<UUID> evaluators) throws SQLException {
        return new EvaluationResponse(rs.getObject("id", UUID.class), rs.getObject("project_id", UUID.class),
                rs.getObject("period_id", UUID.class), rs.getObject("evaluated_student_id", UUID.class),
                rs.getTimestamp("evaluation_date").toInstant(), rs.getObject("created_by", UUID.class),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(), evaluators);
    }

    private boolean exists(String sql, Object... args) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (" + sql + ")", Boolean.class, args));
    }
}
