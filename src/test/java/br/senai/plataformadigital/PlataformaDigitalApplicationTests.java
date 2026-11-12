package br.senai.plataformadigital;

import br.senai.plataformadigital.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.GeneralSecurityException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PlataformaDigitalApplicationTests {
    private static final KeyPair TEST_KEY_PAIR = createKeyPair();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @DynamicPropertySource
    static void configureJwtKeys(DynamicPropertyRegistry registry) {
        registry.add("security.jwt.public-key", () -> toPem("PUBLIC KEY", TEST_KEY_PAIR.getPublic().getEncoded()));
        registry.add("security.jwt.private-key", () -> toPem("PRIVATE KEY", TEST_KEY_PAIR.getPrivate().getEncoded()));
    }

    @BeforeEach
    void resetRepository() {
        when(userRepository.findByEmail("inexistente@senai.br")).thenReturn(java.util.Optional.empty());
    }

    @Test
    void contextLoads() {
    }

    @Test
    void invalidCredentialsReturnUnauthenticatedProblem() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "test-correlation")
                        .content("""
                                {"email":"inexistente@senai.br","password":"nao-registrada"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.detail").value("Credenciais inválidas"))
                .andReturn();
        assertFalse(result.getResponse().getContentAsString().contains("nao-registrada"));
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
