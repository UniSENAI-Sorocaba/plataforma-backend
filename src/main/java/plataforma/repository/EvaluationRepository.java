package plataforma.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import plataforma.model.Evaluation;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {
}
