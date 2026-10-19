# A avaliação de uma execução

Você vai avaliar o desenho de um sistema pequeno, o de reservas de uma pousada, escrito em Java com Javalin, que um
agente de programação construiu do zero. O agente recebeu uma pasta vazia e um enunciado escrito pelo dono da
pousada: o enunciado descreve o sistema inteiro e, no fim, traz uma parte que precisa estar nele desde o primeiro
dia. É essa parte do fim que pede o pattern, e os casos "que já existiam" são os que a parte comum do enunciado já
traz.

Na pasta de trabalho você tem duas coisas. O `enunciado.md` é o texto exato que o agente recebeu. O `projeto/` é o
projeto como o agente o deixou.

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

Leia o `enunciado.md` inteiro antes de olhar o código, com atenção à parte do fim. Depois leia o código do `projeto/`
que trata dessa parte, e o resto quando precisar. Nas evidências, o caminho é a partir do `projeto/`.

Julgue pelo código, nunca pelo que o agente diz na resposta final, nos comentários, no README ou nos nomes das
classes: uma classe chamada `Strategy` que esconde um `switch` não é um Strategy.

Toda afirmação precisa de evidência no formato `arquivo:linha`. Se ficar em dúvida entre duas classes, escolha a que
a evidência sustenta melhor e conte a dúvida na seção "Dúvidas"; não invente uma classe nova.

Para o comportamento, não confie só na leitura: teste. Faça uma cópia do `projeto/` (nunca mude o `projeto/`), rode `mvn -q package -DskipTests` nela e suba o serviço com
`PORT=7070 java -jar <o jar que o build gerou> &` (o que tem a classe principal no manifesto). Depois chame a API com
`curl` (e o `jq`, se ajudar), do jeito que o enunciado diz que o site chama, e confira as regras da parte do fim: os
valores, as recusas, as mensagens, os relatórios, o que ela disser. Confira também uma ou duas coisas da parte comum do
enunciado (um preço, uma recusa, o comprovante). Rode também o `mvn -q verify` da cópia e
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

# As instruções do pattern: Strategy (cenário 01)

Este avaliador cuida de um enunciado, que você reconhece pela parte do fim do `enunciado.md`: o dos **cinco planos
de tarifa** (cenário 01).

## O ponto de partida: o que a parte comum do enunciado descreve

Dois planos de tarifa, o normal (`STANDARD`) e o não reembolsável (`NON_REFUNDABLE`, 15% de desconto, sem devolução
no cancelamento), e a alta temporada (de 15 de dezembro ao fim de fevereiro e julho inteiro, 20% a mais na noite).

## O que o cenário pede

O texto traz cinco planos de tarifa: os dois que já existiam (o normal e o não reembolsável) e três novos (convênio,
pacote de fim de semana, estadia longa), cada um com o seu jeito de calcular o preço, a sua regra de quem pode usar e
a sua devolução, e diz que o comercial vive inventando plano. A resposta certa é o Strategy: cada um dos cinco planos,
os dois que já existiam inclusive, com o seu comportamento num lugar só dele (preço, validação e devolução), e o
cálculo e o cancelamento pedindo ao plano, sem nomear os planos.

É **acerto** uma interface (ou classe abstrata) com uma implementação por plano, escolhida por um mapa, por um campo do
`enum` ou por um único ponto de seleção. Também é **acerto** o `enum` como estratégia: cada constante com o seu próprio
corpo (métodos abstratos sobrescritos em cada constante, ou um comportamento passado no construtor da constante). Um
único `switch` que só **escolhe** a implementação é aceito; o que não pode é haver `switch` de **comportamento**
espalhado.

É **omissão** quando o comportamento dos planos é decidido por `switch` ou `if` sobre o plano no cálculo do preço, no
cancelamento ou numa validação (por exemplo, os `switch` que já existiam crescendo para os cinco casos, ou
`if (ratePlan == …)` no serviço ou no cálculo). Um `enum` cujos métodos fazem `switch (this)` também é omissão: o
próximo plano continua mexendo em vários lugares.

É **malfeito** quando a estrutura existe, mas parte dos planos ficou fora dela (por exemplo, os dois que já existiam
num `switch`, só os três novos em classes), ou quando uma implementação delega o preço para um cálculo que ainda
decide pelo plano num `switch`, ou quando a validação de cada plano (sexta a domingo, 7 noites, o código da empresa)
ficou em `if` no serviço, fora do plano. Uma regra de plano isolada fora do plano, pequena e que não é de preço nem de
devolução (por exemplo, guardar o código da empresa só quando o plano é o convênio), não muda a classe sozinha:
conte-a nas dúvidas.

O comportamento pedido, para conferir: Tecnosul 12%, Agrovale 18%, Farmavida 25% sobre as diárias; convênio sem
código, ou com código fora da lista, é recusado (400, `INVALID_REQUEST`); o pacote só vai de uma sexta ao domingo logo
depois, com preço fechado de R$ 520,00 (standard), R$ 820,00 (deluxe) e R$ 1.200,00 (suíte), sem temporada, e devolve
metade se cancelado depois de pago; a estadia longa exige 7 noites ou mais e dá 10% sobre as diárias (com a temporada
valendo); convênio e estadia longa devolvem tudo; os exemplos do texto (R$ 686,22, R$ 820,00 e R$ 410,00,
R$ 1.574,37) conferem; o normal e o não reembolsável funcionam como o ponto de partida diz.
