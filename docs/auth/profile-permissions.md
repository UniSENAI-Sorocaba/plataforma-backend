# Perfil e permissões do usuário autenticado — AUTH-08-BE

Issue: [AUTH-08-BE / #33](https://github.com/UniSENAI-Sorocaba/plataforma-backend/issues/33), funcional
`AUTH-08` (governança #21). Contrato: [`contracts/auth-profile.openapi.json`](../../contracts/auth-profile.openapi.json).

## O que esta entrega faz

A issue pede disponibilizar **no contrato da API** os dados de perfil e permissões que o frontend precisa para
redirecionar por perfil (AUTH-08-FE-02) e exibir menus por perfil (AUTH-08-FE-03). Esta entrega é o contrato,
escrito antes do código (ADR-009), e **já pode ser consumida pelo mock**:

```bash
npm run contracts:bundle
npm run contracts:mock -- target/contracts/auth-profile.openapi.json
curl -H "Authorization: Bearer qualquer-valor" http://127.0.0.1:4010/api/v1/auth/me
```

O mock devolve o exemplo sintético com 200, 401 sem credencial e 403/503 com `Prefer: code=403` ou
`code=503`. Ele não valida token nem papel: serve apenas para o frontend desenvolver as telas.

## A rota

`GET /api/v1/auth/me`, protegida por `bearerAuth`, sob o mesmo prefixo `/api/v1/auth` do login (BE-004).

```json
{
  "id": "3f1c0a52-8b0e-4c61-9d0f-2f2d8a5c7e11",
  "fullName": "Aluno Sintético",
  "role": "STUDENT",
  "permissions": ["projects:read", "projects:create"]
}
```

| Campo | Para que serve | Origem |
| --- | --- | --- |
| `id` | identificar o usuário | `subject` do access token |
| `fullName` | nome no cabeçalho da área autenticada | `auth.user_account.full_name` |
| `role` | redirecionar após o login (FE-02) | claim `role` do token / `auth.user_account.role` |
| `permissions` | exibir só as opções permitidas (FE-03) | política institucional, ver abaixo |

Erros: 401 `UNAUTHENTICATED` (com `WWW-Authenticate: Bearer`), 403 `FORBIDDEN` e 503
`AUTHORIZATION_UNAVAILABLE`, todos RFC 9457 com `correlation_id`, iguais aos que a autorização da BE-010 já
devolve. A resposta usa `Cache-Control: no-store`, por conter dados pessoais.

## Decisões tomadas

1. **Sem e-mail no perfil.** O frontend não precisa dele para menus nem para redirecionamento, e as diretrizes de banco
   da governança pedem minimização de dados pessoais. Se o design precisar do e-mail, é uma adição compatível.
2. **Um único `role`.** Cada usuário tem um perfil (`STUDENT`, `PROFESSOR`, `COORDINATOR`, `CPA`, `ADMIN`), como
   no `auth.user_account` e no claim do token do PR #46.
3. **`permissions` como lista de ações do catálogo.** São os mesmos identificadores `recurso:ação` que a BE-010
   usa no campo `action` do catálogo de rotas. Assim o frontend e o servidor falam da mesma coisa. O frontend
   deve tratar uma ação ausente como negada, e **esconder uma opção não substitui a autorização do servidor**,
   que continua valendo em cada rota.
4. **Arquivo próprio em `contracts/`**, como o contrato de impacto (BE-019), sem alterar o `openapi.yaml` da BE-003.

## O que NÃO está feito, e por quê

O **endpoint ainda não existe no código**. Os motivos são dependências reais, não escolha:

| Dependência | Situação | Efeito |
| --- | --- | --- |
| Login e token (BE-004) | PR #46 aberto; não há código de login na `develop` | sem token, não há identidade para devolver |
| Matriz de perfis e ações (SPEC-AUTH-003, PG-006) | Draft, não aprovada | os valores de `permissions` por perfil não podem ser inventados |
| Política institucional na BE-010 | só nega: exige exatamente uma política por ação, e zero ou várias negam | qualquer rota nova responderia 403 |
| `AUTH-08` funcional (governança #21) | aberta e sem responsável; depende de AUTH-02 e AUTH-07 | critérios de aceite genéricos |

Quando as duas primeiras existirem, a implementação é pequena: classificar a rota no catálogo
(`src/main/resources/authorization/`), ler o usuário pelo `subject` e montar `permissions` a partir da política.

## Critérios de aceite da issue

| Critério | Situação |
| --- | --- |
| A funcionalidade está descrita e validada conforme o contrato vigente | **Feito:** contrato OpenAPI 3.1 validado por lint, bundle, segurança e exemplos, mock e comparação com a `develop` (`npm run contracts:verify`, 15 testes, e `contracts:breaking`) |
| As issues dependentes necessárias foram concluídas | **Pendente:** BE-004, PG-006 e AUTH-08 (ver tabela acima) |
| Evidência funcional e técnica anexada | **Parcial:** só a técnica (contrato e mock); a funcional exige o endpoint |

A issue **não deve ser fechada** com esta entrega.

## Perguntas para o PO e o frontend

1. O frontend precisa do e-mail ou de outro dado (matrícula, curso) no cabeçalho?
2. Basta o `role` para escolher a tela inicial, ou o backend deve devolver também a rota inicial por perfil?
3. O formato `recurso:ação` atende aos menus ou o frontend prefere agrupar permissões por módulo?
