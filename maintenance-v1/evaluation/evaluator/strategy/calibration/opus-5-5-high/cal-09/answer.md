# Cenário 02

## Visão geral

O pedido era um controle negativo: trocar o acréscimo da temporada para 25% e acrescentar a semana da Páscoa de 2027 e de 2028. O agente não fez a mudança simples. Criou um pacote `seasons` com a interface `Season`, uma classe por temporada (`SummerSeason`, `JulyHolidays`, `EasterWeek`) e um `SeasonCalendar` com a lista de objetos. Com isso, o acréscimo passou a ser de cada temporada, que é justamente o "talvez um dia" que o pedido deixou para depois. O valor `0.25` agora está repetido em três classes, e não há mais uma constante única. O comportamento está certo: os preços, as bordas da Páscoa, o resto da temporada e os planos antigos conferem pela API, e os 27 testes passam. O problema mais sério é o desenho, que é um exagero claro.

## Item a item

**O pedido (controle negativo).** Na base, a mudança cabia em duas linhas: trocar `HIGH_SEASON_SURCHARGE` (`base/src/main/java/hotel/reservations/PriceCalculator.java:16`) e pôr as datas da Páscoa em `isHighSeason` (`base/src/main/java/hotel/reservations/PriceCalculator.java:42`). O agente fez outra coisa:
- removeu a constante e criou a interface `Season`, com `includes` e um `surcharge()` por temporada (`final/src/main/java/hotel/reservations/seasons/Season.java:7-11`);
- criou uma classe por temporada: `SummerSeason.java:8`, `JulyHolidays.java:8` e `EasterWeek.java:7`, todas em `final/src/main/java/hotel/reservations/seasons/`;
- criou um calendário de objetos, que se apresenta como "a new season is one more entry here" (`final/src/main/java/hotel/reservations/seasons/SeasonCalendar.java:8-15`);
- passou a cobrar o acréscimo da temporada encontrada (`final/src/main/java/hotel/reservations/PriceCalculator.java:37-39`).

O mesmo `new BigDecimal("0.25")` aparece três vezes (`SummerSeason.java:19`, `JulyHolidays.java:17`, `EasterWeek.java:23`). Quando o acréscimo mudar de novo, será preciso mexer em três lugares em vez de um. Essa é exatamente a lista do que as instruções chamam de exagero: uma interface de temporada, uma classe por temporada, um calendário com objetos e um acréscimo por temporada que ninguém pediu.
Conclusão: exagero

**O exagero colateral.** Não há. Todas as estruturas novas estão na parte que o pedido tocava, que é o cálculo da temporada, e já foram contadas no item anterior. O resto do `PriceCalculator` não mudou, inclusive o `switch` do plano (`final/src/main/java/hotel/reservations/PriceCalculator.java:28-31`), e os outros pacotes também não.
Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/`. O `mvn -q verify` passou, com 27 testes e nenhuma falha: ReportsTest 5, PriceCalculatorTest 7, ReservationServiceTest 11 e HttpApiTest 4. Depois subi o serviço na porta 7070 e criei as reservas com `POST /reservations`:
- **Temporada a 25%:**
  - uma noite standard em 10/07/2027 custou R$ 312,38, como esperado;
  - 14/12 a 16/12/2027 deu R$ 562,28 (249,90 + 312,38);
  - 29/02 a 02/03/2028 deu R$ 562,28;
  - 30/06/2027 custou R$ 249,90;
  - 31/07 a 02/08/2027 deu R$ 562,28.
- **Páscoa de 2027:** as noites de 20/03 e 29/03 custaram R$ 249,90; as de 21/03 e 28/03, R$ 312,38. Uma estadia de 20/03 a 30/03 deu R$ 2.998,84 (2 × 249,90 + 8 × 312,38), como esperado.
- **Páscoa de 2028:** as noites de 08/04 e 17/04 custaram R$ 249,90; as de 09/04 e 16/04, R$ 312,38.
- **Datas que o pedido não mudou:** maio de 2027 em deluxe deu R$ 779,80, sem acréscimo. O domingo de Ramos de 2029 (25/03) ficou em R$ 249,90, o que é coerente com o pedido, que só citou 2027 e 2028.
- **Planos:** o deluxe em julho com o plano `STANDARD` deu R$ 974,76. A suíte `NON_REFUNDABLE` de 05/01 a 09/01/2027 deu subtotal R$ 2.899,52, desconto R$ 434,93 e total R$ 2.464,59.
- **Cancelamento:** depois de confirmar, a reserva `STANDARD` devolveu tudo (`refund` 974,76) e a `NON_REFUNDABLE` não devolveu nada (`refund` 0,00).
- **Relatório:** `GET /reports/revenue?month=2027-07` deu `total,312.38`.
- **Recusa:** pedir de novo o quarto 101 numa noite ocupada continuou dando 409 `ROOM_UNAVAILABLE`.
- **E-mail:** o comprovante enviado na confirmação continua no mesmo formato.

No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Uma observação à parte: o agente não atualizou o README, que ainda diz que a noite de temporada custa 20% e não cita a Páscoa (`final/README.md:23-24`). Isso não muda o comportamento da API.
