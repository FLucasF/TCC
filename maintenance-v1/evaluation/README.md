# A avaliação do maintenance-v1

Por enquanto, a avaliação é feita pelo Lucas e pelo Claude, com duas peças na mão: o **gabarito**, que diz o
que conta como acerto, omissão, malfeito ou exagero no desenho, e as **soluções de referência**, um exemplo
de cada lado para cada cenário. As ferramentas automáticas vêm depois.

## O gabarito (`answer-key/GABARITO.md`)

A regra que separa os cenários que pedem o pattern dos controles negativos, a classificação de cada execução
e, por cenário, o que conta como acerto, alternativa equivalente, omissão, malfeito e exagero, e o
comportamento novo que o pedido exige.

## As soluções de referência (`solutions/`)

Para cada cenário, duas soluções, como patches sobre a base:

- nos cenários que pedem o pattern (os ímpares): `NN-pattern.patch`, a resposta do gabarito, e
  `NN-naive.patch`, o mesmo comportamento sem o pattern (o `if` a mais, a cópia a mais);
- nos controles negativos (os pares): `NN-contained.patch`, a mudança pequena e certa, e
  `NN-overengineered.patch`, a mesma mudança com a estrutura que ninguém pediu.

As duas de cada cenário fazem a mesma coisa: o que as separa é só o desenho, e é isso que a avaliação tem que
enxergar. As 20 compilam e passam nos próprios testes (`mvn verify`); em 15/10/2026 também passaram numa
suíte pela API que depois foi retirada. Foram escritas pelo Claude e ainda não foram lidas pelo Lucas.

Para ver uma solução aplicada, numa cópia da base (de dentro da cópia):

```bash
git apply ../../evaluation/solutions/01-pattern.patch
```

**Os geradores** (`solutions/generators/`): cada patch é o resultado de um script, `solve_NN.py`, que copia a
base para uma pasta temporária, faz as trocas de texto exatas, roda o `mvn verify` da solução e grava o patch.
Se a base mudar, é só rodar o script de novo; quando uma troca não encontra mais o texto esperado, o script
para e diz qual.

```bash
python evaluation/solutions/generators/solve_01.py
```

## As checagens do Python

```bash
python -m mypy
```

```bash
python -m ruff check evaluation
```
