# Autorização HTTP — BE-010

O código contém núcleo, integração HTTP, catálogo de rotas e verificação de
cobertura, não apenas um harness. Nenhuma matriz institucional Draft foi
promovida a norma. Interfaces públicas estão em auth.api.authorization;
implementação HTTP em auth.web.

## Proteção real

AuthorizationConfiguration é carregada pela infraestrutura de auto-configuration
do Spring Boot. O provider padrão obtém a identidade do Principal do servidor;
não interpreta JWT sem o autenticador BE-004 e ignora papéis/subject em body,
query ou headers arbitrários. O serviço distingue ALLOWED, UNAUTHENTICATED,
DENIED e UNAVAILABLE. Somente ALLOWED libera o controller.

AuthorizationInterceptor protege todos os HandlerMethods da aplicação. O catálogo
precisa declarar cada método/rota. Uma rota desconhecida é negada; na inicialização,
qualquer controller da aplicação sem classificação impede o startup. Métodos HTTP
implícitos/irrestritos não passam no gate. HEAD usa a regra GET quando apropriado.
Actuator e handlers técnicos do framework não são controllers de negócio.

O interceptor retorna ProblemDetail RFC 9457 sanitizado: 401 para identidade
ausente, 403 para negação e 503 para falha das fontes. Usa no-store, correlação
validada e WWW-Authenticate em 401. Erros privados, tokens e payloads não são
copiados para respostas ou eventos.

## Catálogo técnico

Cada módulo pode incluir arquivos em src/main/resources/authorization/*.json.
O catálogo é técnico: não atribui papéis nem aprova regras institucionais.

```json
[
  {"method":"GET","path":"/api/v1/example","publicAccess":true},
  {"method":"GET","path":"/api/v1/scopes/{scope}/records/{id}","publicAccess":false,"action":"example:read","resourceVariable":"id","scopeVariable":"scope"}
]
```

Rotas protegidas declaram action e exatamente uma origem para resource: valor
fixo resource ou variável resourceVariable. scopeVariable é opcional, mas, se
declarada, precisa existir no padrão da rota. As variáveis vêm dos atributos
resolvidos pelo Spring, nunca de query/body que tentem sobrescrevê-las.
Valores vazios, chaves desconhecidas, regras duplicadas ou classificação ambígua
falham na configuração. Cada método HTTP deve ser explícito.

O catálogo não contém perfis institucionais hardcoded. AuthorizationPolicy pode
ser fornecida pelo adapter institucional. Sem ela, o dispatcher considera as
AuthorizationPolicyContribution dos módulos e exige exatamente uma responsável
pela ação. Zero ou múltiplas contribuições negam acesso. Isso permite ligar
políticas de domínio sem importar seus internals ou instalar permissões globais.
O módulo continua responsável por consultar vínculos confiáveis/aprovados.

## Auditoria e métricas

AuthorizationAudit recebe método, padrão de rota, ação, identidade autenticada,
decisão e correlação. Não recebe token, body ou URI concreta com identificadores.
É uma porta para BE-016; o padrão sem adapter não persiste auditoria. Se um adapter
instalado falhar, o acesso é bloqueado com 503. A identidade do evento é a mesma
avaliada pelo serviço, não uma segunda interpretação de campos do cliente.

authorization.decisions usa somente o outcome como tag, evitando cardinalidade
e dados pessoais em labels. A retenção/armazenamento central não é reimplementada.

## Testes e evidências

```bash
mvn clean verify
```

Além dos unitários, AuthorizationRuntimeTests importa controllers somente no
contexto de teste e usa o interceptor real. Cobre allow/deny, 401/403/503,
HEAD, recurso/escopo, campos forjados, falhas de política/auditoria, métricas e
correlação. AuthorizationRoutesTests demonstra que uma rota nova sem classificação
falha o gate. As fixtures não definem perfis de produção.

JaCoCo exige 90% de linhas e 80% de branches no núcleo e no adapter HTTP.
target/authorization inclui inventário de rotas e classificações; target/site/jacoco
e target/surefire-reports contêm as evidências publicadas pelo CI.

## Dependências externas restantes

- PG-006: aprovar perfis, ações, recursos, escopos e regras de atribuição/acumulação.
- DB-006: fornecer o modelo/vínculos persistidos para a política aprovada.
- BE-004: instalar Principal após validação institucional de identidade/JWT.
- BE-016: adapter de entrega/persistência central da auditoria.
- Governança: baseline oficial, contrato e revisão humana exigidos pela KB.

Filtros/adapters HTTP, classificação, gate, erros, métricas e testes não ficaram
como tarefas futuras. Adapters institucionais podem substituir os defaults,
que sempre negam acesso sem permissão. Integração de outras PRs deve incluir seu
catálogo de rotas; um domínio novo não recebe dispensa automática.

Rollback: revert da PR. Não há migration ou credencial versionada.
