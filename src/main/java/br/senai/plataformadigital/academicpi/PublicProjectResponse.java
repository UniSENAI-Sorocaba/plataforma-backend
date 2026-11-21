package br.senai.plataformadigital.academicpi;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicProjectResponse(UUID id, String title, String summary, String description,
        ProjectClassification classification, ProjectContinuity continuity, String courseCode,
        List<String> semesters, List<String> odsCodes, Instant updatedAt) { }
