# Desenho: serviço de resumo da compra

## Varia por caso (cada caso tem comportamento próprio)
- Modalidade de entrega: valor do frete, prazo, se atende o pedido (motoboy até 5 kg).
- Cupom: se é aplicável ao pedido e quanto desconta.
- Forma de pagamento: parcelas permitidas, se atende o total, e como cobra (ajuste, parcela).
- Nível do clube: crédito, frete grátis, brinde.

## Igual em todos os casos
- Validação de pedido (itens) e ordem dos erros.
- Cálculo: subtotal, frete -> desconto -> seguro -> total, arredondamento HALF_EVEN por etapa.
- Seguro: só a taxa muda por região (dado, não comportamento).
- Formato de entrada e saída JSON.

## Estrutura
- Cada variação é um `enum` com um constante por caso. O comportamento de cada caso fica no próprio constante; sobrescrevemos só o que difere (padrões na própria enum).
- Escolher o caso = busca pelo código (`porCodigo`), sem cadeia de `if`.
- Assinaturas escolhidas pelo caso mais exigente:
  - Cupom FRETEGRATIS precisa do frete e LEVE3PAGUE2 precisa dos itens: `desconto(Carrinho, BigDecimal frete)`.
  - Forma de pagamento precisa do total e das parcelas: `cobrar(total, parcelas)` devolve `Cobranca(totalFinal, valorParcela)`.
- Código de erro: exceção `PedidoRecusadoException` com o código; um `@RestControllerAdvice` devolve `{ "erro": CODIGO }`.
- Campos de entrada como `String` (e `Integer` para parcelas): a validação de código desconhecido é nossa, para devolver o erro certo em vez de falha de desserialização.
- Sem banco, sem estrutura extra além do necessário.

## Pacote
- `com.loja.resumo` com subpacotes mínimos: `api` (controller e tratador de erro) e `calculo` (domínio, regras e entrada `Pedido`/saída `ResumoCompra`, para o serviço não depender de web).
- Parcelamento do cartão: a divisão "até 3x sem juros / acima de 3x" fica dentro de CARTAO por ser uma única regra de uma forma de pagamento.
