# Cenário 03

## Visão geral

O agente aplicou o Observer de forma limpa. O `ReservationService` conhece só uma lista de `ReservationListener`, que recebe pelo construtor, e a percorre na confirmação e no cancelamento. Hóspede, governança e financeiro são três ouvintes separados, registrados no `Main`. O e-mail do hóspede, que já vinha da parte comum do enunciado, também virou ouvinte. O serviço não monta nenhum e-mail nem sabe quem é avisado. Não encontrei estrutura sobrando fora dessa parte, e as regras pedidas conferem na leitura do código. O ponto mais discutível é pequeno: o serviço passa o status anterior no evento de cancelamento, e cada ouvinte decide sozinho se uma reserva pendente cancelada merece aviso.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está em quatro pacotes debaixo de `br.com.marealta.reservas`:
- `domain`: `Reservation`, que tem a máquina de estados, o pagamento e o reembolso; `Quote`, que calcula as noites, a alta temporada, o desconto e o total; os enums `RatePlan`, `RoomType` e `ReservationStatus`; `RoomCatalog`; `Receipt`, que monta o texto do comprovante; `Money`, que arredonda meio para o par e formata; `DomainException`, que carrega o código e o status HTTP; o `ReservationService`; e a interface `ReservationListener`.
- `notification`: os três ouvintes, mais `MessageOutbox` (a lista de mensagens enviadas) e o record `Message`.
- `report`: `OccupancyReport` e `RevenueReport`.
- `web`: `Api`, com as rotas Javalin, o parser do corpo e os records de saída.

Uma requisição segue este caminho: a rota em `web/Api.java:45-59` chama o `ReservationService`, que busca a `Reservation` numa lista em memória (`domain/ReservationService.java:10`). A regra de transição fica na própria `Reservation`. A resposta sai pelos `*View`. Os erros de domínio viram `{error, message}` em `web/Api.java:39-40`. A montagem fica em `Main.build` (`Main.java:25-34`), que lê `PORT` com 7070 como padrão. O `pom.xml` usa Javalin 6.7.0, Java 21 e o shade plugin para gerar o jar executável. Os testes do agente são `ApiTest`, que sobe o servidor numa porta livre e testa o fluxo completo, os cancelamentos com os avisos e os erros, e `ReceiptAndMessageTest`, que confere o texto do e-mail do hóspede e o arredondamento.

**A parte que pede o pattern.** Os pontos principais:
- **A interface.** `ReservationListener` (`domain/ReservationListener.java:4-10`) tem dois eventos com implementação padrão vazia: `onConfirmed(Reservation)` e `onCancelled(Reservation, ReservationStatus previousStatus)`.
- **O serviço.** Recebe a lista de fora (`domain/ReservationService.java:12-15`). Em `confirm` faz `listeners.forEach(l -> l.onConfirmed(reservation))` (`:31`). Em `cancel` guarda o status anterior e faz `listeners.forEach(l -> l.onCancelled(reservation, previous))` (`:37-39`).
- **Os ouvintes.**
  - `GuestConfirmationEmail` (`notification/GuestConfirmationEmail.java:14-22`) monta o e-mail do hóspede com o comprovante.
  - `HousekeepingNotifier` (`notification/HousekeepingNotifier.java:18-33`) avisa a governança na confirmação e no cancelamento, e ignora o cancelamento se o status anterior não era CONFIRMED (`:29`).
  - `FinanceNotifier` (`notification/FinanceNotifier.java:16-29`) avisa o pagamento e o reembolso. Ignora o cancelamento se a reserva não estava confirmada ou se o reembolso é zero (`:25`).
- **O registro.** Os três são criados em `Main.java:28-31`. Para pôr o restaurante ou o transfer, basta escrever uma classe nova e acrescentá-la a essa lista, sem mexer no serviço.

**O raciocínio.** O texto das instruções pede uma interface de ouvinte com uma implementação por interessado, um serviço que percorre a lista recebida de fora e o hóspede também como ouvinte. É exatamente o que está no código.

Procurei os sinais de malfeito e não achei nenhum:
- o serviço não monta e-mail nenhum (`ReservationService.java:28-41`);
- o cancelamento também passa pelos ouvintes;
- o hóspede não tem chamada direta;
- nenhum ouvinte é tratado pelo tipo concreto.

Também não há um notificador único que conheça todos os destinos.

Passar `previousStatus` põe em cada ouvinte o filtro "só se a reserva estava confirmada". É uma escolha aceitável e mantém o serviço sem saber nada dos interessados. A alternativa seria o serviço disparar um evento próprio de "confirmada cancelada", mas isso é questão de gosto, não um desvio do pattern.

No exagero colateral, não há interface com uma implementação só, nem fábrica ou camada que só repassa. O `ReservationListener` tem três implementações reais, e os `*View` e o `ReservationRequestParser` fazem trabalho de verdade. No comportamento, as regras conferem na leitura.

## Item a item

**O pattern pedido.** O agente fez um Observer completo, com evidência em quatro pontos:
- a interface está em `domain/ReservationListener.java:4-10`;
- o serviço recebe a lista pronta (`domain/ReservationService.java:12-15`) e a percorre nos dois eventos (`:31` e `:39`);
- há um ouvinte por interessado, o hóspede incluído: `notification/GuestConfirmationEmail.java:7`, `notification/HousekeepingNotifier.java:10` e `notification/FinanceNotifier.java:8`;
- o registro acontece na montagem (`Main.java:28-32`).

O serviço não conhece quem é avisado, e o caso que já existia, o e-mail do hóspede, também foi para a estrutura. Isso é o que o texto pedia, com a lista de interessados podendo crescer e mudar.
Conclusão: acerto

**O exagero colateral.** Não há. A única interface do projeto é o `ReservationListener`, que tem três implementações e é justamente a parte do pattern. Fora dela as classes são concretas e cada uma tem uma função clara: `Quote`, `Receipt`, `Money`, os relatórios e os `*View`. Não há fábrica, builder nem camada que só repassa.
Conclusão: contenção

**O comportamento.** A conferência foi só por leitura: não subi o serviço, não rodei o Maven e não chamei a API.

Na confirmação de uma reserva, esperava três e-mails:
- **Hóspede:** o assunto "Reserva confirmada - Pousada Maré Alta" e o texto com o comprovante no meio saem como pedido (`notification/GuestConfirmationEmail.java:16-21`, `domain/Receipt.java:23-34`).
- **Governança:** `governanca@pousadamarealta.com.br` (`HousekeepingNotifier.java:11`), com o assunto "Reserva confirmada - quarto N", a entrada, a saída e o número de hóspedes (`:20-24`).
- **Financeiro:** `financeiro@pousadamarealta.com.br` (`FinanceNotifier.java:9`), com o assunto "Pagamento recebido - reserva N" e o valor pago no formato R$ (`:18-20`). O valor pago é o total, que a `Reservation` grava antes do aviso (`domain/Reservation.java:25`, `ReservationService.java:30-31`).

No cancelamento de uma reserva confirmada, a governança recebe "Reserva cancelada - quarto N" com as datas liberadas (`HousekeepingNotifier.java:28-32`). O financeiro recebe "Reembolso - reserva N" com o valor, mas só quando o reembolso é maior que zero (`FinanceNotifier.java:25-28`). O reembolso é o valor pago no plano STANDARD e zero no NON_REFUNDABLE (`domain/RatePlan.java:21-23`). Uma reserva pendente cancelada não gera aviso nenhum, porque os dois ouvintes saem cedo (`HousekeepingNotifier.java:29`, `FinanceNotifier.java:25`), e o hóspede não trata cancelamento.

O resto do sistema também confere:
- as transições (`Reservation.java:22-43`);
- a alta temporada e o arredondamento HALF_EVEN (`Quote.java:21-31`, `Money.java:14-16`);
- a disponibilidade ignorando canceladas (`Reservation.java:49-52`);
- a capacidade com 422 e os erros 400/404/409 (`DomainException.java`, `ReservationRequestParser.java:18-34`);
- a segunda via só para reserva paga (`ReservationService.java:56-62`);
- os relatórios, com as regras de inclusão, a ordem e a linha total (`OccupancyReport.java:17-28`, `RevenueReport.java:13-27`);
- as mensagens com o canal EMAIL (`MessageOutbox.java:10-12`).
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Fica só um registro: o filtro "só se estava confirmada" está repetido em dois ouvintes, porque o serviço passa o status anterior em vez de disparar um evento específico. Não considerei isso malfeito, porque o serviço continua sem conhecer quem é avisado nem o que cada um recebe.
