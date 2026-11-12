# Plataforma Digital — Backend

API REST da Plataforma Digital, em **Java 21** e **Spring Boot 3**, com PostgreSQL 16. Cobre os
módulos do MVP: Projetos Integradores, Extensão Universitária e a base de usuários.

Esta é a estrutura inicial: todos os recursos têm CRUD completo, e as regras de negócio entram nas
próximas sprints (veja [O que vem nas próximas sprints](#o-que-vem-nas-próximas-sprints)).

## Como rodar

Requisito: **Docker**. Antes de iniciar, copie `.env.example` para `.env` e defina `DB_PASSWORD`
com um valor local exclusivo. O arquivo `.env` é ignorado pelo Git.

```bash
docker compose up --build
```

Esse comando sobe dois containers:

| Container | Porta | O que é |
| --- | --- | --- |
| `postgres` | 5432 | PostgreSQL 16 (usuário `plataforma`, credencial configurada localmente) |
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
4. `POST /api/v1/certificates`: registre o certificado da inscrição.

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
| `Certificate` | `extension.certificate` | → `Enrollment` (um certificado por inscrição) |
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

Também existem `GET /actuator/health` (healthcheck) e, só no perfil `local`,
`POST /api/v1/local/audit-events` para criar eventos de auditoria de teste.

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

Os parâmetros de Argon2id podem ser ajustados por `ARGON2_MEMORY_KIB`, `ARGON2_ITERATIONS` e
`ARGON2_PARALLELISM`. Os valores de partida no `.env.example` são os defaults mínimos da OWASP;
devem ser tratados como **a calibrar no ambiente alvo** antes de produção. O fluxo de login local
continua pendente da BE-004.

Para rodar fora do Docker (com o Postgres do compose no ar):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Defina `DB_PASSWORD` previamente no ambiente local antes de executar o comando.

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
| `PlataformaDigitalApplicationTests` | a aplicação sobe |
| `PasswordHasherTest` | hash Argon2id, sal distinto, verificação, rehash, persistência sem senha e saída de log capturada |
| `Argon2CalibrationBenchmark` | benchmark manual; executar explicitamente, não roda na suíte Maven padrão |
| `SecretRedactorTest` | sanitização de campos sensíveis e saída de log capturada |
| `AuditEventMapperTest` | sanitização dos dados do evento antes de persistir e responder |

Os testes de integração sobem um PostgreSQL real com Testcontainers: precisam do **Docker em
execução**. Sem Docker, eles são pulados.

## Secret scan

O repositório estende as regras padrão do Gitleaks em `.gitleaks.toml`. Para escanear o conteúdo
atual da árvore de trabalho, execute `gitleaks dir --redact .`.

## Argon2id

A política de hashing, limites anti-DoS para hashes salvos e instruções do benchmark manual estão em
[`docs/security/argon2id.md`](docs/security/argon2id.md). O benchmark não roda no `mvn verify` padrão.

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
| BE-017 | `CertificateService` | gerar hash/QR verificável e validação pública |

Até BE-004 e BE-010, **os endpoints não exigem login**. Por isso os CRUDs, o Swagger e a consulta de
auditoria ficam desligados no perfil padrão (produção). Quando a autenticação existir, remova a
propriedade `platform.crud.api.enabled` e as anotações `@ConditionalOnProperty` dos controllers.

## Governança

Consulte `.governance-baseline` antes de qualquer mudança. A tag declarada ainda precisa ser publicada no repositório de governança e resolver para o SHA registrado; até lá, o gate remoto de spec-compliance permanece uma pendência organizacional.

## Contratos de API — BE-003

O contrato local está em [`contracts/openapi.yaml`](contracts/openapi.yaml).
As ferramentas de contrato usam Node.js 24.14+ da linha 24 e npm, somente em
desenvolvimento/CI; o runtime do backend continua Java 21.

```bash
npm ci
npm run contracts:install-diff
npm run contracts:verify
npm run contracts:breaking -- origin/develop
mvn verify
npm run spec-compliance
```

Para consumir os exemplos sem iniciar o backend:

```bash
npm run contracts:bundle
npm run contracts:mock
curl http://127.0.0.1:4010/actuator/health
curl -H "Prefer: code=503, example=down" http://127.0.0.1:4010/actuator/health
```

Consulte [`contracts/README.md`](contracts/README.md) para escopo, limitações,
compatibilidade e evidências. A BE-003 permanece pendente dos contratos globais
e da publicação da tag de governança; a aprovação técnica local não dispensa
esses requisitos.
