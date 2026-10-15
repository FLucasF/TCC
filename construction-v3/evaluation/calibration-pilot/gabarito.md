---
padrao: strategy
enunciado: history/pilot/prompt.md
enunciado_hash: 53db3424b397279573658bfc048a369a33e0a2c8b71530252105e4f841bfd124
lotes: SMOKE, BATCH
---

# Gabarito de calibração: enunciado do piloto (três pontos)

> **CALIBRAÇÃO.** Estes pacotes estão fora da análise. Servem para achar onde a
> [régua](../regua.md) faz hesitar antes de ela ser congelada.
>
> O enunciado do piloto tem só P1 a P3, com **os mesmos casos** do enunciado do
> Strategy (conferido). As seções abaixo são cópia das seções P1 a P3 de
> [`evaluation/strategy/gabarito.md`](../strategy/gabarito.md), sem o que depende do
> clube, que este enunciado não tem. O arredondamento não é parte comum (régua §2.5).

| ponto | tipo | casos |
|---|---|---|
| **P1** entrega | positivo, com caso exigente | `ECONOMICA`, `EXPRESSA`, `RETIRADA_LOJA`, `MOTOBOY` |
| **P2** cupom | positivo, com casos exigentes | `BEMVINDO10`, `MENOS50`, `FRETEGRATIS`, `LEVE3PAGUE2` |
| **P3** pagamento | positivo, com casos exigentes | `PIX`, `CARTAO`, `BOLETO` |

Não há controle negativo neste enunciado.

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
  - `BOLETO`: tem restrição de disponibilidade. Fica **indisponível quando o total
    do pedido passa de R$ 1.000,00** (`FORMA_PAGAMENTO_INDISPONIVEL`).
- **Caso hipotético:** `VALE`, pagamento com vale-presente, sem ajuste, só à
  vista (mais de uma parcela dá `PARCELAMENTO_INVALIDO`, como PIX e BOLETO),
  sempre disponível.

---

## O que não é ponto de variação

- **A ordem de precedência dos erros** é uma sequência fixa, e não uma variação
  por caso.
- **A moeda.** O enunciado diz "só reais". Estrutura para moeda é `especulativa`.
- **A troca de produto.** Não entra no cálculo.
