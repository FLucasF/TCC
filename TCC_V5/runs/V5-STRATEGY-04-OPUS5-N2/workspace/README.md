# Serviço de resumo da compra

Calcula o resumo que o site mostra antes de o cliente confirmar o pedido.

## Como rodar

```
mvn verify          # compila e roda todos os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

Chamada: `POST /checkout/resumo` com o pedido em JSON. Quando o pedido é
recusado, a resposta é `{"erro": "CODIGO"}` com status 400.

## Onde mora cada coisa

Cada caso que o enunciado descreve como variando tem uma classe só dele, e o
cálculo acha o caso certo pelo código que o site envia (sem sequência de `if`):

| O que varia | Pasta | Para incluir uma opção nova |
|---|---|---|
| Opções de entrega | `entrega/` | nova classe `@Component` que implementa `ModalidadeEntrega` |
| Cupons | `cupom/` | nova classe `@Component` que implementa `Cupom` |
| Níveis do clube | `clube/` | nova classe `@Component` que implementa `NivelClube` |
| Formas de pagamento | `pagamento/` | nova classe `@Component` que implementa `FormaPagamento` |

A opção nova entra sozinha no catálogo, sem mexer no cálculo.

O seguro fica em `seguro/`: a conta é a mesma em todas as regiões, só a
porcentagem muda, então é só a porcentagem que está na região (`Regiao`).

`aplicacao/CalculadoraResumo` é a ordem do cálculo e a ordem das conferências
de recusa (os códigos estão em `aplicacao/ErroCheckout`). `dominio/Dinheiro`
guarda o arredondamento em centavos, meio para o par, usado em toda etapa.
