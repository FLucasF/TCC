# Cenário 03

## Visão geral

O agente resolveu os avisos com um Observer completo. O `ReservationService` só anuncia dois eventos, a confirmação e o cancelamento, por um `EventDispatcher` que recebe pronto pelo construtor. Os cinco interessados são ouvintes separados, todos registrados no `Main`: o e-mail do hóspede, a governança e o financeiro na confirmação, e a governança e o financeiro no cancelamento. O serviço não monta nenhum e-mail nem sabe quem é avisado, e o hóspede também é um ouvinte, sem chamada direta. O desenho do pattern está certo. Os problemas estão no comportamento do resto do sistema:

- o conflito de datas recusa reservas que encostam uma na outra;
- dá para cancelar uma reserva que já fez check-in;
- o arredondamento é "meio para cima", e não "meio para o par";
- o relatório financeiro inclui reservas canceladas que não deixaram dinheiro;
- o dinheiro sai sem separador de milhar ("R$ 1159,80");
- a segunda via sai para reserva cancelada.

O projeto também não tem nenhum teste automatizado.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote raiz tem o `Main`, que monta tudo à mão: os serviços, o `MessageStore`, o `EventDispatcher` e os controllers. Ele registra os ouvintes, declara as rotas do Javalin e lê a porta de `PORT`, com 7070 como padrão (`Main.java:25-58`, `Main.java:62-72`). Os pacotes são estes:

- `api`: os controllers, um por recurso, e os DTOs de entrada e saída. `ReservationController` faz o parse do corpo, valida e traduz `ReservationException` em status HTTP (`api/ReservationController.java:134-144`).
- `service`: `ReservationService` (as regras de estado, a disponibilidade e a criação, com os dados num `HashMap` em memória, `service/ReservationService.java:17`), `PricingService` (a diária, a alta temporada, o desconto e o arredondamento), `RoomService` (os cinco quartos num `Map` fixo, `service/RoomService.java:9-15`) e `ReportService` (os dois CSVs).
- `domain`: `Reservation`, uma classe imutável com métodos `withX`, os enums e os records `Room` e `Message`.
- `notification`: o mecanismo de avisos e o `MessageStore`, que é a lista de mensagens enviadas.
- `util`: o `ComprovantFormatter`, que monta o texto do comprovante.

Uma requisição segue este caminho: a rota no `Main` chama o método do controller, que chama o serviço. O serviço lê e grava o mapa e, na confirmação e no cancelamento, dispara um evento. O controller converte o resultado num DTO e responde. O jar é gerado pelo `maven-shade-plugin` com `mainClass` `com.pousadamarealta.Main` (`pom.xml:48-59`).

**Testes:** não há nenhum. Não existe `src/test`, o `pom.xml` não declara JUnit e o único artefato de verificação é um script de demonstração com `curl` (`test-demo.sh`). Isso contradiz a premissa de que o projeto "passou nos testes do próprio agente".

**A parte que pede o pattern.** As peças são estas:

- `ReservationEventListener`, a interface do ouvinte, com um único método `onReservationEvent(ReservationEvent)` (`notification/ReservationEventListener.java:5-7`).
- `ReservationEvent`, uma interface selada com dois records: `ReservationConfirmedEvent(reservation)` e `ReservationCancelledEvent(reservation, previousStatus)` (`notification/event/ReservationEvent.java:5`, `ReservationCancelledEvent.java:6-9`).
- `EventDispatcher`, que guarda a lista de ouvintes, com `subscribe` e `unsubscribe`, e a percorre em `dispatch` (`notification/EventDispatcher.java:8-22`).
- Os ouvintes, um por interessado e por evento:
  - `GuestConfirmationNotifier`, o e-mail do hóspede com o comprovante (`notification/GuestConfirmationNotifier.java:21-41`);
  - `GovernanceConfirmationNotifier` (`notification/GovernanceConfirmationNotifier.java:22-43`);
  - `FinancialConfirmationNotifier` (`notification/FinancialConfirmationNotifier.java:20-35`);
  - `GovernanceCancellationNotifier` (`notification/GovernanceCancellationNotifier.java:23-47`);
  - `FinancialCancellationNotifier` (`notification/FinancialCancellationNotifier.java:21-46`).

  Cada ouvinte filtra com `instanceof` o evento que lhe interessa, monta a própria mensagem e a grava no `MessageStore`.
- O registro dos ouvintes fica no `Main` (`Main.java:33-37`). O serviço recebe o dispatcher pelo construtor (`Main.java:30`, `service/ReservationService.java:23-27`).

No serviço, `confirmReservation` só muda o estado e chama `eventDispatcher.dispatch(new ReservationConfirmedEvent(updated))` (`service/ReservationService.java:103-108`). `cancelReservation` calcula o reembolso e dispara `ReservationCancelledEvent(updated, previousStatus)` (`service/ReservationService.java:154-159`). As regras de "quem é avisado quando" ficam todas nos ouvintes:

- a governança e o financeiro ignoram o cancelamento de reserva que não estava paga (`notification/GovernanceCancellationNotifier.java:27-29`, `notification/FinancialCancellationNotifier.java:25-27`);
- o financeiro também ignora o cancelamento sem nada a devolver (`notification/FinancialCancellationNotifier.java:29-31`).

**O raciocínio.** É o desenho que as instruções chamam de acerto:

- há uma interface de ouvinte e uma implementação por interessado;
- o serviço percorre, pelo dispatcher, uma lista que recebe de fora, montada na classe que monta o sistema;
- o hóspede também é um ouvinte;
- o serviço não tem nenhum endereço, assunto ou texto de e-mail.

Não sobrou nada feito à mão: o cancelamento também passa pelos ouvintes, e não existe "notificador" central que conheça os destinos. Para incluir o restaurante ou o transfer, basta uma classe nova e uma linha no `Main`. O filtro por `instanceof` em cada ouvinte é uma escolha de granularidade, com uma interface só para todos os eventos. Não afasta do pattern, porque quem faz a escolha é o ouvinte, e não o sujeito.

No exagero colateral não vi estrutura sobrando. O `EventDispatcher` e a hierarquia de eventos fazem parte do pattern. Os serviços são classes concretas e não há interfaces com uma implementação só, fábricas ou builders. O que existe é o contrário, duplicação:

- o tipo do quarto é resolvido por um `switch` no número do quarto em dois lugares, ignorando o `RoomService` (`util/ComprovantFormatter.java:47-53`, `api/dto/DocumentResponse.java:49-56`);
- `RoomType` repete a capacidade e a diária que já estão em `Room` (`domain/RoomType.java:4-6`).

Isso é desleixo, não exagero.

No comportamento, os avisos estão certos, mas há várias regras do resto do sistema implementadas errado, detalhadas no item abaixo.

## Item a item

**O pattern pedido.** O serviço anuncia `ReservationConfirmedEvent` e `ReservationCancelledEvent` por um `EventDispatcher` injetado (`service/ReservationService.java:108`, `service/ReservationService.java:159`, `service/ReservationService.java:23-27`). Esse dispatcher percorre a lista de `ReservationEventListener` (`notification/EventDispatcher.java:18-22`). Cada interessado é uma classe própria, incluindo o hóspede (`notification/GuestConfirmationNotifier.java:10`), e todas são registradas no `Main` (`Main.java:33-37`). O serviço não monta nenhum e-mail nem conhece destinatários, e o cancelamento também passa pelos ouvintes. É o Observer que o texto pede, aplicado a todos os casos, inclusive ao e-mail do hóspede, que já existia antes da parte nova.
Conclusão: acerto

**O exagero colateral.** Não há. Não existe interface com uma implementação só, fábrica, builder ou camada que só repassa. A sobra que existe é duplicação, não estrutura: o `switch` de tipo de quarto aparece duas vezes (`util/ComprovantFormatter.java:47-53`, `api/dto/DocumentResponse.java:49-56`) e `RoomType` repete dados de `Room` (`domain/RoomType.java:4-6`). O `unsubscribe` sem uso (`notification/EventDispatcher.java:14-16`) é parte natural do pattern e casa com o pedido de que os interessados "entram e saem".
Conclusão: contenção

**O comportamento.** A conferência foi só por leitura do código: não subi o serviço, não rodei o Maven e não chamei a API.

Os avisos conferem:
- Na confirmação saem três e-mails. O do hóspede tem o assunto e o texto pedidos, com o comprovante (`notification/GuestConfirmationNotifier.java:24-38`). O da governança vai para `governanca@...`, com o assunto "Reserva confirmada - quarto N", as datas e os hóspedes (`notification/GovernanceConfirmationNotifier.java:24-40`). O do financeiro vai para `financeiro@...`, com o assunto "Pagamento recebido - reserva N" e o valor (`notification/FinancialConfirmationNotifier.java:22-32`).
- No cancelamento de reserva confirmada sai o e-mail da governança, com o assunto "Reserva cancelada - quarto N" e as datas (`notification/GovernanceCancellationNotifier.java:31-44`). Sai também o do financeiro, com "Reembolso - reserva N", só se o reembolso for maior que zero (`notification/FinancialCancellationNotifier.java:29-43`).
- Reserva pendente cancelada não gera aviso.

O resto do sistema tem desvios:
1. **Conflito de datas.** `hasConflict` usa `!checkOut.isBefore(existing.checkIn)`, então uma reserva que sai no mesmo dia em que outra entra é recusada com `ROOM_UNAVAILABLE` (`service/ReservationService.java:174`). Exemplo: 10–12/07 e depois 12–14/07 no mesmo quarto. Isso contradiz "na mesma noite".
2. **Cancelamento após o check-in.** O código só recusa `CHECKED_OUT` e `CANCELLED`, então cancelar uma reserva `CHECKED_IN` é aceito, com reembolso e avisos (`service/ReservationService.java:139`, `service/ReservationService.java:146`). O texto diz que o cancelamento só acontece antes do check-in.
3. **Arredondamento.** `roundHalfToEven` é `Math.round(value*100)/100` sobre `double`, ou seja, "meio para cima" (`service/PricingService.java:51-53`). Diverge de "meio para o par" nos valores com meio centavo exato. O exemplo do enunciado (R$ 599,76) bate.
4. **Relatório financeiro.** A condição `amount > 0 || status == CANCELLED` inclui reservas canceladas com valor 0 (`service/ReportService.java:77`). Entram tanto a pendente cancelada quanto a STANDARD reembolsada, e o relatório deveria mostrar só as que deixaram dinheiro.
5. **Formato do dinheiro.** `String.format("R$ %.2f").replace(".", ",")` não põe separador de milhar: sai "R$ 1159,80" em vez de "R$ 1.159,80" (`util/ComprovantFormatter.java:36-38`). Isso quebra o comprovante e o e-mail do hóspede em totais a partir de R$ 1.000. Além disso, `%.2f` depende do locale da JVM, o que também afeta o CSV (`service/ReportService.java:78`, `service/ReportService.java:90`).
6. **Segunda via.** Só a pendente é recusada, então uma reserva cancelada recebe comprovante (`api/ReservationController.java:121`). Deveria valer só para confirmada, com check-in ou com check-out.
7. **Ordem das validações.** A capacidade (422) é checada antes de "menos de um hóspede" e das datas (400) (`service/ReservationService.java:44-54`). É um desvio menor.

O e-mail do hóspede e os dos dois novos interessados seguem o pedido, mas regras centrais do sistema não: a disponibilidade, o cancelamento, o arredondamento, o relatório financeiro e a segunda via.
Conclusão: em parte

## Dúvidas

- A instrução diz que o projeto "passou nos testes do próprio agente", mas não há nenhum teste no projeto (não existe `src/test`, nem JUnit no `pom.xml`). Não consegui confirmar que o `mvn verify` faria algo além de compilar e empacotar.
- Não deu para saber, só pela leitura, como o Javalin 6.7.0 trata um corpo que não é JSON. `bodyAsClass` pode lançar uma exceção do Jackson que não é capturada por `IllegalArgumentException | NullPointerException` (`api/ReservationController.java:51`). Nesse caso, a resposta seria 500 em vez de 400 `INVALID_REQUEST`. Deixei isso fora da conclusão do comportamento.
- A escolha do formato das datas no comprovante em JSON (dd/MM/yyyy, `api/dto/DocumentResponse.java:38-40`) não está fixada no enunciado. Não contei como erro.
