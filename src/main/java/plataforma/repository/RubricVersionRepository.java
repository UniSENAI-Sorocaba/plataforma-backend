package plataforma.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import plataforma.model.RubricVersion;

public interface RubricVersionRepository extends JpaRepository<RubricVersion, UUID> {
    Optional<RubricVersion> findByRubricIdAndVersionNumber(UUID rubricId, int versionNumber);
    Optional<RubricVersion> findTopByRubricIdOrderByVersionNumberDesc(UUID rubricId);
}
