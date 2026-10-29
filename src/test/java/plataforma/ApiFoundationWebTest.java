package plataforma;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import plataforma.config.CorrelationIdFilter;
import plataforma.exception.ApiExceptionHandler;

@WebMvcTest(properties = {"spring.mvc.throw-exception-if-no-handler-found=true", "spring.web.resources.add-mappings=false"})
@Import({ApiExceptionHandler.class, CorrelationIdFilter.class, ApiFoundationWebTest.TestController.class})
class ApiFoundationWebTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void addsValidatedCorrelationToResponses() throws Exception {
        mockMvc.perform(get("/test/ping").header(CorrelationIdFilter.HEADER, "request-42"))
                .andExpect(status().isOk()).andExpect(header().string(CorrelationIdFilter.HEADER, "request-42"));
    }

    @Test
    void preservesFrameworkStatusesInTheProblemFormat() throws Exception {
        mockMvc.perform(get("/not-found")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mockMvc.perform(put("/test/ping")).andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        mockMvc.perform(post("/test/body").contentType("text/plain").content("text"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/ping") ResponseEntity<Void> ping() { return ResponseEntity.ok().build(); }
        @PostMapping(path = "/test/body", consumes = "application/json") ResponseEntity<Void> body(@RequestBody Payload payload) { return ResponseEntity.noContent().build(); }
    }
    record Payload(String name) { }
}
