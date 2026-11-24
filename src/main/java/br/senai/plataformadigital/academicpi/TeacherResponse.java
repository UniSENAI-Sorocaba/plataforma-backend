package br.senai.plataformadigital.academicpi;

import java.util.UUID;

public record TeacherResponse(UUID id, String fullName, String email, String status) { }
