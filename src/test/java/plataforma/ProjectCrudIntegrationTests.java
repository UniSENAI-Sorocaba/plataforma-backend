package plataforma;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Exemplo de teste ponta a ponta do CRUD: requisição HTTP → controller → service → banco. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ProjectCrudIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void criaConsultaAtualizaEExcluiProjeto() throws Exception {
        var leaderId = createUser("lider-" + UUID.randomUUID() + "@plataforma.test");

        var created = mockMvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson("Sistema IoT", leaderId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROPOSED"))
                .andExpect(jsonPath("$.leaderId").value(leaderId))
                .andReturn();
        String projectId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/v1/projects/{id}", projectId).contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson("Sistema IoT v2", leaderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sistema IoT v2"));

        mockMvc.perform(get("/api/v1/projects/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sistema IoT v2"));

        mockMvc.perform(delete("/api/v1/projects/{id}", projectId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/projects/{id}", projectId)).andExpect(status().isNotFound());
    }

    @Test
    void rejeitaCamposInvalidosELiderInexistente() throws Exception {
        mockMvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists());

        mockMvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson("Projeto", UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejeitaEmailDuplicado() throws Exception {
        var email = "duplicado-" + UUID.randomUUID() + "@plataforma.test";
        createUser(email);
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(userJson(email)))
                .andExpect(status().isConflict());
    }

    private String createUser(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private static String userJson(String email) {
        return """
                {"email": "%s", "fullName": "Aluno Teste", "role": "STUDENT", "active": true}
                """.formatted(email);
    }

    private static String projectJson(String title, String leaderId) {
        return """
                {"title": "%s", "description": "Monitoramento de sensores", "projectType": "ACADEMIC_PI",
                 "status": "PROPOSED", "semester": "2026.2", "course": "Engenharia de Software",
                 "leaderId": "%s"}
                """.formatted(title, leaderId);
    }
}
