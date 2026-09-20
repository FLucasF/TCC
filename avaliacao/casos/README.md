# Casos da suíte escondida

> [!danger] Nunca entra no container
> `.dockerignore` é lista branca e já barra `avaliacao/` do contexto de build.
> O modelo nunca vê nenhum destes valores.

Formato do caso:

```json
{ "id": "...", "descricao": "...", "requisicao": { ... },
  "status_esperado": 400, "esperado": { "campo": valor } }
```

`status_esperado` é opcional e vale **200**. Caso de erro declara 400 e põe o
código em `esperado.erro`.

Rodar um conjunto contra a aplicação de uma execução:

```bash
CASOS=avaliacao/casos/pagamento.json avaliacao/ferramentas/conferir-exemplos.sh <run_id>
```

---

## Os arquivos, e o grupo da §14.3 a que pertencem

| arquivo | casos | grupo | o que cobre |
|---|---|---|---|
| `exemplos-enunciado.json` | 4 | — | os quatro exemplos que o modelo **viu**. Mínimo, não prova nada sozinho |
| `entrega.json` | 9 | **Entrega** | custo e prazo de cada modalidade · motoboy em 5,00 e 5,01 kg · peso somado sem arredondar · modalidade inexistente |
| `cupons.json` | 14 | **Cupons** | cada cupom · sem cupom · MENOS50 em 299,99 e 300,00 · LEVE3PAGUE2 com 2, 3, 6, 7 e misto · FRETEGRATIS em três modalidades · código minúsculo · cupom inexistente |
| `rotas-sem-exemplo.json` | 1 | **Cupons** | a rota do FRETEGRATIS que nenhum exemplo cobria, e que 4 de 10 execuções erraram |
| `pagamento.json` | 11 | **Pagamento** | cartão em 1×, 3×, 4× e 12× · 0× e 13× · boleto em 1.000,00 e 1.000,01 · PIX e boleto parcelados · forma inexistente |
| `arredondamento.json` | 4 | **Pagamento** | empate meio-para-o-par em cada etapa: desconto percentual, frete por peso, ajuste do PIX e valor da parcela |
| `opcionais-validacao.json` | 10 | **Validação** | cupom ausente e `null` · parcelas ausente · e os códigos de erro isolados |
| `precedencia-erros.json` | 7 | **Validação** | a ordem entre os oito códigos, quando a requisição viola duas regras ao mesmo tempo |

**60 casos** ao todo, contra 4 que o enunciado dá.

---

## Os empates de arredondamento

O enunciado manda arredondar meio-para-o-par em cada etapa e dá dois exemplos da
regra em si — 2,995 → 3,00 e 2,985 → 2,98 —, mas **nenhum dos quatro exemplos de
API cai num empate**. Uma implementação que use `HALF_UP` passa nos quatro e erra
em produção, por um centavo de cada vez.

`arredondamento.json` força o empate em cada etapa do cálculo:

| caso | onde o empate acontece | meio-para-o-par | `HALF_UP` daria |
|---|---|---|---|
| `arr-percentual` | desconto de 10% sobre 12,25 = **1,225** | 1,22 | 1,23 |
| `arr-frete-peso` | frete ECONOMICA com 0,1125 kg = **12,225** | 12,22 | 12,23 |
| `arr-pix-20485` | 5% do PIX sobre 409,70 = **20,485** | 20,48 | 20,49 |
| `arr-parcela` | parcela de 100,05 em 2× = **50,025** | 50,02 | 50,03 |

> [!note] `arr-parcela` também exercita a assimetria documentada
> Sem juros, o enunciado diz que "o valor final é o próprio total do pedido".
> Então `totalFinal` é 100,05 e `valorParcela` é 50,02, e 2 × 50,02 = 100,04.
> A diferença de um centavo está no contrato e é intencional. Uma implementação
> que calcule o total como parcela × parcelas devolve 100,04 e reprova aqui.

---

## Como os valores foram calculados

Por `ferramentas/gerar-casos.mjs`, com **BigInt em micros**. Ponto flutuante não
serviria: 0,1125 não é representável em binário, e metade destes casos existe
justamente para cair em empate.

O gerador **se confere antes de escrever**: reproduz os quatro exemplos do
enunciado e os casos E5 e E6 do gabarito, e aborta sem escrever nada se algum
divergir. Conferido em 20/09/2026 adulterando a taxa do cartão de 1,99% para
2,99%: ele acusou os dois casos afetados e não escreveu.

```bash
node avaliacao/ferramentas/gerar-casos.mjs
```

> [!warning] Não edite os valores à mão
> Mudou uma regra do enunciado, muda no gerador e regera. Valor esperado editado
> à mão é valor sem conferência, e um erro aqui reprova implementação correta em
> silêncio — que é o pior defeito possível num instrumento de medida.
