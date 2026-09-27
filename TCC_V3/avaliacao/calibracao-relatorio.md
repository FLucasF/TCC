# Calibração da régua

26 e 27/09/2026 · três rodadas · régua na versão 3, **ainda não congelada**

## Resumo

| | rodada 1 (régua v1) | rodada 2 (régua v2) | rodada 3 (régua v3) |
|---|---|---|---|
| pacotes | 27 | 27 | 6, os que ainda travavam |
| células com `indeterminado` | **50** | **7** | **0** |
| das decididas pelos dois leitores, concordam | 100% | 100% | 100% |

A régua passou de 48% de parte comum travada para nenhuma trava, sem que a
concordância caísse. As decisões que levaram a isso estão na §5 da régua (1 a 7
depois da rodada 1; 8 a 11 depois da rodada 2), tomadas por Claude por delegação
do Lucas.

**O limite continua o mesmo:** os leitores são agentes do Claude, com as mesmas
instruções. Concordância entre eles mostra que a régua é clara para quem a segue à
letra; não mostra que uma pessoa leria igual. Isso é o que a leitura humana da
Parte 4 mede.

## Rodadas 2 e 3

**Rodada 2** (27 pacotes, régua v2). As 7 travas que sobraram:

| pacote e ponto | propriedade | causa | resolvida por |
|---|---|---|---|
| JHG3 P2, WGW2 P2 | seleção | cupom achado por consulta, mas a conta feita por um `if` que o nomeia, no lugar da estrutura | decisão 9 |
| JHG3 P3 | seleção | `enum` que é só rótulo, com a conta num `switch` | §2.3, "enum que é só rótulo" |
| FX38 P1 | seleção | `isMotoboy()` no enum, usado só na validação | §2.3, "`if` só de validação" |
| RDKS P3 | parte comum | a relação do ajuste escrita duas vezes no fluxo | decisão 8 (P3 fora da parte comum) |
| Q84C P2, WM6L P3 | custo | fábrica dentro do serviço; desenho sem enum nem registro | decisões 10 e 11 |

Sete dos doze leitores apontaram, cada um por conta própria, que a parte comum do P3
media o formato do contrato da API. Foi o que levou à decisão 8.

**Rodada 3** (os 6 pacotes acima, régua v3): zero `indeterminado`, zero
divergência. Os dois leitores pediram os mesmos esclarecimentos de texto, que não
mudam nenhum valor, e entraram na régua depois: o código de erro esperado do `VALE`
parcelado, o método de um `enum-dados` que mora fora do enum, a lista de códigos
válidos como registro quando não há enum, e a comparação sem efeito como "só nome".

## Desvios de escopo dos leitores

Três agentes, ao buscar com `grep`/`ls` a partir da pasta `pacotes/` inteira, viram
nomes ou trechos de pacotes fora do seu grupo (rodadas 1 e 2). Nenhum abriu planilha
do outro leitor nem mapa. A partir da rodada 2 as instruções passaram a exigir busca
dentro da pasta de cada pacote.

---

# Rodada 1, em detalhe

26/09/2026 · régua v1

> **Os dois leitores são agentes do Claude, não pessoas.** O leitor B foi rotulado
> `B-simulado` e **não é a leitura do Lucas**: ocupa o lugar dela só para testar o
> fluxo e achar brechas. A concordância entre dois agentes do mesmo modelo, com as
> mesmas instruções, tende a ser **inflada**: erros que os dois cometem do mesmo
> jeito não aparecem. A medida que vale continua sendo a leitura humana (Parte 4).

## O que foi feito

| | |
|---|---|
| pacotes | 27, fora da análise: 7 `SMOKE` + 18 `BATCH` (enunciado de 3 pontos) e 2 `TESTE-P4` (enunciado de 5 pontos) |
| anonimização | `anonimizar.mjs --padrao`, sementes 101 e 102, em `avaliacao/calibracao-piloto/` e `avaliacao/calibracao-strategy/`. Os mapas foram tirados das pastas **sem serem abertos** e ficaram fora do repositório |
| leitores | A e B-simulado, cada um dividido em 6 agentes (5 pacotes por agente), sem acesso à leitura do outro |
| linhas lidas | 85 por leitor (25 × 3 pontos + 2 × 5 pontos) |
| planilhas | `avaliacao/calibracao-*/partes/{A,B}-<n>.csv` e `-pacote.csv` |

Um desvio: um agente do leitor B, ao buscar identificadores com `grep`, viu trechos
de pacotes de outros grupos. Não viu nenhuma planilha do leitor A, então a
independência entre A e B não foi afetada.

## Os números

### Por propriedade, sem contar `indeterminado` como concordância

| propriedade | linhas | com `indeterminado` em algum leitor | decididas pelos dois | concordam |
|---|---|---|---|---|
| forma | 85 | 0 | 85 | 85 |
| localização | 85 | 1 | 84 | 84 |
| seleção | 85 | **7** | 78 | 78 |
| assinatura | 83 | 1 | 82 | 82 |
| **parte comum** | 85 | **41 (48%)** | 44 | 44 |
| custo de caso novo | 85 | 0 | 85 | 85 |

**Onde os dois decidiram, concordaram em 100%.** O problema da régua não é a
divergência, é a **indecisão**: quase metade das células de parte comum ficou sem
valor.

O acerto pela ficha do Strategy, nos pontos positivos, teve 99% de concordância
(κ = 0,97). A calibração não separa acerto por braço: isso não é o objetivo dela, e
os mapas continuam fechados.

### O custo de caso novo mede a forma, não o acoplamento

| forma | custo registrado (leitor A) |
|---|---|
| `classes` | 0 em 42 linhas, 1 em 10 |
| `enum-abstrato` | 1 em todas (15) |
| `enum-dados` | 1 em todas (8) |
| `switch` | 2 ou 3 em 9 de 10 |

A concordância é perfeita, mas a medida está errada. Com `enum` o custo é sempre 1
porque se edita a própria constante; com classes registradas pelo Spring, é 0.
Os dois desenhos são igualmente isolados, e a hipótese D4 mediria só qual deles o
modelo escolheu.

### O que quase não foi testado

- **Fábrica (`condicional-unica`)**: 1 pacote só. A decisão §5.1 continua sem base.
- **Controle negativo (`proporcao`)**: 2 pacotes, os dois `dados`. Nenhum exagero
  para calibrar.

---

## Achados, e a correção proposta para cada um

### 1. Parte comum: a propriedade mais mal definida (48% indeterminado)

Três situações, em quase todos os pacotes:

| situação | dúvida | proposta |
|---|---|---|
| o arredondamento está numa função auxiliar, e **cada caso a chama** | chamada repetida é "trecho copiado"? | chamar uma função comum **não** é repetição; `repetida` só quando a lógica em si (fórmula, `setScale` literal, conta) aparece escrita duas vezes |
| arredondamento central **e** também dentro dos casos | os critérios de `unica` e `repetida` se cumprem juntos | vale `unica` quando a versão central já basta; a redundância vai para `observacao` |
| a regra "comum" **não existe** em lugar nenhum: cada caso calcula o próprio ajuste, cada um de um jeito | nem `unica` nem `repetida` | `repetida` passa a cobrir "a regra comum é reimplementada em cada caso", literal ou não |

E uma mudança no gabarito, que elimina a maior parte do ruído: **tirar o
arredondamento da lista de partes comuns.** Ele aparece em todo pacote, em todo
ponto, e não é o que a regra 1 do harness trata (separar o que muda do que é igual
**no negócio**). Ficam só as fórmulas de negócio: fixo + kg × peso (P1), a conta do
ajuste (P3), a porcentagem do crédito (P4). Se o arredondamento interessar, vira uma
propriedade à parte.

Mais dois ajustes pedidos pelos leitores:
- Com mais de uma parte comum no gabarito, basta **uma** repetida para o valor ser
  `repetida`.
- O `CARTAO` calcular o próprio ajuste a partir do total com juros (tabela Price) é
  necessário, não repetição. O gabarito passa a dizer isso.

### 2. Custo de caso novo: medir o acoplamento, não a forma

**Proposta:** não contar o arquivo que **é a própria estrutura do ponto** (o enum, o
registro, a classe do caso). Contar só arquivos **fora** dela que precisam mudar:
serviço, validação, cálculo. Com isso, `enum` e `classes` isolados dão 0, e o custo
passa a medir o que D4 pergunta.

Regras que faltavam, todas pedidas por mais de um leitor:
- conta toda edição necessária para o caso **funcionar certo**, inclusive a que o
  compilador obriga (`switch` exaustivo) e a que só a regra de negócio exige (o
  `VALE` "só à vista");
- dois pontos no mesmo arquivo contam **1**.

### 3. Seleção: um `if` avulso sobre o caso (7 indeterminados)

O caso é achado por consulta (`valueOf`, `Map.get`), mas existe **um** `if (== MOTOBOY)`
ou `if (== BOLETO)` em outro lugar, só para disponibilidade ou código de erro. Os
leitores se dividiram até na proposta.

**Proposta:** `selecao` avalia **só o mecanismo que acha o caso**. Um `if` sobre o
caso em outro lugar é registrado em `localizacao` (`espalhado`) e, se o caso for
exigente, em `assinatura` (`remendo`). Assim cada coisa tem um lugar e o mesmo `if`
não conta três vezes.

E, pedido por 5 leitores: **busca genérica por nome** (laço ou `stream` sobre
`values()` com `equals`, sem nomear nenhum caso) é `consulta`.

### 4. Localização: unidade ou arquivo?

A tabela da §2.2 fala em **unidade** (classe, constante, função), mas o "como
verificar" fala em **arquivo**. Com duas funções no mesmo arquivo, os dois testes
dão resultados diferentes. **Proposta:** vale a unidade, e o texto de verificação
passa a dizer "unidade".

Mais três regras:
- uma função auxiliar **genérica e parametrizada**, que não cita o caso, não espalha;
- uma constante declarada de um caso (`PESO_MAXIMO_MOTOBOY`) é dado, não comportamento;
- código **não alcançável** (método nunca chamado) não entra na leitura.

### 5. Assinatura: a única divergência real entre A e B

No pacote L3RG, P4, o contrato do nível tem um booleano `freteGratis()`, e quem zera
o frete é um ternário no cálculo geral, que lê esse booleano sem citar nenhum nível.
A marcou `comporta`, B deixou `indeterminado`.

**Proposta:** `comporta`. O contrato foi desenhado para o caso exigente, e o chamador
não nomeia nenhum caso. É decisão sua (ver abaixo).

Mais uma, pedida por 4 leitores: quando a forma é `switch` e não há contrato, a
"estrutura" é o `switch` que calcula; `if` sobre o caso fora dele é `remendo`.

### 6. Forma: casos híbridos sem regra

Os dois leitores resolveram do mesmo jeito, mas a régua não diz:

| caso | proposta |
|---|---|
| enum com corpo por constante **e** dados no construtor | `enum-abstrato` |
| `enum-dados` com `if (this == X)` no método único | `enum-dados`, e a seleção fica `condicional-no-calculo` |
| `switch` cujos ramos só chamam um método por caso | `switch` |
| classes escolhidas por um `Map` | `classes` |

### 7. Planilha por pacote

- **`arquivos_main` sai errado quando é contado por quem lê**: o leitor B errou por 1
  em dois pacotes (L9KV e NMR2). É uma contagem mecânica, e passa a ser feita por
  script.
- **Parâmetro que nenhum caso usa** (`pesoTotal`, `prazoDias(Pedido)`, `frete`): os
  leitores divergiram entre marcar `especulativa` ou não, em 3 pacotes. Precisa de
  decisão.
- A planilha por pacote ganha `indeterminado` e a coluna `duvida`, e `especulativa`
  ganha o valor `nenhuma`.
- `pista_condicao` vale para **qualquer arquivo do pacote**, README inclusive. Javadoc
  que descreve extensibilidade ("basta escrever uma classe") **não** é pista, porque
  não cita o harness.

### 8. Gabarito

- **A frase do `BOLETO` está ambígua, e pode ser lida ao contrário.** Seis leitores
  tropeçaram nela. O texto certo: "indisponível quando o total passa de R$ 1.000,00".
- "Um cupom por pedido" não aparece como código, e sai da lista de partes comuns.
- Disponibilidade como dado (um `pesoMaximo` opcional) conta como `comporta` no P1.
- A cópia do gabarito de calibração herdou a frase "mora em `avaliacao/strategy/`".

### 9. Nome dos arquivos

A §4 da régua diz `leitura-<leitor>.csv`. Na calibração, as partes saíram em
`partes/`, e depois são juntadas. A régua passa a descrever os dois.

---

## Decisões que são do Lucas

As quatro que já estavam em aberto, com o que a calibração mostrou:

1. **Fábrica com `switch` conta como acerto?** Só apareceu uma vez (1 pacote). Sem
   base empírica; decidir pelo princípio.
2. **No P5, um `switch` que devolve a porcentagem é acerto?** Não apareceu (os 2
   pacotes usaram `dados`).
3. **A assinatura conta só no P4?** Na calibração ela separou pacotes em P1, P2 e P3
   também (38 `remendo` somando os dois leitores).
4. **O custo conta a lista de códigos válidos?** Substituída pela proposta 2: não se
   conta o que é a própria estrutura do ponto.

E três novas:

5. **Critério da parte comum**: aceitar as propostas do achado 1, inclusive tirar o
   arredondamento do gabarito?
6. **Flag booleana no contrato lida pelo chamador** (achado 5): `comporta`?
7. **Parâmetro que nenhum caso usa**: é `especulativa`?

## Próximo passo

Com as decisões, a régua e os gabaritos são corrigidos, e uma **rodada 2** relê só as
células afetadas (parte comum, seleção, custo). A régua pode ser congelada quando uma
rodada terminar sem `indeterminado` que venha de brecha da régua.
