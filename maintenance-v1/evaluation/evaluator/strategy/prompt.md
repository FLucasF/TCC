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
