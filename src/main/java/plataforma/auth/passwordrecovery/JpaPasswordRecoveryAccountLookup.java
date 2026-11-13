package plataforma.auth.passwordrecovery;

import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import plataforma.repository.UserAccountRepository;

/**
 * Temporary account adapter; the official identity key remains owned by AUTH-10-DB.
 *
 * <p>The repository is resolved when a lookup runs, not when the bean is created, so application
 * contexts that run without a database (for example the HTTP and contract tests) can still start.
 * With a database nothing changes; without one, a lookup fails with a clear missing-bean error.
 */
@Component
class JpaPasswordRecoveryAccountLookup implements PasswordRecoveryAccountLookup {

    private final ObjectProvider<UserAccountRepository> users;

    JpaPasswordRecoveryAccountLookup(ObjectProvider<UserAccountRepository> users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findUserIdByEmail(String normalizedEmail) {
        return users.getObject().findByEmailIgnoreCase(normalizedEmail).map(user -> user.getId());
    }
}
