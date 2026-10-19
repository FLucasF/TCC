# Cenário 05

## Visão geral

O agente juntou num lugar só a escolha de qual documento emitir: a `DocumentFactory`. Ela tem um único método privado, que olha o tipo de hóspede e devolve um dos três escritores (`PersonReceiptWriter`, `CompanyInvoiceWriter`, `ForeignReceiptWriter`). Os três lugares onde o documento sai (o e-mail de confirmação, o check-out e a segunda via) chamam todos `documents.issue(...)` e nenhum deles monta documento por conta própria. O comprovante da pessoa com CPF também passa pela fábrica. Para um quarto tipo, o da agência, basta um escritor novo e um ramo novo na fábrica. Não achei problema sério. O que fica de ressalva, sem afetar a classe: o tipo de hóspede não é um tipo próprio, e sim um record com sete campos e dois métodos booleanos. E o comprovante e a fatura repetem quase todo o texto um do outro. O comportamento testado confere com o enunciado.

## Como foi implementado, e por que o julgamento

**O projeto.** Os pacotes são estes:
- `hotel.rooms`: os cinco quartos, fixos no código (`rooms/Rooms.java:10-15`).
- `hotel.reservations`:
  - `Reservation`, a entidade, com o estado e o que foi pago e devolvido;
  - `NewReservation` e `Guest`, records de dados;
  - `PriceCalculator`, com a alta temporada, o desconto de 15% e o arredondamento HALF_EVEN a cada passo;
  - `ReservationService`, com o ciclo de vida da reserva;
  - `ReservationStore`, um `LinkedHashMap` na memória;
  - `ErrorCode` e `HotelException`, para os erros.
- `hotel.documents`: a fábrica, os escritores e o record `ReservationReceipt`, que é o JSON do documento.
- `hotel.messages`: `EmailSender` e `SentMessages`, a lista de mensagens enviadas.
- `hotel.reports`: os dois CSVs.
- `hotel.web`: `HttpApi`, com as rotas, a validação e o mapeamento de erro para status HTTP, e os records de entrada e saída.

`Hotel.java:19-28` monta tudo à mão, sem framework de injeção, e `Hotel.java:40-43` lê a variável `PORT`, com 7070 como padrão.

O caminho de uma requisição: a rota em `HttpApi` lê o corpo em `CreateReservationRequest`. `readNewReservation` (`web/HttpApi.java:109-140`) valida os campos e monta o `Guest`, depois chama o `ReservationService`, que aplica as regras e grava no `ReservationStore`. As `HotelException` viram `{error, message}` (`web/HttpApi.java:94-107`).

O agente escreveu 29 testes, em cinco classes:
- `PriceCalculatorTest`: 6;
- `ReservationServiceTest`: 11;
- `DocumentsByGuestTest`: 3, um por tipo de hóspede;
- `ReportsTest`: 5;
- `HttpApiTest`: 4, que sobem o servidor de verdade.

**A parte que pede o pattern.**
- `DocumentWriter` (`documents/DocumentWriter.java:7-12`) é a interface de um tipo de documento, com `title()` e `text(reservation, room)`.
- Cada documento tem a sua implementação:
  - o comprovante: `documents/PersonReceiptWriter.java:15-31`;
  - a fatura, com o ISS de 5% arredondado HALF_EVEN: `documents/CompanyInvoiceWriter.java:17-37`, com o ISS em `:33-35`;
  - o *booking receipt*, com `BRL` em `Locale.US` e datas ISO: `documents/ForeignReceiptWriter.java:17-38`.
- A escolha mora só em `DocumentFactory.writerFor` (`documents/DocumentFactory.java:19-27`). Se `guest.isCompany()`, sai a fatura; se `guest.isForeigner()`, o recibo em inglês; senão, o comprovante.
- `issue` (`documents/DocumentFactory.java:14-17`) embrulha o título e o texto em `ReservationReceipt`. Esse record traz os cinco campos de identificação (os que não se aplicam vêm null) e o `text` (`documents/ReservationReceipt.java:10-48`).
- Os três lugares chamam essa fábrica:
  - a confirmação, `reservations/ReservationService.java:70`, que põe `receipt.text()` dentro do e-mail em português (`:128-134`);
  - o check-out, `reservations/ReservationService.java:112`;
  - a segunda via, `reservations/ReservationService.java:121`.
- Procurei `isCompany`, `isForeigner` e `new ...Writer` no resto do código e eles só aparecem na fábrica. O `Guest` (`reservations/Guest.java:7-27`) é um record plano, e a validação "um jeito só" fica na borda HTTP (`web/HttpApi.java:126-134`).

**O raciocínio.** As instruções aceitam como acerto uma fábrica concentrada que escolhe o documento pelo tipo de hóspede, e é isso que existe aqui:
- a decisão está num `if` só (`documents/DocumentFactory.java:19-27`);
- os três lugares pedem o documento à fábrica;
- nenhum deles pergunta qual é o tipo nem cria o documento à mão;
- a fábrica devolve documentos diferentes, e não um documento único que alguém escolhe depois;
- o caso que já existia, o comprovante do CPF, também foi para a estrutura (`documents/PersonReceiptWriter.java`).

O que afasta um pouco o desenho do ideal não muda a classe. O tipo de hóspede é inferido de campos nulos (`reservations/Guest.java:20-26`), em vez de ser um tipo selado, então uma agência vai exigir mexer no `Guest`, na validação e na fábrica. E o texto do comprovante e o da fatura são quase cópias (`documents/PersonReceiptWriter.java:20-29` contra `documents/CompanyInvoiceWriter.java:23-32`). Isso é duplicação, não falta de pattern: a decisão de qual criar continua num lugar só.

No exagero colateral, nada pesou. A interface `DocumentWriter` tem três implementações e é a peça do pattern. `TextFormat` é um utilitário pequeno que dois escritores usam. Não há fábricas, builders ou camadas que só repassam fora dessa parte, e o `Hotel` monta tudo com `new`.

No comportamento, tudo o que testei bateu: valores, recusas, mensagens e relatórios.

## Item a item

**O pattern pedido.** Há uma fábrica concentrada. `DocumentFactory.writerFor` (`documents/DocumentFactory.java:19-27`) é o único lugar que decide entre fatura, *booking receipt* e comprovante. `issue` (`documents/DocumentFactory.java:14-17`) cria o `ReservationReceipt` com o título e o texto do escritor escolhido. A confirmação (`reservations/ReservationService.java:70`), o check-out (`reservations/ReservationService.java:112`) e a segunda via (`reservations/ReservationService.java:121`) só chamam `documents.issue(...)`. Não há `if` sobre o tipo de hóspede nem criação de documento à mão fora da fábrica, e o comprovante do CPF também passa por ela. É a forma de "fábrica concentrada" que as instruções aceitam como acerto, e uma agência entra como um escritor novo mais um ramo na fábrica.
Conclusão: acerto

**O exagero colateral.** Não há. A interface `DocumentWriter` (`documents/DocumentWriter.java:7`) tem três implementações e faz parte do pattern. `TextFormat` (`documents/TextFormat.java:9`) é usada por dois escritores. A montagem é direta (`Hotel.java:19-28`), sem interface de uma implementação só nem camada que só repassa. Vale anotar, sem ser exagero, a duplicação de texto entre o comprovante e a fatura (`documents/PersonReceiptWriter.java:20-29` e `documents/CompanyInvoiceWriter.java:23-32`).
Conclusão: contenção

**O comportamento.** Testei numa cópia do projeto, com `mvn -q package -DskipTests` e `PORT=7070 java -jar target/hotel.jar`, chamando a API com `curl`:
- **Identificação, aceitos:** criei reservas com CPF, com CNPJ e razão social e com passaporte e país. As três voltaram 201, com os cinco campos e null nos que não se aplicam.
- **Identificação, recusados:** nenhum identificador, CPF junto com passaporte, CPF junto com CNPJ, só o CNPJ, só a razão social, só o passaporte, só o país e `guestCpf` vazio. Todos voltaram 400 `INVALID_REQUEST`.
- **Fatura:** o e-mail da reserva 2 mostra "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", "Hóspede: Ana Souza" e "ISS retido (5%): R$ 38,99" sobre R$ 779,80. Com o plano NON_REFUNDABLE aparece a linha "Desconto" e o ISS sai R$ 25,49 sobre R$ 509,80.
- **Booking receipt:** "Booking receipt no. 3", "Guest: John Smith (passport X1234567, United Kingdom)", "Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights" e "BRL 1,159.80". Uma noite com desconto deu "1 night" e "Discount: BRL 44.98", e quatro noites deram "BRL 2,783.52".
- **Comprovante do CPF:** o e-mail sai igual ao do enunciado, com "Comprovante de reserva nº 1", "(CPF 111.222.333-44)", "2 diárias" e "R$ 779,80".
- **E-mail:** o assunto é "Reserva confirmada - Pousada Maré Alta" e o corpo é em português nos três tipos.
- **Os três lugares:** nos três tipos, o JSON do check-out é idêntico ao da segunda via, com `title`, os cinco campos e `text`, e o `text` está contido no corpo do e-mail.
- **Segunda via e operações:** a segunda via de reserva pendente ou cancelada dá 409 `OPERATION_NOT_ALLOWED`, e o check-out sem check-in também.
- **Parte comum:** o STANDARD de 10/07 a 12/07/2027 deu 599,76. Também conferi:
  - quarto 999: 404 `ROOM_NOT_FOUND`;
  - quarto ocupado: 409 `ROOM_UNAVAILABLE`;
  - três pessoas no 101: 422 `CAPACITY_EXCEEDED`;
  - corpo que não é JSON: 400;
  - mês `2027-7`: 400;
  - cancelamento NON_REFUNDABLE: refund 0,00;
  - relatório de ocupação: `text/csv; charset=utf-8`, com `total,1/5`;
  - relatório financeiro de 2027-07: conta as não reembolsáveis canceladas, total 3130.69.
- **Testes do agente:** o `mvn verify` da cópia passou, com 29 testes, 0 falhas e 0 erros. Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

A forma usada foi a fábrica concentrada: uma classe, `DocumentFactory`, com um método `writerFor` que escolhe por `if`, e escritores polimórficos atrás de uma interface. Não é o Factory Method clássico, com um criador sobrescrito por tipo, nem um método polimórfico no próprio hóspede. Fica uma dúvida pequena, que não muda a classe: o "tipo de hóspede" é deduzido de campos nulos num record plano (`reservations/Guest.java:20-26`), sem um tipo próprio. Por isso, quando a agência chegar, a mudança passa pelo `Guest`, pela validação HTTP e pela fábrica, e não fica só num ponto.
