# Revisão: manutenção, cenário 02

## Visão geral

A base é um serviço Javalin com a montagem em `hotel/Hotel.java:25`. Uma requisição entra pelas rotas de `hotel.web` (`HttpApi`), vai para `ReservationService` (`hotel.reservations`), que calcula o preço com `PriceCalculator` e grava pelo `ReservationStore` (JDBC, com pool) num PostgreSQL. O esquema vem de uma migração do Flyway (`V1__create_tables.sql`). O pedido era pontual: o acréscimo passa de 20% para 25%, e as duas semanas de Páscoa (2027 e 2028) passam a contar como alta temporada. No comportamento, o agente acertou tudo: 72 de 72 conferências passaram, e os 27 testes passam. Para isso, porém, ele criou uma estrutura que ninguém pediu. Há agora um pacote novo, `hotel.reservations.seasons`, com uma interface `Season`, uma classe por temporada (`SummerSeason`, `JulyHolidays`, `EasterWeek`) e um `SeasonCalendar`. Cada classe tem o seu próprio acréscimo, que o texto diz não querer "por enquanto". E o 0,25 ficou copiado em três lugares. É exatamente o exagero que este controle negativo existe para detectar. Nos testes, o preço das noites de Páscoa nunca é calculado. O README não foi atualizado e continua dizendo 20% (`README.md`, seção "Preço").

## Arquitetura

**Números:** 8 pacotes (7 na base), 18 dependências entre pacotes (17 na base) e um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). O pacote novo `hotel.reservations.seasons` tem 5 classes e 97 linhas. Ele não depende de nenhum pacote do projeto, e só `hotel.reservations` o usa.
**Nota:** bom

Pelo jdeps, a mudança não estraga a arquitetura. O pacote novo é uma folha, sem Javalin e sem JDBC, e só `PriceCalculator` o usa (`src/main/java/hotel/reservations/PriceCalculator.java:3-4`). O ciclo vem da base: `ReservationService` importa `ReservationReceipt` (`src/main/java/hotel/reservations/ReservationService.java:3`), e `ReservationReceipt` importa `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`). O agente não o piorou. Um ponto pequeno: o calendário é criado num `static final` dentro do calculador (`PriceCalculator.java:18`), e não na montagem (`Hotel.java:25`). Como o objeto é imutável, isso não pesa. As camadas da base (HTTP, regras, dados) continuam como estavam.

## Design de baixo nível

**Números:** a mudança pedida cabia em uma constante e em duas condições ou uma lista de datas. O agente entregou 1 interface, 3 implementações (uma por temporada) e 1 calendário: 5 classes novas e 97 linhas. O acréscimo de 0,25 aparece repetido em 3 lugares.
**Classe:** exagero
**Exagero:** exagero
**Nota:** com problemas

- **O que o critério pedia:** os critérios de `references/controls.md` para o cenário 02 chamam de contenção a constante que vira 0,25 e as datas da Páscoa como dado. Chamam de exagero "uma classe ou estratégia por temporada; uma interface de regra de temporada; um acréscimo por temporada, parametrizado".
- **O que o agente fez:** fez as três coisas.
  - Criou uma interface de regra de temporada, com `includes` e `surcharge` (`src/main/java/hotel/reservations/seasons/Season.java:7-12`).
  - Criou uma classe por temporada (`SummerSeason.java:8`, `JulyHolidays.java:8`, `EasterWeek.java:7`).
  - Deu a cada temporada o seu acréscimo (`surcharge()` em `SummerSeason.java:18-20`, `JulyHolidays.java:16-18` e `EasterWeek.java:23-25`). O preço passou a vir da temporada (`PriceCalculator.java:37-39`).
- **O que já funcionava e foi refeito:** além disso, ele refatorou para essa estrutura a regra de dezembro a fevereiro e a de julho, que o pedido não mandava mudar além do percentual. O `isHighSeason` da base, com as quatro condições, virou duas classes. O próprio comentário do calendário anuncia a intenção de crescer ("a new season is one more entry here", `SeasonCalendar.java:7`).

O resto do código vem da base, que está limpa: `BigDecimal` com meio para o par em `cents` (`PriceCalculator.java:48-50`), `LocalDate` e `enum` para o plano. O que a mudança acrescentou de defeito:

- **O acréscimo duplicado:** o 0,25 está escrito três vezes (`SummerSeason.java:19`, `JulyHolidays.java:17`, `EasterWeek.java:24`). Ninguém pediu um acréscimo por temporada, e o dono disse que ele "é o mesmo para todas". Quando o percentual mudar de novo, alguém precisa achar e trocar os três, e nenhum teste avisa se o da Páscoa ficar para trás (ver Testes). Na base era uma constante só.
- **O dono do conceito:** a constante `HIGH_SEASON_SURCHARGE` sumiu de `PriceCalculator`. Para saber o acréscimo, agora é preciso abrir três classes.
- **O README:** continua dizendo 20% e só os dois períodos antigos.

## Testes

**Números:** 27 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 27 métodos escritos; a base tinha 26. A cobertura de linhas é 93,8% (488 de 520; na base, 93,8%) e a de ramos é 87,5% (105 de 120; na base, 87,1%). No pacote novo, `hotel.reservations.seasons`, 18 de 19 linhas e 14 de 14 ramos estão cobertos. A linha que falta é `EasterWeek.java:24`, o acréscimo da Páscoa (`final/target/site/jacoco/jacoco.xml`, mi=5, ci=0).
**Nota:** com problemas

O agente manteve os 26 testes da base e atualizou os valores esperados para 25%, com as contas no comentário:

- 562,28 na entrada da temporada, em 15/12 (`src/test/java/hotel/reservations/PriceCalculatorTest.java:35-36`);
- 562,28 no fim de fevereiro de ano bissexto (`PriceCalculatorTest.java:44-45`);
- 2899,52, 434,93 e 2464,59 na suíte não reembolsável (`PriceCalculatorTest.java:83-86`);
- 974,76 pela API e no relatório financeiro (`src/test/java/hotel/web/HttpApiTest.java:74` e `:115`).

Nenhum teste foi enfraquecido. Ele também acrescentou `easterWeekIsHighSeason` (`PriceCalculatorTest.java:57-66`), que confere as bordas das duas semanas (20/21 e 28/29 de março de 2027, 8/9 e 16/17 de abril de 2028).

O problema está em como o próprio design dele se encaixa nos testes. Esse teste novo só pergunta `isHighSeason`, e nenhum teste calcula o preço de uma noite de Páscoa. Na pergunta da mutação:

- **Se o 0,25 da Páscoa virasse 0,20** (`EasterWeek.java:24`), nenhum teste falharia. A linha nem é executada, como mostra o JaCoCo.
- **Se o 0,25 de julho mudasse** (`JulyHolidays.java:17`), só o `HttpApiTest` pegaria.
- **Se a regra do verão mudasse**, os testes de preço pegariam.

O acréscimo, que é metade do pedido, fica sem rede justamente na temporada nova.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)`, e não há chave estrangeira sem índice nem data em texto. O `EXCLUDE` com gist impede duas reservas do mesmo quarto em noites que se sobrepõem. A migração `V1__create_tables.sql` está igual à da base, e nenhuma migração nova foi criada. Única ressalva: as colunas de situação e de tipo (`status`, `rate_plan`, `type`, `channel`) estão em `text` sem `CHECK`.
**Nota:** bom

O pedido não tocava o banco, e o agente não o tocou: a migração está intacta (`src/main/resources/db/migration/V1__create_tables.sql`, "igual" em `measures/database.md`). A falta de `CHECK` nas colunas de situação vem da base. Os SQL montados com `+` em `src/main/java/hotel/reservations/ReservationStore.java:33,83,98,111` só juntam constantes (`COLUMNS` e texto fixo), e os dados entram por `?`. Nada do que o agente fez muda o que o banco guarda.

## Comportamento

**Números:** 72 de 72 conferências passaram, nenhuma falhou:

| parte | passaram |
|---|---|
| parte comum | 46 de 46 |
| depois de reiniciar | 3 de 3 |
| o acréscimo novo | 5 de 5 |
| a semana da Páscoa | 10 de 10 |
| os outros períodos | 6 de 6 |
| reservas simultâneas | 2 de 2 |

**Nota:** bom

O sistema cobra o que o dono pediu:

- **O acréscimo novo:** a noite de standard em alta sai a R$ 312,38, a deluxe a R$ 487,38 e a suíte a R$ 724,88. O desconto do não reembolsável incide sobre o valor novo (R$ 46,86 de desconto, total de R$ 265,52).
- **A Páscoa:** as noites de 21/03 a 28/03/2027 e de 09/04 a 16/04/2028, contando os dois domingos, custam R$ 312,38. As noites vizinhas continuam a R$ 249,90.
- **Os outros períodos:** 15/12 a fevereiro e julho continuam como estavam.

Tudo o que já funcionava continua funcionando: a parte comum, os dados depois de reiniciar e as reservas simultâneas.

## Dúvidas

Não ficou dúvida sobre a classe: a interface e as classes por temporada são o exemplo que `controls.md` dá de exagero. Fica uma dúvida sobre a nota de design, entre "com problemas" e "bom". A única falha concreta de design de baixo nível que a mudança trouxe é o acréscimo copiado em três classes, e o resto do código é limpo e veio da base. Dei "com problemas" porque essa cópia é uma regra que o dono disse ser única, e o teste não protege a cópia da Páscoa.
