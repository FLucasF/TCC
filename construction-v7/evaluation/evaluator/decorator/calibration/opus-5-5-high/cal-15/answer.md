# Cenário 07

## Visão geral

O agente montou as regras de envio como um Decorator de verdade. Existe uma interface de envio, `MessageSender`, com um único método `send`. O envio real é o `EmailSender`, e cada regra é um embrulho: `BlockInvalidAddress`, `ArchiveCopy` e `RedirectToTestInbox`. Cada embrulho recebe o próximo envio e o chama. A combinação por modo é decidida num lugar só, na montagem do sistema (`Hotel`). Nenhum embrulho e nenhum ponto de chamada pergunta pelo modo de teste, e quem envia (`ReservationService`) só conhece a interface. As regras novas que o enunciado anuncia (madrugada, limite por dia) entram como mais um embrulho, empilhado na montagem conforme o modo. Fora dessa parte, o desenho é enxuto e não tem estrutura sobrando. No comportamento, tudo o que testei nos dois modos confere. Não encontrei problema sério. O único reparo é pequeno: o bloqueio grava direto na lista de mensagens, em vez de passar pelo envio de dentro (ver "Dúvidas").

## Item a item

**O enunciado (pede o pattern).**
- A interface de envio é `src/main/java/hotel/messages/MessageSender.java:4-7`. O envio real é `src/main/java/hotel/messages/EmailSender.java:4-15`, que só registra a mensagem como `SENT`.
- Cada regra implementa a mesma interface e guarda o próximo envio:
  - `BlockInvalidAddress` (`src/main/java/hotel/messages/BlockInvalidAddress.java:6-25`) valida o endereço e, se ele for inválido, registra a mensagem como `BLOCKED` sem chamar o próximo (linhas 21-24).
  - `ArchiveCopy` (`src/main/java/hotel/messages/ArchiveCopy.java:4-19`) chama o próximo envio e, logo depois, faz a cópia com "[Cópia] " (linhas 16-17).
  - `RedirectToTestInbox` (`src/main/java/hotel/messages/RedirectToTestInbox.java:4-17`) troca o destinatário, o assunto e a primeira linha do texto e então chama o próximo.
- A combinação por modo fica toda em `src/main/java/hotel/Hotel.java:38-43`. O modo é lido na linha 40. A linha 41 escolhe entre o desvio para a caixa de testes e a cópia, e a linha 42 põe o bloqueio por fora de tudo. Assim o bloqueio vale nos dois modos e mensagem bloqueada nunca gera cópia.
- Nenhum embrulho consulta `HOTEL_TEST_MODE`. O `ArchiveCopy` não sabe do modo de teste, e o `EmailSender` não sabe de nada.
- Quem envia, `src/main/java/hotel/reservations/ReservationService.java:17` e `:69-72`, recebe só um `MessageSender` e chama `send`. Não tem nenhum `if` sobre endereço, modo ou cópia.

Estão presentes todas as marcas do acerto: as três regras viraram peças, cada embrulho chama o próximo e a montagem decide a combinação.
Conclusão: acerto

**O exagero colateral.** Não há.
- A única interface do sistema, `MessageSender`, é a que o Decorator exige: tem quatro implementações, empilhadas em `Hotel.java:39-42`.
- Preço, regras da reserva e relatórios são classes concretas e diretas:
  - `src/main/java/hotel/reservations/PriceCalculator.java:19-31`;
  - `src/main/java/hotel/reservations/ReservationService.java:27-53`;
  - `src/main/java/hotel/reports/OccupancyReport.java:23-45` e `src/main/java/hotel/reports/RevenueReport.java:23-48`.
- O desconto e o reembolso usam um `switch` simples sobre `RatePlan` (`PriceCalculator.java:26-29`, `ReservationService.java:86-89`), sem estratégia por plano.
- `Hotel` monta as peças sem fábrica e sem builder próprios. O `JsonMapper.builder()` em `src/main/java/hotel/web/HttpApi.java:45-48` é do Jackson, não é estrutura do agente.

Conclusão: contenção

**O comportamento.** Testei numa cópia do projeto. O build gerou `target/hotel.jar`, que subi duas vezes: na porta 7070 sem a variável e na 7071 com `HOTEL_TEST_MODE=true`. Em cada instância criei e confirmei sete reservas, uma com `ana@gmail.com` e seis com endereços inválidos: `ana@gmail`, `ana.gmail.com`, `a@b@c.com`, `@gmail.com`, `ana@gmail.` e `ana@.com`.

- **Modo normal (`GET /messages`):**
  - A mensagem 1 foi `SENT` para `ana@gmail.com`, com o assunto "Reserva confirmada - Pousada Maré Alta".
  - A mensagem 2, logo em seguida, foi `SENT` para `arquivo@pousadamarealta.com.br`, com o assunto "[Cópia] Reserva confirmada - Pousada Maré Alta" e o mesmo texto.
  - As mensagens 3 a 8 ficaram `BLOCKED`, cada uma com o endereço original e sem cópia.
  - O texto do e-mail bate com o modelo do enunciado, com o comprovante no meio.
  - Cada mensagem tem os campos `id`, `channel` (EMAIL), `to`, `subject`, `body` e `status`.
- **Modo de teste:**
  - A mensagem 1 foi `SENT` para `testes@pousadamarealta.com.br`, com o assunto "[TESTE] Reserva confirmada - Pousada Maré Alta". A primeira linha do texto é "Destinatário original: ana@gmail.com".
  - Não houve cópia para o arquivo.
  - As seis inválidas ficaram `BLOCKED` com o endereço original e o assunto sem "[TESTE] ".
- **Parte comum:**
  - Um STANDARD de 10/07/2027 a 12/07/2027 deu `subtotal` e `total` de 599.76.
  - Um NON_REFUNDABLE de 779.80 deu `discount` de 116.97 e `total` de 662.83. Cancelado depois de confirmado, ficou com `refund` 0.00.
  - As recusas vieram certas: quarto ocupado deu 409 `ROOM_UNAVAILABLE`, três hóspedes no 101 deram 422 `CAPACITY_EXCEEDED`, corpo que não é JSON deu 400 `INVALID_REQUEST`, a reserva 99 deu 404 `RESERVATION_NOT_FOUND` e data fora do formato deu 400.
  - O check-out devolveu o comprovante com os campos pedidos.
  - O relatório financeiro de 2027-03 inclui a não reembolsável cancelada e fecha com `total,2272.43`. O de ocupação de 2027-03-10 lista os quartos em ordem e termina com `total,3/5`.
- **`mvn -q verify` da cópia:** passou (saída 0), com 29 testes do agente, 0 falhas e 0 erros (5 + 11 + 3 + 6 + 4).
- Os dois serviços foram derrubados no fim.

Conclusão: segue

## Dúvidas

Em `BlockInvalidAddress.java:22`, o bloqueio grava a mensagem bloqueada direto na `SentMessages`, em vez de delegar ao envio de dentro. Por isso esse embrulho depende da lista, além do próximo envio (`BlockInvalidAddress.java:14-17`). Não conta como malfeito: a mensagem bloqueada justamente não deve ser enviada, então não há por que repassá-la. A regra continua sendo uma peça própria, e a combinação continua na montagem.

Há também um ponto de ordem para as regras futuras. O `ArchiveCopy` chama o próximo envio duas vezes (`ArchiveCopy.java:16-17`). Se um dia o limite por hóspede for empilhado por dentro da cópia, a cópia também vai contar no limite. Isso é decisão de montagem e não afeta o que foi pedido hoje.
