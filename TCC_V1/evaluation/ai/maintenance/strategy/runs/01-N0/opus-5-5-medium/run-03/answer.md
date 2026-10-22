# Cenário 01

## Visão geral

O agente pôs os três planos novos dentro dos condicionais que a base já tinha e não aplicou o Strategy. O `enum RatePlan` passou de dois para cinco valores e continua sem comportamento. O preço de cada plano é decidido por um `if (ratePlan == WEEKEND_PACKAGE)` seguido de um `switch` em `PriceCalculator.calculate`. A devolução continua num `switch` em `ReservationService.cancel`, agora com cinco casos. As regras de quem pode usar cada plano ficaram num terceiro `switch`, num método novo do serviço. Pela leitura do código, as regras pedidas estão certas e as contas do financeiro conferem. O problema sério é o desenho: um plano novo, como o da melhor idade, obriga a mexer em três lugares e em duas classes.

## Como foi implementado, e por que o julgamento

**O projeto.** O sistema é um serviço Javalin. Os pacotes são estes:
- `hotel.web`: `HttpApi` monta as rotas e traduz os JSON (`CreateReservationRequest`, `ReservationResponse`, `ErrorResponse`).
- `hotel.reservations`: `ReservationService` controla a vida da reserva; `PriceCalculator` calcula o preço; `Reservation`, `NewReservation`, `RatePlan`, `Price`, `HotelException`/`ErrorCode` são os tipos de apoio; `ReservationStore` guarda as reservas em memória.
- `hotel.rooms`: os quartos, fixos em `Rooms`.
- `hotel.documents`, `hotel.messages` e `hotel.reports`: recibo, e-mail e relatórios.

Uma criação de reserva segue este caminho: a rota `POST /reservations` em `HttpApi` monta um `NewReservation`, que agora traz o `companyCode` (`src/main/java/hotel/web/HttpApi.java:128-135`). O serviço `ReservationService.create` valida o pedido e chama `PriceCalculator.calculate`, e o resultado vai para o `ReservationStore`. O `ErrorCode.INVALID_REQUEST` vira a resposta 400 (`src/main/java/hotel/web/HttpApi.java:102`).

O agente fez estas mudanças:
- acrescentou `companyCode` a `NewReservation`, `CreateReservationRequest` e `Reservation`;
- acrescentou três constantes ao `enum`;
- mexeu em `PriceCalculator` e `ReservationService`;
- escreveu testes em `PriceCalculatorTest`, com os três exemplos do financeiro (`src/test/java/hotel/reservations/PriceCalculatorTest.java:177-197`), em `ReservationServiceTest`, com as validações e as devoluções (`src/test/java/hotel/reservations/ReservationServiceTest.java:155-239`), e em `HttpApiTest`, com os 400 (`src/test/java/hotel/web/HttpApiTest.java:121-166`).

**A parte que pede o pattern.** `RatePlan` é um `enum` só com constantes e comentários (`src/main/java/hotel/reservations/RatePlan.java:3-14`). As regras de cada plano estão espalhadas em três lugares:
- **Preço.** Fica em `PriceCalculator.calculate`. Um `if (ratePlan == RatePlan.WEEKEND_PACKAGE)` troca o subtotal pelo preço fechado (`src/main/java/hotel/reservations/PriceCalculator.java:59-63`). Um `switch (ratePlan)` dá o desconto do normal, do não reembolsável, do convênio e da estadia longa (`src/main/java/hotel/reservations/PriceCalculator.java:65-74`). As tabelas do convênio e do pacote também estão nessa classe (`:37-45`).
- **Quem pode usar.** Fica num `switch` em `ReservationService.validateRatePlanRules`: código da empresa, sexta a domingo, 7 noites (`src/main/java/hotel/reservations/ReservationService.java:160-186`). Esse `switch` ainda lê `PriceCalculator.CORPORATE_DISCOUNTS` de fora.
- **Devolução.** Fica no `switch` de `cancel` (`src/main/java/hotel/reservations/ReservationService.java:219-224`).

**Por que o julgamento.** As instruções classificam como omissão justamente os `switch` da base crescendo para os cinco casos e os `if (ratePlan == …)` no cálculo. É o que está aqui, e ainda com um terceiro `switch` de comportamento, o de validação. Não existe interface, nem classe por plano, nem `enum` com corpo por constante; não há nada que aproxime o desenho do Strategy. Não há exagero colateral: só se acrescentaram campos e constantes. Quanto ao comportamento, a conferência por leitura bate com o que foi pedido (detalhes abaixo).

## Item a item

**O pattern pedido.** Não há estrutura por plano. O `enum` não tem comportamento (`src/main/java/hotel/reservations/RatePlan.java:3-14`). O preço é decidido por `if (ratePlan == RatePlan.WEEKEND_PACKAGE)` e por um `switch (ratePlan)` em `src/main/java/hotel/reservations/PriceCalculator.java:59-74`. A validação é um `switch` em `src/main/java/hotel/reservations/ReservationService.java:161-185`. A devolução é um `switch` em `src/main/java/hotel/reservations/ReservationService.java:219-224`. São os `switch` da base ampliados para cinco casos, mais um novo. Cada plano futuro mexe nesses três lugares, que é o que o Strategy evitaria.
Conclusão: omissão

**O exagero colateral.** Não há. O agente só acrescentou o campo `companyCode` (`src/main/java/hotel/reservations/NewReservation.java:12`, `src/main/java/hotel/web/CreateReservationRequest.java:16`, `src/main/java/hotel/reservations/Reservation.java:15`) e mapas de constantes (`src/main/java/hotel/reservations/PriceCalculator.java:37-45`). Não criou interface, fábrica nem camada nova.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código, sem subir o serviço nem rodar testes.
- **Convênio.** Os descontos são TECNOSUL 0,12, AGROVALE 0,18 e FARMAVIDA 0,25 (`src/main/java/hotel/reservations/PriceCalculator.java:37-40`). Sem código ou com código fora da lista, a reserva leva `INVALID_REQUEST`, que vira 400 (`src/main/java/hotel/reservations/ReservationService.java:162-167`, `src/main/java/hotel/web/HttpApi.java:102`). O exemplo do deluxe de 10/03 a 12/03 confere: 2 × 389,90 = 779,80; 12% = 93,576, arredondado para 93,58; total 686,22.
- **Pacote de fim de semana.** A reserva só passa com entrada na sexta e saída no domingo logo depois (`src/main/java/hotel/reservations/ReservationService.java:170-172`). O preço fechado é 520 / 820 / 1.200, sem desconto e sem temporada (`src/main/java/hotel/reservations/PriceCalculator.java:42-45,59-63`). Se cancelado depois de pago, devolve metade (`src/main/java/hotel/reservations/ReservationService.java:222-223`). O exemplo confere: 12/03/2027 é sexta, o preço é 820,00 e a devolução é 410,00.
- **Estadia longa.** Exige 7 noites ou mais (`src/main/java/hotel/reservations/ReservationService.java:179-183`). Dá 10% sobre as diárias, que já incluem a temporada (`src/main/java/hotel/reservations/PriceCalculator.java:51-54,72`). O exemplo confere: 7 × 249,90 = 1.749,30; desconto 174,93; total 1.574,37.
- **Devoluções.** Convênio e estadia longa devolvem tudo (`src/main/java/hotel/reservations/ReservationService.java:220`).
- **Planos que já existiam.** O normal continua sem desconto e devolve tudo; o não reembolsável continua com 15% e não devolve nada (`src/main/java/hotel/reservations/PriceCalculator.java:66-67`, `src/main/java/hotel/reservations/ReservationService.java:220-221`), como na base.
- **`companyCode` em outros planos.** Fica guardado, mas é ignorado.

Conclusão: segue

## Dúvidas

Nenhuma.
