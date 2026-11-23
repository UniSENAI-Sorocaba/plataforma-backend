package br.senai.plataformadigital.academicpi;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CoordinationEvaluationResponse(UUID id, String projectTitle, String academicTerm,
                                             String studentName, Instant evaluationDate,
                                             String evaluatorNames, BigDecimal finalScore) { }
