# As instruções do pattern: Strategy (cenário 01)

## O ponto de partida, na construção (o que a parte comum do enunciado descreve)

Dois planos de tarifa, o normal (`STANDARD`) e o não reembolsável (`NON_REFUNDABLE`, 15% de desconto, sem devolução
no cancelamento), e a alta temporada (de 15 de dezembro ao fim de fevereiro e julho inteiro, 20% a mais na noite).

## O ponto de partida, na manutenção (o que havia na base)

O plano de tarifa (`STANDARD` e `NON_REFUNDABLE`) é um `enum` sem comportamento, e o comportamento de cada plano é
decidido por um `switch` em dois lugares: o desconto, em `PriceCalculator.calculate`, e o reembolso, em
`ReservationService.cancel`. Com dois planos que não mudavam, isso estava certo. O acréscimo da alta temporada é uma
constante, `HIGH_SEASON_SURCHARGE`, e as datas da temporada ficam em `PriceCalculator.isHighSeason`. O normal e o não
reembolsável devem continuar funcionando como na base.

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
