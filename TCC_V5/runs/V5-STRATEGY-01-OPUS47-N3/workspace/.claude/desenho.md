# Desenho — resumo do checkout

## O que muda de caso para caso

- **Modalidade de entrega**: cada opção tem seu custo (constante, por peso, mista),
  seu prazo e sua regra de disponibilidade. Virão outras.
- **Cupom**: cada código tem sua fórmula de desconto e sua condição de aplicabilidade.
- **Nível do clube**: cada nível tem cashback, regra de frete e brinde próprios. Virão outros.
- **Região**: só muda a porcentagem do seguro.
- **Forma de pagamento**: cada forma tem seu ajuste no total, suas parcelas permitidas
  e sua regra de disponibilidade.

## O que é igual em todos os casos

- Ordem do cálculo (produtos → cupom → frete → seguro → total → ajuste de pagamento).
- Arredondamento meio-para-par em centavos a cada etapa.
- Formato da resposta e ordem das validações.

## Estrutura escolhida

- Cada família de variação é um `enum` com métodos abstratos — cada constante
  carrega o seu próprio comportamento no corpo da constante. Nada de `switch`
  nem cadeia de `if`.
    - `ModalidadeEntrega`: `custoBruto(peso)`, `prazoDias()`, `disponivelPara(peso)`.
    - `Cupom`: `desconto(subtotal, itens, frete)`, `aplicavel(subtotal)`.
    - `NivelClube`: `cashback(produtos)`, `ajustaFrete(frete)`, `brinde(produtos)`.
    - `FormaPagamento`: `calcular(total, parcelas)`, `parcelasValidas(parcelas)`,
      `disponivelPara(total)`.
    - `Regiao`: só carrega a porcentagem; a fórmula vive na calculadora.
- `CalculadoraResumo` orquestra: valida em sequência (um método por etapa, retornando
  o primeiro erro encontrado) e, se tudo passa, chama cada família acima.
- `Dinheiro.aCentavos` concentra o arredondamento HALF_EVEN para 2 casas.
- Erro é um `record` próprio devolvido pelo controller como JSON `{"erro": "..."}`
  com HTTP 200, como pede o enunciado.

## Por que

- O enunciado diz que modalidades, cupons e níveis novos entram "o tempo todo"
  / "estamos estudando". A estrutura por enum com corpo próprio deixa adicionar
  um caso novo tocando num lugar só.
- Região o próprio enunciado grifa que "só a porcentagem muda", então não cabe
  método por constante — vira campo e a fórmula fica compartilhada.
- Validação em sequência linear, uma função por etapa, cumpre a ordem exigida
  sem aninhar `if`s.
