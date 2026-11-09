# Revisão: manutenção, cenário 02

## Visão geral

A base é um serviço Javalin em sete pacotes. `hotel.web.HttpApi` recebe a requisição e chama `ReservationService` (`hotel.reservations`). O serviço calcula o preço com `PriceCalculator` e grava por `ReservationStore`, que usa JDBC com parâmetros. Os dados ficam no PostgreSQL, num esquema criado por uma migração Flyway (`V1__create_tables.sql`). O pedido era pontual: a alta temporada passa de 20% para 25%, e as semanas da Páscoa de 2027 e 2028 viram alta temporada. O agente fez a mudança pequena, no lugar onde a regra já morava. Trocou a constante (`src/main/java/hotel/reservations/PriceCalculator.java:16`) e pôs duas condições a mais em `isHighSeason` (`PriceCalculator.java:53-54`). Nos testes, corrigiu os valores esperados que a mudança alterou e acrescentou três testes novos. Não criou nenhuma estrutura para o "acréscimo diferente para cada temporada", que o dono disse ser só uma ideia. As 72 conferências do comportamento passaram e os 29 testes passam. O único ponto solto é o `README.md`, que não foi atualizado: ele ainda diz "a noite custa 20% a mais" e não cita a Páscoa (`README.md:31-32`).

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem os mesmos números (`measures/dependencies-base.md`); a única diferença é `PriceCalculator.java`, que passou de 53 para 62 linhas.
**Nota:** bom

O agente não mexeu na estrutura: a mudança ficou toda dentro de `PriceCalculator`, que não conhece Javalin nem JDBC (`PriceCalculator.java:3-7`). O ciclo já existia na base. Ele vem de `ReservationService`, que importa `ReservationReceipt` para montar o e-mail (`src/main/java/hotel/reservations/ReservationService.java:3`, `:64`), enquanto o comprovante importa `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`). Não é do agente. No resto, a borda HTTP é fina (`src/main/java/hotel/web/HttpApi.java:61-90`), as regras ficam sem SQL e as peças são montadas no pacote `hotel`.

## Design de baixo nível

**Números:** 1 constante alterada (`PriceCalculator.java:16`), 2 condições novas com as datas da Páscoa dentro de `isHighSeason` (`PriceCalculator.java:53-54`), 0 classes, interfaces ou parâmetros novos.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É a resposta que o controle negativo espera. O acréscimo continua sendo uma constante única, `HIGH_SEASON_SURCHARGE = 0.25` (`PriceCalculator.java:16`). As duas semanas da Páscoa entram como dado, em duas condições de `isHighSeason` (`PriceCalculator.java:53-54`), com os dois domingos incluídos (`>= 21 && <= 28`, `>= 9 && <= 16`). O agente separou os três períodos em variáveis com nome (`isDecemberToFebruary`, `isJuly`, `isEasterWeek`, `PriceCalculator.java:47-56`), o que deixa claro onde entra o próximo ano. Também atualizou o Javadoc do método (`PriceCalculator.java:41`). Não há estratégia por temporada, interface de regra nem acréscimo parametrizado, ou seja, nada de exagero.

O resto do código vem da base e está bem resolvido:
- dinheiro em `BigDecimal` com HALF_EVEN (`PriceCalculator.java:59-61`);
- `switch` sobre enums (`PriceCalculator.java:26-29`, `ReservationService.java:82-85`);
- erros tipados com `ErrorCode` e um único tratador HTTP (`HttpApi.java:93-107`).

O único `catch (Exception)` (`HttpApi.java:113`) é da base e tem um comentário que o justifica. As datas da Páscoa ficaram como números soltos dentro da condição; uma lista de períodos seria um pouco mais fácil de estender, mas o texto aceita as duas formas.

## Testes

**Números:** 29 testes rodaram, 0 falharam, 0 pulados; 29 métodos escritos (a base tinha 26). Linhas: 93,9% (475/506; na base, 93,8%). Ramos: 89,1% (123/138; na base, 87,1%). No pacote `hotel.reservations`, os ramos subiram de 49/52 para 71/74.
**Nota:** bom

Nenhum teste da base foi apagado ou enfraquecido. O agente só trocou os valores esperados que mudaram de verdade com os 25%, todos com o valor exato:
- `src/test/java/hotel/reservations/PriceCalculatorTest.java:36` e `:45`: 562.28;
- `PriceCalculatorTest.java:100-102`: 2899.52, 434.93 e 2464.59;
- `src/test/java/hotel/web/HttpApiTest.java:77` e `:118`: 974.76.

Os testes novos conferem as duas bordas de cada semana da Páscoa: a véspera fora, os dois domingos dentro e o dia seguinte fora, em 2027 (`PriceCalculatorTest.java:57-61`) e em 2028 (`:65-69`). Um terceiro teste confere o preço de uma estadia que atravessa a semana de 2027 (`:73-81`, total 2998.84).

Pela pergunta da mutação:
- trocar 0.25 por outro valor derruba vários testes (`:36`, `:45`, `:100`);
- trocar um `>=` por `>`, ou um `<=` por `<`, nas datas da Páscoa derruba `:59-60` ou `:67-68`;
- apagar uma das duas condições derruba o teste do ano correspondente.

Fica sem rede só o preço em reais da Páscoa de 2028, e ele vem do mesmo cálculo de 2027.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma data em texto. Uma migração (`V1__create_tables.sql`), igual à da base. O script acusou só as colunas de situação e de tipo sem CHECK (`reservations.status`, `reservations.rate_plan`, `rooms.type`, `messages.channel`).
**Nota:** bom

O pedido não tocava o banco, e o agente não mexeu nele: a migração da base ficou intacta, então um banco que já roda o sistema sobe sem problema. O esquema da base protege a regra principal com `EXCLUDE USING gist` sobre quarto e período, menos as canceladas (`src/main/resources/db/migration/V1__create_tables.sql:39-40`). As colunas de texto sem CHECK (`V1__create_tables.sql:28`, `:33`) vêm da base. O SQL montado com `+` em `ReservationStore.java` (`:33`, `:83`, `:98`, `:111`) junta só constantes, e os valores entram por `?`.

## Comportamento

**Números:** 72 de 72 conferências passaram:
- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- o acréscimo novo: 5/5;
- a semana da Páscoa: 10/10;
- os outros períodos, que continuam iguais: 6/6;
- duas reservas ao mesmo tempo: 2/2.

**Nota:** bom

Para o dono da pousada, tudo saiu como ele pediu:
- o standard numa noite de temporada sai R$ 312,38, o deluxe R$ 487,38 e a suíte R$ 724,88;
- o não reembolsável em alta temporada tem o desconto calculado sobre o valor novo (R$ 46,86);
- as noites de 21 a 28/03/2027 e de 09 a 16/04/2028 cobram a alta temporada, e as noites vizinhas não;
- 15/12, o fim de fevereiro e julho continuam como antes;
- o resto do sistema (recusas, confirmação e e-mail, check-in e check-out, cancelamentos, relatórios, dados depois de reiniciar e reservas simultâneas) continua funcionando.

## Dúvidas

O `README.md` não foi atualizado: ele ainda descreve a alta temporada com 20% e sem a Páscoa (`README.md:31-32`). O dono disse que o README explica como o sistema é, então a documentação agora contradiz o código. Nenhuma das cinco colunas mede a documentação, por isso a falha não entrou em nenhuma nota, mas um revisor exigente pediria a correção dessas duas linhas.
