# Revisão: manutenção, cenário 02

## Visão geral

O sistema da base tem sete pacotes. `hotel` monta as peças e sobe o servidor (`src/main/java/hotel/Hotel.java:20-29`). `hotel.web` é a borda HTTP em Javalin, `hotel.reservations` tem as regras e o acesso ao banco (`ReservationService`, `PriceCalculator`, `ReservationStore`), e há ainda `hotel.rooms`, `hotel.messages`, `hotel.reports` e `hotel.documents`. Uma requisição entra por `HttpApi` (`src/main/java/hotel/web/HttpApi.java:64-67`), passa por `ReservationService.create` (`src/main/java/hotel/reservations/ReservationService.java:27-48`), que calcula o preço com o `PriceCalculator`, e é gravada pelo `ReservationStore` em JDBC num PostgreSQL. O esquema vem de uma migração do Flyway. O pedido era pontual: o acréscimo de temporada passa de 20% para 25%, e as semanas da Páscoa de 2027 e 2028 viram alta temporada. O agente mudou só o necessário. Em `PriceCalculator`, a constante foi para `0.25` (`src/main/java/hotel/reservations/PriceCalculator.java:16`) e entrou um método `isEasterWeek` com os dois períodos como dado (`PriceCalculator.java:57-64`). Os testes foram ajustados aos novos valores, e entraram três testes novos. Não achei nada sério: os 29 testes passam e as 72 conferências da API também. O único ponto que ficou para trás é o `README.md`, que o dono citou como a descrição do sistema e que continua dizendo "a noite custa 20% a mais", sem falar da Páscoa (`README.md:31-32`). O agente não o atualizou (o `diff` entre `base/README.md` e `final/README.md` volta vazio).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais na base e na final. Um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). O pacote `hotel.reservations` passou de 508 para 524 linhas, e `PriceCalculator` de 53 para 69.
**Nota:** bom

O agente não mexeu na arquitetura: a mudança fica toda dentro de `hotel.reservations/PriceCalculator.java`, a classe que já calculava o preço. A borda HTTP continua fina, só lendo a requisição e chamando o serviço (`src/main/java/hotel/web/HttpApi.java:61-79`). As regras não usam Javalin. O SQL fica isolado no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:30-121`), e tudo é montado num lugar só (`src/main/java/hotel/Hotel.java:20-29`). O ciclo `documents` ↔ `reservations` veio da base: `ReservationService` cria o `ReservationReceipt` (`ReservationService.java:3,64`), e o recibo lê a `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`). O agente não o piorou.

## Design de baixo nível

**Números:** duas linhas de regra mudaram: a constante (`PriceCalculator.java:16`) e uma condição a mais em `isHighSeason` (`PriceCalculator.java:53`). Entrou um método de pacote, `isEasterWeek`, com os dois períodos (`PriceCalculator.java:57-64`). Nenhuma classe, interface ou tipo novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Este é o controle negativo. O texto tenta o agente com "um acréscimo diferente para cada temporada", e o agente não caiu nisso. O acréscimo continua numa constante só (`PriceCalculator.java:16`), aplicado no mesmo `nightPrice` de antes (`PriceCalculator.java:33-39`). As datas da Páscoa entraram como dado, em duas condições sobre `LocalDate` (`PriceCalculator.java:58-63`), que é exatamente a contenção que a referência descreve. Não há estratégia por temporada, nem interface de regra de temporada, nem acréscimo parametrizado. Os dois domingos estão incluídos (`!isBefore` / `!isAfter`), como o dono pediu. Dois detalhes menores, que não pesam:

- a checagem `date.getYear() == 2027` / `== 2028` (`PriceCalculator.java:58,61`) sobra, porque os limites de data já restringem o ano;
- uma lista de períodos ficaria um pouco mais fácil de estender quando o dono mandar os próximos anos. Mesmo assim, acrescentar um ano hoje é acrescentar duas linhas no mesmo lugar.

O resto do código vem da base e continua limpo. O dinheiro é `BigDecimal`, arredondado com `HALF_EVEN` (`PriceCalculator.java:66-68`), as datas são `LocalDate` e o plano e a situação são `enum` (`switch` em `PriceCalculator.java:26-29` e `ReservationService.java:82-85`). Os erros têm código próprio (`ReservationService.java:133-137`) e as falhas de SQL não são engolidas (`ReservationStore.java:54-60`).

## Testes

**Números:** final com 29 testes rodados, 0 falhas, 0 erros, 0 pulados e 29 métodos escritos. Linhas cobertas: 479 de 510 (93,9%); ramos: 113 de 128 (88,3%). Na base eram 26 testes, com 93,8% das linhas e 87,1% dos ramos. Em `hotel.reservations`, os ramos cobertos foram de 49/52 para 61/64.
**Nota:** bom

O agente atualizou com valores os testes que dependiam dos 20%, e não os enfraqueceu:

- `highSeasonStartsOnDecember15` → 562.28 (`src/test/java/hotel/reservations/PriceCalculatorTest.java:36`);
- `highSeasonEndsWithFebruary` → 562.28 (`PriceCalculatorTest.java:45`);
- `nonRefundableDiscountIsOnTheHighSeasonPrice` → 2899.52 / 434.93 / 2464.59 (`PriceCalculatorTest.java:72-74`);
- os dois valores da API → 974.76 (`src/test/java/hotel/web/HttpApiTest.java:77,118`).

Também acrescentou três testes para a parte nova:

- os limites das duas Páscoas, com a véspera, o primeiro domingo, um dia do meio, o último domingo e o dia seguinte (`PriceCalculatorTest.java:78-94`);
- um preço que atravessa a entrada da Páscoa, 874.66 (`PriceCalculatorTest.java:96-101`).

Na pergunta da mutação, voltar `0.25` para `0.20` derruba vários testes. Trocar `!isAfter` por `isBefore` derruba os asserts do dia 28/03 e do dia 16/04. Apagar um dos dois anos também derruba um teste. O único caso sem conferência é que a Páscoa de um ano não listado (2029, por exemplo) continue baixa temporada. Como o código só conhece os dois anos, o risco é pequeno.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` nas 6 colunas e datas em `date`. Nenhuma chave estrangeira sem índice. O `EXCLUDE USING gist` protege contra a reserva dupla. A migração `V1__create_tables.sql` está igual à da base, e nenhuma migração nova entrou. A conferência acusou só que as 4 colunas de situação e tipo (`status`, `rate_plan`, `type`, `channel`) não têm `CHECK`.
**Nota:** bom

O pedido não mexe no banco, e o agente não tocou nele, o que está certo: o preço fica gravado na reserva (`src/main/resources/db/migration/V1__create_tables.sql:30-32`). Por isso, as reservas já feitas continuam com o valor de quando foram criadas, e não há dado a migrar. O que a conferência acusou veio da base: as colunas de texto sem `CHECK` (`V1__create_tables.sql:4,28,33,46`) e os relatórios que trazem a tabela inteira para filtrar em Java (`src/main/java/hotel/reports/RevenueReport.java:25`, `src/main/java/hotel/reports/OccupancyReport.java:25`). O SQL usa parâmetros, e o que entra com `+` é a constante `COLUMNS` (`ReservationStore.java:18,98,111`). As conexões são abertas em try-with-resources sobre um pool.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam:

- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o acréscimo novo: 5 de 5;
- a semana da Páscoa: 10 de 10;
- o resto dos períodos: 6 de 6;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Tudo o que o dono pediu aparece pela API:

- o standard numa noite de julho sai R$ 312,38, o deluxe R$ 487,38 e a suíte R$ 724,88;
- o não reembolsável em alta tem desconto de R$ 46,86 sobre o valor novo;
- as noites de 21/03 a 28/03/2027 e de 09/04 a 16/04/2028 custam o preço de temporada, e as vésperas e os dias seguintes custam o normal;
- 15/12, fevereiro e julho continuam como antes;
- nada do que já funcionava quebrou, nem os dados depois de reiniciar, nem a recusa da segunda reserva simultânea.

## Dúvidas

O `README.md` desatualizado ("20% a mais", sem a Páscoa, em `README.md:31-32`) não cabe bem em nenhuma das cinco colunas. Por isso não mudou nenhuma nota, mas é a única coisa que eu pediria para corrigir antes de aceitar.
