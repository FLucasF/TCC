# Serviço de resumo da compra

Calcula o resumo mostrado ao cliente antes de confirmar o pedido.
Java 21 + Spring Boot 4.1.1, sem banco de dados.

## Como rodar

```bash
mvn verify          # compila e roda os testes
mvn spring-boot:run # sobe o serviço na porta 8080
```

## Endpoint

`POST /checkout/resumo` — corpo e resposta exatamente como no combinado com o
desenvolvedor do site. Erros de negócio saem como `400` com `{"erro": "CODIGO"}`.

## Ordem do cálculo

1. `subtotalProdutos` — preço × quantidade de cada item
2. `descontoCupom`
3. `frete` — conforme a modalidade (zerado quando o nível do clube isenta)
4. `imposto` — alíquota da região sobre produtos − cupom
5. total do pedido = produtos − cupom + frete + imposto
6. `ajustePagamento` — efeito da forma de pagamento sobre o total do pedido

Todo valor em dinheiro é arredondado para centavos em cada etapa, meio para o par
(`Dinheiro.centavos`).

## Onde mexer quando o negócio mudar

Cada regra que o negócio troca com frequência é uma interface com um catálogo que
recolhe todas as implementações registradas no Spring. Para acrescentar uma regra
nova basta criar uma classe `@Component` — nenhum outro arquivo precisa mudar.

| O que mudou | Interface | Pacote |
|---|---|---|
| Transportadora / opção de entrega nova | `ModalidadeEntrega` | `entrega` |
| Promoção nova | `Cupom` | `cupom` |
| Nível novo do clube | `NivelClube` | `clube` |
| Forma de pagamento nova | `FormaPagamento` | `pagamento` |
| Alíquota ou região | `Regiao` (enum) | `imposto` |

## Ponto em aberto

Os exemplos 1 a 4 conferidos pelo financeiro não informam região nem nível do
clube, e os totais deles não incluem imposto. Como o contrato do site trata a
região como obrigatória (`REGIAO_INVALIDA` quando não vem), esses exemplos rodam
nos testes com `SUDESTE` (12%) e `BRONZE`: produtos, cupom, frete e prazo são
exatamente os do documento e os totais incluem o imposto. O exemplo 5, que
informa região e clube, é conferido valor a valor.
