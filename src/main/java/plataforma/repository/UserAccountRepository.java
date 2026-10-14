package plataforma.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import plataforma.model.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
}
