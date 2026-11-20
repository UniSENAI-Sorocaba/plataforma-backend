package br.senai.plataformadigital.academicpi;

import br.senai.plataformadigital.exception.ConflictException;
import br.senai.plataformadigital.exception.ResourceNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "spring.datasource.url")
public class DeliverableService {
    private static final long MAX_BYTES = 25L * 1024 * 1024;
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);
    private final DeliverableRepository repository;
    private final DeliverableStorage storage;
    private final Clock clock;

    public DeliverableService(DeliverableRepository repository, DeliverableStorage storage, Clock clock) {
        this.repository = repository;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional
    public DeliverableResponse initiate(UUID projectId, UUID periodId, DeliverableInitiationRequest request, UUID actor) {
        validateMetadata(request);
        if (!repository.isActiveParticipant(projectId, periodId, actor)) {
            throw new ConflictException("Somente participante ativo pode enviar entregáveis.");
        }
        String rawToken = token();
        Instant expiresAt = clock.instant().plus(TOKEN_LIFETIME);
        UUID id = repository.insert(projectId, periodId, actor, request, "deliverables/" + UUID.randomUUID(), hash(rawToken), expiresAt);
        return withUrls(repository.find(id), rawToken, null);
    }

    @Transactional
    public DeliverableResponse upload(UUID id, String rawToken, String contentType, byte[] content) {
        DeliverableRepository.DeliverableRecord deliverable = findByToken(id, rawToken, "upload");
        if (deliverable.status() != DeliverableStatus.PENDING_UPLOAD || clock.instant().isAfter(deliverable.uploadExpiresAt())) {
            throw new ConflictException("A URL de upload expirou ou já foi utilizada.");
        }
        if (content == null || content.length != deliverable.sizeBytes()) {
            throw new ConflictException("O tamanho do arquivo não corresponde ao informado.");
        }
        if (!contentType.equalsIgnoreCase(deliverable.contentType())) {
            throw new ConflictException("O tipo MIME enviado não corresponde ao informado.");
        }
        String sha256 = sha256(content);
        String downloadToken = token();
        Instant downloadExpiresAt = clock.instant().plus(Duration.ofHours(1));
        try {
            storage.write(deliverable.storageKey(), content);
            repository.markAvailable(id, sha256, hash(downloadToken), downloadExpiresAt);
            return withUrls(repository.find(id), null, downloadToken);
        } catch (IOException exception) {
            try { storage.delete(deliverable.storageKey()); } catch (IOException ignored) { }
            throw new ConflictException("Não foi possível armazenar o arquivo.");
        }
    }

    public DownloadedDeliverable download(UUID id, String rawToken) {
        DeliverableRepository.DeliverableRecord deliverable = findByToken(id, rawToken, "download");
        if (deliverable.status() != DeliverableStatus.AVAILABLE || deliverable.downloadExpiresAt() == null
                || clock.instant().isAfter(deliverable.downloadExpiresAt())) {
            throw new ConflictException("A URL de download expirou.");
        }
        try {
            return new DownloadedDeliverable(deliverable.fileName(), deliverable.contentType(), storage.read(deliverable.storageKey()));
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Arquivo não encontrado.");
        }
    }

    private DeliverableRepository.DeliverableRecord findByToken(UUID id, String rawToken, String kind) {
        if (rawToken == null || rawToken.isBlank()) throw new ConflictException("Token de arquivo inválido.");
        try { return repository.findForToken(id, hash(rawToken), kind); }
        catch (EmptyResultDataAccessException missing) { throw new ResourceNotFoundException("Entregável não encontrado."); }
    }

    private void validateMetadata(DeliverableInitiationRequest request) {
        if (request.sizeBytes() > MAX_BYTES) throw new ConflictException("O arquivo excede o limite de 25 MB.");
        String name = request.fileName().toLowerCase(Locale.ROOT);
        String type = request.contentType().toLowerCase(Locale.ROOT);
        boolean allowed = (name.endsWith(".pdf") && type.equals("application/pdf"))
                || (name.endsWith(".png") && type.equals("image/png"))
                || ((name.endsWith(".jpg") || name.endsWith(".jpeg")) && type.equals("image/jpeg"))
                || (name.endsWith(".txt") && type.equals("text/plain"))
                || (name.endsWith(".csv") && type.equals("text/csv"))
                || (name.endsWith(".zip") && type.equals("application/zip"))
                || (name.endsWith(".docx") && type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                || (name.endsWith(".xlsx") && type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        if (!allowed) throw new ConflictException("Extensão e tipo MIME não permitidos.");
    }

    private DeliverableResponse withUrls(DeliverableResponse response, String uploadToken, String downloadToken) {
        String uploadUrl = uploadToken == null ? null : "/api/v1/deliverables/" + response.id() + "/content?token=" + uploadToken;
        String downloadUrl = downloadToken == null ? null : "/api/v1/deliverables/" + response.id() + "/download?token=" + downloadToken;
        return new DeliverableResponse(response.id(), response.projectId(), response.periodId(), response.ownerId(), response.fileName(),
                response.contentType(), response.sizeBytes(), response.sha256(), response.version(), response.status(), response.createdAt(),
                response.uploadedAt(), response.uploadExpiresAt(), uploadUrl, downloadUrl);
    }

    private static String token() { return Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)); }
    private static String hash(String value) { return sha256(value.getBytes(StandardCharsets.UTF_8)); }
    private static String sha256(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder result = new StringBuilder(64);
            for (byte b : digest) result.append(String.format("%02x", b));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 indisponível.", exception); }
    }

    public record DownloadedDeliverable(String fileName, String contentType, byte[] content) { }
}
