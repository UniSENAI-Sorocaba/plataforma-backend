package plataforma.auth.passwordrecovery;

import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import plataforma.repository.UserAccountRepository;

/** Temporary account adapter; the official identity key remains owned by AUTH-10-DB. */
@Component
@ConditionalOnBean(UserAccountRepository.class)
class JpaPasswordRecoveryAccountLookup implements PasswordRecoveryAccountLookup {

    private final UserAccountRepository users;

    JpaPasswordRecoveryAccountLookup(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findUserIdByEmail(String normalizedEmail) {
        return users.findByEmailIgnoreCase(normalizedEmail).map(user -> user.getId());
    }
}
