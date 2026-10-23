# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin em pacotes por assunto: `hotel.web` (rotas, `HttpApi`), `hotel.reservations` (regras em
`ReservationService`, SQL em `ReservationStore`), `hotel.documents` (o comprovante), `hotel.messages`, `hotel.reports`
e `hotel.rooms`, com a montagem em `hotel.Hotel`. Os dados ficam no PostgreSQL, com o esquema criado por migrações do
Flyway. Uma requisição entra pelo `HttpApi`, que lê o corpo e chama o `ReservationService`, que usa o `ReservationStore`
e monta o `ReservationReceipt` com `new` em três lugares (`ReservationService.java:64`, `:109`, `:118`). Para esta
mudança, o agente alargou o `Guest` com quatro campos que podem ser nulos (`Guest.java:7-14`), validou o "exatamente
um jeito" na borda HTTP (`HttpApi.java:132-153`), acrescentou uma migração nova com um CHECK
(`V2__guest_identification.sql`) e pôs a escolha do documento dentro do próprio record `ReservationReceipt`, com `if`s
sobre quais campos do hóspede são nulos (`ReservationReceipt.java:62-111`). Tudo funciona: 31 testes passam e as 83
conferências do roteiro também. O problema mais sério é de design: não existe um tipo de documento, nem um tipo de
hóspede. A variação ficou em condicionais espalhadas dentro de uma classe só, e a agência de turismo que o dono já
anunciou vai ter de mexer em vários desses `if`s.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, um ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem os
mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo (`measures/dependencies-base.md`).
**Nota:** bom

O agente não acrescentou nenhuma dependência entre pacotes, e o ciclo `documents` ↔ `reservations` vem da base: o
serviço cria o comprovante (`ReservationService.java:3`), e o comprovante lê a `Reservation`. A única dependência nova
dentro desse ciclo já existente é o `PriceCalculator.cents`, que passou a ser `public`
(`PriceCalculator.java:50`) para o cálculo do ISS (`ReservationReceipt.java:107`). A borda continua fina: o
`HttpApi` só lê o corpo e transforma a identificação num `Guest` (`HttpApi.java:132-153`). Esse ponto é validação de
entrada, como já era a dos campos obrigatórios na base (`HttpApi.java:116-125`). O SQL novo ficou no
`ReservationStore` (`ReservationStore.java:32-55`, `:129-137`), e a regra do ISS ficou no pacote `documents`, sem
Javalin nem JDBC (tabela de bibliotecas em `measures/dependencies.md`). As classes cresceram em tamanho razoável
(`HttpApi` foi de 170 para 193 linhas, `ReservationReceipt` de 55 para 131).

## Design de baixo nível

**Números:** 3 tipos de documento (o comprovante, a fatura e o booking receipt), nenhum em estrutura própria. Há um
único record `ReservationReceipt` com 17 campos, e o tipo de hóspede é deduzido 4 vezes por `!= null`
(`ReservationReceipt.java:71`, `:74`, `:78`, `:85`, este último usado de novo em `:89` e `:106`). Os 3 lugares que
emitem o documento continuam chamando o mesmo construtor.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede um documento por tipo de hóspede e avisa que vem um quarto (a agência). A referência aceita
como acerto uma fábrica concentrada ou um método polimórfico no tipo de hóspede. O que o agente fez fica no meio do
caminho. Ponto a favor: os três lugares (`ReservationService.java:64`, `:109`, `:118`) não perguntam qual é o tipo de
hóspede, porque chamam `new ReservationReceipt(reservation, room)`, que já era o ponto único de criação na base. Mas
não existe documento a escolher. Existe um record só, e a variação foi distribuída em condicionais campo a campo:
`titleFor` (`ReservationReceipt.java:70-75`) decide o título, `textFor` (`:77-82`) decide a língua, e `portugueseText`
pergunta outra vez se é empresa para a linha de identificação (`:89-94`) e para o ISS (`:106-109`). O tipo de hóspede
nem existe como tipo: é deduzido de qual campo não é nulo (`isCompany`/`isForeigner`, `:62-68`). Para pôr a agência, é
preciso mexer em `titleFor`, `textFor`, provavelmente em `portugueseText`, além de `Guest`, `HttpApi.readGuest`, o
store, o response e o CHECK. Pelo critério geral da skill ("a variação ficou num `if`"), isso é omissão. A dúvida
sobre a classe está em "Dúvidas".

**O exagero.** Não há estrutura sem necessidade. As fábricas estáticas `Guest.ofCompany`/`Guest.ofForeigner`
(`Guest.java:21-27`) são usadas e deixam a construção legível.

**O resto do código.**
- O `Guest` virou um record com sete campos, dos quais cinco podem ser nulos (`Guest.java:7-14`), sem nada que garanta
  o "exatamente um" no domínio. A invariante só existe na borda (`HttpApi.java:133-145`) e no CHECK do banco. Quem
  criar um `Guest` pelo construtor canônico consegue um hóspede inválido, que só falha no INSERT.
- A decisão do tipo de hóspede aparece em dois lugares independentes, com critérios diferentes: o `HttpApi` decide
  pelos campos do corpo (`HttpApi.java:146-152`), e o comprovante decide por `companyCnpj != null` e
  `passport != null` (`ReservationReceipt.java:62-68`).
- `portugueseText` e `englishText` (`ReservationReceipt.java:84-130`) repetem a mesma sequência de linhas com
  formatadores diferentes. É aceitável para dois idiomas, mas cresce mal.
- Pontos bons: dinheiro em `BigDecimal`, com o ISS arredondado pela mesma regra do resto
  (`ReservationReceipt.java:35`, `:107`); formatadores novos e pequenos em `TextFormat.java` (`englishMoney`,
  `isoDate`); `text` virou um campo do record e por isso sai no JSON, como o texto pede.

## Testes

**Números:** 31 testes rodaram, 0 falharam, 0 pulados; 31 métodos escritos (a base tinha 26, todos mantidos, e o
patch só acrescenta). Cobertura de linhas: 94,8% (548/578; na base, 93,8%). Cobertura de ramos: 89,5% (154/172; na
base, 87,1%). O pacote `hotel.documents` tem 74/74 linhas e 22/22 ramos cobertos, e `hotel.web` foi de 18/28 para
53/66 ramos.
**Nota:** bom

O agente testou o que mudou, com valores:
- A fatura inteira, linha a linha, com o ISS de R$ 38,99 e o texto dentro do e-mail
  (`ReservationServiceTest.java:171-191`). Trocar 0.05 por outro valor, ou tirar a linha da empresa, derruba esse
  teste.
- O booking receipt inteiro (`ReservationServiceTest.java:194-207`), e o caso de uma noite com desconto ("1 night",
  "Discount: BRL 58.48") (`:210-218`).
- Pela API, os campos nulos na reserva e no documento, o título e o `text` (`HttpApiTest.java:131-152`), além de cinco
  recusas: nenhuma identificação, CNPJ sem razão social, passaporte sem país, CPF com CNPJ e CPF com passaporte
  (`HttpApiTest.java:155-167`).

Ficaram sem rede: o separador de milhar do `BRL 1,234.56` (nenhum valor testado passa de mil; trocar `%,.2f` por
`%.2f` em `TextFormat.java` não derruba teste nenhum), o check-out com a fatura ou o booking receipt (só a segunda via
é testada para os tipos novos) e a combinação empresa com passaporte. São lacunas pequenas: o roteiro do comportamento
confirma esses pontos.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice;
nenhuma data em texto. Duas migrações: `V1` igual à da base, `V2__guest_identification.sql` nova. As colunas de
situação sem CHECK (`status`, `rate_plan`, `type`, `channel`) vêm da base.
**Nota:** bom

O agente fez a mudança do jeito certo: uma migração nova (`V2__guest_identification.sql:3-15`), sem editar a `V1`. Ela
tira o `NOT NULL` do `guest_cpf`, acrescenta as quatro colunas e protege a regra "exatamente um jeito" com um CHECK
(`V2__guest_identification.sql:9-15`). Isso compensa, no banco, a falta da invariante no `Guest`. As reservas que já
existem, todas com CPF, passam no CHECK. O SQL novo usa parâmetros (`ReservationStore.java:32-55`), e a leitura monta o
hóspede com todas as colunas (`ReservationStore.java:130-137`). Os candidatos a SQL montado com `+` só juntam
constantes (`COLUMNS`). A proteção contra duas reservas na mesma noite continua sendo a restrição EXCLUDE da base.

## Comportamento

**Números:** 83 de 83 conferências passaram. A parte comum passou em 46 de 46, os dados depois de reiniciar em 3 de 3,
o cenário 05 em 32 de 32 e as reservas simultâneas em 2 de 2.
**Nota:** bom

Nada falhou. Para o dono da pousada: o hóspede com CPF continua recebendo o mesmo comprovante; a empresa recebe a
fatura com a razão social, o CNPJ e o ISS retido de R$ 38,99 sobre R$ 779,80, no e-mail em português, no check-out e na
segunda via; o estrangeiro recebe o booking receipt em inglês, com `BRL 2,783.52`, datas `2027-03-10`, "1 night" e a
linha "Discount". As recusas (nenhuma identificação, CPF com CNPJ, CNPJ sem razão social, passaporte sem país)
respondem 400 INVALID_REQUEST. O que já funcionava continua funcionando, e os dados sobrevivem ao reinício.

## Dúvidas

- A classe do pattern. Escolhi `omissão` porque não há documento nem tipo de hóspede a escolher: a variação está em
  quatro `if`s sobre campos nulos dentro de um record só (`ReservationReceipt.java:70-109`). Uma leitura mais
  generosa diria que o construtor de `ReservationReceipt` funciona como uma "fábrica concentrada" (os três lugares não
  decidem nada, e toda a escolha mora numa classe só), o que contaria como `acerto` pela referência. Não é a forma do
  `if` repetido nos três lugares que a referência descreve como omissão. Nenhuma das três formas aceitas como acerto
  foi usada de fato: não há criador sobrescrito, nem fábrica que escolhe entre documentos, nem método polimórfico no
  hóspede.
