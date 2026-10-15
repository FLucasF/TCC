# Serviço de resumo da compra

Calcula o resumo mostrado ao cliente antes de confirmar o pedido (produtos, cupom,
frete, ajuste da forma de pagamento e valor final). Java 21 + Spring Boot 4.1.1, sem banco de dados.

## Como rodar

```bash
mvn verify          # compila e roda os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

Exemplo de chamada:

```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H 'Content-Type: application/json' \
  -d '{"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
               {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}],
       "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX","parcelas":1}'
```

## Ordem do cálculo

1. `subtotalProdutos` = soma de preço × quantidade
2. `frete` conforme a modalidade de entrega (peso do pedido = soma de peso × quantidade, sem arredondar)
3. `descontoCupom` conforme o cupom (o FRETEGRATIS usa o valor do frete)
4. total do pedido = produtos − cupom + frete
5. `ajustePagamento` = desconto do Pix, tarifa do boleto ou juros do cartão

Todo valor em dinheiro é arredondado para centavos em cada etapa, "meio para o par"
(`com.loja.checkout.dominio.Dinheiro`).

## Onde mexer quando entra algo novo

Cada regra de negócio é uma classe pequena; o serviço descobre as classes sozinho.

- **Nova opção de entrega**: criar uma classe em `entrega/` que implemente `ModalidadeEntrega`
  (código, prazo, cálculo do frete e, se houver limite, o método `atende`) e anotar com `@Component`.
- **Nova promoção**: criar uma classe em `cupom/` que implemente `Cupom` (código, condição
  em `aplicavel` e cálculo do desconto) e anotar com `@Component`.
- **Nova forma de pagamento**: criar uma classe em `pagamento/` que implemente `FormaPagamento`
  e anotar com `@Component`.

Nada mais precisa ser alterado: o catálogo (`ModalidadesEntrega`, `Cupons`, `FormasPagamento`)
é montado a partir das classes existentes, e códigos desconhecidos continuam devolvendo os
erros `MODALIDADE_INVALIDA`, `CUPOM_INVALIDO` e `FORMA_PAGAMENTO_INVALIDA`.

## Erros (HTTP 400, corpo `{"erro":"CODIGO"}`)

Verificados nesta ordem: `PEDIDO_INVALIDO`, `MODALIDADE_INVALIDA`, `MODALIDADE_INDISPONIVEL`,
`CUPOM_INVALIDO`, `CUPOM_NAO_APLICAVEL`, `FORMA_PAGAMENTO_INVALIDA`, `PARCELAMENTO_INVALIDO`,
`FORMA_PAGAMENTO_INDISPONIVEL`.

Os quatro exemplos conferidos pelo financeiro estão cobertos em
`src/test/java/com/loja/checkout/CheckoutApiTest.java`.
