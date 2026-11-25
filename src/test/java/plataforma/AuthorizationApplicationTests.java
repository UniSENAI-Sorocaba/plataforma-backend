package plataforma;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
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
class AuthorizationApplicationTests {
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
