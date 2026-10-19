# Os harnesses do construction-v7

Cada pasta aqui é um braço do experimento: o que vai para a raiz do workspace, que começa vazio. Os braços acumulam,
como na manutenção:

| braço | o que o agente recebe | pasta |
|---|---|---|
| **N0** | só o enunciado | nenhuma |
| **N1** | o `CLAUDE.md` | `N1/` |
| **N2** | o `CLAUDE.md` do N1 com uma linha a mais, mandando consultar a skill, e a skill `gof-patterns` | `N2/` |
| **N2u** | o N2 com a linha da skill dizendo "use" em vez de "consulte" (teste à parte, fora das 45) | `N2u/` |

O `CLAUDE.md` do N2 é o do N1, intacto, com um item a mais no fim: "Antes de decidir a estrutura, consulte a skill
`gof-patterns`." A diferença entre o N1 e o N2 é, então, a skill **e** a instrução de consultá-la: sem a instrução,
nenhum agente abria a skill (0 de 110 no construction-v5; 0 de 11 na manutenção antes da linha, 10 de 11 depois).

**O N2u** (teste à parte, 19/10/2026) é o N2 com a última linha trocada por "Antes de decidir a estrutura, use a
skill `gof-patterns` e aplique no código o que ela indicar para este projeto." A skill é a mesma. Motivo: no N2, o
Sonnet 4.5 abriu a skill passando uma pergunta ("Qual padrão usar?"); o Claude Code entrega essa pergunta como
mensagem do usuário, e ele respondeu como consultor e encerrou a execução sem código (01 e 07), ou saiu com o pattern
errado (09). O N2u roda só o Sonnet 4.5 nesses três cenários, para ver se o verbo muda isso.

## O `CLAUDE.md`

Adaptado do da manutenção (decisão do Lucas, 17/10/2026), com as mesmas ideias e o mesmo tom, trocando o que era de
mudança (ler o código que a mudança toca, mudar só o que o pedido precisa) pelo que é de construção. Curto de
propósito, como a Anthropic recomenda, e sem nomear nenhum pattern (isso fica para a skill do N2). Fala dos dois
lados do que se mede: dar a cada caso um lugar seu quando os casos crescem, e não criar estrutura para uma variação
que ninguém pediu.

**A régua fica fora do harness** (decisão do Lucas, 17/10/2026). A primeira versão tinha mais dois itens: "os termos e
as unidades do sistema de fora ficam numa peça só, do lado dele" e "teste o que o enunciado pede, conferindo os
valores". Eram quase as perguntas da correção no P6 (o Adapter) e no T2 (as asserções conferem valores?), e o
enunciado de propósito não pede testes: com eles, a correção premiaria quem recebeu a própria pergunta. Saíram antes
de qualquer execução. Ficam só os dois princípios, que são a hipótese do estudo, e não uma dica da prova: eles não
nomeiam pattern, caso nem ponto do enunciado. Com isso, o Adapter só pode melhorar pela skill do N2, e os testes, o
banco, a arquitetura e o comportamento medem se um harness voltado aos padrões muda o resto, sem dica direta. O
texto do TCC declara que os princípios do N1 são os mesmos que a régua usa para julgar os padrões.

## A skill

A mesma da manutenção e do construction-v5, byte a byte (veio de `maintenance-v1/experiment/harnesses/N2/`). Ela
cobre os quatro patterns que a correção olha aqui (Strategy, Adapter, State e Factory Method) e fala de quando não
usar cada um.

O `run_one_execution.py` só roda o N1 e o N2 com a pasta do braço commitada; o `meta.json` guarda o hash da árvore
(`harness_tree`) e diz se o agente abriu a skill (`skill_opened`).

## Na construction-v7

Os mesmos arquivos da `construction-v6` (o `CLAUDE.md` do N1 e do N2 e a skill), copiados sem mudança em 18/10/2026. O que este README diz da TransLog e dos testes vem da `construction-v6`, onde o enunciado era a loja; aqui o enunciado é a pousada, e os dois princípios do `CLAUDE.md` valem igual.
