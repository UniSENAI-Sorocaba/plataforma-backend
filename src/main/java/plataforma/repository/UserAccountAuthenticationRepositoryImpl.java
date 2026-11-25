package plataforma.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import plataforma.auth.AuthenticatedAccount;

public class UserAccountAuthenticationRepositoryImpl implements UserAccountAuthenticationRepository {
    private static final String FIND_BY_CPF = """
            SELECT u.id, u.password_hash, u.status,
                   (
                       SELECT CASE r.name
                           WHEN 'ALUNO' THEN 'STUDENT'
                           WHEN 'DOCENTE' THEN 'PROFESSOR'
                           WHEN 'COORDENACAO' THEN 'COORDINATOR'
                           ELSE upper(r.name)
                       END
                       FROM user_roles ur
                       JOIN roles r ON r.id = ur.role_id
                       WHERE ur.user_id = u.id AND ur.scope_type = 'GLOBAL'
                       ORDER BY CASE r.name
                           WHEN 'ADMIN' THEN 1
                           WHEN 'COORDENACAO' THEN 2
                           WHEN 'DOCENTE' THEN 3
                           ELSE 4
                       END
                       LIMIT 1
                   ) AS role
            FROM auth."user" u
            WHERE u.cpf = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public UserAccountAuthenticationRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AuthenticatedAccount> findByCpf(String cpf) {
        List<AuthenticatedAccount> accounts = jdbcTemplate.query(
                FIND_BY_CPF,
                (resultSet, rowNumber) -> mapAccount(resultSet),
                cpf
        );
        return accounts.stream().findFirst();
    }

    @Override
    public int updatePasswordHash(UUID id, String currentHash, String replacementHash) {
        return jdbcTemplate.update(
                """
                UPDATE auth."user"
                SET password_hash = ?, updated_at = now()
                WHERE id = ? AND password_hash = ? AND status = 'active'
                """,
                replacementHash,
                id,
                currentHash
        );
    }

    private AuthenticatedAccount mapAccount(ResultSet resultSet) throws SQLException {
        return new AuthenticatedAccount(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("password_hash"),
                resultSet.getString("status"),
                resultSet.getString("role")
        );
    }
}
