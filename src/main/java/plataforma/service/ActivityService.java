package plataforma.service;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.dto.ActivityRequest;
import plataforma.dto.ActivityResponse;
import plataforma.dto.PageResponse;
import plataforma.exception.ResourceNotFoundException;
import plataforma.mapper.ActivityMapper;
import plataforma.model.Activity;
import plataforma.repository.ActivityRepository;

// TODO BE-014: endpoint de decisão (deferir/indeferir) com motivo e total de horas do aluno.
@Service
@Transactional(readOnly = true)
public class ActivityService {

    private final ActivityRepository repository;
    private final ActivityMapper mapper;
    private final EnrollmentService enrollmentService;

    public ActivityService(ActivityRepository repository, ActivityMapper mapper, EnrollmentService enrollmentService) {
        this.repository = repository;
        this.mapper = mapper;
        this.enrollmentService = enrollmentService;
    }

    @Transactional
    public ActivityResponse create(ActivityRequest request) {
        var activity = new Activity();
        copyToEntity(request, activity);
        return mapper.toResponse(repository.saveAndFlush(activity));
    }

    public PageResponse<ActivityResponse> findAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable), mapper::toResponse);
    }

    public ActivityResponse findById(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional
    public ActivityResponse update(UUID id, ActivityRequest request) {
        var activity = findEntity(id);
        copyToEntity(request, activity);
        return mapper.toResponse(repository.saveAndFlush(activity));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findEntity(id));
    }

    /** Total de horas aprovadas da inscrição; base para a emissão do certificado (BE-017). */
    public long approvedHours(UUID enrollmentId) {
        return repository.sumApprovedHours(enrollmentId);
    }

    public Activity findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada: " + id));
    }

    private void copyToEntity(ActivityRequest request, Activity activity) {
        mapper.copyToEntity(request, activity);
        activity.setEnrollment(enrollmentService.findEntity(request.enrollmentId()));
    }
}
