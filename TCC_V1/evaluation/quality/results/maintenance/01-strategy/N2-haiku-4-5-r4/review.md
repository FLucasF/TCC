# Revisão: manutenção, cenário 01

## Visão geral

O projeto mantém a forma da base: `hotel.web` (Javalin, `HttpApi`) lê a requisição e chama `ReservationService`
(`hotel.reservations`), que valida, calcula o preço e grava pelo `ReservationStore` (JDBC com pool, PostgreSQL, esquema
criado por uma migração do Flyway). A montagem fica em `hotel.Hotel`. Para os três planos novos, o agente tirou os dois
`switch` da base (o desconto em `PriceCalculator.calculate` e a devolução em `ReservationService.cancel`) e criou a
interface `RatePlanStrategy` (`validate`, `calculatePrice`, `calculateRefund`), com uma classe por plano. Os cinco
planos estão nela, os dois antigos também, e um mapa (`RatePlanStrategies`) escolhe a classe. O campo `companyCode`
chega pela API até `NewReservation`. Os 72 itens do roteiro de comportamento passaram e os 35 testes passaram. Nenhum
defeito sério. O que há são problemas pequenos de design: o laço das diárias copiado em quatro estratégias, nomes de
classe escritos por extenso (com o pacote) no meio do código, e um desvio para a reserva pendente que não muda nada.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais na base e na final (`measures/dependencies.md` e `measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já vinha da base. `hotel.reservations` passou de 13 classes e 508 linhas para 18 classes e 719 linhas. A maior classe continua `HttpApi`, que foi de 170 para 176 linhas.
**Nota:** bom

O agente não mexeu na estrutura de pacotes nem acrescentou dependência. A borda HTTP continua fina: a única mudança em
`HttpApi` é passar `body.companyCode()` para `NewReservation` (`src/main/java/hotel/web/HttpApi.java:128-135`). As
regras dos planos novos ficaram em `hotel.reservations`, em classes que não usam Javalin nem JDBC
(`src/main/java/hotel/reservations/CorporateRatePlan.java`, `WeekendPackageRatePlan.java`, `LongStayRatePlan.java`). O
serviço pede a validação ao plano antes de olhar a disponibilidade
(`src/main/java/hotel/reservations/ReservationService.java:42-43`). O ciclo entre `documents` e `reservations` veio da
base, e o agente não o piorou. Um único ponto merece nota: o mapa de estratégias é estático
(`src/main/java/hotel/reservations/RatePlanStrategies.java:7-12`) e não é montado em `Hotel`. Mas as estratégias não têm
estado nem dependências, então isso não causa problema neste tamanho.

## Design de baixo nível

**Números:** os 5 planos (os 2 que já existiam e os 3 novos) estão na estrutura, com 5 implementações de `RatePlanStrategy`. Sobrou 0 `switch` ou `if` de comportamento sobre o plano: o `switch` da base em `PriceCalculator` e o de `ReservationService.cancel` foram removidos. Há 1 ponto de seleção (um mapa). O laço das diárias aparece copiado 4 vezes.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Cada plano tem o seu preço, a sua validação e a sua devolução num lugar só, exatamente como a referência
pede:
- a interface está em `src/main/java/hotel/reservations/RatePlanStrategy.java:6-12`;
- o convênio, com a tabela de empresas e a recusa sem código válido, em `CorporateRatePlan.java:10-20`;
- o pacote, com sexta a domingo, duas noites, preço fixo por tipo de quarto e devolução da metade, em
  `WeekendPackageRatePlan.java:10-38`;
- a estadia longa, com o mínimo de 7 noites e 10%, em `LongStayRatePlan.java:9-34`.

Os dois planos que já existiam também viraram classes (`StandardRatePlan.java`, `NonRefundableRatePlan.java`). O preço e
o cancelamento pedem ao plano sem nomeá-lo (`PriceCalculator.java:18-21`, `ReservationService.java:84-85`). A seleção
acontece num único mapa (`RatePlanStrategies.java:7-16`), o que a referência aceita. Um plano novo pede uma classe, uma
constante no `enum` e uma linha no mapa. O desvio `if (status == PENDING) refund = 0` (`ReservationService.java:86-88`)
não é regra de plano. Além disso, é redundante: a reserva pendente tem `amountPaid` 0,00
(`ReservationStore.java:47`).

**O exagero.** Não há estrutura sem necessidade. A interface tem cinco implementações, e o mapa é o ponto de seleção do
pattern. `PriceCalculator.calculate` virou um método que só repassa ao plano (`PriceCalculator.java:18-21`), mas é a
classe que já existia na base e que guarda a temporada e o arredondamento. Por isso não conta como camada nova.

**O resto do design.** O que um revisor exigente pediria para mudar:
- **O laço das diárias foi copiado em quatro estratégias:** `StandardRatePlan.java:16`, `NonRefundableRatePlan.java:18`,
  `CorporateRatePlan.java:26` e `LongStayRatePlan.java:23`. São o mesmo `for`, o mesmo acumulador e o mesmo
  `new Price(...)`. Três deles só diferem na taxa de desconto. Uma mudança na soma das noites (por exemplo, um novo
  acréscimo) teria de ser feita em quatro lugares. Um método comum em `PriceCalculator` (a soma das noites), ou uma
  estratégia de desconto percentual parametrizada pela taxa, evitaria a cópia.
- **Nomes de classe escritos por extenso, com o pacote, no meio do código, em vez de importados:**
  `java.util.Map` (`WeekendPackageRatePlan.java:10`), `java.time.temporal.ChronoUnit` (`WeekendPackageRatePlan.java:23`,
  `LongStayRatePlan.java:13`) e `java.math.RoundingMode` (`WeekendPackageRatePlan.java:37`).
- **Duas noites fixas no código:** `WeekendPackageRatePlan.java:32` grava `2` direto, em vez de usar o número de
  noites. Isso só funciona porque a validação garante as duas noites.
- **A estratégia é procurada duas vezes por reserva:** uma em `ReservationService.java:42` e outra em
  `PriceCalculator.java:19`.
- **Documentação apagada:** o agente apagou a documentação do `enum` (`RatePlan.java`; na base, cada constante explicava o
  plano) e não descreveu os planos novos. Também não atualizou o README, que ainda fala só de `STANDARD` e
  `NON_REFUNDABLE` (`README.md:32-38`).

Os tipos estão certos: `BigDecimal` com HALF_EVEN em todo o dinheiro, `LocalDate` e `DayOfWeek` nas datas e o `enum`
para o plano. São defeitos localizados, que não atrapalham a próxima mudança de forma séria.

## Testes

**Números:** final com 35 testes rodados, 0 falhas, 0 erros e 0 pulados (35 métodos), cobertura de linhas de 94,4% (538 de 570) e de ramos de 87,9% (116 de 132). Base com 26 testes, 93,8% das linhas e 87,1% dos ramos. Em `hotel.reservations`, 271 de 285 linhas e 64 de 68 ramos (`measures/tests.md`).
**Nota:** com problemas

O agente acrescentou 9 testes de serviço, contra o banco, que conferem valores. Cobrem os três exemplos do pedido:
- o convênio da Tecnosul, com 779,80, 93,58 e 686,22 (`src/test/java/hotel/reservations/ReservationServiceTest.java:179-186`);
- o pacote deluxe de 820,00, cancelado com devolução de 410,00 (`:206-220`);
- a estadia longa, com 1.749,30, 174,93 e 1.574,37 (`:229-235`).

Cobrem também as recusas (convênio sem código e com código inválido, `:171-176`; pacote fora de sexta a domingo,
`:198-203`; estadia longa com 6 noites, `:223-226`) e a devolução total do convênio e da estadia longa (`:189-195`,
`:238-243`). Os 26 testes da base continuam passando. Em `PriceCalculatorTest` só mudou a forma de chamar o método, que
trocou de assinatura (`src/test/java/hotel/reservations/PriceCalculatorTest.java:20-24`), e nenhuma asserção ficou mais
fraca.

A pergunta da mutação deixa regras novas sem rede:
- trocar o 0.18 da Agrovale ou o 0.25 da Farmavida (`CorporateRatePlan.java:11-12`) não faz nenhum teste falhar, porque
  só a Tecnosul é testada;
- o preço do pacote no standard, R$ 520,00, não é conferido;
- a regra `nights != 2` do pacote (`WeekendPackageRatePlan.java:23-26`) pode ser apagada sem nenhum teste falhar: nenhum
  teste tenta de uma sexta a um domingo de outra semana;
- a temporada na estadia longa não é conferida.

Nenhum teste passa pela API com o campo novo `companyCode` (`HttpApiTest` não foi tocado), então a ligação JSON → `NewReservation`
(`HttpApi.java:135`) só é exercida pelo roteiro de comportamento. O que mais importa está testado. As lacunas são
pontuais.

## Banco

**Números:** 3 tabelas; 1 migração (`V1__create_tables.sql`), igual à da base; sem tabela sem chave primária; o dinheiro todo em `numeric(10,2)`; sem chave estrangeira sem índice; sem data em texto. `rate_plan`, `status`, `type` e `channel` estão em `text` sem CHECK, como na base. Há um EXCLUDE contra a mesma noite reservada duas vezes (`measures/database.md`).
**Nota:** bom

A mudança não precisou de coluna nova: o plano já era guardado como texto (`ReservationStore.java:43`), e os novos
nomes cabem nele. O preço calculado por cada estratégia é gravado nas colunas que já existiam
(`ReservationStore.java:44-47`). O agente não editou a migração da base nem acrescentou SQL. A falta de CHECK em
`rate_plan` (`src/main/resources/db/migration/V1__create_tables.sql:28`) veio da base. O SQL usa parâmetros, as
conexões vêm do pool com try-with-resources, e a dupla reserva é barrada pela restrição EXCLUDE (`V1__create_tables.sql:36-38`).
Um ponto em aberto: o código da empresa não é guardado (veja Dúvidas).

## Comportamento

**Números:** 72 de 72 conferências passaram: 46 de 46 na parte comum, 3 de 3 depois de reiniciar, 21 de 21 no cenário 01 e 2 de 2 com reservas simultâneas (`measures/behavior.md`).
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- **O convênio:** os três descontos (Tecnosul 686,22, Agrovale 639,44 e Farmavida 584,85) e a recusa com 400
  `INVALID_REQUEST` sem código ou com código desconhecido.
- **O pacote:** 520,00, 820,00 e 1.200,00, o mesmo preço em julho, recusa fora de sexta a domingo e devolução de
  410,00.
- **A estadia longa:** 1.574,37, com a temporada valendo nas noites de julho, e recusa com 6 noites.
- **O `companyCode` nos outros planos:** é ignorado.

O normal e o não reembolsável continuam como antes, a parte comum segue intacta, os dados sobrevivem ao reinício e as
reservas simultâneas não duplicam.

## Dúvidas

- O código da empresa não é gravado no banco: `ReservationStore.add` (`ReservationStore.java:31-48`) não tem coluna para
  ele. O pedido não manda guardar, e nem o preço nem a devolução do convênio precisam dele depois da criação. Mas a
  pousada perde a informação de qual empresa fez a reserva. Por isso não pesou na nota.
- O código da empresa só é aceito em maiúsculas (`CorporateRatePlan.java:17`), então "tecnosul" é recusado. O pedido diz
  que o código "é o nome dela em maiúsculas", e a escolha é defensável.
