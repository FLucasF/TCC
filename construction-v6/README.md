# construction-v6: a pesquisa nova (em construção)

O próximo experimento: o mesmo serviço de checkout em Java/Spring, agora com um enunciado
mais complexo e testável (o ciclo do pedido, o estoque, uma transportadora com API estrangeira,
persistência e concorrência). Só a **construção** é medida; não há fase de manutenção.
Os padrões observados são o Strategy, o Adapter e o Factory, com o State como secundário.

**Nada aqui está congelado.** É um rascunho de trabalho: tudo pode mudar até o congelamento,
que vai ser feito como o do V5 (um `CONGELADO-V6.sha256` e um commit antes da primeira
execução que vale).

- **As notas do desenho** (os pilotos, as armadilhas, as decisões em aberto, o que falta):
  [`NOTAS.md`](NOTAS.md).

## O que fica onde

| pasta | o que tem |
|---|---|
| `experiment/prompt/` | o enunciado (`prompt.md`) e, em `history/`, as versões anteriores |
| `evaluation/reference/` | o gabarito (a calculadora do resumo e o `Store`, o modelo do estado), os testes dele e a suíte completa que compara o serviço do agente com o gabarito ([README](evaluation/reference/README.md)) |
| `evaluation/measures/` | a medida dos testes do agente, por mutação (PIT), e o relatório de arquitetura (ArchUnit) ([README](evaluation/measures/README.md)) |
| `evaluation/judge/` | **a correção**: as réguas das quatro dimensões, em markdown, para a IA ler e interpretar; o serviço com defeitos plantados e o [protocolo](evaluation/judge/PROTOCOLO.md) ([README](evaluation/judge/README.md)) |
| `infra/scripts/` | o `run_pilot_without_harness.py`, que roda um piloto sem harness com a imagem do V5, e o `freeze.py`, que faz o congelamento |
| `runs/pilots/` | os pilotos: uma pasta por execução, com a transcrição, o build, os resultados da suíte e o relatório de arquitetura |
| `analysis/` | os resultados das medidas, por lote (`analysis/pilots/`: o PIT dos 8 pilotos) |

Os harnesses (N1 a N3) ainda não estão aqui: o Lucas vai redesenhá-los.

## Como a correção é feita

Desde 15/10/2026, a correção é feita pela **IA junto com o Lucas**, lendo o código com uma rubrica. Ela julga
quatro dimensões (17/10/2026): a **arquitetura**, a **implementação dos padrões**, a **cobertura e a qualidade dos
testes** e a **qualidade do banco** (as ligações entre as entidades, os relacionamentos, a organização). As
réguas estão em `evaluation/judge/rubric/`, em markdown: a IA lê a régua de uma dimensão, interpreta o código e
responde item por item com evidência, sem script; o [protocolo](evaluation/judge/PROTOCOLO.md) diz o que o sustenta
e o que falta antes de valer. As ferramentas abaixo são material dessa leitura, não nota:
- **a suíte** (`evaluation/reference/`): a mesma prova para todos os serviços (o resumo, o ciclo do pedido, o
  reinício, as duas máquinas, a concorrência). A correção do desenho não lê o resultado dela; a foto do esquema que o
  banco do agente criou é o material do banco (`database-schema.json`: as tabelas, as colunas com tipo e nulo, as restrições e os
  índices);
- **o PIT** (`evaluation/measures/`): a cobertura de linhas e quantos erros os testes do agente pegam;
- **o relatório de arquitetura** (`evaluation/measures/`): os pacotes, as dependências entre eles e os ciclos,
  lidos pelo ArchUnit.

A régua do Semgrep e a medida de banco saíram; estão no histórico do git (até o commit `3426ad23`), e as
perguntas da régua (P1 a P7) e as decisões de calibração estão nas notas, como base para a rubrica. O
ArchUnit saiu em 15/10 e voltou em 17/10, como relatório.

## As ferramentas

As ferramentas do V6 são em **Python** (só a biblioteca padrão) e em Java (o gabarito e a suíte).
O código e as mensagens estão em inglês; os READMEs e o enunciado, em português.

Duas checagens valem para todo o Python (configuradas no `pyproject.toml`):

```bash
python -m mypy
```

```bash
python -m ruff check evaluation infra
```

O mypy roda no modo estrito. O ruff inclui a regra que barra `open()` sem `encoding="utf-8"` (no
Windows, o padrão seria o cp1252). Os dois são ferramentas de desenvolvimento, instaladas com `pip`.

## Comandos (a partir desta pasta)

```bash
python infra/scripts/run_pilot_without_harness.py OPUS55-N0-04 claude-opus-5-5 medium --dry-run
```

```bash
python evaluation/reference/run_suite_against_service.py runs/pilots/OPUS55-N0-04/workspace runs/pilots/OPUS55-N0-04/suite
```

```bash
python evaluation/measures/measure_test_quality.py runs/pilots analysis/pilots/pit.csv
```

```bash
python evaluation/measures/report_architecture.py runs/pilots
```

O `--dry-run` faz todas as conferências e mostra os comandos do Docker sem rodar o agente; sem ele,
o piloto roda de verdade. Fora do git: o `target/` de cada build e o `classpath.txt` da suíte. O
token da assinatura é o mesmo do V5 (`../construction-v5/.env`), que o Docker lê pelo `--env-file`; o
`run_pilot_without_harness.py` só confere se ele existe, sem imprimir o valor.
