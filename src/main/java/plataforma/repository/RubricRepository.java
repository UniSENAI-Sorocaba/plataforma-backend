package plataforma.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import plataforma.model.Rubric;

public interface RubricRepository extends JpaRepository<Rubric, UUID> {
    boolean existsByCode(String code);
}
