package br.senai.plataformadigital.auth;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

/** PostgreSQL adapter for the DB-005 auth.session and auth.refresh_token tables. */
public final class JdbcRefreshTokenRepository implements RefreshTokenRepository {
    private final DataSource dataSource;

    public JdbcRefreshTokenRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createSession(UUID sessionId, UUID userId, UUID familyId, Instant createdAt, Instant expiresAt) {
        executeUpdate("""
                INSERT INTO auth.session (id, user_id, family_id, status, created_at, expires_at)
                VALUES (?, ?, ?, 'active', ?, ?)
                """, statement -> {
            statement.setObject(1, sessionId);
            statement.setObject(2, userId);
            statement.setObject(3, familyId);
            statement.setTimestamp(4, Timestamp.from(createdAt));
            statement.setTimestamp(5, Timestamp.from(expiresAt));
        });
    }

    @Override
    public void save(RefreshTokenRecord token) {
        executeUpdate("""
                INSERT INTO auth.refresh_token
                    (id, session_id, family_id, token_hash, issued_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, statement -> {
            statement.setObject(1, token.id());
            statement.setObject(2, token.sessionId());
            statement.setObject(3, token.familyId());
            statement.setString(4, token.tokenHash());
            statement.setTimestamp(5, Timestamp.from(token.issuedAt()));
            statement.setTimestamp(6, Timestamp.from(token.expiresAt()));
        });
    }

    @Override
    public Optional<RefreshTokenRecord> findByHash(String tokenHash) {
        return find("SELECT t.id, s.user_id, t.session_id, t.family_id, t.token_hash, t.issued_at, t.expires_at, t.used_at, t.revoked_at "
                + "FROM auth.refresh_token t JOIN auth.session s ON s.id = t.session_id WHERE t.token_hash = ?",
                statement -> statement.setString(1, tokenHash)).stream().findFirst();
    }

    @Override
    public void replace(RefreshTokenRecord token) {
        executeUpdate("UPDATE auth.refresh_token SET used_at = ?, revoked_at = ? WHERE id = ?", statement -> {
            setNullableTimestamp(statement, 1, token.usedAt());
            setNullableTimestamp(statement, 2, token.revokedAt());
            statement.setObject(3, token.id());
        });
    }

    @Override
    public List<RefreshTokenRecord> findByFamily(UUID familyId) {
        return find("SELECT t.id, s.user_id, t.session_id, t.family_id, t.token_hash, t.issued_at, t.expires_at, t.used_at, t.revoked_at "
                + "FROM auth.refresh_token t JOIN auth.session s ON s.id = t.session_id WHERE t.family_id = ?",
                statement -> statement.setObject(1, familyId));
    }

    @Override
    public List<RefreshTokenRecord> findBySession(UUID sessionId) {
        return find("SELECT t.id, s.user_id, t.session_id, t.family_id, t.token_hash, t.issued_at, t.expires_at, t.used_at, t.revoked_at "
                + "FROM auth.refresh_token t JOIN auth.session s ON s.id = t.session_id WHERE t.session_id = ?",
                statement -> statement.setObject(1, sessionId));
    }

    @Override
    public void revokeSession(UUID sessionId, Instant revokedAt) {
        executeUpdate("UPDATE auth.session SET status = 'revoked', revoked_at = ? WHERE id = ?", statement -> {
            statement.setTimestamp(1, Timestamp.from(revokedAt));
            statement.setObject(2, sessionId);
        });
        executeUpdate("UPDATE auth.refresh_token SET revoked_at = ? WHERE session_id = ? AND revoked_at IS NULL",
                statement -> {
                    statement.setTimestamp(1, Timestamp.from(revokedAt));
                    statement.setObject(2, sessionId);
                });
    }

    @Override
    public void revokeFamily(UUID familyId, Instant revokedAt) {
        executeUpdate("UPDATE auth.session SET status = 'revoked', revoked_at = ? "
                + "WHERE family_id = ? AND status = 'active'", statement -> {
            statement.setTimestamp(1, Timestamp.from(revokedAt));
            statement.setObject(2, familyId);
        });
        executeUpdate("UPDATE auth.refresh_token SET revoked_at = ? WHERE family_id = ? AND revoked_at IS NULL",
                statement -> {
                    statement.setTimestamp(1, Timestamp.from(revokedAt));
                    statement.setObject(2, familyId);
                });
    }

    private List<RefreshTokenRecord> find(String sql, StatementBinder binder) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet result = statement.executeQuery()) {
                List<RefreshTokenRecord> tokens = new ArrayList<>();
                while (result.next()) {
                    tokens.add(read(result));
                }
                return tokens;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao consultar tokens de sessão.", exception);
        }
    }

    private void executeUpdate(String sql, StatementBinder binder) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao persistir o estado da sessão.", exception);
        }
    }

    private RefreshTokenRecord read(ResultSet result) throws SQLException {
        return new RefreshTokenRecord(
                result.getObject("id", UUID.class),
                result.getObject("user_id", UUID.class),
                result.getObject("session_id", UUID.class),
                result.getObject("family_id", UUID.class),
                result.getString("token_hash"),
                result.getTimestamp("issued_at").toInstant(),
                result.getTimestamp("expires_at").toInstant(),
                nullableInstant(result, "used_at"),
                nullableInstant(result, "revoked_at"));
    }

    private Instant nullableInstant(ResultSet result, String column) throws SQLException {
        Timestamp timestamp = result.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private void setNullableTimestamp(PreparedStatement statement, int index, Instant value) throws SQLException {
        if (value == null) {
            statement.setTimestamp(index, null);
        } else {
            statement.setTimestamp(index, Timestamp.from(value));
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
