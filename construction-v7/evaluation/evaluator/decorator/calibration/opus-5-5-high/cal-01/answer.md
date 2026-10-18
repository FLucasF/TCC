# Cenário 01

## Visão geral

O agente resolveu as regras de envio com um Decorator. Há uma interface de envio (`MessageSender`), um envio real (`EmailSender`) e um embrulho para cada regra: `BlockInvalidAddress`, `ArchiveCopy` e `RedirectToTestInbox`. Cada embrulho chama o próximo, e quem decide a combinação por modo é a montagem, em `Hotel.messageSender`. Nenhum embrulho e nem o envio real perguntam pelo modo de teste, e quem chama o envio (`ReservationService`) só conhece a interface. O resto do sistema é enxuto, sem estrutura sobrando. Nos dois modos, o comportamento testado pela API confere com o enunciado. O único ponto fraco é pequeno: o bloqueio grava a mensagem `BLOCKED` direto na lista, sem passar pelo envio de dentro, e repete ali o canal `"EMAIL"`. Isso não mistura o pattern com o jeito sem ele.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica no pacote `hotel`, dividido assim:
- `rooms`: os cinco quartos fixos no código (`rooms/Rooms.java:10-15`).
- `reservations`: o domínio.
  - `Reservation` guarda o estado e as transições.
  - `ReservationService` aplica as regras do ciclo de vida, da disponibilidade e da capacidade.
  - `PriceCalculator` faz o preço, com alta temporada e arredondamento `HALF_EVEN` a cada passo.
  - `ReservationStore` guarda as reservas em memória.
  - `HotelException` com `ErrorCode` carrega os erros.
- `documents`: o comprovante (`ReservationReceipt`) e a formatação em pt-BR (`TextFormat`).
- `reports`: os dois CSV.
- `messages`: as regras de envio.
- `web`: as rotas Javalin e os DTOs.

Uma requisição chega numa rota de `web/HttpApi.java:61-91`, que lê e valida o corpo (`HttpApi.java:109-130`) e chama `ReservationService`. O serviço aplica a regra e grava no `ReservationStore`. As `HotelException` viram `{error, message}` com o status HTTP certo (`HttpApi.java:93-107`). Tudo fica em memória. A classe `Hotel` (`Hotel.java:23-32`) monta as peças à mão e sobe o Javalin na porta de `PORT` (`Hotel.java:55-58`). O jar sombreado sai com `hotel.Hotel` no manifesto (`pom.xml:66-68`). O agente escreveu 29 testes em cinco classes: preço, serviço, relatórios, regras de envio e API.

**As regras de envio.**
- **A interface:** `MessageSender.send(to, subject, body)` (`messages/MessageSender.java:4-7`).
- **O envio real:** `EmailSender` só grava a mensagem como `SENT` na lista (`messages/EmailSender.java:13-15`).
- **O bloqueio:** `BlockInvalidAddress` testa o endereço com a expressão `^[^@\s]+@[^@\s.]+(\.[^@\s.]+)+$` (`messages/BlockInvalidAddress.java:9`). Se o endereço for inválido, grava a mensagem como `BLOCKED` com o endereço original e não chama o próximo (`BlockInvalidAddress.java:21-23`). Se for válido, repassa (`:24`).
- **A cópia:** `ArchiveCopy` chama o próximo com a original e depois com a cópia para `arquivo@…` e o assunto `"[Cópia] "` (`messages/ArchiveCopy.java:16-18`).
- **O modo de teste:** `RedirectToTestInbox` troca o destinatário, o assunto e a primeira linha do texto, e repassa (`messages/RedirectToTestInbox.java:16`).
- **A montagem:** `Hotel.messageSender` lê `HOTEL_TEST_MODE`. Fora do teste, empilha bloqueio → cópia → envio; no teste, bloqueio → teste → envio (`Hotel.java:38-43`).
- **Quem usa:** `ReservationService` recebe um `MessageSender` qualquer (`reservations/ReservationService.java:17,24`) e só chama `send` na confirmação (`:69-72`).

**O julgamento.** Isso é o que as instruções chamam de acerto: uma interface, o envio real e um embrulho por regra, cada um chamando o próximo, com a combinação por modo decidida na montagem. Nada do que leva ao malfeito aparece:
- o envio de dentro não olha o modo (`EmailSender.java:14`);
- a cópia não verifica se está em teste (`ArchiveCopy.java:15-19`);
- as três regras estão na estrutura;
- quem chama não tem nenhum `if` de regra (`ReservationService.java:69`).

As regras anunciadas (madrugada, limite por dia) entrariam como novos embrulhos encaixados num dos dois ramos de `Hotel.java:41-42`, ou nos dois. O que destoa um pouco do Decorator puro é o bloqueio: ele recebe a `SentMessages` e grava nela diretamente, repetindo o canal `"EMAIL"` (`BlockInvalidAddress.java:12,22`) em vez de deixar isso para o envio. É acoplamento leve, não um caminho paralelo sem a estrutura.

No exagero colateral, a única interface do projeto é a do pattern, que tem quatro implementações. O resto são classes concretas e DTOs que o formato do JSON pede. No comportamento, todas as regras testadas nos dois modos e na parte comum conferem.

## Item a item

**O enunciado (pede o pattern).** Existe uma interface `MessageSender` (`messages/MessageSender.java:4-7`), com o envio real `EmailSender` (`messages/EmailSender.java:13-15`) e três embrulhos que guardam o `next` e o chamam: `BlockInvalidAddress` (`messages/BlockInvalidAddress.java:11,20-25`), `ArchiveCopy` (`messages/ArchiveCopy.java:8,15-19`) e `RedirectToTestInbox` (`messages/RedirectToTestInbox.java:8,15-17`). A combinação por modo fica só na montagem (`Hotel.java:40-42`): a cópia entra apenas fora do teste e o bloqueio fica por fora nos dois modos. Nenhum embrulho conhece o modo, e o serviço que envia depende só da interface (`reservations/ReservationService.java:17,69`). Por isso é o Decorator que o enunciado precisava, aplicado às três regras.
Conclusão: acerto

**O exagero colateral.** Não há. `ReservationStore`, `PriceCalculator`, `Rooms` e os relatórios são classes concretas, sem interfaces nem fábricas (`reservations/ReservationStore.java:10`, `reservations/PriceCalculator.java:14`). `Hotel` monta tudo com `new` direto (`Hotel.java:24-30`). `ReservationResponse` e `CreateReservationRequest` são DTOs que o contrato do JSON justifica (`web/ReservationResponse.java:9-25`, `web/CreateReservationRequest.java:7-15`).
Conclusão: contenção

**O comportamento.** Subi o serviço duas vezes, na 7070 sem a variável e na 7071 com `HOTEL_TEST_MODE=true`. Criei e confirmei reservas com os e-mails `ana@gmail.com`, `ana@gmail`, `ana.gmail.com`, `a@@b.com` e `ana@.com`.

Fora do teste:
- `GET /messages` trouxe a original para `ana@gmail.com` como `SENT`, logo seguida da cópia para `arquivo@pousadamarealta.com.br` com o assunto "[Cópia] Reserva confirmada - Pousada Maré Alta", o mesmo texto e também `SENT`.
- Os quatro endereços inválidos vieram como `BLOCKED`, com o endereço original e sem cópia.

No teste:
- A mensagem válida foi para `testes@pousadamarealta.com.br` com o assunto "[TESTE] Reserva confirmada - Pousada Maré Alta", a primeira linha "Destinatário original: ana@gmail.com" e sem cópia.
- Os inválidos continuaram `BLOCKED` com o endereço original.
- Todas as mensagens têm `channel: EMAIL` e `status`.

O texto do e-mail e o comprovante batem com o modelo do enunciado.

Na parte comum:
- O STANDARD de 10/07 a 12/07/2027 no 101 deu `subtotal` e `total` de 599.76.
- O NON_REFUNDABLE de 2 noites no DELUXE deu desconto de 116.97 e total de 662.83.
- O cancelamento devolveu 0.00 no não reembolsável e 779.80 no normal.
- As recusas vieram como pedido: quarto ocupado 409 `ROOM_UNAVAILABLE`, capacidade 422 `CAPACITY_EXCEEDED`, corpo inválido 400 `INVALID_REQUEST`, quarto 999 404 `ROOM_NOT_FOUND`, reserva 99 404 `RESERVATION_NOT_FOUND`, reconfirmação 409 `OPERATION_NOT_ALLOWED` e mês `2027-3` 400.
- O check-out devolveu o comprovante, e a segunda via devolveu o `title` "Comprovante de reserva".
- O relatório de ocupação de 2027-03-10 veio em `text/csv; charset=utf-8`, terminando em `total,2/5`.
- O financeiro de 2027-03 incluiu a não reembolsável cancelada (662.83) e fechou em `total,1492.63`.

O `mvn verify` da cópia passou, com 29 testes, 0 falhas e 0 erros.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Fica só uma observação: o bloqueio grava a mensagem `BLOCKED` direto em `SentMessages`, com o canal `"EMAIL"` repetido (`messages/BlockInvalidAddress.java:22`), em vez de delegar isso ao envio. É um acoplamento leve, não chega a ser convivência com o jeito sem o pattern. Os testes do agente cobrem os embrulhos montados à mão (`src/test/java/hotel/messages/SendingRulesTest.java:21,29,40`), mas não a montagem por modo em `Hotel.messageSender`. Essa montagem eu conferi pela API.
