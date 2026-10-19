# Cenário 05

## Visão geral

O agente juntou num só lugar a decisão de qual documento criar. Criou a `DocumentFactory`, e o método `writerFor` dela escolhe entre três redatores pelo tipo de hóspede (`final/src/main/java/hotel/documents/DocumentFactory.java:19-27`). Os três lugares que antes faziam `new ReservationReceipt(...)` agora chamam `documents.issue(...)`: a confirmação por e-mail, o check-out e a segunda via (`final/src/main/java/hotel/reservations/ReservationService.java:70`, `:112`, `:121`). O comprovante da pessoa com CPF também passou para essa estrutura (`PersonReceiptWriter`), então não restou nenhum caminho fora dela. Nos testes pela API, todas as regras pedidas saíram certas. Não achei problema sério. O único reparo é pequeno: o "documento" continua sendo um único record, e o que muda entre os tipos é o título e o texto que o redator escolhido devolve. Mas a escolha acontece dentro da fábrica, e por isso o desenho continua sendo acerto.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin montado à mão em `Hotel.java`, que cria `Rooms`, `ReservationStore`, `PriceCalculator`, `EmailSender` e `SentMessages`, passa tudo ao `ReservationService` e ao `HttpApi` e sobe na porta de `PORT` (`final/src/main/java/hotel/Hotel.java:20-26`). Os dados ficam em memória. Os pacotes:
- `web`: rotas, DTOs de entrada e saída, validação do corpo;
- `reservations`: as regras e o modelo (`Reservation`, `Guest`, `NewReservation`, preço, estados);
- `documents`: o comprovante e a formatação;
- `messages`: os e-mails guardados;
- `reports`: os CSVs.

Uma requisição percorre este caminho: `HttpApi` valida o corpo e monta o `Guest` e o `NewReservation` (`final/src/main/java/hotel/web/HttpApi.java:117-139`), depois chama o `ReservationService`, que usa o `ReservationStore` e o `PriceCalculator` e, nos três pontos de emissão, pede o documento à `DocumentFactory`. O `ReservationService` cria a fábrica ele mesmo, num campo (`ReservationService.java:19`), sem passar pelo `Hotel`.

**A parte nova.** O `Guest` ganhou os campos `companyCnpj`, `companyName`, `passport` e `country`, e dois métodos que dizem qual é o tipo: `isCompany()` e `isForeigner()` (`final/src/main/java/hotel/reservations/Guest.java:20-26`). O construtor de três argumentos continua existindo, para o caso com CPF (`Guest.java:16`). O `HttpApi` conta quantas formas de identificação vieram e recusa nenhuma, mais de uma ou uma incompleta (`HttpApi.java:126-134`). Ali se decide se a reserva é válida, não qual documento sai.

Em `documents` há uma interface de pacote, `DocumentWriter`, com `title()` e `text(reservation, room)`, e três implementações:
- `PersonReceiptWriter`: o texto da base, transferido de `ReservationReceipt.text()`;
- `CompanyInvoiceWriter`: a linha da empresa e o ISS de 5% com `HALF_EVEN` (`CompanyInvoiceWriter.java:33-35`);
- `ForeignReceiptWriter`: inglês, `Locale.US` com `%,.2f` e as datas em ISO (`ForeignReceiptWriter.java:24-37`).

A `DocumentFactory.issue` escolhe o redator e devolve um `ReservationReceipt` com o título, o texto e os campos de identificação (`DocumentFactory.java:14-17`). O `ReservationReceipt` virou um record que só guarda dados e ganhou os quatro campos novos e o campo `text` (`ReservationReceipt.java:10-27`). O e-mail usa `receipt.text()` (`ReservationService.java:131`).

**Os testes do agente.** São três, em `DocumentsByGuestTest`: a fatura com a empresa e o ISS de R$ 38,99, o *booking receipt* com "2 nights" e "BRL 779.80", e a pessoa com CPF recebendo o "Comprovante de reserva". Os testes que já existiam não foram alterados.

**O raciocínio.** As instruções do pattern pedem que a escolha do documento more num lugar só e que os três lugares peçam o documento a esse lugar. É o que o código faz. O único teste sobre o tipo de hóspede que escolhe documento está em `DocumentFactory.java:20-26`, e nenhum dos três pontos do `ReservationService` faz `new` do comprovante nem pergunta qual é o tipo de hóspede. O caso que já existia, a pessoa com CPF, também passa pela fábrica.

Seria malfeito se a fábrica devolvesse sempre o mesmo documento e a escolha acontecesse depois, fora dela. Aqui o tipo Java devolvido é sempre `ReservationReceipt`, mas ele já sai da fábrica com o título e o texto do tipo certo, e ninguém fora dela decide nada. A agência de turismo pediria um redator novo e um ramo novo em `writerFor`, sem mexer nos três pontos de emissão.

A interface `DocumentWriter` e os três redatores fazem parte da variação que o pattern resolve, então não contam como exagero. Fora dela, as mudanças são as que o pedido exige: campos no `Guest`, no DTO de entrada e nas respostas, e a validação. No comportamento, tudo o que testei pela API conferiu.

## Item a item

**O pattern pedido.** É uma fábrica concentrada: `DocumentFactory.issue` (`final/src/main/java/hotel/documents/DocumentFactory.java:14-17`), com a escolha por tipo de hóspede em `writerFor` (`DocumentFactory.java:19-27`). Os três pontos de emissão usam essa fábrica: a confirmação por e-mail em `final/src/main/java/hotel/reservations/ReservationService.java:70`, o check-out em `:112` e a segunda via em `:121`. Nenhum `new ReservationReceipt` sobrou fora da fábrica. O único que existe está em `DocumentFactory.java:16`. O comprovante com CPF, que já existia, também virou um caso da estrutura (`final/src/main/java/hotel/documents/PersonReceiptWriter.java:15-31`). É o que o texto precisava: um lugar só que decide qual documento criar, pronto para receber o documento da agência.
Conclusão: acerto

**O exagero colateral.** Não há. A interface `DocumentWriter` tem três implementações e é a própria variação. `Guest.isCompany()` e `Guest.isForeigner()` são métodos simples que dizem o tipo. O agente não criou builder, nem fábrica extra, nem camada que só repassa. A `DocumentFactory` é criada direto no `ReservationService` (`ReservationService.java:19`), sem injeção nem interface própria.
Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/`, com o `hotel.jar` na porta 7070, e chamei a API com `curl`:
- **Criação:** CPF, CNPJ com razão social e passaporte com país dão 201, e a reserva mostra os campos que não se aplicam como `null`. Nenhum documento, CPF com passaporte, CNPJ sem razão social, passaporte sem país e CPF com só `companyName` dão 400 `INVALID_REQUEST`.
- **Fatura:** a da reserva no quarto 202 (10 a 12/03/2027) saiu igual ao exemplo do pedido, com "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)" e "ISS retido (5%): R$ 38,99". Com desconto, entra "Desconto: R$ 134,95" e o ISS fica R$ 38,23 sobre R$ 764,69.
- ***Booking receipt*:** "Check-in: 2027-04-10 | Check-out: 2027-04-12 | 2 nights" e "Total: BRL 779.80". Com uma noite e desconto, sai "1 night" e "Discount: BRL 104.38". Acima de mil, sai "BRL 1,739.70".
- **Os três pontos de emissão:** o e-mail de confirmação (via `/messages`) traz o documento certo dentro do corpo em português, e o texto do check-out é idêntico ao da segunda via, nas reservas 2 e 3.
- **JSON do documento:** traz os quatro campos novos, com `null` nos que não se aplicam, e o campo `text`.
- **Comparação com a base:** a pessoa com CPF recebe o mesmo texto da base, no documento e no e-mail.
- **O que o pedido não mudou:** a sobreposição ainda dá 409 `ROOM_UNAVAILABLE`, uma reserva inexistente dá 404 e os relatórios de receita e de ocupação saem como antes.
- **`mvn -q verify`:** passou, com 29 testes e 0 falhas. Na base eram 26, e os 3 novos do agente (`DocumentsByGuestTest`) passam.

O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

Sobre a forma usada: é uma fábrica concentrada (um método `issue` que escolhe por `if`), não a forma clássica de criador com subclasses nem um método polimórfico no `Guest`. Cada tipo de documento é um objeto redator, e o produto é sempre o mesmo record `ReservationReceipt`, diferenciado pelo título e pelo texto. Pesei se isso seria "a fábrica devolve sempre o mesmo documento e a escolha acontece depois". Concluí que não, porque o conteúdo específico já sai pronto da fábrica e ninguém fora dela escolhe nada.
