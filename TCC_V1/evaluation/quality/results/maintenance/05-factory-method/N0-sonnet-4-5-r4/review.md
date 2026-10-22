# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (rotas e JSON), `hotel.reservations` (regras, preço e o
`ReservationStore` com o JDBC), `hotel.documents` (o comprovante), `hotel.messages`, `hotel.reports`, `hotel.rooms` e o
`hotel` da montagem. Os dados ficam no PostgreSQL, com o esquema criado por migrações do Flyway. Uma requisição entra
em `HttpApi`, vai ao `ReservationService` e chega ao `ReservationStore`. O agente acrescentou à reserva quatro colunas
de identificação (migração nova `V2__add_guest_identification_types.sql`) e criou a interface `ReservationDocument`
com três documentos: o `ReservationReceipt` da base, o `InvoiceDocument` e o `BookingReceiptDocument`. A escolha do
documento fica num método de fábrica só (`ReservationDocument.create`), e os três lugares que emitem o documento
chamam esse método. As 83 conferências do roteiro passam, e os 30 testes também. O que há de mais sério é pequeno:
- a regra "um jeito de identificação, e só um" deixa passar o CPF junto com um CNPJ ou um passaporte incompleto, e o
  dado a mais some sem aviso;
- os três documentos repetem os mesmos 16 campos e quase o mesmo texto;
- os testes novos não conferem o valor do ISS nem o formato do dinheiro em inglês.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, na `final` e na base. Um ciclo, `hotel.documents` ↔
`hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). As maiores classes são `HttpApi`, de
170 para 185 linhas, e `ReservationStore`, de 143 para 156.
**Nota:** bom

As camadas continuam como na base:
- **Borda HTTP fina:** as rotas novas só embrulham o documento num `DocumentResponse` (`src/main/java/hotel/web/HttpApi.java:76-79`).
- **SQL no lugar:** as colunas novas ficam só no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:18-20,32-55,130-137`).
- **Serviço sem Javalin e sem SQL:** o serviço só troca o `new ReservationReceipt` pela fábrica (`src/main/java/hotel/reservations/ReservationService.java:64,109,118`).

A validação de "qual identificação veio" mora no handler (`src/main/java/hotel/web/HttpApi.java:127-141`), no mesmo
lugar e do mesmo jeito que a base já validava os campos obrigatórios (`HttpApi.java:116-125`). É leitura da
requisição, e não uma regra de negócio escondida.

O ciclo vem da base e não piorou: `ReservationService` já importava o documento, e agora importa a interface. O
documento passou a usar `PriceCalculator.cents`, que virou público (`src/main/java/hotel/reservations/PriceCalculator.java:50`).
Essa ligação é entre os mesmos dois pacotes, então não cria aresta nova.

## Design de baixo nível

**Números:** 3 tipos de documento, os 3 dentro da estrutura (o da base inclusive). A escolha está num `switch` só
(`ReservationDocument.create`), e os 3 lugares que emitem o documento usam a fábrica: confirmação, check-out e
segunda via. Nenhum `new` de documento fora dela.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A criação está concentrada num método de fábrica estático da interface
(`src/main/java/hotel/documents/ReservationDocument.java:46-52`). Ele escolhe o documento pelo tipo do hóspede, e os
três lugares pedem o documento a ele (`src/main/java/hotel/reservations/ReservationService.java:64,109,118`). O e-mail
recebe a interface e chama `document.text()` sem perguntar qual é o caso (`ReservationService.java:125-130`). Cada
documento sabe escrever o seu texto (`InvoiceDocument.java:49-66`, `BookingReceiptDocument.java:49-64`,
`ReservationReceipt.java:48-62`). Para o documento das agências bastam um tipo novo de hóspede, uma classe nova e um
`case` novo, todos num lugar só. É uma "fábrica concentrada", que a referência aceita como acerto.

**O exagero.** Não há estrutura sobrando:
- a interface tem três implementações;
- o `DocumentResponse` (`src/main/java/hotel/web/DocumentResponse.java:28-47`) é preciso para pôr o `text` no JSON,
  porque o `text()` não é componente do record.

**O resto do código** tem defeitos localizados:
- **Duplicação:** os três records repetem os mesmos 16 componentes, e cada construtor preenche com `null` os campos
  que não se aplicam (`InvoiceDocument.java:11-46`, `BookingReceiptDocument.java:11-46`, `ReservationReceipt.java:10-46`).
  O `text()` da fatura copia o do comprovante linha a linha, menos a do hóspede (`InvoiceDocument.java:49-62` contra
  `ReservationReceipt.java:48-61`). Um campo novo no documento obriga a mexer em cinco lugares (os três records, a
  interface e o `DocumentResponse`).
- **O hóspede como record plano:** o `Guest` virou um record de sete campos, quase todos podendo ser nulos
  (`src/main/java/hotel/reservations/Guest.java:3-10`). O tipo dele é deduzido pelos nulos, e `FOREIGNER` é o que
  sobra (`Guest.java:24-32`): um hóspede sem nenhuma identificação vira "estrangeiro" em silêncio. Quem protege isso é
  o handler e o CHECK do banco, não o tipo.
- **Construtores inconsistentes:** dois construtores com `String` em posição ambígua e uma fábrica estática para o
  terceiro caso (`Guest.java:12-22`).
- **O ISS fora do domínio:** o imposto é calculado dentro do `text()` (`InvoiceDocument.java:63`), com o 0.05 solto
  no código. Ele não aparece no documento como valor e, por isso, fica fora do JSON.

## Testes

**Números:**
- `final`: 30 testes rodaram, 0 falhas, 0 pulados (30 métodos escritos); cobertura de 94,7% das linhas (590/623) e
  85,4% dos ramos (134/157). `hotel.documents` tem 94/96 linhas e 11/15 ramos.
- base: 26 testes, 93,8% das linhas e 87,1% dos ramos.

**Nota:** com problemas

**Os testes da base** continuam todos e passam. O único mexido em `ReservationServiceTest` só trocou o tipo
(`src/test/java/hotel/reservations/ReservationServiceTest.java:141-145`) e continua conferindo o total 779.80.

**Os quatro testes novos** são todos pela API (`src/test/java/hotel/web/HttpApiTest.java:124-197`):
- a empresa com a fatura;
- o estrangeiro com o booking receipt;
- a recusa de CPF com CNPJ;
- a recusa sem identificação.

**As asserções da parte nova são fracas**:
- o ISS é conferido só pela presença do rótulo, `contains("ISS retido (5%):")` (`HttpApiTest.java:145`), sem o
  valor R$ 38,99;
- o dinheiro em inglês é conferido só por `contains("BRL")` (`HttpApiTest.java:171`).

**A pergunta da mutação** encontra regras sem rede:
- trocar `0.05` por `0.5` em `InvoiceDocument.java:63` não derruba nenhum teste;
- trocar `Locale.US` por `pt-BR` em `TextFormat.moneyEnglish` também não;
- trocar `" night"` por `" nights"` no caso de uma noite também não.

**Ficaram sem teste:**
- o documento dentro do e-mail de confirmação e na segunda via, para os tipos novos;
- a linha "Discount";
- as recusas de identificação incompleta (CNPJ sem razão social, passaporte sem país);
- os campos nulos no JSON do documento.

Não há teste de unidade dos documentos: a regra só é vista pela API, e mal conferida.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`, chave estrangeira com índice, datas em
`date`. As migrações estão assim: V1 igual à da base, V2 nova. As colunas de situação e de plano sem CHECK
(`status`, `rate_plan`, `type`, `channel`) vêm da base. A duplicação de reserva é protegida por um EXCLUDE.
**Nota:** bom

**A mudança no esquema** veio numa migração nova, sem editar a V1
(`src/main/resources/db/migration/V2__add_guest_identification_types.sql:3-13`). A migração:
- tira o `NOT NULL` do CPF;
- acrescenta as quatro colunas;
- cria um CHECK que exige exatamente uma identificação completa (`V2__...sql:9-13`).

As reservas que já existem passam no CHECK, porque todas têm CPF. O banco de quem já roda o sistema sobe sem perder
nada.

**O SQL** novo usa parâmetros (`src/main/java/hotel/reservations/ReservationStore.java:32-55`), e a leitura monta o
hóspede com as colunas novas (`ReservationStore.java:130-137`).

**Um detalhe do CHECK:** ele aceita um CPF com `company_name` ou `guest_country` preenchidos, porque só olha o CNPJ e
o passaporte nos outros ramos. Como o código nunca grava esses campos junto com o CPF, isso não chega a gerar dado
errado.

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam:
- a parte comum: 46/46;
- depois de reiniciar: 3/3;
- o cenário 05: 32/32;
- as reservas simultâneas: 2/2.

**Nota:** com problemas

Tudo o que o roteiro confere funciona:
- a fatura, com a empresa no lugar do CPF e o ISS de R$ 38,99;
- o booking receipt, com `BRL 2,783.52`, as datas em `2027-03-10`, "1 night" e "Discount";
- o e-mail em português com o documento certo;
- o check-out e a segunda via;
- os campos nulos no JSON;
- as recusas de nenhuma identificação, CPF com CNPJ, CNPJ sem razão social e passaporte sem país.

**O defeito fora do roteiro** está na regra "um desses jeitos, e só um". Chamei a API com o serviço de pé, e dois
pedidos com uma identificação completa e outra incompleta foram aceitos com 201:
- CPF com um `companyCnpj` sem `companyName`;
- CPF com um `guestCountry` sem `guestPassport`.

A reserva foi gravada como de pessoa com CPF, e o CNPJ ou o país foram descartados sem aviso. A causa está em
`src/main/java/hotel/web/HttpApi.java:129-132`: a empresa e o passaporte só "contam" quando os dois campos vêm
juntos, então o campo solto não impede o caminho do CPF. Para o dono da pousada, isso quer dizer o seguinte: se o
site de uma empresa mandar o CPF do funcionário e o CNPJ sem a razão social, a reserva passa, e a empresa recebe um
comprovante de pessoa física em vez da fatura que pediu, sem nenhum erro que avise o site.

## Dúvidas

- **A forma do pattern:** é uma fábrica concentrada, um método estático `create` na própria interface
  `ReservationDocument` com um `switch` sobre `Guest.GuestType`. Não é a forma clássica, com um criador
  sobrescrito, nem um método polimórfico no hóspede. A referência aceita essa forma como acerto.
- **A nota do comportamento:** o roteiro passou inteiro. A nota `com problemas` vem do caso que conferi à mão (CPF com
  identificação incompleta aceito, e o dado a mais descartado). O texto diz "e só um", e a referência do cenário diz
  que um jeito incompleto deve ser recusado. Ainda assim, quem ler o texto como "exatamente um jeito completo" pode
  achar o defeito menor e dar `bom`.
