# Desenho — resumo do checkout

## O que muda de caso para caso (enunciado diz que varia)
- **Modalidade de entrega**: cada uma tem seu jeito de cobrar, prazo e limitações (entra uma nova quase toda semana).
- **Cupom**: cada um tem sua condição e sua conta de desconto (marketing inventa sempre).
- **Nível do clube**: cada um com seu conjunto de vantagens (crédito, frete, brinde); mais níveis virão.
- **Forma de pagamento**: cada uma com seus parcelamentos aceitos, restrição e conta do valor final/parcela.

## O que é igual
- Ordem do cálculo (subtotal → cupom → frete → seguro → total → pagamento) e ordem das validações.
- Arredondamento meio-para-o-par em centavos (`Dinheiro`).
- Seguro: a conta é a mesma, só a porcentagem varia por região → `enum Regiao` com a porcentagem (sem estratégia).
- Peso e subtotal do carrinho (`Carrinho`).

## Estrutura
- Uma interface por eixo que varia (`ModalidadeEntrega`, `Cupom`, `NivelClube`, `FormaPagamento`), cada caso numa
  classe `@Component` própria, com seu `codigo()`. Um `Catalogo` genérico monta o mapa código→implementação a partir
  dos beans; a escolha é uma busca no mapa, nunca uma sequência de `if`/`switch`. Nova opção = nova classe.
- Assinaturas pensadas no caso mais exigente:
  - Entrega recebe o `Carrinho` (peso; motoboy usa para disponibilidade).
  - Cupom recebe `Carrinho` + frete efetivo (FRETEGRATIS depende do frete; LEVE3PAGUE2 depende dos itens).
    Por isso o frete (já com a vantagem do clube) é calculado antes do desconto, embora apareça depois no resumo.
  - Clube: `creditoProximaCompra(subtotal)`, `freteCobrado(frete)`, `brinde(subtotal)`.
  - Pagamento: `aceitaParcelas(n)`, `atende(totalPedido)`, `cobrar(totalPedido, parcelas)` → valor final + parcela.
    Tabela Price fica dentro do `Cartao` (só ele usa).
- `CheckoutService` orquestra: valida na ordem do enunciado e lança `PedidoRecusadoException(codigo)`;
  um `@ExceptionHandler` no próprio controller devolve `{ "erro": CODIGO }` com HTTP 422.
