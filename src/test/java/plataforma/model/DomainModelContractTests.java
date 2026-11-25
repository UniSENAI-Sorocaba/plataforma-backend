package plataforma.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifica os contratos de estado que os adapters JPA e os serviços consomem. */
class DomainModelContractTests {
    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void userProjectAndMembershipKeepTheirRelationshipAndWorkflowState() {
        var leader = user("leader@example.test", Role.STUDENT);
        var advisor = user("advisor@example.test", Role.PROFESSOR);
        var project = new Project();
        assertThat(project.getId()).isNull();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PROPOSED);
        project.setTitle("Projeto de teste");
        project.setDescription("Descrição persistida");
        project.setProjectType(ProjectType.ACADEMIC_PI);
        project.setStatus(ProjectStatus.APPROVED);
        project.setSemester("2026.2");
        project.setCourse("Engenharia");
        project.setLeader(leader);
        project.setAdvisor(advisor);
        assertThat(project.getTitle()).isEqualTo("Projeto de teste");
        assertThat(project.getDescription()).isEqualTo("Descrição persistida");
        assertThat(project.getProjectType()).isEqualTo(ProjectType.ACADEMIC_PI);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.APPROVED);
        assertThat(project.getSemester()).isEqualTo("2026.2");
        assertThat(project.getCourse()).isEqualTo("Engenharia");
        assertThat(project.getLeader()).isSameAs(leader);
        assertThat(project.getAdvisor()).isSameAs(advisor);
        assertThat(project.getCreatedAt()).isNull();
        assertThat(project.getUpdatedAt()).isNull();

        var member = new ProjectMember();
        assertThat(member.getId()).isNull();
        assertThat(member.getStatus()).isEqualTo(InvitationStatus.INVITED);
        member.setProject(project);
        member.setUser(leader);
        member.setMemberRole(MemberRole.MEMBER);
        member.setStatus(InvitationStatus.ACCEPTED);
        assertThat(member.getProject()).isSameAs(project);
        assertThat(member.getUser()).isSameAs(leader);
        assertThat(member.getMemberRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(member.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(member.getInvitedAt()).isNull();
    }

    @Test
    void userAccountKeepsIdentityAndActivationFields() {
        var account = new UserAccount();
        assertThat(account.getId()).isNull();
        assertThat(account.isActive()).isTrue();
        account.setEmail("person@example.test");
        account.setFullName("Example Person");
        account.setRole(Role.PROFESSOR);
        account.setActive(false);
        assertThat(account.getEmail()).isEqualTo("person@example.test");
        assertThat(account.getFullName()).isEqualTo("Example Person");
        assertThat(account.getRole()).isEqualTo(Role.PROFESSOR);
        assertThat(account.isActive()).isFalse();
        assertThat(account.getCreatedAt()).isNull();
        assertThat(account.getUpdatedAt()).isNull();
    }

    @Test
    void noticeEnrollmentAndCertificatePreserveTheEnrollmentChain() {
        var advisor = user("advisor@example.test", Role.PROFESSOR);
        var student = user("student@example.test", Role.STUDENT);
        var notice = new Notice();
        assertThat(notice.getId()).isNull();
        assertThat(notice.isActive()).isTrue();
        notice.setTitle("Edital 2026.2");
        notice.setDescription("Edital de extensão");
        notice.setStartsOn(LocalDate.parse("2026-08-01"));
        notice.setEndsOn(LocalDate.parse("2026-12-01"));
        notice.setTotalHours(40);
        notice.setAdvisor(advisor);
        notice.setActive(false);
        assertThat(notice.getTitle()).isEqualTo("Edital 2026.2");
        assertThat(notice.getDescription()).isEqualTo("Edital de extensão");
        assertThat(notice.getStartsOn()).isEqualTo(LocalDate.parse("2026-08-01"));
        assertThat(notice.getEndsOn()).isEqualTo(LocalDate.parse("2026-12-01"));
        assertThat(notice.getTotalHours()).isEqualTo(40);
        assertThat(notice.getAdvisor()).isSameAs(advisor);
        assertThat(notice.isActive()).isFalse();
        assertThat(notice.getCreatedAt()).isNull();

        var enrollment = new Enrollment();
        assertThat(enrollment.getId()).isNull();
        enrollment.setNotice(notice);
        enrollment.setStudent(student);
        assertThat(enrollment.getNotice()).isSameAs(notice);
        assertThat(enrollment.getStudent()).isSameAs(student);
        assertThat(enrollment.getEnrolledAt()).isNull();

        var certificate = new Certificate();
        assertThat(certificate.getId()).isNull();
        certificate.setEnrollment(enrollment);
        certificate.setHours(40);
        assertThat(certificate.getEnrollment()).isSameAs(enrollment);
        assertThat(certificate.getHours()).isEqualTo(40);
        assertThat(certificate.getValidationHash()).isNull();
        assertThat(certificate.getIssuedAt()).isNull();
    }

    @Test
    void activityExposesRequestedEvidenceAndReviewState() {
        var activity = new Activity();
        assertThat(activity.getId()).isNull();
        assertThat(activity.getStatus()).isEqualTo(ReviewStatus.PENDING);
        var enrollment = new Enrollment();
        var reviewer = user("reviewer@example.test", Role.PROFESSOR);
        activity.setEnrollment(enrollment);
        activity.setTitle("Mentoria");
        activity.setDescription("Apoio técnico");
        activity.setHoursRequested(8);
        activity.setEvidenceKey("evidence/mentor.pdf");
        assertThat(activity.getEnrollment()).isSameAs(enrollment);
        assertThat(activity.getTitle()).isEqualTo("Mentoria");
        assertThat(activity.getDescription()).isEqualTo("Apoio técnico");
        assertThat(activity.getHoursRequested()).isEqualTo(8);
        assertThat(activity.getEvidenceKey()).isEqualTo("evidence/mentor.pdf");
        assertThat(activity.getHoursApproved()).isNull();
        assertThat(activity.getDecisionReason()).isNull();
        assertThat(activity.getReviewer()).isNull();
        assertThat(activity.getSubmittedAt()).isNull();
        assertThat(activity.getReviewedAt()).isNull();
        assertThat(reviewer.getEmail()).isEqualTo("reviewer@example.test");
    }

    @Test
    void boardAndEvaluationKeepSchedulingAndRubricValues() {
        var project = new Project();
        var board = new ExaminationBoard();
        assertThat(board.getId()).isNull();
        assertThat(board.getStatus()).isEqualTo(BoardStatus.SCHEDULED);
        board.setProject(project);
        board.setScheduledAt(NOW);
        board.setEndsAt(NOW.plusSeconds(3600));
        board.setLocation("Sala 1");
        board.setStatus(BoardStatus.COMPLETED);
        assertThat(board.getProject()).isSameAs(project);
        assertThat(board.getScheduledAt()).isEqualTo(NOW);
        assertThat(board.getEndsAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(board.getLocation()).isEqualTo("Sala 1");
        assertThat(board.getStatus()).isEqualTo(BoardStatus.COMPLETED);
        assertThat(board.getFinalGrade()).isNull();
        assertThat(board.getCreatedAt()).isNull();

        var evaluator = user("evaluator@example.test", Role.PROFESSOR);
        var evaluation = new Evaluation();
        assertThat(evaluation.getId()).isNull();
        evaluation.setBoard(board);
        evaluation.setEvaluator(evaluator);
        evaluation.setTechnicalGrade(new BigDecimal("8.25"));
        evaluation.setPresentationGrade(new BigDecimal("9.00"));
        evaluation.setArtifactGrade(new BigDecimal("7.50"));
        evaluation.setFeedback("Boa apresentação");
        assertThat(evaluation.getBoard()).isSameAs(board);
        assertThat(evaluation.getEvaluator()).isSameAs(evaluator);
        assertThat(evaluation.getTechnicalGrade()).isEqualByComparingTo("8.25");
        assertThat(evaluation.getPresentationGrade()).isEqualByComparingTo("9.00");
        assertThat(evaluation.getArtifactGrade()).isEqualByComparingTo("7.50");
        assertThat(evaluation.getFinalGrade()).isNull();
        assertThat(evaluation.getFeedback()).isEqualTo("Boa apresentação");
        assertThat(evaluation.getCreatedAt()).isNull();
    }

    @Test
    void auditEventFactoryExposesAnImmutableEventSnapshot() {
        var eventId = UUID.randomUUID();
        var recordId = UUID.randomUUID();
        var oldData = Map.<String, Object>of("name", "old");
        var newData = Map.<String, Object>of("name", "new");
        var event = AuditEvent.record(eventId, NOW, "auth", "user_account", AuditOperation.UPDATE,
                recordId, USER_ID, oldData, newData);

        assertThat(event.getId()).isEqualTo(eventId);
        assertThat(event.getOccurredAt()).isEqualTo(NOW);
        assertThat(event.getSchemaName()).isEqualTo("auth");
        assertThat(event.getTableName()).isEqualTo("user_account");
        assertThat(event.getOperation()).isEqualTo(AuditOperation.UPDATE);
        assertThat(event.getRecordId()).isEqualTo(recordId);
        assertThat(event.getActorId()).isEqualTo(USER_ID);
        assertThat(event.getOldData()).isSameAs(oldData);
        assertThat(event.getNewData()).isSameAs(newData);
    }

    private UserAccount user(String email, Role role) {
        var user = new UserAccount();
        user.setEmail(email);
        user.setFullName(email.substring(0, email.indexOf('@')));
        user.setRole(role);
        return user;
    }
}
