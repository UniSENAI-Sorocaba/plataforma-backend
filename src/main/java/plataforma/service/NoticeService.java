package plataforma.service;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.dto.NoticeRequest;
import plataforma.dto.NoticeResponse;
import plataforma.dto.PageResponse;
import plataforma.exception.ResourceNotFoundException;
import plataforma.mapper.NoticeMapper;
import plataforma.model.Notice;
import plataforma.repository.NoticeRepository;

@Service
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository repository;
    private final NoticeMapper mapper;
    private final UserService userService;

    public NoticeService(NoticeRepository repository, NoticeMapper mapper, UserService userService) {
        this.repository = repository;
        this.mapper = mapper;
        this.userService = userService;
    }

    @Transactional
    public NoticeResponse create(NoticeRequest request) {
        var notice = new Notice();
        copyToEntity(request, notice);
        return mapper.toResponse(repository.saveAndFlush(notice));
    }

    public PageResponse<NoticeResponse> findAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable), mapper::toResponse);
    }

    public NoticeResponse findById(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional
    public NoticeResponse update(UUID id, NoticeRequest request) {
        var notice = findEntity(id);
        copyToEntity(request, notice);
        return mapper.toResponse(repository.saveAndFlush(notice));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findEntity(id));
    }

    public Notice findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Edital não encontrado: " + id));
    }

    private void copyToEntity(NoticeRequest request, Notice notice) {
        mapper.copyToEntity(request, notice);
        notice.setAdvisor(userService.findEntity(request.advisorId()));
    }
}
