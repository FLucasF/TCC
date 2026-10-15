# Serviço do resumo da compra

Calcula o resumo que o site mostra ao cliente antes de confirmar o pedido:
produtos, desconto do cupom, frete, prazo, seguro, ajuste da forma de pagamento,
valor final, parcelas, crédito do clube e brinde.

Java 21 · Spring Boot 4.1.1 · sem banco de dados (cupons, entregas, níveis do
clube e formas de pagamento ficam no próprio código).

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
          {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
          {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
        ],
        "modalidadeEntrega": "EXPRESSA",
        "cupom": "BEMVINDO10",
        "formaPagamento": "PIX",
        "parcelas": 1,
        "nivelClube": "OURO",
        "regiao": "SUDESTE"
      }'
```

Resposta:

```json
{"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,
 "seguro":4.10,"ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,
 "valorParcela":354.19,"creditoProximaCompra":20.48,"brinde":false}
```

Quando o pedido é recusado, a resposta é `400` com só o código do problema:
`{"erro":"MODALIDADE_INDISPONIVEL"}`.

## Ordem do cálculo

1. `subtotalProdutos` = soma de preço × quantidade de cada item
2. `frete` da opção de entrega (zero quando o nível do clube isenta)
3. `descontoCupom` do cupom, se houver
4. `seguro` = porcentagem da região sobre o subtotal dos produtos
5. total do pedido = produtos − desconto + frete + seguro
6. `ajustePagamento` da forma de pagamento sobre o total do pedido

Todo valor em dinheiro é arredondado em centavos a cada etapa, com
arredondamento "meio para o par" (`Dinheiro.arredondar`).

## Como o código está organizado

| pasta | o que tem |
|---|---|
| `dominio` | carrinho, itens, regiões, resumo, arredondamento e códigos de erro |
| `entrega` | as opções de entrega (`ModalidadeEntrega`) |
| `cupom` | as promoções (`Cupom`) |
| `clube` | os níveis do clube (`NivelClube`) |
| `pagamento` | as formas de pagamento (`FormaPagamento`) |
| `api` | o endereço `/checkout/resumo` e as respostas de erro |
| `CheckoutService` | junta tudo e calcula o resumo |

O `CheckoutService` não conhece nenhuma opção pelo nome: ele recebe do Spring a
lista do que existe e procura pelo código que o site mandou. Por isso cada coisa
nova entra sem mexer no cálculo.

### Entrou uma transportadora nova

Crie uma classe em `entrega` com `@Component`:

```java
@Component
public class EntregaSuperRapida implements ModalidadeEntrega {

    public String codigo() { return "SUPER_RAPIDA"; }

    public int prazoDias() { return 1; }

    public BigDecimal frete(ContextoEntrega contexto) {
        return Dinheiro.arredondar(new BigDecimal("30.00"));
    }

    @Override
    public boolean atende(ContextoEntrega contexto) {   // opcional
        return contexto.regiao() == Regiao.SUDESTE;     // só entrega no Sudeste
    }
}
```

Pronto: o site já pode mandar `"modalidadeEntrega":"SUPER_RAPIDA"`, e um pedido
fora do Sudeste é recusado com `MODALIDADE_INDISPONIVEL`.

### O marketing inventou um cupom novo

Mesma ideia, em `cupom`, implementando `Cupom`: `desconto(...)` devolve o valor
e `aplicavel(...)` (opcional) diz se o pedido cumpre a condição — quando não
cumpre, a resposta é `CUPOM_NAO_APLICAVEL`.

### Um nível novo no clube

Em `clube`, implementando `NivelClube`: o crédito da próxima compra e, se for o
caso, `freteGratis()` e `brinde(...)`.

### Uma forma de pagamento nova

Em `pagamento`, implementando `FormaPagamento`: quais parcelamentos aceita, como
calcula o valor final e a parcela, e se atende o valor do pedido.

## Testes

`src/test/java/com/loja/checkout` tem:

- `ResumoCheckoutApiTest`: os exemplos conferidos pelo financeiro, centavo por centavo
- `PedidoRecusadoApiTest`: os 10 códigos de erro e a ordem de conferência
- `RegrasDoResumoApiTest`: frete grátis, brinde, parcelamento com juros, seguro por região, leve 3 pague 2
- `DinheiroTest`: o arredondamento "meio para o par"
