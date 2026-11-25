package plataforma.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import plataforma.model.Certificate;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
}
