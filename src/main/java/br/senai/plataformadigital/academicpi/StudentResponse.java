package br.senai.plataformadigital.academicpi;

import java.util.UUID;

public record StudentResponse(UUID id, String fullName, String email, String cpf,
                              String registrationNumber, String classCode, String className,
                              String term, String status) { }
