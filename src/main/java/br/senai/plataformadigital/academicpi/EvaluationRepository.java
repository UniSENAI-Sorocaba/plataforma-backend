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

    public boolean isAssignedEvaluator(UUID evaluationId, UUID evaluatorId) {
        return exists("SELECT 1 FROM academic_pi.evaluation_evaluator WHERE evaluation_id = ? AND evaluator_id = ?", evaluationId, evaluatorId);
    }

    public boolean belongsTo(UUID evaluationId, UUID projectId, UUID periodId) {
        return exists("SELECT 1 FROM academic_pi.evaluation WHERE id = ? AND project_id = ? AND period_id = ?", evaluationId, projectId, periodId);
    }

    public RubricVersionResponse findRubric(UUID versionId) {
        var row = jdbc.query("SELECT rv.id, r.id rubric_id, r.code, r.name, rv.version FROM academic_pi.rubric_version rv JOIN academic_pi.rubric r ON r.id = rv.rubric_id WHERE rv.id = ?", (rs, i) -> new Object[]{rs.getObject("id", UUID.class), rs.getObject("rubric_id", UUID.class), rs.getString("code"), rs.getString("name"), rs.getInt("version")}, versionId).stream().findFirst().orElseThrow(() -> new br.senai.plataformadigital.exception.ResourceNotFoundException("Rubrica não encontrada."));
        var criteria = jdbc.query("SELECT id, code, label, weight, min_score, max_score FROM academic_pi.rubric_criterion WHERE rubric_version_id = ? ORDER BY code", (rs, i) -> new RubricCriterionResponse(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("label"), rs.getBigDecimal("weight"), rs.getBigDecimal("min_score"), rs.getBigDecimal("max_score")), versionId);
        return new RubricVersionResponse((UUID) row[0], (UUID) row[1], (String) row[2], (String) row[3], (Integer) row[4], criteria);
    }

    public void saveGrade(UUID evaluationId, UUID rubricVersionId, List<EvaluationCriterionScoreRequest> scores,
                           List<RubricCriterionResponse> criteria, java.math.BigDecimal finalScore) {
        jdbc.update("UPDATE academic_pi.evaluation SET rubric_version_id = ?, final_score = ?, updated_at = now() WHERE id = ?", rubricVersionId, finalScore, evaluationId);
        var ids = criteria.stream().collect(java.util.stream.Collectors.toMap(RubricCriterionResponse::code, RubricCriterionResponse::id));
        scores.forEach(score -> jdbc.update("INSERT INTO academic_pi.evaluation_criterion_score (evaluation_id, criterion_id, score) VALUES (?, ?, ?) ON CONFLICT (evaluation_id, criterion_id) DO UPDATE SET score = EXCLUDED.score, updated_at = now()", evaluationId, ids.get(score.criterionCode().trim()), score.score()));
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
