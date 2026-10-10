# Serviço do resumo da compra

Calcula o resumo que o site mostra antes de o cliente confirmar o pedido:
produtos, desconto do cupom, frete, prazo, seguro, ajuste do pagamento,
valor final, parcelas, crédito do clube e brinde.

## Como rodar

```bash
mvn verify          # compila e roda todos os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

Exemplo de chamada:

```bash
curl -s -X POST http://localhost:8080/checkout/resumo \
  -H 'Content-Type: application/json' \
  -d '{"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
               {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}],
       "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
       "parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}'
```

Quando o pedido é recusado, a resposta é só o código do problema
(`{"erro":"MODALIDADE_INDISPONIVEL"}`, `{"erro":"CUPOM_NAO_APLICAVEL"}` etc.),
com status HTTP 400. Os problemas são conferidos na ordem combinada e volta
sempre o primeiro encontrado.

## Onde mexer quando o negócio muda

Cada regra do negócio mora em um arquivo próprio, e o sistema descobre as
opções sozinho. Para acrescentar uma opção nova, basta criar uma classe nova
anotada com `@Component`, sem mexer no cálculo:

| O que mudou | Onde criar/editar |
|---|---|
| Transportadora nova, prazo ou preço de frete | `entrega/` (interface `ModalidadeEntrega`) |
| Promoção nova ou condição de cupom | `cupom/` (interface `Cupom`) |
| Nível novo do clube ou vantagem nova | `clube/` (interface `NivelClube`) |
| Forma de pagamento, juros, tarifa | `pagamento/` (interface `FormaPagamento`) |
| Percentual do seguro por região | `regiao/CatalogoRegioes` |

A ordem do cálculo e as conferências do pedido ficam em
`calculo/CalculadoraResumo`. O arredondamento em centavos ("meio para o par")
fica em um só lugar: `dominio/Dinheiro`.

## Testes

`src/test/java/com/loja/checkout` tem os cinco exemplos conferidos pelo
financeiro, o exemplo do anexo, as regras de cupom/clube/entrega/pagamento,
cada código de recusa e os casos de limite (motoboy com 5 kg, MENOS50 em
R$ 300,00, boleto em R$ 1.000,00).
