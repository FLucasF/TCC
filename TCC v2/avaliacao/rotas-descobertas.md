# Rotas que os quatro exemplos do enunciado não cobrem

Os exemplos conferidos são a única referência entrada→saída que o enunciado dá.
São quatro:

| # | entrega | cupom | pagamento |
|---|---|---|---|
| 1 | EXPRESSA | BEMVINDO10 | PIX |
| 2 | ECONOMICA | nenhum | CARTAO 6× (com juros) |
| 3 | MOTOBOY | MENOS50 | BOLETO |
| 4 | RETIRADA_LOJA | LEVE3PAGUE2 | CARTAO 3× (sem juros) |

Cobrem as quatro modalidades de entrega, três dos quatro cupons e as três formas
de pagamento. **Não cobrem nenhum erro, nenhuma fronteira e nenhum empate de
arredondamento.**

Uma app que passe nos quatro exemplos pode estar errada em tudo que segue.

---

## 1. Erros: zero cobertura, e é o pior buraco

O enunciado define **oito códigos de erro com ordem de precedência explícita**.
Nenhum exemplo exercita nenhum deles.

| ordem | situação | código |
|---|---|---|
| 1 | carrinho vazio, ou item com preço/quantidade/peso zero, negativo ou ausente | `PEDIDO_INVALIDO` |
| 2 | modalidade inexistente ou ausente | `MODALIDADE_INVALIDA` |
| 3 | modalidade existe mas não atende (motoboy acima de 5 kg) | `MODALIDADE_INDISPONIVEL` |
| 4 | cupom informado que não existe | `CUPOM_INVALIDO` |
| 5 | cupom existe mas o pedido não cumpre a condição | `CUPOM_NAO_APLICAVEL` |
| 6 | forma de pagamento inexistente ou ausente | `FORMA_PAGAMENTO_INVALIDA` |
| 7 | parcelas não permitidas para a forma | `PARCELAMENTO_INVALIDO` |
| 8 | forma existe mas não atende (boleto acima de R$ 1.000) | `FORMA_PAGAMENTO_INDISPONIVEL` |

**A precedência é o que mais provavelmente diverge.** Uma requisição que viola
duas regras ao mesmo tempo tem uma única resposta certa: a de menor ordem.
Casos a testar:

- modalidade inexistente **e** cupom inexistente → `MODALIDADE_INVALIDA` (2 antes de 4)
- item com preço zero **e** forma de pagamento inexistente → `PEDIDO_INVALIDO` (1 antes de 6)
- motoboy acima de 5 kg **e** boleto acima de R$ 1.000 → `MODALIDADE_INDISPONIVEL` (3 antes de 8)
- cupom inexistente **e** parcelas inválidas → `CUPOM_INVALIDO` (4 antes de 7)
- cupom existe mas não aplicável **e** boleto acima do limite → `CUPOM_NAO_APLICAVEL` (5 antes de 8)

> [!check] Escritos em 20/09/2026
> `avaliacao/casos/precedencia-erros.json`, sete casos, cobrindo 1, 2, 3, 4, 5
> e 6 como vencedores da precedência. Rodar com
> `CASOS=avaliacao/casos/precedencia-erros.json`.
>
> **Um dos cinco acima saiu diferente.** "Cupom não aplicável **e** boleto acima
> do limite" não é construível de forma realista: `CUPOM_NAO_APLICAVEL` só
> acontece com `MENOS50` abaixo de R$ 300,00 em produtos, e o cupom não
> aplicado significa total do pedido = produtos + frete. Para passar de
> R$ 1.000,00 com produtos abaixo de 300, o frete teria que passar de R$ 700 —
> um pedido de umas 150 kg na `EXPRESSA`, ou 345 kg na `ECONOMICA`. Um caso
> assim testaria a tolerância da implementação a peso absurdo, não a
> precedência. No lugar dele ficou **5 antes de 7**: `MENOS50` com produtos
> 299,00 mais `PIX` em 2x, que é realista e exercita a mesma ordem relativa.

## 2. FRETEGRATIS: o único cupom sem exemplo

Já medido nas dez execuções de Haiku: **quatro erram**, em duas formas
diferentes.

- **Grave** — ignora o cupom e cobra o frete. Cliente paga R$ 31,44 a mais.
- **Apresentação** — zera o campo `frete` em vez de lançar o desconto. O total
  final fica certo, mas o resumo contraria o contrato, que manda o frete
  aparecer normalmente e o desconto ficar igual a ele.

Uma suíte que confira só `totalFinal` não pega a segunda. **Conferir campo a
campo.**

## 3. Fronteiras numéricas

| regra | fronteira | por que importa |
|---|---|---|
| motoboy até 5 kg | peso exatamente 5,00 → **permitido**; 5,01 → indisponível | "até" inclui o limite |
| MENOS50 a partir de R$ 300 | produtos exatamente 300,00 → **aplicável**; 299,99 → não | "a partir de" inclui |
| boleto até R$ 1.000 | total exatamente 1.000,00 → **permitido**; 1.000,01 → indisponível | "passa de" exclui o limite |
| cartão sem juros até 3× | 3× sem juros; 4× com juros | primeira parcela com juros |
| cartão de 1 a 12× | 12× válido; 13× inválido; 0 e negativo inválidos | |
| PIX e boleto à vista | 1× válido; 2× inválido | |

## 4. Arredondamento meio-para-o-par

O enunciado dá dois exemplos da regra em si (2,995 → 3,00 e 2,985 → 2,98), mas
**nenhum dos quatro exemplos de API cai num empate**. Todos os valores
arredondam sem ambiguidade.

Onde há empate de verdade: `FRETEGRATIS` + `EXPRESSA` + `PIX` com os itens do
Exemplo 1 dá 5% de 409,70 = 20,485. Meio-para-o-par devolve **20,48**;
`HALF_UP` devolveria 20,49 e um total final de 389,21 em vez de 389,22.

Vale montar casos que caiam em empate em cada etapa: desconto percentual, frete
por peso, ajuste de pagamento e valor da parcela.

## 5. Combinações de cupom não exemplificadas

- `LEVE3PAGUE2` com todos os itens em quantidade menor que 3 → desconto 0,00
- `LEVE3PAGUE2` com um item em quantidade 3 e outro em 2 → só o primeiro conta
- `LEVE3PAGUE2` com quantidade 6 do mesmo item → 2 grátis
- `BEMVINDO10` com `RETIRADA_LOJA` (frete zero) — interação percentual × frete zero
- cupom em minúsculas (`bemvindo10`) → o enunciado diz que o código é sempre
  maiúsculo; a resposta certa é `CUPOM_INVALIDO`

## 6. Campos opcionais

- `cupom` ausente do JSON, e `cupom: null` — as duas formas
- `parcelas` ausente → considerar 1
- `parcelas` presente com PIX valendo 1 → válido

## 7. Prazo de entrega

O campo `prazoEntregaDias` aparece nos quatro exemplos, mas sempre junto de
outras variações. Vale um caso por modalidade isolando o prazo: 7, 2, 1 e 0.

---

## Prioridade — tudo coberto em 20/09/2026

1. ~~**Precedência de erros**~~ → `casos/precedencia-erros.json`, 7 casos
2. ~~**FRETEGRATIS**~~ → `casos/rotas-sem-exemplo.json` e três casos em `cupons.json`
3. ~~**Fronteiras**~~ → `entrega.json` (5,00 e 5,01 kg), `cupons.json` (299,99 e 300,00), `pagamento.json` (1.000,00 e 1.000,01; 0×, 1×, 3×, 4×, 12× e 13×)
4. ~~**Empates de arredondamento**~~ → `arredondamento.json`, um por etapa do cálculo
5. ~~**O resto**~~ → `opcionais-validacao.json` e `cupons.json`

**60 casos**, contra os 4 que o enunciado dá. Todos calculados em 20/09/2026
por `ferramentas/gerar-casos.mjs`, que se conferia contra os quatro exemplos do
enunciado e contra E5 e E6 antes de escrever. O gerador saiu em 22/09; os casos
e a prova de como foram calculados ficam em `casos/README.md`.

> [!warning] Conferir campo a campo, nunca só o total
> Duas das dez execuções acertam o `totalFinal` e erram `frete` e
> `descontoCupom`. Uma suíte que compare só o total aprova as duas.
