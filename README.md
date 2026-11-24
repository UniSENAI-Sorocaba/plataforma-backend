# Plataforma Digital — Backend

Monólito modular da Plataforma Digital, em Java 21 e Spring Boot 3. A estrutura inicial atende ao bootstrap de `BE-001`: aplicação compilável, healthcheck e fronteiras de módulos documentadas.

## Módulos

- `auth`: identidade, sessão e autorização;
- `academicpi`: propostas, equipes, bancas, notas e entregáveis;
- `extension`: editais, inscrições, atividades, horas e certificados;
- `portal`: catálogo público e impacto;
- `shared`: infraestrutura estritamente transversal, sem regras de domínio.

Um módulo não acessa internals de outro módulo. Integrações atravessam APIs públicas ou contratos versionados.

## Desenvolvimento local

Requisitos: JDK 21 e Maven 3.9+.

```bash
mvn spring-boot:run
curl http://localhost:8080/actuator/health
mvn verify
```

## Autenticação JWT

O modo padrão mantém a aplicação sem autenticação para preservar o bootstrap local. Para ativar o login e a proteção das demais rotas, forneça as propriedades abaixo por variável de ambiente ou configuração externa; as chaves não devem ser versionadas.

```yaml
security:
  jwt:
    enabled: true
    issuer: https://plataforma.example.edu.br
    audience: plataforma-api
    public-key: ${JWT_PUBLIC_KEY}
    private-key: ${JWT_PRIVATE_KEY}
```

O endpoint `POST /api/v1/auth/login` recebe `{ "email": "...", "password": "..." }` e retorna um access token Bearer RS256 com validade de 15 minutos. A persistência real de usuários será conectada ao repositório de identidade da Fase 2; até essa integração, a implementação usa o contrato `UserAccountRepository`.

Quando `security.jwt.enabled=true` e um `DataSource` PostgreSQL estiver configurado, o adapter JDBC usa as tabelas DB-005 para persistir sessões e refresh tokens. O prazo padrão do refresh token é de 30 dias e pode ser alterado por `security.session.refresh-lifetime`. O modo sem datasource não ativa esse adapter.

## Observabilidade técnica

O escopo técnico entregue para o BE-016 é correlação e registro operacional sanitizado de requisições HTTP. O `RequestLoggingFilter` usa o identificador estabelecido pelo `CorrelationIdFilter` e não cria um segundo ID. O cabeçalho de correlação é `X-Correlation-Id`; valores recebidos só são aceitos quando correspondem a `[A-Za-z0-9._-]{1,64}`. Valores ausentes ou inválidos são substituídos por um UUID gerado no servidor.

O log registra apenas o identificador de correlação, método HTTP permitido, status e duração. Não registra caminho, query string, headers, corpo, conteúdo funcional nem mensagem/stack trace de exceção. O filtro não cria nem altera persistência de auditoria.

A `main` já contém a auditoria institucional `institutional.audit_log` e o endpoint `GET /api/v1/coordination/audit`, adicionados pela PR #74. Esse recurso preexistente não é alterado por esta mudança; sua compatibilidade com a decisão de produto sobre não manter auditoria de negócio precisa ser reconciliada separadamente.

Critérios verificáveis deste escopo: a resposta e o corpo de erro compartilham o mesmo `X-Correlation-Id`; o log de sucesso contém o ID, método permitido, status e duração; o log de falha contém o mesmo ID e método, sem detalhes da exceção; o `CorrelationIdFilter` limpa o MDC ao final da requisição. Componentes futuros devem manter a mesma minimização e não registrar credenciais, dados pessoais desnecessários ou payloads.

## Governança

A autorização HTTP da BE-010 está documentada em
[`docs/authorization/README.md`](docs/authorization/README.md). Inclui negação
por padrão, catálogo obrigatório de rotas, 401/403/503 e testes do interceptor.
A política institucional depende da matriz aprovada, BE-004 e DB-006.

Consulte `.governance-baseline` antes de qualquer mudança. A tag declarada ainda precisa ser publicada no repositório de governança e resolver para o SHA registrado; até lá, o gate remoto de spec-compliance permanece uma pendência organizacional.
