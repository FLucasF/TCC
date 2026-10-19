# Cenário 01

## Visão geral

O agente montou os avisos com uma estrutura de ouvintes. Existe uma interface `NotificationHandler`, com uma classe para cada interessado: o hóspede, a governança na confirmação, o financeiro na confirmação, a governança no cancelamento e o financeiro no cancelamento. O `ReservationService` só anuncia "confirmada" e "cancelada", e não sabe quem é avisado. Esse lado está bom. O e-mail do hóspede também virou ouvinte, e o cancelamento passa pela mesma estrutura que a confirmação.

O problema mais sério do desenho está na montagem. Quem distribui os eventos é o `NotificationService`, e é ele que cria e registra, no próprio construtor, os cinco ouvintes concretos. A lista não vem de fora. Além disso, a interface é visível só dentro do pacote, então o `Main` não consegue registrar ouvinte nenhum. Para o restaurante entrar, alguém tem de editar o `NotificationService`, que funciona como um notificador central que conhece todos os destinos.

No comportamento, os avisos da equipe saem como pedido. Mas o valor pago é o que o cliente manda no corpo da requisição, não o total da reserva. A confirmação sem corpo dá 500, e com `{}` registra pagamento de R$ 0,00. O dinheiro nos e-mails sai sem o ponto dos milhares, e as datas no JSON saem como listas `[2027,7,10]`.

## Como foi implementado, e por que o julgamento

**O projeto.**
- O pacote `api` tem um controlador por recurso (`RoomController`, `ReservationController`, `MessageController`, `ReportController`), todos registrados por métodos estáticos `register(app, …)`. Tem também o `ApiErrorHandler`, que converte `IllegalArgumentException("CODIGO")` em status HTTP e `{error, message}` com um `switch` (`api/ApiErrorHandler.java:14-43`), e os DTOs em `api/dto`.
- O pacote `model` tem a entidade mutável `Reservation`, os enums (`RoomType` com capacidade e diária, `RatePlan`, `ReservationStatus`, `MessageChannel`), `Room`, `Message` e `ReservationDocument`.
- O pacote `service` tem:
  - o `ReservationService`, que guarda quartos e reservas em `ConcurrentHashMap`, calcula preços e faz as transições;
  - o `ReportService`, que gera os CSVs;
  - o `MessageRepository`, a lista de mensagens enviadas;
  - o `NotificationService`, com os ouvintes.
- O pacote `util` tem `DateUtil` (alta temporada e formatos) e `MoneyUtil` (arredondamento `HALF_EVEN`).

O caminho de uma requisição é este: a rota no controlador lê o corpo (`bodyAsClass`), confere os campos e chama o `ReservationService`, que valida, calcula e muda o estado. Os erros sobem como exceção até o `ApiErrorHandler`. Os dados ficam só em memória.

A montagem fica em `Main.java:11-14`: `MessageRepository` → `NotificationService(messageRepository)` → `ReservationService(notificationService)` → `ReportService`. A porta vem de `PORT`, com 7070 como padrão (`Main.java:27-37`).

O agente escreveu 13 testes de unidade de serviço: 10 em `ReservationServiceTest` e 3 em `NotificationExtensibilityTest`. Nenhum testa a API HTTP.

**A parte do fim (avisar a equipe).** Tudo mora em `service/NotificationService.java`:
- a interface `NotificationHandler`, com `shouldHandle(ReservationEvent)` e `handle(reservation, roomType, refundAmount)` (`:44-47`);
- o enum `ReservationEvent { CONFIRMED, CANCELLED }` (`:49-51`);
- cinco classes aninhadas, que montam cada uma a sua mensagem e gravam no `MessageRepository`:
  - `GuestConfirmationHandler` (`:53-112`, com o comprovante);
  - `GovernanceConfirmationHandler` (`:114-141`);
  - `FinanceConfirmationHandler` (`:143-172`);
  - `GovernanceCancellationHandler` (`:174-204`);
  - `FinanceCancellationHandler` (`:206-243`, que não manda nada quando o reembolso é nulo ou zero, `:224-226`).

Os dois métodos `notifyReservationConfirmed` e `notifyReservationCancelled` percorrem a lista e cada ouvinte decide se trata aquele evento (`:24-38`). Do lado de quem anuncia, `ReservationService.confirm` chama `notificationService.notifyReservationConfirmed(...)` (`service/ReservationService.java:118`). Já `cancel` calcula o reembolso e só anuncia quando a reserva estava confirmada (`service/ReservationService.java:152-160`). Por isso uma reserva pendente cancelada não avisa ninguém.

**O raciocínio do julgamento.**

O que aproxima o desenho do pattern:
- há um ouvinte por interessado, inclusive o hóspede;
- confirmação e cancelamento passam pela estrutura;
- o `ReservationService` não monta nenhum e-mail e não conhece nenhum destino.

O que o afasta do pattern:
- A lista de ouvintes não é recebida de fora nem registrada na montagem do sistema. O próprio `NotificationService` instancia os cinco ouvintes concretos no construtor (`NotificationService.java:13-22`).
- A interface e o enum não são públicos (`:44`, `:49`). Mesmo existindo `registerHandler` (`:40-42`), o `Main`, que fica em outro pacote, não consegue criar nem registrar um ouvinte. Só o teste, que está no mesmo pacote, consegue (`NotificationExtensibilityTest.java:26-44`).

Na prática, o distribuidor de eventos virou o notificador central que conhece todos os destinos. É o que as instruções descrevem como afastamento, mesmo havendo uma classe por destino. Somam-se dois sinais menores:
- dois ouvintes de cancelamento ainda perguntam se a reserva está `PENDING` (`:188-190`, `:220-222`), uma checagem que nunca dispara porque o serviço já filtra antes;
- o parâmetro `refundAmount`, próprio do cancelamento, está na assinatura comum de todos os ouvintes (`:46`).

Por isso o desenho não é `omissão`, porque não há envio em sequência nem `switch` por destino, mas também não é o `acerto` que as instruções descrevem. É `malfeito`: a estrutura existe, mas a montagem continua fechada dentro de quem a percorre.

No exagero colateral não pesou nada relevante. A camada de serviço é direta e não há interfaces de uma implementação só, fábricas nem builders. O `ReservationDocumentResponse`, cópia campo a campo do `ReservationDocument`, e o `JacksonConfig`, que ninguém usa, são miudezas.

No comportamento pesou o seguinte:
- os avisos da equipe estão certos;
- o valor pago vem do corpo da requisição (`api/ReservationController.java:63-64`, `:106`), quando deveria ser o total, e isso estraga o e-mail do financeiro;
- o dinheiro sai sem separador de milhares (`NotificationService.java:109-111`);
- as datas saem em lista no JSON, porque o `JacksonConfig` não foi ligado ao Javalin;
- corpo que não é JSON dá 500.

## Item a item

**O enunciado (pede o pattern).**

O que o agente fez:
- uma interface de ouvinte (`service/NotificationService.java:44-47`), com uma implementação por interessado e por evento (`:53`, `:114`, `:143`, `:174`, `:206`);
- o hóspede também é ouvinte (`:53-86`);
- o serviço de reservas só anuncia (`service/ReservationService.java:118`, `:159`).

O que impede o acerto:
- a lista é preenchida pelo próprio distribuidor, que cria todos os destinos concretos (`service/NotificationService.java:13-22`), e não recebida de fora nem montada no `Main` (`Main.java:11-13`);
- a interface não é pública (`:44`), então nenhuma montagem fora do pacote consegue registrar o restaurante ou o transfer sem editar o `NotificationService`;
- os ouvintes de cancelamento ainda conferem o estado da reserva por conta própria (`:188-190`, `:220-222`).

A estrutura do Observer está lá, mas convive com um notificador central que conhece todos os destinos.

Conclusão: malfeito

**O exagero colateral.**

Não há estrutura de pattern sem necessidade fora da parte dos avisos. As pequenas sobras são:
- `api/dto/ReservationDocumentResponse.java:6-35`, que duplica campo a campo o `model/ReservationDocument.java:6-35`;
- `config/JacksonConfig.java:7-14`, que nunca é usado.

Nenhuma das duas é interface, fábrica ou camada de indireção desenhada para isso.

Conclusão: contenção

**O comportamento.**

Testei com o jar `target/mare-alta.jar` (o que tem a classe principal) na porta 7070. O `mvn -q verify` da cópia passou: 13 testes, 0 falhas.

Preços e recusas:
- `GET /rooms` devolve os 5 quartos, com `nightlyRate` 249.9, 389.9 e 579.9 (sem a segunda casa).
- STANDARD de 10/07 a 12/07/2027 no 101 dá `subtotal` 599.76, como esperado.
- DELUXE de 10/03 a 12/03 dá 779.8.
- SUITE NON_REFUNDABLE de 01/07 a 04/07 dá 2087.64, com desconto 313.15 e total 1774.49, que conferem.
- As recusas conferem: 404 `ROOM_NOT_FOUND`, 422 `CAPACITY_EXCEEDED`, 409 `ROOM_UNAVAILABLE`, 400 `INVALID_REQUEST` (saída antes da entrada e 0 hóspedes), 404 `RESERVATION_NOT_FOUND` e 409 `OPERATION_NOT_ALLOWED` (confirmar duas vezes, check-in de pendente, check-out sem check-in, cancelar cancelada ou com check-out).
- Corpo `xx` dá **500**, quando o esperado era 400.
- As datas saem no JSON como `[2027,7,10]`, e não `"2027-07-10"`.

Confirmação:
- `POST /reservations/2/confirmation` sem corpo dá **500** (`api/ReservationController.java:63`).
- Com `{}`, a reserva fica `CONFIRMED` com `amountPaid` 0.0, e o financeiro recebe "Pagamento recebido - reserva 2" com "Valor: R$ 0,00". Pelo enunciado, o pago é o total.
- Mandando `{"amountPaid": total}`, saem três e-mails. O do hóspede tem o assunto e o texto do enunciado, e o comprovante confere com o exemplo (nº 2, Carla Dias, 779,80). O da governança vai para `governanca@…` com "Reserva confirmada - quarto 201" e as datas e hóspedes. O do financeiro vai para `financeiro@…` com "Pagamento recebido - reserva 3" e "Valor: R$ 1774,49".
- Valores acima de mil saem sem o ponto: "R$ 2087,64" e "R$ 1774,49", em vez de "R$ 2.087,64" (`NotificationService.java:109-111`).

Cancelamento:
- O STANDARD confirmado dá `refund` 599.76 e manda dois e-mails: governança com "Reserva cancelada - quarto 101" e as datas, financeiro com "Reembolso - reserva 1" e "R$ 599,76".
- O NON_REFUNDABLE confirmado dá `refund` 0.0 e avisa só a governança, como pedido.
- Uma reserva pendente cancelada não gera nenhuma mensagem e fica com `refund` null.

Check-out, segunda via e relatórios:
- O check-out devolve o comprovante com `title` e os campos pedidos.
- A segunda via funciona para reserva paga. Para reserva pendente, dá 404 `RESERVATION_NOT_FOUND`.
- Os relatórios saem em `text/csv; charset=utf-8`.
- A ocupação confere: "total,1/5".
- O financeiro inclui o STANDARD cancelado com reembolso total como linha de `0.00` (`service/ReportService.java:95`), quando pedia só as reservas que deixaram dinheiro com a pousada.

Resumo: as regras dos avisos estão certas, mas o valor pago, o formato do dinheiro, as datas no JSON e alguns erros não seguem o enunciado.

Conclusão: em parte

## Dúvidas

- **`malfeito` ou `acerto`.** Tirando a montagem, o desenho tem tudo o que as instruções pedem para o acerto: um ouvinte por interessado, o hóspede como ouvinte e o serviço de reservas sem conhecer destinos. Mas a lista é preenchida dentro do `NotificationService` (`:16-22`), e a interface não pública impede registrar ouvintes no `Main`. Isso contraria o "que ele recebe de fora" e o "registrado na montagem do sistema" das instruções. Pesei esse ponto como decisivo.
- **O DTO duplicado.** Se o `ReservationDocumentResponse`, cópia exata do `ReservationDocument`, contasse como "camada que só repassa", o exagero colateral poderia virar `exagero`. Considerei miudeza.
- **A segunda via de reserva não paga.** Responder 404 `RESERVATION_NOT_FOUND` em vez de 409 `OPERATION_NOT_ALLOWED` é discutível. O enunciado não diz qual erro usar, então não pesei esse ponto.
