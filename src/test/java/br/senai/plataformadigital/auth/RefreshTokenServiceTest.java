package br.senai.plataformadigital.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private InMemoryRefreshTokenRepository repository;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRefreshTokenRepository();
        service = new RefreshTokenService(repository, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofDays(30));
    }

    @Test
    void rotatesTokenAndStoresOnlyHash() {
        IssuedRefreshToken first = service.issue(USER_ID);
        IssuedRefreshToken second = service.rotate(first.rawToken());

        assertThat(second.rawToken()).isNotEqualTo(first.rawToken());
        assertThat(repository.findByHash(RefreshTokenService.hash(first.rawToken()))).get()
                .extracting(RefreshTokenRecord::usedAt).isNotNull();
        assertThat(repository.findByHash(RefreshTokenService.hash(first.rawToken()))).get()
                .extracting(RefreshTokenRecord::tokenHash).isNotEqualTo(first.rawToken());
        assertThat(second.familyId()).isEqualTo(first.familyId());
    }

    @Test
    void reuseRevokesEntireFamily() {
        IssuedRefreshToken first = service.issue(USER_ID);
        IssuedRefreshToken second = service.rotate(first.rawToken());

        assertThatThrownBy(() -> service.rotate(first.rawToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.rotate(second.rawToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logoutRevokesSessionTokens() {
        IssuedRefreshToken first = service.issue(USER_ID);
        IssuedRefreshToken second = service.rotate(first.rawToken());

        service.revokeSession(second.sessionId());

        assertThatThrownBy(() -> service.rotate(second.rawToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void expiredTokenCannotRotate() {
        RefreshTokenService shortLived = new RefreshTokenService(repository,
                Clock.fixed(NOW.plus(Duration.ofDays(31)), ZoneOffset.UTC), Duration.ofDays(30));
        IssuedRefreshToken token = service.issue(USER_ID);

        assertThatThrownBy(() -> shortLived.rotate(token.rawToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
