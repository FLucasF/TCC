# Serviço de resumo da compra (checkout)

Calcula o resumo mostrado ao cliente antes de confirmar o pedido: produtos, cupom,
frete, prazo, ajuste da forma de pagamento e valor final. Não usa banco de dados —
entregas, cupons e formas de pagamento ficam no próprio código.

## Como rodar

```bash
mvn verify          # compila e roda todos os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

## Chamada

`POST /checkout/resumo`

```json
{
  "itens": [
    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
    { "nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
  ],
  "modalidadeEntrega": "EXPRESSA",
  "cupom": "BEMVINDO10",
  "formaPagamento": "PIX",
  "parcelas": 1
}
```

Resposta (200):

```json
{
  "subtotalProdutos": 409.70,
  "descontoCupom": 40.97,
  "frete": 33.10,
  "prazoEntregaDias": 2,
  "ajustePagamento": -20.09,
  "totalFinal": 381.74,
  "parcelas": 1,
  "valorParcela": 381.74
}
```

Erro (400): `{ "erro": "CUPOM_INVALIDO" }`, verificado na ordem combinada
(`PEDIDO_INVALIDO`, `MODALIDADE_INVALIDA`, `MODALIDADE_INDISPONIVEL`, `CUPOM_INVALIDO`,
`CUPOM_NAO_APLICAVEL`, `FORMA_PAGAMENTO_INVALIDA`, `PARCELAMENTO_INVALIDO`,
`FORMA_PAGAMENTO_INDISPONIVEL`).

## Onde mexer quando o negócio muda

Todo valor em dinheiro é arredondado para centavos em cada etapa, com
arredondamento "meio para o par" (`dominio/Dinheiro.java`).

| O que mudou | Onde mexer |
|---|---|
| Transportadora nova | nova classe `@Component` em `dominio/entrega` implementando `ModalidadeEntrega` |
| Promoção nova | nova classe `@Component` em `dominio/cupom` implementando `Cupom` |
| Forma de pagamento nova | nova classe `@Component` em `dominio/pagamento` implementando `FormaPagamento` |

As classes novas entram no catálogo sozinhas — não é preciso alterar o serviço nem
o controller. Exemplo de opção de entrega nova:

```java
@Component
public class EntregaSedex implements ModalidadeEntrega {
    public String codigo() { return "SEDEX"; }
    public int prazoEntregaDias() { return 3; }
    public BigDecimal calcularFrete(Pedido pedido) {
        return new BigDecimal("20.00").add(new BigDecimal("3.00").multiply(pedido.pesoKg()));
    }
    @Override public boolean atende(Pedido pedido) {
        return pedido.pesoKg().compareTo(new BigDecimal("30")) <= 0; // limite da transportadora
    }
}
```

## Regras de hoje

- **Entrega**: `ECONOMICA` R$ 12,00 + R$ 2,00/kg (7 dias) · `EXPRESSA` R$ 25,00 + R$ 4,50/kg
  (2 dias) · `RETIRADA_LOJA` grátis (1 dia) · `MOTOBOY` R$ 18,00 (mesmo dia, até 5 kg).
- **Cupons** (um por pedido, código em maiúsculas): `BEMVINDO10` 10% dos produtos ·
  `MENOS50` R$ 50,00 a partir de R$ 300,00 em produtos · `FRETEGRATIS` desconto igual ao frete ·
  `LEVE3PAGUE2` a cada 3 unidades do mesmo item, uma sai de graça.
- **Pagamento**: `PIX` à vista com 5% de desconto · `BOLETO` à vista com tarifa de R$ 3,49
  e só até R$ 1.000,00 de total do pedido · `CARTAO` de 1 a 12x, até 3x sem juros e de 4x a 12x
  com 1,99% ao mês pela tabela Price.
