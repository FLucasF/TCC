# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (rotas, JSON), `hotel.reservations` (regras, `ReservationStore` com o JDBC), `hotel.documents` (o comprovante), `hotel.messages`, `hotel.reports` e `hotel.rooms`, montados em `hotel.Hotel`. Os dados ficam no PostgreSQL, com o esquema criado por migrações do Flyway. Uma reserva entra por `HttpApi.readNewReservation`, vai para `ReservationService.create` e é gravada no `ReservationStore`. O agente mudou nove arquivos. Pôs os quatro campos novos em `Guest`, `CreateReservationRequest`, `ReservationResponse`, `ReservationReceipt` e no `ReservationStore`. Escreveu uma migração nova (`V2__add_company_and_passport_fields.sql`), validou o "um jeito só" no handler e montou os três documentos dentro do próprio `ReservationReceipt`, com dois `if/else` sobre o tipo de hóspede e um método de texto copiado para cada tipo. O roteiro passa inteiro (83 de 83) e o `mvn verify` passa (31 testes). O mais sério é o design: a variação que o texto pede (e que vai crescer com as agências) ficou em `if` e em cópias de texto, sem um ponto de criação próprio. Os testes novos conferem rótulos, não valores.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` contra `measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. Nenhuma biblioteca nova em nenhum pacote. `HttpApi` passou de 170 para 188 linhas, `ReservationReceipt` de 55 para 128, `ReservationStore` de 143 para 156.
**Nota:** bom

O agente seguiu a divisão da base sem criar dependências. O SQL novo ficou no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:32-55` e `:130-137`). O documento ficou em `hotel.documents` sem Javalin nem JDBC. O `ReservationService` não precisou mudar, porque os três lugares que emitem o documento (`ReservationService.java` em `confirm`, `checkOut` e `receiptCopy`) já chamavam o construtor do `ReservationReceipt`. O ciclo `documents` ↔ `reservations` vem da base: `ReservationService` importa `ReservationReceipt`, e `ReservationReceipt` importa `Reservation`. O agente não o piorou. Um ponto pequeno: a regra "exatamente uma identificação" e a escolha do tipo de `Guest` moram no handler HTTP (`src/main/java/hotel/web/HttpApi.java:127-144`), e não no domínio. Mas a base já validava ali os campos obrigatórios (`HttpApi.java:116-125`), e o banco repete a regra num CHECK, então não pesa.

## Design de baixo nível

**Números:** 3 tipos de documento e nenhum tipo próprio para eles: um só record, `ReservationReceipt`. A escolha do tipo aparece em 2 `if/else` (`titleFor` e `textFor`), e o texto é montado por 3 métodos quase iguais. As 3 chamadas que emitem o documento no `ReservationService` continuam com `new ReservationReceipt(...)`.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede um documento por tipo de hóspede, emitido em três lugares, e já avisa que as agências vão querer um quarto tipo. A resposta esperada é um ponto de criação que escolhe o documento, ou um método no próprio tipo de hóspede. O agente não criou essa estrutura. Ele reaproveitou o único record da base e pôs a variação em condicionais por dentro dele:

- `titleFor` (`src/main/java/hotel/documents/ReservationReceipt.java:50-58`) e `textFor` (`:60-68`) fazem a mesma pergunta duas vezes (`isCompany()`, depois `isPassport()`, e o resto é CPF);
- cada caso tem uma cópia do montador de texto: `regularReceiptText` (`:70-86`), `companyInvoiceText` (`:88-108`) e `bookingReceiptText` (`:110-127`). As duas versões em português repetem linha por linha quarto, datas, diárias, desconto e total (`:75-84` contra `:96-105`).

O tipo do hóspede não existe como tipo. Ele é deduzido de qual campo não é nulo (`src/main/java/hotel/reservations/Guest.java:28-34`), e a ordem dos `if` decide o caso. Para pôr as agências, alguém vai ter de mexer em `Guest`, nos dois `if` do `ReservationReceipt`, numa quarta cópia de texto e no `if` de `HttpApi.java:137-144`. O que aproxima de um acerto é a escolha estar numa classe só, sem se repetir nos três lugares do serviço (veja "Dúvidas"). Mas isso já vinha da base, que concentrava o `new` no construtor do record.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface nova, nenhuma fábrica, nenhuma camada que só repassa.

**O resto do código:**

- `Guest.java:20` tem um construtor com um parâmetro `boolean isPassport` que não é usado, só para desfazer a colisão com o construtor `(String, String, String, String)` da empresa (`Guest.java:16`). Quem chama precisa passar um `true` solto (`HttpApi.java:143`). É o sintoma de modelar três identidades como sete campos anuláveis.
- `Guest.isCpf()` (`Guest.java:24-26`) não é usado em nenhum lugar.
- A taxa do ISS é um número mágico, e o arredondamento é escrito de novo em vez de reaproveitar o da base (`ReservationReceipt.java:90-91`, `new BigDecimal("0.05")` e `setScale(2, HALF_EVEN)`, contra `PriceCalculator.cents` em `PriceCalculator.java:50-52`). Pelo menos o resultado é o mesmo (HALF_EVEN, como no preço).
- A validação aceita CPF junto com uma identificação incompleta: `hasCompany` exige CNPJ e razão social (`HttpApi.java:128`), então CPF mais um CNPJ sem razão social conta como "um jeito só", e o CNPJ é descartado sem aviso. Conferi pela API: `POST /reservations` com `guestCpf` e `companyCnpj` deu 201, com `companyCnpj: null`. O mesmo vale para CPF mais `guestCountry` sem passaporte.
- O dinheiro continua em `BigDecimal` e as datas em `LocalDate`, e a formatação em inglês ficou separada em `TextFormat` (`src/main/java/hotel/documents/TextFormat.java:23-33`). Isso está certo.

## Testes

**Números:** 31 testes rodaram, 0 falhas, 0 pulados; 31 métodos escritos (a base tinha 26, todos mantidos e sem mudança; os 5 novos estão em `HttpApiTest`). Cobertura de linhas 94,2% (556 de 590; na base 93,8%). Cobertura de ramos 86,7% (137 de 158; na base 87,1%). `hotel.documents` tem 16 de 20 ramos cobertos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

Os testes novos passam pela API contra o banco e cobrem os caminhos: a empresa (`src/test/java/hotel/web/HttpApiTest.java:123-145`), o estrangeiro (`:148-172`) e três recusas: duas identificações (`:175`), nenhuma (`:188`) e empresa incompleta (`:200`). As asserções do texto, porém, procuram rótulos e não valores:

- o ISS só confere o rótulo `"ISS retido (5%)"` (`HttpApiTest.java:144`), não `R$ 38,99`. Trocar 0.05 por 0.06, ou apagar o cálculo, não derruba nenhum teste;
- o dinheiro em inglês só confere que aparece `"BRL"` (`:171`). Formatar com a localidade brasileira (`BRL 779,80`) passaria;
- não há teste de "1 night", da linha "Discount:", do separador de milhar, da fatura com desconto, do passaporte sem país, nem de CPF misturado com uma identificação incompleta (o caso que a validação deixa passar);
- o documento novo só é conferido no check-out. O e-mail de confirmação e a segunda via com fatura ou booking receipt ficam sem teste, e não há teste de unidade do `ReservationReceipt`.

As regras da base continuam bem testadas, com valores. Na parte nova, a rede pega erros de estrutura (o título errado, um campo que não é null), mas não pega erros de valor.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, nenhuma chave estrangeira está sem índice, nenhuma data está em texto. As migrações são 2: `V1` igual à da base e `V2` nova. As colunas de situação e tipo sem CHECK (`status`, `rate_plan`, `type`, `channel`) vêm da base. As reservas duplicadas continuam impedidas pelo `EXCLUDE USING gist` da base. O SQL montado com `+` só junta constantes (`measures/database.md`).
**Nota:** bom

O agente fez o certo: uma migração nova, sem editar a `V1`. Ela tira o `NOT NULL` de `guest_cpf`, acrescenta as quatro colunas e põe um CHECK que exige exatamente uma identificação (`src/main/resources/db/migration/V2__add_company_and_passport_fields.sql:3-15`). As linhas que já existem (com CPF e o resto nulo) passam no CHECK, então um banco em uso sobe sem problema. O INSERT e a leitura usam parâmetros (`src/main/java/hotel/reservations/ReservationStore.java:32-55`, `:130-137`). Um detalhe: o CHECK não exige `company_name`/`guest_country` nulos nos ramos do CPF e do passaporte (`V2__...sql:12`, `:14`), então uma linha com CPF e uma razão social solta passaria. Hoje o código nunca grava isso.

## Comportamento

**Números:** 83 de 83 conferências passaram: a parte comum 46 de 46, depois de reiniciar 3 de 3, o cenário 05 32 de 32, as reservas simultâneas 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona. O hóspede com CPF recebe o comprovante de antes. A empresa recebe a fatura com a empresa no lugar do CPF e o ISS de R$ 38,99 sobre R$ 779,80. O estrangeiro recebe o booking receipt com `BRL 2,783.52`, datas `2027-03-10`, "1 night" e a linha "Discount". O documento sai igual no e-mail (que continua em português), no check-out e na segunda via. As consultas trazem os campos que não se aplicam como null, e as recusas pedidas (nenhuma identificação, CPF com CNPJ, CNPJ sem razão social, passaporte sem país) dão 400 INVALID_REQUEST. Fora do roteiro, achei um caso que a regra "só um jeito" deixaria recusar e o sistema aceita: CPF junto com um CNPJ sem razão social é criado como reserva de CPF, e o CNPJ some sem aviso (`HttpApi.java:127-132`). Ele está contado no design; aqui não muda a nota.

## Dúvidas

- **A classe do pattern.** A escolha do documento está numa classe só, o `ReservationReceipt` (`ReservationReceipt.java:50-68`), e não se repete nos três lugares do serviço. Lido ao pé da letra, isso lembra a "fábrica concentrada" que a referência aceita como acerto. Dei omissão por três razões: não há um tipo de documento nem um ponto de criação; a mesma pergunta sobre o tipo de hóspede aparece duas vezes, mais uma no handler; e cada caso é uma cópia do texto. A concentração já vinha da base e não foi uma decisão do agente. Um revisor que pese só "onde se decide" pode ler como acerto.
- **O CPF com uma identificação incompleta.** Aceitar CPF junto com um CNPJ sem razão social (ou com um país sem passaporte) pode ser lido como algo que o texto deixava em aberto ("um jeito completo só"). Por isso contei no design e não no comportamento.
