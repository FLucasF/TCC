I li o enunciado e todo o código, e já tenho o que preciso para a correção:

# Cenário 03

## Visão geral

O agente resolveu os avisos com um Observer limpo. O `ReservationService` só publica eventos `Confirmed` e `Cancelled` num distribuidor e não sabe quem recebe. O hóspede, a governança e o financeiro são três ouvintes separados de `ReservationListener`, registrados numa lista na montagem (`App`). O e-mail do hóspede, que é o caso que já existia, também virou ouvinte. Pela leitura do código, as regras pedidas estão implementadas como o texto descreve: assuntos, destinatários, recusas e o caso do cancelamento sem reembolso. Não há problema sério no desenho. O único ponto discutível é a interface `MessageSender`, que tem uma implementação só e nada a substitui.

## Como foi implementado, e por que o julgamento

**O projeto.** Ele está dividido em pacotes:

- `domain`: `Reservation`, com o estado, os cálculos e as transições; `Room` e `RoomType`, com a capacidade e a diária; `Season`, a alta temporada; `RatePlan`, com o desconto e o reembolso; `Receipt`, o comprovante e o texto dele; `Formats`, que arredonda e formata; `ErrorCode` e `DomainException`.
- `service`: `ReservationService`, que cria e guarda as reservas numa `ArrayList` em memória e conduz as transições; `RoomCatalog`, com os cinco quartos fixos; `NewReservation`.
- `notification`: o evento, os ouvintes, o distribuidor e a caixa de saída.
- `report`: os dois CSVs.
- `web`: `App`, que monta as peças e as rotas do Javalin, mais os DTOs e o leitor do corpo da requisição.

O `Main` lê a variável `PORT` (7070 se ela não vier) e sobe `new App().create()` (`Main.java:10-11`). Uma requisição como `POST /reservations/{id}/confirmation` segue este caminho: a rota em `web/App.java:62-63` chama `ReservationService.confirm`. Esse método chama `Reservation.confirm()`, que confere o estado e registra o pagamento (`domain/Reservation.java:45-49`), e depois publica o evento (`service/ReservationService.java:62`). O agente escreveu um teste de integração, `AppTest`, que sobe a aplicação na porta 0. Ele cobre os quartos, a alta temporada, o desconto, os três e-mails da confirmação, o cancelamento com e sem aviso, o ciclo de vida, os erros, os conflitos de datas e os relatórios.

**A parte do pattern.** As peças são estas:

- `ReservationEvent` é uma sealed interface com os records `Confirmed` e `Cancelled(reservation, previousStatus)` (`notification/ReservationEvent.java:7-16`).
- `ReservationListener` tem um único método, `on(ReservationEvent)` (`notification/ReservationListener.java:4-7`).
- `ReservationEvents` recebe a lista de ouvintes pelo construtor e entrega o evento a cada um, isolando as falhas (`notification/ReservationEvents.java:12-25`).
- Há três implementações:
  - `GuestConfirmationEmail` monta o e-mail com o comprovante (`notification/GuestConfirmationEmail.java:15-25`);
  - `HousekeepingNotifier` avisa da confirmação e, quando `previousStatus == CONFIRMED`, do cancelamento (`notification/HousekeepingNotifier.java:20-39`);
  - `FinanceNotifier` avisa do pagamento e, quando `refund > 0`, do reembolso (`notification/FinanceNotifier.java:19-29`).

O registro está em `web/App.java:41-46`, numa lista com o comentário "para incluir ou tirar alguém, mexa só nesta lista". O serviço só conhece o `ReservationEvents` (`service/ReservationService.java:18,62,70`) e não monta nem um e-mail.

**O raciocínio.** É o caso descrito como acerto nas instruções: uma interface de ouvinte com uma implementação por interessado, o hóspede incluído, e um serviço que percorre uma lista recebida de fora. Nenhum dos sinais de malfeito aparece. O serviço não monta e-mails, o cancelamento também passa pelos ouvintes e o hóspede não tem chamada direta. Também não é um notificador único, porque cada destino tem a sua classe. Os `switch` por tipo de evento dentro de cada ouvinte (`FinanceNotifier.java:18`, `HousekeepingNotifier.java:19`) servem para o ouvinte escolher a que ele reage. Não é o serviço perguntando qual é o caso concreto, então isso não afasta do pattern. Para incluir o restaurante ou o transfer, basta uma classe nova e uma linha em `App`.

No exagero colateral, pesou a `MessageSender` (`notification/MessageSender.java:3-10`): uma interface com uma implementação só (`MessageOutbox`), que nada substitui, nem os testes. Do resto, nada é supérfluo. `ReservationEvents` e `ReservationEvent` fazem parte do pattern, e `RequestBody` e `Csv` são utilitários que têm uso.

No comportamento, conferi cada regra pedida e todas batem.

## Item a item

**O pattern pedido.** O serviço publica eventos e não conhece os destinos (`service/ReservationService.java:59-72`). Os ouvintes vêm de fora pelo construtor (`notification/ReservationEvents.java:12-14`) e são registrados na montagem (`web/App.java:42-46`). O hóspede (`notification/GuestConfirmationEmail.java:6`), a governança (`notification/HousekeepingNotifier.java:8`) e o financeiro (`notification/FinanceNotifier.java:7`) são implementações separadas de `ReservationListener`. A confirmação e o cancelamento passam pelo mesmo mecanismo, e nenhum e-mail é montado fora dos ouvintes. É o Observer que o texto pedia, aplicado a todos os casos, inclusive o e-mail do hóspede, que já existia.
Conclusão: acerto

**O exagero colateral.** `MessageSender` (`notification/MessageSender.java:3`) é uma interface com uma implementação só, `MessageOutbox` (`notification/MessageOutbox.java:7`). A `App` injeta sempre a mesma instância concreta (`web/App.java:32,43-45`) e nenhum teste usa outra. É pequena e prevê o envio de verdade, mas hoje nada a substitui. Fora isso, não há fábricas, builders nem camadas que só repassam.
Conclusão: exagero

**O comportamento.** A conferência foi só por leitura do código: não subi o serviço, não rodei o Maven e não chamei a API.

- **Confirmação.** `Reservation.confirm` exige PENDING e faz `amountPaid = total` (`domain/Reservation.java:45-49`). O evento só é publicado se a transição passar (`service/ReservationService.java:61-62`). Saem três e-mails:
  - para o hóspede, no endereço dele, com o assunto "Reserva confirmada - Pousada Maré Alta" e o texto igual ao do enunciado, com o comprovante no meio (`GuestConfirmationEmail.java:17-25`, `domain/Receipt.java:22-36`);
  - para `governanca@pousadamarealta.com.br`, com o assunto "Reserva confirmada - quarto N", as datas em dd/MM/aaaa e o número de hóspedes (`HousekeepingNotifier.java:9,22-28`);
  - para `financeiro@pousadamarealta.com.br`, com o assunto "Pagamento recebido - reserva N" e o valor pago como R$ 1.234,56 (`FinanceNotifier.java:8,21-22`, `domain/Formats.java:22-29`).
- **Cancelamento de uma confirmada.** A governança recebe "Reserva cancelada - quarto N", com as datas (`HousekeepingNotifier.java:30-38`). O financeiro recebe "Reembolso - reserva N", com o valor, só quando o reembolso é maior que zero (`FinanceNotifier.java:24-28`). O reembolso é zero no NON_REFUNDABLE (`domain/RatePlan.java:21-23`).
- **Cancelamento de uma pendente.** A governança filtra por `previousStatus == CONFIRMED`. O reembolso é zero porque `amountPaid` ainda é zero (`domain/Reservation.java:23,64`), então ninguém é avisado.
- **O resto do sistema.** Também por leitura:
  - preços: a alta temporada, o desconto e o arredondamento HALF_EVEN estão em `Room.java:17-19`, `Season.java:13-17` e `Reservation.java:38-42`;
  - transições e recusas: `Reservation.java:45-75`;
  - conflitos entre reservas que não foram canceladas: `ReservationService.java:38-44`;
  - erros e status HTTP: `ErrorCode.java` e `App.java:76-77`;
  - relatórios: a ocupação conta quem entra na noite e não quem sai naquele dia, e a receita usa o pago menos o devolvido (`report/OccupancyReport.java:21-30`, `report/RevenueReport.java:20-32`).

Tudo isso confere com o enunciado.
Conclusão: segue

## Dúvidas

A única dúvida é sobre a `MessageSender`. O enunciado diz que "por enquanto" os e-mails não saem de verdade e que o canal "por enquanto" é sempre EMAIL. Por isso, dá para ler a interface como uma costura razoável para o envio que vai existir, e não como exagero. Marquei exagero porque, pelo critério dado, é uma interface com uma implementação só que nada substitui hoje. Mesmo assim, o peso é pequeno.
