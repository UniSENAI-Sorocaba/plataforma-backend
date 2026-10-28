package br.senai.plataformadigital;

import br.senai.plataformadigital.auth.api.authorization.AuthorizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PlataformaDigitalApplicationTests {
    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
    }

    @Test
    void authorizationPreparationDoesNotActivatePoliciesOrHarnessRoutes() throws IOException {
        assertTrue(context.getBeansOfType(AuthorizationService.class).size() == 1);
        TreeSet<String> inventory = new TreeSet<>();
        context.getBeansOfType(AbstractHandlerMethodMapping.class).values().forEach(handlerMapping ->
                handlerMapping.getHandlerMethods().keySet().forEach(mapping -> {
                    if (mapping instanceof RequestMappingInfo info) {
                        info.getPatternValues().forEach(path -> {
                            assertTrue(!path.startsWith("/__test/"));
                            if (info.getMethodsCondition().getMethods().isEmpty()) {
                                inventory.add("ANY " + path);
                            } else {
                                info.getMethodsCondition().getMethods().forEach(method -> inventory.add(method + " " + path));
                            }
                        });
                    }
                }));
        Path output = Path.of("target/authorization/route-inventory.json");
        Files.createDirectories(output.getParent());
        context.getBean(ObjectMapper.class).writeValue(output.toFile(), inventory);
        context.getBean(ObjectMapper.class).writeValue(Path.of("target/authorization/route-matrix.json").toFile(),
                context.getBean(br.senai.plataformadigital.auth.web.AuthorizationRoutes.class).definitions());
    }
}
