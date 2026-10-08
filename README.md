# Plataforma Digital — Backend

Backend da Plataforma Digital, em Java 21 e Spring Boot 3.

## Estrutura de pastas (MVC com DTOs)

O código fica em `src/main/java/br/senai/plataformadigital/`, separado por camada:

```
config/       configurações do Spring (Swagger, Clock)
controller/   endpoints REST: recebem e devolvem DTOs
dto/          objetos de entrada e saída da API
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
- nomeie os arquivos pela funcionalidade e pela camada: `AuditEventController`, `AuditEventService`,
  `AuditEventResponse`... Uma funcionalidade nova (ex.: propostas) cria `ProposalController`,
  `ProposalService`, `ProposalRepository`, `Proposal` e seus DTOs, cada um na sua pasta;
- use a auditoria (`AuditEvent*`) como exemplo completo de todas as camadas.

O schema é responsabilidade do repositório `plataforma-database`. O backend usa `ddl-auto: validate`
e nunca cria nem altera tabelas. Erros seguem RFC 9457 (`ProblemDetail`).

## Desenvolvimento local

Requisitos: JDK 21, Maven 3.9+ e Docker.

```bash
mvn verify   # testes de integração usam Testcontainers e exigem Docker em execução
```

### Testando pelo Swagger

1. Suba um PostgreSQL 16 com as migrations e a porta 5432 publicada:

   ```bash
   docker run -d --name plataforma-postgres-local \
     -e POSTGRES_DB=plataforma -e POSTGRES_USER=plataforma \
     -e POSTGRES_PASSWORD=local-development-only -p 5432:5432 \
     -v "$PWD/src/test/resources/database:/docker-entrypoint-initdb.d:ro" \
     postgres:16-alpine
   ```

2. Rode a aplicação no perfil `local`:

   ```bash
   DB_PASSWORD=local-development-only mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. Abra <http://localhost:8080/swagger-ui.html>. Crie eventos em **Auditoria (somente local)** e
   consulte-os em **Auditoria**.

O perfil `local` é o único que habilita o Swagger, a consulta de auditoria e o endpoint de criação
manual de eventos. Nos demais ambientes, eles ficam desligados até existir RBAC server-side (BE-010).

## Governança

Consulte `.governance-baseline` antes de qualquer mudança. A tag declarada ainda precisa ser publicada no repositório de governança e resolver para o SHA registrado; até lá, o gate remoto de spec-compliance permanece uma pendência organizacional.
