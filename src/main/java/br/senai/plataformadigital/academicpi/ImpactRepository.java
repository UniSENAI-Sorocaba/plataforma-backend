package br.senai.plataformadigital.academicpi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class ImpactRepository {
    private final JdbcTemplate jdbc;
    public ImpactRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public boolean isResponsible(UUID project, UUID actor) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM academic_pi.project_responsible WHERE project_id=? AND teacher_id=? AND active=true)", Boolean.class, project, actor)); }
    public boolean canManage(UUID impact, UUID actor) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM academic_pi.impact i JOIN academic_pi.project_responsible r ON r.project_id=i.project_id WHERE i.id=? AND r.teacher_id=? AND r.active=true)", Boolean.class, impact, actor)); }
    public boolean isCoordinator(UUID actor) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM auth.user_role ur JOIN auth.role r ON r.id=ur.role_id WHERE ur.user_id=? AND r.code='COORDENACAO')", Boolean.class, actor)); }
    public ImpactResponse insert(UUID project, ImpactRequest request, UUID actor) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO academic_pi.impact (id,project_id,title,created_by) VALUES (?,?,?,?)", id, project, request.title().trim(), actor);
        jdbc.update("INSERT INTO academic_pi.impact_version (id,impact_id,version,summary,evidence_url,created_by) VALUES (?,?,?,?,?,?)", UUID.randomUUID(), id, 1, request.summary().trim(), request.evidenceUrl(), actor);
        return find(id);
    }
    public ImpactResponse find(UUID id) { return jdbc.queryForObject("SELECT i.id,i.project_id,i.title,i.status,v.version,v.summary,v.evidence_url,i.updated_at FROM academic_pi.impact i JOIN academic_pi.impact_version v ON v.impact_id=i.id AND v.version=(SELECT max(v2.version) FROM academic_pi.impact_version v2 WHERE v2.impact_id=i.id) WHERE i.id=?", (rs,n)->map(rs), id); }
    public int updateStatus(UUID id, ImpactStatus status, UUID reviewer) { return jdbc.update("UPDATE academic_pi.impact SET status=?, reviewed_by=CASE WHEN ?='APPROVED' THEN ? ELSE NULL END, reviewed_at=CASE WHEN ?='APPROVED' THEN now() ELSE NULL END, updated_at=now() WHERE id=? AND status=?", status.name(), status.name(), reviewer, status.name(), id, status == ImpactStatus.SUBMITTED ? "SUBMITTED" : "DRAFT"); }
    public List<ImpactResponse> findPublic(UUID project) { String filter = project == null ? "" : " AND i.project_id=?"; Object[] args = project == null ? new Object[]{} : new Object[]{project}; return jdbc.query("SELECT i.id,i.project_id,i.title,i.status,v.version,v.summary,v.evidence_url,i.updated_at FROM academic_pi.impact i JOIN academic_pi.impact_version v ON v.impact_id=i.id AND v.version=(SELECT max(v2.version) FROM academic_pi.impact_version v2 WHERE v2.impact_id=i.id) WHERE i.status='APPROVED'"+filter+" ORDER BY i.updated_at DESC,i.id", (rs,n)->map(rs), args); }
    private static ImpactResponse map(ResultSet rs) throws SQLException { return new ImpactResponse(rs.getObject(1,UUID.class), rs.getObject(2,UUID.class), rs.getString(3), ImpactStatus.valueOf(rs.getString(4)), rs.getInt(5), rs.getString(6), rs.getString(7), rs.getTimestamp(8).toInstant()); }
}
