---
tags: [tcc, experimento, pre-registro]
status: aguardando o digest da imagem v3
fechado: 2026-09-20
---

# Pré-registro

O que está decidido **antes** de rodar o lote e de pontuar qualquer pacote.

Não repete o `plano.md`: aponta para ele. Duplicar texto foi exatamente o que
produziu a divergência da §11.3, onde o exemplo de `meta.json` descrevia um
arquivo que não existia mais.

> [!warning] Ainda não está fechado
> Falta o **digest da imagem `experimento-harness:v3`**. O Dockerfile mudou em
> 20/09/2026 e a imagem não foi reconstruída. Sem o digest, o ambiente não está
> fixado, e a tag `v1-congelado` não deve ser criada.
>
> ```bash
> docker build -f infra/docker/Dockerfile -t experimento-harness:v3 .
> docker image inspect --format '{{.Id}}' experimento-harness:v3
> ```

---

## 1. A pergunta

Ao construir uma API nova, um harness focado em design de baixo nível aumenta a
taxa de reconhecimento e implementação correta do padrão Strategy em modelos
Claude, comparado ao Claude Code sem configuração? E o efeito muda conforme a
dificuldade de perceber onde o padrão é necessário?

Detalhe em `plano.md` §2.

## 2. Desenho

| | |
|---|---|
| Variável independente | harness: `SEM` × `COM` |
| Fator de bloco | modelo: `claude-opus-5`, `claude-sonnet-5`, `claude-haiku-4-5` |
| Repetições | 3 por combinação → **18 execuções** |
| Ordem | `SEM` e `COM` **simultâneos** no mesmo par (§6.2). A ordem sorteada com `schedule.csv` foi abandonada |
| Natureza | exploratório e descritivo. Valores individuais, médias e variação. **Sem** afirmação de significância |

## 3. Hipóteses

| ID | enunciado |
|---|---|
| H1 | Com harness, a proporção de pontos com Strategy correto é maior que sem harness, nos três modelos |
| H2 | O harness **altera** o consumo de tokens e o tempo — **sem direção declarada** |
| H3 | O ganho do harness é maior no Haiku 4.5 do que no Opus 5 |
| H4 | Nas duas condições, a taxa de acerto cai com a dificuldade (P1 > P2 > P3) |
| H5 | O ganho do harness é maior em P2 e P3 do que em P1 |

A H2 é não-direcional de propósito. A redação anterior dizia "são maiores" e
justificava com skill e hook, que saíram do harness; e as execuções de medição
apontam ao contrário — Opus −42% e Sonnet −45% em tokens de entrada, `SEM`
contra `COM`. Ver `plano.md` §2.4.

## 4. O que está fixo

| artefato | hash |
|---|---|
| `experimento/prompt/prompt.md` | `53db3424b397279573658bfc048a369a33e0a2c8b71530252105e4f841bfd124` |
| `experimento/harness/` (árvore) | `560577922737dbb9252fe3dbd0e26d06e45a7abe6b455c002eae61f8ea24b882` |
| `infra/docker/Dockerfile` | `f9dd2d29f2038775d3a522e716e98d6044bf29eeead33f5812fea41bb578abdf` |
| imagem `experimento-harness:v3` | **pendente** — ver o aviso no topo |

| parâmetro | valor |
|---|---|
| execução | `claude -p`, headless |
| `--effort` | `medium` (D8, revisto em 20/09/2026, ver §3.1) |
| `--disallowedTools` | `Agent,Task`. **Web liberada** nas duas condições |
| permissões | `--dangerously-skip-permissions`, container descartável |
| sessão | `--no-session-persistence` |
| ponto de partida | **pasta vazia**. Java 21 e Spring Boot 4.1.1 **pedidos no enunciado** |
| limite de turnos ou tempo | **nenhum automático** |

## 5. O que se mede

**Primário**

- Strategy correto (sim / parcial / não) em P1, P2 e P3, pela rubrica, às cegas
- Pontuação 0–12 por ponto
- Pontos com Strategy correto por execução (0–3)
- Testes funcionais escondidos: % aprovados, total e por grupo
- Teste de extensão por ponto: arquivos criados, arquivos existentes alterados,
  linhas alteradas

**Secundário**

- `tokens.entrada_total`, `tokens.saida`, `tokens.raciocinio`
- `tempo.duracao_api_ms` — **não** `duracao_s`, contaminada pela execução paralela
- turnos, chamadas de ferramenta, `auditoria.chamadas_web`
- `fundacao.obedeceu_versoes`: taxa de obediência por modelo e condição

> [!warning] Reportar `entrada_total`, nunca `entrada`
> `tokens.entrada` fica entre 38 e 345 nas execuções medidas, porque quase tudo
> entra por cache. Uma tabela que preencha "tokens de entrada" com esse campo
> publica um número sem significado.

## 6. As regras de decisão, fixadas antes

| # | regra |
|---|---|
| **Validade** | `valida` é decisão humana pela tabela de exceções da §13.3. O extrator **propõe** em `valida_proposta`; divergir da proposta exige motivo escrito. Só falha de infraestrutura invalida — **build quebrado conta como resultado** |
| **Refazer** | Só por falha de infraestrutura, nunca por qualidade. Cota, erro de Docker, 5xx da API, interrupção à mão. Refazer é do zero, com registro |
| **P7 · desobediência às versões** | Execução continua **válida**. A desobediência vira **taxa reportada** por modelo e condição. Não é covariável nem critério de exclusão |
| **P6 · o que conta como Strategy** | `plano.md` §14.4a e `avaliacao/rubrica-strategy.md` §4. `enum` com corpo por constante conta (2 em C1/C2/C3) mas **1 em C5**; tabela de dados com caso especial por identidade fica em C1=1 e C3=1; `switch` com a lógica dentro é C1=0 |
| **Web** | Liberada nas duas condições. `auditoria.acesso_web_suspeito` deixa de ser marca de violação e vira registro descritivo |
| **Paralelismo** | `par.sh` e `rodada.sh` rodam simultâneo de propósito. Duração **entre modelos** fica contaminada e é declarada |

## 7. Os instrumentos, e seus hashes

| instrumento | hash (16 primeiros) |
|---|---|
| `avaliacao/rubrica-strategy.md` | `06d9984b17667053` |
| `avaliacao/gabarito-avaliador.md` | `06813eb530435427` |
| `casos/exemplos-enunciado.json` | `ca0044ce27530b3a` |
| `casos/entrega.json` | `27769cea0785594d` |
| `casos/cupons.json` | `2bc84b71f4aeefb9` |
| `casos/pagamento.json` | `f408383f87b79127` |
| `casos/arredondamento.json` | `1a415fe8cefdd1fa` |
| `casos/opcionais-validacao.json` | `60f507569d81475e` |
| `casos/precedencia-erros.json` | `885ecbc1820c7a48` |
| `casos/rotas-sem-exemplo.json` | `a58e4892178b7a52` |
| `testes-extensao/p1-drone.json` | `6d6bc443ab671b2d` |
| `testes-extensao/p2-dezoff.json` | `56dbdb6821a2e0e2` |
| `testes-extensao/p3-carteira-digital.json` | `5472480f98f96df6` |

**60 casos** na suíte escondida e **11** nos testes de extensão. Os valores não
foram digitados: saem de `avaliacao/ferramentas/gerar-casos.mjs`, em BigInt, e o
gerador aborta se não reproduzir os quatro exemplos do enunciado e E5/E6.

> [!important] Os autotestes fazem parte do pré-registro
> ```bash
> node avaliacao/ferramentas/autoteste.mjs      # 5/5
> node infra/scripts/auditoria-web.teste.mjs    # 13/13
> node avaliacao/ferramentas/gerar-casos.mjs    # 6/6 de referencia
> ```
> Sete defeitos apareceram nas ferramentas de medição em 19 e 20/09/2026, três
> deles por acaso. Um instrumento sem teste próprio erra em silêncio.

## 8. Avaliação

Anonimização por `avaliacao/ferramentas/anonimizar.mjs`, com semente registrada
no mapa. Rubrica **antes** do teste de extensão. Autor avalia e **commita**,
congelando antes de ver as notas do professor. Concordância por critério e
**kappa de Cohen** na classificação. Análise usa `consenso.csv`. Fluxo completo
em `avaliacao/README.md`.

## 9. O que NÃO está pré-especificado

Declarado para não virar descoberta disfarçada depois:

- A análise qualitativa do que os modelos fizeram no lugar do Strategy
- O catálogo de formas da rubrica §3 pode ganhar entradas novas se o lote
  produzir uma que as 24 execuções de medição não produziram
- A âncora de **C2 = 1**, que nenhuma execução produziu até agora
- Qualquer corte, agrupamento ou teste estatístico não listado na §5

## 10. O que muda depois disto

Nada, até o lote acabar. Mudança de prompt, harness, imagem, casos ou rubrica
**invalida o lote** e exige recomeçar com hashes novos.

Exceção única: defeito de ferramenta de medição que reprove implementação
correta. Nesse caso, o conserto é registrado, os autotestes ganham o caso de
regressão, e **as execuções afetadas são reavaliadas**, não refeitas — foi o que
aconteceu com o detector de acesso externo em 20/09/2026.

---

## Procedência

| | |
|---|---|
| commit no fechamento | `c48dcb9` |
| execuções de calibração | `FUMACA-01`, `FUMACA-02`, `MED-01` a `MED-07`, **todas fora da análise** |
| âncoras da rubrica | tiradas das execuções de calibração, nunca do lote |
