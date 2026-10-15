# Serviço de resumo da compra

Calcula o resumo mostrado ao cliente antes de confirmar o pedido.
Java 21 + Spring Boot 4.1.1, sem banco de dados.

## Rodar

```bash
mvn verify          # compila e roda todos os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

Exemplo de chamada:

```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H 'Content-Type: application/json' \
  -d '{"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}],
       "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
       "parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}'
```

Pedido recusado devolve só o código do problema, com status 400:
`{"erro":"CUPOM_NAO_APLICAVEL"}`.

## Onde fica cada regra

| Regra | Pasta |
|---|---|
| Ordem do cálculo e ordem das conferências | `dominio/CalculadoraResumo.java` |
| Opções de entrega (frete, prazo, limites) | `dominio/entrega/` |
| Cupons | `dominio/cupom/` |
| Níveis do clube | `dominio/clube/` |
| Formas de pagamento (Pix, cartão, boleto) | `dominio/pagamento/` |
| Seguro por região | `dominio/Regiao.java` |
| Arredondamento em centavos (meio para o par) | `dominio/Dinheiro.java` |

## Como entra uma opção nova

Cada opção é uma classe pequena e independente. Para uma transportadora nova,
crie uma classe em `dominio/entrega/` que implemente `PoliticaEntrega`
(código, frete, prazo e, se tiver limite, o método `atende`) e anote com
`@Component` — ela passa a ser aceita no campo `modalidadeEntrega` sem mexer no
resto do cálculo. Vale o mesmo para cupom (`PoliticaCupom`), nível do clube
(`PoliticaClube`) e forma de pagamento (`PoliticaPagamento`).

Para uma região nova, basta acrescentar o nome e o percentual do seguro em
`Regiao.java`.

## Testes

- `src/test/java/com/loja/checkout/ResumoDaCompraTest.java` — os exemplos
  conferidos pelo financeiro e mais alguns casos (frete grátis, brinde do ouro,
  cartão em 12x).
- `src/test/java/com/loja/checkout/RecusaDoPedidoTest.java` — os dez códigos de
  recusa e a ordem de conferência.
