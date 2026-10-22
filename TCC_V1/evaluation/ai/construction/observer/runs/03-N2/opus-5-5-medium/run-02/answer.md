Já li o enunciado e o código todo; agora escrevo a correção.

# Cenário 03

## Visão geral

O agente montou um Observer limpo para os avisos da reserva. O `ReservationService` guarda uma lista de `ReservationListener` que recebe pronta do `Main` e só anuncia dois eventos tipados, `Confirmed` e `Cancelled`. Cada interessado é um ouvinte separado, inclusive o e-mail do hóspede, que já fazia parte da parte comum do enunciado: `GuestNotifier`, `HousekeepingNotifier` e `FinanceNotifier`. Cada ouvinte decide sozinho a que evento responde e com que mensagem. O desenho está bom. O problema mais sério está no comportamento, fora do pattern: o `ObjectMapper` foi configurado com `SNAKE_CASE`, e por isso todo o JSON da API sai e entra com nomes como `room_number` e `amount_paid`, e não `roomNumber` e `amountPaid` como foi combinado com o site. Há também valores no CSV financeiro sem as duas casas decimais e algumas recusas que caem em 500 em vez de 400.

## Como foi implementado, e por que o julgamento

**O projeto.** Os pacotes são estes:
- `domain`: `Reservation` com as transições de estado, `Room`, `RoomType` com capacidade e diária, `RatePlan`, `ReservationStatus`, `Message`, `MessageChannel`.
- `pricing`: o `PricingService` calcula as noites, a alta temporada, o desconto e o arredondamento `HALF_EVEN`.
- `repository`: repositórios em memória de quartos, reservas e mensagens.
- `service`: o `ReservationService` (criação, confirmação, cancelamento, check-in e check-out) e o `ReportService` (os CSVs).
- `notification`: os eventos, os ouvintes e a interface `MessageSender`.
- `api`: o `ApiController` registra as rotas Javalin, os DTOs e o mapeamento das exceções para os códigos de erro.

Uma requisição segue este caminho: a rota no `ApiController` (por exemplo `src/main/java/com/pousadamarealta/api/ApiController.java:117-121`) chama o `ReservationService`. O serviço busca a reserva no `ReservationRepository` e chama a transição em `Reservation` (`domain/Reservation.java:59-74`), que lança `IllegalStateException` quando a situação não permite; a exceção vira 409 em `ApiController.java:78-79`. Os dados ficam em listas na memória. O `Main` monta todas as peças à mão e sobe o servidor na porta de `PORT`, ou na 7070 se ela não vier (`Main.java:19-65`). O jar executável sai do `maven-shade-plugin` (`pom.xml:82-111`).

O agente escreveu três grupos de testes:
- `PricingServiceTest`: preços e temporadas;
- `ApiIntegrationTest`: rotas, erros e relatórios, com corpos em snake_case;
- `NotificationTest`: os três e-mails da confirmação, o cancelamento de reserva confirmada, o cancelamento de reserva pendente e o não reembolsável.

**A parte do pattern.** As peças são estas:
- `ReservationListener` (`notification/ReservationListener.java:3-5`) tem um método só, `onEvent(ReservationEvent)`.
- `ReservationEvent` é uma interface selada com dois records, `Confirmed(reservation)` e `Cancelled(reservation, wasConfirmed)` (`notification/ReservationEvent.java:5-10`).
- O `ReservationService` recebe `List<ReservationListener>` no construtor (`service/ReservationService.java:17-27`). Em `confirm` e `cancel` ele só publica o evento (`ReservationService.java:87` e `:95`) e percorre a lista em `notifyListeners` (`:116-120`). O serviço não monta nenhuma mensagem e não conhece endereço nenhum.
- O registro dos ouvintes acontece na montagem, em `Main.java:27-42`.

Cada regra mora no seu ouvinte:
- `GuestNotifier` reage só a `Confirmed` e monta o e-mail com o comprovante (`notification/GuestNotifier.java:18-43`).
- `HousekeepingNotifier` reage a `Confirmed` e a `Cancelled` quando `wasConfirmed` é verdadeiro (`notification/HousekeepingNotifier.java:17-26`).
- `FinanceNotifier` reage a `Confirmed` e a `Cancelled` quando a reserva estava confirmada e há reembolso maior que zero (`notification/FinanceNotifier.java:17-30`).

Os `switch` sobre o tipo de evento ficam dentro de cada ouvinte, que escolhe o que lhe interessa. Isso não é o serviço perguntando qual é o destinatário concreto. Para pôr o restaurante ou o transfer, basta uma classe nova e uma linha no `Main`, sem mexer no serviço.

**O raciocínio.** O desenho é o que as instruções chamam de acerto. Há uma interface de ouvinte com uma implementação por interessado, e o serviço percorre uma lista que recebe de fora. Não sobra nenhum dos sinais de malfeito:
- o hóspede também é ouvinte, sem chamada direta;
- o cancelamento passa pela mesma lista que a confirmação;
- o serviço não monta e-mail de ninguém.

Há uma sobra que não chega a mudar a classe: o `ApiController` recebe um `GuestNotifier` que nunca usa (`ApiController.java:30`, `:38`, `:44`; `Main.java:52`). É uma dependência morta, não um segundo caminho de envio.

No exagero colateral pesou a interface `MessageSender` (`notification/MessageSender.java:5-7`). Ela tem uma implementação só, o próprio `MessageRepository` (`repository/MessageRepository.java:11`), e nada a substitui, nem nos testes. É pequena.

No comportamento pesaram três coisas: a quebra do contrato JSON pelo `SNAKE_CASE`, o formato dos valores no CSV financeiro e as recusas que caem em 500.

## Item a item

**O pattern pedido.** Os eventos são tipados em `notification/ReservationEvent.java:5-10` e a interface de ouvinte está em `notification/ReservationListener.java:3-5`. Há três implementações, uma por interessado: `GuestNotifier.java:11`, `HousekeepingNotifier.java:9` e `FinanceNotifier.java:9`. O serviço recebe a lista de fora (`ReservationService.java:23-27`), publica na confirmação e no cancelamento (`:87`, `:95`) e percorre a lista sem conhecer os destinos (`:116-120`). O registro acontece no `Main` (`Main.java:31-35`). O e-mail do hóspede, que já existia na parte comum, também virou ouvinte. A recusa de avisar quando a reserva pendente é cancelada e a de avisar o financeiro sem reembolso ficam nos próprios ouvintes (`HousekeepingNotifier.java:22`, `FinanceNotifier.java:22`). É o Observer que o texto pedia, aplicado a todos os casos.
Conclusão: acerto

**O exagero colateral.** A interface `MessageSender` (`notification/MessageSender.java:5-7`) tem uma implementação só, `MessageRepository` (`repository/MessageRepository.java:11`), e nada a substitui no código nem nos testes. O repositório poderia ser passado direto aos ouvintes. O exagero é leve: uma interface de um método, sem fábrica nem camada que só repassa. A injeção de `GuestNotifier` no `ApiController` (`ApiController.java:30,44`) é código morto, não estrutura, e não pesou aqui.
Conclusão: exagero

**O comportamento.** A conferência foi só por leitura do código, sem subir o serviço nem rodar testes.

O que confere:
- **Confirmação.** O hóspede recebe o assunto e o texto pedidos, com o comprovante: a linha de desconto aparece só quando há desconto e "1 diária" sai no singular (`GuestNotifier.java:25-74`). A governança recebe "Reserva confirmada - quarto N" com entrada, saída e hóspedes (`HousekeepingNotifier.java:28-41`). O financeiro recebe "Pagamento recebido - reserva N" com o valor pago em R$ 1.234,56 (`FinanceNotifier.java:32-41`, `:54-56`).
- **Cancelamento de reserva confirmada.** A governança recebe "Reserva cancelada - quarto N" com as datas (`HousekeepingNotifier.java:44-57`). O financeiro recebe "Reembolso - reserva N" só quando o reembolso é maior que zero (`FinanceNotifier.java:22`, `:28-30`, `:43-52`).
- **Cancelamento de reserva pendente.** Não avisa ninguém, porque `wasConfirmed` é falso (`ReservationService.java:93`).
- **Regras gerais.** O reembolso é o valor pago no plano normal e zero no não reembolsável (`Reservation.java:73`). Preço, alta temporada e arredondamento conferem (`PricingService.java:139-177`). Também conferem os estados, a disponibilidade sem contar as canceladas (`ReservationRepository.java:119-125`) e o CSV de ocupação (`ReportService.java:23-47`).

O que não confere:
- **Nomes do JSON.** O `ObjectMapper` usa `PropertyNamingStrategies.SNAKE_CASE` (`ApiController.java:48-50`). Toda resposta sai com `room_number`, `guest_name`, `amount_paid`, `nightly_rate`, `reservation_id` e assim por diante, e o corpo de `POST /reservations` com `roomNumber` não é lido. Os testes do agente usam snake_case (`src/test/java/com/pousadamarealta/api/ApiIntegrationTest.java:92-93`, `:112`), então passam mesmo com o contrato combinado com o site quebrado.
- **Valores no CSV financeiro.** Saem com `stripTrailingZeros()` (`ReportService.java:81-83`): 779.80 vira `779.8`, e não `779.80` como pedido.
- **Recusas.** Corpo que não é JSON e data mal formada no corpo geram exceção do Jackson, que não está mapeada e vira 500 (só `DateTimeParseException` está mapeada, `ApiController.java:80-81`). Falta de `roomNumber` vira 0 e responde 404, não 400. Falta de datas dá `NullPointerException` (`ReservationService.java:53`), que também vira 500.
- **Zero no JSON.** `amountPaid` zero e `refund` zero saem como `0`, sem as 2 casas (`Reservation.java:55`, `:73`).

Os avisos da parte nova seguem o pedido, mas a API como um todo não segue o contrato.
Conclusão: em parte

## Dúvidas

1. **O `MessageSender` como exagero.** Fiquei entre exagero e contenção. A interface é mínima e funciona como porta de saída para os ouvintes, mas tem uma implementação só, que nada substitui. Isso é exatamente o caso que as instruções chamam de exagero, e foi o que decidi.
2. **O peso do snake_case.** O nome dos campos JSON está dito com todas as letras no enunciado, mas não sei se essa quebra deveria levar o comportamento a "não segue". Mantive "em parte" porque as regras de negócio e os avisos estão certos e o desvio está na serialização.
