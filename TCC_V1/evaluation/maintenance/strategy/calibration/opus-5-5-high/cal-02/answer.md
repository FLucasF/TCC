# Cenário 01

## Visão geral

O agente implementou os três planos novos e as regras saem certas, mas o desenho ficou como na base, só que maior. O `enum RatePlan` continua sem comportamento. O preço é decidido por um `if (ratePlan == WEEKEND_PACKAGE)` e por um `switch` de cinco casos em `PriceCalculator.calculate`. A devolução é decidida por outro `switch` em `ReservationService.cancel`. As validações de cada plano viraram três `if (request.ratePlan() == …)` em `ReservationService.create`. Não há uma estrutura por plano, e o próximo plano que o comercial inventar vai mexer em pelo menos três lugares. Isso é a omissão do Strategy que o pedido descrevia. Todos os valores, recusas e devoluções que testei conferem, e o `mvn verify` passa. O agente, porém, não escreveu nenhum teste para os planos novos.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin montado à mão em `hotel.Hotel` (`final/src/main/java/hotel/Hotel.java:19-28`). Essa classe cria `Rooms`, `ReservationStore` (os dados ficam em memória), `PriceCalculator`, `EmailSender`, `ReservationService`, os relatórios (`OccupancyReport`, `RevenueReport`) e a `HttpApi`. Uma requisição `POST /reservations` chega na `HttpApi`, que transforma o `CreateReservationRequest` em `NewReservation` (`final/src/main/java/hotel/web/HttpApi.java:128-129`). Depois disso, `ReservationService.create` confere o quarto, a capacidade e a disponibilidade, chama o `PriceCalculator` e grava a `Reservation` no `ReservationStore`. O cancelamento passa por `ReservationService.cancel`. A mudança só tocou `NewReservation` e `CreateReservationRequest` (que ganharam o campo `companyCode`), `RatePlan`, `PriceCalculator`, `ReservationService` e `HttpApi`. Nos testes, o agente só acrescentou o argumento `null` às chamadas que já existiam. A contagem de `@Test` é a mesma da base (5 + 6 + 11 + 4).

**A parte que pede o pattern.**
- **O plano:** `RatePlan` continua um `enum` sem comportamento, agora com cinco constantes e só comentários (`final/src/main/java/hotel/reservations/RatePlan.java:3-14`).
- **O preço:** fica em `PriceCalculator.calculate`. Um `if (ratePlan == RatePlan.WEEKEND_PACKAGE)` devolve o preço fechado do pacote, com um `switch` sobre o tipo de quarto (`final/src/main/java/hotel/reservations/PriceCalculator.java:32-39`). Para os outros planos, o `switch (ratePlan)` da base cresceu para cinco casos (`PriceCalculator.java:40-46`). A tabela de empresas e o desconto da estadia longa são constantes da calculadora (`PriceCalculator.java:19-23`).
- **As validações:** são três `if` sobre o plano no meio de `ReservationService.create`:
  - o convênio, que consulta `PriceCalculator.COMPANY_DISCOUNTS` (`final/src/main/java/hotel/reservations/ReservationService.java:51-56`);
  - o pacote de sexta a domingo (`ReservationService.java:57-62`);
  - as 7 noites da estadia longa (`ReservationService.java:63-66`).
- **A devolução:** o `switch` de `cancel` cresceu para os cinco planos (`ReservationService.java:105-109`).

**O raciocínio.** Nada aproxima esse desenho do Strategy. Não existe interface nem classe abstrata, as constantes do `enum` não têm corpo e não há mapa de comportamento. Cada regra de cada plano está espalhada por três pontos, todos decidindo pelo plano concreto: o `if` e o `switch` de preço, os três `if` de validação e o `switch` de devolução. É exatamente o caso que as instruções dão como omissão: "os `switch` que já existiam crescendo para os cinco casos, ou `if (ratePlan == …)` no serviço ou no cálculo". Como não há estrutura nenhuma, não cabe malfeito (que pressupõe a estrutura convivendo com o jeito antigo).

**Exagero colateral:** não há. O agente não criou nenhuma classe, interface, fábrica ou camada nova.

**Comportamento:** pela API, tudo o que o pedido define confere, e os planos que já existiam continuam iguais.

## Item a item

**O pattern pedido.** O comportamento dos cinco planos é decidido por condicionais sobre o plano, sem nenhuma estrutura por plano:
- **Preço:** `final/src/main/java/hotel/reservations/PriceCalculator.java:32` faz `if (ratePlan == RatePlan.WEEKEND_PACKAGE)`, e `PriceCalculator.java:40-46` é um `switch (ratePlan)` com STANDARD, NON_REFUNDABLE, CORPORATE, LONG_STAY e WEEKEND_PACKAGE.
- **Validação:** `final/src/main/java/hotel/reservations/ReservationService.java:51`, `:57` e `:63` são três `if (request.ratePlan() == …)`.
- **Devolução:** `ReservationService.java:105-109` é um `switch (reservation.ratePlan())` com os cinco casos.
- **O plano em si:** `RatePlan` (`final/src/main/java/hotel/reservations/RatePlan.java:3-14`) segue um `enum` sem métodos.

Os dois planos que já existiam e os três novos estão todos no mesmo `switch`. O próximo plano exige mexer no `enum`, na calculadora, no serviço (validação) e no cancelamento. Não é o Strategy que o texto pedia.
Conclusão: omissão

**O exagero colateral.** Não há. A mudança não criou tipos novos: só acrescentou constantes ao `enum`, campos aos records (`final/src/main/java/hotel/reservations/NewReservation.java:6-7`, `final/src/main/java/hotel/web/CreateReservationRequest.java:15-16`) e ramos às condicionais que já existiam.
Conclusão: contenção

**O comportamento.** Usei uma cópia da `final/`. O `mvn -q package -DskipTests` gerou `target/hotel.jar`. Subi o serviço na 7070, fiz as chamadas com `curl` e derrubei o serviço no fim.

| Plano | Chamada | Esperado | Veio |
|---|---|---|---|
| Convênio Tecnosul | deluxe 201, 10/03 a 12/03/2027 | 779,80 / 93,58 / 686,22 | 779,80 / 93,58 / 686,22 ✓ |
| Convênio Agrovale | deluxe 202, mesmas datas | desconto 140,36, total 639,44 | 140,36 / 639,44 ✓ |
| Convênio Farmavida | standard 101, mesmas datas | desconto 124,95, total 374,85 | 124,95 / 374,85 ✓ |
| Convênio | sem código, com `ACME` e com `tecnosul` minúsculo | recusa | 400 `INVALID_REQUEST`, "the CORPORATE plan needs the code of a company with an agreement" ✓ |
| Pacote | deluxe, sexta 12/03 a domingo 14/03 | 820,00, desconto 0,00 | 820,00, desconto 0,00 ✓ |
| Pacote | standard em julho (alta temporada) | 520,00 | 520,00 ✓ |
| Pacote | suíte | 1.200,00 | 1.200,00 ✓ |
| Pacote | de quinta a sábado; de sexta a segunda (3 noites) | recusa | 400 `INVALID_REQUEST` nos dois ✓ |
| Estadia longa | standard, 01/03 a 08/03 | 1.749,30 / 174,93 / 1.574,37 | 1.749,30 / 174,93 / 1.574,37 ✓ |
| Estadia longa | 7 noites em julho | subtotal 2.099,16 (temporada valendo), desconto 209,92 | 2.099,16 / 209,92 ✓ |
| Estadia longa | 6 noites | recusa | 400 `INVALID_REQUEST` ✓ |

Cancelamentos depois de confirmar (pagar):

| Plano | Pago | Devolvido |
|---|---|---|
| Convênio | 686,22 | 686,22 ✓ |
| Pacote | 820,00 | 410,00 ✓ |
| Estadia longa | 1.574,37 | 1.574,37 ✓ |
| Standard | 1.159,80 | 1.159,80 ✓ |
| Não reembolsável | 2.365,99 | 0,00 ✓ |

O que o pedido não mudou também se comporta como antes:
- Cancelar um pacote ainda pendente devolve 0,00.
- O não reembolsável numa suíte em janeiro dá 2.783,52 / 417,53, o mesmo que o teste da base.
- O STANDARD com `companyCode` enviado ignora o código.
- Um quarto ocupado continua dando 409 `ROOM_UNAVAILABLE`.
- O relatório de receita de março lista só o que ficou com a pousada (410,00 do pacote cancelado), com o mesmo código de relatório da base.
- O comprovante (`/document`) e as mensagens funcionam.

O `mvn -q verify` passou: 26 testes, 0 falhas. Todos são testes da base. O agente não acrescentou nenhum.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Duas observações menores, que não mudam nada:
- O convênio só aceita o código em maiúsculas: `tecnosul` é recusado. Isso é coerente com "o código é o nome dela em maiúsculas".
- As validações dos planos rodam depois da checagem de disponibilidade (`final/src/main/java/hotel/reservations/ReservationService.java:44-66`). Por isso, um pedido inválido para um quarto ocupado recebe 409, e não 400.
