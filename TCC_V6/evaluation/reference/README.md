# reference: o gabarito do prompt v2

O gabarito da loja do enunciado (`experiment/prompt/prompt.md`): o resumo da compra e o ciclo inteiro do pedido.
Java 21, sem Spring e sem banco, só a regra de negócio. O código, os comentários e as mensagens das
ferramentas estão em inglês (o padrão novo); só o texto que vai para o prompt sai em português.

## As peças

| arquivo | o que é |
|---|---|
| `Purchase`, `Catalog`, `TransLog`, `Result` | os tipos, com os mesmos nomes do JSON da API |
| `Calculator` | **o gabarito do resumo** (`POST /checkout/summary`): as 15 recusas na ordem do prompt e o cálculo, etapa por etapa |
| `Store` | **o gabarito do estado**: o ciclo do pedido, o estoque, o saldo e os avisos. Cada método é uma rota da API |
| `PromptExamples` | gera os 10 exemplos do prompt a partir do gabarito, e confere o prompt (`--check`) |
| `suite/` | **a suíte completa** (`Suite` é a entrada): sobe o serviço do agente e uma TransLog falsa, e faz as mesmas chamadas no serviço e no `Store`, comparando passo a passo (`Lockstep`). Grupos: catálogo, resumo (os exemplos + fronteiras + sorteados, `SummaryCases`), ciclo do pedido (`LifecycleScenarios`), persistência (reinicia o serviço) e concorrência (`ConcurrencyChecks`) |
| `run_suite.py` | acha o jar do agente, sobe um Postgres descartável e roda a `Suite` |
| `plant_mutants.py` | valida a própria suíte: planta 12 erros conhecidos no serviço do OPUS55-N0-03, um de cada vez, e confere que a suíte pega cada um |
| `V5Differential` + `differential.mjs` | compara o gabarito com o do V5 na parte em que as regras são iguais |
| `CalculatorTest` | 54 testes das regras novas e das fronteiras exatas |
| `StoreTest` | 36 testes do ciclo do pedido, contados como pequenas histórias |

## Como o gabarito foi validado

1. **Contra o gabarito do V5**, na parte em que as regras não mudaram: 20.000 compras aleatórias,
   **20.000 iguais**. A matemática em comum (juros, arredondamento, cupons, seguro) herda a validação do V5.
   O diferencial achou um **defeito latente no gabarito do V5**: ele soma os pesos em ponto flutuante, e um
   pedido de exatamente 5 kg pode virar 5,000000000000001 kg, recusando o motoboy. O caso dos 5 kg da suíte do
   V5 (2 × 2,50 kg) dá 5 exatos também em ponto flutuante, então os resultados do V5 não foram afetados.
2. **Os testes**: 90 de 90.
3. **Erros plantados**, para provar que os testes pegam erro:
   - na calculadora, o diferencial pegou o arredondamento meio-para-cima e os juros de 1,98%, mas **não** o brinde
     com R$ 500,00 exatos (compra sorteada quase nunca cai na fronteira), que os testes de borda pegaram;
   - no `Store`, os testes pegaram "repetir o pagamento sempre dá 200", "48 horas exatas recusadas" e "cancelar não
     devolve o estoque"; o "basta um item ter estoque" passava, e ganhou o teste `everyItemNeedsStockNotJustOne`.

## Comandos

```bash
mvn test                                                                          # os 90 testes
mvn -q compile && java -cp target/classes reference.V5Differential 20000 20261014 | node differential.mjs
java -cp target/classes reference.PromptExamples > exemplos.md                    # gera a seção de exemplos
java -cp target/classes reference.PromptExamples --check ../../experiment/prompt/prompt.md  # confere o prompt
python run_suite.py ../../runs/pilots/<RUN>/workspace ../../runs/pilots/<RUN>/suite  # a suíte contra um serviço
python plant_mutants.py ../../runs/pilots/OPUS55-N0-03                       # os erros plantados
```

O `infra/scripts/pilot.py` roda o `--check` antes de cada execução e se recusa a rodar
se os exemplos não conferirem.

Os resultados da suíte contra os pilotos ficam na pasta de cada piloto (`runs/pilots/<RUN>/`); os de
14/10 (`*.pt-api.txt`) ainda são da API em português.
