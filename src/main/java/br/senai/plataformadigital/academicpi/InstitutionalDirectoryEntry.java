package br.senai.plataformadigital.academicpi;

import java.util.UUID;

public record InstitutionalDirectoryEntry(UUID id, String fullName, String email, String role) { }
