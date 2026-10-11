# Desenho — serviço de resumo do checkout

## O que muda de caso para caso (cada caso mora numa classe própria, escolha por mapa de código, sem cadeia de condições)

- **Entrega** (`ModalidadeEntrega`): cada modalidade tem sua fórmula de frete, seu
  prazo e sua limitação de disponibilidade. O enunciado diz que entram opções novas
  toda semana → estrutura aberta, uma classe por modalidade.
- **Cupom** (`Cupom`): cada cupom tem sua condição de uso e sua fórmula de desconto.
  O enunciado diz que o marketing inventa promoção o tempo todo → uma classe por cupom.
- **Nível do clube** (`NivelClube`): cada nível tem seu conjunto de vantagens
  (crédito, isenção de frete, brinde). O enunciado diz que vão criar mais níveis →
  uma classe por nível.
- **Forma de pagamento** (`FormaPagamento`): cada forma ajusta o total de um jeito
  (desconto Pix, tarifa boleto, juros/Price do cartão) e tem suas regras de parcelas
  e disponibilidade → uma classe por forma.

## O que é igual em todos os casos (mora num lugar só)

- **Arredondamento**: sempre centavos, HALF_EVEN → `Dinheiro.centavos`.
- **Seguro**: o enunciado diz "é só a porcentagem que muda, a conta é a mesma".
  Logo NÃO é polimórfico: `Regiao` é um enum que guarda só a porcentagem; a conta
  (porcentagem × subtotal dos produtos) mora na calculadora.
- **Busca por código**: idêntica para as quatro dimensões → `RegistroPorCodigo<T>`
  genérico (um lugar só), alimentado pelos beans Spring de cada dimensão.
- **Sequência de cálculo** e **ordem das validações**: mora na calculadora
  (`CalculadoraResumo`), que é o passo-a-passo fixo do enunciado.

## Assinaturas (pensadas no caso mais exigente)

- `ModalidadeEntrega`: `codigo`, `atende(peso)`, `frete(peso)`, `prazoDias`.
  (atende cobre o motoboy ≤ 5 kg.)
- `Cupom`: `codigo`, `aplicavel(ctx)`, `desconto(ctx)`.
  ctx = subtotal + frete (efetivo) + itens — cobre MENOS50 (subtotal),
  FRETEGRATIS (frete) e LEVE3PAGUE2 (itens).
- `NivelClube`: `codigo`, `credito(subtotal)`, `isentaFrete`, `temBrinde(subtotal)`.
- `FormaPagamento`: `codigo`, `parcelasPermitidas(n)`, `atende(totalPedido)`,
  `calcular(totalPedido, n)` → (totalFinal, valorParcela). ajuste = totalFinal − total.

## Sequência na calculadora

1. valida itens → subtotal e peso.  2. valida clube.  3. valida região.
4. valida modalidade existe.  5. modalidade.atende(peso).  6. cupom existe (se veio).
7. cupom.aplicavel.  8. pagamento existe.  9. parcelas permitidas.  10. pagamento.atende(total).
Primeiro erro encontrado vira `PedidoRecusadoException(codigo)`; o controller devolve `{erro}`.

Cálculo: frete = modalidade.frete(peso), zerado se clube isenta; desconto = cupom (usa frete efetivo);
seguro = região% × subtotal; totalPedido = subtotal − desconto + frete + seguro;
pagamento.calcular → totalFinal/valorParcela; crédito e brinde pelo clube. Tudo em centavos HALF_EVEN.

## Entrega HTTP

Campos de código chegam como String (não enum) para que valor desconhecido vire
nosso código de erro, e não erro de desserialização do Jackson.
