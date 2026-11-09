# Revisão: manutenção, cenário 02

## Visão geral

O sistema da base está dividido em sete pacotes por assunto (`hotel`, `hotel.web`, `hotel.reservations`,
`hotel.rooms`, `hotel.messages`, `hotel.documents`, `hotel.reports`). Uma requisição entra em `hotel.web.HttpApi`
(Javalin), que lê o corpo e chama `ReservationService`. O serviço valida, pede o preço ao `PriceCalculator` e grava
pelo `ReservationStore` (JDBC com pool), num PostgreSQL com o esquema criado por uma migração Flyway (`V1`). O pedido
era pontual: o acréscimo de alta temporada passa de 20% para 25%, e a semana da Páscoa de 2027 e de 2028 entra na alta
temporada. O agente mexeu em pouca coisa: trocou a constante (`PriceCalculator.java:17`), reescreveu `isHighSeason`
como uma lista de períodos (`PriceCalculator.java:46-54`) e criou uma interface nova, `Season`, com duas fábricas
estáticas (`Season.java:7-26`). Também atualizou os valores dos testes, acrescentou um teste para a Páscoa e corrigiu o
README. O comportamento está todo certo (72 de 72 conferências) e o `mvn verify` passa (27 testes). A ressalva mais
séria fica no design: para um controle negativo, a interface `Season` com fábricas é estrutura que o pedido não
pediu, e um exagero pequeno.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base são
os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. A única mudança é em `hotel.reservations`, que passou
de 13 classes e 508 linhas para 14 classes e 540 linhas (`measures/dependencies.md` comparado com
`measures/dependencies-base.md`).
**Nota:** bom

A mudança fica dentro de `hotel.reservations`, onde o preço já morava. `Season` é package-private (`Season.java:8`),
e só o `PriceCalculator` usa (`PriceCalculator.java:46-53`). Não há dependência nova entre pacotes e nenhuma
biblioteca nova. A camada de regras continua sem Javalin: segundo a tabela de bibliotecas, só `hotel` e `hotel.web`
usam o Javalin. A borda HTTP continua fina (`HttpApi.java:61-90`). O ciclo vem da base e o agente não o piorou: de
um lado, `ReservationService.java:3` importa `hotel.documents.ReservationReceipt`; do outro,
`ReservationReceipt.java:3` importa `hotel.reservations.Reservation`.

## Design de baixo nível

**Números:** a mudança tem 1 constante trocada, 1 interface nova (`Season`, 26 linhas) com 2 fábricas estáticas
(`everyYear` e `between`) e 4 períodos numa lista. Os dois períodos que já existiam também foram passados para a
estrutura nova. O acréscimo continua um só para todas as temporadas.
**Classe:** exagero
**Exagero:** exagero
**Nota:** bom

O que aproxima da contenção:
- A constante virou 0,25 no lugar onde já estava (`PriceCalculator.java:17`).
- O acréscimo não foi parametrizado por temporada: a frase que tentava o agente ("um acréscimo diferente para cada
  temporada") não virou código, porque `nightPrice` continua usando a constante única (`PriceCalculator.java:36-37`).
- As datas da Páscoa entraram como dado, dois períodos numa lista (`PriceCalculator.java:49-50`).

O que afasta da contenção:
- Os períodos não são valores simples. São instâncias de uma interface nova, `interface Season { boolean
  includes(LocalDate night); }` (`Season.java:7-10`), com duas fábricas estáticas que devolvem lambdas diferentes
  (`Season.java:13-20` e `Season.java:23-25`).
- Isso é, ao pé da letra, a "interface de regra de temporada" que `controls.md` lista como exagero. É também um
  pequeno sistema de fábricas para dois tipos de período, quando bastavam duas condições a mais no `isHighSeason` ou
  uma lista de pares de datas.
- O agente também reescreveu a parte da base que o pedido não mandava mudar: a regra de dezembro a fevereiro e de
  julho, antes quatro comparações de mês (`base/.../PriceCalculator.java`), virou `Season.everyYear` com a virada de
  ano tratada na fábrica (`Season.java:16-18`).
- A estrutura é pequena e bem escrita, mas é estrutura criada para uma variação que ninguém pediu.

O resto do código, que é quase todo da base, está limpo:
- `BigDecimal` com `HALF_EVEN` em cada passo (`PriceCalculator.java:56-58`) e `LocalDate`/`MonthDay` para as datas.
- `enum` para os planos e as situações, com `switch` exaustivo (`PriceCalculator.java:27-30`).
- Exceções de domínio com código (`ReservationService.java:133-137`).

Dois pontos menores no que o agente escreveu:
- O fim de fevereiro é `MonthDay.of(2, 29)` (`PriceCalculator.java:47`). Funciona em ano não bissexto porque 28/02
  não é depois de 29/02, mas é um truque sem comentário.
- A constante `HIGH_SEASONS` ficou declarada no meio dos métodos, com o javadoc que antes era do método
  (`PriceCalculator.java:42-50`).

## Testes

**Números:**
- Final: 27 testes rodaram, 0 falhas, 0 erros, 0 pulados; 27 métodos escritos. Cobertura de linhas 93,9% (480 de
  511) e de ramos 87,5% (105 de 120).
- Base: 26 testes, 93,8% das linhas (471 de 502) e 87,1% dos ramos (101 de 116).
- `hotel.reservations`: 218 de 231 linhas e 53 de 56 ramos.

**Nota:** bom

O que o agente fez nos testes:
- Manteve os 26 testes da base passando e não apagou nem desligou nenhum.
- Atualizou os valores esperados para os 25%, e as contas estão certas: 562,28 em `PriceCalculatorTest.java:36` e
  `:45`; 2899,52, 434,93 e 2464,59 em `:84-86`; 974,76 em `HttpApiTest.java:77` e `:118`.
- Acrescentou `easterWeekIsHighSeasonInBothYears` (`PriceCalculatorTest.java:57-66`), que confere as duas bordas
  dos dois anos: 20/03 fora, 21/03 e 28/03 dentro, 29/03 fora; e o mesmo para 08 a 17/04/2028.

A pergunta da mutação:
- Voltar 0,25 para 0,20 derruba cinco testes.
- Tirar um dos períodos da Páscoa, ou trocar o primeiro ou o último dia, derruba o teste novo.
- Trocar `!isAfter` por `isBefore` no fim de um período derruba as linhas 60 e 64.

O que ficou sem rede é pouco:
- Nenhum teste de unidade confere o 28/02 de um ano não bissexto, que depende do truque do `MonthDay.of(2, 29)`. O
  roteiro do comportamento confere esse dia e passa.
- O teste da Páscoa chama só `isHighSeason`, não o preço. Mas o preço passa pelo mesmo `isHighSeason`
  (`PriceCalculator.java:36`).

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` nas 6 colunas e não há chave estrangeira sem índice. A migração da base, `V1__create_tables.sql`,
ficou igual, e não há migração nova. O que as conferências acusaram: as colunas `status`, `rate_plan`, `type` e
`channel` são `text` sem CHECK. O SQL montado com `+` só junta constantes (`ReservationStore.java:33`, `:83`, `:98`,
`:111`).
**Nota:** bom

O pedido não mexe no banco, e o agente não mexeu. A reserva guarda o preço calculado no momento em que é criada,
então as reservas já gravadas continuam com o valor antigo, o que é o certo para quem já fechou. Quem impede duas
reservas na mesma noite é o próprio banco, com um `EXCLUDE USING gist` que ignora as canceladas
(`V1__create_tables.sql:38-40`). Os defeitos menores vêm da base, e o agente não os piorou:
- as colunas de situação sem CHECK (`V1__create_tables.sql:28`, `:33`);
- os relatórios lendo a tabela inteira para filtrar em Java (`OccupancyReport.java:25`, `RevenueReport.java:25`,
  por `ReservationStore.java:111`).

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o acréscimo novo: 5 de 5;
- a semana da Páscoa: 10 de 10;
- os outros períodos: 6 de 6;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, o sistema faz o que ele pediu:
- O standard numa noite de temporada sai R$ 312,38, o deluxe R$ 487,38 e a suíte R$ 724,88.
- O não reembolsável tira os 15% sobre o valor novo (desconto de 46,86, total de 265,52).
- As noites de 21 a 28/03/2027 e de 09 a 16/04/2028 custam o preço de temporada, contando os dois domingos. Os dias
  vizinhos (20/03, 29/03, 08/04 e 17/04) continuam com o preço normal.
- Os outros períodos continuam como estavam: 15/12, 28/02 e 31/07 são temporada; 14/12, 01/03 e 01/08 não são.
- Tudo o que já funcionava continua funcionando: os preços, as recusas, a confirmação com o e-mail, os
  cancelamentos, os relatórios, os dados depois de reiniciar e as reservas simultâneas.

## Dúvidas

A classe do design ficou entre `contenção` e `exagero`. Pesa para `contenção` que a mudança é pequena (26 linhas),
fica no pacote do preço, é package-private, e que o acréscimo continua um só, sem parametrização por temporada.
Pesa para `exagero` que o agente criou uma interface de regra de temporada com duas fábricas (`Season.java:7-26`) e
passou para ela também os períodos que já existiam, quando duas condições a mais em `isHighSeason`, ou uma lista de
pares de datas, resolviam o pedido. `controls.md` cita esse caso como exagero, e por isso dei `exagero`. Mas é um
exagero leve.
