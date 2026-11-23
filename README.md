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

## Observabilidade técnica

O escopo técnico entregue para o BE-016 é correlação e registro operacional sanitizado de requisições HTTP. O filtro compartilhado gera um `X-Request-ID` novo no servidor para cada requisição e registra apenas esse identificador, método HTTP permitido, status e duração. O identificador fornecido pelo cliente é substituído. Em falhas, o log não inclui a mensagem nem o stack trace da exceção.

O filtro não registra caminho, query string, headers, corpo, conteúdo funcional, ator, alvo ou motivo de decisões de negócio. Não cria persistência, histórico consultável, API de auditoria ou política própria de retenção/imutabilidade. Esses logs servem à operação e não constituem auditoria de ações de negócio.

Critérios verificáveis deste escopo: cada resposta recebe um identificador gerado no servidor; o log de sucesso contém identificador, método permitido, status e duração; o log de falha contém identificador, método permitido e duração sem detalhes da exceção; o MDC é limpo ao final da requisição. Essa proteção cobre os eventos emitidos pelo filtro. Componentes futuros devem manter a mesma minimização e não registrar credenciais, dados pessoais desnecessários ou payloads.

## Governança

Consulte `.governance-baseline` antes de qualquer mudança. A tag declarada ainda precisa ser publicada no repositório de governança e resolver para o SHA registrado; até lá, o gate remoto de spec-compliance permanece uma pendência organizacional.
