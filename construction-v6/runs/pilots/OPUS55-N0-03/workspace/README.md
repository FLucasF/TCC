# Serviço de pedidos

Calcula o resumo da compra, registra os pedidos, acompanha pagamento e envio e controla o estoque.

## Como rodar

Precisa de Java 21, Maven e um PostgreSQL (o banco pode começar vazio: as tabelas e o catálogo
inicial são criados quando o serviço sobe).

```bash
export DB_URL=jdbc:postgresql://localhost:5432/loja
export DB_USER=loja
export DB_PASSWORD=segredo
export TRANSLOG_URL=https://api.translog.exemplo
mvn spring-boot:run
```

O serviço atende na porta 8080 (ou na porta da variável `PORT`).

## Testes

```bash
mvn verify
```

Os testes não precisam de banco nem de Docker: usam um banco em memória e uma TransLog de mentira.

## Onde fica cada regra

| Regra | Onde |
|---|---|
| Ordem do cálculo e das conferências do resumo | `checkout/CheckoutCalculator` |
| Formas de entrega (uma classe por forma) | `shipping/` |
| Consulta à TransLog | `shipping/carrier/TranslogCarrier` |
| Cupons (uma classe por cupom) e o teto de 30% | `coupon/` |
| Formas de pagamento (Pix, cartão, boleto) | `payment/` |
| Seguro e dias de despacho por região | `checkout/Region` |
| Níveis do clube | `checkout/ClubTier` |
| Situações do pedido e para onde cada uma pode ir | `order/OrderStatus` |
| Pagamento, envio, entrega, cancelamento | `order/OrderService` |
| O que acontece a cada mudança (estoque, crédito, avisos) | implementações de `order/OrderLifecycleListener` |

Para uma forma de entrega, cupom, forma de pagamento ou reação nova ao pagamento (SMS, cashback),
basta criar uma classe nova no pacote correspondente; o resto do serviço a encontra sozinho.
