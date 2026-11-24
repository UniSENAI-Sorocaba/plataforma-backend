# BE-010 — avaliação de prontidão

Issue: [BE-010 / #10](https://github.com/UniSENAI-Sorocaba/plataforma-backend/issues/10).
Responsável: `PedroDias010`. Revisor indicado: João.
Branch: `feat/BE-010-rbac`, criada a partir de `origin/main`.
Consulta das dependências: 2026-10-28.

## Resultado: preparação parcial; integração e conclusão bloqueadas

A própria issue exige uma matriz aprovada e declara: “Bloqueado até aprovação
de escopos e perfis”. Na baseline fixada, SPEC-AUTH-003 permanece Draft e
remete a aprovação da matriz à PG-006. Não foram criados perfis, permissões,
claims JWT, modelos de escopo ou regras de acumulação de papéis por inferência.

Referências normativas: `.governance-baseline`, MH-SEC-002,
`_KB/governance/definition-of-ready.md` e
`_KB/agents/workflows/implement-task.md` no repositório governance.
Este último exige: “Interromper se houver ambiguidade bloqueante”.
O usuário autorizou explicitamente adiantar as partes independentes e relatar
as pendências na PR. Essa autorização permite a preparação técnica abaixo;
não aprova uma matriz de perfis/escopos ou promove a SPEC Draft a norma.

## Dependências verificadas no GitHub

| Dependência | Estado consultado | Responsável | O que precisa ser resolvido |
| --- | --- | --- | --- |
| [PG-006](https://github.com/UniSENAI-Sorocaba/plataforma-governance/issues/6) / SPEC-AUTH-003 | Issue aberta; SPEC Draft na baseline | FellipeRaszejas | Aprovar matriz de papéis × ações × recursos, escopos e quem atribui/remove papéis |
| [BE-004](https://github.com/UniSENAI-Sorocaba/plataforma-backend/issues/4) | Aberta | JoaoVictor015qzn | Disponibilizar autenticação e claims aprovadas; o bootstrap atual não contém esse fluxo |
| [DB-006](https://github.com/UniSENAI-Sorocaba/plataforma-database/issues/6) | Aberta | NicolasPureza | Implementar relações/constraints e escopos após aprovação; a issue também proíbe inferir o esquema pelos nomes dos perfis |
| QA-009 | Referenciada na BE-010 | Não verificado nesta consulta | Coordenar casos allow/deny por perfil e rota após definição da matriz |
| Baseline oficial | Tag governance-v0.1.0 não retornada pela consulta remota | Governança | Publicar/aprovar a baseline pelo processo oficial; não alterar o SHA local para contornar o gate |

Os estados acima são uma fotografia da consulta, não substituem o GitHub
Project como fonte do estado operacional. Issue aberta não comprova ausência
de implementação em uma branch externa; o código disponível em main foi
verificado separadamente.

## Código disponível em main

O backend permanece no bootstrap Java 21 / Spring Boot 3. Há aplicação,
delimitação documental dos módulos e Actuator configurado para health/info,
com probes habilitadas. Não há controllers de negócio, fluxo de autenticação,
persistência de permissões ou dependência Spring Security no pom atual.

A BE-003 permanece em branch/PR separada. Seus arquivos não foram copiados
para esta branch, evitando acoplar esta entrega a uma PR ainda não integrada.

## Definições necessárias para retomar

1. Matriz versionada aprovada, com cada perfil, ação, recurso e escopo.
2. Regras aprovadas de acumulação de papéis, atribuição/remoção e conta sem papel.
3. Comportamento quando a fonte de permissões estiver indisponível ou uma
   atribuição mudar durante uma sessão.
4. Contrato de identidade autenticada/claims e origem das permissões, alinhado
   com BE-004 e DB-006.
5. Inventário das rotas públicas/protegidas e critérios para recursos fora do
   escopo do usuário.

Após resolver essas definições, a BE-010 deve implementar autorização no
servidor, separar 401/403, testar allow/deny e tentativas de adulteração do
cliente, e impedir que novas rotas protegidas fiquem fora da matriz.

## Implementado nesta preparação

Foi adicionada a autorização HTTP com interceptor real, catálogo explícito de
rotas, gate de cobertura no startup, Principal do servidor, políticas substituíveis,
erros RFC 9457, métricas e auditoria por porta. Consulte `../authorization/README.md`.
JWT/banco/matriz institucional permanecem nas dependências externas; não existem
permissões institucionais inventadas. O harness inicial não é mais a única evidência.

`mvn verify` valida a preparação e o bootstrap; os resultados estão em
`target/surefire-reports/` e `target/site/jacoco/`. O relatório
`../compliance/BE-010.md` mantém o resultado normativo FAIL enquanto as
dependências não forem resolvidas. Não se declara conclusão da task.
