# Serviço de resumo da compra

Calcula o resumo do pedido no checkout (produtos, cupom, frete, seguro,
clube e forma de pagamento).

## Como rodar

- Testes e build: `mvn verify`
- Subir o serviço: `mvn spring-boot:run` (fica em `http://localhost:8080`)

O site chama `POST /checkout/resumo` com o pedido em JSON. Quando o pedido é
recusado, a resposta é `{"erro":"CODIGO"}` com status 422.

## Onde fica cada coisa

Cada caso que o enunciado descreve como variando tem um lugar só dele:

| O que varia | Arquivo |
|---|---|
| opções de entrega (custo, prazo, limites) | `dominio/ModalidadeEntrega.java` |
| cupons (desconto e condição) | `dominio/Cupom.java` |
| níveis do clube (crédito, frete, brinde) | `dominio/NivelClube.java` |
| formas de pagamento (ajuste, parcelamento) | `dominio/FormaPagamento.java` |
| aliquota do seguro por região | `dominio/Regiao.java` |

A ordem do cálculo e as recusas ficam em `aplicacao/CalculadoraResumo.java`.
Para entrar uma transportadora nova, basta acrescentar um caso em
`ModalidadeEntrega`; o resto do cálculo não muda.
