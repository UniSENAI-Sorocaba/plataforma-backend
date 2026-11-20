package br.senai.plataformadigital.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String code, String message,
        String path, String correlationId, List<String> details) {
}
