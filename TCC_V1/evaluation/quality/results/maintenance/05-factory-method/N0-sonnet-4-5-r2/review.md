# Revisão: manutenção, cenário 05

## Visão geral

O projeto já vinha da base em sete pacotes (`hotel.web` com o Javalin, `hotel.reservations` com o serviço, as regras e o `ReservationStore` em JDBC, `hotel.documents`, `hotel.messages`, `hotel.reports`, `hotel.rooms` e a montagem em `hotel`). Uma requisição entra no `HttpApi`, que valida o corpo e chama o `ReservationService`. O serviço usa o `ReservationStore` (PostgreSQL, migrações Flyway) e devolve o `ReservationReceipt`. O agente não mexeu nessa estrutura. Ele acrescentou uma interface selada `ReservationDocument` com três records (`PersonReceipt`, `CompanyInvoice`, `ForeignerReceipt`) e um método de fábrica `ReservationDocument.of` que escolhe o documento pelo tipo de hóspede (`documents/ReservationDocument.java:33-41`). Também ampliou o `Guest` com quatro campos que podem ser nulos e criou a migração nova `V2`. Os 31 testes passam, e as 83 conferências do roteiro também. Os pontos mais sérios: (1) a validação aceita um CPF junto com uma segunda identificação incompleta e descarta o resto sem avisar; conferi pela API: CPF + `companyCnpj` sem razão social → 201. (2) O banco perdeu o `NOT NULL` do CPF e não ganhou nada no lugar. (3) Há muita duplicação: os mesmos 16 campos aparecem em quatro records.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). É idêntico à base (`measures/dependencies-base.md`: 7 pacotes, 17 dependências, o mesmo ciclo). `hotel.documents` passou de 2 para 6 classes (79 → 333 linhas), e `HttpApi` passou de 170 para 194 linhas.
**Nota:** bom

O agente não criou dependência nova nem ciclo novo. O ciclo `documents` ↔ `reservations` já existia na base: o serviço cria o `ReservationReceipt` (`reservations/ReservationService.java:64,109,118`), e o documento lê a `Reservation`. O `ReservationDocument` novo segue o mesmo caminho (`documents/ReservationDocument.java:33`). Cada camada ficou no lugar dela:
- O SQL das colunas novas está só no `ReservationStore` (`reservations/ReservationStore.java:18-20,32-55,130-137`).
- Os documentos não conhecem o Javalin nem o JDBC (tabela de bibliotecas: `hotel.documents` sem nenhuma).
- O HTTP só ganhou os campos novos no corpo e na resposta (`web/CreateReservationRequest.java`, `web/ReservationResponse.java`).

A regra "exatamente uma identificação" ficou na borda HTTP (`web/HttpApi.java:133-154`). É validação de entrada, no mesmo lugar onde a base já conferia os campos obrigatórios (`web/HttpApi.java:116-125`), e não pesa.

## Design de baixo nível

**Números:** 3 tipos de documento, os 3 na estrutura (`ReservationDocument.of`). Os 3 lugares onde o documento sai (e-mail de confirmação, check-out, segunda via) passam pela fábrica: `reservations/ReservationService.java:64,109,118` → `documents/ReservationReceipt.java:29-31` → `documents/ReservationDocument.java:33`. Nenhum lugar decide sozinho qual documento criar. Os 16 campos se repetem em 4 records.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A escolha do documento mora num lugar só, `ReservationDocument.of` (`documents/ReservationDocument.java:33-41`), com um `if` sobre `guest.isCompany()` e `guest.isForeigner()`. Os três lugares do serviço, inclusive os que já existiam na base, pedem o documento a essa fábrica pelo construtor de `ReservationReceipt` (`documents/ReservationReceipt.java:29-31`). Ninguém fora da fábrica pergunta o tipo de hóspede, e o e-mail usa o texto que vem do documento (`reservations/ReservationService.java:128`). É a forma "fábrica concentrada" que a referência aceita. A interface selada com um record por documento deixa cada texto na sua classe: `documents/PersonReceipt.java:49`, `documents/CompanyInvoice.java:52`, `documents/ForeignerReceipt.java:50`. Para a agência entrar, basta um record novo e uma linha na fábrica.

Duas coisas afastam o código do ideal:
- O tipo de hóspede não existe como tipo. O código deduz o tipo pelos campos nulos (`reservations/Guest.java:28-35`), e um `Guest` com tudo nulo cai calado no `PersonReceipt`.
- A fábrica é chamada de dentro do construtor de um DTO (`documents/ReservationReceipt.java:29-31`), o que esconde a decisão.

**O exagero.** Não há estrutura sem necessidade fora do pattern. O `ReservationReceipt` continua sendo a resposta JSON que a base já tinha, e a interface tem três implementações de verdade.

**O resto do código.** Os defeitos estão localizados:
- **Duplicação.** Os mesmos 16 componentes aparecem em `ReservationDocument` e nos três records. Cada record preenche à mão os nulos dos campos que não se aplicam (`documents/CompanyInvoice.java:32-49`, `documents/ForeignerReceipt.java:30-47`, `documents/PersonReceipt.java:29-46`). O `ReservationReceipt` copia campo por campo (`documents/ReservationReceipt.java:33-52`). O texto da fatura repete o do comprovante linha por linha, só que com a empresa e o ISS (compare `documents/CompanyInvoice.java:52-71` com `documents/PersonReceipt.java:49-63`). A agência vai exigir mais quatro cópias disso.
- **Um `@Deprecated` sem sentido.** O agente marcou como `@Deprecated` o acessor `text()` de um record (`documents/ReservationReceipt.java:54-58`), e o próprio serviço continua chamando esse método (`reservations/ReservationService.java:128`).
- **A validação deixa passar dados.** `web/HttpApi.java:134-136` só conta as identificações completas. Um CPF que chega com um CNPJ sem razão social é aceito, e o CNPJ é descartado (`web/HttpApi.java:147-148`).

O dinheiro está certo: `BigDecimal` com HALF_EVEN, igual ao `PriceCalculator.cents` (`documents/CompanyInvoice.java:68`, `reservations/PriceCalculator.java:271-273`).

## Testes

**Números:** 31 testes rodaram, 0 falhas, 0 erros, 0 pulados; 31 métodos escritos (a base tinha 26, e todos foram mantidos). Cobertura de linhas: 587 de 620 (94,7%; a base tinha 93,8%). Cobertura de ramos: 134 de 152 (88,2%; a base tinha 87,1%). `hotel.documents` cobre 116 de 118 linhas e 12 de 16 ramos.
**Nota:** com problemas

O agente acrescentou cinco testes, contra o banco e pela API, e só trocou `new Guest(...)` por `Guest.withCpf(...)` nos testes antigos, sem enfraquecer nenhum.

O que os testes conferem com valores:
- o ISS de R$ 38,99 sobre 779,80 (`src/test/java/hotel/reservations/ReservationServiceTest.java:178`);
- "BRL 779.80" e "2 nights" no booking receipt (`ReservationServiceTest.java:197-198`);
- as quatro recusas: nenhuma identificação, CPF+CNPJ, CNPJ sem nome, passaporte sem país (`src/test/java/hotel/web/HttpApiTest.java:175-206`);
- os campos que vêm null na resposta (`HttpApiTest.java:137-138,162-163`).

As asserções são todas `contains`, nunca o texto inteiro. A pergunta da mutação mostra o que fica sem rede:
- trocar `%,.2f` por `%.2f` em `documents/ForeignerReceipt.java:69` (o milhar "BRL 1,234.56");
- escrever as datas do booking receipt como dd/MM/yyyy;
- apagar o caso `nights == 1 ? " night"` ou a linha "Discount:".

Nenhuma dessas mudanças derruba um teste. Também não há teste de que o e-mail de confirmação e a segunda via levam a fatura ou o booking receipt (o teste de e-mail, `ReservationServiceTest.java:84-97`, é o da base, só com CPF). E não há teste de CPF com uma identificação incompleta, justamente o caso que falha.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`, nenhuma chave estrangeira está sem índice e nenhuma data é texto. A `V1` está igual à da base, e a `V2` é nova. As colunas de situação e de plano sem CHECK (`status`, `rate_plan`, `type`, `channel`) já eram assim na base. O SQL montado com `+` só concatena constantes (`reservations/ReservationStore.java:35,89,104,117`).
**Nota:** com problemas

A evolução foi feita do jeito certo: uma migração nova, sem editar a da base (`src/main/resources/db/migration/V2__add_guest_identification_types.sql`). As colunas novas entram no INSERT com parâmetros (`reservations/ReservationStore.java:32-55`).

Só que a `V2` tira o `NOT NULL` de `guest_cpf` (`V2__add_guest_identification_types.sql:3`) e não põe nenhuma restrição no lugar. O banco aceita uma reserva sem identificação nenhuma, ou com CPF e CNPJ ao mesmo tempo (`measures/database-schema.sql:94,106-109`: as cinco colunas podem ser nulas, e só os dois CHECKs antigos existem). A regra "exatamente uma identificação" ficou só no `HttpApi`. Uma linha assim viraria um "Comprovante" com "CPF null" (`documents/ReservationDocument.java:33-41`). Um CHECK sobre as três combinações fecharia isso. A reserva dupla continua protegida pelo `EXCLUDE` da base.

## Comportamento

**Números:** 83 de 83 conferências passaram:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- as reservas simultâneas: 2 de 2.

Fora do roteiro, a minha chamada à API mostrou 1 caso errado.
**Nota:** com problemas

O que o roteiro conferiu está certo:
- a pessoa com CPF continua recebendo o mesmo comprovante;
- a fatura mostra a empresa no lugar do CPF e o ISS de R$ 38,99;
- o booking receipt sai em inglês, com "BRL 2,783.52", datas 2027-03-10, "1 night" e "Discount:";
- o e-mail continua em português e leva a fatura;
- as quatro combinações erradas recebem 400 INVALID_REQUEST.

Fora do roteiro, chamei a API eu mesmo com o serviço de pé:
- CPF + `companyCnpj` sem `companyName` → **201**: a reserva foi criada com `companyCnpj: null`;
- CPF + `guestPassport` sem `guestCountry` → **201**: o passaporte foi descartado.

O texto diz "tem que ser um desses jeitos, e só um; se não for, a resposta é 400". Para o dono da pousada: uma empresa cujo site manda o CPF do hóspede e esquece a razão social não recebe erro. Recebe o comprovante de pessoa física, sem a fatura e sem o ISS que ela pediu.

## Dúvidas

- **A nota do comportamento.** Pode-se ler que CPF + identificação incompleta "é um dos jeitos" e, portanto, válido. A referência do pattern diz que um incompleto tem de ser recusado, e por isso dei `com problemas`. Pela leitura mais branda, a coluna seria `bom`, já que o roteiro passou inteiro.
- **A forma do pattern.** O agente usou a "fábrica concentrada" (um método estático com `if` sobre o tipo de hóspede, `documents/ReservationDocument.java:33-41`), e não um criador sobrescrito nem um método polimórfico no hóspede. A referência aceita essa forma como acerto.
- **O `ReservationReceipt` como cópia.** Ele copia campo por campo o documento escolhido. Contei isso como duplicação, e não como exagero, porque mantém a resposta JSON da base.
