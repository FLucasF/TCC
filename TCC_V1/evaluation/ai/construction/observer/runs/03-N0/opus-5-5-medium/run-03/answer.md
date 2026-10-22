# Cenário 03

## Visão geral

O agente resolveu os avisos com um Observer limpo. O `ReservationService` só publica um evento de "reserva confirmada" ou de "reserva cancelada" num barramento, o `EventService`, e não sabe quem recebe. São cinco ouvintes separados: o e-mail do hóspede, a governança e o financeiro na confirmação, e a governança e o financeiro no cancelamento. Todos são registrados no `main`. O e-mail do hóspede, que é o caso que já existia, também virou ouvinte. O desenho é o que as instruções pedem, e as regras dos avisos conferem na leitura. Os problemas estão no resto do sistema: a criação da reserva aceita sem reclamar nome, e-mail ou CPF faltando, e o campo `refund` some do JSON em vez de vir `null`. Também há defeitos menores: um id que não é número dá 500, e o valor em reais é formatado de um jeito que depende do locale da máquina.

## Como foi implementado, e por que o julgamento

**O projeto.** Tudo fica em `com.pousadamarealta`, dividido em pacotes:
- `controller`: os handlers do Javalin para quartos, reservas, mensagens e relatórios.
- `service`: `RoomService` (os cinco quartos fixos), `PricingService` (diárias, alta temporada, desconto, arredondamento HALF_EVEN), `ReservationService` (regras e estados), `MessageService` (a lista de mensagens enviadas), `ReportService` (os CSVs) e `EventService` (o barramento de eventos).
- `domain`: `Reservation`, `Room`, e os enums `RoomType`, `RatePlan` e `ReservationStatus`, que guardam a capacidade, a diária, o percentual de desconto e se o plano devolve dinheiro.
- `dto`: os records de entrada e de saída.
- `event`: a parte do pattern.

Uma requisição vai da rota em `App.java:41-54` para o controller. O controller lê o corpo, chama o `ReservationService` e converte as exceções internas no código de erro. Por exemplo, `ReservationController.java:262-271` faz isso na criação. Os dados ficam em `HashMap`s na memória, dentro dos serviços. Tudo é montado à mão no `App.main` (`App.java:17-29`), que lê `PORT` e usa 7070 se ela não vier (`App.java:66-76`). O agente escreveu dois testes: `PricingServiceTest` (preço e desconto) e `ReservationFlowTest`. Este último monta as peças e registra os mesmos ouvintes (`ReservationFlowTest.java:33-37`). Ele confere o fluxo, o reembolso e as recusas, e também que a confirmação gera 3 mensagens (`ReservationFlowTest.java:62`).

**A parte que pede o pattern.** Peças envolvidas:
- **Contrato do ouvinte:** a interface genérica `EventHandler<T extends ReservationEvent>` (`event/EventHandler.java:3-5`).
- **Eventos:** `ReservationEvent` é a classe base e carrega a reserva (`event/ReservationEvent.java:5-15`). Há duas subclasses. `ReservationConfirmedEvent` (`event/ReservationConfirmedEvent.java:5`) e `ReservationCancelledEvent`, que também guarda o status anterior e expõe `wasConfirmed()` (`event/ReservationCancelledEvent.java:6-20`).
- **Barramento:** o `EventService` guarda um mapa do tipo do evento para a lista de ouvintes. `register` acrescenta um ouvinte (`service/EventService.java:14-16`) e `publish` percorre a lista daquele tipo (`service/EventService.java:19-26`).
- **Ouvintes:** um por interessado e por evento.
  - `GuestNotificationHandler`: o e-mail do hóspede com o comprovante (`event/GuestNotificationHandler.java:24-40`).
  - `HousekeepingNotificationHandler`: a governança na confirmação (`event/HousekeepingNotificationHandler.java:22-34`).
  - `FinanceConfirmationHandler`: o financeiro na confirmação (`event/FinanceConfirmationHandler.java:19-26`).
  - `HousekeepingCancellationHandler`: a governança no cancelamento, saindo cedo se a reserva não estava confirmada (`event/HousekeepingCancellationHandler.java:20-35`).
  - `FinanceCancellationHandler`: o financeiro no cancelamento, saindo cedo se não há reembolso (`event/FinanceCancellationHandler.java:19-30`).
- **Registro:** os cinco ouvintes são registrados em `App.registerEventHandlers` (`App.java:57-64`).

O serviço só faz `eventService.publish(new ReservationConfirmedEvent(reservation))` (`service/ReservationService.java:86`) e `eventService.publish(new ReservationCancelledEvent(reservation, previousStatus))` (`service/ReservationService.java:101`). Ele não monta nenhum e-mail e não tem nenhuma referência ao `MessageService`.

**Raciocínio do julgamento.** Os três pontos que as instruções chamam de acerto estão todos aqui:
- Existe uma interface de ouvinte com uma implementação por interessado.
- O serviço recebe de fora a lista de ouvintes, por meio do `EventService` injetado no construtor (`service/ReservationService.java:20-24`), e só a percorre.
- O e-mail do hóspede também é ouvinte. Ele não ficou como chamada direta.

Nenhum dos sinais de malfeito aparece. O serviço não monta e-mail da governança nem do financeiro. O cancelamento também passa por ouvintes. Nada pergunta qual é o caso concreto: a escolha do ouvinte vem do tipo do evento, e a regra "só avisa se estava confirmada" ou "se há algo a devolver" mora dentro de cada ouvinte. Para pôr o restaurante ou o transfer, basta uma classe nova e uma linha no `main`.

Um detalhe que não muda a classe: o evento de cancelamento também é publicado para reservas pendentes, e os ouvintes filtram. Do jeito que está, não sai nenhum aviso indevido.

No exagero colateral, o `EventService` com mapa por tipo de evento e a classe base `ReservationEvent` fazem parte da própria estrutura do pattern. Fora dela não há fábrica, builder, interface com uma implementação só nem camada que só repassa. Há repetição, como o `formatMoney` copiado em três ouvintes, mas repetição não é estrutura demais.

O comportamento pesou para "em parte". As regras dos avisos conferem, mas há desvios do contrato em outras partes do sistema, detalhados no item abaixo.

## Item a item

**O pattern pedido.** O `ReservationService` só publica eventos (`service/ReservationService.java:86` e `:101`) no `EventService`. Esse barramento guarda listas de `EventHandler` por tipo de evento e as percorre (`service/EventService.java:14-26`). Cada interessado é um ouvinte próprio: o hóspede (`event/GuestNotificationHandler.java:13`), a governança (`event/HousekeepingNotificationHandler.java:9`, `event/HousekeepingCancellationHandler.java:9`) e o financeiro (`event/FinanceConfirmationHandler.java:9`, `event/FinanceCancellationHandler.java:9`). Todos são registrados na montagem (`App.java:57-64`). Com isso o serviço não conhece os destinos, o caso que já existia (o hóspede) também passou pela estrutura, e confirmação e cancelamento seguem o mesmo mecanismo. É o Observer que o texto pedia.
Conclusão: acerto

**O exagero colateral.** Não há. O `EventService` (`service/EventService.java:11`) e a classe base de evento (`event/ReservationEvent.java:5`) fazem parte do pattern. Os serviços e controllers fora dele são concretos e cada um tem papel próprio. A repetição do `formatMoney` (`event/GuestNotificationHandler.java:80-82`, `event/FinanceConfirmationHandler.java:28-30`, `event/FinanceCancellationHandler.java:32-34`) é duplicação, não estrutura sem necessidade.
Conclusão: contenção

**O comportamento.** A conferência foi feita só por leitura do código, sem subir o serviço e sem chamar a API.

O que confere:
- **Confirmação:** `Reservation.confirm` põe `amountPaid = total` (`domain/Reservation.java:45-51`) e os três ouvintes disparam.
  - O hóspede recebe o assunto "Reserva confirmada - Pousada Maré Alta" e o texto com o comprovante, incluindo a linha "Desconto" só quando há desconto e "diária/diárias" (`event/GuestNotificationHandler.java:28-60`).
  - A governança recebe em `governanca@pousadamarealta.com.br` o assunto "Reserva confirmada - quarto N", com entrada, saída e hóspedes (`event/HousekeepingNotificationHandler.java:12`, `:25-31`).
  - O financeiro recebe em `financeiro@pousadamarealta.com.br` o assunto "Pagamento recebido - reserva N", com o valor pago (`event/FinanceConfirmationHandler.java:10`, `:22-23`).
- **Cancelamento de uma reserva confirmada:**
  - A governança recebe "Reserva cancelada - quarto N" com as datas liberadas (`event/HousekeepingCancellationHandler.java:21-32`).
  - O financeiro recebe "Reembolso - reserva N" só se `refund > 0` (`event/FinanceCancellationHandler.java:22-27`). O `refund` é o valor pago no plano STANDARD e zero no NON_REFUNDABLE ou se a reserva estava pendente (`domain/Reservation.java:72-76`).
- **Cancelamento de uma reserva pendente:** não avisa ninguém, porque a governança sai por `wasConfirmed()` e o financeiro sai por reembolso zero.
- **Resto do sistema:**
  - O preço segue as regras (`service/PricingService.java`), com HALF_EVEN a cada passo e a alta temporada certa.
  - A sobreposição de noites considera só as reservas não canceladas (`service/ReservationService.java:150-160`).
  - Os relatórios usam as colunas e a linha `total` pedidas (`service/ReportService.java:393-406`, `:415-435`).

Os desvios:
- **Campos faltando:** a criação não confere `guestName`, `guestEmail` e `guestCpf` (`service/ReservationService.java:26-46`), então uma reserva sem esses campos é criada em vez de dar 400. Além disso, `roomNumber` e `guests` são `int` (`dto/CreateReservationRequest.java:6`, `:12`). Sem `roomNumber`, o Jackson põe 0 e a resposta é 404 ROOM_NOT_FOUND em vez de 400.
- **`refund` e valores zerados:** `@JsonInclude(NON_NULL)` (`dto/ReservationResponse.java:11`) tira o `refund` do JSON em vez de mandá-lo como `null`. `amountPaid` e `discount` zerados saem como `0`, não com 2 casas.
- **Id não numérico:** `Long.parseLong` sem tratamento (`controller/ReservationController.java:58`, `:69`, etc.) faz um id não numérico dar 500.
- **Formato do dinheiro:** `String.format("R$ %,.2f")` usa o locale padrão e depois troca vírgula por ponto (`event/GuestNotificationHandler.java:81`). Isso só sai "R$ 1.234,56" se a JVM rodar em locale de língua inglesa. Em pt-BR, sairia "R$ 1,234.56".

Conclusão: em parte

## Dúvidas

- **Peso dos desvios no comportamento:** os desvios estão fora da parte do pattern. Os avisos em si conferem na leitura. Se o critério olhasse só para as regras do cenário, o item ficaria "segue". Escolhi "em parte" porque a instrução manda conferir também se o resto do sistema continua funcionando como pedido.
- **Formato do dinheiro:** o resultado depende do locale de quem roda o jar. Não dá para decidir isso só pela leitura.
