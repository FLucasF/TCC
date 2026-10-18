# Cenário 03

## Visão geral

O agente resolveu os avisos da equipe com um Observer completo. O serviço de reservas recebe de fora uma lista de `ReservationListener`. Na confirmação e no cancelamento de uma reserva confirmada, ele percorre essa lista e não sabe quem está nela. O e-mail do hóspede, a governança e o financeiro são três ouvintes separados, registrados no `Hotel`, a classe que monta as peças. Fora dessa parte não há estrutura sobrando: são classes concretas, sem interfaces de uma implementação só, sem fábricas e sem camadas que só repassam. Nos testes pela API, todas as regras do fim do enunciado e da parte comum deram o resultado pedido. Não encontrei problema sério.

## Item a item

**O enunciado (pede o pattern).** A interface de ouvinte é `src/main/java/hotel/reservations/ReservationListener.java:6`. Ela tem dois eventos, `confirmed` (linha 9) e `cancelled` (linha 13), com corpo vazio por padrão, e assim cada interessado implementa só o evento que lhe interessa. O `ReservationService` recebe a lista pelo construtor (`src/main/java/hotel/reservations/ReservationService.java:15-22`). Na confirmação, ele só avisa os ouvintes (`ReservationService.java:67`). No cancelamento, avisa os ouvintes apenas se a reserva estava confirmada (`ReservationService.java:81`, `:87-90`). Isso respeita a regra de que uma reserva pendente cancelada não avisa ninguém.

O serviço não monta nenhum e-mail nem conhece endereço algum. Os únicos lugares que chamam `EmailSender` são os três ouvintes e a montagem:
- o hóspede é um ouvinte (`src/main/java/hotel/notifications/GuestConfirmationEmail.java:10`, envio na linha 23);
- a governança trata a confirmação e o cancelamento (`src/main/java/hotel/notifications/HousekeepingNotice.java:10`, `:21-34`);
- o financeiro trata os dois eventos, e a regra "só avisa se houver reembolso" fica dentro dele (`src/main/java/hotel/notifications/FinanceNotice.java:10`, `:29`).

O registro está em `src/main/java/hotel/Hotel.java:28-31`. Para pôr o restaurante ou o transfer, basta escrever uma classe nova e acrescentar uma linha ali, sem mexer no serviço. A confirmação e o cancelamento passam pelos ouvintes, e o hóspede também. É o pattern que o enunciado precisava.
Conclusão: acerto

**O exagero colateral.** Não há.
- `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:4`) é uma classe concreta e pequena que grava na lista de enviadas, não uma interface.
- `PriceCalculator` (`src/main/java/hotel/reservations/PriceCalculator.java:14`) é concreto e usa um `switch` simples para o plano de tarifa (linhas 26-29).
- `ReservationStore`, `Rooms`, os relatórios e `HttpApi` são classes diretas.
- Os records `ReservationResponse`, `CreateReservationRequest` e `ReservationReceipt` são o formato do JSON pedido, não uma camada a mais.

Não vi fábrica, builder próprio, interface de implementação única ou camada que só repassa.
Conclusão: contenção

**O comportamento.** Rodei o jar gerado (`target/hotel.jar`) na porta 7070 e testei com `curl`.
- **Confirmação da reserva 2** (quarto 201, 10 a 12/03/2027, 2 hóspedes): saíram 3 e-mails.
  - O do hóspede tem o assunto "Reserva confirmada - Pousada Maré Alta" e o texto exato do enunciado, com o comprovante (R$ 779,80).
  - A governança recebeu "Reserva confirmada - quarto 201", com as datas 10/03/2027 e 12/03/2027 e "Hóspedes: 2".
  - O financeiro recebeu "Pagamento recebido - reserva 2", com "pagamento de R$ 779,80".
- **Cancelamento da reserva 1** (STANDARD, já confirmada): a governança recebeu "Reserva cancelada - quarto 101" com 10/07/2027 a 12/07/2027. O financeiro recebeu "Reembolso - reserva 1" com "Devolver R$ 599,76". A resposta veio com `refund: 599.76`.
- **Cancelamento da reserva 3** (NON_REFUNDABLE, confirmada): só a governança foi avisada, e `refund` veio `0.00`.
- **Cancelamento da reserva 5** (pendente): nenhuma mensagem nova.
- **Formatação no e-mail:** a reserva 4 mostrou R$ 1.403,64, "Desconto: R$ 210,55" e R$ 1.193,09. A reserva 3 mostrou "1 diária".
- **Parte comum:**
  - Preços: o STANDARD de 10 a 12/07/2027 deu 599.76, como no enunciado. O NON_REFUNDABLE da suíte deu desconto de 104.38 e total de 591.50; os 15% do 202 deram 210.55 (210,546 arredondado), conforme a conta.
  - Recusas: noite já ocupada deu 409 ROOM_UNAVAILABLE; capacidade, 422 CAPACITY_EXCEEDED; quarto inexistente, 404; saída igual à entrada, 0 hóspedes, corpo que não é JSON e campo faltando deram 400 INVALID_REQUEST.
  - Situação da reserva: confirmar de novo, cancelar depois do check-in, fazer check-out antes do check-in e pedir a segunda via de uma reserva cancelada deram 409 OPERATION_NOT_ALLOWED. O check-out devolveu o comprovante.
  - Relatórios em `text/csv; charset=utf-8`: na ocupação, `total,1/5`, e quem sai no dia não conta. O financeiro de 2027-07 inclui a NON_REFUNDABLE cancelada (591.50) e fecha com `total,1784.59`.
  - Mês ou data fora do formato deram 400. O quarto liberado pelo cancelamento pôde ser reservado de novo.
- **Testes do agente:** `mvn -q verify` passou, com 29 testes e nenhuma falha (TeamNoticesTest 3, PriceCalculatorTest 6, HttpApiTest 4, ReportsTest 5, ReservationServiceTest 11). Depois disso o serviço foi derrubado.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Um detalhe menor que não muda a conclusão: uma reserva pendente cancelada fica com `refund: 0.00`, e não `null`. O enunciado só diz que é `null` "enquanto não for cancelada", e 0,00 corresponde a "nada a devolver".
