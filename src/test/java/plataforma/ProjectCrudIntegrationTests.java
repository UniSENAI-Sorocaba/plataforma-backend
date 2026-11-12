package plataforma;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import plataforma.dto.AuditEventFilter;
import plataforma.model.AuditOperation;
import plataforma.service.AuditEventService;

/** Exemplo de teste ponta a ponta do CRUD: requisição HTTP → controller → service → banco. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ProjectCrudIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

        @Autowired
        private AuditEventService auditEventService;

    @Test
    void criaConsultaAtualizaEExcluiProjeto() throws Exception {
        var leaderId = createUser("lider-" + UUID.randomUUID() + "@plataforma.test");
        var advisorId = createUser("docente-" + UUID.randomUUID() + "@plataforma.test", "PROFESSOR");

        var created = mockMvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson("Sistema IoT", leaderId, advisorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROPOSED"))
                .andExpect(jsonPath("$.leaderId").value(leaderId))
                .andExpect(jsonPath("$.advisorId").value(advisorId))
                .andReturn();
        String projectId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        var creationEvents = auditEventService.search(
                new AuditEventFilter("academic_pi", "project", AuditOperation.INSERT,
                        UUID.fromString(projectId), null, null, null),
                PageRequest.of(0, 10));
        assertThat(creationEvents.content()).hasSize(1);
        assertThat(creationEvents.content().getFirst().newData()).containsEntry("status", "PROPOSED");

        mockMvc.perform(put("/api/v1/projects/{id}", projectId).contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson("Sistema IoT", leaderId, advisorId, "APPROVED")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
        mockMvc.perform(get("/api/v1/projects/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROPOSED"));

        mockMvc.perform(put("/api/v1/projects/{id}", projectId).contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson("Sistema IoT v2", leaderId, advisorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sistema IoT v2"));
        var updateEvents = auditEventService.search(
                new AuditEventFilter("academic_pi", "project", AuditOperation.UPDATE,
                        UUID.fromString(projectId), null, null, null),
                PageRequest.of(0, 10));
        assertThat(updateEvents.content()).hasSize(1);

        mockMvc.perform(get("/api/v1/projects/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sistema IoT v2"));

        mockMvc.perform(delete("/api/v1/projects/{id}", projectId)).andExpect(status().isNoContent());
        var deleteEvents = auditEventService.search(
                new AuditEventFilter("academic_pi", "project", AuditOperation.DELETE,
                        UUID.fromString(projectId), null, null, null),
                PageRequest.of(0, 10));
        assertThat(deleteEvents.content()).hasSize(1);
        mockMvc.perform(get("/api/v1/projects/{id}", projectId)).andExpect(status().isNotFound());
    }

    @Test
    void rejeitaCamposInvalidosELiderInexistente() throws Exception {
        mockMvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists());

        mockMvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson("Projeto", UUID.randomUUID().toString(), UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejeitaProjetoSemDocenteResponsavel() throws Exception {
        var leaderId = createUser("sem-docente-" + UUID.randomUUID() + "@plataforma.test");
        var request = """
                {"title":"Projeto","description":"Descrição","projectType":"ACADEMIC_PI",
                 "status":"PROPOSED","semester":"2026.2","course":"Engenharia",
                 "leaderId":"%s"}
                """.formatted(leaderId);

        mockMvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.advisorId").exists());
    }

    @Test
    void rejeitaPapeisIncompativeisNaProposta() throws Exception {
        var teacherId = createUser("docente-lider-" + UUID.randomUUID() + "@plataforma.test", "PROFESSOR");
        var studentId = createUser("aluno-responsavel-" + UUID.randomUUID() + "@plataforma.test");

        mockMvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson("Projeto", teacherId, teacherId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));

        mockMvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson("Projeto", studentId, studentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    @Test
    void rejeitaEmailDuplicado() throws Exception {
        var email = "duplicado-" + UUID.randomUUID() + "@plataforma.test";
        createUser(email);
                mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(userJson(email, "STUDENT")))
                .andExpect(status().isConflict());
    }

    private String createUser(String email) throws Exception {
                return createUser(email, "STUDENT");
        }

        private String createUser(String email, String role) throws Exception {
        var result = mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                                                .content(userJson(email, role)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

        private static String userJson(String email, String role) {
        return """
                                {"email": "%s", "fullName": "Usuário Teste", "role": "%s", "active": true}
                                """.formatted(email, role);
    }

        private static String projectJson(String title, String leaderId, String advisorId) {
                return projectJson(title, leaderId, advisorId, "PROPOSED");
        }

        private static String projectJson(String title, String leaderId, String advisorId, String status) {
        return """
                {"title": "%s", "description": "Monitoramento de sensores", "projectType": "ACADEMIC_PI",
                                 "status": "%s", "semester": "2026.2", "course": "Engenharia de Software",
                                 "leaderId": "%s", "advisorId": "%s"}
                                """.formatted(title, status, leaderId, advisorId);
    }
}
