# Cenário 02

## Visão geral

O pedido é um controle negativo, e o agente fez a mudança simples. Trocou a constante do acréscimo de 0,20 para 0,25 e pôs a semana da Páscoa em `isHighSeason` com um `if` a mais. Esse `if` chama dois métodos estáticos privados da própria `PriceCalculator`: um vê se a data cai entre o domingo de Ramos e o domingo de Páscoa, e o outro calcula a data da Páscoa pelo algoritmo de Meeus/Jones/Butcher. Não criou interface, classe por temporada, calendário de objetos nem acréscimo por temporada, e não caiu na isca do "acréscimo diferente para cada temporada". Os preços que testei na API batem com o pedido. Há dois pontos menores. O primeiro: como a Páscoa é calculada, ela vira temporada em todo ano, não só em 2027 e 2028. O segundo: o README ficou desatualizado e ainda diz 20%.

## Item a item

**O pedido (controle negativo).** A constante mudou de valor e continua uma só, para todas as temporadas (`final/src/main/java/hotel/reservations/PriceCalculator.java:16`). O cálculo da noite ficou igual (`final/src/main/java/hotel/reservations/PriceCalculator.java:33-39`). As datas antigas continuam na mesma condição de antes (`final/src/main/java/hotel/reservations/PriceCalculator.java:47-50`), e a Páscoa entra como mais uma condição: `return isEasterWeek(date);` (`final/src/main/java/hotel/reservations/PriceCalculator.java:53`). Os métodos `isEasterWeek` (`final/src/main/java/hotel/reservations/PriceCalculator.java:57-62`) e `calculateEaster` (`final/src/main/java/hotel/reservations/PriceCalculator.java:65-81`) são estáticos, ficam na mesma classe e só calculam datas. Não são um tipo novo nem um ponto de extensão, e não dão acréscimo próprio a nenhuma temporada. Isso é "duas condições a mais em `isHighSeason`", com a data calculada em vez de escrita à mão. As instruções chamam isso de contenção.
Conclusão: contenção

**O exagero colateral.** Não há. O patch só toca `PriceCalculator.java` e dois testes (`PriceCalculatorTest.java` e `HttpApiTest.java`). Não aparece nenhuma interface, fábrica, builder ou camada nova em outra parte do código.
Conclusão: contenção

**O comportamento.** Rodei `mvn -q verify` numa cópia da `final/`: 28 testes, sem falhas (ReportsTest 5, PriceCalculatorTest 8, ReservationServiceTest 11, HttpApiTest 4). Subi o serviço na porta 7070 e criei reservas com `POST /reservations`:

- **Acréscimo de 25%:**
  - Standard numa noite de julho de 2027: 312,38, como o pedido diz.
  - Deluxe numa noite de julho: 487,38.
  - Suíte `NON_REFUNDABLE` de 05/01 a 09/01/2027: subtotal 2.899,52, desconto 434,93, total 2.464,59.
- **Páscoa de 2027:**
  - As noites de 20/03 e de 29/03 saem a 249,90 (fora da temporada).
  - As noites de 21/03 e de 28/03 saem a 312,38, então os dois domingos contam.
  - Uma estadia de 19/03 a 30/03 dá 3.248,74: 2 × 249,90 + 8 × 312,38 + 249,90, que é o esperado.
- **Páscoa de 2028:**
  - As noites de 08/04 e de 17/04 saem a 249,90.
  - As noites de 09/04 e de 16/04 saem a 312,38.
  - Uma estadia de 08/04 a 18/04 dá 2.998,84, que é o esperado.
- **Datas que não mudaram:**
  - Deluxe de 13/12 a 15/12/2027: 779,80, sem temporada.
  - Deluxe de 28/02 a 02/03/2028: 1.364,66, com 29/02 em temporada e 01/03 fora.
  - A noite de 30/06 sai fora da temporada e a de 31/07 dentro (877,28 até 02/08).
  - O desconto `NON_REFUNDABLE` continua 15% (116,97 sobre 779,80).
- **Outras regras que não mudaram:**
  - Cancelar uma `STANDARD` paga devolve tudo (312,38). Cancelar uma `NON_REFUNDABLE` paga devolve 0,00.
  - Quarto 999 dá `ROOM_NOT_FOUND`, e reservar de novo noites já ocupadas dá `ROOM_UNAVAILABLE`.
  - O e-mail de confirmação mostra "Diárias: R$ 312,38".

No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

- **Páscoa em todo ano.** O pedido dá as datas de 2027 e 2028, e o agente calcula a Páscoa para qualquer ano (`final/src/main/java/hotel/reservations/PriceCalculator.java:57-81`). Com isso, a semana da Páscoa de 2026 também sobe: a suíte de 29/03 a 06/04/2026 saiu a 5.799,04, ou seja, 8 noites a 724,88. Ler "a semana da Páscoa também vira alta temporada" como regra geral é sensato, e as duas datas pedidas conferem. Ainda assim, isso vai além dos dois anos citados, e quem lê pode não esperar. Não muda a classe: é um cálculo de datas, não uma estrutura.
- **README desatualizado.** O README não foi atualizado e ainda diz "a noite custa 20% a mais", sem falar da Páscoa (`base/README.md`, sem mudança na `final/`). É documentação, não comportamento.
