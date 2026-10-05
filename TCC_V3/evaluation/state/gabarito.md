---
padrao: state
enunciado: experiment/prompt/state.md
enunciado_hash: ebffe1724ca316b55ea218ef53e3ba4c1928a2ee0b5137be4f36af753ef98580
lotes: STATE
---

# Gabarito: enunciado do State

> **RASCUNHO**, junto com a [régua](../regua.md). Nível 3: vale só para o enunciado
> e os lotes do cabeçalho. **Ainda não rodado nem calibrado**: antes do lote, uma
> rodada `SMOKE` para ver se o enunciado não bate no teto nem no chão, e uma
> calibração da régua sobre ela.
>
> Este arquivo mora na mesma pasta dos pacotes que ele lê (`packages/`, ao lado).

**O que este enunciado testa, e o Strategy não testava:** o caso (a situação do
pedido) **muda** durante a vida do objeto, a cada ação. No Strategy o caso é
escolhido uma vez, a partir da entrada, e não muda mais. Aqui, cada situação
decide também **qual é a próxima**.

Para cada ponto de variação: os casos como aparecem no enunciado, o que varia, a
regra comum, o caso exigente e o caso hipotético.

| ponto | tipo | casos |
|---|---|---|
| **E1** ações por situação | positivo | `AGUARDANDO_PAGAMENTO`, `PAGO`, `EM_SEPARACAO`, `ENVIADO`, `ENTREGUE`, `CANCELADO`, `DEVOLVIDO` |
| **E2** efeitos do cancelamento e da devolução | positivo, com casos exigentes | as mesmas situações |
| **E3** texto para o cliente | **controle negativo** | as mesmas situações |

Os três pontos têm os **mesmos casos** (as situações), porque no State a variação
tem um eixo só; o que muda de um ponto para outro é o comportamento lido.

---

## E1: ações por situação

- **O que varia:** quais ações valem em cada situação e para qual situação cada
  uma leva.
- **Regra comum:** registrar a situação nova no `historico`; recusar com
  `ACAO_NAO_PERMITIDA` o que não vale. Escrever essas duas coisas em cada
  situação é `parte_comum = repetida`.
- **Caso exigente:** nenhum (`assinatura = n/a`). As transições são todas do
  mesmo tipo: uma ação leva a uma situação.
- **O que se procura:** o `switch (situacao)` dentro de cada ação (um método
  `pagar()` com um `switch` sobre a situação, outro `cancelar()` com outro
  `switch`...) é a mesma escolha repetida em vários lugares:
  `selecao = condicional-no-calculo`. Uma unidade por situação, que sabe quais
  ações aceita e para onde cada uma leva, é `isolado` + `consulta`. Uma **tabela**
  de transições (`Map<Situacao, Map<Acao, Situacao>>`) também é `consulta`, e
  para E1 é resposta legítima: as transições, sozinhas, são dado.
- **Caso hipotético:** `RETIDO`, entre `ENVIADO` e `ENTREGUE`: `ENVIAR` passa a
  levar a `RETIDO`, e `RETIDO` aceita `ENTREGAR` (leva a `ENTREGUE`) e `CANCELAR`
  (leva a `CANCELADO`, reembolso do valor total menos o frete). Texto: "Retido
  na fiscalização".

## E2: efeitos do cancelamento e da devolução

- **O que varia:** o valor do reembolso, e os efeitos que acompanham a mudança de
  situação, conforme a situação em que o pedido estava.

  | situação de origem | ação | reembolso | estoque devolvido | coleta |
  |---|---|---|---|---|
  | `AGUARDANDO_PAGAMENTO` | `CANCELAR` | 0 | não | não |
  | `PAGO` | `CANCELAR` | total | não | não |
  | `EM_SEPARACAO` | `CANCELAR` | total − 15,00, mínimo 0 | **sim** | não |
  | `ENTREGUE` | `DEVOLVER` | só os produtos | não | **sim** |

- **Regra comum:** nenhuma listada; `parte_comum = n/a`.
- **Casos exigentes:**
  - `EM_SEPARACAO`: o cancelamento afeta **duas** saídas (reembolso com taxa e
    estoque devolvido), além da mudança de situação.
  - `ENTREGUE`: a devolução afeta **duas** saídas (reembolso só dos produtos e
    coleta agendada).

  Quem desenha o contrato olhando só `AGUARDANDO_PAGAMENTO` e `PAGO` escreve algo
  como "devolve o valor do reembolso", e os exigentes não cabem. O remendo típico
  é um `if (situacao == EM_SEPARACAO) estoqueDevolvido = true` no serviço, fora
  da unidade da situação. `assinatura = comporta` se o que cada situação faz ao
  sair dela passa pelo contrato comum, sem nomear a situação fora dela.
- **Caso hipotético:** o `RETIDO` de E1, com o cancelamento reembolsando o total
  menos o frete, sem estoque nem coleta.

## E3: texto para o cliente (controle negativo)

- **O que varia:** só o texto. O enunciado diz: *"É só o texto que muda de uma
  situação para outra, não tem regra nenhuma por trás."*
- **Resposta proporcional:** o texto como dado (`proporcao = dados`: um campo da
  constante, um mapa) ou um `switch` que só devolve o texto (`condicional`).
- **Exagero:** uma classe, ou um método sobrescrito por situação, **só** para o
  texto, separada da unidade que já cuida do comportamento dela (`estrutura`). Um
  método `descricao()` dentro da unidade que a situação já tem por causa de E1 e
  E2 **não** é exagero: é um dado a mais num lugar que já existe.
- Como é controle negativo, só `forma` e `proporcao` são lidas (régua §2.7).

---

## O que não é ponto de variação

- **A ordem de precedência dos erros** é uma sequência fixa.
- **A criação do pedido** é uma só: todo pedido começa em `AGUARDANDO_PAGAMENTO`.
- **O `historico`** é uma lista comum a todos os pedidos, e não um caso.
- **Guardar os pedidos na memória** é infraestrutura, e não variação.
