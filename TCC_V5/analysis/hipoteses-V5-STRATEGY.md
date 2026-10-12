# Hipoteses: V5-STRATEGY

Gerado por evaluation/tools/hipoteses.mjs. Regras: OBJETIVO, secao 4.1. 5 replicas x 11 modelos x 4 niveis.

## ★ Desenho: isola cada caso no P4

Medida: p4_acerto, N1 x N0; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 1 | 4 | 0 | 0 | +1 |
| HAIKU55 | 2 | 3 | 0 | 0 | +2 |
| SONNET45 | 3 | 2 | 0 | 0 | +3 |
| SONNET46 | 2 | 3 | 0 | 0 | +2 |
| SONNET5 | 1 | 4 | 0 | 0 | +1 |
| SONNET55 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS46 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS47 | 5 | 0 | 0 | 0 | +5 |
| OPUS48 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS5 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS55 (teto) | 0 | 5 | 0 | 0 | +0 |
| **todos** | 14 | 41 | 0 | 0 | +14 |

maioria dos 6 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.25.

**Veredito: apoiada.**

## ★ Exagero: aplica onde nao pede

Medida: p5_exagero, N1 x N0; regra: sem-direcao-binaria.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 0 | 5 | 0 | 0 | +0 |
| HAIKU55 | 0 | 5 | 0 | 0 | +0 |
| SONNET45 | 1 | 4 | 0 | 0 | +1 |
| SONNET46 | 0 | 5 | 0 | 0 | +0 |
| SONNET5 | 0 | 5 | 0 | 0 | +0 |
| SONNET55 | 0 | 5 | 0 | 0 | +0 |
| OPUS46 | 0 | 5 | 0 | 0 | +0 |
| OPUS47 | 0 | 5 | 0 | 0 | +0 |
| OPUS48 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| OPUS55 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 1 | 54 | 0 | 0 | +1 |

1 par(es) nao empatado(s) (1 sobem, 0 descem); com 1, o lado maior precisa de -. Tamanho do efeito (saldo / pares): 0.02.

**Veredito: contrariada.**

## ★ Correcao: suite inteira

Medida: pontos, N1 x N0; regra: nao-inferioridade, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 0 | 0 | 5 | 0 | -5 |
| HAIKU55 | 0 | 5 | 0 | 0 | +0 |
| SONNET45 | 3 | 1 | 1 | 0 | +2 |
| SONNET46 | 3 | 1 | 1 | 0 | +2 |
| SONNET5 | 1 | 4 | 0 | 0 | +1 |
| SONNET55 | 0 | 5 | 0 | 0 | +0 |
| OPUS46 | 1 | 4 | 0 | 0 | +1 |
| OPUS47 | 2 | 3 | 0 | 0 | +2 |
| OPUS48 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| OPUS55 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 10 | 38 | 7 | 0 | +3 |

saldo (piores - melhores) = -3, limite 7 (13% de 55 pares). Tamanho do efeito (saldo / pares): 0.05.

**Veredito: apoiada.**

## ★ Qualidade: menos complexidade

Medida: cognitiva, N1 x N0; regra: direcional, melhor = menos.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 4 | 0 | 1 | 0 | +3 |
| HAIKU55 | 5 | 0 | 0 | 0 | +5 |
| SONNET45 | 4 | 0 | 1 | 0 | +3 |
| SONNET46 | 5 | 0 | 0 | 0 | +5 |
| SONNET5 | 5 | 0 | 0 | 0 | +5 |
| SONNET55 | 5 | 0 | 0 | 0 | +5 |
| OPUS46 | 3 | 0 | 2 | 0 | +1 |
| OPUS47 | 5 | 0 | 0 | 0 | +5 |
| OPUS48 | 4 | 1 | 0 | 0 | +4 |
| OPUS5 | 5 | 0 | 0 | 0 | +5 |
| OPUS55 | 4 | 1 | 0 | 0 | +4 |
| **todos** | 49 | 2 | 4 | 0 | +45 |

maioria dos 11 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.82.

**Veredito: apoiada.**

## ★ Modelo: efeito maior no mais fraco

Pares do desenho no P4 (N1 x N0); regra: modelo.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 1 | 4 | 0 | 0 | +1 |
| HAIKU55 | 2 | 3 | 0 | 0 | +2 |
| SONNET45 | 3 | 2 | 0 | 0 | +3 |
| SONNET46 | 2 | 3 | 0 | 0 | +2 |
| SONNET5 | 1 | 4 | 0 | 0 | +1 |
| SONNET55 | 0 | 5 | 0 | 0 | +0 |
| OPUS46 | 0 | 5 | 0 | 0 | +0 |
| OPUS47 | 5 | 0 | 0 | 0 | +5 |
| OPUS48 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| OPUS55 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 14 | 41 | 0 | 0 | +14 |

mais fraco(s) no N0: HAIKU45, OPUS47 (acerto medio 0.00); maior saldo: OPUS47 (5). Tamanho do efeito (saldo / pares): 0.25.

**Veredito: apoiada.**

## Desenho: isola cada caso no P1 a P3

Medida: p1p3_acertos, N1 x N0; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 3 | 2 | 0 | 0 | +3 |
| HAIKU55 | 3 | 2 | 0 | 0 | +3 |
| SONNET45 | 5 | 0 | 0 | 0 | +5 |
| SONNET46 | 3 | 2 | 0 | 0 | +3 |
| SONNET5 | 2 | 3 | 0 | 0 | +2 |
| SONNET55 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS46 | 2 | 3 | 0 | 0 | +2 |
| OPUS47 | 5 | 0 | 0 | 0 | +5 |
| OPUS48 | 1 | 4 | 0 | 0 | +1 |
| OPUS5 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS55 (teto) | 0 | 5 | 0 | 0 | +0 |
| **todos** | 24 | 31 | 0 | 0 | +24 |

maioria dos 8 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.44.

**Veredito: apoiada.**

## Desenho: nao repete o comum

Medida: duplicacao, N1 x N0; regra: direcional, melhor = menos.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 0 | 4 | 1 | 0 | -1 |
| HAIKU55 | 2 | 3 | 0 | 0 | +2 |
| SONNET45 | 0 | 0 | 5 | 0 | -5 |
| SONNET46 | 4 | 0 | 1 | 0 | +3 |
| SONNET5 | 1 | 3 | 1 | 0 | +0 |
| SONNET55 | 0 | 2 | 3 | 0 | -3 |
| OPUS46 | 1 | 0 | 4 | 0 | -3 |
| OPUS47 | 0 | 4 | 1 | 0 | -1 |
| OPUS48 | 1 | 1 | 3 | 0 | -2 |
| OPUS5 | 2 | 1 | 2 | 0 | +0 |
| OPUS55 | 0 | 2 | 3 | 0 | -3 |
| **todos** | 11 | 20 | 24 | 0 | -13 |

maioria dos 11 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): -0.24.

**Veredito: contrariada.**

## Desenho: replicas mais parecidas

Sem regra de pares no OBJETIVO: descritiva. Perfis diferentes do Semgrep (P1 a P5) entre as replicas de cada modelo; menos perfis = mais parecidas.

| modelo | N0 | N1 |
|---|---|---|
| HAIKU45 | 3 | 5 |
| HAIKU55 | 4 | 1 |
| SONNET45 | 4 | 3 |
| SONNET46 | 4 | 1 |
| SONNET5 | 3 | 1 |
| SONNET55 | 1 | 1 |
| OPUS46 | 2 | 1 |
| OPUS47 | 3 | 1 |
| OPUS48 | 2 | 1 |
| OPUS5 | 1 | 1 |
| OPUS55 | 1 | 1 |

## Exagero: mais arquivos

Medida: classes, N1 x N0; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 5 | 0 | 0 | 0 | +5 |
| HAIKU55 | 2 | 0 | 3 | 0 | -1 |
| SONNET45 | 5 | 0 | 0 | 0 | +5 |
| SONNET46 | 1 | 0 | 4 | 0 | -3 |
| SONNET5 | 5 | 0 | 0 | 0 | +5 |
| SONNET55 | 4 | 1 | 0 | 0 | +4 |
| OPUS46 | 3 | 1 | 1 | 0 | +2 |
| OPUS47 | 5 | 0 | 0 | 0 | +5 |
| OPUS48 | 4 | 0 | 1 | 0 | +3 |
| OPUS5 | 0 | 1 | 4 | 0 | -4 |
| OPUS55 | 0 | 1 | 4 | 0 | -4 |
| **todos** | 34 | 4 | 17 | 0 | +17 |

maioria dos 11 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.31.

**Veredito: inconclusiva.**

## Exagero: estrutura especulativa (CBO)

Medida: cbo, N1 x N0; regra: sem-direcao-continua.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 2 | 0 | 3 | 0 | -1 |
| HAIKU55 | 2 | 0 | 3 | 0 | -1 |
| SONNET45 | 4 | 0 | 1 | 0 | +3 |
| SONNET46 | 2 | 0 | 3 | 0 | -1 |
| SONNET5 | 4 | 0 | 1 | 0 | +3 |
| SONNET55 | 4 | 0 | 1 | 0 | +3 |
| OPUS46 | 4 | 0 | 1 | 0 | +3 |
| OPUS47 | 2 | 0 | 3 | 0 | -1 |
| OPUS48 | 4 | 0 | 1 | 0 | +3 |
| OPUS5 | 1 | 0 | 4 | 0 | -3 |
| OPUS55 | 4 | 0 | 1 | 0 | +3 |
| **todos** | 33 | 0 | 22 | 0 | +11 |

lado maior 33 de 55 pares; apoiada com 36 ou mais, contrariada com 33 ou menos. Tamanho do efeito (saldo / pares): 0.20.

**Veredito: contrariada.**

## Exagero: estrutura especulativa (LCOM)

Medida: lcom, N1 x N0; regra: sem-direcao-continua.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 0 | 0 | 5 | 0 | -5 |
| HAIKU55 | 1 | 0 | 4 | 0 | -3 |
| SONNET45 | 0 | 0 | 5 | 0 | -5 |
| SONNET46 | 1 | 0 | 4 | 0 | -3 |
| SONNET5 | 3 | 0 | 2 | 0 | +1 |
| SONNET55 | 3 | 0 | 2 | 0 | +1 |
| OPUS46 | 4 | 0 | 1 | 0 | +3 |
| OPUS47 | 2 | 0 | 3 | 0 | -1 |
| OPUS48 | 2 | 0 | 3 | 0 | -1 |
| OPUS5 | 5 | 0 | 0 | 0 | +5 |
| OPUS55 | 4 | 0 | 1 | 0 | +3 |
| **todos** | 25 | 0 | 30 | 0 | -5 |

lado maior 30 de 55 pares; apoiada com 36 ou mais, contrariada com 33 ou menos. Tamanho do efeito (saldo / pares): -0.09.

**Veredito: contrariada.**

## Correcao: contas

Medida: contas, N1 x N0; regra: nao-inferioridade, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 1 | 1 | 3 | 0 | -2 |
| HAIKU55 | 0 | 5 | 0 | 0 | +0 |
| SONNET45 | 3 | 2 | 0 | 0 | +3 |
| SONNET46 | 2 | 2 | 1 | 0 | +1 |
| SONNET5 | 1 | 4 | 0 | 0 | +1 |
| SONNET55 | 0 | 5 | 0 | 0 | +0 |
| OPUS46 | 1 | 4 | 0 | 0 | +1 |
| OPUS47 | 0 | 5 | 0 | 0 | +0 |
| OPUS48 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| OPUS55 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 8 | 43 | 4 | 0 | +4 |

saldo (piores - melhores) = -4, limite 7 (13% de 55 pares). Tamanho do efeito (saldo / pares): 0.07.

**Veredito: apoiada.**

## Correcao: recusas

Medida: recusas, N1 x N0; regra: nao-inferioridade, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 0 | 2 | 3 | 0 | -3 |
| HAIKU55 | 0 | 5 | 0 | 0 | +0 |
| SONNET45 | 3 | 1 | 1 | 0 | +2 |
| SONNET46 | 1 | 3 | 1 | 0 | +0 |
| SONNET5 | 0 | 5 | 0 | 0 | +0 |
| SONNET55 | 0 | 5 | 0 | 0 | +0 |
| OPUS46 | 1 | 4 | 0 | 0 | +1 |
| OPUS47 | 2 | 3 | 0 | 0 | +2 |
| OPUS48 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| OPUS55 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 7 | 43 | 5 | 0 | +2 |

saldo (piores - melhores) = -2, limite 7 (13% de 55 pares). Tamanho do efeito (saldo / pares): 0.04.

**Veredito: apoiada.**

## Correcao: nao quebra o build

Medida: build_ok, N1 x N0; regra: nao-inferioridade, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 0 | 5 | 0 | 0 | +0 |
| HAIKU55 | 0 | 5 | 0 | 0 | +0 |
| SONNET45 | 1 | 4 | 0 | 0 | +1 |
| SONNET46 | 0 | 4 | 1 | 0 | -1 |
| SONNET5 | 0 | 5 | 0 | 0 | +0 |
| SONNET55 | 0 | 5 | 0 | 0 | +0 |
| OPUS46 | 1 | 4 | 0 | 0 | +1 |
| OPUS47 | 0 | 5 | 0 | 0 | +0 |
| OPUS48 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| OPUS55 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 2 | 52 | 1 | 0 | +1 |

saldo (piores - melhores) = -1, limite 7 (13% de 55 pares). Tamanho do efeito (saldo / pares): 0.02.

**Veredito: apoiada.**

## Correcao: casos de borda

Medida: borda, N1 x N0; regra: nao-inferioridade, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 0 | 0 | 5 | 0 | -5 |
| HAIKU55 | 0 | 5 | 0 | 0 | +0 |
| SONNET45 | 2 | 2 | 1 | 0 | +1 |
| SONNET46 | 2 | 2 | 1 | 0 | +1 |
| SONNET5 | 1 | 4 | 0 | 0 | +1 |
| SONNET55 | 0 | 5 | 0 | 0 | +0 |
| OPUS46 | 1 | 4 | 0 | 0 | +1 |
| OPUS47 | 2 | 3 | 0 | 0 | +2 |
| OPUS48 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| OPUS55 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 8 | 40 | 7 | 0 | +1 |

saldo (piores - melhores) = -1, limite 7 (13% de 55 pares). Tamanho do efeito (saldo / pares): 0.02.

**Veredito: apoiada.**

## Qualidade: menos code smells

Medida: smells, N1 x N0; regra: direcional, melhor = menos.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 4 | 0 | 1 | 0 | +3 |
| HAIKU55 | 1 | 0 | 4 | 0 | -3 |
| SONNET45 | 4 | 0 | 1 | 0 | +3 |
| SONNET46 | 1 | 0 | 4 | 0 | -3 |
| SONNET5 | 2 | 1 | 2 | 0 | +0 |
| SONNET55 | 2 | 3 | 0 | 0 | +2 |
| OPUS46 | 2 | 1 | 2 | 0 | +0 |
| OPUS47 | 5 | 0 | 0 | 0 | +5 |
| OPUS48 | 2 | 1 | 2 | 0 | +0 |
| OPUS5 | 0 | 0 | 5 | 0 | -5 |
| OPUS55 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 23 | 11 | 21 | 0 | +2 |

maioria dos 11 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.04.

**Veredito: inconclusiva.**

## Custo: tokens de entrada

Medida: tokens, N1 x N0; regra: sem-direcao-continua.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 2 | 0 | 3 | 0 | -1 |
| HAIKU55 | 3 | 0 | 2 | 0 | +1 |
| SONNET45 | 4 | 0 | 1 | 0 | +3 |
| SONNET46 | 3 | 0 | 2 | 0 | +1 |
| SONNET5 | 2 | 0 | 3 | 0 | -1 |
| SONNET55 | 2 | 0 | 3 | 0 | -1 |
| OPUS46 | 5 | 0 | 0 | 0 | +5 |
| OPUS47 | 5 | 0 | 0 | 0 | +5 |
| OPUS48 | 4 | 0 | 1 | 0 | +3 |
| OPUS5 | 2 | 0 | 3 | 0 | -1 |
| OPUS55 | 2 | 0 | 3 | 0 | -1 |
| **todos** | 34 | 0 | 21 | 0 | +13 |

lado maior 34 de 55 pares; apoiada com 36 ou mais, contrariada com 33 ou menos. Tamanho do efeito (saldo / pares): 0.24.

**Veredito: inconclusiva.**

## Custo: tempo de API

Medida: tempo, N1 x N0; regra: sem-direcao-continua.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 1 | 0 | 4 | 0 | -3 |
| HAIKU55 | 4 | 0 | 1 | 0 | +3 |
| SONNET45 | 5 | 0 | 0 | 0 | +5 |
| SONNET46 | 1 | 0 | 4 | 0 | -3 |
| SONNET5 | 3 | 0 | 2 | 0 | +1 |
| SONNET55 | 2 | 0 | 3 | 0 | -1 |
| OPUS46 | 3 | 0 | 2 | 0 | +1 |
| OPUS47 | 5 | 0 | 0 | 0 | +5 |
| OPUS48 | 4 | 0 | 1 | 0 | +3 |
| OPUS5 | 1 | 0 | 4 | 0 | -3 |
| OPUS55 | 1 | 0 | 4 | 0 | -3 |
| **todos** | 30 | 0 | 25 | 0 | +5 |

lado maior 30 de 55 pares; apoiada com 36 ou mais, contrariada com 33 ou menos. Tamanho do efeito (saldo / pares): 0.09.

**Veredito: contrariada.**

## Custo: processo de trabalho (turnos)

Medida: turnos, N1 x N0; regra: sem-direcao-continua.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 2 | 0 | 3 | 0 | -1 |
| HAIKU55 | 3 | 1 | 1 | 0 | +2 |
| SONNET45 | 5 | 0 | 0 | 0 | +5 |
| SONNET46 | 1 | 0 | 4 | 0 | -3 |
| SONNET5 | 2 | 0 | 3 | 0 | -1 |
| SONNET55 | 2 | 0 | 3 | 0 | -1 |
| OPUS46 | 4 | 0 | 1 | 0 | +3 |
| OPUS47 | 5 | 0 | 0 | 0 | +5 |
| OPUS48 | 4 | 0 | 1 | 0 | +3 |
| OPUS5 | 2 | 1 | 2 | 0 | +0 |
| OPUS55 | 1 | 1 | 3 | 0 | -2 |
| **todos** | 31 | 3 | 21 | 0 | +10 |

lado maior 31 de 55 pares; apoiada com 36 ou mais, contrariada com 33 ou menos. Tamanho do efeito (saldo / pares): 0.18.

**Veredito: contrariada.**

## Custo: sinal muda por modelo

Sem regra de pares no OBJETIVO: descritiva. A direcao dos tokens (N1 x N0) em cada modelo:

| modelo | sobem | descem | iguais |
|---|---|---|---|
| HAIKU45 | 2 | 3 | 0 |
| HAIKU55 | 3 | 2 | 0 |
| SONNET45 | 4 | 1 | 0 |
| SONNET46 | 3 | 2 | 0 |
| SONNET5 | 2 | 3 | 0 |
| SONNET55 | 2 | 3 | 0 |
| OPUS46 | 5 | 0 | 0 |
| OPUS47 | 5 | 0 | 0 |
| OPUS48 | 4 | 1 | 0 |
| OPUS5 | 2 | 3 | 0 |
| OPUS55 | 2 | 3 | 0 |

## Modelo: no teto, nao piora (desenho)

Medida: p4_acerto, N1 x N0; regra: nao-inferioridade, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| SONNET55 | 0 | 5 | 0 | 0 | +0 |
| OPUS46 | 0 | 5 | 0 | 0 | +0 |
| OPUS48 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| OPUS55 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 0 | 25 | 0 | 0 | +0 |

saldo (piores - melhores) = 0, limite 3 (13% de 25 pares). Tamanho do efeito (saldo / pares): 0.00.

**Veredito: apoiada.**

## Modelo: no teto, nao piora (correcao)

Medida: pontos, N1 x N0; regra: nao-inferioridade, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| SONNET55 | 0 | 5 | 0 | 0 | +0 |
| OPUS46 | 1 | 4 | 0 | 0 | +1 |
| OPUS48 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| OPUS55 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 1 | 24 | 0 | 0 | +1 |

saldo (piores - melhores) = -1, limite 3 (13% de 25 pares). Tamanho do efeito (saldo / pares): 0.04.

**Veredito: apoiada.**

## Skills: mais efeito no desenho

Medida: p4_acerto, N2 x N1; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 1 | 3 | 1 | 0 | +0 |
| HAIKU55 (teto) | 0 | 5 | 0 | 0 | +0 |
| SONNET45 (teto) | 0 | 5 | 0 | 0 | +0 |
| SONNET46 (teto) | 0 | 5 | 0 | 0 | +0 |
| SONNET5 (teto) | 0 | 5 | 0 | 0 | +0 |
| SONNET55 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS46 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS47 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS48 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS5 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS55 (teto) | 0 | 5 | 0 | 0 | +0 |
| **todos** | 1 | 53 | 1 | 0 | +0 |

maioria dos 1 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.00.

**Veredito: inconclusiva.**

## Skills: mais custo (tokens)

Medida: tokens, N2 x N1; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 4 | 0 | 1 | 0 | +3 |
| HAIKU55 | 0 | 0 | 5 | 0 | -5 |
| SONNET45 | 0 | 0 | 5 | 0 | -5 |
| SONNET46 | 1 | 0 | 4 | 0 | -3 |
| SONNET5 | 2 | 0 | 3 | 0 | -1 |
| SONNET55 | 0 | 0 | 5 | 0 | -5 |
| OPUS46 | 2 | 0 | 3 | 0 | -1 |
| OPUS47 | 3 | 0 | 2 | 0 | +1 |
| OPUS48 | 2 | 0 | 3 | 0 | -1 |
| OPUS5 | 5 | 0 | 0 | 0 | +5 |
| OPUS55 | 2 | 0 | 3 | 0 | -1 |
| **todos** | 21 | 0 | 34 | 0 | -13 |

maioria dos 11 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): -0.24.

**Veredito: contrariada.**

## Skills: mudam o exagero

Medida: p5_exagero, N2 x N1; regra: sem-direcao-binaria.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 2 | 3 | 0 | 0 | +2 |
| HAIKU55 | 0 | 5 | 0 | 0 | +0 |
| SONNET45 | 0 | 5 | 0 | 0 | +0 |
| SONNET46 | 0 | 5 | 0 | 0 | +0 |
| SONNET5 | 0 | 5 | 0 | 0 | +0 |
| SONNET55 | 0 | 5 | 0 | 0 | +0 |
| OPUS46 | 0 | 5 | 0 | 0 | +0 |
| OPUS47 | 0 | 5 | 0 | 0 | +0 |
| OPUS48 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| OPUS55 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 2 | 53 | 0 | 0 | +2 |

2 par(es) nao empatado(s) (2 sobem, 0 descem); com 2, o lado maior precisa de -. Tamanho do efeito (saldo / pares): 0.04.

**Veredito: contrariada.**

## Skills: so valem se carregadas

Sem regra de pares no OBJETIVO: descritiva. Execucoes do N2 que usaram (chamaram a skill):

| modelo | usaram | de |
|---|---|---|
| HAIKU45 | 0 | 5 |
| HAIKU55 | 0 | 5 |
| SONNET45 | 0 | 5 |
| SONNET46 | 0 | 5 |
| SONNET5 | 0 | 5 |
| SONNET55 | 0 | 5 |
| OPUS46 | 0 | 5 |
| OPUS47 | 0 | 5 |
| OPUS48 | 0 | 5 |
| OPUS5 | 0 | 5 |
| OPUS55 | 0 | 5 |

## Processo: corrige mais

Medida: pontos, N3 x N2; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 4 | 0 | 1 | 0 | +3 |
| HAIKU55 (teto) | 0 | 5 | 0 | 0 | +0 |
| SONNET45 | 3 | 0 | 2 | 0 | +1 |
| SONNET46 | 1 | 4 | 0 | 0 | +1 |
| SONNET5 | 1 | 4 | 0 | 0 | +1 |
| SONNET55 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS46 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS47 | 1 | 2 | 2 | 0 | -1 |
| OPUS48 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS5 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS55 (teto) | 0 | 5 | 0 | 0 | +0 |
| **todos** | 10 | 40 | 5 | 0 | +5 |

maioria dos 5 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.09.

**Veredito: inconclusiva.**

## Processo: mais efeito no desenho

Medida: p4_acerto, N3 x N2; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 3 | 2 | 0 | 0 | +3 |
| HAIKU55 (teto) | 0 | 5 | 0 | 0 | +0 |
| SONNET45 (teto) | 0 | 5 | 0 | 0 | +0 |
| SONNET46 (teto) | 0 | 5 | 0 | 0 | +0 |
| SONNET5 (teto) | 0 | 5 | 0 | 0 | +0 |
| SONNET55 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS46 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS47 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS48 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS5 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS55 (teto) | 0 | 5 | 0 | 0 | +0 |
| **todos** | 3 | 52 | 0 | 0 | +3 |

maioria dos 1 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.05.

**Veredito: apoiada.**

## Processo: menos exagero

Medida: p5_exagero, N3 x N2; regra: direcional, melhor = menos.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 2 | 3 | 0 | 0 | +2 |
| HAIKU55 (teto) | 0 | 5 | 0 | 0 | +0 |
| SONNET45 | 1 | 4 | 0 | 0 | +1 |
| SONNET46 (teto) | 0 | 5 | 0 | 0 | +0 |
| SONNET5 (teto) | 0 | 5 | 0 | 0 | +0 |
| SONNET55 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS46 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS47 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS48 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS5 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS55 (teto) | 0 | 5 | 0 | 0 | +0 |
| **todos** | 3 | 52 | 0 | 0 | +3 |

maioria dos 2 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.05.

**Veredito: apoiada.**

## Processo: mais custo (tokens)

Medida: tokens, N3 x N2; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 3 | 0 | 2 | 0 | +1 |
| HAIKU55 | 5 | 0 | 0 | 0 | +5 |
| SONNET45 | 5 | 0 | 0 | 0 | +5 |
| SONNET46 | 4 | 0 | 1 | 0 | +3 |
| SONNET5 | 4 | 0 | 1 | 0 | +3 |
| SONNET55 | 5 | 0 | 0 | 0 | +5 |
| OPUS46 | 2 | 0 | 3 | 0 | -1 |
| OPUS47 | 1 | 0 | 4 | 0 | -3 |
| OPUS48 | 2 | 0 | 3 | 0 | -1 |
| OPUS5 | 5 | 0 | 0 | 0 | +5 |
| OPUS55 | 5 | 0 | 0 | 0 | +5 |
| **todos** | 41 | 0 | 14 | 0 | +27 |

maioria dos 11 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.49.

**Veredito: inconclusiva.**

## Processo: mais custo (tempo)

Medida: tempo, N3 x N2; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 5 | 0 | 0 | 0 | +5 |
| HAIKU55 | 5 | 0 | 0 | 0 | +5 |
| SONNET45 | 5 | 0 | 0 | 0 | +5 |
| SONNET46 | 2 | 0 | 3 | 0 | -1 |
| SONNET5 | 5 | 0 | 0 | 0 | +5 |
| SONNET55 | 5 | 0 | 0 | 0 | +5 |
| OPUS46 | 5 | 0 | 0 | 0 | +5 |
| OPUS47 | 5 | 0 | 0 | 0 | +5 |
| OPUS48 | 5 | 0 | 0 | 0 | +5 |
| OPUS5 | 5 | 0 | 0 | 0 | +5 |
| OPUS55 | 5 | 0 | 0 | 0 | +5 |
| **todos** | 52 | 0 | 3 | 0 | +49 |

maioria dos 11 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.89.

**Veredito: inconclusiva.**

## Processo: so vale se usado

Sem regra de pares no OBJETIVO: descritiva. Execucoes do N3 que usaram (chamaram o revisor):

| modelo | usaram | de |
|---|---|---|
| HAIKU45 | 5 | 5 |
| HAIKU55 | 5 | 5 |
| SONNET45 | 5 | 5 |
| SONNET46 | 5 | 5 |
| SONNET5 | 5 | 5 |
| SONNET55 | 5 | 5 |
| OPUS46 | 5 | 5 |
| OPUS47 | 5 | 5 |
| OPUS48 | 5 | 5 |
| OPUS5 | 5 | 5 |
| OPUS55 | 5 | 5 |

## Tendencia de N0 a N3 (teste de Page, so informacao)

| medida | quartetos | z | p (unilateral) |
|---|---|---|---|
| p4_acerto (subir) | 55 | 2.38 | 0.007 |
| pontos (subir) | 55 | 1.12 | 0.129 |
| cognitiva (cair) | 55 | 6.98 | 0.000 |
| tokens (subir) | 55 | 3.41 | 0.000 |

## Resumo

| hipotese | veredito |
|---|---|
| ★ Desenho: isola cada caso no P4 | apoiada |
| ★ Exagero: aplica onde nao pede | contrariada |
| ★ Correcao: suite inteira | apoiada |
| ★ Qualidade: menos complexidade | apoiada |
| ★ Modelo: efeito maior no mais fraco | apoiada |
| Desenho: isola cada caso no P1 a P3 | apoiada |
| Desenho: nao repete o comum | contrariada |
| Desenho: replicas mais parecidas | descritiva |
| Exagero: mais arquivos | inconclusiva |
| Exagero: estrutura especulativa (CBO) | contrariada |
| Exagero: estrutura especulativa (LCOM) | contrariada |
| Correcao: contas | apoiada |
| Correcao: recusas | apoiada |
| Correcao: nao quebra o build | apoiada |
| Correcao: casos de borda | apoiada |
| Qualidade: menos code smells | inconclusiva |
| Custo: tokens de entrada | inconclusiva |
| Custo: tempo de API | contrariada |
| Custo: processo de trabalho (turnos) | contrariada |
| Custo: sinal muda por modelo | descritiva |
| Modelo: no teto, nao piora (desenho) | apoiada |
| Modelo: no teto, nao piora (correcao) | apoiada |
| Skills: mais efeito no desenho | inconclusiva |
| Skills: mais custo (tokens) | contrariada |
| Skills: mudam o exagero | contrariada |
| Skills: so valem se carregadas | descritiva |
| Processo: corrige mais | inconclusiva |
| Processo: mais efeito no desenho | apoiada |
| Processo: menos exagero | apoiada |
| Processo: mais custo (tokens) | inconclusiva |
| Processo: mais custo (tempo) | inconclusiva |
| Processo: so vale se usado | descritiva |
