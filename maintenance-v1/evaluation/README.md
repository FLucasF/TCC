# A avaliação do maintenance-v1

Três peças. A **suíte** diz se o serviço do agente funciona. As **soluções de referência** provam que a
suíte e o cenário estão certos. O **gabarito** diz o que conta como acerto, omissão, malfeito ou exagero no
desenho. A suíte não olha o desenho: a solução com o pattern e a ingênua passam igual. Separar as duas é
trabalho do avaliador (ainda por fazer), com o gabarito na mão.

## A suíte (`suite/`)

Sobe o serviço do agente e conversa com ele pela API, como o site faria. Cada checagem começa com um
serviço novo, de memória vazia, para uma não ver as reservas da outra.

Ela tem duas partes:

- **a regressão** (`regression.py`): o que a base já fazia e todo cenário tem que continuar fazendo (os
  preços, as recusas, o ciclo da reserva, o reembolso, o e-mail, os relatórios). Quando um cenário muda
  de propósito alguma coisa que a regressão olha (a porcentagem da temporada no 02, o título dos relatórios
  no 09, a coluna nova no 10), só aquela parte é pulada ou adaptada, e isso é decidido pelo texto do
  cenário, nunca pelo que o agente fez;
- **a aceitação** (`acceptance_NN_*.py`): o que o pedido daquele cenário pediu. Por enquanto só existe a
  do 01.

```bash
python evaluation/suite/run_suite.py experiment/base/hotel 00 suite-out/base
```

O `00` roda só a regressão (a base contra ela mesma). Com `01` a `10`, roda a regressão e a aceitação do
cenário. A pasta do projeto nunca é tocada: o Maven roda numa cópia. Saem `suite-report.txt`,
`suite-result.json` (cada checagem, com o motivo de cada falha), `build.log` (o `mvn verify` do agente; se os
testes dele falham, o serviço é empacotado sem eles, para a suíte ainda poder julgar) e `service.log`.

**Onde o tipo é fraco:** a suíte é Python com mypy estrito, mas o JSON que volta do serviço do agente é
`Any`, porque é uma fronteira de tempo de execução. Cada checagem diz o que espera encontrar, e o dinheiro é
lido como `Decimal`, nunca como `float`.

## As soluções de referência (`solutions/`)

Para cada cenário, patches sobre a base: `NN-pattern.patch` (a resposta do gabarito) e `NN-naive.patch` (o
mesmo comportamento sem o pattern, o `if` a mais). O `check_solutions.py` é o teste de "poder de
discriminação" (a ideia da v3 do benchmark do Akita):

```bash
python evaluation/solutions/check_solutions.py 01 solutions-out/01
```

Ele aplica cada patch numa cópia limpa da base e roda a suíte. O esperado é que **a base falhe em todas as
checagens de aceitação**, para provar que a aceitação mede o pedido, e que **todas as soluções passem na
suíte inteira**, para provar que o cenário dá para fazer e que a suíte não reprova o certo.

| cenário | base | pattern | ingênua | data |
|---|---|---|---|---|
| 01 · planos de tarifa (Strategy) | regressão 13/13, aceitação 0/7 | 20/20 | 20/20 | 15/10 |

Os patches foram escritos pelo Claude (15/10) e ainda não foram lidos pelo Lucas.

## O gabarito (`answer-key/GABARITO.md`)

A classificação de cada execução e, por cenário, o que conta como acerto, alternativa equivalente,
omissão, malfeito e exagero, e o que a aceitação confere.

## As checagens

```bash
python -m mypy
```

```bash
python -m ruff check evaluation
```
