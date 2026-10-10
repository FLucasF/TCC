# `infra/`: a bancada que roda e confere o experimento

As duas peças que fazem uma execução acontecer do mesmo jeito todas as vezes: a
**imagem Docker** onde o agente trabalha, e os **scripts** que lançam as execuções,
medem a correção e conferem que os dados batem com o desenho.

| pasta | o que tem | README |
|---|---|---|
| `docker/` | o `Dockerfile` da imagem única do experimento (`experimento-harness:v5`: Java 21, Maven, Node e o Claude Code 2.1.288, travados por versão) e o `aquecimento/`, um projeto Spring Boot mínimo que só existe durante o build da imagem, para o cache do Maven já vir cheio e o download não entrar na medição | `docker/README.md`: o que é cada linha do `Dockerfile` e para que serve o aquecimento |
| `scripts/` | os scripts que **rodam** (`run-levels.sh` → `run-one.sh` → `extract-meta.mjs`), que **medem a correção** (`acceptance.sh`), que **juntam o custo** (`aggregate.mjs`), que **sorteiam a ordem** (`draw-order.mjs`) e que **conferem a coerência** (`verify.mjs`, com a prova `verify-teste.mjs`) | `scripts/README.md`: o que cada script faz e como, e o caminho de uma execução |

## Como as peças se encaixam

```
experiment/desenho-v5.json + experiment/ordem-v5.csv
        │  (o apelido do modelo, o effort, a imagem)
        ▼
infra/scripts/run-levels.sh  ── lança 4 ao mesmo tempo ──>  infra/scripts/run-one.sh
                                                                │
        ┌───────────────────────────────────────────────────────┘
        ▼
container da imagem v5 (infra/docker/)  ──>  runs/<id>/  (workspace, transcrição, build, meta.json)
        │
        ▼
infra/scripts/acceptance.sh (a suíte, no mesmo tipo de container, sem token)
infra/scripts/aggregate.mjs (o custo)  ──>  analysis/
infra/scripts/verify.mjs    (tudo bate com o desenho?)
```

A avaliação do código (Semgrep, leitura, nota, hipóteses, métricas) não fica aqui: está
em `evaluation/`.

## O que fica na raiz do TCC, e não aqui

- `.dockerignore`: uma **lista branca** do que entra no build da imagem (só o
  `Dockerfile` e o `aquecimento/`). O enunciado, os harnesses e o `.env` nunca entram.
- `.env` (fora do git): o token da assinatura, que o `run-one.sh` passa ao container. O
  script recusa um `.env` com variável que troque cobrança, provedor ou modelo.
- `.env.sonar` (fora do git): o token do SonarQube, para o `evaluation/tools/metrics.sh`.

## Vai para o `TCC_V5`?

Sim, tudo (congelado), menos o `aquecimento/target/` que um `mvn` local deixou no
`TCC_V3`: não é commitado, e o `.dockerignore` o deixa fora do build da imagem.
