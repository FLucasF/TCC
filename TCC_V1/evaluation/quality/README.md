# O verificador de qualidade

Só da avaliação: **nunca entra no experimento**. Depois que as execuções terminam, cada projeto é copiado para uma
pasta de trabalho só dele e revisado pelo Claude Code com o agente `quality-reviewer` (Claude Opus 5.5, effort
`high`), num contêiner com um PostgreSQL próprio. Os agentes testados nunca veem nada disto.

A revisão dá uma nota curta por coluna da tabela do TCC (arquitetura, design de baixo nível, testes, banco) e mais o
comportamento: `bom`, `com problemas` ou `ruim`; no design, também a classe do pattern (`acerto`, `alternativa
equivalente`, `malfeito`, `omissão`) e o exagero (`contenção` ou `exagero`). Cada nota vem com os números das
ferramentas e o porquê, com `arquivo:linha`.

## As peças

| pasta | o que tem |
|---|---|
| `agent/quality-reviewer.md` | o agente: o que ele recebe, como revisa, o significado das notas e o formato do `review.md` |
| `skills/review-architecture/` | os pacotes e as camadas; o script `measure_dependencies.sh` (jdeps: dependências entre pacotes, ciclos pelo `tsort`, as bibliotecas de cada pacote, as maiores classes) |
| `skills/review-design/` | a variação que o texto pede e o design do resto; sem script, com os critérios de cada pattern em `references/` (os da correção unificada, commit `cc934ab5`) |
| `skills/review-tests/` | os testes do agente; o script `measure_tests.sh` (o `mvn verify` com o JaCoCo pela linha de comando, sem mexer no pom; a contagem de testes, a cobertura de linhas e ramos, os tipos de teste) |
| `skills/review-database/` | o banco; o script `measure_database.sh` (o serviço sobe num banco vazio, o `pg_dump --schema-only` e as conferências: tabela sem chave, dinheiro sem 2 casas, chave estrangeira sem índice, datas em texto, restrições, migrações, SQL concatenado) |
| `skills/review-behavior/` | o comportamento pela API; os roteiros `check_01.sh` a `check_09.sh` (os valores que cada texto pede), com a parte comum, os dados depois de reiniciar e duas reservas ao mesmo tempo, em `behavior.sh` |
| `lib/project.sh` | o que os scripts dividem (achar o pom, empacotar, achar o jar, um banco vazio por medida, subir o serviço); o pipeline copia para junto dos scripts de cada skill |
| `docker/Dockerfile` | a imagem `tcc-v1-reviewer:1`: a do experimento mais o cliente do PostgreSQL 18 (`pg_dump`, `psql`) e o JaCoCo aquecido |
| `review_runs.py` | o pipeline |

Os nomes ficam em inglês; o texto das skills e das conferências, em português. Os scripts são fixos e recebem só a
pasta do projeto; cada um grava um arquivo de resultado em `measures/`. O revisor roda os scripts e julga; no fim, o
pipeline confere se todos os resultados existem, roda os que faltarem e registra no `meta.json` que o revisor pulou.

## Como rodar (a partir do `TCC_V1`)

```bash
docker build -t tcc-v1-reviewer:1 evaluation/quality/docker
```

```bash
python evaluation/quality/review_runs.py review-all maintenance --workers 3
```

```bash
python evaluation/quality/review_runs.py summary
```

Também `review <pasta de uma execução>...` para revisar execuções escolhidas, `--scenario NN` e `--arm NX` no
`review-all`, `--resume` para refazer só as que ficaram sem `review.md`, e `--dry-run` para montar as pastas sem rodar
nada. A saída de cada revisão vai para `results/<pesquisa>/<cenário>/<braço>-<modelo>/`: o `review.md`, as notas lidas
dele (`review.json`), as medidas (`measures/`), a transcrição e o `meta.json`; o `summary` junta tudo em
`results/summary.md`.

## A calibração

```bash
python evaluation/quality/review_runs.py calibrate --workers 3
```

Os casos ficam fora do `TCC_V1`, em `../maintenance-v1/evaluation/solutions-postgres/` (veja o README de lá): as 10
soluções de referência dos patterns refeitas sobre a base com banco, revisadas nas duas pesquisas, e 6 exemplos
estragados de propósito para a arquitetura, os testes e o banco (metade bons, metade ruins, com as duas soluções
boas), revisados na manutenção. O `cases.json` de lá é o gabarito; o resultado vai para
`calibration/<modelo>-<effort>/summary.md`. Os roteiros de comportamento passam em todas as conferências nas 10
soluções.

**O resultado (21/10/2026, Opus 5.5, effort `high`): 52 de 52 notas conferem**, em 26 revisões, por US$ 21,86, sem
nenhum script pulado pelo revisor (`calibration/opus-5-5-high/summary.md`). A primeira rodada deu 50 de 52 e fica em
`calibration/opus-5-5-high-first-run/`: o revisor deu `com problemas` nos testes da 01-pattern e da 05-pattern, com
razão (os testes das soluções deixavam regras sem conferir); os testes delas foram completados, e os 10 casos cujo
patch mudou foram revisados de novo, já com a regra de que cada defeito pesa numa coluna só. O exemplo de testes sem
asserção (`tests-hollow`, com a cobertura alta) saiu `ruim` sem o PIT, que por isso não entrou.

## O que o revisor recebe

A pasta de trabalho tem o `task.md` (a pesquisa, o cenário, o pattern, as pastas), o texto que o agente recebeu, o
projeto (sem o `CLAUDE.md`, a `.claude/`, o `.git` e o `target/` do agente, que diriam o braço), o agente e as skills.
Nunca o nome da execução, o modelo ou o braço. O PostgreSQL da revisão chega em `PGHOST`, `PGPORT`, `PGUSER` e
`PGPASSWORD`; cada script cria nele um banco vazio para cada medida.
