# Desenho — serviço de resumo do checkout

## O que muda de caso para caso

| Eixo | O que varia | Caso mais exigente (define a assinatura) |
|---|---|---|
| Modalidade de entrega | custo, prazo, se atende o pedido | custo depende do peso (ECONOMICA/EXPRESSA); disponibilidade depende do peso (MOTOBOY ≤ 5 kg) |
| Cupom | desconto e condição de uso | FRETEGRATIS precisa do frete; LEVE3PAGUE2 precisa dos itens; MENOS50 precisa do subtotal |
| Nível do clube | crédito, isenção de frete, brinde | OURO (três vantagens ao mesmo tempo) |
| Forma de pagamento | ajuste, parcelas permitidas, disponibilidade | CARTAO (parcela por tabela Price, 1..12); BOLETO precisa do total do pedido |
| Região | **só** o percentual do seguro | — (a conta é a mesma; enunciado é explícito) |

## O que é igual em todos os casos

- Arredondamento para centavos, meio para o par (`Dinheiro.centavos`).
- A ordem do cálculo: produtos → cupom → frete → seguro → total → pagamento.
- A conta do seguro (percentual × subtotal) e a do crédito (percentual × subtotal).
- O formato do resumo e o formato do erro.

## Estrutura escolhida

- `ModalidadeEntrega`, `Cupom`, `NivelClube`, `FormaPagamento`: **enums com corpo por
  constante**. Cada caso tem seu comportamento num lugar só dele; a escolha é feita por
  `valueOf` (`Enums.buscar`), nunca por cadeia de `if`/`switch`.
- `Regiao`: enum de **dados** (apenas o percentual), porque só o percentual varia.
- `Cupom` recebe `ContextoCupom(itens, subtotal, frete)` — assinatura desenhada para o caso
  mais exigente, não para o primeiro.
- `FormaPagamento.cobrar(total, parcelas)` devolve `ResultadoPagamento(totalFinal, valorParcela)`;
  o ajuste é derivado (`totalFinal − total`) e por isso não é responsabilidade de cada caso.
- `CalculadoraResumo` é a parte igual em todos os casos: aplica a ordem do cálculo e pergunta
  a cada eixo o que é dele.
- Validação: os erros 1–3 (itens/clube/região) e as traduções de código inexistente vivem na
  tradução da requisição; as regras "existe mas não atende" vivem no próprio caso
  (`atende`, `aplicavel`, `parcelasValidas`, `disponivel`). A ordem exigida pelo enunciado sai
  da ordem natural do cálculo, não de uma cadeia de condições: cada etapa valida o seu eixo no
  momento em que já tem os valores de que precisa e lança `PedidoRecusadoException`.

## Decisões de leitura do enunciado

- **Precedência dos erros**: a lista 1–10 é regra de negócio e está escrita num lugar só, no
  javadoc de `CodigoErro`; `CalculadoraResumo` confere nessa ordem (etapas numeradas) e os
  testes de precedência travam cada par vizinho.
- **OURO + FRETEGRATIS**: o cupom vê o frete que vai aparecer no resumo, já com a isenção do
  clube. Logo o desconto é R$ 0,00 — descontar o frete da modalidade daria desconto sobre algo
  que o cliente não estava pagando.
- **Cartão até 3x / de 4x a 12x**: é um limiar dentro da política do cartão, não dois casos que
  o enunciado trate como independentes; ficou como uma única condição dentro de `CARTAO`, sem
  criar estrutura nova.

## Por quê

A loja diz que "quase toda semana entra uma opção nova de entrega" e que vão criar mais níveis
de clube, e marketing inventa cupom novo sempre. Entrega, cupom, clube e pagamento entram como
novas constantes com o comportamento junto — sem tocar na calculadora. Região não ganhou
estrutura: o enunciado diz que só a porcentagem muda.
