# Cenário 03

## Visão geral

O agente montou um Observer de verdade. Criou a interface `ReservationListener`, com um evento para a confirmação e outro para o cancelamento de reserva confirmada. Cada interessado virou uma classe: hóspede, governança e financeiro. O `ReservationService` só percorre a lista de ouvintes e não monta mais nenhum e-mail. O e-mail do hóspede também virou ouvinte, com o texto idêntico ao da base. As regras pedidas estão certas. O problema mais sério está no registro dos ouvintes. A classe de montagem `Hotel` não foi alterada e usa um construtor de conveniência do próprio serviço, que chama `DefaultListeners.create`. Com isso, o pacote `reservations` passa a depender do pacote `notifications`, e quem decide a lista em produção é o serviço, não a montagem. Isso desvia do que as instruções pedem, mas o serviço continua sem citar nenhum interessado concreto.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin. `hotel.Hotel` monta as peças: `Rooms`, `ReservationStore`, `SentMessages`, `EmailSender`, `ReservationService`, os relatórios e a `HttpApi`. `HttpApi` expõe as rotas e delega ao `ReservationService`, que guarda as regras do ciclo de vida da reserva (criar, confirmar, cancelar, check-in, check-out e segunda via do comprovante). Os dados ficam em memória no `ReservationStore`. Os e-mails "enviados" ficam guardados em `SentMessages` e aparecem em `/messages`. O agente criou o pacote `hotel.notifications` com `GuestNotifier`, `HousekeepingNotifier`, `FinanceNotifier` e `DefaultListeners`, e criou a interface `hotel.reservations.ReservationListener`. Também escreveu testes novos em `ReservationServiceTest`: confirmação avisa governança e financeiro, cancelamento de confirmada avisa os dois, sem reembolso o financeiro não recebe nada, e cancelar reserva pendente não avisa ninguém. Por fim, ajustou as contagens de mensagens em `ReservationServiceTest` e `HttpApiTest`.

**A parte do pattern.**
- **A interface.** `ReservationListener` (`final/src/main/java/hotel/reservations/ReservationListener.java:9-18`) tem dois métodos `default` vazios: `onConfirmed(Reservation, Room)` e `onConfirmedReservationCancelled(Reservation, Room)`.
- **O serviço.** O `ReservationService` guarda um `List<ReservationListener>` (`final/src/main/java/hotel/reservations/ReservationService.java:17`), recebido no construtor principal (`ReservationService.java:24-33`). Em `confirm`, só avisa todos (`ReservationService.java:76-77`). Em `cancel`, avisa todos apenas se a reserva estava `CONFIRMED` (`ReservationService.java:95-100`). Não há `if` por interessado nem e-mail montado no serviço: `confirmationEmailBody` e `HOTEL_NAME` saíram do serviço.
- **Os ouvintes.** Cada regra mora no seu ouvinte:
  - o comprovante do hóspede em `GuestNotifier.java:21-31`;
  - governança na confirmação e no cancelamento em `HousekeepingNotifier.java:21-40`;
  - financeiro em `FinanceNotifier.java:21-39`, com a regra "só se há algo a devolver" dentro dele (`FinanceNotifier.java:31-33`).
- **O registro.** A lista fica em `DefaultListeners.create` (`final/src/main/java/hotel/notifications/DefaultListeners.java:13-18`). Quem chama esse método é o construtor de conveniência do serviço (`ReservationService.java:19-22`, com o import em `ReservationService.java:5`), e não o `Hotel`. O `Hotel` continua idêntico à base e passa um `EmailSender` (`final/src/main/java/hotel/Hotel.java:23-24`).

**O raciocínio.**
- **O que aproxima do acerto.** Há um ouvinte por interessado, o hóspede incluído. Há eventos para os dois momentos, a confirmação e o cancelamento. O serviço só percorre a lista e é possível injetar outra lista pelo construtor principal. Para incluir o restaurante, basta criar uma classe e acrescentá-la a uma lista, sem tocar no serviço.
- **O que afasta.** O registro não está na montagem. O serviço chama `DefaultListeners`, que conhece as classes concretas, e isso cria um ciclo entre pacotes (`reservations` → `notifications` → `reservations`).
- **Por que acerto mesmo assim.** Nenhum dos casos de malfeito das instruções se aplica: o serviço não monta e-mail de ninguém, o cancelamento também passa pelos ouvintes e o hóspede não tem chamada direta. Por isso o julgamento é acerto, com essa ressalva registrada em "Dúvidas".
- **Exagero colateral.** `DefaultListeners` é exatamente o ponto de registro da lista, ainda que no lugar errado, e não uma camada sem função. Não vi outra estrutura além do necessário.
- **Comportamento.** Assuntos, endereços, conteúdo e as regras de "quem é avisado quando" conferem com o pedido.

## Item a item

**O pattern pedido.** Há uma interface de ouvinte, `ReservationListener.java:9-18`, com três implementações, uma por interessado: `GuestNotifier.java:10`, `HousekeepingNotifier.java:10` e `FinanceNotifier.java:10`. O serviço só percorre a lista na confirmação (`ReservationService.java:76-77`) e no cancelamento de reserva confirmada (`ReservationService.java:95-100`), e não sabe quem recebe o quê. O e-mail do hóspede também é ouvinte (`GuestNotifier.java:21-31`), e a regra do reembolso zero fica no ouvinte do financeiro (`FinanceNotifier.java:31-33`). Isso é o Observer que o texto pedia. A ressalva é que a lista vem de `DefaultListeners`, chamado pelo construtor de conveniência do serviço (`ReservationService.java:19-22`), e não da montagem em `Hotel.java:23-24`.
Conclusão: acerto

**O exagero colateral.** Não há. `DefaultListeners` (`DefaultListeners.java:8-19`) concentra a lista de interessados. Ela está mal colocada (deveria estar em `Hotel`), mas cumpre uma função real. Os métodos `default` da interface também se justificam: cada ouvinte implementa só os eventos que lhe interessam.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código, sem rodar nada:
- **Confirmação, hóspede.** O assunto "Reserva confirmada - Pousada Maré Alta" e o corpo ficam idênticos aos da base (`GuestNotifier.java:23-31` contra `base/src/main/java/hotel/reservations/ReservationService.java:71,126-132`).
- **Confirmação, governança.** Vai para `governanca@pousadamarealta.com.br` com o assunto "Reserva confirmada - quarto N", as datas de entrada e saída em dd/MM/yyyy e o número de hóspedes (`HousekeepingNotifier.java:22-29`).
- **Confirmação, financeiro.** Vai para `financeiro@pousadamarealta.com.br` com o assunto "Pagamento recebido - reserva N" e o valor pago em `R$ x.xxx,xx` (`FinanceNotifier.java:22-26`).
- **Cancelamento de reserva confirmada, governança.** Assunto "Reserva cancelada - quarto N", com as datas (`HousekeepingNotifier.java:33-39`).
- **Cancelamento de reserva confirmada, financeiro.** Assunto "Reembolso - reserva N", com o valor a devolver, enviado só se `refund > 0` (`FinanceNotifier.java:31-38`). O reembolso já está gravado antes dos avisos, porque `reservation.cancel(refund)` roda em `ReservationService.java:96`, antes de `ReservationService.java:99`.
- **Cancelamento de reserva pendente.** Não avisa ninguém (`ReservationService.java:95-97`).
- **O resto do sistema.** Não muda: o construtor antigo com `EmailSender` continua existindo (`ReservationService.java:19-22`), então `Hotel` e `ReportsTest` seguem compilando sem alteração.

Tudo confere com o pedido.
Conclusão: segue

## Dúvidas

- **O registro fora da montagem.** As instruções dizem que os ouvintes devem ser "registrados na montagem do sistema" e que o serviço os recebe "de fora". Aqui o serviço até aceita a lista de fora, pelo construtor de `ReservationService.java:24-28`, mas a montagem real (`Hotel.java:23-24`) usa o construtor que monta a lista lá dentro, via `DefaultListeners` (`ReservationService.java:21`). O serviço não cita nenhum interessado concreto, mas depende do pacote que os conhece.
- **Por que não malfeito.** Esse desvio não corresponde a nenhum dos casos de malfeito descritos, que são o serviço montando e-mails, o cancelamento feito à mão ou o hóspede com chamada direta. Por isso fiquei com acerto. Quem der peso maior à regra "registrado na montagem" pode ler o caso como malfeito.
