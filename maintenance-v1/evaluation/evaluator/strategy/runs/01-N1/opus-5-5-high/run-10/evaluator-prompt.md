# A avaliação de uma execução

Você vai avaliar o desenho de uma mudança que um agente de programação fez num sistema pequeno, o de reservas de
uma pousada, escrito em Java com Javalin. O sistema de partida não tem nenhum design pattern. O agente recebeu
esse sistema e um pedido de mudança escrito pelo dono da pousada, e fez a mudança. Você não sabe qual agente ou
modelo fez o trabalho, e não deve tentar adivinhar.

Na pasta de trabalho você tem quatro coisas. O `pedido.md` é o texto exato que o agente recebeu. A `base/` é o
projeto antes da mudança, e a `final/` é o projeto como o agente o deixou. O `changes.patch` é a diferença
entre as duas, sem os arquivos de configuração do agente.

## O que se mede

Os pedidos são de dois tipos. Uns pedem, sem dizer o nome, um pattern: o texto mostra uma variação que já existe
e que vai continuar crescendo, e a resposta certa é reorganizar o código com o pattern, inclusive nos casos que
já existiam. Os outros são controles negativos: parecem pedir uma estrutura, mas são uma mudança pontual, e a
resposta certa é a mudança simples. Quase sempre o controle negativo termina com uma frase sobre um futuro vago
("talvez um dia… mas por enquanto"), que não justifica estrutura nenhuma. As instruções do pattern, mais abaixo,
dizem qual pedido é de qual tipo e o que conta em cada caso.

Você vai dar a cada execução uma destas classes:

- `acerto`: o pedido precisava do pattern, e ele foi aplicado direito;
- `alternativa_equivalente`: o pedido precisava do pattern, e a variação foi resolvida com outra estrutura que as
  instruções do pattern aceitam de antemão;
- `malfeito`: a estrutura do pattern existe, mas convive com o jeito antigo. Por exemplo, só os casos novos foram
  para a estrutura nova, ou quem usa a estrutura ainda pergunta qual é o caso concreto, ou um dos lugares que
  deveriam passar por ela continua fazendo à mão;
- `omissao`: o pedido precisava do pattern, e a mudança foi feita sem ele (mais um `if`, mais um `case`, mais
  uma cópia);
- `contencao`: o pedido era um controle negativo, e a mudança foi a simples;
- `exagero`: o pedido era um controle negativo, e foi criada estrutura que ninguém pediu (uma interface, uma
  classe por caso, uma fábrica, um embrulho, um esqueleto genérico).

Além da classe, registre duas coisas. A primeira é o **exagero colateral**: estrutura criada numa parte do
código que o pedido não tocava (uma interface com uma implementação só, uma fábrica, um builder, uma camada
nova). Isso vale em qualquer pedido e é registrado à parte da classe. A segunda é o **comportamento**: se as
regras do pedido parecem implementadas como foram pedidas (valores, recusas, mensagens). Um pattern correto com o
comportamento errado continua com a classe do desenho, mas o problema fica anotado.

## Como avaliar

Leia o `pedido.md` inteiro antes de olhar o código. Depois leia o `changes.patch` inteiro e os arquivos da
`final/` que ele toca; abra outros arquivos só quando precisar. Julgue pelo código, nunca pelo que o agente diz
na resposta final, nos comentários ou nos nomes das classes: uma classe chamada `Strategy` que esconde um
`switch` não é um Strategy.

Toda afirmação precisa de evidência no formato `arquivo:linha`, com o caminho a partir da `final/` (ou da
`base/`, quando for sobre o que havia antes). Se ficar em dúvida entre duas classes, escolha a que a evidência
sustenta melhor e conte a dúvida no campo `duvidas`; não invente uma classe nova.

Para o comportamento, leia o código das regras e compare com o pedido. Se puder rodar comandos, rode
`mvn -q verify` dentro de uma cópia da `final/` e conte o resultado; não mude nenhum arquivo. Use `certo` quando
as regras conferem, `errado` quando você achou uma regra que não confere (com a evidência) e `nao_verificado`
quando não deu para conferir.

## A resposta

Responda só com um objeto JSON, sem texto antes ou depois:

```json
{
  "cenario": "01",
  "tipo": "pede_pattern",
  "classe": "acerto",
  "resumo": "duas ou três frases dizendo o que o agente fez com o desenho",
  "evidencias": [
    {"arquivo": "src/main/java/hotel/reservations/RatePlan.java", "linha": 18, "o_que_mostra": "..."}
  ],
  "exagero_colateral": {"houve": false, "evidencias": []},
  "comportamento": {"situacao": "certo", "observacoes": "..."},
  "duvidas": ""
}
```

O `tipo` é `pede_pattern` ou `controle_negativo`, e a `classe` é uma das seis acima.

---

# As instruções do pattern: Strategy

Este avaliador cuida de dois pedidos, que você reconhece pelo texto do `pedido.md`: o dos **planos de tarifa
novos** (cenário 01, que pede o pattern) e o da **alta temporada que mudou** (cenário 02, controle negativo).

## O que havia na base

O plano de tarifa (`STANDARD` e `NON_REFUNDABLE`) é um `enum` sem comportamento, e o comportamento de cada plano
é decidido por um `switch` em dois lugares: o desconto, em `PriceCalculator.calculate`, e o reembolso, em
`ReservationService.cancel`. Com dois planos que não mudavam, isso estava certo. O acréscimo da alta temporada é
uma constante, `HIGH_SEASON_SURCHARGE`, e as datas da temporada ficam em `PriceCalculator.isHighSeason`.

## Cenário 01: os planos de tarifa novos (pede o pattern)

O pedido traz três planos novos (convênio, pacote de fim de semana, estadia longa), cada um com o seu jeito de
calcular o preço, a sua regra de quem pode usar e a sua devolução, e diz que o comercial vive inventando plano. A
resposta certa é o Strategy: cada um dos cinco planos, os dois antigos inclusive, com o seu comportamento num
lugar só dele (preço, validação e reembolso), e o cálculo e o cancelamento pedindo ao plano, sem nomear os
planos.

É **acerto** uma interface (ou classe abstrata) com uma implementação por plano, escolhida por um mapa, por um
campo do `enum` ou por um único ponto de seleção. Também é **acerto** o `enum` como estratégia: cada constante
de `RatePlan` com o seu próprio corpo (métodos abstratos sobrescritos em cada constante, ou um comportamento
passado no construtor da constante). Um único `switch` que só **escolhe** a implementação é aceito; o que não
pode é haver `switch` de **comportamento** espalhado.

É **omissão** quando os `switch` sobre o plano crescem para os cinco casos (no desconto, no reembolso, numa
validação nova) ou quando entram `if (ratePlan == …)` no serviço ou no cálculo. Um `enum` cujos métodos fazem
`switch (this)` também é omissão: o `switch` só mudou de arquivo, e o próximo plano continua mexendo em vários
lugares.

É **malfeito** quando a estrutura existe, mas os planos antigos continuam no `switch` (só os novos foram para
as classes novas), ou quando uma implementação delega o preço para um cálculo que ainda decide pelo plano num
`switch`, ou quando a validação de cada plano (sexta a domingo, 7 noites, o código da empresa) ficou em `if` no
serviço, fora do plano. Uma regra de plano isolada fora do plano, pequena e que não é de preço nem de reembolso
(por exemplo, guardar o código da empresa só quando o plano é o convênio), não muda a classe sozinha: conte-a nas
`duvidas`.

O comportamento pedido, para conferir: Tecnosul 12%, Agrovale 18%, Farmavida 25% sobre as diárias; convênio sem
código, ou com código fora da lista, é recusado; o pacote só vai de uma sexta ao domingo logo depois, com preço
fechado de R$ 520,00 (standard), R$ 820,00 (deluxe) e R$ 1.200,00 (suíte), sem temporada, e devolve metade se
cancelado depois de pago; a estadia longa exige 7 noites ou mais e dá 10% sobre as diárias (com a temporada
valendo); convênio e estadia longa devolvem tudo; os exemplos do pedido (R$ 686,22, R$ 820,00 e R$ 410,00,
R$ 1.574,37) conferem; os dois planos antigos não mudaram.

## Cenário 02: a alta temporada mudou (controle negativo)

O pedido muda a porcentagem da temporada para 25% e acrescenta a semana da Páscoa de 2027 e de 2028. A frase "já
pensamos em ter um acréscimo diferente para cada temporada, mas por enquanto é o mesmo para todas" é a isca.

É **contenção** trocar a constante e acrescentar as datas da Páscoa como dado: uma lista de períodos, ou duas
condições a mais em `isHighSeason`.

É **exagero** criar uma interface de temporada, uma classe ou estratégia por temporada, um calendário de
temporadas com objetos, ou um acréscimo por temporada parametrizado que ninguém pediu.

O comportamento pedido: a noite de temporada custa 25% a mais (standard R$ 312,38); a Páscoa vai do domingo de
Ramos ao domingo de Páscoa, as duas noites incluídas (2027: 21/03 a 28/03; 2028: 09/04 a 16/04); o resto das
datas da temporada não mudou.
