# Cenário 03

## Visão geral

O agente montou a estrutura do Observer para a equipe. Criou a interface `ReservationListener`, uma família de eventos (`ReservationEvent.Confirmed` / `Cancelled`) e dois ouvintes separados, a governança e o financeiro. Os dois são registrados na montagem (`Hotel`). O serviço percorre a lista de ouvintes, que recebe de fora, tanto na confirmação quanto no cancelamento. Essa parte está boa: o serviço não conhece a governança nem o financeiro, e as regras de cada destino ficam em cada ouvinte. O problema mais sério é que o hóspede não virou ouvinte. O e-mail dele continua sendo mandado à mão dentro de `ReservationService.confirm`, com o texto em `confirmationEmailBody` e o `EmailSender` ainda injetado no serviço. Assim, o serviço continua conhecendo um dos interessados, e é exatamente o caso que as instruções chamam de malfeito. O comportamento pedido confere na leitura do código.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin pequeno, com estes pacotes:
- `hotel.web` (`HttpApi`, com as rotas e os DTOs);
- `hotel.reservations` (`ReservationService` com o ciclo de vida da reserva, `Reservation`, `ReservationStore` em memória, `PriceCalculator`, `RatePlan`, erros);
- `hotel.rooms` (`Rooms`, os quartos fixos);
- `hotel.documents` (`ReservationReceipt`, `TextFormat` para R$ e datas);
- `hotel.messages` (`EmailSender`, que grava em `SentMessages`, exposto em `GET /messages`);
- `hotel.reports` (ocupação e receita);
- e o novo `hotel.notifications`.

Uma requisição como `POST /reservations/{id}/confirmation` (`final/src/main/java/hotel/web/HttpApi.java:70-71`) chama `ReservationService.confirm`, que muda o estado da `Reservation` guardada no `ReservationStore` em memória. A classe `Hotel` monta tudo e sobe o servidor (`final/src/main/java/hotel/Hotel.java:23-35`). O agente escreveu `StaffNotificationsTest`, com quatro casos: confirmação, cancelamento com reembolso, cancelamento sem reembolso e cancelamento de pendente. Também ajustou `HttpApiTest`, que agora espera três mensagens na confirmação, e os construtores de `ReportsTest` e `ReservationServiceTest`, que passam `List.of()`.

**A parte do pattern.**
- `ReservationListener` tem um método só, `on(ReservationEvent)` (`final/src/main/java/hotel/reservations/ReservationListener.java:4-7`).
- `ReservationEvent` é uma interface selada com dois records: `Confirmed(reservation)` e `Cancelled(reservation, wasConfirmed)` (`final/src/main/java/hotel/reservations/ReservationEvent.java:4-15`).
- O serviço recebe a lista no construtor (`final/src/main/java/hotel/reservations/ReservationService.java:26,31`) e a percorre em `publish` (`:132-134`). Publica `Confirmed` depois de confirmar (`:80`) e `Cancelled` depois de cancelar, guardando antes se a reserva estava confirmada (`:98-100`).
- `HousekeepingNotifier` trata os dois eventos. No cancelamento, só avisa se `wasConfirmed` (`final/src/main/java/hotel/notifications/HousekeepingNotifier.java:21-30`).
- `FinanceNotifier` manda "Pagamento recebido" na confirmação e "Reembolso" só quando `refund().signum() > 0` (`final/src/main/java/hotel/notifications/FinanceNotifier.java:21-43`).
- `Hotel` registra os dois ouvintes (`final/src/main/java/hotel/Hotel.java:28-31`). Para incluir o restaurante ou o transfer, basta outro ouvinte nessa lista, sem mexer no serviço.

O hóspede, porém, continua de fora da estrutura. `confirm` monta o comprovante e chama `emailSender.send(...)` direto para o e-mail do hóspede (`final/src/main/java/hotel/reservations/ReservationService.java:75-79`). O texto continua em `confirmationEmailBody`, dentro do serviço (`:140-146`), e o serviço ainda depende de `EmailSender` (`:18,25,30`).

**O raciocínio.** O que aproxima o desenho do pattern é real: há uma interface de ouvinte, uma implementação por interessado da equipe, uma lista injetada pela montagem e os dois eventos (confirmação e cancelamento) passando pela lista. Nada de cancelamento feito à mão, e o serviço não monta os e-mails da governança nem do financeiro. O que afasta o desenho do pattern é que um dos três interessados na confirmação, justamente o que já existia na base, continua com chamada direta no serviço. As instruções dizem que o e-mail do hóspede também é um ouvinte. Dizem também que é malfeito "quando o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes", e é o que se vê aqui: a estrutura convive com o jeito antigo. No exagero colateral, nada pesou. O evento selado e os dois ouvintes fazem parte do pattern, e não há fábrica, builder nem camada que só repassa. No comportamento, as regras conferem na leitura. O único detalhe é que o financeiro não olha `wasConfirmed`: ele depende de o reembolso de uma pendente ser zero, o que o código garante.

## Item a item

**O pattern pedido.** Há um Observer para a equipe: `ReservationListener` (`final/src/main/java/hotel/reservations/ReservationListener.java:4-7`), os eventos `ReservationEvent` (`final/src/main/java/hotel/reservations/ReservationEvent.java:4-15`) e os ouvintes `HousekeepingNotifier` e `FinanceNotifier` (`final/src/main/java/hotel/notifications/HousekeepingNotifier.java:10`, `final/src/main/java/hotel/notifications/FinanceNotifier.java:10`). Eles são registrados em `final/src/main/java/hotel/Hotel.java:28-31` e anunciados em `final/src/main/java/hotel/reservations/ReservationService.java:80,100,132-134`. Mas o e-mail do hóspede, o interessado que a base já tinha, continua sendo mandado à mão no serviço (`final/src/main/java/hotel/reservations/ReservationService.java:76-79`), com o texto em `confirmationEmailBody` (`:140-146`). Por isso o serviço ainda conhece um destino concreto. É o caso de malfeito que as instruções descrevem: o hóspede com chamada direta, e só a governança e o financeiro como ouvintes.
Conclusão: malfeito

**O exagero colateral.** Não há. As classes novas são a interface de ouvinte, os eventos e os dois ouvintes, todas parte do pattern. Não há fábrica, builder, interface sem uso nem camada de repasse. O `Hotel` só ganhou a lista (`final/src/main/java/hotel/Hotel.java:27-31`).
Conclusão: contenção

**O comportamento.** A conferência foi por leitura do código, sem subir o serviço nem rodar testes.
- Na confirmação, o e-mail do hóspede não mudou em relação à base: mesmo assunto e mesmo corpo (`final/src/main/java/hotel/reservations/ReservationService.java:76-79,140-146`).
- A governança recebe "Reserva confirmada - quarto N", com a entrada, a saída e o número de hóspedes formatados por `TextFormat.date` (`final/src/main/java/hotel/notifications/HousekeepingNotifier.java:33-40`).
- O financeiro recebe "Pagamento recebido - reserva N", com o valor pago em R$ (`final/src/main/java/hotel/notifications/FinanceNotifier.java:29-34`).
- No cancelamento de uma reserva confirmada, a governança recebe "Reserva cancelada - quarto N" com as datas (`final/src/main/java/hotel/notifications/HousekeepingNotifier.java:42-47`). O financeiro recebe "Reembolso - reserva N" com o valor, só se o reembolso for maior que zero, então a tarifa não reembolsável não avisa (`final/src/main/java/hotel/notifications/FinanceNotifier.java:36-42`).
- Uma reserva pendente cancelada não avisa ninguém. A governança filtra por `wasConfirmed` (`final/src/main/java/hotel/notifications/HousekeepingNotifier.java:25`). No financeiro, o reembolso da pendente é `amountPaid`, que vale zero antes da confirmação (`final/src/main/java/hotel/reservations/Reservation.java:30`, `final/src/main/java/hotel/reservations/ReservationService.java:94-96`), e portanto nada sai.
- O resto do sistema segue igual. Só mudou a assinatura do construtor, e os testes antigos foram ajustados para ela.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe: o desenho cai exatamente no caso de malfeito "hóspede com chamada direta" das instruções. Há uma ressalva menor: o `FinanceNotifier` não consulta `wasConfirmed` e depende de uma regra de outro lugar (pendente tem `amountPaid` zero) para não avisar no cancelamento de pendente. Hoje o resultado está certo, mas é um acoplamento implícito.
