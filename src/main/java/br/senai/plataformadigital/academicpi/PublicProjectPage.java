package br.senai.plataformadigital.academicpi;

import java.util.List;

public record PublicProjectPage(List<PublicProjectResponse> items, int page, int pageSize, long totalItems,
        int totalPages) { }
