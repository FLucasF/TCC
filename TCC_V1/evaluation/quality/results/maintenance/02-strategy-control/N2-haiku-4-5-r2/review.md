# Revisão: manutenção, cenário 02

## Visão geral

A base é um serviço Javalin em sete pacotes. `hotel` faz a montagem e o banco, `hotel.web` cuida do HTTP,
`hotel.reservations` tem as regras e o `ReservationStore` (JDBC), e `hotel.rooms`, `hotel.messages`,
`hotel.documents` e `hotel.reports` completam o sistema. Uma requisição entra no `HttpApi`, que chama o
`ReservationService`. O serviço usa o `PriceCalculator` e grava pelo `ReservationStore` no PostgreSQL, com o esquema
criado por uma migração Flyway. O pedido trocava o acréscimo da alta temporada de 20% para 25% e punha a semana da
Páscoa de 2027 e de 2028 na alta temporada. O agente mudou só três arquivos (`changes.patch`). Em
`PriceCalculator.java:16` a constante passou de `0.20` para `0.25`. Em `PriceCalculator.java:44-63` o `isHighSeason`
ganhou um método privado `isEasterWeek` com as duas semanas. Os testes da base foram ajustados aos valores novos e
ganharam dois testes novos para as bordas da Páscoa. Não há nada sério. O único ponto é que o `README.md:31-32`
continua dizendo "20% a mais" e não cita a Páscoa: a documentação ficou desatualizada.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base, os
mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo (`measures/dependencies-base.md`). O único número que
mudou é o tamanho de `hotel.reservations`, de 508 para 523 linhas (o `PriceCalculator`, de 53 para 68).
**Nota:** bom

O agente não mexeu na estrutura. A regra nova ficou onde a regra de preço já ficava, no `PriceCalculator`
(`src/main/java/hotel/reservations/PriceCalculator.java:42-63`). Essa classe não conhece Javalin nem JDBC; só importa
`hotel.rooms.Room` (`PriceCalculator.java:3`). A borda HTTP continua fina: as rotas só leem a requisição e chamam o
serviço (`src/main/java/hotel/web/HttpApi.java:61-90`). O ciclo vem da base: `ReservationService` importa
`hotel.documents.ReservationReceipt` (`ReservationService.java:3`), e `ReservationReceipt` importa
`hotel.reservations.Reservation` (`ReservationReceipt.java:3`). O agente não o criou nem o piorou.

## Design de baixo nível

**Números:** 1 constante alterada (`PriceCalculator.java:16`), 1 método privado novo com 2 condições, uma por ano
(`PriceCalculator.java:51-63`). Nenhuma classe, interface ou enum novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É um controle negativo, e a frase que tenta o agente é "um acréscimo diferente para cada temporada". O agente não
caiu nela:

- **O acréscimo:** continua uma constante única, `HIGH_SEASON_SURCHARGE = 0.25` (`PriceCalculator.java:16`), usada
  num lugar só (`PriceCalculator.java:36`).
- **A Páscoa:** entrou como dado, em duas condições num método privado (`PriceCalculator.java:56-61`), chamado no fim
  do `isHighSeason` (`PriceCalculator.java:48`). É exatamente o que a referência do controle chama de contenção.
- **O resto:** as regras que já existiam ficaram como estavam (`PriceCalculator.java:44-45`). Não há estratégia por
  temporada, interface de regra nem acréscimo parametrizado.

Detalhe: as datas estão como números soltos (ano, mês, dia) dentro do método, e não numa lista de períodos com
`LocalDate`. Uma lista seria um pouco mais fácil de estender quando o dono mandar os próximos anos, mas com dois
anos isso não pesa.

O resto do código vem da base e está limpo:

- **Dinheiro:** `BigDecimal` com arredondamento meio para o par a cada passo (`PriceCalculator.java:65-67`).
- **Domínio:** `switch` exaustivo sobre os enums `RatePlan` e `ErrorCode` (`PriceCalculator.java:26-29`,
  `HttpApi.java:100-107`), um tipo próprio para os erros (`HotelException`) e métodos curtos no
  `ReservationService`.
- **Ponto menor, da base:** o `catch (Exception)` em `HttpApi.java:113` é comentado e transforma o erro num 400.

## Testes

**Números:** 28 testes rodaram, 0 falhas, 0 erros, 0 pulados, 28 métodos escritos (na base eram 26).

| | linhas | ramos |
|---|---|---|
| final | 93,9% (481/512) | 88,6% (117/132) |
| base | 93,8% (471/502) | 87,1% (101/116) |
| `hotel.reservations` | 219/232 | 65/68 |
**Nota:** bom

O que o agente testou:

- **O acréscimo novo, com valores:** os testes da base passaram a conferir 25% com valores calculados à mão.
  Dezembro 15 vale 312,38, que é o valor que o dono dá (`src/test/java/hotel/reservations/PriceCalculatorTest.java:35-36`).
  Fevereiro de ano bissexto está em `:44-45`. Na suíte não reembolsável, o subtotal dá 2899,52, o desconto 434,93
  e o total 2464,59 (`:87-90`). Pela API, o total de julho dá 974,76 (`src/test/java/hotel/web/HttpApiTest.java:77`)
  e o relatório financeiro também (`HttpApiTest.java:118`).
- **A Páscoa, nas quatro bordas de cada ano:** em 2027, 20/03 fora, 21/03 e 28/03 dentro, 29/03 fora
  (`PriceCalculatorTest.java:57-62`). Em 2028, 08/04 fora, 09/04 e 16/04 dentro, 17/04 fora (`:65-70`).

O que o agente manteve: nenhum teste da base foi apagado nem enfraquecido. Os asserts mudados só trocaram o valor
esperado pelo novo.

A pergunta da mutação:

- Voltar a constante para `0.20` derruba quatro testes.
- Trocar `>=` por `>` ou `<=` por `<` em `PriceCalculator.java:57` ou `:60` derruba os testes de borda.
- Apagar uma das condições da Páscoa derruba o teste daquele ano.

Falta só um teste de preço de uma estadia que cruza a borda da Páscoa. O que existe confere `isHighSeason`
isoladamente, mas o preço da noite já fica coberto pelos testes do acréscimo, e por isso essa falta não pesa.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, nenhuma chave
estrangeira está sem índice e nenhuma data está em texto. A migração `V1__create_tables.sql` está "igual" à da base,
e não há migração nova. A única conferência que acusou algo: 4 colunas de situação ou tipo em `text` sem `CHECK`
(`messages.channel`, `reservations.rate_plan`, `reservations.status`, `rooms.type`).
**Nota:** bom

O pedido não mexia no banco, e o agente não mexeu: a migração da base está intacta
(`src/main/resources/db/migration/V1__create_tables.sql`). As colunas sem `CHECK` vêm da base
(`V1__create_tables.sql:4`, `:28`, `:33`, `:46`). O que a base tem de bom continua:

- **Reserva dupla:** o `EXCLUDE USING gist` impede duas reservas do mesmo quarto na mesma noite
  (`V1__create_tables.sql:38-40`).
- **SQL:** tudo com parâmetros. Os `+` em `ReservationStore.java` só juntam constantes.

Como o preço é gravado na reserva (`V1__create_tables.sql:30-32`), as reservas feitas antes da mudança mantêm o
valor de 20%. Isso é o comportamento sensato.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam.

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o acréscimo novo | 5 de 5 |
| a semana da Páscoa | 10 de 10 |
| o resto dos períodos | 6 de 6 |
| duas reservas ao mesmo tempo | 2 de 2 |
**Nota:** bom

Tudo o que o dono pediu funciona pela API:

- **O acréscimo novo:** o standard numa noite de temporada sai R$ 312,38, o valor que ele deu. O deluxe sai
  R$ 487,38, a suíte R$ 724,88, e o desconto não reembolsável incide sobre o valor novo.
- **A Páscoa:** as noites de 21/03 a 28/03/2027 e de 09/04 a 16/04/2028 cobram o acréscimo, contando os dois
  domingos. As noites vizinhas (20/03, 29/03, 08/04, 17/04) não cobram.
- **Os outros períodos:** 15/12 a fevereiro e julho continuam iguais.
- **O resto do sistema:** confirmação, e-mail, check-in e check-out, cancelamentos, relatórios, os dados depois de
  reiniciar e as reservas simultâneas funcionam como antes.

## Dúvidas

- O `README.md:31-32` ainda diz "a noite custa 20% a mais" e não cita a Páscoa. O dono disse que o README explica
  como o sistema é, então um revisor pediria para atualizá-lo. Não abaixei nenhuma nota por isso, porque não é
  código e nenhuma coluna mede a documentação. Mas é uma pendência real.
