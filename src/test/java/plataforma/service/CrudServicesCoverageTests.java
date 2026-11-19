package plataforma.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import plataforma.dto.*;
import plataforma.exception.ResourceNotFoundException;
import plataforma.mapper.*;
import plataforma.model.*;
import plataforma.repository.*;

/** Exercita fluxos CRUD e a resolução de relacionamentos dos services já implementados. */
class CrudServicesCoverageTests {

    private final UUID id = UUID.randomUUID();
    private final UUID relatedId = UUID.randomUUID();

    @Test
    void activityCrudResolvesEnrollmentAndReportsMissingResource() {
        var repository = mock(ActivityRepository.class);
        var mapper = new ActivityMapper();
        var enrollments = mock(EnrollmentService.class);
        var service = new ActivityService(repository, mapper, enrollments);
        var entity = new Activity();
        var enrollment = new Enrollment();
        entity.setEnrollment(enrollment);
        var request = new ActivityRequest(relatedId, "Horas", "descrição", 2, "evidence/key");
        when(enrollments.findEntity(relatedId)).thenReturn(enrollment);
        when(repository.saveAndFlush(any(Activity.class))).thenAnswer(call -> call.getArgument(0));
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));

        service.create(request);
        assertThat(service.findAll(PageRequest.of(0, 10)).content()).hasSize(1);
        service.findById(id);
        service.update(id, request);
        assertThat(entity.getEnrollment()).isSameAs(enrollment);
        assertThat(entity.getTitle()).isEqualTo("Horas");
        service.delete(id);
        verify(repository).delete(entity);
        when(repository.findById(relatedId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findEntity(relatedId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void certificateCrudResolvesEnrollmentAndReportsMissingResource() {
        var repository = mock(CertificateRepository.class);
        var mapper = new CertificateMapper();
        var enrollments = mock(EnrollmentService.class);
        var service = new CertificateService(repository, mapper, enrollments);
        var entity = new Certificate();
        var enrollment = new Enrollment();
        entity.setEnrollment(enrollment);
        var request = new CertificateRequest(relatedId, 12);
        when(enrollments.findEntity(relatedId)).thenReturn(enrollment);
        when(repository.saveAndFlush(any(Certificate.class))).thenAnswer(call -> call.getArgument(0));
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));

        service.create(request);
        service.findAll(PageRequest.of(0, 10));
        service.findById(id);
        service.update(id, request);
        assertThat(entity.getEnrollment()).isSameAs(enrollment);
        assertThat(entity.getHours()).isEqualTo(12);
        service.delete(id);
        verify(repository).delete(entity);
        when(repository.findById(relatedId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findEntity(relatedId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void enrollmentCrudResolvesNoticeAndStudentAndReportsMissingResource() {
        var repository = mock(EnrollmentRepository.class);
        var mapper = new EnrollmentMapper();
        var notices = mock(NoticeService.class);
        var users = mock(UserService.class);
        var service = new EnrollmentService(repository, mapper, notices, users);
        var entity = new Enrollment();
        var notice = new Notice();
        var student = new UserAccount();
        entity.setNotice(notice);
        entity.setStudent(student);
        var request = new EnrollmentRequest(relatedId, id);
        when(notices.findEntity(relatedId)).thenReturn(notice);
        when(users.findEntity(id)).thenReturn(student);
        when(repository.saveAndFlush(any(Enrollment.class))).thenAnswer(call -> call.getArgument(0));
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));

        service.create(request);
        service.findAll(PageRequest.of(0, 10));
        service.findById(id);
        service.update(id, request);
        assertThat(entity.getNotice()).isSameAs(notice);
        assertThat(entity.getStudent()).isSameAs(student);
        service.delete(id);
        verify(repository).delete(entity);
        when(repository.findById(relatedId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findEntity(relatedId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void noticeCrudResolvesAdvisorAndReportsMissingResource() {
        var repository = mock(NoticeRepository.class);
        var mapper = new NoticeMapper();
        var users = mock(UserService.class);
        var service = new NoticeService(repository, mapper, users);
        var entity = new Notice();
        var advisor = new UserAccount();
        entity.setAdvisor(advisor);
        var request = new NoticeRequest("Edital", "Descrição", LocalDate.now(), LocalDate.now(), 10, relatedId, true);
        when(users.findEntity(relatedId)).thenReturn(advisor);
        when(repository.saveAndFlush(any(Notice.class))).thenAnswer(call -> call.getArgument(0));
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));

        service.create(request);
        service.findAll(PageRequest.of(0, 10));
        service.findById(id);
        service.update(id, request);
        assertThat(entity.getAdvisor()).isSameAs(advisor);
        assertThat(entity.getTitle()).isEqualTo("Edital");
        service.delete(id);
        verify(repository).delete(entity);
        when(repository.findById(relatedId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findEntity(relatedId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void projectCrudResolvesOptionalAdvisorAndReportsMissingResource() {
        var repository = mock(ProjectRepository.class);
        var mapper = new ProjectMapper();
        var users = mock(UserService.class);
        var service = new ProjectService(repository, mapper, users);
        var entity = new Project();
        var leader = new UserAccount();
        var advisor = new UserAccount();
        entity.setLeader(leader);
        entity.setAdvisor(advisor);
        var withAdvisor = new ProjectRequest("Projeto", "Descrição", ProjectType.ACADEMIC_PI,
                ProjectStatus.PROPOSED, "2026.2", "Curso", relatedId, id);
        var withoutAdvisor = new ProjectRequest("Projeto", "Descrição", ProjectType.ACADEMIC_PI,
                ProjectStatus.PROPOSED, "2026.2", "Curso", relatedId, null);
        when(users.findEntity(relatedId)).thenReturn(leader);
        when(users.findEntity(id)).thenReturn(advisor);
        when(repository.saveAndFlush(any(Project.class))).thenAnswer(call -> call.getArgument(0));
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));

        service.create(withAdvisor);
        service.update(id, withoutAdvisor);
        assertThat(entity.getLeader()).isSameAs(leader);
        assertThat(entity.getAdvisor()).isNull();
        service.findAll(PageRequest.of(0, 10));
        service.findById(id);
        service.delete(id);
        verify(repository).delete(entity);
        when(repository.findById(relatedId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findEntity(relatedId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void projectMemberCrudResolvesProjectAndUserAndReportsMissingResource() {
        var repository = mock(ProjectMemberRepository.class);
        var mapper = new ProjectMemberMapper();
        var projects = mock(ProjectService.class);
        var users = mock(UserService.class);
        var service = new ProjectMemberService(repository, mapper, projects, users);
        var entity = new ProjectMember();
        var project = new Project();
        var user = new UserAccount();
        entity.setProject(project);
        entity.setUser(user);
        var request = new ProjectMemberRequest(relatedId, id, MemberRole.MEMBER, InvitationStatus.INVITED);
        when(projects.findEntity(relatedId)).thenReturn(project);
        when(users.findEntity(id)).thenReturn(user);
        when(repository.saveAndFlush(any(ProjectMember.class))).thenAnswer(call -> call.getArgument(0));
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));

        service.create(request);
        service.findAll(PageRequest.of(0, 10));
        service.findById(id);
        service.update(id, request);
        assertThat(entity.getProject()).isSameAs(project);
        assertThat(entity.getUser()).isSameAs(user);
        assertThat(entity.getMemberRole()).isEqualTo(MemberRole.MEMBER);
        service.delete(id);
        verify(repository).delete(entity);
        when(repository.findById(relatedId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findEntity(relatedId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void examinationBoardCrudResolvesProjectAndReportsMissingResource() {
        var repository = mock(ExaminationBoardRepository.class);
        var mapper = new ExaminationBoardMapper();
        var projects = mock(ProjectService.class);
        var service = new ExaminationBoardService(repository, mapper, projects);
        var entity = new ExaminationBoard();
        var project = new Project();
        entity.setProject(project);
        var request = new ExaminationBoardRequest(relatedId, Instant.parse("2026-11-20T12:00:00Z"),
                Instant.parse("2026-11-20T13:00:00Z"), "Sala", BoardStatus.SCHEDULED);
        when(projects.findEntity(relatedId)).thenReturn(project);
        when(repository.saveAndFlush(any(ExaminationBoard.class))).thenAnswer(call -> call.getArgument(0));
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));

        service.create(request);
        service.findAll(PageRequest.of(0, 10));
        service.findById(id);
        service.update(id, request);
        assertThat(entity.getProject()).isSameAs(project);
        assertThat(entity.getLocation()).isEqualTo("Sala");
        service.delete(id);
        verify(repository).delete(entity);
        when(repository.findById(relatedId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findEntity(relatedId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void evaluationCrudResolvesBoardAndEvaluatorAndReportsMissingResource() {
        var repository = mock(EvaluationRepository.class);
        var mapper = new EvaluationMapper();
        var boards = mock(ExaminationBoardService.class);
        var users = mock(UserService.class);
        var service = new EvaluationService(repository, mapper, boards, users);
        var entity = new Evaluation();
        var board = new ExaminationBoard();
        var evaluator = new UserAccount();
        entity.setBoard(board);
        entity.setEvaluator(evaluator);
        var request = new EvaluationRequest(relatedId, id, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, "feedback");
        when(boards.findEntity(relatedId)).thenReturn(board);
        when(users.findEntity(id)).thenReturn(evaluator);
        when(repository.saveAndFlush(any(Evaluation.class))).thenAnswer(call -> call.getArgument(0));
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));

        service.create(request);
        service.findAll(PageRequest.of(0, 10));
        service.findById(id);
        service.update(id, request);
        assertThat(entity.getBoard()).isSameAs(board);
        assertThat(entity.getEvaluator()).isSameAs(evaluator);
        assertThat(entity.getTechnicalGrade()).isEqualByComparingTo(BigDecimal.TEN);
        service.delete(id);
        verify(repository).delete(entity);
        when(repository.findById(relatedId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findEntity(relatedId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void userCrudCopiesRequestAndReportsMissingResource() {
        var repository = mock(UserAccountRepository.class);
        var mapper = new UserMapper();
        var service = new UserService(repository, mapper);
        var entity = new UserAccount();
        var request = new UserRequest("ana@example.test", "Ana", Role.STUDENT, true);
        when(repository.saveAndFlush(any(UserAccount.class))).thenAnswer(call -> call.getArgument(0));
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));

        service.create(request);
        service.findAll(PageRequest.of(0, 10));
        service.findById(id);
        service.update(id, request);
        assertThat(entity.getEmail()).isEqualTo("ana@example.test");
        service.delete(id);
        verify(repository).delete(entity);
        when(repository.findById(relatedId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findEntity(relatedId)).isInstanceOf(ResourceNotFoundException.class);
    }
}
