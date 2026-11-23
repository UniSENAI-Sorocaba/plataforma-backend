package br.senai.plataformadigital.academicpi;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "spring.datasource.url")
public class InstitutionalDirectoryRepository {
    private final JdbcTemplate jdbc;

    public InstitutionalDirectoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<InstitutionalDirectoryEntry> find(String role, String query) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT u.id, u.full_name, u.email, r.name AS role
                FROM auth."user" u
                JOIN user_roles ur ON ur.user_id = u.id AND ur.scope_type = 'GLOBAL'
                JOIN roles r ON r.id = ur.role_id
                WHERE u.status = 'active' AND r.name IN ('DOCENTE', 'COORDENACAO')
                """);
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        if (role != null && !role.isBlank()) {
            sql.append(" AND r.name = ?");
            args.add(role.equalsIgnoreCase("COORDENADOR") ? "COORDENACAO" : role.toUpperCase());
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (u.full_name ILIKE ? OR u.email ILIKE ?)");
            String pattern = "%" + query.trim() + "%";
            args.add(pattern);
            args.add(pattern);
        }
        sql.append(" ORDER BY u.full_name, u.email LIMIT 100");
        return jdbc.query(sql.toString(), (rs, row) -> new InstitutionalDirectoryEntry(
                rs.getObject("id", java.util.UUID.class), rs.getString("full_name"),
                rs.getString("email"), rs.getString("role")), args.toArray());
    }
}
