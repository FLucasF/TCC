# Desenho — serviço de resumo do checkout

## O que muda de caso para caso

| Eixo | O que varia | Novos casos previstos pelo enunciado |
|---|---|---|
| Modalidade de entrega | valor do frete, prazo em dias, se atende o pedido (peso) | sim, "quase toda semana" |
| Cupom | como calcula o desconto, qual condição exige | sim, marketing inventa promoção |
| Nível do clube | % de crédito, se paga frete, se manda brinde | sim, "estudando criar mais níveis" |
| Forma de pagamento | ajuste sobre o total, parcelas permitidas, se atende o total | não anunciado, mas já são 3 regras bem diferentes |

## O que é igual em todos os casos

- A ordem do cálculo: produtos → cupom → frete → seguro → total → pagamento.
- O arredondamento: sempre centavos, meio-para-par (`Dinheiro.centavos`).
- O seguro: **só a alíquota muda por região**, a conta é a mesma → região é um
  enum com uma taxa, não uma estratégia.
- O crédito do clube e o brinde incidem sempre sobre o subtotal de produtos.
- A ordem de validação (10 códigos de erro) é uma sequência fixa.

## Estrutura escolhida

- Um contrato por eixo que varia (`ModalidadeEntrega`, `Cupom`, `NivelClube`,
  `FormaPagamento`), cada caso numa classe só dele, registrada como bean.
- Escolher o caso é busca por código num `Catalogo<T>` (mapa montado a partir
  dos beans), nunca uma cadeia de `if`/`switch`. Adicionar uma transportadora
  nova = criar uma classe nova, sem tocar no resto.
- `CalculadoraResumo` orquestra: conhece a ordem do cálculo e a ordem das
  validações, não conhece nenhum caso concreto.
- Código inexistente → `Optional.empty()` do catálogo → erro mapeado; o
  catálogo nunca lança, para a calculadora decidir qual código devolver.

## Assinaturas (decididas olhando o caso mais exigente)

- `ModalidadeEntrega`: `atende(pesoKg)` + `calcular(pesoKg) -> Frete(valor, prazoDias)`
  (motoboy precisa recusar por peso; econômica/expressa precisam do peso).
- `Cupom`: `aplicavel(ctx)` + `desconto(ctx)`, com
  `ContextoCupom(itens, subtotal, frete)` — LEVE3PAGUE2 precisa dos itens,
  MENOS50 do subtotal, FRETEGRATIS do frete.
- `NivelClube`: `credito(subtotal)`, `freteGratis()`, `brinde(subtotal)`.
- `FormaPagamento`: `aceitaParcelas(n)`, `atende(total)`,
  `calcular(total, n) -> ResultadoPagamento(totalFinal, valorParcela)`;
  o `ajustePagamento` é derivado (`totalFinal - total`) pela calculadora.

## Decisões de borda

- Entrada chega como texto nos campos de código (`modalidadeEntrega`, `cupom`,
  `formaPagamento`, `nivelClube`, `regiao`) para que valor inválido vire o
  código de erro certo, e não erro de desserialização.
- Dinheiro é `BigDecimal` de ponta a ponta (meio-para-par exige decimal exato).
- Cupom ausente = cupom "nenhum" (desconto zero), não é erro.
- Erro responde `{ "erro": "CODIGO" }` com HTTP 400.

## Ajustes depois da revisão

- Cartão sem juros vs. com juros é **uma condição interna** de `PagamentoCartao`
  (`parcelas <= 3`), não um caso novo: o enunciado trata cartão como uma única
  forma de pagamento, com uma regra que tem faixa. Fica registrado para não
  parecer descuido.
- Arredondar o frete é da calculadora: as modalidades devolvem o valor cru
  (`RetiradaNaLoja` inclusive), e o contrato diz isso, para que a próxima
  transportadora não precise adivinhar.
- `CalculadoraResumo` e os DTOs moram em `aplicacao`; `web` só tem o controller
  e o tradutor de erro.
- "A cada 3 unidades de um mesmo item" é lido como por linha do carrinho (é o
  que os exemplos conferidos mostram); duas linhas do mesmo produto não somam.
