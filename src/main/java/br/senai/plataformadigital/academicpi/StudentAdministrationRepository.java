package br.senai.plataformadigital.academicpi;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class StudentAdministrationRepository {
    private final JdbcTemplate jdbc;

    public StudentAdministrationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean isCoordinator(UUID userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                    WHERE ur.user_id = ? AND r.name = 'COORDENACAO' AND ur.scope_type = 'GLOBAL')
                """, Boolean.class, userId));
    }

    public List<StudentResponse> findAll(String query) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT u.id, u.full_name, u.email, u.cpf, al.registration_number,
                       c.code AS class_code, c.name AS class_name, c.term, u.status
                FROM auth."user" u
                LEFT JOIN institutional.academic_link al ON al.user_id = u.id AND al.role = 'student'
                LEFT JOIN institutional.academic_class c ON c.id = al.class_id
                WHERE EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                              WHERE ur.user_id = u.id AND r.name = 'ALUNO')
                """);
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        if (query != null && !query.isBlank()) {
            sql.append(" AND (u.full_name ILIKE ? OR u.email ILIKE ? OR u.cpf ILIKE ? OR al.registration_number ILIKE ?)");
            String value = "%" + query.trim() + "%";
            args.add(value); args.add(value); args.add(value); args.add(value);
        }
        sql.append(" ORDER BY u.full_name, c.code NULLS LAST LIMIT 500");
        return jdbc.query(sql.toString(), (rs, row) -> new StudentResponse(
                rs.getObject("id", UUID.class), rs.getString("full_name"), rs.getString("email"),
                rs.getString("cpf"), rs.getString("registration_number"), rs.getString("class_code"),
                rs.getString("class_name"), rs.getString("term"), rs.getString("status")), args.toArray());
    }

    public StudentResponse create(StudentRequest request, UUID actor) {
        UUID id = UUID.randomUUID();
        UUID classId = jdbc.queryForObject("SELECT id FROM institutional.academic_class WHERE code = ? AND is_active = true", UUID.class, request.classCode().trim());
        jdbc.update("INSERT INTO auth.\"user\" (id, email, full_name, cpf, status) VALUES (?, ?, ?, ?, 'pending')",
                id, request.email().trim().toLowerCase(), request.fullName().trim(), request.cpf().trim());
        jdbc.update("INSERT INTO user_roles (user_id, role_id, scope_type, scope_id, assigned_by) SELECT ?, id, 'GLOBAL', '*', ? FROM roles WHERE name = 'ALUNO'",
                id, actor);
        jdbc.update("INSERT INTO institutional.academic_link (id, user_id, class_id, role, registration_number, status) VALUES (?, ?, ?, 'student', ?, 'active')",
                UUID.randomUUID(), id, classId, request.registrationNumber().trim());
        return findById(id);
    }

    public StudentResponse findById(UUID id) {
        return findAll("").stream().filter(student -> student.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Aluno criado, mas não pôde ser consultado."));
    }

    public void updateStatus(UUID id, String status) {
        jdbc.update("UPDATE auth.\"user\" SET status = ?, updated_at = now() WHERE id = ?", status, id);
        jdbc.update("UPDATE institutional.academic_link SET status = ?, updated_at = now() WHERE user_id = ? AND role = 'student'",
                "active".equals(status) ? "active" : "inactive", id);
    }

    public StudentResponse update(UUID id, StudentUpdateRequest request) {
        UUID classId = jdbc.queryForObject("SELECT id FROM institutional.academic_class WHERE code = ? AND is_active = true", UUID.class, request.classCode().trim());
        jdbc.update("UPDATE auth.\"user\" SET email = ?, full_name = ?, cpf = ?, updated_at = now() WHERE id = ?", request.email().trim().toLowerCase(), request.fullName().trim(), request.cpf().trim(), id);
        jdbc.update("UPDATE institutional.academic_link SET class_id = ?, registration_number = ?, updated_at = now() WHERE user_id = ? AND role = 'student'", classId, request.registrationNumber().trim(), id);
        return findById(id);
    }

    public String exportCsv(String query) {
        StringBuilder csv = new StringBuilder("nome,email,cpf,matricula,turma,periodo,status\n");
        findAll(query).forEach(item -> csv.append(String.join(",", csv(item.fullName()), csv(item.email()), csv(item.cpf()), csv(item.registrationNumber()), csv(item.classCode()), csv(item.term()), csv(item.status()))).append('\n'));
        return csv.toString();
    }
    private String csv(String value) { if (value == null) return ""; return "\"" + value.replace("\"", "\"\"") + "\""; }

    public List<TeacherResponse> findTeachers(String query) {
        String pattern = query == null || query.isBlank() ? "%" : "%" + query.trim() + "%";
        return jdbc.query("SELECT u.id, u.full_name, u.email, u.status FROM auth.\"user\" u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id WHERE r.name = 'DOCENTE' AND (u.full_name ILIKE ? OR u.email ILIKE ?) ORDER BY u.full_name LIMIT 500", (rs, row) -> new TeacherResponse(rs.getObject("id", UUID.class), rs.getString("full_name"), rs.getString("email"), rs.getString("status")), pattern, pattern);
    }
    public TeacherResponse createTeacher(StudentRequest request, UUID actor) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO auth.\"user\" (id, email, full_name, cpf, status) VALUES (?, ?, ?, ?, 'pending')", id, request.email().trim().toLowerCase(), request.fullName().trim(), request.cpf().trim());
        jdbc.update("INSERT INTO user_roles (user_id, role_id, scope_type, scope_id, assigned_by) SELECT ?, id, 'GLOBAL', '*', ? FROM roles WHERE name = 'DOCENTE'", id, actor);
        return findTeachers(request.email()).stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }
}
