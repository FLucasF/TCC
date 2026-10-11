# Serviço de resumo da compra

Calcula o resumo que o site mostra antes de o cliente confirmar o pedido:
produtos, desconto do cupom, frete, prazo, seguro, ajuste do pagamento, total
final, parcelas, crédito do clube e brinde.

Java 21 + Spring Boot 4.1.1, sem banco de dados.

## Como rodar

    mvn verify          # compila e roda todos os testes
    mvn spring-boot:run # sobe o serviço em http://localhost:8080

Exemplo de chamada:

    curl -X POST http://localhost:8080/checkout/resumo \
      -H 'Content-Type: application/json' \
      -d '{
            "itens": [
              { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
              { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
            ],
            "modalidadeEntrega": "EXPRESSA",
            "cupom": "BEMVINDO10",
            "formaPagamento": "PIX",
            "parcelas": 1,
            "nivelClube": "OURO",
            "regiao": "SUDESTE"
          }'

Quando o pedido é recusado, a resposta é só o código do problema
(`{"erro":"MODALIDADE_INDISPONIVEL"}`, por exemplo) com status HTTP 400.

## Onde fica cada regra

| Pasta | O que tem lá |
|---|---|
| `entrega/` | as formas de entrega (uma classe por modalidade) |
| `cupom/` | os cupons do marketing (uma classe por cupom) |
| `clube/` | os níveis do clube (uma classe por nível) |
| `seguro/` | as regiões e a taxa do seguro de cada uma |
| `pagamento/` | Pix, cartão (tabela Price) e boleto |
| `servico/` | a ordem do cálculo e a ordem das recusas |
| `dominio/` | itens, pedido e o arredondamento em centavos |

Para acrescentar uma transportadora nova, um cupom novo ou um nível de clube
novo, basta criar uma classe nova na pasta correspondente implementando a
interface daquela pasta (`ModalidadeEntrega`, `Cupom`, `NivelClube`) e marcá-la
com `@Component`: ela entra no catálogo sozinha, sem mexer no cálculo.

## Decisões que valem registrar

- Todo valor em dinheiro é arredondado para centavos a cada etapa, com
  arredondamento "meio para o par" (`2,995 → 3,00`, `2,985 → 2,98`).
- Quando o nível do clube isenta o frete (OURO), o frete sai zerado no resumo,
  e por isso o cupom `FRETEGRATIS` combinado com OURO desconta R$ 0,00.
- `cupom` ausente ou em branco significa "cliente não usou cupom"; qualquer
  outro código que não exista é `CUPOM_INVALIDO` (o código é sensível a
  maiúsculas, como combinado).
- No cartão sem juros (até 3x), o valor final é o próprio total do pedido e a
  parcela é o total dividido pelas parcelas, arredondado — a soma das parcelas
  pode ficar alguns centavos diferente do total.
- `parcelas` ausente vale 1.
- Corpo JSON ilegível (ou campo com tipo errado) é recusado como
  `PEDIDO_INVALIDO`.
