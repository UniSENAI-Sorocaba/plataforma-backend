package br.senai.plataformadigital.academicpi;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.senai.plataformadigital.exception.ConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ImpactServiceTest {
    @Test
    void createsImpactOnlyForProjectResponsible() {
        var repository = Mockito.mock(ImpactRepository.class);
        var service = new ImpactService(repository);
        UUID project = UUID.randomUUID(), actor = UUID.randomUUID();
        var request = new ImpactRequest("Impacto social", "Descrição suficiente do impacto gerado.", null);
        when(repository.isResponsible(project, actor)).thenReturn(false);
        assertThatThrownBy(() -> service.create(project, request, actor)).isInstanceOf(ConflictException.class);
        when(repository.isResponsible(project, actor)).thenReturn(true);
        service.create(project, request, actor);
        verify(repository).insert(project, request, actor);
    }

    @Test
    void submitsAndApprovesOnlyValidTransitions() {
        var repository = Mockito.mock(ImpactRepository.class);
        var service = new ImpactService(repository);
        UUID id = UUID.randomUUID(), actor = UUID.randomUUID();
        var response = new ImpactResponse(id, UUID.randomUUID(), "Impacto", ImpactStatus.SUBMITTED, 1, "Resumo do impacto", null, java.time.Instant.now());
        when(repository.canManage(id, actor)).thenReturn(true);
        when(repository.isCoordinator(actor)).thenReturn(true);
        when(repository.updateStatus(id, ImpactStatus.SUBMITTED, actor)).thenReturn(1);
        when(repository.updateStatus(id, ImpactStatus.APPROVED, actor)).thenReturn(1);
        when(repository.find(id)).thenReturn(response);
        service.submit(id, actor);
        service.approve(id, actor);
        verify(repository).updateStatus(id, ImpactStatus.SUBMITTED, actor);
        verify(repository).updateStatus(id, ImpactStatus.APPROVED, actor);
    }

    @Test
    void rejectsInvalidTransition() {
        var repository = Mockito.mock(ImpactRepository.class);
        var service = new ImpactService(repository);
        UUID id = UUID.randomUUID(), actor = UUID.randomUUID();
        when(repository.isCoordinator(actor)).thenReturn(true);
        when(repository.updateStatus(id, ImpactStatus.APPROVED, actor)).thenReturn(0);
        assertThatThrownBy(() -> service.approve(id, actor)).isInstanceOf(ConflictException.class);
    }
}
