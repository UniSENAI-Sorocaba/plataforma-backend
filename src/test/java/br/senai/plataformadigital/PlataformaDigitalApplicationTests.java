package br.senai.plataformadigital;

import br.senai.plataformadigital.portal.api.impact.PublicImpactQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PlataformaDigitalApplicationTests {
    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
    }

    @Test
    void impactPreparationDoesNotActivatePublicQueriesAutomatically() {
        assertTrue(context.getBeansOfType(PublicImpactQueryService.class).isEmpty());
    }
}
