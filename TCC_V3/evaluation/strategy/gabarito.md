---
padrao: strategy
enunciado: history/prompt-v3/prompt.md
enunciado_hash: b7cdb594cb49efee4c0081e947a157b6bd2ebaecc015ec0e5b5f93d973f01e35
lotes: EXT
---

# Gabarito: enunciado do Strategy

> **RASCUNHO**, junto com a [régua](../regua.md). Este é o nível 3: vale só para
> o enunciado e os lotes do cabeçalho acima. O cabeçalho é lido por script: o
> `verificar.mjs` (Parte 3) confere se `enunciado_hash` é o `prompt_hash` gravado
> no `meta.json` de cada execução dos lotes listados.
>
> Este arquivo mora na mesma pasta dos pacotes que ele lê (`packages/`, ao lado).
> É isso que impede ler pacote de um padrão com gabarito de outro.

Para cada ponto de variação: os casos como aparecem no enunciado (é o que se
busca no código), o que varia entre eles, a regra de negócio comum a todos, qual
caso exige mais que os outros e o caso hipotético usado para contar o custo de um
caso novo. O arredondamento para centavos vale em todo lugar e **não** é listado
como parte comum (régua §2.5).

| ponto | tipo | casos |
|---|---|---|
| **P1** entrega | positivo, com caso exigente | `ECONOMICA`, `EXPRESSA`, `RETIRADA_LOJA`, `MOTOBOY` |
| **P2** cupom | positivo, com casos exigentes | `BEMVINDO10`, `MENOS50`, `FRETEGRATIS`, `LEVE3PAGUE2` |
| **P3** pagamento | positivo, com casos exigentes | `PIX`, `CARTAO`, `BOLETO` |
| **P4** clube | positivo, com caso exigente (**o foco**) | `BRONZE`, `PRATA`, `OURO` |
| **P5** imposto | **controle negativo** | `SUDESTE`, `SUL`, `CENTRO_OESTE`, `NORTE`, `NORDESTE` |

---

## P1: entrega

- **O que varia:** o custo do frete, o prazo e se a opção está disponível.
- **Regra comum:** `ECONOMICA` e `EXPRESSA` usam a **mesma fórmula** (fixo +
  valor por kg × peso), só com números diferentes. Escrever a conta nas duas é
  `parte_comum = repetida`; uma função comum chamada pelas duas é `unica`.
- **Caso exigente:** `MOTOBOY`, o único com restrição de disponibilidade (até 5 kg,
  senão `MODALIDADE_INDISPONIVEL`). `assinatura = comporta` se a pergunta "está
  disponível?" faz parte do contrato comum, como método ou como dado (um
  `pesoMaximo` opcional lido sem nomear o caso); `remendo` se o limite de 5 kg é
  um `if` sobre `MOTOBOY` fora dele.
- **Caso hipotético:** `AGENDADA`, R$ 20,00 fixo, prazo 3 dias, sempre disponível.
- **Nota:** o enunciado avisa que "quase toda semana entra uma opção nova". A
  variação aqui é descrita, e não imaginada.

## P2: cupom

- **O que varia:** o valor do desconto, e se o cupom se aplica ao pedido.
- **Regra comum:** nenhuma regra de negócio é comum aos quatro cupons além de
  "o desconto sai dos produtos", que não é uma conta. `parte_comum = n/a`.
- **Casos exigentes:**
  - `MENOS50`: tem condição de aplicação (produtos a partir de R$ 300,00, senão
    `CUPOM_NAO_APLICAVEL`).
  - `FRETEGRATIS`: o desconto depende do **frete**, que na ordem de cálculo vem
    depois do cupom. O contrato precisa dar acesso ao frete.
  - `LEVE3PAGUE2`: o desconto depende dos **itens**, um a um, e não só do
    subtotal.

  `assinatura = comporta` se o contrato comum atende os três sem `if` sobre o
  cupom fora da estrutura.
- **Caso hipotético:** `MENOS20`, R$ 20,00 de desconto nos produtos, sem condição.
- **Interação:** `FRETEGRATIS` com clube `OURO`. O OURO já zera o frete, então o
  desconto do cupom fica igual a um frete de R$ 0,00. Tratar essa colisão com um
  `if` que cita os dois é `remendo` no ponto em que ele estiver.

## P3: pagamento

- **O que varia:** o ajuste sobre o total (desconto, tarifa ou juros), as parcelas
  permitidas e se a forma está disponível.
- **Regra comum:** nenhuma listada; `parte_comum = n/a`. A única candidata seria a
  relação `totalFinal = total + ajuste`, mas ela tem um operador só e a resposta
  da API exige os dois campos, então quase todo desenho a escreve em mais de um
  lugar. Na calibração ela deu `repetida` em quase todo pacote e mediu o formato
  do contrato, não a separação do que é comum (régua §5, decisão 8).
- **Casos exigentes:**
  - `CARTAO`: é o único com parcelas (1 a 12) e juros acima de 3× (tabela Price).
  - `BOLETO`: tem restrição de disponibilidade. Fica **indisponível quando
    produtos − cupom + frete, sem o imposto, passa de R$ 1.000,00**
    (`FORMA_PAGAMENTO_INDISPONIVEL`).
- **Caso hipotético:** `VALE`, pagamento com vale-presente, sem ajuste, só à
  vista (mais de uma parcela dá `PARCELAMENTO_INVALIDO`, como PIX e BOLETO),
  sempre disponível.

## P4: clube (o ponto desenhado para a assinatura)

- **O que varia:** quantas saídas o nível afeta. `BRONZE` não afeta nada; `PRATA`
  afeta **uma** (o crédito); `OURO` afeta **três** (o crédito, o frete e o brinde).
- **Regra comum:** o crédito é uma porcentagem dos produtos, sem desconto e sem
  frete, em `PRATA` e em `OURO`; só a porcentagem muda. A conta escrita nas duas é
  `repetida`; uma função comum chamada pelas duas é `unica`.
- **Caso exigente:** `OURO`. Quem decide a assinatura olhando `BRONZE` e `PRATA`
  escreve "devolve o crédito", e o OURO não cabe: ele zera o frete, que é
  calculado em outro lugar. O remendo típico é um `if (nivel == OURO)` no cálculo
  do frete. É o que a regra 3 do harness pede para evitar.
- **Caso hipotético:** `DIAMANTE`, 7% de crédito, não paga frete, brinde acima
  de R$ 300,00.
- **Nota:** o enunciado diz "estamos estudando criar mais níveis". A variação é
  descrita.

## P5: imposto (controle negativo)

- **O que varia:** só a porcentagem. O enunciado diz: *"É só a porcentagem que
  muda, a conta é a mesma em todas."*
- **O que é comum:** toda a conta (porcentagem sobre os produtos já com o desconto
  do cupom). Como é controle negativo, só `forma` e `proporcao` são lidas (régua
  §2.7).
- **Resposta proporcional:** a variação como dado (`proporcao = dados`) ou um
  `switch` que só devolve a porcentagem (`condicional`). Uma classe ou uma
  constante de enum com corpo por região é `estrutura`: o **exagero** que este
  ponto existe para medir.
- **Caso exigente:** nenhum (`assinatura = n/a`).
- **Caso hipotético:** não se aplica; no controle negativo o custo é `n/a`.

---

## O que não é ponto de variação

Para não virar ponto por engano na leitura:

- **A ordem de precedência dos erros** (10 códigos) é uma sequência fixa, e não
  uma variação por caso. Não é lida pela régua.
- **A moeda.** O enunciado diz "só reais". Estrutura para moeda é `especulativa`.
- **A troca de produto.** O enunciado diz que "não entra no cálculo".
