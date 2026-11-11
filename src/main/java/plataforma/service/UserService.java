package plataforma.service;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import plataforma.dto.PageResponse;
import plataforma.dto.UserRequest;
import plataforma.dto.UserResponse;
import plataforma.exception.ResourceNotFoundException;
import plataforma.mapper.UserMapper;
import plataforma.model.UserAccount;
import plataforma.repository.UserAccountRepository;

// TODO BE-010: restringir a gestão de usuários ao perfil ADMIN.
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserAccountRepository repository;
    private final UserMapper mapper;

    public UserService(UserAccountRepository repository, UserMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        var user = new UserAccount();
        mapper.copyToEntity(request, user);
        return mapper.toResponse(repository.saveAndFlush(user));
    }

    public PageResponse<UserResponse> findAll(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable), mapper::toResponse);
    }

    public UserResponse findById(UUID id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional
    public UserResponse update(UUID id, UserRequest request) {
        var user = findEntity(id);
        mapper.copyToEntity(request, user);
        return mapper.toResponse(repository.saveAndFlush(user));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findEntity(id));
    }

    /** Usado também por outros services para resolver relações (líder, orientador, aluno...). */
    public UserAccount findEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }
}
