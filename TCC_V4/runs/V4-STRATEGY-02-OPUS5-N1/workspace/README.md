# Resumo da compra no checkout

Servico que calcula o resumo que o site mostra antes do cliente confirmar o pedido.

## Como rodar

    mvn verify          # compila e roda todos os testes
    mvn spring-boot:run # sobe o servico em http://localhost:8080

O site manda os dados da compra em `POST /checkout/resumo` e recebe o resumo de
volta. Quando o pedido e recusado, a resposta e so o codigo do problema
(`{"erro": "CODIGO"}`), com status 422.

## Onde fica cada coisa

O calculo tem uma parte que e igual em toda compra e uma parte que muda conforme
a escolha do cliente. Cada escolha fica num arquivo so dela, e o servico acha a
escolha pelo codigo que o site mandou:

| arquivo | o que guarda |
|---|---|
| `dominio/ModalidadeEntrega` | cada opcao de entrega, com seu frete, seu prazo e suas limitacoes |
| `dominio/Cupom` | cada promocao, com seu desconto e sua condicao |
| `dominio/NivelClube` | cada nivel do clube, com as vantagens que da |
| `dominio/FormaPagamento` | cada forma de pagamento, com seu ajuste e seu parcelamento |
| `dominio/Regiao` | a porcentagem do seguro em cada regiao |

Para entrar uma transportadora nova, um cupom novo ou um nivel novo do clube,
basta acrescentar o caso no arquivo correspondente: nada mais no calculo muda.

A parte igual em toda compra fica em:

| arquivo | o que faz |
|---|---|
| `api/CalculadoraDeResumo` | a ordem do calculo e a ordem em que os problemas sao conferidos |
| `dominio/Seguro` | a conta do seguro, que e a mesma em todas as regioes |
| `dominio/Dinheiro` | o arredondamento em centavos, meio para o par |

Os cinco exemplos conferidos pelo financeiro estao em
`src/test/java/com/loja/checkout/CalculadoraDeResumoTest.java`.
