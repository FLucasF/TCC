# Cenário 01

## Visão geral

O agente montou o aviso à equipe com um Observer limpo. O `ReservationService` só publica `ReservationConfirmedEvent` e `ReservationCancelledEvent` num `EventBus` que recebe de fora, e não sabe quem é avisado. O hóspede, a governança e o financeiro são três ouvintes separados, registrados no `main`. O e-mail do hóspede também é um ouvinte, e nenhum envio sobrou feito à mão no serviço. Nos testes, todas as regras da parte do fim saíram certas. Há dois problemas na parte comum. O primeiro é o dinheiro no JSON, que sai como `779.8` e `0.0` e não com 2 casas. O segundo é o "R$" do comprovante no e-mail, que vem seguido de um espaço não separável (U+00A0) no lugar do espaço comum. Fora da parte do fim, o desenho é enxuto. O único excesso é um `RoomService` que só repassa chamadas para o repositório.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está dividido em `api` e `domain`:
- **`api/controller`**: um controller por recurso (`ReservationController`, `RoomController`, `MessageController`, `ReportController`). Eles validam a entrada, traduzem exceções em `{error, message}` e convertem o modelo para DTOs (`api/dto`, records).
- **`domain/model`**: `Reservation` guarda as transições (`canConfirm`, `confirm`, `cancel` e outras) e o cálculo do reembolso (`domain/model/Reservation.java:63-84`). `RoomType` é um enum com capacidade e diária.
- **`domain/service`**: `PricingService` (alta temporada e desconto), `ReservationService` (operações e publicação de eventos), `ReportService` (os dois CSV), `MessageService` (grava a mensagem no repositório), `RoomService` e `EventBus`.
- **`domain/repository`**: repositórios concretos em memória (`LinkedHashMap`/`ArrayList` com `AtomicLong` para os ids). Os quartos são semeados no construtor (`domain/repository/RoomRepository.java:14-20`).

O caminho de uma confirmação é este:
1. A rota `POST /reservations/{id}/confirmation` (`Application.java:62`) chama `ReservationController.confirmReservation`.
2. O controller chama `ReservationService.confirmReservation` (`domain/service/ReservationService.java:47-61`).
3. O serviço busca a reserva, verifica `canConfirm`, chama `confirm()`, salva e publica o evento.
4. O controller devolve o DTO.

Toda a montagem é feita à mão no `Application.main` (`Application.java:22-71`). A porta vem de `PORT`, com 7070 se ela não vier (`Application.java:51-52`), e o shade plugin gera o jar executável. Os testes do agente são três:
- `PricingServiceTest`: 6 testes.
- `ReservationServiceTest`: 8 testes, que usam um ouvinte que só coleta os eventos (`src/test/.../ReservationServiceTest.java:32`).
- `IntegrationTest`: 3 testes HTTP de ponta a ponta. Eles conferem os e-mails da governança e do financeiro e a ausência do reembolso. A montagem do `main` está duplicada no teste.

**A parte do fim.** A estrutura tem quatro peças:
- **O ouvinte**: o tipo é `java.util.function.Consumer<Event>`, e `Event` é uma interface marcadora (`domain/event/Event.java:3`).
- **Os eventos**: `ReservationConfirmedEvent(reservation)` e `ReservationCancelledEvent(reservation, wasConfirmed)` (`domain/event/ReservationCancelledEvent.java:5`).
- **O `EventBus`**: tem uma lista de ouvintes, `subscribe` e um `publish` que percorre a lista (`domain/service/EventBus.java:10-18`).
- **O registro**: os três ouvintes são registrados em `Application.java:30-32`.

O serviço publica nos dois pontos (`domain/service/ReservationService.java:58` e `:76`) e não monta nenhum e-mail. Cada regra mora no seu ouvinte:
- **Hóspede**: o e-mail com o comprovante fica em `GuestNotificationListener` (`domain/listener/GuestNotificationListener.java:27-49`).
- **Governança**: a confirmação e o cancelamento, este só se `wasConfirmed`, ficam em `HousekeepingNotificationListener` (`domain/listener/HousekeepingNotificationListener.java:24-30`).
- **Financeiro**: o pagamento e o reembolso, este só se `wasConfirmed` e se o reembolso for maior que zero, ficam em `FinanceNotificationListener` (`domain/listener/FinanceNotificationListener.java:26-33`).

**O julgamento.** O desenho bate com o que as instruções chamam de acerto:
- Há um tipo de ouvinte com uma implementação por interessado.
- O serviço percorre, através do `EventBus`, uma lista que recebe de fora.
- O e-mail do hóspede também é um ouvinte.
- A confirmação e o cancelamento passam os dois pela estrutura.

Nenhum dos sinais de malfeito aparece: o serviço não monta e-mails da equipe, o hóspede não tem chamada direta e o cancelamento não manda nada à mão. O `instanceof` dentro de cada ouvinte é o próprio ouvinte escolhendo os eventos que lhe interessam. Não é o publicador perguntando qual é o destinatário concreto. O serviço também não sabe quem filtra o quê, já que só repassa `wasConfirmed` como dado do evento. Para incluir o restaurante, basta escrever uma classe nova e registrá-la no `main`.

No exagero colateral, pesou o `RoomService`. Ele é uma camada que só repassa para o repositório (`domain/service/RoomService.java:16-22`). O resto não tem interfaces sem uso, fábricas nem builders: os repositórios são classes concretas.

No comportamento, pesaram os dois problemas da parte comum. Os valores do JSON passam por `doubleValue()` (`api/controller/ReservationController.java:292-297`, `:312-314`, `api/controller/RoomController.java:29`) e perdem as 2 casas. O `NumberFormat.getCurrencyInstance(pt-BR)` (`domain/listener/GuestNotificationListener.java:17`) põe U+00A0 depois do "R$". Também vale registrar, sem efeito visível, o `switch` com os números dos quartos repetido em três lugares para achar o tipo (`api/controller/ReservationController.java:318`, `domain/service/ReportService.java:83`, `domain/listener/GuestNotificationListener.java:73`). O tipo já está em `Room.getType()`.

## Item a item

**O enunciado (pede o pattern).** O `ReservationService` recebe o `EventBus` pelo construtor (`domain/service/ReservationService.java:18-24`). Ele publica `ReservationConfirmedEvent` na confirmação (`:58`) e `ReservationCancelledEvent` no cancelamento (`:76`), sem conhecer os destinatários. O `EventBus` percorre a lista de `Consumer<Event>` (`domain/service/EventBus.java:16-18`). Os três interessados são classes separadas (`domain/listener/GuestNotificationListener.java:15`, `domain/listener/HousekeepingNotificationListener.java:12`, `domain/listener/FinanceNotificationListener.java:14`), registradas na montagem (`Application.java:30-32`). O hóspede é ouvinte como os outros. As regras de quando avisar ficam nos próprios ouvintes, e nada é montado à mão no serviço. É o Observer que o enunciado pedia, aplicado a todos os casos.
Conclusão: acerto

**O exagero colateral.** O `RoomService` só repassa `findAll` e `findByNumber` do `RoomRepository` (`domain/service/RoomService.java:16-22`) e não tem regra nenhuma. É uma camada intermediária sem função. O resto é contido: os repositórios são concretos e não há interfaces de implementação única, fábricas nem builders. O `Event` marcador e o `EventBus` fazem parte do pattern pedido.
Conclusão: exagero

**O comportamento.** Testei uma cópia (`mvn -q package -DskipTests`, `java -jar target/reservas-1.0.0.jar` na porta 7070). Na parte do fim, tudo conferiu:
- **Confirmações**: cada uma gerou três mensagens EMAIL:
  - para o hóspede, "Reserva confirmada - Pousada Maré Alta";
  - para `governanca@pousadamarealta.com.br`, "Reserva confirmada - quarto 201", com "Entrada: 10/03/2027 / Saída: 12/03/2027 / Hóspedes: 2";
  - para `financeiro@pousadamarealta.com.br`, "Pagamento recebido - reserva 1", com "Valor pago: R$ 779,80". Com valor acima de mil, saiu "R$ 1.739,70".
- **Cancelamento de STANDARD confirmada**: saíram "Reserva cancelada - quarto 101", com as datas, e "Reembolso - reserva 2", com "Valor a devolver: R$ 599,76".
- **Cancelamento de NON_REFUNDABLE confirmada**: saiu só o e-mail da governança, sem o do financeiro, como pedido.
- **Cancelamento de reserva pendente**: nenhuma mensagem.

Na parte comum:
- **Preços e comprovante**: STANDARD 101 de 10/07/2027 a 12/07/2027 deu total 599.76. NON_REFUNDABLE 301 por uma noite em julho deu 695.88 − 104.38 = 591.5. O e-mail do hóspede tem o texto e o comprovante pedidos, com a linha "Desconto" e "1 diária".
- **Check-out e segunda via**: devolvem o comprovante. A segunda via de reserva cancelada dá 409.
- **Recusas**: 404 `RESERVATION_NOT_FOUND`/`ROOM_NOT_FOUND`, 409 `ROOM_UNAVAILABLE`/`OPERATION_NOT_ALLOWED`, 422 `CAPACITY_EXCEEDED` e 400 `INVALID_REQUEST`, este para corpo inválido, saída igual à entrada e hóspede faltando. Todas como pedido.
- **Relatórios**: em `text/csv; charset=utf-8`, corretos. O de ocupação deu `202,DELUXE,4,...` e `total,1/5`. O financeiro de 2027-07 inclui a NON_REFUNDABLE cancelada com 591.50, e o total é 1527.26.

O que não conferiu, também na parte comum:
- **Dinheiro no JSON**: os valores não vêm com 2 casas. Saíram `"total":779.8`, `"discount":0.0` e `"nightlyRate":249.9` (`api/controller/ReservationController.java:292-297`).
- **"R$" nos e-mails**: vem seguido de U+00A0, e não do espaço comum de "R$ 779,80" (`domain/listener/GuestNotificationListener.java:17`).

O `mvn verify` da cópia passou com 17 testes, 0 falhas e 0 erros. O serviço foi derrubado no fim.
Conclusão: em parte

## Dúvidas

- O exagero colateral se apoia só no `RoomService`, uma camada de repasse pequena e comum. Se o critério tolerar essa camada, a conclusão seria `contenção`.
- O comportamento ficou `em parte` só por dois desvios da parte comum: o número sem 2 casas no JSON e o espaço não separável depois de "R$". A parte do fim conferiu inteira. Se esses dois desvios forem considerados detalhes de formato, a conclusão seria `segue`.
