package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.exception.ResourceNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class RubricRepository {
    private final JdbcTemplate jdbc;
    public RubricRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean isCoordinator(UUID userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ? AND r.name = 'COORDENACAO')", Boolean.class, userId));
    }

    public RubricVersionResponse create(RubricCreateRequest request, UUID actor) {
        UUID rubricId = UUID.randomUUID(), versionId = UUID.randomUUID();
        jdbc.update("INSERT INTO academic_pi.rubric (id, code, name, created_by) VALUES (?, ?, ?, ?)", rubricId, request.code().trim(), request.name().trim(), actor);
        jdbc.update("INSERT INTO academic_pi.rubric_version (id, rubric_id, version, created_by) VALUES (?, ?, 1, ?)", versionId, rubricId, actor);
        request.criteria().forEach(c -> jdbc.update("INSERT INTO academic_pi.rubric_criterion (id, rubric_version_id, code, label, weight, min_score, max_score) VALUES (?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(), versionId, c.code().trim(), c.label().trim(), c.weight(), c.minScore(), c.maxScore()));
        return find(rubricId, 1);
    }

    public RubricVersionResponse find(UUID rubricId, int version) {
        var header = jdbc.query("SELECT rv.id, r.id rubric_id, r.code, r.name, rv.version FROM academic_pi.rubric_version rv JOIN academic_pi.rubric r ON r.id = rv.rubric_id WHERE r.id = ? AND rv.version = ?", (rs, row) -> new Object[]{rs.getObject("id", UUID.class), rs.getObject("rubric_id", UUID.class), rs.getString("code"), rs.getString("name"), rs.getInt("version")}, rubricId, version).stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Rubrica não encontrada."));
        @SuppressWarnings("unchecked") List<RubricCriterionResponse> criteria = jdbc.query("SELECT id, code, label, weight, min_score, max_score FROM academic_pi.rubric_criterion WHERE rubric_version_id = ? ORDER BY code", (rs, row) -> mapCriterion(rs), (UUID) header[0]);
        return new RubricVersionResponse((UUID) header[0], (UUID) header[1], (String) header[2], (String) header[3], (Integer) header[4], criteria);
    }

    private static RubricCriterionResponse mapCriterion(ResultSet rs) throws SQLException {
        return new RubricCriterionResponse(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("label"), rs.getBigDecimal("weight"), rs.getBigDecimal("min_score"), rs.getBigDecimal("max_score"));
    }
}
