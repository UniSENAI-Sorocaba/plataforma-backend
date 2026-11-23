package br.senai.plataformadigital.academicpi;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class AcademicClassRepository {
    private final JdbcTemplate jdbc;
    public AcademicClassRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean isCoordinator(UUID actor) { return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND r.name = 'COORDENACAO' AND ur.scope_type = 'GLOBAL')
            """, Boolean.class, actor)); }

    public List<AcademicClassResponse> findAll() { return jdbc.query("SELECT id, code, name, term, is_active FROM institutional.academic_class ORDER BY term DESC, code",
            (rs, row) -> new AcademicClassResponse(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"), rs.getString("term"), rs.getBoolean("is_active"))); }

    public AcademicClassResponse create(AcademicClassRequest request) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO institutional.academic_class (id, code, name, term, is_active) VALUES (?, ?, ?, ?, true)", id, request.code().trim(), request.name().trim(), request.term().trim());
        return findAll().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow(() -> new IllegalStateException("Turma criada, mas não pôde ser consultada."));
    }

    public void updateStatus(UUID id, boolean active) {
        if (jdbc.update("UPDATE institutional.academic_class SET is_active = ?, updated_at = now() WHERE id = ?", active, id) != 1) throw new EmptyResultDataAccessException(1);
    }
}
