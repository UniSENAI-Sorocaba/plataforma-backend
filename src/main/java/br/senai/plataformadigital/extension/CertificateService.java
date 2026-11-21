package br.senai.plataformadigital.extension;

import br.senai.plataformadigital.exception.ConflictException;
import br.senai.plataformadigital.exception.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "spring.datasource.url")
public class CertificateService {
    private final CertificateRepository repository;
    public CertificateService(CertificateRepository repository) { this.repository = repository; }
    @Transactional
    public CertificateResponse issue(UUID project, UUID period, CertificateRequest request, UUID actor) {
        if (!repository.canIssue(project, period, request.studentId())) throw new ConflictException("O aluno não possui atividade aprovada para o certificado.");
        String code = UUID.randomUUID().toString().replace("-", "");
        String canonical = project+"|"+period+"|"+request.studentId()+"|"+code;
        try { return repository.insert(project, period, request.studentId(), actor, code, HexFormat.of().formatHex(hash(canonical))); }
        catch (DataIntegrityViolationException ex) { throw new ConflictException("Já existe certificado para este participante."); }
    }
    public CertificateResponse validate(String code) { try { return repository.find(code); } catch (EmptyResultDataAccessException ex) { throw new ResourceNotFoundException("Certificado não encontrado."); } }
    @Transactional public CertificateResponse revoke(String code, String reason) { if (repository.revoke(code, reason) != 1) throw new ConflictException("Certificado inexistente, já revogado ou motivo inválido."); return validate(code); }
    private byte[] hash(String value) { try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); } catch (Exception ex) { throw new IllegalStateException("SHA-256 indisponível", ex); } }
}
