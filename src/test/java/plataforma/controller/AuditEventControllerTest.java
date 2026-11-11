package plataforma.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import plataforma.dto.AuditEventResponse;
import plataforma.model.AuditOperation;
import plataforma.service.AuditEventService;
import plataforma.dto.PageResponse;
import plataforma.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuditEventController.class, properties = "platform.audit.api.enabled=true")
class AuditEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditEventService service;

    @Test
    void listaEventosPaginados() throws Exception {
        var event = new AuditEventResponse(UUID.randomUUID(), Instant.parse("2026-10-08T12:00:00Z"),
                "auth", "user_account", AuditOperation.INSERT, UUID.randomUUID(), null, null, null);
        when(service.search(any(), any())).thenReturn(new PageResponse<>(List.of(event), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/audit-events").param("operation", "INSERT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tableName").value("user_account"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void retornaProblemDetailQuandoNaoEncontrado() throws Exception {
        var id = UUID.randomUUID();
        when(service.findById(id)).thenThrow(new ResourceNotFoundException("Evento de auditoria não encontrado: " + id));

        mockMvc.perform(get("/api/v1/audit-events/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }
}
