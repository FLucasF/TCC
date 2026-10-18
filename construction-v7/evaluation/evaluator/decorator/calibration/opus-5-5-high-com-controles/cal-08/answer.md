# Cenário 07

## Visão geral

O sistema está bem organizado e enxuto, e tudo o que foi testado funciona como o enunciado pede: preço, recusas, comprovante, relatórios e as três regras de e-mail, nos dois modos. O problema está no desenho da parte do fim. O enunciado traz três regras que se combinam de jeitos diferentes conforme o modo e avisa que vêm mais duas (madrugada e limite por dia), cada uma valendo sempre, só no teste ou só fora dele. Mesmo assim, o agente resolveu tudo com três `if` em sequência dentro de um único `EmailSender.send`, que recebe a flag `testMode`. Não há interface de envio, embrulhos nem filtros, e a combinação por modo está escrita dentro do próprio envio. Para incluir uma regra nova, a pessoa teria de mexer nesse método e cruzar mais uma condição com o modo. Os testes do agente passam, mas nenhum cobre as regras de mensagem.

## Item a item

**O enunciado (pede o pattern).** O envio é uma classe concreta só, `src/main/java/hotel/messages/EmailSender.java:9`, sem interface. O modo de teste entra como um `boolean` no construtor (`EmailSender.java:16`, `EmailSender.java:22-25`). As três regras são três ramos em sequência do mesmo método:
- o bloqueio de endereço inválido é um `if` com a regex (`EmailSender.java:28-30`);
- o desvio para a caixa de testes é um `if (testMode)` (`EmailSender.java:31-34`);
- a cópia para o arquivo é o código que sobra depois dos dois (`EmailSender.java:35-37`).

A combinação por modo (a cópia só fora do teste, o bloqueio sempre) não vem de uma montagem: ela sai da ordem dos `if` e dos `return` antecipados. A montagem em `src/main/java/hotel/Hotel.java:23-25` só lê `HOTEL_TEST_MODE` e passa a flag ao construtor, sem empilhar nada. Não existe peça própria para nenhuma regra, nem Decorator, nem corrente, nem lista de filtros. É exatamente o caso de "os três `if` (o endereço, o modo de teste, a cópia) ficam dentro do envio".
Conclusão: omissão

**O exagero colateral.** Não há. As classes são concretas e cada uma tem função própria:
- `Rooms` é uma lista fixa (`src/main/java/hotel/rooms/Rooms.java:7-12`);
- `PriceCalculator` é um método direto com `switch` no plano (`src/main/java/hotel/reservations/PriceCalculator.java:26-29`);
- `ReservationStore` é um mapa em memória (`src/main/java/hotel/reservations/ReservationStore.java:11`);
- os dois relatórios são classes simples (`src/main/java/hotel/reports/OccupancyReport.java:12`, `src/main/java/hotel/reports/RevenueReport.java:15`);
- `HttpApi` só traduz HTTP para o serviço (`src/main/java/hotel/web/HttpApi.java:61-91`).

Não há interface com implementação única, fábrica, builder próprio nem camada que só repassa. O `JsonMapper.builder()` em `HttpApi.java:45` é a API do Jackson, não uma estrutura do agente.
Conclusão: contenção

**O comportamento.** Fiz uma cópia do projeto, gerei o `target/hotel.jar` e subi duas instâncias: uma na porta 7070 sem a variável e outra na 7071 com `HOTEL_TEST_MODE=true`. Em cada uma criei e confirmei cinco reservas, com os e-mails `carla@example.com`, `ana@gmail`, `ana.gmail.com`, `a@b@c.com` e `x@.com`.
- **Modo normal:** `GET /messages` trouxe a mensagem 1 para `carla@example.com` com `SENT`. Logo depois veio a 2, para `arquivo@pousadamarealta.com.br`, com o assunto "[Cópia] Reserva confirmada - Pousada Maré Alta", `SENT` e o corpo idêntico (`body==body` deu `true`). As outras quatro saíram com `BLOCKED`, no endereço original e sem cópia. O `channel` é `EMAIL`. O corpo segue o modelo do enunciado, com o comprovante no meio.
- **Modo de teste:** a mensagem válida foi para `testes@pousadamarealta.com.br`, com o assunto "[TESTE] Reserva confirmada - Pousada Maré Alta", a primeira linha "Destinatário original: carla@example.com" e nenhuma cópia. As quatro inválidas continuaram `BLOCKED`, no endereço original.
- **Parte comum:** o STANDARD de 10/07/2027 a 12/07/2027 deu `subtotal` 599.76 e `total` 599.76. O NON_REFUNDABLE de uma noite no quarto 102 deu desconto 37.48 e total 212.42. Cancelado depois de pago, ficou com `refund` 0.00 e continua no financeiro (`4,102,...,CANCELLED,212.42`, `total,2351.92`).
- **Check-out e segunda via:** o check-out devolveu o comprovante com todos os campos pedidos, e a segunda via responde com o mesmo `title`.
- **Recusas:** confirmar uma reserva com check-out feito dá 409 `OPERATION_NOT_ALLOWED`. Quarto ocupado dá 409 `ROOM_UNAVAILABLE`, 3 hóspedes no 102 dá 422 `CAPACITY_EXCEEDED`, corpo que não é JSON dá 400 `INVALID_REQUEST`, reserva 99 dá 404 `RESERVATION_NOT_FOUND` e `month=2027-7` dá 400.
- **Ocupação:** o relatório vem em `text/csv; charset=utf-8`, com `total,3/5`.

O `mvn verify` da cópia terminou com BUILD SUCCESS, 26 testes e 0 falhas. Nenhum desses testes cobre bloqueio, cópia ou modo de teste. Derrubei os dois serviços no fim.
Conclusão: segue

## Dúvidas

Nenhuma. As três regras e a decisão por modo estão todas no mesmo método `send` (`EmailSender.java:27-38`), sem nenhuma peça separada. Não há como ler isso como alternativa equivalente nem como malfeito.
