# TCC_V4: o experimento que vale

O lote **pré-registrado** do TCC: o harness (N0 a N3) muda como agentes do Claude Code
aplicam o padrão Strategy num serviço de checkout em Java/Spring?

- **O que o experimento é, e como cada hipótese é lida:** [`OBJETIVO.md`](OBJETIVO.md).
- **Como rodar, passo a passo:** [`COMO-RODAR-V4.md`](COMO-RODAR-V4.md).
- **O porquê de cada escolha, e o que acontecer durante as rodadas:** o
  [`DECISOES.md`](../TCC_V3/DECISOES.md) do V3, que continua sendo o registro único.

## Congelado

Tudo o que está aqui, menos este README e o que as rodadas produzirem, foi **congelado
em 09/10/2026, antes da primeira execução** (commit `06a1172` do repositório). O
[`CONGELADO-V4.sha256`](CONGELADO-V4.sha256) tem o sha256 de cada um dos 113 arquivos.
Antes de começar, e sempre que houver dúvida:

```bash
sha256sum -c CONGELADO-V4.sha256 --quiet     # sem saída: tudo idêntico ao congelado
```

Uma mudança no que congelou vira **emenda datada** no fim do `OBJETIVO`, com o motivo.

## O que fica onde

| pasta | o que tem |
|---|---|
| `experiment/` | o enunciado, os três harnesses (N1 a N3), os dois desenhos (confirmatório e exploratório) e as duas ordens sorteadas |
| `infra/` | o `Dockerfile` da imagem `experimento-harness:v3` e os scripts que rodam (`run-levels.sh`, `run-one.sh`...) |
| `evaluation/` | a régua, o guia, o gabarito, a suíte e todas as ferramentas de medida e de análise |
| `runs/` | **criada pelas rodadas**: uma pasta por execução |
| `analysis/` | **criada depois**: os CSVs e os relatórios |
| `evaluation/reading/` | **criada pelo sorteio**: a amostra e a planilha do Lucas |

Fora do git: o `.env` (o token da assinatura), o `.env.sonar` (o token do SonarQube, que
só é preciso no fim, para as métricas), o mapa de anonimização e os pacotes cegos.

## Os lotes

| lote | modelos | execuções | o que diz |
|---|---|---|---|
| `V4-STRATEGY` (confirmatório) | Haiku 4.5, Sonnet 4.5, Opus 4.6, Sonnet 5, Opus 5 | 100 | testa as hipóteses |
| `V4-EXPLOR` (exploratório) | Haiku 5.5, Sonnet 4.6, Opus 4.7, Opus 4.8, Sonnet 5.5 | 100 | só descreve |
