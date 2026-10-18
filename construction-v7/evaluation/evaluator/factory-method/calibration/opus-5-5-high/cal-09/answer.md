# Cenário 01

## Visão geral

O agente resolveu o documento por tipo de hóspede com uma fábrica concentrada. A classe `DocumentFactory` é o único lugar que decide qual documento criar: comprovante, fatura ou *booking receipt*. Os três lugares onde o documento sai (e-mail de confirmação, check-out e segunda via) pedem o documento a ela pelo mesmo método `issue`. Cada documento tem sua própria classe, e todas implementam a mesma interface. O resto do sistema é enxuto e quase não tem estrutura sobrando. Tudo o que testei pela API confere com o enunciado: as recusas da identificação, o ISS de R$ 38,99, o formato `BRL 1,234.56`, "1 night", o documento igual nos três lugares, o campo `text`, os relatórios e os erros. Não há problema sério. O ponto fraco é o que custa acrescentar a agência de turismo: o hóspede é um record com sete campos que podem ser nulos, e a escolha usa os booleanos `isCompany`/`isForeigner`. A agência vai exigir um campo novo em `Guest`, `ReservationReceipt`, `ReservationResponse` e no pedido, além de um ramo novo na fábrica. Mesmo assim, a escolha continua num lugar só.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está no pacote `hotel`, dividido assim:
- `rooms`: `Rooms` tem os cinco quartos fixos no código (`src/main/java/hotel/rooms/Rooms.java:10-15`), e `Room` e `RoomType` descrevem cada um.
- `reservations`: `ReservationService` cuida do ciclo de vida da reserva. `PriceCalculator` calcula o preço com arredondamento HALF_EVEN a cada passo (`src/main/java/hotel/reservations/PriceCalculator.java:50-52`). `ReservationStore` guarda as reservas num `LinkedHashMap` em memória. `Reservation` é uma entidade com estado, e `Guest`, `NewReservation` e `Price` são records. `HotelException` carrega o `ErrorCode`.
- `documents`: a fábrica e os três documentos.
- `messages`: `EmailSender` e `SentMessages`, a lista de mensagens enviadas.
- `reports`: os dois CSV.
- `web`: `HttpApi`, com rotas, leitura do corpo, validação e tradução de `ErrorCode` para status HTTP (`src/main/java/hotel/web/HttpApi.java:100-107`), mais os DTOs de entrada e saída.

`Hotel.java` monta tudo à mão no construtor (`src/main/java/hotel/Hotel.java:19-28`) e sobe o servidor na porta de `PORT` ou na 7070 (`Hotel.java:41`). O jar é gerado pelo shade com `hotel.Hotel` no manifesto.

O caminho de uma requisição começa na rota de `HttpApi`. Ela lê e valida o corpo (`HttpApi.java:109-140`) e monta o `Guest` e o `NewReservation`. Depois chama `ReservationService`, que aplica a regra, usa o `PriceCalculator` e grava no `ReservationStore`. A resposta volta como `ReservationResponse` ou `ReservationReceipt`. O agente escreveu 29 testes em cinco classes: preço, serviço, relatórios, documentos por hóspede e API por HTTP.

**A parte do fim.**
- **O hóspede.** É um record só, `Guest`, com os cinco campos de identificação; os que não se aplicam ficam nulos. Ele tem dois predicados, `isCompany()` e `isForeigner()` (`src/main/java/hotel/reservations/Guest.java:7-26`).
- **A validação.** Fica em `HttpApi.java:126-134`. Ela conta os jeitos de identificação, exige exatamente um e recusa CNPJ sem razão social, ou passaporte sem país.
- **Os documentos.** A interface `DocumentWriter` (`src/main/java/hotel/documents/DocumentWriter.java:7-12`) tem `title()` e `text(reservation, room)`. Cada documento é uma implementação dela:
  - `PersonReceiptWriter`: o comprovante (`PersonReceiptWriter.java:7-31`);
  - `CompanyInvoiceWriter`: a fatura, com o ISS de 5% calculado em HALF_EVEN (`CompanyInvoiceWriter.java:33-35`);
  - `ForeignReceiptWriter`: o *booking receipt*, com `brl()` em `Locale.US` e datas ISO (`ForeignReceiptWriter.java:25-27`, `:36-38`).
- **A escolha.** O único `if` sobre o tipo de hóspede está em `DocumentFactory.writerFor` (`src/main/java/hotel/documents/DocumentFactory.java:19-27`). `DocumentFactory.issue` (`DocumentFactory.java:14-17`) monta o `ReservationReceipt` com o título, os cinco campos e o `text` (`ReservationReceipt.java:10-48`).
- **Os três lugares.** Todos chamam `documents.issue(...)`: a confirmação, cujo texto entra no e-mail (`ReservationService.java:70-74`), o check-out (`ReservationService.java:112`) e a segunda via (`ReservationService.java:121`). Nenhum deles cria o documento por conta própria ou pergunta o tipo do hóspede; o `grep` por `isCompany`/`isForeigner` só encontra a fábrica. O e-mail continua em português e só embute `receipt.text()` (`ReservationService.java:128-134`).

**O raciocínio.** O que o enunciado precisava era concentrar a decisão de qual documento criar e fazer os três lugares pedirem o documento a esse ponto. É exatamente o que existe aqui. A fábrica concentrada é uma das formas que as instruções do pattern aceitam como acerto, e a variação de texto fica em classes polimórficas que ela escolhe. Não há nada de malfeito: nenhum lugar cria o documento à mão, e a fábrica não devolve um documento genérico para alguém decidir depois. A agência entra com uma classe nova e um ramo novo em `writerFor`. O que afasta o desenho do ideal é o hóspede modelado por campos nulos e booleanos em vez de um tipo por jeito de identificação, e o texto do comprovante duplicado quase linha a linha na fatura (`PersonReceiptWriter.java:17-29` contra `CompanyInvoiceWriter.java:19-32`). Isso é custo de manutenção, não espalha a decisão, e por isso não muda a classe.

No exagero colateral, fora da parte do fim não há interfaces nem fábricas. O único ponto discutível é `EmailSender`, uma classe concreta que só repassa para `SentMessages.add("EMAIL", ...)` (`src/main/java/hotel/messages/EmailSender.java:12-14`). Ela é mínima e representa o canal que o enunciado diz que vai sair de verdade um dia, então não contei como exagero. A interface `DocumentWriter` fica dentro da parte do fim e tem três implementações. No comportamento, tudo o que testei conferiu.

## Item a item

**O enunciado (pede o pattern).** A escolha do documento mora só em `DocumentFactory.writerFor` (`src/main/java/hotel/documents/DocumentFactory.java:19-27`), e cada documento é uma implementação de `DocumentWriter` (`CompanyInvoiceWriter.java:9`, `ForeignReceiptWriter.java:9`, `PersonReceiptWriter.java:7`). A confirmação com o e-mail, o check-out e a segunda via chamam o mesmo `documents.issue(...)` (`src/main/java/hotel/reservations/ReservationService.java:70`, `:112`, `:121`). Fora da fábrica, nenhum código pergunta o tipo do hóspede para decidir o documento. Isso é a fábrica concentrada que as instruções aceitam como Factory Method. O comprovante da pessoa com CPF também passa pela estrutura, não só os casos novos.
Conclusão: acerto

**O exagero colateral.** Não há. Fora dos documentos, as classes são concretas e cada uma tem uma função clara: `Rooms`, `ReservationStore`, `PriceCalculator`, os relatórios e `HttpApi`. A montagem é feita à mão em `Hotel.java:19-28`, sem fábricas nem builders. O caso mais próximo de uma camada que só repassa é `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:12-14`), mas ele é pequeno e concreto, e representa o canal de e-mail descrito no enunciado.
Conclusão: contenção

**O comportamento.** Testei numa cópia em `/tmp/copia`, com o jar `target/hotel.jar` na porta 7070:
- **Identificação.** As cinco recusas voltaram 400 INVALID_REQUEST: nenhuma identificação, CPF junto com passaporte, CNPJ sem razão social, passaporte sem país e país sem passaporte. A criação com CPF voltou 201, com os cinco campos e `null` nos que não se aplicam.
- **Fatura.** Para a empresa, 779,80 deu "ISS retido (5%): R$ 38,99", com "Empresa: Tecnosul Ltda (CNPJ ...)" e "Hóspede: Ana Souza". Com desconto, saiu "Desconto: R$ 140,36", "Total: R$ 795,40" e ISS de R$ 39,77, todos corretos.
- ***Booking receipt*.** Saíram "Booking receipt no. 3", "Guest: John Smith (passport X1234567, United Kingdom)", "2027-03-10 | Check-out: 2027-03-12 | 2 nights" e "BRL 499.80". Com uma noite, saiu "1 night", com "Discount: BRL 104.38" e "Total: BRL 591.50". Com três noites, "BRL 2,087.64".
- **Os três lugares.** Para as reservas 1, 2 e 3, o JSON do check-out é idêntico ao da segunda via, e o campo `text` aparece igual dentro do e-mail. O e-mail continua em português e o título acompanha o tipo do documento.
- **Comprovante da parte comum.** A pessoa com CPF recebeu o comprovante exatamente como no enunciado. Também conferem "1 diária", a linha de desconto e "R$ 1.499,40".
- **Regras comuns.** O STANDARD de 10 a 12/07/2027 deu 599.76. Também conferiram 409 ROOM_UNAVAILABLE, 422 CAPACITY_EXCEEDED, 404 ROOM_NOT_FOUND e RESERVATION_NOT_FOUND, 400 para corpo que não é JSON e para o mês 2027-13, e 409 para check-in e segunda via de reserva pendente. O cancelamento NON_REFUNDABLE deu refund 0.00.
- **Relatórios.** A ocupação veio em `text/csv; charset=utf-8` com `total,2/5`, excluindo a cancelada e a pendente. O financeiro veio em ordem de entrada e número, com a não reembolsável cancelada incluída e o total 4749.03.
- **Testes do agente.** O `mvn verify` passou: 29 testes, 0 falhas, BUILD SUCCESS. O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

A forma usada foi a **fábrica concentrada**: uma classe que escolhe o documento pelo tipo de hóspede e delega o texto a implementações de `DocumentWriter`. Não é a forma clássica, com criador sobrescrito, nem o método polimórfico no próprio hóspede. A escolha se apoia em booleanos de um `Guest` com campos nulos (`Guest.java:20-26`), e não num tipo por jeito de identificação. Isso não espalha a decisão, mas deixa a entrada da agência mais trabalhosa do que precisaria. Considerei e descartei contar o `EmailSender` como camada que só repassa.
