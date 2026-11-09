package plataforma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Fluxo BE-017 pela API com PostgreSQL real: emissão → validação pública → revogação. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class CertificateIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void emiteValidaERevogaCertificado() throws Exception {
        var enrollmentId = createEnrollment();
        approveActivity(createActivity(enrollmentId, 30), 25);
        approveActivity(createActivity(enrollmentId, 10), 10);
        createActivity(enrollmentId, 50); // pendente: não conta

        var issued = mockMvc.perform(post("/api/v1/certificates").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enrollmentId\": \"%s\"}".formatted(enrollmentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hours").value(35))
                .andExpect(jsonPath("$.status").value("VALID"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(issued, "$.id");
        String hash = JsonPath.read(issued, "$.validationHash");
        assertThat(hash).matches("[0-9a-f]{64}");

        mockMvc.perform(post("/api/v1/certificates").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enrollmentId\": \"%s\"}".formatted(enrollmentId)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/public/certificates/{id}", id).param("hash", hash))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.hours").doesNotExist());
        mockMvc.perform(get("/api/v1/public/certificates/{id}", id).param("hash", "f".repeat(64)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/certificates/{id}/qr-code", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));

        mockMvc.perform(post("/api/v1/certificates/{id}/revocation", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Horas lançadas por engano\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
        mockMvc.perform(post("/api/v1/certificates/{id}/revocation", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"de novo\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/public/certificates/{id}", id).param("hash", hash))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.revokedAt").exists())
                .andExpect(jsonPath("$.revocationReason").doesNotExist());

        mockMvc.perform(put("/api/v1/certificates/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/certificates/{id}", id)).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void naoEmiteSemHorasAprovadasEDetectaAdulteracaoNoBanco() throws Exception {
        var semHoras = createEnrollment();
        createActivity(semHoras, 20);
        mockMvc.perform(post("/api/v1/certificates").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enrollmentId\": \"%s\"}".formatted(semHoras)))
                .andExpect(status().isConflict());

        var enrollmentId = createEnrollment();
        approveActivity(createActivity(enrollmentId, 8), 8);
        var issued = mockMvc.perform(post("/api/v1/certificates").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enrollmentId\": \"%s\"}".formatted(enrollmentId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(issued, "$.id");
        String hash = JsonPath.read(issued, "$.validationHash");

        jdbc.update("update extension.certificate set hours = 800 where id = ?", UUID.fromString(id));

        mockMvc.perform(get("/api/v1/public/certificates/{id}", id).param("hash", hash))
                .andExpect(status().isNotFound());
    }

    private void approveActivity(String activityId, int hours) {
        // A decisão de horas é escopo da BE-014, ainda sem endpoint; aqui simulamos o deferimento.
        jdbc.update("update extension.activity set status = 'APPROVED', hours_approved = ? where id = ?",
                hours, UUID.fromString(activityId));
    }

    private String createEnrollment() throws Exception {
        var studentId = createUser("STUDENT");
        var professorId = createUser("PROFESSOR");
        var notice = createAndGetId("/api/v1/notices", """
                {"title": "Edital de extensão", "description": "Ações comunitárias", "startsOn": "2026-08-01",
                 "endsOn": "2026-12-15", "totalHours": 60, "advisorId": "%s", "active": true}
                """.formatted(professorId));
        return createAndGetId("/api/v1/enrollments", """
                {"noticeId": "%s", "studentId": "%s"}
                """.formatted(notice, studentId));
    }

    private String createActivity(String enrollmentId, int hours) throws Exception {
        return createAndGetId("/api/v1/activities", """
                {"enrollmentId": "%s", "title": "Oficina", "hoursRequested": %d, "evidenceKey": "evidencias/oficina.pdf"}
                """.formatted(enrollmentId, hours));
    }

    private String createUser(String role) throws Exception {
        return createAndGetId("/api/v1/users", """
                {"email": "%s@plataforma.test", "fullName": "Pessoa Teste", "role": "%s", "active": true}
                """.formatted(UUID.randomUUID(), role));
    }

    private String createAndGetId(String uri, String json) throws Exception {
        var body = mockMvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
