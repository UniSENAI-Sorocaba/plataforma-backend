package br.senai.plataformadigital.extension;

import java.math.BigDecimal;
import java.util.UUID;

public record HoursTotalResponse(UUID projectId, UUID periodId, UUID studentId, BigDecimal approvedHours) { }
