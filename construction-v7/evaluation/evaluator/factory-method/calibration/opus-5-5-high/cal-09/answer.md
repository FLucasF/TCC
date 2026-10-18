# Cenário 01

## Visão geral

O agente concentrou num lugar só a escolha do documento por tipo de hóspede. A `DocumentFactory` decide, a partir do `Guest`, qual `DocumentWriter` usar: comprovante, fatura ou *booking receipt*. Os três lugares onde o documento sai (e-mail de confirmação, check-out e segunda via) pedem o documento só a ela. O caso da pessoa com CPF, que já vinha da parte comum, também passa pela fábrica e tem o seu próprio writer. No resto do sistema não há estrutura sobrando, e tudo o que testei confere com o enunciado. Não achei nenhum problema sério. O ponto mais fraco é pequeno: os três writers repetem o corpo do texto (quarto, datas, diárias, desconto e total), o que não afeta o desenho da escolha.

## Item a item

**O enunciado (pede o pattern).** Toda a escolha do documento fica em `DocumentFactory.writerFor`, em `src/main/java/hotel/documents/DocumentFactory.java:19-27`. Se o hóspede é empresa, a fábrica devolve `companyInvoice`; se é estrangeiro, devolve `foreignReceipt`; nos outros casos, devolve `personReceipt`. Quem a chama usa o método `issue`, em `src/main/java/hotel/documents/DocumentFactory.java:14-17`, e não conhece o tipo concreto. Cada documento implementa a interface `DocumentWriter` (`src/main/java/hotel/documents/DocumentWriter.java:7-12`) numa classe própria: `PersonReceiptWriter.java:7`, `CompanyInvoiceWriter.java:9` e `ForeignReceiptWriter.java:9`, todas em `src/main/java/hotel/documents/`. Os três lugares passam pela fábrica: o e-mail de confirmação em `src/main/java/hotel/reservations/ReservationService.java:70`, o check-out em `ReservationService.java:112` e a segunda via em `ReservationService.java:121`. O corpo do e-mail só encaixa o `receipt.text()` (`ReservationService.java:128-134`) e não pergunta qual é o tipo de hóspede. Procurei `isCompany`, `isForeigner`, `cpf()` e `passport()` no `src/main`: o teste de tipo só aparece na fábrica. Os outros usos apenas copiam campos para o JSON (`ReservationReceipt.java:34-38` e `ReservationResponse.java:37-41`) ou montam o texto dentro do próprio writer. Para a agência de turismo, basta criar um writer novo e acrescentar um ramo em `writerFor`, sem mexer nos três lugares. É uma fábrica concentrada, que as instruções aceitam como acerto.
Conclusão: acerto

**O exagero colateral.** Não há. Fora de `documents/`, as classes são concretas e cada uma tem uma função clara: `PriceCalculator`, `ReservationStore`, `Rooms`, os dois relatórios e o `HttpApi`. Não há interface com uma implementação só, fábrica, builder ou camada que só repassa. A única peça fina é o `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:12-14`): ele só chama `SentMessages.add` com o canal `"EMAIL"`. Mesmo assim, é ele que fixa o canal e representa o envio que o enunciado diz que um dia vai sair de verdade, então não o conto como exagero.
Conclusão: contenção

**O comportamento.** Testei a API num jar construído a partir de uma cópia do projeto, rodando na porta 7070:
- **Criação de reservas:** criei uma com CPF, uma com CNPJ e razão social e três com passaporte e país (201). A resposta mostra os cinco campos de identificação, com null nos que não se aplicam.
- **Recusas:** tentei sem nenhuma identificação, com CPF e passaporte juntos, com CPF e empresa juntos, só com o CNPJ, só com a razão social, só com o passaporte e só com o país. Todas voltaram 400 com `INVALID_REQUEST`.
- **E-mails de confirmação** (`GET /messages`):
  - A pessoa com CPF recebeu o comprovante igual ao do enunciado.
  - A empresa recebeu "Fatura de hospedagem nº 2", com "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", "Hóspede: Ana Souza" e "ISS retido (5%): R$ 38,99" sobre R$ 779,80.
  - O estrangeiro recebeu o *booking receipt* com "2027-03-10", "2 nights" e "BRL 499.80". Uma reserva de uma noite em alta temporada, não reembolsável, mostrou "1 night", "Nights: BRL 695.88", "Discount: BRL 104.38" e "Total: BRL 591.50". Uma reserva de três noites mostrou "BRL 1,739.70".
  - Nos três e-mails o envelope ficou em português, e o assunto foi "Reserva confirmada - Pousada Maré Alta".
- **Check-out e segunda via:** o JSON traz os cinco campos e o campo `text`. Para as reservas 1, 2 e 3, o `text` da segunda via aparece idêntico dentro do corpo do e-mail, e o do check-out tem o mesmo conteúdo.
- **Parte comum:** o STANDARD de 10/07 a 12/07/2027 no quarto 102 deu 599,76. A segunda via de uma reserva pendente deu 409 `OPERATION_NOT_ALLOWED`. Reservar um quarto já ocupado deu 409 `ROOM_UNAVAILABLE`, e passar da capacidade deu 422 `CAPACITY_EXCEEDED`. Cancelar uma reserva não reembolsável já confirmada devolveu `refund` 0.00, e ela continuou no financeiro de 2027-07 com 591.50. A ocupação de 2027-03-11 veio em `text/csv; charset=utf-8`, ordenada por quarto, com `total,3/5`.
- **Testes do agente:** o `mvn -q verify` passou, com 29 testes, nenhuma falha e nenhum erro (DocumentsByGuest 3, Reports 5, PriceCalculator 6, ReservationService 11, HttpApi 4). No fim derrubei o serviço.
Conclusão: segue

## Dúvidas

A forma usada é uma fábrica concentrada: uma classe que escolhe o writer pelo tipo de hóspede com dois `if` (`DocumentFactory.java:19-27`). Não é o Factory Method clássico com criador sobrescrito, nem um método polimórfico no `Guest`. O `Guest` é um record com campos que podem ser null e marcadores booleanos (`src/main/java/hotel/reservations/Guest.java:20-26`). O "senão é pessoa" da fábrica depende da validação do `HttpApi` (`src/main/java/hotel/web/HttpApi.java:126-134`) para nunca receber um hóspede sem identificação.

O campo `title` do JSON muda com o documento ("Fatura de hospedagem", "Booking receipt"). O enunciado só fixa "Comprovante de reserva" para o caso comum, então tratei isso como uma decisão razoável e não como desvio.
