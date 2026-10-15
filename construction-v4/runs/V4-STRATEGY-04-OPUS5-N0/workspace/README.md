# Resumo da compra (checkout)

Servico que calcula o resumo da compra que o site mostra antes do cliente confirmar o pedido.
Java 21 + Spring Boot 4.1.1, sem banco de dados.

## Como rodar

```bash
mvn verify        # compila e roda todos os testes
mvn spring-boot:run   # sobe o servico em http://localhost:8080
```

## Como chamar

`POST /checkout/resumo`, com o corpo em JSON:

```json
{
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
}
```

Resposta:

```json
{
  "subtotalProdutos": 409.70,
  "descontoCupom": 40.97,
  "frete": 0.00,
  "prazoEntregaDias": 2,
  "seguro": 4.10,
  "ajustePagamento": -18.64,
  "totalFinal": 354.19,
  "parcelas": 1,
  "valorParcela": 354.19,
  "creditoProximaCompra": 20.48,
  "brinde": false
}
```

Quando o pedido e recusado, a resposta e `400` com apenas o codigo do problema,
por exemplo `{"erro": "CUPOM_NAO_APLICAVEL"}`.

## Onde fica cada regra

| Regra de negocio | Onde mexer |
|---|---|
| Ordem do calculo e ordem das validacoes | `dominio/CalculadoraResumo.java` |
| Arredondamento do dinheiro (centavos, meio para o par) | `dominio/Dinheiro.java` |
| Formas de entrega (preco, prazo, limites) | pasta `entrega/` |
| Cupons do marketing | pasta `cupom/` |
| Niveis do clube | pasta `clube/` |
| Formas de pagamento, juros e tarifas | pasta `pagamento/` |
| Percentual do seguro por regiao | `dominio/Regiao.java` |

### Entrar uma transportadora, um cupom, um nivel ou uma forma de pagamento nova

Cada um desses e uma classe que implementa a interface da sua pasta
(`ModalidadeEntrega`, `Cupom`, `NivelClube`, `FormaPagamento`) e leva a anotacao
`@Component`. Nada mais precisa ser alterado: a classe nova entra sozinha no catalogo.
Exemplo de uma entrega nova de R$ 30,00 em 3 dias, so para pedidos de ate 10 kg:

```java
@Component
public class EntregaAgendada implements ModalidadeEntrega {

    public String codigo() { return "AGENDADA"; }

    public int prazoEntregaDias() { return 3; }

    public BigDecimal frete(Pedido pedido) { return new BigDecimal("30.00"); }

    public boolean atende(Pedido pedido) {
        return pedido.pesoTotalKg().compareTo(new BigDecimal("10")) <= 0;
    }
}
```

## Testes

`src/test/java/com/loja/checkout/` tem os cinco exemplos conferidos pelo financeiro,
os casos de cada cupom, nivel, entrega e pagamento, a ordem dos erros e dois testes
que chamam o endpoint de verdade conferindo o JSON da resposta.
