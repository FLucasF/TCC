# Gabarito dos 10 cenários da construção

> Só para a avaliação: o agente nunca vê este arquivo. Cada execução recebe uma pasta vazia (com o harness do braço)
> e o enunciado de **um** cenário: o sistema da pousada descrito por inteiro (`experiment/scenarios/frame.md`), com a
> parte do cenário no fim.

O gabarito é o mesmo da manutenção (`maintenance-v1/evaluation/answer-key/GABARITO.md`), com a diferença de que o
agente constrói do zero: não há base sem pattern para reorganizar, então "aplicar o pattern" é construir com ele, e
"os casos que já existiam" são os casos que a parte comum do enunciado já traz (o normal e o não reembolsável, o
e-mail do hóspede, o comprovante com CPF, a ocupação e o financeiro).

## A regra que separa os dois tipos de cenário

O cenário que pede o pattern traz **variação concreta desde o começo** (três ou mais casos, ou dois lugares, ou duas
situações) **e** uma frequência de mudança dita ("vive inventando", "vai ter mais"). A resposta certa é o pattern,
cobrindo todos os casos, os da parte comum inclusive. A armadilha é o `if` (ou a cópia) por caso.

O controle negativo traz **uma regra simples** (uma data, um texto, um campo, uma coluna) e uma frase de futuro vaga
("talvez um dia… mas por enquanto"). A resposta certa é a solução simples. A armadilha é levar a sério o "talvez um
dia" e construir a estrutura para ele (YAGNI).

## A classificação de cada execução

| | aplicou o pattern certo | aplicou mal | não aplicou |
|---|---|---|---|
| **cenário que pede** | **acerto** | **malfeito** | **omissão** |
| **controle negativo** | **exagero** | **exagero** | **contenção** |

Mais duas classes: a **alternativa equivalente** (outra estrutura que as instruções do pattern aceitam de antemão) e
o **exagero colateral** (estrutura sem necessidade fora da parte do cenário, contada à parte). E o **comportamento**
(as regras do cenário e as da parte comum funcionam) é registrado junto, sem mudar a classe do desenho.

## Os cenários

Os critérios de cada cenário (o acerto, a alternativa equivalente, a omissão, o malfeito, a contenção, o exagero e o
comportamento a conferir) estão nas instruções do avaliador, uma por pattern, em `../evaluator/<pattern>/prompt.md`:

| cenário | pattern | tipo |
|---|---|---|
| 01 planos de tarifa | Strategy | pede |
| 02 a semana da Páscoa | Strategy | controle negativo |
| 03 avisar a equipe | Observer | pede |
| 04 o texto da confirmação | Observer | controle negativo |
| 05 o documento por tipo de hóspede | Factory Method | pede |
| 06 as crianças na reserva | Factory Method | controle negativo |
| 07 as regras de envio | Decorator | pede |
| 08 a assinatura das mensagens | Decorator | controle negativo |
| 09 os relatórios | Template Method | pede |
| 10 a coluna do hóspede | Template Method | controle negativo |

A única regra de comportamento que difere da manutenção é a do cenário 02: na construção o acréscimo da temporada
continua 20% (a manutenção pedia 25%), e a Páscoa entra como alta temporada.
