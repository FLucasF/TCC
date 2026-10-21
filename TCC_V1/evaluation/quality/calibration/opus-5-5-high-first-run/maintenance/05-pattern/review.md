# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas e a leitura do JSON), `hotel.reservations` (as
regras em `ReservationService`, o preço em `PriceCalculator` e o SQL em `ReservationStore`), `hotel.rooms`,
`hotel.messages`, `hotel.reports`, `hotel.documents` (o comprovante) e `hotel` (a montagem e o Flyway). Uma
requisição entra em `HttpApi`, que valida o corpo e chama o `ReservationService`. O serviço usa o `ReservationStore`
(JDBC com `?`, pool) para gravar no PostgreSQL, e o esquema vem das migrações do Flyway. O agente pôs a escolha do
documento numa fábrica só (`DocumentFactory`), com um escritor por tipo de documento. Os três lugares que emitem o
documento pedem a ela. Os dados novos do hóspede ficam em colunas novas, criadas numa migração nova (`V2`); a `V1`
não foi tocada. A validação de "um jeito e só um" ficou na borda HTTP, e o banco também tem um CHECK para isso. O
roteiro do comportamento passou inteiro (83 de 83) e os 29 testes passam. Não há nada sério. Os defeitos são
menores: o texto dos três documentos copiado de um escritor para o outro, testes que não conferem a validação nova
nem parte do recibo em inglês, e um CHECK no banco que deixa passar um par incompleto quando outro jeito de
identificação está preenchido.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais na base e na final (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já estava na base.
`hotel.documents` passou de 2 para 7 classes (de 79 para 222 linhas), e `HttpApi` de 170 para 184 linhas.
**Nota:** bom

- As camadas continuam como a base as tinha. Só `hotel.web` usa o Javalin, e o JDBC fica em `ReservationStore`,
  `Rooms` e `SentMessages` (a tabela de bibliotecas do script). A borda HTTP continua fina: a checagem nova de
  identificação está em `src/main/java/hotel/web/HttpApi.java:126-137`, ao lado das checagens de campo obrigatório
  que a base já fazia ali (`HttpApi.java:116-125`), e o handler só chama o serviço (`HttpApi.java:64-67`).
- O SQL novo ficou no `ReservationStore`: `src/main/java/hotel/reservations/ReservationStore.java:18-20` (as
  colunas), `:32-54` (o INSERT com quatro parâmetros a mais) e `:128-131` (a leitura). Não há SQL fora dele.
- Os documentos ficaram no pacote `hotel.documents`, que não conhece nem o Javalin nem o JDBC. O serviço só chama
  `documents.issue(...)` (`src/main/java/hotel/reservations/ReservationService.java:66`, `:111`, `:120`).
- O ciclo `documents` ↔ `reservations` veio da base: lá, `ReservationReceipt` já lia `Reservation` e o serviço já
  criava `ReservationReceipt`. O agente não o piorou: o número de dependências não mudou.
- Um detalhe: o `DocumentFactory` é criado dentro do serviço (`ReservationService.java:19`) e não é injetado pela
  montagem. Como ele não tem estado nem dependências externas, isso não pesa.

## Design de baixo nível

**Números:** 3 tipos de documento, todos na estrutura: o comprovante da base, a fatura e o booking receipt. A escolha
está num lugar só (`DocumentFactory.writerFor`), e os 3 lugares que emitem o documento (o e-mail de confirmação, o
check-out e a segunda via) pedem à fábrica. Nenhum `new ReservationReceipt` sobrou fora dela, e nenhum `if` sobre o
tipo de hóspede aparece fora dela.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A forma é a fábrica concentrada que a referência aceita como acerto.
`src/main/java/hotel/documents/DocumentFactory.java:19-27` decide, num lugar só, qual `DocumentWriter` usar pelo
tipo de hóspede, e `DocumentFactory.java:14-17` monta o documento com o título e o texto daquele escritor. O caso que
já existia (o comprovante com CPF) também foi para a estrutura (`PersonReceiptWriter.java`). O texto que morava em
`ReservationReceipt.text()` na base saiu de lá, e o `record` virou só os dados mais o `text` pronto
(`ReservationReceipt.java:87-125`). Os três lugares de emissão (`ReservationService.java:66`, `:111`, `:120`) não
perguntam o tipo de hóspede. Para a agência de turismo, bastam um escritor novo e um ramo novo em `writerFor`.

**O exagero.** Não há. A interface `DocumentWriter` (`DocumentWriter.java:7-12`) tem três implementações reais e faz
parte do pattern. Não sobrou nenhuma camada que só repassa.

**O resto do código.** O que um revisor exigente apontaria:

- **O texto copiado.** O `CompanyInvoiceWriter` repete o `PersonReceiptWriter` quase linha por linha: as linhas de
  quarto, entrada e saída, diárias, desconto e total (`CompanyInvoiceWriter.java:23-32` e
  `PersonReceiptWriter.java:20-29`). O `ForeignReceiptWriter` repete a mesma sequência com outros rótulos
  (`ForeignReceiptWriter.java:23-32`). Com o documento da agência, a regra "a linha de desconto só quando há
  desconto" vai estar em quatro lugares.
- **O título repetido.** O título aparece duas vezes em cada escritor, uma em `title()` e outra no texto (por
  exemplo, `CompanyInvoiceWriter.java:13` e `:19`).
- **O ISS solto.** O ISS usa um literal sem nome e repete o arredondamento à mão
  (`CompanyInvoiceWriter.java:33-34`: `new BigDecimal("0.05")` e `setScale(2, HALF_EVEN)`). O `PriceCalculator` dá
  nome às taxas (`PriceCalculator.java:16-17`) e centraliza o arredondamento em `cents` (`PriceCalculator.java:50`).
  O resultado está certo, mas a regra ficou num lugar diferente das outras.
- **O hóspede como sete `String` que podem ser `null`.** Em `Guest.java:9-16`, o tipo do hóspede é deduzido pelo
  `null` (`Guest.java:22-28`). O próprio tipo não impede uma combinação inválida. A única proteção está em
  `HttpApi.java:126-134`, e um `Guest` montado em código com CNPJ e sem razão social gera uma fatura com
  "Empresa: null". O `ReservationReceipt` também cresceu para 17 campos, quatro deles mutuamente `null`
  (`ReservationReceipt.java:87-104`); isso o texto pedia para o JSON.

Esses defeitos estão concentrados nos escritores e não espalhados pelo código, por isso a nota é `com problemas`. A
validação em `HttpApi` (contar as identificações e checar os pares incompletos, `HttpApi.java:126-131`) é clara.

## Testes

**Números:** 29 testes rodaram, 0 falharam e 0 foram pulados; há 29 métodos escritos (26 da base, todos mantidos sem
mudança, mais 3 novos em `DocumentsByGuestTest`). A cobertura de linhas é de 94,2% (549 de 583; a base tinha 93,8%) e
a de ramos, de 81,6% (124 de 152; a base tinha 87,1%). Em `hotel.web` os ramos cobertos são 29 de 48; na base eram
18 de 28. Em `hotel.documents` são 85 de 87 linhas e 12 de 16 ramos.
**Nota:** com problemas

**O que os testes novos conferem com valores** (`src/test/java/hotel/documents/DocumentsByGuestTest.java`):

- a fatura com o título, a linha da empresa e o ISS de R$ 38,99 (`:41-50`): trocar 0,05 por outra taxa derruba o
  teste;
- o booking receipt com o título, as datas `2027-03-10`, "2 nights" e `BRL 779.80` (`:53-62`);
- o título do comprovante com CPF (`:65-69`).

Os três passam pelo serviço e pelo banco de verdade, de modo que a gravação e a leitura das colunas novas também são
exercitadas.

**O que ficou sem rede:**

- **A validação nova** (nenhum jeito, dois jeitos, um par incompleto → 400). Não há teste dela, e o `HttpApiTest` é o
  mesmo da base. É daí que vêm os ramos novos descobertos em `hotel.web`. Apagar a condição `identifications != 1`
  em `HttpApi.java:131` não derruba nenhum teste.
- **O separador de milhar do BRL.** O único valor testado é 779.80, que não tem milhar. Trocar `%,.2f` por `%.2f` em
  `ForeignReceiptWriter.java:37` passaria.
- **O "1 night" e a linha "Discount:" em inglês.** Nenhum teste os confere (`ForeignReceiptWriter.java:28`, `:30`).
- **O documento no e-mail de confirmação e no check-out.** Para a empresa e para o estrangeiro, só a segunda via é
  testada. O teste da base confere o título no check-out apenas para o CPF (`HttpApiTest.java:87`).
- **O JSON com os campos novos e os `null`.** Nenhum teste os confere.

O roteiro do comportamento mostra que essas partes funcionam. Os testes, porém, não protegem quem mudar o código
depois.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` e nenhuma data como texto. A chave
estrangeira tem índice. Há 2 migrações do Flyway: a `V1` igual à da base e a `V2` nova
(`measures/database.md`). As conferências só acusaram as colunas de situação e de tipo sem CHECK (`status`,
`rate_plan`, `type`, `channel`), e isso vem da base.
**Nota:** bom

- **A evolução do esquema.** O agente fez a evolução do jeito certo: uma migração nova,
  `src/main/resources/db/migration/V2__guest_identification.sql:2-11`, que tira o `NOT NULL` de `guest_cpf`,
  acrescenta as quatro colunas e põe um CHECK de "um jeito só". Quem já roda o sistema migra sem perder nada: as
  reservas antigas têm CPF, então satisfazem o CHECK.
- **O código que fala com o banco.** O SQL continua com parâmetros (`ReservationStore.java:32-54`). Os `+`
  apontados pelo script juntam só constantes (`COLUMNS`). As conexões continuam em try-with-resources, e a reserva
  em dobro continua impedida pelo `EXCLUDE` da base.
- **Uma lacuna pequena no CHECK** (`V2__guest_identification.sql:8-11`). Ele soma o CPF, o par completo da empresa e
  o par completo do passaporte, mas não recusa um par pela metade quando há outro jeito preenchido. Uma linha com
  CPF e `company_cnpj` sem `company_name` soma 1 e é aceita, e o `Guest` lido dela seria tratado como empresa
  (`Guest.java:22-24`). Pela API isso não acontece, porque `HttpApi.java:129-131` recusa o pedido. Vale apertar o
  CHECK, mas a lacuna não perde nem corrompe dado no caminho real.

## Comportamento

**Números:** 83 de 83 conferências passaram (`measures/behavior.md`):

- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:

- **Com CPF:** o hóspede continua recebendo o mesmo comprovante.
- **A empresa:** recebe a fatura, com a empresa no lugar do CPF e o ISS retido de R$ 38,99 sobre R$ 779,80. A fatura
  vai dentro do e-mail em português, sai no check-out e na segunda via.
- **O estrangeiro:** recebe o booking receipt, com as datas `2027-03-10`, o dinheiro como `BRL 2,783.52`, "1 night"
  e a linha "Discount:".
- **As recusas:** a reserva sem identificação, com CPF e CNPJ juntos, ou com um par incompleto é recusada com 400
  INVALID_REQUEST.
- **O JSON:** a reserva e o documento trazem os campos novos, com `null` nos que não se aplicam.

O que já funcionava continua funcionando, inclusive os dados depois de reiniciar e as duas reservas simultâneas.

## Dúvidas

- A forma do pattern é a fábrica concentrada: um método privado com `if`s (`DocumentFactory.java:19-27`) que escolhe
  entre três objetos que implementam `DocumentWriter`. Não é o criador clássico com o método de fábrica sobrescrito,
  nem um método polimórfico no `Guest`. A referência aceita essa forma como acerto.
- A nota `com problemas` no design vem quase toda da cópia do texto entre os três escritores. Um revisor mais
  tolerante, num sistema deste tamanho, poderia aceitar isso como está.
- O agente não atualizou o README: a seção da API ainda mostra o corpo com `guestCpf` como o único jeito. O texto não
  pedia isso, e não pesou em nenhuma coluna.
