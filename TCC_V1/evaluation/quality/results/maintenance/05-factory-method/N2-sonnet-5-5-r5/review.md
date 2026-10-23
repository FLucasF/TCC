# Revisão: manutenção, cenário 05

## Visão geral

O agente manteve a estrutura da base: sete pacotes (`hotel`, `hotel.web`, `hotel.reservations`, `hotel.documents`,
`hotel.messages`, `hotel.reports`, `hotel.rooms`), o `Hotel` montando as peças, o `HttpApi` só lendo o JSON e chamando
o `ReservationService`, e o `ReservationStore` com o SQL (PostgreSQL, migrações do Flyway). A mudança é esta: o
`Guest` agora carrega uma `Identification` (`Cpf`, `Company` ou `ForeignGuest`), montada a partir dos campos por
`Identifications.from`, que exige exatamente um jeito. Cada identificação diz qual documento recebe
(`documentLayout()`), e um `DocumentLayout` abstrato (com `ReceiptLayout`, `InvoiceLayout` e `BookingReceiptLayout`
mais duas `Wording`, português e inglês) monta o `ReservationReceipt` com o novo campo `text`. Os três lugares do
serviço pedem o documento por um único método. A migração V2 acrescenta as colunas sem mexer na V1. Os 32 testes
passam, e as 83 conferências do roteiro também. O que há de mais sério é pouco: o banco perdeu o `NOT NULL` do CPF e
não ganhou uma restrição que garanta uma identificação por reserva, e o pacote `reservations` passou a conhecer as
classes concretas dos documentos, o que reforça um ciclo que já vinha da base.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números
são iguais aos da base (`measures/dependencies-base.md`: 7 pacotes, 17 dependências, o mesmo ciclo). `hotel.documents`
foi de 2 para 8 classes (de 79 para 356 linhas), e `hotel.reservations` foi de 13 para 19 classes (de 508 para 701
linhas).
**Nota:** bom

- A borda HTTP continua fina. `HttpApi.readNewReservation` só junta os cinco campos num `IdentificationFields` e
  pede a identificação a `Identifications.from` (`src/main/java/hotel/web/HttpApi.java:129-131`). A regra "um jeito
  e só um" fica fora do Javalin (`src/main/java/hotel/reservations/Identifications.java:17-29`), e o JSON de saída só
  copia os getters (`src/main/java/hotel/web/ReservationResponse.java:37-41`).
- O SQL continua todo no `ReservationStore`: as colunas novas estão em
  `src/main/java/hotel/reservations/ReservationStore.java:18-20`, `:32-46` e `:130-136`. O serviço não ganhou SQL,
  e o `hotel.documents` não usa JDBC nem Javalin (tabela de bibliotecas em `measures/dependencies.md`).
- O ciclo `documents` ↔ `reservations` já existia na base: o serviço criava o `ReservationReceipt`, e o recibo lia
  a `Reservation`. Não é do agente, mas ele o reforçou: os tipos de identificação, que são do domínio, agora importam
  e criam as classes concretas dos documentos (`src/main/java/hotel/reservations/Cpf.java:4,15-17`,
  `Company.java:4,23-25`, `ForeignGuest.java:3,24-26`, `Identification.java:3,11`). Isso não criou um ciclo novo nem
  mistura de camadas que um revisor exigiria desfazer agora. Fica como observação.
- Nenhuma classe disparou de tamanho: a maior continua sendo o `HttpApi`, com 174 linhas (eram 170).

## Design de baixo nível

**Números:** os 3 tipos de hóspede (inclusive o CPF, que já existia) estão na estrutura, e nenhum fica fora. Os 3
lugares que emitem o documento (confirmação, check-out e segunda via) passam pelo mesmo método
(`ReservationService.java:64`, `:109` e `:118` → `:121-123`). Não sobrou `if` nem `switch` sobre o tipo de hóspede
em lugar nenhum. Acrescentar a agência pede uma `Identification` nova, um `DocumentLayout` e uma entrada em
`Identifications.KINDS`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

- **O pattern.** A escolha do documento é um método polimórfico no próprio tipo de hóspede, que a referência aceita
  como acerto: `Identification.documentLayout()` (`src/main/java/hotel/reservations/Identification.java:11`) devolve
  o criador certo em cada implementação (`Cpf.java:15-17`, `Company.java:23-25`, `ForeignGuest.java:24-26`). O
  criador é um `DocumentLayout` com o método `issue` final, que monta o documento
  (`src/main/java/hotel/documents/DocumentLayout.java:22-43`), e os passos variáveis são sobrescritos por tipo:
  `title`, `partyLines` e `closingLines` (`:45-53`). O serviço não sabe qual documento sai
  (`ReservationService.java:122`). Na base, os três lugares faziam `new ReservationReceipt(...)` à mão
  (`base/.../ReservationService.java:64,109,118`), e todos os três passaram para a estrutura. O comprovante do CPF
  também está nela (`src/main/java/hotel/documents/ReceiptLayout.java`). Não houve caso esquecido.
- **A escolha do tipo** a partir dos campos também fica num lugar só. `Identifications.KINDS`
  (`src/main/java/hotel/reservations/Identifications.java:11-12`) é uma lista de funções, uma por tipo, e
  `from` (`:17-29`) exige exatamente um resultado. Cada tipo recusa o próprio par incompleto (`Company.java:16-18`,
  `ForeignGuest.java:16-19`).
- **O exagero.** Não há. A interface `Wording` tem duas implementações que de fato variam (formato do dinheiro, das
  datas, das palavras: `PortugueseWording.java`, `EnglishWording.java`) e substituiu o `TextFormat` estático da base.
  O `IdentificationFields` é só o registro dos cinco campos, usado na entrada HTTP e na leitura do banco. Não há
  interface de uma implementação só, builder ou camada de repasse.
- **O resto do código.** O texto do documento é escrito uma vez só (`DocumentLayout.java:63-79`), em vez de ser
  copiado em três versões. O ISS usa `BigDecimal` e o mesmo arredondamento de sempre
  (`src/main/java/hotel/documents/InvoiceLayout.java:13,34`, com `PriceCalculator.cents` agora público). Os tipos
  novos são `record`s imutáveis. Há pontos menores que não pedem mudança antes de aceitar:
  - a interface `Identification` tem cinco getters com `default null` (`Identification.java:13-31`). Isso
    acompanha o JSON achatado que o texto pede, mas faz os layouts confiarem em receber o tipo certo
    (`BookingReceiptLayout.java:21-22` lê `passport()` sem garantia de tipo);
  - `ReservationStore.read` reusa a validação da requisição (`ReservationStore.java:131`), de modo que uma linha
    inconsistente no banco viraria um `400 INVALID_REQUEST` numa leitura;
  - há um import duplicado em `src/test/java/hotel/reports/ReportsTest.java:8-9`.

## Testes

**Números:** 32 testes rodaram, com 0 falhas, 0 erros e 0 pulados, e há 32 métodos de teste no código (a base tinha
26: o agente acrescentou 6 e manteve os 26). Cobertura de linhas: 95,2% (593 de 623; na base, 93,8%). Cobertura de
ramos: 86,4% (121 de 140; na base, 87,1%). `hotel.documents` tem 86 de 86 linhas e 6 de 6 ramos cobertos.
**Nota:** bom

- **A parte nova é conferida com valores e com o texto inteiro.**
  - A fatura é comparada linha a linha, inclusive com `ISS retido (5%): R$ 38,99`, no e-mail e na segunda via
    (`src/test/java/hotel/reservations/ReservationServiceTest.java`, teste
    `aCompanyGetsALodgingInvoiceWithTheWithheldIss`, cerca da linha 45).
  - O booking receipt é comparado inteiro, em inglês, com `BRL 779.80` e datas ISO, e confere que o e-mail continua
    começando em português (`aForeignGuestGetsABookingReceiptInEnglish`).
  - O desconto e "1 night" têm teste próprio (`theBookingReceiptShowsTheDiscountAndASingleNight`), e a identificação
    é conferida depois de ir ao banco e voltar (`theIdentificationSurvivesBeingStored`).
  - Pela API, `identifiesTheGuestByExactlyOneOfTheThreeWays` (`src/test/java/hotel/web/HttpApiTest.java`, cerca da
    linha 109) testa nenhum jeito, CPF com CNPJ, CPF com passaporte, CNPJ com passaporte, CNPJ sem razão social e
    passaporte sem país, sempre com 400, e também os `null` na reserva.
  - `theDocumentCarriesTheIdentificationAndItsText` confere os campos e o `text` no JSON do documento.
- **A pergunta da mutação.** Trocar 0.05 por outra taxa, apagar a linha do ISS, trocar "nights" por "night", trocar
  o formato da data ou deixar passar dois jeitos ao mesmo tempo faria testes falharem. Ficariam sem rede:
  - o separador de milhar do `BRL 1,234.56`: trocar `%,.2f` por `%.2f` em
    `src/main/java/hotel/documents/EnglishWording.java:13` passaria, porque nenhum valor de teste chega a mil;
  - o arredondamento do ISS: 779,80 × 5% dá exato;
  - o check-out devolvendo a fatura ou o booking receipt: só o comprovante do CPF é conferido no check-out.

  O roteiro do comportamento confere esses três pontos, e eles funcionam.
- Os testes da base foram só adaptados ao novo construtor do `Guest` (`new Cpf(...)`), sem perder nenhuma asserção.
  O texto completo do comprovante do CPF continua conferido só por partes, como já era na base.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, não há chave estrangeira
sem índice e não há data em texto. As conferências só acusaram o que já vinha da base: `status`, `rate_plan`,
`rooms.type` e `messages.channel` sem `CHECK`. A V1 está igual à da base, e a V2 é nova (`measures/database.md`).
**Nota:** com problemas

- **A evolução está certa.** O agente não editou a V1 e criou
  `src/main/resources/db/migration/V2__guest_identification.sql:2-7`, que solta o `NOT NULL` de `guest_cpf` e
  acrescenta as quatro colunas novas. Um banco que já roda o sistema sobe sem perder nada, e as reservas antigas
  (todas com CPF) continuam válidas.
- **O defeito.** A V2 tira a única garantia que o banco dava sobre a identificação (`guest_cpf NOT NULL`, em
  `V1__create_tables.sql:23`) e não põe nada no lugar. Não há `CHECK` que exija o CPF, ou o CNPJ com a razão social,
  ou o passaporte com o país. A regra "exatamente um" fica só no Java (`Identifications.java:22-27`). Uma linha sem
  identificação, ou com meio par, gravada por fora do serviço, faz `ReservationStore.read`
  (`ReservationStore.java:131`) lançar `HotelException` ao ler. Com isso, `find` e `all` falhariam, e os relatórios,
  que dependem de `all`, também.
- **O código que fala com o banco.** O SQL continua com parâmetros (`?`). O que entra por concatenação é só a
  constante `COLUMNS` (`ReservationStore.java:18-20`, `:105`, `:118`). As conexões são devolvidas com
  try-with-resources, há pool, e a reserva dupla segue protegida pelo `EXCLUDE` da base. O `all()`, que traz a
  tabela inteira, já vinha da base.

## Comportamento

**Números:** 83 de 83 conferências passaram:

- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 05: 32 de 32;
- reservas simultâneas: 2 de 2.

**Nota:** bom

O que o dono da pousada pediu funciona pela API:

- a pessoa com CPF recebe o comprovante de antes, com o mesmo texto;
- a empresa recebe a fatura com a empresa no lugar do CPF e o ISS de R$ 38,99 sobre R$ 779,80, no e-mail (que
  continua em português) e no check-out;
- o estrangeiro recebe o booking receipt com `BRL`, o milhar com vírgula (`BRL 2,783.52`), as datas `2027-03-10`, a
  linha `Discount` e "1 night";
- os campos que não se aplicam vêm `null`, na reserva e no documento;
- nenhuma identificação, CPF com CNPJ, CNPJ sem razão social e passaporte sem país dão `400 INVALID_REQUEST`.

Nada do que já funcionava quebrou, os dados sobrevivem ao reinício e o mesmo quarto não é reservado duas vezes.

## Dúvidas

- A forma do pattern: o agente usou o método polimórfico no tipo de hóspede (`identification.documentLayout()`),
  que devolve um criador com os passos sobrescritos por tipo (`DocumentLayout` e as três subclasses). A referência
  aceita as duas coisas como acerto. Não há dúvida sobre a classe, só registro qual forma foi.
- No banco, fiquei entre `bom` e `com problemas`. O defeito, a falta de um `CHECK` no lugar do `NOT NULL` que foi
  tirado, só aparece com dado gravado por fora do serviço. Escolhi `com problemas` porque a migração enfraqueceu de
  fato uma garantia que a base tinha.
