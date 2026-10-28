# Contratos executáveis — BE-003

Issue: [BE-003 / #3](https://github.com/UniSENAI-Sorocaba/plataforma-backend/issues/3).
Referências: ADR-008, ADR-009, diretrizes de API, MH-GOV-002 e MH-QA-002 da
baseline fixada em `../.governance-baseline`. Revisor da task: João.

## Escopo e autoridade

`openapi.yaml` é o contrato local, versionado em OpenAPI 3.1 com JSON Schema
2020-12, para os quatro endpoints operacionais existentes: health, liveness,
readiness e info. O link de descoberta `/actuator`, o fallback `/error` e os
comportamentos HTTP implícitos do framework não são APIs de domínio.

O YAML é a fonte editável; `target/contracts/openapi.json` é um bundle derivado
e não deve ser versionado ou editado. `info.version` identifica a versão deste
contrato local, não uma aprovação dos contratos globais. O endereço HTTPS
`api.example.test` é sintético: o cliente deve selecionar a URL do ambiente.

Todos os endpoints atuais são públicos, declarados com `security: []`. Não há
login, autorização ou recurso paginado implementado no bootstrap. O teste de
segurança usa uma operação **sintética**, sem criar endpoints de negócio, e
rejeita contratos protegidos sem security scheme, 401 e 403 com schema
`application/problem+json`. Ele verifica documentação, não implementação RBAC.

O Actuator utiliza seu formato operacional nativo para 200/503; não convertemos
suas probes em erros de domínio RFC 9457. Por esse motivo, o lint não exige uma
resposta 4xx artificial nesses endpoints. Erros RFC 9457, paginação, header de
correlação e detalhes de autenticação dos contratos de domínio dependem de
definição/aprovação dos produtores e consumidores. Nenhum desses padrões foi
inventado ou publicado como norma por esta implementação.

## Verificação local

Requisitos: Java 21, Maven 3.9+, Node.js 24.14+ da linha 24, npm, Git e `tar`.
As dependências npm são exclusivas de desenvolvimento, fixadas no lockfile e
instaladas sem scripts de terceiros. `oasdiff` 1.34.1 é baixado do release
oficial, tem SHA-256 verificado contra valores versionados e fica em `target/`.
O instalador aceita Windows/Linux/macOS em x64 e ARM64.

```bash
npm ci
npm run contracts:install-diff
npm run contracts:verify
npm run contracts:breaking -- origin/main
mvn verify
npm audit --audit-level=high
npm run spec-compliance
```

`contracts:verify` executa lint, bundle e testes determinísticos. Os testes
validam fixtures independentes contra schemas 2020-12 e exemplos da OpenAPI,
exercitam o mock por HTTP e demonstram que o gate rejeita remoção de endpoint,
nova entrada obrigatória e mudança de tipo. Comparações idênticas e alterações
de descrição permanecem compatíveis. Erros de execução/parsing do comparador
não contam como prova de breaking change: os testes exigem exit code 1 e
diagnósticos JSON não vazios.

`mvn verify` inicia o produtor Spring Boot em porta aleatória, verifica os
quatro casos de sucesso e força falhas reais de dependência, readiness e
liveness no contexto de teste para verificar os 503. Nenhum indicador sintético
é adicionado à aplicação de produção.
Um teste adicional compara as rotas de controllers da aplicação com o contrato
para detectar operações futuras implementadas sem documentação.

## Mock consumível

```bash
npm run contracts:bundle
npm run contracts:mock
curl http://127.0.0.1:4010/actuator/health
curl -H "Prefer: code=503, example=down" http://127.0.0.1:4010/actuator/health
curl -H "Prefer: code=503, example=outOfService" http://127.0.0.1:4010/actuator/health/readiness
```

O mock só escuta em `127.0.0.1:4010`. A variável `CONTRACT_MOCK_PORT` permite
escolher outra porta. Ele retorna exemplos estáticos do bundle, validando o
schema antes da resposta. `Prefer: code=..., example=...` seleciona um cenário;
sem preferência, retorna o primeiro exemplo 2xx. Operação ausente retorna 404;
status/exemplo inexistente retorna 400. Esses erros são diagnósticos do mock,
não contratos novos da aplicação. O mock não simula estado, persistência ou
autorização e não é um serviço de produção. Após editar o YAML, gere o bundle
novamente e reinicie o processo.

## Compatibilidade e CI

O workflow `.github/workflows/contracts.yml` roda em todos os PRs e pushes em
`main`, com permissões de leitura e Actions fixadas por SHA. Executa audit,
lint, testes do mock, testes negativos, comparação com o SHA imutável da base
do PR e `mvn verify`. Evidências são preservadas mesmo se um passo falhar.

O gate compara o contrato candidato com a versão no commit base, nunca com
uma cópia editável dentro da própria branch. Primeira publicação sem contrato
anterior é registrada explicitamente; uma referência Git inválida ou a remoção
de um contrato existente falha. `oasdiff --fail-on WARN` bloqueia mudanças
potencialmente incompatíveis e incompatíveis. Não há flag de bypass por simples
alteração de `info.version`: uma nova versão deve preservar o contrato anterior
e possuir plano de migração aprovado antes de adaptar o gate.

Os administradores precisam configurar os jobs `Contracts and producer tests`
e `Spec compliance` como required checks nas regras de proteção do repositório.
Criar o workflow, isoladamente, não configura a proteção da branch.
Governance é privado. Um administrador precisa configurar o Actions secret
`GOVERNANCE_READ_TOKEN` no backend (ou disponibilizá-lo via secret da organização),
com acesso a `UniSENAI-Sorocaba/plataforma-governance` e permissão `Contents: read`.
Um fine-grained token deve selecionar esse repositório e receber aprovação da
organização quando exigida. O `GITHUB_TOKEN` automático do backend não concede
acesso a outro repositório privado. Em PRs de forks, secrets podem não estar
disponíveis; nesses runs, a validação continua bloqueada.

O job de compliance não persiste a credencial do checkout, para não misturá-la
com o token de leitura da governança. Secret ausente produz uma mensagem
explícita e relatório FAIL antes da consulta remota. Falha de acesso não é
classificada como tag ausente. Depois de configurar o secret, reexecutar o job;
isso permite consultar a baseline, mas não publica a tag ou os contratos faltantes.
Localmente, as credenciais Git já configuradas são reutilizadas; nenhum token é
versionado ou incluído nos relatórios.

## Pendências para concluir a issue

- Publicar `governance-v0.1.0` apontando para o SHA já fixado; a baseline não foi alterada.
- Publicar/aprovar `CONTRACT-AUTH-001`, `CONTRACT-PI-001`, `CONTRACT-EXT-001` e
  `CONTRACT-PORTAL-001` e atualizar a baseline dos consumidores por PR, se necessário.
- Integrar os endpoints, erros, paginação, correlação e segurança desses contratos
  sem promover SPECs Draft por conta própria.
- Coordenar os testes do consumidor frontend/QA-010; o consumidor HTTP local é
  evidência da infraestrutura e não substitui homologação do frontend.
- Obter revisão de João, checks remotos e aprovações exigidas pela governança.

`spec-compliance` resolve tag/SHA e verifica a presença dos contratos na árvore
remota do commit fixado. Falhas retornam código 1. Esse gate específico da
BE-003 não implementa análise semântica universal de SPECs e nunca produz PASS
automaticamente: sem falhas determinísticas, retorna WARN para revisão humana.
O relatório atual deve permanecer FAIL enquanto as dependências faltarem.

## Evidências e rollback

- `target/contracts/openapi.json`: bundle de consumo.
- `target/contracts/mock-responses.json`: dez respostas consumidas por HTTP.
- `target/contracts/breaking-*.json`: incompatibilidades intencionais rejeitadas.
- `target/contracts/breaking.json`: comparação com a base Git.
- `target/contracts/spec-compliance.json`: baseline, pendências e resultado.
- `target/surefire-reports/`: resultados dos testes do produtor.
- `target/contracts/verification.log`: saída das verificações no CI.

O CI publica `contract-evidence` e `spec-compliance-report` por 14 dias. Não há
migration ou alteração do runtime de produção; o rollback desta infraestrutura
é o revert do PR. A issue não deve ser fechada enquanto as pendências acima
continuarem abertas.
