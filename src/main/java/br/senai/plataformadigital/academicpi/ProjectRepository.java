package br.senai.plataformadigital.academicpi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class ProjectRepository {
    private final JdbcTemplate jdbc;

    public ProjectRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean hasRole(UUID userId, String role) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                    WHERE ur.user_id = ? AND r.name = ?
                      AND ur.scope_type = 'GLOBAL'
                ) OR EXISTS (
                    SELECT 1 FROM institutional.academic_link al
                    WHERE al.user_id = ? AND al.role = ? AND al.status = 'active'
                )
                """, Boolean.class, userId, role, userId, role.equals("DOCENTE") ? "professor" : "coordinator"));
    }

    public boolean isActiveStudent(UUID userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM institutional.academic_link
                    WHERE user_id = ? AND role = 'student' AND status = 'active'
                )
                """, Boolean.class, userId));
    }

    public boolean isResponsible(UUID projectId, UUID userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM academic_pi.project_responsible
                    WHERE project_id = ? AND user_id = ?)
                """, Boolean.class, projectId, userId));
    }

    public UUID insertProject(ProjectRequest request, UUID actor) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO academic_pi.project
                    (id, title, summary, description, classification, continuity, end_date,
                     publication_status, lifecycle_status, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)
                """, id, request.title(), request.summary(), request.description(),
                request.classification().name(), request.continuity().name(), request.endDate(),
                request.publicationStatus().name(), actor);
        request.responsibleTeacherIds().stream().distinct().forEach(teacher ->
                jdbc.update("INSERT INTO academic_pi.project_responsible (project_id, user_id) VALUES (?, ?)", id, teacher));
        return id;
    }

    public ProjectResponse find(UUID id) {
        return jdbc.queryForObject("""
                SELECT id, title, summary, description, classification, continuity, end_date,
                       publication_status, lifecycle_status, created_at, updated_at
                FROM academic_pi.project WHERE id = ?
                """, (rs, row) -> map(rs, id), id);
    }

    public List<ProjectResponse> findPublic() {
        return jdbc.query("""
                SELECT id, title, summary, description, classification, continuity, end_date,
                       publication_status, lifecycle_status, created_at, updated_at
                FROM academic_pi.project
                WHERE publication_status = 'PUBLISHED' AND lifecycle_status = 'ACTIVE'
                ORDER BY updated_at DESC, id
                """, (rs, row) -> map(rs, rs.getObject("id", UUID.class)));
    }

    public PublicProjectPage findPublicPage(String courseCode, String semester, String odsCode, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE p.publication_status = 'PUBLISHED' AND p.lifecycle_status = 'ACTIVE'");
        List<Object> args = new ArrayList<>();
        if (courseCode != null && !courseCode.isBlank()) { where.append(" AND p.course_code = ?"); args.add(courseCode.trim()); }
        if (semester != null && !semester.isBlank()) { where.append(" AND EXISTS (SELECT 1 FROM academic_pi.enrollment_period epf WHERE epf.project_id = p.id AND epf.academic_term = ?)"); args.add(semester.trim()); }
        if (odsCode != null && !odsCode.isBlank()) { where.append(" AND ? = ANY(p.ods_codes)"); args.add(odsCode.trim()); }
        Number total = jdbc.queryForObject("SELECT count(*) FROM academic_pi.project p" + where, Number.class, args.toArray());
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(pageSize); queryArgs.add(page * pageSize);
        List<PublicProjectResponse> items = jdbc.query("""
                SELECT p.id, p.title, p.summary, p.description, p.classification, p.continuity,
                       p.course_code, p.ods_codes, p.updated_at,
                       COALESCE((SELECT array_agg(ep.academic_term ORDER BY ep.academic_term)
                                 FROM academic_pi.enrollment_period ep WHERE ep.project_id = p.id), '{}') AS semesters
                FROM academic_pi.project p
                """ + where + " ORDER BY p.updated_at DESC, p.id LIMIT ? OFFSET ?", (rs, row) -> new PublicProjectResponse(
                rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("summary"),
                rs.getString("description"), ProjectClassification.valueOf(rs.getString("classification")),
                ProjectContinuity.valueOf(rs.getString("continuity")), rs.getString("course_code"),
                List.of((String[]) rs.getArray("semesters").getArray()),
                rs.getArray("ods_codes") == null ? List.of() : List.of((String[]) rs.getArray("ods_codes").getArray()),
                rs.getTimestamp("updated_at").toInstant()), queryArgs.toArray());
        int totalItems = total.intValue();
        return new PublicProjectPage(items, page, pageSize, totalItems, totalItems == 0 ? 0 : (totalItems + pageSize - 1) / pageSize);
    }

    public List<ProjectResponse> findManaged(UUID userId) {
        return jdbc.query("""
                SELECT p.id, p.title, p.summary, p.description, p.classification, p.continuity, p.end_date,
                       p.publication_status, p.lifecycle_status, p.created_at, p.updated_at
                FROM academic_pi.project p
                JOIN academic_pi.project_responsible r ON r.project_id = p.id
                WHERE r.user_id = ?
                ORDER BY p.updated_at DESC, p.id
                """, (rs, row) -> map(rs, rs.getObject("id", UUID.class)), userId);
    }

    public List<UUID> responsibleIds(UUID projectId) {
        return jdbc.query("SELECT user_id FROM academic_pi.project_responsible WHERE project_id = ? ORDER BY user_id",
                (rs, row) -> rs.getObject("user_id", UUID.class), projectId);
    }

    public void updatePublication(UUID projectId, ProjectPublicationStatus status) {
        int updated = jdbc.update("""
                UPDATE academic_pi.project SET publication_status = ?, updated_at = now()
                WHERE id = ? AND lifecycle_status = 'ACTIVE'
                """, status.name(), projectId);
        if (updated != 1) throw new IllegalStateException("Projeto inexistente ou arquivado.");
    }

    public void updateLifecycle(UUID projectId, ProjectLifecycleStatus status) {
        int updated = jdbc.update("""
                UPDATE academic_pi.project SET lifecycle_status = ?, updated_at = now()
                WHERE id = ?
                """, status.name(), projectId);
        if (updated != 1) throw new IllegalStateException("Projeto não encontrado.");
    }

    public UUID activePeriod(UUID projectId, Instant now) {
        return jdbc.query("""
                SELECT id FROM academic_pi.enrollment_period
                WHERE project_id = ? AND starts_at <= ? AND ? < ends_at
                ORDER BY starts_at DESC, ends_at DESC, id DESC LIMIT 1
                """, (rs, row) -> rs.getObject("id", UUID.class), projectId, Timestamp.from(now), Timestamp.from(now))
                .stream().findFirst().orElse(null);
    }

    public EnrollmentPeriodResponse insertPeriod(UUID projectId, EnrollmentPeriodRequest request, UUID actor) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO academic_pi.enrollment_period
                    (id, project_id, academic_term, starts_at, ends_at, created_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, projectId, request.academicTerm().trim(), Timestamp.from(request.startsAt()),
                Timestamp.from(request.endsAt()), actor);
        return findPeriod(projectId, id);
    }

    public EnrollmentPeriodResponse findPeriod(UUID projectId, UUID periodId) {
        return jdbc.queryForObject("""
                SELECT id, project_id, academic_term, starts_at, ends_at, created_at, updated_at
                FROM academic_pi.enrollment_period WHERE project_id = ? AND id = ?
                """, (rs, row) -> new EnrollmentPeriodResponse(
                rs.getObject("id", UUID.class), rs.getObject("project_id", UUID.class),
                rs.getString("academic_term"), rs.getTimestamp("starts_at").toInstant(),
                rs.getTimestamp("ends_at").toInstant(), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()), projectId, periodId);
    }

    public List<EnrollmentPeriodResponse> findPeriods(UUID projectId) {
        return jdbc.query("""
                SELECT id, project_id, academic_term, starts_at, ends_at, created_at, updated_at
                FROM academic_pi.enrollment_period WHERE project_id = ?
                ORDER BY starts_at DESC, id
                """, (rs, row) -> new EnrollmentPeriodResponse(
                rs.getObject("id", UUID.class), rs.getObject("project_id", UUID.class),
                rs.getString("academic_term"), rs.getTimestamp("starts_at").toInstant(),
                rs.getTimestamp("ends_at").toInstant(), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()), projectId);
    }

    public ParticipationRequestResponse createRequest(UUID projectId, UUID periodId, UUID studentId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO academic_pi.participation_request (id, project_id, period_id, student_id)
                VALUES (?, ?, ?, ?)
                """, id, projectId, periodId, studentId);
        return findRequest(id);
    }

    public ParticipationRequestResponse findRequest(UUID id) {
        return jdbc.queryForObject("""
                SELECT id, project_id, period_id, student_id, status, requested_at, decided_at, withdrawn_at
                FROM academic_pi.participation_request WHERE id = ?
                """, ProjectRepository::mapRequest, id);
    }

    public List<ParticipationRequestResponse> findProjectRequests(UUID projectId) {
        return jdbc.query("""
                SELECT id, project_id, period_id, student_id, status, requested_at, decided_at, withdrawn_at
                FROM academic_pi.participation_request WHERE project_id = ?
                ORDER BY requested_at DESC, id
                """, ProjectRepository::mapRequest, projectId);
    }

    public List<ParticipationRequestResponse> findStudentRequests(UUID studentId) {
        return jdbc.query("""
                SELECT id, project_id, period_id, student_id, status, requested_at, decided_at, withdrawn_at
                FROM academic_pi.participation_request WHERE student_id = ?
                ORDER BY requested_at DESC, id
                """, ProjectRepository::mapRequest, studentId);
    }

    public List<ParticipantResponse> findParticipants(UUID projectId, UUID periodId) {
        return jdbc.query("""
                SELECT project_id, period_id, student_id, status, joined_at, removed_at, removal_reason
                FROM academic_pi.project_participant
                WHERE project_id = ? AND period_id = ?
                ORDER BY joined_at, student_id
                """, (rs, row) -> new ParticipantResponse(
                rs.getObject("project_id", UUID.class), rs.getObject("period_id", UUID.class),
                rs.getObject("student_id", UUID.class), rs.getString("status"),
                rs.getTimestamp("joined_at").toInstant(),
                rs.getTimestamp("removed_at") == null ? null : rs.getTimestamp("removed_at").toInstant(),
                rs.getString("removal_reason")), projectId, periodId);
    }

    public ParticipationRequestResponse decideRequest(UUID requestId, ParticipationRequestStatus status, UUID actor) {
        int updated = jdbc.update("""
                UPDATE academic_pi.participation_request
                SET status = ?, decided_at = now(), decided_by = ?
                WHERE id = ? AND status = 'PENDING'
                """, status.name(), actor, requestId);
        if (updated != 1) throw new IllegalStateException("Solicitação inexistente ou já decidida.");
        if (status == ParticipationRequestStatus.APPROVED) {
            ParticipationRequestResponse request = findRequest(requestId);
            jdbc.update("""
                    INSERT INTO academic_pi.project_participant (project_id, period_id, student_id)
                    VALUES (?, ?, ?)
                    """, request.projectId(), request.periodId(), request.studentId());
        }
        return findRequest(requestId);
    }

    public int withdrawRequest(UUID requestId, UUID studentId, UUID periodId) {
        return jdbc.update("""
                UPDATE academic_pi.participation_request
                SET status = 'WITHDRAWN', withdrawn_at = now()
                WHERE id = ? AND student_id = ? AND period_id = ? AND status = 'PENDING'
                """, requestId, studentId, periodId);
    }

    public int removeParticipant(UUID projectId, UUID periodId, UUID studentId, UUID actor, String reason) {
        return jdbc.update("""
                UPDATE academic_pi.project_participant
                SET status = 'REMOVED', removed_at = now(), removed_by = ?, removal_reason = ?
                WHERE project_id = ? AND period_id = ? AND student_id = ? AND status = 'ACTIVE'
                """, actor, reason, projectId, periodId, studentId);
    }

    private static ParticipationRequestResponse mapRequest(ResultSet rs, int row) throws SQLException {
        return new ParticipationRequestResponse(
                rs.getObject("id", UUID.class), rs.getObject("project_id", UUID.class),
                rs.getObject("period_id", UUID.class), rs.getObject("student_id", UUID.class),
                ParticipationRequestStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("requested_at").toInstant(),
                rs.getTimestamp("decided_at") == null ? null : rs.getTimestamp("decided_at").toInstant(),
                rs.getTimestamp("withdrawn_at") == null ? null : rs.getTimestamp("withdrawn_at").toInstant());
    }

    private ProjectResponse map(ResultSet rs, UUID id) throws SQLException {
        return new ProjectResponse(id, rs.getString("title"), rs.getString("summary"),
                rs.getString("description"), ProjectClassification.valueOf(rs.getString("classification")),
                ProjectContinuity.valueOf(rs.getString("continuity")),
                rs.getObject("end_date", LocalDate.class),
                ProjectPublicationStatus.valueOf(rs.getString("publication_status")),
                rs.getString("lifecycle_status"), responsibleIds(id),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
