# Cenário 03

## Visão geral

O agente montou as peças de um Observer. Há uma interface `ReservationNotifier` com `onConfirmation` e `onCancellation`, e uma implementação para cada interessado: `GuestNotifier`, `GovernanceNotifier` e `FinanceNotifier`. O `ReservationService` só percorre a lista de ouvintes, na confirmação e no cancelamento. O e-mail do hóspede saiu do serviço e virou um desses ouvintes. O que está bom: nenhum texto de e-mail ficou no serviço, as duas operações passam pela lista e o hóspede é tratado como os outros. O problema mais sério é quem cria a lista. É o próprio serviço, no construtor, com `new` de cada ouvinte concreto (`src/main/java/hotel/reservations/ReservationService.java:26-29`). A classe `Hotel`, que monta o sistema, não mudou. Assim o serviço ainda sabe quem é avisado, e para pôr ou tirar o restaurante ou o transfer é preciso editar o `ReservationService`, que é justamente o que o pedido quer evitar. As regras pedidas conferem, pela leitura do código.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin. A classe `hotel.Hotel` monta as peças e sobe o servidor (`src/main/java/hotel/Hotel.java:19-28`): `Rooms`, `ReservationStore`, `SentMessages`, `EmailSender`, `ReservationService`, os relatórios (`OccupancyReport`, `RevenueReport`) e a `HttpApi`. A `HttpApi` define as rotas. Por exemplo, `POST /reservations/{id}/confirmation` chama `reservations.confirm`, e `POST /reservations/{id}/cancellation` chama `reservations.cancel` (`src/main/java/hotel/web/HttpApi.java:70-73`). `GET /messages` lista o que foi "enviado" (`src/main/java/hotel/web/HttpApi.java:81`).

As regras de reserva ficam em `hotel.reservations.ReservationService`. Os dados ficam em memória: as reservas num `LinkedHashMap` do `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:12`) e os e-mails no `SentMessages`, que o `EmailSender` alimenta. Os textos de comprovante ficam em `hotel.documents` (`ReservationReceipt`, `TextFormat`).

Nos testes, o agente adaptou `ReservationServiceTest` e `HttpApiTest` para os três e-mails da confirmação e acrescentou três casos de cancelamento: confirmada com reembolso, pendente e não reembolsável.

**A parte que pede o pattern.**
- A interface `ReservationNotifier` tem os dois eventos (`src/main/java/hotel/reservations/ReservationNotifier.java:3-6`).
- O `GuestNotifier` leva o e-mail do hóspede, igual ao da base: o mesmo assunto, `confirmationEmailBody` movido sem mudança e o comprovante montado com o quarto. O cancelamento dele não faz nada (`src/main/java/hotel/reservations/GuestNotifier.java:21-44`).
- O `GovernanceNotifier` monta os dois e-mails da governança (`src/main/java/hotel/reservations/GovernanceNotifier.java:17-31`).
- O `FinanceNotifier` monta o pagamento e o reembolso. O reembolso sai só quando o valor não é nulo nem zero (`src/main/java/hotel/reservations/FinanceNotifier.java:17-31`).
- No serviço, a confirmação só chama `notifiers.forEach(n -> n.onConfirmation(reservation))` (`src/main/java/hotel/reservations/ReservationService.java:73`).
- O cancelamento guarda se a reserva estava confirmada e, depois de cancelar, chama `onCancellation` em todos (`src/main/java/hotel/reservations/ReservationService.java:87-95`).
- A lista, porém, é criada e preenchida dentro do construtor do serviço. O construtor continua recebendo o `EmailSender`, só para repassá-lo aos ouvintes que ele mesmo instancia (`src/main/java/hotel/reservations/ReservationService.java:21-30`). Ninguém de fora consegue registrar ou tirar um ouvinte.

**O raciocínio.** O que aproxima o desenho do pattern: há uma implementação por interessado, cada regra mora no seu ouvinte, o hóspede também é ouvinte e as duas operações passam pela lista. O que o afasta: o serviço não "recebe de fora" a lista, como as instruções exigem, e os ouvintes não são "registrados na montagem do sistema". O `Hotel` continua idêntico (`src/main/java/hotel/Hotel.java:23-24`). O sujeito continua conhecendo concretamente quem é avisado. Pela definição, isso é estrutura que convive com o jeito sem ela: a variação que deveria ser mudada na montagem ainda é mudada à mão dentro do serviço. Não é omissão, porque não há um notificador único nem envios em sequência no serviço. Os destinos estão em ouvintes separados atrás de uma interface. Por isso dei `malfeito`.

No exagero colateral, não há nada fora da parte do pattern. A interface tem três implementações reais. No comportamento, os valores, assuntos, destinatários e condições conferem.

## Item a item

**O pattern pedido.** Existem a interface `ReservationNotifier` (`src/main/java/hotel/reservations/ReservationNotifier.java:3-6`) e uma implementação por interessado, o hóspede entre eles (`GuestNotifier.java:8`, `GovernanceNotifier.java:6`, `FinanceNotifier.java:6`). O serviço percorre a lista na confirmação e no cancelamento (`ReservationService.java:73`, `ReservationService.java:93-95`). Mas é o próprio `ReservationService` que instancia e registra os três ouvintes concretos (`src/main/java/hotel/reservations/ReservationService.java:26-29`), e o `Hotel` não registra nada (`src/main/java/hotel/Hotel.java:23-24`). O serviço continua sabendo quem é avisado, e cada novo interessado exige mexer nele. É a estrutura do Observer sem a parte que separa o sujeito dos observadores.
Conclusão: malfeito

**O exagero colateral.** Não há. A interface nova tem três implementações usadas, e não foram criadas fábricas, builders nem camadas que só repassam. A constante `HOTEL_NAME` ficou duplicada no serviço (`ReservationService.java:14`) e no `GuestNotifier` (`GuestNotifier.java:10`). Os imports `ReservationReceipt` e `EmailSender` continuam no serviço porque ainda são usados. Nada disso é estrutura sobrando.
Conclusão: contenção

**O comportamento.** A conferência foi só por leitura: não subi o serviço, não rodei o Maven nem chamei a API.
- **Confirmação.** O e-mail do hóspede tem o mesmo destinatário, assunto e corpo da base (`GuestNotifier.java:22-27`, `GuestNotifier.java:38-44`). A governança recebe em `governanca@pousadamarealta.com.br` o assunto "Reserva confirmada - quarto N", com entrada, saída e hóspedes, as datas por `TextFormat.date` (`GovernanceNotifier.java:8`, `GovernanceNotifier.java:18-22`). O financeiro recebe em `financeiro@pousadamarealta.com.br` o assunto "Pagamento recebido - reserva N", com o valor pago em `TextFormat.money` (`FinanceNotifier.java:8`, `FinanceNotifier.java:18-20`).
- **Cancelamento de reserva confirmada.** A governança recebe "Reserva cancelada - quarto N" com as datas (`GovernanceNotifier.java:27-30`). O financeiro recebe "Reembolso - reserva N" com o valor, só se o reembolso for maior que zero (`FinanceNotifier.java:25-31`). No plano não reembolsável o reembolso é zero (`ReservationService.java:88-91`), então o financeiro não é avisado.
- **Cancelamento de reserva pendente.** Não avisa ninguém (`ReservationService.java:87`, `ReservationService.java:93`).
- **O resto do sistema.** Criação, check-in, check-out e cópia do comprovante ficaram iguais (`ReservationService.java:32-58`, `ReservationService.java:99-125`).

Tudo confere com o pedido.
Conclusão: segue

## Dúvidas

Fiquei entre `malfeito` e `acerto`. O serviço só depende da interface na hora de avisar, e a lista é uma lista de verdade. Mas as instruções exigem explicitamente que a lista venha de fora e que os ouvintes sejam registrados na montagem. Aqui o construtor do serviço instancia os ouvintes concretos, e isso me fez escolher `malfeito`.
