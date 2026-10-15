# Serviço de resumo do checkout

Calcula o resumo da compra (produtos, cupom, frete, imposto, ajuste de pagamento,
crédito do clube e brinde) mostrado ao cliente antes de confirmar o pedido.

Java 21 + Spring Boot 4.1.1, sem banco de dados.

## Rodando

```bash
mvn verify          # compila e roda todos os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

## Endpoint

`POST /checkout/resumo` — corpo e resposta conforme o combinado com o desenvolvedor do site.
Erros de negócio devolvem `400` com `{ "erro": "CODIGO" }`.

## Onde mexer quando o negócio mudar

Cada regra vive em uma classe pequena, registrada sozinha no Spring:

| O que mudou | Onde mexer |
|---|---|
| Transportadora nova | `dominio/entrega` — nova classe `@Component` implementando `ModalidadeEntrega` (ou `EntregaPorPeso`, se cobra fixo + por kg) |
| Promoção nova | `dominio/cupom` — nova classe `@Component` implementando `Cupom` |
| Nível novo do clube | `dominio/clube` — nova classe `@Component` implementando `NivelClube` |
| Forma de pagamento nova | `dominio/pagamento` — nova classe `@Component` implementando `FormaPagamento` |
| Alíquota ou região nova | `dominio/Regiao` |

A ordem do cálculo e a ordem de verificação dos erros ficam em
`aplicacao/ResumoCheckoutService`. Todo arredondamento monetário passa por
`dominio/Moeda` (centavos, meio para o par).

## Observação sobre os exemplos do financeiro

Os exemplos 1 a 4 não informam a região do cliente e os totais deles foram
conferidos sem imposto. Como a região é obrigatória e o imposto sempre incide,
o serviço segue a regra escrita (imposto da região sobre os produtos já com o
desconto). Nos testes, esses exemplos conferem produtos, cupom, frete e prazo,
e os valores de total final/parcela do financeiro estão fixados em
`PagamentoTest`, que testa o ajuste de pagamento sobre os mesmos totais.
