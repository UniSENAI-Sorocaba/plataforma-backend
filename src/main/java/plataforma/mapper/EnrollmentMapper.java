package plataforma.mapper;

import org.springframework.stereotype.Component;
import plataforma.dto.EnrollmentResponse;
import plataforma.model.Enrollment;

/** A inscrição só tem relações (edital e aluno), resolvidas no service. */
@Component
public class EnrollmentMapper {

    public EnrollmentResponse toResponse(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getNotice().getId(),
                enrollment.getStudent().getId(),
                enrollment.getEnrolledAt());
    }
}
