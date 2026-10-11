# Serviço de resumo da compra

Calcula o resumo que o site mostra antes de confirmar o pedido.

## Como rodar

```
mvn verify          # compila e roda todos os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

Exemplo de chamada:

```
curl -X POST http://localhost:8080/checkout/resumo \
  -H 'Content-Type: application/json' \
  -d '{"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
               {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}],
       "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
       "parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}'
```

Quando o pedido é recusado, a resposta é `{"erro":"CODIGO"}` com status 400.

## Onde mexer quando o negócio muda

Cada regra que muda com o tempo é uma classe só dela. Para incluir uma regra
nova, basta criar uma classe nova anotada com `@Component`: ela entra sozinha
no catálogo, sem alterar o cálculo.

| O que mudou | Onde |
|---|---|
| Nova transportadora / opção de entrega | `entrega/` — implementar `ModalidadeEntrega` |
| Nova promoção | `cupom/` — implementar `Cupom` |
| Novo nível do clube | `clube/` — implementar `NivelClube` |
| Nova forma de pagamento | `pagamento/` — implementar `FormaPagamento` |
| Percentual do seguro por região | `dominio/Regiao.java` |
| Ordem do cálculo e ordem das recusas | `calculo/ResumoCompraService.java` |

O arredondamento em centavos ("meio para o par") fica em `dominio/Dinheiro.java`
e é usado por todas as etapas.

## Combinados assumidos

- `cupom` ausente, nulo ou em branco é tratado como "cliente não usou cupom".
  Um código preenchido que não exista é `CUPOM_INVALIDO`.
- Quando o cliente OURO não paga frete, o frete sai `0.00` no resumo e o cupom
  `FRETEGRATIS` passa a descontar `0.00` (o desconto é sempre igual ao frete).
- Corpo JSON quebrado ou com campo de tipo errado é recusado como
  `PEDIDO_INVALIDO`.
