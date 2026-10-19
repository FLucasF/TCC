# A avaliação de uma execução

Você vai avaliar o desenho de uma mudança que um agente de programação fez num sistema pequeno, o de reservas de
uma pousada, escrito em Java com Javalin. O sistema de partida (a base) não tem nenhum design pattern. O agente
recebeu esse sistema e um pedido de mudança escrito pelo dono da pousada, e fez a mudança. Os casos "que já
existiam" são os que a base já tinha.

Na pasta de trabalho você tem quatro coisas. O `pedido.md` é o texto exato que o agente recebeu. A `base/` é o
projeto antes da mudança, e a `final/` é o projeto como o agente o deixou. O `changes.patch` é a diferença entre as
duas, sem os arquivos de configuração do agente.

Você não sabe qual agente ou modelo fez o trabalho, e não deve tentar adivinhar.

## O que se mede

O texto que o agente recebeu pede, sem dizer o nome, um pattern: ele mostra uma variação que já tem vários casos e
que vai continuar crescendo, e a resposta certa é ter essa variação com o pattern, para todos os casos, inclusive os
que já existiam antes da parte nova. As instruções do pattern, mais abaixo, dizem o ponto de partida e o que conta.

Você vai dar a cada execução uma destas classes:

- `acerto`: o pattern foi aplicado direito;
- `alternativa equivalente`: a variação foi resolvida com outra estrutura que as instruções do pattern aceitam de
  antemão;
- `malfeito`: a estrutura do pattern existe, mas convive com o jeito sem ela. Por exemplo, só uma parte dos casos
  foi para a estrutura, ou quem usa a estrutura ainda pergunta qual é o caso concreto, ou um dos lugares que
  deveriam passar por ela continua fazendo à mão;
- `omissão`: a variação ficou sem o pattern (um `if`, um `case` ou uma cópia por caso).

Além da classe, registre duas coisas. A primeira é o **exagero colateral**: estrutura sem necessidade fora da parte
que o pattern resolve (uma interface com uma implementação só que nada substitui, uma fábrica, um builder, uma camada
que só repassa). Isso é registrado à parte da classe. A segunda é o **comportamento**: se as regras pedidas foram
implementadas como foram pedidas (valores, recusas, mensagens), e se o resto do sistema continua funcionando. Um
pattern correto com o comportamento errado continua com a classe do desenho, mas o problema fica anotado.

## Como avaliar

Leia o `pedido.md` inteiro antes de olhar o código. Depois leia o `changes.patch` inteiro e os arquivos da `final/`
que ele toca; abra outros arquivos só quando precisar. Nas evidências, o caminho é a partir da `final/` (ou da
`base/`, quando for sobre o que havia antes).

Julgue pelo código, nunca pelo que o agente diz na resposta final, nos comentários, no README ou nos nomes das
classes: uma classe chamada `Strategy` que esconde um `switch` não é um Strategy.

Toda afirmação precisa de evidência no formato `arquivo:linha`. Se ficar em dúvida entre duas classes, escolha a que
a evidência sustenta melhor e conte a dúvida na seção "Dúvidas"; não invente uma classe nova.

Para o comportamento, não confie só na leitura: teste. Faça uma cópia da `final/` (nunca mude a `final/` nem a `base/`), rode `mvn -q package -DskipTests` nela e suba o
serviço com `PORT=7070 java -jar <o jar que o build gerou> &` (o da base é `target/hotel.jar`). Depois chame a API com
`curl` (e o `jq`, se ajudar), do jeito que o site chamaria, e confira as regras do pedido: os valores, as recusas, as
mensagens, os relatórios, o que o pedido disser. Confira também uma ou duas coisas que o pedido não mudou, para ver se
continuam iguais. O README da `base/` mostra a API. Rode também o `mvn -q verify` da cópia e
conte o resultado dos testes do próprio agente. No fim, derrube o serviço.

## A resposta

Responda só com a correção, em markdown, sem texto antes ou depois, neste formato (no título, o número do cenário que
as instruções do pattern indicam):

```markdown
# Cenário NN

## Visão geral

Um parágrafo: o que o agente fez com o desenho, o que está bom e o problema mais sério, se houver.

## Como foi implementado, e por que o julgamento

A explicação, para quem não vai abrir o código, de como o projeto foi feito e de por que você o julgou como julgou.
Primeiro, o projeto: os pacotes e as classes principais e o que cada um faz; o caminho de uma requisição, da rota até
a regra e os dados; onde ficam os dados e como o sistema é montado e sobe; os testes que o agente escreveu. Depois,
com mais detalhe, a parte que pede o pattern: as classes envolvidas, como cada caso é representado e escolhido, e
onde mora cada regra dele. E, por fim, o raciocínio do julgamento: o que nesse desenho leva à classe que você deu (o
que o aproxima do pattern e o que o afasta dele, comparado com o que as instruções do pattern dizem), o que pesou no
exagero colateral e o que pesou no comportamento. Cite `arquivo:linha` nos pontos principais.

## Item a item

**O pattern pedido.** O que o agente fez, com a evidência em `arquivo:linha`, e por que isso é ou não o pattern que
o texto precisava.
Conclusão: acerto

**O exagero colateral.** A estrutura sem necessidade fora da parte que o pattern resolve, com a evidência, ou "não há".
Conclusão: contenção

**O comportamento.** O que você testou (as chamadas principais), o que esperava e o que veio.
Conclusão: segue

## Dúvidas

O que a evidência não decidiu, ou "nenhuma".
```

São três itens, sempre nesta ordem, e cada um fecha com uma linha de conclusão, sozinha na linha, começando por
`Conclusão:` e com uma palavra só:

- **o pattern pedido**: uma das quatro classes (`acerto`, `alternativa equivalente`, `malfeito`, `omissão`);
- **o exagero colateral**: `contenção` (não há) ou `exagero` (há);
- **o comportamento**: `segue` (o que você testou confere), `em parte`, `não segue` ou `não se aplica` (não deu
  para testar: diga por quê). Se não há nada para testar porque o agente não entregou nada (a pasta vazia, ou
  nenhuma linha mudada), diga isso com todas as letras no texto.

O texto é o que um leitor lê; a conclusão é o que se conta. Os dois têm de dizer a mesma coisa.

---

# As instruções do pattern: Observer (cenário 03)

Este avaliador cuida de um pedido, que você reconhece pelo texto do `pedido.md`: o de **avisar a equipe quando a
reserva é confirmada ou cancelada** (cenário 03).

## O ponto de partida: o que havia na base

Ao confirmar uma reserva, `ReservationService.confirm` monta o comprovante e chama o `EmailSender` direto, com o texto
do e-mail escrito em `confirmationEmailBody`. Havia um único interessado na confirmação, o hóspede, e nenhum no
cancelamento. A montagem do sistema é a classe `Hotel`. O e-mail do hóspede deve continuar igual ao da base.

## O que o cenário pede

O texto traz, além do hóspede, mais dois interessados na confirmação (a governança e o financeiro) e dois no
cancelamento de uma reserva confirmada, e diz que vai ter cada vez mais gente querendo ser avisada (o restaurante, o
transfer). A resposta certa é o Observer: o serviço de reservas anuncia que a reserva foi confirmada ou cancelada e
não conhece quem é avisado; cada interessado (o hóspede, a governança, o financeiro) é um ouvinte separado,
registrado na montagem do sistema (o `main` ou a classe que monta as peças).

É **acerto** uma interface de ouvinte (ou de evento) com uma implementação por interessado, e o serviço percorrendo a
lista de ouvintes, que ele recebe de fora. O e-mail do hóspede também é um ouvinte. Não há alternativa equivalente
aceita: um "notificador" único que conhece todos os destinos é omissão, mesmo que esteja numa classe separada.

É **omissão** quando a confirmação e o cancelamento mandam os e-mails um depois do outro, ou quando uma classe
"notificadora" recebe a reserva e manda os e-mails ela mesma.

É **malfeito** quando a lista de ouvintes existe, mas o serviço ainda monta os e-mails da governança ou do
financeiro; quando só a confirmação tem ouvintes e o cancelamento manda à mão; ou quando o hóspede tem chamada direta
e só a governança e o financeiro viraram ouvintes.

O comportamento pedido: na confirmação, o e-mail do hóspede como o ponto de partida diz, a governança
(`governanca@pousadamarealta.com.br`, assunto "Reserva confirmada - quarto N", com as datas e o número de hóspedes) e
o financeiro (`financeiro@pousadamarealta.com.br`, assunto "Pagamento recebido - reserva N", com o valor pago); no
cancelamento de uma reserva confirmada, a governança ("Reserva cancelada - quarto N", com as datas) e o financeiro
("Reembolso - reserva N", com o valor), este só quando há algo a devolver; uma reserva pendente cancelada não avisa
ninguém.
