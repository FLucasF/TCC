# Cenário 03

## Visão geral

O agente montou os avisos de confirmação e de cancelamento como um Observer de verdade. Existe uma interface `NotificationHandler` com uma implementação para cada interessado: o hóspede, a governança e o financeiro. Os três são registrados no `main`. O `ReservationService` só anuncia, por meio de um `NotificationService` que recebe de fora, e não sabe quem é avisado. O e-mail do hóspede também virou ouvinte, e o cancelamento passa pela mesma lista. O problema mais sério está fora do desenho e quebra o fluxo inteiro. O endpoint de confirmação exige no corpo um campo `amountPaid` que o contrato combinado com o site não tem, e grava o valor que o cliente mandar em vez do total da reserva. Chamado como o enunciado descreve, `POST /reservations/{id}/confirmation` responde 400, e aí nenhuma confirmação acontece nem os e-mails saem. Além disso, o agente não escreveu nenhum teste: não existe `src/test`.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica todo em `com.pousada.marealta`, dividido assim:
- **`api`**: um único `ReservationController`, que registra todas as rotas (`api/ReservationController.java:41-53`), e os DTOs de resposta (`ReservationResponse`, `ReceiptResponse`, `MessageResponse`, `RoomResponse`, `ErrorResponse`).
- **`service`**:
  - `ReservationService`: regras de estado, disponibilidade e preço.
  - `RoomService`: os cinco quartos fixos.
  - `MessageService`: a lista de mensagens enviadas, guardada na memória.
  - `OccupancyReportService` e `RevenueReportService`: os relatórios em CSV.
- **`model`**: `Reservation` com campos públicos e mutáveis, os enums `RatePlan`, `ReservationStatus` e `RoomType`, mais `Room` e `Message`.
- **`util`**: `PriceCalculator`, `SeasonHelper` e `FormatHelper`.
- **`notification`**: as peças do pattern.

Uma requisição chega ao método do controller. Ele lê o JSON com `bodyAsClass(JsonNode.class)` e chama o serviço. Os erros do serviço são `IllegalArgumentException` cuja mensagem é o código de erro, e `handleServiceException` traduz esse código para o status HTTP (`api/ReservationController.java:223-244`). Os dados ficam em listas na memória dentro dos serviços. `Application.main` monta as peças à mão e sobe o Javalin na porta de `PORT` ou na 7070 (`Application.java:18-41`). O jar executável sai do maven-shade (`pom.xml:63-80`). O agente não escreveu testes.

**A parte do pattern.** As peças são estas:
- **A interface**: `NotificationHandler` declara `handleConfirmation` e `handleCancellation` (`notification/NotificationHandler.java:5-8`).
- **O sujeito**: `NotificationService` guarda uma `List<NotificationHandler>`, aceita registros por `registerHandler` e percorre a lista em `notifyConfirmation` e `notifyCancellation` (`notification/NotificationService.java:8-24`).
- **Os ouvintes**:
  - `GuestEmailHandler` monta o comprovante e o e-mail do hóspede (`notification/GuestEmailHandler.java:15-25`). Ele ignora o cancelamento (`:27-29`).
  - `GoveranceNotificationHandler` avisa a governança na confirmação, com datas e hóspedes, e no cancelamento, com datas (`notification/GoveranceNotificationHandler.java:16-40`).
  - `FinanceNotificationHandler` avisa o pagamento e, no cancelamento, só manda e-mail quando `refund > 0` (`notification/FinanceNotificationHandler.java:16-31`).
- **A montagem**: os três são registrados em `Application.java:25-27`, e o `NotificationService` é injetado no `ReservationService` (`Application.java:29`).

O serviço chama `notificationService.notifyConfirmation(reservation)` depois de mudar o estado (`service/ReservationService.java:80`). No cancelamento, só chama `notifyCancellation` quando a reserva estava confirmada (`service/ReservationService.java:95-105`). É a regra de domínio "pendente cancelada não avisa ninguém", posta no lugar certo: o serviço decide quando há evento, não quem recebe. O serviço não monta nenhum e-mail e não conhece nenhum destinatário.

**O julgamento.** O desenho é exatamente o que as instruções chamam de acerto:
- uma interface de ouvinte com uma implementação por interessado;
- o hóspede também como ouvinte;
- a lista recebida de fora e registrada na montagem;
- a confirmação e o cancelamento passando pelo mesmo mecanismo.

Nada aproxima o desenho de malfeito: não sobra e-mail montado no serviço, nem chamada direta ao hóspede, nem cancelamento feito à mão. O detalhe menos elegante é que a interface tem dois métodos, o que obriga o hóspede a ter um `handleCancellation` vazio (`GuestEmailHandler.java:27-29`). Isso não desvia do pattern.

Quanto ao exagero colateral, o `NotificationService` faz o papel de sujeito do pattern e não é camada à toa. Fora dele não há interface de uma implementação só, fábrica nem builder.

Quanto ao comportamento, as regras de aviso estão certas, mas o gatilho de tudo, a confirmação, não funciona do jeito que o contrato descreve (`api/ReservationController.java:112-113`). Também há desvios menores nos relatórios e na segunda via.

## Item a item

**O pattern pedido.** A interface `NotificationHandler` (`notification/NotificationHandler.java:5-8`) tem três implementações, uma por interessado: `GuestEmailHandler`, `GoveranceNotificationHandler` e `FinanceNotificationHandler`. São registradas em `Application.java:25-27` num `NotificationService` que percorre a lista (`notification/NotificationService.java:14-24`). O `ReservationService` recebe esse sujeito pelo construtor (`service/ReservationService.java:21-24`) e só anuncia os eventos (`:80`, `:104`). Ele não monta mensagem nem conhece destinatário. O e-mail do hóspede, que já existia na parte comum do enunciado, também passou pela estrutura. Para pôr o restaurante ou o transfer, basta uma classe nova e uma linha no `main`. É o Observer que o texto pedia.
Conclusão: acerto

**O exagero colateral.** Não há. Fora do pattern, o código é até enxuto demais: um controller só e um modelo com campos públicos. Não aparece interface com uma implementação só, fábrica, builder ou camada que apenas repassa.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código, sem subir o serviço.

O que confere:
- **Os avisos**:
  - O e-mail do hóspede tem o assunto e o texto pedidos (`GuestEmailHandler.java:19-24`, `:61-66`).
  - A governança recebe "Reserva confirmada - quarto N", com entrada, saída e hóspedes (`GoveranceNotificationHandler.java:17-25`), e "Reserva cancelada - quarto N", com as datas (`:31-38`).
  - O financeiro recebe "Pagamento recebido - reserva N" (`FinanceNotificationHandler.java:17-20`) e "Reembolso - reserva N" só quando o reembolso é maior que zero (`:25-30`).
  - A reserva pendente cancelada não avisa ninguém (`ReservationService.java:98-105`).
- **As regras de estado e de reembolso** batem com o enunciado (`ReservationService.java:73`, `:89-103`, `:114`, `:126`).
- **O preço** segue o pedido: alta temporada com 20%, desconto de 15% e arredondamento HALF_EVEN a cada passo (`util/PriceCalculator.java`, `util/SeasonHelper.java`).

O que não confere:
- **A confirmação quebra o fluxo.** O `confirmReservation` lê o corpo e exige `amountPaid` (`api/ReservationController.java:112-113`). O contrato não prevê corpo nesse POST, e o enunciado diz que o valor pago é o total da reserva. Sem corpo, o Jackson falha, a exceção cai no `catch` genérico e a resposta é 400 INVALID_REQUEST. Assim nenhuma reserva confirma e nenhum aviso sai. Se o cliente mandar `amountPaid`, o sistema grava qualquer valor como pago (`ReservationService.java:78`).
- **O valor no e-mail do financeiro.** Ele mostra `reservation.total`, não o valor pago (`FinanceNotificationHandler.java:18`). Isso só coincide se `amountPaid` for igual ao total.
- **O relatório financeiro:**
  - Inclui reservas com valor zero, como as pendentes e as STANDARD canceladas, porque o filtro é `amount.signum() >= 0` (`service/RevenueReportService.java:35`). O pedido era só as que "deixaram dinheiro".
  - Escreve zero como `0` em vez de `0.00` (`util/FormatHelper.java:23-25`).
- **A segunda via** de uma reserva não paga responde 404 RESERVATION_NOT_FOUND (`api/ReservationController.java:172-175`), quando o esperado era um 409 OPERATION_NOT_ALLOWED.
- **O tipo do quarto no comprovante** sai de um `switch` duplicado, fora do `RoomType` (`GuestEmailHandler.java:52-59`). Hoje o resultado está certo.

Com as regras de aviso certas e a confirmação quebrada no contrato combinado, o comportamento segue só em parte.
Conclusão: em parte

## Dúvidas

- A gravidade da quebra na confirmação depende de como o site chama o endpoint. Se o site mandasse `amountPaid` igual ao total, tudo funcionaria. Pelo contrato do enunciado, não manda. Fiquei com "em parte", e não com "não segue", porque as regras pedidas para os avisos estão corretas e o resto da API funciona.
- O `formatCurrency` usa `String.format` com o locale padrão da JVM (`util/FormatHelper.java:19-21`). Num ambiente pt_BR, a troca de pontos e vírgulas inverteria o formato e produziria "R$ 1,234.56". Não deu para decidir por leitura em que ambiente o sistema roda.
- O enunciado diz que o projeto passou nos testes do próprio agente, mas não existe nenhum teste no projeto. Isso não muda as classes, só fica registrado.
