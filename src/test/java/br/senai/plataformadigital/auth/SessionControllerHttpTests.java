package br.senai.plataformadigital.auth;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SessionControllerHttpTests {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private RefreshTokenService refreshTokens;
    private JwtTokenService accessTokens;
    private UserAccountRepository users;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        refreshTokens = new RefreshTokenService(new InMemoryRefreshTokenRepository(),
                java.time.Clock.systemUTC(), java.time.Duration.ofDays(30));
        accessTokens = org.mockito.Mockito.mock(JwtTokenService.class);
        users = org.mockito.Mockito.mock(UserAccountRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new SessionController(refreshTokens, accessTokens, users)).build();
    }

    @Test
    void refreshRotatesAndReturnsBothCredentials() throws Exception {
        IssuedRefreshToken original = refreshTokens.issue(USER_ID);
        IssuedRefreshToken rotated = new IssuedRefreshToken("refresh-new", USER_ID, UUID.randomUUID(),
                UUID.randomUUID(), Instant.parse("2026-02-01T00:00:00Z"));
        AuthenticatedUser user = new AuthenticatedUser(USER_ID, "student@example.org", "hash", "ALUNO", true);
        when(users.findById(USER_ID)).thenReturn(java.util.Optional.of(user));
        when(accessTokens.issue(user)).thenReturn(new LoginResponse("access-new", "Bearer", 900));

        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + original.rawToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-new"))
                .andExpect(jsonPath("$.refreshToken").isString());
    }

    @Test
    void invalidRefreshReturnsUnauthorizedWithoutCredentialDetails() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh-old\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Refresh token inválido ou expirado."));
    }

    @Test
    void logoutIsNoContentAndDelegatesTokenRevocation() throws Exception {
        IssuedRefreshToken issued = refreshTokens.issue(USER_ID);
        mvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + issued.rawToken() + "\"}"))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> refreshTokens.rotate(issued.rawToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
