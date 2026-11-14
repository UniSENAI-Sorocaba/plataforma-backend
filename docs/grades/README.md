# BE-015 — Rubricas e notas

Esta entrega implementa o núcleo de rubricas versionadas e cálculo determinístico.

Cada versão congela escala, fórmula, arredondamento, critérios, limites e pesos. Uma versão
existente não é atualizada; a criação de uma nova versão recebe o próximo número. O motor usa
`BigDecimal` e, provisoriamente, a fórmula `WEIGHTED_AVERAGE` com `HALF_UP` quando a política é
explicitamente configurada pela API. A aprovação institucional da escala/fórmula/arredondamento
continua pendente na `SPEC-PI-003`.

O lançamento de nota exige:

- banca em `IN_EVALUATION`;
- avaliador professor ativo e alocado na banca;
- identidade autenticada igual ao avaliador informado;
- exatamente os critérios da versão usada, todos dentro dos limites;
- auditoria append-only do lançamento, alteração ou exclusão.

Quando todos os avaliadores alocados enviam uma avaliação, a média consolidada da banca é
recalculada dentro da transação.

## Endpoints provisórios

- `POST /api/v1/rubrics`: cria rubrica e versão 1;
- `POST /api/v1/rubrics/{rubricId}/versions`: cria uma versão imutável;
- `GET /api/v1/rubrics/{rubricId}/versions/{version}`: consulta um snapshot;
- `POST /api/v1/evaluations`: aceita `rubricVersionId` e `criterionScores` para o fluxo versionado.

O contrato local foi atualizado em `contracts/openapi.yaml`; o contrato oficial `CONTRACT-PI-001`
ainda não está publicado na baseline. O SQL para o time de banco está em
`docs/grades/rubric-calculation.sql` e não deve ser aplicado diretamente pelo backend.

## Limitações registradas

`BE-011` e `BE-016` ainda precisam fornecer a matriz completa de autorização e as regras formais
de alocação/consolidação. A implementação aplica a autorização mínima do avaliador e mantém a
política de fórmula substituível para não esconder a decisão pendente do PO.
