package br.senai.plataformadigital.academicpi;

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
public class DeliverableRepository {
    private final JdbcTemplate jdbc;

    public DeliverableRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean isActiveParticipant(UUID projectId, UUID periodId, UUID studentId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM academic_pi.project_participant
                 WHERE project_id = ? AND period_id = ? AND student_id = ? AND status = 'ACTIVE')
                """, Boolean.class, projectId, periodId, studentId));
    }

    public UUID insert(UUID projectId, UUID periodId, UUID ownerId, DeliverableInitiationRequest request,
                       String storageKey, String uploadTokenHash, Instant expiresAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO academic_pi.deliverable
                    (id, project_id, period_id, owner_id, original_filename, content_type, size_bytes,
                     storage_key, upload_token_hash, upload_expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, projectId, periodId, ownerId, request.fileName().trim(), request.contentType().trim(),
                request.sizeBytes(), storageKey, uploadTokenHash, Timestamp.from(expiresAt));
        return id;
    }

    public DeliverableResponse find(UUID id) {
        return jdbc.queryForObject("""
                SELECT id, project_id, period_id, owner_id, original_filename, content_type, size_bytes,
                       sha256, version, status, created_at, uploaded_at, upload_expires_at
                FROM academic_pi.deliverable WHERE id = ?
                """, DeliverableRepository::map, id);
    }

    public DeliverableRecord findForToken(UUID id, String tokenHash, String tokenKind) {
        String column = tokenKind.equals("upload") ? "upload_token_hash" : "download_token_hash";
        return jdbc.queryForObject("""
                SELECT id, project_id, period_id, owner_id, original_filename, content_type, size_bytes,
                       sha256, version, status, storage_key, upload_expires_at, download_expires_at
                FROM academic_pi.deliverable WHERE id = ? AND %s = ?
                """.formatted(column), (rs, row) -> new DeliverableRecord(
                rs.getObject("id", UUID.class), rs.getObject("project_id", UUID.class),
                rs.getObject("period_id", UUID.class), rs.getObject("owner_id", UUID.class),
                rs.getString("original_filename"), rs.getString("content_type"), rs.getLong("size_bytes"),
                rs.getString("sha256"), rs.getInt("version"), DeliverableStatus.valueOf(rs.getString("status")),
                rs.getString("storage_key"), rs.getTimestamp("upload_expires_at").toInstant(),
                rs.getTimestamp("download_expires_at") == null ? null : rs.getTimestamp("download_expires_at").toInstant()), id, tokenHash);
    }

    public void markAvailable(UUID id, String sha256, String downloadTokenHash, Instant downloadExpiresAt) {
        jdbc.update("""
                UPDATE academic_pi.deliverable SET status = 'AVAILABLE', sha256 = ?, uploaded_at = now(),
                    download_token_hash = ?, download_expires_at = ?
                WHERE id = ? AND status = 'PENDING_UPLOAD'
                """, sha256, downloadTokenHash, Timestamp.from(downloadExpiresAt), id);
    }

    public void cancel(UUID id) {
        jdbc.update("UPDATE academic_pi.deliverable SET status = 'CANCELLED', cancelled_at = now() WHERE id = ? AND status = 'PENDING_UPLOAD'", id);
    }

    public record DeliverableRecord(UUID id, UUID projectId, UUID periodId, UUID ownerId, String fileName,
                                    String contentType, long sizeBytes, String sha256, int version,
                                    DeliverableStatus status, String storageKey, Instant uploadExpiresAt,
                                    Instant downloadExpiresAt) { }

    private static DeliverableResponse map(ResultSet rs, int row) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        return new DeliverableResponse(id, rs.getObject("project_id", UUID.class), rs.getObject("period_id", UUID.class),
                rs.getObject("owner_id", UUID.class), rs.getString("original_filename"), rs.getString("content_type"),
                rs.getLong("size_bytes"), rs.getString("sha256"), rs.getInt("version"),
                DeliverableStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("uploaded_at") == null ? null : rs.getTimestamp("uploaded_at").toInstant(),
                rs.getTimestamp("upload_expires_at").toInstant(), null, null);
    }
}
