# Revisão: manutenção, cenário 02

## Visão geral

A base é um serviço Javalin em 7 pacotes: `hotel.web` (as rotas em `HttpApi`), `hotel.reservations` (as regras em
`ReservationService` e `PriceCalculator`, o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages`,
`hotel.documents` e `hotel.reports`. A montagem fica em `hotel.Hotel` (`src/main/java/hotel/Hotel.java:21-27`). Uma
requisição passa por `HttpApi` → `ReservationService` → `PriceCalculator`/`ReservationStore` → PostgreSQL (esquema
pela migração Flyway `V1__create_tables.sql`). O pedido era pontual: o acréscimo da alta temporada passa de 20% para
25% e a semana da Páscoa de 2027 e de 2028 entra como alta temporada. O agente mexeu em três arquivos: um de produção
(`PriceCalculator.java`) e dois de teste. A constante virou 0,25 (`src/main/java/hotel/reservations/PriceCalculator.java:17`).
As duas semanas da Páscoa entraram como uma lista de períodos (`PriceCalculator.java:21-29`), consultada por mais uma
condição em `isHighSeason` (`PriceCalculator.java:60`). Os testes da base com os valores antigos foram atualizados, e
entrou um teste para as bordas da Páscoa. Os 72 de 72 testes do roteiro de comportamento passaram. Não há defeito
sério. O único esquecimento é a documentação: o `README.md:31-32` ainda diz "20% a mais" e não cita a Páscoa, e o
pedido conta que o dono usa esse README para saber como o sistema é.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), igual à base;
a única diferença com `measures/dependencies-base.md` é `hotel.reservations` de 13 para 14 classes (508 → 521 linhas),
o record privado novo.
**Nota:** bom

O agente não mudou a arquitetura. A mudança ficou toda dentro de `PriceCalculator`, a classe de regra que já calculava
o preço, e essa classe não conhece Javalin nem JDBC (`PriceCalculator.java:3-8`). O ciclo já existia na base:
`ReservationService.java:3` importa `ReservationReceipt`, e `ReservationReceipt.java:3` importa `Reservation`. O
agente não piorou esse ciclo. A borda HTTP continua fina (`src/main/java/hotel/web/HttpApi.java:61-91` só lê, chama o
serviço e responde). O SQL continua isolado nas classes de acesso a dados, e a montagem continua num lugar só
(`Hotel.java:21-27`).

## Design de baixo nível

**Números:** 1 arquivo de produção mudado (+13 linhas): uma constante trocada, uma lista com 2 períodos, um record
privado de 3 linhas e mais uma condição em `isHighSeason`; nenhuma interface, classe ou parâmetro novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É controle negativo. A frase que tenta o agente é "a gente já pensou em ter um acréscimo diferente para cada
temporada, mas por enquanto é o mesmo para todas". O agente não caiu: continua havendo um só
`HIGH_SEASON_SURCHARGE` (`PriceCalculator.java:17`). Não há classe por temporada, interface de regra de temporada nem
acréscimo parametrizado. As datas da Páscoa entraram como dado, que é o que `references/controls.md` descreve como
contenção ("uma lista de períodos"): `EASTER_WEEKS` (`PriceCalculator.java:21-23`) e um `record DateRange` privado com
um único `contains` (`PriceCalculator.java:25-29`). Esse record é um contêiner de valores com o intervalo fechado
explícito (`!isBefore && !isAfter`, as duas noites de domingo incluídas, como o pedido diz). Não é uma abstração para
substituir. A condição nova fica junto das que já existiam (`PriceCalculator.java:56-60`), e o Javadoc foi atualizado
(`PriceCalculator.java:53`).

O resto do código veio da base e continua limpo: dinheiro em `BigDecimal` com `HALF_EVEN` em todo passo
(`PriceCalculator.java:63-65`), `LocalDate` para as noites, `enum` para os planos e as situações, e `switch` exaustivo
nos planos (`PriceCalculator.java:38-41`). O detalhe de estilo é usar `stream().anyMatch` sobre uma lista de dois
itens, o que é aceitável.

## Testes

**Números:** final: 27 testes rodaram, 0 falhas, 0 erros, 0 pulados; 27 métodos escritos. Base: 26/26. Cobertura de
linhas 93,9% (477/508), contra 93,8% na base; de ramos 87,7% (107/122), contra 87,1%. Em `hotel.reservations`, 55 de
58 ramos.
**Nota:** bom

O agente não apagou nenhum teste da base nem enfraqueceu nenhum. Atualizou os valores esperados para 25%, com as
contas certas: `src/test/java/hotel/reservations/PriceCalculatorTest.java:36` (562,28 = 249,90 + 312,38),
`PriceCalculatorTest.java:45`, `PriceCalculatorTest.java:84-86` (suíte não reembolsável: 2899,52 / 434,93 / 2464,59) e
`src/test/java/hotel/web/HttpApiTest.java:77,118` (974,76). Escreveu `easterWeekIsHighSeason`
(`PriceCalculatorTest.java:57-65`), que confere as quatro bordas de cada ano (o dia antes, os dois domingos e o dia
depois).

A pergunta da mutação:
- se a constante voltar a 0,20, falham `PriceCalculatorTest.java:36,45,84` e `HttpApiTest.java:77`;
- se `!isAfter(last)` virar `isBefore(last)`, ou se a data de um dos anos estiver errada por um dia, falha
  `PriceCalculatorTest.java:60,64` (ou `58-65`);
- se a condição da linha 60 for apagada, falha o teste novo.

O teste novo confere só `isHighSeason`, sem um preço de noite da Páscoa. Como o preço da noite de alta já é conferido
com valor em outros testes, essa lacuna é pequena.

## Banco

**Números:** 3 tabelas, 1 migração Flyway (`V1__create_tables.sql`, igual à da base). Nenhuma tabela sem chave
primária. Dinheiro em `numeric(10,2)` nas 6 colunas. Nenhuma chave estrangeira sem índice e nenhuma data em texto. A
única ressalva do script: as colunas `status`, `rate_plan`, `type` e `channel` não têm CHECK.
**Nota:** bom

O pedido não mexe no banco, e o agente não mexeu: a migração da base ficou intacta (o script a acusa como "igual").
Tudo o que as conferências mostram veio da base. Isso inclui a restrição `EXCLUDE USING gist` contra a reserva dupla
(`src/main/resources/db/migration/V1__create_tables.sql:38-40`) e a falta de CHECK nas colunas de situação e de tipo
(`V1__create_tables.sql:4,28,33,46`). O agente não piorou nada. O preço continua gravado na reserva quando ela é
criada (`V1__create_tables.sql:30-32`), então as reservas antigas mantêm o valor que foi cobrado, que é o
comportamento sensato para uma mudança de tabela de preços. Os SQL montados com `+` em `ReservationStore.java:33,83,98,111`
só concatenam constantes (`COLUMNS`) e são da base.

## Comportamento

**Números:** 72 de 72 conferências passaram:
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- acréscimo novo: 5/5;
- semana da Páscoa: 10/10;
- os outros períodos: 6/6;
- duas reservas ao mesmo tempo: 2/2.
**Nota:** bom

Nada falhou. Para o dono da pousada:
- o standard numa noite de temporada sai R$ 312,38, como ele pediu (o deluxe sai R$ 487,38 e a suíte R$ 724,88);
- o não reembolsável aplica os 15% sobre o valor novo (R$ 265,52);
- as noites de 21/03 a 28/03/2027 e de 09/04 a 16/04/2028 cobram o acréscimo, e o dia antes e o dia depois não;
- 15/12 até o fim de fevereiro e o mês de julho continuam como antes;
- todo o resto do sistema segue igual: recusas, confirmação e e-mail, cancelamentos, relatórios, dados depois de
  reiniciar e reserva simultânea.

## Dúvidas

O `README.md:31-32` ficou desatualizado: ainda diz 20% e não cita a Páscoa. Nenhuma das cinco colunas mede a
documentação, então isso não entrou em nenhuma nota. Mesmo assim, um revisor pediria a correção, porque o pedido conta
que o dono usa o README para saber como o sistema funciona.
