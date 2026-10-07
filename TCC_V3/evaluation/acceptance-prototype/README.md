# Teste de aceitação

> **Ainda não congelado.** Começou em 30/09/2026 como protótipo, para as rodadas
> de teste `TESTE-STATE-02` e `TESTE-STRATEGY-01`, e virou a suíte do V4 (Parte 2
> do plano). Para congelar com hash, faltam três coisas:
> 1. ~~a revisão das leituras pelo Lucas~~: **feita em 07/10**, sem divergência;
> 2. a **concordância de implementações independentes** com a calculadora do V4,
>    no teste de bancada do V4;
> 3. o `aceitacao.sh` (item f do plano).

Caixa-preta, pela API HTTP que o enunciado define. Sobe o serviço de um workspace
num container da imagem da bancada, sem token, e roda os casos.

| arquivo | o que faz |
|---|---|
| `executor.sh` | roda **dentro** do container: copia o workspace, reaproveita o `.jar` do build pós-execução (ou compila), sobe o serviço na porta 18080 e roda o teste |
| `strategy.mjs` | 21 casos do enunciado do Strategy do V4: os 5 exemplos e a resposta de exemplo do anexo, como estão escritos, colisões (OURO + FRETEGRATIS), erros, ordem de precedência, o seguro dentro do limite do boleto e quatro **fronteiras** (o valor exato de "até 5 kg", "passarem de R$ 500", "a partir de R$ 300" e "passa de R$ 1.000") |
| `ref-strategy.mjs` | a calculadora de referência do Strategy, em centavos, com meio-para-o-par. Reproduz os 5 exemplos e a resposta de exemplo do anexo |
| `mutantes.mjs` | 17 versões da calculadora de referência com **um** erro plantado cada, um por regra do enunciado que tem armadilha |
| `validar-mutantes.mjs` | serve a referência e cada mutante por HTTP e roda a suíte contra eles: a referência tem de passar, e cada mutante tem de ser reprovado |
| `conferir-enunciado.mjs` | confere os números escritos no enunciado do Strategy (os 5 exemplos e a resposta do anexo) contra a calculadora: pega erro de cópia entre os dois, não erro da calculadora |
| `state.mjs` | 12 casos do enunciado do State: os 8 exemplos (com os textos para o cliente) e 4 erros |
| `analisar-rodada.mjs` | resume os `meta.json` de uma rodada: término, build, versões, tokens, tempo, hashes, isolamento, pares |

As duas suítes contam **casos**, como a hipótese da correção está escrita: um caso
passa se todos os campos dele batem, e os campos que falharam aparecem nas linhas
`FALHA`. Cada caso roda isolado: uma exceção derruba só o caso dela, não os
seguintes.

## Como rodar

Da raiz do `TCC_V3`, para um workspace:

```bash
A=$(cygpath -w "$PWD/evaluation/acceptance-prototype")
W=$(cygpath -w "$PWD/runs/<RUN_ID>/workspace")
MSYS_NO_PATHCONV=1 docker run --rm \
  --mount "type=bind,source=$W,target=/ws,readonly" \
  --mount "type=bind,source=$A,target=/aceitacao,readonly" \
  experimento-harness:v3 bash /aceitacao/executor.sh strategy.mjs   # ou state.mjs
```

O resumo de uma rodada:

```bash
node evaluation/acceptance-prototype/analisar-rodada.mjs TESTE-STRATEGY-01
```

A validação da própria suíte, sem modelo nem Docker (sai com 0 se a referência
passa e todos os mutantes são reprovados):

```bash
node evaluation/acceptance-prototype/validar-mutantes.mjs
```

A conferência dos números do enunciado contra a calculadora (sai com 0 se todos
batem; no enunciado da bancada, `history/prompt-v3/prompt.md`, acusa as
inconsistências conhecidas):

```bash
node evaluation/acceptance-prototype/conferir-enunciado.mjs
```

## Como se sabe que a suíte mede certo

A suíte não tem os valores esperados escritos à mão: ela os pede à calculadora de
referência. Então a pergunta é se a calculadora está certa, e se a suíte reprova
quem difere dela. São quatro verificações, cada uma pegando um tipo de erro.

| verificação | pega | situação |
|---|---|---|
| 1. o enunciado contra a calculadora (`conferir-enunciado.mjs`) | erro de cópia entre os números do texto e a calculadora | **66 de 66** (07/10) |
| 2. os mutantes (`validar-mutantes.mjs`) | uma regra que a suíte não cobra | **17 de 17** reprovados (07/10) |
| 3. implementações independentes | erro de conta ou de arredondamento na calculadora | **a refazer no V4**, ver abaixo |
| 4. a revisão das leituras, pelo Lucas | uma leitura do enunciado que a IA fez e o autor não faria | **feita** (07/10), sem divergência |

### 2. Os mutantes

Passar a referência só mostra que a suíte não reprova o certo. O outro lado é que
ela **reprova o errado**:

| suíte | referência | mutantes reprovados | os que passaram |
|---|---|---|---|
| 15 casos (a de 30/09) | passa | 12 de 16 | mutantes 7 (boleto), 8 (motoboy), 10 (brinde), 11 (MENOS50) |
| com as fronteiras | passa | 16 de 16 | nenhum |
| 18 casos, a de 03/10 (sem o mutante 7) | passa | 15 de 15 | nenhum |
| 21 casos, a de 06/10 (enunciado do V4 com imposto) | passa | 17 de 17 | nenhum |
| **a atual**, 21 casos (enunciado do V4 com seguro, 07/10) | passa | **17 de 17** | nenhum |

Os buracos eram todos **fronteiras**: nenhum caso tinha o valor exato de "até
5 kg", "passarem de R$ 500", "passa de R$ 1.000" ou "a partir de R$ 300". Para o
próximo enunciado: toda frase dessas pede um caso no valor exato.

Onze mutantes são pegos por **um caso só** (o validador lista quais). Não é
defeito, mas é o que avisa se uma mudança nos casos deixar uma regra descoberta:
rodar o `validar-mutantes.mjs` depois de mexer no `strategy.mjs`.

Os mutantes testam a suíte **contra a calculadora**: provam que ela reprova o que
difere do gabarito, não que o gabarito está certo. Isso são as verificações 3 e 4.

### 3. Implementações independentes

Se implementações escritas pelos agentes, cada uma a partir do enunciado, chegam
aos mesmos números da calculadora em todos os casos, um erro de conta ou de
arredondamento teria de ser o mesmo em todas, o que é muito improvável. Na
bancada, as cinco de Opus e Sonnet (`TESTE-STRATEGY-01` e `TESTE-P4-OPUS-CONTROL`)
concordaram em tudo: 85 de 85 verificações em 30/09, 18 de 18 casos em 03/10, 21
de 21 em 06/10.

**No V4 isso precisa ser refeito.** Essas implementações seguem o enunciado da
bancada, com imposto; a calculadora agora calcula o seguro. A concordância volta
com o teste de bancada do V4 (item f4 do plano): a suíte roda sobre o que ele
produzir, e as implementações de Opus e Sonnet devem passar em tudo. O de Haiku
sozinho mede a cota, mas acerta menos, e serve pouco aqui: o teste deve incluir
ao menos um quarteto de Sonnet ou de Opus.

### 4. A revisão das leituras

> **A FAZER, pelo Lucas, sem IA.** Substitui, desde 07/10, a "conferência humana do
> gabarito" (os casos A e B calculados à mão e a tabela de 22 regras), que por
> sua vez substituíra a implementação de referência escrita do zero. Motivo: a
> aritmética já é verificada pelas implementações independentes (verificação 3);
> o que nenhuma IA pode decidir é o que o enunciado quer dizer onde o texto admite
> mais de uma leitura. A troca vai ao orientador junto com as outras.

Nos pontos abaixo a calculadora escolheu uma leitura, e os agentes, da mesma
família de modelo, tendem a escolher a mesma. Para cada um: leia o trecho no
enunciado (`experiment/prompt/prompt.md`) e marque se concorda. Divergência se
resolve pelo **texto**: se ele diz outra coisa, a calculadora é corrigida e os
exemplos do enunciado, recalculados; se o texto não decide, vira inconsistência no
§6 do `OBJETIVO.md`.

| # | trecho do enunciado | o que a calculadora entende | concordo? |
|---|---|---|---|
| 1 | OURO "não paga frete nunca"; FRETEGRATIS: "o desconto do cupom fica igual ao valor do frete" | o OURO zera o frete primeiro, e o cupom vale R$ 0 | sim: aceito, sem erro; o enunciado não dá condição ao FRETEGRATIS, então recusar seria regra inventada |
| 2 | OURO: "se os produtos passarem de R$ 500,00 a gente manda um brinde" | conta os produtos **antes** do cupom | sim |
| 3 | LEVE3PAGUE2: "a cada 3 unidades de um mesmo item do carrinho, uma sai de graça" | por linha do carrinho: a cada 3 unidades daquela linha, uma grátis (7 meias, 2 grátis) | sim: o exemplo 4 já mostra 39,80 |
| 4 | "Sem juros (até 3x), o valor final é o próprio total do pedido e a parcela é o total dividido pelo número de parcelas, arredondado" | o valor final é o total, mesmo que parcela × n dê um centavo diferente | sim: o centavo cai numa das parcelas, como faz a operadora; o resumo mostra o valor nominal |
| 5 | tabela Price: "A parcela é arredondada para centavos e o valor final é a parcela × número de parcelas" | calcula com precisão total e só arredonda a parcela | sim: é o padrão do mercado, com o meio-para-o-par do enunciado |

Saiu da lista o ponto da base do imposto com o FRETEGRATIS: o seguro, que
substituiu o imposto, é "sobre o valor dos produtos, sem desconto e sem frete", e
o texto não deixa dúvida.

**Resultado:** revisado em 07/10/2026 por Lucas, ponto a ponto contra o texto do
enunciado, **sem divergência**: as cinco leituras da calculadora ficam. Falta a
verificação 3 (implementações independentes no teste de bancada do V4).

## Decisões

**A unidade é o caso** (03/10). Contar verificações de campo dava denominadores
diferentes (um caso que devolve erro em vez de 200 vira uma verificação em vez de
onze) e pesava cada erro pelo número de campos que ele contamina (o único erro do
Haiku CONTROL derrubava cinco). A hipótese da correção já está escrita em casos.

**Contradição do enunciado não conta** (03/10). No enunciado da bancada, o limite
do boleto tinha duas leituras e os exemplos 1 a 4 não traziam clube nem região;
esses pontos eram registrados como observação, e o mutante 7 saiu por um tempo.
No V4 as contradições foram corrigidas (06/10), e a suíte não tem mais
observações.

**O P5 é o seguro** (07/10). O imposto por região virou seguro por região, com a
mesma forma (ver o README do enunciado). O boleto passou a usar o total do pedido,
com o seguro dentro; o mutante 7 planta o contrário.

## Histórico: os pacotes da bancada

Estes resultados são do enunciado **da bancada**, com imposto. A suíte atual não
se aplica a eles: o campo `imposto` virou `seguro`. A versão da suíte de cada
data está no git.

Suíte de 06/10 (21 casos, V4 com imposto), no container:

| | Strategy |
|---|---|
| Opus CONTROL / HARNESS | 21 / 21 |
| Sonnet CONTROL / HARNESS | 21 / 21 |
| Haiku CONTROL | 20: FRETEGRATIS sem OURO |
| Haiku HARNESS | 17: FRETEGRATIS sem OURO, precedência região/modalidade, e os dois casos do boleto (que ele lia de outro jeito, válido no enunciado da bancada) |
| `TESTE-P4-OPUS-CONTROL` | 21 |

Suíte de 03/10 (18 casos), com Java 21 local sobre os mesmos `.jar` (o Docker
estava fechado; com a suíte de 30/09, o resultado local foi idêntico ao do
container):

| | Strategy (18 casos) | boleto 950 + imposto (observação) | State (12 casos) |
|---|---|---|---|
| Opus CONTROL / HARNESS | 18 / 18 | aceita / aceita | 12 / 12 |
| Sonnet CONTROL / HARNESS | 18 / 18 | aceita / aceita | 12 / 12 |
| Haiku CONTROL | 17: FRETEGRATIS sem OURO | aceita | 12 |
| Haiku HARNESS | 16: FRETEGRATIS sem OURO, precedência região/modalidade | **recusa** (lê com imposto) | 12 |
| `TESTE-P4-OPUS-CONTROL` | 18 | aceita | — |
