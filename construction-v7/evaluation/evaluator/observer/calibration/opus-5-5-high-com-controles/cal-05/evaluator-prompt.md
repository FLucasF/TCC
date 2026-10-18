# A avaliação de uma execução

Você vai avaliar o desenho de um sistema pequeno, o de reservas de uma pousada, escrito em Java com Javalin, que um
agente de programação construiu do zero. O agente recebeu uma pasta vazia e um enunciado escrito pelo dono da
pousada: o enunciado descreve o sistema inteiro e, no fim, traz uma parte que precisa estar nele desde o primeiro
dia. Você não sabe qual agente ou modelo fez o trabalho, e não deve tentar adivinhar.

Na pasta de trabalho você tem duas coisas. O `enunciado.md` é o texto exato que o agente recebeu. O `projeto/` é o
projeto como o agente o deixou.

## O que se mede

A parte do fim do enunciado é de dois tipos. Umas pedem, sem dizer o nome, um pattern: o texto mostra uma variação
que já começa com vários casos e que vai continuar crescendo, e a resposta certa é construir essa parte com o
pattern, para todos os casos. As outras são controles negativos: parecem pedir uma estrutura, mas são uma regra
simples, e a resposta certa é a solução simples. Quase sempre o controle negativo termina com uma frase sobre um
futuro vago ("talvez um dia… mas por enquanto"), que não justifica estrutura nenhuma. As instruções do pattern, mais
abaixo, dizem qual enunciado é de qual tipo e o que conta em cada caso.

Você vai dar a cada execução uma destas classes:

- `acerto`: o enunciado precisava do pattern, e ele foi aplicado direito;
- `alternativa equivalente`: o enunciado precisava do pattern, e a variação foi resolvida com outra estrutura que
  as instruções do pattern aceitam de antemão;
- `malfeito`: a estrutura do pattern existe, mas convive com o jeito sem ela. Por exemplo, só uma parte dos casos
  foi para a estrutura, ou quem usa a estrutura ainda pergunta qual é o caso concreto, ou um dos lugares que
  deveriam passar por ela continua fazendo à mão;
- `omissão`: o enunciado precisava do pattern, e a parte foi construída sem ele (um `if`, um `case` ou uma cópia
  por caso);
- `contenção`: o enunciado era um controle negativo, e a solução foi a simples;
- `exagero`: o enunciado era um controle negativo, e foi criada estrutura que ninguém pediu (uma interface, uma
  classe por caso, uma fábrica, um embrulho, um esqueleto genérico).

Além da classe, registre duas coisas. A primeira é o **exagero colateral**: estrutura sem necessidade numa parte do
sistema fora da que o fim do enunciado traz (uma interface com uma implementação só que nada substitui, uma fábrica,
um builder, uma camada que só repassa). Isso vale em qualquer enunciado e é registrado à parte da classe. A segunda é
o **comportamento**: se as regras da parte do fim do enunciado foram implementadas como foram pedidas (valores,
recusas, mensagens), e se o resto do sistema descrito funciona. Um pattern correto com o comportamento errado
continua com a classe do desenho, mas o problema fica anotado.

## Como avaliar

Leia o `enunciado.md` inteiro antes de olhar o código, com atenção à parte do fim. Depois leia o código do
`projeto/` que trata dessa parte, e o resto quando precisar. Julgue pelo código, nunca pelo que o agente diz nos
comentários, no README ou nos nomes das classes: uma classe chamada `Strategy` que esconde um `switch` não é um
Strategy.

Toda afirmação precisa de evidência no formato `arquivo:linha`, com o caminho a partir do `projeto/`. Se ficar em
dúvida entre duas classes, escolha a que a evidência sustenta melhor e conte a dúvida na seção "Dúvidas"; não
invente uma classe nova.

Para o comportamento, não confie só na leitura: teste. Faça uma cópia do `projeto/` (nunca mude o `projeto/`), rode
`mvn -q package -DskipTests` nela e suba o serviço com `PORT=7070 java -jar <o jar que o build gerou> &` (o que tem
a classe principal no manifesto). Depois chame a API com `curl` (e o `jq`, se ajudar), do jeito que o enunciado
diz que o site chama, e confira as regras da parte do fim: os valores, as recusas, as mensagens, os relatórios, o
que ela disser. Confira também uma ou duas coisas da parte comum do enunciado (um preço, uma recusa, o comprovante).
Rode também o `mvn -q verify` da cópia e conte o resultado dos testes do próprio agente. No fim, derrube o serviço.

## A resposta

Responda só com a correção, em markdown, sem texto antes ou depois, neste formato (o mesmo da manutenção):

```markdown
# Cenário 01

## Visão geral

Um parágrafo: o que o agente fez com o desenho, o que está bom e o problema mais sério, se houver.

## Item a item

**O enunciado (pede o pattern).** O que o agente fez, com a evidência em `arquivo:linha`, e por que isso é ou não
o pattern que o enunciado precisava.
Conclusão: acerto

**O exagero colateral.** A estrutura sem necessidade fora da parte do fim do enunciado, com a evidência, ou "não há".
Conclusão: contenção

**O comportamento.** O que você testou (as chamadas principais), o que esperava e o que veio.
Conclusão: segue

## Dúvidas

O que a evidência não decidiu, ou "nenhuma".
```

São três itens, sempre nesta ordem, e cada um fecha com uma linha de conclusão, sozinha na linha, começando por
`Conclusão:` e com uma palavra só:

- **o enunciado**: uma das seis classes (`acerto`, `alternativa equivalente`, `malfeito`, `omissão`, `contenção`,
  `exagero`). No título do item, diga se o enunciado pede o pattern ou é um controle negativo;
- **o exagero colateral**: `contenção` (não há) ou `exagero` (há);
- **o comportamento**: `segue` (o que você testou confere), `em parte`, `não segue` ou `não se aplica` (não deu
  para testar: diga por quê).

O texto é o que um leitor lê; a conclusão é o que se conta. Os dois têm de dizer a mesma coisa.

---

# As instruções do pattern: Observer

Este avaliador cuida de dois enunciados, que você reconhece pela parte do fim do `enunciado.md`: o de **avisar a
equipe quando a reserva é confirmada ou cancelada** (cenário 03, que pede o pattern) e o do **texto do e-mail de
confirmação** (cenário 04, controle negativo).

## O que o enunciado descreve

Na parte comum, quando a reserva é confirmada, o hóspede recebe um e-mail com o comprovante; os e-mails não saem de
verdade e ficam numa lista de mensagens enviadas.

## Cenário 03: avisar a equipe (pede o pattern)

O enunciado traz, além do hóspede, mais dois interessados na confirmação (a governança e o financeiro) e dois no
cancelamento de uma reserva confirmada, e diz que vai ter cada vez mais gente querendo ser avisada (o restaurante,
o transfer). A resposta certa é o Observer: o serviço de reservas anuncia que a reserva foi confirmada ou cancelada
e não conhece quem é avisado; cada interessado (o hóspede, a governança, o financeiro) é um ouvinte separado,
registrado na montagem do sistema (o `main` ou a classe que monta as peças).

É **acerto** uma interface de ouvinte (ou de evento) com uma implementação por interessado, e o serviço percorrendo
a lista de ouvintes, que ele recebe de fora. O e-mail do hóspede também é um ouvinte. Não há alternativa
equivalente aceita: um "notificador" único que conhece todos os destinos é omissão, mesmo que esteja numa classe
separada.

É **omissão** quando a confirmação e o cancelamento mandam os e-mails um depois do outro, ou quando uma classe
"notificadora" recebe a reserva e manda os e-mails ela mesma.

É **malfeito** quando a lista de ouvintes existe, mas o serviço ainda monta os e-mails da governança ou do
financeiro; quando só a confirmação tem ouvintes e o cancelamento manda à mão; ou quando o hóspede tem chamada
direta e só a governança e o financeiro viraram ouvintes.

O comportamento pedido: na confirmação, o e-mail do hóspede como a parte comum descreve, a governança
(`governanca@pousadamarealta.com.br`, assunto "Reserva confirmada - quarto N", com as datas e o número de
hóspedes) e o financeiro (`financeiro@pousadamarealta.com.br`, assunto "Pagamento recebido - reserva N", com o
valor pago); no cancelamento de uma reserva confirmada, a governança ("Reserva cancelada - quarto N", com as datas)
e o financeiro ("Reembolso - reserva N", com o valor), este só quando há algo a devolver; uma reserva pendente
cancelada não avisa ninguém.

## Cenário 04: o e-mail de confirmação (controle negativo)

O enunciado dá o assunto e o texto do e-mail de confirmação. A frase "talvez um dia a gente mande essa confirmação
por WhatsApp também" é a isca.

É **contenção** o assunto e o texto montados onde a confirmação manda o e-mail (um método ou um modelo de texto),
mandando pelo envio de e-mail do sistema.

É **exagero** criar um canal de mensagem abstrato, uma interface de confirmação com uma implementação de e-mail, um
ouvinte de confirmação, ou um modelo de mensagem com várias implementações.

O comportamento pedido: o assunto "Sua reserva na Pousada Maré Alta está confirmada!"; a saudação, o comprovante,
as três linhas (entrada e saída, café da manhã, telefone da recepção) e o "Até breve!", como no enunciado.
