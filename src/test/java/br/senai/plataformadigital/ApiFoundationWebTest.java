package br.senai.plataformadigital;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApiFoundationWebTest {
    @Autowired
    private TestRestTemplate client;

    @Test
    void healthcheckIsPublicAndReturnsCorrelationId() {
        ResponseEntity<String> response = client.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isNotBlank();
    }

    @Test
    void correlationIdIsPropagated() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-Id", "test-correlation-id");

        ResponseEntity<String> response = client.exchange("/actuator/health", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("test-correlation-id");
    }
}
