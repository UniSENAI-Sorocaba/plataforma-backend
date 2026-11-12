package plataforma.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import plataforma.dto.CertificateValidationResponse;
import plataforma.exception.ResourceNotFoundException;
import plataforma.model.CertificateStatus;
import plataforma.service.CertificateService;

class PublicCertificateControllerTest {

    private static final String HASH = "a".repeat(64);

    @Nested
    @WebMvcTest(controllers = PublicCertificateController.class)
    class PadraoDesligado {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private CertificateService service;

        @Test
        void rotaNaoExisteSemAPropriedade() throws Exception {
            mockMvc.perform(get("/api/v1/public/certificates/{id}", UUID.randomUUID()).param("hash", HASH))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @WebMvcTest(controllers = PublicCertificateController.class,
            properties = "platform.certificate.validation.enabled=true")
    class Ligado {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private CertificateService service;

        @Test
        void respondeSomenteSituacaoEDatasSemCache() throws Exception {
            var id = UUID.randomUUID();
            when(service.validate(id, HASH)).thenReturn(new CertificateValidationResponse(
                    id, CertificateStatus.REVOKED, Instant.parse("2026-11-01T12:00:00Z"),
                    Instant.parse("2026-11-05T12:00:00Z")));

            mockMvc.perform(get("/api/v1/public/certificates/{id}", id).param("hash", HASH))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.status").value("REVOKED"))
                    .andExpect(jsonPath("$.issuedAt").value("2026-11-01T12:00:00Z"))
                    .andExpect(jsonPath("$.hours").doesNotExist())
                    .andExpect(jsonPath("$.enrollmentId").doesNotExist())
                    .andExpect(jsonPath("$.revocationReason").doesNotExist())
                    .andExpect(jsonPath("$.validationHash").doesNotExist());
        }

        @Test
        void hashObrigatorio() throws Exception {
            mockMvc.perform(get("/api/v1/public/certificates/{id}", UUID.randomUUID()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void naoVerificadoRetorna404Generico() throws Exception {
            var id = UUID.randomUUID();
            when(service.validate(eq(id), anyString())).thenThrow(new ResourceNotFoundException("Certificado não verificado"));

            mockMvc.perform(get("/api/v1/public/certificates/{id}", id).param("hash", HASH))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }
}
