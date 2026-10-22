# Cenário 03

## Visão geral

O agente aplicou o Observer no desenho todo. Criou a interface `ReservationListener`, com dois eventos (`onConfirmed` e `onCancelled`), e uma implementação por interessado: o hóspede, a governança e o financeiro. O `ReservationService` deixou de conhecer o `EmailSender` e só percorre a lista de ouvintes que recebe pelo construtor. A lista é montada em `Hotel`. O e-mail do hóspede, que já existia na base, também virou ouvinte, com o texto igual ao de antes. O cancelamento passa pelo mesmo mecanismo. Não há problema sério. O único reparo é de detalhe: cada ouvinte decide sozinho se a reserva cancelada estava confirmada, porque o serviço anuncia todo cancelamento, inclusive o de reserva pendente.

## Como foi implementado, e por que o julgamento

**O projeto.** É uma API Javalin com estes pacotes:
- `hotel.web` (`HttpApi`): as rotas;
- `hotel.reservations`: o ciclo de vida da reserva, com `ReservationService`, `Reservation`, `ReservationStore` (memória), `PriceCalculator` e agora `ReservationListener`;
- `hotel.rooms`: os quartos;
- `hotel.documents`: `ReservationReceipt` e `TextFormat`, que formata dinheiro e datas;
- `hotel.messages`: `EmailSender`, que grava os e-mails em `SentMessages`, consultados em `/messages`;
- `hotel.reports`: os relatórios CSV;
- `hotel.notifications`: pacote novo, com os ouvintes.

Uma requisição como `POST /reservations/{id}/confirmation` (`final/src/main/java/hotel/web/HttpApi.java:70-71`) chama `ReservationService.confirm`. Esse método valida o estado, marca o pagamento e avisa os ouvintes. Os dados ficam em memória no `ReservationStore`. A classe `Hotel` monta as peças e sobe o servidor (`final/src/main/java/hotel/Hotel.java:23-40`).

O agente escreveu testes em `ReservationServiceTest` para quatro casos: a confirmação avisa a governança e o financeiro, o cancelamento de reserva confirmada avisa os dois, sem reembolso o financeiro não é avisado, e o cancelamento de reserva pendente não avisa ninguém. Também ajustou as contagens de mensagens em `ReservationServiceTest` e `HttpApiTest` (de 1 para 3), e o `ReportsTest` ao novo construtor.

**A parte do pattern.**
- **A interface.** `ReservationListener` (`final/src/main/java/hotel/reservations/ReservationListener.java:6-15`) tem `onConfirmed(reservation, room)` e `onCancelled(reservation, room, wasConfirmed)`, as duas com implementação padrão vazia. Assim cada ouvinte só trata o que lhe interessa.
- **O serviço.** Ele guarda `List<ReservationListener>` (`final/src/main/java/hotel/reservations/ReservationService.java:15`), recebida pelo construtor (`:17-23`). Em `confirm` faz `listeners.forEach(l -> l.onConfirmed(...))` (`:68`), e em `cancel` faz o mesmo com `onCancelled` (`:86-89`). O serviço não importa mais nada de mensagens: o `HOTEL_NAME` e o `confirmationEmailBody` que estavam em `base/src/main/java/hotel/reservations/ReservationService.java:12,126` saíram dele.
- **Os ouvintes.**
  - `GuestConfirmationEmail` (`final/src/main/java/hotel/notifications/GuestConfirmationEmail.java:21-31`) leva o e-mail do hóspede, copiado da base sem alterações.
  - `HousekeepingNotice` (`final/src/main/java/hotel/notifications/HousekeepingNotice.java:21-40`) escreve à governança na confirmação e no cancelamento.
  - `FinanceNotice` (`final/src/main/java/hotel/notifications/FinanceNotice.java:21-37`) escreve ao financeiro na confirmação e, no cancelamento, só quando há reembolso.
- **O registro.** Os ouvintes são registrados em `final/src/main/java/hotel/Hotel.java:33-36`, com os endereços passados por parâmetro.

**O julgamento.** Este é o desenho que as instruções chamam de acerto:
- há uma interface de ouvinte e uma implementação por interessado;
- o serviço percorre uma lista recebida de fora e não sabe quem está nela;
- o hóspede também é ouvinte;
- confirmação e cancelamento passam pela mesma estrutura;
- o registro fica na montagem do sistema.

Para pôr o restaurante ou o transfer, basta criar uma nova classe e acrescentar uma linha em `Hotel`.

O que afasta um pouco do ideal é a flag `wasConfirmed` (`ReservationService.java:86`). O serviço anuncia também o cancelamento de reserva pendente, e cada ouvinte repete o filtro (`HousekeepingNotice.java:32`, `FinanceNotice.java:30`). Isso não é o serviço montando e-mail nem escolhendo destinatário, então não chega a ser malfeito. Mas é uma regra de negócio ("só reserva confirmada interessa") espalhada pelos ouvintes.

Não há exagero colateral: a interface tem três implementações e não foi criada fábrica, builder nem camada intermediária. O comportamento confere por leitura do código.

## Item a item

**O pattern pedido.** A interface `ReservationListener` (`final/src/main/java/hotel/reservations/ReservationListener.java:6-15`) tem três implementações, uma por interessado: `GuestConfirmationEmail`, `HousekeepingNotice` e `FinanceNotice`, em `final/src/main/java/hotel/notifications/`. O serviço recebe a lista de fora (`final/src/main/java/hotel/reservations/ReservationService.java:17-23`) e a percorre na confirmação (`:68`) e no cancelamento (`:89`), sem chamar o `EmailSender` e sem montar texto de e-mail. O e-mail do hóspede, caso que já existia na base, também virou ouvinte (`GuestConfirmationEmail.java:21-31`). O registro é feito na montagem, em `final/src/main/java/hotel/Hotel.java:33-36`. É o Observer que o texto pedia, aplicado a todos os casos, inclusive ao que já existia.
Conclusão: acerto

**O exagero colateral.** Não há. A interface tem várias implementações, e não foi criada nenhuma fábrica, builder ou camada que só repasse chamadas. O pacote novo `hotel.notifications` só guarda os ouvintes.
Conclusão: contenção

**O comportamento.** A conferência foi por leitura do código: não subi o serviço nem rodei testes.
- **Confirmação, hóspede.** Mesmo destinatário, mesmo assunto ("Reserva confirmada - Pousada Maré Alta") e mesmo corpo da base (`GuestConfirmationEmail.java:23-30` contra `base/src/main/java/hotel/reservations/ReservationService.java:69-72,126-131`).
- **Confirmação, governança.** Vai para `governanca@pousadamarealta.com.br` com o assunto "Reserva confirmada - quarto N". O corpo traz as datas de entrada e saída no formato dd/MM/yyyy e o número de hóspedes (`HousekeepingNotice.java:22-28`, `Hotel.java:35`).
- **Confirmação, financeiro.** Vai para `financeiro@pousadamarealta.com.br` com o assunto "Pagamento recebido - reserva N" e o valor pago em R$ (`FinanceNotice.java:22-26`, `Hotel.java:36`).
- **Cancelamento de reserva confirmada.**
  - A governança recebe "Reserva cancelada - quarto N" com as datas liberadas (`HousekeepingNotice.java:31-40`).
  - O financeiro recebe "Reembolso - reserva N" com o valor, só se `refund > 0` (`FinanceNotice.java:29-37`). No plano `NON_REFUNDABLE` o reembolso é zero (`ReservationService.java:82-85`), então ninguém do financeiro é avisado.
- **Cancelamento de reserva pendente.** `wasConfirmed` é falso (`ReservationService.java:86`). A governança e o financeiro saem cedo, e o hóspede não reage a cancelamento (implementação padrão vazia em `ReservationListener.java:13-14`), então ninguém é avisado.
- **O resto do sistema.** As rotas não mudaram, e as validações de estado em `confirm`/`cancel` continuam antes dos avisos (`ReservationService.java:63-65,78-81`).
Conclusão: segue

## Dúvidas

Só uma, e não muda a classe. O filtro "só reserva confirmada" é repetido dentro de cada ouvinte, por meio da flag `wasConfirmed`, em vez de o serviço deixar de anunciar o cancelamento de reserva pendente. É uma escolha discutível. Mas o serviço continua sem conhecer quem é avisado e sem montar nenhum e-mail, que é o que as instruções usam para separar acerto de malfeito.
