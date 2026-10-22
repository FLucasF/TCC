# Cenário 03

## Visão geral

O agente montou o Observer na forma certa. O `ReservationService` publica `ReservationConfirmedEvent` e `ReservationCancelledEvent` num `EventPublisher` que recebe pronto. Esse publicador percorre uma lista de `EventListener`, e o `main` registra nela três ouvintes: hóspede, governança e financeiro. O serviço de reservas não sabe quem é avisado, o e-mail do hóspede também virou ouvinte e o cancelamento passa pelo mesmo caminho. O problema mais sério é que os ouvintes são só cascas. Eles decidem quando avisar, mas não montam nada: o destinatário, o assunto e o texto de cada interessado ficam todos numa `NotificationService` única. Essa classe tem um método por destino e conhece todos os endereços, e é justamente o "notificador único" que as instruções chamam de omissão. Ela convive com a estrutura de ouvintes. Para incluir o restaurante é preciso mexer em duas classes, e o próprio agente mostra isso em `exemplo-adicionar-restaurante.java:38-40` ("Adicionar o método no NotificationService"). Por isso a classe é `malfeito`. No comportamento, as regras dos avisos conferem. Os problemas estão em outras partes: o dinheiro sai com espaço não separável, os campos que faltam não são todos validados, a segunda via de uma reserva não paga dá o código errado e o relatório financeiro inclui linhas que não deveria.

## Como foi implementado, e por que o julgamento

**O projeto.** Os pacotes ficam em `com.pousada`:
- `domain`: `Reservation`, que guarda o estado, o cálculo de preço e as transições; `Money`, `Room` e os enums `RoomType`, `RatePlan` e `ReservationStatus`.
- `repository`: listas em memória para quartos, reservas e mensagens.
- `service`: `ReservationService`, `RoomService`, `DocumentService` (o texto do comprovante), `NotificationService` (todos os e-mails) e `ReportService` (os CSVs).
- `controller`: um controller por recurso.
- `dto`: as respostas JSON.
- `event`: `EventListener`, `EventPublisher` e os eventos.
- `listener`: os três ouvintes.
- `util`: os formatadores.

O caminho de uma requisição é este: a rota é registrada em `Application.java:99-109`, o controller lê o corpo ou o parâmetro (`ReservationController.java:21-77`), o `ReservationService` valida e muda o estado da `Reservation` (`ReservationService.java:60-92`), e o DTO recalcula os valores na hora de responder (`ReservationResponse.java:26-53`). Os dados ficam só em memória. A montagem é manual, no `Application.main`: ali se criam os repositórios e os serviços, registram-se os ouvintes, escolhe-se a porta pela variável `PORT` com 7070 como padrão (`Application.java:116-122`) e mapeiam-se as exceções para os códigos de erro (`Application.java:59-97`). Os testes do agente são de unidade, de `Money` e `Reservation`, mais um do `EventPublisher` e dois de "integração" que chamam os serviços direto, sem HTTP. O `NotificationFlowTest` confere a quantidade de e-mails em cada caso.

**A parte que pede o pattern.**
- `EventListener` (`event/EventListener.java:3-5`) tem um método só, `onEvent(ReservationEvent)`.
- `EventPublisher` (`event/EventPublisher.java:6-18`) guarda a lista, tem `register` e percorre todos os ouvintes em `publish`.
- O serviço recebe o publicador no construtor (`ReservationService.java:16-21`) e publica na confirmação (`ReservationService.java:74`) e no cancelamento (`ReservationService.java:86-89`, com `wasConfirmed` e o valor do reembolso).
- Os ouvintes são registrados no `main` (`Application.java:36-39`).
- `GuestNotificationListener` reage só à confirmação (`listener/GuestNotificationListener.java:17-21`).
- `HousekeepingNotificationListener` reage à confirmação e ao cancelamento de reserva que estava confirmada (`listener/HousekeepingNotificationListener.java:17-26`).
- `FinanceNotificationListener` reage à confirmação e ao cancelamento confirmado com reembolso maior que zero (`listener/FinanceNotificationListener.java:20-41`).

Cada ouvinte, porém, só chama um método de `NotificationService`. Quem monta e grava os e-mails é essa classe:
- `sendGuestConfirmationEmail` em `service/NotificationService.java:22-39`;
- `sendHousekeepingConfirmationEmail` em `service/NotificationService.java:41-56`, com o endereço da governança fixo na linha 55;
- `sendFinancePaymentEmail` em `service/NotificationService.java:58-69`;
- `sendHousekeepingCancellationEmail` em `service/NotificationService.java:71-83`;
- `sendFinanceRefundEmail` em `service/NotificationService.java:85-96`.

**O raciocínio.** O que aproxima o desenho do pattern é que o serviço anuncia e não conhece os interessados. Há uma implementação por interessado, registrada na montagem, o hóspede é ouvinte como os outros e os dois eventos usam a mesma lista. Para cada ouvinte, a regra de *quando* avisar mora nele. O que afasta o desenho do pattern é que *o que* cada interessado recebe, e *para onde* vai, mora numa classe central que conhece todos os destinos. As instruções dizem que essa classe, sozinha, seria omissão. Aqui ela não está sozinha: está atrás de ouvintes de verdade. Isso não é a ausência do pattern, é o pattern convivendo com o jeito sem ele. Cada interessado novo exige mexer no ouvinte novo e também na classe compartilhada, que cresce um método por destino. Por isso a classe é `malfeito`, e não `acerto` nem `omissão`.

No exagero colateral pesou a falta de estrutura sobrando. `ReservationEvent` tem duas implementações, e o `EventPublisher` faz parte do próprio pattern. O `RoomService` quase só repassa ao repositório (`service/RoomService.java:15-22`), mas é uma camada fina e comum, sem fábricas nem interfaces de uma implementação só.

No comportamento pesou que as regras dos avisos estão certas. Os desvios ficam em outras partes: a formatação do dinheiro, a validação dos campos, o código da segunda via e o relatório financeiro.

## Item a item

**O pattern pedido.** Existem a interface `EventListener` (`event/EventListener.java:3-5`), um publicador que percorre a lista (`event/EventPublisher.java:13-17`), o serviço recebendo esse publicador de fora (`ReservationService.java:16-21`) e publicando nos dois momentos (`ReservationService.java:74`, `ReservationService.java:89`), e três ouvintes registrados no `main` (`Application.java:36-39`), inclusive o do hóspede. Mas nenhum ouvinte monta a mensagem do seu interessado. Todos delegam a `NotificationService`, que tem um método por destino com os endereços fixos (`service/NotificationService.java:55`, `service/NotificationService.java:68`, `service/NotificationService.java:82`, `service/NotificationService.java:95`). É o notificador único que conhece todos os destinos, convivendo com a estrutura de ouvintes. Um interessado novo exige alterar essa classe compartilhada, como o próprio agente mostra em `exemplo-adicionar-restaurante.java:38-40`. A estrutura do pattern existe, mas parte do que ela deveria isolar continua centralizado.
Conclusão: malfeito

**O exagero colateral.** Não há estrutura sem necessidade fora do pattern. Não há fábrica, builder nem interface de uma implementação só. O `RoomService` é uma camada fina sobre o repositório (`service/RoomService.java:15-22`), mas também é usado por relatórios, DTOs e notificações, e não chega a ser exagero.
Conclusão: contenção

**O comportamento.** A conferência foi só por leitura do código: não subi o serviço nem chamei a API.

O que confere:
- Na confirmação saem três e-mails. O do hóspede tem o assunto e o texto pedidos (`service/NotificationService.java:29-36`). O da governança vai para `governanca@pousadamarealta.com.br`, com o assunto "Reserva confirmada - quarto N", as datas e o número de hóspedes (`service/NotificationService.java:41-55`). O do financeiro vai para `financeiro@...`, com o assunto "Pagamento recebido - reserva N" e o valor pago (`service/NotificationService.java:58-68`).
- No cancelamento de reserva confirmada, a governança recebe "Reserva cancelada - quarto N" com as datas (`listener/HousekeepingNotificationListener.java:21-24`). O financeiro recebe "Reembolso - reserva N" só quando o reembolso é maior que zero (`listener/FinanceNotificationListener.java:24-27`).
- A reserva pendente cancelada não avisa ninguém, porque `wasConfirmed` é falso (`ReservationService.java:86`).
- O reembolso é integral no plano STANDARD e zero no NON_REFUNDABLE ou na reserva pendente (`domain/Reservation.java:132-139`).
- A alta temporada (`domain/Reservation.java:70-87`), o arredondamento "meio para o par" a cada passo (`domain/Money.java:15`) e as transições de estado conferem.

O que não confere:
- **Espaço não separável no dinheiro.** Os valores são formatados com `NumberFormat.getCurrencyInstance(pt-BR)` (`domain/Money.java:10`, `domain/Money.java:47`). No Java 21 isso produz "R$\u00a0779,80", com espaço não separável, e não "R$ 779,80". O comprovante dentro do e-mail do hóspede e os valores nos e-mails da equipe ficam diferentes do texto pedido. O teste do agente usa `contains` e não pega isso (`src/test/java/com/pousada/domain/MoneyTest.java:43`).
- **Campos que faltam.** Sem `roomNumber`, a resposta é 404 ROOM_NOT_FOUND em vez de 400 (`ReservationService.java:27-30`). Nome, e-mail e CPF não são validados. Sem `ratePlan`, a reserva é salva (`ReservationService.java:52`) antes de o DTO estourar NPE (`domain/Reservation.java:60`). A resposta vira 400, mas a reserva fica gravada e ocupando o quarto.
- **Segunda via de reserva não paga.** Dá 400 INVALID_REQUEST pelo handler genérico (`ReservationController.java:72-74`, `Application.java:94-97`), quando deveria dar 409 OPERATION_NOT_ALLOWED.
- **Relatório financeiro.** Ele inclui toda reserva que não está pendente (`repository/ReservationRepository.java:59`), e assim entram linhas de valor zero: reservas STANDARD canceladas com reembolso integral e reservas canceladas ainda pendentes. Elas não "deixaram dinheiro com a pousada".
- **Valores no JSON.** Saem como `double` (`dto/ReservationResponse.java:19-24`), e um valor como 599.70 aparece como 599.7, sem as 2 casas.

O resto do sistema funciona como pedido: rotas, ocupação, códigos 404, 409 e 422, porta e numeração.
Conclusão: em parte

## Dúvidas

A classe ficou entre `malfeito` e `acerto`. As instruções descrevem o malfeito como "o serviço ainda monta os e-mails", pensando no serviço de reservas, e aqui o `ReservationService` está limpo. Pesou contra o acerto que a montagem de todos os destinos está numa classe central compartilhada (`service/NotificationService.java:22-96`), exatamente o notificador único que as instruções recusam como alternativa. Também pesou que um interessado novo exige mexer nela. Fiquei com `malfeito` porque a estrutura existe, mas convive com esse notificador.
