package br.senai.plataformadigital.academicpi;

import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class InstitutionalAuditRepository {
    private final JdbcTemplate jdbc;
    public InstitutionalAuditRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void record(UUID actor, String action, String targetType, UUID targetId, String details) {
        jdbc.update("INSERT INTO institutional.audit_log (actor_id, action, target_type, target_id, details) VALUES (?, ?, ?, ?, ?::jsonb)", actor, action, targetType, targetId, details == null ? "{}" : details);
    }
    public java.util.List<AuditResponse> findAll() { return jdbc.query("SELECT a.id, a.action, a.target_type, a.target_id, a.details::text, a.created_at, u.full_name AS actor_name FROM institutional.audit_log a JOIN auth.\"user\" u ON u.id = a.actor_id ORDER BY a.created_at DESC LIMIT 500", (rs, row) -> new AuditResponse(rs.getObject("id", UUID.class), rs.getString("action"), rs.getString("target_type"), rs.getObject("target_id", UUID.class), rs.getString("details"), rs.getTimestamp("created_at").toInstant(), rs.getString("actor_name"))); }
}
