# Cenário 01

## Visão geral

O agente fez do aviso à equipe um Observer de verdade. O `ReservationService` só publica um evento `Confirmed` ou `Cancelled` num `EventBus` que recebe pelo construtor, e não sabe quem é avisado. O hóspede, a governança e o financeiro são cinco ouvintes registrados um a um na montagem do sistema: três para a confirmação e dois para o cancelamento, com o e-mail do hóspede entre eles. Fora dessa parte o projeto é enxuto, sem interfaces nem camadas de repasse. Tudo o que testei pela API confere com o enunciado, e os 9 testes do agente passam. O único ponto discutível é que os cinco ouvintes são métodos de uma mesma classe, `Notifications`, que conhece todos os endereços. Mesmo assim o serviço não chama essa classe, e cada destino é uma inscrição separada no barramento.

## Como foi implementado, e por que o julgamento

**O projeto.** São cinco pacotes:
- `domain`: `Room`, `RoomType` (capacidade e diária no enum), `RatePlan` (desconto e se é reembolsável), `Reservation` (um record imutável; cada mudança de estado gera outra instância), `ReservationStatus`, `Pricing` (preço por noite e alta temporada), `Money` (arredondamento meio-para-o-par e o formato R$) e `Receipt` (o comprovante e o texto dele).
- `service`: `ReservationService` (quartos e reservas em memória, num `LinkedHashMap`, mais as transições de estado), `ReportService` (os dois CSV) e `DomainException` (status, código e mensagem).
- `event`: `EventBus` e `ReservationEvent`.
- `notification`: `MessageLog` (a lista de mensagens enviadas), `Message`, `Channel` e `Notifications` (os ouvintes).
- `web`: `Api`, que monta as peças e declara as rotas.

`Main` só lê a variável `PORT` e sobe a `Api` (`Main.java:7-8`). A montagem fica no construtor da `Api` (`web/Api.java:36-42`): ele cria o barramento, o `MessageLog`, o `ReservationService(bus)` e o `ReportService`, e chama `Notifications.register(bus, messages)`. Uma requisição entra pela rota (por exemplo `web/Api.java:53`), vai ao método do serviço, que valida a situação com `requireStatus` (`service/ReservationService.java:123-132`), troca a reserva no mapa e, na confirmação e no cancelamento, publica o evento. Os erros viram JSON num handler único (`web/Api.java:47`). O agente escreveu 5 testes de API, que sobem o Javalin numa porta livre e cobrem criação, validações, notificações, cancelamentos, ciclo de vida e relatórios, e 4 testes de preço.

**A parte do fim.** `ReservationEvent` é uma interface selada com dois records, `Confirmed` e `Cancelled(reservation, wasConfirmed)` (`event/ReservationEvent.java:6-15`). O `EventBus` guarda uma lista de `Consumer` por tipo de evento e entrega o evento a cada ouvinte; a falha de um ouvinte não impede os outros (`event/EventBus.java:21-38`). O serviço recebe o barramento de fora (`service/ReservationService.java:35-37`) e só publica:
- `events.publish(new ReservationEvent.Confirmed(confirmed))` em `service/ReservationService.java:86`;
- `events.publish(new ReservationEvent.Cancelled(cancelled, wasConfirmed))` em `service/ReservationService.java:110`.

O serviço não monta nenhum e-mail e não referencia `Notifications` nem os endereços. As cinco inscrições ficam em `notification/Notifications.java:22-26`. Cada uma é um método próprio com a sua regra:
- hóspede: `guestConfirmed`, em `:33-41`;
- governança na confirmação: `governanceConfirmed`, em `:43-51`;
- financeiro na confirmação: `financeConfirmed`, em `:53-57`;
- governança no cancelamento: `governanceCancelled`, em `:59-68`, que ignora a reserva que estava pendente;
- financeiro no cancelamento: `financeCancelled`, em `:70-77`, que ignora a reserva pendente e o reembolso zero.

O cálculo do reembolso continua no serviço (`service/ReservationService.java:107`), que é onde ele deve estar.

**O julgamento.** As instruções do pattern dizem o que conta como acerto, e o projeto atende a cada ponto:
- há uma interface de ouvinte (o `Consumer<? super E>`, sobre eventos tipados);
- há uma implementação por interessado (uma inscrição para cada destino e evento);
- o serviço percorre a lista de ouvintes por meio do barramento que recebe de fora;
- o e-mail do hóspede também é um ouvinte;
- o registro acontece na montagem.

Nenhum dos sinais de malfeito aparece: o serviço não monta e-mail da governança ou do financeiro, o cancelamento também passa pelos ouvintes, e o hóspede não tem chamada direta. O que afasta o projeto do desenho ideal é agrupar os ouvintes numa só classe que conhece os endereços (`notification/Notifications.java:11-12`). Isso não é o "notificador único" que as instruções chamam de omissão, porque ninguém chama essa classe com a reserva para ela disparar tudo. A classe só registra ouvintes independentes no barramento, e um restaurante ou um transfer entraria com mais uma inscrição, sem tocar no serviço. O `EventBus` genérico faz parte do pattern, por isso não conta como exagero colateral. Fora dele não há interface de implementação única, fábrica, builder ou camada de repasse. `ReportService` tem lógica própria, e `Pricing` e `Money` são utilitários estáticos. No comportamento, as regras do fim do enunciado e as da parte comum que testei conferem.

## Item a item

**O enunciado (pede o pattern).** O serviço publica eventos num barramento que recebe pelo construtor (`service/ReservationService.java:35-37`, `:86`, `:110`) e não conhece os destinatários. O barramento entrega o evento a cada ouvinte inscrito no tipo (`event/EventBus.java:25-38`). Os cinco ouvintes, contando o do hóspede, são inscritos separadamente (`notification/Notifications.java:22-26`). A inscrição é chamada na montagem (`web/Api.java:41`). Cada regra mora no seu ouvinte, inclusive o filtro de "só reserva que estava confirmada" e de "só com algo a devolver" (`notification/Notifications.java:60`, `:72`). É o Observer que o enunciado precisava: um aviso novo entra como mais uma inscrição, sem mudar o serviço.
Conclusão: acerto

**O exagero colateral.** Não há. Não existem interfaces de implementação única, fábricas, builders nem camadas que só repassam. A `Api` concentra a montagem e as rotas (`web/Api.java:36-63`), e os utilitários de domínio são classes finais estáticas (`domain/Pricing.java`, `domain/Money.java`).
Conclusão: contenção

**O comportamento.** Subi o jar da cópia na porta 7070 e chamei a API com `curl`.
- **Parte comum:**
  - Um STANDARD de 10/07 a 12/07/2027 deu total 599.76, como no exemplo do enunciado.
  - O quarto 301 de 14/12 a 16/12/2027, NON_REFUNDABLE, deu subtotal 1275.78, desconto 191.37 e total 1084.41: só a noite de 15/12 tem os 20% de alta temporada.
  - As recusas vieram certas: 409 `ROOM_UNAVAILABLE`, 422 `CAPACITY_EXCEEDED`, 404 `ROOM_NOT_FOUND`, 400 `INVALID_REQUEST` para corpo que não é JSON e para mês "2027-7", 404 `RESERVATION_NOT_FOUND`, e 409 `OPERATION_NOT_ALLOWED` ao cancelar de novo e ao pedir a segunda via de reserva pendente.
  - A segunda via e o check-out devolvem o comprovante com os campos pedidos.
  - A ocupação de 2027-07-11 veio `101,STANDARD,1,...` e `total,1/5`, com o cabeçalho `text/csv; charset=utf-8`.
  - O financeiro de 2027-12 traz a reserva NON_REFUNDABLE cancelada com 1084.41. O de 2027-03, com a reserva reembolsada, veio só com `total,0.00`.
- **Confirmação da reserva 2** (quarto 201): saíram 3 mensagens EMAIL.
  - Ao hóspede, o texto do enunciado, com o comprovante idêntico. Na reserva 3, entrou a linha "Desconto: R$ 191,37" e o valor "R$ 1.275,78".
  - À governança, "Reserva confirmada - quarto 201", com Entrada 10/03/2027, Saída 12/03/2027 e Hóspedes 2.
  - Ao financeiro, "Pagamento recebido - reserva 2", com R$ 779,80.
- **Cancelamentos:**
  - A reserva 4, pendente, foi cancelada sem nenhuma mensagem (a lista continuou com 6).
  - A reserva 2, confirmada e STANDARD, gerou "Reserva cancelada - quarto 201", com as datas liberadas, e "Reembolso - reserva 2", com R$ 779,80.
  - A reserva 3, confirmada e NON_REFUNDABLE, gerou só "Reserva cancelada - quarto 301". O financeiro não foi avisado, porque o reembolso é 0,00.
- **Testes do agente:** o `mvn verify` da cópia rodou 9 testes, sem falhas nem erros: BUILD SUCCESS. Depois dos testes, derrubei o serviço.
Conclusão: segue

## Dúvidas

Os cinco ouvintes são métodos de uma única classe, `Notifications`, que tem como constantes os endereços da governança e do financeiro (`notification/Notifications.java:11-12`, `:22-26`). Uma leitura rígida de "uma implementação por interessado" pediria uma classe por destino. Julguei como acerto porque cada destino é uma inscrição independente no barramento, e o serviço não chama nem conhece essa classe. Isso a separa do "notificador único" que as instruções chamam de omissão.
