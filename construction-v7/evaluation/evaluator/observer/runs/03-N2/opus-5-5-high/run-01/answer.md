# Cenário 01

## Visão geral

O agente não usou Observer. Os avisos da equipe ficaram dentro do `ReservationService`, como métodos privados chamados um depois do outro. A confirmação chama o hóspede, a governança e o financeiro em sequência, e o serviço monta o destinatário, o assunto e o texto de cada e-mail. Não existe interface de ouvinte, não existe lista e o `main` não registra nada. O resto do desenho é enxuto e não tem estrutura sobrando. O problema mais sério está no comportamento: o cancelamento de uma reserva confirmada nunca avisa ninguém. O serviço muda o status para `CANCELLED` e só depois pergunta se a reserva estava `CONFIRMED`. A parte comum também tem defeitos: a alta temporada vale para dezembro inteiro, e o dinheiro sai como `R$ 779.80` em vez de `R$ 779,80`.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está em `com.pousadamarealta` e se divide em quatro pacotes:
- `domain`: `Reservation` (classe mutável com `confirm`, `cancel`, `checkIn` e `checkOut`) e os records `Room`, `Message` e `ReservationDocument`, além dos enums.
- `repository`: `ReservationRepository`, `RoomRepository` e `MessageRepository`. Os três guardam os dados em memória, em `HashMap` ou `ArrayList`, e os quartos são uma lista fixa (`repository/RoomRepository.java:9-15`).
- `service`: `PricingService` (diárias, desconto e arredondamento `HALF_EVEN`), `ReservationService` (regras de estado, disponibilidade, comprovante e e-mails) e `ReportService` (os dois CSVs).
- `api`: `ReservationApi` (rotas Javalin, validação do corpo e tradução de erros) e os DTOs em `api/dto`.

Uma requisição segue um caminho simples. A rota em `api/ReservationApi.java:39-51` chama o método do `ReservationService`. Esse método lê do repositório, aplica a regra e grava. Os erros saem como `IllegalArgumentException` com o código na mensagem, e um `switch` em `api/ReservationApi.java:192-210` converte esse código no status HTTP. O `App.main` monta as peças à mão e sobe o Javalin na porta da variável `PORT`, ou na 7070 (`App.java:16-41`). Os testes do agente são 28 testes unitários de serviço (`PricingServiceTest`, `ReportServiceTest`, `ReservationServiceTest`), sem nenhum teste HTTP. Para os avisos, eles só conferem que a confirmação gera 3 mensagens (`ReservationServiceTest.java:127`) e que o cancelamento de uma reserva pendente gera 0 (`ReservationServiceTest.java:184`). Nenhum teste confere os avisos do cancelamento de uma reserva confirmada.

**A parte do fim do enunciado.** Tudo fica em `service/ReservationService.java`. O `confirmReservation` chama em sequência `sendGuestConfirmationEmail`, `sendGovernanceConfirmationEmail` e `sendFinanceConfirmationEmail` (`service/ReservationService.java:100-102`). O `cancelReservation` chama `sendGovernanceCancellationEmail` e, se houver reembolso, `sendFinanceCancellationEmail` (`service/ReservationService.java:126-132`). Cada um desses métodos privados escreve no próprio serviço o endereço fixo, o assunto e o texto, e grava a `Message` direto no `MessageRepository`:
- governança: `service/ReservationService.java:214-231` e `246-261`;
- financeiro: `service/ReservationService.java:233-244` e `263-274`.

Os interessados não têm representação própria. "Escolher o caso" aqui é só uma lista fixa de chamadas no meio da regra de negócio. O construtor do serviço recebe o `MessageRepository` e nenhuma lista de ouvintes (`service/ReservationService.java:23-33`). O `App` não registra nenhum interessado (`App.java:21-26`).

**O raciocínio.** As instruções do pattern pedem duas coisas: o serviço anuncia o evento sem conhecer quem é avisado, e cada interessado é um ouvinte separado, registrado na montagem. Este código é exatamente o caso que as instruções chamam de omissão: "a confirmação e o cancelamento mandam os e-mails um depois do outro". Para pôr o restaurante ou o transfer, seria preciso editar o `ReservationService` e acrescentar mais um método privado e mais uma chamada. Não há nenhum traço do pattern que pudesse fazer disso um caso de malfeito.

No exagero colateral, não encontrei interface com uma implementação só, fábrica, builder nem camada de repasse. Os repositórios são classes concretas simples. Os DTOs com `from` apenas convertem o domínio para o JSON pedido.

No comportamento, a confirmação está certa, a não ser pelo formato do dinheiro. O cancelamento de uma reserva confirmada falha por completo. O código chama `reservation.cancel(refund)` na linha 124, e esse método põe o status em `CANCELLED` (`domain/Reservation.java:87-89`). Na linha 126 o serviço testa `getStatus() == CONFIRMED`, o que nunca é verdade nesse ponto. Por isso a governança e o financeiro nunca recebem aviso de cancelamento. Na parte comum há mais desvios:
- `isHighSeason` trata dezembro inteiro como alta temporada (`service/PricingService.java:13-18`);
- `formatCurrency` usa `String.format("R$ %.2f")`, que depende da localidade e não põe separador de milhar (`service/ReservationService.java:306-308`);
- o financeiro lista uma reserva reembolsada por inteiro com valor 0,00, embora ela não tenha deixado dinheiro na pousada (`service/ReportService.java:23`, que filtra só por `amountPaid > 0`).

## Item a item

**O enunciado (pede o pattern).** Os avisos são métodos privados do `ReservationService`, chamados em sequência na confirmação (`service/ReservationService.java:100-102`) e no cancelamento (`service/ReservationService.java:126-132`). Cada método monta destinatário, assunto e texto e grava a mensagem no repositório (`service/ReservationService.java:202-274`). Não existe interface de ouvinte nem lista injetada, e nada é registrado no `App` (`App.java:21-26`). O serviço conhece todos os destinos, então é a omissão que as instruções descrevem, e não o Observer.
Conclusão: omissão

**O exagero colateral.** Não há. A estrutura é direta: repositórios concretos, três serviços concretos e um `ReservationApi` que registra as rotas (`App.java:16-38`). Os DTOs (`api/dto/ReservationResponse.java`, `api/dto/RoomResponse.java`) só traduzem o domínio para o formato de JSON combinado, sem camada de repasse.
Conclusão: contenção

**O comportamento.** Fiz o build de uma cópia, subi o jar `reserva-system-1.0.0.jar` e chamei a API com `curl`.

O que confere:
- `GET /rooms` está certo.
- O STANDARD de 10/07 a 12/07/2027 dá 599.76.
- O NON_REFUNDABLE da SUITE de 01/03 a 06/03/2027 dá subtotal 2899.50, desconto 434.92 e total 2464.58, todos certos.
- A confirmação da reserva 1 (quarto 201) gerou três mensagens: a do hóspede, com o texto certo; a da governança (`governanca@...`, "Reserva confirmada - quarto 201", "Entrada: 10/03/2027 | Saída: 12/03/2027\nHóspedes: 2"); e a do financeiro (`financeiro@...`, "Pagamento recebido - reserva 1", "Valor pago: R$ 779.80").
- O cancelamento da reserva pendente 5 não gerou mensagem, como pedido.
- Check-out e segunda via devolvem o comprovante. A segunda via de uma reserva pendente dá 409.
- O erro ROOM_UNAVAILABLE dá 409, CAPACITY_EXCEEDED dá 422, corpo inválido dá 400 e reserva inexistente dá 404.
- A ocupação de 10/07/2027 vem como `total,1/5` em `text/csv; charset=utf-8`.
- `mvn verify` na cópia: 28 testes, 0 falhas, BUILD SUCCESS.

O que falha:
- O cancelamento da reserva confirmada 1 (STANDARD, refund 779.80) e o da 3 (NON_REFUNDABLE, refund 0) não geraram nenhuma mensagem: a lista ficou com 9 itens antes e depois. A governança deveria receber "Reserva cancelada - quarto 201" e "Reserva cancelada - quarto 301", e o financeiro "Reembolso - reserva 1". Aconteceu pelo erro de ordem em `service/ReservationService.java:124-126`.
- O dinheiro nos e-mails sai como `R$ 779.80` e `R$ 2464.58`, e não como `R$ 779,80` e `R$ 2.464,58`.
- Uma DELUXE de 10/12 a 11/12/2027 custou 467.88; o certo é 389.90, porque a alta temporada só começa em 15/12.
- O financeiro de 2027-03 lista a reserva 1, reembolsada por inteiro, com valor 0.00.

Os avisos do cancelamento são metade da regra do fim do enunciado e não funcionam. A confirmação funciona, com o formato do dinheiro errado.
Conclusão: em parte

## Dúvidas

Nenhuma sobre a classe. No comportamento, a nota fica entre `em parte` e `não segue`. Escolhi `em parte` porque os três avisos da confirmação e o silêncio da reserva pendente cancelada estão certos. Os avisos do cancelamento de uma reserva confirmada, porém, nunca saem.
