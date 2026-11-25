package plataforma;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import plataforma.auth.PasswordHasher;

@SpringBootTest(properties = "security.jwt.enabled=true")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class AuthenticationIntegrationTests {
    private static final String ISSUER = "https://auth.plataforma.test";
    private static final String AUDIENCE = "plataforma-api-test";
    private static final String PASSWORD = "SenhaSegura123";
    private static final KeyPair TEST_KEY_PAIR = createKeyPair();
    private static final AtomicLong CPF_SEQUENCE = new AtomicLong();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private PasswordHasher passwordHasher;

    @DynamicPropertySource
    static void configureJwtKeys(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.issuer", () -> ISSUER);
        registry.add("security.jwt.audience", () -> AUDIENCE);
        registry.add("security.jwt.public-key", () -> toPem("PUBLIC KEY", TEST_KEY_PAIR.getPublic().getEncoded()));
        registry.add("security.jwt.private-key", () -> toPem("PRIVATE KEY", TEST_KEY_PAIR.getPrivate().getEncoded()));
    }

    @Test
    void activeCpfLoginReturnsVerifiedAccessTokenAndRehashesPassword() throws Exception {
        String cpf = nextCpf();
        String outdatedHash = weakHash(PASSWORD);
        UUID userId = insertUser(cpf, outdatedHash, "active");

        var result = mockMvc.perform(loginRequest(cpf, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        String accessToken = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.accessToken"
        );
        Jwt jwt = jwtDecoder.decode(accessToken);

        org.assertj.core.api.Assertions.assertThat(jwt.getHeaders().get("alg")).isEqualTo("RS256");
        org.assertj.core.api.Assertions.assertThat(jwt.getIssuer().toString()).isEqualTo(ISSUER);
        org.assertj.core.api.Assertions.assertThat(jwt.getAudience()).containsExactly(AUDIENCE);
        org.assertj.core.api.Assertions.assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        org.assertj.core.api.Assertions.assertThat(jwt.getClaimAsString("role")).isEqualTo("STUDENT");
        org.assertj.core.api.Assertions.assertThat(jwt.getClaimAsString("token_type")).isEqualTo("access");

        String upgradedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM auth.\"user\" WHERE id = ?",
                String.class,
                userId
        );
        org.assertj.core.api.Assertions.assertThat(upgradedHash).isNotEqualTo(outdatedHash);
        org.assertj.core.api.Assertions.assertThat(passwordHasher.verify(PASSWORD, upgradedHash)).isTrue();

        mockMvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownInactiveAndIncorrectCredentialsReturnTheSameGenericProblem() throws Exception {
        String inactiveCpf = nextCpf();
        insertUser(inactiveCpf, weakHash(PASSWORD), "inactive");
        String pendingCpf = nextCpf();
        insertUser(pendingCpf, null, "pending");
        String activeCpf = nextCpf();
        insertUser(activeCpf, weakHash(PASSWORD), "active");
        String malformedHashCpf = nextCpf();
        insertUser(malformedHashCpf, "not-an-argon2-hash", "active");

        var unknown = mockMvc.perform(loginRequest(nextCpf(), PASSWORD)
                        .header("X-Correlation-Id", "unknown-login")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.detail").value("Authentication is required"))
                .andExpect(jsonPath("$.correlation_id").value("unknown-login"))
                .andExpect(jsonPath("$.detail", not(org.hamcrest.Matchers.containsString(PASSWORD))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("X-Correlation-Id", "unknown-login"))
                .andReturn();

        var inactive = mockMvc.perform(loginRequest(inactiveCpf, PASSWORD)
                        .header("X-Correlation-Id", "same-correlation")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.detail").value("Authentication is required"))
                .andReturn();

        var noPassword = mockMvc.perform(loginRequest(pendingCpf, PASSWORD)
                        .header("X-Correlation-Id", "same-correlation")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.detail").value("Authentication is required"))
                .andReturn();

        var incorrectPassword = mockMvc.perform(loginRequest(activeCpf, "senha-errada")
                        .header("X-Correlation-Id", "same-correlation")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.detail").value("Authentication is required"))
                .andReturn();

        var malformedHash = mockMvc.perform(loginRequest(malformedHashCpf, PASSWORD)
                        .header("X-Correlation-Id", "same-correlation")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.detail").value("Authentication is required"))
                .andReturn();

        org.assertj.core.api.Assertions.assertThat(unknown.getResponse().getContentAsString())
                .doesNotContain(PASSWORD, inactiveCpf);
        org.assertj.core.api.Assertions.assertThat(inactive.getResponse().getContentAsString())
                .isEqualTo(incorrectPassword.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(noPassword.getResponse().getContentAsString())
                .isEqualTo(incorrectPassword.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(malformedHash.getResponse().getContentAsString())
                .isEqualTo(incorrectPassword.getResponse().getContentAsString());
    }

    @Test
    void rejectsCpfOutsideElevenDigits() throws Exception {
        mockMvc.perform(loginRequest("12345", PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rateLimitsRepeatedFailuresByCpf() throws Exception {
        String cpf = nextCpf();
        insertUser(cpf, weakHash(PASSWORD), "active");

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(loginRequest(cpf, "senha-errada"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(loginRequest(cpf, "senha-errada"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.detail").value("Authentication is required"))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void rateLimitsRepeatedFailuresByIp() throws Exception {
        String remoteAddress = "203.0.113.100";

        for (int attempt = 0; attempt < 5; attempt++) {
            String cpf = nextCpf();
            insertUser(cpf, weakHash(PASSWORD), "active");
            mockMvc.perform(loginRequest(cpf, "senha-errada", remoteAddress))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(loginRequest(nextCpf(), PASSWORD, remoteAddress))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.detail").value("Authentication is required"));
    }

    private UUID insertUser(String cpf, String passwordHash, String status) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO auth."user" (id, email, full_name, cpf, password_hash, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                id,
                id + "@plataforma.test",
                "Usuário de teste",
                cpf,
                passwordHash,
                status
        );
        jdbcTemplate.update(
                """
                INSERT INTO user_roles (user_id, role_id, scope_type, scope_id)
                SELECT ?, id, 'GLOBAL', '*' FROM roles WHERE name = 'ALUNO'
                """,
                id
        );
        return id;
    }

    private String weakHash(String password) {
        var encoder = new org.springframework.security.crypto.argon2.Argon2PasswordEncoder(
                16, 32, 1, 8_192, 1
        );
        return encoder.encode(password);
    }

    private static String loginJson(String cpf, String password) {
        return """
                {"cpf":"%s","password":"%s"}
                """.formatted(cpf, password);
    }

    private static MockHttpServletRequestBuilder loginRequest(String cpf, String password) {
        long sequence = CPF_SEQUENCE.incrementAndGet();
        String remoteAddress = "198.51.100." + (sequence % 200 + 1);
        return loginRequest(cpf, password, remoteAddress);
    }

    private static MockHttpServletRequestBuilder loginRequest(String cpf, String password, String remoteAddress) {
        return post("/api/v1/auth/login")
                .with(request -> {
                    request.setRemoteAddr(remoteAddress);
                    return request;
                })
                .contentType("application/json")
                .content(loginJson(cpf, password));
    }

    private static String nextCpf() {
        return "%011d".formatted(CPF_SEQUENCE.incrementAndGet());
    }

    private static KeyPair createKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static String toPem(String keyType, byte[] encodedKey) {
        return "-----BEGIN " + keyType + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encodedKey)
                + "\n-----END " + keyType + "-----";
    }
}
