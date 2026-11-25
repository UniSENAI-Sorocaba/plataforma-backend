package plataforma.mapper;

import org.springframework.stereotype.Component;
import plataforma.dto.ProjectRequest;
import plataforma.dto.ProjectResponse;
import plataforma.model.Project;

@Component
public class ProjectMapper {

    public ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getProjectType(),
                project.getStatus(),
                project.getSemester(),
                project.getCourse(),
                project.getLeader().getId(),
                project.getAdvisor() == null ? null : project.getAdvisor().getId(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    /** Copia os campos simples. Líder e orientador são resolvidos no service. */
    public void copyToEntity(ProjectRequest request, Project project) {
        project.setTitle(request.title());
        project.setDescription(request.description());
        project.setProjectType(request.projectType());
        project.setStatus(request.status());
        project.setSemester(request.semester());
        project.setCourse(request.course());
    }
}
