# Serviço de resumo do checkout

Calcula o resumo da compra (produtos, desconto do cupom, frete, seguro, ajuste da
forma de pagamento, total final, crédito do clube e brinde) na finalização do pedido.

- Java 21 · Spring Boot 4.1.1 · sem banco de dados.

## Como rodar

```bash
mvn verify        # compila e roda todos os testes
mvn spring-boot:run   # sobe o serviço em http://localhost:8080
```

## Como chamar

Envie um POST para `/checkout/resumo` com o JSON da compra. Exemplo:

```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H "Content-Type: application/json" \
  -d '{
    "itens": [
      {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
      {"nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
    ],
    "modalidadeEntrega": "EXPRESSA",
    "cupom": "BEMVINDO10",
    "formaPagamento": "PIX",
    "parcelas": 1,
    "nivelClube": "BRONZE",
    "regiao": "NORTE"
  }'
```

Quando dá certo, devolve o resumo em JSON. Quando o pedido é recusado, devolve só o
código do problema, por exemplo `{ "erro": "CUPOM_NAO_APLICAVEL" }`.

## Onde mexer para adicionar coisas novas

Cada assunto fica em um arquivo em `src/main/java/com/loja/checkout/domain/`:

- Nova transportadora/entrega → `ModalidadeEntrega.java`
- Novo cupom → `Cupom.java`
- Novo nível do clube → `NivelClube.java`
- Nova região / percentual de seguro → `Regiao.java`

Basta acrescentar um novo valor no arquivo correspondente. Os testes com os exemplos
conferidos pelo financeiro estão em `src/test/java/.../CheckoutServiceTest.java`.
