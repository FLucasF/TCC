# Cenário 03

## Visão geral

O agente aplicou o Observer como o pedido precisava. Criou a interface `ReservationListener` com dois eventos (confirmada e cancelada) e um ouvinte para cada interessado: `GuestNotifier`, `HousekeepingNotifier` e `FinanceNotifier`. O e-mail do hóspede, que já existia na base, também virou ouvinte. O `ReservationService` recebe a lista de ouvintes de fora, percorre essa lista nos dois eventos e não conhece mais nenhum destinatário nem texto de e-mail. A lista é montada em `Hotel`. Não há estrutura a mais fora do pattern. Pela leitura do código, o comportamento confere com o pedido. O único ponto fraco é pequeno. O evento de cancelamento não diz se a reserva estava confirmada antes, então cada ouvinte deduz isso pelo `amountPaid`/`refund` para não avisar quando uma reserva pendente é cancelada. O resultado está certo, mas essa regra fica um pouco escondida dentro dos ouvintes.

## Como foi implementado, e por que o julgamento

**O projeto.** É uma aplicação Javalin com estes pacotes:
- `hotel.rooms`: os quartos;
- `hotel.reservations`: o domínio e o `ReservationService`, o ciclo de vida da reserva;
- `hotel.documents`: o comprovante e o `TextFormat`, que formata dinheiro e datas;
- `hotel.messages`: o `EmailSender`, que registra o envio em `SentMessages`;
- `hotel.reports`: os relatórios de ocupação e receita;
- `hotel.web`: o `HttpApi`, com as rotas.

Uma requisição chega a uma rota do `HttpApi`, passa para o `ReservationService`, e este valida, muda o estado da `Reservation` e grava no `ReservationStore`, que fica em memória. A classe `Hotel` monta todas as peças e sobe o servidor (`final/src/main/java/hotel/Hotel.java:24-37`).

Testes que o agente escreveu ou mudou:
- atualizou os testes que já existiam para a nova assinatura do serviço e para 3 mensagens na confirmação;
- acrescentou em `ReservationServiceTest` testes da confirmação com governança e financeiro, do cancelamento de reserva confirmada, do cancelamento de reserva pendente (ninguém avisado) e do cancelamento de reserva não reembolsável (financeiro não avisado).

**A parte do pattern.**
- A interface `ReservationListener` tem `onReservationConfirmed` e `onReservationCancelled`, os dois recebendo `Reservation` e `Room` (`final/src/main/java/hotel/reservations/ReservationListener.java:6-13`).
- O serviço guarda uma `List<ReservationListener>` recebida no construtor (`final/src/main/java/hotel/reservations/ReservationService.java:15-22`).
- Ele avisa todos os ouvintes depois de `confirmPayment` (`ReservationService.java:66-69`) e depois de `cancel` (`ReservationService.java:88-91`).
- No serviço não sobrou nenhum `EmailSender`, nem endereço, nem texto. O `confirmationEmailBody` e o `HOTEL_NAME` saíram dele.

Cada interessado é uma classe com a sua regra:
- **Hóspede.** O `GuestNotifier` monta o comprovante e o mesmo corpo de e-mail da base (`final/src/main/java/hotel/reservations/GuestNotifier.java:19-38`; compare com `base/src/main/java/hotel/reservations/ReservationService.java:127-131`). No cancelamento ele não faz nada (`GuestNotifier.java:28-31`).
- **Governança.** O `HousekeepingNotifier` manda as datas e o número de hóspedes na confirmação, e as datas livres no cancelamento, desde que algo tenha sido pago (`final/src/main/java/hotel/reservations/HousekeepingNotifier.java:19-36`).
- **Financeiro.** O `FinanceNotifier` manda o valor pago na confirmação, e o reembolso no cancelamento só quando ele é diferente de zero (`final/src/main/java/hotel/reservations/FinanceNotifier.java:19-34`).
- **Montagem.** O registro dos três ouvintes fica em `Hotel` (`Hotel.java:28-33`). Para entrar o restaurante ou o transfer, basta uma classe nova e uma linha nessa lista, sem mexer no serviço.

**O raciocínio.** O desenho é o que as instruções chamam de acerto: uma interface de ouvinte, uma implementação por interessado, o serviço percorrendo uma lista que recebe de fora, e o hóspede também como ouvinte. Não há nenhum dos sinais de malfeito:
- o serviço não monta e-mails;
- o cancelamento não é feito à mão;
- não sobra chamada direta para o hóspede.

O único ponto que afasta um pouco do ideal é que o evento de cancelamento não carrega o status anterior. Por isso a regra "reserva pendente não avisa ninguém" está espalhada nos ouvintes, como uma dedução a partir do valor pago (`HousekeepingNotifier.java:29-31`, `FinanceNotifier.java:28-30`). É um detalhe de qual dado o evento leva, não uma volta ao desenho sem pattern. No exagero colateral não pesou nada: a interface tem três implementações e é justamente a do pattern. No comportamento, conferi as regras pela leitura e elas fecham.

## Item a item

**O pattern pedido.** O `ReservationService` anuncia os dois eventos para uma lista de `ReservationListener` recebida pelo construtor (`final/src/main/java/hotel/reservations/ReservationService.java:15-22`, `:66-69`, `:88-91`) e não conhece nenhum destinatário. Hóspede, governança e financeiro são ouvintes separados (`GuestNotifier.java:8`, `HousekeepingNotifier.java:8`, `FinanceNotifier.java:9`), registrados na montagem em `Hotel.java:29-33`. O caso que já existia, o e-mail do hóspede, também foi para a estrutura. Esse é o Observer que o texto pedia.
Conclusão: acerto

**O exagero colateral.** Não há. As únicas classes novas são a interface do ouvinte e as três implementações. Não há fábrica, builder, camada que só repassa nem interface com uma implementação só.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código, sem subir o serviço nem rodar testes.
- **Confirmação.** O hóspede recebe o e-mail igual ao da base: mesmo assunto, "Reserva confirmada - Pousada Maré Alta", e mesmo corpo (`GuestNotifier.java:21-24`, `:33-38`). A governança recebe em `governanca@pousadamarealta.com.br`, com o assunto "Reserva confirmada - quarto N", as datas no formato do comprovante (`TextFormat.date`) e o número de hóspedes (`HousekeepingNotifier.java:20-24`). O financeiro recebe em `financeiro@pousadamarealta.com.br`, com o assunto "Pagamento recebido - reserva N" e o valor pago em `TextFormat.money` (`FinanceNotifier.java:20-24`). O `amountPaid` já é o total quando os ouvintes rodam (`Reservation.java:95-98`).
- **Cancelamento de reserva confirmada.** A governança recebe "Reserva cancelada - quarto N" com as datas (`HousekeepingNotifier.java:32-35`). O financeiro recebe "Reembolso - reserva N" com o valor a devolver, e só quando o reembolso é diferente de zero. Assim, a tarifa não reembolsável não avisa o financeiro (`FinanceNotifier.java:28-33`, com o reembolso calculado em `ReservationService.java:83-86`).
- **Cancelamento de reserva pendente.** O `amountPaid` vale zero (`Reservation.java:30`). Com isso a governança é pulada (`HousekeepingNotifier.java:29-31`). O reembolso da tarifa padrão é o `amountPaid`, também zero, e o da não reembolsável é zero, então o financeiro também é pulado. Ninguém é avisado, como foi pedido.
- **O resto do sistema.** Nada mais mudou além da assinatura do construtor, que foi ajustada em todos os lugares que o usam.
Conclusão: segue

## Dúvidas

Uma só, que não muda a classe. O evento de cancelamento não informa o status anterior. Os ouvintes deduzem que a reserva estava confirmada pelo `amountPaid > 0` (governança) ou pelo `refund != 0` (financeiro). Hoje isso dá o resultado certo, porque uma reserva confirmada sempre tem total pago positivo. Mas a regra "pendente cancelada não avisa ninguém" fica implícita e repetida em cada ouvinte, em vez de estar no evento ou no serviço.
