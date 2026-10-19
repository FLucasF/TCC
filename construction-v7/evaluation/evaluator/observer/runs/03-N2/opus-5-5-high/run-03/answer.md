# Cenário 01

## Visão geral

O agente construiu os avisos da equipe com um Observer completo. O `ReservationService` publica um evento `CONFIRMED` ou `CANCELLED` num `ReservationEventPublisher` que recebe de fora. O hóspede, a governança e o financeiro são três ouvintes separados, registrados no `main`. O serviço não sabe quem é avisado e não monta nenhum e-mail. Todas as regras do fim do enunciado funcionaram no teste: destinatários, assuntos, conteúdo, reembolso só quando há valor, e nenhum aviso ao cancelar uma reserva pendente. Não há estrutura sobrando fora dessa parte. Os problemas mais sérios estão na parte comum do sistema:
- o e-mail do hóspede não tem a linha em branco antes de "A entrada é a partir das 14h";
- o relatório financeiro escreve `509.8` em vez de `509.80`;
- um corpo que não é JSON devolve 500 em vez de 400;
- a segunda via sai para reserva cancelada.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica em `com.pousadamarealta`:
- `Application` monta tudo e declara as rotas Javalin no `main`.
- `api/dto` tem os DTOs de entrada e saída, cada um com um método estático `from`.
- `model` tem `Reservation`, `Room`, os enums e `Message`.
- `exception/ApiException` carrega o código, a mensagem e o status HTTP.
- `service` tem três serviços e uma caixa de saída de e-mails. `PricingService` calcula as diárias, a alta temporada, o desconto e o arredondamento HALF_EVEN. `ReservationService` guarda quartos e reservas em `HashMap` na memória e cuida das transições de status. `ReportService` gera os dois CSVs. `EmailService` guarda a lista de mensagens enviadas e gera o texto do comprovante.
- `observer` tem a parte do fim do enunciado.

Uma requisição segue este caminho: a rota em `Application.java` valida o corpo (`Application.java:180-205`), chama o `ReservationService` e devolve o DTO. Os erros viram `{error, message}` no handler de `ApiException` (`Application.java:168-171`). O `main` cria as peças à mão (`Application.java:26-34`) e sobe na porta da variável `PORT`, ou na 7070 (`Application.java:215-225`). O shade gera `reservations-1.0.0.jar` com a classe principal no manifesto.

O agente escreveu três classes de teste: `PricingServiceTest` (6 testes), `ReservationServiceTest` (8) e `ObserverTest` (4). Os testes de Observer montam os ouvintes do mesmo jeito que o `main` (`ObserverTest.java:22-31`).

**A parte do fim do enunciado.**
- O contrato é a interface `ReservationObserver` com `onReservationEvent(ReservationEvent)` (`observer/ReservationObserver.java:3-5`).
- O evento leva o tipo (`ReservationEventType`: `CONFIRMED` ou `CANCELLED`), a reserva e uma cópia do estado anterior (`observer/ReservationEvent.java:5-14`).
- O sujeito é `ReservationEventPublisher`: uma lista de ouvintes com `attach` e `detach`, e um `notify` que percorre a lista (`observer/ReservationEventPublisher.java:7-23`).
- O `ReservationService` recebe o publisher no construtor (`service/ReservationService.java:22-24`). Ele só faz `eventPublisher.notify(...)` ao confirmar (`service/ReservationService.java:99`) e ao cancelar (`service/ReservationService.java:127`).

Cada interessado é uma classe própria, e as regras de cada um moram nela:
- `GuestNotificationObserver` monta o e-mail do hóspede com o comprovante, só no `CONFIRMED` (`observer/GuestNotificationObserver.java:17-38`).
- `HousekeepingNotificationObserver` manda "Reserva confirmada - quarto N" com as datas e os hóspedes. No cancelamento, só se o estado anterior era `CONFIRMED`, manda "Reserva cancelada - quarto N" com as datas liberadas (`observer/HousekeepingNotificationObserver.java:20-48`).
- `FinanceNotificationObserver` manda "Pagamento recebido - reserva N" com o valor pago. No cancelamento, só se a reserva estava confirmada e o reembolso é maior que zero, manda "Reembolso - reserva N" (`observer/FinanceNotificationObserver.java:22-47`).

Os três são registrados no `main` (`Application.java:32-34`).

**O raciocínio.** Isso é o que as instruções chamam de acerto:
- há uma interface de ouvinte com uma implementação por interessado;
- o serviço percorre uma lista que recebe de fora;
- o e-mail do hóspede também é um ouvinte, e não uma chamada direta;
- confirmação e cancelamento passam pela mesma estrutura;
- o serviço não monta nenhum e-mail da governança ou do financeiro.

Para o restaurante ou o transfer, bastaria uma classe nova e uma linha de `attach`. Os `switch` sobre o tipo de evento dentro de cada ouvinte não afastam o desenho do pattern. Cada ouvinte decide a quais eventos reage, e ninguém pergunta qual é o ouvinte concreto. O único ponto discutível é o filtro "estava confirmada" repetido em dois ouvintes (`HousekeepingNotificationObserver.java:24-25`, `FinanceNotificationObserver.java:26-27`): o serviço publica `CANCELLED` também para reserva pendente. Isso é uma escolha de onde filtrar, não uma volta ao jeito sem pattern.

Fora dessa parte, não achei interface de implementação única, fábrica, builder nem camada de repasse. Os serviços são classes concretas e os DTOs são simples. No comportamento, a parte do fim está toda certa, mas a parte comum tem desvios concretos (detalhados no item abaixo), por isso "em parte".

## Item a item

**O enunciado (pede o pattern).**
- Há a interface `ReservationObserver` (`observer/ReservationObserver.java:3-5`) e três implementações, uma por interessado: `GuestNotificationObserver.java:7`, `HousekeepingNotificationObserver.java:9` e `FinanceNotificationObserver.java:12`.
- O sujeito `ReservationEventPublisher` percorre a lista (`observer/ReservationEventPublisher.java:19-23`) e é injetado no serviço (`service/ReservationService.java:22-24`).
- A confirmação e o cancelamento só publicam eventos (`service/ReservationService.java:99`, `service/ReservationService.java:127`).
- Os ouvintes são registrados na montagem (`Application.java:32-34`).
- O e-mail do hóspede também é um ouvinte, e o serviço não conhece nenhum destino nem monta nenhum texto.

É o Observer como as instruções descrevem, aplicado a todos os casos, inclusive ao que a parte comum já trazia.

Conclusão: acerto

**O exagero colateral.** Não há:
- `PricingService`, `ReportService` e `EmailService` são classes concretas sem interface (`service/PricingService.java:10`, `service/ReportService.java:15`, `service/EmailService.java:15`);
- os DTOs só têm o `from` (`api/dto/ReservationResponse.java:28`);
- não há fábricas, builders nem camadas de repasse.

O `detach` não usado (`observer/ReservationEventPublisher.java:15`) e a cópia do estado anterior (`service/ReservationService.java:171-191`) ficam dentro da parte do pattern.

Conclusão: contenção

**O comportamento.** Testei uma cópia (`mvn -q package -DskipTests`, `java -jar target/reservations-1.0.0.jar`, porta 7070) com `curl` e `jq`.

Na parte do fim, tudo veio como pedido:
- **Confirmação da reserva 1 (quarto 201):** saíram três e-mails.
  - Hóspede: "Reserva confirmada - Pousada Maré Alta".
  - `governanca@pousadamarealta.com.br`: "Reserva confirmada - quarto 201", corpo "Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 hóspedes".
  - `financeiro@pousadamarealta.com.br`: "Pagamento recebido - reserva 1", corpo "Valor: R$ 779,80".
- **Cancelamento da reserva confirmada 2 (STANDARD):** "Reserva cancelada - quarto 101" com "Datas liberadas: 10/07/2027 a 12/07/2027", e "Reembolso - reserva 2" com "Valor a devolver: R$ 599,76".
- **Cancelamento da confirmada 3 (NON_REFUNDABLE):** só a governança foi avisada, porque não há reembolso.
- **Cancelamento da pendente 4:** a lista continuou com o mesmo número de mensagens.

Na parte comum, o que conferiu:
- os preços: 10–12/07/2027 STANDARD dá 599,76; a suíte não reembolsável em julho dá 695,88 − 104,38 = 591,50;
- os erros: 409 `ROOM_UNAVAILABLE`, 422 `CAPACITY_EXCEEDED`, 404 `ROOM_NOT_FOUND` e `RESERVATION_NOT_FOUND`, 400 para guests 0, saída igual à entrada, data fora do formato e falta de campo;
- as recusas de transição: 409 `OPERATION_NOT_ALLOWED` ao confirmar de novo, ao fazer check-in de pendente e ao cancelar depois do check-in;
- o check-out devolve o comprovante;
- o relatório de ocupação vem com `text/csv; charset=utf-8` e `total,1/5`.

O que não conferiu:
- **E-mail do hóspede:** falta a linha em branco entre "Total: R$ ..." e "A entrada é a partir das 14h…". O comprovante não termina em `\n` (`service/EmailService.java:47`) e o formato junta com um `\n` só (`observer/GuestNotificationObserver.java:34`).
- **Relatório financeiro:** os valores saem como `509.8`, `591.5` e `779.8`, por causa de `stripTrailingZeros` (`service/ReportService.java:95`).
- **Corpo que não é JSON:** dá 500 `INTERNAL_ERROR` em vez de 400 `INVALID_REQUEST` (`Application.java:55`, `Application.java:173-177`).
- **Segunda via:** sai com 200 para reserva cancelada não reembolsável e também para cancelada com reembolso. A checagem olha só `amountPaid` (`Application.java:120`).
- **Zeros no JSON:** `discount` e `amountPaid` vão como `0`, e não com 2 casas (`service/PricingService.java:37`).

O `mvn -q verify` da cópia passou: 18 testes, 0 falhas (PricingServiceTest 6, ObserverTest 4, ReservationServiceTest 8). Derrubei o serviço no fim.

Conclusão: em parte

## Dúvidas

Nenhuma sobre a classe. A única dúvida é de peso: as regras do fim do enunciado estão todas certas, e só a parte comum falha. Ainda assim, essas falhas atingem coisas que o enunciado define com exatidão (o texto do e-mail, o formato do CSV, o 400 para corpo inválido, a regra da segunda via), por isso o comportamento ficou em "em parte" e não em "segue".
