# Cenário 05

## Visão geral

O agente colocou os textos dos três documentos em funções separadas (`DocumentTexts`). Mas a decisão de qual documento criar não ficou num lugar só: o mesmo `if / else if / else` sobre o tipo de hóspede aparece copiado nos três lugares onde o documento sai (confirmação, check-out e segunda via), todos em `ReservationService`. Quando chegar o documento das agências, alguém vai ter de achar e mudar as três cópias. Esse é o problema mais sério. O comportamento está correto em tudo o que testei. Não há estrutura sem necessidade no resto do sistema.

## Item a item

**O enunciado (pede o pattern).** O hóspede é um único `record Guest`, com os sete campos e dois testes de tipo, `isCompany()` e `isForeigner()` (`src/main/java/hotel/reservations/Guest.java:7-26`). Os textos ficam em três funções estáticas: `personReceipt`, `companyInvoice` e `foreignReceipt` (`src/main/java/hotel/documents/DocumentTexts.java:15`, `:33`, `:55`). Não existe criador, fábrica nem método polimórfico no tipo de hóspede. A escolha de qual documento criar se repete, com a mesma cadeia `if (isCompany()) … else if (isForeigner()) … else …`, no e-mail de confirmação (`src/main/java/hotel/reservations/ReservationService.java:71-80`), no check-out (`src/main/java/hotel/reservations/ReservationService.java:124-133`) e na segunda via (`src/main/java/hotel/reservations/ReservationService.java:145-154`). As três cópias também repetem à mão o título de cada documento ("Fatura de hospedagem", "Booking receipt", "Comprovante de reserva"). As instruções descrevem exatamente este caso como omissão: o `if` sobre o tipo de hóspede repetido nos três lugares, mesmo que o texto de cada documento esteja em funções auxiliares separadas.
Conclusão: omissão

**O exagero colateral.** Não há. Quartos, preço, armazenamento, relatórios e API são classes concretas e diretas (`src/main/java/hotel/rooms/Rooms.java:8`, `src/main/java/hotel/reservations/PriceCalculator.java:14`, `src/main/java/hotel/reservations/ReservationStore.java:10`, `src/main/java/hotel/reports/OccupancyReport.java:13`, `src/main/java/hotel/reports/RevenueReport.java:61`), sem interface com uma implementação só, sem fábrica e sem builder. O `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:4-14`) é uma classe concreta pequena que fixa o canal `EMAIL` antes de guardar a mensagem, então não é uma camada que só repassa. `Hotel` só monta as peças (`src/main/java/hotel/Hotel.java:284-293`).
Conclusão: contenção

**O comportamento.** Fiz uma cópia do projeto. O `mvn -q package -DskipTests` gerou `target/hotel.jar` (classe principal `hotel.Hotel`), e o `mvn -q verify` passou: 29 testes do agente, em 5 classes, todos passaram (0 falhas, 0 erros). Subi o serviço na porta 7070 e chamei a API com `curl`:
- **Identificação.** CPF, CNPJ com razão social e passaporte com país dão 201, e a reserva mostra os cinco campos, com null nos que não se aplicam. Dão 400 INVALID_REQUEST: nenhuma identificação, CPF junto com passaporte, CNPJ sem razão social e passaporte sem país (`src/main/java/hotel/web/HttpApi.java:126-133`).
- **Fatura (reserva 2).** Mostra "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", a linha "Hóspede: Ana Souza", "Total: R$ 779,80" e "ISS retido (5%): R$ 38,99", como pedido.
- **Booking receipt.** A reserva 3 traz "Booking receipt no. 3", "Check-in: 2027-04-10 | Check-out: 2027-04-12 | 2 nights" e "Nights: BRL 779.80". A reserva 4 (SUITE, alta temporada, NON_REFUNDABLE, uma noite) traz "1 night", "Nights: BRL 695.88", "Discount: BRL 104.38" e "Total: BRL 591.50", com o arredondamento certo.
- **CPF (reserva 1).** Recebe o comprovante exatamente igual ao do enunciado.
- **Os três lugares.** Para as reservas 1 a 4, o JSON do check-out é idêntico byte a byte ao da segunda via e traz o campo `text`. O `text` aparece inteiro dentro do corpo do e-mail de confirmação, que é sempre em português, tem o assunto "Reserva confirmada - Pousada Maré Alta" e o canal `EMAIL`.
- **Parte comum.**
  - STANDARD no 101, de 10/07 a 12/07/2027: total 599.76.
  - Recusas: 422 CAPACITY_EXCEEDED, 409 ROOM_UNAVAILABLE, 400 para corpo que não é JSON, 404 RESERVATION_NOT_FOUND, e 409 OPERATION_NOT_ALLOWED para a segunda via de uma reserva pendente.
  - Cancelamento de STANDARD confirmada devolve tudo (refund 599.76).
  - O relatório financeiro de 2027-07 traz só a reserva 4 com 591.50, porque a 5 foi cancelada com reembolso total.
  - O de ocupação de 2027-07-10 traz `total,1/5`, em `text/csv; charset=utf-8`.

Depois derrubei o serviço e conferi que a porta 7070 ficou livre.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe: não há criador, fábrica nem método no tipo de hóspede, nenhuma das formas aceitas como acerto foi usada, e a decisão está copiada três vezes. Também não é `malfeito`, porque não existe nenhuma estrutura do pattern convivendo com a criação à mão.
