# Revisão: manutenção, cenário 05

## Visão geral

A base já separava as camadas assim: `hotel.web` (Javalin, as rotas e o JSON), `hotel.reservations` (as regras e o
`ReservationStore` com JDBC), `hotel.documents` (o comprovante), `hotel.messages`, `hotel.reports` e `hotel.rooms`,
com a montagem em `hotel.Hotel`. Os dados ficam no PostgreSQL, com o esquema criado por migrações do Flyway. O agente
manteve essa organização. Para a mudança, criou um tipo de identificação do hóspede (`Identification`, com
`CpfIdentification`, `CompanyIdentification` e `PassportIdentification`), e cada tipo traz o seu `DocumentLayout`
(`ReceiptLayout`, `InvoiceLayout`, `BookingReceiptLayout`). Os três lugares que emitem o documento passaram a pedi-lo
a um único método, `documentOf` (`src/main/java/hotel/reservations/ReservationService.java:121-123`). As colunas novas
entraram numa migração nova, `V2__guest_identification.sql`. O `mvn verify` passa (32 testes, nenhuma falha) e o
roteiro de comportamento passou inteiro (83 de 83). O defeito mais sério é pequeno e fica no banco: o agente tirou o
`NOT NULL` de `guest_cpf` e não pôs no lugar uma restrição que garanta um jeito, e só um, de identificar o hóspede.
Essa regra só existe no Java.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos
da base (`measures/dependencies-base.md`: 7 pacotes, 17 dependências, o mesmo ciclo). `hotel.reservations` cresceu de
13 para 19 classes (de 508 para 670 linhas) e `hotel.documents` de 2 para 6 classes (de 79 para 253 linhas). A maior
classe continua sendo `HttpApi`, com 173 linhas (eram 170).
**Nota:** bom

- **A borda HTTP continua fina.** `HttpApi.java:128-130` só monta os `IdentificationFields` com os campos do corpo e
  passa para `Identifications.from`. Quem decide se a identificação é válida é o pacote das regras
  (`src/main/java/hotel/reservations/Identifications.java:21-31`), sem Javalin nem JDBC. `ReservationResponse.java:38-42`
  só copia os campos.
- **O SQL continua só no `ReservationStore`.** As colunas novas entraram em `ReservationStore.java:40-45` (o INSERT)
  e em `ReservationStore.java:129-137` (a leitura).
- **O ciclo já existia na base:** o `ReservationService` da base já usava `ReservationReceipt`, e
  `hotel.documents` já lia `Reservation`. O agente não criou um ciclo novo, mas o reforçou. Agora os tipos de
  identificação, que ficam no pacote das regras, criam os layouts concretos de `hotel.documents`
  (`CpfIdentification.java:10`, `CompanyIdentification.java:10`, `PassportIdentification.java:10`). O domínio passou
  a conhecer a apresentação. Num sistema deste tamanho, e com o ciclo herdado, isso fica como observação e não muda a
  nota.

## Design de baixo nível

**Números:** 3 de 3 tipos de hóspede estão na estrutura, inclusive a pessoa com CPF, que já existia na base. Os 3
lugares que emitem o documento (o e-mail da confirmação, o check-out e a segunda via) usam o mesmo método. Não sobrou
nenhum `if` ou `switch` sobre o tipo de hóspede.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A escolha de qual documento criar é um método polimórfico no próprio tipo de hóspede:
`Identification.documentLayout()` (`src/main/java/hotel/reservations/Identification.java:10`), que cada tipo
implementa (`CpfIdentification.java:22-23`, `CompanyIdentification.java:24-25`, `PassportIdentification.java:24-25`).
`ReservationService.documentOf` (`ReservationService.java:121-123`) é o único ponto que pede o documento, e o
`confirm` (`:64`), o `checkOut` (`:109`) e o `receiptCopy` (`:118`) passam todos por ele. O `new ReservationReceipt`
que a base repetia nos três lugares sumiu. O comprovante de CPF também foi para a estrutura (`ReceiptLayout`). Uma
agência de turismo seria um tipo novo de `Identification` com o seu layout, mais uma entrada em `Identifications.KINDS`
(`Identifications.java:14-15`), sem mexer no serviço. Isso é a forma que a referência aceita como "método polimórfico
no próprio tipo de hóspede".

**O texto dos documentos** usa um Template Method: `DocumentLayout.text` fixa a ordem das linhas
(`src/main/java/hotel/documents/DocumentLayout.java:38-45`), e cada layout só dá as linhas dele. A fatura herda o
comprovante e troca só a identificação e o fechamento com o ISS (`InvoiceLayout.java:20-30`). Isso evita copiar o
comprovante e faz parte da variação que o pattern resolve, então não conta como exagero. Também não há interface sem
uso nem camada que só repassa.

**O resto do código** está limpo. O dinheiro continua em `BigDecimal` com o arredondamento da base (o ISS usa
`PriceCalculator.cents`, `InvoiceLayout.java:28`), as datas em `LocalDate` e os nomes são claros. Ficam algumas
observações que não pedem mudança antes de aceitar:

- `Identification.fields()` devolve um registro "largo", com cinco campos, quase todos `null`, e os layouts leem a
  identificação por ele (`ReceiptLayout.java:23`, `InvoiceLayout.java:21`, `BookingReceiptLayout.java:23`), e não
  pelo tipo concreto. A ligação entre o tipo e o layout só vale por convenção. Se ela se quebrar, o documento sai com
  "null", e o compilador não avisa.
- A leitura do banco reaproveita a validação do pedido (`ReservationStore.java:137`). Assim, uma linha inconsistente
  no banco viraria um `INVALID_REQUEST` (400) numa consulta.
- `ReservationResponse.java:38-42` chama `identificationOf` cinco vezes, uma por campo (o efeito é desprezível).

## Testes

**Números:** 32 testes rodaram, com 0 falhas, 0 erros e 0 pulados, e há 32 métodos de teste no código (a base tinha
26). A cobertura de linhas é de 95,0% (569 de 599) e a de ramos de 89,1% (114 de 128); na base eram 93,8% e 87,1%.
`hotel.documents` tem 100% das linhas e dos ramos cobertos (73 de 73 linhas, 8 de 8 ramos).
**Nota:** bom

- **Os 26 testes da base continuam passando.** O agente só trocou o CPF em texto por `new CpfIdentification(...)` nos
  ajudantes (`ReservationServiceTest.java:34`, `ReportsTest.java:32`), sem enfraquecer nenhuma asserção.
- **A fatura é conferida com o texto inteiro**, com os valores do pedido (R$ 779,80, ISS de R$ 38,99), na segunda via
  e no e-mail (`src/test/java/hotel/reservations/ReservationServiceTest.java:165-183`). Se alguém trocasse 0.05 por
  outro valor, ou apagasse a linha da empresa, o teste falharia.
- **O booking receipt também é conferido com o texto inteiro**, e o teste confere que o e-mail continua em português
  (`ReservationServiceTest.java:186-199`). Há ainda um teste de "1 night" e da linha "Discount"
  (`ReservationServiceTest.java:202-212`).
- **As recusas são conferidas na regra e pela API.** Pela regra: nenhuma identificação, duas, a da empresa incompleta
  e o passaporte com o país em branco (`ReservationServiceTest.java:215-227`). Pela API, com 400 e `INVALID_REQUEST`
  (`src/test/java/hotel/web/HttpApiTest.java:116-126`).
- **O JSON é conferido pela API:** os campos novos, os `null` e o `text` do documento (`HttpApiTest.java:92-113`).

Ficam sem rede:

- o separador de milhar do inglês (`BRL 2,783.52`), porque nenhum teste usa um valor acima de mil;
- o check-out da empresa e do estrangeiro, porque só o comprovante de CPF passa pelo check-out nos testes. Como o
  caminho é o mesmo `documentOf`, o risco é baixo.

O texto completo do comprovante de CPF também não é conferido linha a linha (`ReservationServiceTest.java:84-97` só
procura trechos), mas essa lacuna já vinha da base.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`. Não há chave estrangeira
sem índice nem data guardada como texto. Há 2 migrações: a `V1` ficou igual à da base e a `V2` é nova. A restrição
`EXCLUDE` contra duas reservas na mesma noite foi mantida. As colunas de situação e de tipo continuam sem `CHECK`, como
já era na base.
**Nota:** com problemas

- **A evolução do esquema está certa.** A mudança entrou numa migração nova, sem editar a `V1`
  (`src/main/resources/db/migration/V2__guest_identification.sql:2-6`). Os bancos que já existem recebem as colunas
  novas, e as reservas antigas, todas com CPF, continuam válidas. O SQL novo usa parâmetros
  (`ReservationStore.java:31-55`).
- **O defeito:** a `V2` tira o `NOT NULL` de `guest_cpf` (`V2__guest_identification.sql:2`) e não põe nada no lugar.
  Antes, o banco garantia que toda reserva tinha CPF. Agora, uma linha sem nenhuma identificação, ou com CPF e CNPJ
  juntos, ou com CNPJ sem a razão social, entra sem erro. A regra "um jeito, e só um, completo" ficou só no Java
  (`Identifications.java:21-47`). E, como a leitura passa por essa validação (`ReservationStore.java:137`), uma única
  linha assim faria `store.all()` lançar exceção e derrubaria os dois relatórios (`OccupancyReport.java:25`,
  `RevenueReport.java:25`). Um `CHECK` com os três jeitos possíveis resolveria. Pelo serviço, porém, nenhuma linha
  assim é gravada, porque a validação vem antes do INSERT.
- **Ponto menor:** as colunas novas são `text` sem limite e sem formato, o que é coerente com o `guest_cpf` da base.

## Comportamento

**Números:** 83 de 83 conferências passaram e nenhuma falhou:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- as duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo o que o pedido descreve funciona pela API:

- a pessoa com CPF continua recebendo o mesmo comprovante;
- a empresa recebe a fatura com a razão social, o CNPJ e o ISS retido de R$ 38,99 sobre R$ 779,80, no e-mail (que
  continua em português), no check-out e na segunda via;
- o estrangeiro recebe o booking receipt com `BRL`, as datas `2027-03-10`, "1 night", a linha "Discount" e o milhar
  com vírgula (`BRL 2,783.52`);
- nenhuma identificação, CPF e CNPJ juntos e uma identificação incompleta são recusados com 400 `INVALID_REQUEST`;
- a consulta da reserva e o documento trazem os campos novos (os que não se aplicam vêm `null`), e o documento traz o
  campo `text`.

O que já funcionava continua funcionando. O defeito do banco (a falta de uma restrição sobre a identificação) não
aparece em nenhuma chamada que o texto descreve, e por isso não muda esta nota.

## Dúvidas

- Na referência do pattern, o agente usou a forma do "método polimórfico no próprio tipo de hóspede"
  (`Identification.documentLayout()`), e não a de um criador com o método de fábrica sobrescrito por subclasse.
  Combinou essa forma com um Template Method para o texto dos documentos.
- Ficou em aberto se a falta de um `CHECK` sobre a identificação deve pesar como `com problemas` no banco. Escolhi que
  sim, porque o agente tirou uma garantia que o banco da base dava e não deu outra no lugar. Mas nenhuma chamada da API
  consegue gravar um dado errado.
