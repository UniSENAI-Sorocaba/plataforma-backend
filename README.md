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

## Governança

A preparação técnica da BE-010 está documentada em
[`docs/authorization/README.md`](docs/authorization/README.md). Ela inclui uma
API isolada de autorização e testes sintéticos; ainda não protege endpoints
de produção. A integração depende da matriz aprovada, BE-004 e DB-006.

Consulte `.governance-baseline` antes de qualquer mudança. A tag declarada ainda precisa ser publicada no repositório de governança e resolver para o SHA registrado; até lá, o gate remoto de spec-compliance permanece uma pendência organizacional.
