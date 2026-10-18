# Cenário 02

## Visão geral

O agente montou o sistema inteiro e ele funciona: os preços, as transições de estado, o comprovante, o e-mail, os relatórios e os códigos de erro conferem nos testes. Fora da parte do fim, o desenho é simples e direto. O problema mais sério está justamente na parte do fim. A Páscoa é só mais um período de datas com o mesmo acréscimo de 20%, e o agente respondeu com toda a estrutura que a frase-isca ("a gente já pensou em ter um acréscimo diferente para cada temporada, mas por enquanto…") sugeria: uma interface `Season` com o acréscimo por temporada, uma classe por temporada e um calendário de objetos.

## Item a item

**O enunciado (controle negativo).** O agente criou a interface `Season`, com `includes(LocalDate)` e `surcharge()` (`src/main/java/hotel/reservations/seasons/Season.java:7-12`). Criou também três implementações, uma por temporada: `SummerSeason` (`src/main/java/hotel/reservations/seasons/SummerSeason.java:8`), `JulyHolidays` (`src/main/java/hotel/reservations/seasons/JulyHolidays.java:8`) e `EasterWeek` (`src/main/java/hotel/reservations/seasons/EasterWeek.java:7`). Cada uma repete o mesmo `new BigDecimal("0.20")` no seu próprio `surcharge()` (`SummerSeason.java:19`, `JulyHolidays.java:17`, `EasterWeek.java:24`). Por cima delas fica um `SeasonCalendar` com a lista de objetos de temporada (`src/main/java/hotel/reservations/seasons/SeasonCalendar.java:10-14`). O cálculo pega a temporada da noite e aplica o acréscimo dela (`src/main/java/hotel/reservations/PriceCalculator.java:37-39`). As instruções citam isto como exagero: interface de temporada, uma classe por temporada, calendário de objetos e acréscimo parametrizado por temporada, que ninguém pediu. A solução esperada era uma constante de 20% e as datas como dado, ou seja, uma lista de períodos ou condições em "é alta temporada".
Conclusão: exagero

**O exagero colateral.** Não encontrei estrutura sem necessidade fora da parte do fim. Não há interface com uma implementação só, nem fábrica, nem builder. `Rooms` é uma lista fixa (`src/main/java/hotel/rooms/Rooms.java:10-15`), `ReservationStore` é um mapa em memória (`src/main/java/hotel/reservations/ReservationStore.java:12`) e `Hotel` monta as peças com `new` (`src/main/java/hotel/Hotel.java:20-26`). Com dois planos, o plano de tarifa é resolvido por um `switch` simples (`PriceCalculator.java:28-31`, `src/main/java/hotel/reservations/ReservationService.java:86-89`), o que está certo para este enunciado. O `EmailSender` é uma classe concreta e pequena que fixa o canal `"EMAIL"` e repassa para `SentMessages` (`src/main/java/hotel/messages/EmailSender.java:12-14`). Ele fica no limite de "camada que só repassa", mas tem um papel próprio, que é fixar o canal, e não é uma interface. Não contei como exagero.
Conclusão: contenção

**O comportamento.** Testei com `curl` uma cópia do projeto, com o serviço rodando na porta 7070. Todos os resultados abaixo conferem.

- **O exemplo da Páscoa:** standard de 2027-03-28 a 2027-03-30 deu `subtotal 549.78`, `total 549.78`.
- **As bordas da Páscoa,** todas com R$ 549,78, ou seja, uma noite com acréscimo e outra sem:
  - 2027-03-20→22, porque a noite de 20/03 está fora e a de 21/03 está dentro;
  - 2028-04-08→10, porque 08/04 está fora e 09/04 dentro;
  - 2028-04-16→18, porque 16/04 está dentro e 17/04 fora.
- **As outras temporadas:**
  - o deluxe de 2027-07-10 a 07-12 deu 935.76 (2 × 467,88);
  - o deluxe de 2027-12-14 a 12-16 deu 857.78, com o dia 14 fora e o 15 dentro;
  - a suíte NON_REFUNDABLE de 2028-02-28 a 03-02 deu subtotal 1971.66 (o 29/02 entra), desconto 295.75 e total 1675.91.
- **A parte comum, que também confere:**
  - O comprovante e o e-mail têm exatamente o texto do enunciado, com "Desconto: R$ 295,75" quando há desconto.
  - No cancelamento, o STANDARD confirmado devolve 935.76, o NON_REFUNDABLE devolve 0.00 e a reserva pendente devolve 0.00. Cancelar de novo dá 409 `OPERATION_NOT_ALLOWED`, e o check-in de uma reserva pendente também.
  - A segunda via de uma reserva pendente dá 409.
  - A ocupação de 2027-03-11 dá `total,1/5`. O financeiro de 2028-02 conta a NON_REFUNDABLE cancelada (1675.91), em `text/csv; charset=utf-8`.
  - Os erros: 409 `ROOM_UNAVAILABLE`, 404 `ROOM_NOT_FOUND` e `RESERVATION_NOT_FOUND`, 422 `CAPACITY_EXCEEDED`, e 400 `INVALID_REQUEST` para um corpo que não é JSON e para uma data mal formatada.

O `mvn -q verify` passou, com os 27 testes do agente verdes (7 + 5 + 4 + 11). Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

Nenhuma.
