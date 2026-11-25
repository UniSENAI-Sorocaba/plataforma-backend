# Evidências — AUTH-10-BE

## Rastreabilidade

- Issue funcional: `AUTH-10`
- Entrega backend: `AUTH-10-BE`
- Dependência de integração: `AUTH-10-DB`
- Base da branch: `origin/develop` em `421512e`
- Branch de trabalho: `feat/AUTH-10-BE-password-recovery-tokens`

## Critérios verificados

| Critério | Evidência automatizada |
| --- | --- |
| Geração de token opaco e identidade por UUID | `PasswordRecoveryTokenServiceTests#generatesOpaqueTokenAndStoresOnlyItsDigestAndUserId` |
| Validação e expiração | `#validatesUntilTheExactExpirationInstant` |
| Uso único | `#consumesTokenOnlyOnceEvenWhenRequestsRace` |
| Revogação por nova emissão e explícita | `#newIssuanceRevokesPreviousActiveTokenForTheSameSubject`, `#explicitRevocationPreventsConsumption` |
| Revogação por usuário e retorno seguro ao reset | `#consumptionReturnsUserIdAndRevocationCanTargetTheUser` |
| Não enumeração no pedido | `#requestServiceDoesNotRevealWhetherTheAccountExists` |
| Limite, entrega resiliente e reset | `#requestServiceStopsAfterThreeIssuancesPerHour`, `#deliveryFailureDoesNotEscapeTheRequest`, `#resetHashesTheNewPasswordAndRevokesSessionsAndOtherTokens` |
| Proteção contra vazamento no modelo | `#generatesOpaqueTokenAndStoresOnlyItsDigest` |

## Comandos

- `mvn test -q`: PASS — 12 testes da funcionalidade e o teste de contexto do Spring.
- `mvn verify -q`: PASS.
- `npm run spec-compliance`: não executável no commit-base — não existe
  `package.json` nem script `spec-compliance`. Esta pendência deve ser resolvida
  pela infraestrutura de contratos antes do merge; não foi ocultada nem tratada
  como PASS.

## Risco e rollback

O repositório em memória é somente um adapter de teste. O adapter JPA e o SQL
de handoff foram preparados, mas a migration durável, o índice por hash e a
integração com sessão/Argon2id devem ser fornecidos ou aprovados por
`AUTH-10-DB`/`BE-005` antes de produção. O rollback desta entrega é o revert do
PR; nenhuma migration foi aplicada ao banco externo.
