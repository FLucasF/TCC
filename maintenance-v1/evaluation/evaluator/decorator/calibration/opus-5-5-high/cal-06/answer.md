# Cenário 07

## Visão geral

O agente pôs as três regras novas dentro de `EmailSender.send`, como três `if` em sequência: o endereço inválido, o modo de teste e a cópia para o arquivo. O modo de teste chega como um `boolean` no construtor. Não há interface de envio, nem embrulhos, nem lista de regras, e a combinação por modo é decidida dentro do próprio envio, não na montagem. O comportamento está certo nos dois modos e o `mvn verify` passa. O problema sério é o desenho. O pedido anuncia mais regras (madrugada, limite por dia) que podem valer sempre, só no teste ou só fora dele. Do jeito que ficou, cada regra nova será mais um `if` no `send`, cruzado com o `testMode`.

## Item a item

**O pedido (pede o pattern).** Todo o comportamento novo está em `final/src/main/java/hotel/messages/EmailSender.java:27-38`:
- o bloqueio do endereço inválido é um `if` (`:28-30`);
- o desvio para a caixa de testes é um `if (testMode)` (`:31-34`);
- a cópia para `arquivo@` vem escrita logo depois, no mesmo método (`:35-37`).

O modo de teste é um campo do envio (`EmailSender.java:16`, `:22-25`). A montagem só passa esse `boolean` (`final/src/main/java/hotel/Hotel.java:25`), sem empilhar nada. `EmailSender` continua `final`, não implementa nenhuma interface e não chama um "próximo" envio. `ReservationService` continua dependendo da classe concreta (`final/src/main/java/hotel/reservations/ReservationService.java:17`).

A regra "a cópia só fora do teste" existe apenas pela ordem dos `if` e pelo `return` antecipado da linha `:33`. É o caso que as instruções chamam de omissão: os três `if` dentro do `send`.
Conclusão: omissão

**O exagero colateral.** A única peça nova é o enum `MessageStatus` (`final/src/main/java/hotel/messages/MessageStatus.java:3-7`), que o pedido exige (o campo `status`). O `record Message` só ganhou esse campo (`final/src/main/java/hotel/messages/Message.java:3-4`), e `SentMessages.add` ganhou o parâmetro correspondente (`final/src/main/java/hotel/messages/SentMessages.java:11-12`). Não há interface, fábrica nem camada nova fora do pedido.
Conclusão: contenção

**O comportamento.** Montei uma cópia da `final/` e subi o serviço duas vezes: na porta 7070 sem variável e na 7071 com `HOTEL_TEST_MODE=true`. Em cada uma criei e confirmei (`POST /reservations` e `POST /reservations/{id}/confirmation`) reservas com estes endereços: `carla@example.com`, `ana@gmail`, `ana.gmail.com`, `a@@b.com`, `@b.com`, `a@b.`, `a@.com` e `a@b.com.br`. Depois li o `GET /messages`.

- **Modo normal:** os dois endereços válidos saem como `SENT`. Cada um vem seguido, no id seguinte, de uma cópia para `arquivo@pousadamarealta.com.br` com o assunto "[Cópia] Reserva confirmada - Pousada Maré Alta" e o corpo idêntico ao original (conferido com `jq '.[0].body == .[1].body'` → `true`). Os seis inválidos aparecem como `BLOCKED`, com o endereço original e sem cópia.
- **Modo de teste:** os válidos vão para `testes@pousadamarealta.com.br` com o assunto "[TESTE] …" e a primeira linha "Destinatário original: carla@example.com" (e "…: a@b.com.br"), sem cópia. Os inválidos continuam `BLOCKED`, com o endereço original.
- **O que não mudou:** check-in e check-out da reserva 1 devolvem o comprovante como antes. O relatório `GET /reports/revenue?month=2027-08` sai normal. Confirmar de novo uma reserva já encerrada devolve `OPERATION_NOT_ALLOWED`, e nenhuma mensagem nova aparece (a lista continuou com 10).
- **Testes:** o `mvn -q verify` da cópia passou, com 26 testes e nenhuma falha (5 ReportsTest, 6 PriceCalculatorTest, 11 ReservationServiceTest, 4 HttpApiTest). O agente não escreveu nenhum teste novo para as regras. Só ajustou as contagens esperadas de 1 para 2 (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:82`, `:96`; `final/src/test/java/hotel/web/HttpApiTest.java:78`).

No fim derrubei os dois serviços.
Conclusão: segue

## Dúvidas

Nenhuma.
