# Evidências BE-015

| Aceite | Evidência |
|---|---|
| cálculo determinístico | `RubricCalculatorTest.calculaMediaPonderadaComPrecisaoDecimalEArredondamentoDeterministico` |
| limites e critérios exatos | `RubricCalculatorTest.exigeExatamenteOsCriteriosDaVersao`, `rejeitaNotaForaDoLimiteDoCriterio` |
| pesos válidos | `RubricCalculatorTest.rejeitaSomaDePesosDiferenteDeUm`, `RubricServiceTest.rejeitaVersaoComPesosQueNaoSomamUm` |
| versionamento | `RubricServiceTest.criaPrimeiraVersaoERegistraAuditoria`, `proximaVersaoRecebeNumeroNovoSemAlterarAAnterior` |
| autorização do avaliador | `EvaluationServiceTest.bloqueiaAvaliadorDiferenteDaIdentidadeAutenticada` |
| cálculo integrado à avaliação | `EvaluationServiceTest.calculaNotaPelaVersaoPersistidaDaRubrica` |
| contrato e rotas | `OperationalContractTests`, `contracts/openapi.yaml`, `authorization/develop-routes.json` |

Validações finais esperadas: `mvn verify` e o gate `spec-compliance`. A baseline atual não publica
`CONTRACT-PI-001` nem uma decisão de PG-008 para fórmula/arredondamento; por isso a política
`WEIGHTED_AVERAGE`/`HALF_UP` desta entrega está documentada como provisória.
