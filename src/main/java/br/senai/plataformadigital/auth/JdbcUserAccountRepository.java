package br.senai.plataformadigital.auth;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

/** JDBC adapter for the existing auth.user and RBAC tables. */
public final class JdbcUserAccountRepository implements UserAccountRepository {
    private final DataSource dataSource;

    public JdbcUserAccountRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<AuthenticatedUser> findByEmail(String email) {
        return find("email", email);
    }

    @Override
    public Optional<AuthenticatedUser> findById(UUID id) {
        return find("id", id);
    }

    @Override
    public Optional<AuthenticatedUser> findByCpf(String cpf) {
        return find("cpf", cpf);
    }

    @Override
    public boolean activateFirstAccess(String cpf, String passwordHash) {
        return update("UPDATE auth.\"user\" SET password_hash = ?, status = 'active', updated_at = now() "
                + "WHERE cpf = ? AND status IN ('pending', 'active') AND password_hash IS NULL", passwordHash, cpf);
    }

    @Override
    public boolean updatePasswordByEmail(String email, String passwordHash) {
        return update("UPDATE auth.\"user\" SET password_hash = ?, status = 'active', updated_at = now() "
                + "WHERE email = ? AND status <> 'inactive'", passwordHash, email);
    }

    private Optional<AuthenticatedUser> find(String column, Object value) {
        String sql = "SELECT u.id, u.email, u.full_name, u.password_hash, u.status, "
                + "COALESCE((SELECT r.name FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
                + "WHERE ur.user_id = u.id AND ur.scope_type = 'GLOBAL' "
                + "ORDER BY CASE r.name WHEN 'ADMIN' THEN 1 WHEN 'COORDENACAO' THEN 2 WHEN 'DOCENTE' THEN 3 ELSE 4 END LIMIT 1), 'ALUNO') AS role "
                + "FROM auth.\"user\" u WHERE u." + column + " = ?";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            try (var result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(new AuthenticatedUser(
                        result.getObject("id", UUID.class),
                        result.getString("email"),
                        result.getString("password_hash"),
                        normalizeRole(result.getString("role")),
                        "active".equals(result.getString("status")),
                        result.getString("full_name")));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao consultar conta acadêmica.", exception);
        }
    }

    private boolean update(String sql, String value, String lookup) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            statement.setString(2, lookup);
            return statement.executeUpdate() == 1;
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao atualizar credencial acadêmica.", exception);
        }
    }

    private String normalizeRole(String role) {
        return "COORDENACAO".equals(role) ? "COORDENADOR" : role;
    }
}
