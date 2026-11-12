package plataforma;

import org.junit.jupiter.api.Test;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import br.senai.plataformadigital.portal.impact.ImpactRepository;
import br.senai.plataformadigital.portal.impact.ImpactFailure;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = PlataformaDigitalApplication.class,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
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
@AutoConfigureMockMvc
class ImpactApplicationTests {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ImpactRepository repository;

    @Test
    void contextLoads() {
    }

    @Test
    void productionDefaultsNeverUseTemporaryStorageOrUnauthenticatedWrites() throws Exception {
        mvc.perform(get("/api/v1/impacts/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/v1/impacts/00000000-0000-0000-0000-000000000001/versions"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/impacts/00000000-0000-0000-0000-000000000001/versions").principal(() -> "synthetic-user"))
                .andExpect(status().isForbidden());
        assertThrows(ImpactFailure.class, () -> repository.create(null));
        assertThrows(ImpactFailure.class, () -> repository.replace(UUID.randomUUID(), 1, null));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
