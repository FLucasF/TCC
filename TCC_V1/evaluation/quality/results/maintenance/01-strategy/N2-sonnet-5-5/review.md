# Revisão: manutenção, cenário 01

## Visão geral

A base é um serviço Javalin em pacotes por assunto (`hotel.web` para a borda HTTP, `hotel.reservations` para as
regras e o acesso às reservas, `hotel.rooms`, `hotel.messages`, `hotel.documents`, `hotel.reports`, e `hotel` para a
montagem). Uma requisição entra em `HttpApi`, vira um `NewReservation` (`web/HttpApi.java:128-130`), vai para
`ReservationService.create`, que pede o preço a `PriceCalculator` (`reservations/ReservationService.java:42`) e grava
por `ReservationStore` (JDBC com `?`) no PostgreSQL, com o esquema criado por migrações do Flyway. O agente pôs os
cinco planos num Strategy: o `enum RatePlan` guarda uma `RatePlanRule` por constante (`reservations/RatePlan.java:8-16`),
e o cálculo e o cancelamento pedem ao plano, sem nomear nenhum (`reservations/PriceCalculator.java:26`,
`reservations/ReservationService.java:82`). O código da empresa chegou por uma migração nova e aditiva
(`V2__add_company_code.sql`). O roteiro passou inteiro (72 de 72) e os 34 testes passam. Nada sério. Os pontos fracos
são dois: a devolução dos três planos novos não tem teste (o R$ 410,00 do pacote, por exemplo), e o `companyCode` fica
gravado e volta na resposta também nos planos que deviam ignorá-lo, o contrário do que diz o comentário do próprio
campo.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base são
os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. `hotel.reservations` passou de 508 para 667 linhas;
a maior classe continua sendo `HttpApi` (171 linhas, eram 170).
**Nota:** bom

A mudança não mexeu na forma do sistema. As classes novas (`RatePlanRule`, `CorporateRule`, `WeekendPackageRule`,
`PercentageDiscountRule`) ficam em `hotel.reservations`, não usam Javalin nem JDBC e são package-private. A borda só
ganhou o campo `companyCode`, repassado sem regra nenhuma (`web/CreateReservationRequest.java:16`,
`web/HttpApi.java:130`, `web/ReservationResponse.java:19,39`). O SQL novo ficou só no `ReservationStore`
(`reservations/ReservationStore.java:44,133`). O ciclo `documents` ↔ `reservations` já existia na base:
`ReservationService` importa `ReservationReceipt`, que importa `Reservation`. O agente não o piorou.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (os 2 que já existiam e os 3 novos); 0 `switch`/`if` sobre o plano no código
de produção (os dois `switch` da base, em `PriceCalculator.calculate` e em `ReservationService.cancel`, foram
removidos); 1 interface (`RatePlanRule`, `reservations/RatePlanRule.java:7`) com 3 implementações.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Cada constante do `enum` recebe a sua estratégia no construtor (`reservations/RatePlan.java:8-16`), e a
referência aceita essa forma como acerto. Cada plano guarda num lugar só o preço, a validação e a devolução:

- o convênio tem a tabela das empresas e recusa um código fora dela (`reservations/CorporateRule.java:10-21`);
- o pacote confere a sexta a domingo e usa o preço fechado por tipo de quarto (`reservations/WeekendPackageRule.java:12-27`)
  e devolve metade (`:31-33`);
- o mínimo de noites e o percentual de desconto estão em `PercentageDiscountRule` (`reservations/PercentageDiscountRule.java:24-30`).

Essa última classe atende `STANDARD` (0%, 1 noite), `NON_REFUNDABLE` (15%, devolução zero) e `LONG_STAY` (10%,
7 noites), com a devolução passada como função. Os dois planos que já existiam também entraram na estrutura.
`PriceCalculator.calculate` só calcula a temporada noite a noite e entrega ao plano (`reservations/PriceCalculator.java:18-26`).
`ReservationService.cancel` pede a devolução ao plano (`reservations/ReservationService.java:82`). Nenhum dos dois
nomeia um plano. O próximo plano (melhor idade, antecedência) é uma constante nova e, se precisar, uma classe nova.

**O exagero.** Não há. A interface tem três implementações de verdade. Os dois métodos de `RatePlan` que repassam para
a regra (`reservations/RatePlan.java:24-30`) são o próprio ponto de seleção, e o `withDiscount` em `Price`
(`reservations/Price.java:7-11`) evita repetir a conta do desconto em duas regras. Não há fábrica, builder nem camada
que só repassa.

**O resto do código.**

- Dinheiro continua em `BigDecimal`, com o arredondamento meio para o par num lugar só (`PriceCalculator.cents`).
- O que um revisor exigente pediria para mudar:
  - **O `companyCode` não é ignorado nos outros planos.** `Reservation` guarda o código que veio, seja qual for o plano
    (`reservations/Reservation.java:39`), o `ReservationStore` grava esse código no banco
    (`reservations/ReservationStore.java:44`) e a resposta o devolve (`web/ReservationResponse.java:39`). Assim, uma
    reserva `STANDARD` enviada com `TECNOSUL` fica com `company_code = 'TECNOSUL'`. O comentário do próprio getter diz
    "null for the other plans" (`reservations/Reservation.java:74`), e o de `NewReservation` diz o mesmo
    (`reservations/NewReservation.java:5`). O código não faz isso. Um relatório futuro por empresa contaria reservas
    que não são do convênio.
  - **O mapa do pacote pode não ter o tipo do quarto.** O preço vem de `PRICE_BY_ROOM_TYPE.get(room.type())`, sem
    tratar a falta (`reservations/WeekendPackageRule.java:26`). Um tipo de quarto novo daria um `null` no preço e um
    erro 500 mais adiante. Um `EnumMap` com uma conferência, ou o preço do pacote no próprio `RoomType`, fecharia isso.
  - **A devolução do não reembolsável ficou num lambda.** O lambda está na lista das constantes e chama
    `PriceCalculator.cents` (`reservations/RatePlan.java:10`), enquanto a "devolução total" tem nome
    (`PercentageDiscountRule.EVERYTHING_BACK`, `reservations/PercentageDiscountRule.java:10`). É uma assimetria pequena.

## Testes

**Números:** 34 testes rodaram, 0 falhas, 0 erros, 0 pulados; 34 métodos escritos (a base tinha 26, todos mantidos:
+6 em `PriceCalculatorTest`, +2 em `HttpApiTest`). Cobertura de linhas: 510 de 543 (93,9%; base 93,8%). Cobertura de
ramos: 109 de 124 (87,9%; base 87,1%). Em `hotel.reservations`: 246 de 261 linhas cobertas (base 209 de 222), ou seja,
duas linhas descobertas a mais.
**Nota:** com problemas

O preço dos planos novos está testado com os valores do texto:

- Tecnosul 779,80 / 93,58 / 686,22, e Agrovale e Farmavida com os seus descontos (`src/test/java/hotel/reservations/PriceCalculatorTest.java:95-109`);
- o convênio sem código, com código vazio, em minúsculas e com código desconhecido (`:111-116`);
- o pacote nos três tipos de quarto e em alta temporada (`:119-133`) e as três datas recusadas (`:136-143`);
- a estadia longa de 1.749,30 / 174,93 / 1.574,37 (`:146-153`), com temporada e com 6 noites recusadas (`:156-166`);
- pela API, o `companyCode` lido e a recusa 400 `INVALID_REQUEST` (`src/test/java/hotel/web/HttpApiTest.java:124-133`).

A pergunta da mutação pega bem o preço: trocar 0.12 por 0.21, ou `<` por `<=` no mínimo de noites
(`reservations/PercentageDiscountRule.java:24`), derruba um teste.

A devolução, que é a outra metade de cada plano, não tem teste nenhum. `ReservationServiceTest` não foi mudado e só
confere a devolução do `STANDARD` e do `NON_REFUNDABLE` (`src/test/java/hotel/reservations/ReservationServiceTest.java:109-118`).
Trocar `REFUND_RATE` de 0.50 para 0.40 (`reservations/WeekendPackageRule.java:31-33`), ou fazer `CorporateRule.refund`
devolver zero (`reservations/CorporateRule.java:24-26`), não derruba nenhum teste. O R$ 410,00 que o texto dá não está
conferido em lugar nenhum, e essas são as duas linhas descobertas a mais em `hotel.reservations`.

Há ainda duas fraquezas menores:

- `ignoresTheCompanyCodeInOtherPlans` confere só o total (`src/test/java/hotel/web/HttpApiTest.java:137-142`) e não
  pegaria o `companyCode` gravado na reserva `STANDARD`;
- o total é conferido com `asDouble()`, que compara dinheiro como ponto flutuante.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(10,2)` nas
6 colunas de valor. A chave estrangeira tem índice e nenhuma data está guardada como texto. A `EXCLUDE` contra duas
reservas na mesma noite está lá. As migrações: `V1__create_tables.sql` igual à da base, `V2__add_company_code.sql` nova;
o Flyway aplicou 2. As colunas `rate_plan`, `status`, `type` e `channel` estão sem CHECK, como na base.
**Nota:** bom

O agente fez a mudança do esquema do jeito certo: uma migração nova e aditiva
(`src/main/resources/db/migration/V2__add_company_code.sql:2`, `ALTER TABLE reservations ADD COLUMN company_code text`),
sem tocar na `V1`. Um banco que já roda o sistema sobe sem problema, e as reservas antigas ficam com `company_code`
nulo. O SQL novo usa parâmetro (`reservations/ReservationStore.java:44`) e a leitura acompanha
(`reservations/ReservationStore.java:133`). As concatenações que o script lista (`ReservationStore.java:33,84,99,112`)
juntam só constantes (`COLUMNS`).

Duas coisas vieram da base e o agente não as piorou:

- a falta de CHECK em `rate_plan`;
- o `findAll` sem filtro (`ReservationStore.java:112`).

A coluna `company_code` também ficou sem CHECK, mas a lista de empresas mora no código e vai mudar, então isso é
razoável. O defeito de gravar o código também nos outros planos está contado no design.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam. A parte comum: 46 de 46. Os dados depois de reiniciar: 3 de 3.
O cenário 01: 21 de 21. As reservas simultâneas: 2 de 2 (10 rodadas, sempre uma criada e a outra recusada com 409).
**Nota:** bom

Para o dono da pousada, os três planos novos fazem o que ele pediu, com as contas do financeiro:

- convênio Tecnosul R$ 686,22, Agrovale e Farmavida com 18% e 25%, devolução total;
- recusa 400 `INVALID_REQUEST` sem código ou com código desconhecido;
- pacote de R$ 520,00 / R$ 820,00 / R$ 1.200,00, igual em julho, recusado fora de sexta a domingo, devolvendo
  R$ 410,00;
- estadia longa de R$ 1.574,37, com a temporada valendo, recusada com 6 noites, devolução total.

O normal e o não reembolsável continuam como antes. Os dados sobrevivem a um reinício, e o quarto não é reservado duas
vezes.

Um detalhe fica fora do roteiro: uma reserva `STANDARD` enviada com `companyCode` sai com o preço certo, mas a resposta
traz o código de volta e o banco o guarda (`reservations/Reservation.java:39`). Esse defeito pesa no design, não aqui.

## Dúvidas

- A nota do design ficou em `com problemas` principalmente por causa do `companyCode` guardado e devolvido nos planos
  que deviam ignorá-lo, o contrário do que o próprio código documenta (`reservations/Reservation.java:74`). O texto diz
  "se vier, pode ignorar", e o preço de fato o ignora. Um revisor menos exigente aceitaria o design como `bom`.
- Ao validar o plano antes de olhar a disponibilidade (`reservations/ReservationService.java:42-46`), o agente mudou a
  ordem dos erros. Agora um pedido que fere a regra do plano num quarto ocupado responde 400, e não 409. O texto não
  diz qual vem primeiro, e o roteiro não depende disso.
