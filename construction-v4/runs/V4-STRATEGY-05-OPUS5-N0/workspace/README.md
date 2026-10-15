# Serviço de resumo da compra

Calcula o resumo do checkout da loja: produtos, desconto do cupom, frete, seguro,
total do pedido e o ajuste da forma de pagamento.

## Como rodar

```bash
mvn verify          # compila e roda todos os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

Exemplo de chamada:

```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H 'Content-Type: application/json' \
  -d '{
        "itens": [
          {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
          {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
        ],
        "modalidadeEntrega": "EXPRESSA",
        "cupom": "BEMVINDO10",
        "formaPagamento": "PIX",
        "parcelas": 1,
        "nivelClube": "OURO",
        "regiao": "SUDESTE"
      }'
```

Quando o pedido é recusado, a resposta é `{"erro":"CODIGO"}` com status HTTP 422.

## Onde fica cada regra

| Regra | Pasta |
|---|---|
| Ordem do cálculo e ordem das conferências | `servico/CheckoutService.java` |
| Arredondamento do dinheiro (meio para o par) | `comum/Dinheiro.java` |
| Opções de entrega | `dominio/entrega/` |
| Cupons | `dominio/cupom/` |
| Níveis do clube | `dominio/clube/` |
| Formas de pagamento | `dominio/pagamento/` |
| Percentual do seguro por região | `dominio/Regiao.java` |

## Como cadastrar coisas novas

O serviço foi montado pensando que entra opção nova toda semana. Em todos os casos
abaixo, **não é preciso mexer no cálculo**: basta criar a classe nova com `@Component`
que ela entra sozinha no catálogo.

- **Transportadora nova**: crie uma classe em `dominio/entrega/` que implemente
  `ModalidadeEntrega` (código, prazo, conta do frete e, se tiver limitação, o `atende`).
  Veja `EntregaMotoboy` como exemplo de modalidade com limite de peso.
- **Promoção nova**: crie uma classe em `dominio/cupom/` que implemente `Cupom`
  (código, condição em `aplicavel` e a conta do desconto).
- **Nível novo do clube**: crie uma classe em `dominio/clube/` que implemente
  `NivelClube` (crédito, frete grátis e brinde — só o que o nível tiver).
- **Forma de pagamento nova**: crie uma classe em `dominio/pagamento/` que implemente
  `FormaPagamento` (parcelas aceitas, limite de valor e a conta do valor final).
- **Região nova**: acrescente o nome e o percentual do seguro em `dominio/Regiao.java`.

## Testes

`src/test/java/com/loja/checkout/` tem os cinco exemplos conferidos pelo financeiro,
as regras de cada opção de entrega, cupom, nível e pagamento, a ordem das recusas e
o formato da resposta da API.
