# Cenário 05

## Visão geral

O agente montou um sistema de reservas enxuto e correto, mas não aplicou o pattern. Não existe nenhum lugar que concentre a escolha do documento. O mesmo `if / else if / else` sobre o tipo de hóspede (`isCompany()`, `isForeigner()`, senão CPF) aparece três vezes no `ReservationService`: na confirmação, que manda o e-mail, no check-out e na segunda via. Cada cópia escolhe o texto em `DocumentTexts` e o título. Uma agência de turismo obrigaria a mexer nos três lugares, além de no `Guest`. O comportamento confere em tudo o que testei: a validação da identificação, a fatura com ISS, o *booking receipt* em inglês, o documento igual nos três lugares, os preços e os relatórios. Não há estrutura sobrando.

## Como foi implementado, e por que o julgamento

**O projeto.** É um único módulo Maven com Java 21 e Javalin 6.7.0, que gera um jar sombreado `target/hotel.jar` com a classe principal `hotel.Hotel`. Os pacotes:
- `hotel.Hotel` monta tudo à mão e sobe na porta de `PORT`, ou na 7070 (`src/main/java/hotel/Hotel.java:19-43`). As peças são `Rooms`, `ReservationStore`, `SentMessages`, `EmailSender`, `ReservationService`, `OccupancyReport`, `RevenueReport` e `HttpApi`.
- `hotel.web`: `HttpApi` registra as rotas, traduz `HotelException` em status e JSON (`HttpApi.java:93-107`) e valida o corpo da criação (`HttpApi.java:109-140`). `CreateReservationRequest` e `ReservationResponse` são records de entrada e saída.
- `hotel.reservations`: `ReservationService` guarda as regras de ciclo de vida (criar, confirmar, cancelar, check-in, check-out, segunda via). `PriceCalculator` calcula o preço com alta temporada e desconto, arredondando meio para o par a cada passo. `Reservation` é a entidade com estado. `Guest` é um record com os sete campos de identificação, dos quais só um jeito vem preenchido, e tem `isCompany()` e `isForeigner()` (`Guest.java:7-26`). `ReservationStore` guarda as reservas num `LinkedHashMap` em memória.
- `hotel.documents`: `DocumentTexts` tem três funções estáticas, uma por texto: `personReceipt`, `companyInvoice` e `foreignReceipt` (`DocumentTexts.java:15,33,55`). `ReservationReceipt` é o record do JSON do documento, com os cinco campos de identificação e `text` (`ReservationReceipt.java:10-27`). `TextFormat` cuida do formato brasileiro.
- `hotel.messages`: `EmailSender` grava a mensagem em `SentMessages`, uma lista em memória.
- `hotel.reports`: os dois CSVs.

Uma requisição segue este caminho: rota no `HttpApi`, depois `ReservationService`, que consulta `Rooms` e `ReservationStore`, depois `ReservationResponse` ou `ReservationReceipt` serializados pelo Jackson. Os testes do agente são 29 no total, todos passando: `PriceCalculatorTest` (6), `ReservationServiceTest` (11), `DocumentsByGuestTest` (3), `HttpApiTest` (4) e `ReportsTest` (5).

**A parte que pede o pattern.** O tipo de hóspede não é representado por um tipo próprio. É deduzido dos campos nulos do `Guest`, por `isCompany()` (`Guest.java:20`) e `isForeigner()` (`Guest.java:24`). O documento é sempre o mesmo record `ReservationReceipt`, que muda só no título e no texto. A escolha entre os três textos é feita em três lugares, com a mesma cadeia de `if`:
- na confirmação, que monta o e-mail: `ReservationService.java:70-80`;
- no check-out: `ReservationService.java:123-133`;
- na segunda via: `ReservationService.java:144-154`.

As regras de cada documento moram em `DocumentTexts`: o ISS de 5% em `DocumentTexts.java:49-51` e o formato `BRL` em `DocumentTexts.java:74-76`. Os títulos ("Fatura de hospedagem", "Booking receipt", "Comprovante de reserva") estão escritos à mão em cada uma das três cópias.

**O raciocínio.** As instruções do pattern pedem que a decisão de *qual* documento criar more num lugar só, e que os três lugares peçam o documento a esse lugar. Aqui é exatamente o contrário. A decisão está repetida nos três lugares, e as instruções classificam isso como omissão, "mesmo que o texto de cada documento esteja em funções auxiliares separadas". As funções de `DocumentTexts` e o construtor de conveniência de `ReservationReceipt` só evitam repetir o texto. Não concentram a escolha. Também não há fábrica parcial que permita falar em malfeito: nenhum dos três lugares pede o documento a ninguém. Uma agência exigiria um novo campo e um novo `isAgency()` no `Guest`, além de um quarto ramo em cada uma das três cópias. Quanto ao exagero colateral, as classes são concretas e necessárias: não há interface com uma implementação só, fábrica, builder nem camada que só repassa. O comportamento conferiu em tudo o que testei.

## Item a item

**O pattern pedido.** A escolha do documento pelo tipo de hóspede é um `if (isCompany()) … else if (isForeigner()) … else …` copiado em `ReservationService.java:71-80` (confirmação e e-mail), `ReservationService.java:124-133` (check-out) e `ReservationService.java:145-154` (segunda via). Cada cópia escolhe a função de texto (`DocumentTexts.java:15,33,55`) e escreve o título à mão. Não existe criador, fábrica concentrada nem método polimórfico no tipo de hóspede: o `Guest` é um record de dados com dois testes de nulo (`Guest.java:20-26`). É o `if` repetido nos três lugares que as instruções descrevem como omissão.
Conclusão: omissão

**O exagero colateral.** Não há. `ReservationService`, `PriceCalculator`, `EmailSender`, `ReservationStore` e os relatórios são classes concretas, cada uma usada e com responsabilidade própria (`Hotel.java:20-26`). Não há interfaces de implementação única, fábricas nem builders, e a única camada a mais, `EmailSender` sobre `SentMessages`, acrescenta o canal "EMAIL" (`EmailSender.java:12-14`).
Conclusão: contenção

**O comportamento.** Testei numa cópia (`/tmp/av05`), com o `hotel.jar` na porta 7070:
- `GET /rooms` devolve os cinco quartos com as diárias certas.
- A criação com CPF, com CNPJ e razão social, e com passaporte e país dá 201. Os campos que não se aplicam vêm `null`.
- Nenhuma identificação, CPF junto com passaporte, CNPJ sem razão social, país sem passaporte e CPF com `companyName` dão todos 400 `INVALID_REQUEST`. Corpo que não é JSON também dá 400.
- O exemplo STANDARD de 10/07 a 12/07/2027 no quarto 102 dá 599,76.
- 3 hóspedes no quarto 101 dão 422 `CAPACITY_EXCEEDED`. Noites sobrepostas no 201 dão 409 `ROOM_UNAVAILABLE`. A segunda via de reserva pendente dá 409 `OPERATION_NOT_ALLOWED`. Reserva 99 dá 404.
- Na confirmação, `GET /messages` traz três e-mails em português, com o assunto "Reserva confirmada - Pousada Maré Alta":
  - o comprovante da Carla, idêntico ao do enunciado;
  - a fatura com "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)" e "ISS retido (5%): R$ 38,99" sobre 779,80;
  - o *booking receipt* com "1 night", `2027-03-10`, "Discount: BRL 37.48" e "Total: BRL 212.42".
- Uma suíte de 3 noites deu "BRL 1,739.70" e "3 nights".
- Nas três reservas, o `text` do check-out é igual ao da segunda via, e o e-mail contém o mesmo documento. O JSON tem os cinco campos e `text`, com `title` diferente por tipo.
- Um NON_REFUNDABLE confirmado e cancelado dá `refund` 0.00. Ele continua no financeiro (`total,1395.16` em julho).
- A ocupação de 2027-07-10 dá `total,1/5`, com `text/csv; charset=utf-8`. O mês `2027-7` dá 400.

O `mvn -q verify` passou, com 29 testes, 0 falhas e 0 erros. Depois derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe: nenhuma das formas aceitas pelas instruções foi usada (criador clássico, fábrica concentrada ou `guest.issueDocument(...)`), e a escolha está triplicada. Há um detalhe de comportamento fora das regras da parte do fim: o `title` do JSON muda por tipo de documento ("Fatura de hospedagem", "Booking receipt"). A parte comum fala só em "Comprovante de reserva", mas a parte do fim permite ler o título como próprio de cada documento. Não pesei isso contra o comportamento.
