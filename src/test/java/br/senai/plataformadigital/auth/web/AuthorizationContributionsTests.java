package br.senai.plataformadigital.auth.web;

import br.senai.plataformadigital.auth.api.authorization.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationContributionsTests {
    @Test void missingOrAmbiguousModulePoliciesNeverGrantAccess() {
        var beans = new StaticListableBeanFactory();
        var configuration = new AuthorizationConfiguration();
        var policy = configuration.denyPolicy(beans.getBeanProvider(AuthorizationPolicyContribution.class));
        var subject = new AuthenticatedSubject("synthetic-subject");
        var request = new AuthorizationRequest("synthetic:read", "resource", Optional.empty());
        assertFalse(policy.permits(subject, request));
        AuthorizationPolicyContribution contribution = new AuthorizationPolicyContribution() {
            public boolean supports(String action) { return action.equals("synthetic:read"); }
            public boolean permits(AuthenticatedSubject actor, AuthorizationRequest target) { return true; }
        };
        beans.addBean("first", contribution);
        assertTrue(policy.permits(subject, request));
        assertFalse(policy.permits(subject, new AuthorizationRequest("other:read", "resource", Optional.empty())));
        beans.addBean("ambiguous", contribution);
        assertFalse(policy.permits(subject, request));
    }
}
