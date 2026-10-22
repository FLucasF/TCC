# Cenário 01

## Visão geral

O agente transformou o `enum RatePlan` numa estratégia: cada uma das cinco constantes tem o seu próprio corpo e sobrescreve só o que difere do padrão. São três métodos: `validate` (quem pode usar o plano), `adjust` (preço) e `refund` (devolução). Os dois `switch` da base saíram. `PriceCalculator.calculate` e `ReservationService.cancel` agora pedem ao plano e não citam nenhum plano pelo nome. Os dois planos que já existiam também foram para a estrutura. Não há estrutura sobrando. Pela leitura do código, as regras e os exemplos do pedido conferem. O problema mais sério é pequeno: a validação do plano roda duas vezes, uma no serviço e outra no cálculo, e a constante `NON_REFUNDABLE_DISCOUNT` continuou em `PriceCalculator`, embora só o plano a use.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin. `hotel.web.HttpApi` recebe o JSON (`CreateReservationRequest`) e o converte em `NewReservation`. `hotel.reservations.ReservationService` cuida da regra de negócio: cria, confirma, cancela, faz check-in e check-out. `PriceCalculator` calcula o preço das noites, com a alta temporada. `ReservationStore` guarda as reservas em memória. `hotel.rooms.Rooms` tem os quartos fixos no código: 101 e 102 standard a R$ 249,90, 201 e 202 deluxe a R$ 389,90, 301 suíte a R$ 579,90. Há ainda `documents` (o comprovante), `messages` (os e-mails) e `reports` (os CSV). O caminho de uma reserva é este: `POST /reservations`, depois `HttpApi.toNewReservation`, depois `ReservationService.create`. Este valida as datas e os hóspedes, a regra do plano, o quarto, a capacidade e a disponibilidade, chama `PriceCalculator.calculate` e grava a reserva. O agente acrescentou o campo `companyCode` em `CreateReservationRequest` (`src/main/java/hotel/web/CreateReservationRequest.java:16`) e em `NewReservation` (`src/main/java/hotel/reservations/NewReservation.java:13`). Também criou um construtor de compatibilidade sem o código (`NewReservation.java:15-18`). Escreveu um teste novo, `src/test/java/hotel/reservations/RatePlansTest.java`, que cobre os três exemplos do financeiro, as recusas e as devoluções. Os testes que já existiam não foram tocados.

**A parte do pattern.** `src/main/java/hotel/reservations/RatePlan.java` define, no próprio enum, os comportamentos padrão. `validate` não faz nada (`:98-99`), `adjust` devolve o preço cheio (`:102-104`) e `refund` devolve tudo (`:107-109`). Cada constante sobrescreve só o que muda:
- `STANDARD` (`:17`) usa os três padrões;
- `NON_REFUNDABLE` (`:19-29`) dá 15% de desconto e não devolve nada;
- `CORPORATE` (`:31-48`) tem um mapa de empresa para desconto, recusa código ausente ou desconhecido e aplica o desconto da empresa;
- `WEEKEND_PACKAGE` (`:50-74`) tem um mapa de tipo de quarto para preço fechado, aceita só de sexta a domingo e devolve metade;
- `LONG_STAY` (`:76-88`) exige 7 noites ou mais e dá 10% de desconto.

`PriceCalculator.calculate` soma as noites com a temporada e entrega o "preço cheio" a `ratePlan.adjust` (`src/main/java/hotel/reservations/PriceCalculator.java:33-34`). `ReservationService.cancel` faz só `reservation.ratePlan().refund(...)` (`src/main/java/hotel/reservations/ReservationService.java:88`). Não sobrou nenhum `switch` nem `if` sobre o plano fora do enum. Os únicos usos de `RatePlan` no código são para repassar o valor.

**O raciocínio.** As instruções do pattern aceitam como acerto o `enum` como estratégia, em que cada constante tem o próprio corpo com métodos sobrescritos. É exatamente o caso aqui, e não há `switch (this)`. Os cinco planos estão na estrutura, inclusive os dois antigos. A validação de cada plano (o código da empresa, sexta a domingo, as 7 noites) mora no plano, e não no serviço. Um plano novo é uma constante nova, sem mexer no cálculo nem no cancelamento. Há um ponto que afasta um pouco do ideal. `validate` é chamado duas vezes, em `ReservationService.java:34-35` e em `PriceCalculator.java:25-26`, mas as duas chamadas passam pelo plano e nenhuma decide pelo plano à mão. Há também uma sobra cosmética: a constante `PriceCalculator.NON_REFUNDABLE_DISCOUNT` (`PriceCalculator.java:17`), lida pelo plano em `RatePlan.java:22`. Nenhum dos dois pontos tira o desenho do pattern. No exagero colateral, o registro `StayDates` (`RatePlan.java:91-95`) e os auxiliares `discounted` e `invalid` (`:111-118`) são pequenos e servem diretamente ao pattern. Não há interface, fábrica nem camada que só repasse. No comportamento, conferi as contas e as regras pela leitura do código, e elas fecham.

## Item a item

**O pattern pedido.** O `RatePlan` virou um enum-estratégia. Cada constante tem corpo próprio com `validate`, `adjust` e `refund` (`src/main/java/hotel/reservations/RatePlan.java:17-88`), e os padrões ficam em `:98-109`. O cálculo pede o preço ao plano (`src/main/java/hotel/reservations/PriceCalculator.java:34`), o cancelamento pede a devolução (`src/main/java/hotel/reservations/ReservationService.java:88`) e a criação pede a validação (`ReservationService.java:34-35`). Os dois `switch` da base (`base/src/main/java/hotel/reservations/PriceCalculator.java` e `base/src/main/java/hotel/reservations/ReservationService.java`) foram removidos. Os cinco planos, inclusive `STANDARD` e `NON_REFUNDABLE`, estão na estrutura. Isso corresponde à forma de acerto aceita: o enum com comportamento por constante, sem `switch (this)`.
Conclusão: acerto

**O exagero colateral.** Não há. O que foi criado (`StayDates`, `discounted`, `invalid` em `RatePlan.java:91-118`) é pequeno e é usado pelo próprio pattern. O construtor de compatibilidade em `NewReservation.java:15-18` e a sobrecarga de `calculate` em `PriceCalculator.java:19-21` servem para não quebrar quem chama com a assinatura antiga, e não são camadas novas. A validação duplicada (`ReservationService.java:34` e `PriceCalculator.java:26`) é redundância, não estrutura a mais.
Conclusão: contenção

**O comportamento.** Conferi tudo pela leitura do código, sem subir o serviço nem rodar os testes, conforme a instrução.
- **Convênio.** O mapa tem TECNOSUL 0,12, AGROVALE 0,18 e FARMAVIDA 0,25 (`RatePlan.java:32-35`). Código nulo ou fora do mapa gera `HotelException(INVALID_REQUEST)` (`:39-40`, `:116-118`), que vira 400, como os outros erros de dados. Um código em branco vira `null` em `src/main/java/hotel/web/HttpApi.java:130`. A conta: deluxe de 10/03 a 12/03/2027 dá 2 × 389,90 = 779,80, com desconto de 93,576, arredondado a 93,58, e total de 686,22. Confere.
- **Pacote.** Exige entrada na sexta e saída em entrada + 2 dias, ou seja, no domingo (`:58-59`). O preço fechado vem do mapa por tipo de quarto (`:51-54`), com subtotal igual ao total e desconto zero, ignorando a temporada (`:66-67`). A devolução é metade de `amountPaid` (`:72`). As contas do texto conferem: deluxe de 12/03 a 14/03/2027 dá R$ 820,00, e pago e cancelado devolve R$ 410,00.
- **Estadia longa.** Recusa menos de 7 noites (`:79`) e dá 10% sobre o subtotal, que já inclui a temporada (`:86`, `PriceCalculator.java:29-33`). Na conta, 7 × 249,90 = 1.749,30, desconto de 174,93 e total de 1.574,37. Confere.
- **Devoluções.** Convênio e estadia longa herdam o `refund` padrão, que devolve tudo (`:107-109`).
- **`companyCode` nos outros planos.** É ignorado, porque os outros planos não leem o campo.
- **Planos antigos.** `STANDARD` tem preço cheio e devolve o que foi pago. `NON_REFUNDABLE` dá 15% e devolve `cents(0)`, igual à base.

Há um detalhe que o pedido não decide. Como a validação do plano roda antes da busca do quarto (`ReservationService.java:34-38`), um convênio sem código num quarto inexistente responde 400, e não 404.
Conclusão: segue

## Dúvidas

Nenhuma que mude a classe. A validação chamada duas vezes e a constante `NON_REFUNDABLE_DISCOUNT` deixada em `PriceCalculator` são imperfeições pequenas, e as duas passam pelo plano ou servem a ele.
