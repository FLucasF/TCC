# Revisão: manutenção, cenário 01

## Visão geral

O projeto é o da base: sete pacotes (`hotel`, `hotel.web`, `hotel.reservations`, `hotel.rooms`, `hotel.documents`,
`hotel.messages`, `hotel.reports`), com a requisição entrando por `hotel.web.HttpApi`, que monta um `NewReservation` e
chama `ReservationService`. O serviço calcula o preço com `PriceCalculator` e grava por `ReservationStore` (JDBC com
`?`) num PostgreSQL criado pelo Flyway. A mudança (12 arquivos, `changes.patch`) acrescenta três constantes ao `enum`
`RatePlan` e passa o `companyCode` da borda até o banco, numa migração nova (`V2__add_company_code.sql`). As regras de
cada plano foram encaixadas nos `switch` que já existiam: o desconto (`PriceCalculator.java:32-38`), um `switch` novo
de validação e escolha do cálculo no serviço (`ReservationService.java:47-68`) e a devolução
(`ReservationService.java:108-112`). O sistema faz tudo o que o texto pede: as 72 conferências do roteiro passam, e os
40 testes também. O que há de mais sério é o design. O texto avisa que "o comercial vive inventando plano", mas cada
plano novo continua exigindo mexer em três `switch` de dois arquivos. A lista de empresas aparece duplicada, e o
README não foi atualizado com os planos novos (ele ainda descreve só `STANDARD` e `NON_REFUNDABLE`).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos na base e na final (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base.
`hotel.reservations` passou de 508 para 578 linhas; `ReservationService` passou de 138 para 165 e `PriceCalculator` de
53 para 87.
**Nota:** bom

O agente não mexeu nas camadas. A borda continua fina: a única mudança em `HttpApi` é repassar o campo novo
(`src/main/java/hotel/web/HttpApi.java:128-130`), e `CreateReservationRequest` só ganhou o campo
(`src/main/java/hotel/web/CreateReservationRequest.java:16`). As regras dos planos ficaram em `hotel.reservations`,
sem Javalin. O SQL continua só em `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:31-45`
e `:132`), e a montagem continua no `main` da base. O ciclo `documents` ↔ `reservations` veio da base, e nenhuma
dependência nova apareceu. O lugar das regras é discutível (a validação de elegibilidade dos planos mora no
`ReservationService`, e `isWeekendPackageValid` no `PriceCalculator`, `PriceCalculator.java:81-86`), mas isso pesa no
design, não nas camadas.

## Design de baixo nível

**Números:** 5 planos (2 da base e 3 novos), nenhum com o seu comportamento num lugar próprio. Há 3 `switch` sobre
`RatePlan` com comportamento: o desconto (`PriceCalculator.java:32-38`, 5 casos), a validação e a escolha do cálculo
(`ReservationService.java:47-68`, 5 casos, novo) e a devolução (`ReservationService.java:108-112`, 5 casos). Fora
deles, há ainda 1 `switch` sobre o tipo de quarto para o preço do pacote (`PriceCalculator.java:43-47`) e 2 listas
das empresas (`PriceCalculator.java:51-57` e `ReservationService.java:72-74`). Nenhuma interface ou classe por plano.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto deixa claro que os planos são uma variação que cresce ("o comercial vive inventando plano",
cada um "com o seu jeito de calcular o preço, com as suas regras de quem pode usar e com a sua regra de devolução").
O agente não deu a cada plano um lugar só dele. Ele fez crescer os dois `switch` da base, o do desconto em
`PriceCalculator.java:32-38` e o da devolução em `ReservationService.java:108-112`, e criou um terceiro `switch` de
comportamento no serviço (`ReservationService.java:47-68`), com as regras de quem pode usar escritas em `if` em cada
`case`: o código da empresa em `:49`, a sexta a domingo em `:55` e as 7 noites em `:61-63`. O pacote de fim de semana
nem passa pelo cálculo comum. Ele tem um método próprio, `calculateWeekendPackage` (`PriceCalculator.java:42-49`), e
um `case WEEKEND_PACKAGE` morto no `switch` do desconto (`:37`), que daria o preço por diária se alguém chamasse
`calculate` com esse plano. Um plano novo, como o da melhor idade, exigiria mexer em pelo menos três lugares em dois
arquivos. É o caso de omissão que a referência descreve: "os `switch` que já existiam crescendo para os cinco casos".
Nada aproxima de um Strategy.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface, fábrica ou camada nova. A conta é contenção.

**O resto do código.** Há defeitos localizados:
- **Duplicação:** as três empresas estão listadas duas vezes, em `ReservationService.java:72-74` (validação) e em
  `PriceCalculator.java:51-57` (desconto). Uma empresa nova incluída só em um dos lugares passa na validação e cai no
  `IllegalArgumentException` de `PriceCalculator.java:56`, que vira um erro 500, não um 400.
- **Método longo:** `ReservationService.create` agora tem um `switch` com validação, `throw` e `yield` aninhados
  (`ReservationService.java:47-68`). As linhas `:52`, `:65` e `:67` passam de 120 colunas e repetem a mesma chamada
  com `null` para o código da empresa.
- **Nomes totalmente qualificados no meio do código:** `java.time.temporal.ChronoUnit` em
  `ReservationService.java:61`, e `java.math.BigDecimal` e `java.math.RoundingMode` em `:111`, onde o
  `PriceCalculator.cents(...)` em volta de um `divide` que já arredonda para 2 casas é redundante.
- **Parâmetros que não servem para nada:** `checkIn` e `checkOut` em `calculateWeekendPackage`
  (`PriceCalculator.java:42`). A contagem de noites também é feita por conta própria (`ReservationService.java:61`)
  em vez de vir do `Price`.
- **Valores soltos:** os preços do pacote estão como literais no meio do método (`PriceCalculator.java:44-46`),
  enquanto os percentuais têm constantes com nome.
- **Tipo:** o código da empresa é um `String` solto (`NewReservation.java:7`) e não um tipo do domínio. É uma escolha
  aceitável neste tamanho, mas ajudou a espalhar a lista.

O dinheiro continua em `BigDecimal`, com o meio para o par, e as datas em `LocalDate`, como na base.

## Testes

**Números:** 40 testes rodaram, 0 falharam e 0 foram pulados (`measures/tests.md`); na base, eram 26
(`measures/tests-base.md`). Há 40 métodos escritos, 14 deles novos. A cobertura de linhas ficou em 93,3% (505 de 541;
era 93,8%) e a de ramos em 83,4% (126 de 151; era 87,1%). Em `hotel.reservations`: 242 de 260 linhas e 74 de 87 ramos.
**Nota:** com problemas

**O que está bem.** Os 26 testes da base continuam lá e passando, e só mudaram para receber o parâmetro novo
(`null`). Nenhum teste foi apagado ou enfraquecido. Os três exemplos do financeiro estão conferidos com valores:
- Tecnosul, 779,80 / 93,58 / 686,22: `src/test/java/hotel/reservations/PriceCalculatorTest.java:78-88`;
- pacote deluxe, 820,00: `PriceCalculatorTest.java:91-100`;
- estadia longa, 1.749,30 / 174,93 / 1.574,37: `PriceCalculatorTest.java:103-112`;
- devolução de 410,00 do pacote: `src/test/java/hotel/reservations/ReservationServiceTest.java:218-228`.

As recusas também são testadas: convênio sem código e com código inválido (`ReservationServiceTest.java:164-172`),
pacote fora de sexta a domingo (`:186-189`) e estadia longa com 6 noites (`:203-206`, que junto com o de 7 noites em
`:209-215` pega a troca de `<` por `<=`). Pela API, há testes de convênio, pacote e estadia longa
(`src/test/java/hotel/web/HttpApiTest.java:124-172`).

**A pergunta da mutação.** Parte das regras novas fica sem rede:
- trocar 0.18 (Agrovale) ou 0.25 (Farmavida) por outro número (`PriceCalculator.java:21-22`) não derruba nenhum
  teste, porque só a Tecnosul é testada;
- trocar R$ 520,00 ou R$ 1.200,00 (`PriceCalculator.java:44` e `:46`) não derruba nenhum teste, porque só o deluxe é
  testado;
- tirar `CORPORATE` ou `LONG_STAY` da devolução integral (`ReservationService.java:109`) não derruba nenhum teste,
  porque só a devolução do pacote é testada;
- não há teste da estadia longa com noites de temporada, nem do pacote em alta temporada (o preço fechado);
- a regra do pacote tem um caso só de recusa (quarta a sexta); sábado a segunda, ou sexta a segunda, não são
  testados.

O teste do convênio válido pela API só confere o status e o plano, não o valor (`HttpApiTest.java:133-142`).

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` e datas em `date`
(`measures/database.md`). As conferências não acusaram nenhuma chave estrangeira sem índice nem data em texto. As
colunas `rate_plan`, `status`, `type` e `channel` estão sem CHECK, como na base. A `V1__create_tables.sql` está igual
à da base, e a `V2__add_company_code.sql` é nova. O `EXCLUDE USING gist` contra duas reservas na mesma noite foi
mantido.
**Nota:** bom

O agente fez o que um banco em produção precisa: uma migração nova e aditiva
(`src/main/resources/db/migration/V2__add_company_code.sql:1`, `ALTER TABLE reservations ADD COLUMN company_code
text`), sem tocar na V1. Os dados de quem já usa o sistema continuam válidos (a coluna é anulável). O SQL alterado
continua com parâmetros (`ReservationStore.java:31-45`, o código entra por `setString(9, ...)`), e a leitura devolve o
campo (`ReservationStore.java:133`). Dois pontos menores, que não mudam a nota:
- O código da empresa é gravado para qualquer plano (`ReservationStore.java:44`), embora o texto diga que nos outros
  planos ele "pode ignorar".
- Nenhum CHECK liga `company_code` a `rate_plan = 'CORPORATE'`. A falta de CHECK em `rate_plan` vem da base.

## Comportamento

**Números:** 72 de 72 conferências passaram (`measures/behavior.md`): a parte comum, 46 de 46; os dados depois de
reiniciar, 3 de 3; o cenário 01, 21 de 21; duas reservas ao mesmo tempo, 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- **Convênio:** Tecnosul, Agrovale e Farmavida com os descontos certos (686,22, 639,44 e 584,85). A reserva sem
  código, ou com um código desconhecido, é recusada com 400 `INVALID_REQUEST`, e o convênio cancelado devolve tudo.
- **Pacote:** 520,00, 820,00 e 1.200,00 com preço fechado, inclusive em julho. A reserva de sábado a segunda, ou de
  sexta a segunda, é recusada, e o cancelamento devolve a metade (410,00).
- **Estadia longa:** 1.574,37, com a temporada valendo (1.754,30); 6 noites são recusadas, e o cancelamento devolve
  tudo.
- **O que já existia:** o `companyCode` é ignorado no preço do STANDARD, e o NON_REFUNDABLE continua com 15%. A parte
  comum, a persistência depois de reiniciar e a proteção contra a reserva dupla continuam como na base.

Fora do roteiro, uma observação que não muda a nota: a disponibilidade do quarto é conferida antes das regras do plano
(`ReservationService.java:42-45`, antes de `:47`). Um pacote pedido em datas inválidas para um quarto ocupado recebe
409 `ROOM_UNAVAILABLE`, e não 400.

## Dúvidas

- **Gravar o código da empresa em qualquer plano** (`ReservationStore.java:44`): o texto diz que, nos outros planos,
  o código "pode ignorar". Gravar o valor não muda o preço nem a resposta. Pela referência, é uma regra pequena fora
  do plano e não mudou a classe.
- **O README sem os planos novos:** ele não foi atualizado e continua descrevendo só `STANDARD` e `NON_REFUNDABLE` (no
  preço e no cancelamento). Não pesei isso em nenhuma coluna.
