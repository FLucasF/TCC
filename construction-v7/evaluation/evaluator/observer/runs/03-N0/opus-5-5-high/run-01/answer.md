# Cenário 01

## Visão geral

O agente não usou o pattern na parte do fim do enunciado. Os três e-mails da confirmação (hóspede, governança, financeiro) e os dois do cancelamento são montados e enviados um depois do outro, à mão, em dois métodos privados do `ReservationService` (`sendConfirmationEmails` e `sendCancellationEmails`). Não há interface de ouvinte, não há lista de interessados e nada é registrado no `main`. Para avisar o restaurante ou o transfer, seria preciso editar o serviço de reservas. O que está bom: os textos e os destinatários pedidos saem certos, e uma reserva pendente cancelada não avisa ninguém. O problema mais sério de desenho é que a regra do reembolso (o valor e o `setRefund`) ficou dentro do método que manda o e-mail do financeiro. No comportamento há várias falhas fora da parte do fim: dezembro inteiro é cobrado como alta temporada, o dinheiro sai sem separador de milhar ("R$ 2319,60"), o relatório de ocupação conta reservas pendentes e o financeiro lista reservas canceladas que não deixaram dinheiro.

## Como foi implementado, e por que o julgamento

**O projeto.** É um único pacote `com.pousadamarealta` com dois subpacotes:
- `model`: `Reservation`, `Room`, `Message`, `Receipt` e os enums `RatePlan`, `RoomType` e `ReservationStatus`. São classes de dados com getters e setters.
- `service`:
  - `PricingService`: métodos estáticos de preço e arredondamento com `BigDecimal` HALF_EVEN sobre `double`.
  - `FormatService`: métodos estáticos de formatação de datas, dinheiro e CPF.
  - `ReservationService`: um singleton com `getInstance()` (`service/ReservationService.java:26-31`) que guarda tudo em `LinkedHashMap`s na memória (quartos, reservas, mensagens). Ele faz as transições de estado, os relatórios CSV e os e-mails.

O `App` (`App.java:25-43`) pega o singleton, registra as rotas do Javalin com lambdas que chamam métodos estáticos do próprio `App` e sobe na porta da variável `PORT`. Cada handler:
- lê o corpo com Gson (`App.java:72`);
- valida e confere o status da reserva para devolver os códigos de erro (por exemplo `App.java:175-179`);
- chama o serviço, que devolve `null` quando não consegue;
- serializa à mão em `Map` (`App.java:408-427`).

O comprovante (`Receipt`) é montado no `App` para o check-out e para a segunda via (`App.java:290-302` e `App.java:335-347`). O texto do comprovante no e-mail é montado de novo, à parte, no serviço (`service/ReservationService.java:167-184`). O shade gera `target/app.jar` com a classe principal no manifesto. O agente escreveu 21 testes JUnit 4: 11 de preço e 10 do serviço. Nenhum confere as mensagens enviadas.

**A parte do fim.**
- **Confirmação.** `confirmReservation` muda o status e chama `sendConfirmationEmails(res)` (`service/ReservationService.java:104-107`). Esse método:
  - monta o comprovante e o e-mail do hóspede e envia (`:167-192`);
  - em seguida monta e envia o da governança, com o endereço fixo no código (`:194-197`);
  - depois monta e envia o do financeiro (`:199-201`).
- **Cancelamento.** `cancelReservation` verifica `if (oldStatus == CONFIRMED)` e chama `sendCancellationEmails` (`:145-147`). Esse método:
  - envia o e-mail da governança (`:208-210`);
  - calcula o reembolso com `res.getRatePlan() == RatePlan.STANDARD ? ...`, grava `res.setRefund` e só então envia o e-mail do financeiro (`:212-219`).

Cada interessado é um bloco de código seguido dentro do serviço. Não há tipo que represente um interessado, nem um ponto onde se registre um.

**O raciocínio.** As instruções do Observer pedem que o serviço anuncie o evento sem conhecer quem é avisado, com um ouvinte por interessado recebido de fora. Aqui acontece o contrário. O serviço conhece cada destino, cada endereço e cada texto, e a confirmação e o cancelamento mandam os e-mails "um depois do outro", que é exatamente a descrição de omissão. Não é malfeito, porque não existe lista de ouvintes nenhuma que conviva com o jeito manual. Há um agravante: o cálculo do reembolso, que é regra de negócio do cancelamento, só acontece dentro do método de notificação (`:212-215`). Tirar o aviso do financeiro mudaria o `refund` da reserva.

No exagero colateral não pesa nada. Não há interfaces, fábricas, builders nem camadas que só repassam. O singleton com `getInstance` é um estado global desnecessário, mas não é estrutura sobrando no sentido da lista. Se pesasse para algum lado, seria para menos estrutura: o `App` concentra validação, regra de status e montagem do comprovante.

No comportamento, a parte do fim funciona: os assuntos, os destinatários e os conteúdos saem como pedidos, e as recusas de aviso também. A parte comum tem vários erros que testei:
- **Preço de dezembro:** 10/12 cobrado com 20% a mais (`service/PricingService.java:20`).
- **Formato do dinheiro:** "R$ 2319,60" no comprovante e nos e-mails (`service/FormatService.java:10`, `:21`).
- **Valores no JSON:** saem sem 2 casas (`249.9`, `779.8`, `0.0`).
- **Relatório de ocupação:** conta reservas pendentes (`service/ReservationService.java:237`).
- **Relatório financeiro:** lista canceladas com 0 e escreve "0.0" (`:275`, `:282`).
- **Reembolso:** `refund` fica `null` depois de cancelar quando não há nada a devolver (`:212-215`).
- **Datas do comprovante no JSON:** saem como `10/07/2027` (`App.java:296`).

## Item a item

**O enunciado (pede o pattern).** Os avisos são chamadas seguidas a `sendMessage` dentro do serviço de reservas: o hóspede, a governança e o financeiro na confirmação (`service/ReservationService.java:192`, `:197`, `:201`) e a governança e o financeiro no cancelamento (`:210`, `:218`). A escolha de quando avisar é um `if` no próprio serviço (`:145`). Não há interface de ouvinte, nem lista de interessados, nem registro na montagem (`App.java:25-43` só cria o singleton e as rotas). Um novo interessado, como o restaurante, exige editar `ReservationService`. É o caso que as instruções chamam de omissão: "a confirmação e o cancelamento mandam os e-mails um depois do outro".
Conclusão: omissão

**O exagero colateral.** Não há interface com uma implementação só, fábrica, builder ou camada que só repassa. `PricingService` e `FormatService` são utilitários estáticos (`service/PricingService.java:8`, `service/FormatService.java:7`). O singleton `ReservationService.getInstance()` (`service/ReservationService.java:26-31`) é um estado global evitável, mas não é estrutura sobrando.
Conclusão: contenção

**O comportamento.** Rodei uma cópia em `PORT=7070` e testei com `curl`.

*A parte do fim:*
- **Confirmações** da 1 (101, julho), da 2 (201, março, Carla Dias) e da 3 (301, NON_REFUNDABLE, 1 noite):
  - o hóspede recebeu o e-mail com o texto exato e o comprovante, inclusive "1 diária" e "Desconto: R$ 86,98";
  - a governança recebeu "Reserva confirmada - quarto 201" com "Entrada: 10/03/2027 / Saída: 12/03/2027 / Hóspedes: 2";
  - o financeiro recebeu "Pagamento recebido - reserva 2" com "Valor pago: R$ 779,80".
- **Cancelamentos:**
  - a pendente 4 não gerou mensagem (a lista ficou com 12);
  - a confirmada STANDARD 2 gerou "Reserva cancelada - quarto 201" com as datas e "Reembolso - reserva 2" com "R$ 779,80", e `refund` ficou 779.8;
  - a confirmada NON_REFUNDABLE 3 avisou só a governança, que é o correto. Mas `refund` ficou `null`, quando o certo seria 0, e o mesmo acontece na pendente cancelada.
- **Cancelar de novo** devolveu 409 OPERATION_NOT_ALLOWED.

*A parte comum:*
- **Preços:** o exemplo de julho dá 599.76, o que confere. Já um STANDARD de 10/12/2027 saiu 299.88 em vez de 249.90, porque dezembro inteiro conta como alta temporada (`service/PricingService.java:20`).
- **Comprovante:** um total de R$ 2.319,60 saiu "R$ 2319,60" no e-mail. No JSON, as datas vêm `10/07/2027`, e o dinheiro sai como `249.9`, `779.8` e `0.0` em vez de duas casas.
- **Relatórios** (os dois saem em `text/csv; charset=utf-8`, o que confere):
  - o de ocupação de 10/12/2027 listou a reserva 5, ainda pendente;
  - o financeiro de 2027-03 listou as canceladas 2 (devolvida) e 4 (pendente) com "0.0".
- **Check-in, check-out e segunda via** funcionaram. A segunda via de uma pendente deu 404.
- **Erros:** 404 ROOM_NOT_FOUND, 409 ROOM_UNAVAILABLE, 422 CAPACITY_EXCEEDED, 400 para corpo que não é JSON e 400 para mês 2027-13, todos conferem.

*Os testes do agente:* `mvn -q verify` passou com 21 testes (11 em `PricingServiceTest` e 10 em `ReservationServiceTest`), 0 falhas. Nenhum confere as mensagens enviadas, e um teste fixa o erro de dezembro (`PricingServiceTest.java:17`). Derrubei o serviço no fim.

A parte do fim segue o pedido, salvo o `refund` nulo quando não há o que devolver. O resto do sistema tem erros de preço, de formato e de relatório.
Conclusão: em parte

## Dúvidas

Nenhuma sobre a classe: não existe nenhuma estrutura de ouvinte que pudesse sustentar malfeito. No comportamento, o enunciado não diz que código de erro usar para a segunda via de uma reserva não paga. O 404 RESERVATION_NOT_FOUND que o agente escolheu (`App.java:322-327`) é discutível, mas não contei como falha.
