# Tokens de recuperação de senha — AUTH-10-BE

## Limite da entrega

Esta entrega implementa o ciclo de vida do token, a configuração Spring, o
limite de emissão e o serviço de reset. A persistência relacional, o hash
Argon2id, a revogação real das sessões e o adaptador de envio continuam sendo
portas dos módulos/issues dependentes (`AUTH-10-DB` e `BE-005`).

O pacote público é `plataforma.auth.passwordrecovery`. A configuração usa o
repositório em memória apenas no perfil `test`; fora dele espera o adapter JPA
da tabela entregue por `AUTH-10-DB`.

## Garantias

- Cada token é gerado com `SecureRandom`, usando no mínimo 32 bytes de entropia.
- O valor bruto só existe no retorno imediato de `issueForUser` para a porta
  de entrega. O objeto persistido contém somente SHA-256 do token.
- A emissão de um token novo revoga tokens ativos anteriores do mesmo usuário.
- `validate` é uma operação de leitura; `consume` faz a transição atômica para
  usado e deve ser chamado pela operação que conclui a recuperação.
- Tokens vencidos, usados, revogados ou inexistentes não são aceitos.
- A resposta de `PasswordRecoveryRequestService.request` não informa se a
  conta existe. Há limite de três emissões por usuário em uma hora e a entrega
  ocorre fora da chamada; falhas são absorvidas. O adaptador de entrega não deve colocar o token em logs,
  URLs ou telemetria.
- O token guarda somente `userId`; o e-mail é usado apenas para localizar a
  conta e para a entrega.
- `PasswordResetService.reset` consome o token, grava o hash recebido por
  `PasswordHasher`, revoga sessões e invalida os demais tokens do usuário em
  uma transação.
- O prazo é uma política injetada (`PasswordRecoveryTokenPolicy`), evitando
  cristalizar no backend um prazo de produto ainda não aprovado.

## Evidência

`PasswordRecoveryTokenServiceTests` cobre token opaco, armazenamento por hash,
identidade por UUID, expiração no instante-limite, revogação por nova emissão,
revogação explícita, uso único concorrente, limite de emissão, falha de entrega,
reset e solicitação neutra para conta inexistente. `PlataformaDigitalApplicationTests`
confirma a resolução do bean no contexto Spring com o perfil `test`.

O contrato HTTP e o SQL de handoff estão em
`docs/auth/password-recovery-openapi.yaml` e
`docs/auth/password-recovery-tokens.sql`. Ambos permanecem provisórios até a
decisão do PO e a entrega de `AUTH-10-DB`.
