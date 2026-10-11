# Desenho — serviço de resumo do checkout

## O que muda de caso para caso

| Eixo | O que varia | Como varia |
|---|---|---|
| Modalidade de entrega | valor do frete, prazo, disponibilidade | cada modalidade tem fórmula própria (fixa, por kg, grátis) e restrição própria (motoboy ≤ 5 kg). Entra opção nova quase toda semana. |
| Cupom | valor do desconto e condição de uso | cada cupom é uma regra diferente (% dos produtos, valor fixo com piso, igual ao frete, unidades grátis por item) |
| Nível do clube | crédito, isenção de frete, brinde | cada nível tem seu conjunto de vantagens; devem vir mais níveis |
| Forma de pagamento | ajuste sobre o total, parcelas permitidas, disponibilidade | Pix (desconto), Boleto (tarifa + teto), Cartão (sem/com juros Price) |
| Região | **só a porcentagem** do seguro | o enunciado diz explicitamente que a conta é a mesma |

## O que é igual em todos os casos

- A ordem do cálculo: produtos → cupom → frete → seguro → total → ajuste do pagamento.
- O arredondamento: sempre centavos, meio-para-o-par (`Dinheiro`).
- A ordem de validação (1..10) e o formato `{ "erro": "CODIGO" }`.
- A conta do seguro e a conta do crédito do clube (porcentagem sobre o subtotal).

## Estrutura escolhida

- `Dinheiro`: único lugar que arredonda (HALF_EVEN, 2 casas).
- `Carrinho` / `ItemPedido`: subtotal, peso, unidades.
- **Um tipo por caso que varia em comportamento**, cada um dono do seu cálculo:
  - `ModalidadeEntrega` → `Economica`, `Expressa`, `RetiradaLoja`, `Motoboy`
  - `Cupom` → `Bemvindo10`, `Menos50`, `FreteGratis`, `Leve3Pague2`
  - `FormaPagamento` → `Pix`, `Cartao`, `Boleto`
  - `NivelClube`: enum com corpo por constante (BRONZE/PRATA/OURO)
- `Regiao`: enum **só com a taxa**, porque ali só a porcentagem muda — criar uma classe por
  região seria estrutura sem variação de comportamento.
- `Catalogo<T extends Identificavel>`: resolve código → caso. A escolha do caso é um lookup,
  não uma cadeia de `if`/`switch`. Modalidades, cupons e formas de pagamento são beans
  injetados como lista: adicionar uma modalidade nova = criar uma classe.
- `CalculadoraResumo`: um pipeline linear, sem ramificação, que faz as validações 1..10 na
  ordem e delega cada parte ao caso correspondente.

## Assinaturas (decididas olhando todos os casos, não o primeiro)

- `ModalidadeEntrega`: `frete(pesoKg)`, `prazoDias()`, `atende(pesoKg)` — peso é o que o caso
  mais exigente (econômica/expressa por kg, motoboy com limite) precisa.
- `Cupom`: `aplicavel(ctx)` / `desconto(ctx)` com `ContextoCupom(carrinho, frete)` — FRETEGRATIS
  precisa do frete, LEVE3PAGUE2 precisa dos itens, MENOS50 precisa do subtotal.
- `FormaPagamento`: `parcelasPermitidas(n)`, `atende(totalPedido)` e
  `calcular(totalPedido, parcelas) -> ResultadoPagamento(totalFinal, valorParcela)` — cartão
  com juros precisa definir o total final e a parcela juntos.
- `NivelClube`: `credito(subtotal)`, `freteCobrado(frete)`, `temBrinde(subtotal)`.

## Decisões de cálculo

- O frete é calculado antes do cupom (a isenção do OURO zera o frete), porque FRETEGRATIS
  tem o desconto igual ao frete efetivamente cobrado.
- Campos do pedido chegam como texto/número cru e são resolvidos pelos catálogos, para que
  valor inexistente vire o código de erro certo em vez de 400 do framework.

## Ajustes depois da revisão

- Arredondamento num lugar só de verdade: cada caso diz o valor cru
  (`ModalidadeEntrega.custo`, `Cupom.valorAbatido`) e o arredondamento mora no `default`
  da interface. A calculadora não arredonda de novo o que já veio em centavos.
- `Codigos.buscar` resolve os conjuntos fechados (clube, região); antes a mesma busca
  estava copiada nos dois enums.
- `Cartao` escolhe entre sem juros e Price com uma condição sobre o número de parcelas.
  É a única escolha que não é lookup, e de propósito: o enunciado descreve o cartão como
  **uma** forma de pagamento com faixas de parcelamento, não como duas formas.
- Corpo HTTP ausente ou mal formado cai no tratador de erros como `PEDIDO_INVALIDO`;
  a classe que existia só para isso saiu.
- Cupom em branco é tratado como "sem cupom", igual a cupom ausente.
