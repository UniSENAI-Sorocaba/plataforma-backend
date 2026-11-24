package br.senai.plataformadigital.academicpi;

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
class ProjectAuthorizationPolicyTest {
    @Mock JdbcTemplate jdbc;
    private ProjectAuthorizationPolicy policy;
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        policy = new ProjectAuthorizationPolicy(jdbc);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class))).thenReturn(true);
    }

    @Test
    void allowsCoordinationManagementAndEvaluationViews() {
        var subject = new AuthenticatedSubject(user.toString());
        for (String action : new String[]{"CREATE_PROJECT", "LIST_COORDINATION_PROJECTS", "UPDATE_COORDINATION_PROJECT",
                "DELETE_COORDINATION_PROJECT", "LIST_COORDINATION_EVALUATIONS", "MANAGE_PROJECT",
                "DECIDE_PARTICIPATION", "CHANGE_PROJECT_LIFECYCLE", "LIST_PROJECT_PERIODS"}) {
            assertThat(policy.supports(action)).isTrue();
            assertThat(policy.permits(subject, request(action, UUID.randomUUID().toString()))).isTrue();
        }
    }

    private static AuthorizationRequest request(String action, String scope) {
        return new AuthorizationRequest(action, "project", Optional.ofNullable(scope));
    }
}
