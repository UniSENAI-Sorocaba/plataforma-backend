package br.senai.plataformadigital.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import br.senai.plataformadigital.auth.api.authorization.AuthenticatedSubject;
import br.senai.plataformadigital.auth.api.authorization.AuthorizationRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ExtensionAuthorizationPolicyTest {
    private static final UUID USER = UUID.randomUUID();
    @Mock JdbcTemplate jdbc;
    private ExtensionAuthorizationPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ExtensionAuthorizationPolicy(jdbc);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class))).thenReturn(true);
    }

    @Test
    void supportsAndAllowsCoordinatorOperations() {
        AuthenticatedSubject subject = new AuthenticatedSubject(USER.toString());
        assertThat(policy.supports("CREATE_EXTENSION_OPPORTUNITY")).isTrue();
        assertThat(policy.supports("PUBLISH_EXTENSION_OPPORTUNITY")).isTrue();
        assertThat(policy.supports("CLOSE_EXTENSION_OPPORTUNITY")).isTrue();
        assertThat(policy.permits(subject, request("CREATE_EXTENSION_OPPORTUNITY", null))).isTrue();
        assertThat(policy.permits(subject, request("PUBLISH_EXTENSION_OPPORTUNITY", USER.toString()))).isTrue();
        assertThat(policy.permits(subject, request("CLOSE_EXTENSION_OPPORTUNITY", USER.toString()))).isTrue();
    }

    @Test
    void allowsStudentApplicationAndOwnWithdrawal() {
        AuthenticatedSubject subject = new AuthenticatedSubject(USER.toString());
        assertThat(policy.permits(subject, request("APPLY_EXTENSION_OPPORTUNITY", USER.toString()))).isTrue();
        assertThat(policy.permits(subject, request("LIST_OWN_EXTENSION_APPLICATIONS", null))).isTrue();
        assertThat(policy.permits(subject, request("WITHDRAW_EXTENSION_APPLICATION", USER.toString()))).isTrue();
    }

    @Test
    void allowsCoordinatorOrResponsibleToListApplications() {
        AuthenticatedSubject subject = new AuthenticatedSubject(USER.toString());
        assertThat(policy.permits(subject, request("LIST_EXTENSION_APPLICATIONS", USER.toString()))).isTrue();
        assertThat(policy.supports("UNKNOWN")).isFalse();
        assertThat(policy.permits(subject, request("UNKNOWN", USER.toString()))).isFalse();
    }

    private static AuthorizationRequest request(String action, String scope) {
        return new AuthorizationRequest(action, "opportunity", Optional.ofNullable(scope));
    }
}
