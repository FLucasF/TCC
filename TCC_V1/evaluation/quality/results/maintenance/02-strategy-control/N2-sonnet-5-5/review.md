# Revisão: manutenção, cenário 02

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (as regras, o
preço e o acesso às reservas por JDBC), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`, todos
montados em `hotel.Hotel` (`src/main/java/hotel/Hotel.java:25`). Uma requisição entra em `HttpApi`
(`src/main/java/hotel/web/HttpApi.java:64`), passa por `ReservationService.create`, que chama `PriceCalculator` e grava
em `ReservationStore` no PostgreSQL. As tabelas são criadas por uma migração do Flyway. O pedido era pontual: o
acréscimo de temporada passa de 20% para 25%, e a semana da Páscoa de 2027 e de 2028 vira alta temporada. O agente fez
as duas coisas certo: a constante virou `0.25` (`PriceCalculator.java:17`) e as 72 conferências do roteiro passaram.
Mas não parou na mudança pequena. Criou uma interface pública `Season` com três fábricas (`Season.java:8-34`) e
reescreveu em cima dela a regra de dezembro a fevereiro e de julho, que o pedido não mandava mexer. É justamente o
exagero que este controle negativo procura. Fora isso, a mudança é pequena e limpa, com testes atualizados e novos.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem
os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. A única diferença é `hotel.reservations` com 14 classes
e 546 linhas, contra 13 classes e 508 linhas na base.
**Nota:** bom

O agente não mudou a forma do sistema. A classe nova (`Season`) fica no pacote onde o preço já morava, e nenhuma
dependência nova apareceu (`measures/dependencies.md` contra `measures/dependencies-base.md`). O ciclo já existia na
base: `ReservationService` importa `ReservationReceipt` (`src/main/java/hotel/reservations/ReservationService.java:3`),
e o comprovante importa `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`). A borda HTTP
continua fina (`HttpApi.java:62-72`), as regras não conhecem o Javalin, o JDBC fica em `ReservationStore`, `Rooms` e
`SentMessages`, e a montagem acontece num lugar só (`Hotel.java:25`). Não há nada que o agente tenha piorado.

## Design de baixo nível

**Números:** a mudança tem 2 partes. (1) O acréscimo: uma constante trocada, 0,20 → 0,25 (`PriceCalculator.java:17`),
o que é contenção. (2) As temporadas: uma interface pública nova, `Season` (`Season.java:8`), com 3 fábricas
(`wholeMonth`, `yearly`, `between`, `Season.java:13`, `18`, `32`). Os 4 períodos estão numa lista de `Season`
(`PriceCalculator.java:20-24`), e entre eles estão os 2 que a base já tinha e o pedido não mandava mexer.
**Classe:** exagero
**Exagero:** exagero
**Nota:** bom

**O que o pedido pedia.** A tentação do texto é "um acréscimo diferente para cada temporada". A resposta contida
trocaria a constante e acrescentaria as datas da Páscoa como dado: uma lista de períodos, ou duas condições a mais no
`isHighSeason`. O agente resistiu ao acréscimo por temporada: continua uma constante só (`PriceCalculator.java:17`,
`43`). Mas criou uma "interface de regra de temporada", que `references/controls.md` cita com todas as letras como
exagero para este cenário:

- `public interface Season` com `@FunctionalInterface` e três fábricas estáticas que devolvem lambdas
  (`Season.java:7-34`);
- as quatro condições simples que a base tinha em `isHighSeason` (`base/.../PriceCalculator.java`, `month == JANUARY
  || ... || (DECEMBER && day >= 15)`) foram refeitas como `Season.yearly(Month.DECEMBER, 15, Month.FEBRUARY)` e
  `Season.wholeMonth(Month.JULY)` (`PriceCalculator.java:21-22`). Ou seja, uma parte que o pedido não tocava foi
  refatorada para a estrutura nova.

**O que afasta da contenção.** A fábrica `yearly` (`Season.java:18-29`) é uma generalização especulativa: trata
intervalos que atravessam o ano e intervalos que não atravessam. Só o primeiro caso é usado, e o ramo `current > from
&& current <= to` (`Season.java:26`) nunca roda. O JaCoCo mostra os ramos das linhas 22 e 26 parcialmente cobertos. São
doze linhas para dizer "de 15/12 ao fim de fevereiro", que a base dizia em quatro, de forma mais legível. A interface
também ficou `public` sem ninguém fora do pacote usá-la. Ainda assim, o resultado é pequeno e correto: as datas da
Páscoa entram como dado (`Season.between(LocalDate.of(2027, 3, 21), ...)`, `PriceCalculator.java:23-24`), e a
pergunta "é alta temporada?" continua num lugar só (`PriceCalculator.java:49-51`).

**O resto do código.** A nota mede o design do código, e a classe já registra o exagero. O que o agente escreveu usa os
tipos certos (`BigDecimal` com `HALF_EVEN` em `PriceCalculator.java:53-55`, `LocalDate`, `Month`), não tem estado
mutável (a lista é `List.of`, imutável, `PriceCalculator.java:20`) e não duplica regras. Duas pequenas repetições vêm
da base: o `switch` sobre o plano em `PriceCalculator.java:33` e em `ReservationService.java:82`, e
`cents(BigDecimal.ZERO)` repetido. O agente não as piorou. Fora a estrutura já contada no exagero, não há defeito de
design a pedir.

## Testes

**Números:** final: 28 testes rodaram, 0 falhas, 0 erros, 0 pulados; 28 métodos escritos; cobertura de linhas 485/516
(94,0%) e de ramos 110/132 (83,3%). Base: 26 testes, 471/502 linhas (93,8%), 101/116 ramos (87,1%). Em
`hotel.reservations`, os ramos foram de 49/52 para 58/68.
**Nota:** bom

O agente manteve os 26 testes da base passando e acrescentou 2. Os valores dos testes da base foram atualizados para o
acréscimo novo, com as contas certas e os comentários refeitos: 549,78 → 562,28 em
`src/test/java/hotel/reservations/PriceCalculatorTest.java:36` e `:45`; subtotal 2899,52, desconto 434,93 e total
2464,59 em `:72-74`; 974,76 pela API em `src/test/java/hotel/web/HttpApiTest.java:77` e `:118`. Nenhum foi apagado
nem enfraquecido. Os testes novos conferem a Páscoa nas duas pontas dos dois anos, com a véspera e o dia seguinte
(`PriceCalculatorTest.java:78-87`), e o preço de duas noites de Páscoa, 624,76 (`:90-96`).

A pergunta da mutação, na parte nova:

- 0,25 voltando a 0,20: falham `:36`, `:45` e `:72`;
- tirar o período de 2028, ou trocar `!isAfter(last)` por `isBefore(last)`: `:85` e `:86` falham;
- `>= firstDay` virando `>`: `:36` falha.

O que fica sem rede é só o ramo de `Season.yearly` para um intervalo que não atravessa o ano (`Season.java:26`), que
nenhum código usa. É por ele que a cobertura de ramos caiu de 87% para 83%: a culpa é do código especulativo, não da
falta de teste para uma regra pedida.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), criadas por 1 migração do Flyway
(`V1__create_tables.sql`), **igual** à da base. Nenhuma tabela sem chave primária. Dinheiro todo em `numeric(10,2)`.
Nenhuma chave estrangeira sem índice e nenhuma data em texto. Há um `EXCLUDE USING gist` que impede duas reservas não
canceladas do mesmo quarto na mesma noite. As conferências só acusaram as colunas `status`, `rate_plan`, `type` e
`channel` em `text` sem `CHECK`.
**Nota:** bom

O pedido não mexe no banco, e o agente não tocou nele: nenhuma migração editada nem acrescentada, nenhum SQL mudado.
Os pontos menores vêm da base: as colunas de situação sem `CHECK`, e os relatórios que trazem a tabela inteira com
`ReservationStore.all()` (`src/main/java/hotel/reservations/ReservationStore.java:108-111`, usado em
`RevenueReport.java:25` e em `OccupancyReport.java:25`). O SQL usa parâmetros: os `+` listados pelo script só juntam
constantes (`ReservationStore.java:33`, `83`, `98`, `111`).

## Comportamento

**Números:** 72 de 72 conferências passaram. A parte comum: 46/46. Depois de reiniciar: 3/3. O acréscimo novo: 5/5.
A semana da Páscoa: 10/10. Os outros períodos continuam: 6/6. Duas reservas ao mesmo tempo: 2/2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API. Um standard numa noite de julho sai R$ 312,38, o deluxe R$ 487,38 e a
suíte R$ 724,88. O não reembolsável tira 15% sobre o valor novo (desconto 46,86, total 265,52). As noites de 21 a 28/03/2027
e de 09 a 16/04/2028 custam o preço de temporada, e as vésperas e os dias seguintes, o preço normal. De 20 a 22/03/2027
dá R$ 562,28. Os períodos antigos continuam com as mesmas fronteiras (14/12 normal, 15/12 alta, 28/02 alta, 01/03
normal, 31/07 alta, 01/08 normal). Nada do que funcionava quebrou: as recusas, os e-mails, os relatórios, os dados
depois de reiniciar e a proteção contra duas reservas simultâneas.

## Dúvidas

A linha entre "uma lista simples de valores" (contenção) e "uma interface de regra de temporada" (exagero) é fina
aqui. A lista em `PriceCalculator.java:20-24` lembra um dado, e se fosse uma lista de pares de datas seria contenção.
Mas os elementos dela são instâncias de uma interface pública nova com fábricas que geram comportamento
(`Season.java:8-34`), incluindo uma generalização que ninguém usa (`Season.java:26`), e as regras antigas de dezembro a
fevereiro e de julho foram refatoradas para ela. É isso que sustenta `exagero`.
