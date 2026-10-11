# Desenho — serviço de resumo do checkout

## O que muda de caso para caso
- **Modalidade de entrega**: fórmula do frete, prazo e restrição de atendimento
  (motoboy só até 5 kg). O enunciado diz que entram opções novas toda semana.
- **Cupom**: condição de aplicabilidade e fórmula do desconto (percentual sobre
  produtos, valor fixo com mínimo, igual ao frete, unidades grátis por item).
- **Forma de pagamento**: ajuste sobre o total, parcelas permitidas e restrição
  (boleto acima de R$ 1.000,00).
- **Nível do clube**: crédito, isenção de frete e brinde; o enunciado diz que
  novos níveis virão, cada um com seu conjunto de vantagens.

## O que é igual em todos os casos
- A ordem do cálculo (produtos → cupom → frete → seguro → total → pagamento).
- Arredondamento de todo valor monetário: 2 casas, HALF_EVEN, em cada etapa.
- O seguro: o enunciado é explícito que **só a porcentagem muda**, a conta é a
  mesma (percentual sobre o subtotal de produtos).
- A ordem de validação dos erros.

## Estrutura escolhida
- Um tipo por eixo que varia, com o comportamento de cada caso num lugar só
  dele e escolha por **registro (lista de casos, busca pelo código)**, nunca
  cadeia de ifs:
  - `ModalidadeEntrega` (Economica, Expressa, RetiradaLoja, Motoboy)
  - `Cupom` (Bemvindo10, Menos50, FreteGratis, Leve3Pague2)
  - `FormaPagamento` (Pix, Cartao, Boleto)
  - `NivelClube` (Bronze, Prata, Ouro)
  Cada registro resolve nome → caso e devolve vazio para código inexistente,
  o que separa "não existe" (erro 4/6/8) de "não atende" (erro 5/7/10).
- `Regiao` é **enum com a alíquota**, não hierarquia: a conta é a mesma.
- `Dinheiro` concentra o arredondamento; `Pedido` concentra subtotal e peso.
- Assinaturas decididas olhando o caso mais exigente:
  - frete: `ModalidadeEntrega.frete(Pedido)` — precisa do peso do pedido.
  - cupom: `Cupom.desconto(Pedido, BigDecimal frete)` — FRETEGRATIS depende do
    frete (já isento pelo clube), LEVE3PAGUE2 depende dos itens.
  - pagamento: `FormaPagamento.cobrar(BigDecimal total, int parcelas)` devolve
    `Cobranca(totalFinal, valorParcela)` — o cartão com juros precisa definir
    os dois, e o ajuste sai de `totalFinal - total`.
  - clube: `credito(subtotal)`, `isentaFrete()`, `brinde(subtotal)`.
- `ResumoService` só orquestra: valida na ordem e aplica as etapas.
- Erros: `PedidoRecusadoException(codigo)` tratada por `@RestControllerAdvice`.

## Decisões de borda
- Cupom ausente ou em branco = sem cupom; qualquer outro código desconhecido é
  `CUPOM_INVALIDO` (o código é sempre maiúsculo e exato).
- `parcelas` ausente vale 1.
- Recusa responde HTTP 400 com `{"erro":"CODIGO"}`; corpo ilegível (JSON fora do
  formato) também é `PEDIDO_INVALIDO`.
- O limite do boleto compara o total do pedido (etapa 5), antes da tarifa.
- Cupom que não tem o que descontar (LEVE3PAGUE2 sem 3 unidades, FRETEGRATIS
  com frete já zerado) é aceito com desconto 0,00: o enunciado só recusa cupom
  por condição não cumprida, e essas não têm condição.
