package plataforma.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import plataforma.model.Project;
import plataforma.model.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

  Page<Project> findBystatusNot(ProjectStatus status, Pageable pageable);
}
