package plataforma.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import plataforma.model.Certificate;
import plataforma.model.CertificateStatus;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    boolean existsByEnrollmentIdAndStatus(UUID enrollmentId, CertificateStatus status);
}
