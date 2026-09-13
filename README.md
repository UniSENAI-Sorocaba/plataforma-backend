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

## Governança

Consulte `.governance-baseline` antes de qualquer mudança. A tag declarada ainda precisa ser publicada no repositório de governança e resolver para o SHA registrado; até lá, o gate remoto de spec-compliance permanece uma pendência organizacional.
