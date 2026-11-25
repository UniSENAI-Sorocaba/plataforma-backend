package plataforma.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import plataforma.model.Project;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
}
