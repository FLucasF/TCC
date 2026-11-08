# Revisão: manutenção, cenário 01

## Visão geral

O sistema da base tem sete pacotes: `hotel` (a montagem e o Flyway), `hotel.web` (o Javalin), `hotel.reservations`
(as regras e o `ReservationStore`, com o JDBC), mais `rooms`, `messages`, `documents` e `reports`. Uma criação de
reserva entra em `HttpApi`, que lê o JSON e monta um `NewReservation` (`web/HttpApi.java:128-130`). Depois passa por
`ReservationService.create` (`reservations/ReservationService.java:27-48`), que valida, pergunta o preço ao
`PriceCalculator` e grava pelo `ReservationStore` no PostgreSQL. O banco é criado por migrações do Flyway. O agente
pôs os planos de tarifa num Strategy: o `enum RatePlan` recebe no construtor uma `RatePlanRule` por constante
(`reservations/RatePlan.java:6-24`). O cálculo e o cancelamento só perguntam ao plano
(`PriceCalculator.java:26`, `ReservationService.java:82`). O agente também criou uma migração nova (`V2`) para o
`company_code`. O roteiro de comportamento passou inteiro (72 de 72) e os 37 testes passam. Não achei nada sério. Os
pontos menores são três: o `companyCode` é gravado em qualquer plano, a Farmavida (25%) não tem teste e a regra de
quem pode usar o plano fica dentro do cálculo do preço.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais na base e na final. Há 1 ciclo
(`hotel.documents` ↔ `hotel.reservations`), que já existia na base (`measures/dependencies-base.md`).
`hotel.reservations` passou de 13 para 18 classes (508 → 686 linhas); a maior classe continua sendo
`web/HttpApi.java` (171 linhas, eram 170).
**Nota:** bom

O agente não mexeu na estrutura de pacotes. As cinco classes novas de regra ficam em `hotel.reservations` e são
package-private, assim como o acesso `RatePlan.rule()` (`reservations/RatePlan.java:22`). Nenhuma delas importa
Javalin ou JDBC. Na borda HTTP, a única mudança foi repassar o campo novo (`web/HttpApi.java:128-130`,
`web/CreateReservationRequest.java:16`), e a regra do convênio não entrou no handler. O SQL do campo novo ficou no
`ReservationStore` (`reservations/ReservationStore.java:44`, `:133`). O ciclo `documents` ↔ `reservations` vem da
base: o `ReservationService` monta o `ReservationReceipt`, e o recibo lê a `Reservation`. O agente não o piorou.

## Design de baixo nível

**Números:** os 5 planos estão na estrutura (5 implementações de `RatePlanRule`, os 2 da base inclusive), com 0
planos fora dela. Os 2 `switch` de comportamento da base (desconto em `PriceCalculator.calculate` e devolução em
`ReservationService.cancel`) foram removidos, e não sobrou nenhum `switch` nem `if` sobre o plano em `src/main`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

- **O pattern:** é o "enum como estratégia" que a referência aceita. Cada constante recebe no construtor a sua regra
  (`reservations/RatePlan.java:6-14`), e a interface `RatePlanRule` tem o preço e a devolução
  (`reservations/RatePlanRule.java:15-18`). O cálculo faz a parte comum (as noites com a temporada) e entrega o
  resultado ao plano (`reservations/PriceCalculator.java:25-26`). O cancelamento só pergunta ao plano
  (`reservations/ReservationService.java:82`).
- **Cada plano num lugar só dele:**
  - o pacote de fim de semana: validação, preço fechado por tipo de quarto e devolução de metade
    (`reservations/WeekendPackageRule.java:20-31`);
  - o convênio: o mapa de empresas e a recusa (`reservations/CorporateRule.java:9-21`);
  - a estadia longa: o mínimo de 7 noites (`reservations/LongStayRule.java:13-17`);
  - o normal e o não reembolsável, que saíram dos `switch` (`reservations/StandardRule.java`,
    `reservations/NonRefundableRule.java`).

  O próximo plano é uma constante e uma classe, sem mexer no serviço nem no cálculo.
- **O que afasta um pouco:**
  - A regra de quem pode usar o plano não tem método próprio. Ela fica dentro de `price` ou de `discountRate`, que
    lançam a recusa (`LongStayRule.java:12-17`, `CorporateRule.java:15-20`). Um método chamado `discountRate` que
    valida e recusa esconde a regra pelo nome. Mesmo assim, a regra continua dentro do plano.
  - O `companyCode` é guardado em qualquer plano (`reservations/Reservation.java:39`,
    `reservations/ReservationStore.java:44`). Isso contradiz o comentário da mesma classe, que diz "null for the
    other plans" (`reservations/Reservation.java:74`), e o próprio teste grava "QUALQUER" numa reserva STANDARD
    (`src/test/java/hotel/web/HttpApiTest.java:120-121`). A referência manda contar isso nas dúvidas, e não muda a
    classe.
- **O exagero:** `DiscountedRule` (`reservations/DiscountedRule.java:7-17`) é uma classe abstrata com três subclasses
  (não reembolsável, convênio e estadia longa), que de fato tiram o desconto do mesmo jeito. Ela está dentro da parte
  que o pattern resolve e evita copiar o cálculo do desconto. Não há interface com implementação única, fábrica nem
  camada que só repasse.
- **O resto do código:** o dinheiro é `BigDecimal` com `cents()` (meio para o par), as datas são `LocalDate` e os
  mapas são imutáveis (`Map.of`). Há duas miudezas:
  - `LongStayRule.java:13` recalcula as noites com `ChronoUnit`, quando o `undiscounted.nights()` já as tem, porque
    `discountRate` não recebe o preço;
  - `WeekendPackageRule.java:25` daria `NullPointerException` (500) se aparecesse um tipo de quarto novo sem preço de
    pacote.

  Nenhuma delas pede mudança antes de aceitar.

## Testes

**Números:** 37 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 37 métodos escritos (a base tinha 26, e os 26
continuam lá). Cobertura: linhas 94,3% (512 de 543) e ramos 87,7% (107 de 122); a base tinha 93,8% e 87,1%. No pacote
`hotel.reservations`, 249 de 262 linhas e 55 de 58 ramos.
**Nota:** bom

O agente escreveu 11 testes novos, todos com valores, e não apagou nem enfraqueceu os da base. Os da base só
ganharam o argumento `null` novo (`ReservationServiceTest.java:36`, `ReportsTest.java:33`).

- **No cálculo** (`src/test/java/hotel/reservations/PriceCalculatorTest.java`):
  - o exemplo da Tecnosul (779,80 / 93,58 / 686,22) e a Agrovale (639,44) estão nas linhas 95-104;
  - a recusa sem código, com código vazio, com código em minúsculas e com código desconhecido está nas linhas
    107-113;
  - o código ignorado nos outros planos está nas linhas 116-121;
  - o pacote com os três preços, inclusive em julho, que é alta temporada, está nas linhas 124-133;
  - o pacote de quinta a sábado, de três noites e de uma noite está nas linhas 136-143;
  - o exemplo da estadia longa (1.749,30 / 174,93 / 1.574,37) e a temporada valendo nela estão nas linhas 146-160;
  - a estadia de 6 noites recusada (o limite exato) está nas linhas 163-166.
- **No serviço** (`ReservationServiceTest.java`):
  - as devoluções de 686,22, 410,00 e 1.574,37 e o `companyCode` lido de volta do banco estão nas linhas 164-177;
  - as recusas pelo serviço estão nas linhas 180-187.
- **Pela API**, o 400 `INVALID_REQUEST` e o 201 com código estão em `HttpApiTest.java:109-122`.

Pela pergunta da mutação: trocar 7 por 8 noites, sexta por sábado ou 0,50 por 0,40 na devolução quebra um teste. O
único buraco é a Farmavida: trocar 0.25 por 0.52 em `CorporateRule.java:12` não quebra nenhum teste. A Farmavida não
aparece em nenhum teste.

## Banco

**Números:** 3 tabelas e 2 migrações do Flyway (a `V1` igual à da base e uma `V2__add_company_code.sql` nova). Nenhuma
tabela sem chave primária, todo o dinheiro em `numeric(10,2)`, nenhuma chave estrangeira sem índice e nenhuma data em
texto. Sem CHECK ficaram `rate_plan`, `status`, `type` e `channel`, que já estavam assim na base. O `EXCLUDE` contra
reserva dupla foi mantido (`measures/database.md`).
**Nota:** bom

O agente evoluiu o esquema do jeito certo: uma migração nova
(`src/main/resources/db/migration/V2__add_company_code.sql:1`, `ALTER TABLE reservations ADD COLUMN company_code
text`), sem editar a `V1`. Por isso, um banco que já roda sobe sem problema. A coluna aceita nulo, o que está certo:
só o convênio usa o código. O INSERT e a leitura usam parâmetros (`reservations/ReservationStore.java:31-50`, `:133`).
Os candidatos a SQL concatenado que o script listou só juntam constantes (`COLUMNS`). A falta de CHECK em `rate_plan`
vem da base e ficou de fora o mesmo. Um efeito pequeno da escolha do design: o `company_code` também fica gravado
nas reservas que não são de convênio (o teste grava "QUALQUER" numa STANDARD, `HttpApiTest.java:120-121`). É um
dado sem uso, mas não é dado errado em nenhum valor de dinheiro.

## Comportamento

**Números:** 72 de 72 conferências passaram, com 0 falhas:
- parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- cenário 01: 21 de 21;
- reservas simultâneas: 2 de 2.
**Nota:** bom

Todos os exemplos do financeiro conferem pela API:
- Tecnosul: 779,80 de diárias, 93,58 de desconto e 686,22 de total;
- pacote no deluxe: 820,00, com 410,00 de volta no cancelamento;
- estadia longa: 1.749,30, 174,93 e 1.574,37.

Também conferem:
- Agrovale (639,44) e Farmavida (584,85);
- as recusas com 400 `INVALID_REQUEST`: convênio sem código ou com código desconhecido, pacote fora de sexta a
  domingo e estadia longa de 6 noites;
- o pacote a 820,00 em julho;
- a estadia longa com noites de temporada (1.754,30);
- as devoluções integrais do convênio e da estadia longa;
- o `companyCode` ignorado num STANDARD.

O normal e o não reembolsável continuam como na base: desconto de 37,48 e nada de volta no cancelamento. Para o dono
da pousada, os três planos funcionam como ele pediu, e o que já existia não mudou.

## Dúvidas

- **A nota dos testes:** fiquei entre `bom` e `com problemas`. Só a taxa da Farmavida (25%) não tem teste. É uma
  linha de mapa, e as outras duas empresas e todas as outras regras novas estão conferidas com valores. Por isso
  ficou `bom`.
- **O `companyCode` nos outros planos:** ele é guardado em qualquer plano, em vez de ser descartado fora do convênio
  (`reservations/Reservation.java:39`, contra o comentário em `:74`). O pedido dizia "pode ignorar". Pela API isso não
  muda nenhum valor, mas é uma regra do plano (só o convênio usa o código) que ficou fora dele. Pela referência, isso
  não muda a classe.
