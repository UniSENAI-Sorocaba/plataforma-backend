package br.senai.plataformadigital;

import static org.assertj.core.api.Assertions.assertThat;

import br.senai.plataformadigital.dto.AuditEventFilter;
import br.senai.plataformadigital.dto.RecordAuditEventRequest;
import br.senai.plataformadigital.model.AuditOperation;
import br.senai.plataformadigital.service.AuditEventService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class AuditEventIntegrationTests {

    @Autowired
    private AuditEventService service;

    @Test
    void persisteEConsultaEventoComJsonb() {
        var recordId = UUID.randomUUID();
        var created = service.record(new RecordAuditEventRequest(
                "academic_pi", "proposal", AuditOperation.UPDATE, recordId, UUID.randomUUID(),
                Map.of("status", "DRAFT"), Map.of("status", "SUBMITTED")));

        var found = service.findById(created.id());
        assertThat(found.newData()).containsEntry("status", "SUBMITTED");
        assertThat(found.oldData()).containsEntry("status", "DRAFT");

        var page = service.search(
                new AuditEventFilter(null, null, null, recordId, null, null, null), PageRequest.of(0, 10));
        assertThat(page.content()).extracting("id").containsExactly(created.id());
    }
}
