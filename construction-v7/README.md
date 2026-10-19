# construction-v7: a construção com patterns, no molde da manutenção

A pesquisa de **construção** refeita para seguir a mesma estrutura da manutenção (`maintenance-v1`), por decisão do
Lucas (18/10/2026): "ambos devem seguir a mesma maneira de estruturar o teste". O agente recebe uma pasta vazia e o
enunciado de **um** cenário, e constrói o sistema do zero. A pergunta: quando o enunciado precisa de um pattern, o
agente aplica (e aplica certo)? E o harness muda isso?

As construções anteriores ficam como registro: a `construction-v6` (a loja, com um enunciado só e vários pontos de
padrão; o batch-1 em Spring e o batch-2 em Javalin) e as versões de antes.

**Nada aqui está congelado.**

## O desenho

| | |
|---|---|
| patterns medidos | os 5 da manutenção: Strategy, Observer, Factory Method, Decorator e Template Method |
| domínio | a Pousada Maré Alta, o mesmo da manutenção |
| pilha | Java 21 + Javalin 6.7.0, sem Spring; dados em memória; porta em `PORT`; um jar executável |
| cenários | 5, os que pedem o pattern na manutenção (01, 03, 05, 07, 09); os controles negativos saíram das duas pesquisas em 18/10/2026 |
| o enunciado | o sistema da pousada descrito por inteiro (o que a base da manutenção faz), com a parte do cenário no fim, em prosa, na voz do dono, sem nomear pattern |
| harness | os braços N0, N1 e N2 da construção (o N1 com os dois princípios; o N2 com a linha que manda consultar a skill `gof-patterns`) |
| execuções | 1 por modelo, cenário e braço: 5 × 3 × 3 = 45 |
| avaliação | o mesmo avaliador da manutenção, adaptado: a classe do cenário, o exagero colateral e o comportamento testado pela API |

## O que fica onde

| pasta | o que tem |
|---|---|
| [`experiment/scenarios/`](experiment/scenarios/) | o texto comum (`frame.md`), os 5 cenários e os trechos do texto comum que o cenário 05 troca (`defaults/` e `NN-*.<TRECHO>.md`) |
| [`experiment/harnesses/`](experiment/harnesses/) | os braços N1 e N2 (os mesmos da `construction-v6`) |
| [`infra/scripts/run_one_scenario.py`](infra/scripts/run_one_scenario.py) | roda uma execução: a pasta vazia com o harness, o enunciado do cenário e o agente na imagem do experimento |
| [`evaluation/evaluator/`](evaluation/evaluator/) | o avaliador (o comum e um por pattern), a calibração e a avaliação das runs |
| [`evaluation/answer-key/GABARITO.md`](evaluation/answer-key/GABARITO.md) | o gabarito |
| `runs/` | as execuções, uma pasta por cenário e uma por braço e modelo |

**Os nomes dos cenários trazem o pattern esperado** (`01-strategy-planos-de-tarifa.md`, por exemplo), para a
leitura do Lucas (19/10/2026). O agente nunca vê esse nome: o script monta o texto e o entrega pela entrada padrão,
e o agente trabalha em `/workspace`. O texto montado é o mesmo, byte a byte, que as 48 execuções receberam (o
`prompt_sha256` confere); só o `scenario_file` dos `meta.json` dessas execuções guarda o nome antigo
(`01-planos-de-tarifa.md`, sem o pattern).

**As soluções de referência** são as da manutenção: a base da pousada com o patch de cada solução aplicado é,
exatamente, um projeto construído do zero com o cenário feito com o pattern (ou sem ele). São 10: duas por cenário.

## Comandos (a partir desta pasta)

```bash
python infra/scripts/run_one_scenario.py 01 N0 claude-sonnet-5-5 medium --dry-run
```

```bash
python evaluation/evaluator/evaluate_with_agent.py calibrate claude-opus-5-5 high --workers 3
```

```bash
python evaluation/evaluator/evaluate_with_agent.py evaluate-runs 01 N0 claude-opus-5-5 high --workers 3
```

O token da assinatura é o mesmo das outras pesquisas (`../construction-v5/.env`), que o Docker lê pelo
`--env-file`; os scripts só conferem se ele existe, sem imprimir o valor.
