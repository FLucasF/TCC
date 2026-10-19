# Cenário 01

## Visão geral

O agente concentrou num só lugar a escolha de qual documento emitir. O método `determineDocumentGenerator` do `ReservationService` olha quais campos de identificação vieram, recusa nenhum ou mais de um jeito e devolve um de três geradores (`CpfDocumentGenerator`, `CnpjDocumentGenerator`, `PassportDocumentGenerator`), todos com a interface `DocumentGenerator`. O gerador escolhido fica guardado na reserva. O e-mail de confirmação, o check-out e a segunda via pedem o documento à reserva e nenhum deles volta a perguntar o tipo de hóspede. É a fábrica concentrada que as instruções aceitam, e o resto do sistema não tem estrutura sobrando. O problema mais sério é de comportamento: o comprovante e a fatura formatam o dinheiro com `NumberFormat` pt-BR, que põe um espaço não separável (U+00A0) entre "R$" e o valor. Por isso, o texto desses dois documentos, e do e-mail que os traz, não é igual byte a byte ao do enunciado. Os valores, as recusas, o ISS e o *booking receipt* conferem.

## Como foi implementado, e por que o julgamento

**O projeto.** São três pacotes. O `api` tem os controllers Javalin (`RoomController`, `ReservationController`, `MessageController`, `ReportController`), o DTO `CreateReservationRequest` e o `ApiMapper`, que é estático e monta os `Map` de resposta e faz o parse das datas e do mês. O `service` tem o `ReservationService` (criação, transições, disponibilidade e relatórios em CSV), o `RoomService` (os cinco quartos fixos numa lista), o `MessageService` (a lista de e-mails "enviados" e o texto do e-mail) e as exceções de negócio. O `domain` tem a `Reservation`, que guarda o estado e calcula noites, subtotal, desconto e total (`domain/Reservation.java:123-188`), além de `Room`, `RoomType`, `RatePlan`, `ReservationStatus`, `Message`, `DocumentData` e os geradores de documento.

Uma requisição percorre este caminho: rota em `App.java:36-49`, depois o método do controller, que valida os campos obrigatórios e as datas (`api/ReservationController.java:18-36`), depois o `ReservationService`, que busca o quarto, checa capacidade e disponibilidade e guarda a reserva numa `ArrayList` em memória (`service/ReservationService.java:15,25-57`). Os erros viram JSON `{error, message}` nos handlers de exceção em `App.java:54-97`. O `App.main` monta tudo à mão e lê a porta de `PORT`, com 7070 como padrão (`App.java:99-109`).

O agente escreveu 13 testes de integração com RestAssured (`DocumentIT`, `ReportsIT`, `ReservationIT`). Eles sobem o `App.main` numa thread e chamam a API.

**A parte do fim (documento por tipo de hóspede).** A interface `DocumentGenerator` (`domain/DocumentGenerator.java:3-6`) tem dois métodos, `generateDocument(Reservation)` e `validate(Reservation)`. Cada tipo de hóspede é uma classe:

- `CpfDocumentGenerator` monta o comprovante (`domain/CpfDocumentGenerator.java:21-61`).
- `CnpjDocumentGenerator` monta a fatura e calcula o ISS de 5% com `HALF_EVEN` (`domain/CnpjDocumentGenerator.java:26-71`, ISS na linha 67).
- `PassportDocumentGenerator` monta o *booking receipt*, com `BRL #,##0.00`, datas ISO e "night"/"nights" (`domain/PassportDocumentGenerator.java:13-18,53-72`).

A escolha mora em `service/ReservationService.java:59-87`. Ali uma sequência de `if` conta quantos jeitos de identificação vieram e cria o gerador correspondente, e lança `INVALID_REQUEST` se não for exatamente um. A checagem de dado incompleto (CNPJ sem razão social, passaporte sem país) fica no `validate` de cada gerador (`service/ReservationService.java:48`). O gerador vai para o construtor da `Reservation` e fica num campo (`domain/Reservation.java:24,48`), e `Reservation.generateDocument()` só repassa a chamada a ele (`domain/Reservation.java:196-198`). Os três lugares que emitem o documento chamam esse método:

- a confirmação: `service/ReservationService.java:117-118`, e o `MessageService` só embute `document.getText()` no corpo português (`service/MessageService.java:24-31`);
- o check-out: `api/ReservationController.java:84`;
- a segunda via: `service/ReservationService.java:167`.

O JSON do documento sai de um `DocumentData` comum, com os cinco campos de identificação e o `text` (`api/ApiMapper.java:51-71`).

**O raciocínio do julgamento.** O pattern pede que a decisão de qual documento criar fique num lugar só e que os lugares que emitem o documento o peçam a esse lugar. Aqui a decisão acontece uma vez, na criação, e os três consumidores dependem só da abstração. Para incluir a agência basta uma classe nova e um ramo novo no método de fábrica, sem tocar em confirmação, check-out ou segunda via. Nenhum consumidor faz `instanceof` nem pergunta o tipo. Dois pontos afastam o desenho do ideal sem mudar a classe:

- O tipo de hóspede não existe como conceito. A reserva carrega cinco campos soltos e a fábrica decide pela presença deles.
- A validação dos campos está misturada ao gerador do documento, e os três geradores repetem o preenchimento do `DocumentData`.

Fora da parte do fim não há interface de implementação única, fábrica, builder nem camada que só repasse. Os services têm regra ou estado próprio. No comportamento pesou o texto em português: "R$" seguido de U+00A0 em vez do espaço comum, nos dois documentos em português e no e-mail. Pesaram também, em menor grau, o `discount` null no plano STANDARD e as datas do JSON do comprovante e da fatura em `dd/MM/yyyy`.

## Item a item

**O enunciado (pede o pattern).** A fábrica é concentrada: `determineDocumentGenerator` em `service/ReservationService.java:59-87` escolhe entre `CpfDocumentGenerator`, `CnpjDocumentGenerator` e `PassportDocumentGenerator`, que implementam `DocumentGenerator` (`domain/DocumentGenerator.java:3-6`). O resultado fica na reserva (`domain/Reservation.java:24,196-198`). Os três lugares passam por ele: confirmação/e-mail em `service/ReservationService.java:117`, check-out em `api/ReservationController.java:84`, segunda via em `service/ReservationService.java:167`. Todos os casos, inclusive o CPF da parte comum, foram para a estrutura, e nenhum lugar cria documento à mão nem pergunta o tipo concreto. É o pattern que o enunciado precisava.
Conclusão: acerto

**O exagero colateral.** Não há. `RoomService`, `MessageService` e `ReservationService` são classes concretas com estado ou regra própria (`service/RoomService.java:13-20`, `service/MessageService.java:15-31`). O `ApiMapper` é um conjunto de funções estáticas de conversão (`api/ApiMapper.java:17-105`). A única interface do projeto é a `DocumentGenerator`, que está na parte do fim e tem três implementações. As sete classes de exceção correspondem um a um aos códigos de erro do enunciado (`App.java:55-81`).
Conclusão: contenção

**O comportamento.** Testei numa cópia, com o serviço em `PORT=7070`.

O que confere:

- **Recusas.** A criação recusa com 400 `INVALID_REQUEST` estes casos: nenhum jeito de identificação, CPF + passaporte, CPF + CNPJ, CPF vazio, CNPJ sem razão social ("Company name is required"), razão social sem CNPJ, passaporte sem país e país sem passaporte.
- **Reservas em março (2 noites).** A de CPF no 201 dá 779.80. A de CNPJ dá 779.80, com `companyCnpj`/`companyName` e os outros campos null.
- **Fatura.** No e-mail, no check-out e na segunda via sai "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", "Hóspede: Ana Souza" e "ISS retido (5%): R$ 38,99".
- **Booking receipt.** Para a SUITE em março: "Nights: BRL 1,159.80", "Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights". Para uma noite NON_REFUNDABLE em julho: "1 night", "Nights: BRL 299.88", "Discount: BRL 44.98", "Total: BRL 254.90".
- **Mesmo documento nos três lugares.** Para os quatro casos, o JSON do check-out é idêntico ao da segunda via, o `text` aparece literalmente dentro do corpo do e-mail e o e-mail continua em português.
- **Parte comum.** STANDARD de 10/07 a 12/07/2027 dá subtotal e total 599.76. Quarto ocupado dá 409 `ROOM_UNAVAILABLE`; capacidade excedida, 422 `CAPACITY_EXCEEDED`; JSON inválido, 400; check-in de reserva pendente, 409 `OPERATION_NOT_ALLOWED`; segunda via de reserva pendente, 409; quarto 999, 404 `ROOM_NOT_FOUND`; reserva 99, 404 `RESERVATION_NOT_FOUND`. Cancelar uma STANDARD confirmada devolve `refund` 599.76.
- **Relatórios.** Os CSV vêm com `text/csv; charset=utf-8`, cabeçalho e "total,1/5" ou "total,2719.40", e mês mal formado dá 400.

O que não confere:

- **O espaço depois de "R$".** O `od -c` do texto mostra `R $ 302 240 7 7 9 , 8 0`: depois de "R$" vem um espaço não separável (U+00A0), não um espaço comum. Isso vale para o comprovante, a fatura e o e-mail, porque `NumberFormat.getCurrencyInstance(pt-BR)` está em `domain/CpfDocumentGenerator.java:10` e `domain/CnpjDocumentGenerator.java:11`. Os testes do agente não pegam isso porque só procuram "499,80".
- **Desvios menores.** `discount` vem null no plano STANDARD, tanto na reserva quanto no documento (`domain/Reservation.java:176`). `amountPaid` vem como `0`, não `0.00`. Os campos `checkIn`/`checkOut` do JSON do comprovante e da fatura vêm como "10/03/2027", não em ISO (`domain/CpfDocumentGenerator.java:33-34`).

`mvn verify` na cópia: 13 testes, 0 falhas, BUILD SUCCESS.
Conclusão: em parte

## Dúvidas

- **Forma do pattern.** É a fábrica concentrada: um método privado com uma sequência de `if` em `service/ReservationService.java:59-87`. O resultado é guardado na reserva como estratégia, e o README chama isso de "Strategy". A escolha é feita pelos campos presentes, porque não existe um tipo de hóspede explícito. Não vi nisso motivo para baixar a classe, já que a decisão está num lugar só.
- **Peso do comportamento.** Fiquei entre `segue` e `em parte`. Os valores, as recusas e as regras do fim conferem. Mas o texto em português, que o enunciado mostra literalmente e que vai no e-mail, difere no caractere entre "R$" e o número. Isso quebraria uma comparação exata, então fiquei com `em parte`.
- **Erro nos testes do agente.** Durante o `mvn verify` aparece "Failed to start Javalin": cada classe de teste tenta subir o `App.main` na mesma porta. Os testes passam mesmo assim porque reaproveitam o servidor já de pé.
