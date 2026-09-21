# Serviço de resumo do checkout

Calcula o resumo da compra (produtos, cupom, frete, prazo e ajuste do pagamento) antes de o cliente
confirmar o pedido. Não usa banco de dados: tudo é calculado na hora, a partir do que o site envia.

## Como rodar

```bash
mvn verify        # compila e roda os testes
mvn spring-boot:run   # sobe o serviço em http://localhost:8080
```

## A chamada

`POST /checkout/resumo` com o corpo combinado com o desenvolvedor do site. Os erros de negócio
voltam como `400` com `{"erro": "CODIGO"}`, sempre o primeiro erro encontrado na ordem combinada.

## Onde mexer quando o negócio mudar

| Mudança | Onde |
|---|---|
| Nova opção de entrega | `entrega/`: crie uma classe com `@Component` implementando `ModalidadeEntrega` |
| Novo cupom | `cupom/`: crie uma classe com `@Component` implementando `Cupom` |
| Nova forma de pagamento | `pagamento/`: crie uma classe com `@Component` implementando `FormaPagamento` |
| Valores de frete, juros, tarifas, limites | constantes no topo da classe correspondente |

As classes novas são reconhecidas sozinhas — não é preciso registrar em nenhuma lista.
Exemplo de uma transportadora nova (R$ 30,00 + R$ 1,00 por kg, 3 dias, até 30 kg):

```java
@Component
public class EntregaTransportadoraX implements ModalidadeEntrega {
    public String codigo() { return "TRANSPORTADORA_X"; }
    public int prazoDias() { return 3; }
    public BigDecimal calcularFrete(Pedido pedido) {
        return new BigDecimal("30.00").add(pedido.pesoKg());
    }
    public boolean atende(Pedido pedido) {
        return pedido.pesoKg().compareTo(new BigDecimal("30")) <= 0;
    }
}
```

## Regras de dinheiro

Todo valor é arredondado para centavos em cada etapa, com arredondamento "meio para o par"
(`Dinheiro.centavos`). O peso do pedido é somado sem arredondar.
