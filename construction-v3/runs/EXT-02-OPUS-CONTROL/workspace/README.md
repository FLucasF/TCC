# Resumo da compra (checkout)

Servico que calcula o resumo mostrado antes de o cliente confirmar o pedido.
Java 21 + Spring Boot 4.1.1, sem banco de dados.

```bash
mvn verify          # compila e roda os testes
mvn spring-boot:run # sobe o servico em http://localhost:8080
```

## Chamada

`POST /checkout/resumo` com o corpo combinado com o desenvolvedor do site.
Sucesso devolve 200 com o resumo; qualquer regra quebrada devolve 400 com
`{ "erro": "CODIGO" }`, sempre o primeiro erro da ordem combinada.

## Ordem do calculo

produtos -> desconto do cupom -> frete -> imposto da regiao (sobre os produtos
ja descontados) -> total do pedido -> ajuste da forma de pagamento. Todo valor
em dinheiro e arredondado para centavos em cada etapa, meio para o par.

## Onde mexer quando o negocio mudar

Cada regra que muda com frequencia e uma implementacao de interface anotada com
`@Component`; criar a classe nova ja coloca a opcao no ar, sem tocar no calculo:

| Novidade | Interface | Pasta |
|---|---|---|
| Transportadora / forma de entrega | `ModalidadeEntrega` | `domain/entrega` |
| Promocao / cupom | `Cupom` | `domain/cupom` |
| Nivel do clube | `NivelClube` | `domain/clube` |
| Forma de pagamento | `FormaPagamento` | `domain/pagamento` |
| Regiao / aliquota de imposto | enum `Regiao` (a conta e a mesma, so a aliquota muda) | `domain/regiao` |

## Um ponto do documento que precisa de confirmacao

Os exemplos 1 a 4 nao informam a regiao do cliente e os totais deles nao incluem
o imposto (o exemplo 1, por exemplo, fecha em 381,74 = 401,83 - 5% do Pix, sem
imposto nenhum). O exemplo 5 e as regras escritas dizem o contrario: o imposto
entra no total do pedido antes do ajuste do pagamento.

O servico segue a regra escrita e o exemplo 5, que fecha exato (imposto 49,16,
total 435,92, credito 20,48). Os exemplos 1 a 4 estao nos testes com uma regiao
escolhida por nos, e os valores conferem em tudo que nao depende do imposto
(subtotal, cupom, frete, prazo, parcela). Se a intencao era outra, e so avisar.
