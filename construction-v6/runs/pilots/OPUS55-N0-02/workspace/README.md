# Serviço de pedidos

Calcula o resumo da compra, registra pedidos, acompanha pagamento e envio e controla o estoque.

## Como rodar

Precisa de Java 21 e de um PostgreSQL. As tabelas e o catálogo inicial são criados ao subir
(o catálogo só é inserido se o produto ainda não existir; o estoque atual nunca é sobrescrito).

```
export DB_URL=jdbc:postgresql://localhost:5432/loja
export DB_USER=loja
export DB_PASSWORD=segredo
export TRANSLOG_URL=https://api.translog.exemplo
mvn package
java -jar target/pedidos-1.0.0.jar
```

`mvn verify` roda os testes com um banco em memória e uma TransLog falsa; não precisa de banco.

## Onde mexer no dia a dia

| Para…                         | Crie uma classe nova (`@Component`) que implemente | Pasta                      |
|-------------------------------|----------------------------------------------------|----------------------------|
| nova opção de entrega         | `ModalidadeEntrega`                                | `checkout/entrega`         |
| trocar de transportadora      | `Transportadora`                                   | `checkout/entrega/transportadora` |
| novo cupom                    | `Cupom`                                            | `checkout/cupom`           |
| novo nível do clube           | `NivelClube`                                       | `checkout/clube`           |
| nova forma de pagamento       | `FormaPagamento`                                   | `checkout/pagamento`       |
| reagir a pagamento/cancelamento (SMS, cashback…) | um `@EventListener` de `PagamentoAprovado` / `PedidoCancelado` | `pedido/eventos` |

Novas situações do pedido (como "em separação") ficam em `pedido/SituacaoPedido`.
