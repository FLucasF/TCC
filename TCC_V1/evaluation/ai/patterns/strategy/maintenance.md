Este avaliador cuida de um pedido, que você reconhece pelo texto do `pedido.md`: o dos **planos de tarifa novos**
(cenário 01).

## O ponto de partida: o que havia na base

O plano de tarifa (`STANDARD` e `NON_REFUNDABLE`) é um `enum` sem comportamento, e o comportamento de cada plano é
decidido por um `switch` em dois lugares: o desconto, em `PriceCalculator.calculate`, e o reembolso, em
`ReservationService.cancel`. Com dois planos que não mudavam, isso estava certo. O acréscimo da alta temporada é uma
constante, `HIGH_SEASON_SURCHARGE`, e as datas da temporada ficam em `PriceCalculator.isHighSeason`. O normal e o não
reembolsável devem continuar funcionando como na base.
