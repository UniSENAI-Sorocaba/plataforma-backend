package plataforma.auth.passwordrecovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PasswordRecoveryJpaAdapterTests {
    private static final Instant ISSUED = Instant.parse("2026-11-12T10:00:00Z");
    private static final Instant EXPIRES = ISSUED.plusSeconds(600);
    private static final UUID TOKEN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void entityConversionPreservesIssuedUsedAndRevokedStates() {
        var issued = entity(PasswordRecoveryToken.issued(TOKEN_ID, USER_ID, "digest", ISSUED, EXPIRES)).toDomain();
        assertEquals(USER_ID, issued.userId());
        assertEquals("digest", issued.tokenHash());
        assertTrue(issued.isActiveAt(ISSUED.plusSeconds(1)));
        assertTrue(issued.toString().contains("used=false, revoked=false"));
        assertFalse(issued.toString().contains("digest"));

        var used = entity(domainToken().markUsedAt(ISSUED.plusSeconds(5))).toDomain();
        assertEquals(ISSUED.plusSeconds(5), used.usedAt());
        assertFalse(used.isActiveAt(ISSUED.plusSeconds(6)));
        assertTrue(used.toString().contains("used=true, revoked=false"));

        var revoked = entity(domainToken().markRevokedAt(ISSUED.plusSeconds(7))).toDomain();
        assertEquals(ISSUED.plusSeconds(7), revoked.revokedAt());
        assertFalse(revoked.isActiveAt(ISSUED.plusSeconds(8)));
        assertTrue(revoked.toString().contains("used=false, revoked=true"));
    }

    @Test
    void tokenRejectsInvalidExpiryAndStateTimestamps() {
        assertThrows(IllegalArgumentException.class, () -> PasswordRecoveryToken.issued(
                TOKEN_ID, USER_ID, "digest", ISSUED, ISSUED));
        assertThrows(IllegalArgumentException.class, () -> PasswordRecoveryToken.issued(
                TOKEN_ID, USER_ID, "digest", ISSUED, ISSUED.minusSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> domainToken().markUsedAt(ISSUED.minusSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> domainToken().markRevokedAt(ISSUED.minusSeconds(1)));
    }

    @Test
    void adapterMapsQueriesWritesRevocationsAndAtomicConsumption() {
        var jpa = mock(PasswordRecoveryTokenJpaRepository.class);
        var adapter = new JpaPasswordRecoveryTokenRepository(jpa);
        var savedEntity = entity(domainToken());
        when(jpa.findByTokenHash("digest")).thenReturn(Optional.of(savedEntity));
        when(jpa.countByUserIdAndIssuedAtGreaterThanEqual(USER_ID, ISSUED)).thenReturn(2L);
        when(jpa.revokeActiveByUserId(USER_ID, ISSUED)).thenReturn(3);

        assertEquals(USER_ID, adapter.findByTokenHash("digest").orElseThrow().userId());
        adapter.save(domainToken());
        verify(jpa).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.toDomain().tokenHash().equals("digest")));
        assertEquals(2L, adapter.countIssuedSince(USER_ID, ISSUED));
        assertEquals(3, adapter.revokeActiveByUserId(USER_ID, ISSUED));

        when(jpa.consumeIfActive("digest", ISSUED.plusSeconds(1))).thenReturn(0);
        assertTrue(adapter.consumeIfActive("digest", ISSUED.plusSeconds(1)).isEmpty());
        when(jpa.consumeIfActive("digest", ISSUED.plusSeconds(2))).thenReturn(1);
        assertEquals(USER_ID, adapter.consumeIfActive("digest", ISSUED.plusSeconds(2)).orElseThrow().userId());
        when(jpa.revokeIfActive("digest", ISSUED)).thenReturn(1);
        when(jpa.revokeIfActive("missing", ISSUED)).thenReturn(0);
        assertTrue(adapter.revokeIfActive("digest", ISSUED));
        assertFalse(adapter.revokeIfActive("missing", ISSUED));
    }

    private PasswordRecoveryToken domainToken() {
        return PasswordRecoveryToken.issued(TOKEN_ID, USER_ID, "digest", ISSUED, EXPIRES);
    }

    private PasswordRecoveryTokenEntity entity(PasswordRecoveryToken token) {
        return PasswordRecoveryTokenEntity.from(token);
    }
}
