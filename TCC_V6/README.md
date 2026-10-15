# TCC_V6: a pesquisa nova (em construção)

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
| `evaluation/semgrep/` | a régua do Semgrep: Strategy (P1 a P4), o controle negativo da região (P5), Adapter (P6), State (P7) e Factory (descritiva) ([README](evaluation/semgrep/README.md)) |
| `evaluation/measures/` | as medidas de testes (PIT), arquitetura (ArchUnit) e banco (as restrições do esquema) ([README](evaluation/measures/README.md)) |
| `infra/scripts/` | o `pilot.py`, que roda um piloto sem harness com a imagem do V5 |
| `runs/pilots/` | os pilotos: uma pasta por execução, com a transcrição, o build e os resultados da suíte |

Os harnesses (N1 a N3) ainda não estão aqui: o Lucas vai redesenhá-los.

## As ferramentas

As ferramentas do V6 são em **Python** (só a biblioteca padrão) e em Java (o gabarito e a suíte).
A exceção é o `differential.mjs`, que precisa importar o gabarito do V5, escrito em JavaScript.
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
python infra/scripts/pilot.py OPUS55-N0-04 claude-opus-5-5 medium --dry-run
```

```bash
python evaluation/reference/run_suite.py runs/pilots/OPUS55-N0-04/workspace runs/pilots/OPUS55-N0-04/suite
```

```bash
python evaluation/semgrep/detect.py runs/pilots analysis/semgrep-pilots.csv
```

O `--dry-run` faz todas as conferências e mostra os comandos do Docker sem rodar o agente; sem ele,
o piloto roda de verdade. Fora do git: o `target/` de cada build e o `classpath.txt` da suíte. O
token da assinatura é o mesmo do V5 (`../TCC_V5/.env`), que o Docker lê pelo `--env-file`; o
`pilot.py` só confere se ele existe, sem imprimir o valor.
