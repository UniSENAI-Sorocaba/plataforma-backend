package plataforma.service;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.dto.EvaluationRequest;
import plataforma.dto.EvaluationResponse;
import plataforma.dto.PageResponse;
import plataforma.exception.ResourceNotFoundException;
import plataforma.mapper.EvaluationMapper;
import plataforma.model.Evaluation;
import plataforma.repository.EvaluationRepository;

// TODO BE-015: calcular a nota final pela rubrica e consolidar o resultado da banca (auditável).
@Service
@Transactional(readOnly = true)
public class EvaluationService {

    private final EvaluationRepository repository;
    private final EvaluationMapper mapper;
    private final ExaminationBoardService boardService;
    private final UserService userService;

    public EvaluationService(EvaluationRepository repository, EvaluationMapper mapper,
            ExaminationBoardService boardService, UserService userService) {
        this.repository = repository;
        this.mapper = mapper;
        this.boardService = boardService;
        this.userService = userService;
    }

    @Transactional
    public EvaluationResponse create(EvaluationRequest request) {
        var evaluation = new Evaluation();
        copyToEntity(request, evaluation);
        return mapper.toResponse(repository.saveAndFlush(evaluation));
    }

    public PageResponse<EvaluationResponse> findAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable), mapper::toResponse);
    }

    public EvaluationResponse findById(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional
    public EvaluationResponse update(UUID id, EvaluationRequest request) {
        var evaluation = findEntity(id);
        copyToEntity(request, evaluation);
        return mapper.toResponse(repository.saveAndFlush(evaluation));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findEntity(id));
    }

    public Evaluation findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada: " + id));
    }

    private void copyToEntity(EvaluationRequest request, Evaluation evaluation) {
        mapper.copyToEntity(request, evaluation);
        evaluation.setBoard(boardService.findEntity(request.boardId()));
        evaluation.setEvaluator(userService.findEntity(request.evaluatorId()));
    }
}
