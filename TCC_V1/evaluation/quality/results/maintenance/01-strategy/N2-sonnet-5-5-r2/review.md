# Revisão: manutenção, cenário 01

## Visão geral

O projeto é o da base, com os mesmos 7 pacotes: `hotel` (montagem e `main`), `hotel.web` (Javalin: rotas, corpo e
resposta em JSON), `hotel.reservations` (serviço, cálculo do preço, planos e o acesso ao banco em `ReservationStore`),
`hotel.rooms`, `hotel.documents`, `hotel.messages` e `hotel.reports`. Uma criação de reserva entra em
`HttpApi` (que monta um `NewReservation`, `hotel/web/HttpApi.java:128-130`), passa por `ReservationService.create`
(`hotel/reservations/ReservationService.java:27-48`), que pede o preço ao `PriceCalculator` e grava pelo
`ReservationStore` em PostgreSQL (esquema por Flyway). A mudança do agente foi pequena e localizada: o `enum RatePlan`
ganhou uma estratégia por constante (`RatePlanPolicy`, com cinco implementações, inclusive as duas que já existiam), os
dois `switch` da base (desconto no cálculo, devolução no cancelamento) sumiram, e o `companyCode` foi acrescentado ao
corpo da criação. Os 72 itens do roteiro passaram, os 39 testes passaram, e o banco ficou como estava. Não achei
defeito sério; os pontos abertos são pequenos (o código da empresa não é guardado na reserva, alguns detalhes de forma).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais à base (`measures/dependencies.md` e
`measures/dependencies-base.md`); 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base.
`hotel.reservations` passou de 13 para 17 classes e de 508 para 689 linhas; `HttpApi` 170 → 171 linhas,
`ReservationService` 138 → 135.
**Nota:** bom

O agente não mexeu na forma do sistema: as classes novas (`CorporatePlan`, `WeekendPackagePlan`, `LongStayPlan`,
`StandardPlan`, `NonRefundablePlan`, `RatePlanPolicy`) ficam em `hotel.reservations`, sem Javalin nem JDBC (só
importam `hotel.rooms` e `java.*`, por exemplo `hotel/reservations/WeekendPackagePlan.java:3-7`). A borda HTTP só
ganhou um campo repassado (`hotel/web/CreateReservationRequest.java:16`, `hotel/web/HttpApi.java:129-130`); a regra
de quem pode usar o plano mora no plano, não no handler, e o erro sai pela `HotelException` que o tratador de erros
já transformava em 400 `INVALID_REQUEST`. O ciclo `documents` ↔ `reservations` vem da base
(`hotel/reservations/ReservationService.java:3` importa `ReservationReceipt`, que importa `Reservation` em
`hotel/documents/ReservationReceipt.java:3`) e não foi piorado. Nenhuma classe cresceu de forma desproporcional.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (os 2 da base e os 3 novos), 0 fora dela; 0 `switch`/`if` sobre o plano
restante no código de produção (os dois `switch` da base, em `PriceCalculator.calculate` e
`ReservationService.cancel`, foram removidos); 1 ponto de seleção, o construtor da constante do `enum`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern: é o Strategy na forma que a referência aceita como acerto, o `enum` com o comportamento passado no
construtor de cada constante (`hotel/reservations/RatePlan.java:7-31`): `STANDARD(new StandardPlan())` …
`LONG_STAY(new LongStayPlan())`, e o `enum` só delega (`RatePlan.java:25-31`). Cada plano tem num lugar só o preço, a
regra de quem pode usar e a devolução: o convênio valida o código e aplica o desconto da empresa
(`CorporatePlan.java:16-26`, mapa em `:10-13`) e devolve tudo (`:28-30`); o pacote valida sexta → domingo com 2 noites
e fixa o preço por tipo de quarto (`WeekendPackagePlan.java:19-29`) e devolve metade (`:32-34`); a estadia longa exige
7 noites e dá 10% (`LongStayPlan.java:11-20`); o normal e o não reembolsável foram levados para a estrutura também
(`StandardPlan.java:8-16`, `NonRefundablePlan.java:10-18`). Quem usa não pergunta qual é o plano: o cálculo monta o
preço base (com a temporada) e entrega ao plano (`PriceCalculator.java:25-26`), e o cancelamento só pede a devolução
(`ReservationService.java:82`). O próximo plano (melhor idade, 60 dias de antecedência) é uma classe e uma constante;
a interface recebe o `NewReservation` inteiro (`RatePlanPolicy.java:14`), o que já dá ao plano de antecedência o que
ele vai precisar (com a data de hoje, faltaria um relógio, mas isso é para o próximo pedido).

O exagero: não há. A interface tem cinco implementações e é o próprio pattern; os dois métodos estáticos auxiliares
(`RatePlanPolicy.java:19-26`) são pequenos e evitam repetir o arredondamento do desconto em três planos.

O resto do código: dinheiro em `BigDecimal` arredondado por `PriceCalculator.cents` (meio para o par), datas em
`LocalDate`, o dia da semana por `DayOfWeek` (`WeekendPackagePlan.java:21-22`), constantes com nome
(`LongStayPlan.java:8-9`, `NonRefundablePlan.java:8`). As estratégias não têm estado, então guardá-las nas constantes
do `enum` é seguro. A troca de ordem em `ReservationService.java:42-46` (o preço, e com ele a validação do plano, antes
da disponibilidade) é deliberada e testada. Detalhes que não pedem mudança antes de aceitar: o construtor de 6
argumentos mantido em `NewReservation.java:15-18` (usado pela leitura do banco, `ReservationStore.java:125-131`, e
pelos testes da base), que deixa o `companyCode` nulo em silêncio; `WeekendPackagePlan.java:27` daria
`NullPointerException` para um tipo de quarto novo sem preço no mapa (hoje os três tipos estão lá); e a linha de
comentário longa em `PriceCalculator.java:11`.

## Testes

**Números:** 39 testes rodaram, 0 falhas, 0 erros, 0 pulados; 39 métodos escritos (a base tinha 26, todos mantidos
e passando; 13 novos: 8 em `PriceCalculatorTest`, 2 em `ReservationServiceTest`, 3 em `HttpApiTest`). Cobertura:
linhas 510 de 541 (94,3%; base 93,8%), ramos 109 de 124 (87,9%; base 87,1%); `hotel.reservations` com 247 de 260
linhas e 57 de 60 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** bom

Os testes conferem a parte nova com valores, e com os números do pedido: Tecnosul 779,80 / 93,58 / 686,22, Agrovale
140,36 e Farmavida 194,95 (`src/test/java/hotel/reservations/PriceCalculatorTest.java:94-107`); o código ausente,
vazio, minúsculo e desconhecido recusados (`:110-115`); o pacote a 820,00 / 1.200,00 / 520,00, inclusive em janeiro e
julho (`:126-138`), e recusado de quinta a sábado, de sexta a segunda, de sexta a sábado e de sexta ao domingo da
semana seguinte (`:141-152`); a estadia longa 1.749,30 / 174,93 / 1.574,37 (`:155-161`), com a temporada (`:164-169`)
e recusada com 6 noites (`:172-175`). A devolução de cada plano novo é conferida pelo serviço, contra o banco
(`ReservationServiceTest.java:110-124`: 204,92, 410,00 e 1.574,37), e a API confere o 400 `INVALID_REQUEST` e o
`companyCode` ignorado nos outros planos (`HttpApiTest.java:124-150`).

A pergunta da mutação: trocar 0.12 por 0.21 ou 0.50 por 0.25 quebra testes (93,58 e 410,00); trocar `<` por `<=` em
`LongStayPlan.java:13` quebra o teste de 7 noites (`:155`); tirar qualquer uma das três condições de
`WeekendPackagePlan.java:20-22` quebra um dos casos de `:141-152`. Os testes da base só mudaram para a nova assinatura
de `calculate` (um auxiliar, `PriceCalculatorTest.java:82-87`), sem perder asserção. Ficou sem teste só o
cancelamento do pacote ainda pendente (devolve 0,00 pela regra que já existia), o que é pouco. A forma de
`ReservationServiceTest.java:108-133` tem uma linha em branco a mais e uma a menos entre os métodos, só cosmética.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` nas 6 colunas; nenhuma chave
estrangeira sem índice; nenhuma data em texto; `EXCLUDE USING gist` contra duas reservas na mesma noite; 1 migração
Flyway, `V1__create_tables.sql`, igual à da base; as colunas de situação e plano (`reservations.rate_plan`,
`reservations.status`, `rooms.type`, `messages.channel`) em `text` sem `CHECK` (`measures/database.md`).
**Nota:** bom

O agente não precisou mudar o banco e não mudou: os planos novos cabem em `rate_plan text`
(`src/main/resources/db/migration/V1__create_tables.sql:28`), que não tem `CHECK` (por isso nenhuma migração era
necessária), e a migração da base ficou intacta. O desconto e o total de cada reserva são gravados como já eram
(`ReservationStore.java:32-43`), então o preço não é recalculado ao ler. O SQL continua com parâmetros (os `+` que o
script lista só juntam constantes, `ReservationStore.java:19,33,83,98,111`). A falta de `CHECK` nas colunas de
situação e de tipo vem da base. O único ponto do agente é que o `companyCode` não é guardado (não há coluna, e
`ReservationStore.java:125-131` relê a reserva sem ele): a reserva de convênio não diz de qual empresa é, o que o
pedido não exigiu (veja Dúvidas).

## Comportamento

**Números:** 72 de 72 conferências passaram (`measures/behavior.md`): a parte comum 46 de 46, os dados depois de
reiniciar 3 de 3, o cenário 01 21 de 21, e as reservas simultâneas 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API: o convênio com os três descontos (686,22 na Tecnosul, como o financeiro
conferiu) e a recusa 400 `INVALID_REQUEST` sem código ou com código desconhecido; o pacote com preço fechado (820,00
no deluxe, 520,00, 1.200,00, e 820,00 também em julho), recusado fora de sexta → domingo, e devolvendo 410,00 quando
pago e cancelado; a estadia longa a 1.574,37, com a temporada nas noites de julho, recusada com 6 noites e devolvendo
tudo; o `companyCode` ignorado nos outros planos; e o normal e o não reembolsável como antes. Nada da parte comum
quebrou, os dados ficam depois de reiniciar e duas reservas simultâneas do mesmo quarto não passam juntas.

## Dúvidas

- O `companyCode` não é guardado com a reserva (nenhuma coluna nova; `ReservationStore.java:125-131`). O pedido não
  pede que ele apareça em lugar nenhum depois da criação, então não pesei; mas o dono provavelmente vai querer saber
  de qual empresa é cada reserva de convênio (para cobrar ou conferir o convênio), e hoje só o desconto gravado
  indica isso.
- O pedido diz que o código "é o nome dela em maiúsculas"; o agente recusa `tecnosul` em minúsculas
  (`CorporatePlan.java:16`, testado em `PriceCalculatorTest.java:111`). É uma leitura razoável e estrita do texto; aceitar
  sem diferenciar maiúsculas também seria defensável.
