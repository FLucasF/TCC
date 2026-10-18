# Cenário 01

## Visão geral

O agente construiu um sistema enxuto e correto: os três documentos saem certos nos três lugares, e as regras da parte comum também conferem. O problema está no desenho da parte do fim. O agente não concentrou em lugar nenhum a escolha de qual documento criar. O mesmo `if / else if / else` sobre o tipo de hóspede (`isCompany()` / `isForeigner()` / o resto) aparece copiado em `confirm`, `checkOut` e `receiptCopy`. Separar o texto de cada documento em `DocumentTexts` não muda isso, porque a decisão continua repetida nos três pontos. Quando chegar o documento da agência, vai ser preciso mexer nos três lugares, além do `Guest` e da validação.

## Item a item

**O enunciado (pede o pattern).** O que define o tipo de hóspede são campos que podem ou não estar preenchidos num record só (`src/main/java/hotel/reservations/Guest.java:7-14`), com os testes `isCompany()` e `isForeigner()` (`src/main/java/hotel/reservations/Guest.java:20-26`). A escolha do documento aparece três vezes, sempre com a mesma cadeia de `if`:
- no e-mail de confirmação, em `src/main/java/hotel/reservations/ReservationService.java:71-80`;
- no check-out, em `src/main/java/hotel/reservations/ReservationService.java:124-133`;
- na segunda via, em `src/main/java/hotel/reservations/ReservationService.java:145-154`.

Os textos ficam em funções estáticas separadas (`src/main/java/hotel/documents/DocumentTexts.java:15`, `:33`, `:55`), e o título é passado como literal em cada ramo das três cópias. Falta uma fábrica que escolha o documento pelo tipo de hóspede, um criador com método sobrescrito ou um `guest.issueDocument(...)`. É exatamente o caso que as instruções chamam de omissão: o `if` sobre o tipo de hóspede repetido nos três lugares, ainda que o texto esteja em funções auxiliares.
Conclusão: omissão

**O exagero colateral.** Não há. As classes são concretas e cada uma tem um papel: `Rooms`, `ReservationStore`, `PriceCalculator`, `EmailSender`, os relatórios e `HttpApi`. Não há interfaces com uma implementação só, fábricas, builders ou camadas que só repassam. A montagem é direta em `src/main/java/hotel/Hotel.java:19-27`. O `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:12-14`) é fino, mas fixa o canal `EMAIL` e é o lugar natural do envio. Não chega a ser exagero.
Conclusão: contenção

**O comportamento.** Testei numa cópia: o `mvn package` gerou `target/hotel.jar`, e o serviço subiu na porta 7070.
- **Identificação.** O POST com CPF, com CNPJ e razão social e com passaporte e país devolveu 201. Os cinco campos voltam, com `null` nos que não se aplicam. As recusas vieram como pedido: sem identificação, com dois jeitos (CPF e passaporte), só com CNPJ, só com passaporte, só com país, e com CPF mais `companyName`. Todas deram 400 `INVALID_REQUEST`. A regra fica em `src/main/java/hotel/web/HttpApi.java:126-134`.
- **Fatura.** 779,80 deu "ISS retido (5%): R$ 38,99", e a empresa aparece no lugar do CPF.
- **Booking receipt.** Com um NON_REFUNDABLE de uma noite saíram "1 night", "Discount: BRL 58.48" e as datas `2027-04-10`. A SUITE em julho, com duas noites, deu "BRL 1,391.76" e "2 nights".
- **Pessoa com CPF.** O comprovante saiu idêntico ao modelo do enunciado.
- **Os três lugares.** O e-mail é em português, com o assunto "Reserva confirmada - Pousada Maré Alta". O `text` da segunda via e o do check-out são iguais e estão contidos no corpo do e-mail, nos três tipos. O JSON do documento traz `title`, os cinco campos e `text`.
- **Parte comum.**
  - O STANDARD de 10/07 a 12/07/2027 deu 599.76.
  - A segunda via de reserva pendente deu 409 `OPERATION_NOT_ALLOWED`.
  - A sobreposição de datas deu 409 `ROOM_UNAVAILABLE`, e 3 pessoas no 102 deram 422 `CAPACITY_EXCEEDED`.
  - Cancelar um NON_REFUNDABLE confirmado deu `refund` 0.00, e a reserva continua no financeiro.
  - Os CSVs saíram com `text/csv; charset=utf-8`, com `total,1/5` e `total,1789.46`.
  - Um mês fora do formato e um corpo que não é JSON deram 400.
- **Testes do agente.** O `mvn verify` passou com 29 testes, sem falhas nem erros.
- **Fim.** O serviço foi derrubado.
Conclusão: segue

## Dúvidas

Nenhuma. A cadeia de `if` sobre o tipo de hóspede está copiada literalmente nos três pontos de emissão, e não existe nenhuma estrutura de fábrica, nem parcial.
