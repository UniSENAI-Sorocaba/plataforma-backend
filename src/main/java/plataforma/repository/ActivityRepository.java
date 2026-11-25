package plataforma.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import plataforma.model.Activity;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {
}
