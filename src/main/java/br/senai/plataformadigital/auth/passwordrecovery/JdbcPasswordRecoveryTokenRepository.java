package br.senai.plataformadigital.auth.passwordrecovery;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

/** Durable PostgreSQL adapter for auth.password_recovery_token. */
public final class JdbcPasswordRecoveryTokenRepository implements PasswordRecoveryTokenRepository {
    private final DataSource dataSource;

    public JdbcPasswordRecoveryTokenRepository(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public Optional<PasswordRecoveryToken> findByTokenHash(String tokenHash) {
        return find("SELECT t.id, u.email AS subject, t.token_hash, t.created_at, t.expires_at, t.used_at, t.revoked_at "
                + "FROM auth.password_recovery_token t JOIN auth.\"user\" u ON u.id = t.user_id WHERE t.token_hash = ?",
                tokenHash).stream().findFirst();
    }

    @Override
    public void save(PasswordRecoveryToken token) {
        execute("INSERT INTO auth.password_recovery_token (id, user_id, token_hash, created_at, expires_at) "
                + "SELECT ?, u.id, ?, ?, ? FROM auth.\"user\" u WHERE u.email = ?",
                statement -> {
                    statement.setObject(1, token.id());
                    statement.setString(2, token.tokenHash());
                    statement.setTimestamp(3, Timestamp.from(token.issuedAt()));
                    statement.setTimestamp(4, Timestamp.from(token.expiresAt()));
                    statement.setString(5, token.subject());
                });
    }

    @Override
    public int revokeActiveBySubject(String subject, Instant revokedAt) {
        return update("UPDATE auth.password_recovery_token t SET revoked_at = ? "
                + "FROM auth.\"user\" u WHERE u.id = t.user_id AND lower(u.email) = lower(?) "
                + "AND t.used_at IS NULL AND t.revoked_at IS NULL AND t.expires_at > ?", revokedAt, subject, revokedAt);
    }

    @Override
    public Optional<PasswordRecoveryToken> consumeIfActive(String tokenHash, Instant consumedAt) {
        int changed = update("UPDATE auth.password_recovery_token SET used_at = ? "
                + "WHERE token_hash = ? AND used_at IS NULL AND revoked_at IS NULL AND expires_at > ?",
                consumedAt, tokenHash, consumedAt);
        return changed == 0 ? Optional.empty() : findByTokenHash(tokenHash);
    }

    @Override
    public boolean revokeIfActive(String tokenHash, Instant revokedAt) {
        return update("UPDATE auth.password_recovery_token SET revoked_at = ? "
                + "WHERE token_hash = ? AND used_at IS NULL AND revoked_at IS NULL AND expires_at > ?",
                revokedAt, tokenHash, revokedAt) == 1;
    }

    private List<PasswordRecoveryToken> find(String sql, Object... values) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            try (var result = statement.executeQuery()) {
                List<PasswordRecoveryToken> tokens = new ArrayList<>();
                while (result.next()) {
                    tokens.add(PasswordRecoveryToken.restored(
                            result.getObject("id", UUID.class), result.getString("subject"), result.getString("token_hash"),
                            result.getTimestamp("created_at").toInstant(), result.getTimestamp("expires_at").toInstant(),
                            nullable(result, "used_at"), nullable(result, "revoked_at")));
                }
                return tokens;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao consultar token de recuperação.", exception);
        }
    }

    private int update(String sql, Object... values) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            return statement.executeUpdate();
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao atualizar token de recuperação.", exception);
        }
    }

    private void execute(String sql, Binder binder) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao persistir token de recuperação.", exception);
        }
    }

    private Instant nullable(java.sql.ResultSet result, String column) throws java.sql.SQLException {
        Timestamp value = result.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    @FunctionalInterface private interface Binder { void bind(java.sql.PreparedStatement statement) throws Exception; }
}
