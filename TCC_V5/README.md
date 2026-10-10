# TCC_V5: o experimento que vale

O lote **pré-registrado** do TCC: o harness (N0 a N3) muda como agentes do Claude Code
aplicam o padrão Strategy num serviço de checkout em Java/Spring? Onze modelos, num lote
só. Substitui o V4 (`../TCC_V4`), descartado antes da análise e usado como ensaio geral
(ver o §2 do `OBJETIVO`).

- **O que o experimento é, e como cada hipótese é lida:** [`OBJETIVO.md`](OBJETIVO.md).
- **Como rodar, passo a passo:** [`COMO-RODAR-V5.md`](COMO-RODAR-V5.md).
- **O porquê de cada escolha, e o que acontecer durante as rodadas:** o
  [`DECISOES.md`](../TCC_V3/DECISOES.md) do V3, que continua sendo o registro único.

## Congelado

Tudo o que está aqui, menos este README e o que as rodadas produzirem, foi **congelado
em 10/10/2026, antes da primeira execução** (commit `54e13fc` do repositório). O
[`CONGELADO-V5.sha256`](CONGELADO-V5.sha256) tem o sha256 de cada um dos 122 arquivos.
Antes de começar, e sempre que houver dúvida:

```bash
sha256sum -c CONGELADO-V5.sha256 --quiet     # sem saída: tudo idêntico ao congelado
```

Uma mudança no que congelou vira **emenda datada** no fim do `OBJETIVO`, com o motivo.

## O que fica onde

Cada pasta tem um README próprio, que diz o que ela é e como é organizada.

| pasta | o que tem |
|---|---|
| `experiment/` | o enunciado, os três harnesses (N1 a N3), o desenho do V5 (`desenho-v5.json`) e a ordem sorteada dos 55 quartetos (`ordem-v5.csv`) |
| `infra/` | o `Dockerfile` da imagem `experimento-harness:v5` (Claude Code 2.1.288) e os scripts que rodam e conferem (`run-levels.sh`, `run-one.sh`, `acceptance.sh`, `verify.mjs`...) |
| `evaluation/` | a régua (versão 5), o guia, o gabarito, a suíte e todas as ferramentas de medida e de análise, com os testes de cada uma |
| `runs/` | **enchida pelas rodadas**: uma pasta por execução |
| `analysis/` | **enchida depois**: os CSVs e os relatórios |
| `evaluation/reading/` | **criada pelo sorteio**: a amostra dos 44 pacotes e a planilha do Lucas |

Fora do git: o `.env` (o token da assinatura), o `.env.sonar` (o token do SonarQube, que
só é preciso no fim, para as métricas), o mapa de anonimização e os pacotes cegos.

## O lote

| lote | modelos | execuções | o que diz |
|---|---|---|---|
| `V5-STRATEGY` | Haiku 4.5, Haiku 5.5, Sonnet 4.5, Sonnet 4.6, Sonnet 5, Sonnet 5.5, Opus 4.6, Opus 4.7, Opus 4.8, Opus 5, Opus 5.5 | 220 (55 quartetos de 4 níveis) | testa as hipóteses |
