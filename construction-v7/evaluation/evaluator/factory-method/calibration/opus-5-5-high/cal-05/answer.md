# Cenário 01

## Visão geral

O agente montou um sistema simples e organizado: um serviço de reservas, um cálculo de preço, um armazenamento em memória, relatórios e uma camada HTTP fina, sem interfaces nem fábricas sobrando. A parte do fim do enunciado foi resolvida sem o pattern. Os textos dos três documentos ficaram em funções separadas, mas a escolha de qual documento criar é uma cadeia `if / else if / else` sobre o tipo de hóspede, copiada nos três lugares onde o documento sai: a confirmação (o e-mail), o check-out e a segunda via. Para o quarto tipo, o das agências, seria preciso mexer nas três cópias, e esse é o problema mais sério. O comportamento, testado pela API, está todo certo: identificação, fatura com ISS, *booking receipt* em inglês e documento igual nos três lugares.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica no pacote `hotel`, dividido em subpacotes:
- `rooms`: os cinco quartos fixos no código (`rooms/Rooms.java:10-15`).
- `reservations`:
  - `Reservation`, que guarda o estado e tem as transições como métodos de pacote.
  - `ReservationService`, que tem as regras do ciclo de vida, a disponibilidade, a capacidade e a emissão de documentos.
  - `PriceCalculator`, com a alta temporada e o desconto do `NON_REFUNDABLE`, arredondando com `HALF_EVEN` a cada passo (`reservations/PriceCalculator.java:19-52`).
  - `ReservationStore`, um `LinkedHashMap` em memória com ids a partir de 1 (`reservations/ReservationStore.java:12-18`).
  - `Guest`, um record com os sete campos de identificação, quase todos opcionais.
- `documents`: `DocumentTexts`, com os três textos; `ReservationReceipt`, o DTO do documento; e `TextFormat`, para o dinheiro e as datas em pt-BR.
- `messages`: `EmailSender` e `SentMessages`, a lista de mensagens enviadas.
- `reports`: os dois CSVs.
- `web`: `HttpApi`, com rotas, validação do corpo e tradução de erros, mais os DTOs de entrada e de saída.

O caminho de uma requisição é este: a rota em `web/HttpApi.java:61-91` lê e valida o corpo (`web/HttpApi.java:109-140`), monta um `NewReservation` e chama o `ReservationService`. O serviço aplica as regras, grava no `ReservationStore` e devolve a `Reservation`, que vira `ReservationResponse`. As exceções `HotelException` viram `{error, message}` com o status mapeado num `switch` sobre o `ErrorCode` (`web/HttpApi.java:100-107`). A montagem é manual, no construtor de `Hotel` (`Hotel.java:19-28`), e o `main` lê `PORT`, com 7070 como padrão (`Hotel.java:41`). O jar é sombreado com `hotel.Hotel` no manifesto (`pom.xml`). O agente escreveu 29 testes em cinco classes: preço, serviço, relatórios, documentos por hóspede e HTTP.

**A parte do fim.** O tipo de hóspede não tem representação própria. É deduzido de quais campos do `Guest` estão preenchidos, com `isCompany()` e `isForeigner()` (`reservations/Guest.java:20-26`), e a pessoa com CPF é o "senão". Cada documento tem o seu texto num método estático de `DocumentTexts`:
- `personReceipt` (`documents/DocumentTexts.java:15-31`);
- `companyInvoice`, com o ISS calculado ali mesmo (`documents/DocumentTexts.java:33-53`, ISS em `:49-51`);
- `foreignReceipt`, com o formato `BRL` e as datas ISO (`documents/DocumentTexts.java:55-76`).

O DTO é um só para os três (`documents/ReservationReceipt.java:10-48`): recebe o título e o texto de fora. A escolha de qual título e qual texto usar está escrita três vezes no `ReservationService`:
- `confirm`: `reservations/ReservationService.java:71-80`;
- `checkOut`: `reservations/ReservationService.java:124-133`;
- `receiptCopy`: `reservations/ReservationService.java:145-154`.

As três cópias são idênticas: `if (guest.isCompany()) … else if (guest.isForeigner()) … else …`, e cada ramo chama `new ReservationReceipt("<título>", …, DocumentTexts.<texto>(…))`.

**O raciocínio.** As instruções do pattern pedem que a escolha de qual documento criar more num lugar só e que os três lugares peçam o documento a esse lugar. Pode ser um criador com subclasses, uma fábrica concentrada ou `guest.issueDocument(...)`. Aqui não há esse lugar: nenhuma classe ou método centraliza a decisão. Os três lugares decidem sozinhos, com o mesmo `if` sobre o tipo de hóspede. É exatamente o caso que as instruções chamam de omissão, "mesmo que o texto de cada documento esteja em funções auxiliares separadas". O que aproxima o desenho do pattern é só a separação dos textos em `DocumentTexts` e o DTO comum. Isso não basta, porque essa separação não concentra a decisão. Também não é `malfeito`, porque não existe uma fábrica com algum lugar escapando dela: ela simplesmente não existe. Fora dessa parte, o desenho é contido: classes concretas, sem interfaces de uma implementação só, sem builders nem fábricas. O comportamento foi testado de ponta a ponta e confere com o enunciado.

## Item a item

**O enunciado (pede o pattern).** O agente não concentrou a criação do documento. A cadeia `if (reservation.guest().isCompany()) … else if (reservation.guest().isForeigner()) … else …` que escolhe entre "Fatura de hospedagem", "Booking receipt" e "Comprovante de reserva" aparece três vezes no `ReservationService`:
- na confirmação, que monta o e-mail (`reservations/ReservationService.java:71-80`);
- no check-out (`reservations/ReservationService.java:124-133`);
- na segunda via (`reservations/ReservationService.java:145-154`).

O texto de cada documento está em funções auxiliares separadas (`documents/DocumentTexts.java:15`, `:33`, `:55`), mas a decisão de qual criar não está num lugar só. Com as agências de turismo, seria preciso acrescentar um ramo nas três cópias, além de um novo `isAgency()` em `Guest`. Não é o pattern que o enunciado pedia.
Conclusão: omissão

**O exagero colateral.** Não há. Não existem interfaces de uma implementação só, fábricas, builders nem camadas genéricas. O `EmailSender` (`messages/EmailSender.java:12-14`) é uma classe concreta curta que fixa o canal `EMAIL` e é a única porta de entrada para `SentMessages.add`, que é de pacote (`messages/SentMessages.java:11`). Tem um papel próprio e não chega a ser um repasse vazio. Os DTOs `ReservationResponse` e `CreateReservationRequest` servem ao contrato JSON.
Conclusão: contenção

**O comportamento.** Testei com o jar de uma cópia, `PORT=7070`.
- **Identificação** (`POST /reservations`):
  - CPF, CNPJ com razão social e passaporte com país dão 201, e a reserva volta com os cinco campos, `null` nos que não se aplicam.
  - Os casos inválidos dão 400 `INVALID_REQUEST`: nenhuma identificação, CPF junto com passaporte, só o CNPJ, só o passaporte, só o país.
- **E-mails** (`GET /messages`), depois de confirmar as reservas 1 a 4: quatro mensagens `EMAIL` com o assunto "Reserva confirmada - Pousada Maré Alta", o texto em português e o documento certo dentro.
  - Comprovante: igual ao do enunciado.
  - Fatura: "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)" e "ISS retido (5%): R$ 38,99" sobre R$ 779,80.
  - *Booking receipt*: "Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night", "Discount: BRL 86.98" antes do total, e "2 nights" na reserva de duas noites.
- **Milhar**: "BRL 2,319.60" / "BRL 1,971.66" e "R$ 1.559,60" / "R$ 1.325,66".
- **ISS sobre total com desconto**: R$ 212,42 dá R$ 10,62, certo.
- **Documento nos três lugares**: a segunda via e o check-out devolvem JSON com `title`, os cinco campos de identificação e `text`. O `text` é idêntico ao bloco que saiu no e-mail, conferido para as reservas 1, 2 e 3.
- **Parte comum**:
  - STANDARD de 10/07 a 12/07 no quarto 101 dá 599,76.
  - Segunda via de reserva pendente dá 409.
  - Cancelamento: o `NON_REFUNDABLE` confirmado devolve 0,00 e o STANDARD confirmado devolve 499,80; cancelar uma reserva com check-out dá 409.
  - Erros: 422 `CAPACITY_EXCEEDED`, 409 `ROOM_UNAVAILABLE`, 404 `ROOM_NOT_FOUND` e `RESERVATION_NOT_FOUND`, 400 para corpo que não é JSON e para mês inválido.
  - Relatórios: o de ocupação vem em `text/csv; charset=utf-8` com `total,3/5`. O financeiro conta a não reembolsável cancelada (1971.66).
- **Testes do agente**: o `mvn verify` da cópia passou com 29 testes, 0 falhas e 0 erros.

O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe: as três cópias da escolha são inequívocas, e as instruções dão esse caso como omissão. A única dúvida, pequena, é se o `EmailSender` (`messages/EmailSender.java:4-15`) seria uma "camada que só repassa". Julguei que não, porque ele fixa o canal e é o único caminho para gravar mensagens.
