package plataforma.service;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.dto.ExaminationBoardRequest;
import plataforma.dto.ExaminationBoardResponse;
import plataforma.dto.PageResponse;
import plataforma.exception.ResourceNotFoundException;
import plataforma.mapper.ExaminationBoardMapper;
import plataforma.model.ExaminationBoard;
import plataforma.repository.ExaminationBoardRepository;

// TODO BE-011: impedir conflito de horário e vínculo inválido de avaliadores.
@Service
@Transactional(readOnly = true)
public class ExaminationBoardService {

    private final ExaminationBoardRepository repository;
    private final ExaminationBoardMapper mapper;
    private final ProjectService projectService;

    public ExaminationBoardService(ExaminationBoardRepository repository, ExaminationBoardMapper mapper,
            ProjectService projectService) {
        this.repository = repository;
        this.mapper = mapper;
        this.projectService = projectService;
    }

    @Transactional
    public ExaminationBoardResponse create(ExaminationBoardRequest request) {
        var board = new ExaminationBoard();
        copyToEntity(request, board);
        return mapper.toResponse(repository.saveAndFlush(board));
    }

    public PageResponse<ExaminationBoardResponse> findAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable), mapper::toResponse);
    }

    public ExaminationBoardResponse findById(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional
    public ExaminationBoardResponse update(UUID id, ExaminationBoardRequest request) {
        var board = findEntity(id);
        copyToEntity(request, board);
        return mapper.toResponse(repository.saveAndFlush(board));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findEntity(id));
    }

    public ExaminationBoard findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banca não encontrada: " + id));
    }

    private void copyToEntity(ExaminationBoardRequest request, ExaminationBoard board) {
        mapper.copyToEntity(request, board);
        board.setProject(projectService.findEntity(request.projectId()));
    }
}
