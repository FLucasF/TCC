# Revisão: manutenção, cenário 01

## Visão geral

O projeto é o da base, mudado com cuidado: os mesmos 7 pacotes (`hotel` monta tudo, `hotel.web` é a borda HTTP com Javalin, `hotel.reservations` guarda as regras e o acesso ao banco em `ReservationStore`, mais `rooms`, `documents`, `messages` e `reports`). Uma reserva entra por `HttpApi.readNewReservation` (`src/main/java/hotel/web/HttpApi.java:109-136`), passa por `ReservationService.create`, que agora pede ao plano a validação (`src/main/java/hotel/reservations/ReservationService.java:43`) e o preço (`ReservationService.java:48` → `PriceCalculator.java:18-20`), e é gravada no PostgreSQL por SQL com parâmetros. O schema evolui com Flyway: o agente acrescentou `V2__add_company_code.sql` sem mexer na V1. Os dois `switch` de plano da base (desconto e devolução) saíram e deram lugar a uma interface `RatePlanStrategy` com uma implementação por plano, ligada ao `enum RatePlan` pelo construtor de cada constante. Os 40 testes passam e as 72 conferências do roteiro também. O defeito mais sério é pequeno: o laço que soma as diárias foi copiado em quatro das cinco estratégias.

## Arquitetura

**Números:** 7 pacotes e 17 dependências, como na base (`measures/dependencies.md` e `measures/dependencies-base.md` mostram o mesmo grafo). Um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. `hotel.reservations` passou de 13 para 17 classes (508 → 742 linhas). A maior classe ainda é `HttpApi` (176 linhas; eram 170).
**Nota:** bom

A mudança fica toda na camada de regras. As cinco estratégias (`src/main/java/hotel/reservations/*RatePlanStrategy.java`) não conhecem Javalin nem JDBC, e o jdeps mostra `hotel.reservations` sem Javalin. A borda HTTP só ganhou o campo `companyCode`, que repassa sem regra (`src/main/java/hotel/web/CreateReservationRequest.java:16`, `HttpApi.java:135`). O SQL continua isolado em `ReservationStore`, que ganhou só a coluna nova (`ReservationStore.java:19`, `:32-33`, `:44`, `:133`). A montagem não mudou. O agente não criou dependência nova nem ciclo novo. O ciclo `documents` ↔ `reservations` vem da base.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (os 2 que já existiam e os 3 novos). Nenhum `switch` ou `if` sobre o plano fora das estratégias (o único `switch` de `src/main` é o de `ErrorCode` → status HTTP, `HttpApi.java:101`). O laço das diárias aparece copiado em 4 classes.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É o Strategy que a referência descreve como acerto: o `enum` com a estratégia passada no construtor de cada constante (`src/main/java/hotel/reservations/RatePlan.java:3-23`). A interface tem os três comportamentos de um plano: `validate`, `calculatePrice` e `calculateRefund` (`RatePlanStrategy.java:17-23`). Os dois planos que já existiam também foram para a estrutura (`StandardRatePlanStrategy.java`, `NonRefundableRatePlanStrategy.java`). Quem usa pede ao plano sem nomear nenhum: a validação em `ReservationService.java:43`, o preço em `PriceCalculator.java:19` e a devolução em `ReservationService.java:84`. Os `switch` da base sumiram (veja o `changes.patch` em `PriceCalculator.calculate` e `ReservationService.cancel`). As regras de quem pode usar o plano estão dentro do próprio plano: a sexta-domingo e as 2 noites em `WeekendPackageRatePlanStrategy.java:19-36`, as 7 noites em `LongStayRatePlanStrategy.java:14-21` e o código da empresa em `CorporateRatePlanStrategy.java:17-27`. Um plano novo é uma classe nova e uma constante nova no `enum`.

**O exagero.** Não há. A interface tem cinco implementações. `PriceCalculator.calculate` virou um repasse de uma linha (`PriceCalculator.java:18-20`), mas a classe já existia na base e continua guardando a temporada e o arredondamento (`:22-41`).

**O resto do design:**
- **Duplicação.** O laço que soma as diárias com a temporada (o `for` sobre as noites, o contador e `nightPrice`) está copiado em `StandardRatePlanStrategy.java:17-24`, `NonRefundableRatePlanStrategy.java:19-26`, `CorporateRatePlanStrategy.java:31-39` e `LongStayRatePlanStrategy.java:25-32`. O mesmo vale para a montagem do `Price` com o desconto. Mudar a regra da temporada ou do arredondamento das diárias continua num lugar só (`PriceCalculator.nightPrice`), mas o próximo plano "com desconto" vai copiar o laço de novo. Bastava um método comum, por exemplo `PriceCalculator.subtotal`, ou uma classe base para os planos de desconto percentual.
- **Acoplamento entre `validate` e `calculatePrice`.** `CorporateRatePlanStrategy.calculatePrice` supõe que `validate` já rodou: com um código inválido, `COMPANY_DISCOUNTS.get` devolve `null` e `subtotal.multiply(null)` dá `NullPointerException` (`CorporateRatePlanStrategy.java:37-38`). Pelo serviço isso não acontece, porque `ReservationService.java:43` valida antes. Mas chamar `PriceCalculator.calculate(..., CORPORATE, null)` diretamente quebra.
- **Comentário errado.** `WeekendPackageRatePlanStrategy.java:41` diz "subtotal = discount = 0", mas o código põe o preço do pacote no subtotal (`:42`). O comportamento é o pedido; o comentário engana. O mesmo método também fixa `2` noites em vez de contar (`:42`).
- **Detalhes.** `java.time.temporal.ChronoUnit` vem escrito por extenso no meio do código (`LongStayRatePlanStrategy.java:15`, `WeekendPackageRatePlanStrategy.java:30`). Para as estratégias usarem `nightPrice`, `isHighSeason` e `cents`, o agente os tornou `public static` (`PriceCalculator.java:22`, `:31`, `:39`).

Os tipos estão certos: `BigDecimal` com HALF_EVEN, `LocalDate`, `DayOfWeek` e `Map<RoomType, BigDecimal>` para os preços do pacote (`WeekendPackageRatePlanStrategy.java:13-16`). Não há estado mutável novo.

## Testes

**Números:** 40 testes rodaram, 0 falhas, 0 erros, 0 pulados, e há 40 métodos de teste no código. A base tinha 26, todos mantidos e só adaptados ao parâmetro novo, sem nenhuma asserção removida (`changes.patch`). Cobertura de linhas: 94,4% (538/570; base 93,8%). Cobertura de ramos: 87,1% (115/132; base 87,1%). Em `hotel.reservations`: 271/285 linhas e 63/68 ramos (`measures/tests.md`).
**Nota:** bom

O agente escreveu 14 testes novos, em três níveis:
- **Cálculo do preço.** O exemplo do financeiro para a Tecnosul, com diárias, desconto e total (`src/test/java/hotel/reservations/PriceCalculatorTest.java:78-87`, 779,80 / 93,58 / 686,22). Agrovale e Farmavida com valores (`:90-108`), a Farmavida já na alta temporada. O pacote deluxe a 820,00 (`:111-120`). A estadia longa a 1.574,37 (`:123-131`).
- **Serviço.** O convênio sem código e com código inválido dá `INVALID_REQUEST` (`ReservationServiceTest.java:164-173`). O pacote fora de sexta-domingo é recusado (`:188-197`). A devolução de 410,00 no pacote (`:200-206`). As 7 noites (6 recusa, 7 aceita, `:209-214`). A devolução integral do convênio e da estadia longa (`:176-185`, `:217-223`).
- **API.** Os status 400/201 e os totais dos três planos (`src/test/java/hotel/web/HttpApiTest.java:124-187`).

**A pergunta da mutação.** Trocar 0.12 por 0.21, o 7 por 8, `<` por `<=` nas 7 noites ou a metade da devolução do pacote faz algum teste falhar. Ficam sem rede:
- **O `nights != 2` do pacote** (`WeekendPackageRatePlanStrategy.java:31`). Nenhum teste tenta de uma sexta a um domingo de outra semana: o caso 12→15/03 cai antes, na regra do domingo.
- **Os preços do pacote standard (520,00) e da suíte (1.200,00)**, e o pacote na alta temporada.
- **A estadia longa com noites de temporada.**
- **O `companyCode` ignorado nos outros planos.**

As comparações de dinheiro nos testes da API também são frouxas: `asDouble()` com igualdade de `double` (`HttpApiTest.java:146`, `:166`, `:186`) funciona, mas é frágil para dinheiro; o jeito veio da base (`HttpApiTest.java:77`). São lacunas pequenas perto do que está conferido.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` e datas em `date`. A chave estrangeira tem índice. O `EXCLUDE` contra reserva dupla está mantido. Duas migrações Flyway: V1 igual à base e V2 nova (`measures/database.md`). A única conferência que acusou algo foram as colunas de tipo e situação sem `CHECK` (`rate_plan`, `status`, `type`, `channel`), que vêm da base.
**Nota:** bom

O agente fez certo ao acrescentar uma migração nova (`src/main/resources/db/migration/V2__add_company_code.sql:2`, `ALTER TABLE reservations ADD COLUMN company_code text`) em vez de editar a V1. Quem já roda o sistema sobe sem perder dado. O SQL novo usa parâmetro (`ReservationStore.java:32-33`, `:44`). Os quatro candidatos de SQL concatenado que o script listou juntam só a constante `COLUMNS`.

Um detalhe: o código da empresa é gravado em todo plano, não só no convênio (`ReservationStore.java:44` grava `request.companyCode()` sem olhar o plano). Depois a coluna é lida (`:133`), mas `Reservation` não guarda o valor (não há `companyCode` em `Reservation.java`), então ela só é escrita. Não corrompe nada, porque o texto diz que nos outros planos o código "pode ignorar". A falta de `CHECK` em `rate_plan` vem da base e o agente não a piorou: os 5 valores novos continuam como texto livre.

## Comportamento

**Números:** 72 de 72 conferências passaram (`measures/behavior.md`):
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 01: 21 de 21;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Os planos que o dono pediu funcionam pela API com os valores do financeiro:
- **Convênio.** Tecnosul a 686,22, com 779,80 de diárias e 93,58 de desconto. Agrovale e Farmavida com os seus percentuais. Sem código ou com código inexistente dá 400 `INVALID_REQUEST`. O cancelamento depois de pago devolve tudo.
- **Pacote de fim de semana.** 520,00, 820,00 e 1.200,00, inclusive em julho. Cancelado depois de pago devolve 410,00 no deluxe. Fora de sexta-domingo é recusado.
- **Estadia longa.** 1.574,37, a temporada vale nas noites de julho, 6 noites é recusado e o cancelamento devolve tudo.

O normal e o não reembolsável continuam como antes. O `companyCode` num plano STANDARD é ignorado. As reservas sobrevivem ao reinício e as duas reservas simultâneas nunca passam juntas. Para o dono da pousada, o que ele pediu está funcionando e nada do que já funcionava quebrou.

## Dúvidas

- O código da empresa só é aceito em maiúsculas: `"tecnosul"` é recusado (`CorporateRatePlanStrategy.java:22`). O texto diz que o código "é o nome dela em maiúsculas", então tratei como escolha razoável, mas um site que não padronize a caixa vai ter recusas.
- `PriceCalculator.calculate` ficou como um repasse de uma linha para a estratégia (`PriceCalculator.java:18-20`). Não contei como exagero porque a classe já existia na base e ainda guarda a temporada. Um revisor mais rígido poderia pedir que o serviço chamasse o plano direto.
