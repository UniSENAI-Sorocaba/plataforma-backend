package plataforma.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import plataforma.model.Enrollment;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
}
