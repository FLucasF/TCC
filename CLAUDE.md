# Laboratório do experimento

Este repositório **não** é o projeto sob teste. É a bancada que roda o Claude
Code em cima de um projeto, compara com e sem harness, e guarda o resultado.

O projeto sob teste nasce em `runs/<id>/workspace/` a cada execução.

## A divisão que importa

| pasta | o agente enxerga? |
|---|---|
| `experimento/` | **sim.** `prompt/` e `harness/` são copiados para dentro do workspace. O workspace em si nasce vazio |
| `infra/` | não. Dockerfile e scripts rodam de fora |
| `avaliacao/` | **nunca.** Gabarito, rotas de teste e notas |
| `docs/` | não |
| `runs/` | cada run só enxerga a própria `workspace/` |

IMPORTANT: `infra/scripts/executar.sh` faz `cp -r experimento/harness/. workspace/`.
Tudo que estiver em `experimento/harness/` chega ao agente. Anotação sobre o
harness vai em `docs/harness-notas.md`, nunca ali dentro.

## Regras desta bancada

- Nada em `experimento/prompt/` ou `experimento/harness/` pode nomear o padrão
  de projeto avaliado, nem citar o domínio do enunciado. Rodar a checagem de
  palavras proibidas antes de congelar.
- Run nunca é reaproveitada nem editada. Deu errado, cria outra com id novo.
- Mudou o harness, muda a versão: registrar redação, hash e quais runs cada
  versão produziu, em `docs/harness-notas.md`.
- A imagem é fixada por digest. Não reconstruir no meio de uma coleta; se
  reconstruir, é tag nova e rodada nova.
- `.env` tem o token da assinatura. Nunca versionar, nunca imprimir.

## Como rodar

```bash
infra/scripts/executar.sh <run_id> <modelo> <SEM|COM>   # uma execução
infra/scripts/par.sh <prefixo> <modelo>                 # o par SEM || COM junto
EFFORT=medium infra/scripts/rodada.sh <prefixo>         # os três modelos, 6 de uma vez
```

`EFFORT` é opcional e vale `high` por padrão, conforme D8 do plano. O valor
usado vai para o `meta.json` de cada run.

Runs com prefixo `FUMACA-` são teste de infraestrutura e não entram na análise.
Prefixo `MED-` é rodada de medição, também fora do conjunto.

## Onde olhar primeiro

- `docs/plano.md` — o desenho do experimento e as decisões numeradas
- `docs/harness-notas.md` — o que está no harness, por quê, e o diário de versões
- `avaliacao/rotas-descobertas.md` — o que os exemplos do enunciado não cobrem
