# Teste de aceitação: protótipo

> **PROTÓTIPO, não é a Parte 2 do plano.** Foi escrito em 30/09/2026 para analisar
> as rodadas de teste `TESTE-STATE-02` e `TESTE-STRATEGY-01`. Não está congelado,
> e não vale para as execuções da análise. A suíte de verdade (Parte 2) parte
> daqui, mas precisa ser revisada, validada numa implementação de referência
> escrita à mão e congelada com hash antes de ser usada.

Caixa-preta, pela API HTTP que o enunciado define. Sobe o serviço de um workspace
num container da imagem da bancada, sem token, e roda os casos.

| arquivo | o que faz |
|---|---|
| `executor.sh` | roda **dentro** do container: copia o workspace, reaproveita o `.jar` do build pós-execução (ou compila), sobe o serviço na porta 18080 e roda o teste |
| `strategy.mjs` | 15 casos do enunciado do Strategy: o exemplo 5 literal, os exemplos 1 a 4 **com** clube e região, colisões (OURO + FRETEGRATIS), erros e ordem de precedência. Mais uma **observação**, que não conta: o exemplo 1 como está escrito, sem clube e região |
| `ref-strategy.mjs` | a calculadora de referência do Strategy, em centavos, com meio-para-o-par. Reproduz os 5 exemplos conferidos do enunciado |
| `state.mjs` | os 8 exemplos do enunciado do State, os textos para o cliente e os erros |
| `analisar-rodada.mjs` | resume os `meta.json` de uma rodada: término, build, versões, tokens, tempo, hashes, isolamento, pares |

## Como rodar

Da raiz do `TCC_V3`, para um workspace:

```bash
A=$(cygpath -w "$PWD/avaliacao/aceitacao-prototipo")
W=$(cygpath -w "$PWD/runs/<RUN_ID>/workspace")
MSYS_NO_PATHCONV=1 docker run --rm \
  --mount "type=bind,source=$W,target=/ws,readonly" \
  --mount "type=bind,source=$A,target=/aceitacao,readonly" \
  experimento-harness:v3 bash /aceitacao/executor.sh strategy.mjs   # ou state.mjs
```

E o resumo de uma rodada:

```bash
node avaliacao/aceitacao-prototipo/analisar-rodada.mjs TESTE-STRATEGY-01
```

## Validação que já existe

- `ref-strategy.mjs` reproduz os 5 exemplos do enunciado, centavo a centavo.
- Os quatro pacotes de Opus e Sonnet de `TESTE-STRATEGY-01`, e o `TESTE-P4-OPUS-CONTROL`,
  passaram em 85 de 85 verificações do `strategy.mjs`: cinco implementações
  independentes chegam aos mesmos números da referência, inclusive nos casos que o
  enunciado não exemplifica.
- Ainda falta a implementação de referência **escrita à mão** que o plano exige.
