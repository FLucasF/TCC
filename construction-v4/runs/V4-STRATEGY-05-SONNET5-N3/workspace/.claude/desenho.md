# Desenho: serviço de resumo de compra

## O que varia de caso para caso

- **Modalidade de entrega**: fórmula de frete, prazo em dias e condição de
  disponibilidade (ex.: motoboy só até 5kg) são diferentes para cada opção, e
  novas opções são adicionadas com frequência.
- **Cupom**: condição de aplicabilidade e fórmula de desconto são diferentes
  para cada cupom (percentual, valor fixo com piso, frete grátis, N-1 grátis).
- **Nível do clube**: percentual de crédito, se paga frete e se ganha brinde
  mudam por nível, e a loja pretende criar mais níveis no futuro.
- **Forma de pagamento**: parcelas permitidas, disponibilidade (boleto até
  R$1000) e o cálculo do ajuste (desconto Pix, juros Price no cartão, tarifa
  do boleto) são diferentes por forma.

## O que é igual em todos os casos

- **Região**: só a porcentagem do seguro muda; a conta (percentual sobre o
  subtotal dos produtos, arredondado) é sempre a mesma — não precisa de
  comportamento próprio por região, só um valor.
- A ordem do cálculo (produtos → cupom → frete → seguro → total → pagamento)
  e o arredondamento (meio para o par, em cada etapa) são iguais sempre.
- A ordem e os códigos de validação são fixos e iguais para qualquer pedido.

## Estrutura escolhida

- `ModalidadeEntrega`: enum que implementa uma interface `Entrega` com
  `calcularFrete(pesoTotalKg)`, `prazoEntregaDias()` e `disponivel(pesoTotalKg)`.
  Cada constante implementa o próprio corpo — adicionar uma modalidade nova
  é só adicionar uma constante, sem tocar em condicionais existentes. Usa
  interface separada porque é o único caso com mais de uma classe cliente
  em potencial (o enum em si já bastaria, mas deixa o contrato explícito).
- `Cupom`, `NivelClube` e `FormaPagamento`: enums com métodos abstratos
  declarados direto no corpo do enum (sem interface própria — não há
  ganho nenhum em extrair uma, já que só o enum implementa o contrato).
  Cada constante define o próprio corpo: `Cupom.aplicavel(contexto)` e
  `calcularDesconto(contexto)` (`contexto` carrega subtotal, frete e os
  itens, necessário para o LEVE3PAGUE2, que depende da quantidade por
  item); `NivelClube.percentualCredito()`, `freteGratis()` e
  `temBrinde(subtotalProdutos)`; `FormaPagamento.parcelasValidas(parcelas)`,
  `disponivel(totalPedido)` e `calcular(totalPedido, parcelas)` devolvendo
  ajuste, total final e valor da parcela — cada forma com sua fórmula
  própria (Pix, Price do cartão, tarifa do boleto).
- `Regiao`: enum simples com um campo `percentualSeguro`; não precisa de
  método próprio porque a conta do seguro é igual para todas as regiões,
  só o número muda.
- Em nenhum dos enums a seleção do comportamento é um if/else — é sempre
  despacho polimórfico pelo valor do enum (`request.valor().comportamento()`).
- `ResumoCompraService` só orquestra a sequência fixa de passos e a ordem de
  validação; nenhuma regra de cálculo específica de caso mora nele.
- Erros: uma exceção `PedidoRecusadoException` carregando o código, mapeada
  por um `@ExceptionHandler` para `{ "erro": "CODIGO" }`.
