package plataforma.repository;

import java.util.Optional;
import java.util.UUID;
import plataforma.auth.AuthenticatedAccount;

public interface UserAccountAuthenticationRepository {
    Optional<AuthenticatedAccount> findByCpf(String cpf);

    int updatePasswordHash(UUID id, String currentHash, String replacementHash);
}
