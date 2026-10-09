# Resumo da compra (checkout)

Servico que calcula o resumo do pedido antes da confirmacao.

## Rodar

    mvn verify          # compila e roda os testes
    mvn spring-boot:run # sobe o servico em http://localhost:8080

Chamada: `POST /checkout/resumo` com o JSON da compra. Quando o pedido e
recusado, a resposta e `400` com `{ "erro": "CODIGO" }`.

## Onde mexer quando o negocio mudar

Cada coisa que varia de pedido para pedido tem seu proprio lugar; a escolha
entre os casos e feita pelo codigo que o site envia, sem sequencia de condicoes.

| Mudanca | Onde |
|---|---|
| Nova transportadora / opcao de entrega | `dominio/entrega`: nova classe `@Component` que implementa `ModalidadeEntrega` |
| Nova promocao | `dominio/cupom`: nova classe `@Component` que implementa `Cupom` |
| Novo nivel do clube | `dominio/clube`: nova classe `@Component` que implementa `NivelClube` |
| Nova forma de pagamento | `dominio/pagamento`: nova classe `@Component` que implementa `FormaPagamento` |
| Aliquota do seguro por regiao | `dominio/Regiao` (so a porcentagem muda) |
| Ordem das conferencias do pedido | `config/ConfiguracaoDoCheckout.regrasDoPedido()` |
| Arredondamento do dinheiro | `dominio/Dinheiro` |

A ordem do calculo (produtos, cupom, frete, seguro, total, pagamento) fica em
`aplicacao/Rascunho` e `aplicacao/CalculadoraDoResumo`.
