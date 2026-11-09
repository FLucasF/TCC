# Revisão: manutenção, cenário 02

## Visão geral

O projeto vem da base e o agente não mexeu na estrutura. Uma requisição entra pelas rotas Javalin em `hotel.web` (`src/main/java/hotel/web/HttpApi.java:61-91`). Elas chamam o `ReservationService` (`src/main/java/hotel/reservations/ReservationService.java`), que usa o `PriceCalculator` para o preço e o `ReservationStore` (JDBC com parâmetros) para gravar no PostgreSQL. O esquema sai de uma migração Flyway (`src/main/resources/db/migration/V1__create_tables.sql`), e tudo é montado em `src/main/java/hotel/Hotel.java:278-287`. A mudança foi a menor possível e está no lugar certo: só `PriceCalculator.java` mudou no código de produção. A constante foi de 0,20 para 0,25 (`PriceCalculator.java:16`) e as duas semanas da Páscoa entraram no `isHighSeason` (`PriceCalculator.java:50-67`). Os testes foram ajustados e ganharam três métodos novos. Não há nada sério. Os problemas são pequenos: o README continua dizendo "20% a mais" e não fala da Páscoa (`README.md:31-32`), e o trecho novo tem uma checagem de mês repetida e mistura `Month` com `int`.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), tudo igual na base e na final (`measures/dependencies.md` e `measures/dependencies-base.md`). A única diferença é o tamanho de `hotel.reservations`, de 508 para 527 linhas: o `PriceCalculator` passou de 53 para 72 linhas.
**Nota:** bom

As rotas são finas: leem a requisição, chamam o serviço e escrevem a resposta (`src/main/java/hotel/web/HttpApi.java:64-90`). As regras não conhecem o Javalin. O SQL está isolado em `ReservationStore`, `Rooms` e `SentMessages`, e a montagem fica num lugar só (`src/main/java/hotel/Hotel.java:278-287`). O ciclo `hotel.documents` ↔ `hotel.reservations` já existia na base: `ReservationService` cria o `ReservationReceipt`, que lê a `Reservation`. Ele não é do agente. A regra nova ficou dentro do `PriceCalculator`, onde a temporada já morava, sem dependência nova.

## Design de baixo nível

**Números:** um arquivo de produção mudou (`PriceCalculator.java`, 19 linhas a mais), nenhuma classe, interface ou tipo novo. O acréscimo continua sendo uma constante só para todas as temporadas (`PriceCalculator.java:16`). As duas semanas da Páscoa são duas condições a mais, chamadas a partir de `isHighSeason` (`PriceCalculator.java:61-65`).
**Classe:** contenção
**Exagero:** contenção
**Nota:** com problemas

É um controle negativo. O pedido traz a isca "a gente já pensou em ter um acréscimo diferente para cada temporada, mas por enquanto é o mesmo para todas", e o agente não caiu nela. Não criou uma classe por temporada, nem uma interface de regra de temporada, nem um acréscimo parametrizado por período. Ficou exatamente no que `references/controls.md` chama de contenção: a constante virou 0,25 e as datas da Páscoa entraram como duas condições a mais no `isHighSeason`. Não há estrutura sem necessidade em lugar nenhum, então o exagero também é `contenção`.

Os defeitos são pequenos e todos estão no trecho novo:
- **Mês conferido duas vezes.** `isHighSeason` filtra março e abril (`PriceCalculator.java:50`) antes de chamar `isEasterSeason`, que confere o mês de novo (`PriceCalculator.java:62` e `:64`). O filtro de fora é redundante.
- **Dois jeitos de representar o mês.** O método de cima usa o enum `Month` (`PriceCalculator.java:43-47`), e o de baixo usa números soltos (`getMonthValue()`, `3`, `4`, `21`, `28`, em `PriceCalculator.java:57-65`). Duas faixas de `LocalDate` (início e fim de cada semana), numa lista ou em constantes com nome, diriam a mesma coisa de um jeito mais legível e mais fácil de estender: o dono já avisou que vai mandar os próximos anos.
- **O README ficou desatualizado.** O pedido diz que quem fez o sistema deixou um README explicando como ele é, e esse README ainda diz que na alta temporada "a noite custa 20% a mais" e lista só dezembro/fevereiro e julho (`README.md:31-32`). O comentário da classe também não fala do acréscimo nem da Páscoa (`PriceCalculator.java:9-13`). A documentação agora contradiz o código.

O resto do código vem da base e está bem resolvido: dinheiro em `BigDecimal` com `HALF_EVEN` (`PriceCalculator.java:69-71`), datas em `LocalDate`, `enum` para o plano e para a situação da reserva, e erros tipados em `HotelException` com `ErrorCode`. Nada disso foi piorado.

## Testes

**Números:** 29 testes rodaram, 0 falharam e 0 foram pulados; 29 métodos escritos (a base tinha 26, todos mantidos). Cobertura de linhas: 93,8% (482 de 514; na base, 93,8%, 471 de 502). Cobertura de ramos: 88,2% (120 de 136; na base, 87,1%, 101 de 116). Em `hotel.reservations`, 68 de 72 ramos estão cobertos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** bom

O agente testou as duas partes do pedido com valores:
- **O acréscimo novo:** os testes que já existiam foram atualizados para 25%, com a conta no comentário. Exemplos: 562,28 em `src/test/java/hotel/reservations/PriceCalculatorTest.java:36` e `:45`; subtotal 2.899,52, desconto 434,93 e total 2.464,59 no não reembolsável em alta (`PriceCalculatorTest.java:98-100`); 974,76 pela API e no relatório financeiro (`src/test/java/hotel/web/HttpApiTest.java:77` e `:118`).
- **A Páscoa:** as duas pontas de cada ano, por dentro e por fora (`PriceCalculatorTest.java:57-69`), e o preço de duas noites dentro da semana, 624,76 (`PriceCalculatorTest.java:73-79`).

Os testes da base não foram enfraquecidos: só os valores esperados mudaram, porque a regra mudou.

A pergunta da mutação:
- Voltar 0,25 para 0,20 faz falhar pelo menos quatro testes.
- Trocar `day >= 21` por `day > 21`, ou `day <= 16` por `day < 16`, faz falhar `PriceCalculatorTest.java:59` ou `:68`.
- Fica sem rede: nenhum teste confere uma data de março ou abril de outro ano (por exemplo, 09/04/2029). Um erro que ignorasse o ano em `PriceCalculator.java:61-65` passaria. É uma lacuna pequena.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` nas seis colunas, sem chave estrangeira sem índice e sem datas em texto. A única migração (`V1__create_tables.sql`) está igual à da base. As conferências acusaram só `status`, `rate_plan`, `type` e `channel` em `text` sem `CHECK` (`measures/database.md`).
**Nota:** bom

O agente não tocou no banco, e o pedido não precisava disso. A migração da base não foi editada, então um banco que já roda o sistema sobe sem problema. O esquema que vem da base é sólido:
- dinheiro em `numeric(10,2)` (`src/main/resources/db/migration/V1__create_tables.sql:6`, `:30-35`);
- a reserva ligada ao quarto por chave estrangeira (`V1__create_tables.sql:21`);
- uma restrição `EXCLUDE` que impede o mesmo quarto em noites sobrepostas mesmo com pedidos simultâneos (`V1__create_tables.sql:38-40`);
- SQL só com parâmetros: o que se concatena é a constante `COLUMNS` (`src/main/java/hotel/reservations/ReservationStore.java:18`, `:98`, `:111`);
- conexões em try-with-resources sobre um pool HikariCP (`src/main/java/hotel/Database.java:327-331`).

Os pequenos defeitos vêm todos da base: as colunas de situação e de plano sem `CHECK`, e os relatórios que leem a tabela de reservas inteira para filtrar em Java (`src/main/java/hotel/reports/OccupancyReport.java:25`, `src/main/java/hotel/reports/RevenueReport.java:25`).

## Comportamento

**Números:** 72 de 72 conferências passaram, nenhuma falhou (`measures/behavior.md`):
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o acréscimo novo: 5 de 5;
- a semana da Páscoa: 10 de 10;
- os outros períodos continuam: 6 de 6;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- O standard numa noite de temporada sai R$ 312,38, o deluxe R$ 487,38 e a suíte R$ 724,88.
- O não reembolsável em alta aplica o desconto sobre o valor novo: R$ 46,86 de desconto e total de R$ 265,52.
- As noites de 21/03 a 28/03/2027 e de 09/04 a 16/04/2028 são de temporada, com os dois domingos incluídos, e as noites vizinhas não são.
- Uma estadia de 20 a 22/03/2027 cobra uma noite normal e uma de temporada (R$ 562,28).
- Dezembro a partir do dia 15, fevereiro e julho continuam como antes.
- O resto do sistema segue igual ao que era: reservas, confirmação com e-mail, check-in e check-out, cancelamentos e devoluções, relatórios, os dados depois de reiniciar, e a recusa da segunda reserva simultânea do mesmo quarto.

## Dúvidas

- A nota do design ficou entre `bom` e `com problemas`. O código novo está certo e no lugar certo; o que pesou foi o README que contradiz o código (`README.md:31-32`), somado à checagem de mês repetida e aos números soltos em `PriceCalculator.java:50-65`. São defeitos pequenos e localizados, mas um revisor exigente pediria para corrigir antes de aceitar, sobretudo o README.
- Nenhum teste nem conferência diz o que acontece na semana da Páscoa de 2029 em diante: hoje ela é cobrada como baixa temporada. Isso é o que o dono pediu ("pode deixar só esses dois anos"), mas o sistema não avisa quando os anos cadastrados acabam.
