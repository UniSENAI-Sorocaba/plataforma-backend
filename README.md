# Plataforma Digital — Backend

API REST da Plataforma Digital, em **Java 21** e **Spring Boot 3**, com PostgreSQL 16. Cobre os
módulos do MVP: Projetos Integradores, Extensão Universitária e a base de usuários.

Esta é a estrutura inicial: todos os recursos têm CRUD completo, e as regras de negócio entram nas
próximas sprints (veja [O que vem nas próximas sprints](#o-que-vem-nas-próximas-sprints)).

## Como rodar

Requisito: **Docker**.

```bash
docker compose up --build
```

Esse comando sobe dois containers:

| Container | Porta | O que é |
| --- | --- | --- |
| `postgres` | 5432 | PostgreSQL 16 (usuário `plataforma`, senha `local-development-only`) |
| `api` | 8080 | a API, compilada dentro da imagem |

Quando subir, abra o **Swagger**: <http://localhost:8080/swagger-ui.html>.

- Parar: `docker compose down`
- Parar e apagar os dados do banco: `docker compose down -v`
- Ver os logs da API: `docker compose logs -f api`

## Testando pelo Swagger

Os recursos dependem uns dos outros pelo `id`, então crie nesta ordem. Cada `POST` devolve o `id`
gerado, que você usa no passo seguinte.

**Projetos Integradores**

1. `POST /api/v1/users`: crie um aluno e um professor.
   ```json
   { "email": "aluno@plataforma.test", "fullName": "Aluno Teste", "role": "STUDENT", "active": true }
   ```
2. `POST /api/v1/projects`: use o id do aluno como `leaderId` e o do professor como `advisorId`.
   ```json
   { "title": "Sistema IoT", "description": "Monitoramento de sensores", "projectType": "ACADEMIC_PI",
     "status": "PROPOSED", "semester": "2026.2", "course": "Engenharia de Software",
     "leaderId": "<id do aluno>", "advisorId": "<id do professor>" }
   ```
3. `POST /api/v1/project-members`: vincule alunos ao projeto.
4. `POST /api/v1/boards`: agende a banca do projeto (horários em ISO-8601, ex.: `2026-11-20T14:00:00-03:00`).
5. `POST /api/v1/evaluations`: lance as notas (0 a 10) de um avaliador na banca.

**Extensão**

1. `POST /api/v1/notices`: crie um edital (datas `AAAA-MM-DD`, `advisorId` = professor).
2. `POST /api/v1/enrollments`: inscreva um aluno no edital.
3. `POST /api/v1/activities`: registre uma atividade com horas e a referência da evidência.
4. `POST /api/v1/certificates`: emita o certificado da inscrição (`{ "enrollmentId": "<id>" }`). As horas
   são a soma das atividades **aprovadas** e precisam cobrir a carga (`totalHours`) do edital; abaixo
   disso a emissão retorna 409.
5. `GET /api/v1/public/certificates/{id}?hash=<validationHash>`: valide o certificado como um visitante.

As listagens (`GET` sem id) são paginadas, por exemplo `GET /api/v1/projects?page=0&size=20&sort=createdAt,desc`.
O `sort` aceita qualquer campo do recurso.

## Estrutura de pastas (MVC com DTOs)

O código fica em `src/main/java/plataforma/`, separado por camada:

```
config/       configurações do Spring (Swagger, Clock)
controller/   endpoints REST: recebem e devolvem DTOs
dto/          objetos de entrada (Request) e saída (Response) da API
exception/    exceções e tratamento global de erros
mapper/       conversão entre entidade (model) e DTO
model/        entidades JPA, espelho das tabelas do banco
repository/   acesso ao banco (Spring Data JPA)
service/      regras de negócio e transações
```

Caminho de uma requisição:

```
Cliente → controller → service → repository → banco
                          ↕
                  mapper (model ↔ dto)
```

Regras de convivência:

- controller só conversa com service; nunca com repository;
- entidades de `model` nunca saem pela API; a resposta é sempre um DTO;
- validação de formato (campo obrigatório, tamanho, faixa de nota) fica no DTO, com Bean Validation;
- regra de negócio (quem pode, em que status, quantas horas) fica no service;
- um service usa outro para buscar entidades relacionadas: `userService.findEntity(id)`.

### Adicionando um recurso novo

Cada recurso tem os mesmos 7 arquivos, um em cada pasta. Use projetos como modelo:

| Pasta | Arquivo | Papel |
| --- | --- | --- |
| `model/` | `Project` | entidade JPA com os campos e relações |
| `repository/` | `ProjectRepository` | interface `JpaRepository` |
| `dto/` | `ProjectRequest` | campos aceitos no POST/PUT, com validações |
| `dto/` | `ProjectResponse` | campos devolvidos pela API |
| `mapper/` | `ProjectMapper` | `toResponse` e `copyToEntity` |
| `service/` | `ProjectService` | create, findAll, findById, update, delete |
| `controller/` | `ProjectController` | rotas em `/api/v1/projects` |

## Modelo de dados

| Entidade | Tabela | Relações |
| --- | --- | --- |
| `UserAccount` | `auth.user_account` | — |
| `Project` | `academic_pi.project` | líder e orientador → `UserAccount` |
| `ProjectMember` | `academic_pi.project_member` | → `Project`, → `UserAccount` |
| `ExaminationBoard` | `academic_pi.examination_board` | → `Project` (uma banca por projeto) |
| `Evaluation` | `academic_pi.evaluation` | → `ExaminationBoard`, avaliador → `UserAccount` |
| `Notice` | `extension.notice` | orientador → `UserAccount` |
| `Enrollment` | `extension.enrollment` | → `Notice`, aluno → `UserAccount` |
| `Activity` | `extension.activity` | → `Enrollment`, revisor → `UserAccount` |
| `Certificate` | `extension.certificate` | → `Enrollment` (no máximo um `VALID` por inscrição) |
| `AuditEvent` | `public.audit_event` | — (já existe no `plataforma-database`, DB-001) |

```
UserAccount ─┬─< Project >── ProjectMember
             │      └── ExaminationBoard ──< Evaluation
             └─< Notice ──< Enrollment ──< Activity
                               └── Certificate
```

Ids são UUID gerados pela aplicação; datas e horas usam `Instant` (UTC) e `LocalDate`.

Valores aceitos nos enums:

| Campo | Valores |
| --- | --- |
| `role` | `STUDENT`, `PROFESSOR`, `COORDINATOR`, `CPA`, `ADMIN` |
| `projectType` | `ACADEMIC_PI`, `EXTENSION`, `HYBRID` |
| `status` (projeto) | `PROPOSED`, `APPROVED`, `IN_PROGRESS`, `EVALUATED`, `ARCHIVED` |
| `memberRole` | `LEADER`, `MEMBER` |
| `status` (membro) | `INVITED`, `ACCEPTED`, `DECLINED` |
| `status` (banca) | `SCHEDULED`, `IN_EVALUATION`, `COMPLETED` |
| `status` (atividade) | `PENDING`, `APPROVED`, `REJECTED` |
| `status` (certificado) | `VALID`, `REVOKED` |

## Endpoints

Todos os recursos têm o mesmo CRUD:

| Método | Rota | Ação | Sucesso |
| --- | --- | --- | --- |
| `POST` | `/api/v1/{recurso}` | criar | 201 |
| `GET` | `/api/v1/{recurso}` | listar (paginado) | 200 |
| `GET` | `/api/v1/{recurso}/{id}` | buscar por id | 200 |
| `PUT` | `/api/v1/{recurso}/{id}` | atualizar | 200 |
| `DELETE` | `/api/v1/{recurso}/{id}` | excluir | 204 |

| Recurso | Rota |
| --- | --- |
| Usuários | `/api/v1/users` |
| Projetos | `/api/v1/projects` |
| Membros de projeto | `/api/v1/project-members` |
| Bancas | `/api/v1/boards` |
| Avaliações | `/api/v1/evaluations` |
| Editais | `/api/v1/notices` |
| Inscrições | `/api/v1/enrollments` |
| Atividades de extensão | `/api/v1/activities` |
| Certificados | `/api/v1/certificates` |
| Auditoria (somente leitura) | `/api/v1/audit-events` |

Exceção: certificados não têm `PUT` nem `DELETE` (veja [Certificados](#certificados-be-017)).

Também existem `GET /actuator/health` (healthcheck) e, só no perfil `local`,
`POST /api/v1/local/audit-events` para criar eventos de auditoria de teste.

## Certificados (BE-017)

| Método | Rota | Ação | Sucesso |
| --- | --- | --- | --- |
| `POST` | `/api/v1/certificates` | emitir para uma inscrição | 201 |
| `GET` | `/api/v1/certificates` | listar (paginado) | 200 |
| `GET` | `/api/v1/certificates/{id}` | buscar por id | 200 |
| `POST` | `/api/v1/certificates/{id}/revocation` | revogar com motivo (`{ "reason": "..." }`) | 200 |
| `GET` | `/api/v1/certificates/{id}/qr-code` | QR Code PNG com o link de validação | 200 |
| `GET` | `/api/v1/public/certificates/{id}?hash=...` | validação pública, sem login | 200 |

Regras:

- emite somente se as horas aprovadas da inscrição forem iguais ou maiores que a carga do edital
  (`totalHours`); as horas não vêm do cliente. Regra aguardando confirmação do PO;
- no máximo um certificado `VALID` por inscrição (segunda emissão → 409). Depois de revogado, a
  inscrição pode receber um novo certificado. A emissão trava a linha da inscrição para evitar duas
  emissões simultâneas; o índice definitivo fica com a DB-013:
  `create unique index on extension.certificate (enrollment_id) where status = 'VALID'`;
- o conteúdo emitido é imutável: `validationHash` é o SHA-256 de
  `plataforma-certificate:v1|enrollment=…|student=…|notice=…|hours=…|issuedAt=…`;
- revogar não apaga o registro; revogar de novo → 409;
- a validação pública exige id **e** hash e responde só `id`, `status`, `issuedAt` e `revokedAt`
  (sem aluno, horas ou motivo, conforme AC-S3-06), com `Cache-Control: no-store`;
- id inexistente, hash diferente e registro adulterado no banco respondem o mesmo 404.

O link do QR Code vem de `platform.certificate.validation-url-template` (variável
`CERTIFICATE_VALIDATION_URL_TEMPLATE`), que deve apontar para a página de validação do frontend e
conter `{id}` e `{hash}`, por exemplo `https://<frontend>/certificados/validar?id={id}&hash={hash}`.
**Não há valor padrão fora de `local`/`test`: sem a variável, a aplicação não sobe**, para nenhum QR
ser impresso apontando para localhost. URL relativa ou sem `http(s)` também impede a inicialização.
A validação pública é ligada por `platform.certificate.validation.enabled` (ligada em `local` e `test`).

Ainda dependem de decisão do PO (SPEC-EXT-003): PDF, assinatura digital, validade, campos públicos,
como evitar PDF falsificado com QR verdadeiro e quem pode emitir/revogar (BE-010).

## Erros

Os erros seguem o padrão RFC 9457 (Problem Details):

```json
{ "title": "Recurso não encontrado", "status": 404,
  "detail": "Projeto não encontrado: 3f1c...", "instance": "/api/v1/projects/3f1c..." }
```

| Status | Quando |
| --- | --- |
| 400 | campo inválido; a resposta traz `fields` com o erro de cada campo |
| 404 | id não encontrado, inclusive ids de relações (ex.: `leaderId`) |
| 409 | valor duplicado (ex.: e-mail) ou exclusão de registro ainda em uso |

## Configuração

| Perfil | Uso | Tabelas | Endpoints de CRUD | Swagger |
| --- | --- | --- | --- | --- |
| padrão | produção | só valida (`ddl-auto: validate`) | desligados | desligado |
| `local` | Docker Compose e desenvolvimento | Hibernate cria (`update`) | ligados | ligado |
| `test` | testes automatizados | Hibernate cria e apaga (`create-drop`) | ligados | desligado |

Os endpoints de CRUD são ligados pela propriedade `platform.crud.api.enabled`, e a consulta de
auditoria por `platform.audit.api.enabled`.

Conexão com o banco por variáveis de ambiente: `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.

Para rodar fora do Docker (com o Postgres do compose no ar):

```bash
DB_PASSWORD=local-development-only mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Banco de dados

O schema é responsabilidade do time de banco (repositório `plataforma-database`): este repositório
não tem SQL. Enquanto as migrations oficiais não existem, os perfis `local` e `test` deixam o
Hibernate criar as tabelas a partir das entidades.

Quando as migrations forem publicadas, remova o bloco `spring.jpa` de `application-local.yaml`. O
perfil volta ao `validate`, que confere se as entidades batem com as tabelas oficiais. Se algum
campo divergir, ajuste a entidade.

## Testes

```bash
mvn verify
```

| Teste | O que cobre |
| --- | --- |
| `ProjectCrudIntegrationTests` | CRUD de projeto pela API, validação (400), relação inexistente (404) e e-mail duplicado (409) |
| `AuditEventIntegrationTests` | gravação e consulta de auditoria com `jsonb` |
| `AuditEventControllerTest` | rotas de auditoria e resposta 404 |
| `AuditEventServiceTest` | regras do service de auditoria (unitário) |
| `CertificateIntegrationTests` | emissão, validação pública, QR, revogação e adulteração com PostgreSQL |
| `CertificateServiceTest` | regras de emissão, revogação e validação (unitário) |
| `CertificateHasherTest` | hash determinístico, formato canônico v1 e detecção de alteração |
| `PublicCertificateControllerTest` | rota pública ligada/desligada e resposta mínima |
| `QrCodeGeneratorTest` | o PNG gerado é lido de volta com o conteúdo correto |
| `PlataformaDigitalApplicationTests` | a aplicação sobe |

Os testes de integração sobem um PostgreSQL real com Testcontainers: precisam do **Docker em
execução**. Sem Docker, eles são pulados.

## O que vem nas próximas sprints

As regras abaixo estão marcadas no código com `TODO BE-0xx`, o ID da task no backlog.

| Task | Onde | O que falta |
| --- | --- | --- |
| BE-004 / BE-005 | `UserAccount` | login JWT RS256 e senha com hash Argon2id |
| BE-010 | `UserService` e demais | permissões por perfil (RBAC) em todos os endpoints |
| BE-007 | `ProjectService`, `ProjectStatus` | validar papéis, campos e transições de status |
| BE-008 | `ProjectMemberService` | convites: aceitar/recusar sem duplicidade |
| BE-011 | `ExaminationBoardService` | impedir conflito de horário e avaliador inválido |
| BE-015 | `EvaluationService` | calcular `finalGrade` pela rubrica e consolidar a nota da banca |
| BE-009 | `EnrollmentService` | respeitar janela do edital e elegibilidade |
| BE-014 | `ActivityService` | deferir/indeferir com motivo e total de horas |
| BE-017 | `CertificateService` | PDF, assinatura, validade e reemissão (aguardam o PO); RBAC na emissão/revogação |

Até BE-004 e BE-010, **os endpoints não exigem login**. Por isso os CRUDs, o Swagger e a consulta de
auditoria ficam desligados no perfil padrão (produção). Quando a autenticação existir, remova a
propriedade `platform.crud.api.enabled` e as anotações `@ConditionalOnProperty` dos controllers.

## Governança

Consulte `.governance-baseline` antes de qualquer mudança. A tag declarada ainda precisa ser publicada no repositório de governança e resolver para o SHA registrado; até lá, o gate remoto de spec-compliance permanece uma pendência organizacional.
