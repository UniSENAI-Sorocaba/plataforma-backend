package br.senai.plataformadigital.academicpi;

import java.util.List;
import java.util.UUID;

public record RubricVersionResponse(UUID id, UUID rubricId, String code, String name, int version,
                                    List<RubricCriterionResponse> criteria) { }
