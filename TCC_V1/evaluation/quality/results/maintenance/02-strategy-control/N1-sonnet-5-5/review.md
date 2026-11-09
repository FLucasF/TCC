# Revisão: manutenção, cenário 02

## Visão geral

O sistema (já pronto na base) tem sete pacotes: `hotel.web` (as rotas Javalin em `HttpApi`), `hotel.reservations`
(as regras em `ReservationService`, o preço em `PriceCalculator`, o acesso ao banco em `ReservationStore`),
`hotel.rooms`, `hotel.messages`, `hotel.documents`, `hotel.reports` e `hotel` (a montagem). Uma requisição entra em
`HttpApi` (`src/main/java/hotel/web/HttpApi.java:64-67`), vai para `ReservationService.create`, que calcula o preço
com `PriceCalculator` e grava por `ReservationStore` no PostgreSQL (migração Flyway `V1__create_tables.sql`). O
pedido era pontual: o acréscimo da temporada de 20% para 25% e a semana da Páscoa de 2027 e 2028 como alta
temporada. O agente mudou a constante (`src/main/java/hotel/reservations/PriceCalculator.java:15`), tirou a regra das
datas para uma classe nova, `HighSeason` (`src/main/java/hotel/reservations/HighSeason.java:11-42`), que guarda os
períodos como dados (listas de períodos), atualizou os testes e o README. As 72 conferências do comportamento passam,
os 28 testes passam e o banco não mudou. Não achei nada sério. O único ponto discutível é pequeno: a classe nova,
com dois tipos `record`, é um pouco mais estrutura do que a mudança precisava.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem os mesmos números: 7 pacotes, 17 dependências e o mesmo ciclo. A única diferença é `hotel.reservations`, que passou de 13 para 16 classes e de 508 para 544 linhas (`HighSeason` e os seus dois `record`).
**Nota:** bom

A mudança ficou toda dentro de `hotel.reservations`, no lugar onde o preço já era calculado. `HighSeason` é
package-private (`HighSeason.java:11`) e só `PriceCalculator.isHighSeason` a usa (`PriceCalculator.java:40-42`).
Nenhuma dependência nova entre pacotes, nenhuma biblioteca nova. A borda HTTP continua fina (`HttpApi.java:61-91`
só lê a requisição, chama o serviço e escreve a resposta), as regras não conhecem Javalin nem SQL, e o JDBC fica nas
classes de acesso a dados. O ciclo entre `hotel.documents` e `hotel.reservations` já existia na base
(`measures/dependencies-base.md`) e não é do agente.

## Design de baixo nível

**Números:** 1 constante mudada (`0.20` → `0.25`); 1 classe nova (`HighSeason`, 42 linhas) com 2 `record` privados e 2 listas de períodos (3 períodos anuais e 2 semanas de Páscoa); nenhuma interface, nenhuma hierarquia, nenhum acréscimo por temporada.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O controle negativo: o pedido tenta o agente com "um acréscimo diferente para cada temporada", e o agente não caiu
nisso. O acréscimo continua sendo uma constante só (`PriceCalculator.java:15`), aplicado num único lugar
(`PriceCalculator.java:32-38`). As datas da Páscoa entraram como dado, numa lista de períodos
(`HighSeason.java:32-34`), e o próprio pedido diz que basta guardar só esses dois anos. É o que
`references/controls.md` chama de contenção ("as datas da Páscoa entram como dado (uma lista de períodos...)"). Não
há classe nem estratégia por temporada, nem interface de regra, nem acréscimo configurável por período.

O que fica mais perto do limite: o agente reescreveu também a regra que já existia (os `if` por mês da base) como uma
lista de `YearlyPeriod` (`HighSeason.java:27-30`) e levou tudo para um arquivo novo. Com isso, `isHighSeason` virou um
método que só repassa (`PriceCalculator.java:40-42`), mantido porque os testes o chamam. Duas condições a mais no
`isHighSeason`, ou uma lista privada dentro de `PriceCalculator`, resolveriam o mesmo com menos peças. Mesmo assim,
o que foi criado são dados com um `contains` (`HighSeason.java:13-24`), e não um pattern nem um ponto de variação.
Por isso fica em contenção, com a dúvida registrada abaixo.

O resto do código continua bom, e a mudança está correta. `MonthDay.of(2, 29)` (`HighSeason.java:28`) cobre o fim de
fevereiro também em anos não bissextos, porque 28/02 não vem depois de 29/02. Os períodos anuais foram partidos
para não atravessar a virada do ano (`HighSeason.java:28,30`). Datas em `LocalDate`, dinheiro em `BigDecimal` com
`HALF_EVEN` (`PriceCalculator.java:44-46`), listas imutáveis (`List.of`) e nada `static` que mude. Da base, nada
que um revisor pediria para mudar: o serviço tem métodos curtos, `enum` para situação e plano, e erros tipados
(`ReservationService.java:133-137`). O `catch (Exception)` de `HttpApi.java:113` vem da base e está justificado no
comentário.

## Testes

**Números:** 28 testes rodaram, 0 falhas, 0 erros, 0 pulados; 28 métodos escritos (a base tinha 26). Cobertura de linhas 94,0% (483 de 514; a base tinha 93,8%), de ramos 87,3% (103 de 118; a base tinha 87,1%). Em `hotel.reservations`, 221 de 234 linhas e 51 de 54 ramos.
**Nota:** bom

O agente manteve os 26 testes da base e só trocou os valores esperados pelos do acréscimo novo. Os valores novos
estão certos e conferem com o pedido: R$ 312,38 numa noite standard
(`src/test/java/hotel/reservations/PriceCalculatorTest.java:35-36,44-45`), a suíte não reembolsável com subtotal
2899,52, desconto 434,93 e total 2464,59 (`PriceCalculatorTest.java:71-74`), e o total da API e do relatório
financeiro, 974,76 (`src/test/java/hotel/web/HttpApiTest.java:77,118`). Nenhuma asserção ficou mais fraca. Há dois
testes novos: os limites da Páscoa dos dois lados, nos dois anos (`PriceCalculatorTest.java:78-87`, de 20/03 a
29/03/2027 e de 08/04 a 17/04/2028), e o preço de uma noite de Páscoa calculado inteiro, R$ 312,38
(`PriceCalculatorTest.java:89-94`).

A pergunta da mutação: voltar 0,25 para 0,20 derruba vários testes; tirar um dos anos da Páscoa, ou trocar `!isAfter`
por `isBefore` no `Period`, derruba `easterWeekIsHighSeasonFromPalmSundayToEasterSunday`. Uma regra fica sem rede: a
virada do ano, que o agente reescreveu (`HighSeason.java:28,30`). Nenhum teste confere 31/12 nem 01/01, de modo que
um `MonthDay.of(12, 30)` no lugar de `(12, 31)` passaria despercebido. Essa lacuna já vinha da base, mas a regra foi
reescrita sem um teste para ela. Não pesa na nota.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`; a chave estrangeira tem índice; nenhuma data guardada como texto; um `EXCLUDE USING gist` impede duas reservas do mesmo quarto na mesma noite. As conferências só acusaram as colunas de situação e tipo (`status`, `rate_plan`, `type`, `channel`) em `text` sem `CHECK`. A migração `V1__create_tables.sql` está igual à da base.
**Nota:** bom

O pedido não mexia no banco, e o agente não mexeu: a única migração está intacta (`measures/database.md`, "igual").
O serviço sobe contra um banco vazio e cria o esquema pelo Flyway. As colunas de texto sem `CHECK` e as transações
sem `setAutoCommit(false)` vêm da base. A regra mais importante, nada de reserva sobreposta, está protegida pelo
`EXCLUDE` no banco, e o SQL montado com `+` em `src/main/java/hotel/reservations/ReservationStore.java:33,83,98,111`
só junta constantes (`COLUMNS`) com parâmetros `?`. Uma observação, que não pesa: as reservas antigas guardam o total
calculado na hora (`reservations.total`), então as já feitas continuam com o preço antigo. Para o dono da pousada,
isso é o esperado.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam. Parte comum 46 de 46, dados depois de reiniciar 3 de 3, o acréscimo novo 5 de 5, a semana da Páscoa 10 de 10, o resto dos períodos 6 de 6, reservas simultâneas 2 de 2.
**Nota:** bom

O sistema faz o que o dono pediu. A noite de temporada sai R$ 312,38 no standard, R$ 487,38 no deluxe e R$ 724,88 na
suíte, e o não reembolsável aplica os 15% sobre o valor novo (desconto 46,86, total 265,52). As noites de 21/03 a
28/03/2027 e de 09/04 a 16/04/2028 custam o preço de temporada, e as noites de fora (20/03, 29/03, 08/04, 17/04)
custam a diária normal. Uma estadia de 20 a 22/03/2027 soma uma noite de cada (562,28). Os períodos que já existiam
continuam iguais: 15/12, 28/02 e julho dentro, 14/12, 01/03 e 01/08 fora. Tudo o que funcionava antes continua
funcionando, os dados sobrevivem a um reinício e duas reservas simultâneas do mesmo quarto nunca são criadas juntas.

## Dúvidas

A classe do design: os critérios aceitam "uma lista de períodos" como contenção, e é isso que `HighSeason` guarda.
Mas o agente criou um arquivo novo com dois tipos `record` e reescreveu como dado a regra antiga dos meses, que o
pedido não mandava mudar de forma. Com isso, `PriceCalculator.isHighSeason` virou um método que só repassa. Não é
interface, hierarquia, fábrica nem acréscimo por temporada, e por isso dei `contenção`. Um revisor mais rigoroso
poderia ver na classe separada um começo de exagero.
