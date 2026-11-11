package plataforma.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import plataforma.model.ExaminationBoard;

public interface ExaminationBoardRepository extends JpaRepository<ExaminationBoard, UUID> {
}
