# Revisão: manutenção, cenário 01

## Visão geral

O projeto (herdado da base) tem sete pacotes: `hotel.web` (as rotas Javalin em `HttpApi`), `hotel.reservations` (o
serviço, o cálculo do preço e o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages`,
`hotel.documents`, `hotel.reports` e o `hotel`, que monta tudo. Uma reserva entra por `HttpApi.readNewReservation`
(`src/main/java/hotel/web/HttpApi.java:109`), vai para `ReservationService.create`
(`src/main/java/hotel/reservations/ReservationService.java:27`), que pede o preço ao `PriceCalculator` e grava pelo
`ReservationStore` no PostgreSQL, com o esquema criado por migrações do Flyway. O agente trocou os dois `switch` sobre
o plano (desconto e devolução) por uma interface `RatePlanStrategy` com uma classe para cada um dos cinco planos,
escolhida por um mapa. Acrescentou o campo `companyCode` da API até o banco, por uma migração nova (`V2`). As 72
conferências do roteiro passam, e os 38 testes também. Nada de sério. Os defeitos são pequenos: o mesmo
`calculatePrice` copiado em quatro estratégias, alguns valores fixos no pacote de fim de semana, o `companyCode`
guardado mesmo nos planos que não o usam e o README que não fala dos planos novos.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base.
`hotel.reservations` passou de 13 classes/508 linhas para 18 classes/770 linhas. A maior classe continua sendo
`HttpApi` (176 linhas; eram 170).
**Nota:** bom

A mudança respeita as camadas da base. A borda HTTP só ganhou o repasse do campo novo
(`src/main/java/hotel/web/CreateReservationRequest.java:16`, `src/main/java/hotel/web/HttpApi.java:128-135`), sem
nenhuma regra de plano no handler. As regras novas ficam em `hotel.reservations`, em classes sem Javalin e sem JDBC
(`CorporateRatePlan`, `WeekendPackageRatePlan`, `LongStayRatePlan` etc.). O SQL do campo novo ficou no
`ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:19,32,44,133`). O serviço continua só
pedindo ao calculador (`ReservationService.java:46-47,83`). O agente não criou dependência nova entre pacotes nem
ciclo novo. O ciclo `documents`↔`reservations` veio da base.

## Design de baixo nível

**Números:** os 5 planos (os 2 que já existiam e os 3 novos) estão na estrutura, e nenhum fica fora dela. Sobrou 0
`switch`/`if` de comportamento sobre o plano no código de produção: o único `switch` restante é o do código de erro
para o status HTTP (`HttpApi.java:101`), que veio da base. A seleção é um único mapa
(`PriceCalculator.java:16-21`). Nas estratégias, há 4 cópias quase iguais de `calculatePrice`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A interface `RatePlanStrategy` (`src/main/java/hotel/reservations/RatePlanStrategy.java:11-25`) junta
num lugar as três coisas que variam por plano:

- a validação (`validate`);
- o preço (`calculatePrice`);
- a devolução (`calculateRefund`).

Cada plano tem a sua classe, os dois antigos inclusive (`StandardRatePlan.java:10`, `NonRefundableRatePlan.java:10`).
As regras de quem pode usar cada plano moram no próprio plano:

- o código da empresa em `CorporateRatePlan.java:19-25`;
- a sexta a domingo em `WeekendPackageRatePlan.java:21-38`;
- as 7 noites em `LongStayRatePlan.java:16-23`.

O cálculo e o cancelamento pedem ao plano sem nomeá-lo (`PriceCalculator.java:23-33`,
`ReservationService.java:83`). O próximo plano (melhor idade, antecedência de 60 dias) pede uma classe nova, uma
constante no `enum` e uma linha no mapa. Isso é o acerto que a referência descreve: uma implementação por plano,
escolhida por um mapa.

Há duas pequenas ressalvas no pattern, que não mudam a classe:

- o `companyCode` atravessa a assinatura de todas as estratégias (`RatePlanStrategy.java:18,21`), embora só o
  convênio o use;
- o mapa é `static` dentro do `PriceCalculator` (`PriceCalculator.java:16`), e não injetado. Ele é imutável
  (`Map.of`), então não causa problema de estado.

**O exagero.** A classe abstrata `BaseRatePlanStrategy` tem cinco filhas e guarda o que é comum. Não há fábrica,
builder nem camada que só repasse. Por isso: contenção.

**O resto do design** tem defeitos localizados:

- **Duplicação.** `calculatePrice` é a mesma sequência de quatro linhas em quatro classes: contar as noites, somar as
  diárias, aplicar uma porcentagem e montar o `Price`. Veja `StandardRatePlan.java:13-18`,
  `NonRefundableRatePlan.java:15-20`, `CorporateRatePlan.java:28-34` e `LongStayRatePlan.java:26-31`. Só a
  porcentagem muda. Um gancho de desconto na classe base tiraria a cópia.
- **Contagem de noites.** O `countNights` é um laço que conta dia a dia (`BaseRatePlanStrategy.java:39-46`), quando
  `ChronoUnit.DAYS.between` resolve. O `LongStayRatePlan` conta as noites duas vezes, na validação e no preço
  (`LongStayRatePlan.java:17,27`).
- **Valores fixos e `null` no pacote.** O pacote fixa `int nights = 2` (`WeekendPackageRatePlan.java:43`) em vez de
  usar a contagem. Escreve a porcentagem de devolução ali mesmo, como `new BigDecimal("0.50")`
  (`WeekendPackageRatePlan.java:50`), enquanto os outros planos usam constantes com nome. O
  `PACKAGE_PRICES.get(room.type())` devolveria `null` para um tipo de quarto novo sem preço de pacote
  (`WeekendPackageRatePlan.java:42`).
- **Temporada no lugar errado.** A regra da temporada saiu do `PriceCalculator` e foi para a classe base das
  estratégias (`BaseRatePlanStrategy.java:13,31-37,48-54`). O comentário do `PriceCalculator`
  (`PriceCalculator.java:9-13`) continua dizendo que é ele quem soma o acréscimo da temporada, e isso já não é
  verdade. A temporada não é uma regra de plano, e o pacote herda um `nightPrice` que não usa.

Os tipos estão certos: `BigDecimal` com `cents` meio-para-o-par e `LocalDate`. Não há estado mutável novo.

## Testes

**Números:** final: 38 testes rodaram (0 falhas, 0 erros, 0 pulados), 38 métodos escritos; linhas 535/568 (94,2%),
ramos 110/126 (87,3%); `hotel.reservations` 268/283 linhas e 58/62 ramos (`measures/tests.md`). Base: 26 testes,
linhas 93,8%, ramos 87,1% (`measures/tests-base.md`). Há testes contra o banco (4 arquivos) e pela API (1); nenhum
usa mocks.
**Nota:** com problemas

O agente manteve os 26 testes da base passando. Mudou neles só a assinatura (o `null` do `companyCode`) e a classe do
`isHighSeason` (`PriceCalculatorTest.java:49-54`); não apagou nem enfraqueceu nenhum. Escreveu 12 testes novos que
conferem, com valores, os três exemplos do texto:

- o convênio da Tecnosul dá R$ 779,80, R$ 93,58 e R$ 686,22 (`PriceCalculatorTest.java:78-90`);
- o pacote no deluxe sai R$ 820,00 (`PriceCalculatorTest.java:93-106`);
- a estadia longa dá R$ 1.749,30, R$ 174,93 e R$ 1.574,37 (`PriceCalculatorTest.java:109-121`);
- a devolução do pacote é R$ 410,00 (`ReservationServiceTest.java:213-220`).

As recusas também estão testadas:

- convênio sem código e com código inválido (`ReservationServiceTest.java:171-181`, e pela API em
  `HttpApiTest.java:139-149`, com 400 e `INVALID_REQUEST`);
- pacote fora de sexta a domingo, nos três casos (`ReservationServiceTest.java:194-210`);
- estadia longa com 6 noites recusada e com 7 aceita (`ReservationServiceTest.java:223-230`), o que pega a troca de
  `<` por `<=`.

Ficam sem rede algumas regras novas. Pela pergunta da mutação:

- Os descontos da Agrovale (18%) e da Farmavida (25%) nunca são conferidos. A Agrovale só aparece num teste de
  devolução (`ReservationServiceTest.java:185`). Trocar `0.18` por `0.81` ou `0.25` por `0.52` em
  `CorporateRatePlan.java:14-16` não derruba nenhum teste.
- Os preços do pacote no standard (R$ 520,00) e na suíte (R$ 1.200,00) não são testados. Também não há teste do
  pacote em alta temporada, que deveria continuar com o preço fechado.
- A estadia longa com noites de temporada (o desconto sobre a diária com os 20%) não é testada.
- As devoluções do convênio e da estadia longa só conferem `amountPaid == refund`
  (`ReservationServiceTest.java:190,239`), sem um valor.

As regras pedidas estão quase todas testadas e passam. As lacunas estão na parte nova, e por isso a nota é `com
problemas`.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)` e as datas em `date`. Não
há chave estrangeira sem índice. As colunas `rate_plan`, `status`, `rooms.type` e `messages.channel` são `text` sem
`CHECK`, como já eram na base. A reserva dupla é protegida por `EXCLUDE USING gist`. Há 2 migrações:
`V1__create_tables.sql` está igual à da base e `V2__add_company_code.sql` é nova (`measures/database.md`).
**Nota:** bom

O agente fez a mudança do jeito certo: acrescentou uma migração nova
(`src/main/resources/db/migration/V2__add_company_code.sql:2`, `ALTER TABLE reservations ADD COLUMN company_code
text`) sem tocar na `V1`, de modo que um banco que já roda o sistema sobe e ganha a coluna. A coluna é anulável, o
que está certo porque só o convênio a usa. O SQL novo usa parâmetros (`ReservationStore.java:31-33,44`), e a leitura
traz o campo de volta (`ReservationStore.java:133`).

Uma observação pequena: o código grava o `companyCode` que vier em qualquer plano (`ReservationStore.java:44`), sem
olhar se o plano é o convênio. Uma reserva STANDARD pode ficar com um código de empresa qualquer no banco, embora o
preço o ignore (a conferência do roteiro passa). Não há `CHECK` sobre `company_code` nem sobre `rate_plan`, mas a
falta desse `CHECK` em `rate_plan` vem da base. Isso não perde nem corrompe dado.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam (`measures/behavior.md`):

- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 01: 21/21;
- reservas simultâneas: 2/2.

**Nota:** bom

O sistema faz o que o dono pediu.

- **Convênio.** Os três convênios dão os descontos certos: Tecnosul R$ 686,22, Agrovale R$ 639,44 e Farmavida
  R$ 584,85. Sem código, ou com um código que não existe, a reserva é recusada com 400 `INVALID_REQUEST`. Quem cancela
  o convênio recebe tudo.
- **Pacote de fim de semana.** O pacote sai R$ 520,00 no standard, R$ 820,00 no deluxe (também em julho) e R$ 1.200,00
  na suíte. É recusado fora de sexta a domingo e devolve R$ 410,00 quando é cancelado.
- **Estadia longa.** Dá R$ 1.574,37 no exemplo do financeiro, aplica a temporada nas noites de julho (R$ 1.754,30),
  recusa 6 noites e devolve tudo.
- **O que já existia.** O STANDARD ignora o `companyCode` no preço, e o NON_REFUNDABLE continua com 15% e sem
  devolução. A parte comum, os dados depois de reiniciar e as reservas simultâneas seguem como na base.

## Dúvidas

- O `companyCode` é guardado em qualquer plano (`ReservationStore.java:44`). O pedido diz que nos outros planos "pode
  ignorar". O preço o ignora, mas o dado fica no banco. A referência trata esse tipo de regra pequena fora do plano
  como dúvida, e por isso não mudei a classe por ela.
- A validação do plano roda depois da conferência de quarto ocupado (`ReservationService.java:42-47`). Um pacote numa
  data errada e num quarto ocupado responde 409, e não 400. O texto não decide qual erro vem primeiro.
- O README (`README.md:32-38`) continua descrevendo só os dois planos antigos. O pedido não exigia que fosse
  atualizado.
