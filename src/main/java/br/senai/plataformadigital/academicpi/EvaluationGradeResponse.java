package br.senai.plataformadigital.academicpi;

import java.math.BigDecimal;
import java.util.UUID;

public record EvaluationGradeResponse(UUID evaluationId, UUID rubricVersionId, BigDecimal finalScore) { }
