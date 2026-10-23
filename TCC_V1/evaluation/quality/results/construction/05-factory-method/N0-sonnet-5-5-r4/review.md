# Revisão: construção, cenário 05

## Visão geral

O projeto é um pacote só (`br.com.marealta`, 10 arquivos, 956 linhas), dividido por papel: `Main` monta as peças
(`Main.java:8-10`), `Api` é a borda Javalin (lê o JSON, valida, chama o serviço e escreve a resposta),
`ReservationService` tem as regras (situação da reserva, cancelamento e devolução, relatórios), `Pricing` e `Money`
cuidam do preço e do dinheiro, `Documents` escolhe e monta o documento do hóspede, `Store` concentra todo o SQL e
`Db` cria o esquema e o pool HikariCP. Uma requisição como `POST /reservations/{id}/confirmation` vai de
`Api.java:32` para `ReservationService.confirm` (`ReservationService.java:55-66`), que abre uma transação em
`Store.tx` (`Store.java:25-39`), trava a reserva com `FOR UPDATE`, atualiza, pede o documento a `Documents.render` e
grava o e-mail na tabela `messages`. Os dados ficam no PostgreSQL (3 tabelas criadas por `CREATE TABLE IF NOT EXISTS`
em `Db.java:12-59`). Nada é grave: as 83 conferências do comportamento passam, os 12 testes passam com 91,7% de
cobertura de linhas, e o Factory Method está como devia, numa fábrica concentrada. O que pesa: tudo num pacote só,
com o JDBC (`Connection`) e os códigos HTTP vazando para o serviço, e o esquema criado no código, sem migrações e sem
restrição nenhuma no banco.

## Arquitetura

**Números:** 1 pacote (`br.com.marealta`, 25 classes compiladas, 956 linhas), 0 dependências entre pacotes, nenhum ciclo. O único pacote usa Javalin, JDBC, Jackson e o pool. Maiores classes: `Api` 236 linhas, `Store` 228, `ReservationService` 153.
**Nota:** com problemas

As camadas existem como classes, mas não como pacotes, e por isso o jdeps não tem como mostrar a direção das
dependências. Pela leitura, ela está certa na maior parte. A borda é fina: os handlers só convertem e chamam o serviço
(`Api.java:29-39`), e a validação da entrada fica em `Api.parse` (`Api.java:69-107`). Todo o SQL mora em `Store`
(`Store.java`), o esquema mora em `Db` e a montagem acontece num lugar só (`Main.java:8-10`), sem `new` de acesso a
dados dentro do serviço e sem `static` global.

Os defeitos são localizados:
- o serviço conhece o JDBC: recebe `java.sql.Connection` e lança `SQLException` (`ReservationService.java:107`,
  `ReservationService.java:112`), e cada operação é uma lambda sobre a conexão passada para `store.tx`
  (`ReservationService.java:32`, `56`, `69`…). A transação, que é coisa de dados, fica entrelaçada com a regra;
- a regra conhece o HTTP: o serviço lança `ApiException` com o status HTTP dentro (404, 409, 422 em
  `ReservationService.java:34`, `36`, `40`; `ApiException.java:4-5`);
- a montagem do CSV, que é formato de saída, fica no serviço (`ReservationService.java:124-152`).

Num sistema deste tamanho isso não impede o crescimento. Mas, com tudo num pacote, nada impede que a próxima mudança
misture ainda mais as camadas.

## Design de baixo nível

**Números:** 3 tipos de hóspede e 3 documentos, todos na estrutura: uma única escolha (`switch` sobre a interface selada `Identification`, `Documents.java:20-27`), chamada pelos 3 lugares que emitem o documento (o e-mail de confirmação em `ReservationService.java:61`, o check-out em `ReservationService.java:92` e a segunda via em `ReservationService.java:103`, os dois últimos por `issue`, `ReservationService.java:107-110`). Nenhum lugar decide fora da fábrica qual documento criar.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A referência aceita como acerto uma fábrica concentrada que escolhe o documento pelo tipo de hóspede, e
é isso que existe. `Documents.render` é o único ponto que decide qual documento criar (`Documents.java:20-27`), e a
decisão é um `switch` exaustivo sobre a interface selada `Identification` (`Model.java:23-27`). Quando entrar a
agência, o compilador aponta o `switch` que precisa do novo caso. Cada documento tem o seu método (`receipt`,
`invoice`, `bookingReceipt`, `Documents.java:29-61`), e o corpo em português é compartilhado entre o comprovante e a
fatura (`addPtBody`, `Documents.java:63-71`). O e-mail recebe o documento pronto e só o embrulha
(`Documents.java:77-83`). O JSON do documento usa o `title` e o `text` que a fábrica devolveu (`Api.java:205-219`),
sem nova escolha.

O que afasta um pouco: outros lugares também perguntam o tipo de identificação, mas para serializar os campos, não para
escolher o documento. São `Api.putIdentification` (`Api.java:179-185`, cadeia de `instanceof`), `Store.insert`
(`Store.java:92-96`, `switch` exaustivo) e `Store.reservation` (`Store.java:187-194`, `if/else` que cai em `Passport`
por omissão). Um tipo novo mexe nesses três pontos, e dois deles o compilador não aponta. Isso não tira o acerto,
porque a criação do documento está num lugar só.

**O exagero.** Nenhuma estrutura sem necessidade: sem interfaces de uma implementação só, sem builders, sem camadas que
só repassam. A única interface própria, `Store.Work` (`Store.java:20-23`), existe porque a lambda precisa lançar
`SQLException`.

**O resto.** O código é enxuto e usa os tipos certos: `BigDecimal` com `HALF_EVEN` em cada passo (`Money.java:15-17`,
`Pricing.java:22-33`), `LocalDate` e `YearMonth` nas datas, `enum` em `Status` e `RatePlan` (`Model.java:9-18`),
records imutáveis e nenhum `static` mutável. Os erros vão por `ApiException`, e o 500 genérico registra a causa
(`Api.java:42-45`). Pontos pequenos que não chegam a pedir mudança antes de aceitar:
- o tipo do quarto é `String` (`Model.java:20`);
- a lista das situações pagas está repetida no SQL (`Store.java:157`), além de `Status.isPaid` (`Model.java:15-17`);
- `confirm` busca o quarto e chama a fábrica à mão em vez de usar `issue` (`ReservationService.java:61` contra
  `ReservationService.java:107-110`), uma duplicação de duas linhas;
- o `Reservation` de 16 campos é montado por posição (`ReservationService.java:43-45`).

## Testes

**Números:** 12 testes rodaram, 0 falhas, 0 erros, 0 pulados; 12 métodos escritos (7 em `DomainTest`, 5 em `ApiTest`). Cobertura de 420 de 458 linhas (91,7%) e de 133 de 168 ramos (79,2%). Há testes de unidade e testes pela API contra o PostgreSQL de `DB_URL`, num esquema temporário; nenhum mock.
**Nota:** bom

O texto não pedia testes, e os que existem conferem as regras com valores:
- o exemplo de alta temporada, 599,76 (`DomainTest.java:20-25`);
- as bordas da temporada: 14/12, 15/12, 29/02, 01/03, 30/06 e 01/08 (`DomainTest.java:27-35`);
- o desconto com meio para o par, 37,48 e 212,42 (`DomainTest.java:37-43`);
- os formatos de dinheiro (`DomainTest.java:45-50`).

A parte nova do cenário também é conferida pelo texto inteiro do documento:
- o comprovante e o e-mail (`DomainTest.java:52-72`);
- a fatura com desconto e ISS de R$ 33,14 (`DomainTest.java:74-87`);
- o booking receipt de "1 night" (`DomainTest.java:89-99`).

Pela API, o teste cobre:
- o ciclo inteiro, com 409 e 422, noites encostadas, e-mail e os dois CSV comparados por igualdade
  (`ApiTest.java:81-129`);
- as devoluções e o financeiro com a NON_REFUNDABLE cancelada (`ApiTest.java:131-157`);
- a fatura com ISS de R$ 57,99 e o booking receipt dentro do e-mail em português (`ApiTest.java:159-185`);
- as recusas da identificação (`ApiTest.java:196-197`);
- reservas simultâneas (`ApiTest.java:209-220`).

A pergunta da mutação: trocar o ISS de 0.05, trocar o `case Company` para o comprovante, ou trocar "night" e "nights"
derruba `DomainTest`, e trocar `>` por `>=` no ocupado derruba a ocupação comparada por igualdade. Ficam sem rede:
- a linha "Discount:" do booking receipt (nenhum teste usa NON_REFUNDABLE com passaporte);
- o plural "nights" no inglês;
- a igualdade entre o texto do check-out e o da segunda via para empresa e estrangeiro (o check-out só é testado com
  CPF e só pelo `title`, `ApiTest.java:121-123`).

Na validação, os testes conferem quase sempre só o status, não o código `error` (`ApiTest.java:189-205`; só
`ROOM_NOT_FOUND` é conferido, linha 200). São lacunas menores, diante da cobertura e da qualidade das asserções.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro todo em `numeric(12,2)`, datas em `date`, a chave estrangeira `reservations.room_number` com índice. O que as conferências acusaram: status, plano, tipo e canal em `varchar` sem CHECK, nenhuma restrição UNIQUE, EXCLUDE ou CHECK, e o esquema criado por `CREATE TABLE` no código (0 arquivos de migração). Há 5 candidatos a SQL montado com `+`, todos constantes.
**Nota:** com problemas

O que está certo:
- os tipos e as chaves (`Db.java:12-59`);
- o pool HikariCP (`Db.java:63-70`);
- o SQL sempre com `?`. Os candidatos do script são concatenações de constantes, e o único sufixo variável é
  `" FOR UPDATE"` ou `""` (`Store.java:54`, `58`, `129`);
- try-with-resources em toda conexão;
- uma transação por operação (`Store.java:25-39`), juntando a mudança de situação e o e-mail na confirmação
  (`ReservationService.java:59-63`).

A regra de um quarto por noite é protegida com uma trava real no banco: a criação trava a linha do quarto com
`SELECT … FOR UPDATE` antes de conferir a sobreposição (`Store.java:53-54`, `ReservationService.java:33-41`). Isso
vale entre processos, e o roteiro confirmou isso nas 10 rodadas.

Os defeitos:
- o esquema não evolui. São `CREATE TABLE IF NOT EXISTS` no código (`Db.java:12-59`), sem migração versionada, e a
  próxima coluna nova (a do documento da agência, por exemplo) não chega a um banco que já existe;
- o banco não protege nenhuma regra por conta própria. Não há `EXCLUDE` para as noites, `CHECK` de status, plano,
  `guests >= 1` ou `check_out > check_in`, nem `CHECK` de que só um jeito de identificação está preenchido
  (`Db.java:21-41`);
- a leitura cai em `Passport` quando nem CPF nem CNPJ estão gravados (`Store.java:192-193`). Uma linha inconsistente
  vira um passaporte nulo em silêncio.

Nada perde nem corrompe dado pelas rotas do sistema.

## Comportamento

**Números:** 83 de 83 conferências passaram. A parte comum passou 46 de 46, os dados depois de reiniciar 3 de 3, o cenário 05 32 de 32 e as reservas simultâneas 2 de 2.
**Nota:** bom

Nada falhou. Para o dono da pousada:
- os preços, a alta temporada, o desconto e o arredondamento saem como o texto pede;
- as recusas trazem os códigos combinados com o site;
- os relatórios em CSV saem no formato pedido;
- os dados sobrevivem a um reinício;
- duas reservas simultâneas do mesmo quarto nunca passam juntas.

Na parte nova:
- a pessoa com CPF recebe o comprovante;
- a empresa recebe a fatura com o ISS retido de R$ 38,99 sobre R$ 779,80, no e-mail em português, no check-out e na
  segunda via;
- o estrangeiro recebe o booking receipt em inglês, com "BRL 2,783.52", as datas 2027-03-10, "1 night" e a linha
  "Discount";
- nenhuma identificação, duas juntas ou uma incompleta são recusadas com 400 INVALID_REQUEST.

## Dúvidas

- A forma do Factory Method usada é a fábrica concentrada (um método estático com `switch` sobre a interface selada,
  `Documents.java:20-27`), não a forma clássica com criador sobrescrito nem um método polimórfico no tipo de hóspede.
  A referência aceita as três como acerto.
- Na arquitetura, a nota fica entre `com problemas` e `bom`. As camadas estão bem separadas em classes, mas há um
  pacote só, o JDBC vaza para o serviço e o status HTTP vaza para as regras. Fiquei com `com problemas` pela soma dos
  vazamentos, não por um defeito grave.
