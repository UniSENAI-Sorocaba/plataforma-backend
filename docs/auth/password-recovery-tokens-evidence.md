# Evidências — AUTH-10-BE

## Rastreabilidade

- Issue funcional: `AUTH-10`
- Entrega backend: `AUTH-10-BE`
- Dependência de integração: `AUTH-10-DB`
- Base da branch: `main` em `0b9d2c6`
- Branch de trabalho: `codex/auth-10-be-password-recovery-tokens`

## Critérios verificados

| Critério | Evidência automatizada |
| --- | --- |
| Geração de token opaco | `PasswordRecoveryTokenServiceTests#generatesOpaqueTokenAndStoresOnlyItsDigest` |
| Validação e expiração | `#validatesUntilTheExactExpirationInstant` |
| Uso único | `#consumesTokenOnlyOnceEvenWhenRequestsRace` |
| Revogação por nova emissão e explícita | `#newIssuanceRevokesPreviousActiveTokenForTheSameSubject`, `#explicitRevocationPreventsConsumption` |
| Revogação por sujeito e retorno seguro ao reset | `#consumptionReturnsNormalizedSubjectAndRevocationCanTargetTheSubject` |
| Não enumeração no pedido | `#requestServiceDoesNotRevealWhetherTheSubjectExists` |
| Proteção contra vazamento no modelo | `#generatesOpaqueTokenAndStoresOnlyItsDigest` |

## Comandos

- `mvn test -q`: PASS — 8 testes da funcionalidade e o teste de contexto do Spring.
- `mvn verify -q`: PASS.
- `npm run spec-compliance`: não executável no commit-base — não existe
  `package.json` nem script `spec-compliance`. Esta pendência deve ser resolvida
  pela infraestrutura de contratos antes do merge; não foi ocultada nem tratada
  como PASS.

## Risco e rollback

O repositório em memória é somente um adapter local/teste. A persistência
durável, o índice por hash e a transação de consumo devem ser fornecidos por
`AUTH-10-DB` antes de produção. O rollback desta entrega é o revert do PR;
nenhuma migration ou dado externo foi alterado.
