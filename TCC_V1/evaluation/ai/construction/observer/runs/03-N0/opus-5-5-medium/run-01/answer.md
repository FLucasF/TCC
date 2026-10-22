# Cenário 03

## Visão geral

O agente montou o Observer completo e limpo. O `ReservationService` só publica dois eventos, a confirmação e o cancelamento de uma reserva que estava confirmada, num `EventBus` que ele recebe de fora. Cada interessado é um ouvinte separado, que implementa `ReservationListener`, e são três: o hóspede, a governança e o financeiro. Os três são registrados no `Main`. O e-mail do hóspede, que já vinha da parte comum do enunciado, também virou ouvinte. O serviço não monta nenhum e-mail e não sabe quem é avisado. Não há estrutura sobrando fora do pattern. Pela leitura, as regras pedidas conferem: assuntos, endereços, conteúdo, o reembolso só quando há valor, e nenhum aviso quando se cancela uma reserva pendente. Não achei problema sério.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote raiz `br.com.marealta.pousada` tem duas classes:
- `Main` monta as peças e sobe o servidor.
- `ReservationService` guarda os quartos e as reservas numa `ArrayList` em memória. Ele cria, confirma, cancela, faz o check-in e o check-out, emite o comprovante e gera os dois CSVs.

O pacote `domain` tem:
- `Reservation`, a máquina de estados, que guarda o valor pago e o reembolso;
- `Pricing`, que calcula as noites, a alta temporada e o desconto, com HALF_EVEN a cada passo;
- `Money`, que arredonda e formata "R$ 1.234,56";
- `Receipt`, o comprovante, com o texto pronto;
- `Message`, os enums e `ApiException`.

O pacote `web` tem `Api`, que registra as rotas do Javalin, converte o JSON, valida a entrada e traduz `ApiException` para `{error, message}`.

O pacote `events` tem:
- `ReservationEvent`, uma interface sealed com os records `Confirmed` e `CancelledAfterConfirmation`;
- `ReservationListener`, a interface dos ouvintes;
- `EventBus`, a lista de ouvintes com `subscribe` e `publish`;
- `Outbox`, a lista das mensagens enviadas;
- os três ouvintes: `GuestConfirmationNotifier`, `HousekeepingNotifier` e `FinanceNotifier`.

**O caminho de uma requisição.** Uma chamada como `POST /reservations/{id}/confirmation` entra pela `Api` (`web/Api.java:50`) e chega a `service.confirm`. O serviço chama `Reservation.confirm()`, que confere o estado e registra o valor pago (`domain/Reservation.java:66-70`), e em seguida publica `Confirmed` no bus (`ReservationService.java:79`). `GET /messages` lê o `Outbox` (`web/Api.java:55`).

**A montagem.** O `Main.build()` cria o `Outbox` e o `EventBus`, assina os três ouvintes, cria o serviço com o bus e devolve o Javalin (`Main.java:9-17`). A porta vem de `PORT` ou é 7070 (`Main.java:20-21`). O jar sobe sozinho com o shade plugin, com `mainClass` no `pom.xml:63`.

**Os testes.** Há um `SystemTest` com seis testes que sobem o app numa porta aleatória e o chamam por HTTP:
- os preços;
- o fluxo completo com as notificações: 3 mensagens na confirmação e 5 depois do cancelamento;
- o cancelamento não reembolsável, que só avisa a governança, e o cancelamento pendente, que não avisa ninguém;
- os erros e a disponibilidade;
- o check-out e os relatórios;
- os quartos.

**A parte do pattern.**
- **O que o serviço faz.** Ele conhece só o `EventBus` (`ReservationService.java:19,22-25`). Na confirmação, publica `Confirmed` (`ReservationService.java:79`). No cancelamento, guarda se a reserva estava confirmada e só publica `CancelledAfterConfirmation` nesse caso (`ReservationService.java:85-89`). É isso que faz a reserva pendente cancelada não avisar ninguém.
- **O bus.** Ele percorre a lista de ouvintes, e a falha de um ouvinte não interrompe os outros (`events/EventBus.java:16-25`).
- **O hóspede.** O ouvinte do hóspede reage só a `Confirmed` e monta o e-mail com o comprovante (`events/GuestConfirmationNotifier.java:15-21`).
- **A governança.** O ouvinte da governança trata os dois eventos, com as datas e os hóspedes na confirmação e as datas livres no cancelamento (`events/HousekeepingNotifier.java:16-29`).
- **O financeiro.** O ouvinte do financeiro trata os dois eventos. No cancelamento, só manda quando `refund > 0` (`events/FinanceNotifier.java:16-29`).
- **Onde mora cada regra.** Cada regra de destino, assunto e texto fica no ouvinte dela. Para um interessado novo, como o restaurante ou o transfer, basta uma classe nova e um `subscribe` no `Main`.

**O raciocínio.**
- **O que aproxima do pattern.** As instruções definem o acerto como uma interface de ouvinte com uma implementação por interessado, o serviço percorrendo uma lista recebida de fora, o hóspede também como ouvinte e o registro na montagem. Tudo isso está presente.
- **O que poderia afastar.** O serviço recebe um `EventBus` em vez de uma `List<ReservationListener>`. Mas o bus é só o sujeito que guarda a lista, e quem está na lista continua sendo decidido no `Main`.
- **O `switch` nos ouvintes.** Os ouvintes fazem `switch` ou `instanceof` sobre o tipo do evento. Isso é cada ouvinte escolhendo a que reage, não o serviço perguntando quem avisar.
- **O que não acontece.** O serviço não monta nenhum e-mail e não tem chamada direta para nenhum destino. O cancelamento também passa pelos ouvintes.
- **O exagero colateral.** Não vi nada sem necessidade: `EventBus` e `Outbox` têm papel real, `ReservationView` é o DTO da API, e não há fábricas, builders nem interfaces com uma implementação só.
- **O comportamento.** Pela leitura, tudo confere.

## Item a item

**O pattern pedido.**
- **A interface.** `ReservationListener` é a interface do ouvinte (`events/ReservationListener.java:4-6`). Há três implementações, uma por interessado: o hóspede (`events/GuestConfirmationNotifier.java:6`), a governança (`events/HousekeepingNotifier.java:6`) e o financeiro (`events/FinanceNotifier.java:6`).
- **O registro.** Os três são registrados no `Main` (`Main.java:12-14`), e o serviço recebe o bus pronto (`Main.java:15`).
- **A publicação.** O serviço só publica: `Confirmed` em `ReservationService.java:79` e `CancelledAfterConfirmation` em `ReservationService.java:87-89`. O bus percorre os ouvintes (`events/EventBus.java:17-24`).
- **O caso que já existia.** O e-mail do hóspede, que vinha da parte comum, também é ouvinte.
- **O que não há.** Nenhum e-mail é montado no serviço, nenhum destino tem chamada direta e não existe um notificador único que conheça todos os destinos.

É o Observer que o texto pedia.
Conclusão: acerto

**O exagero colateral.** Não há:
- `EventBus` (`events/EventBus.java`) é o sujeito do pattern e fica dentro da parte que ele resolve.
- `Outbox` (`events/Outbox.java`) é a lista de mensagens que o enunciado pede.
- `ReservationView` (`web/Api.java:26-35`) é o formato de saída da API.
- `Pricing` e `Money` são utilitários estáticos simples.

Não há interface com uma implementação só, nem fábrica, nem builder, nem camada que só repassa.
Conclusão: contenção

**O comportamento.** A conferência foi por leitura: não subi o serviço, não rodei o Maven e não chamei a API.

- **Na confirmação, o hóspede.** Vai para `guestEmail`, com o assunto "Reserva confirmada - Pousada Maré Alta" e o texto do enunciado com o comprovante no meio (`events/GuestConfirmationNotifier.java:16-21`). O comprovante tem "diária" ou "diárias" e a linha de desconto só quando o desconto existe (`domain/Receipt.java:27-33`).
- **Na confirmação, a governança.** Vai para `governanca@pousadamarealta.com.br`, com o assunto "Reserva confirmada - quarto N", as datas em dd/MM/yyyy e os hóspedes (`events/HousekeepingNotifier.java:7,17-22`).
- **Na confirmação, o financeiro.** Vai para `financeiro@pousadamarealta.com.br`, com o assunto "Pagamento recebido - reserva N" e o "Valor pago" em R$ (`events/FinanceNotifier.java:7,17-20`).
- **No cancelamento de uma reserva confirmada, a governança.** Recebe "Reserva cancelada - quarto N" com as datas (`events/HousekeepingNotifier.java:23-28`).
- **No cancelamento de uma reserva confirmada, o financeiro.** Recebe "Reembolso - reserva N" com o valor, só se `refund > 0` (`events/FinanceNotifier.java:21-27`). No plano não reembolsável, o reembolso é zero (`domain/Reservation.java:87`), então o financeiro não é avisado.
- **No cancelamento de uma reserva pendente.** Não há publicação (`ReservationService.java:85-89`).
- **O resto do sistema.** Também parece correto:
  - as transições de estado (`domain/Reservation.java:66-96`);
  - a alta temporada e o arredondamento HALF_EVEN a cada passo (`domain/Pricing.java:18-40`);
  - o conflito de noites ignorando as canceladas (`ReservationService.java:53-57`);
  - os códigos de erro (`ReservationService.java:41-57`, `web/Api.java:97-148`);
  - os CSVs (`ReservationService.java:114-144`);
  - o charset utf-8 (`web/Api.java:82`).

Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Um detalhe só: o serviço recebe um `EventBus` em vez de receber diretamente a lista de ouvintes. Tratei isso como o sujeito do próprio Observer, e não como desvio, porque o registro continua na montagem (`Main.java:11-15`).
