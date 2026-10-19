# Cenário 01

## Visão geral

O agente montou um sistema de reservas simples e organizado, e o comportamento está certo em tudo o que testei, inclusive os três exemplos do texto. Mas os cinco planos de tarifa não foram feitos com o pattern. O `RatePlan` é um `enum` só de rótulos, sem nenhum comportamento. O preço de cada plano é decidido por um `if` e um `switch` no `PriceCalculator`. A devolução é decidida por um `switch` no `ReservationService`. As regras de quem pode usar o plano ficam em três `if (request.ratePlan() == …)` no serviço. Os dois planos que já existiam também estão nesses `switch`. Para criar um plano novo, como o da melhor idade, seria preciso mexer em pelo menos três lugares. Esse é o problema central.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica no pacote `hotel`, dividido assim:
- `rooms`: os cinco quartos, fixos no código (`Rooms`, `Room`, `RoomType`).
- `reservations`: o centro do sistema, com `ReservationService`, `PriceCalculator`, `Reservation`, `RatePlan`, `ReservationStore`, `Price` e `NewReservation`, além de `ErrorCode` e `HotelException`.
- `documents`: o comprovante (`ReservationReceipt`, `TextFormat`).
- `messages`: o e-mail simulado (`EmailSender`, `SentMessages`, `Message`).
- `reports`: os dois relatórios em CSV (`OccupancyReport`, `RevenueReport`).
- `web`: a API Javalin (`HttpApi`, `CreateReservationRequest`, `ReservationResponse`, `ErrorResponse`).

A classe `Hotel` (`Hotel.java:19-28`) monta tudo à mão, sem framework, e o `main` lê a variável `PORT`, com 7070 se ela não vier (`Hotel.java:41`).

O caminho de uma reserva é este:
1. A rota `POST /reservations` (`web/HttpApi.java:64`) lê o corpo como `CreateReservationRequest`. Um `ratePlan` desconhecido faz o Jackson falhar, e a resposta é 400.
2. A rota confere os campos obrigatórios e monta um `NewReservation` com o `companyCode` (`HttpApi.java:109-130`).
3. `ReservationService.create` valida as datas, o número de hóspedes, o quarto, a capacidade, a disponibilidade e as regras de cada plano. Depois pede o preço ao `PriceCalculator` e grava a reserva no `ReservationStore`, um `LinkedHashMap` em memória (`ReservationStore.java:12`).

Os erros saem como `HotelException` com um `ErrorCode`, que um único handler traduz para o status HTTP (`HttpApi.java:94-107`).

O agente escreveu 26 testes:
- `PriceCalculatorTest`, com 6: temporada e não reembolsável;
- `ReservationServiceTest`, com 11;
- `ReportsTest`, com 5;
- `HttpApiTest`, com 4.

Nenhum teste cobre os três planos novos.

**A parte que pede o pattern.** O plano é um `enum` sem corpo (`reservations/RatePlan.java:3-14`). As regras de cada plano estão espalhadas em três lugares:
- **Preço**, em `PriceCalculator.calculate`:
  - um `if (ratePlan == RatePlan.WEEKEND_PACKAGE)` com o preço fechado por tipo de quarto (`PriceCalculator.java:32-39`);
  - um `switch (ratePlan)` com os cinco casos de desconto: STANDARD zero, NON_REFUNDABLE 15%, CORPORATE pelo mapa de empresas, LONG_STAY 10% e WEEKEND_PACKAGE zero (`PriceCalculator.java:40-46`).
  - Os percentuais e o mapa das empresas são constantes do calculador (`PriceCalculator.java:18-23`).
- **Quem pode usar**, em três `if` no `ReservationService.create`:
  - convênio com código válido (`ReservationService.java:51-56`), consultando o mapa do calculador;
  - pacote de sexta a domingo (`:57-62`);
  - estadia longa com 7 noites ou mais (`:63-66`).
- **Devolução**, em um `switch (reservation.ratePlan())` no `ReservationService.cancel` (`ReservationService.java:105-109`). STANDARD, CORPORATE e LONG_STAY devolvem tudo, NON_REFUNDABLE devolve zero e WEEKEND_PACKAGE devolve metade.

**O raciocínio.** As instruções do cenário chamam de omissão justamente isto: o comportamento dos planos decidido por `switch` ou `if` sobre o plano no cálculo, no cancelamento e na validação, com os `switch` dos planos antigos crescendo até os cinco casos. Não existe interface, classe abstrata nem constante do `enum` com corpo próprio. Também não há nenhum ponto que escolha uma implementação. Quem calcula e quem cancela nomeia os planos o tempo todo. Nenhuma parte dos planos foi para uma estrutura própria, então não dá para falar em malfeito: a estrutura do pattern simplesmente não existe.

**Exagero colateral:** não encontrei. Cada classe tem uma razão de existir, e não há interface com uma implementação só, fábrica ou camada que só repassa.

**Comportamento:** tudo o que testei confere com o enunciado. Encontrei só uma ordem de validação discutível, contada nas Dúvidas.

## Item a item

**O pattern pedido.** Os cinco planos são constantes de um `enum` sem comportamento (`reservations/RatePlan.java:3-14`). O preço é decidido por `if (ratePlan == RatePlan.WEEKEND_PACKAGE)` e por um `switch (ratePlan)` de cinco casos (`reservations/PriceCalculator.java:32-46`). A elegibilidade fica em três `if (request.ratePlan() == …)` no serviço (`reservations/ReservationService.java:51-66`). A devolução é outro `switch` de cinco casos no cancelamento (`reservations/ReservationService.java:105-109`). Os dois planos que já existiam e os três novos estão todos nesses mesmos condicionais. Nenhum plano tem o próprio preço, a própria validação e a própria devolução num lugar só dele, e um plano novo exige mexer no `enum`, no `PriceCalculator` e em dois pontos do `ReservationService`. Isso é a variação sem o Strategy.
Conclusão: omissão

**O exagero colateral.** Não há. Os pacotes e as classes são poucos e cada um tem uma função: o serviço, o calculador, o armazenamento em memória, os relatórios, o comprovante e o envio de e-mail (`Hotel.java:19-28`). Não há interfaces com uma implementação só, fábricas, builders nem camadas de repasse.
Conclusão: contenção

**O comportamento.** Fiz o build numa cópia do projeto, subi o `target/hotel.jar` na porta 7070 e testei com `curl`:
- **Convênio:**
  - Tecnosul, deluxe de 10/03 a 12/03/2027: subtotal 779.80, desconto 93.58 e total 686.22, como no exemplo.
  - Agrovale, mesmo quarto e datas: desconto 140.36, que é 18%.
  - Farmavida, standard: 499.80 com 124.95 de desconto, que é 25%.
  - Sem código, com `ACME` ou com `tecnosul` em minúsculas: 400 `INVALID_REQUEST`.
  - Pago e cancelado: devolveu 686.22, ou seja, tudo.
- **Pacote:**
  - Deluxe de sexta 12/03 a domingo 14/03/2027: 820.00 sem desconto. Pago e cancelado, devolveu 410.00, como no exemplo.
  - Standard em julho, alta temporada: 520.00, sem acréscimo de temporada.
  - Suíte: 1200.00.
  - Começando no sábado ou indo de sexta a segunda: 400.
  - Cancelado ainda pendente: devolveu 0.00.
- **Estadia longa:**
  - Standard de 01/03 a 08/03/2027: 1749.30, desconto de 174.93 e total de 1574.37, como no exemplo.
  - Em julho: 2099.16 com 209.92 de desconto, com a temporada valendo.
  - Seis noites: 400.
  - Paga e cancelada: devolveu tudo.
- **Parte comum:**
  - STANDARD de 10/07 a 12/07/2027: 599.76.
  - Não reembolsável pago e cancelado: devolveu 0.00 e continuou no financeiro de julho.
  - STANDARD com `companyCode` sobrando: o código foi ignorado.
  - E-mail de confirmação com a linha de desconto, comprovante do check-out e a segunda via: conferem.
  - Financeiro de março: o pacote cancelado entrou com 410.00. Ocupação: `total,1/5`.
  - CSV com `text/csv; charset=utf-8`.
  - Recusas: quarto ocupado deu 409, capacidade excedida deu 422, check-in de reserva cancelada deu 409, plano inexistente deu 400.

O `mvn verify` da cópia terminou com BUILD SUCCESS: 26 testes, nenhuma falha.
Conclusão: segue

## Dúvidas

As regras de plano são conferidas depois da disponibilidade do quarto (`reservations/ReservationService.java:44-66`). Por isso, um pacote em datas inválidas num quarto ocupado recebe 409 `ROOM_UNAVAILABLE`, e não 400: foi o que vi no teste com o quarto 202. O enunciado não fixa qual erro vem primeiro, então não contei isso contra o comportamento. Fora isso, nenhuma: a classe do desenho não deixa dúvida.
