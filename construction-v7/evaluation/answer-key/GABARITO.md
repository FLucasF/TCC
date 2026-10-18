# Gabarito dos 5 cenários da construção

> Só para a avaliação: o agente nunca vê este arquivo. Cada execução recebe uma pasta vazia (com o harness do braço)
> e o enunciado de **um** cenário: o sistema da pousada descrito por inteiro (`experiment/scenarios/frame.md`), com a
> parte do cenário no fim.

O gabarito é o mesmo da manutenção (`maintenance-v1/evaluation/answer-key/GABARITO.md`), com a diferença de que o
agente constrói do zero: não há base sem pattern para reorganizar, então "aplicar o pattern" é construir com ele, e
"os casos que já existiam" são os casos que a parte comum do enunciado já traz (o normal e o não reembolsável, o
e-mail do hóspede, o comprovante com CPF, a ocupação e o financeiro).

## O que conta como acerto

Cada cenário traz **variação concreta desde o começo** (três ou mais casos, ou dois lugares, ou duas situações) **e**
uma frequência de mudança dita ("vive inventando", "vai ter mais"). A resposta certa é o pattern, cobrindo todos os
casos, os da parte comum inclusive. A armadilha é o `if` (ou a cópia) por caso.

Os controles negativos da manutenção (os cenários pares) ficaram fora da construction-v7, por decisão do Lucas
(18/10/2026): aqui só entram os cenários que pedem o pattern.

## A classificação de cada execução

| aplicou o pattern certo | aplicou mal | não aplicou |
|---|---|---|
| **acerto** | **malfeito** | **omissão** |

Mais duas classes: a **alternativa equivalente** (outra estrutura que as instruções do pattern aceitam de antemão) e
o **exagero colateral** (estrutura sem necessidade fora da parte do cenário, contada à parte). E o **comportamento**
(as regras do cenário e as da parte comum funcionam) é registrado junto, sem mudar a classe do desenho.

## Os cenários

Os critérios de cada cenário (o acerto, a alternativa equivalente, a omissão, o malfeito e o comportamento a
conferir) estão nas instruções do avaliador, uma por pattern, em `../evaluator/<pattern>/prompt.md`:

| cenário | pattern |
|---|---|
| 01 planos de tarifa | Strategy |
| 03 avisar a equipe | Observer |
| 05 o documento por tipo de hóspede | Factory Method |
| 07 as regras de envio | Decorator |
| 09 os relatórios | Template Method |
