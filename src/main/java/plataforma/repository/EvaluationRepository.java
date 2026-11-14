package plataforma.repository;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import plataforma.model.Evaluation;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {
    List<Evaluation> findAllByBoardId(UUID boardId);
}
