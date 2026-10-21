# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin em sete pacotes. Uma requisição entra por `hotel.web.HttpApi`, que lê o corpo e chama `hotel.reservations.ReservationService`. As regras ficam no serviço e no `PriceCalculator`, e o SQL fica nas classes de acesso (`ReservationStore`, `Rooms`, `SentMessages`). O documento é montado em `hotel.documents`, e os dados ficam no PostgreSQL. Para os três tipos de hóspede, o agente criou uma fábrica concentrada, `DocumentFactory`, com três `DocumentWriter`, e os três lugares que emitem o documento passaram a usá-la. Essa parte está certa, e o roteiro passou inteiro (83 de 83). O defeito mais sério está no banco. O agente apagou a migração `V1__create_tables.sql` e tirou o Flyway, colocando no lugar um `CREATE TABLE IF NOT EXISTS` sem restrições (`Database.java:17-31`). Assim o banco perdeu a trava contra reserva dupla, a chave estrangeira, os `NOT NULL` e os `CHECK`. Pior: no banco que a pousada já usa, as colunas novas nunca são criadas. Conferi isso subindo a base, criando uma reserva e depois subindo a final no mesmo banco: `GET /reservations/1` e `POST /reservations` passaram a responder 500 (`column "company_cnpj" does not exist`). Além disso, o `INSERT` da reserva passou a montar o SQL juntando os dados do hóspede no texto (`ReservationStore.java:34-41`). Um hóspede chamado "John O'Neil" recebe 500, e o código fica aberto a injeção de SQL.

## Arquitetura

**Números:** 7 pacotes e 17 dependências na final, iguais aos da base (7 e 17). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). Na final, o pacote `hotel` não usa mais o Flyway e passou a guardar o esquema em SQL. A maior classe é `HttpApi`, que passou de 170 para 184 linhas.
**Nota:** bom

A mudança respeita a estrutura da base. A borda HTTP continua fina e só ganhou a checagem de forma do corpo, ou seja, se veio exatamente um jeito de identificar o hóspede (`web/HttpApi.java:126-134`). A base já fazia a checagem dos campos obrigatórios no mesmo lugar (`HttpApi.java:116-125`). A escolha e a montagem do documento ficam em `hotel.documents`, sem Javalin e sem JDBC (`documents/DocumentFactory.java:14-27`). O serviço só pede o documento nos três lugares (`reservations/ReservationService.java:66`, `:111` e `:120`). O ciclo `documents` ↔ `reservations` veio da base; o agente o manteve, sem criar outro. Há dois pontos pequenos, que não chegam a pesar. O `DocumentFactory` é criado com `new` dentro do serviço (`ReservationService.java:19`), em vez de ser montado em `Hotel`, mas não tem estado nem dependências. E a regra de "um jeito só" vale apenas na borda, porque o `Guest` aceita qualquer combinação (`reservations/Guest.java:7-18`). A troca do Flyway pelo SQL em `Database.java` é problema de banco e está contada na coluna do banco.

## Design de baixo nível

**Números:** 3 de 3 tipos de documento estão na estrutura (pessoa, empresa e estrangeiro, incluindo o comprovante que já existia). Os 3 de 3 lugares de emissão pedem o documento à fábrica (confirmação, check-out e segunda via). Não ficou nenhum `new ReservationReceipt` fora da fábrica. A decisão sobre o tipo de hóspede está num lugar só.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É uma fábrica concentrada. `DocumentFactory.writerFor` escolhe o `DocumentWriter` pelo tipo de hóspede, e esse é o único lugar que decide (`documents/DocumentFactory.java:19-27`). `issue` monta o documento (`:14-17`). O comprovante que já existia saiu do `ReservationReceipt` e virou `PersonReceiptWriter`, com o texto igual ao da base. Os três lugares do serviço chamam `documents.issue(...)` (`ReservationService.java:66`, `:111`, `:120`). Para criar o documento da agência, basta uma classe nova e um ramo em `writerFor`. A referência aceita essa forma como acerto.

**O exagero.** A interface `DocumentWriter` (`documents/DocumentWriter.java:7-12`) tem três implementações e é a parte que o pattern resolve. Não há estrutura sobrando fora dela.

**O resto do código.** Achei estes defeitos localizados:
- Duplicação. `PersonReceiptWriter.text` e `CompanyInvoiceWriter.text` repetem quase linha a linha o quarto, as datas, as diárias, o desconto e o total (`documents/PersonReceiptWriter.java:20-29` e `documents/CompanyInvoiceWriter.java:23-32`). Uma correção no comprovante precisa ser feita duas vezes, e com a agência serão três.
- O cálculo do ISS (5%, meio para o par) mora dentro do método que escreve o texto (`CompanyInvoiceWriter.java:33-34`). O valor não aparece em nenhum campo do documento.
- O `Guest` virou um registro com sete campos que podem ser nulos (`reservations/Guest.java:7-14`), sem nada que garanta "exatamente um jeito". `isCompany` e `isForeigner` deduzem o tipo pela presença de um campo (`:20-26`), quando um tipo próprio (um `enum` ou um tipo selado) diria isso direto.
- A constante `EXCLUSION_VIOLATION` ficou morta (`reservations/ReservationStore.java:23`), porque o agente apagou o tratamento que a usava.
- O SQL montado juntando texto (`ReservationStore.java:34-54`) também é um defeito de design, mas pesa na coluna do banco.

## Testes

**Números:** na final, 32 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 32 métodos escritos. Na base eram 26 de 26. A cobertura é de 94,6% das linhas (543 de 574) e 89,5% dos ramos (136 de 152); na base era 93,8% e 87,1%. Em `hotel.documents`, 86 de 87 linhas e 14 de 16 ramos estão cobertos (`measures/tests.md`).
**Nota:** bom

O agente manteve os 26 testes da base, sem apagar nem enfraquecer nenhum, e escreveu 6 novos que conferem a parte nova com valores:
- A fatura inteira, linha por linha, com o ISS de R$ 38,99 (`src/test/java/hotel/documents/DocumentsByGuestTest.java:52-72`).
- A fatura no e-mail em português e no check-out (`:75-84`).
- O booking receipt inteiro (`:87-105`).
- "1 night", a linha "Discount: BRL 58.48" e o milhar "BRL 2,783.52" (`:108-116`).
- O comprovante de CPF mantido (`:119-128`).
- As recusas pela API: nenhum jeito, dois jeitos, CNPJ sem a razão social, passaporte sem o país. E também os campos `null` na consulta (`src/test/java/hotel/web/HttpApiTest.java:109-128`).

Fiz a pergunta da mutação. Trocar 0.05 por outra taxa, `Locale.US` por outro, `nights == 1` ou a ordem de `writerFor` derruba algum teste. Ficaram sem rede três coisas: o JSON do documento pela API (o campo `text` e os quatro campos novos em `GET /reservations/{id}/document`), o check-out e a segunda via do estrangeiro, e um nome com apóstrofo. Este último deixaria ver o defeito do `INSERT`. Os testes também não teriam como pegar a regressão do banco: rodam num banco vazio, e a reserva dupla está barrada pelo `synchronized` do serviço.

## Banco

**Números:** 3 tabelas, cada uma com chave primária e só 1 `NOT NULL` (o da chave). Dinheiro em `numeric(10,2)` e datas em `date`. Nenhuma chave estrangeira, nenhum `CHECK` e nenhum `UNIQUE`/`EXCLUDE`. A migração `V1__create_tables.sql` da base foi **APAGADA**, e o esquema passou a ser um `CREATE TABLE` dentro do Java. O `INSERT` da reserva monta o SQL juntando dado de fora (`measures/database.md`).
**Nota:** ruim

- **A migração da base apagada e o Flyway removido do código.** Ver `changes.patch`, o arquivo `src/main/resources/db/migration/V1__create_tables.sql` apagado, e `src/main/java/hotel/Database.java:17-31` e `:45-49`. O `CREATE TABLE IF NOT EXISTS` não acrescenta colunas a uma tabela que já existe. Conferi num banco criado pela base, com uma reserva dentro: a final sobe, mas a consulta e a criação de reservas respondem 500 (`ERROR: column "company_cnpj" does not exist`). No sistema que a pousada já roda, a mudança quebra as reservas. O README ainda manda fazer a mudança de esquema como uma migração `V2__...sql` (`README.md:20-21`), e o `pom.xml` ainda tem o Flyway (`pom.xml:43-49`).
- **As restrições sumiram.** A tabela nova não tem a `EXCLUDE` que impedia duas reservas do mesmo quarto na mesma noite, nem a chave estrangeira para `rooms`, os `NOT NULL`, o `CHECK (check_out > check_in)` e o `CHECK (guests >= 1)` que a base tinha. A reserva dupla agora só é barrada pelo `synchronized` de um processo (`reservations/ReservationService.java:29`, `:44-47`). O tratamento do código `23P01` saiu de `ReservationStore.add` (`ReservationStore.java:47-49`).
- **SQL montado com dado de fora.** `ReservationStore.java:34-41` junta nome, e-mail, CPF, CNPJ, razão social, passaporte e país dentro de aspas, sem `?`, e executa com `Statement` (`:43-44`). Conferi: `"guestName": "John O'Neil"` dá 500 (`Unterminated string literal`), e qualquer campo desses aceita injeção. A base usava `PreparedStatement` com parâmetros.

O resto continua como na base: o pool (Hikari), o try-with-resources e as outras consultas com `?` (`ReservationStore.java:57-114`).

## Comportamento

**Números:** 83 de 83 conferências passaram. Foram 46 de 46 na parte comum, 3 de 3 depois de reiniciar, 32 de 32 no cenário 05 e 2 de 2 nas reservas simultâneas (`measures/behavior.md`).
**Nota:** bom

Partindo de um banco vazio, o sistema faz tudo o que o texto descreve. Com CPF, sai o comprovante de sempre. A empresa recebe a fatura, com a empresa no lugar do CPF e o ISS retido de R$ 38,99, dentro do e-mail em português e também no check-out. O estrangeiro recebe o booking receipt, com `BRL 779.80`, as datas no formato 2027-03-10, "1 night", a linha "Discount" e `BRL 2,783.52`. As recusas por nenhum jeito, dois jeitos ou um jeito incompleto dão 400 `INVALID_REQUEST`, e os campos que não se aplicam vêm `null`. As duas reservas simultâneas foram barradas, mas só pelo `synchronized` de um processo.

Achei dois defeitos fora do roteiro. Eles estão contados na coluna do banco, sem mudar esta nota:
- No banco que a pousada já usa, a versão nova quebra a consulta e a criação de reservas (500).
- Um hóspede com apóstrofo no nome (O'Neil, D'Ávila) não consegue reservar (500).

## Dúvidas

- A forma usada no design foi a fábrica concentrada (uma classe que escolhe o documento pelo tipo de hóspede), e não o Factory Method clássico com criador sobrescrito, nem um método polimórfico no `Guest`.
- Não verifiquei se a injeção pelo `INSERT` chega a executar um segundo comando. A tentativa que fiz deu 500 sem efeito visível, mas o SQL montado com o texto do hóspede basta para a nota do banco.
