# Revisão: manutenção, cenário 02

## Visão geral

O sistema da base tem sete pacotes: `hotel` (a montagem e o `main`), `hotel.web` (as rotas do Javalin), `hotel.reservations` (as regras da reserva, o preço e o acesso ao banco das reservas), `hotel.rooms`, `hotel.messages`, `hotel.reports` e `hotel.documents`. Uma requisição entra em `HttpApi`, que lê o corpo e chama `ReservationService`. O serviço consulta `Rooms` e `ReservationStore` (JDBC com HikariCP), calcula o preço em `PriceCalculator` e grava no PostgreSQL. O esquema vem de uma migração do Flyway. O pedido trazia duas mudanças: o acréscimo de temporada passa de 20% para 25%, e a semana da Páscoa de 2027 e de 2028 vira alta temporada. O agente fez só isso, no lugar onde a regra já morava:
- a constante foi para 0,25 (`final/src/main/java/hotel/reservations/PriceCalculator.java:16`);
- `isHighSeason` ganhou duas condições com as datas da Páscoa (`PriceCalculator.java:53-60`);
- quatro valores esperados dos testes foram atualizados e dois testes novos entraram.

Nenhum outro arquivo de código mudou (`diff -rq base final`). As 72 conferências do roteiro passaram, e os 28 testes também. Nada sério a apontar. O único esquecimento é o `README.md:31-32`, que continua dizendo "20% a mais" e não cita a Páscoa.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números são iguais aos da base: só `hotel.reservations` mudou de tamanho, de 508 para 522 linhas, por causa do `PriceCalculator` (de 53 para 67 linhas).
**Nota:** bom

A mudança ficou dentro de `PriceCalculator`, que não conhece nem Javalin nem JDBC (`PriceCalculator.java:3-7` só importa `Room` e tipos do Java). O agente não criou nenhuma dependência nova e não mexeu na montagem. O único ciclo já existia na base: `ReservationService` importa `hotel.documents.ReservationReceipt` (`src/main/java/hotel/reservations/ReservationService.java:3`), e `ReservationReceipt` importa `hotel.reservations.Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`). O resto da arquitetura da base é sadio:
- as rotas são finas e só chamam o serviço (`src/main/java/hotel/web/HttpApi.java:61-90`);
- o SQL fica isolado em `ReservationStore`, `Rooms` e `SentMessages`.

## Design de baixo nível

**Números:** 2 constantes/condições mudadas ou acrescentadas em 1 método; 0 classes, interfaces ou tipos novos; 3 arquivos tocados, 1 em `src/main` e 2 em `src/test`.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É um controle negativo. A frase que tenta o agente é "um acréscimo diferente para cada temporada", e o agente não caiu nela. O acréscimo continua uma constante só (`PriceCalculator.java:16`, `0.25`), aplicada no mesmo lugar (`PriceCalculator.java:36`). As duas semanas da Páscoa entraram como dado, em duas condições a mais em `isHighSeason` (`PriceCalculator.java:54` e `:58`), com os limites inclusivos pedidos (`!isBefore` e `!isAfter`, de 21 a 28/03/2027 e de 09 a 16/04/2028). Não há classe de temporada, interface de regra de temporada nem acréscimo parametrizado. É exatamente a resposta de contenção que os critérios descrevem.

No resto do código, o design da base se mantém:
- dinheiro em `BigDecimal` com arredondamento `HALF_EVEN` (`PriceCalculator.java:64-66`), datas em `LocalDate`, planos e situações em `enum` com `switch` exaustivo (`PriceCalculator.java:26-29`, `ReservationService.java:82-85`);
- erros tipados por `ErrorCode` (`HttpApi.java:100-107`).

Dois detalhes não pedem mudança antes de aceitar:
- a forma `if … return true; … return false` (`PriceCalculator.java:47-61`) é um pouco mais longa que uma expressão booleana;
- as datas da Páscoa ficam literais dentro do método, e não numa lista nomeada. Com só dois anos, as duas formas se equivalem.

## Testes

**Números:** 28 testes rodaram, 0 falhas, 0 erros, 0 pulados; 28 métodos escritos (a base tinha 26). Cobertura de linhas de 93,9% (477/508), contra 93,8% na base; de ramos, 87,9% (109/124), contra 87,1%. Em `hotel.reservations`, 57 de 60 ramos estão cobertos.
**Nota:** bom

O agente atualizou, com os valores novos e certos, os testes da base que dependiam dos 20%:
- 562,28 no início de dezembro e no fim de fevereiro (`src/test/java/hotel/reservations/PriceCalculatorTest.java:36`, `:45`);
- 2899,52, 434,93 e 2464,59 na suíte não reembolsável (`PriceCalculatorTest.java:72-74`);
- 974,76 na reserva e no relatório financeiro de julho (`src/test/java/hotel/web/HttpApiTest.java:77`, `:118`).

Nenhum teste foi apagado ou enfraquecido. Os dois testes novos conferem as bordas das duas semanas da Páscoa, um dia antes, o primeiro, um do meio, o último e um dia depois (`PriceCalculatorTest.java:78-95`).

A pergunta da mutação tem resposta em todos os casos:
- trocar 0.25 por 0.20 derruba `PriceCalculatorTest.java:36`, `:45`, `:72` e `HttpApiTest.java:77`;
- trocar `!isAfter` por `isBefore` num limite derruba `:83` ou `:93`;
- apagar o bloco de 2028 derruba `:91-93`.

Uma lacuna pequena: os testes da Páscoa conferem o booleano `isHighSeason`, não o preço de uma noite de Páscoa. O caminho do preço é o mesmo das outras temporadas, que tem teste com valores, e o roteiro de comportamento confere esse preço pela API.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` nas 6 colunas de valor; nenhuma chave estrangeira sem índice; nenhuma data em texto. Há `EXCLUDE USING gist` contra duas reservas do mesmo quarto na mesma noite. A migração `V1__create_tables.sql` está igual à da base.
**Nota:** bom

O pedido não mexia no banco, e o agente não o tocou: a única migração está igual à da base (`src/main/resources/db/migration/V1__create_tables.sql`). Também está certo não recalcular as reservas já gravadas, porque o preço fica guardado em `reservations.total` no momento da criação. O SQL usa parâmetros (`src/main/java/hotel/reservations/ReservationStore.java:33`, `:83`, `:98`); as concatenações listadas pelo script juntam só a constante `COLUMNS`.

Dois pontos menores vêm da base e o agente não os piorou:
- `status`, `rate_plan`, `rooms.type` e `messages.channel` são `text` sem `CHECK`;
- os relatórios trazem a tabela inteira de reservas para filtrar em Java (`ReservationStore.java:108-111`, usado em `src/main/java/hotel/reports/RevenueReport.java:25` e `OccupancyReport.java:25`).

## Comportamento

**Números:** 72 de 72 conferências passaram:
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- acréscimo novo: 5/5;
- semana da Páscoa: 10/10;
- os outros períodos continuam: 6/6;
- reservas simultâneas: 2/2.

**Nota:** bom

Para o dono da pousada, tudo funciona como ele pediu:
- a noite de temporada do standard sai R$ 312,38, a do deluxe R$ 487,38 e a da suíte R$ 724,88;
- o não reembolsável aplica os 15% sobre o valor novo;
- as noites de 21 a 28/03/2027 e de 09 a 16/04/2028 cobram o acréscimo, e as vizinhas (20/03, 29/03, 08/04, 17/04) não;
- os períodos antigos continuam iguais (14 e 15/12, 28/02 e 01/03, 31/07 e 01/08).

O resto do sistema segue igual: as recusas, a confirmação e o e-mail, os cancelamentos, os relatórios, os dados depois de reiniciar e a recusa da segunda reserva simultânea.

## Dúvidas

O `README.md:31-32` continua descrevendo a alta temporada antiga ("a noite custa 20% a mais", sem a Páscoa). O dono disse que o README explica o sistema, então um revisor pediria para atualizá-lo. Como é documentação, e não código, banco ou comportamento, não baixei nenhuma nota por isso. Fora esse ponto, nenhuma.
