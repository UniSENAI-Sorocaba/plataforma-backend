# Tokens de recuperação de senha — AUTH-10-BE

## Limite da entrega

Esta entrega implementa o ciclo de vida do token: geração, validação,
expiração, consumo único e revogação. Ela não implementa conta de usuário,
alteração da senha, envio de e-mail ou persistência relacional; essas são
fronteiras dos módulos/issues dependentes (`AUTH-10-FE` e `AUTH-10-DB`) e são
conectadas pelas portas públicas do pacote `auth.passwordrecovery`.

O commit-base ainda não contém `CONTRACT-AUTH-001` nem um endpoint AUTH. Por
isso, a entrega não inventa uma rota HTTP: o contrato local desta mudança é a
API de aplicação `PasswordRecoveryTokenService` e suas portas de integração.

## Garantias

- Cada token é gerado com `SecureRandom`, usando no mínimo 32 bytes de entropia.
- O valor bruto só existe no retorno imediato de `issueForSubject` para a porta
  de entrega. O objeto persistido contém somente SHA-256 do token.
- A emissão de um token novo revoga tokens ativos anteriores do mesmo sujeito.
- `validate` é uma operação de leitura; `consume` faz a transição atômica para
  usado e deve ser chamado pela operação que conclui a recuperação.
- Tokens vencidos, usados, revogados ou inexistentes não são aceitos.
- A resposta de `PasswordRecoveryRequestService.request` não informa se o
  sujeito existe. O adaptador de entrega não deve colocar o token em logs,
  URLs ou telemetria.
- O prazo é uma política injetada (`PasswordRecoveryTokenPolicy`), evitando
  cristalizar no backend um prazo de produto ainda não aprovado.

## Evidência

`PasswordRecoveryTokenServiceTests` cobre token opaco, armazenamento por hash,
expiração no instante-limite, revogação por nova emissão, revogação explícita,
uso único concorrente e solicitação neutra para conta inexistente.
