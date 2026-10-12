# Resultados do V5

> Gerado em 12/10/2026 a partir dos arquivos do `TCC_V5` (a suíte, as métricas, o Semgrep conferido pela leitura do Lucas e o relatório `analysis/hipoteses-V5-STRATEGY.md`).
> **Pré-registrado:** as seções 2 a 5 (a conferência e os vereditos, pelas regras do §4.1 do `OBJETIVO`).
> **Descritivo:** as seções 6 a 9 (médias e contagens, sem regra de decisão). Só agregados por modelo e nível: nenhum código de pacote, para não atrapalhar a releitura.

## 1. O lote

| | |
|---|---|
| execuções | **220** (11 modelos × 4 níveis × 5 réplicas), em 55 quartetos simultâneos |
| cortadas pela cota | **0** (nenhum quarteto refeito) |
| compilaram e subiram | **215 de 220** |
| passaram a suíte inteira (21 de 21) | **177 de 220** |
| custo total (estimado pelo Claude Code) | **US$ 346.16** |
| Claude Code / imagem | 2.1.288 / `experimento-harness:v5` |

## 2. A conferência do Semgrep (leitura do Lucas, 44 pacotes às cegas)

| pergunta | concordaram | decisão |
|---|---|---|
| P4_localizacao | 44 de 44 | **vale** para o lote |
| P4_selecao | 44 de 44 | **vale** para o lote |
| P5_forma | 42 de 44 | **vale** para o lote |
| P5_proporcao | 44 de 44 | **vale** para o lote |

Regra de saída: a pergunta vale para os 220 pacotes com pelo menos 40 de 44 (90%). **As 4 valem.** No total, 174 de 176 respostas iguais.

## 3. As 5 hipóteses principais (N1 × N0: o `CLAUDE.md` contra nenhum harness)

| hipótese | tipo de regra | veredito | o que decidiu |
|---|---|---|---|
| Desenho: isola cada caso no P4 | direcional | **apoiada** | maioria dos 6 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores; efeito 0.25 |
| Exagero: aplica onde não pede | altera? (sim/não) | **contrariada** | 1 par(es) nao empatado(s) (1 sobem, 0 descem); com 1, o lado maior precisa de -; efeito 0.02 |
| Correção: suíte inteira | não piora | **apoiada** | saldo (piores - melhores) = -3, limite 7 (13% de 55 pares); efeito 0.05 |
| Qualidade: menos complexidade | direcional | **apoiada** | maioria dos 11 modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores; efeito 0.82 |
| Modelo: efeito maior no mais fraco | modelo | **apoiada** | mais fraco(s) no N0: HAIKU45, OPUS47 (acerto medio 0.00); maior saldo: OPUS47 (5); efeito 0.25 |

Na regra "altera?" (sem direção), **contrariada** quer dizer que o harness **não muda** a medida.

## 4. Todas as hipóteses, por bloco

### Desenho

| hipótese | comparação | tipo de regra | veredito |
|---|---|---|---|
| ★ isola cada caso no P4 | N1 × N0 | direcional | **apoiada** |
| isola cada caso no P1 a P3 | N1 × N0 | direcional | **apoiada** |
| não repete o comum | N1 × N0 | direcional | **contrariada** |
| réplicas mais parecidas | — | descritiva | **descritiva** |

### Exagero

| hipótese | comparação | tipo de regra | veredito |
|---|---|---|---|
| ★ aplica onde não pede | N1 × N0 | altera? (sim/não) | **contrariada** |
| mais arquivos | N1 × N0 | direcional | **inconclusiva** |
| estrutura especulativa (CBO) | N1 × N0 | altera? (contínua) | **contrariada** |
| estrutura especulativa (LCOM) | N1 × N0 | altera? (contínua) | **contrariada** |

### Correção

| hipótese | comparação | tipo de regra | veredito |
|---|---|---|---|
| ★ suíte inteira | N1 × N0 | não piora | **apoiada** |
| contas | N1 × N0 | não piora | **apoiada** |
| recusas | N1 × N0 | não piora | **apoiada** |
| não quebra o build | N1 × N0 | não piora | **apoiada** |
| casos de borda | N1 × N0 | não piora | **apoiada** |

### Qualidade

| hipótese | comparação | tipo de regra | veredito |
|---|---|---|---|
| ★ menos complexidade | N1 × N0 | direcional | **apoiada** |
| menos code smells | N1 × N0 | direcional | **inconclusiva** |

### Modelo

| hipótese | comparação | tipo de regra | veredito |
|---|---|---|---|
| ★ efeito maior no mais fraco | N1 × N0 | modelo | **apoiada** |
| no teto, não piora (desenho) | N1 × N0 | não piora | **apoiada** |
| no teto, não piora (correcao) | N1 × N0 | não piora | **apoiada** |

### Custo

| hipótese | comparação | tipo de regra | veredito |
|---|---|---|---|
| tokens de entrada | N1 × N0 | altera? (contínua) | **inconclusiva** |
| tempo de API | N1 × N0 | altera? (contínua) | **contrariada** |
| processo de trabalho (turnos) | N1 × N0 | altera? (contínua) | **contrariada** |
| sinal muda por modelo | — | descritiva | **descritiva** |

### Skills

| hipótese | comparação | tipo de regra | veredito |
|---|---|---|---|
| mais efeito no desenho | N2 × N1 | direcional | **inconclusiva** |
| mais custo (tokens) | N2 × N1 | direcional | **contrariada** |
| mudam o exagero | N2 × N1 | altera? (sim/não) | **contrariada** |
| só valem se carregadas | — | descritiva | **descritiva** |

### Processo

| hipótese | comparação | tipo de regra | veredito |
|---|---|---|---|
| corrige mais | N3 × N2 | direcional | **inconclusiva** |
| mais efeito no desenho | N3 × N2 | direcional | **apoiada** |
| menos exagero | N3 × N2 | direcional | **apoiada** |
| mais custo (tokens) | N3 × N2 | direcional | **inconclusiva** |
| mais custo (tempo) | N3 × N2 | direcional | **inconclusiva** |
| só vale se usado | — | descritiva | **descritiva** |

## 5. A hipótese principal do desenho, por modelo (P4, N1 × N0, 5 pares cada)

| modelo | melhores | iguais | piores | sem dado | saldo |
|---|---|---|---|---|---|
| Haiku 4.5 | 1 | 4 | 0 | 0 | +1 |
| Haiku 5.5 | 2 | 3 | 0 | 0 | +2 |
| Sonnet 4.5 | 3 | 2 | 0 | 0 | +3 |
| Sonnet 4.6 | 2 | 3 | 0 | 0 | +2 |
| Sonnet 5 | 1 | 4 | 0 | 0 | +1 |
| Sonnet 5.5 (teto) | 0 | 5 | 0 | 0 | +0 |
| Opus 4.6 (teto) | 0 | 5 | 0 | 0 | +0 |
| Opus 4.7 | 5 | 0 | 0 | 0 | +5 |
| Opus 4.8 (teto) | 0 | 5 | 0 | 0 | +0 |
| Opus 5 (teto) | 0 | 5 | 0 | 0 | +0 |
| Opus 5.5 (teto) | 0 | 5 | 0 | 0 | +0 |
| **todos** | 14 | 41 | 0 | 0 | +14 |

"(teto)": o modelo já acertava o P4 nas 5 réplicas do N0; ali o harness só podia empatar ou piorar, e saiu da contagem de "melhor" (regra do §4.1).

### Tendência de N0 a N3 (teste de Page, só informação)

| medida | quartetos | z | p (unilateral) |
|---|---|---|---|
| p4_acerto (subir) | 55 | 2.38 | 0.007 |
| pontos (subir) | 55 | 1.12 | 0.129 |
| cognitiva (cair) | 55 | 6.98 | 0.000 |
| tokens (subir) | 55 | 3.41 | 0.000 |

## 6. Descritivo: a escada, por nível (55 execuções em cada)

| medida | N0 | N1 | N2 | N3 |
|---|---|---|---|---|
| P4 certo (isolado e sem condicional no cálculo) | 67% | 93% | 93% | 98% |
| P4 isolado | 67% | 93% | 93% | 98% |
| P5 com exagero (estrutura) | 0% | 2% | 5% | 0% |
| suíte inteira (21 de 21) | 71% | 84% | 80% | 87% |
| pontos da suíte (média, de 21) | 19.5 | 19.9 | 20.0 | 20.2 |
| nota A (média, 0 a 100) | 82.2 | 93.8 | 94.0 | 96.8 |
| complexidade cognitiva (média) | 33.7 | 22.9 | 23.5 | 22.5 |
| linhas de código (média) | 518.5 | 584.4 | 613.6 | 570.7 |
| classes (média) | 22.2 | 28.1 | 30.2 | 27.7 |
| duplicação (% de linhas, média) | 1.2 | 2.5 | 2.5 | 2.1 |
| tokens de entrada (média) | 1340 mil | 1571 mil | 1490 mil | 1987 mil |
| tempo de API (min, média) | 5.8 | 6.2 | 6.3 | 8.9 |
| turnos (média) | 31.0 | 35.3 | 35.8 | 42.2 |
| custo (US$, média) | 1.32 | 1.52 | 1.47 | 1.98 |

## 7. Descritivo: por modelo e nível

### P4 certo (de 5 réplicas)

| modelo | N0 | N1 | N2 | N3 |
|---|---|---|---|---|
| Haiku 4.5 | 0 | 1 | 1 | 4 |
| Haiku 5.5 | 3 | 5 | 5 | 5 |
| Sonnet 4.5 | 2 | 5 | 5 | 5 |
| Sonnet 4.6 | 3 | 5 | 5 | 5 |
| Sonnet 5 | 4 | 5 | 5 | 5 |
| Sonnet 5.5 | 5 | 5 | 5 | 5 |
| Opus 4.6 | 5 | 5 | 5 | 5 |
| Opus 4.7 | 0 | 5 | 5 | 5 |
| Opus 4.8 | 5 | 5 | 5 | 5 |
| Opus 5 | 5 | 5 | 5 | 5 |
| Opus 5.5 | 5 | 5 | 5 | 5 |

### P5 com exagero (de 5 réplicas)

| modelo | N0 | N1 | N2 | N3 |
|---|---|---|---|---|
| Haiku 4.5 | 0 | 0 | 2 | 0 |
| Haiku 5.5 | 0 | 0 | 0 | 0 |
| Sonnet 4.5 | 0 | 1 | 1 | 0 |
| Sonnet 4.6 | 0 | 0 | 0 | 0 |
| Sonnet 5 | 0 | 0 | 0 | 0 |
| Sonnet 5.5 | 0 | 0 | 0 | 0 |
| Opus 4.6 | 0 | 0 | 0 | 0 |
| Opus 4.7 | 0 | 0 | 0 | 0 |
| Opus 4.8 | 0 | 0 | 0 | 0 |
| Opus 5 | 0 | 0 | 0 | 0 |
| Opus 5.5 | 0 | 0 | 0 | 0 |

### Suíte inteira (de 5 réplicas)

| modelo | N0 | N1 | N2 | N3 |
|---|---|---|---|---|
| Haiku 4.5 | 1 | 0 | 0 | 2 |
| Haiku 5.5 | 5 | 5 | 5 | 5 |
| Sonnet 4.5 | 0 | 2 | 2 | 3 |
| Sonnet 4.6 | 2 | 4 | 4 | 5 |
| Sonnet 5 | 4 | 5 | 4 | 5 |
| Sonnet 5.5 | 5 | 5 | 5 | 5 |
| Opus 4.6 | 4 | 5 | 5 | 5 |
| Opus 4.7 | 3 | 5 | 4 | 3 |
| Opus 4.8 | 5 | 5 | 5 | 5 |
| Opus 5 | 5 | 5 | 5 | 5 |
| Opus 5.5 | 5 | 5 | 5 | 5 |

### Complexidade cognitiva (média)

| modelo | N0 | N1 | N2 | N3 |
|---|---|---|---|---|
| Haiku 4.5 | 63.6 | 53.0 | 48.4 | 46.8 |
| Haiku 5.5 | 21.2 | 12.8 | 12.0 | 11.0 |
| Sonnet 4.5 | 43.4 | 34.8 | 32.4 | 30.6 |
| Sonnet 4.6 | 36.6 | 20.2 | 29.0 | 29.0 |
| Sonnet 5 | 27.6 | 19.4 | 23.6 | 20.0 |
| Sonnet 5.5 | 23.8 | 14.2 | 14.0 | 13.8 |
| Opus 4.6 | 31.4 | 28.4 | 26.0 | 30.0 |
| Opus 4.7 | 48.0 | 23.8 | 28.2 | 21.0 |
| Opus 4.8 | 33.2 | 20.0 | 22.8 | 21.2 |
| Opus 5 | 24.4 | 15.6 | 14.0 | 15.4 |
| Opus 5.5 | 18.0 | 10.2 | 8.2 | 8.8 |

### Custo por execução (US$, média)

| modelo | N0 | N1 | N2 | N3 |
|---|---|---|---|---|
| Haiku 4.5 | 0.47 | 0.39 | 0.59 | 0.79 |
| Haiku 5.5 | 1.01 | 1.08 | 0.93 | 1.34 |
| Sonnet 4.5 | 0.80 | 1.00 | 0.72 | 1.40 |
| Sonnet 4.6 | 2.27 | 2.04 | 2.07 | 2.79 |
| Sonnet 5 | 1.00 | 1.26 | 0.99 | 1.52 |
| Sonnet 5.5 | 0.34 | 0.34 | 0.29 | 0.52 |
| Opus 4.6 | 1.93 | 2.16 | 2.30 | 3.00 |
| Opus 4.7 | 1.40 | 2.43 | 2.35 | 2.48 |
| Opus 4.8 | 2.36 | 3.40 | 2.95 | 3.50 |
| Opus 5 | 2.04 | 1.88 | 2.22 | 3.19 |
| Opus 5.5 | 0.88 | 0.78 | 0.76 | 1.30 |

## 8. Descritivo: por modelo (as 20 execuções de cada)

| modelo | P4 certo | suíte inteira | complexidade cognitiva | tokens de entrada | tempo de API (min) | custo total (US$) |
|---|---|---|---|---|---|---|
| Haiku 4.5 | 6 de 20 | 3 de 20 | 53.0 | 2412 mil | 5.6 | 11.17 |
| Haiku 5.5 | 18 de 20 | 20 de 20 | 14.3 | 1239 mil | 3.9 | 21.78 |
| Sonnet 4.5 | 17 de 20 | 7 de 20 | 35.3 | 988 mil | 6.4 | 19.56 |
| Sonnet 4.6 | 18 de 20 | 15 de 20 | 28.7 | 2535 mil | 14.1 | 45.86 |
| Sonnet 5 | 19 de 20 | 18 de 20 | 22.6 | 2529 mil | 7.0 | 23.84 |
| Sonnet 5.5 | 20 de 20 | 20 de 20 | 16.4 | 206 mil | 2.0 | 7.46 |
| Opus 4.6 | 20 de 20 | 19 de 20 | 28.9 | 2005 mil | 9.4 | 46.97 |
| Opus 4.7 | 15 de 20 | 15 de 20 | 30.3 | 1519 mil | 6.2 | 43.28 |
| Opus 4.8 | 20 de 20 | 20 de 20 | 24.3 | 2469 mil | 9.3 | 61.02 |
| Opus 5 | 20 de 20 | 20 de 20 | 17.4 | 1278 mil | 7.9 | 46.62 |
| Opus 5.5 | 20 de 20 | 20 de 20 | 11.3 | 385 mil | 3.4 | 18.57 |

## 9. Descritivo: CPU e memória dos agentes (o monitor de recursos)

| medida | N0 | N1 | N2 | N3 |
|---|---|---|---|---|
| execuções com medida | 54 | 55 | 55 | 54 |
| pico de memória do agente (MiB, média) | 554.9 | 563.8 | 549.6 | 584.4 |
| CPU média do agente (% de 1 núcleo) | 19.4 | 18.5 | 16.5 | 15.4 |
| pico de CPU do agente (% de 1 núcleo, média) | 163.3 | 167.4 | 155.3 | 181.1 |

O monitor começou no meio do primeiro quarteto (por isso o N0 a N3 podem ter uma execução a menos com medida), lê a cada 15 s e mede o trabalho **local** do agente (o Claude Code, o Maven, o Java), não o modelo, que roda nos servidores da Anthropic. 100% = um núcleo inteiro.
