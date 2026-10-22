# TCC_V1: o piloto com banco e o verificador de qualidade

22 de outubro de 2026. Resultados das 90 execuções do piloto do `TCC_V1` com PostgreSQL, revisadas pelo verificador de
qualidade.

## 1. O que foi rodado

O mesmo desenho da rodada de 21/10 (sem banco, que ficou em `TCC_V1/<pesquisa>/runs-2026-10-21-sem-banco/`), agora com
os dados num PostgreSQL 18:

- **Construção:** o agente recebe uma pasta vazia e o enunciado do dono da pousada, que agora diz que os dados ficam
  num PostgreSQL (o endereço em `DB_URL`). O texto continua sem pedir testes.
- **Manutenção:** o agente recebe a base, agora com PostgreSQL e o esquema em migrações do Flyway, e um pedido de
  mudança.
- **Os 5 cenários** (Strategy no 01, Observer no 03, Factory Method no 05, Decorator no 07, Template Method no 09) e
  **os 3 braços** (N0 só o texto; N1 com o `CLAUDE.md`; N2 com o `CLAUDE.md` e a skill `design-patterns`) não mudaram.
- **Os 3 modelos:** Claude Haiku 4.5, Claude Sonnet 4.5 e Claude Sonnet 5.5, effort `medium`, uma execução por
  combinação: 2 pesquisas × 5 cenários × 3 braços × 3 modelos = **90 execuções**.

Cada execução teve um PostgreSQL só dela; a foto do esquema que o agente deixou está em `database-schema.sql`, na
pasta da execução.

## 2. Como foi corrigido

Pelo verificador de qualidade (`TCC_V1/evaluation/quality`): o Claude Opus 5.5, effort `high`, às cegas (sem saber
o modelo nem o braço), com uma skill por coluna, cada uma com o seu script. O mecânico ficou com as ferramentas: as
dependências entre pacotes (jdeps), os testes e a cobertura (Maven + JaCoCo), o esquema e as conferências do banco
(`pg_dump`), e um roteiro de chamadas à API por cenário (a parte comum, os dados depois de reiniciar e duas reservas
simultâneas). O julgamento ficou com a IA: uma nota por coluna (**bom**, **com problemas** ou **ruim**) e, no design,
a classe do pattern (✅ acerto, 🔷 alternativa equivalente, ⚠️ malfeito, ❌ omissão).

Antes de valer, o verificador foi calibrado em 26 casos de resposta conhecida (as 10 soluções de referência dos
patterns, nas duas pesquisas, e 6 exemplos estragados de propósito em arquitetura, testes e banco): **52 de 52 notas
conferiram**. Nas 90 revisões, todas escreveram a revisão completa e nenhum script foi pulado.

## 3. Os resultados

### A tabela do TCC

Em cada célula das três últimas colunas: **bom · com problemas · ruim** (no pattern, **acerto ou alternativa ·
malfeito · omissão**), em 15 execuções por braço. As duas primeiras colunas dão a parte boa (bom, ou acerto mais
alternativa) sem e com harness.

**Construir**

| coluna | sem harness (N0) | com harness (N1 + N2) | N0 | N1 | N2 |
|---|---|---|---|---|---|
| Arquitetura | 4 de 15 (27%) | 3 de 30 (10%) | 4 · 9 · 2 | 0 · 15 · 0 | 3 · 12 · 0 |
| Design: o pattern (acerto + alternativa) | 9 de 15 (60%) | 23 de 30 (77%) | 9 · 2 · 4 | 11 · 4 · 0 | 12 · 2 · 1 |
| Design: o resto do código | 3 de 15 (20%) | 8 de 30 (27%) | 3 · 8 · 4 | 4 · 8 · 3 | 4 · 8 · 3 |
| Testes | 2 de 15 (13%) | 5 de 30 (17%) | 2 · 3 · 10 | 3 · 6 · 6 | 2 · 5 · 8 |
| Banco | 0 de 15 (0%) | 0 de 30 (0%) | 0 · 9 · 6 | 0 · 8 · 7 | 0 · 7 · 8 |
| Comportamento | 5 de 15 (33%) | 10 de 30 (33%) | 5 · 5 · 5 | 5 · 4 · 6 | 5 · 5 · 5 |

**Manter**

| coluna | sem harness (N0) | com harness (N1 + N2) | N0 | N1 | N2 |
|---|---|---|---|---|---|
| Arquitetura | 12 de 15 (80%) | 26 de 30 (87%) | 12 · 3 · 0 | 13 · 2 · 0 | 13 · 1 · 1 |
| Design: o pattern (acerto + alternativa) | 3 de 15 (20%) | 17 de 30 (57%) | 3 · 6 · 6 | 6 · 5 · 4 | 11 · 3 · 1 |
| Design: o resto do código | 1 de 15 (7%) | 9 de 30 (30%) | 1 · 14 · 0 | 5 · 10 · 0 | 4 · 10 · 1 |
| Testes | 7 de 15 (47%) | 12 de 30 (40%) | 7 · 8 · 0 | 8 · 7 · 0 | 4 · 10 · 1 |
| Banco | 12 de 15 (80%) | 25 de 30 (83%) | 12 · 3 · 0 | 12 · 3 · 0 | 13 · 1 · 1 |
| Comportamento | 13 de 15 (87%) | 21 de 30 (70%) | 13 · 2 · 0 | 12 · 3 · 0 | 9 · 5 · 1 |

### Por modelo

Quantas execuções ficaram boas (bom, ou acerto mais alternativa no pattern), em 15 de cada pesquisa, e quantas
terminaram com o `mvn verify` falhando:

| modelo | pesquisa | arquitetura | pattern | design | testes | banco | comportamento | build falhou |
|---|---|---|---|---|---|---|---|---|
| Haiku 4.5 | Construir | 0 | 5 | 0 | 0 | 0 | 0 | 0 |
| Haiku 4.5 | Manter | 10 | 4 | 1 | 4 | 11 | 9 | 0 |
| Sonnet 4.5 | Construir | 2 | 12 | 0 | 0 | 0 | 0 | 8 |
| Sonnet 4.5 | Manter | 14 | 6 | 0 | 5 | 14 | 10 | 0 |
| Sonnet 5.5 | Construir | 5 | 15 | 11 | 7 | 0 | 15 | 0 |
| Sonnet 5.5 | Manter | 14 | 10 | 9 | 10 | 12 | 15 | 0 |

### O pattern, execução a execução

Em cada célula, a classe do pattern e as quatro notas, nesta ordem: arquitetura, testes, banco, comportamento (**B**
bom, **C** com problemas, **R** ruim).

**Construir**

| cenário | modelo | N0 | N1 | N2 |
|---|---|---|---|---|
| **01 Strategy** | Haiku 4.5 | ❌ CRRR | ⚠️ CCCR | ⚠️ CRRR |
|  | Sonnet 4.5 | ✅ BRRC | ⚠️ CCRC | ✅ CCCC |
|  | Sonnet 5.5 | ✅ CCCB | ✅ CBCB | ✅ BCCB |
| **03 Observer** | Haiku 4.5 | ⚠️ RRRR | ✅ CRRR | ⚠️ CRRR |
|  | Sonnet 4.5 | ✅ CRCC | ✅ CRRR | ✅ CRRC |
|  | Sonnet 5.5 | ✅ BCCB | ✅ CBCB | ✅ CCCB |
| **05 Factory Method** | Haiku 4.5 | ✅ CRRR | ✅ CRRR | ✅ CRRR |
|  | Sonnet 4.5 | ✅ RRCC | ✅ CRCC | ✅ CRCC |
|  | Sonnet 5.5 | ✅ CCCB | ✅ CBCB | ✅ CBCB |
| **07 Decorator** | Haiku 4.5 | ❌ CRRC | ⚠️ CRRR | ❌ CRRR |
|  | Sonnet 4.5 | ⚠️ CRCC | 🔷 CCRC | 🔷 CCRC |
|  | Sonnet 5.5 | 🔷 CBCB | 🔷 CCCB | ✅ BBCB |
| **09 Template Method** | Haiku 4.5 | ❌ CRRR | ⚠️ CRRR | ✅ CRRR |
|  | Sonnet 4.5 | ❌ BRCR | ✅ CCCC | ✅ CRRC |
|  | Sonnet 5.5 | 🔷 BBCB | 🔷 CCCB | ✅ BCCB |

**Manter**

| cenário | modelo | N0 | N1 | N2 |
|---|---|---|---|---|
| **01 Strategy** | Haiku 4.5 | ❌ BBBB | ❌ BBBB | ❌ RRRR |
|  | Sonnet 4.5 | ❌ BBBB | ✅ BCCB | ✅ BCBB |
|  | Sonnet 5.5 | ✅ BBBB | ✅ BBBB | ✅ BCBB |
| **03 Observer** | Haiku 4.5 | ⚠️ CBBB | ⚠️ CBBB | ✅ BCBB |
|  | Sonnet 4.5 | ⚠️ CCBB | ⚠️ BBBB | ⚠️ BBBB |
|  | Sonnet 5.5 | ⚠️ BCBB | ⚠️ CBBB | ✅ BBBB |
| **05 Factory Method** | Haiku 4.5 | ✅ BCCC | ❌ BCCC | ✅ BCCC |
|  | Sonnet 4.5 | ✅ BCBB | ✅ BCBC | ✅ BCBC |
|  | Sonnet 5.5 | ❌ BCCB | ✅ BBCB | ✅ BCBB |
| **07 Decorator** | Haiku 4.5 | ❌ CCBB | ❌ BCBB | ⚠️ CCBB |
|  | Sonnet 4.5 | ⚠️ BBBB | ❌ BCBC | ⚠️ BCBC |
|  | Sonnet 5.5 | ❌ BBCB | 🔷 BBBB | ✅ BBBB |
| **09 Template Method** | Haiku 4.5 | ❌ BCBC | ⚠️ BCBB | ✅ BCBC |
|  | Sonnet 4.5 | ⚠️ BBBB | ⚠️ BCBB | ✅ BCBC |
|  | Sonnet 5.5 | ⚠️ BCBB | ✅ BBBB | ✅ BBBB |

## 4. O que os resultados dizem

1. **O harness ajuda no pattern, e ajuda mais na manutenção.** Mantendo, o pattern sai certo em 3 de 15 sem harness, 6
   de 15 com o `CLAUDE.md` e 11 de 15 com a skill. Construindo, de 9 para 11 e 12. É a mesma tendência da rodada sem
   banco, mais forte na manutenção.
2. **Nas outras colunas, o harness quase não muda nada.** Arquitetura, testes, banco e comportamento ficam parecidos
   com e sem harness, nas duas pesquisas. O que o harness diz é sobre a variação e os passos repetidos, e é lá que ele
   age. Na manutenção, o comportamento até cai no N2 (de 13 para 9 bons), por uma execução sem nenhuma mudança e por
   regras novas que ficaram pela metade (veja o item 6).
3. **Manter sai muito melhor que construir nas colunas que a base já resolve.** A base tem a arquitetura, o banco
   (migrações, chaves, tipos, a restrição contra reserva dupla) e 26 testes; mantendo, o agente quase sempre conserva
   isso: 45 de 45 continuaram no Flyway, com uma migração nova quando precisaram de coluna, sem editar a que existia, e
   nenhuma deixou passar reserva dupla. Construindo do zero, nenhuma das 45 usou migração (44 criam as tabelas no
   próprio código), 23 têm chave estrangeira sem índice, e 15 deixam o mesmo quarto ser reservado duas vezes quando
   dois pedidos chegam juntos. Por isso o **banco nunca saiu `bom` na construção** (0 de 45).
4. **O modelo pesa mais que o harness, de novo.** O Sonnet 5.5 tem o comportamento `bom` nas 30 execuções e acerta o
   pattern em 25 de 30. O Haiku, construindo, não entrega um sistema que funcione em nenhuma das 15 (o comportamento é
   `ruim` em 14: por exemplo, o `POST /reservations` recusando um corpo correto, ou o `GET /rooms` com erro 500); e o
   Sonnet 4.5 constrói com o pattern certo em 12 de 15, mas o comportamento fica `com problemas` em 13 e 13 das 15
   construções deixam passar a reserva dupla.
5. **Testes: o Sonnet 4.5 escreve testes de integração que não passam no build.** Em 8 das 15 construções dele o
   `mvn verify` falhou, sempre num teste de integração do próprio agente: em 5, o teste usa o Testcontainers (que
   precisa de Docker, que o ambiente não tem, e ignora o banco que o texto dá em `DB_URL`); em 2, o teste chama um
   serviço que precisaria estar no ar; em 1, os testes de integração falham de verdade (4 de 6). Os testes de unidade
   passam. Nenhum outro modelo teve build falhando.
6. **A armadilha da skill aberta como pergunta voltou, agora com o Haiku.** No N2 da manutenção do cenário 01, o Haiku
   abriu a skill, respondeu "A resposta é **Strategy**" com uma explicação, e terminou sem mudar nenhum arquivo: todas
   as colunas saíram `ruim`. É o mesmo erro do Sonnet 4.5 na `construction-v7`.
7. **A arquitetura na construção raramente é `bom`** (7 de 45), quase sempre por uma mistura localizada ou um ciclo
   entre pacotes. As soluções de referência também saíram `com problemas` na construção, pelo ciclo entre o comprovante
   e a reserva que a própria base tem; por isso essa coluna, na construção, separa pouco.

## 5. Os limites

- **Uma execução por combinação.** As tendências (o modelo pesa mais; o harness age no pattern e mais na manutenção)
  são as conclusões; diferenças de uma ou duas execuções entre braços podem ser acaso.
- **O verificador é uma IA.** Foi calibrado (52 de 52) e julga com os números das ferramentas e evidência em
  `arquivo:linha`, mas a nota curta resume uma leitura; a revisão inteira de cada execução está em
  `TCC_V1/evaluation/quality/results/`.
- **O ambiente não tem Docker dentro do contêiner do agente,** o que pesa contra quem escreve testes com o
  Testcontainers (só o Sonnet 4.5 fez isso, em 5 construções). O banco pronto em `DB_URL` estava no texto.
- **A skill e o `CLAUDE.md` foram escritos por quem conhece as tarefas,** o que tende a favorecer os braços com
  harness (declarado em `TCC_V1/skills/README.md`).
- **Uma revisão terminou sem o `review.md`** (o revisor rodou dois scripts em segundo plano e encerrou a sessão); o
  agente revisor ganhou a instrução de rodar em primeiro plano, e a revisão foi refeita.

## 6. Custo e onde estão os dados

- **As execuções:** US$ 77,28 em equivalente de API (Sonnet 4.5 44,60; Haiku 16,55; Sonnet 5.5 16,13).
- **O verificador nas 90:** US$ 78,92. **A calibração:** US$ 21,86 na rodada final (a primeira, de 50 de 52, está
  guardada ao lado).
- **Os projetos:** `TCC_V1/<construction|maintenance>/runs/<cenário>/<braço>-<modelo>/workspace/`, com o
  `database-schema.sql` ao lado.
- **As revisões:** `TCC_V1/evaluation/quality/results/<pesquisa>/<cenário>/<braço>-<modelo>/review.md`, com as medidas
  em `measures/`, e o resumo de todas em `TCC_V1/evaluation/quality/results/summary.md`.
