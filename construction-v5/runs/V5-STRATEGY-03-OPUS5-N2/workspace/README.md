# Servico de resumo do checkout

Calcula o resumo da compra (produtos, cupom, frete, seguro, pagamento, clube) na
finalizacao do pedido.

## Como rodar

```
mvn verify          # compila e roda os testes
mvn spring-boot:run # sobe o servico em http://localhost:8080
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

## Onde fica cada coisa

O que varia de pedido para pedido tem um tipo proprio, e o pedido escolhe pelo
codigo que o site envia (`dominio/Catalogo`). Para entrar uma opcao nova, basta
criar a classe e registra-la em `config/CatalogosConfig`; nenhum calculo existente
muda.

| O que varia | Contrato | Implementacoes |
|---|---|---|
| forma de entrega (frete, prazo, o que aceita levar) | `entrega/ModalidadeEntrega` | economica, expressa, retirada na loja, motoboy |
| cupom (quando vale, quanto desconta) | `cupom/Cupom` | BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2 |
| nivel do clube (credito, isencao de frete, brinde) | `clube/NivelClube` | bronze, prata, ouro |
| forma de pagamento (parcelas, ajuste do total) | `pagamento/FormaPagamento` | pix, cartao, boleto |
| regiao | `dominio/Regiao` | so a porcentagem do seguro muda, entao e um enum com a aliquota |

A ordem do calculo e a ordem das conferencias de erro ficam em
`api/ResumoCheckoutService`, num lugar so.

## Duas decisoes que o enunciado nao fixou

- **Status HTTP do pedido recusado**: o corpo e exatamente `{"erro":"CODIGO"}`,
  como pedido; escolhi devolve-lo com `422 Unprocessable Entity` (inclusive para
  JSON ilegivel, como `PEDIDO_INVALIDO`). Se o site esperar outro status, e uma
  linha em `api/TratadorDeErros`.
- **Cupom**: so a ausencia do campo (`cupom` nao enviado ou `null`) conta como
  "sem cupom". Qualquer texto enviado que nao seja um codigo cadastrado, inclusive
  vazio ou em minusculas, e `CUPOM_INVALIDO`.
