# Cenário 03

## Visão geral

O agente aplicou o Observer. Criou uma interface de ouvinte com dois eventos (confirmada e cancelada) e escreveu uma implementação para cada interessado: o hóspede, a governança e o financeiro. O e-mail do hóspede, que antes ficava dentro do serviço, também virou um ouvinte. O `ReservationService` agora só anuncia os dois eventos a uma lista de ouvintes que recebe pronta, e essa lista é montada em `Hotel`. Para entrar o restaurante ou o transfer, basta escrever uma classe nova e acrescentar uma linha em `Hotel`, sem mexer no serviço. O ponto mais fraco é pequeno. O evento de cancelamento é anunciado também quando a reserva ainda estava pendente. Cada ouvinte é que descobre se a reserva tinha sido confirmada: a governança olha `amountPaid > 0` e o financeiro olha `refund > 0`. A regra "pendente cancelada não avisa ninguém" fica assim espalhada pelos ouvintes, em vez de estar no evento. O comportamento confere com o pedido.

## Como foi implementado, e por que o julgamento

**O projeto.** É uma aplicação Javalin. Os pacotes ficam em `hotel/`:
- `rooms`: os quartos;
- `reservations`: o modelo `Reservation`, o `ReservationService`, que cuida do ciclo de vida da reserva, o `PriceCalculator` e o `ReservationStore`, que guarda os dados em memória;
- `documents`: o comprovante (`ReservationReceipt`) e a formatação de valores e datas (`TextFormat`);
- `messages`: o `EmailSender`, que grava as mensagens em `SentMessages`, um registro consultável por `/messages`;
- `reports`: os relatórios;
- `web`: o `HttpApi`, com as rotas.

Uma requisição como `POST /reservations/{id}/confirmation` passa pelo `HttpApi` e chega ao `ReservationService.confirm`, que muda o estado da reserva no store. A classe `Hotel` monta todas as peças e sobe o servidor (`final/src/main/java/hotel/Hotel.java:23-37`).

A mudança criou o pacote `hotel.notifications`, com cinco arquivos:
- `ReservationEventListener`, a interface com `onReservationConfirmed` e `onReservationCancelled` (`final/src/main/java/hotel/notifications/ReservationEventListener.java:9-11`);
- `ReservationEventNotifier`, que guarda a lista de ouvintes, oferece `addListener` e percorre a lista nos dois eventos (`final/src/main/java/hotel/notifications/ReservationEventNotifier.java:11-27`);
- `GuestNotifier`, `HousekeepingNotifier` e `FinanceNotifier`, um para cada interessado.

O serviço deixou de receber o `EmailSender` e passou a receber o `ReservationEventNotifier` (`final/src/main/java/hotel/reservations/ReservationService.java:15-24`). O `confirm` apenas anuncia o evento (`ReservationService.java:67`), e o `cancel` anuncia depois de calcular o reembolso (`ReservationService.java:86`). Os ouvintes são registrados em `Hotel.java:28-31`, e os endereços da governança e do financeiro entram ali, pelo construtor.

Os testes do agente:
- adaptaram `ReservationServiceTest`, `ReportsTest` e `HttpApiTest` à nova montagem;
- conferem os três e-mails da confirmação (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:89`);
- conferem o cancelamento de uma reserva confirmada STANDARD, com a governança e o financeiro (`ReservationServiceTest.java:177`);
- conferem o cancelamento de uma NON_REFUNDABLE, só com a governança (`ReservationServiceTest.java:203`);
- conferem o cancelamento de uma pendente, sem nenhum e-mail (`ReservationServiceTest.java:218`).

**A parte do pattern.** Cada interessado é uma classe que implementa `ReservationEventListener`, e cada regra mora no seu ouvinte:
- O hóspede: o texto que antes estava em `base/src/main/java/hotel/reservations/ReservationService.java:69-71` e `:126-132` foi movido inteiro para `final/src/main/java/hotel/notifications/GuestNotifier.java:20-26` e `:34-40`. No cancelamento o hóspede não faz nada (`GuestNotifier.java:29-31`).
- A governança: o e-mail da confirmação traz o quarto, as datas e o número de hóspedes (`HousekeepingNotifier.java:21-28`). O do cancelamento traz o quarto e as datas, e só é enviado se `amountPaid > 0` (`HousekeepingNotifier.java:31-39`).
- O financeiro: "Pagamento recebido", com o valor pago (`FinanceNotifier.java:20-26`). "Reembolso" só sai se `refund > 0` (`FinanceNotifier.java:29-36`).

O serviço não escolhe nenhum destino: percorre a lista e nada mais. Não sobrou montagem de e-mail no serviço; o `HOTEL_NAME` e o `confirmationEmailBody` foram retirados dele.

**Por que acerto.** O desenho é o que as instruções pedem. Há uma interface de ouvinte, uma implementação por interessado, inclusive o hóspede, e o serviço percorre uma lista que recebe de fora, montada em `Hotel`. A confirmação e o cancelamento passam os dois pela lista, e o hóspede não tem chamada direta. Não aparece nenhum dos sinais de malfeito ou de omissão: o serviço não monta e-mail da governança nem do financeiro, o cancelamento não envia à mão, e não há um notificador único que conheça os destinos. O `ReservationEventNotifier` só guarda e percorre a lista. Não sabe quem são os ouvintes nem o que fazem.

O que afasta um pouco do ideal é a condição "a reserva estava confirmada". O evento `onReservationCancelled` é disparado para qualquer cancelamento, e a governança deduz o estado anterior por `amountPaid` (`HousekeepingNotifier.java:32`). Um ouvinte novo, como o transfer, teria de repetir essa dedução. Isso não chega a ser o serviço perguntando qual é o caso concreto, mas é conhecimento do evento que vazou para os ouvintes.

**Exagero.** Nada de fora do pattern. O `ReservationEventNotifier` é concreto e faz o papel da lista de ouvintes do sujeito, que é parte do próprio pattern. Não há fábrica, builder nem interface sobrando.

**Comportamento.** A conferência foi por leitura, e as regras pedidas estão ali, como mostra o item abaixo.

## Item a item

**O pattern pedido.** A interface `ReservationEventListener` (`final/src/main/java/hotel/notifications/ReservationEventListener.java:7-12`) tem três implementações, uma por interessado: `GuestNotifier`, `HousekeepingNotifier` e `FinanceNotifier`. Elas são registradas na montagem (`final/src/main/java/hotel/Hotel.java:28-31`). O serviço anuncia os eventos sem conhecer os destinos (`final/src/main/java/hotel/reservations/ReservationService.java:67` e `:86`), e o e-mail do hóspede também virou ouvinte (`final/src/main/java/hotel/notifications/GuestNotifier.java:19-26`). É o Observer que o texto pedia, aplicado também ao caso que já existia. A ressalva: a regra "só avisa se a reserva estava confirmada" é deduzida dentro do ouvinte (`HousekeepingNotifier.java:32`), e não vem expressa no evento.
Conclusão: acerto

**O exagero colateral.** Não há. As classes novas são o ouvinte, o registro de ouvintes e um ouvinte por interessado. Não surgiram fábricas, builders, camadas de repasse nem interfaces sem uso.
Conclusão: contenção

**O comportamento.** A conferência foi por leitura do código; não subi o serviço nem rodei testes, como pede esta correção.
- **E-mail do hóspede:** destinatário, assunto e corpo são idênticos aos da base. Compare `GuestNotifier.java:22-25` e `:34-40` com `base/src/main/java/hotel/reservations/ReservationService.java:69-71` e `:126-132`.
- **Confirmação, governança:** vai para `governanca@pousadamarealta.com.br`, com o assunto "Reserva confirmada - quarto N", as datas no formato `TextFormat.date` e o número de hóspedes (`HousekeepingNotifier.java:22-27`).
- **Confirmação, financeiro:** vai para `financeiro@pousadamarealta.com.br`, com o assunto "Pagamento recebido - reserva N" e o valor pago por `TextFormat.money` (`FinanceNotifier.java:21-25`).
- **Cancelamento de reserva confirmada, governança:** "Reserva cancelada - quarto N", com as datas (`HousekeepingNotifier.java:33-38`).
- **Cancelamento de reserva confirmada, financeiro:** "Reembolso - reserva N", com o valor, e só se `refund > 0` (`FinanceNotifier.java:30-35`). Na tarifa NON_REFUNDABLE o reembolso é zero (`ReservationService.java:83`), então o financeiro não é avisado.
- **Cancelamento de reserva pendente:** não avisa ninguém. Numa pendente, `amountPaid` é zero (`final/src/main/java/hotel/reservations/Reservation.java:30`) e o reembolso também, o que barra a governança e o financeiro; o hóspede não faz nada no cancelamento.
- **O resto do sistema:** as rotas, os relatórios e o comprovante não foram alterados. Os testes existentes só foram adaptados à nova montagem e à nova contagem de e-mails.
Conclusão: segue

## Dúvidas

A governança decide se avisa do cancelamento olhando `amountPaid > 0`, e não o estado da reserva antes do cancelamento. Isso equivale a "estava confirmada" enquanto toda reserva confirmada tiver total positivo. Se alguma tarifa ou desconto puder levar o total a zero, uma reserva confirmada cancelada não avisaria a governança. A leitura não mostrou se isso é possível. De todo modo, não muda a classe do desenho.
