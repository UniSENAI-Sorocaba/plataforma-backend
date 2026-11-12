# BE-007 — CRUD de propostas

**Issue:** [plataforma-backend#7](https://github.com/UniSENAI-Sorocaba/plataforma-backend/issues/7)  

## Encerramento administrativo

Texto proposto para registrar um eventual fechamento parcial no tracker:

> Encerrada administrativamente com escopo parcial, não como concluída. Permanecem pendentes RBAC/isolamento, contrato e alinhamento de schema, publicação/duração e execução dos testes com JDK 21 e Docker. Esses itens serão acompanhados em Issues de follow-up. O fechamento não representa aceite dos critérios funcionais pendentes; requer aprovação do PO.

**Situação no tracker:** a Issue não foi fechada por esta alteração. O texto acima é uma proposta e
depende da aprovação do PO e da criação ou vinculação das Issues de follow-up.

## Escopo implementado

- Criação exige um líder com papel `STUDENT` e um orientador obrigatório com papel `PROFESSOR`.
- Toda proposta nova inicia como `PROPOSED` (rascunho); outros estados enviados na criação são rejeitados.
- Atualização que altera o estado é rejeitada enquanto a SPEC não definir as transições permitidas.
- Violações de regra retornam `400` em Problem Details (RFC 9457).
- Criação, edição e exclusão registram eventos append-only em `public.audit_event`, na mesma transação.
- O snapshot de auditoria contém somente tipo e estado. `actorId` é `null`, pois o backend ainda não fornece principal autenticado para esse fluxo.

## Evidências e mocks

`ProjectServiceTest` foi criado para testar as regras de serviço com Mockito. Ele simula:

- `ProjectRepository`, para verificar persistência e rejeição antes de salvar;
- `UserService`, para fornecer usuários sintéticos com papéis definidos;
- `AuditEventService`, para verificar os eventos solicitados pelo serviço.

Esses testes isolam a lógica; não comprovam persistência ou autorização reais.

`ProjectCrudIntegrationTests` usa Testcontainers/PostgreSQL. Os cenários escritos cobrem criação,
validação dos papéis, transição recusada sem alteração do estado e consulta dos eventos de auditoria
de criação, edição e exclusão. Esses testes não foram executados nesta entrega: o ambiente disponível
usa JDK 17, enquanto o projeto requer Java 21, e os testes de integração também requerem Docker.

## Cobertura dos critérios

| Critério | Situação |
| --- | --- |
| Proposta nasce como rascunho e exige docente responsável | Implementado; unitário mockado e cenário de API escrito. |
| Papéis de líder e docente validados no servidor | Implementado; unitário mockado e cenário de API escrito. |
| Transição incompleta/inválida falha | Mudanças de estado são recusadas enquanto a SPEC não definir transições; teste unitário e cenário HTTP escritos. |
| Usuário sem escopo recebe `403` sem vazamento | Não implementado; depende de identidade e RBAC server-side. |
| Estado e auditoria persistidos atomicamente | Implementação participa da mesma transação; falta executar integração real. Invariantes de concorrência dependem de DB-008. |
| Alteração crítica gera evento | Eventos INSERT/UPDATE/DELETE implementados; integração com PostgreSQL escrita, mas não executada. A autoria do evento não é atribuída até existir usuário autenticado. |
| Publicação, catálogo, tipo e duração | Não implementados; dependem de contrato e modelo de dados aprovados. |

## Dependências e limitações

- `SPEC-PI-001`: encontrada como Draft; deixa pendentes as transições de reabrir/arquivar e a lista de campos editáveis.
- `CONTRACT-PI-001`: não localizado no catálogo consultado; payloads e operações ainda precisam ser confirmados.
- `DB-008`: Issue aberta no repositório de Database; há decisões de modelo, publicação e integridade que precisam coincidir com as entidades do backend.
- `BE-004`/`BE-010`: autenticação e RBAC ainda não entregues/aprovados; sem eles não é possível implementar isolamento por usuário/equipe nem demonstrar `401`/`403` reais.
- `QA-011`: a evidência disponível é mockada e declara que a integração HTTP real depende de BE-007/008.
- `mvn verify`: não validado após as alterações por falta de JDK 21. `spec-compliance` não está disponível neste checkout.

Não habilitar as rotas CRUD genéricas em produção como substituto de autorização. A publicação de
rascunhos e o isolamento por equipe devem permanecer bloqueados até que contrato, schema e RBAC
definam o comportamento completo.

## Próximos passos para concluir

1. Aprovar as lacunas de `SPEC-PI-001` e publicar `CONTRACT-PI-001`.
2. Alinhar entidades e migrations com DB-008, incluindo tipo/duração e visibilidade.
3. Integrar identidade e política RBAC aprovada para autorizar docente por escopo e negar acesso sem vazamento.
4. Executar `mvn verify` com JDK 21 e Docker, validar `spec-compliance` e registrar evidências reais.
5. Vincular o PR com `Closes #7` somente após cumprir os critérios restantes e obter revisão.