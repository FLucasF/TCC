# Resumo da compra (checkout)

Serviço que calcula o resumo mostrado ao cliente antes de confirmar o pedido.
Java 21, Spring Boot 4.1.1, sem banco de dados: as regras de negócio (entregas,
cupons, níveis do clube, formas de pagamento) ficam no próprio código.

## Como rodar

```bash
mvn verify          # compila e roda todos os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

Exemplo de chamada:

```bash
curl -s -X POST http://localhost:8080/checkout/resumo \
  -H 'Content-Type: application/json' \
  -d '{
        "itens": [
          {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
          {"nome": "Tenis",    "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
        ],
        "modalidadeEntrega": "EXPRESSA",
        "cupom": "BEMVINDO10",
        "formaPagamento": "PIX",
        "parcelas": 1,
        "nivelClube": "OURO",
        "regiao": "SUDESTE"
      }'
```

Quando o pedido é recusado, a resposta é `400` com só o código do problema:
`{"erro":"MODALIDADE_INDISPONIVEL"}`.

## Onde fica cada regra

| Regra | Lugar |
|---|---|
| Ordem do cálculo e das conferências | `aplicacao/CalculadoraResumo.java` |
| Arredondamento para centavos (meio para o par) | `dominio/Dinheiro.java` |
| Opções de entrega | `dominio/entrega/` |
| Cupons | `dominio/cupom/` |
| Níveis do clube | `dominio/clube/` |
| Formas de pagamento e tabela Price | `dominio/pagamento/` |
| Percentual do seguro por região | `dominio/seguro/Regiao.java` |

## Como acrescentar uma regra nova

As quatro famílias de regras são interfaces, e cada implementação anotada com
`@Component` entra sozinha no catálogo — não é preciso mexer no cálculo.

Uma transportadora nova, por exemplo:

```java
@Component
public class EntregaDrone implements ModalidadeEntrega {

    @Override public String codigo() { return "DRONE"; }

    @Override public boolean atende(BigDecimal pesoKgPedido) {
        return pesoKgPedido.compareTo(new BigDecimal("2")) <= 0;
    }

    @Override public BigDecimal frete(BigDecimal pesoKgPedido) {
        return Dinheiro.centavos(new BigDecimal("30.00"));
    }

    @Override public int prazoDias() { return 0; }
}
```

A partir daí o site já pode enviar `"modalidadeEntrega": "DRONE"`; se o pedido
passar de 2 kg, a resposta é `MODALIDADE_INDISPONIVEL`. O mesmo vale para um
cupom novo (`Cupom`), um nível do clube novo (`NivelClube`) e uma forma de
pagamento nova (`FormaPagamento`).

Para uma região nova no seguro, basta acrescentar o valor e o percentual no
enum `Regiao` — a conta é a mesma em todas.

## Testes

`src/test/java/com/loja/checkout/` tem os cinco exemplos conferidos pelo
financeiro, o exemplo do anexo, os casos de limite (motoboy em 5 kg, MENOS50 em
R$ 300,00, boleto em R$ 1.000,00, brinde em R$ 500,00), a ordem de conferência
dos dez códigos de erro e o formato do JSON de entrada e saída.
