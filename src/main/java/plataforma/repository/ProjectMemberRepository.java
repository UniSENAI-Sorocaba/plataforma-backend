package plataforma.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import plataforma.model.ProjectMember;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
}
