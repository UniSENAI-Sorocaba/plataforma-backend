package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.exception.ConflictException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "spring.datasource.url")
public class RubricService {
    private static final BigDecimal ONE = BigDecimal.ONE;
    private final RubricRepository repository;

    public RubricService(RubricRepository repository) { this.repository = repository; }

    @Transactional
    public RubricVersionResponse create(RubricCreateRequest request, UUID actor) {
        if (!repository.isCoordinator(actor)) throw new ConflictException("A criação de rubrica exige coordenação.");
        validate(request.criteria());
        try { return repository.create(request, actor); }
        catch (DataIntegrityViolationException ex) { throw new ConflictException("Código de rubrica já existe."); }
    }

    public RubricVersionResponse get(UUID rubricId, int version) {
        return repository.find(rubricId, version);
    }

    private void validate(List<RubricCriterionRequest> criteria) {
        if (criteria.stream().map(RubricCriterionRequest::code).map(String::trim).map(String::toLowerCase).distinct().count() != criteria.size())
            throw new ConflictException("Critérios duplicados.");
        BigDecimal total = criteria.stream().map(RubricCriterionRequest::weight).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.subtract(ONE).abs().compareTo(new BigDecimal("0.0001")) > 0)
            throw new ConflictException("A soma dos pesos deve ser 1.0.");
        criteria.forEach(c -> {
            if (c.minScore().compareTo(c.maxScore()) >= 0 || c.maxScore().compareTo(new BigDecimal("10")) > 0)
                throw new ConflictException("Faixa de nota inválida para o critério " + c.code() + ".");
        });
    }

    static BigDecimal calculate(List<RubricCriterionResponse> criteria, List<EvaluationCriterionScoreRequest> scores) {
        if (scores.stream().map(EvaluationCriterionScoreRequest::criterionCode).map(String::trim).distinct().count() != scores.size())
            throw new ConflictException("Notas de critérios duplicadas.");
        if (scores.size() != criteria.size()) throw new ConflictException("É necessário informar todos os critérios.");
        var byCode = scores.stream().collect(java.util.stream.Collectors.toMap(s -> s.criterionCode().trim(), s -> s));
        BigDecimal total = BigDecimal.ZERO;
        for (RubricCriterionResponse criterion : criteria) {
            var score = byCode.get(criterion.code());
            if (score == null || score.score().compareTo(criterion.minScore()) < 0 || score.score().compareTo(criterion.maxScore()) > 0)
                throw new ConflictException("Nota inválida para o critério " + criterion.code() + ".");
            total = total.add(score.score().multiply(criterion.weight()));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
