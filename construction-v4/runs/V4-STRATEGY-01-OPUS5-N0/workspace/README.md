# Serviço de resumo da compra

Calcula o resumo que o site mostra ao cliente antes de confirmar o pedido:
produtos, desconto do cupom, frete, prazo, seguro, ajuste da forma de pagamento,
valor final, parcelas, crédito do clube e brinde.

Java 21 + Spring Boot 4.1.1, sem banco de dados.

## Rodar

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
          {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
          {"nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
        ],
        "modalidadeEntrega": "EXPRESSA",
        "cupom": "BEMVINDO10",
        "formaPagamento": "PIX",
        "parcelas": 1,
        "nivelClube": "OURO",
        "regiao": "SUDESTE"
      }'
```

Quando o pedido é recusado, a resposta é só o código do problema
(`{"erro": "MODALIDADE_INDISPONIVEL"}`), com status HTTP 400.

## Onde fica cada regra de negócio

| Regra | Pasta |
|---|---|
| Ordem do cálculo e ordem das recusas | `servico/CheckoutService.java` |
| Arredondamento do dinheiro (centavos, meio para o par) | `dominio/Moeda.java` |
| Opções de entrega (preço, prazo, limitações) | `dominio/entrega/` |
| Cupons do marketing | `dominio/cupom/` |
| Níveis do clube | `dominio/clube/` |
| Formas de pagamento (Pix, cartão, boleto) | `dominio/pagamento/` |
| Percentual do seguro por região | `dominio/Regiao.java` |

## Como cadastrar coisa nova

Cada família de regra é uma interface com uma classe por opção. Para entrar no
sistema, a classe nova só precisa da anotação `@Component`: ela é encontrada
sozinha e passa a ser aceita pelo serviço, sem mexer no cálculo.

- **Transportadora nova**: nova classe em `dominio/entrega/` implementando
  `ModalidadeEntrega` (código, prazo, conta do frete e, se tiver limitação,
  `atende`). Ex.: `EntregaMotoboy` mostra como recusar pedidos acima de 5 kg.
- **Promoção nova**: nova classe em `dominio/cupom/` implementando `Cupom`
  (código, desconto e, se tiver condição, `aplicavel`). Ex.: `CupomMenos50`.
- **Nível novo do clube**: nova classe em `dominio/clube/` implementando
  `NivelClube` (crédito, frete grátis, brinde). Ex.: `ClubeOuro`.
- **Forma de pagamento nova**: nova classe em `dominio/pagamento/` implementando
  `FormaPagamento` (parcelamento permitido, disponibilidade, valor final).
- **Região nova**: nova constante em `dominio/Regiao.java` com o percentual do
  seguro — a conta é a mesma em todas as regiões.

## Decisões que o documento não fechava

Dois pontos não estavam ditos com todas as letras; vale confirmar com o negócio
se a escolha abaixo é a desejada (cada uma é uma linha só no código):

1. **LEVE3PAGUE2 sem nenhum trio no carrinho** (ex.: 2 camisetas e 1 tênis):
   o pedido é recusado com `CUPOM_NAO_APLICAVEL`, em vez de aceitar o cupom com
   desconto de R$ 0,00. Fica em `CupomLeve3Pague2.aplicavel`.
2. **FRETEGRATIS quando o frete já é zero** (retirada na loja, ou cliente OURO):
   o cupom é aceito e o desconto fica R$ 0,00, porque a regra escrita é
   "o desconto do cupom fica igual ao valor do frete". Fica em `CupomFreteGratis`.
