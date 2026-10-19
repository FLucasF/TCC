# Cenário 01

## Visão geral

O agente montou os cinco planos de tarifa como um Strategy limpo. Há uma interface `RatePlanPolicy` com três operações: validar, calcular o preço e calcular a devolução. Cada plano tem a sua classe, inclusive o normal e o não reembolsável, que a parte comum do enunciado já trazia. Um único `EnumMap` liga a constante do `enum` à implementação. O cálculo do preço e o cancelamento pedem o comportamento ao plano e não nomeiam nenhum plano. Não sobrou `switch` nem `if` sobre o plano no código de produção. Não há estrutura desnecessária fora do pattern. Todos os valores, recusas e mensagens que testei pela API conferem, inclusive os quatro exemplos do texto, e o `mvn verify` passa com os 30 testes do agente. Não encontrei problema sério. O único detalhe é a ordem das recusas: quando a reserva quebra a regra do plano e o quarto também está ocupado, quem responde é a disponibilidade (409), e não a regra do plano (400).

## Como foi implementado, e por que o julgamento

**O projeto.** O código está em `hotel.*`:
- `hotel.rooms` tem os cinco quartos fixos (`Room`, `Rooms`, `RoomType`).
- `hotel.reservations` tem o domínio: `Reservation`, `ReservationService`, `ReservationStore` (memória), `PriceCalculator`, os planos e os erros (`ErrorCode`, `HotelException`).
- `hotel.documents` tem o comprovante (`ReservationReceipt`) e a formatação em pt-BR (`TextFormat`).
- `hotel.messages` tem o "envio" de e-mail para uma lista em memória (`EmailSender`, `SentMessages`).
- `hotel.reports` tem os dois CSV (`OccupancyReport`, `RevenueReport`).
- `hotel.web` tem as rotas Javalin (`HttpApi`) e os DTOs.

`Hotel` monta tudo à mão no construtor (`src/main/java/hotel/Hotel.java:21-29`) e sobe na porta de `PORT`, ou na 7070 se ela não vier (`Hotel.java:42-45`). O jar é gerado pelo shade com `hotel.Hotel` como classe principal (`pom.xml:66-67`).

O caminho de uma requisição: `POST /reservations` → `HttpApi.readNewReservation` (`src/main/java/hotel/web/HttpApi.java:109-130`) valida os campos e monta o `NewReservation`, com o `companyCode` opcional. Depois `ReservationService.create` (`src/main/java/hotel/reservations/ReservationService.java:29-55`) confere datas, hóspedes, quarto, capacidade e disponibilidade. Por fim, `PriceCalculator.calculate` aplica o plano, e a reserva vai para o `ReservationStore`. Os erros viram `HotelException`, e um único mapeamento de código para status HTTP responde por eles (`HttpApi.java:100-107`).

O agente escreveu 30 testes JUnit:
- `PriceCalculatorTest`: 6
- `ReservationServiceTest`: 11
- `NewRatePlansTest`: 4
- `ReportsTest`: 5
- `HttpApiTest`: 4

**A parte que pede o pattern.**
- `RatePlan` é um `enum` simples, sem comportamento, com os cinco nomes (`RatePlan.java:3-14`).
- `RatePlanPolicy` é a interface com `validate`, `price` e `refund` (`RatePlanPolicy.java:7-15`).
- Há uma implementação por plano:
  - `StandardPlan`: sem validação, 0% de desconto, devolve o que foi pago (`StandardPlan.java:9-20`).
  - `NonRefundablePlan`: 15% de desconto, devolução zero (`NonRefundablePlan.java:8-22`).
  - `CorporatePlan`: mapa empresa→desconto, recusa sem código ou com código desconhecido, devolve tudo (`CorporatePlan.java:10-31`).
  - `WeekendPackagePlan`: sexta→domingo, preço fechado por tipo de quarto sem temporada, devolve metade (`WeekendPackagePlan.java:12-37`).
  - `LongStayPlan`: mínimo de 7 noites, 10% sobre as diárias com temporada, devolve tudo (`LongStayPlan.java:10-28`).
- A escolha acontece num único ponto, o `EnumMap` de `RatePlans` (`RatePlans.java:9-21`).
- O cálculo da reserva só faz `policyFor(...)`, depois `validate`, depois `price` (`PriceCalculator.java:25-29`). O `PriceCalculator` só cuida do que é comum a todos os planos: as noites e a temporada (`PriceCalculator.java:31-56`).
- O cancelamento só faz `ratePlans.policyFor(reservation.ratePlan()).refund(reservation)` (`ReservationService.java:88`).
- `PercentOff` é um auxiliar sem decisão nenhuma, que quatro planos usam para "x% sobre as diárias" (`PercentOff.java:10-13`).

Uma busca por `RatePlan.` e `ratePlan` no código de produção só encontra o mapa, a seleção, a cópia do campo para a reserva e o DTO. Não há nenhum `switch` ou `if` sobre o plano.

**Por que o julgamento.** As instruções aceitam como acerto exatamente isto: uma interface com uma implementação por plano, escolhida por um mapa. Os cinco planos, os dois antigos inclusive, estão na estrutura. Cada um guarda num lugar só o preço, a validação e a devolução. Nada fora do plano decide pelo plano: nem o preço, nem a recusa, nem a devolução. Um plano novo é uma classe, uma constante e uma linha no mapa. Nada me afasta do acerto.

**Exagero colateral.** Pesei se `RatePlans`, `NightlyCharges` e `PercentOff` seriam camadas a mais. Não são:
- `RatePlans` é o ponto de seleção do pattern.
- `NightlyCharges` é um record com dados reais.
- `PercentOff` evita repetir a mesma conta em quatro planos.

As demais classes são concretas e têm papel próprio. Não há interface com implementação única nem fábrica.

**Comportamento.** Os testes pela API conferiram com o enunciado, como descrevo abaixo.

## Item a item

**O pattern pedido.** É um Strategy por interface:
- A interface: `src/main/java/hotel/reservations/RatePlanPolicy.java:7-15`.
- Uma implementação para cada um dos cinco planos: `StandardPlan.java:6`, `NonRefundablePlan.java:6`, `CorporatePlan.java:8`, `WeekendPackagePlan.java:10`, `LongStayPlan.java:8`.
- A seleção por um único `EnumMap`: `RatePlans.java:9-21`.
- O preço e a validação pedidos ao plano: `PriceCalculator.java:26-28`.
- A devolução pedida ao plano: `ReservationService.java:88`.

O normal e o não reembolsável também saíram do `switch` e viraram classes. Regras de quem pode usar o plano (código da empresa, sexta a domingo, 7 noites) ficam dentro dos planos (`CorporatePlan.java:16-21`, `WeekendPackagePlan.java:19-26`, `LongStayPlan.java:14-18`), e não no serviço. É o pattern que o texto pedia.
Conclusão: acerto

**O exagero colateral.** Não há. `RatePlans` é o ponto único de escolha que o pattern precisa. `PercentOff` (`PercentOff.java:5-14`) é uma conta compartilhada sem decisão. `NightlyCharges` e `Price` são records de dados. Não há interfaces com uma implementação só, fábricas, builders nem camadas que só repassam. A montagem em `Hotel.java:21-29` é direta.
Conclusão: contenção

**O comportamento.** Subi o jar de uma cópia do projeto (`PORT=7070 java -jar target/hotel.jar`) e chamei a API com `curl`.

Convênio:
- TECNOSUL num deluxe de 10 a 12/03/2027: subtotal 779.80, desconto 93.58, total 686.22. ✔
- AGROVALE no mesmo quarto e datas: desconto 140.36. ✔
- FARMAVIDA na suíte: 1159.80 − 289.95 = 869.85. ✔
- Sem código, com "ACME" ou com "tecnosul" em minúsculas: 400 `INVALID_REQUEST`. ✔
- Confirmado e cancelado: devolveu 686.22. ✔

Pacote de fim de semana:
- Deluxe de sexta 12/03 a domingo 14/03/2027: 820.00, sem desconto. ✔
- Confirmado e cancelado: devolveu 410.00. ✔
- O financeiro de março mostra 410.00 retidos. ✔
- Standard em julho (alta temporada): 520.00, a temporada não entra. ✔
- Suíte: 1200.00. ✔
- De sábado a segunda, ou de sexta a segunda: 400 `INVALID_REQUEST`. ✔
- Cancelado ainda pendente: devolução 0.00. ✔

Estadia longa:
- Standard de 01 a 08/03/2027: 1749.30 − 174.93 = 1574.37. ✔
- Cancelada depois de confirmada: devolveu tudo. ✔
- De 25/02 a 04/03/2027, com temporada parcial: 1949.22 − 194.92 = 1754.30, que é a conta certa. ✔
- Com 6 noites: 400. ✔
- O `companyCode` enviado com outro plano foi ignorado. ✔

Parte comum do enunciado:
- STANDARD num quarto 102 de 10 a 12/07/2027: 599.76. ✔
- NON_REFUNDABLE: 15% de desconto, e devolução 0.00 depois de pago. ✔
- STANDARD cancelado depois de pago: devolveu tudo. ✔
- E-mail com o assunto e o texto exatos, com a linha "Desconto: R$ 93,58" quando há desconto. ✔
- O comprovante do check-out e o da segunda via trazem os campos pedidos. ✔
- Segunda via de reserva pendente: 409 `OPERATION_NOT_ALLOWED`. ✔
- Cancelar depois do check-in e fazer check-out sem check-in: 409. ✔
- Capacidade excedida: 422. ✔
- Quarto ocupado: 409. ✔
- Quarto inexistente e reserva inexistente: 404. ✔
- Corpo que não é JSON, plano desconhecido e mês `2027-7`: 400. ✔
- O CSV de ocupação vem em `text/csv; charset=utf-8`, com a linha `total,2/5`. ✔
- O financeiro está ordenado e soma certo, contando o não reembolsável cancelado (julho: 795.40 + 599.76 = 1395.16). ✔

O `mvn verify` da cópia deu BUILD SUCCESS, com os 30 testes passando e nenhuma falha. No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Duas pequenas, e nenhuma muda as conclusões:

- **A ordem das recusas.** A validação do plano roda depois das conferências de capacidade e de disponibilidade (`ReservationService.java:39-51`). Um pacote numa data errada e num quarto ocupado recebe 409 `ROOM_UNAVAILABLE` em vez de 400; eu testei esse caso. O enunciado não define a precedência.
- **A devolução de reserva pendente.** "Pendente não tem nada a devolver" não é uma regra explícita. Depende de cada plano calcular a devolução sobre o `amountPaid`, que é zero antes da confirmação (`Reservation.java:30`, `StandardPlan.java:19`, `WeekendPackagePlan.java:36`). Funciona e não decide pelo plano, mas um plano futuro que devolvesse um valor fixo teria de lembrar disso.
