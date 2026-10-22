# Cenário 01

## Visão geral

O agente aplicou o Strategy aos cinco planos. Criou a interface `RatePlanStrategy`, com três operações: validar, calcular o preço e calcular a devolução. Cada plano, inclusive o normal e o não reembolsável que já existiam, virou uma classe própria. Um mapa único escolhe a classe certa, e o serviço só pede ao plano, sem dizer qual é. Os dois `switch` de comportamento da base sumiram. As regras pedidas conferem na leitura do código. O ponto mais fraco não é do pattern, é uma sobra: o `PriceCalculator` ficou oco. Ele só repassa para a estratégia, monta um hóspede falso para isso e continua sendo injetado no `ReservationService`, que não o usa mais. Há também o laço das diárias, copiado em quatro estratégias.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin. `hotel.Hotel` monta as peças: `Rooms`, `ReservationStore` (em memória), `PriceCalculator`, `EmailSender` e `ReservationService` (`final/src/main/java/hotel/Hotel.java:24`). O `HttpApi` lê o corpo do `POST /reservations` num `CreateReservationRequest`, que agora tem o campo `companyCode` (`final/src/main/java/hotel/web/CreateReservationRequest.java:16`). Esse corpo vira um `NewReservation`, que também ganhou `companyCode` (`final/src/main/java/hotel/reservations/NewReservation.java:12`), e o `HttpApi` o passa para `ReservationService.create`. O `HotelException` com `INVALID_REQUEST` vira 400 (`final/src/main/java/hotel/web/HttpApi.java:102`). Os documentos (`ReservationReceipt`) e os relatórios não mudaram. A ajuda de preço que antes ficava no `PriceCalculator` (diária com alta temporada, `isHighSeason`, arredondamento em centavos) foi para `PriceHelper` sem mudar de lógica (`final/src/main/java/hotel/reservations/PriceHelper.java:18-38`).

**Os testes do agente.** Ele escreveu `NewRatePlansTest`, que confere os três exemplos do financeiro e as recusas de cada plano, e um teste HTTP em `HttpApiTest.supportsNewRatePlans`. Também ajustou os testes antigos para usar `PriceHelper`.

**A parte do pattern.**
- A interface `RatePlanStrategy` tem `validate`, `calculatePrice` e `calculateRefund` (`final/src/main/java/hotel/reservations/RatePlanStrategy.java:10-20`).
- Há uma classe por plano: `StandardRatePlan`, `NonRefundableRatePlan`, `CorporateRatePlan`, `WeekendPackageRatePlan` e `LongStayRatePlan`.
- O único ponto de seleção é um `Map<RatePlan, RatePlanStrategy>` em `RatePlanStrategies` (`final/src/main/java/hotel/reservations/RatePlanStrategies.java:8-13`). O `enum RatePlan` ficou só como identificador (`final/src/main/java/hotel/reservations/RatePlan.java:3-14`).
- O `ReservationService.create` pede a estratégia, valida e calcula o preço com ela (`final/src/main/java/hotel/reservations/ReservationService.java:42-43,51`).
- O `cancel` pede a devolução à estratégia (`final/src/main/java/hotel/reservations/ReservationService.java:88-89`). O `switch` que a base tinha nesse lugar (`base/src/main/java/hotel/reservations/ReservationService.java:86-89`) foi removido.

Onde mora cada regra:
- **Lista de empresas e recusa do convênio:** em `CorporateRatePlan` (`final/src/main/java/hotel/reservations/CorporateRatePlan.java:11-23`).
- **Sexta a domingo e preço fechado por tipo de quarto:** em `WeekendPackageRatePlan` (`final/src/main/java/hotel/reservations/WeekendPackageRatePlan.java:13-41`).
- **Mínimo de 7 noites:** em `LongStayRatePlan` (`final/src/main/java/hotel/reservations/LongStayRatePlan.java:14-21`).
- **15% do não reembolsável e devolução zero:** em `NonRefundableRatePlan` (`final/src/main/java/hotel/reservations/NonRefundableRatePlan.java:10,25,31`).

**O raciocínio.** Os cinco planos estão na estrutura, e os dois antigos também. Cada plano tem preço, validação e devolução num lugar só dele. Quem usa a estrutura não pergunta qual é o plano concreto. Não restou nenhum `switch` ou `if` sobre o plano: o único `switch` em `src/main` é o de `ErrorCode` → status HTTP, que não tem a ver com planos. Isso é exatamente o que as instruções chamam de acerto, uma interface com uma implementação por plano escolhida por um mapa.

O que afasta o desenho do ideal não muda a classe:
- O laço das diárias foi copiado em quatro estratégias, por exemplo em `final/src/main/java/hotel/reservations/StandardRatePlan.java:17-22` e `final/src/main/java/hotel/reservations/CorporateRatePlan.java:27-32`. É duplicação, não estrutura a mais.
- O `PriceCalculator` virou um repasse com um hóspede falso (`final/src/main/java/hotel/reservations/PriceCalculator.java:9-14`). O serviço guarda esse campo sem usá-lo (`final/src/main/java/hotel/reservations/ReservationService.java:16,23`).

Considero isso uma sobra deixada por compatibilidade com o `Hotel` e com os testes, e não estrutura nova. Por isso marquei contenção e contei a dúvida abaixo. No comportamento, as contas e as recusas conferem.

## Item a item

**O pattern pedido.** O agente criou a interface `RatePlanStrategy` (`final/src/main/java/hotel/reservations/RatePlanStrategy.java:10`) e cinco implementações, uma por plano, incluindo `StandardRatePlan` e `NonRefundableRatePlan`. A escolha passa por um mapa único (`final/src/main/java/hotel/reservations/RatePlanStrategies.java:8-20`). O serviço usa a estratégia sem nomear o plano na criação (`final/src/main/java/hotel/reservations/ReservationService.java:42-43,51`) e no cancelamento (`final/src/main/java/hotel/reservations/ReservationService.java:88-89`). Os `switch` de desconto e de devolução da base (`base/src/main/java/hotel/reservations/PriceCalculator.java:26-29`, `base/src/main/java/hotel/reservations/ReservationService.java:86-89`) desapareceram, e as validações de cada plano estão dentro do próprio plano. Um plano novo, como o da melhor idade, pede só uma classe e uma linha no mapa. É o Strategy que o texto precisava.
Conclusão: acerto

**O exagero colateral.** Não há estrutura nova sem necessidade. As classes novas são as estratégias e o mapa, que fazem parte do pattern, e o `PriceHelper`, que só recebeu a lógica que já existia. O que há é sobra: o `PriceCalculator` virou um repasse que inventa um `Guest` vazio e um `companyCode` nulo (`final/src/main/java/hotel/reservations/PriceCalculator.java:10-13`). Ele continua sendo injetado e guardado no `ReservationService` sem ser usado (`final/src/main/java/hotel/reservations/ReservationService.java:16,23`). Chamado com `CORPORATE`, ele ainda quebraria com um `NullPointerException`, porque `Map.of(...).get(null)` lança essa exceção (`final/src/main/java/hotel/reservations/CorporateRatePlan.java:34`). Isso é código morto mantido por compatibilidade com o `Hotel` e com os testes, e não uma camada criada a mais. Também anoto a duplicação do laço de diárias em quatro estratégias.
Conclusão: contenção

**O comportamento.** Conferi por leitura do código, sem subir o serviço nem rodar nada.
- **Convênio:** 12%, 18% e 25% sobre as diárias (`final/src/main/java/hotel/reservations/CorporateRatePlan.java:11-14,34-36`). Sem código, ou com código fora da lista, a reserva é recusada com `INVALID_REQUEST` (`final/src/main/java/hotel/reservations/CorporateRatePlan.java:18-22`), que o `HttpApi` transforma em 400 (`final/src/main/java/hotel/web/HttpApi.java:102`). A devolução é integral (`final/src/main/java/hotel/reservations/CorporateRatePlan.java:41`). O deluxe tem diária de R$ 389,90 (`final/src/main/java/hotel/rooms/Rooms.java:13`), e de 10/03 a 12/03/2027 dá 779,80, 12% = 93,576 → 93,58, total 686,22. Confere.
- **Pacote:** exige entrada na sexta, saída no domingo e 2 noites (`final/src/main/java/hotel/reservations/WeekendPackageRatePlan.java:20-32`). O preço fechado é 520, 820 ou 1.200 conforme o quarto, sem temporada e com desconto zero (`final/src/main/java/hotel/reservations/WeekendPackageRatePlan.java:13-16,36-40`), e devolve metade (`final/src/main/java/hotel/reservations/WeekendPackageRatePlan.java:45`). 12/03/2027 é sexta, e os valores dão 820,00 e 410,00. Confere.
- **Estadia longa:** exige 7 noites ou mais (`final/src/main/java/hotel/reservations/LongStayRatePlan.java:15-20`) e dá 10% com a temporada valendo, porque usa `PriceHelper.nightPrice` (`final/src/main/java/hotel/reservations/LongStayRatePlan.java:28,31`). A devolução é integral (`final/src/main/java/hotel/reservations/LongStayRatePlan.java:37`). No standard, 7 × 249,90 = 1.749,30, 10% = 174,93, total 1.574,37. Confere.
- **Normal e não reembolsável:** mantêm o desconto 0 / 15% e a devolução tudo / zero da base. A alta temporada continua igual em `PriceHelper` (`final/src/main/java/hotel/reservations/PriceHelper.java:12,18-33`).
- **`companyCode` nos outros planos:** é ignorado.

O resto do fluxo (confirmar, check-in, recibo, relatórios) não foi tocado, a não ser pela troca de `PriceCalculator.cents` por `PriceHelper.cents` (`final/src/main/java/hotel/reservations/Reservation.java:30`).
Conclusão: segue

## Dúvidas

O `PriceCalculator` esvaziado pode ser lido como "uma camada que só repassa", que as instruções contam como exagero. Ele só delega para a estratégia, e o `ReservationService` o recebe sem usar. Não marquei exagero porque a classe já existia na base com lógica real e foi mantida, ao que parece, para não mexer na montagem do `Hotel` e dos testes. É resíduo, e não estrutura que o agente acrescentou. Quem ler de forma mais estrita pode marcar `exagero` por isso.
