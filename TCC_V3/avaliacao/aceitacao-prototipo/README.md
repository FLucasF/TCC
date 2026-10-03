# Teste de aceitação: protótipo

> **PROTÓTIPO, não é a Parte 2 do plano.** Foi escrito em 30/09/2026 para analisar
> as rodadas de teste `TESTE-STATE-02` e `TESTE-STRATEGY-01`. Não está congelado,
> e não vale para as execuções da análise. A suíte de verdade (Parte 2) parte
> daqui, mas precisa ter o gabarito conferido à mão (seção "Conferência humana do
> gabarito", abaixo) e ser congelada com hash antes de ser usada.

Caixa-preta, pela API HTTP que o enunciado define. Sobe o serviço de um workspace
num container da imagem da bancada, sem token, e roda os casos.

| arquivo | o que faz |
|---|---|
| `executor.sh` | roda **dentro** do container: copia o workspace, reaproveita o `.jar` do build pós-execução (ou compila), sobe o serviço na porta 18080 e roda o teste |
| `strategy.mjs` | 18 casos do enunciado do Strategy: o exemplo 5 literal, os exemplos 1 a 4 **com** clube e região, colisões (OURO + FRETEGRATIS), erros, ordem de precedência e três **fronteiras** (o valor exato de "até 5 kg", "passarem de R$ 500" e "a partir de R$ 300"). Mais duas **observações**, que não contam, nos pontos em que o enunciado se contradiz: o exemplo 1 como está escrito, e o limite do boleto |
| `ref-strategy.mjs` | a calculadora de referência do Strategy, em centavos, com meio-para-o-par. Reproduz os 5 exemplos conferidos do enunciado |
| `mutantes.mjs` | 15 versões da calculadora de referência com **um** erro plantado cada, um por regra do enunciado que tem armadilha |
| `validar-mutantes.mjs` | serve a referência e cada mutante por HTTP e roda a suíte contra eles: a referência tem de passar, e cada mutante tem de ser reprovado |
| `state.mjs` | 12 casos do enunciado do State: os 8 exemplos (com os textos para o cliente) e 4 erros |

As duas suítes contam **casos**, como a C1 está escrita: um caso passa se todos os
campos dele batem, e os campos que falharam aparecem nas linhas `FALHA`. Cada caso
roda isolado: uma exceção derruba só o caso dela, não os seguintes.
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

E a validação da própria suíte, sem modelo nem Docker (sai com 0 se a referência
passa e todos os mutantes são reprovados):

```bash
node avaliacao/aceitacao-prototipo/validar-mutantes.mjs
```

## Validação que já existe

- `ref-strategy.mjs` reproduz os 5 exemplos do enunciado, centavo a centavo.
- Os quatro pacotes de Opus e Sonnet de `TESTE-STRATEGY-01`, e o `TESTE-P4-OPUS-CONTROL`,
  passaram em tudo no `strategy.mjs` (85 de 85 verificações com 15 casos, em
  30/09; 18 de 18 casos em 03/10): cinco implementações independentes chegam aos
  mesmos números da referência, inclusive nos casos que o enunciado não
  exemplifica.
- Ainda falta a **conferência humana do gabarito** (última seção): a calculadora
  foi escrita com o Claude, e a concordância das cinco implementações só mostra
  que ninguém discorda dela, não que ela leu o enunciado certo.

### A suíte reprova código errado (03/10/2026)

Passar a referência só mostra que a suíte não reprova o certo. O outro lado é
que ela **reprova o errado**, e isso se mostra com os mutantes:

| suíte | referência | mutantes reprovados | os que passaram |
|---|---|---|---|
| 15 casos (a de 30/09) | passa | 12 de 16 | M7 boleto, M8 motoboy, M10 brinde, M11 MENOS50 |
| com as fronteiras | passa | 16 de 16 | nenhum |
| a atual, 18 casos (sem o M7, ver abaixo) | passa | **15 de 15** | nenhum |

Os buracos eram todos **fronteiras**: nenhum caso tinha o valor exato de "até
5 kg", "passarem de R$ 500", "passa de R$ 1.000" ou "a partir de R$ 300". Para o
próximo enunciado: toda frase dessas pede um caso no valor exato.

Dez mutantes são pegos por **um caso só** (o validador lista quais). Não é
defeito, mas é o que avisa se uma mudança nos casos deixar uma regra descoberta:
rodar o `validar-mutantes.mjs` depois de mexer no `strategy.mjs`.

Os mutantes testam a suíte **contra a calculadora**: provam que ela reprova o que
difere do gabarito, não que o gabarito está certo. Isso é a conferência humana,
na última seção.

### Duas decisões tomadas antes de rodar sobre o lote EXT (03/10/2026)

**O limite do boleto é observação, não caso.** O passo 5 do enunciado define o
total do pedido **com** imposto; a regra do boleto, entre parênteses, **sem**.
Num pedido de R$ 950 + imposto, cada leitura fica de um lado do limite. Contar
isso como erro mediria a contradição do enunciado, não o código; é o mesmo
tratamento dos exemplos 1 a 4, e está no §6 do `OBJETIVO.md` como terceira
inconsistência. Por isso o **M7** (o limite contando o imposto) saiu dos
mutantes: é uma leitura válida, não um erro. O limite continua testado onde as
duas leituras concordam (o caso "erro: boleto acima de 1000" e o exemplo 3).

**A unidade é o caso.** Contar verificações de campo dava denominadores
diferentes (um caso que devolve erro em vez de 200 vira uma verificação em vez
de onze) e pesava cada erro pelo número de campos que ele contamina (o único erro
do Haiku CONTROL derrubava cinco). A C1 já está escrita em casos.

Decidir as duas **antes** de a suíte rodar sobre o EXT é o que as mantém como
pré-registro: depois, a escolha poderia ser guiada por qual braço ela favorece.

### Os pacotes de teste com a suíte atual

| | Strategy (18 casos) | boleto 950 + imposto (observação) | State (12 casos) |
|---|---|---|---|
| Opus CONTROL / HARNESS | 18 / 18 | aceita / aceita | 12 / 12 |
| Sonnet CONTROL / HARNESS | 18 / 18 | aceita / aceita | 12 / 12 |
| Haiku CONTROL | 17: FRETEGRATIS sem OURO | aceita | 12 |
| Haiku HARNESS | 16: FRETEGRATIS sem OURO, precedência região/modalidade | **recusa** (lê com imposto) | 12 |
| `TESTE-P4-OPUS-CONTROL` | 18 | aceita | — |

Rodado com Java 21 local sobre os mesmos `.jar` do build pós-execução, porque o
Docker estava fechado. Com a suíte antiga, o resultado local foi idêntico ao do
container de 30/09, campo a campo.

## Conferência humana do gabarito

> **A FAZER, pelo Lucas, à mão e sem IA.** A calculadora (`ref-strategy.mjs`) foi
> escrita com o Claude; se ela leu mal uma regra, a suíte, os mutantes e ela
> concordam no erro, e nada acusa. Esta conferência é o que substitui, na Parte 2,
> a implementação de referência escrita do zero (troca de 03/10, no plano).

Divergência se resolve pelo **texto do enunciado**, não pela calculadora nem por
quem confere. Se o texto não decide, é inconsistência: vai para o §6 do
`OBJETIVO.md`, como o limite do boleto.

### 1. Regra por regra

Pega uma regra implementada diferente do que o enunciado diz. A lista saiu só do
enunciado; a linha da calculadora é quem confere que acha.

| # | regra (do enunciado) | linha no `ref-strategy.mjs` | diz o mesmo? |
|---|---|---|---|
| 1 | subtotal = Σ preço × quantidade | | |
| 2 | todo valor arredondado para centavos em cada etapa, meio-para-o-par | | |
| 3 | ordem: produtos → cupom → frete → imposto → total → ajuste do pagamento | | |
| 4 | ECONOMICA: R$ 12 + R$ 2/kg, 7 dias | | |
| 5 | EXPRESSA: R$ 25 + R$ 4,50/kg, 2 dias | | |
| 6 | RETIRADA_LOJA: grátis, 1 dia | | |
| 7 | MOTOBOY: R$ 18, 0 dias, **só até 5 kg** | | |
| 8 | peso = Σ peso × quantidade, **sem arredondar** | | |
| 9 | BEMVINDO10: 10% dos produtos | | |
| 10 | MENOS50: R$ 50, **a partir de R$ 300** em produtos | | |
| 11 | FRETEGRATIS: o frete aparece normal, e o desconto é igual ao frete | | |
| 12 | LEVE3PAGUE2: a cada 3 unidades **de um mesmo item**, uma grátis | | |
| 13 | PRATA: crédito de 2%. OURO: crédito de 5%, frete zero, brinde se os produtos **passarem de R$ 500** | | |
| 14 | crédito sobre os produtos **sem desconto e sem frete**; não abate nada nesta compra | | |
| 15 | imposto: % da região sobre **produtos − cupom** (Sudeste 12, Sul 11, Centro-Oeste 9, Norte 7, Nordeste 7) | | |
| 16 | Pix: 5% de desconto **no total do pedido** | | |
| 17 | cartão até 3×: final = total, parcela = total ÷ n arredondada | | |
| 18 | cartão de 4× a 12×: Price a 1,99%, **parcela arredondada**, final = parcela × n | | |
| 19 | boleto: + R$ 3,49; recusado se produtos − cupom + frete **passa de R$ 1.000** | | |
| 20 | Pix e boleto só em 1 parcela; parcelas ausente = 1 | | |
| 21 | ajustePagamento = totalFinal − total do pedido | | |
| 22 | erros na ordem da tabela do anexo (1 a 10), devolvendo o primeiro | | |

### 2. Dois casos de colisão, calculados à mão

Pega regras certas uma a uma mas combinadas na ordem errada, que a lista acima
não vê. **Calcular antes de rodar a calculadora:** quem vê a resposta antes tende
a concordar com ela. Numa planilha, o `ARRED` arredonda meio-para-cima, não
meio-para-o-par; nos valores que terminam em meio centavo, conferir na mão.

- **Caso A**, OURO + FRETEGRATIS com juros (é um caso da suíte): Bota R$ 349,90 ×
  2 (2,10 kg cada), `EXPRESSA`, cupom `FRETEGRATIS`, `CARTAO` em 10×, clube `OURO`,
  região `SUL`.
- **Caso B**, FRETEGRATIS sem OURO, com boleto (fora da suíte, de propósito: os
  números do caso "FRETEGRATIS sem OURO" da suíte já apareceram em saídas de
  teste): Tênis R$ 249,90 × 1 (1,20 kg), `EXPRESSA`, cupom `FRETEGRATIS`, `BOLETO`,
  clube `BRONZE`, região `NORDESTE`.

Depois de calcular, da raiz do `TCC_V3` (a saída vem em **centavos**):

```bash
node --input-type=module -e 'import { calcular } from "./avaliacao/aceitacao-prototipo/ref-strategy.mjs"; console.log("A", calcular({ itens: [{ nome: "Bota", precoUnitario: 349.90, quantidade: 2, pesoKg: 2.10 }], modalidadeEntrega: "EXPRESSA", cupom: "FRETEGRATIS", formaPagamento: "CARTAO", parcelas: 10, nivelClube: "OURO", regiao: "SUL" })); console.log("B", calcular({ itens: [{ nome: "Tenis", precoUnitario: 249.90, quantidade: 1, pesoKg: 1.20 }], modalidadeEntrega: "EXPRESSA", cupom: "FRETEGRATIS", formaPagamento: "BOLETO", nivelClube: "BRONZE", regiao: "NORDESTE" }));'
```

| campo | A: à mão | A: calculadora | B: à mão | B: calculadora |
|---|---|---|---|---|
| subtotalProdutos | | | | |
| descontoCupom | | | | |
| frete | | | | |
| prazoEntregaDias | | | | |
| imposto | | | | |
| total do pedido | | | | |
| ajustePagamento | | | | |
| totalFinal | | | | |
| valorParcela | | | | |
| creditoProximaCompra | | | | |
| brinde | | | | |

**Resultado:** _(preencher: data, quem conferiu, divergências e como se
resolveram)_
