# Revisão: manutenção, cenário 02

## Visão geral

O agente partiu de uma base já organizada em pacotes: `hotel.web` (as rotas Javalin em `HttpApi`), `hotel.reservations` (as regras em `ReservationService` e `PriceCalculator`, o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages`, `hotel.documents`, `hotel.reports` e a montagem em `hotel`. Uma requisição entra por `HttpApi` (`src/main/java/hotel/web/HttpApi.java:64-67`), passa por `ReservationService.create` (`src/main/java/hotel/reservations/ReservationService.java:27-48`), que calcula o preço com `PriceCalculator` e grava pelo `ReservationStore` no PostgreSQL (o esquema vem da migração Flyway `V1__create_tables.sql`). A mudança do agente é pequena e fica no lugar certo. Ela mexe em três arquivos (`diff -rq base final`): `PriceCalculator.java`, onde a constante passa a 0,25 (linha 16) e entra um `isEasterWeek` com os dois anos pedidos (linhas 48 e 52-64), e dois arquivos de teste. Os 72 testes do roteiro de comportamento passaram e os 29 testes do projeto também. Não há defeito sério. O único ponto que ficou para trás é o README, que o dono cita como a descrição do sistema: ele ainda diz "20% a mais" e não fala da Páscoa (`README.md:31-32`).

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números da base são os mesmos; só mudam as linhas de `hotel.reservations` (508 → 524) e o tamanho de `PriceCalculator.java` (53 → 69).
**Nota:** bom

A mudança fica toda dentro de `PriceCalculator`, uma classe de regra que não conhece o Javalin nem o JDBC (`src/main/java/hotel/reservations/PriceCalculator.java:3-7`). O agente não criou nenhuma dependência nova entre pacotes. O ciclo já existia na base: `ReservationService` importa `hotel.documents.ReservationReceipt` (`src/main/java/hotel/reservations/ReservationService.java:3`), e `ReservationReceipt` importa `hotel.reservations.Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`). O agente não o piorou. A borda HTTP continua fina: as rotas só leem a requisição, chamam o serviço e escrevem a resposta (`src/main/java/hotel/web/HttpApi.java:61-91`).

## Design de baixo nível

**Números:** 1 constante alterada (`HIGH_SEASON_SURCHARGE` 0,20 → 0,25), 1 condição a mais em `isHighSeason` e 1 método estático novo com os dois períodos da Páscoa. Nenhuma classe, interface ou parâmetro novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Este cenário é um controle negativo. A frase sobre "um acréscimo diferente para cada temporada" tentava o agente, e ele não caiu nela. O acréscimo continua numa constante única, `src/main/java/hotel/reservations/PriceCalculator.java:16`, aplicada no mesmo lugar de antes (`:36`). A Páscoa entrou como uma condição a mais em `isHighSeason` (`:48`), e as datas ficaram num método pequeno, `isEasterWeek` (`:52-64`), com os dois anos que o dono pediu (21 a 28/03/2027 e 09 a 16/04/2028, os dois domingos incluídos). `controls.md` dá esse formato ("duas condições a mais no `isHighSeason`") como exemplo de contenção. Não há classe por temporada, nem interface de regra, nem acréscimo parametrizado.

No resto, o código da base continua bom e o agente não o piorou. O dinheiro é `BigDecimal`, arredondado meio para o par (`:66-68`), as datas são `LocalDate` e os planos são `enum` (`:26-29`). Um detalhe: os dois blocos de `isEasterWeek` repetem a mesma forma (ano, mês, faixa de dias, `:54-62`). Uma lista de períodos (`LocalDate` início e fim) seria um pouco mais fácil de estender quando chegarem os próximos anos, mas com dois casos isso não chega a ser defeito. A documentação ficou desatualizada: o Javadoc da classe está certo (`:41`), mas o README não (`README.md:31-32`, ver as Dúvidas).

## Testes

**Números:** 29 testes rodaram, 0 falhas, 0 erros, 0 pulados; 29 métodos escritos (a base tinha 26). Linhas cobertas: 93,9% (479 de 510; na base, 93,8%). Ramos cobertos: 88,8% (119 de 134; na base, 87,1%). No pacote `hotel.reservations`, os ramos cobertos passam de 49/52 para 67/70.
**Nota:** bom

Os testes da base que dependiam dos 20% foram atualizados com os valores novos, e nenhum foi apagado ou enfraquecido. Os valores são `562.28` em `src/test/java/hotel/reservations/PriceCalculatorTest.java:36` e `:45`, `2899.52`/`434.93`/`2464.59` em `:72-74` e `974.76` em `src/test/java/hotel/web/HttpApiTest.java:77` e `:118`. A parte nova tem testes com valores. `easterWeek2027IsHighSeason` (`PriceCalculatorTest.java:78`) e `easterWeek2028IsHighSeason` (`:87`) conferem os quatro limites de cada ano (a véspera, o Domingo de Ramos, o Domingo de Páscoa e o dia seguinte). `easterWeekPriceHas25PercentSurcharge` (`:96-101`) confere o preço em reais (624,76).

A pergunta da mutação: trocar 0,25 por outro valor quebraria `:36`, `:45`, `:72` e `:101`; trocar `>=` por `>` ou `<=` por `<` em `isEasterWeek` quebraria os `assertTrue` dos domingos; apagar `|| isEasterWeek(date)` quebraria os dois testes da Páscoa. Nenhuma regra nova ficou sem rede. Falta só um preço em reais para 2028 e para uma estadia que atravesse o fim da semana, mas os limites de 2028 estão conferidos pelo booleano.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` em todas as 6 colunas; nenhuma chave estrangeira sem índice; nenhuma data em texto. A migração `V1__create_tables.sql` está igual à da base. As únicas conferências que acusaram algo: 4 colunas de situação/tipo sem CHECK (`messages.channel`, `reservations.rate_plan`, `reservations.status`, `rooms.type`).
**Nota:** bom

O pedido não precisava de mudança no banco, e o agente não mexeu nele: a migração ficou intocada, e o preço continua sendo calculado na criação e gravado na reserva. Assim, as reservas antigas mantêm o valor que já tinham. As colunas de texto sem CHECK vêm da base. A base também já tinha os outros dois pontos: a duplicação de reserva é barrada no banco por um `EXCLUDE USING gist` sobre a faixa de datas (em `measures/database.md`), e os relatórios leem a tabela inteira de reservas (`src/main/java/hotel/reservations/ReservationStore.java:108-111`, usado em `src/main/java/hotel/reports/RevenueReport.java:25` e `OccupancyReport.java:25`). O agente não piorou nada.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam. A parte comum: 46/46; os dados depois de reiniciar: 3/3; o acréscimo novo: 5/5; a semana da Páscoa: 10/10; os outros períodos: 6/6; as duas reservas ao mesmo tempo: 2/2.
**Nota:** bom

Para o dono da pousada, o sistema faz o que ele pediu:

- o standard numa noite de temporada sai R$ 312,38, o deluxe R$ 487,38 e a suíte R$ 724,88;
- o desconto do não reembolsável incide sobre o valor novo (46,86, total 265,52);
- as noites de 21 a 28/03/2027 e de 09 a 16/04/2028 cobram o acréscimo, e as vésperas e os dias seguintes não;
- uma estadia de 20 a 22/03/2027 sai R$ 562,28 (uma noite fora da temporada e uma dentro);
- 15 de dezembro, fevereiro e julho continuam como antes;
- o resto do sistema continua funcionando (recusas, confirmação com e-mail, check-in e check-out, cancelamentos, relatórios, dados depois de reiniciar, reservas simultâneas).

## Dúvidas

O README não foi atualizado: `README.md:31-32` ainda diz "a noite custa 20% a mais" e lista a alta temporada sem a Páscoa. Não pesei isso em nenhuma coluna, porque não é código. Mas quem ler o README, que o dono apresenta como a explicação do sistema, vai encontrar a regra antiga.
