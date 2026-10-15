# Desenho

## Varia de caso para caso (cada caso mora numa classe própria)
- **Entrega** (novas toda semana): frete, prazo, disponibilidade (motoboy <= 5 kg).
- **Cupom** (marketing inventa): condição de aplicação + cálculo do desconto.
  FRETEGRATIS precisa do frete, LEVE3PAGUE2 dos itens, MENOS50 do subtotal -> o
  cálculo recebe `Compra` (itens, subtotal, peso) e o `frete`.
- **Nível do clube** (virão mais): crédito, frete (OURO zera), brinde.
- **Forma de pagamento**: parcelas válidas, disponibilidade (boleto <= 1000),
  cálculo do valor final e da parcela (Pix desconto, boleto tarifa, cartão Price).

## Não varia
- **Região**: só muda a porcentagem do seguro -> enum com o percentual (dado, não comportamento).
- A sequência do cálculo, a ordem de validação dos erros e o arredondamento (meio para o par).

## Estrutura
- Cada variação é uma interface + uma classe `@Component` por caso. Um `Catalogo<T>`
  genérico indexa os beans por código; escolher o caso é um lookup, sem if/switch.
  Caso novo = classe nova, nada mais muda.
- `ResumoService` executa o fluxo fixo e lança `RecusaException(codigo)`.
- `CheckoutController` (POST /checkout/resumo) + handler que devolve `{"erro": "CODIGO"}` (422).
- DTOs de entrada com campos tolerantes (String/boxed) para mapear entradas
  inválidas nos códigos de erro corretos em vez de falhar no parse.
