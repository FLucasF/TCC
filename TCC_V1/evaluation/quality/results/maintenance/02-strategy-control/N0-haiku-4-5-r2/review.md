# Revisão: manutenção, cenário 02

## Visão geral

O sistema (da base) tem 7 pacotes: `hotel` monta tudo (`src/main/java/hotel/Hotel.java:21-27`) e roda as migrações do
Flyway sobre um pool Hikari (`src/main/java/hotel/Database.java:23-28`); `hotel.web.HttpApi` lê a requisição e chama
`ReservationService`, que valida, consulta `ReservationStore` (JDBC, PostgreSQL) e calcula o preço em
`PriceCalculator`. O pedido era pontual: o acréscimo da alta temporada de 20% para 25% e a semana da Páscoa de 2027 e
2028 como alta temporada. O agente mudou só `PriceCalculator` (a constante em
`src/main/java/hotel/reservations/PriceCalculator.java:16` e duas condições a mais em `isHighSeason`, linhas 51-59),
atualizou os valores esperados dos testes e acrescentou 3 testes. `diff -rq base final` só mostra esses arquivos (e o
`target/`). Não há nada sério: o `mvn verify` passa (29 de 29) e o roteiro do comportamento passa inteiro (72 de 72).
O único descuido é o `README.md:31-32`, que ainda diz "20% a mais" e não fala da Páscoa; o dono disse que é o README
que explica o sistema, e agora ele está desatualizado.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais na
base e no final (`measures/dependencies.md` e `measures/dependencies-base.md`; só muda o tamanho de `hotel.reservations`,
de 508 para 522 linhas).
**Nota:** bom

O agente não mexeu na estrutura: a mudança ficou dentro da classe que já tinha a regra de preço
(`src/main/java/hotel/reservations/PriceCalculator.java:42-62`), que não conhece Javalin nem JDBC. A borda HTTP é
fina (`src/main/java/hotel/web/HttpApi.java:61-91`), o JDBC fica em `ReservationStore`, `Rooms` e `SentMessages`, e a
montagem fica num lugar só (`src/main/java/hotel/Hotel.java:21-27`). O ciclo `documents` ↔ `reservations` vem da base
(`ReservationService` cria o `ReservationReceipt`, `src/main/java/hotel/reservations/ReservationService.java:64`, e
`src/main/java/hotel/documents/ReservationReceipt.java:3` importa `Reservation`); o agente não o piorou.

## Design de baixo nível

**Números:** 1 arquivo de produção mudado (`PriceCalculator.java`, +18/−4 linhas); nenhuma classe, interface ou
enum nova; a constante do acréscimo continua única, para todas as temporadas.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É um controle negativo, e a frase que tenta o exagero ("já pensou em ter um acréscimo diferente para cada temporada,
mas por enquanto é o mesmo para todas") não levou a nada a mais: não há classe por temporada, nem interface de regra
de temporada, nem acréscimo parametrizado. A constante passou a valer 0,25
(`src/main/java/hotel/reservations/PriceCalculator.java:16`) e a Páscoa entrou como duas condições a mais no
`isHighSeason`, uma por ano, com as datas que o dono deu (`PriceCalculator.java:54-59`), exatamente a forma que os
critérios do cenário aceitam. O cálculo do preço da noite não mudou (`PriceCalculator.java:33-39`), com `BigDecimal` e
arredondamento meio-para-o-par (`PriceCalculator.java:64-66`).

Daria para escrever as semanas da Páscoa como uma lista de períodos (`LocalDate` de início e de fim), mais fácil de
ler e de completar nos próximos anos do que comparar ano, mês e dia em `int`. Com dois anos só, não é um defeito.
O resto do código vem da base e está limpo: tipos do domínio (`enum` `RatePlan`, `ReservationStatus`, `ErrorCode`),
erros traduzidos num ponto só (`src/main/java/hotel/web/HttpApi.java:93-107`) e métodos curtos. Só o README ficou
desatualizado (`README.md:31-32`); isso não entra na nota do código.

## Testes

**Números:** 29 testes rodaram, 0 falhas, 0 erros, 0 pulados; 29 métodos escritos (a base tinha 26, todos mantidos).
Cobertura de linhas de 93,9% (479 de 510; a base tinha 93,8%) e de ramos de 88,6% (117 de 132; a base tinha 87,1%);
`hotel.reservations` com 65 de 68 ramos cobertos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** bom

O agente recalculou os valores esperados dos testes que dependiam do acréscimo, e as contas estão certas (249,90 × 1,25
= 312,375 → 312,38): `src/test/java/hotel/reservations/PriceCalculatorTest.java:36`, `:45`, `:97-99`, e
`src/test/java/hotel/web/HttpApiTest.java:77` e `:118`. Nenhum teste foi apagado nem enfraquecido; as asserções
continuam com valores exatos. Os 3 testes novos conferem as duas pontas das duas semanas da Páscoa, com o dia de antes
e o de depois (`PriceCalculatorTest.java:57-70`), e o preço de 4 noites da Páscoa de 2027 (`:73-78`). Pela pergunta da
mutação: voltar a constante para 0,20 derruba vários testes; trocar `>=` por `>` ou `<=` por `<` nas datas da Páscoa
derruba os testes das pontas. O que fica sem rede é a condição do ano: se alguém apagasse `year == 2027` ou
`year == 2028` (`PriceCalculator.java:54` e `:57`), nenhum teste falharia (nenhum confere, por exemplo, 21/03/2028
ou 10/04/2027 como baixa temporada). É uma lacuna pequena. Os testes misturam unidade, banco de verdade e API
(`measures/tests.md`, "Os tipos de teste").

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária; dinheiro em `numeric(10,2)`;
nenhuma chave estrangeira sem índice; nenhuma data em texto; 1 migração do Flyway, igual à da base; as conferências
só acusam as colunas `status`, `rate_plan`, `type` e `channel` em `text` sem CHECK (`measures/database.md`).
**Nota:** bom

A mudança não precisava mexer no banco, e o agente não mexeu: `V1__create_tables.sql` está igual à da base, e não há
migração nova. Os preços ficam gravados na reserva quando ela é criada (`subtotal`, `discount` e `total` em
`src/main/resources/db/migration/V1__create_tables.sql:30-32`), então a mudança de 20% para 25% não altera as
reservas que já existem, o que é o certo. A reserva dupla é barrada no banco pelo `EXCLUDE USING gist`
(`V1__create_tables.sql:38-40`), e a violação vira 409 (`src/main/java/hotel/reservations/ReservationStore.java:21`
e `:54-56`). As colunas de situação sem CHECK e os relatórios que leem a tabela inteira de reservas
(`ReservationStore.java:108-111`, usado em `src/main/java/hotel/reports/RevenueReport.java:25` e
`OccupancyReport.java:25`) vêm da base e não são do agente.

## Comportamento

**Números:** 72 de 72 conferências passaram: a parte comum 46 de 46, os dados depois de reiniciar 3 de 3, o acréscimo
novo 5 de 5, a semana da Páscoa 10 de 10, os outros períodos 6 de 6, as reservas simultâneas 2 de 2
(`measures/behavior.md`).
**Nota:** bom

Não houve falha. Pela API, a noite standard de temporada sai R$ 312,38, como o dono pediu; a deluxe sai R$ 487,38 e a
suíte R$ 724,88; o desconto do não reembolsável é calculado sobre o valor novo (R$ 46,86, total R$ 265,52). As noites
de 21/03 a 28/03/2027 e de 09/04 a 16/04/2028 custam o preço de temporada, e as vizinhas (20/03, 29/03/2027, 08/04 e
17/04/2028) não. Uma estadia de 20 a 22/03/2027 soma uma noite de cada (R$ 562,28). De 15/12 ao fim de fevereiro e
julho continuam como antes. Para o dono da pousada, o sistema cobra o que ele pediu e o resto segue funcionando.

## Dúvidas

- O `README.md:31-32` ainda diz "20% a mais" e não fala da Páscoa. Pesei isso como descuido de documentação, não de
  código, e não baixei nenhuma nota por isso. Um revisor mais rígido pode pedir que o README seja corrigido antes de
  aceitar.
