# Resumo da compra

Serviço que calcula o resumo do pedido no fechamento da compra.

- Rodar os testes e empacotar: `mvn verify`
- Subir o serviço: `mvn spring-boot:run` (porta 8080)
- Endereço: `POST /checkout/resumo`, com o pedido em JSON.
  Quando o pedido é recusado, a resposta é `{"erro": "CODIGO"}` com status 422.

## Onde mexer quando o negócio muda

| Mudança | Onde |
|---|---|
| nova opção de entrega | `resumo/entrega`: uma classe nova + registro em `Entregas` |
| nova promoção | `resumo/cupom`: uma classe nova + registro em `Cupons` |
| novo nível do clube | `resumo/clube`: uma classe nova + registro em `NiveisClube` |
| nova forma de pagamento | `resumo/pagamento`: uma classe nova + registro em `FormasPagamento` |
| porcentagem do seguro por região | `resumo/Regiao.java` |

A ordem das contas do resumo, igual para todos os pedidos, fica em
`resumo/CalculadoraResumo.java`.
