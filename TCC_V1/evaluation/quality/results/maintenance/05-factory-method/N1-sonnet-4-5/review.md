# Revisão: manutenção, cenário 05

## Visão geral

O agente manteve a estrutura da base: `hotel.web` (Javalin, `HttpApi` e os records de JSON), `hotel.reservations`
(o `ReservationService`, o `PriceCalculator` e o `ReservationStore` com JDBC), `hotel.documents` (o comprovante e a
formatação), `hotel.messages`, `hotel.reports`, `hotel.rooms`, e o `hotel.Main`, que monta as peças. Uma requisição de
reserva entra em `HttpApi.readNewReservation` (`src/main/java/hotel/web/HttpApi.java:109`), que agora decide o tipo de
hóspede e monta o `Guest`. O serviço grava pelo `ReservationStore`, e os dados ficam no PostgreSQL, numa migração nova
(`V2__add_guest_types.sql`). Os três lugares que emitem o documento (confirmação, check-out e segunda via) continuam
chamando `new ReservationReceipt(reservation, room)` (`ReservationService.java:64,109,118`), e é dentro desse record
que se escolhe qual documento sai, por uma cadeia de `if` sobre campos nulos do `Guest`. Tudo funciona: o roteiro deu
83 de 83 e os 29 testes passam. Os pontos fracos são de design: o tipo de hóspede é implícito (sete campos `String`,
quatro deles anuláveis), a escolha aparece duas vezes na mesma classe e os três textos são cópias quase iguais. Achei
também uma brecha fora do roteiro: um CPF junto com um CNPJ incompleto é aceito, e o CNPJ some sem aviso.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já vinha da base.
`hotel.documents` passou de 79 para 219 linhas, `hotel.web` de 236 para 268 e `hotel.reservations` de 508 para 540.
**Nota:** bom

A mudança coube nas camadas que já existiam. O SQL novo ficou só no `ReservationStore`
(`ReservationStore.java:18-20,32-55,130-137`) e o mapeamento para JSON nos records da borda
(`ReservationResponse.java:15-18,38-41`). O texto dos documentos ficou em `hotel.documents`, sem Javalin e sem JDBC.
A montagem continua no `Main`, sem `new` de acesso a dados espalhado. A borda HTTP ganhou a regra "exatamente uma
identificação" e a montagem do `Guest` conforme o tipo (`HttpApi.java:127-146`). É validação de entrada, como a base já
fazia com os campos obrigatórios (`HttpApi.java:116-125`), e não regra de preço nem de situação da reserva.

O ciclo `documents` ↔ `reservations` vem da base. O agente o reforçou um pouco: o documento agora importa `Guest` e
`PriceCalculator`, e `PriceCalculator.cents` virou público para o cálculo do ISS (`PriceCalculator.java:50`,
`ReservationReceipt.java:3-4,153`), mas não surgiu ciclo novo. O `ReservationReceipt` passou a ser a segunda maior
classe (185 linhas), mais por causa da duplicação apontada no design do que por excesso de responsabilidade.

## Design de baixo nível

**Números:** 3 tipos de documento (comprovante, fatura, booking receipt). Os 3 lugares que emitem
(`ReservationService.java:64,109,118`) pedem o documento ao mesmo construtor, e nenhum deles decide o tipo. A decisão
sobre o tipo de hóspede aparece em 3 cadeias de `if`: 2 dentro do `ReservationReceipt` (`:67-75` e `:90-99`) e 1 na
borda para montar o `Guest` (`HttpApi.java:139-146`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A forma é a da fábrica concentrada. O construtor público `ReservationReceipt(Reservation, Room)`
(`ReservationReceipt.java:31-65`) escolhe, pelo `Guest`, qual texto e qual título montar (`generateText`, `:77-100`,
despacha para `personalReceiptText`, `companyInvoiceText` ou `bookingReceiptText`). A pessoa com CPF, caso que já
existia, passa pelo mesmo caminho (`:96-99`). Os três lugares da base não mudaram uma linha e não perguntam o tipo de
hóspede: a escolha mora numa classe só, e é isso que a referência pede.

Há três coisas que afastam a solução da forma limpa:

- **A escolha é feita duas vezes, por cópia.** `title()` (`:67-75`) e `generateText()` (`:90-99`) repetem a mesma
  cadeia `companyCnpj != null` / `passport != null` / senão. Uma agência de turismo exigiria mexer nas duas e ainda no
  `Guest` e no `HttpApi`.
- **O tipo de hóspede não existe como tipo.** Ele é deduzido de qual campo não é nulo: o `Guest` tem sete `String`
  (`Guest.java:3-10`), quatro delas anuláveis, sem `enum` nem tipo selado.
- **O documento é sempre o mesmo record**, com campos que só valem para um dos tipos (`ReservationReceipt.java:16-20`).
  O pedido aceita isso, porque o JSON tem de trazer todos os campos.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface, fábrica ou camada nova (o pacote `documents`
continua com 2 classes).

**O resto do código** tem defeitos localizados:

- **Duplicação.** `personalReceiptText` (`:102-126`), `companyInvoiceText` (`:128-156`) e `bookingReceiptText`
  (`:158-185`) repetem a mesma sequência (quarto, datas, diárias, desconto, total). A fatura é o comprovante com duas
  linhas trocadas e uma a mais, mas foi copiada inteira.
- **Parâmetros demais.** São métodos com 10 e 11 parâmetros posicionais (`:77-88`, `:102-112`). Um deles, `title`, é
  recebido por `generateText` e nunca usado (`:78`), e o título é calculado duas vezes (`:37` e `:54`).
- **Construtores confusos.** `Guest(name, email, cpf)` e `Guest(name, email, companyCnpj, companyName)` diferem só pelo
  número de `String`, e o terceiro tipo usa uma fábrica estática (`Guest.java:12-22`).
- **Número mágico.** A alíquota do ISS aparece como `new BigDecimal("0.05")` solto no meio do texto (`:153`).
- **Validação com brecha.** `hasCompany` e `hasPassport` só valem quando o par está completo (`HttpApi.java:127-129`).
  Por isso, um CPF junto com um CNPJ sem razão social (ou com um passaporte sem país) passa na regra "só um" e o campo
  a mais é descartado sem aviso.

O dinheiro continua em `BigDecimal`, arredondado pelo mesmo `cents` da base, e as datas em `LocalDate`.

## Testes

**Números:** 29 testes rodaram, 0 falharam e nenhum foi pulado. Há 29 métodos escritos (a base tinha 26; o agente
acrescentou 3, todos em `HttpApiTest`, e não apagou nem enfraqueceu nenhum). A cobertura de linhas é de 94,6%
(562/594) e a de ramos de 88,3% (143/162); na base eram 93,8% e 87,1%. No pacote `hotel.documents`: 90/92 linhas e
16/20 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

Os testes novos passam pela API contra o banco e conferem bastante coisa:

- **A fatura:** os campos da reserva com os nulos que não se aplicam (`HttpApiTest.java:124-153`), o título e as linhas
  "Fatura de hospedagem nº", "Empresa: ... (CNPJ ...)" e "Hóspede:".
- **O booking receipt:** o hóspede com passaporte e país, as datas ISO e "2 nights" (`:156-187`).
- **As recusas:** sem identificação, com CPF e CNPJ juntos, com CNPJ sem razão social e com passaporte sem país
  (`:190-223`).

Mas as regras com valor da parte nova ficaram sem rede:

- **O ISS:** só o rótulo "ISS retido (5%)" é conferido (`:152`), nunca o valor. Trocar 0.05 por 0.5 em
  `ReservationReceipt.java:153` não derruba nenhum teste.
- **O dinheiro em inglês:** o teste só procura "BRL " (`:186`). Trocar o `Locale.US` de `TextFormat.moneyEnglish` por
  outro (de "1,234.56" para "1.234,56") passaria.
- **Casos sem teste:** "1 night", a linha "Discount:", o documento dentro do e-mail de confirmação e no check-out
  (os testes leem só a segunda via, `:145` e `:177`) e a recusa de CPF com passaporte.
- **Uma variável enganosa:** `checkedOut` guarda a resposta do check-in (`:143`) e não é usada.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`, nenhuma data foi guardada como
texto e nenhuma chave estrangeira está sem índice. A base tinha 1 migração, mantida igual, e o agente acrescentou outra
(`V2__add_guest_types.sql`). A conferência das colunas de situação e de tipo acusou `status`, `rate_plan`, `type` e
`channel` sem CHECK, o que já vinha da base (`measures/database.md`).
**Nota:** bom

A evolução foi feita do jeito certo. Numa migração nova, sem tocar na V1
(`src/main/resources/db/migration/V2__add_guest_types.sql:2-7`), o agente tirou o `NOT NULL` de `guest_cpf` e
acrescentou quatro colunas anuláveis, de modo que as reservas já gravadas continuam válidas. Ele também pôs no banco a
regra "exatamente uma identificação", com um CHECK (`:8-12`). O CHECK é um pouco frouxo: no ramo do CPF não exige
`company_name` e `guest_country` nulos, e não recusa texto vazio. A API não grava esses casos (`HttpApi.java:139-146`),
então não há dado errado hoje. O SQL novo usa só parâmetros `?` (`ReservationStore.java:32-55`), e o que entra por
concatenação é a constante `COLUMNS` (`:18-20`). A proteção contra reserva dupla (o EXCLUDE com `daterange`) vem da
base e continua lá.

## Comportamento

**Números:** 83 de 83 conferências passaram, nenhuma falhou (`measures/behavior.md`). Por parte:

- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** com problemas

Para o dono da pousada, o que foi pedido funciona:

- a pessoa com CPF recebe o comprovante de antes;
- a empresa recebe a fatura com o ISS de R$ 38,99 sobre R$ 779,80;
- o estrangeiro recebe o booking receipt em inglês (BRL 2,783.52, 2027-03-10, "1 night", "Discount:");
- o documento sai no e-mail, que continua em português, no check-out e na segunda via, com o campo `text`;
- as recusas pedidas dão 400 INVALID_REQUEST.

Fora do roteiro, chamei a API com o serviço de pé e achei uma brecha na regra "tem que ser um desses jeitos, e só um".
Um `POST /reservations` com `guestCpf` e só `companyCnpj` (sem `companyName`) respondeu **201** e criou uma reserva
de pessoa física com `companyCnpj: null`. O mesmo aconteceu com `guestCpf` e `guestPassport` sem `guestCountry`. A
causa é `HttpApi.java:127-137`, que só conta um tipo quando o par está completo. Na prática, um site que mande o CPF do
funcionário e um CNPJ pela metade gera um comprovante de pessoa física, e não a fatura que a empresa esperava, sem
nenhum aviso. O pedido diz que nesse caso a resposta é 400, e a referência do cenário manda recusar "um incompleto".

## Dúvidas

- **A classe do pattern.** Dei `acerto` porque a escolha está concentrada no `ReservationReceipt` e os três lugares só
  pedem o documento (forma de "fábrica concentrada": um construtor que escolhe pelo tipo de hóspede, e não um criador
  sobrescrito nem um método polimórfico no `Guest`). Mas é a versão mais fraca dessa forma. O tipo de hóspede é
  deduzido de campos nulos e a cadeia de `if` está repetida duas vezes dentro da classe (`ReservationReceipt.java:67-75`
  e `:90-99`). Um revisor mais rígido poderia ler isso como `malfeito`.
- **A nota do comportamento.** O roteiro passou inteiro. Dei `com problemas` pela brecha do CPF com uma identificação
  incompleta, que confirmei pela API. Quem ler "vem ele, ou..." como "basta o CPF" consideraria o caso em aberto e
  daria `bom`.
- **Arquivos soltos.** O patch traz `test-api.sh`, com credenciais e um `localhost:5432` fixos, e um `CHANGES.md`. Não
  pesei esses arquivos em nenhuma coluna.
