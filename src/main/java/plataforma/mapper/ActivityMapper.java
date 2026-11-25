package plataforma.mapper;

import org.springframework.stereotype.Component;
import plataforma.dto.ActivityRequest;
import plataforma.dto.ActivityResponse;
import plataforma.model.Activity;

@Component
public class ActivityMapper {

    public ActivityResponse toResponse(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getEnrollment().getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getHoursRequested(),
                activity.getHoursApproved(),
                activity.getEvidenceKey(),
                activity.getStatus(),
                activity.getDecisionReason(),
                activity.getReviewer() == null ? null : activity.getReviewer().getId(),
                activity.getSubmittedAt(),
                activity.getReviewedAt());
    }

    /** Copia os campos simples. A inscrição é resolvida no service. */
    public void copyToEntity(ActivityRequest request, Activity activity) {
        activity.setTitle(request.title());
        activity.setDescription(request.description());
        activity.setHoursRequested(request.hoursRequested());
        activity.setEvidenceKey(request.evidenceKey());
    }
}
