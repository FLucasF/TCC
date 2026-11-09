# Revisão: manutenção, cenário 02

## Visão geral

A base é um serviço Javalin em 7 pacotes. `hotel.web.HttpApi` lê a requisição e chama `hotel.reservations.ReservationService`, que usa `PriceCalculator` para o preço e `ReservationStore` (JDBC com HikariCP) para gravar no PostgreSQL. O esquema vem de uma migração Flyway (`V1__create_tables.sql`), e as peças são ligadas em `Hotel.java:21-27`. O pedido era pontual: o acréscimo da alta temporada passa a 25%, e as semanas da Páscoa de 2027 e 2028 entram como alta temporada. O agente mudou só `PriceCalculator.java`: a constante foi para 0,25 em `PriceCalculator.java:16`, e o método `isEasterWeek` (`PriceCalculator.java:53-65`) tem as duas faixas de datas. Ele também acertou os valores esperados nos testes que já existiam e escreveu 3 testes novos. Não criou estrutura nova para "um acréscimo por temporada", que o texto só cita como ideia futura. As 72 conferências da API passaram, e os 29 testes também. O único ponto em aberto é pequeno: o README não foi atualizado e continua dizendo "20% a mais", sem falar da Páscoa (`README.md:31-32`).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Há 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que também está em `measures/dependencies-base.md`. A única diferença em relação à base é `hotel.reservations`, que passou de 508 para 525 linhas, e `PriceCalculator`, de 53 para 70.
**Nota:** bom

O agente não mexeu na estrutura: a mudança ficou toda em `src/main/java/hotel/reservations/PriceCalculator.java`, o lugar onde a regra de preço já ficava. A classe não conhece Javalin nem JDBC (só importa `hotel.rooms.Room` e `java.*`, `PriceCalculator.java:3-7`). O ciclo vem da base: `ReservationService.java:3` importa `hotel.documents.ReservationReceipt`, e `ReservationReceipt.java:3` importa `hotel.reservations.Reservation`. O agente não o piorou. A borda HTTP continua fina (`HttpApi.java:61-91`), e a montagem continua num lugar só (`Hotel.java:21-27`).

## Design de baixo nível

**Números:** 1 classe de produção mudou, sem nenhuma classe, interface ou enum novo. A mudança tem 1 constante alterada e 1 método estático novo, com 2 condições de data.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É um controle negativo, e o texto tenta o agente com "um acréscimo diferente para cada temporada". O agente fez o que a referência chama de contenção. Mudou o valor de `HIGH_SEASON_SURCHARGE` para `0.25` (`PriceCalculator.java:16`) e manteve um acréscimo único, aplicado em `nightPrice` (`PriceCalculator.java:33-39`). A Páscoa entrou como duas condições a mais, que `isHighSeason` chama (`PriceCalculator.java:50`, `:58-63`). Não há regra de temporada abstrata, nem classe por temporada, nem acréscimo parametrizado.

O código novo tem dois detalhes de estilo, sem efeito no comportamento:
- `isEasterWeek` compara números (`getMonthValue()`, `month == 3`, `PriceCalculator.java:55-61`), enquanto o método logo acima usa o enum `Month` (`PriceCalculator.java:43-47`);
- o padrão `if (...) return true; ... return false;` (`PriceCalculator.java:58-64`) poderia ser uma só expressão.

As datas fixas no código são o que o dono pediu ("pode deixar só esses dois anos"). O resto do código, herdado da base, é limpo: `BigDecimal` com HALF_EVEN, `LocalDate`, enums para plano e situação, e erros tipados com `HotelException` e `ErrorCode`. O agente não piorou nada disso.

## Testes

**Números:** 29 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 29 métodos de teste no código (a base tinha 26). A cobertura de linhas foi de 93,8% para 93,9% (481 de 512), e a de ramos, de 87,1% para 88,6% (117 de 132). Em `hotel.reservations`, os ramos cobertos passaram de 49 de 52 para 65 de 68.
**Nota:** bom

O agente atualizou, com valores calculados, os testes da base que dependiam dos 20%:
- `PriceCalculatorTest.java:36` e `:45`: 562.28;
- `PriceCalculatorTest.java:72-74`: subtotal 2899.52, desconto 434.93 e total 2464.59;
- `HttpApiTest.java`, na reserva de julho e no relatório financeiro: 974.76.

Nenhum desses testes foi apagado ou enfraquecido. Os testes novos conferem as duas bordas de cada semana da Páscoa: a véspera fora, o primeiro e o último domingo dentro e o dia seguinte fora (`PriceCalculatorTest.java:78-82` e `:86-90`). Também conferem o preço de uma noite de Páscoa, R$ 312,38 (`PriceCalculatorTest.java:94-99`).

A pergunta da mutação:
- trocar 0.25 por outro valor quebra vários testes;
- trocar `<=` por `<` ou `>=` por `>` nas faixas da Páscoa quebra os testes das bordas;
- uma mutação passaria: apagar a comparação de ano (`year == 2027`) em `PriceCalculator.java:58`. Ninguém confere que 21/03 de outro ano continua fora da temporada.

É uma lacuna pequena e não muda a nota.

## Banco

**Números:** 3 tabelas, todas com chave primária. Todo o dinheiro está em `numeric(10,2)`, nenhuma chave estrangeira está sem índice e nenhuma data está guardada como texto. A migração `V1__create_tables.sql` é igual à da base. A única coisa que o script acusou foram as colunas de situação e de tipo sem CHECK (`status`, `rate_plan`, `type`, `channel`).
**Nota:** bom

O agente não mexeu no banco, e o pedido não precisava. As colunas `text` sem CHECK vêm da base (`V1__create_tables.sql:28`, `:33`), assim como a leitura da tabela inteira de reservas (`ReservationStore.java:111`). A duplicação de reserva é impedida pelo próprio banco, com um `EXCLUDE USING gist` sobre o quarto e o intervalo de datas (`V1__create_tables.sql:40`). Os preços ficam gravados na reserva quando ela é criada, então as reservas que já existiam mantêm o valor com 20%. É o comportamento esperado para dinheiro já combinado com o hóspede.

## Comportamento

**Números:** 72 de 72 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o acréscimo novo: 5 de 5;
- a semana da Páscoa: 10 de 10;
- o resto dos períodos: 6 de 6;
- as duas reservas simultâneas: 2 de 2.

**Nota:** bom

Nada falhou. Para o dono da pousada:
- uma noite de temporada custa agora R$ 312,38 no standard, R$ 487,38 no deluxe e R$ 724,88 na suíte;
- as noites de 21 a 28/03/2027 e de 09 a 16/04/2028 cobram o acréscimo, e as vésperas e os dias seguintes não;
- dezembro, fevereiro e julho continuam como estavam;
- o desconto do não reembolsável é calculado sobre o valor novo (R$ 46,86).

## Dúvidas

Nenhuma sobre as notas. O README desatualizado (`README.md:31-32`) não pesou em nenhuma coluna, porque é documentação e não código. Mesmo assim, é a única coisa que alguém ainda precisaria ajustar.
