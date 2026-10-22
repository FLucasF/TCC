# Revisão: manutenção, cenário 05

## Visão geral

O agente manteve a estrutura da base: `hotel.web` (Javalin, `HttpApi` lê o JSON e valida), `hotel.reservations`
(o `ReservationService` com as regras, o `ReservationStore` com o SQL), `hotel.documents` (o documento) e a montagem
em `hotel`. Os dados ficam no PostgreSQL, com o esquema criado por Flyway. Para a mudança, ele pôs quatro campos
opcionais no `Guest` (`final/src/main/java/hotel/reservations/Guest.java:3-14`), uma migração nova `V2`, a validação
"exatamente um jeito" no `HttpApi` (`src/main/java/hotel/web/HttpApi.java:127-144`) e, dentro do próprio record
`ReservationReceipt`, a escolha do documento por `if` sobre quais campos são nulos
(`src/main/java/hotel/documents/ReservationReceipt.java:50-68`). Os três lugares que emitem o documento continuam
chamando o mesmo construtor, então a escolha fica num lugar só. O roteiro passou por inteiro (83 de 83), e o
`mvn verify` passa (30 testes). O mais sério é um descuido na validação: a checagem desconsidera campos vazios, mas o
documento é escolhido por `!= null`. Assim, uma reserva com CPF e `"companyCnpj": ""` é aceita e o hóspede recebe
por e-mail uma "Fatura de hospedagem" com "Empresa: null (CNPJ )" e ISS retido (eu reproduzi isso pela API). Além
disso, os testes novos só conferem o título do documento, e nenhum confere o texto.

## Arquitetura

**Números:** 7 pacotes, 17 dependências, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números são os
mesmos da base (`measures/dependencies-base.md`: 7 pacotes, 17 dependências, o mesmo ciclo). O `HttpApi` passou de
170 para 196 linhas e o `ReservationReceipt` de 55 para 133.
**Nota:** bom

O agente não criou dependência nova nem ciclo novo. O ciclo `documents` ↔ `reservations` já existia na base: o
`ReservationService` cria o `ReservationReceipt` (`src/main/java/hotel/reservations/ReservationService.java:64,109,118`),
e o recibo lê a `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:29`). O SQL novo ficou no
`ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:32-58,129-137`), e a coluna nova do banco
só aparece ali e na migração. A validação nova está na borda HTTP (`src/main/java/hotel/web/HttpApi.java:127-144`),
junto com a validação de campos obrigatórios que a base já fazia ali. É uma regra de entrada, não de negócio, e
segue o padrão da base. A montagem não mudou. O `ReservationReceipt` cresceu bastante (133 linhas, com três textos
inteiros), mas continua sem Javalin nem JDBC.

## Design de baixo nível

**Números:** 3 casos de documento (comprovante, fatura, booking receipt). Os três saem do mesmo ponto de criação
(`new ReservationReceipt(reservation, room)`), e nenhum dos três lugares que emitem (`confirm`, `checkOut`,
`receiptCopy`) pergunta qual é o caso. Dentro do record, a decisão aparece em duas cadeias `if/else` paralelas
(`inferTitle` e `generateText`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A escolha do documento fica num lugar só: o construtor de `ReservationReceipt`
(`src/main/java/hotel/documents/ReservationReceipt.java:29-48`), que chama `inferTitle` (`:50-58`) e
`generateText` (`:60-68`). Os três pontos de emissão do `ReservationService` não mudaram e não sabem qual é o tipo
de hóspede (`src/main/java/hotel/reservations/ReservationService.java:64,109,118`). Isso é a "fábrica concentrada" que
a referência aceita, e o comprovante da base também passa por ela (`generateReceipt`, `:70-86`). Três coisas afastam
o resultado da forma limpa:

- o tipo de hóspede não existe como tipo; ele é deduzido de quais `String` são nulas;
- a decisão está duplicada em duas cadeias `if` no mesmo arquivo (`:51-57` e `:61-67`). Uma agência de turismo
  obriga a mexer nas duas, além de `Guest`, `ReservationResponse`, `CreateReservationRequest`, `ReservationStore` e
  `HttpApi`;
- o "documento" é sempre o mesmo record, com `title` e `text` variando. Os campos de todos os tipos convivem ali, e
  o ISS da fatura só existe dentro do texto.

**Exagero.** Não há estrutura sem necessidade. `InternationalFormat` (`src/main/java/hotel/documents/InternationalFormat.java:9-24`)
é o par do `TextFormat` que a base já tinha.

**O resto do código.** Há defeitos localizados:

- **Duplicação.** `generateReceipt` e `generateInvoice` repetem as mesmas cinco linhas: quarto, datas e diárias,
  diárias, desconto e total (`:75-84` e `:94-103`). `generateBookingReceipt` repete a mesma estrutura com outro
  formato (`:109-126`).
- **ISS.** A alíquota é um literal dentro do método (`:129`). O arredondamento usa `java.math.RoundingMode` escrito
  por extenso (`:131`) em vez do `PriceCalculator.cents` que a base usa para dinheiro.
- **Nulos e vazios.** A validação conta só os campos não vazios (`src/main/java/hotel/web/HttpApi.java:128-130`).
  Mas o `Guest` recebe os valores crus (`:146-153`), e o documento é escolhido por `!= null`
  (`src/main/java/hotel/documents/ReservationReceipt.java:51,53`). As duas regras não concordam, e isso causa o
  defeito descrito no Comportamento.
- **O `Guest`.** Virou um record de sete campos que podem ser nulos, mais um construtor de compatibilidade
  (`src/main/java/hotel/reservations/Guest.java:3-14`). Nada nele garante que a combinação é válida.
- **Comentários.** O agente apagou o comentário que explicava o `catch (Exception)` da base
  (`src/main/java/hotel/web/HttpApi.java:113`). É pouca coisa.

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados. São 30 métodos escritos (a base tinha 26: o agente
acrescentou 4 e não apagou nenhum). A cobertura é de 94,1% das linhas (557/592) e 86,3% dos ramos (126/146). Na base
era 93,8% e 87,1%. Em `hotel.documents`: 85/87 linhas e 16/20 ramos.
**Nota:** com problemas

Os quatro testes novos ficam todos em `src/test/java/hotel/web/HttpApiTest.java`:

- `acceptsCompanyIdentification` (`:124-143`) e `acceptsPassportIdentification` (`:146-165`) só conferem que os
  campos voltam no JSON e o `title` do documento (`:142`, `:164`). Nenhum confere o `text`.
- `rejectsMultipleIdentificationTypes` (`:168-179`) testa CPF + CNPJ; `rejectsNoIdentification` (`:181-190`) testa
  a ausência de identificação.

Pela pergunta da mutação, estas mudanças passariam sem nenhum teste falhar:

- trocar `0.05` por `0.5` (`src/main/java/hotel/documents/ReservationReceipt.java:129`);
- tirar a linha do ISS;
- escrever o dinheiro como `R$` no booking receipt;
- trocar o formato da data;
- trocar `" night"` por `" nights"`;
- apagar a linha "Discount:";
- mostrar o CPF na fatura.

Também não há teste para CNPJ sem razão social nem para passaporte sem país
(`src/main/java/hotel/web/HttpApi.java:138-144`). A cobertura alta vem só de executar o código, sem conferir o que
ele produz. Os testes da base continuam passando, e o comprovante com CPF ainda é conferido por valor no e-mail
(`src/test/java/hotel/reservations/ReservationServiceTest.java:95-96`). Por fim, o agente deixou uma pasta vazia,
`src/test/java/hotel/documents/`.

## Banco

**Números:** 3 tabelas, 2 migrações Flyway (`V1` igual à base, `V2__add_identification_fields.sql` nova). O dinheiro
está todo em `numeric(10,2)`, as chaves estrangeiras têm índice, as datas estão em `date`, e o `EXCLUDE` contra
reserva sobreposta foi mantido. As conferências não acusaram nada novo. As colunas de situação sem CHECK
(`status`, `rate_plan`, `type`, `channel`) já eram assim na base.
**Nota:** com problemas

A evolução foi feita do jeito certo: uma migração nova, sem editar a `V1`
(`src/main/resources/db/migration/V2__add_identification_fields.sql:1-8`). O SQL usa parâmetros, e as colunas novas
foram incluídas no `INSERT` e na leitura (`src/main/java/hotel/reservations/ReservationStore.java:32-58,129-137`). O
defeito é que a `V2` tira o `NOT NULL` do `guest_cpf` (`V2__add_identification_fields.sql:7-8`; na base era
`V1__create_tables.sql:24`) e não põe nada no lugar. Na base, o banco garantia que toda reserva tinha identificação.
Agora ele aceita uma reserva sem nenhuma identificação, com duas, com CNPJ sem razão social e com campo vazio. Um
`CHECK (num_nonnulls(guest_cpf, company_cnpj, guest_passport) = 1)` e checagens de par (CNPJ com razão social,
passaporte com país) seguiriam o padrão da base, que já protegia as próprias regras com CHECK
(`V1__create_tables.sql:27,36`). Por isso a regra "exatamente um jeito" vive só no `HttpApi`. Nada se perde nem se
corrompe com a migração.

## Comportamento

**Números:** 83 de 83 conferências passaram: parte comum 46/46, depois de reiniciar 3/3, cenário 05 32/32, reservas
simultâneas 2/2.
**Nota:** com problemas

Fora do roteiro, chamei a API com o serviço de pé:

- **CPF com `"companyCnpj": ""`.** O POST responde 201, e a reserva volta com `"companyCnpj": ""` em vez de `null`.
  Ao confirmar, o e-mail leva "Fatura de hospedagem nº 1 / Empresa: null (CNPJ ) / … / ISS retido (5%): R$ 38,99".
  A causa está explicada no Design (`src/main/java/hotel/web/HttpApi.java:128-130`,
  `src/main/java/hotel/documents/ReservationReceipt.java:51`). Sites costumam mandar os campos não preenchidos como
  texto vazio, então o caso é realista.
- **CPF com `companyName` e `guestCountry`, sem CNPJ e sem passaporte.** O POST responde 201, e a reserva guarda e
  mostra a razão social e o país de um hóspede com CPF. O pedido diz "tem que ser um desses jeitos, e só um", e isso
  mistura três.

Para o dono da pousada: o roteiro inteiro funciona, inclusive a fatura com o ISS de R$ 38,99 e o booking receipt em
BRL. Mas se o site mandar um campo de empresa em branco, um hóspede com CPF recebe por e-mail uma fatura com
"Empresa: null", e o sistema aceita combinações de campos que o pedido mandava recusar.

## Dúvidas

- **A classe do pattern.** Classifiquei como `acerto` (fábrica concentrada) porque a escolha mora num lugar só, e os
  três pontos de emissão pedem o documento a esse lugar sem perguntar o caso. Mas a forma é a mais fraca que a
  referência aceita: um `if` duplicado (`inferTitle` e `generateText`) dentro do construtor de um record único, sem
  tipo de hóspede nem tipo de documento. Um revisor mais rigoroso poderia chamá-la de `malfeito`.
- **O peso das falhas de validação.** Não está claro se o texto obriga a recusar campos "a mais" de outro jeito
  (CPF com `companyName`). Eu li "só um" como recusa. O caso do CNPJ vazio, por outro lado, é um documento errado
  sem ambiguidade.
