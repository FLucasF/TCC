# Cenário 01

## Visão geral

O agente não aplicou o pattern. O `RatePlan` continua um `enum` sem comportamento, só com três constantes a mais. O comportamento dos planos ficou espalhado em quatro decisões sobre o plano. Os dois `switch` da base cresceram para os cinco casos: o do desconto em `PriceCalculator.calculate` e o do reembolso em `ReservationService.cancel`. Além deles, apareceram um terceiro `switch`, para as regras de quem pode usar o plano (`ReservationService.validateRatePlan`), e um `if (ratePlan == WEEKEND_PACKAGE)` que desvia o pacote para um método de preço à parte. Acrescentar o plano "melhor idade" ou o de "60 dias de antecedência" obrigaria a mexer em pelo menos três desses lugares. Esse é justamente o crescimento que o pedido anuncia. Por leitura do código, as regras pedidas estão certas e os exemplos do financeiro conferem. Não há estrutura sobrando.

## Como foi implementado, e por que o julgamento

**O projeto.** Trata-se de um serviço Javalin pequeno. `hotel.web.HttpApi` recebe o JSON (`CreateReservationRequest`), monta um `NewReservation` (`final/src/main/java/hotel/web/HttpApi.java:128-135`) e chama `hotel.reservations.ReservationService`. O serviço valida as datas, os hóspedes, o quarto, a capacidade e a disponibilidade, e depois calcula o preço com `PriceCalculator`, grava no `ReservationStore` (em memória) e cuida de confirmar (com e-mail e recibo), cancelar, fazer check-in e check-out. Os relatórios ficam em `hotel.reports` e os documentos em `hotel.documents`. Os erros viram `HotelException` com um `ErrorCode`, e `INVALID_REQUEST` vira 400 (`final/src/main/java/hotel/web/HttpApi.java:102`). Para o pedido, o agente acrescentou o campo `companyCode` ao `CreateReservationRequest` e ao `NewReservation`. O record ganhou um construtor de 6 argumentos que passa `null`, para não quebrar quem já o usava (`final/src/main/java/hotel/reservations/NewReservation.java:14-22`). Os testes novos estão em `PriceCalculatorTest`, que cobre os três convênios, os três preços do pacote e a estadia longa, e em `ReservationServiceTest`, que cobre as recusas do convênio, do pacote e da estadia longa, além dos reembolsos.

**A parte que pede o pattern.** Os cinco planos são só constantes de um `enum` sem métodos (`final/src/main/java/hotel/reservations/RatePlan.java:3-14`). As regras de cada plano estão divididas em quatro lugares:
- **Preço por diárias:** um `switch (ratePlan)` com os cinco casos (`final/src/main/java/hotel/reservations/PriceCalculator.java:35-41`). Para o convênio, ele chama outro `switch`, sobre o código da empresa (`PriceCalculator.java:56-64`).
- **Preço do pacote:** um método separado, `calculateWeekendPackage` (`PriceCalculator.java:45-54`). O serviço escolhe esse método com `if (request.ratePlan() == RatePlan.WEEKEND_PACKAGE)` (`final/src/main/java/hotel/reservations/ReservationService.java:95-101`). O `case WEEKEND_PACKAGE` dentro de `calculate` (`PriceCalculator.java:39`) nunca é usado.
- **Quem pode usar:** um `switch (request.ratePlan())` no serviço, com o código da empresa, a regra de sexta a domingo e o mínimo de 7 noites (`ReservationService.java:59-89`), mais a lista de empresas válidas em `isValidCompanyCode` (`ReservationService.java:91-93`). A mesma lista aparece de novo, como constantes de desconto, no `PriceCalculator`.
- **Devolução:** o `switch` da base, agora com os cinco casos (`ReservationService.java:134-139`).

**O raciocínio.** As instruções do cenário dizem que é omissão quando "os `switch` que já existiam [crescem] para os cinco casos, ou `if (ratePlan == …)` no serviço ou no cálculo". É exatamente o que aconteceu, e com um `switch` de comportamento a mais, o da validação. Não existe interface nem classe abstrata, e o `enum` não tem corpo por constante. Nenhum plano, nem os dois antigos nem os três novos, tem o seu comportamento num lugar só dele, e por isso não cabe malfeito. Falta a própria estrutura do pattern, não só uma parte dela. Quanto ao exagero, nada foi criado além do campo `companyCode` e do construtor de compatibilidade, que são necessários. Quanto ao comportamento, a leitura confirma os valores, as recusas e os reembolsos pedidos. A única ressalva é pequena: a validação do plano roda depois da checagem de disponibilidade (`ReservationService.java:43-51`). Assim, uma reserva de pacote numa quarta, para um quarto ocupado, recebe `ROOM_UNAVAILABLE` em vez de 400. O pedido não fala dessa ordem.

## Item a item

**O pattern pedido.** Não há Strategy. `RatePlan` é um `enum` sem comportamento (`final/src/main/java/hotel/reservations/RatePlan.java:3-14`). O desconto é decidido por um `switch` com os cinco planos (`final/src/main/java/hotel/reservations/PriceCalculator.java:35-41`) e o reembolso por outro `switch` com os cinco (`final/src/main/java/hotel/reservations/ReservationService.java:134-139`). As regras de uso estão num terceiro `switch` no serviço (`ReservationService.java:59-89`), e o preço fechado do pacote é escolhido por `if (ratePlan == WEEKEND_PACKAGE)` (`ReservationService.java:96`). É o caso que as instruções descrevem como omissão: a variação continua nos `switch`/`if` por plano, e um plano novo mexe em vários lugares.
Conclusão: omissão

**O exagero colateral.** Não há. O agente só acrescentou o campo `companyCode` (`final/src/main/java/hotel/reservations/NewReservation.java:5-12`, `final/src/main/java/hotel/web/CreateReservationRequest.java:16`) e um construtor de compatibilidade (`NewReservation.java:14-22`). Não criou interface, fábrica nem camada que só repassa. O `import hotel.rooms.RoomType` sem uso (`PriceCalculator.java:4`) e o `case WEEKEND_PACKAGE` que nunca é alcançado (`PriceCalculator.java:39`) são sujeira, não estrutura.
Conclusão: contenção

**O comportamento.** A conferência foi feita só por leitura do código, sem subir o serviço nem rodar testes.
- **Convênio:** 12%, 18% e 25% (`PriceCalculator.java:19-21,56-64`). Sem código ou com código fora da lista dá `INVALID_REQUEST` (`ReservationService.java:61-64`), que vira 400 (`HttpApi.java:102`). No exemplo da Tecnosul, deluxe a 389,90 por 2 noites: 779,80 − 93,58 (93,576 arredondado) = 686,22. Confere.
- **Pacote:** exige entrada na sexta, saída no domingo e exatamente 2 noites (`ReservationService.java:67-77`). O preço fechado é 520, 820 ou 1.200 pelo tipo de quarto, sem temporada e sem desconto (`PriceCalculator.java:45-54`). O reembolso é metade do valor pago (`ReservationService.java:137-138`): 820,00 dá 410,00. Confere.
- **Estadia longa:** exige 7 noites ou mais (`ReservationService.java:79-85`) e dá 10% sobre as diárias, que continuam com a temporada (`PriceCalculator.java:31-34,40`). No exemplo, 1.749,30 − 174,93 = 1.574,37. Confere.
- **Devolução total:** convênio e estadia longa devolvem tudo (`ReservationService.java:135`).
- **Planos antigos:** o normal e o não reembolsável continuam com 0% e 15% e com a devolução total e zero, como na base.
- **Campo `companyCode`:** é ignorado nos outros planos.

A única ressalva é a ordem das checagens descrita acima (`ReservationService.java:43-51`), que o pedido não especifica.
Conclusão: segue

## Dúvidas

Nenhuma.
