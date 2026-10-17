# Cenário 05

## Visão geral

O comportamento pedido está implementado direito: as três formas de identificar o hóspede, a validação, a fatura com ISS, o *booking receipt* em inglês, o campo `text` e os nulos nos campos que não se aplicam. O desenho, porém, não concentra a criação do documento. A escolha entre fatura, *booking receipt* e comprovante virou um `if / else if / else` sobre o tipo de hóspede, copiado igual nos três lugares que criam o documento: `confirm`, `checkOut` e `receiptCopy`. Os textos de cada documento foram para funções auxiliares, mas a decisão de qual documento criar continua repetida. Quando chegar o documento das agências, vai ser preciso mexer nos três lugares de novo. Esse é o problema mais sério.

## Item a item

**O pedido (pede o pattern).** Na base, o comprovante era criado com `new` em três lugares (`base/src/main/java/hotel/reservations/ReservationService.java:68`, `:110` e `:119`). O agente manteve esses três lugares e pôs em cada um a mesma cadeia de decisão:
- em `confirm`: `src/main/java/hotel/reservations/ReservationService.java:71-80`;
- em `checkOut`: `src/main/java/hotel/reservations/ReservationService.java:124-133`;
- em `receiptCopy`: `src/main/java/hotel/reservations/ReservationService.java:145-154`.

Cada cópia testa `reservation.guest().isCompany()`, depois `isForeigner()`, e chama `new ReservationReceipt(<título>, reservation, room, DocumentTexts.<texto>(...))`. Até o título ("Fatura de hospedagem", "Booking receipt", "Comprovante de reserva") está escrito literalmente nas três cópias.

A classe `src/main/java/hotel/documents/DocumentTexts.java:15`, `:33` e `:55` só reúne três funções estáticas de texto. Ela não decide nada. O `Guest` ganhou só os testes `isCompany()`/`isForeigner()` (`src/main/java/hotel/reservations/Guest.java:20-26`), sem nenhum método que emita o documento. O `ReservationReceipt` passou a receber o título e o texto prontos de fora (`src/main/java/hotel/documents/ReservationReceipt.java:29`).

Não existe um criador, nem uma fábrica, nem um método polimórfico. Quem chama é que pergunta o tipo de hóspede, e faz isso em cada um dos três lugares. É exatamente o caso que as instruções chamam de omissão: o `if` sobre o tipo repetido nos três lugares, mesmo com o texto separado em funções auxiliares.
Conclusão: omissão

**O exagero colateral.** Não há. As mudanças fora dos documentos foram mínimas:
- campos novos em `Guest`, `CreateReservationRequest` e `ReservationResponse`;
- a validação e o auxiliar `nullIfBlank` em `src/main/java/hotel/web/HttpApi.java:126-137` e `:181-183`.

Não apareceu nenhuma interface, fábrica, builder ou camada nova.
Conclusão: contenção

**O comportamento.** Testei numa cópia em `/tmp/eval`. O serviço subiu na porta 7070 e foi derrubado no fim.

- **Criação das reservas.** Criei reservas por empresa (201, de 10/03 a 12/03/2027), estrangeiro (202, as mesmas datas), CPF (101), estrangeiro em `NON_REFUNDABLE` na suíte em julho (3 noites, com desconto e total acima de mil) e estrangeiro com 1 noite. Todas deram 201. O `GET /reservations/{id}` mostra `companyCnpj`/`companyName`/`guestPassport`/`guestCountry`, e os campos que não se aplicam vêm `null`.
- **Recusas.** Testei seis casos: nenhuma identificação; CPF junto com passaporte; só o CNPJ; só o passaporte; CPF junto com `companyName`; empresa junto com passaporte. Todos deram 400 `INVALID_REQUEST`.
- **Segunda via (`GET /reservations/1/document`).** O `text` saiu idêntico ao exemplo do pedido: "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", "Hóspede: Ana Souza", "Total: R$ 779,80" e "ISS retido (5%): R$ 38,99". O `guestCpf` vem `null`.
- **Booking receipt.** Reserva 2: "Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights", "Nights: BRL 779.80". Reserva 4: "Nights: BRL 2,087.64", "Discount: BRL 313.15", "Total: BRL 1,774.49". Reserva 5: "1 night".
- **CPF.** Recebe o comprovante de antes, com o título "Comprovante de reserva".
- **E-mail de confirmação (`/messages`).** Continua em português, com o documento certo dentro.
- **Check-out.** O JSON do check-out das reservas 1, 2 e 3 é igual, byte a byte, ao da segunda via.
- **O que não mudou.** O cancelamento `NON_REFUNDABLE` devolve 0,00. A capacidade excedida dá 422. A falta de `guestName` dá 400. Uma reserva inexistente dá 404. Os relatórios de receita e de ocupação saem como antes.
- **Testes.** `mvn verify` na cópia: 29 testes, 0 falhas, BUILD SUCCESS. Desses, 3 são testes novos do agente, em `DocumentsByGuestTest`.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Nenhuma das formas aceitas aparece no código: nem o criador clássico, nem uma fábrica concentrada, nem `guest.issueDocument(...)`. A única coisa que foi concentrada é o texto, em `DocumentTexts`. A escolha do documento está nos três lugares que o criam, e é isso que define a omissão.
