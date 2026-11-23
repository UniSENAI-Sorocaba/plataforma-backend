package br.senai.plataformadigital.academicpi;

import java.util.UUID;

public record AcademicClassResponse(UUID id, String code, String name, String term, boolean active) { }
