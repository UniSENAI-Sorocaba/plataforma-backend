# Mapa de módulos e dependências permitidas — BE-001

Este documento descreve como o código do backend está organizado hoje e quais dependências entre pacotes
são permitidas. As regras de dependência da seção 3 são verificadas por testes: se alguém violá-las, o
`mvn verify` falha.

## 1. Organização do código

O backend é um monólito (ADR-001/ADR-007: Java 21 e Spring Boot 3) com **um único processo** e **uma única
aplicação** (`plataforma.PlataformaDigitalApplication`). Dentro dele há três grupos de pacotes.

### 1.1 Camadas (`plataforma.*`)

Cada recurso de negócio é dividido por camada, com os mesmos sufixos em cada pasta:

| Pacote | Papel |
| --- | --- |
| `controller` | endpoints REST; recebem e devolvem DTOs |
| `service` | regras de negócio e transações |
| `repository` | acesso ao banco (Spring Data JPA) |
| `model` | entidades JPA, espelho das tabelas |
| `dto` | objetos de entrada e saída da API |
| `mapper` | conversão entre entidade e DTO |
| `exception` | exceções e tratamento global de erros (RFC 9457) |
| `config` | configurações do Spring (relógio, correlação, Swagger, verificação de credencial) |

### 1.2 Pacotes transversais de segurança (`plataforma.auth`, `plataforma.security`)

| Pacote | Conteúdo |
| --- | --- |
| `plataforma.auth` | hash de senha Argon2id (BE-005) |
| `plataforma.auth.passwordrecovery` | tokens de recuperação de senha (AUTH-10-BE) |
| `plataforma.security` | redação de segredos em logs, métricas e auditoria (BE-005) |

### 1.3 Pacotes legados (`br.senai.plataformadigital.*`)

Entraram com os PRs BE-010 e BE-019 e ainda usam o pacote raiz antigo:

| Pacote | Conteúdo | Como é ligado à aplicação |
| --- | --- | --- |
| `auth.api.authorization` e `auth.web` | autorização HTTP com negação por padrão (BE-010) | auto-configuration (`AutoConfiguration.imports`) |
| `portal.impact` | API de impacto com versões e aprovação (BE-019) | `@Import` em `plataforma.config.ImpactConfiguration` |

Como o Spring só varre `plataforma`, esses pacotes dependem dessas ligações explícitas. Unificar o pacote
raiz é uma pendência (seção 4).

## 2. Módulos de domínio

Os módulos do produto correspondem aos schemas do banco (ADR-002). Hoje o código **não** tem um pacote
por módulo: os recursos de um módulo ficam espalhados pelas camadas, e o schema do banco é o que os agrupa.

| Módulo | Schema | Recursos no código | Situação |
| --- | --- | --- | --- |
| `auth` | `auth` | `UserAccount`; hash de senha; recuperação de senha; autorização | autorização (BE-010) e hash de senha (BE-005) já estão na `develop`; login (BE-004) e refresh token (BE-006) ainda estão em PRs abertos |
| `academic-pi` | `academic_pi` | `Project`, `ProjectMember`, `ExaminationBoard`, `Evaluation` | CRUD básico; regras em BE-007, BE-008, BE-011 e BE-015 |
| `extension` | `extension` | `Notice`, `Enrollment`, `Activity`, `Certificate` | CRUD básico; regras em BE-009, BE-014 e BE-017 |
| `portal` | ainda sem schema (repositório em memória) | API de impacto (`br.senai.plataformadigital.portal.impact`) | BE-019; persistência depende de DB-015 |
| `assets` | `assets` | nenhum | Fase 2 (fora do MVP) |
| `institutional` | `institutional` | nenhum | Fase 2 (fora do MVP) |
| transversal | `public` | `AuditEvent` (`public.audit_event`, DB-001) | consulta de leitura; registro automático em BE-016 |

## 3. Dependências permitidas

A direção permitida é **controller → service → repository → model**, com `dto` e `mapper` ao lado:

```
controller ──> service ──> repository ──> model
    │             │
    └──> dto <────┘          mapper converte model <-> dto
```

| Regra | Teste que a verifica (`LayerDependencyTest`) |
| --- | --- |
| `controller` não acessa `repository` | `controllersDoNotReachRepositories` |
| `dto` não acessa `repository` nem `service` | `dtosDoNotReachPersistence` |
| `model` não depende de nenhuma outra camada | `modelDoesNotDependOnOtherLayers` |
| `repository` não acessa `service` nem `controller` | `repositoriesDoNotReachServicesOrControllers` |
| `service` não acessa `controller` | `servicesDoNotReachControllers` |
| `mapper` não acessa `controller`, `service` nem `repository` | `mappersOnlyConvertBetweenModelAndDto` |
| `exception` não acessa `controller`, `service` nem `repository` | `exceptionsDoNotReachApplicationLayers` |

Para verificar: `mvn -Dtest=LayerDependencyTest test`.

Regras de convivência que **ainda não** são verificadas por teste, mas valem na revisão de código:

- um módulo não acessa os detalhes internos de outro; a integração passa pelo `service` ou por contrato
  versionado (`contracts/`);
- entidades de `model` nunca saem pela API: a resposta é sempre um DTO;
- os pacotes legados (`br.senai.plataformadigital.*`) não são cobertos pelo `LayerDependencyTest`, que
  analisa apenas `plataforma`.

## 4. Pendências

1. **Estrutura por camada x por módulo.** A BE-001 pede um pacote por módulo (`auth`, `academic-pi`,
   `extension`, `assets`, `institutional`) e `architecture/components.md` da governança prevê módulos
   explícitos. A organização por camada foi uma escolha para facilitar a entrada do time. Pelo
   MH-ARCH-002, mudança arquitetural global relevante exige ADR: a decisão precisa ser aprovada pelos
   Technical Leads, que podem manter as camadas ou pedir a migração por módulo.
2. **Dois pacotes raiz.** `plataforma` e `br.senai.plataformadigital` convivem. Unificar evita ligações
   explícitas e deixa o `LayerDependencyTest` cobrir todo o código.
