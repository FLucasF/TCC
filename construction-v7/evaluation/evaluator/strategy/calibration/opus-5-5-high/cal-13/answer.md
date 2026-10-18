# Cenário 01

## Visão geral

O agente montou um sistema limpo e enxuto, e tudo o que testei se comportou como o enunciado pede. O problema está no desenho dos planos de tarifa. `RatePlan` é um `enum` só com nomes, sem nenhum comportamento. Tudo o que é próprio de cada plano ficou em código que pergunta qual é o plano, espalhado em três lugares:
- **Preço:** um `if` para o pacote e um `switch` de desconto no `PriceCalculator`.
- **Validação:** três `if`s no `ReservationService.create`.
- **Devolução:** um `switch` no `ReservationService.cancel`.

O próximo plano que o comercial inventar (melhor idade, 60 dias de antecedência) vai exigir mudança em todos esses lugares. Isso é a omissão do Strategy que o enunciado pedia. Fora dessa parte não há estrutura sobrando.

## Item a item

**O enunciado (pede o pattern).** Os cinco planos existem só como constantes, sem corpo, em `src/main/java/hotel/reservations/RatePlan.java:3-14`. O comportamento de cada um é decidido fora do plano, por condições sobre o próprio plano:
- **Preço:**
  - `if (ratePlan == RatePlan.WEEKEND_PACKAGE)`, com um `switch` sobre o tipo de quarto para o preço fechado (`src/main/java/hotel/reservations/PriceCalculator.java:32-39`).
  - `switch (ratePlan)` que calcula o desconto de STANDARD, NON_REFUNDABLE, CORPORATE, LONG_STAY e WEEKEND_PACKAGE (`src/main/java/hotel/reservations/PriceCalculator.java:40-46`).
  - As taxas de cada plano e a tabela das empresas ficam como constantes soltas na calculadora (`src/main/java/hotel/reservations/PriceCalculator.java:18-23`).
- **Validação:**
  - CORPORATE: `if (request.ratePlan() == RatePlan.CORPORATE ...)` (`src/main/java/hotel/reservations/ReservationService.java:51-56`).
  - WEEKEND_PACKAGE: `if (... == RatePlan.WEEKEND_PACKAGE ...)` (`src/main/java/hotel/reservations/ReservationService.java:57-62`).
  - LONG_STAY: `if (... == RatePlan.LONG_STAY ...)` (`src/main/java/hotel/reservations/ReservationService.java:63-66`).
- **Devolução:** `switch (reservation.ratePlan())` com STANDARD/CORPORATE/LONG_STAY, NON_REFUNDABLE e WEEKEND_PACKAGE (`src/main/java/hotel/reservations/ReservationService.java:105-109`).

Não existe interface, nem classe por plano, nem `enum` com um corpo por constante. Nenhum plano, nem os dois antigos nem os três novos, tem o seu comportamento num lugar só dele. Por isso não é `malfeito`, já que não há estrutura nenhuma convivendo com o jeito antigo: é o caso descrito nas instruções, com `switch`/`if` de comportamento no preço, na validação e no cancelamento.
Conclusão: omissão

**O exagero colateral.** Não há. Quase tudo é classe concreta que faz trabalho de verdade: `Rooms` com a lista fixa (`src/main/java/hotel/rooms/Rooms.java:10-15`), `ReservationStore` com um mapa em memória (`src/main/java/hotel/reservations/ReservationStore.java:10-30`), e a montagem feita à mão em `src/main/java/hotel/Hotel.java:19-27`, sem fábrica nem builder próprio. O `EmailSender` é concreto e só fixa o canal `"EMAIL"` (`src/main/java/hotel/messages/EmailSender.java:12-14`). É fino, mas não é uma interface com implementação única nem uma camada que só repassa sem acrescentar nada. Não há interfaces no projeto.
Conclusão: contenção

**O comportamento.** Testei numa cópia em `/tmp/av`. O `mvn -q package -DskipTests` gerou `target/hotel.jar`, que subi com `PORT=7070` e chamei com `curl`. Os resultados:
- **Convênio:**
  - Exemplo do enunciado (Tecnosul, 201, 10/03 a 12/03/2027): 779.80 / 93.58 / 686.22.
  - Agrovale no 202: desconto 140.36 (18%). Farmavida no 301: 1159.80 / 289.95 / 869.85 (25%).
  - Sem `companyCode`, ou com `ACME`: 400 `INVALID_REQUEST`.
- **Pacote de fim de semana:**
  - Deluxe de sexta 12/03 a domingo 14/03: 820.00, desconto 0.00.
  - Standard em julho (alta temporada): 520.00, sem a temporada. Suíte: 1200.00, e o `companyCode` enviado foi ignorado.
  - Entrada no sábado, ou de sexta a segunda: 400 `INVALID_REQUEST`.
- **Estadia longa:**
  - Exemplo do enunciado (101, 01/03 a 08/03): 1749.30 / 174.93 / 1574.37.
  - De 28/06 a 05/07, com 4 noites de temporada: 1949.22 / 194.92 / 1754.30, a temporada aplicada.
  - Com 6 noites: 400 `INVALID_REQUEST`.
- **Cancelamento depois de pago:**
  - CORPORATE devolveu 686.22 (tudo); LONG_STAY devolveu 1574.37 (tudo); STANDARD devolveu 935.76 (tudo).
  - WEEKEND_PACKAGE pagou 820.00 e recebeu de volta 410.00.
  - NON_REFUNDABLE devolveu 0.00.
  - Uma reserva pendente cancelada deu `refund` 0.00.
- **Parte comum:**
  - Standard 102 de 10/07 a 12/07/2027: 599.76. NON_REFUNDABLE em julho: 935.76 − 140.36 = 795.40.
  - Conflito de quarto: 409 `ROOM_UNAVAILABLE`. Capacidade: 422 `CAPACITY_EXCEEDED`. Corpo que não é JSON: 400. Reserva inexistente: 404 `RESERVATION_NOT_FOUND`.
  - Cancelar depois do check-in: 409 `OPERATION_NOT_ALLOWED`. Segunda via de uma reserva pendente: 409.
  - O check-out devolveu o comprovante com todos os campos.
  - O e-mail de confirmação veio com canal, assunto e texto certos, incluindo a linha "Desconto: R$ 93,58".
  - O relatório financeiro de março, em `text/csv; charset=utf-8`, contou o pacote cancelado com 410.00 e fechou com `total,1279.85`. O de ocupação de 10/03 deu `total,1/5`.
- **`mvn verify` na cópia:** 26 testes, 0 falhas, 0 erros, BUILD SUCCESS. Depois derrubei o serviço.

Conclusão: segue

## Dúvidas

Nenhuma.
