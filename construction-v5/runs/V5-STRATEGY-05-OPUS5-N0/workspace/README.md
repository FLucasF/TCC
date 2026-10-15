# Serviço de resumo da compra (checkout)

Calcula o resumo do pedido antes de o cliente confirmar a compra: produtos,
desconto do cupom, frete, prazo, seguro, ajuste do pagamento, valor final,
parcelas, crédito do clube e brinde. Java 21 + Spring Boot 4.1.1, sem banco de dados.

## Como rodar

```bash
mvn verify          # compila e roda todos os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

Exemplo de chamada:

```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H 'Content-Type: application/json' \
  -d '{"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}],
       "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
       "parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}'
```

Quando o pedido não pode ser calculado, a resposta é só o código do problema,
ex.: `{"erro":"MODALIDADE_INDISPONIVEL"}`.

## Onde fica cada regra

| Regra | Pasta |
|---|---|
| Ordem do cálculo e ordem das recusas | `service/CheckoutService.java` |
| Opções de entrega (preço, prazo, limites) | `entrega/` |
| Cupons | `cupom/` |
| Níveis do clube | `clube/` |
| Formas de pagamento (Pix, cartão, boleto) | `pagamento/` |
| Percentual de seguro por região | `domain/Regiao.java` |
| Arredondamento em centavos (meio para o par) | `domain/Dinheiro.java` |

## Como entra uma opção nova

Cada tipo de regra é uma interface; cada opção é uma classe marcada com
`@Component`, encontrada sozinha pelo sistema. Não precisa mexer no cálculo.

- **Transportadora nova**: criar uma classe em `entrega/` implementando
  `ModalidadeEntrega` (código, prazo, cálculo do frete e, se houver, limite em `atende`).
- **Cupom novo**: criar uma classe em `cupom/` implementando `Cupom`
  (código, condição em `aplicavel` e o desconto).
- **Nível de clube novo**: criar uma classe em `clube/` implementando `NivelClube`
  (percentual de crédito, frete grátis, brinde).
- **Forma de pagamento nova**: criar uma classe em `pagamento/` implementando
  `FormaPagamento` (parcelas aceitas, disponibilidade e o ajuste).
- **Região nova ou percentual de seguro diferente**: um valor a mais em `Regiao`.

## Testes

`src/test/java/com/loja/checkout/` cobre os cinco exemplos conferidos pelo
financeiro, o exemplo do anexo, cada código de recusa, a ordem em que as recusas
são conferidas, os casos de limite (motoboy com 5 kg, MENOS50 com R$ 300,00,
boleto com R$ 1.000,00) e o arredondamento em centavos.
