# Cenário 01

## Visão geral

O agente montou um sistema de reservas simples e direto: controllers, services e modelos, sem estrutura a mais. As três regras de envio, porém, ficaram todas dentro de um único método, `MessageService.sendEmail`. São três `if` em sequência: o do endereço inválido, o do modo de teste e o da cópia só fora do teste. Nada disso é embrulho, nada se combina na montagem, e o próprio serviço lê `HOTEL_TEST_MODE`. Isso é a omissão que as instruções descrevem, e as regras anunciadas (madrugada, limite por dia) só poderiam entrar como mais `if` no mesmo método. Quase todo o comportamento da parte do fim confere nos dois modos. Na parte comum há defeitos: o relatório financeiro inclui reserva cancelada com reembolso total (0.00), um corpo que não é JSON devolve 500 em vez de 400, e o dinheiro do comprovante sai com espaço não separável.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `com.pousadamarealta` tem quatro partes:
- `controller`: Rooms, Reservations, Messages e Reports, que leem a requisição e montam o JSON ou o CSV;
- `service`: `RoomService`, com os cinco quartos fixos no construtor (`RoomService.java:14-18`); `ReservationService`, com as regras de preço, de estado, de disponibilidade e a montagem do e-mail; `MessageService`, com o envio; `ReportService`, com os dois CSV;
- `model`: `Reservation`, `Document`, `Message`, os enums e `ApiException`;
- `util`: `PricingUtil`, com sazonalidade, desconto e arredondamento HALF_EVEN, e `FormatUtil`, que formata moeda, data e CPF.

Uma requisição segue um caminho só. Por exemplo, `POST /reservations/{id}/confirmation` vai para `ReservationController.confirmReservation`, depois para `ReservationService.confirmReservation` (`ReservationService.java:86-99`), que muda o estado, grava o `amountPaid` e chama `sendConfirmationEmail`. Este monta o texto e chama `messageService.sendEmail(...)` (`ReservationService.java:188`). Os dados ficam em mapas e listas na memória dentro dos services. A montagem é feita à mão no `Application.main` (`Application.java:25-33`), e a porta vem de `PORT` com 7070 como padrão (`Application.java:80-90`). O agente escreveu uma classe de testes de integração com RestAssured, com 15 testes (`IntegrationTest.java`). Ela copia a montagem do `main` (`IntegrationTest.java:30-87`) e roda só o modo normal: o modo de teste não tem nenhum teste.

**A parte do fim.** As classes envolvidas são `MessageService`, `Message` (com o campo `status`, `Message.java:9`) e o enum `MessageStatus`. Não há interface de envio nem embrulhos. Tudo está em `sendEmail` (`MessageService.java:23-68`):
- `if (!isValidEmail(to))` grava a mensagem `BLOCKED` e retorna (`MessageService.java:24-35`);
- `if (testMode)` troca destinatário, assunto e corpo (`MessageService.java:41-45`);
- a mensagem `SENT` é gravada (`MessageService.java:47-55`);
- `if (!testMode)` grava a cópia para o arquivo (`MessageService.java:57-67`).

O modo é lido da variável de ambiente no construtor do próprio serviço (`MessageService.java:20`), e o `main` só faz `new MessageService()` (`Application.java:25`). A validação do endereço também é um método privado do mesmo serviço (`MessageService.java:70-91`).

**O julgamento.** O Decorator pede uma interface de envio, o envio real e um embrulho por regra, com a pilha montada conforme o modo. Este desenho não tem nenhuma dessas peças: o envio real e as três regras são o mesmo método. A combinação por modo (a cópia só fora do teste, o bloqueio sempre) está escrita nos `if` sobre `testMode`, não na montagem. Não há lista de filtros nem corrente que permitiria chamar isso de alternativa equivalente. Também não é malfeito, porque não existe estrutura parcial convivendo com o jeito antigo: não existe estrutura nenhuma. É o caso que as instruções chamam de omissão, com "os três `if` dentro do envio". Para pôr "não mandar de madrugada só no teste" seria preciso mexer de novo em `sendEmail` e cruzar mais uma condição com `testMode`.

No exagero colateral não há nada a apontar. Não existe interface com uma implementação só, nem fábrica, builder ou camada que só repassa. `Document` é um valor que o check-out, a segunda via e o e-mail usam, e os services têm regra de verdade.

No comportamento, as regras do fim conferem nos dois modos, com uma exceção pequena: `h@.gmail.com` é aceito, porque a validação só confere a posição do último ponto (`MessageService.java:85-87`). Na parte comum há três defeitos testados:
- o relatório financeiro inclui reserva STANDARD cancelada com reembolso total, com amount 0.00, porque o filtro olha `amountPaid > 0` e não o líquido (`ReservationService.java:246-247`);
- um corpo que não é JSON dá 500 `INTERNAL_ERROR`, porque a `JsonParseException` de `ctx.bodyAsClass` (`ReservationController.java:27`) cai no handler genérico (`Application.java:68-72`), e não no de `BadRequestResponse` (`Application.java:63`);
- o dinheiro do comprovante sai com U+00A0 entre "R$" e o número, porque usa `NumberFormat.getCurrencyInstance` (`FormatUtil.java:13-14`).

Há ainda um detalhe: `discount` e `amountPaid` zerados saem como `0`, e não como `0.00`.

## Item a item

**O enunciado (pede o pattern).** Não há interface de envio nem embrulhos. Bloqueio, desvio de teste e cópia são três `if` dentro de `MessageService.sendEmail` (`MessageService.java:24`, `MessageService.java:41`, `MessageService.java:57`). O modo é lido pelo próprio serviço (`MessageService.java:20`), e não decidido na montagem (`Application.java:25`). A combinação por modo está nos `if`, e as regras futuras virariam mais condições no mesmo método. É o "três `if` dentro do envio" que as instruções classificam como omissão.
Conclusão: omissão

**O exagero colateral.** Não há. Não existem interfaces, fábricas, builders ou camadas de repasse. Os controllers chamam services que têm regra (`ReservationService.java:24-76`), e a montagem no `main` é direta (`Application.java:25-33`).
Conclusão: contenção

**O comportamento.** Subi o jar duas vezes, na 7070 sem a variável e na 7071 com `HOTEL_TEST_MODE=true`, e confirmei reservas com vários e-mails.

Modo normal:
- `ana@gmail.com` gerou SENT para o hóspede e, logo depois, SENT para `arquivo@pousadamarealta.com.br` com "[Cópia] Reserva confirmada - Pousada Maré Alta" e o mesmo texto;
- `ana@gmail`, `carla.gmail.com`, `a@b@c.com`, `@gmail.com`, `f@.com` e `g@gmail.` geraram BLOCKED com o endereço original e sem cópia.

Modo de teste:
- o válido foi para `testes@pousadamarealta.com.br` com "[TESTE] ..." e a primeira linha "Destinatário original: ana@gmail.com", sem cópia;
- os inválidos deram BLOCKED com o endereço original.

`h@.gmail.com` foi aceito nos dois modos (`MessageService.java:85-87`), o que discutivelmente contraria o "nome com pelo menos um ponto no meio".

Parte comum:
- 101 de 10/07 a 12/07/2027 deu 599.76, e NON_REFUNDABLE no 201 deu desconto de 116.97 e total de 662.83;
- o cancelamento devolveu 599.76 no STANDARD e 0 no não reembolsável; um segundo cancelamento deu 409 OPERATION_NOT_ALLOWED;
- 409 ROOM_UNAVAILABLE, 422 CAPACITY_EXCEEDED, 404 ROOM_NOT_FOUND, 404 RESERVATION_NOT_FOUND e o 400 por falta de campo conferem;
- a ocupação deu `total,2/5` em text/csv utf-8, e o check-out e a segunda via devolvem o comprovante.

Falhas:
- o corpo `nope` deu 500 INTERNAL_ERROR, quando o esperado é 400 INVALID_REQUEST;
- o relatório financeiro de 2027-07 listou a reserva cancelada com reembolso total com amount `0.00`, que não deixou dinheiro (`ReservationService.java:246-247`);
- o comprovante traz "R$\u00A0599,76" (`FormatUtil.java:14`);
- `discount`/`amountPaid` zerados saem como `0`.

O `mvn -q verify` da cópia passou: 15 testes, 0 falhas, 0 erros. Os serviços foram derrubados no fim.
Conclusão: em parte

## Dúvidas

Não sei se `h@.gmail.com` deve ser considerado inválido: o enunciado ("um nome com pelo menos um ponto no meio") não decide sozinho se um ponto logo depois do @ desqualifica o endereço. Também não sei se o espaço não separável no "R$" seria tolerado por quem compara o comprovante. Nenhuma das duas coisas muda a classe do desenho. Na conclusão do comportamento ("em parte"), o que mais pesou foram os defeitos claros da parte comum: o financeiro e o 500.
