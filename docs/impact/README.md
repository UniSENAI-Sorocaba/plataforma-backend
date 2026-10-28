# Preparação da consulta de publicação de impacto — BE-019

Issue: [BE-019 / #19](https://github.com/UniSENAI-Sorocaba/plataforma-backend/issues/19).
Branch `feat/BE-019-impact-publication`, criada a partir de main. Esta entrega
implementa uma seleção defensiva de referências publicáveis, isolada do runtime.
Não é a API pública de impacto completa nem define os campos institucionais.

## O que foi implementado

O pacote público Portal `br.senai.plataformadigital.portal.api.impact` contém:

- `ImpactVersion`: referências opacas de impacto, versão e origem interna;
  origem ausente/vazia não pode representar uma versão/rascunho.
- `ImpactApproval`: vínculo à identidade da versão aprovada, fornecido por
  adapter confiável; não representa uma aprovação autorizada por si só.
- `ImpactPublicationSnapshot`: fotografia interna de versão, aprovação,
  publicação explícita e retirada. Não contém métricas ou dados pessoais.
- `ImpactSnapshotReader`: porta para a fotografia persistida selecionada pelo
  fluxo de publicação futuro.
- `PublicImpactQueryService`: retorna somente uma referência de impacto/versão
  se houver publicação explícita, aprovação exatamente da mesma versão e
  ausência de retirada. Também rejeita resposta do reader com outro impactId.
- `PublicImpactReference`: resultado técnico sem origem ou metadados de aprovação;
  não é o DTO público final de CONTRACT-PORTAL-001.

Não há bean automático, endpoint, modelo de banco ou implementação de autorização.
Uma aprovação não torna um rascunho público. O flag de retirada domina a seleção,
mesmo quando a fonte ainda informa published=true. `withdraw()` produz uma cópia
retirada, preservando versão/origem/aprovação na memória e sem alterar o snapshot
original. Não prova persistência, histórico durável ou auditoria append-only.

Cada consulta lê novamente a porta e não reutiliza uma seleção anterior em cache
próprio. Falha da fonte ou retorno null indevido interrompe a consulta com erro
genérico, sem retornar dado antigo ou mensagem privada da fonte. Isso não define
HTTP, observabilidade, retries ou política operacional de produção.

As portas devem ser ligadas somente a fontes confiáveis no servidor. Campos de
aprovação/publicação recebidos do cliente não podem ser usados para construir
o snapshot. A implementação DB-015 deve garantir que uma versão identifica
conteúdo/origem imutáveis e que os vínculos de aprovação/publicação são coerentes.

## Testes e cobertura

```bash
mvn clean verify
```

Fixtures sintéticas em `src/test/resources/impact/publication-fixtures.json`
exercitam rascunho, aprovado sem publicação, publicação sem aprovação, publicação
aprovada, retirada, aprovação de outra versão, aprovação de outro impacto e
resposta da fonte com um impacto diferente do solicitado.

Os testes também cobrem retirada sem perda das referências na memória,
reconsulta da fonte, indisponibilidade sem reaproveitar dados antigos,
obrigatoriedade de origem e serialização da referência de saída sem origem ou
aprovação. Esse teste de formato local não substitui contrato OpenAPI aprovado.
O contexto Spring confirma que a consulta não é ativada automaticamente.

JaCoCo publica HTML/XML/CSV em `target/site/jacoco/` e bloqueia `mvn verify`
abaixo de 90% de linhas e 80% dos branches do código de impacto, por se tratar
de regra core (POL-QA-001). Não substitui o gate global do backend. O workflow
`impact.yml` executa os testes e publica evidências e o relatório pendente.
Job técnico verde não equivale a aprovação normativa ou autorização de merge.

## O que falta e dependências

| Trabalho restante | Dependência |
| --- | --- |
| Aprovar métricas/campos, origem, responsável/período, critérios de publicação/retirada e dados públicos versus internos | PG-012 / SPEC-PORTAL-002 (Draft) e CONTRACT-PORTAL-001 ausente |
| Persistir versões/conteúdo/origem e vínculos de aprovação/publicação, com histórico e concorrência/transação adequados | DB-015 |
| Implementar comandos autorizados de criar, revisar, aprovar, publicar e retirar | Continuação BE-019, com BE-010/identidade e matriz aprovadas |
| Restringir consulta de origem a auditor autorizado | BE-019 / MH-SEC-002 e política aprovada de acesso |
| Gravar ator, ação, alvo, instante, correlação e resultado com retenção/imutabilidade aprovadas | BE-016 |
| Implementar DTOs/rotas reais, erros RFC 9457 e contract tests | CONTRACT-PORTAL-001 e infraestrutura BE-003 |
| Invalidar caches de servidor/CDN/cliente após retirada ou alteração de permissão | PG-012 e integração frontend/Cloud |
| Validar publicação/retirada ponta a ponta, origem/responsável/período e ausência de dados privados | QA-022 e testes integrados da BE-019 |
| Resolver baseline oficial e obter revisão/aceite | Governança e Victor, revisor da task |

Na consulta de 2026-10-28: PG-012 estava aberta e atribuída a FellipeRaszejas;
DB-015 a cortevinicius; BE-016 a lucasmiguelleite. Issue aberta não prova ausência
de implementação em branch externa; na main consultada há apenas o bootstrap.
QA-022 é uma referência da issue, sem atribuição confirmada nesta consulta.

Permanecem indefinidos fluxo de reapreciação/republicação, edição de conteúdo já
aprovado, granularidade dos períodos e campos de origem. Esta preparação não
escolhe esses comportamentos por inferência. Também não presume que todos os
campos de uma versão possam ser expostos por estarem tecnicamente aprovados.

As PRs BE-003 e BE-010 continuam separadas: nenhum código dessas branches foi
copiado para criar dependência não integrada. Esta proposta de API interna pode
precisar de ajustes após aprovação dos contratos. Não alterou SPEC ou baseline.
Rollback: revert da preparação; não há migration, alteração de dados ou exposição
de endpoint de impacto em produção.
