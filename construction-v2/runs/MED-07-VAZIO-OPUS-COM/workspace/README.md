# Servico de resumo da compra

`POST /checkout/resumo` calcula o resumo do pedido: produtos, cupom, frete,
total do pedido e ajuste da forma de pagamento. Sem banco de dados.

    mvn verify        # compila e roda os testes
    mvn spring-boot:run

## Onde mexer quando o negocio muda

Cada opcao mora numa classe so dela; o catalogo acha a escolhida pelo codigo.

- Nova opcao de entrega: uma classe em `entrega` que implementa
  `ModalidadeEntrega` (codigo, se atende o pedido, frete e prazo).
- Nova promocao: uma classe em `cupom` que implementa `Cupom`
  (codigo, condicao e desconto).
- Nova forma de pagamento: uma classe em `pagamento` que implementa
  `FormaPagamento` (codigo, parcelas aceitas, se atende o total e a cobranca).

Basta anotar a classe com `@Component`: ela entra no catalogo sozinha.
