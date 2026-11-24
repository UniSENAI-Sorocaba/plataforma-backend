package br.senai.plataformadigital.extension;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class CertificateRepository {
    private final JdbcTemplate jdbc;
    public CertificateRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public boolean canIssue(UUID project, UUID period, UUID student) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM academic_pi.project_participant pp JOIN extension.activity a ON a.project_id=pp.project_id AND a.period_id=pp.period_id AND a.student_id=pp.student_id WHERE pp.project_id=? AND pp.period_id=? AND pp.student_id=? AND pp.status='ACTIVE' AND a.status='APPROVED')", Boolean.class, project, period, student)); }
    public CertificateResponse insert(UUID project, UUID period, UUID student, UUID actor, String code, String hash) { UUID id=UUID.randomUUID(); jdbc.update("INSERT INTO extension.certificate (id,public_code,project_id,period_id,student_id,content_hash,issued_by) VALUES (?,?,?,?,?,?,?)", id, code, project, period, student, hash, actor); return find(code); }
    public CertificateResponse find(String code) { return jdbc.queryForObject("SELECT id,public_code,project_id,period_id,student_id,content_hash,status,issued_at,revoked_at FROM extension.certificate WHERE public_code=?", (rs,n)->map(rs), code); }
    public int revoke(String code, String reason) { return jdbc.update("UPDATE extension.certificate SET status='REVOKED',revoked_at=now(),revocation_reason=? WHERE public_code=? AND status='ACTIVE'", reason.trim(), code); }
    private static CertificateResponse map(ResultSet rs) throws SQLException { String code=rs.getString("public_code"); return new CertificateResponse(rs.getObject("id",UUID.class),code,rs.getObject("project_id",UUID.class),rs.getObject("period_id",UUID.class),rs.getObject("student_id",UUID.class),rs.getString("content_hash"),rs.getString("status"),rs.getTimestamp("issued_at").toInstant(),rs.getTimestamp("revoked_at")==null?null:rs.getTimestamp("revoked_at").toInstant(),"/public/certificates/"+code); }
}
