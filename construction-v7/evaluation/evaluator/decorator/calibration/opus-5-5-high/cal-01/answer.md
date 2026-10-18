# Cenário 01

## Visão geral

O agente montou as regras de envio como um Decorator. Há uma interface de envio com um só método (`MessageSender`), um envio real que só grava na lista e um embrulho para cada regra: bloqueio de endereço, cópia para o arquivo e desvio para a caixa de testes. Cada embrulho chama o próximo. A combinação por modo fica num único lugar, a montagem em `Hotel`. Nenhuma peça pergunta pelo modo de teste, e o `ReservationService` só conhece a interface. O resto do sistema não tem estrutura sobrando. Todas as regras do fim do enunciado se comportaram como pedido nos dois modos, e os 29 testes do agente passam. Não achei problema sério. O único detalhe é que o bloqueio grava direto na lista, sem passar pelo envio de dentro, e repete ali o canal `"EMAIL"`.

## Item a item

**O enunciado (pede o pattern).**
- **A interface:** `MessageSender` (`src/main/java/hotel/messages/MessageSender.java:4-7`) tem um só método, `send(to, subject, body)`.
- **O envio real:** `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:13-15`) só grava a mensagem como `SENT`, sem nenhuma regra.
- **Os três embrulhos:** todos implementam a mesma interface e recebem o próximo no construtor.
  - `BlockInvalidAddress` (`src/main/java/hotel/messages/BlockInvalidAddress.java:20-25`) para no endereço inválido e grava `BLOCKED`; nos outros casos chama `next.send`.
  - `ArchiveCopy` (`src/main/java/hotel/messages/ArchiveCopy.java:15-19`) chama o próximo duas vezes: a original e depois a cópia com "[Cópia] ".
  - `RedirectToTestInbox` (`src/main/java/hotel/messages/RedirectToTestInbox.java:15-17`) troca o destinatário, o assunto e a primeira linha e repassa.
- **A combinação por modo:** fica só na montagem (`src/main/java/hotel/Hotel.java:38-43`). A variável é lida na linha 40. A linha 41 escolhe o desvio ou a cópia, e a linha 42 põe o bloqueio por fora nos dois modos. Por isso a mensagem bloqueada no teste guarda o endereço original.
- **Quem chama:** o `ReservationService` recebe a interface (`src/main/java/hotel/reservations/ReservationService.java:17`) e faz uma chamada só, sem nenhum `if` sobre regra ou modo (`src/main/java/hotel/reservations/ReservationService.java:69-72`).
- **O que falta para ser malfeito:** nenhum embrulho e nenhum envio de dentro olha o modo de teste, e nenhuma das três regras ficou fora da estrutura.

As regras anunciadas (madrugada, limite por dia) entram como novos embrulhos, empilhados na montagem conforme o modo.

Conclusão: acerto

**O exagero colateral.** Não há. A única interface é `MessageSender`, que é a do próprio pattern e tem quatro implementações. As outras peças são classes concretas, sem interface de uma implementação só, sem fábrica, sem builder e sem camada que só repassa:
- `PriceCalculator` (`src/main/java/hotel/reservations/PriceCalculator.java:14`);
- `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:10`);
- `Rooms` (`src/main/java/hotel/rooms/Rooms.java:8`);
- os dois relatórios.

O `HttpApi` (`src/main/java/hotel/web/HttpApi.java:61-91`) chama o serviço direto. Os `switch` sobre o plano de tarifa (`src/main/java/hotel/reservations/PriceCalculator.java:26-29` e `src/main/java/hotel/reservations/ReservationService.java:86-89`) são o oposto de exagero.

Conclusão: contenção

**O comportamento.** Testei com uma cópia em `/tmp`, com o serviço no ar duas vezes: uma sem a variável (porta 7070) e outra com `HOTEL_TEST_MODE=true` (porta 7071). Criei e confirmei reservas para cada caso.

Modo normal:
- `ana@gmail.com`: a original sai como `SENT` e logo em seguida (id 2) vem a cópia para `arquivo@pousadamarealta.com.br`, com "[Cópia] Reserva confirmada - Pousada Maré Alta" e o mesmo texto.
- `ana@gmail` e `ana.gmail.com` ficam `BLOCKED`, com o endereço original e sem cópia.
- Outros endereços bloqueados: `a@@b.com`, `@gmail.com`, `ana@.com`, `ana@gmail.`, `a@b@c.com`, `ana@g..com`.
- Outros endereços enviados, com cópia: `ana@b.c.d` e `ana.silva@mail.com.br`.

Modo de teste:
- O endereço válido vai para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto e a primeira linha "Destinatário original: ana@gmail.com". Não há cópia.
- Os dois endereços inválidos ficam `BLOCKED` com o endereço original, o assunto e o texto sem mudança.
- Toda mensagem tem `status`.

Parte comum:
- O STANDARD do 101 de 10/07/2027 a 12/07/2027 deu `subtotal` 599.76 e `total` 599.76.
- O NON_REFUNDABLE do 201 em março deu desconto 116.97 e total 662.83. O texto do e-mail traz "Desconto: R$ 116,97".
- O texto do e-mail confere com o modelo do enunciado.
- Confirmação, check-in e check-out funcionam. O check-out devolve o comprovante com todos os campos.
- A segunda via existe para a reserva paga e dá 409 `OPERATION_NOT_ALLOWED` para a cancelada.
- Cancelamento: o NON_REFUNDABLE devolve 0.00 e o STANDARD confirmado devolve 599.76.
- Recusas:
  - noite ocupada: 409 `ROOM_UNAVAILABLE`;
  - 3 hóspedes no 101: 422 `CAPACITY_EXCEEDED`;
  - corpo que não é JSON: 400 `INVALID_REQUEST`;
  - reserva 99: 404 `RESERVATION_NOT_FOUND`;
  - `month=2027-7`: 400 `INVALID_REQUEST`.
- Relatório de ocupação: `text/csv; charset=utf-8` com `total,1/5`.
- Relatório financeiro de março: lista a não reembolsável cancelada (662.83).
- `mvn -q verify` passou: 29 testes, 0 falhas. São 3 em `SendingRulesTest`, 5 em `ReportsTest`, 6 em `PriceCalculatorTest`, 11 em `ReservationServiceTest` e 4 em `HttpApiTest`.

Derrubei o serviço no fim.

Conclusão: segue

## Dúvidas

Nenhuma que mude a classe. O bloqueio não chama o próximo quando o endereço é inválido: grava direto na lista (`src/main/java/hotel/messages/BlockInvalidAddress.java:22`) e repete o canal `"EMAIL"` que o envio real também usa (`src/main/java/hotel/messages/EmailSender.java:14`). Parar a cadeia é legítimo num Decorator, então isso não tira o acerto. Mas, se um dia houver outro canal, o bloqueio vai continuar escrevendo `"EMAIL"`.
