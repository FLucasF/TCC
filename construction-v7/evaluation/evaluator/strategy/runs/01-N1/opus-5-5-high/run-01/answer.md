# Cenário 01

## Visão geral

O agente montou os planos de tarifa como Strategy. Há uma classe abstrata `RatePlan` com quatro operações (validar, calcular as diárias, calcular o desconto, calcular a devolução) e uma subclasse para cada um dos cinco planos, o normal e o não reembolsável inclusive. Um único `switch` escolhe a subclasse a partir do código do plano. O cálculo do preço e o cancelamento só chamam o plano e não citam nenhum plano pelo nome. Nos testes, todas as regras dos planos conferem: os valores das três empresas, as recusas, o pacote com preço fechado e devolução de metade, a estadia longa com a temporada valendo e os exemplos do enunciado. O que pesa contra o desenho é cópia de código: o laço das diárias com a temporada está repetido em quatro classes, e a checagem de capacidade, que vale para todos os planos, foi copiada para dentro de cada um. Os problemas sérios estão na parte comum:
- o dinheiro no comprovante sai como "R$ 1.159.80";
- o check-out e a segunda via devolvem texto em vez do JSON combinado;
- corpo que não é JSON, campo faltando e `guests` 0 dão 500 em vez de 400;
- o relatório financeiro lista reservas canceladas que não deixaram dinheiro.

## Como foi implementado, e por que o julgamento

**O projeto.** É um único módulo Maven (Java 21, Javalin 6.7.0, Jackson, JUnit 4). O shade gera `target/pousada.jar` com `com.pousada.Main` no manifesto. Os pacotes são estes:
- `com.pousada.models`: as entidades `Room`, `Reservation`, `Message` e `Receipt`; o `RatePlan` e as cinco subclasses; e o `PricingUtils`, com o arredondamento meio-para-o-par, a regra da alta temporada e a contagem de noites.
- `com.pousada.services`:
  - `ReservationService` guarda os quartos e as reservas em `HashMap`s na memória (`ReservationService.java:11-26`), cria as reservas e faz as transições de estado.
  - `ReportService` monta os dois CSVs.
  - `ReceiptFormatter` monta o texto do comprovante e do e-mail.
  - `MessageService` é a lista de mensagens enviadas.
- `com.pousada.api.dto`: os DTOs públicos.

O `Main` cria os serviços como campos estáticos, registra as rotas em lambdas e lê `PORT` (`Main.java:21-37`). Cada rota captura `ApiException` e responde `{error, message}`; o resto vai para um handler genérico que devolve 500 (`Main.java:203-208`).

Uma criação de reserva passa por estes passos:
1. `POST /reservations` faz `bodyAsClass` e uma validação de campos, as duas fora do `try` (`Main.java:50-51`).
2. A rota chama `ReservationService.createReservation` (`ReservationService.java:36-72`), que valida datas e quarto.
3. O serviço escolhe o plano (`ReservationService.java:51`) e chama `ratePlan.validate(...)` (`:53`).
4. Confere a disponibilidade do quarto.
5. Pede ao plano `calculateSubtotal` e `calculateDiscount` e calcula o total (`:62-64`).

O agente escreveu 15 testes JUnit em `ReservationServiceTest`. Todos chamam o serviço direto, nenhum passa pela HTTP. Cobrem os cinco planos, os códigos de empresa, a capacidade, a disponibilidade, o fluxo de estados e as devoluções.

**A parte dos planos.** `RatePlan` (`models/RatePlan.java:6-23`) é abstrata e declara `validate(roomCapacity, guests, checkIn, checkOut, companyCode)`, `calculateSubtotal(...)`, `calculateDiscount(subtotal)` e `calculateRefund(reservation)`. Cada plano é uma subclasse:
- **`StandardRatePlan`**: desconto zero (`StandardRatePlan.java:33`); devolve o que foi pago, ou zero se a reserva estava pendente (`:38-41`).
- **`NonRefundableRatePlan`**: 15% de desconto (`NonRefundableRatePlan.java:33`); devolução sempre zero (`:38`).
- **`CorporateRatePlan`**: um mapa empresa→percentual (`CorporateRatePlan.java:8-12`). A validação recusa código ausente ou fora do mapa (`:32-35`). O desconto vem do percentual da empresa (`:57`) e a devolução é integral (`:61-66`).
- **`WeekendPackageRatePlan`**: a validação exige entrada na sexta e saída no domingo seguinte (`WeekendPackageRatePlan.java:20-29`). O preço é fechado por tipo de quarto (`:38-44`); o `switch` ali é sobre o tipo de quarto, não sobre o plano. Não há desconto (`:48`) e a devolução é metade, arredondada (`:52-57`).
- **`LongStayRatePlan`**: exige 7 noites ou mais (`LongStayRatePlan.java:15-19`); dá 10% sobre as diárias com a temporada (`:27-41`) e devolve tudo (`:44-49`).

A escolha do plano fica num só lugar, o `switch` de `createRatePlan` (`ReservationService.java:203-212`), que só instancia a classe. O cancelamento só faz `reservation.getRatePlan().calculateRefund(reservation)` (`ReservationService.java:124`). A resposta usa `getRatePlan().getCode()` (`Main.java:231`). Nada fora das subclasses pergunta qual é o plano: o `grep` por `CORPORATE`, `WEEKEND`, `LONG_STAY`, `NON_REFUNDABLE` e `instanceof` só encontra o `switch` de seleção. O relatório financeiro usa `amountPaid - refund` sem saber o plano (`ReportService.java:91-99`).

**O julgamento.** É o que as instruções aceitam como acerto: uma classe abstrata com uma implementação por plano, os cinco dentro dela, e um único ponto de seleção. Preço, validação e devolução de cada plano moram na sua classe. Não sobrou `switch` de comportamento em nenhum lugar. Um sexto plano pede uma classe nova e uma linha no `switch` de seleção.

O que afasta o desenho do ideal é qualidade de código, não a estrutura:
- O laço que soma as noites com a temporada está copiado em quatro planos (`StandardRatePlan.java:20-29`, `NonRefundableRatePlan.java:20-29`, `CorporateRatePlan.java:44-53`, `LongStayRatePlan.java:27-36`).
- A checagem de capacidade, que não é regra de plano, está copiada em todos os `validate`, e o serviço traduz o código de erro para 422 olhando a string (`ReservationService.java:55`).
- A assinatura de `validate` carrega o `companyCode` para todos os planos.
- `CorporateRatePlan` guarda estado dentro de `validate` (`CorporateRatePlan.java:40`), e `calculateDiscount` depende disso.

Nada disso tira um plano da estrutura nem faz o serviço decidir pelo plano.

No exagero colateral não há interface de uma implementação só, fábrica, builder nem camada de repasse. Há código morto: o `ReceiptResponse` não é usado, nem `isWeekend` e `getFollowingSunday` em `PricingUtils.java:37-46`. Mas código morto não é estrutura sem necessidade.

No comportamento, a parte dos planos confere inteira. A parte comum tem várias falhas, descritas no item a item.

## Item a item

**O enunciado (pede o pattern).** A estrutura é a classe abstrata `RatePlan` (`models/RatePlan.java:6-23`) com cinco subclasses, uma por plano, o `STANDARD` e o `NON_REFUNDABLE` inclusive (`StandardRatePlan.java:6`, `NonRefundableRatePlan.java:6`, `CorporateRatePlan.java:7`, `WeekendPackageRatePlan.java:7`, `LongStayRatePlan.java:6`). Cada uma tem a sua validação, o seu preço e a sua devolução. Um único `switch` só instancia a subclasse (`ReservationService.java:203-212`). O preço (`ReservationService.java:62-63`), a validação (`:53`) e o cancelamento (`:124`) pedem ao plano sem nomeá-lo. As regras próprias de cada plano ficaram dentro dele: a sexta a domingo (`WeekendPackageRatePlan.java:20-29`), as 7 noites (`LongStayRatePlan.java:15-19`) e o código da empresa (`CorporateRatePlan.java:32-35`). As ressalvas são de qualidade e não mudam a classe: o laço das diárias repetido em quatro planos e a capacidade copiada em todos os `validate`.
Conclusão: acerto

**O exagero colateral.** Não há. Fora dos planos o desenho é direto: serviços concretos, um formatador estático e DTOs simples. Não há interface de uma implementação só, fábrica, builder nem camada que só repassa. Existe código morto (`api/dto/ReceiptResponse.java`, nunca usado; `PricingUtils.java:37-46`), mas isso é sobra, não estrutura.
Conclusão: contenção

**O comportamento.** Compilei uma cópia, subi `target/pousada.jar` na 7070, chamei a API com `curl` e derrubei o serviço no fim. O `mvn -q verify` passou: 15 testes, 0 falhas, 0 erros.

O que confere, na parte dos planos:

| Teste | Resultado |
|---|---|
| Tecnosul, deluxe de 10/03 a 12/03/2027 | 779.80 / 93.58 / 686.22 |
| Agrovale, suíte | desconto 208.76 (18%) |
| Farmavida, suíte | desconto 289.95 (25%) |
| Convênio sem código, com `ACME` e com `tecnosul` minúsculo | 400 `INVALID_REQUEST` |
| Pacote no deluxe, de 12/03 a 14/03 | 820; pago e cancelado, devolve 410.00 |
| Pacote na suíte, em julho | 1200, sem temporada |
| Pacote no standard | 520 |
| Pacote de sábado a segunda, ou de sexta a nove noites depois | 400 |
| Estadia longa no standard, de 01/03 a 08/03 | 1749.30 / 174.93 / 1574.37 |
| Estadia longa em julho, no deluxe | 3275.16 / 327.52, com a temporada |
| Estadia longa de 6 noites | 400 |
| Convênio e estadia longa cancelados depois de pagos | devolvem tudo |
| Reservas pendentes canceladas | devolvem 0 |
| `companyCode` num plano `STANDARD` | ignorado |
| Não reembolsável | desconto de 15%; cancelado, devolve 0 e o financeiro conta 424.83 |
| `STANDARD` de 10/07 a 12/07 | 599.76 |

Na parte comum também conferem os erros 404, 409 e 422, o e-mail com o assunto certo e o relatório de ocupação.

O que não confere:
- O dinheiro no comprovante e no e-mail sai como "R$ 779.80" e "R$ 1.159.80", em vez de "R$ 779,80" e "R$ 1.159,80" (`services/ReceiptFormatter.java:74`). O e-mail ainda tem uma linha em branco a mais (`ReceiptFormatter.java:25-26`).
- O check-out e a segunda via devolvem `text/plain` em vez do JSON com `title`, `reservationId` etc. (`Main.java:123-125`, `:145-147`).
- Corpo que não é JSON, campo faltando e `guests` 0 dão 500 `INTERNAL_ERROR` em vez de 400, porque a validação está fora do `try` (`Main.java:50-51`).
- O financeiro lista canceladas que não deixaram dinheiro, com amount 0.00 (`ReservationService.java:168-171`).
- `month=2027-13` é aceito e devolve 200.
- O JSON traz valores sem duas casas: 820.0 (`WeekendPackageRatePlan.java:10-12`), 0 e 249.9.

Como a parte dos planos segue e a parte comum falha em vários pontos, a conclusão é "em parte".
Conclusão: em parte

## Dúvidas

A checagem de capacidade, que vale para todos os planos, foi copiada para dentro do `validate` de cada um (por exemplo `StandardRatePlan.java:13-16`, `WeekendPackageRatePlan.java:31-34`). Ela não é regra de plano, mas virou uma obrigação de toda implementação nova. Isso tem um efeito visível: com o convênio, quatro hóspedes no 101 e sem código, a resposta é 400, não 422, porque cada plano decide a ordem das checagens. Também conta que o `CorporateRatePlan` guarda o código dentro do `validate` (`CorporateRatePlan.java:40`), e o `calculateDiscount` depende disso. Os dois são defeitos de qualidade dentro da estrutura, não decisões tomadas pelo plano fora dela, então mantive `acerto`.
