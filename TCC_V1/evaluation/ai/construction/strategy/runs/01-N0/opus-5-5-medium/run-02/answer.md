# Cenário 01

## Visão geral

O agente entregou os cinco planos de tarifa, mas nenhum deles é um Strategy. O plano é uma `String` e o que cada plano faz é decidido por `switch` sobre essa string em três lugares: o preço fica em `PricingService`, a validação de quem pode usar fica em `ReservationService` e a devolução fica no cancelamento. Os dois planos que já existiam e os três novos entraram todos nos mesmos `switch`, e um plano novo, como o da melhor idade, obriga a mexer em pelo menos três métodos de duas classes. O código é enxuto e não tem estrutura sobrando. As contas do enunciado conferem na leitura. O problema mais sério é o desenho, e há também falhas de comportamento: o comprovante escreve o dinheiro sem separador de milhar (R$ 1574,37 em vez de R$ 1.574,37), o JSON não sai com 2 casas em valores redondos (520.0), a segunda via sai para reserva cancelada que tinha sido paga, e a falta de nome, e-mail ou CPF não é recusada.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote é `com.mareaalta`:
- **`Application`** monta o Javalin numa classe só, com as rotas, o mapa de código de erro para status, a montagem e o texto do comprovante e o e-mail de confirmação.
- **`service`** tem quatro classes:
  - `ReservationService`: as reservas num `HashMap` em memória, o número sequencial, a validação, a criação e as transições de estado;
  - `PricingService`: métodos estáticos de preço, temporada, noites e arredondamento;
  - `MessageService`: a lista de e-mails enviados;
  - `ReportService`: os dois CSV.
- **`model`** tem `Reservation`, `Receipt`, `Message`, `ErrorResponse` e `Room`. `Reservation` é uma classe com campos públicos: o plano e o status são `String`, e o dinheiro é `double`.
- **`data.RoomData`** guarda os cinco quartos fixos.

O caminho de um `POST /reservations` é este:
1. A rota desserializa o corpo em `ReservationRequest` (`Application.java:28`).
2. Chama `validateCreateReservation`, que devolve um código de erro ou `null` (`Application.java:30-43`).
3. Chama `createReservation`, que pede o preço a `PricingService.calculatePrice` (`ReservationService.java:124`).

A porta vem de `PORT`, com 7070 se ela não vier (`Application.java:16-17`), e o jar é montado pelo shade com `mainClass` (`pom.xml:78`). Os testes são unitários dos dois serviços. Em `PricingServiceTest` estão os exemplos do enunciado, o não reembolsável, a temporada e o arredondamento. Em `ReservationServiceTest` estão as transições, as devoluções, as recusas de cada plano e a sobreposição de quartos. Não há teste das rotas HTTP.

**A parte que pede o pattern.** Não existe tipo para o plano: ele é a `String` `ratePlan` (`model/Reservation.java:31`). O comportamento de cada plano está espalhado em três `switch` sobre essa string:
- **Preço:** `PricingService.calculatePrice` faz `switch (ratePlan)` e chama um método privado por plano (`service/PricingService.java:33-42`). Os métodos `calculateStandard`, `calculateNonRefundable`, `calculateCorporate` e `calculateLongStay` repetem quatro vezes o mesmo laço de diárias com temporada (`PricingService.java:44-58`, `60-76`, `78-97`, `123-141`). O preço do pacote é escolhido comparando a diária do quarto com 249,90, 389,90 e 579,90, e não pelo tipo do quarto (`PricingService.java:115-121`). Os descontos das empresas estão em outro `switch` (`PricingService.java:143-150`).
- **Quem pode usar:** `ReservationService.isRatePlanValid` faz `switch (ratePlan)` com regras próprias para estadia longa, convênio e pacote (`service/ReservationService.java:66-74`). As regras do convênio e do pacote aparecem de novo em `isValidCompanyCode` (`ReservationService.java:85-87`) e `isValidWeekendPackage` (`ReservationService.java:89-104`), repetindo o que `PricingService` já faz (`PricingService.java:104-107`, `143-150`).
- **Devolução:** `calculateRefund` faz `switch (res.ratePlan)` (`ReservationService.java:191-198`).

**O raciocínio.** As instruções do cenário dizem que é omissão quando o comportamento dos planos é decidido por `switch` ou `if` sobre o plano no preço, no cancelamento ou na validação. É exatamente o que acontece aqui, nos três pontos. Não há interface, classe abstrata nem `enum` com corpo por constante. Nada aproxima o desenho do pattern: os métodos privados por plano em `PricingService` são só um jeito de organizar o `switch`, e as regras de cada plano continuam em três lugares diferentes. Por isso a classe é omissão, e não malfeito: não há uma estrutura de pattern parcialmente usada, há só `switch`.

No exagero colateral não pesa nada. Não há interfaces, fábricas nem camadas de repasse. O que sobra é duplicação, que é outro problema.

No comportamento, as quatro contas do enunciado conferem. O que pesa contra é a formatação do dinheiro no comprovante, a falta das 2 casas no JSON, a segunda via de reserva cancelada e a falta de validação dos campos de texto.

## Item a item

**O pattern pedido.** O plano é uma `String` (`model/Reservation.java:31`), e o comportamento de cada plano é decidido em três `switch` sobre ela:
- o preço em `service/PricingService.java:34-41`;
- a validação de quem pode usar em `service/ReservationService.java:67-73`;
- a devolução em `service/ReservationService.java:192-197`.

Os planos que já existiam (STANDARD e NON_REFUNDABLE) e os três novos estão nos mesmos `switch`. Não há interface, classe abstrata nem `enum` com corpo por constante. Para criar um plano novo é preciso editar pelo menos `calculatePrice`, `isRatePlanValid` e `calculateRefund`, em duas classes. É a forma de omissão que as instruções descrevem.
Conclusão: omissão

**O exagero colateral.** Não há. As classes de serviço são concretas e cada uma tem o seu trabalho. Não há interface com uma implementação só, nem fábrica, nem builder, nem camada que só repassa. O que existe é duplicação (o laço de diárias quatro vezes em `PricingService.java:44-141`, e a validação do pacote e do convênio duas vezes, em `ReservationService.java:85-104` e `PricingService.java:104-107`, `143-150`), além de um `DateTimeFormatter` com padrão "0.00" que ninguém usa (`service/ReportService.java:35`). Nada disso é estrutura sobrando.
Conclusão: contenção

**O comportamento.** A conferência foi feita só pela leitura do código, sem subir o serviço nem rodar o Maven.

O que confere:
- **Convênio:** Tecnosul 12%, Agrovale 18% e Farmavida 25% (`PricingService.java:144-149`). Sem código ou com código fora da lista dá 400 `INVALID_REQUEST` (`ReservationService.java:70`, `55-57`, e `Application.java:227-233`). O exemplo dá 779,80 − 93,58 (de 93,576) = 686,22.
- **Pacote:**
  - Exige 2 noites, de sexta a domingo (`PricingService.java:104-107`, `ReservationService.java:258-261`).
  - Os preços são 520, 820 e 1.200, sem temporada (`PricingService.java:117-119`).
  - 12/03/2027 é sexta, então o exemplo dá R$ 820,00, e a devolução é metade, R$ 410,00 (`ReservationService.java:195`).
- **Estadia longa:** recusa menos de 7 noites (`ReservationService.java:242`) e dá 10% de desconto com a temporada valendo (`PricingService.java:123-140`). O exemplo dá 1.749,30 − 174,93 = 1.574,37.
- **Devoluções:** convênio e estadia longa devolvem tudo, o não reembolsável devolve 0, e reserva pendente devolve 0 (`ReservationService.java:181-186`, `193-194`).
- **Normal com temporada:** o exemplo do enunciado dá 2 × 299,88 = 599,76.
- **Resto:**
  - as transições de estado (`ReservationService.java:162-170`, `175-178`, `205-209`, `217-221`);
  - o conflito de quarto ignorando canceladas (`ReservationService.java:145`);
  - o `companyCode` ignorado fora do convênio;
  - os relatórios com `total,N/5` e a soma do que foi pago menos o que foi devolvido (`ReportService.java:74`, `129-133`).

O que não confere:
- **Comprovante e e-mail sem separador de milhar:** o dinheiro é formatado com `String.format("R$ %.2f").replace(".", ",")` (`Application.java:285-287`). Sai "R$ 1574,37" em vez de "R$ 1.574,37", e o pacote da suíte sai "R$ 1200,00".
- **JSON sem 2 casas em valores redondos:** o dinheiro é `double` (`model/Reservation.java:39-55`), então 520,00 e 0,00 saem como `520.0` e `0.0`, e não com 2 casas como foi pedido.
- **Segunda via de reserva cancelada:** a segunda via é liberada por `amountPaid == 0` (`Application.java:170`). Uma reserva confirmada e depois cancelada ainda tem valor pago e ganha comprovante, quando só deveria ganhar a confirmada, com check-in ou com check-out.
- **Campos de texto sem validação:** nome, e-mail e CPF não são validados, e a reserva é criada com `null`. Sem `roomNumber`, a resposta é 404 `ROOM_NOT_FOUND` em vez de 400 (`ReservationService.java:30-63`).
- **Arredondamento sobre `double`:** o arredondamento usa `new BigDecimal(double)` (`PricingService.java:13`), que pode arredondar diferente do "meio para o par" num empate exato. Isso não aparece nos exemplos.
Conclusão: em parte

## Dúvidas

- O enunciado não diz que código usar quando se pede a segunda via de uma reserva não paga. O agente usou 404 `RESERVATION_NOT_FOUND` (`Application.java:171-172`), e 409 `OPERATION_NOT_ALLOWED` seria igualmente defensável. Por isso só contei como erro a segunda via liberada para reserva cancelada.
- A regra de que o código da empresa só vale no convênio não está isolada: o `companyCode` é guardado e sai no JSON em qualquer plano (`model/Reservation.java:33`). É pequeno e não muda a classe, que já é omissão pelos três `switch`.
