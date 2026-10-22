# Revisão: manutenção, cenário 01

## Visão geral

O agente manteve a estrutura da base: `hotel.web.HttpApi` lê o JSON (agora com `companyCode`,
`src/main/java/hotel/web/CreateReservationRequest.java:16`) e monta um `NewReservation`
(`src/main/java/hotel/web/HttpApi.java:128-129`). `hotel.reservations.ReservationService` valida e chama o
`PriceCalculator`, e o `ReservationStore` grava no PostgreSQL por JDBC, num esquema criado pela migração Flyway `V1`,
que o agente não mexeu. Os três planos novos (CORPORATE, WEEKEND_PACKAGE, LONG_STAY) foram acrescentados ao `enum`
`RatePlan`, e o comportamento de cada um foi espalhado em quatro `switch` sobre o plano: um que escolhe o cálculo, um do
desconto, um da validação e um da devolução. O sistema faz o que o dono pediu: 72 de 72 conferências do roteiro
passaram, e os 41 testes passam. O problema mais sério está no design: o texto avisa que "o comercial vive inventando
plano", e o pedido ficou como **omissão** do Strategy. O próximo plano vai exigir mudanças em quatro lugares de duas
classes.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e um ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números
são os mesmos de `measures/dependencies-base.md`: só mudaram as linhas de `hotel.reservations` (de 508 para 591) e de
`hotel.web` (de 236 para 237).
**Nota:** bom

O agente não criou pacote, dependência nem ciclo. O ciclo `documents` ↔ `reservations` veio da base. A borda HTTP
continua fina: a única mudança em `HttpApi` é passar `body.companyCode()` adiante (`src/main/java/hotel/web/HttpApi.java:129`).
As regras novas ficaram em `hotel.reservations`, sem Javalin e sem SQL (`src/main/java/hotel/reservations/PriceCalculator.java:36-89`,
`src/main/java/hotel/reservations/ReservationService.java:54-82`). O acesso ao banco continua isolado em
`ReservationStore`, que só mudou no `null` da linha 132. `ReservationService` cresceu de 138 para 173 linhas e passou a
ser a maior classe do projeto. Ainda não é grande demais, mas é ali que o próximo plano vai pesar (veja o design).

## Design de baixo nível

**Números:** 5 planos, nenhum numa estrutura própria. Há 4 `switch` de comportamento sobre `RatePlan`: o que escolhe o
cálculo (`PriceCalculator.java:37-40`), o do desconto (`PriceCalculator.java:50-56`), o da validação
(`ReservationService.java:55-77`) e o da devolução (`ReservationService.java:116-120`). Os códigos das empresas estão
escritos em 2 lugares e a contagem de noites em 2.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O pattern: na base, o comportamento dos planos já era decidido por dois `switch`, um do desconto e um da devolução. O
agente fez esses dois `switch` crescerem para cinco casos e acrescentou mais dois. Pelos caminhos (os paths) do projeto:

- o preço: `src/main/java/hotel/reservations/PriceCalculator.java:37-40` desvia o WEEKEND_PACKAGE para
  `calculateWeekendPackage`. O `switch` do desconto, nas linhas 50-56, trata os outros quatro planos e tem um
  `case WEEKEND_PACKAGE -> throw new AssertionError(...)` (linha 55), um caso que só existe porque a escolha ficou
  dividida entre dois `switch`;
- quem pode usar: `src/main/java/hotel/reservations/ReservationService.java:54-78` é um `switch` com as três regras de
  elegibilidade dentro do serviço, fora de qualquer objeto de plano;
- a devolução: `src/main/java/hotel/reservations/ReservationService.java:116-120`.

Nenhum plano tem o seu preço, a sua validação e a sua devolução num lugar só dele. Os dois planos que já existiam
continuam nos `switch`, e um plano para a melhor idade exigiria mexer nos quatro. A referência chama isso de omissão,
e é omissão. Não há estrutura sem necessidade (nem interface sem uso, nem fábrica), então o exagero é contenção.

O resto do código novo tem defeitos localizados:

- a lista de empresas está em dois lugares: no mapa `CORPORATE_DISCOUNTS` (`PriceCalculator.java:22-25`) e, de novo, em
  `isValidCompanyCode` (`ReservationService.java:80-82`). Quem acrescentar uma empresa no mapa e esquecer o serviço vai
  ter a reserva recusada;
- a contagem de noites foi copiada num laço (`ReservationService.java:67-70`), igual ao de `PriceCalculator.java:44-49`;
- o preço do pacote fica num `Map<String, BigDecimal>` cuja chave é `room.type().toString()`
  (`PriceCalculator.java:27-30` e `61-62`), em vez de um `EnumMap<RoomType, ...>`. É texto solto onde já existe um
  `enum`;
- o `calculate` de quatro argumentos passa `companyCode = null` (`PriceCalculator.java:32-34`). Com CORPORATE, a chamada
  `CORPORATE_DISCOUNTS.get(null)` lança um `NullPointerException`, porque `Map.of` não aceita `get(null)` (linha 53). O
  código só não quebra porque o serviço valida antes;
- o pacote tem as 2 noites fixas no código (`PriceCalculator.java:63`) e a devolução usa o número `0.5` direto
  (`ReservationService.java:119`), enquanto os outros percentuais são constantes nomeadas;
- quando lê uma reserva, o `NewReservation` é montado com `companyCode` igual a `null`
  (`src/main/java/hotel/reservations/ReservationStore.java:132`). Isso só não mente porque o campo nunca é lido depois
  da criação.

O que já existia (`nightPrice`, `isHighSeason`, `cents` com HALF_EVEN, `BigDecimal` para o dinheiro) foi mantido como
na base.

## Testes

**Números:** 41 testes rodaram, nenhum falhou ou foi pulado, e há 41 métodos escritos (a base tinha 26: são 15 novos e
nenhum foi apagado). Cobertura de linhas: 94,2% (506 de 537; na base, 93,8%). Cobertura de ramos: 87,2% (129 de 148;
na base, 87,1%). No pacote `hotel.reservations`, 244 de 257 linhas e 77 de 84 ramos.
**Nota:** bom

As regras novas são conferidas com valores:

- os três descontos de empresa, com os números do financeiro (`src/test/java/hotel/reservations/PriceCalculatorTest.java:78`,
  `:156`);
- os três preços fechados do pacote (`PriceCalculatorTest.java:99`);
- a estadia longa, com e sem alta temporada (`PriceCalculatorTest.java:122`, `:134`);
- a validação de sexta a domingo (`PriceCalculatorTest.java:146`);
- as recusas e a devolução de cada plano novo, contra o banco (`src/test/java/hotel/reservations/ReservationServiceTest.java:164-226`),
  incluindo o R$ 410,00 do pacote (`:203`) e o limite de 6 e 7 noites (`:213`);
- os três planos pela API (`src/test/java/hotel/web/HttpApiTest.java:124-181`).

Fiz a pergunta da mutação: trocar 0.12 por 0.21, trocar `nights < 7` por `nights <= 7`, trocar o 0.5 da devolução ou
apagar o caso de código inválido faria algum teste falhar. Os testes têm fraquezas pequenas:

- os testes da API que esperam 400 conferem só o status, e não o código `INVALID_REQUEST` que o site pediu
  (`HttpApiTest.java:130`, `:137`, `:154`, `:173`);
- nenhum teste confere que o `companyCode` é ignorado nos outros planos;
- `validatesUserProvidedNumbers` (`PriceCalculatorTest.java:156`) repete o que `corporateDiscountByCompany` e
  `longStayDiscount` já conferem.

Os 26 testes da base continuam lá e passando. A única mudança neles foi o `null` a mais no construtor.

## Banco

**Números:** 3 tabelas e 1 migração (`V1__create_tables.sql`), igual à da base. Nenhuma tabela sem chave primária,
todo o dinheiro em `numeric(10,2)`, nenhuma chave estrangeira sem índice e nenhuma data guardada como texto. A coluna
`rate_plan` é `text` sem CHECK, como na base. Os candidatos a SQL montado com `+` só juntam constantes.
**Nota:** bom

Os planos novos não precisavam de mudança no esquema, porque a coluna `rate_plan` é texto e não tem CHECK que recusaria
os nomes novos (`src/main/resources/db/migration/V1__create_tables.sql`, tabela `reservations`). O agente não editou a
migração da base. O preço calculado (diárias, desconto e total) é gravado como antes (`ReservationStore.java:44-47`), e
por isso a devolução do pacote sai do `amount_paid` gravado, sem recalcular nada. A proteção contra reserva dupla
continua na restrição `EXCLUDE` da base. Um ponto fica em aberto: o código da empresa não é guardado
(`ReservationStore.java:31-33` não tem a coluna, e a linha 132 lê `null`). O texto não pedia que fosse; veja as
dúvidas.

## Comportamento

**Números:** 72 de 72 conferências passaram: 46 de 46 na parte comum, 3 de 3 depois de reiniciar, 21 de 21 no cenário
01 e 2 de 2 nas reservas simultâneas.
**Nota:** bom

Nada falhou. Para o dono da pousada, isso quer dizer o seguinte:

- os números do financeiro saem como ele mandou: Tecnosul 686,22, pacote deluxe 820,00 com devolução de 410,00, e
  estadia longa 1.574,37;
- Agrovale e Farmavida dão 18% e 25%;
- o convênio sem código, ou com um código que não existe, é recusado com 400 `INVALID_REQUEST`;
- o pacote fora de sexta a domingo é recusado, e o preço não muda em julho;
- a estadia longa com 6 noites é recusada, e a temporada continua valendo nas noites de temporada;
- o `companyCode` é ignorado nos outros planos;
- o normal e o não reembolsável continuam como antes.

## Dúvidas

- O código da empresa é descartado depois de dar o desconto (`ReservationStore.java:132`): a pousada não consegue
  saber, pela reserva, de qual empresa foi o convênio. O texto não pede que ele seja guardado, por isso não pesou na
  nota do banco. Mas é provável que o comercial queira esse dado para cobrar ou conferir o convênio.
- O agente decidiu que o código da empresa tem de vir exatamente em maiúsculas (`ReservationService.java:81`), e que
  "tecnosul" é recusado. O texto diz que "o código é o nome dela em maiúsculas", e por isso a escolha é defensável.
