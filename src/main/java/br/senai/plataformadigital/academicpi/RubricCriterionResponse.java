package br.senai.plataformadigital.academicpi;

import java.math.BigDecimal;
import java.util.UUID;

public record RubricCriterionResponse(UUID id, String code, String label, BigDecimal weight,
                                      BigDecimal minScore, BigDecimal maxScore) { }
