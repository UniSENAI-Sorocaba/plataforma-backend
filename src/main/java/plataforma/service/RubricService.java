package plataforma.service;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubjectProvider;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import plataforma.dto.RubricCreateRequest;
import plataforma.dto.RubricCriterionRequest;
import plataforma.dto.RubricCriterionResponse;
import plataforma.dto.RubricVersionRequest;
import plataforma.dto.RubricVersionResponse;
import plataforma.exception.ConflictException;
import plataforma.exception.ResourceNotFoundException;
import plataforma.exception.RubricRuleViolationException;
import plataforma.model.AuditOperation;
import plataforma.model.Rubric;
import plataforma.model.RubricCriterion;
import plataforma.model.RubricVersion;
import plataforma.repository.RubricRepository;
import plataforma.repository.RubricVersionRepository;

/** Cria snapshots de rubricas; uma versão existente nunca é editada. */
@Service
@Validated
@ConditionalOnBean({RubricRepository.class, RubricVersionRepository.class})
@Transactional(readOnly = true)
public class RubricService {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository versionRepository;
    private final AuditEventService auditEventService;
    private final AuthenticatedSubjectProvider subjects;

    public RubricService(RubricRepository rubricRepository, RubricVersionRepository versionRepository,
            AuditEventService auditEventService, AuthenticatedSubjectProvider subjects) {
        this.rubricRepository = rubricRepository;
        this.versionRepository = versionRepository;
        this.auditEventService = auditEventService;
        this.subjects = subjects;
    }

    @Transactional
    public RubricVersionResponse create(@Valid RubricCreateRequest request) {
        if (rubricRepository.existsByCode(request.code())) {
            throw new ConflictException("Rubrica já existe");
        }
        UUID actorId = actorId();
        var rubric = new Rubric(request.code(), request.name(), actorId);
        var version = buildVersion(1, request.version(), actorId);
        rubric.addVersion(version);
        validate(version);
        rubricRepository.saveAndFlush(rubric);
        var response = toResponse(version);
        audit(AuditOperation.INSERT, version.getId(), null, rubricSnapshot(rubric, version));
        return response;
    }

    @Transactional
    public RubricVersionResponse createVersion(UUID rubricId, @Valid RubricVersionRequest request) {
        var rubric = findRubric(rubricId);
        int nextVersion = versionRepository.findTopByRubricIdOrderByVersionNumberDesc(rubricId)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
        UUID actorId = actorId();
        var version = buildVersion(nextVersion, request, actorId);
        validate(version);
        rubric.addVersion(version);
        rubricRepository.saveAndFlush(rubric);
        var response = toResponse(version);
        audit(AuditOperation.INSERT, version.getId(), null, versionSnapshot(version));
        return response;
    }

    public RubricVersionResponse findVersion(UUID rubricId, int versionNumber) {
        return versionRepository.findByRubricIdAndVersionNumber(rubricId, versionNumber)
                .map(RubricService::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Versão de rubrica não encontrada"));
    }

    private Rubric findRubric(UUID id) {
        return rubricRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rubrica não encontrada: " + id));
    }

    private RubricVersion buildVersion(int number, RubricVersionRequest request, UUID actorId) {
        var version = new RubricVersion(number, request.minimumScore(), request.maximumScore(),
                request.roundingScale(), request.roundingMode(), request.formula(), actorId);
        request.criteria().forEach(criterion -> version.addCriterion(new RubricCriterion(
                criterion.code(), criterion.name(), criterion.weight(), criterion.minimumScore(),
                criterion.maximumScore(), criterion.position())));
        return version;
    }

    private void validate(RubricVersion version) {
        try {
            version.calculationPolicy();
        } catch (IllegalArgumentException invalid) {
            throw new RubricRuleViolationException(invalid.getMessage());
        }
    }

    private UUID actorId() {
        return subjects.currentSubject()
                .map(subject -> parseActor(subject.id()))
                .orElse(null);
    }

    private UUID parseActor(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private void audit(AuditOperation operation, UUID recordId, Map<String, Object> oldData,
            Map<String, Object> newData) {
        auditEventService.record(new plataforma.dto.RecordAuditEventRequest(
                "academic_pi", "rubric_version", operation, recordId, actorId(), oldData, newData));
    }

    private static Map<String, Object> rubricSnapshot(Rubric rubric, RubricVersion version) {
        var data = new LinkedHashMap<String, Object>(versionSnapshot(version));
        data.put("rubric_code", rubric.getCode());
        data.put("rubric_name", rubric.getName());
        return data;
    }

    private static Map<String, Object> versionSnapshot(RubricVersion version) {
        var data = new LinkedHashMap<String, Object>();
        data.put("rubric_id", version.getRubric() == null ? null : version.getRubric().getId());
        data.put("version", version.getVersionNumber());
        data.put("minimum_score", version.getMinimumScore());
        data.put("maximum_score", version.getMaximumScore());
        data.put("rounding_scale", version.getRoundingScale());
        data.put("rounding_mode", version.getRoundingMode().name());
        data.put("formula", version.getFormula().name());
        data.put("criteria", version.getCriteria().stream().map(RubricService::criterionSnapshot).toList());
        return data;
    }

    private static Map<String, Object> criterionSnapshot(RubricCriterion criterion) {
        return Map.of(
                "code", criterion.getCode(),
                "name", criterion.getName(),
                "weight", criterion.getWeight(),
                "minimum_score", criterion.getMinimumScore(),
                "maximum_score", criterion.getMaximumScore(),
                "position", criterion.getPosition());
    }

    static RubricVersionResponse toResponse(RubricVersion version) {
        var rubric = version.getRubric();
        return new RubricVersionResponse(
                version.getId(),
                rubric.getId(),
                rubric.getCode(),
                rubric.getName(),
                version.getVersionNumber(),
                version.getMinimumScore(),
                version.getMaximumScore(),
                version.getRoundingScale(),
                version.getRoundingMode(),
                version.getFormula(),
                version.getCriteria().stream().map(criterion -> new RubricCriterionResponse(
                        criterion.getId(), criterion.getCode(), criterion.getName(), criterion.getWeight(),
                        criterion.getMinimumScore(), criterion.getMaximumScore(), criterion.getPosition())).toList(),
                version.getCreatedAt());
    }
}
