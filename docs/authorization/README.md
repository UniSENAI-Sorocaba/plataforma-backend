# Preparação técnica da autorização — BE-010

Esta entrega adianta código independente da matriz institucional. Não é RBAC
completo, não instala filtros e não protege endpoints de produção. A issue
[BE-010](https://github.com/UniSENAI-Sorocaba/plataforma-backend/issues/10)
continua bloqueada para conclusão pela aprovação de perfis/escopos e pelas
dependências registradas em `../tasks/BE-010-readiness.md`.

## API pública e responsabilidade das portas

O pacote `br.senai.plataformadigital.auth.api.authorization` pode ser utilizado
por futuros adapters e módulos através da API pública de Auth:

- `AuthenticatedSubjectProvider`: fornece uma identidade já autenticada pelo
  servidor; nunca ler subject, papéis ou permissões de campos arbitrários do cliente.
- `AuthorizationPolicy`: avalia explicitamente identidade, ação, recurso e escopo
  conforme a futura matriz aprovada e fontes confiáveis de permissões.
- `AuthorizationService`: consulta ambas as portas para cada decisão. Não possui
  cache ou concessões próprias; não combina papéis nem interpreta wildcards.
- `AuthorizationRequest`: contém somente alvo/ação/escopo. A identidade não faz
  parte da requisição de autorização e é obtida pelo provider separado.
- `AuthorizationDecision`: distingue ALLOWED, UNAUTHENTICATED, DENIED e UNAVAILABLE;
  somente ALLOWED passa em `allowsAccess()`.

Identidade ausente não consulta a política. Falha do provider/política, incluindo
retorno null indevido do provider, nunca concede acesso: retorna UNAVAILABLE sem
mensagem de exceção ou token. Falhas de programação por argumentos inválidos
são rejeitadas antes da consulta. Identificadores são opacos; não há enums de
perfis, normalização institucional ou tratamento especial de valores como `*`.

Scope ausente não concede acesso global: ainda exige true explícito da política.
Identidade inválida/não autenticada não pode ser representada como concessão pelo
serviço; isso exige que o futuro adapter cumpra o contrato do provider. O serviço
não valida JWT, assinatura ou claims por conta própria.

O resultado UNAVAILABLE é um mecanismo técnico de não concessão, não uma decisão
normativa sobre a resposta HTTP, retenção/auditoria, retries ou UX de falha da
fonte de permissões. Essas decisões permanecem para aprovação e integração.

## Testes e limites da evidência

```bash
mvn clean verify
```

Os testes unitários exercitam autorização explícita, negação, anonimato, falhas
das portas, argumentos inválidos, escopo ausente e reavaliação após uma mudança
na fonte de permissões.

`AuthorizationHttpHarnessTests` monta um controller **apenas em MockMvc**, com
identidades e política sintéticas. Demonstra allow 200, ausência de identidade
401, deny 403, troca de recurso/escopo e tentativas de forjar subject, papéis e
permissões por headers/query. Não testa login real nem a segurança de endpoints
de produção. Os payloads desse harness não constituem contratos de API.

O controller é marcado como TestComponent. Um teste do contexto Spring confirma
que nem as rotas `/__test/` nem o serviço de autorização são ativados
automaticamente na aplicação. Ao adicionar integração real, esse teste deverá
ser substituído por evidências dos filtros/adapters aprovados.
O mesmo teste exporta os padrões de rotas registrados pelo Spring, incluindo
Actuator e fallback de erro, para `target/authorization/route-inventory.json`.
Esse inventário técnico não classifica rotas públicas/protegidas nem aprova
permissões; padrões como health/** não representam uma matriz de endpoints.

JaCoCo gera HTML/XML/CSV em `target/site/jacoco/`. A regra desta entrega analisa
somente as classes da API de autorização e exige pelo menos 90% das linhas e
80% dos branches, conforme POL-QA-001. Não é um gate global de cobertura de todo
o backend; esse trabalho continua no escopo correspondente de qualidade.

O workflow `authorization.yml` executa build/testes/cobertura e publica as
evidências, incluindo o relatório de compliance pendente no resumo do run.
O sucesso desse job técnico não representa PASS de spec-compliance, aprovação
de matriz ou autorização de merge. O gate global de spec-compliance/QA-003
não é reimplementado neste PR.

## Integração restante

1. Aprovar a matriz em PG-006 / SPEC-AUTH-003, com vocabulário de ações, recursos,
   escopos, acumulação/atribuição de papéis e critérios de aceite.
2. Implementar o provider a partir da identidade validada por BE-004; rejeitar
   tokens adulterados/expirados e nunca aceitar identidade arbitrária do cliente.
3. Implementar a política usando o modelo DB-006 aprovado e suas relações de
   escopo; decidir cache, mudanças de atribuição e comportamento da fonte indisponível.
4. Instalar a proteção server-side real, com rotas públicas/protegidas explícitas,
   negação por padrão e tratamento HTTP 401/403 e erros RFC 9457 aprovados.
5. Inventariar todas as rotas reais e criar o gate que cobra cobertura de cada
   endpoint protegido pela matriz. O harness não substitui esse inventário.
6. Completar testes por perfil, ação, recurso e escopo com QA-009; ligar
   observabilidade/auditoria e validar ausência de dados sensíveis nos erros/logs.
7. Resolver baseline oficial e executar compliance contra normas aprovadas antes
   de promover a entrega a concluída.

Essas etapas são continuação da BE-010 ou integrações com suas dependências;
não foram declaradas prontas nesta preparação. Não há migration ou dado alterado.
Rollback: reverter o PR de preparação, sem operação de banco ou rotação de tokens.
