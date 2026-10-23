# Revisão: manutenção, cenário 05

## Visão geral

O agente mudou a base por dentro das peças que ela já tinha, sem criar nenhum pacote nem classe nova (só a migração
`V2__guest_identification.sql`). Uma requisição continua passando por `hotel.web.HttpApi`, que lê o corpo. Agora há
também um `readGuest` (`HttpApi.java:132-151`) que aceita um, e só um, dos três jeitos de identificar o hóspede. Depois
ela vai ao `ReservationService` (regras) e ao `ReservationStore` (JDBC, PostgreSQL com Flyway). O `Guest` ganhou
quatro campos que podem ser nulos (`Guest.java:8-15`), e o banco ganhou quatro colunas e um CHECK que exige exatamente
uma identificação. O documento continua sendo um único record, `ReservationReceipt`, criado com o mesmo `new` nos três
lugares do serviço (`ReservationService.java:64,109,118`). A escolha entre comprovante, fatura e *booking receipt* fica
em condicionais dentro desse record: `titleOf` (`ReservationReceipt.java:55-60`) e `text()`
(`ReservationReceipt.java:64-69`). O comportamento está todo certo (83 de 83 conferências) e os testes passam. O ponto
mais fraco é o design pedido: em vez de concentrar a criação de documentos diferentes, a variação ficou num `if`, e um
quarto documento (o das agências) obrigaria a mexer em vários pontos do mesmo record e do `Guest`.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, na `final` e na `base`. Um ciclo, `hotel.documents` ↔
`hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). A maior classe é `HttpApi` (170 → 191
linhas). `ReservationReceipt` passou de 55 para 118 linhas.
**Nota:** bom

A mudança respeitou as camadas da base. A validação da identificação do hóspede ficou na borda HTTP
(`HttpApi.java:132-151`), no mesmo lugar onde a base já conferia os campos obrigatórios (`HttpApi.java:116-125`). Os
handlers continuam finos (`HttpApi.java:64-79`). O SQL novo ficou no `ReservationStore`
(`ReservationStore.java:32-55`, `130-137`), e o serviço não foi tocado. Não há ciclo novo. O agente só reforçou o ciclo
que veio da base: `hotel.documents` passou a usar também `PriceCalculator.cents`, que ficou público para isso
(`PriceCalculator.java:50`, `ReservationReceipt.java:4,85`). Há um detalhe menor: `hotel.documents` passou a depender
do Jackson por causa de um `@JsonProperty` no record de domínio (`ReservationReceipt.java:3,63`), o que mistura um
pouco a serialização HTTP com o documento. Nada disso pede mudança antes de aceitar.

## Design de baixo nível

**Números:** 3 tipos de documento, todos no mesmo record `ReservationReceipt`. A escolha é feita por condicionais em 2
lugares do record: `titleOf` (`:55-60`, por `guest.isCompany()`/`isForeigner()`) e `text()` (`:64-69`, por
`companyCnpj != null`/`guestPassport != null`). Nenhuma fábrica e nenhum tipo por documento. Os 3 pontos de criação
(`ReservationService.java:64,109,118`) continuam com o `new ReservationReceipt(...)` da base.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede que a escolha de *qual documento criar* fique num lugar só, já que vem um quarto
documento (o das agências). O que aproxima o código disso: os três lugares do serviço não perguntam qual é o caso,
porque todos chamam o mesmo construtor (`ReservationService.java:64,109,118`), e o texto de cada documento está num
método próprio (`receiptText`, `invoiceText`, `bookingReceiptText`, `ReservationReceipt.java:71-117`). O que afasta:
não há documentos diferentes nem nada que crie um ou outro. Há um só record de 16 componentes, com os campos de todos
os tipos, que pergunta o caso sempre que alguém lhe pede algo. A pergunta aparece duas vezes, com sinais diferentes:
na criação, para o título (`:55-60`), e depois, a cada chamada de `text()` (`:64-69`), olhando quais campos estão
nulos. O ISS só existe dentro do texto (`:85`). Para acrescentar o documento das agências seria preciso mexer em
`Guest` (mais campos e mais um `isX`), em `titleOf`, em `text()`, nos componentes do record, em `ReservationResponse`,
na tabela e no CHECK. É a variação resolvida em `if`/ternário, ou seja, omissão (veja as Dúvidas).

**O exagero.** Não há estrutura sobrando. As fábricas estáticas `Guest.ofCompany`/`ofForeigner` (`Guest.java:21-27`)
são pequenas e usadas.

**O resto do código.**
- O `Guest` virou uma união feita de campos de texto que podem ser nulos (`Guest.java:8-15`). A regra "exatamente um
  jeito" está só no comentário (`:3-7`), no `HttpApi.readGuest` e no CHECK do banco. O construtor canônico, que é
  público, aceita qualquer combinação. Por exemplo, `Guest.ofCompany(nome, email, null, "X")` gera um "Comprovante de
  reserva" com "CPF null", porque `isCompany()` só olha o CNPJ (`Guest.java:29-35`, `ReservationReceipt.java:59,74`).
- O tipo do hóspede é deduzido de campos nulos em vez de vir de um tipo próprio (`enum` ou `sealed`). Isso também
  explica por que `titleOf` e `text()` usam testes diferentes (`ReservationReceipt.java:56` contra `:65`).
- A anotação Jackson no record de domínio (`ReservationReceipt.java:63`).
- A disposição do texto em português e em inglês se repete em boa parte (`:90-100` e `:107-115`). Com idiomas
  diferentes, isso é aceitável.
- O `catch (Exception)` de `HttpApi.java:113` veio da base.
- Os tipos de dinheiro e de data estão certos (`BigDecimal` arredondado por `PriceCalculator.cents`, `LocalDate`), e
  os nomes são claros.

## Testes

**Números:** na `final`, 30 testes rodaram, 0 falharam, 0 pulados, e há 30 métodos escritos. Linhas: 541 de 571
(94,7%). Ramos: 152 de 170 (89,4%). Em `hotel.documents`: 67 de 67 linhas e 16 de 16 ramos. Na base eram 26 testes,
93,8% das linhas e 87,1% dos ramos. O agente manteve os 26 testes da base e escreveu 4 novos.
**Nota:** bom

Os testes novos conferem a parte nova com valores exatos:
- A fatura inteira, linha por linha, com o ISS de R$ 38,99 sobre R$ 779,80, também no e-mail
  (`ReservationServiceTest.java:106-123`).
- O *booking receipt* inteiro, com `BRL 779.80` e as datas `2027-03-10`, e o e-mail que continua em português
  (`ReservationServiceTest.java:126-141`).
- "1 night" e a linha `Discount: BRL 86.98` (`ReservationServiceTest.java:144-155`).
- Pela API: nove combinações inválidas, todas com 400 `INVALID_REQUEST` (nenhuma identificação, CNPJ ou passaporte
  incompleto, dois jeitos juntos). Também os campos que não se aplicam vindo `null` na reserva e no documento, o título
  "Fatura de hospedagem" e o `text` com o ISS de R$ 46,79 (`HttpApiTest.java:117-150`).

Pela pergunta da mutação: trocar 0.05, `nights == 1`, `" no. "`, a ordem dos campos ou o formato da data quebra
algum teste. Ficam sem rede:
- O separador de milhar do inglês (`TextFormat.java:26`). Com `%.2f` no lugar de `%,.2f`, nenhum teste falha, porque
  todos os valores ficam abaixo de mil.
- A fatura com desconto.
- O check-out que devolve a fatura ou o *booking receipt*. O risco é baixo, porque os três lugares usam o mesmo
  construtor.

São lacunas pequenas perto do que foi conferido.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)` e não há datas em texto.
Nenhuma chave estrangeira está sem índice. A V1 está igual à da base e a V2 é nova. Há 6 restrições, entre elas o CHECK
novo de identificação e o EXCLUDE contra duas reservas na mesma noite. As colunas `status`, `rate_plan`, `type` e
`channel` estão sem CHECK, mas isso veio da base (`measures/database.md`).
**Nota:** bom

A mudança de esquema veio numa migração nova (`V2__guest_identification.sql:2-14`), sem editar a V1. Ela tira o
`NOT NULL` do `guest_cpf`, acrescenta quatro colunas e protege a regra "exatamente um jeito" com um CHECK
(`:6-14`). Esse CHECK aceita as linhas que já existiam: todas têm CPF e as colunas novas nulas. O SQL continua todo
com parâmetros: o INSERT tem 18 `?` (`ReservationStore.java:32-55`), e o que entra por `+` são constantes
(`COLUMNS`, `ReservationStore.java:18-20`). A leitura monta o `Guest` com as colunas novas (`ReservationStore.java:130-137`).
A consulta que traz a tabela inteira (`ReservationStore.java:117`) veio da base.

## Comportamento

**Números:** 83 de 83 conferências passaram:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- o hóspede com CPF continua recebendo o mesmo comprovante;
- a empresa recebe a fatura, com a razão social, o CNPJ e o ISS retido de R$ 38,99, no e-mail (em português), no
  check-out e na segunda via;
- o estrangeiro recebe o *booking receipt* em inglês, com `BRL 2,783.52`, as datas `2027-03-10`, "1 night" e a linha
  "Discount";
- a reserva sem identificação, com dois jeitos ou com um jeito incompleto é recusada com 400 `INVALID_REQUEST`;
- os campos que não se aplicam vêm `null`, e o documento traz `text`.

Nada do que já funcionava quebrou, e os dados continuam lá depois de reiniciar.

## Dúvidas

- A classe do pattern ficou entre **omissão** e **malfeito**, com uma leitura possível de acerto. A referência define a
  omissão pelo `if` repetido nos três lugares, e aqui os três lugares chamam o mesmo construtor, sem `if`. Por outro
  lado, não existe estrutura de criação: há um único tipo de documento que decide o caso por condicionais, uma vez no
  título (`ReservationReceipt.java:55-60`) e outra no texto (`:64-69`). Se o construtor for lido como "a fábrica", ele
  devolve sempre o mesmo documento, e a escolha acontece depois, em `text()`, o que seria malfeito pela referência. A
  leitura mais favorável, de um "método que escolhe o documento pelo tipo de hóspede" (acerto, na forma de fábrica
  concentrada), não se sustenta, porque nenhum método escolhe entre documentos. Escolhi omissão porque a variação
  ficou, literalmente, num `if`/ternário, sem estrutura.
