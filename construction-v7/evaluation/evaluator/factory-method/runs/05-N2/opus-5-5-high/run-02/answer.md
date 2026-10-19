# Cenário 01

## Visão geral

O agente concentrou a escolha do documento numa fábrica só. `DocumentService.selectGenerator` decide, a partir dos campos de identificação da reserva, qual de três geradores usar, e cada gerador implementa a interface `DocumentGenerator`. Os três lugares onde o documento sai (e-mail de confirmação, check-out e segunda via) pedem o documento a essa fábrica, e nenhum deles o monta à mão. Isso é o Factory Method na forma de fábrica concentrada, e o documento das agências vai caber com uma classe nova e um ramo novo. O desenho está certo. Os problemas estão no comportamento:

- O JSON da reserva e o do documento omitem os campos nulos, quando o enunciado pede que venham `null`.
- A alta temporada conta dezembro inteiro, e não a partir do dia 15.
- A segunda via sai para uma reserva cancelada.
- O relatório financeiro lista reservas que não deixaram dinheiro.
- A validação e a fábrica discordam sobre o que é um CPF "informado". Um `guestCpf: ""` junto com um CNPJ válido gera um comprovante com "CPF " vazio.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está todo em `com.pousadamareaalta`:

- `App` (`src/main/java/com/pousadamareaalta/App.java:8-46`) cria tudo à mão, sem framework. São dois repositórios em memória (`RoomRepository`, com os cinco quartos fixos no construtor, e `ReservationRepository`, um `HashMap` com contador de id), `MessageService` (lista de mensagens enviadas), `DocumentService`, `ReservationService` (as regras) e `ReportService` (os CSVs). Os handlers ficam em `api/ApiHandlers` e as rotas são registradas no Javalin. A porta vem de `PORT`, com 7070 como padrão (`App.java:48-51`).
- O caminho de uma requisição: a rota chama um método de `ApiHandlers`, que lê o corpo como `CreateReservationRequest`, valida os campos obrigatórios e chama `ReservationService`. O serviço valida, calcula preço e desconto e grava no repositório. Na volta, o resultado é convertido em `ReservationResponse` ou `DocumentResponse`.
- Os erros de regra são uma `ValidationException` com código, e cada handler traduz o código para o status HTTP com um `switch` (por exemplo `ApiHandlers.java:79-85`).
- O `maven-shade-plugin` gera um jar executável.
- Os testes (39) são de unidade: serviços e utilitários, sem nenhum teste HTTP.

**A parte do fim.**
- O contrato é `document/DocumentGenerator.java:7-9`: `Document generate(Reservation, Room)`.
- Há três implementações:
  - `CpfComprovante` (`document/CpfComprovante.java:12-58`): o comprovante;
  - `CnpjFatura` (`document/CnpjFatura.java:11-62`): a fatura, com o ISS de 5% calculado em `CnpjFatura.java:23`;
  - `PassportBookingReceipt` (`document/PassportBookingReceipt.java:10-57`): o recibo em inglês, com `BRL %,.2f` em `Locale.US` (`:64`) e "1 night"/"N nights" (`:60`).
- Cada gerador devolve um `Document` (`model/Document.java:5-23`) com os cinco campos de identificação, os que não se aplicam com null, e o campo `text`.
- A escolha mora num lugar só, `service/DocumentService.java:14-23`: um `if/else` sobre `guestCpf`, `companyCnpj` e `guestPassport`, que devolve o gerador.
- Os três lugares chamam `documentService.generateDocument(...)`:
  - na confirmação, `ReservationService.java:124`, com o texto colocado no e-mail em `:126`;
  - no check-out, `ReservationService.java:158`;
  - na segunda via, `ReservationService.java:196`.
- A regra de "exatamente um jeito" de se identificar fica em `ReservationService.java:59-70` e `:228-233`.

**O raciocínio.**
- O que aproxima do pattern: a decisão de qual documento criar acontece uma vez, num ponto só, e quem usa recebe um `DocumentGenerator`/`Document` sem perguntar o tipo. O e-mail usa `document.text()` sem saber qual documento é.
- O que afasta, e não chega a mudar a classe: a "classificação" do hóspede está implícita em campos nulos e é feita duas vezes com critérios diferentes. A validação trata string vazia como ausente (`ReservationService.java:230-232`), e a fábrica usa só `!= null` (`DocumentService.java:15-20`). Isso não cria um segundo ponto de decisão sobre qual documento criar, mas causa um defeito de comportamento.
- O exagero colateral: fora dessa parte não há interfaces, fábricas ou camadas de repasse. Repositórios e serviços são classes concretas.
- O comportamento: as regras novas de texto (fatura, ISS, recibo em inglês, documento igual nos três lugares) conferem. Pesaram contra a omissão dos nulos no JSON (`api/DocumentResponse.java:9`, `api/ReservationResponse.java:9`) e os defeitos da parte comum.

## Item a item

**O enunciado (pede o pattern).** A escolha do documento está concentrada em `DocumentService.selectGenerator` (`src/main/java/com/pousadamareaalta/service/DocumentService.java:14-23`), que devolve uma das três implementações de `DocumentGenerator` (`document/DocumentGenerator.java:7-9`): `CpfComprovante`, `CnpjFatura` e `PassportBookingReceipt`. Confirmação (`service/ReservationService.java:124`), check-out (`:158`) e segunda via (`:196`) passam todos por `documentService.generateDocument`. Não há `if` sobre o tipo de hóspede em nenhum desses lugares, e nenhum monta o texto à mão: o e-mail só embrulha `document.text()` (`:126`, `:220-226`). Um documento de agência pede uma classe nova e um ramo em `selectGenerator`, sem tocar nos três lugares. É a fábrica concentrada que as instruções aceitam como acerto.
Conclusão: acerto

**O exagero colateral.** Não há. `RoomRepository`, `ReservationRepository`, `MessageService` e `ReportService` são classes concretas, sem interface nem fábrica (`App.java:11-28`). A única interface do projeto, `DocumentGenerator`, pertence à parte do fim e tem três implementações. Há duplicação, mas não há estrutura a mais: `DateUtils.formatDateEnglish` repete `formatDateIso` (`util/DateUtils.java:26-32`), e cada gerador repete `getNightsLabel`/`formatMoney`.
Conclusão: contenção

**O comportamento.** Testei uma cópia (`mvn -q package -DskipTests`, `java -jar target/reservas-1.0.0.jar` na 7070).

*O que confere:*
- A identificação:
  - com CPF, com CNPJ + razão social e com passaporte + país, o retorno é 201;
  - sem nenhum, com CPF + CNPJ, com CNPJ sem razão social, com passaporte sem país e só com país, o retorno é 400 `INVALID_REQUEST`.
- O e-mail de confirmação tem o assunto certo e o texto em português, com o documento certo dentro:
  - para a empresa: "Fatura de hospedagem nº 2 / Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90) / Hóspede: Ana Souza / … / Total: R$ 779,80 / ISS retido (5%): R$ 38,99";
  - para o estrangeiro: "Booking receipt no. 3 / Guest: John Smith (passport X1234567, United Kingdom) / … / 2 nights / Nights: BRL 1,159.80".
- Uma reserva estrangeira de 1 noite no plano não reembolsável sai com "1 night" e "Discount: BRL 70.18" antes do total.
- O `text` do check-out e o da segunda via são idênticos ao bloco do e-mail, para os três tipos.
- A pessoa com CPF recebe o comprovante da parte comum, idêntico ao exemplo.
- Da parte comum também conferem:
  - 101 de 10/07 a 12/07: 599,76;
  - os erros 409 `ROOM_UNAVAILABLE`, 422 `CAPACITY_EXCEEDED`, 404 `ROOM_NOT_FOUND`/`RESERVATION_NOT_FOUND` e 400 para corpo que não é JSON e para saída igual à entrada;
  - a segunda via de reserva pendente dá 409;
  - o reembolso: 0 no plano não reembolsável e o total no normal;
  - o relatório de ocupação (`total,1/5`, `text/csv; charset=utf-8`).

*O que falha:*
- **Campos nulos omitidos.** O enunciado pede null nos campos de identificação que não se aplicam, na reserva e no documento, mas o JSON simplesmente não os traz. O mesmo acontece com `refund` antes do cancelamento. A causa é o `@JsonInclude(NON_NULL)` em `api/DocumentResponse.java:9` e `api/ReservationResponse.java:9`.
- **Identificação com campos soltos aceita.** `guestCpf` + `companyName`, ou `guestCpf` + `guestCountry`, é aceito com 201 e o campo solto fica gravado na reserva.
- **CPF vazio com CNPJ.** `guestCpf: ""` + CNPJ passa na validação, mas a fábrica escolhe o comprovante e imprime "Hóspede: Z (CPF )".
- **Alta temporada.** Dezembro inteiro conta como alta (`util/DateUtils.java:16`): uma noite em 01/12/2027 no 102 sai por 299,88, e não por 249,90.
- **Segunda via de reserva cancelada.** Sai com 200, porque só a pendente é recusada (`service/ReservationService.java:191`).
- **Relatório financeiro.** Lista reservas que não deixaram dinheiro: uma cancelada ainda pendente e uma reembolsada aparecem com `0.00` (`service/ReportService.java:64`).
- **Valores zerados.** Saem como `0` e não `0.00` (`discount`, `amountPaid`, `refund`).

O `mvn verify` da cópia passou: 39 testes, 0 falhas, 0 erros. O serviço foi derrubado no fim.
Conclusão: em parte

## Dúvidas

A forma usada foi a fábrica concentrada: um método no `DocumentService` escolhe uma implementação de `DocumentGenerator`. Não foi o criador clássico com o método de fábrica sobrescrito, nem um método polimórfico no tipo de hóspede. O tipo de hóspede nem existe como conceito: a fábrica deduz o caso pelos campos nulos da reserva. Isso continua sendo uma escolha num lugar só, mas o critério da fábrica (`!= null`) e o da validação (não vazio) divergem, e isso causa o defeito do CPF vazio. Esse defeito fica registrado no comportamento e não muda a classe.

Também fica em aberto o formato das datas no JSON do documento. O comprovante e a fatura trazem `checkIn` como "10/03/2027", e o recibo em inglês traz "2027-03-10". O enunciado não diz explicitamente o formato desses campos no documento, então não contei isso como falha.
