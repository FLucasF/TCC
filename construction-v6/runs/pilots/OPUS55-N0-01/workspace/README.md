# Serviço de pedidos

Calcula o resumo da compra, registra pedidos, acompanha pagamento e envio e controla o estoque.

## Como rodar

Requer Java 21 e um PostgreSQL. As tabelas e o catálogo inicial são criados ao subir.

```
export DB_URL=jdbc:postgresql://localhost:5432/loja
export DB_USER=loja
export DB_PASSWORD=segredo
export TRANSLOG_URL=https://api.translog.exemplo
mvn spring-boot:run
```

`mvn verify` roda os testes sem precisar de banco (usa um banco em memória e uma TransLog de mentira).

## Onde mexer quando o negócio mudar

| Mudança | Onde |
|---|---|
| Nova opção de entrega | nova classe que implementa `entrega/ModalidadeEntrega` |
| Nova transportadora | nova classe que implementa `entrega/Transportadora` |
| Novo cupom | nova classe que implementa `cupom/Cupom` |
| Novo nível do clube | nova classe que implementa `clube/NivelClube` |
| Nova forma de pagamento | nova classe que implementa `pagamento/FormaPagamento` |
| Algo novo ao confirmar o pagamento (SMS, cashback) | nova classe que implementa `pedido/EfeitoPagamento` |
| Nova situação do pedido ("em separação") | `pedido/SituacaoPedido` |
| Seguro ou prazo por região | `checkout/Regiao` |
