# Cenário 03

## Visão geral

O agente criou uma interface de ouvinte, `ReservationEventListener`, com um evento de confirmação e outro de cancelamento. Fez uma implementação para a governança (`HousekeepingNotifier`) e outra para o financeiro (`FinanceNotifier`), e a lista de ouvintes é registrada na montagem, em `Hotel`. O `ReservationService` percorre essa lista sem saber quem está nela. A estrutura do Observer está certa e é enxuta. O problema mais sério é o hóspede: ele não virou ouvinte. O e-mail dele continua sendo montado e enviado à mão dentro de `ReservationService.confirm`, logo antes do laço dos ouvintes. As instruções do pattern tratam esse caso como `malfeito`. Pela leitura do código, o comportamento pedido confere.

## Como foi implementado, e por que o julgamento

**O projeto.** É uma aplicação Javalin organizada em pacotes:
- `hotel.rooms`: os quartos e o catálogo `Rooms`.
- `hotel.reservations`: `Reservation`, `ReservationService`, `ReservationStore` (memória), `PriceCalculator` e erros.
- `hotel.documents`: o comprovante `ReservationReceipt` e o `TextFormat`, que formata dinheiro e datas.
- `hotel.messages`: `EmailSender`, que grava cada mensagem em `SentMessages`, exposto em `GET /messages`.
- `hotel.reports`: os relatórios de ocupação e de receita.
- `hotel.web.HttpApi`: as rotas.

Uma requisição chega pela rota (por exemplo `POST /reservations/{id}/cancellation`, em `final/src/main/java/hotel/web/HttpApi.java:72`), passa ao `ReservationService`, que aplica a regra, e a reserva fica no `ReservationStore`. A classe `Hotel` monta tudo e sobe o servidor (`final/src/main/java/hotel/Hotel.java:22-36`). O agente acrescentou quatro testes de serviço em `ReservationServiceTest`: confirmação, cancelamento de reserva confirmada, cancelamento de pendente e não reembolsável. Também ajustou o `HttpApiTest` para esperar 3 mensagens na confirmação.

**A parte do pattern.**
- A interface `final/src/main/java/hotel/reservations/ReservationEventListener.java:6-13` declara `onReservationConfirmed(Reservation, Room)` e `onReservationCancelled(Reservation, Room)`.
- O serviço recebe a lista de ouvintes no construtor (`ReservationService.java:19,26,31`).
- Na confirmação, ele percorre a lista (`ReservationService.java:81-83`). No cancelamento, só quando a reserva estava confirmada (`ReservationService.java:97,103-108`).
- O texto da governança mora em `final/src/main/java/hotel/notifications/HousekeepingNotifier.java:21-39`.
- O texto do financeiro mora em `final/src/main/java/hotel/notifications/FinanceNotifier.java:21-39`. Ali também está a regra de só avisar quando há reembolso (`:32`).
- O registro dos ouvintes é feito em `Hotel.java:32`.
- O hóspede ficou de fora: `ReservationService.java:76-80` monta o comprovante e chama `emailSender.send` direto, com o texto em `confirmationEmailBody` (`:144-150`). Por isso o serviço ainda depende de `EmailSender` (`:18,25`).

**O raciocínio.** O que aproxima o desenho do Observer: interface de ouvinte, uma classe por interessado, lista recebida de fora, registro na montagem, e o serviço não conhece a governança nem o financeiro. Os dois eventos passam pelos ouvintes, e o serviço não monta nenhum e-mail novo. O que afasta o desenho do pattern é o interessado que já existia, o hóspede. Ele continua com chamada direta, e as instruções descrevem esse caso como `malfeito`: "o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes". O pedido também exige que a variação cubra os casos que já existiam. Hoje, tirar ou trocar o aviso ao hóspede ainda exige mexer no serviço. No exagero colateral não pesou nada: as classes novas são as do próprio pattern. No comportamento, assuntos, destinatários, conteúdo, a regra do reembolso e o silêncio no cancelamento de pendente conferem pela leitura.

## Item a item

**O pattern pedido.** Há interface de ouvinte (`final/src/main/java/hotel/reservations/ReservationEventListener.java:6`) com uma implementação para a governança (`final/src/main/java/hotel/notifications/HousekeepingNotifier.java:10`) e outra para o financeiro (`final/src/main/java/hotel/notifications/FinanceNotifier.java:11`). A lista é injetada no serviço (`final/src/main/java/hotel/reservations/ReservationService.java:26,31`) e registrada em `final/src/main/java/hotel/Hotel.java:32`. O serviço a percorre na confirmação (`ReservationService.java:81-83`) e no cancelamento (`:105-107`). Mas o e-mail do hóspede continua como chamada direta no serviço (`ReservationService.java:77-80`, texto em `:144-150`), em vez de ser mais um ouvinte. O jeito antigo convive com a estrutura nova. Pelas instruções, isso é malfeito, não acerto.
Conclusão: malfeito

**O exagero colateral.** Não há. As únicas peças novas são a interface de ouvinte e as duas implementações, ambas do pattern. Não surgiu fábrica, builder nem camada de repasse. O `TextFormat` existente foi reaproveitado (`FinanceNotifier.java:26`, `HousekeepingNotifier.java:25-26`).
Conclusão: contenção

**O comportamento.** A conferência foi por leitura do código, sem subir o serviço nem rodar testes.
- **E-mail do hóspede.** Continua igual à base: mesmo destinatário, mesmo assunto "Reserva confirmada - Pousada Maré Alta" e mesmo corpo com o comprovante (`ReservationService.java:77-80,144-150`). A única mudança foi extrair `room` para uma variável (`:75`).
- **Governança na confirmação.** Vai para `governanca@pousadamarealta.com.br` com o assunto "Reserva confirmada - quarto N". O corpo traz entrada e saída em dd/MM/yyyy e o número de hóspedes (`HousekeepingNotifier.java:12,22-28`).
- **Financeiro na confirmação.** Vai para `financeiro@pousadamarealta.com.br` com o assunto "Pagamento recebido - reserva N" e o valor pago em R$ (`FinanceNotifier.java:13,23-27`).
- **Cancelamento de reserva confirmada.** A governança recebe "Reserva cancelada - quarto N" com as datas liberadas (`HousekeepingNotifier.java:33-38`). O financeiro recebe "Reembolso - reserva N" com o valor, e só quando `refund > 0` (`FinanceNotifier.java:32-37`). No não reembolsável, o reembolso é zero (`ReservationService.java:100`), e o financeiro não é avisado.
- **Cancelamento de reserva pendente.** Não avisa ninguém, porque o aviso fica atrás de `wasConfirmed` (`ReservationService.java:97,103`).
- **Resto do sistema.** As rotas e os relatórios não foram alterados. Os testes existentes só foram adaptados ao construtor novo e às mensagens a mais (`final/src/test/java/hotel/web/HttpApiTest.java`).
Conclusão: segue

## Dúvidas

Nenhuma. As instruções do pattern descrevem literalmente este caso (só o hóspede com chamada direta) como `malfeito`.
