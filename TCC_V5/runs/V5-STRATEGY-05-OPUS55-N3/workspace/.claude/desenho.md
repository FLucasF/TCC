# Desenho — resumo do checkout

## O que muda de caso para caso (variação descrita no enunciado)
- **Modalidade de entrega**: cada uma tem sua forma de cobrar, prazo e limitação
  (e entram novas toda semana). → interface `ModalidadeEntrega`
  (`codigo`, `atende(peso)`, `frete(peso)`, `prazoDias`), uma classe por modalidade.
- **Cupom**: cada um com sua regra de desconto e sua condição. → interface `Cupom`
  (`codigo`, `aplicavel(ctx)`, `desconto(ctx)`), uma classe por cupom.
  Assinatura dimensionada pelo caso mais exigente: FRETEGRATIS precisa do frete,
  LEVE3PAGUE2 dos itens, MENOS50 do subtotal → `ContextoCupom(itens, subtotal, frete)`.
- **Nível do clube**: cada nível com seu conjunto de vantagens. → interface `NivelClube`
  (`codigo`, `freteCobrado(frete)`, `credito(subtotal)`, `brinde(subtotal)`), uma classe
  por nível; o caso mais exigente (OURO) define a assinatura.
- **Forma de pagamento**: Pix desconta, cartão parcela com/sem juros, boleto tem tarifa e
  limite. → interface `FormaPagamento` (`codigo`, `aceitaParcelas(n)`,
  `disponivel(total)`, `pagar(total, n)` → `Pagamento(totalFinal, valorParcela)`),
  uma classe por forma.

## O que é igual
- Ordem do cálculo (subtotal → frete → cupom → seguro → total → ajuste; o frete vem antes
  do cupom porque FRETEGRATIS precisa dele, sem mudar o resultado), arredondamento
  HALF_EVEN em centavos (`Dinheiro.centavos`), peso do pedido, ordem das validações e
  formato da resposta/erro. Fica no `CalculadoraResumo` e no controller.
- **Região**: só a porcentagem muda, a conta é a mesma → é dado, não comportamento:
  enum `Regiao` com a porcentagem e um único cálculo de seguro.

## Escolha entre os casos
Cada implementação é um `@Component`; um `Catalogo<T>` genérico monta um mapa
`codigo → implementação` a partir da lista injetada pelo Spring. Escolher o caso é
uma busca no mapa (ausente → código de erro), sem cadeia de `if`/`switch`.
Nova modalidade/cupom/nível/forma = nova classe, sem tocar no restante.

## Decisões de interpretação
- LEVE3PAGUE2 conta por linha do carrinho (cada linha é um item) e é sempre aplicável
  (sem linha com 3+ unidades, o desconto é 0,00). FRETEGRATIS também é sempre aplicável.
- Arredondamento: a calculadora arredonda o que vem de entrega, cupom e clube; cada forma
  de pagamento devolve seus valores já em centavos, pois esse arredondamento faz parte da
  regra dela (parcela, desconto do Pix).

## Erros
`CheckoutException(codigo)` lançada na ordem da tabela; um `@ExceptionHandler` no controller
devolve `{ "erro": "CODIGO" }` com HTTP 422. Corpo ilegível (tipo errado, quantidade
fracionária, JSON quebrado) devolve `{ "erro": "PEDIDO_INVALIDO" }` com HTTP 400.
