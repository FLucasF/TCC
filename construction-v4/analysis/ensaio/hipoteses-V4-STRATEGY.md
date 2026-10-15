# Hipoteses: V4-STRATEGY

Gerado por evaluation/tools/hipoteses.mjs. Regras: OBJETIVO, secao 4.1. 5 replicas x 5 modelos x 4 niveis.

## ★ Desenho: isola cada caso no P4

Medida: p4_acerto, N1 x N0; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 0 | 5 | 0 | 0 | +0 |
| SONNET45 | 1 | 4 | 0 | 0 | +1 |
| OPUS46 | 1 | 1 | 0 | 3 | +1 |
| SONNET5 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS5 (teto) | 0 | 5 | 0 | 0 | +0 |
| **todos** | 2 | 20 | 0 | 3 | +2 |

maioria dos 3 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.09.

**Veredito: apoiada.**

## ★ Exagero: aplica onde nao pede

Medida: p5_exagero, N1 x N0; regra: sem-direcao-binaria.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 0 | 5 | 0 | 0 | +0 |
| SONNET45 | 0 | 5 | 0 | 0 | +0 |
| OPUS46 | 0 | 2 | 0 | 3 | +0 |
| SONNET5 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 0 | 22 | 0 | 3 | +0 |

0 par(es) nao empatado(s) (0 sobem, 0 descem); com 0, o lado maior precisa de -. Tamanho do efeito (saldo / pares): 0.00.

**Veredito: contrariada.**

## ★ Correcao: suite inteira

Medida: pontos, N1 x N0; regra: nao-inferioridade, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 3 | 1 | 1 | 0 | +2 |
| SONNET45 | 4 | 0 | 1 | 0 | +3 |
| OPUS46 | 0 | 5 | 0 | 0 | +0 |
| SONNET5 | 1 | 4 | 0 | 0 | +1 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 8 | 15 | 2 | 0 | +6 |

saldo (piores - melhores) = -6, limite 3 (13% de 25 pares). Tamanho do efeito (saldo / pares): 0.24.

**Veredito: apoiada.**

## ★ Qualidade: menos complexidade

Medida: cognitiva, N1 x N0; regra: direcional, melhor = menos.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 1 | 0 | 4 | 0 | -3 |
| SONNET45 | 5 | 0 | 0 | 0 | +5 |
| OPUS46 | 5 | 0 | 0 | 0 | +5 |
| SONNET5 | 4 | 0 | 1 | 0 | +3 |
| OPUS5 | 5 | 0 | 0 | 0 | +5 |
| **todos** | 20 | 0 | 5 | 0 | +15 |

maioria dos 5 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.60.

**Veredito: inconclusiva.**

## ★ Modelo: efeito maior no mais fraco

Pares do desenho no P4 (N1 x N0); regra: modelo.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 0 | 5 | 0 | 0 | +0 |
| SONNET45 | 1 | 4 | 0 | 0 | +1 |
| OPUS46 | 1 | 1 | 0 | 3 | +1 |
| SONNET5 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 2 | 20 | 0 | 3 | +2 |

mais fraco(s) no N0: HAIKU45 (acerto medio 0.00); maior saldo: SONNET45, OPUS46 (1). Tamanho do efeito (saldo / pares): 0.09.

**Veredito: inconclusiva (empate no maior saldo).**

## Desenho: isola cada caso no P1 a P3

Medida: p1p3_acertos, N1 x N0; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 1 | 4 | 0 | 0 | +1 |
| SONNET45 | 5 | 0 | 0 | 0 | +5 |
| OPUS46 | 2 | 0 | 0 | 3 | +2 |
| SONNET5 | 2 | 3 | 0 | 0 | +2 |
| OPUS5 (teto) | 0 | 5 | 0 | 0 | +0 |
| **todos** | 10 | 12 | 0 | 3 | +10 |

maioria dos 4 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.45.

**Veredito: apoiada.**

## Desenho: nao repete o comum

Medida: duplicacao, N1 x N0; regra: direcional, melhor = menos.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 0 | 4 | 1 | 0 | -1 |
| SONNET45 | 0 | 0 | 5 | 0 | -5 |
| OPUS46 | 3 | 1 | 1 | 0 | +2 |
| SONNET5 | 1 | 2 | 2 | 0 | -1 |
| OPUS5 | 2 | 1 | 2 | 0 | +0 |
| **todos** | 6 | 8 | 11 | 0 | -5 |

maioria dos 5 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): -0.20.

**Veredito: contrariada.**

## Desenho: replicas mais parecidas

Sem regra de pares no OBJETIVO: descritiva. Perfis diferentes do Semgrep (P1 a P5) entre as replicas de cada modelo; menos perfis = mais parecidas.

| modelo | N0 | N1 |
|---|---|---|
| HAIKU45 | 3 | 4 |
| SONNET45 | 2 | 1 |
| OPUS46 | 4 | 2 |
| SONNET5 | 2 | 1 |
| OPUS5 | 1 | 1 |

## Exagero: mais arquivos

Medida: classes, N1 x N0; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 5 | 0 | 0 | 0 | +5 |
| SONNET45 | 5 | 0 | 0 | 0 | +5 |
| OPUS46 | 2 | 0 | 3 | 0 | -1 |
| SONNET5 | 3 | 0 | 2 | 0 | +1 |
| OPUS5 | 1 | 0 | 4 | 0 | -3 |
| **todos** | 16 | 0 | 9 | 0 | +7 |

maioria dos 5 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.28.

**Veredito: inconclusiva.**

## Exagero: estrutura especulativa (CBO)

Medida: cbo, N1 x N0; regra: sem-direcao-continua.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 2 | 0 | 3 | 0 | -1 |
| SONNET45 | 5 | 0 | 0 | 0 | +5 |
| OPUS46 | 0 | 0 | 5 | 0 | -5 |
| SONNET5 | 3 | 0 | 2 | 0 | +1 |
| OPUS5 | 3 | 0 | 2 | 0 | +1 |
| **todos** | 13 | 0 | 12 | 0 | +1 |

lado maior 13 de 25 pares; apoiada com 18 ou mais, contrariada com 15 ou menos. Tamanho do efeito (saldo / pares): 0.04.

**Veredito: contrariada.**

## Exagero: estrutura especulativa (LCOM)

Medida: lcom, N1 x N0; regra: sem-direcao-continua.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 2 | 0 | 3 | 0 | -1 |
| SONNET45 | 0 | 0 | 5 | 0 | -5 |
| OPUS46 | 0 | 0 | 5 | 0 | -5 |
| SONNET5 | 2 | 0 | 3 | 0 | -1 |
| OPUS5 | 3 | 0 | 2 | 0 | +1 |
| **todos** | 7 | 0 | 18 | 0 | -11 |

lado maior 18 de 25 pares; apoiada com 18 ou mais, contrariada com 15 ou menos. Tamanho do efeito (saldo / pares): -0.44.

**Veredito: apoiada (altera).**

## Correcao: contas

Medida: contas, N1 x N0; regra: nao-inferioridade, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 1 | 1 | 3 | 0 | -2 |
| SONNET45 | 4 | 0 | 1 | 0 | +3 |
| OPUS46 | 0 | 5 | 0 | 0 | +0 |
| SONNET5 | 1 | 4 | 0 | 0 | +1 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 6 | 15 | 4 | 0 | +2 |

saldo (piores - melhores) = -2, limite 3 (13% de 25 pares). Tamanho do efeito (saldo / pares): 0.08.

**Veredito: apoiada.**

## Correcao: recusas

Medida: recusas, N1 x N0; regra: nao-inferioridade, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 3 | 1 | 1 | 0 | +2 |
| SONNET45 | 2 | 2 | 1 | 0 | +1 |
| OPUS46 | 0 | 5 | 0 | 0 | +0 |
| SONNET5 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 5 | 18 | 2 | 0 | +3 |

saldo (piores - melhores) = -3, limite 3 (13% de 25 pares). Tamanho do efeito (saldo / pares): 0.12.

**Veredito: apoiada.**

## Correcao: nao quebra o build

Medida: build_ok, N1 x N0; regra: nao-inferioridade, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 1 | 4 | 0 | 0 | +1 |
| SONNET45 | 1 | 4 | 0 | 0 | +1 |
| OPUS46 | 0 | 5 | 0 | 0 | +0 |
| SONNET5 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 2 | 23 | 0 | 0 | +2 |

saldo (piores - melhores) = -2, limite 3 (13% de 25 pares). Tamanho do efeito (saldo / pares): 0.08.

**Veredito: apoiada.**

## Correcao: casos de borda

Medida: borda, N1 x N0; regra: nao-inferioridade, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 4 | 0 | 1 | 0 | +3 |
| SONNET45 | 2 | 2 | 1 | 0 | +1 |
| OPUS46 | 0 | 5 | 0 | 0 | +0 |
| SONNET5 | 1 | 4 | 0 | 0 | +1 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 7 | 16 | 2 | 0 | +5 |

saldo (piores - melhores) = -5, limite 3 (13% de 25 pares). Tamanho do efeito (saldo / pares): 0.20.

**Veredito: apoiada.**

## Qualidade: menos code smells

Medida: smells, N1 x N0; regra: direcional, melhor = menos.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 3 | 1 | 1 | 0 | +2 |
| SONNET45 | 3 | 0 | 2 | 0 | +1 |
| OPUS46 | 4 | 0 | 1 | 0 | +3 |
| SONNET5 | 1 | 1 | 3 | 0 | -2 |
| OPUS5 | 0 | 1 | 4 | 0 | -4 |
| **todos** | 11 | 3 | 11 | 0 | +0 |

maioria dos 5 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.00.

**Veredito: inconclusiva.**

## Custo: tokens de entrada

Medida: tokens, N1 x N0; regra: sem-direcao-continua.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 2 | 0 | 3 | 0 | -1 |
| SONNET45 | 3 | 0 | 2 | 0 | +1 |
| OPUS46 | 2 | 0 | 3 | 0 | -1 |
| SONNET5 | 3 | 0 | 2 | 0 | +1 |
| OPUS5 | 2 | 0 | 3 | 0 | -1 |
| **todos** | 12 | 0 | 13 | 0 | -1 |

lado maior 13 de 25 pares; apoiada com 18 ou mais, contrariada com 15 ou menos. Tamanho do efeito (saldo / pares): -0.04.

**Veredito: contrariada.**

## Custo: tempo de API

Medida: tempo, N1 x N0; regra: sem-direcao-continua.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 2 | 0 | 3 | 0 | -1 |
| SONNET45 | 3 | 0 | 2 | 0 | +1 |
| OPUS46 | 2 | 0 | 3 | 0 | -1 |
| SONNET5 | 2 | 0 | 3 | 0 | -1 |
| OPUS5 | 2 | 0 | 3 | 0 | -1 |
| **todos** | 11 | 0 | 14 | 0 | -3 |

lado maior 14 de 25 pares; apoiada com 18 ou mais, contrariada com 15 ou menos. Tamanho do efeito (saldo / pares): -0.12.

**Veredito: contrariada.**

## Custo: processo de trabalho (turnos)

Medida: turnos, N1 x N0; regra: sem-direcao-continua.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 3 | 0 | 2 | 0 | +1 |
| SONNET45 | 5 | 0 | 0 | 0 | +5 |
| OPUS46 | 1 | 0 | 4 | 0 | -3 |
| SONNET5 | 3 | 0 | 2 | 0 | +1 |
| OPUS5 | 1 | 0 | 4 | 0 | -3 |
| **todos** | 13 | 0 | 12 | 0 | +1 |

lado maior 13 de 25 pares; apoiada com 18 ou mais, contrariada com 15 ou menos. Tamanho do efeito (saldo / pares): 0.04.

**Veredito: contrariada.**

## Custo: sinal muda por modelo

Sem regra de pares no OBJETIVO: descritiva. A direcao dos tokens (N1 x N0) em cada modelo:

| modelo | sobem | descem | iguais |
|---|---|---|---|
| HAIKU45 | 2 | 3 | 0 |
| SONNET45 | 3 | 2 | 0 |
| OPUS46 | 2 | 3 | 0 |
| SONNET5 | 3 | 2 | 0 |
| OPUS5 | 2 | 3 | 0 |

## Modelo: no teto, nao piora (desenho)

Medida: p4_acerto, N1 x N0; regra: nao-inferioridade, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| SONNET5 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 0 | 10 | 0 | 0 | +0 |

saldo (piores - melhores) = 0, limite 1 (13% de 10 pares). Tamanho do efeito (saldo / pares): 0.00.

**Veredito: apoiada.**

## Modelo: no teto, nao piora (correcao)

Medida: pontos, N1 x N0; regra: nao-inferioridade, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| SONNET5 | 1 | 4 | 0 | 0 | +1 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 1 | 9 | 0 | 0 | +1 |

saldo (piores - melhores) = -1, limite 1 (13% de 10 pares). Tamanho do efeito (saldo / pares): 0.10.

**Veredito: apoiada.**

## Skills: mais efeito no desenho

Medida: p4_acerto, N2 x N1; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 2 | 3 | 0 | 0 | +2 |
| SONNET45 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS46 | 0 | 1 | 0 | 4 | +0 |
| SONNET5 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS5 (teto) | 0 | 5 | 0 | 0 | +0 |
| **todos** | 2 | 19 | 0 | 4 | +2 |

maioria dos 2 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.10.

**Veredito: inconclusiva.**

## Skills: mais custo (tokens)

Medida: tokens, N2 x N1; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 1 | 0 | 4 | 0 | -3 |
| SONNET45 | 4 | 0 | 1 | 0 | +3 |
| OPUS46 | 3 | 0 | 2 | 0 | +1 |
| SONNET5 | 3 | 0 | 2 | 0 | +1 |
| OPUS5 | 4 | 0 | 1 | 0 | +3 |
| **todos** | 15 | 0 | 10 | 0 | +5 |

maioria dos 5 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.20.

**Veredito: inconclusiva.**

## Skills: mudam o exagero

Medida: p5_exagero, N2 x N1; regra: sem-direcao-binaria.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 0 | 5 | 0 | 0 | +0 |
| SONNET45 | 1 | 4 | 0 | 0 | +1 |
| OPUS46 | 0 | 1 | 0 | 4 | +0 |
| SONNET5 | 0 | 5 | 0 | 0 | +0 |
| OPUS5 | 0 | 5 | 0 | 0 | +0 |
| **todos** | 1 | 20 | 0 | 4 | +1 |

1 par(es) nao empatado(s) (1 sobem, 0 descem); com 1, o lado maior precisa de -. Tamanho do efeito (saldo / pares): 0.05.

**Veredito: contrariada.**

## Skills: so valem se carregadas

Sem regra de pares no OBJETIVO: descritiva. Execucoes do N2 que usaram (chamaram a skill):

| modelo | usaram | de |
|---|---|---|
| HAIKU45 | 0 | 5 |
| SONNET45 | 0 | 5 |
| OPUS46 | 0 | 5 |
| SONNET5 | 0 | 5 |
| OPUS5 | 0 | 5 |

## Processo: corrige mais

Medida: pontos, N3 x N2; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 3 | 1 | 1 | 0 | +2 |
| SONNET45 | 1 | 1 | 3 | 0 | -2 |
| OPUS46 | 1 | 4 | 0 | 0 | +1 |
| SONNET5 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS5 (teto) | 0 | 5 | 0 | 0 | +0 |
| **todos** | 5 | 16 | 4 | 0 | +1 |

maioria dos 3 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.04.

**Veredito: inconclusiva.**

## Processo: mais efeito no desenho

Medida: p4_acerto, N3 x N2; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 1 | 3 | 1 | 0 | +0 |
| SONNET45 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS46 | 0 | 4 | 0 | 1 | +0 |
| SONNET5 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS5 (teto) | 0 | 5 | 0 | 0 | +0 |
| **todos** | 1 | 22 | 1 | 1 | +0 |

maioria dos 2 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.00.

**Veredito: inconclusiva.**

## Processo: menos exagero

Medida: p5_exagero, N3 x N2; regra: direcional, melhor = menos.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 (teto) | 0 | 5 | 0 | 0 | +0 |
| SONNET45 | 1 | 4 | 0 | 0 | +1 |
| OPUS46 | 0 | 4 | 0 | 1 | +0 |
| SONNET5 (teto) | 0 | 5 | 0 | 0 | +0 |
| OPUS5 (teto) | 0 | 5 | 0 | 0 | +0 |
| **todos** | 1 | 23 | 0 | 1 | +1 |

maioria dos 2 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.04.

**Veredito: inconclusiva.**

## Processo: mais custo (tokens)

Medida: tokens, N3 x N2; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 5 | 0 | 0 | 0 | +5 |
| SONNET45 | 5 | 0 | 0 | 0 | +5 |
| OPUS46 | 2 | 0 | 3 | 0 | -1 |
| SONNET5 | 4 | 0 | 1 | 0 | +3 |
| OPUS5 | 3 | 0 | 2 | 0 | +1 |
| **todos** | 19 | 0 | 6 | 0 | +13 |

maioria dos 5 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 0.52.

**Veredito: inconclusiva.**

## Processo: mais custo (tempo)

Medida: tempo, N3 x N2; regra: direcional, melhor = mais.

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| HAIKU45 | 5 | 0 | 0 | 0 | +5 |
| SONNET45 | 5 | 0 | 0 | 0 | +5 |
| OPUS46 | 5 | 0 | 0 | 0 | +5 |
| SONNET5 | 5 | 0 | 0 | 0 | +5 |
| OPUS5 | 5 | 0 | 0 | 0 | +5 |
| **todos** | 25 | 0 | 0 | 0 | +25 |

maioria dos 5 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores. Tamanho do efeito (saldo / pares): 1.00.

**Veredito: apoiada.**

## Processo: so vale se usado

Sem regra de pares no OBJETIVO: descritiva. Execucoes do N3 que usaram (chamaram o revisor):

| modelo | usaram | de |
|---|---|---|
| HAIKU45 | 5 | 5 |
| SONNET45 | 5 | 5 |
| OPUS46 | 5 | 5 |
| SONNET5 | 5 | 5 |
| OPUS5 | 5 | 5 |

## Tendencia de N0 a N3 (teste de Page, so informacao)

| medida | quartetos | z | p (unilateral) |
|---|---|---|---|
| p4_acerto (subir) | 21 | 0.83 | 0.202 |
| pontos (subir) | 25 | 0.66 | 0.255 |
| cognitiva (cair) | 25 | 2.53 | 0.004 |
| tokens (subir) | 25 | 3.05 | 0.001 |

## Resumo

| hipotese | veredito |
|---|---|
| ★ Desenho: isola cada caso no P4 | apoiada |
| ★ Exagero: aplica onde nao pede | contrariada |
| ★ Correcao: suite inteira | apoiada |
| ★ Qualidade: menos complexidade | inconclusiva |
| ★ Modelo: efeito maior no mais fraco | inconclusiva (empate no maior saldo) |
| Desenho: isola cada caso no P1 a P3 | apoiada |
| Desenho: nao repete o comum | contrariada |
| Desenho: replicas mais parecidas | descritiva |
| Exagero: mais arquivos | inconclusiva |
| Exagero: estrutura especulativa (CBO) | contrariada |
| Exagero: estrutura especulativa (LCOM) | apoiada (altera) |
| Correcao: contas | apoiada |
| Correcao: recusas | apoiada |
| Correcao: nao quebra o build | apoiada |
| Correcao: casos de borda | apoiada |
| Qualidade: menos code smells | inconclusiva |
| Custo: tokens de entrada | contrariada |
| Custo: tempo de API | contrariada |
| Custo: processo de trabalho (turnos) | contrariada |
| Custo: sinal muda por modelo | descritiva |
| Modelo: no teto, nao piora (desenho) | apoiada |
| Modelo: no teto, nao piora (correcao) | apoiada |
| Skills: mais efeito no desenho | inconclusiva |
| Skills: mais custo (tokens) | inconclusiva |
| Skills: mudam o exagero | contrariada |
| Skills: so valem se carregadas | descritiva |
| Processo: corrige mais | inconclusiva |
| Processo: mais efeito no desenho | inconclusiva |
| Processo: menos exagero | inconclusiva |
| Processo: mais custo (tokens) | inconclusiva |
| Processo: mais custo (tempo) | apoiada |
| Processo: so vale se usado | descritiva |
