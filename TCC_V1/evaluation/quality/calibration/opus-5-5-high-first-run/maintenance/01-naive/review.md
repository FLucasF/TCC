# Revisão: manutenção, cenário 01

## Visão geral

O agente manteve a estrutura da base: sete pacotes (`hotel`, `hotel.web`, `hotel.reservations`, `hotel.rooms`,
`hotel.documents`, `hotel.messages`, `hotel.reports`). Uma requisição entra pelo `HttpApi` (Javalin), que monta um
`NewReservation` e chama o `ReservationService`. O serviço valida o pedido, pede o preço ao `PriceCalculator` e grava
pelo `ReservationStore` (JDBC sobre PostgreSQL, com o esquema em migrações do Flyway). A mudança (11 arquivos) põe três
constantes novas no `enum RatePlan` e o campo `companyCode` do pedido até o banco, com a migração `V2`. A regra de cada
plano ficou espalhada: as validações viraram três `if (ratePlan == …)` no serviço, o preço virou um `if` mais um
`switch` maior no `PriceCalculator`, e a devolução virou um `switch` maior no `cancel`. O comportamento está certo: o
roteiro passou em 72 de 72 conferências. Os dois problemas mais sérios são estes. Primeiro, o Strategy que o texto
pedia não foi feito, e o próximo plano (melhor idade, 60 dias de antecedência) vai mexer de novo em três lugares de
duas classes. Segundo, nenhum teste novo: todas as regras novas estão sem teste.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem
os mesmos números (`measures/dependencies-base.md`): só mudam as linhas de `hotel.reservations` (508 → 554) e de
`hotel.web` (236 → 237).
**Nota:** bom

A mudança não criou dependência nem pacote novo, e as camadas continuam no lugar:

- o `HttpApi` só repassa o campo novo (`src/main/java/hotel/web/HttpApi.java:128-129`);
- as regras novas não usam Javalin nem SQL (`src/main/java/hotel/reservations/ReservationService.java:48-63`);
- o SQL novo fica só no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:19,32-33,50,133`).

O ciclo `documents` ↔ `reservations` já existia na base: o serviço importa `ReservationReceipt`
(`ReservationService.java:3`). O agente não o piorou.

Há um acoplamento novo pequeno, dentro do mesmo pacote: o serviço consulta a tabela de descontos do calculador
(`ReservationService.java:50` lê `PriceCalculator.COMPANY_DISCOUNTS`). Fica registrado no design.

## Design de baixo nível

**Números:** 5 planos e nenhum numa estrutura própria. O comportamento dos planos é decidido em 5 lugares, por
`switch` ou `if` sobre o plano:
- 3 `if` de validação em `ReservationService.java:48-63`;
- 1 `if` mais 1 `switch` de preço em `PriceCalculator.java:32-46`;
- 1 `switch` de devolução em `ReservationService.java:101-105`.

**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto deixa claro que os planos vão continuar chegando ("o comercial vive inventando plano"), cada
um com o seu preço, a sua regra de uso e a sua devolução. O agente fez o que a referência chama de omissão. O
`RatePlan` continua um `enum` sem comportamento (`src/main/java/hotel/reservations/RatePlan.java:3-14`). Os dois
`switch` da base apenas cresceram para cinco casos:
- o do desconto, `PriceCalculator.java:40-46`;
- o da devolução, `ReservationService.java:101-105`.

A regra de cada plano novo também ficou fora de qualquer estrutura:
- o pacote é um `if` à parte, antes do `switch` (`PriceCalculator.java:32-39`);
- as validações (código da empresa, sexta a domingo, 7 noites) são `if (request.ratePlan() == …)` no serviço
  (`ReservationService.java:48-63`).

Um plano novo, como o da melhor idade, obriga a mexer no `enum`, no `if` de validação, no `switch` de preço e no
`switch` de devolução, em duas classes.

**Exagero.** Não há estrutura sobrando: nenhuma interface, fábrica ou camada nova.

**O resto do código.** O código novo tem defeitos localizados:
- **Regra do convênio em duas classes.** A tabela de empresas mora no calculador (`PriceCalculator.java:19-23`) e é
  consultada pelo serviço (`ReservationService.java:50`).
- **Caso morto no preço.** O calculador faz um `get` sem conferir (`PriceCalculator.java:43`) e só funciona porque o
  serviço validou antes.
- **Ramo inalcançável.** O `case WEEKEND_PACKAGE` do `switch` (`PriceCalculator.java:45`) nunca é executado, porque o
  `if` da linha 32 retorna antes.
- **Conta jogada fora.** Para o pacote, o laço calcula o subtotal noite a noite (`PriceCalculator.java:28-31`) e
  depois o descarta.
- **Números mágicos no meio do código.** Os preços do pacote ficam dentro do `switch` (`PriceCalculator.java:34-36`)
  e o `0.5` da devolução fica solto (`ReservationService.java:104`). Os outros percentuais são constantes nomeadas
  (`PriceCalculator.java:17-18`).
- **`null` como sentinela.** O `companyCode` viaja como `String` que pode ser `null` por todas as camadas
  (`NewReservation.java:7`, `PriceCalculator.java:25`). Os testes passam `null` em toda chamada
  (`src/test/java/hotel/reservations/PriceCalculatorTest.java:22`).

Os tipos continuam certos: `BigDecimal` com HALF_EVEN pelo `cents` (`PriceCalculator.java:67-69`), `LocalDate` e
`DayOfWeek` nas datas (`ReservationService.java:55-61`).

## Testes

**Números:** 26 testes rodaram, 0 falharam, 0 pulados; 26 métodos de teste escritos, os mesmos 26 da base. Nenhum
teste novo. Cobertura (`measures/tests.md` × `measures/tests-base.md`):

| | base | final |
|---|---|---|
| linhas | 93,8% (471/502) | 91,3% (485/531) |
| ramos | 87,1% (101/116) | 74,5% (105/141) |
| ramos de `hotel.reservations` | 49 de 52 | 53 de 77 |

**Nota:** ruim

O agente só mexeu nos testes para compilar: acrescentou o argumento `null` em três lugares:
- `src/test/java/hotel/reports/ReportsTest.java:33`;
- `src/test/java/hotel/reservations/PriceCalculatorTest.java:22,33,42,59,69`;
- `src/test/java/hotel/reservations/ReservationServiceTest.java:36`.

Nenhum teste usa `CORPORATE`, `WEEKEND_PACKAGE`, `LONG_STAY` ou `companyCode` com valor (o `grep` em `src/test` só
acha os `null`). A pergunta da mutação dá a mesma resposta em toda regra nova: o `mvn verify` continuaria verde com
qualquer uma destas mudanças:
- trocar 0.12 por 0.21 (`PriceCalculator.java:20`);
- trocar `< 7` por `< 6` (`ReservationService.java:61`);
- trocar o `0.5` da devolução por `1` (`ReservationService.java:104`);
- apagar a recusa do convênio sem código (`ReservationService.java:48-53`).

O texto até dava os valores prontos (R$ 686,22, R$ 410,00, R$ 1.574,37), e eles não viraram teste. Os 26 testes da
base continuam passando e nenhum foi enfraquecido, mas a parte nova, que é toda a mudança, está sem testes. A queda
dos ramos cobertos (de 87% para 75%) mostra isso.

## Banco

**Números:** 3 tabelas, todas com chave primária. 2 migrações: a `V1` igual à da base, a
`V2__reservation_company_code.sql` nova. Dinheiro em `numeric(10,2)`, datas em `date`, a chave estrangeira com
índice, o `EXCLUDE` contra reserva dupla. As colunas `rate_plan` e `status` em `text` sem `CHECK` vêm da base. Nenhum
SQL montado com dado de fora: os `+` só juntam constantes.
**Nota:** bom

A mudança no esquema foi feita como deve ser numa base que já roda: uma migração nova que só acrescenta uma coluna
anulável (`src/main/resources/db/migration/V2__reservation_company_code.sql:2`), sem editar a `V1`. O `INSERT` e a
leitura continuam com parâmetros (`ReservationStore.java:31-33,50,133`).

Um detalhe pequeno: o código da empresa é gravado em qualquer plano (`ReservationStore.java:50` grava
`request.companyCode()` sem olhar o plano). Assim, uma reserva `STANDARD` com um `companyCode` qualquer guarda esse
texto no banco. O texto pedia para ignorá-lo nos outros planos. Não muda preço nem devolução, e por isso não pesa na
nota.

O resto vem da base e não mudou:
- a reserva dupla é protegida por restrição no banco, não só pelo `synchronized`;
- não há transações explícitas nem `CHECK` no plano.

## Comportamento

**Números:** 72 de 72 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 01: 21 de 21;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- **Convênio:** Tecnosul dá diárias de R$ 779,80, desconto de R$ 93,58 e total de R$ 686,22. Agrovale e Farmavida
  dão 18% e 25%. Sem código, ou com código desconhecido, a resposta é 400 `INVALID_REQUEST`. Se cancelar depois de
  pagar, devolve tudo.
- **Pacote:** o deluxe sai R$ 820,00 e devolve R$ 410,00. O standard sai R$ 520,00 e a suíte R$ 1.200,00, mesmo em
  julho. De sábado a segunda, ou de sexta a segunda, a resposta é 400.
- **Estadia longa:** o standard de 01 a 08/03 dá R$ 1.749,30, R$ 174,93 e R$ 1.574,37. A temporada continua valendo.
  Com 6 noites, a resposta é 400. Se cancelar, devolve tudo.
- **Outros planos:** o `companyCode` é ignorado no preço, e o `NON_REFUNDABLE` continua com 15%.

A parte que já existia continua igual à base.

Um ponto que o roteiro não confere e que o texto não fechou: as validações do plano rodam depois da checagem de
disponibilidade (`ReservationService.java:44-63`). Um convênio sem código para um quarto ocupado responde 409
`ROOM_UNAVAILABLE` em vez de 400. É uma escolha de ordem, não um erro.

## Dúvidas

- **Nota dos testes: `ruim` ou `com problemas`.** Os 26 testes da base passam e conferem com valores o que já
  existia; por esse lado caberia `com problemas`. Dei `ruim` porque o agente não escreveu nenhum teste e a parte nova
  ficou inteira sem teste. Isso fica mais perto de "não há testes (ou quase)" do que de "deixam parte das regras
  novas sem conferir".
- **Gravar o código da empresa em plano que não é convênio** (`ReservationStore.java:50`). É uma regra de plano fora
  do plano, pequena e que não é de preço nem de devolução. Não pesou na classe, que já é omissão pelos `switch` e
  pelos `if` de validação.
