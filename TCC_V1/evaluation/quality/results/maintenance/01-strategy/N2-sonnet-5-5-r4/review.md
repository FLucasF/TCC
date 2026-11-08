# Revisão: manutenção, cenário 01

## Visão geral

O projeto (herdado da base) tem sete pacotes: `hotel.web` (Javalin, a borda HTTP), `hotel.reservations` (as regras e o
acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.documents`, `hotel.messages`, `hotel.reports` e `hotel`
(a montagem e o Flyway). Uma criação de reserva entra em `HttpApi.readNewReservation`
(`src/main/java/hotel/web/HttpApi.java:109`), vira um `NewReservation`, passa por `ReservationService.create`
(`src/main/java/hotel/reservations/ReservationService.java:27`), que pede o preço ao `PriceCalculator` e grava pelo
`ReservationStore` no PostgreSQL (esquema por Flyway, `V1__create_tables.sql`). A mudança do agente é pequena e
localizada: tirou os dois `switch` de plano da base (o desconto em `PriceCalculator.calculate` e a devolução em
`ReservationService.cancel`), criou a interface `RatePlanRule` com uma classe por plano (os cinco, inclusive os dois que
já existiam), ligou cada uma a uma constante do `enum RatePlan` e acrescentou o `companyCode` ao pedido. Não mexeu no
banco. Os 36 testes passam e as 72 conferências do roteiro passam. Não achei nada sério; o único ponto em aberto é que
o código da empresa não fica guardado na reserva (ver "Dúvidas").

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Igual à base
(7 pacotes, 17 dependências, o mesmo ciclo, em `measures/dependencies-base.md`); `hotel.reservations` passou de 13
para 17 classes e de 508 para 689 linhas.
**Nota:** bom

A mudança cabe na arquitetura que já existia e não acrescenta dependência nenhuma entre pacotes. As regras novas
ficam todas em `hotel.reservations`, em classes que não conhecem o Javalin nem o JDBC
(`src/main/java/hotel/reservations/CorporateRule.java:9`, `WeekendPackageRule.java:11`, `LongStayRule.java:8`). A
borda HTTP continua fina: a única mudança em `HttpApi` é repassar o `companyCode` (`src/main/java/hotel/web/HttpApi.java:128-130`)
e no DTO, um campo novo (`src/main/java/hotel/web/CreateReservationRequest.java:16`). O erro de regra de plano sai
como `HotelException(INVALID_REQUEST)` e vira 400 pelo tratador que já existia (`HttpApi.java:94-102`), sem
tratamento especial na borda. O ciclo `documents` ↔ `reservations` (o serviço monta o comprovante,
`ReservationService.java:65`, e o comprovante lê a reserva) vem da base, e o agente não o piorou. Nenhuma classe
cresceu demais: a maior continua sendo `HttpApi` (171 linhas).

## Design de baixo nível

**Números:** 5 planos, os 5 na estrutura (`StandardRule`, `NonRefundableRule`, `CorporateRule`,
`WeekendPackageRule`, `LongStayRule`) e nenhum fora; nenhum `switch` ou `if` sobre o plano em `src/main` (o `grep`
por `RatePlan`/`ratePlan` só acha a seleção `ratePlan.rule()` e o transporte do valor); 1 interface com 5
implementações.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern: é o Strategy que a referência descreve como acerto. A interface `RatePlanRule`
(`src/main/java/hotel/reservations/RatePlanRule.java:8`) junta o que é de cada plano: o preço com a validação
(`price`, linha 16, que lança `INVALID_REQUEST` quando a estadia não serve ao plano) e a devolução (`refund`, linha
19). Cada constante do `enum` recebe a sua regra no construtor (`src/main/java/hotel/reservations/RatePlan.java:6-14`),
que é um ponto único de seleção. Os dois `switch` de comportamento da base sumiram: o cálculo só monta o preço de
tabela e pede ao plano (`src/main/java/hotel/reservations/PriceCalculator.java:27-28`), e o cancelamento só pergunta a
devolução (`src/main/java/hotel/reservations/ReservationService.java:83`). Os dois planos que já existiam também foram
para a estrutura (`StandardRule.java:8`, `NonRefundableRule.java:8`), e as validações de cada plano (sexta a domingo
em `WeekendPackageRule.java:21`, 7 noites em `LongStayRule.java:15`, a lista de empresas em `CorporateRule.java:11-22`)
moram na regra do plano, não no serviço. Um plano novo (melhor idade, antecedência de 60 dias) é uma classe e uma
constante, sem mexer no cálculo nem no cancelamento.

O que afasta um pouco: a assinatura de `price` recebe `companyCode` em todos os planos, embora só o convênio o use
(`RatePlanRule.java:16`); e o plano de antecedência que o comercial já anunciou vai pedir a data da reserva, que a
assinatura não traz, então ela vai mudar nas cinco classes. É um custo pequeno e comum, e não tira a classe.

O exagero: não há. A interface tem cinco implementações de verdade; o método estático `withDiscount`
(`RatePlanRule.java:22-26`) tira a repetição do desconto dos três planos que o usam, em vez de acrescentar camada. O
construtor extra de `NewReservation` (`src/main/java/hotel/reservations/NewReservation.java:15-18`) só mantém as
chamadas antigas compilando sem o código da empresa.

O resto: dinheiro em `BigDecimal` com o mesmo `cents` (meio para o par) da base (`PriceCalculator.java:48-50`),
usado também na devolução de metade (`WeekendPackageRule.java:31`); preços fechados num `Map<RoomType, BigDecimal>`
(`WeekendPackageRule.java:13-16`); constantes com nome (`MINIMUM_NIGHTS`, `DISCOUNT`, `REFUND_SHARE`); o `null` do
código da empresa é tratado de forma explícita (`CorporateRule.java:18`). Defeitos só de forma: a linha do Javadoc em
`PriceCalculator.java:11` ficou bem mais comprida que as outras, e `new Price(2, ...)` em `WeekendPackageRule.java:26`
repete o número de noites em vez de usar `standardPrice.nights()` (está certo porque a validação acima garante as
duas noites). Nada que um revisor exigente seguraria.

## Testes

**Números:** 36 testes rodaram, 0 falhas, 0 erros, 0 pulados; 36 métodos escritos (a base tinha 26, todos mantidos e
passando). Cobertura de linhas de 94,2% (508 de 539) e de ramos de 87,7% (107 de 122); na base, 93,8% e 87,1%. No
pacote `hotel.reservations`, 245 de 258 linhas e 55 de 58 ramos.
**Nota:** bom

O agente testou o que mudou, com valores, nos três níveis que o projeto já usava. No cálculo: os três exemplos do
financeiro com diárias, desconto e total (`src/test/java/hotel/reservations/PriceCalculatorTest.java:89`, R$ 779,80 /
93,58 / 686,22; `:140`, R$ 1.749,30 / 174,93 / 1.574,37), as outras duas empresas (639,44 e 584,85, linha 89), o
código ausente, desconhecido ou em minúsculas recusado (`:101`), o código ignorado nos outros planos (`:109`), o
pacote nos três quartos e na alta temporada (`:115`), a recusa de quinta a sábado, de três noites e de uma noite
(`:130`), as 6 noites recusadas (`:140`) e a temporada mantida na estadia longa (`:151`). No serviço, contra o banco:
as devoluções de 410,00, 686,22 e 1.574,37 (`src/test/java/hotel/reservations/ReservationServiceTest.java:164`) e as
três recusas de regra de plano (`:182`). Pela API: 400 com `INVALID_REQUEST` sem código, 201 com total 686,22 com
código e o código ignorado no plano normal (`src/test/java/hotel/web/HttpApiTest.java:124`).

A pergunta da mutação: trocar 0.12 por 0.21 quebraria `PriceCalculatorTest.java:89`; trocar `<` por `<=` em
`LongStayRule.java:15` quebraria o teste de 7 noites (`:140`); trocar a metade da devolução quebraria
`ReservationServiceTest.java:164`; tirar a checagem da sexta quebraria `:130`. Ficam sem teste só detalhes: a
devolução de um pacote cancelado ainda pendente (que dá 0,00 pela conta) e uma recusa de pacote que comece num sábado
com duas noites (o roteiro do comportamento confere esta). Os testes da base só ganharam o argumento `null` novo
(`PriceCalculatorTest.java:21-75`), sem perder nenhuma asserção.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`
nas 6 colunas; nenhuma chave estrangeira sem índice; nenhuma data em texto; a única migração,
`V1__create_tables.sql`, igual à da base; as colunas `rate_plan`, `status`, `type` e `channel` sem `CHECK` (já assim na
base); a restrição `EXCLUDE` contra duas reservas na mesma noite mantida.
**Nota:** bom

O agente não precisou mexer no esquema e não mexeu: os planos novos cabem na coluna `rate_plan text`
(`src/main/resources/db/migration/V1__create_tables.sql:28`), que não tem `CHECK` limitando os valores, e o
`ReservationStore` grava e lê o plano pelo nome do `enum`
(`src/main/java/hotel/reservations/ReservationStore.java:43` e `:131`), então as constantes novas funcionam sem
migração. A migração da base não foi editada, o que é o que importa para quem já roda o sistema. O SQL continua com
parâmetros (os `+` que o script lista só juntam constantes, `ReservationStore.java:33`, `:83`, `:98`, `:111`), as
conexões vêm do pool e são devolvidas por try-with-resources. A falta de `CHECK` em `rate_plan` e `status` e o
`RevenueReport` que traz a tabela inteira para filtrar em Java (`src/main/java/hotel/reports/RevenueReport.java:25`)
vêm da base. O código da empresa não é guardado (não há coluna para ele); isso não perde dado de dinheiro, porque o
desconto e o total ficam gravados, e vai para as "Dúvidas".

## Comportamento

**Números:** 72 de 72 conferências passaram: a parte comum 46 de 46, os dados depois de reiniciar 3 de 3, o cenário
01 21 de 21 e as reservas simultâneas 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API: os três exemplos do financeiro saem exatos (686,22; 820,00 com devolução de
410,00; 1.574,37), as outras empresas dão 18% e 25%, o convênio sem código ou com código desconhecido é recusado com
400 `INVALID_REQUEST`, o pacote tem o preço fechado nos três quartos e em julho e é recusado de sábado a segunda e em
três noites, a estadia longa mantém a temporada e recusa 6 noites, convênio e estadia longa devolvem tudo, e o
`companyCode` é ignorado nos outros planos. O que já funcionava continua igual (o normal, o não reembolsável com o
arredondamento meio para o par, os erros, o e-mail, os relatórios, os dados depois de reiniciar e as duas reservas ao
mesmo tempo). Uma mudança pequena de ordem, que o texto não fechou: o preço (com a validação do plano) agora é
calculado antes de ver se o quarto está ocupado (`src/main/java/hotel/reservations/ReservationService.java:42-47`),
então um pedido que quebra a regra do plano para um quarto ocupado recebe 400 em vez de 409. É uma escolha razoável e
não pesa.

## Dúvidas

- O código da empresa do convênio não fica guardado: `NewReservation` o recebe
  (`src/main/java/hotel/reservations/NewReservation.java:13`), mas `ReservationStore.add` não o grava e `read`
  reconstrói a reserva sem ele (`src/main/java/hotel/reservations/ReservationStore.java:123-131`). Depois de criada, uma
  reserva `CORPORATE` não diz de qual empresa é, nem na resposta, nem no comprovante, nem no relatório. O pedido não
  manda guardar o código e o dinheiro fica certo, por isso não baixei nota; mas se o financeiro precisar cobrar ou
  conferir o convênio por empresa, vai faltar o dado, e as reservas feitas até lá não vão ter como recuperá-lo.
