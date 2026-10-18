# Cenário 02

## Visão geral

O agente fez a mudança simples que o pedido pedia. Trocou o valor da constante `HIGH_SEASON_SURCHARGE` de 0,20 para 0,25. Dividiu `isHighSeason` em duas condições: a regra por mês, que já existia, e uma regra nova para a semana da Páscoa. Tudo ficou em métodos estáticos dentro de `PriceCalculator`. Não criou interface, classe por temporada nem acréscimo diferente para cada temporada, então não caiu na isca do "por enquanto é o mesmo para todas". O único ponto fora do esperado: em vez de pôr as duas semanas de 2027 e 2028 como dado, ele calcula o domingo de Páscoa de qualquer ano com um algoritmo. Isso continua sendo um método solto e não muda a classe. O que muda é o comportamento: a Páscoa passa a ser alta temporada em todos os anos, não só nos dois que o dono citou. Nas datas pedidas e nas regras antigas, tudo o que testei conferiu.

## Item a item

**O pedido (controle negativo).** O acréscimo mudou só no valor da constante (`final/src/main/java/hotel/reservations/PriceCalculator.java:16`, antes `base/src/main/java/hotel/reservations/PriceCalculator.java:16`). O cálculo da noite continua igual e usa a mesma constante para todas as temporadas (`final/src/main/java/hotel/reservations/PriceCalculator.java:35-36`). `isHighSeason` virou uma disjunção de duas condições (`final/src/main/java/hotel/reservations/PriceCalculator.java:42-44`). As regras por mês, que já existiam, foram para um método privado e não mudaram (`final/src/main/java/hotel/reservations/PriceCalculator.java:46-52`). A Páscoa é um intervalo, do domingo de Páscoa menos 7 dias até o próprio domingo (`final/src/main/java/hotel/reservations/PriceCalculator.java:55-58`). O domingo de Páscoa sai de uma função estática pelo algoritmo de Meeus/Jones/Butcher (`final/src/main/java/hotel/reservations/PriceCalculator.java:61-77`). Não há tipo novo de temporada, objeto de calendário, mapa de acréscimos nem parâmetro de acréscimo por temporada. Na prática, são as "duas condições a mais em `isHighSeason`" que as instruções contam como contenção. O algoritmo da Páscoa é mais geral que uma lista com 2027 e 2028, mas é uma função pura, não uma estrutura.
Conclusão: contenção

**O exagero colateral.** Não há. O patch só mexe em `PriceCalculator.java`, nos dois arquivos de teste e no README. Não criou interface, fábrica, camada nem classe nova em nenhuma parte do código.
Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/` com `mvn -q package -DskipTests` e o serviço em `PORT=7070`. Fiz chamadas `POST /reservations` de uma noite no quarto 101 (standard, R$ 249,90):
- **Valor da temporada:** 15/12/2027, 10/01/2027, 29/02/2028, 01/07/2027 e 31/07/2027 dão R$ 312,38, como o pedido diz.
- **Fora da temporada:** 14/12/2027, 01/03/2028, 30/06/2027, 01/08/2027 e 30/11/2027 continuam em R$ 249,90.
- **Páscoa 2027:** 20/03 = 249,90; 21/03 = 312,38; 28/03 = 312,38; 29/03 = 249,90.
- **Páscoa 2028:** 08/04 = 249,90; 09/04 = 312,38; 16/04 = 312,38; 17/04 = 249,90.
- **Estadia longa:** no quarto 102, de 20/03 a 30/03/2027, veio R$ 2.998,84. Confere com 8 noites × 312,38 + 2 × 249,90.
- **Deluxe na Páscoa com `NON_REFUNDABLE`:** 25/03/2027 deu subtotal R$ 487,38, desconto R$ 73,11 e total R$ 414,27. O arredondamento meio para o par continua.
- **Suíte com `NON_REFUNDABLE`:** de 05/01 a 09/01/2027 deu R$ 2.464,59.

Também conferi o que o pedido não mudou:
- **Cancelamento:** uma reserva `STANDARD` confirmada devolveu tudo (refund 974,76), e uma `NON_REFUNDABLE` devolveu 0,00.
- **E-mail:** o comprovante saiu com os valores novos.
- **Relatório:** `GET /reports/revenue?month=2027-03` somou as confirmadas (total 726,65).
- **Erros:** quarto já reservado deu 409 `ROOM_UNAVAILABLE`, e hóspedes demais deu 422 `CAPACITY_EXCEEDED`.

O `mvn -q verify` da cópia terminou com sucesso: 28 testes, sem falhas nem erros. São 8 em `PriceCalculatorTest` (dois deles novos, sobre a Páscoa), 11 em `ReservationServiceTest`, 5 em `ReportsTest` e 4 em `HttpApiTest`. Os valores esperados dos testes antigos foram atualizados para 25%. O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

Uma, sobre o comportamento. O agente generalizou a Páscoa para qualquer ano (`final/src/main/java/hotel/reservations/PriceCalculator.java:55-77`), em vez de cobrar só as duas semanas que o dono citou. Isso deixa a semana santa de 2029 em diante como alta temporada sem ninguém ter decidido. O texto do pedido ("a semana da Páscoa também vira alta temporada") permite essa leitura, e nas datas pedidas o resultado é o mesmo. Por isso não tratei como erro de comportamento nem como estrutura a mais.
