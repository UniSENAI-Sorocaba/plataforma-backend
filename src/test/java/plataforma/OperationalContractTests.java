package plataforma;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import plataforma.service.ActivityService;
import plataforma.service.AuditEventService;
import plataforma.service.CertificateService;
import plataforma.service.EnrollmentService;
import plataforma.service.EvaluationService;
import plataforma.service.ExaminationBoardService;
import plataforma.service.NoticeService;
import plataforma.service.ProjectMemberService;
import plataforma.service.ProjectService;
import plataforma.service.UserService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "platform.crud.api.enabled=true",
        "platform.audit.api.enabled=true",
        "springdoc.api-docs.enabled=false"
})
@ActiveProfiles("local")
@MockitoBean(types = {ActivityService.class,
        AuditEventService.class,
        CertificateService.class,
        EnrollmentService.class,
        EvaluationService.class,
        ExaminationBoardService.class,
        NoticeService.class,
        ProjectMemberService.class,
        ProjectService.class,
        UserService.class})
@Import(OperationalContractTests.HealthConfiguration.class)
class OperationalContractTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final MediaType ACTUATOR_JSON =
            MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json");

    @Autowired
    private TestRestTemplate client;

    @Autowired
    private AtomicBoolean dependencyAvailable;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    static Stream<JsonNode> successfulFixtures() throws IOException {
        JsonNode fixtures = JSON.readTree(Path.of("contracts/fixtures/operational.json").toFile());
        return StreamSupport.stream(fixtures.spliterator(), false)
                .filter(fixture -> fixture.path("status").asInt() == 200);
    }

    @AfterEach
    void restoreAvailability() {
        dependencyAvailable.set(true);
        AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
        AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
    }

    @ParameterizedTest
    @MethodSource("successfulFixtures")
    void producerMatchesSuccessfulFixtures(JsonNode fixture) throws IOException {
        assertProducerResponse(fixture.path("path").asText(), 200, fixture.path("body"));
    }

    @Test
    void unhealthyDependencyProducesDocumented503() throws IOException {
        dependencyAvailable.set(false);
        assertProducerResponse("/actuator/health", 503,
                JSON.readTree("{\"status\":\"DOWN\",\"groups\":[\"liveness\",\"readiness\"]}"));
    }

    @Test
    void refusingTrafficProducesDocumented503() throws IOException {
        AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
        assertProducerResponse("/actuator/health/readiness", 503,
                JSON.readTree("{\"status\":\"OUT_OF_SERVICE\"}"));
    }

    @Test
    void brokenLivenessProducesDocumented503() throws IOException {
        AvailabilityChangeEvent.publish(context, LivenessState.BROKEN);
        assertProducerResponse("/actuator/health/liveness", 503, JSON.readTree("{\"status\":\"DOWN\"}"));
    }

    @Test
    void applicationControllersCannotIntroduceUndocumentedOperations() throws IOException {
        List<JsonNode> contracts = new ArrayList<>();
        ObjectMapper parser = new ObjectMapper(new YAMLFactory());
        try (var files = Files.walk(Path.of("contracts"))) {
            for (Path source : files.filter(Files::isRegularFile).filter(path ->
                    path.getFileName().toString().matches("(?i)(openapi\\.(json|ya?ml)|.+\\.openapi\\.(json|ya?ml))")).toList()) {
                contracts.add(parser.readTree(source.toFile()));
            }
        }
        requestMappingHandlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            if (!handler.getBeanType().getPackageName().startsWith("plataforma.")) {
                return;
            }
            assertTrue(!mapping.getMethodsCondition().getMethods().isEmpty(),
                    "Controller deve declarar os métodos HTTP suportados");
            for (String path : mapping.getPatternValues()) {
                for (RequestMethod method : mapping.getMethodsCondition().getMethods()) {
                    long declarations = contracts.stream().filter(contract -> contract.path("paths").path(path)
                            .has(method.name().toLowerCase(java.util.Locale.ROOT))).count();
                    assertEquals(1, declarations, () -> "Operação sem contrato único: " + method + " " + path);
                }
            }
        });
    }

    private void assertProducerResponse(String path, int status, JsonNode expected) throws IOException {
        JsonNode contract = new ObjectMapper(new YAMLFactory())
                .readTree(Path.of("contracts/openapi.yaml").toFile());
        JsonNode responseContract = contract.path("paths").path(path).path("get")
                .path("responses").path(String.valueOf(status));
        if (responseContract.has("$ref")) {
            responseContract = contract.at(responseContract.path("$ref").asText().substring(1));
        }
        JsonNode content = responseContract.path("content").path(ACTUATOR_JSON.toString());
        assertTrue(content.has("schema"), "Status e media type devem estar no contrato");
        assertTrue(StreamSupport.stream(content.path("examples").spliterator(), false)
                .anyMatch(example -> expected.equals(example.path("value"))),
                "Fixture deve corresponder a um exemplo do contrato");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(ACTUATOR_JSON));
        ResponseEntity<String> response = client.exchange(path, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertEquals(status, response.getStatusCode().value());
        assertNotNull(response.getHeaders().getContentType());
        assertTrue(ACTUATOR_JSON.isCompatibleWith(response.getHeaders().getContentType()));
        assertEquals(expected, JSON.readTree(response.getBody()));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class HealthConfiguration {
        @Bean
        AtomicBoolean dependencyAvailable() {
            return new AtomicBoolean(true);
        }

        @Bean
        HealthIndicator syntheticDependency(AtomicBoolean dependencyAvailable) {
            return () -> dependencyAvailable.get() ? Health.up().build() : Health.down().build();
        }
    }
}
