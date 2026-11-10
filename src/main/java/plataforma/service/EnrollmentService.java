package plataforma.service;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.dto.EnrollmentRequest;
import plataforma.dto.EnrollmentResponse;
import plataforma.dto.PageResponse;
import plataforma.exception.ResourceNotFoundException;
import plataforma.mapper.EnrollmentMapper;
import plataforma.model.Enrollment;
import plataforma.repository.EnrollmentRepository;

// TODO BE-009: respeitar a janela do edital e a elegibilidade do aluno; impedir inscrição duplicada.
@Service
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository repository;
    private final EnrollmentMapper mapper;
    private final NoticeService noticeService;
    private final UserService userService;

    public EnrollmentService(EnrollmentRepository repository, EnrollmentMapper mapper, NoticeService noticeService,
            UserService userService) {
        this.repository = repository;
        this.mapper = mapper;
        this.noticeService = noticeService;
        this.userService = userService;
    }

    @Transactional
    public EnrollmentResponse create(EnrollmentRequest request) {
        var enrollment = new Enrollment();
        copyToEntity(request, enrollment);
        return mapper.toResponse(repository.saveAndFlush(enrollment));
    }

    public PageResponse<EnrollmentResponse> findAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable), mapper::toResponse);
    }

    public EnrollmentResponse findById(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional
    public EnrollmentResponse update(UUID id, EnrollmentRequest request) {
        var enrollment = findEntity(id);
        copyToEntity(request, enrollment);
        return mapper.toResponse(repository.saveAndFlush(enrollment));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findEntity(id));
    }

    /**
     * Busca a inscrição travando a linha até o fim da transação. Serializa operações concorrentes
     * sobre a mesma inscrição, como duas emissões de certificado ao mesmo tempo (BE-017).
     */
    @Transactional
    public Enrollment findEntityForUpdate(UUID id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada: " + id));
    }

    public Enrollment findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada: " + id));
    }

    private void copyToEntity(EnrollmentRequest request, Enrollment enrollment) {
        enrollment.setNotice(noticeService.findEntity(request.noticeId()));
        enrollment.setStudent(userService.findEntity(request.studentId()));
    }
}
