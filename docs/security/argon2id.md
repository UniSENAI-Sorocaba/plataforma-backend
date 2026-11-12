# Argon2id: hashing, verificação e calibração

## Política de hashing

A aplicação gera hashes Argon2id auto-descritivos no formato PHC. O encoder usa salt aleatório
por hash, com 16 bytes por padrão, saída de 32 bytes, 19.456 KiB de memória, 2 iterações e
paralelismo 1. Esses custos são os valores atuais, não uma calibração final de produção.

Na verificação, os parâmetros do hash salvo são lidos do próprio PHC. Eles não precisam ser iguais
ou menores que a configuração de geração atual: valores diferentes que ficam dentro dos limites de
sanidade são verificados e `needsRehash` retorna `true` se qualquer parâmetro diferir do atual.
Assim o fluxo pode verificar hashes antigos/mais fortes e atualizar o hash após autenticação bem
sucedida, quando o fluxo de login da BE-004 existir. Formatos inválidos e parâmetros fora dos
limites são rejeitados antes de chamar o encoder. A API não inclui o hash ou a senha em erros/logs.

## Ponto de integração de autenticação (pendente)

O ponto técnico disponível para BE-004 é `PasswordHasher.verify(password, encodedHash)`, seguido de
`needsRehash(encodedHash)` após uma verificação bem-sucedida e `hash(password)` se o fluxo aprovado
precisar gerar um hash atualizado. A persistência do hash atualizado pertence ao fluxo/contrato de
autenticação aprovado. Este repositório ainda não define serviço ou endpoint de login nem publica
`CONTRACT-AUTH-001`; por isso não há integração implementada e nenhuma regra de CPF, persistência,
resposta ou sessão é inferida aqui.

## Limites de sanidade para hashes já armazenados

Os limites são configuráveis em `platform.security.password.argon2.hash-sanity` e pelas variáveis
de ambiente correspondentes `ARGON2_HASH_*`. Eles limitam custo por verificação e tamanho do PHC;
não substituem limitação de taxa/concorrência no endpoint de login.

| Parâmetro | Faixa padrão aceita | Justificativa |
| --- | ---: | --- |
| Memória `m` | 8–262.144 KiB | O mínimo respeita `m >= 8p` do Argon2; o teto de 256 MiB limita a memória de uma única verificação e ainda permite configurações mais fortes que a atual. |
| Iterações `t` | 1–10 | Aceita hashes mais fracos/fortes que a configuração de geração, com teto explícito para limitar CPU. |
| Paralelismo `p` | 1–16 | Faixa positiva e limitada para evitar valores extremos de lanes; também se aplica `m >= 8p`. |
| Salt | 16–128 bytes | 16 bytes é o tamanho de geração padrão; o teto limita campos PHC sem impedir tamanhos futuros razoáveis. |
| Hash | 16–128 bytes | Mantém saídas com pelo menos 128 bits e limita o tamanho codificado; a geração atual usa 32 bytes. |

Os limites máximos e mínimos podem ser ajustados por ambiente dentro desses tetos absolutos,
incluindo para reduzir o custo aceito. Configurações operacionais (`memory-kib`, `iterations`,
`parallelism`, `salt-length`, `hash-length`) precisam continuar dentro dos limites selecionados.
Revise o impacto de memória sob concorrência antes de aumentar os tetos.

## Referência de custo e calibração

A OWASP Password Storage Cheat Sheet lista para Argon2id, entre outras opções de custo mínimo,
`m=19.456 KiB, t=2, p=1` e `m=47.104 KiB, t=1, p=1`; recomenda medir no sistema alvo e equilibrar
memória/CPU. A meta interna inicial deste card é observar aproximadamente 250–500 ms por hash no
ambiente alvo; o tempo de uma máquina local não define o custo final.

Execute o benchmark manual (não faz parte do padrão do `mvn verify`):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -Dtest=Argon2CalibrationBenchmark test
```

O harness faz um aquecimento e cinco medições por combinação. É uma medição exploratória curta;
para decidir produção, repita no ambiente alvo, com o limite real de CPU/memória e carga concorrente,
e alinhe a escolha com Gustavo.

Medição local em 2026-10-30, Windows, JDK 21.0.12.1, Maven 3.9.14; uma JVM e cinco amostras por
combinação. Este ambiente não foi identificado como o ambiente alvo de produção.

| Memória (KiB) | Iterações | Paralelismo | Média (ms) | Min–máx (ms) |
| ---: | ---: | ---: | ---: | ---: |
| 19.456 | 2 | 1 | 61,8 | 57,2–70,3 |
| 19.456 | 3 | 1 | 70,6 | 65,2–86,0 |
| 32.768 | 2 | 1 | 79,1 | 73,3–86,9 |
| 47.104 | 1 | 1 | 166,1 | 144,6–197,3 |
| 32.768 | 2 | 2 | 211,0 | 191,9–225,3 |
| 65.536 | 2 | 1 | 416,2 | 394,6–447,1 |
| 65.536 | 3 | 1 | 582,1 | 549,3–641,6 |
| 98.304 | 2 | 1 | 595,9 | 561,0–633,1 |
| 131.072 | 2 | 1 | 535,7 | 397,5–904,4 |

**Recomendação provisória para discutir:** `65.536 KiB / 2 / 1` mediu 416,2 ms em média
(394,6–447,1 ms), dentro da meta interna de 250–500 ms neste computador. Não alterei os parâmetros
operacionais padrão: a calibração final precisa ser repetida no ambiente alvo e alinhada com Gustavo.
Os tempos locais não comprovam a capacidade de produção; o resultado de `131.072 KiB` teve variação
alta e não deve ser escolhido por esta amostra.

## Referências

- [OWASP Password Storage Cheat Sheet — Argon2id](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [RFC 9106 — Argon2, seção 3.1 (parâmetros)](https://www.rfc-editor.org/rfc/rfc9106.html#section-3.1)
