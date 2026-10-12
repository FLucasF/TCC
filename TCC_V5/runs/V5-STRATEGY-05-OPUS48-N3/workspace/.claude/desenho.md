# Desenho — serviço de resumo do checkout

## O que é igual em todo pedido
- A sequência do cálculo: subtotal → desconto do cupom → frete → seguro →
  total do pedido → ajuste do pagamento → crédito/brinde.
- O arredondamento: sempre centavos, meio-para-o-par (`HALF_EVEN`), em cada etapa.
- A ordem das validações (10 checagens, devolve o primeiro erro).
- O formato de entrada/saída (DTOs JSON) e o contrato de erro `{ "erro": "..." }`.

## O que muda de caso para caso (cada caso num lugar só)
Cada dimensão que o enunciado descreve como variando vira um `enum` com o
comportamento dentro de cada constante — escolher o caso é `Enum.valueOf`, não
`if/switch`:

- **ModalidadeEntrega**: fórmula do frete, prazo e disponibilidade.
  (motoboy só até 5 kg). Novas transportadoras = nova constante.
- **Cupom**: condição de aplicação e cálculo do desconto. FRETEGRATIS depende
  do frete, por isso o frete é calculado antes e passado no contexto.
- **NivelClube**: % de crédito, se paga frete, limite do brinde. Novos níveis =
  nova constante.
- **Regiao**: só a % do seguro muda; a conta é a mesma → campo de porcentagem,
  cálculo compartilhado (não é estratégia).
- **FormaPagamento**: parcelas permitidas, cálculo do valor final/parcela e
  disponibilidade (boleto até R$ 1.000).

## Assinaturas pensadas pelo caso mais exigente
- `Cupom`: recebe itens + subtotal + frete (FRETEGRATIS exige frete; MENOS50
  exige subtotal; LEVE3PAGUE2 exige itens).
- `FormaPagamento.calcular(total, parcelas)` devolve (totalFinal, valorParcela);
  o ajuste = totalFinal − total é calculado num lugar só, no serviço.

## Estrutura
- `web`: controller `/checkout/resumo`, DTOs (campos de enum como String para
  controlar a ordem dos erros, não deixar o Jackson falhar antes), handler que
  traduz `CheckoutException(codigo)` em `{ "erro": codigo }` (HTTP 422).
- `domain`: os enums acima + `Dinheiro` (arredondamento) + `ResultadoPagamento`.
- `service`: `CheckoutService` com o pipeline e as validações na ordem exata.

Sem banco. Java 21 + Spring Boot 4.1.1. Testes cobrem os 5 exemplos do
financeiro e os casos de erro. `mvn verify`.
