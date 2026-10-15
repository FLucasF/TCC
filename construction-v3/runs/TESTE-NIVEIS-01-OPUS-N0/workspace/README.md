# Servico de resumo da compra

Calcula o resumo que o site mostra antes do cliente confirmar o pedido:
produtos, desconto do cupom, frete, prazo, seguro, ajuste do pagamento,
valor final, parcelas, credito do clube e brinde.

## Como rodar

```bash
mvn verify        # compila e roda todos os testes
mvn spring-boot:run   # sobe o servico em http://localhost:8080
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

Quando o pedido e recusado, a resposta e so o codigo do problema,
por exemplo `{"erro":"MODALIDADE_INDISPONIVEL"}`.

## Onde fica cada regra do negocio

| Regra | Pasta |
|---|---|
| Ordem do calculo do resumo | `resumo/CalculadoraResumo.java` |
| Conferencia do carrinho, clube e regiao | `resumo/ValidadorPedido.java` |
| Opcoes de entrega (valor, prazo, limites) | `entrega/` |
| Cupons do marketing | `cupom/` |
| Niveis do clube | `clube/` |
| Formas de pagamento (Pix, cartao, boleto) | `pagamento/` |
| Percentual do seguro por regiao | `pedido/Regiao.java` |
| Arredondamento para centavos | `comum/Dinheiro.java` |

## Como entra uma opcao nova

As partes que mudam toda semana sao independentes: cada opcao e uma classe
anotada com `@Component`, e o sistema a descobre sozinho ao subir.

- **Transportadora nova**: crie uma classe em `entrega/` implementando
  `ModalidadeEntrega` (codigo, prazo, como calcula o frete e, se tiver limite,
  o metodo `atende`). Se ela cobra fixo + por quilo, basta herdar de
  `EntregaPorPeso`, como a `ECONOMICA`.
- **Cupom novo**: crie uma classe em `cupom/` implementando `Cupom`
  (codigo, desconto e, se tiver condicao, o metodo `aplicavel`).
- **Nivel novo do clube**: crie uma classe em `clube/` implementando
  `NivelClube` e sobrescreva so as vantagens que esse nivel tem.
- **Forma de pagamento nova**: crie uma classe em `pagamento/` implementando
  `FormaPagamento`.

Nenhum desses casos pede mudanca no `CalculadoraResumo`.
