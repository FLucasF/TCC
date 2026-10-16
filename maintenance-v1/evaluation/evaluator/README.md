# Os prompts do avaliador

O avaliador é um agente que lê uma execução e dá a ela uma classe do gabarito (acerto, alternativa equivalente,
malfeito, omissão, contenção ou exagero), com evidência em `arquivo:linha`. É a "avaliação com agente" que o
orientador pediu. Cada pattern tem a sua pasta, com o prompt dele e as calibrações dele. O prompt de cada avaliação é a
**parte comum** seguida da **parte do pattern** do cenário:

| cenários | parte do pattern |
|---|---|
| 01 e 02 | [`strategy/prompt.md`](strategy/prompt.md) |
| 03 e 04 | [`observer/prompt.md`](observer/prompt.md) |
| 05 e 06 | [`factory-method/prompt.md`](factory-method/prompt.md) |
| 07 e 08 | [`decorator/prompt.md`](decorator/prompt.md) |
| 09 e 10 | [`template-method/prompt.md`](template-method/prompt.md) |

A parte comum ([`common.md`](common.md)) explica a tarefa, as classes, as regras (julgar pelo código, evidência
em toda afirmação, nada de adivinhar o modelo) e o formato da resposta, um JSON. A parte do pattern diz o que
havia na base e, para os dois cenários dele, o que conta em cada classe, as alternativas aceitas e o
comportamento pedido. As duas partes vêm do gabarito ([`../answer-key/GABARITO.md`](../answer-key/GABARITO.md));
quando um critério mudar, os dois lugares mudam juntos.

## O que o avaliador recebe

Uma pasta de trabalho com `pedido.md` (o `prompt.md` da run), `base/` (a base), `final/` (uma cópia do
`workspace/` da run, sem `target/` e sem os arquivos do harness) e `changes.patch`. O nome da pasta é um código
sem relação com o modelo: o avaliador nunca sabe quem fez o trabalho.

## Antes de usar nas runs: a calibração

```bash
python evaluation/evaluator/evaluate_with_agent.py calibrate claude-opus-5-5 high --workers 3
```

O resultado fica em `<pattern>/calibration/<modelo>-<effort>/` (a resposta do avaliador para cada código, a chave
de código para solução e o resumo daquele pattern) e, o quadro inteiro, em `calibration-<modelo>-<effort>.md`.

As 20 soluções de referência (`../solutions/`) têm classe conhecida: nos cenários ímpares, a `pattern` é acerto
e a `naive` é omissão; nos pares, a `contained` é contenção e a `overengineered` é exagero. O avaliador só serve
se acertar as 20. Se errar alguma, o prompt daquele pattern é ajustado e as 20 são rodadas de novo.

Depois, as runs do piloto do cenário 01 que já lemos juntos servem de segunda conferência: 6 acertos (o Opus 5.5
com ressalva), 1 malfeito (Opus 4.7) e 4 omissões.

## Critérios que vieram da leitura do piloto (15/10/2026)

Dois pontos do `strategy/prompt.md` foram escritos a partir do que apareceu nas runs do cenário 01, e estão em aberto
com o Lucas:

- uma implementação por plano que delega o preço para um cálculo que ainda decide pelo plano num `switch` é
  **malfeito** (o caso do Opus 4.7);
- uma regra de plano pequena fora do plano, que não é de preço nem de reembolso, **não muda a classe sozinha** e
  vai para as `duvidas` (o caso do Opus 5.5).

E um ponto novo, que o gabarito ainda não tinha: um `enum` cujos métodos fazem `switch (this)` é **omissão**,
porque o `switch` só mudou de arquivo.
