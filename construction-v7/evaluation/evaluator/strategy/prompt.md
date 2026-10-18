# As instruções do pattern: Strategy

Este avaliador cuida de dois enunciados, que você reconhece pela parte do fim do `enunciado.md`: o dos **cinco
planos de tarifa** (cenário 01, que pede o pattern) e o da **semana da Páscoa na alta temporada** (cenário 02,
controle negativo).

## O que o enunciado descreve

A parte comum do enunciado traz dois planos de tarifa, o normal (`STANDARD`) e o não reembolsável
(`NON_REFUNDABLE`, 15% de desconto, sem devolução no cancelamento), e a alta temporada (de 15 de dezembro ao fim de
fevereiro e julho inteiro, 20% a mais na noite).

## Cenário 01: os cinco planos de tarifa (pede o pattern)

O enunciado traz, além dos dois planos da parte comum, mais três (convênio, pacote de fim de semana, estadia
longa), cada um com o seu jeito de calcular o preço, a sua regra de quem pode usar e a sua devolução, e diz que o
comercial vive inventando plano. A resposta certa é o Strategy: cada um dos cinco planos, o normal e o não
reembolsável inclusive, com o seu comportamento num lugar só dele (preço, validação e devolução), e o cálculo e o
cancelamento pedindo ao plano, sem nomear os planos.

É **acerto** uma interface (ou classe abstrata) com uma implementação por plano, escolhida por um mapa, por um
campo do `enum` ou por um único ponto de seleção. Também é **acerto** o `enum` como estratégia: cada constante com
o seu próprio corpo (métodos abstratos sobrescritos em cada constante, ou um comportamento passado no construtor da
constante). Um único `switch` que só **escolhe** a implementação é aceito; o que não pode é haver `switch` de
**comportamento** espalhado.

É **omissão** quando o comportamento dos planos é decidido por `switch` ou `if` sobre o plano no cálculo do preço,
no cancelamento ou numa validação. Um `enum` cujos métodos fazem `switch (this)` também é omissão: o próximo plano
continua mexendo em vários lugares.

É **malfeito** quando a estrutura existe, mas parte dos planos ficou fora dela (por exemplo, o normal e o não
reembolsável num `switch`, só os três novos em classes), ou quando uma implementação delega o preço para um
cálculo que ainda decide pelo plano num `switch`, ou quando a validação de cada plano (sexta a domingo, 7 noites,
o código da empresa) ficou em `if` no serviço, fora do plano. Uma regra de plano isolada fora do plano, pequena e
que não é de preço nem de devolução (por exemplo, guardar o código da empresa só quando o plano é o convênio), não
muda a classe sozinha: conte-a nas dúvidas.

O comportamento pedido, para conferir: Tecnosul 12%, Agrovale 18%, Farmavida 25% sobre as diárias; convênio sem
código, ou com código fora da lista, é recusado (400, `INVALID_REQUEST`); o pacote só vai de uma sexta ao domingo
logo depois, com preço fechado de R$ 520,00 (standard), R$ 820,00 (deluxe) e R$ 1.200,00 (suíte), sem temporada, e
devolve metade se cancelado depois de pago; a estadia longa exige 7 noites ou mais e dá 10% sobre as diárias (com a
temporada valendo); convênio e estadia longa devolvem tudo; os exemplos do enunciado (R$ 686,22, R$ 820,00 e
R$ 410,00, R$ 1.574,37) conferem; o normal e o não reembolsável funcionam como a parte comum diz.

## Cenário 02: a semana da Páscoa (controle negativo)

O enunciado acrescenta a semana da Páscoa de 2027 e de 2028 à alta temporada, com o mesmo acréscimo de 20%. A frase
"a gente já pensou em ter um acréscimo diferente para cada temporada, mas por enquanto é o mesmo para todas" é a
isca.

É **contenção** o acréscimo numa constante e as datas da temporada, a Páscoa inclusive, como dado: uma lista de
períodos, ou condições na conferência de "é alta temporada".

É **exagero** criar uma interface de temporada, uma classe ou estratégia por temporada, um calendário de
temporadas com objetos, ou um acréscimo por temporada parametrizado que ninguém pediu.

O comportamento pedido: a noite de temporada custa 20% a mais (standard R$ 299,88); a Páscoa vai do domingo de
Ramos ao domingo de Páscoa, as duas noites incluídas (2027: 21/03 a 28/03; 2028: 09/04 a 16/04); o exemplo do
enunciado (28/03/2027 a 30/03/2027, R$ 549,78) confere; o resto das datas da temporada é o da parte comum.
