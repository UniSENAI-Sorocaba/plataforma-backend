# API de impacto — BE-019

Issue: [BE-019 / #19](https://github.com/UniSENAI-Sorocaba/plataforma-backend/issues/19).
PR: [#40](https://github.com/UniSENAI-Sorocaba/plataforma-backend/pull/40).
Revisor: Victor. Esta branch contém a implementação do fluxo da task, e não
somente uma consulta de preparação. O código inicial de seleção isolada foi
substituído pelo módulo `portal.impact`.

## Fluxo implementado

1. Criar um rascunho com conteúdo textual e origem obrigatória, responsável e período.
2. Criar novas versões sem alterar versões anteriores ou sua aprovação.
3. Aprovar uma versão específica sem publicá-la automaticamente.
4. Publicar somente uma versão que tenha aprovação. Uma nova versão em rascunho
   não substitui a versão publicada anteriormente.
5. Consultar publicamente somente o conteúdo da versão efetivamente publicada.
6. Retirar a publicação sem apagar versões, aprovações ou histórico.
7. Consultar versões e histórico com paginação e origem com autorização específica.

As operações create/revise/approve/publish/withdraw são do escopo BE-019 e estão
implementadas. As interfaces de autorização e persistência também estão ligadas
ao fluxo. Sua realização com identidade/banco institucionais depende das entregas
externas descritas abaixo; isso não transfere a lógica desta task para outras áreas.

## Contrato HTTP local

`contracts/impact.openapi.json` versiona a proposta executável OpenAPI 3.1,
schemas 2020-12, segurança por operação, erros e headers. O teste de contrato
compara as rotas reais do Spring e valida respostas de sucesso/erro contra os
schemas com NetworkNT. Não substitui a aprovação de CONTRACT-PORTAL-001.

| Método e rota | Ação |
| --- | --- |
| POST /api/v1/impacts | Criar rascunho |
| POST /api/v1/impacts/{id}/versions | Criar versão |
| GET /api/v1/impacts/{id}/versions | Listar versões internas |
| POST /api/v1/impacts/{id}/versions/{version}/approval | Aprovar versão |
| PUT /api/v1/impacts/{id}/publication | Publicar a versão do body |
| DELETE /api/v1/impacts/{id}/publication | Retirar publicação |
| GET /api/v1/impacts/{id} | Consultar conteúdo publicado, sem origem |
| GET /api/v1/impacts/{id}/versions/{version}/provenance | Consultar rastreabilidade da versão |
| GET /api/v1/impacts/{id}/history | Consultar histórico de alterações |

O conteúdo público mínimo desta proposta contém `title` e `summary`. Não foram
inventadas métricas numéricas ou dados de beneficiários. Origem interna contém
`sourceReference`, `responsibleReference`, `periodStart` e `periodEnd`. Os campos
e limites locais estão sujeitos à revisão da PG-012; a baseline não foi alterada
e a SPEC continua Draft. O intervalo usa datas inclusivas, com início <= fim.

Exemplo sintético: `src/test/resources/impact/draft.json`. A saída pública contém
somente id, version, title e summary. Origem, responsável, período e atores de
aprovação não são serializados nas consultas públicas ou na listagem de versões.
Campos extras no body são rejeitados, inclusive tentativas de enviar approved,
actor ou permissões que não pertencem ao contrato.

As respostas de alteração incluem ETag com a revisão do agregado. Todas as
alterações após create exigem If-Match com esse ETag: ausência retorna 428,
formato inválido 400 e revisão desatualizada 412. O repository também executa
compare-and-set, evitando gravações concorrentes mesmo após a primeira leitura.
A segunda aprovação/publicação da mesma versão e retirada sem publicação ativa
retornam 409. Republicação de uma versão já aprovada exige novo comando autorizado.

Versões/histórico usam page (base zero, padrão 0) e size (1–100, padrão 25), com
items, page, size e totalElements. As versões são ordenadas por número e eventos
por revisão. Erros usam ProblemDetail RFC 9457 e correlação, sem payload interno.

Todas as respostas de impacto, inclusive erros e consultas públicas, usam
Cache-Control: no-store. X-Correlation-Id é validado por tamanho/caracteres ou
substituído por UUID; essa correlação acompanha os eventos das alterações.
O servidor não mantém cache de conteúdo publicado. O teste de retirada consulta
novamente o mesmo endpoint e verifica 404 imediatamente.

## Segurança e integração

ImpactAccess obtém o ator de um Principal autenticado pelo servidor; headers de
papel/ator, body e query não definem identidade. ImpactPermissionProvider decide
cada ação e alvo no servidor. Sem provider institucional, todas as permissões são
negadas: anônimo recebe 401 e identidade sem permissão recebe 403. Falha da fonte
de autorização resulta em 503 sem concessão. A consulta pública não exige login,
mas exige publicação da versão aprovada.

CREATE recebe alvo null porque o recurso ainda não existe; o adapter deve
autorizar a capacidade de criar. READ, REVISE, APPROVE, PUBLISH, WITHDRAW e AUDIT
recebem o id real para verificação de escopo. Não existem papéis institucionais
hardcoded, permissões de fallback ou autenticação por token sintético em runtime.
Origem e histórico exigem AUDIT antes da leitura, inclusive para recurso inexistente.

ImpactRepository persiste estado imutável, versões e eventos de alteração juntos.
Cada evento inclui impactId, versão, revisão, ação, ator, instante, correlação e
resultado SUCCESS. Uma gravação recusada/falha não acrescenta histórico. Esses
eventos não contêm conteúdo, origem, senha ou token. O adapter durável deve gravar
estado e outbox na mesma transação e encaminhar os eventos à BE-016, sem janela
de publicação sem registro. Denegações/retenção/consulta da auditoria central
seguem o mecanismo transversal da BE-016.

## Execução e testes

```bash
mvn clean verify
mvn spring-boot:run -Dspring-boot.run.profiles=impact-local
```

O perfil explícito impact-local utiliza InMemoryImpactRepository. Ele permite
testar versões/concorrência/histórico sem apropriar migrations da squad de banco.
Os dados se perdem ao reiniciar. Esse perfil não oferece login ou permissões:
as escritas continuam protegidas. Os testes de integração fornecem Principal e
provider de permissões somente no contexto de teste para executar o ciclo HTTP.

Fora desse perfil, ausência de adapter durável retorna 503 nas leituras e não
instala armazenamento em memória. Os testes do bootstrap verificam esse padrão,
401/403 nas rotas protegidas e preservação do healthcheck.

A suíte cobre fluxo HTTP completo, schemas de contrato, preservação de versões,
origem/responsável/período, negações/forja de identidade, retirada, concorrência,
falhas de gravação, paginação, erros sanitizados, overposting e indisponibilidade.
JaCoCo exige 90% das linhas e 80% dos branches de todo o módulo portal.impact.
Evidências: target/surefire-reports e target/site/jacoco. O workflow impact.yml
executa clean verify e publica esses relatórios, contrato, fixtures e compliance.

## Dependências externas

| Entrega externa | Task / responsável consultado | Ponto já pronto para integração |
| --- | --- | --- |
| Schema/migrations e regras persistentes de versões/origem | DB-015 / cortevinicius | ImpactRepository com create e replace atômicos por revisão, incluindo eventos |
| Autenticação institucional e Principal validado | BE-004 / JoaoVictor015qzn | ImpactAccess já usa Principal; não reimplementa emissor/validador de JWT |
| Matriz aprovada e dados de permissões/escopos | PG-006 / FellipeRaszejas e DB-006 / NicolasPureza | ImpactPermissionProvider recebe ator, ação e id; nega por padrão |
| Armazenamento/entrega central de auditoria, retenção e imutabilidade | BE-016 / lucasmiguelleite | Eventos completos e histórico por alteração, preparados para persistência/outbox |
| Aprovação dos campos públicos, origem, período e regras institucionais | PG-012 / FellipeRaszejas, owners do contrato | OpenAPI local e fluxo executável para revisar, sem promoção de Draft |
| Política de CDN/frontend coerente com retirada | PG-012 e responsáveis Cloud/frontend | Respostas no-store e retirada imediata no servidor já implementadas |
| Baseline oficial/publicação da tag | Governança | SHA mantido; sem contorno da validação normativa |

As tasks estavam abertas na consulta de 2026-10-28. A ausência do schema impede
escrever um adapter SQL fiel sem inventar tabelas da DB-015. A ligação do adapter
definitivo e os testes contra suas entregas devem ocorrer quando esses artefatos
existirem; o fluxo e os endpoints locais não ficaram como tarefas futuras.

Rollback: revert desta PR. Não há migrations ou credenciais versionadas. A
liberação institucional ainda exige resolver dependências e revisão de Victor.
