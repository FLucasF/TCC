# Revisão: manutenção, cenário 05

## Visão geral

O projeto manteve a forma da base: `hotel.web` (Javalin, `HttpApi` lê o JSON e chama o serviço), `hotel.reservations`
(o `ReservationService` com as regras e o `ReservationStore` com o SQL por JDBC), `hotel.documents` (o comprovante),
`hotel.messages` (o e-mail gravado no banco), `hotel.reports` e `hotel.rooms`, montados em `hotel.Hotel`. Os dados
ficam no PostgreSQL, com o esquema criado pelo Flyway. Para atender o pedido, o agente acrescentou ao `Guest` os
campos da empresa e do passaporte, uma migração nova (`V2__guest_identification.sql`) com uma restrição de "um jeito
só", a validação na borda HTTP e uma classe `DocumentTexts` com o texto de cada documento. O comportamento está todo
certo: 83 de 83 conferências passaram. O defeito mais sério é de design: a escolha de qual documento emitir (um
`if / else if / else` sobre o tipo de hóspede) foi copiada igual nos três lugares que emitem o documento
(`ReservationService.java:67-76`, `:123-132`, `:144-153`). É exatamente o ponto que o pedido avisava que vai crescer
("agência de turismo... vão precisar de um documento próprio"), e o quarto tipo vai exigir mexer nas três cópias.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`); na base, os
mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. Maiores classes: `HttpApi` 184 linhas (170 na base),
`ReservationService` 174 (138), `ReservationStore` 150 (143).
**Nota:** bom

O agente não mudou a forma do sistema. A borda HTTP continua fina: `HttpApi` só lê o corpo, valida os campos e monta
o `NewReservation` (`src/main/java/hotel/web/HttpApi.java:109-140`). A regra nova de identificação (um jeito só, sem
um par incompleto) fica junto da validação de campos obrigatórios que a base já fazia ali
(`HttpApi.java:126-134`). Isso é validação de entrada, e o banco garante a mesma regra com um CHECK
(`V2__guest_identification.sql:8-11`). O SQL novo continua só no `ReservationStore` (`ReservationStore.java:33-34`,
`:51-54`, `:129-131`), e o serviço não conhece nem o Javalin nem o JDBC. O ciclo `documents` ↔ `reservations` já
existia na base (o `ReservationReceipt` lê um `Reservation` e o serviço cria o recibo). O agente o manteve com uma
dependência nova do mesmo tipo (`ReservationService.java:3` importa `DocumentTexts`, que lê `Reservation`), mas não
criou ciclo novo. O `ReservationService` cresceu 36 linhas, e quase todas vêm das três cópias da escolha do
documento. Isso pesa no design, não aqui.

## Design de baixo nível

**Números:** 3 tipos de documento (comprovante, fatura, booking receipt); 3 lugares que emitem o documento (`confirm`,
`checkOut`, `receiptCopy`); 3 cópias do mesmo `if/else if/else` sobre o tipo de hóspede e nenhum lugar único de
criação. Não há classe nova além de `DocumentTexts` (3 métodos estáticos de texto).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O pedido descreve três documentos que saem em três lugares e avisa que vem um quarto (o das agências).
A resposta esperada era concentrar a escolha de qual documento criar num lugar só. Pode ser uma fábrica, um método
de fábrica ou um método polimórfico no tipo de hóspede, e os três lugares pediriam o documento a ele. O agente fez o
contrário: a decisão `isCompany() / isForeigner() / senão` aparece copiada, linha por linha, em
`ReservationService.java:66-76` (confirmação), `:121-133` (check-out) e `:142-154` (segunda via). Os textos foram
separados em funções auxiliares (`DocumentTexts.java:15`, `:33`, `:55`), mas a referência do pattern diz que isso
não muda a classe: o que importa é onde se decide qual documento criar, e essa decisão está repetida em três
lugares. É **omissão**. Para pôr o documento das agências, alguém terá de mexer nas três cópias e acertar o título
em cada uma. O título também está repetido como texto solto em cada ramo (`"Fatura de hospedagem"` em `:68`, `:124`
e `:145`), separado do texto que já começa com ele (`DocumentTexts.java:35`), e nada garante que os dois concordem.

**Exagero.** Não há estrutura sem necessidade. Nenhuma interface, fábrica ou camada nova; o `Guest` ganhou só os
campos e dois testes de tipo (`Guest.java:20-26`). Contenção.

**O resto do código.**
- Duplicação também nos textos: `personReceipt` e `companyInvoice` repetem as linhas de quarto, datas, diárias,
  desconto e total (`DocumentTexts.java:20-29` e `:40-48`). Na base, essas linhas moravam num só `text()`.
- O ISS é calculado dentro do método de texto, com o 5% como literal solto e o arredondamento repetido à mão
  (`DocumentTexts.java:49-50`). Não usa o `PriceCalculator.cents` (`PriceCalculator.java:50-52`), nem é uma
  constante com nome, como a base faz com `NON_REFUNDABLE_DISCOUNT` (`PriceCalculator.java:17`). O resultado é
  certo (HALF_EVEN, como o resto do sistema), mas a regra de dinheiro ficou escondida no texto e não aparece no JSON
  do documento.
- O tipo de hóspede é deduzido de qual campo está preenchido (`Guest.java:20-26`), num record com quatro campos
  anuláveis. Não há um tipo próprio (um `enum` ou um tipo por identificação) que diga qual é o caso. Um `Guest` com
  CNPJ e CPF juntos, criado fora da API, viraria fatura sem aviso, e a validação mora só na borda HTTP
  (`HttpApi.java:126-134`).
- O que está bem: dinheiro em `BigDecimal`, datas em `LocalDate`, o formato `BRL 1,234.56` com `Locale.US`
  (`DocumentTexts.java:74-76`), e os `null` dos campos vazios tratados na entrada (`HttpApi.java:181-183`).

São defeitos localizados, mas a duplicação está justamente no ponto que o pedido diz que vai mudar.

## Testes

**Números:** 32 testes rodaram, 0 falhas, 0 erros, 0 pulados; 32 métodos escritos (26 na base, mais 6 novos: 5 em
`DocumentsByGuestTest` e 1 em `HttpApiTest`). Cobertura de linhas: 94,2% (557 de 591; base 93,8%). Ramos: 88,1%
(141 de 160; base 87,1%). Em `hotel.documents`: 69 de 70 linhas e 10 de 12 ramos.
**Nota:** bom

Os 26 testes da base continuam lá, sem mudança, e passam: o patch só acrescenta a `HttpApiTest.java`. Os novos
conferem a parte nova com valores, contra o banco de verdade:
- a fatura inteira, texto exato, com o ISS de R$ 38,99 e os campos nulos que não se aplicam
  (`src/test/java/hotel/documents/DocumentsByGuestTest.java:52-72`);
- a fatura no e-mail de confirmação (em português) e no check-out (`DocumentsByGuestTest.java:75-84`);
- o booking receipt inteiro, texto exato, e no e-mail (`DocumentsByGuestTest.java:87-105`);
- "1 night", a linha `Discount: BRL 58.48`, o total com desconto e o milhar `BRL 2,783.52`
  (`DocumentsByGuestTest.java:108-116`);
- a pessoa com CPF continua recebendo o comprovante (`DocumentsByGuestTest.java:119-128`);
- pela API, as recusas (nenhuma identificação, CPF+CNPJ, CPF+passaporte, CNPJ sem razão social, passaporte sem país)
  com 400 INVALID_REQUEST, e os campos nulos no JSON (`src/test/java/hotel/web/HttpApiTest.java:109-128`).

A pergunta da mutação: trocar 0.05 por 0.06, apagar a linha do ISS, trocar `night` por `nights` ou o `Locale.US`
quebraria um teste. Ficam sem rede: o arredondamento do ISS (38,99 é exato, então HALF_EVEN, HALF_UP ou truncar dão
o mesmo resultado); a fatura com desconto; o booking receipt no check-out (os 2 ramos não cobertos de `documents`);
uma razão social sem CNPJ. Nenhum teste confere pela API o campo `text` no JSON do documento, mas o roteiro do
comportamento confere. As lacunas são pequenas, e as regras novas estão testadas com valores.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice;
nenhuma data em texto; 2 migrações (V1 igual à base, V2 nova); SQL só com parâmetros. Colunas de situação sem CHECK
(`status`, `rate_plan`, `type`, `channel`), herdadas da base.
**Nota:** bom

O agente fez a mudança do jeito certo: uma migração nova, sem tocar na V1
(`src/main/resources/db/migration/V2__guest_identification.sql:2-11`). Ela tira o `NOT NULL` do `guest_cpf`, cria as
quatro colunas e acrescenta um CHECK de "exatamente uma identificação" (`:8-11`). As reservas que já existem têm
todas CPF e passam no CHECK, então um banco em uso sobe sem problema. O INSERT e a leitura usam `?` e incluem as
colunas novas (`ReservationStore.java:33-34`, `:51-54`, `:129-131`), e o reinício mantém os dados (3 de 3 no
roteiro). A proteção contra reserva dupla (o EXCLUDE) vem da base e continua lá. Um detalhe: o CHECK só conta os
pares completos, então aceita um CPF com um CNPJ solto, sem razão social (`V2__guest_identification.sql:9-11`). A
API recusa esse caso antes de chegar ao banco (`HttpApi.java:129-131`), por isso não pesa na nota.

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam. Parte comum: 46 de 46. Dados depois de reiniciar: 3 de 3.
Cenário 05: 32 de 32. Duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API. A pessoa com CPF continua recebendo o mesmo comprovante. A empresa recebe
a fatura com a razão social e o CNPJ no lugar do CPF e o ISS retido de R$ 38,99, no e-mail (que continua em
português), no check-out e na segunda via. O estrangeiro recebe o booking receipt em inglês, com `BRL`, datas
`2027-03-10`, "1 night", a linha `Discount` e o milhar `BRL 2,783.52`. As combinações erradas de identificação dão
400 INVALID_REQUEST. Nada do que já funcionava quebrou, e os dados sobrevivem ao reinício. Fora do roteiro, uma
observação que não muda a nota: o README não foi atualizado e ainda mostra o `guestCpf` como o único jeito de
identificar o hóspede (`README.md:66`). Isso não muda o que o sistema faz.

## Dúvidas

- Classe do pattern: o caso é claro pela referência (o `if` repetido nos três lugares, com o texto em funções
  auxiliares, é omissão). Fica registrado que existe uma forma parcial de concentração, a `DocumentTexts`, que separa
  o texto mas não a escolha.
- Arquitetura: a regra de "um jeito só" ficou na borda HTTP, e não no domínio. Dei `bom` porque a base já validava os
  campos obrigatórios ali e o banco garante a mesma regra. Um revisor mais rígido poderia pedir que o
  `ReservationService.create` também a conferisse.
