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

## Contratos de API — BE-003

O contrato local está em [`contracts/openapi.yaml`](contracts/openapi.yaml).
As ferramentas de contrato usam Node.js 24.14+ da linha 24 e npm, somente em
desenvolvimento/CI; o runtime do backend continua Java 21.

```bash
npm ci
npm run contracts:install-diff
npm run contracts:verify
npm run contracts:breaking -- origin/main
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

## Governança

Consulte `.governance-baseline` antes de qualquer mudança. A tag declarada ainda precisa ser publicada no repositório de governança e resolver para o SHA registrado; até lá, o gate remoto de spec-compliance permanece uma pendência organizacional.
