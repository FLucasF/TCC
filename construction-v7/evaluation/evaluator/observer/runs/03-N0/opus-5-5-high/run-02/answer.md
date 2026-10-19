# Cenário 01

## Visão geral

O agente construiu os avisos à equipe com um Observer que funciona. O `ReservationService` só publica dois eventos (`ReservationConfirmedEvent` e `ReservationCancelledEvent`) e não sabe quem recebe. Cada interessado (hóspede, governança, financeiro) é uma implementação separada de `NotificationHandler`. O e-mail do hóspede também passou pela estrutura, e o cancelamento usa a mesma estrutura da confirmação. Testei pela API todas as regras da parte do fim e todas conferem. O ponto fraco do desenho é onde os ouvintes são registrados. O `NotificationService` cria e registra os três ouvintes no próprio construtor, em vez de recebê-los da montagem em `Application.main`. Para pôr o restaurante na lista, é preciso mexer nessa classe. No comportamento, a parte comum tem falhas: um corpo que não é JSON e um `ratePlan` inválido dão 500, quando deviam dar 400; e os valores zero saem como `0`, não como `0.00`.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `com.pousadamarealta` tem cinco partes:
- `Application` monta tudo e registra as rotas e os tratadores de exceção (`Application.java:19-96`).
- `controller` tem um controller por recurso: Room, Reservation, Message e Report.
- `service` tem `RoomService`, que guarda os cinco quartos fixos num `HashMap` (`RoomService.java:14-21`), `ReservationService`, `ReportService` e `NotificationService`, além de uma exceção por código de erro.
- `domain` tem `Reservation`, que calcula o preço e controla os estados, e os enums `RoomType` (com capacidade e diária), `RatePlan` e `ReservationStatus`.
- `dto` tem records com fábricas `from` para o JSON.

Uma requisição segue este caminho. Em `POST /reservations/{id}/confirmation`, `ReservationController.confirmReservation` (`ReservationController.java:63-67`) chama `ReservationService.confirmReservation` (`ReservationService.java:75-86`). Esse método chama `Reservation.confirm()` (`Reservation.java:95-101`) e depois `notificationService.notify(new ReservationConfirmedEvent(...))` (`ReservationService.java:81`). As transições de estado recusadas lançam `IllegalStateException`, que vira `OperationNotAllowedException` e chega ao cliente como 409.

Os dados ficam em memória: os mapas de reservas (`ReservationService.java:13`) e a lista de mensagens enviadas (`NotificationService.java:10`). O `main` cria os serviços e os controllers à mão (`Application.java:20-28`) e sobe o Javalin na porta da variável `PORT`, ou na 7070. O agente escreveu 12 testes de unidade sem HTTP: 8 em `ReservationTest` (preço, alta temporada, desconto, transições, reembolso) e 4 em `NotificationTest` (os avisos da confirmação e do cancelamento).

**A parte do fim.** As peças são estas:
- `ReservationEvent`, uma sealed interface com as duas implementações em record (`ReservationEvent.java:5`, `ReservationConfirmedEvent.java:5`, `ReservationCancelledEvent.java:6`). O evento de cancelamento leva o `previousStatus`.
- `NotificationHandler`, a interface do ouvinte, com `List<NotificationMessage> handle(ReservationEvent)` (`NotificationHandler.java:5-7`).
- Um ouvinte por interessado: `GuestNotificationHandler`, que monta o e-mail do hóspede com o comprovante (`GuestNotificationHandler.java:17-67`); `HousekeepingNotificationHandler`, que cuida da confirmação e, se o estado anterior era CONFIRMED, do cancelamento (`HousekeepingNotificationHandler.java:15-25`); e `FinanceNotificationHandler`, que cuida do pagamento e do reembolso só quando o reembolso é maior que zero (`FinanceNotificationHandler.java:17-29`).

Cada ouvinte decide sozinho a quais eventos reage. As regras de "pendente não avisa" e "sem reembolso não avisa o financeiro" ficam dentro dele, e o serviço de reservas não pergunta nada. O `NotificationService` faz o papel de distribuidor: percorre a lista, dá número às mensagens e as guarda (`NotificationService.java:23-37`). A confirmação (`ReservationService.java:81`) e o cancelamento (`ReservationService.java:120`) só publicam o evento.

**O julgamento.** O desenho tem o que define o Observer:
- uma interface de ouvinte com uma implementação por interessado;
- o hóspede tratado como ouvinte;
- os dois eventos passando pela mesma lista;
- nenhum e-mail montado no `ReservationService`.

Nenhum dos sinais de malfeito previstos aparece. Não há chamada direta para o hóspede, o cancelamento não manda nada à mão e o serviço não monta e-mail de nenhum interessado.

O que afasta o desenho do pattern ideal é o registro. Os ouvintes são criados com `new` dentro do construtor do `NotificationService` (`NotificationService.java:13-21`). Assim, o distribuidor conhece as classes concretas, e a lista não vem de fora nem é montada em `Application.main` (`Application.java:21`). Isso é uma falha de ligação num lugar só, e não uma convivência com o jeito sem o pattern. Para incluir um ouvinte, basta uma linha num lugar só, sem tocar em quem publica. Por isso fico com `acerto`, e registro a ressalva em "Dúvidas".

Há também código morto. Cada ouvinte tem um `messageIdCounter` próprio (`FinanceNotificationHandler.java:15,39,61`), que o distribuidor descarta ao renumerar as mensagens (`NotificationService.java:27-33`). O `formatMoney` está copiado em dois ouvintes. São defeitos de limpeza, não de desenho.

**O exagero colateral.** Não achei estrutura sem necessidade. As camadas controller, service e dto têm função real: os controllers fazem o parse e escolhem o DTO, e os serviços guardam o estado. Não há interface de uma implementação só nem fábrica fora da parte das notificações.

**O comportamento.** Todas as regras da parte do fim conferem. Na parte comum, conferem os preços (inclusive o exemplo de alta temporada, R$ 599,76), o comprovante, o e-mail do hóspede, os relatórios e quase todas as recusas. Há três falhas:
- Um corpo que não é JSON e um `ratePlan` desconhecido dão `500 Server Error`. O `bodyAsClass` (`ReservationController.java:26`) lança uma exceção que nenhum tratador de `Application.java:55-93` captura.
- Os zeros saem como `0`, não com duas casas: em `discount`, `amountPaid` e `refund` (`Reservation.java:42,92,128`).
- O relatório financeiro de um mês vazio termina em `total,0` (`ReportService.java:72,89,95`).

## Item a item

**O enunciado (pede o pattern).** O `ReservationService` publica `ReservationConfirmedEvent` (`ReservationService.java:81`) e `ReservationCancelledEvent` com o estado anterior (`ReservationService.java:116-120`), sem saber quem é avisado. O `NotificationService` percorre uma lista de `NotificationHandler` (`NotificationService.java:23-37`). Há um ouvinte para o hóspede (`GuestNotificationHandler.java:13`), um para a governança (`HousekeepingNotificationHandler.java:10`) e um para o financeiro (`FinanceNotificationHandler.java:13`). Cada um filtra os eventos e as condições que lhe dizem respeito: pendente cancelada em `HousekeepingNotificationHandler.java:20`, reembolso zero em `FinanceNotificationHandler.java:22-24`. Todos os casos passam pela estrutura, inclusive o e-mail do hóspede e o cancelamento. A ressalva é que os ouvintes são criados e registrados no construtor do próprio `NotificationService` (`NotificationService.java:13-21`), e não recebidos da montagem em `Application.java:21`. Isso deixa o pattern menos aberto, mas não reintroduz o jeito sem ele.
Conclusão: acerto

**O exagero colateral.** Não há. As camadas controller → service → domain e os DTOs `from(...)` (`ReservationDTO.java:28`, `DocumentDTO.java:16`) fazem trabalho real. As exceções por código de erro mapeiam direto para os tratadores (`Application.java:55-93`). Não há interface com implementação única, fábrica, builder nem camada que só repassa.
Conclusão: contenção

**O comportamento.** Testei numa cópia, com o jar `reservas-1.0.0.jar` rodando na porta 7070.

A parte do fim:
- **Confirmação da reserva 1 (201, de 10/03 a 12/03/2027, STANDARD):** saíram três mensagens. O hóspede recebeu o assunto "Reserva confirmada - Pousada Maré Alta", com o texto e o comprovante exatos. A governança recebeu "Reserva confirmada - quarto 201", com entrada em 10/03/2027, saída em 12/03/2027 e 2 hóspedes. O financeiro recebeu "Pagamento recebido - reserva 1", com o valor de R$ 779,80.
- **Cancelamento da reserva 1, confirmada e STANDARD:** a governança recebeu "Reserva cancelada - quarto 201", com as datas de 10/03/2027 a 12/03/2027. O financeiro recebeu "Reembolso - reserva 1", com R$ 779,80.
- **Cancelamento da reserva 3, confirmada e NON_REFUNDABLE:** só a governança foi avisada, e o `refund` ficou 0.
- **Cancelamento da reserva 5, pendente:** nenhuma mensagem saiu.

A parte comum:
- **Preços:** o STANDARD de 10/07 a 12/07/2027 deu 599.76. O NON_REFUNDABLE de uma noite no DELUXE em julho deu 467.88 − 70.18 = 397.70, e o e-mail trouxe "1 diária" e a linha "Desconto".
- **Segunda via:** com a reserva pendente, deu 409. Depois do check-in, deu 200. O check-out devolveu o comprovante.
- **Recusas que conferem:**
  - quarto ocupado: 409 ROOM_UNAVAILABLE;
  - quarto 999: 404 ROOM_NOT_FOUND;
  - 3 pessoas num STANDARD: 422;
  - 0 hóspedes, saída igual à entrada, data malformada e campo faltando: 400;
  - reserva 99: 404;
  - check-out de uma reserva confirmada: 409.
- **Relatórios:** o de ocupação de 2027-07-10 veio com `text/csv; charset=utf-8` e terminou em `total,1/5`. O financeiro de 2027-07 incluiu a reserva NR cancelada (397.70) e terminou em `total,2389.22`.
- **Recusas que não conferem:** o corpo `nao json` e o `ratePlan` "FOO" deram **500**, quando deviam dar 400.
- **Formato de valores:** `discount`, `amountPaid` e `refund` zero saem como `0`, não como `0.00`. O financeiro de um mês sem reservas termina em `total,0`.

O `mvn verify` da cópia passou com 12 testes e 0 falhas. Derrubei o serviço no fim. Toda a parte do fim confere, mas falha uma recusa que a parte comum pede explicitamente.
Conclusão: em parte

## Dúvidas

Fiquei entre `acerto` e `malfeito`. A definição de acerto pede que a lista de ouvintes venha de fora, registrada na montagem do sistema. Aqui ela é montada com `new` dentro do construtor do `NotificationService` (`NotificationService.java:13-21`), e por isso o distribuidor conhece as classes concretas e os testes não podem injetar ouvintes. Mesmo assim, nenhum dos sinais de malfeito previstos aparece: nenhum caso escapa da estrutura e quem publica não pergunta qual é o caso. Por isso a evidência sustenta melhor `acerto`. No comportamento, a conclusão `em parte` vem só da parte comum (os 500 e os zeros sem duas casas). A parte do fim está inteira certa.
