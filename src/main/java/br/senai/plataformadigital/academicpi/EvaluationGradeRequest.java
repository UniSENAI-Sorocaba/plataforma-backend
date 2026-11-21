package br.senai.plataformadigital.academicpi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record EvaluationGradeRequest(
        @NotNull UUID rubricVersionId,
        @NotEmpty List<@Valid EvaluationCriterionScoreRequest> scores) { }
