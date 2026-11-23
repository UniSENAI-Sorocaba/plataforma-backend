package br.senai.plataformadigital.academicpi;

import java.util.List;

public record StudentImportResponse(int total, int imported, int rejected, List<String> errors) { }
