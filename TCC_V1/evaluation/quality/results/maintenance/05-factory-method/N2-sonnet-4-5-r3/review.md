# Revisão: manutenção, cenário 05

## Visão geral

O agente manteve a organização da base: sete pacotes (`hotel.web` com o Javalin, `hotel.reservations` com as regras e o
acesso ao banco em `ReservationStore`, `hotel.documents` com os documentos, mais `rooms`, `messages`, `reports` e a
montagem em `hotel.Hotel`). Uma reserva entra por `HttpApi`, que valida o corpo e monta o hóspede
(`src/main/java/hotel/web/HttpApi.java:132-150`), passa pelo `ReservationService` e é gravada pelo `ReservationStore`
no PostgreSQL. A coluna `guest_cpf` passou a aceitar nulo e ganhou quatro colunas novas numa migração nova
(`V2__add_guest_identification_types.sql`). Para a mudança, o agente trocou o `Guest` por uma interface selada com três
registros (`PersonGuest`, `CompanyGuest`, `ForeignGuest`) e criou a interface `ReservationDocument`, com três
documentos e uma fábrica única (`ReservationDocumentFactory`). Os três lugares que emitem o documento pedem o documento
a essa fábrica. Os 83 itens do roteiro passaram e os 39 testes passam. O defeito mais sério é pequeno: a validação
aceita um CPF acompanhado de um CNPJ sem razão social (ou de um país sem passaporte) e joga fora, sem avisar, o dado
que sobrou. Quanto ao design, a escolha do tipo de hóspede aparece em `instanceof`/`if` em mais três lugares (a
gravação, a leitura e a resposta JSON), e os documentos repetem a montagem do texto.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos
da base (`measures/dependencies-base.md`: 7 pacotes, 17 dependências e o mesmo ciclo). O Javalin só aparece em `hotel`
e `hotel.web`. O JDBC fica em `hotel`, `hotel.messages`, `hotel.reservations` e `hotel.rooms`, como na base. A maior
classe é `HttpApi` (190 linhas; eram 170) e a segunda, `ReservationStore` (182; eram 143).
**Nota:** bom

A mudança não criou dependência nova nem ciclo novo. O ciclo `documents` ↔ `reservations` já existia na base: o
serviço cria o documento e o documento lê a reserva. O agente reforçou esse ciclo um pouco: os documentos agora
importam os tipos de hóspede (`src/main/java/hotel/documents/ReservationDocumentFactory.java:3-5`) e o
`PriceCalculator.cents`, que virou público para a fatura (`src/main/java/hotel/documents/CompanyInvoice.java:42`,
`src/main/java/hotel/reservations/PriceCalculator.java:50`). Mesmo assim, é o mesmo par de pacotes. A borda HTTP
continua fina. A regra de "exatamente um jeito de identificar" mora no `HttpApi`
(`src/main/java/hotel/web/HttpApi.java:132-150`), junto com a validação de campos que a base já fazia ali. É validação
de entrada, não regra de negócio. O SQL novo ficou todo no `ReservationStore`
(`src/main/java/hotel/reservations/ReservationStore.java:31-75`, `:146-162`). O cálculo do ISS ficou no documento
(`CompanyInvoice.java:26`, `:42`), sem Javalin nem SQL. A montagem continua num lugar só, em `hotel.Hotel`, que o agente
não mudou.

## Design de baixo nível

**Números:** 3 tipos de documento, os 3 dentro da estrutura (`GuestReceipt`, `CompanyInvoice`, `BookingReceipt`, todos
`implements ReservationDocument`). A escolha de qual documento criar fica num lugar só: `switch` exaustivo sobre a
interface selada em `ReservationDocumentFactory.java:16-20`. Os 3 lugares que emitem o documento usam a fábrica
(`src/main/java/hotel/reservations/ReservationService.java:64`, `:109`, `:118`). Nenhum `new` de documento fica fora da
fábrica. Fora dela, o tipo de hóspede ainda é decidido em 3 lugares (gravação, leitura e JSON da reserva).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A referência aceita como acerto "uma fábrica concentrada (uma classe ou um método que escolhe o
documento pelo tipo de hóspede)", e foi isso que o agente fez. A escolha está em `ReservationDocumentFactory.create`
(`ReservationDocumentFactory.java:15-21`). O `confirm`, o `checkOut` e o `receiptCopy` pedem o documento por
`ReservationDocument.create` e só usam a interface: `text()` no e-mail (`ReservationService.java:68`, `:128`) e o
documento devolvido nos outros dois. O comprovante da base, `ReservationReceipt`, virou `GuestReceipt` e também entrou
na estrutura. Como o `switch` é sobre uma interface selada, um quarto tipo de hóspede (a agência) faz o compilador
apontar o lugar que precisa de um caso novo. Duas coisas afastam o código da forma ideal, sem mudar a classe:

- cada documento recebe a `Reservation` inteira e converte o hóspede à força para o seu tipo
  (`GuestReceipt.java:27`, `CompanyInvoice.java:31-32`, `BookingReceipt.java:29-30`). Se alguém chamar o construtor
  direto com o hóspede errado, o resultado é um `ClassCastException`. Passar o `PersonGuest`/`CompanyGuest`/
  `ForeignGuest` já resolvido pela fábrica evitaria isso;
- a interface obriga cada documento a ter os seis campos de identificação, e cada um devolve `null` nos que não se
  aplicam (`GuestReceipt.java:43-61`, `CompanyInvoice.java:50-63`, `BookingReceipt.java:46-59`). Isso leva para o
  domínio o formato do JSON, que pertence ao `DocumentResponse`. Cada documento novo vai precisar de mais quatro
  métodos `return null`.

**O exagero.** Não achei estrutura sem uso: a interface tem três implementações, e a interface selada `Guest` é o que
permite o `switch` exaustivo. Há dois repasses pequenos: `ReservationDocument.create` só chama a fábrica
(`ReservationDocument.java:32-34`), e `Guest.withCpf/withCompany/withPassport` só chamam o construtor
(`src/main/java/hotel/reservations/Guest.java:7-17`). São duas linhas cada e não chegam a ser uma camada.

**O resto do código.**
- A decisão por tipo de hóspede está espalhada fora da fábrica. A gravação usa uma cadeia `if/else if` de `instanceof`
  sem `else` (`ReservationStore.java:43-61`): se um hóspede não cair em nenhum ramo, os parâmetros 4 a 8 ficam sem
  valor e o erro só aparece no JDBC. A leitura cai em passaporte por omissão (`ReservationStore.java:156-162`). A
  resposta repete o `instanceof` campo a campo (`src/main/java/hotel/web/ReservationResponse.java:40-44`). Esse é o
  lugar que vai mudar quando a agência chegar, e nada ali é exaustivo como na fábrica, ao contrário do `switch`.
- A montagem do texto está copiada nos três documentos. As linhas de quarto, entrada/saída, diárias, desconto e total
  se repetem em `GuestReceipt.java:66-77` e `CompanyInvoice.java:68-79`, e o `BookingReceipt.java:64-77` tem as
  mesmas linhas em inglês. Além disso, a formatação em BRL ficou num método privado do `BookingReceipt`
  (`BookingReceipt.java:80-82`), e não ao lado do `TextFormat`, que já existia para isso.
- O construtor da `Reservation` virou público só para os testes (`src/main/java/hotel/reservations/Reservation.java:20`).
- O que está bem: o ISS em `BigDecimal` com o arredondamento da casa (`CompanyInvoice.java:26`, `:42`), as datas em
  `LocalDate` e os registros imutáveis.

## Testes

**Números:** 39 testes rodaram, 0 falhas, 0 erros, 0 pulados; 39 métodos escritos. Cobertura de linhas de 95,4%
(619/649) e de ramos de 91,0% (152/167). O pacote `hotel.documents` tem 100% de linhas e de ramos. Na base eram 26
testes, com 93,8% de linhas e 87,1% de ramos (`measures/tests-base.md`).
**Nota:** bom

Os 26 testes da base continuam lá e passam. O agente só trocou `new Guest(...)` por `Guest.withCpf(...)` e
`ReservationReceipt` por `ReservationDocument` (`src/test/java/hotel/reservations/ReservationServiceTest.java:34`,
`:141-145`), sem enfraquecer nenhuma asserção. Os 13 testes novos estão em três níveis:

- **De unidade, sobre os documentos** (`src/test/java/hotel/documents/DocumentsTest.java`). Conferem com valores o ISS
  de 779,80 (`R$ 38,99`, `:67`) e o ISS com desconto (`R$ 10,62`, `:121`), os campos nulos de cada tipo (`:36-41`,
  `:59-64`, `:80-86`), `BRL 779.80` (`:91`), a data ISO e "2 nights" (`:89-90`) e a linha `Discount:` (`:134`).
- **Contra o banco** (`src/test/java/hotel/IntegrationTest.java`). Fazem o ciclo inteiro para os três tipos e
  conferem o booking receipt dentro do e-mail (`:131-145`).
- **Pela API** (`src/test/java/hotel/web/HttpApiTest.java:122-200`). Cobrem a criação de empresa e de estrangeiro com
  os campos nulos e as recusas: nenhuma identificação, CPF e CNPJ juntos, CNPJ sem razão social e passaporte sem país.

A pergunta da mutação:
- trocar 0.05 por outra taxa derruba `DocumentsTest.java:67`;
- trocar `Locale.US` derruba `:91`;
- trocar `nights == 1 ? " night" : " nights"` por sempre `" nights"` **não** derruba nada, porque `"1 nights"` também
  contém `"1 night"` (`DocumentsTest.java:105`);
- o separador de milhar (`BRL 2,783.52`) não tem teste;
- a recusa de um jeito completo misturado com um incompleto (CPF + CNPJ sem razão social) também não, e é justamente o
  caso que o sistema aceita (veja Comportamento).

Uma asserção é circular: ela recalcula o ISS esperado com a mesma fórmula do código (`IntegrationTest.java:95-96`). Os
textos são conferidos com `contains`, linha a linha, e nunca inteiros. São lacunas pequenas: as regras novas principais
estão conferidas com valores.

## Banco

**Números:** 3 tabelas, todas com chave primária. O Flyway aplica 2 migrações: `V1__create_tables.sql` está igual à da
base, e a `V2__add_guest_identification_types.sql` é nova. Dinheiro em `numeric(10,2)`, nenhuma chave estrangeira sem
índice e nenhuma data em texto. A conferência só acusou `status`, `rate_plan`, `type` e `channel` sem CHECK, e essas
colunas vieram da base. Os candidatos a "SQL com `+`" só juntam constantes (`ReservationStore.java:35`, `:106`, `:121`,
`:134`).
**Nota:** bom

A mudança no esquema veio numa migração nova, sem editar a que já existia. Quem já roda o sistema sobe sem problema:
as reservas antigas têm CPF e passam no CHECK novo. A V2 (`src/main/resources/db/migration/V2__add_guest_identification_types.sql:3-13`)
tira o `NOT NULL` de `guest_cpf`, acrescenta as quatro colunas e coloca no banco a regra de "exatamente um jeito":
CPF sozinho, ou CNPJ com razão social, ou passaporte com país. O banco recusa uma linha incoerente mesmo que o código
erre. O `INSERT` e a leitura usam parâmetros (`ReservationStore.java:32-75`) e try-with-resources, com o pool da base.
A proteção contra duas reservas na mesma noite continua sendo o `EXCLUDE USING gist` da base. O defeito da leitura
(cair em passaporte por omissão, `ReservationStore.java:156-162`) fica no design, porque o CHECK impede que uma linha
assim exista.

## Comportamento

**Números:** 83 de 83 itens passaram: parte comum 46/46, depois de reiniciar 3/3, cenário 05 32/32, reservas
simultâneas 2/2.
**Nota:** com problemas

Tudo o que o roteiro confere funciona. A pessoa com CPF continua recebendo o comprovante de hoje. A empresa recebe a
fatura com `Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)` e `ISS retido (5%): R$ 38,99`, no e-mail em português,
na segunda via e no check-out. O estrangeiro recebe o booking receipt com `BRL 779.80`, `BRL 2,783.52`, datas ISO,
"1 night" e `Discount: BRL 58.48`. Os campos que não se aplicam vêm `null`, e as quatro recusas do roteiro dão 400
`INVALID_REQUEST`.

Fora do roteiro, chamei a API com o serviço de pé. O pedido diz "tem que ser um desses jeitos, e só um; se não for, a
resposta é 400". Mesmo assim, o sistema aceitou (201):
- `guestCpf` junto com um `companyCnpj` sem `companyName`;
- `guestCpf` junto com um `guestCountry` sem passaporte;
- `companyCnpj` + `companyName` junto com um `guestPassport` sem país.

Nos três casos, o dado que sobrou foi descartado sem aviso. A reserva com CPF + CNPJ saiu com o comprovante de pessoa
física e `companyCnpj: null`. A causa está em `HttpApi.java:133-139`: um jeito incompleto conta como "nenhum", em vez
de "um a mais". Para o dono da pousada: uma empresa que manda o CPF do funcionário e o CNPJ, mas esquece a razão
social, não recebe erro. Ela recebe um comprovante comum no lugar da fatura, e o ISS retido não aparece. É um caso de
borda, mas o texto pedia a recusa.

## Dúvidas

- A forma do pattern é a fábrica concentrada: um `switch` exaustivo sobre a interface selada `Guest`, numa classe
  própria (`ReservationDocumentFactory.java:15-21`), e não o Factory Method clássico com um criador sobrescrito.
- Pesei como defeito de comportamento aceitar um jeito completo misturado com outro incompleto, porque o texto diz "só
  um". Alguém pode ler o pedido como "os campos de um jeito incompleto são ignorados". Por essa leitura, a coluna seria
  `bom`.
- Os repasses `ReservationDocument.create` e `Guest.with*` ficaram como contenção, por serem triviais. Um revisor mais
  rígido poderia chamá-los de exagero leve.
