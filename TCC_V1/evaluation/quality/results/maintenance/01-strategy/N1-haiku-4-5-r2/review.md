# Revisão: manutenção, cenário 01

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel` (montagem e `main`), `hotel.web` (rotas e JSON),
`hotel.reservations` (regras, cálculo de preço e acesso ao banco em `ReservationStore`), `hotel.rooms`,
`hotel.messages`, `hotel.documents` e `hotel.reports`. Uma requisição `POST /reservations` passa por
`HttpApi.readNewReservation` (`src/main/java/hotel/web/HttpApi.java:109`), segue para `ReservationService.create`
(`src/main/java/hotel/reservations/ReservationService.java:27`), que valida, chama `PriceCalculator.calculate` e grava
com `ReservationStore.add`. Os dados ficam no PostgreSQL, num esquema criado por uma migração do Flyway. O agente
acrescentou os três planos (CORPORATE, WEEKEND_PACKAGE, LONG_STAY) e o campo `companyCode` sem mexer nos pacotes nem no
banco. Pela API, o sistema faz tudo o que o pedido pede (72 de 72 conferências). O problema mais sério está no design:
o pedido avisa que vão chegar mais planos, e mesmo assim o comportamento de cada plano ficou espalhado em três `switch`
sobre o `RatePlan`, nos dois que já existiam e em um novo, de validação. Também há cópias da mesma lógica, e o código
da empresa informado pelo hóspede não é gravado.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais na base e na final. Há um ciclo, `hotel.documents` ↔
`hotel.reservations`, que já vinha da base (`measures/dependencies.md` e `measures/dependencies-base.md`). JDBC só em
`hotel`, `hotel.messages`, `hotel.reservations` e `hotel.rooms`; Javalin só em `hotel` e `hotel.web`.
**Nota:** bom

O agente não mexeu na estrutura. A borda HTTP continua fina: a única mudança em `HttpApi` é passar `body.companyCode()`
para o `NewReservation` (`src/main/java/hotel/web/HttpApi.java:128`), e o campo novo entra no DTO
`src/main/java/hotel/web/CreateReservationRequest.java`. As regras novas ficaram em `ReservationService` e em
`PriceCalculator`, que não conhecem o Javalin nem o JDBC (`ReservationService.java:52-86`, `PriceCalculator.java:24-97`).
O acesso ao banco continua isolado em `ReservationStore`, e o agente só mudou o mapeamento da linha
(`ReservationStore.java:131`). O único ciclo veio da base, e o agente não o piorou. As classes cresceram:
`PriceCalculator` foi de 53 para 119 linhas e `ReservationService` de 138 para 178, mas nenhuma ficou fora de
proporção. O que pesa contra a extensibilidade é a forma desse crescimento, e isso entra no design.

## Design de baixo nível

**Números:** 5 planos, nenhum numa estrutura própria. O comportamento de cada plano é escolhido por `switch` sobre o
`RatePlan` em 4 lugares: o preço (`PriceCalculator.java:26-31`), um `switch` interno no cálculo dos dois planos antigos
(`PriceCalculator.java:41-45`), a validação (`ReservationService.java:53-72`) e a devolução
(`ReservationService.java:120-125`). O laço que soma as diárias está copiado 3 vezes, e a lista de empresas aparece em
2 lugares.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O pedido descreve uma variação que vai continuar crescendo ("o comercial vive inventando plano… cada
plano chega com o seu jeito de calcular o preço, com as suas regras de quem pode usar e com a sua regra de devolução").
A resposta esperada era um Strategy: cada plano com o seu preço, a sua validação e a sua devolução num lugar só dele.
O agente fez o contrário e ampliou os `switch` que a base já tinha:
- `RatePlan` continua um `enum` sem comportamento, só com comentários novos (`src/main/java/hotel/reservations/RatePlan.java:3-14`);
- `PriceCalculator.calculate` virou um `switch` com um método privado por plano (`PriceCalculator.java:26-31`), e
  `calculateStandard` ainda decide entre STANDARD e NON_REFUNDABLE com outro `switch` que termina em
  `default -> throw new AssertionError()` (`PriceCalculator.java:41-45`);
- a validação de quem pode usar cada plano virou um `switch` novo no serviço (`ReservationService.java:53-72`);
- a devolução cresceu no `switch` de `cancel` (`ReservationService.java:120-125`).

Para o próximo plano (melhor idade, 60 dias de antecedência), alguém vai ter de mexer no `enum` e em três ou quatro
`switch` em duas classes. É a definição de omissão na referência.

**O exagero.** Nenhuma estrutura nova sem necessidade: nenhuma interface, fábrica ou camada de repasse. Contenção.

**O resto do código** (o que o agente escreveu):
- **Duplicação.** O laço que soma as diárias foi copiado em `calculateStandard`, `calculateCorporate` e
  `calculateLongStay` (`PriceCalculator.java:37-40`, `52-55`, `70-73`). Os códigos das empresas também estão em dois
  lugares que precisam andar juntos: a validação em `ReservationService.java:75-77` e o desconto em
  `PriceCalculator.java:82-87`. Com isso, `discountForCompany` tem dois `throw new IllegalArgumentException` que nunca
  rodam pelo serviço (`PriceCalculator.java:79-86`).
- **Tipos.** `packagePrice` faz `switch` sobre o texto `room.type().name()` em vez do `enum` `RoomType`, com um `default`
  que só existe por causa disso (`PriceCalculator.java:91-95`). Os preços e as taxas das empresas são constantes soltas
  na classe, e não dados do plano (`PriceCalculator.java:19-22`, `92-94`).
- **Restos.** O parâmetro `nights` de `packagePrice` não é usado (`PriceCalculator.java:90`), o import `DayOfWeek` não é
  usado (`PriceCalculator.java:6`), e há nomes qualificados por extenso no meio do código, sem import
  (`java.time.temporal.ChronoUnit` em `PriceCalculator.java:62` e em `ReservationService.java:65,80`,
  `java.math.RoundingMode` em `ReservationService.java:124`).
- **`null` sem cuidado.** `companyCode` é um `String` que pode ser `null` em todo o caminho. O `ReservationStore`
  remonta toda reserva lida do banco com `null` no lugar dele (`ReservationStore.java:131`).
- **Arredondamento.** O que funciona: `BigDecimal` com `HALF_EVEN` nas contas novas, e a devolução do pacote com
  `divide(TWO, 2, HALF_EVEN)` (`ReservationService.java:123-124`).

São defeitos localizados, mas reais. Um revisor exigente pediria a correção antes de aceitar.

## Testes

**Números:** 41 testes rodaram, 0 falhas, 0 erros, 0 pulados; 41 métodos escritos (a base tinha 26 e todos continuam
lá). Cobertura de linhas de 93,5% (531/568) e de ramos de 85,0% (136/160); na base eram 93,8% e 87,1%. No pacote
`hotel.reservations`, 264/283 linhas e 84/96 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

O que o agente fez bem: manteve os 26 testes da base, mudando só as assinaturas para o parâmetro novo
(`changes.patch`), e escreveu 15 testes em três níveis.
- **No cálculo:** os três exemplos do financeiro conferidos com valores (diárias, desconto e total), em
  `src/test/java/hotel/reservations/PriceCalculatorTest.java:78`, `:90` e `:102`.
- **No serviço:** as recusas com `INVALID_REQUEST` e as devoluções. O CORPORATE sem código e com código desconhecido
  está em `ReservationServiceTest.java:169`. A devolução da Agrovale, 639,44, está em `:184` e confere a taxa de 18%.
  O pacote em quarta a sexta e em quinta a domingo está em `:194`. A metade de 410,00 está em `:209`. As 6 noites
  recusadas estão em `:219`, e a devolução integral da estadia longa em `:230`.
- **Pela API:** 400 com `INVALID_REQUEST` e os totais, de `HttpApiTest.java:124` a `:187`.

As lacunas, pela pergunta da mutação:
- trocar a taxa da Farmavida (0.25) não derruba nenhum teste, porque FARMAVIDA não aparece nos testes;
- trocar os preços fechados de 520,00 e 1.200,00 também não, porque o pacote só é testado no deluxe;
- se o pacote passasse a aplicar a alta temporada, nenhum teste falharia (não há pacote em temporada);
- a estadia longa com noites de temporada não é testada (`PriceCalculatorTest.java:102` usa só março);
- não há teste de que o `companyCode` é ignorado nos outros planos;
- trocar `nights < 7` por `nights <= 7` derrubaria `longStaySevenNights` e o teste de serviço; essa regra tem rede.

Há ainda um comentário errado, "March 10-12 (3 nights): 249.90 each = 749.70", num teste que confere 2 noites de deluxe
(`PriceCalculatorTest.java:79`). Os testes passam e cobrem os exemplos do pedido, mas deixam sem conferir parte das
regras novas.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`;
datas em `date`; a chave estrangeira da reserva para o quarto tem índice; 1 migração do Flyway (`V1__create_tables.sql`),
igual à da base; o `EXCLUDE` contra reserva dupla está no lugar. As conferências só acusaram `rate_plan`, `status`,
`type` e `channel` em `text` sem `CHECK`, e isso veio da base (`measures/database.md`).
**Nota:** com problemas

O agente não mudou o esquema nem editou a migração da base, então nada quebra num banco que já tem dados. Como
`rate_plan` é `text` sem `CHECK`, os três valores novos entram sem migração. O SQL continua com parâmetros
(`src/main/java/hotel/reservations/ReservationStore.java:31-33`), com conexões em try-with-resources e pool, tudo da
base.

O defeito do agente: o código da empresa que o hóspede informa no convênio não é gravado. O `INSERT` não tem coluna
para ele (`ReservationStore.java:31-33`), `Reservation` não o guarda, e ao ler a reserva do banco o agente pôs `null` no
lugar (`ReservationStore.java:131`). Uma reserva CORPORATE fica no banco com um desconto de 12%, 18% ou 25% e nenhuma
indicação de qual empresa o deu. A pousada perde a informação de que vai precisar para conferir ou cobrar o convênio.
O preço e a devolução não dependem dela depois de criada a reserva, por isso a coluna não fica em `ruim`. Uma migração
`V2` com uma coluna `company_code` resolveria.

## Comportamento

**Números:** 72 de 72 conferências passaram, nenhuma falhou: parte comum 46/46, dados depois de reiniciar 3/3,
cenário 01 21/21 e reservas simultâneas 2/2 (`measures/behavior.md`).
**Nota:** bom

Pela API, tudo o que o dono pediu funciona:
- o convênio dá 12%, 18% e 25% (686,22, 639,44 e 584,85) e recusa a reserva sem código ou com um código desconhecido
  (400 `INVALID_REQUEST`);
- o pacote sai a preço fechado de 520,00, 820,00 e 1.200,00, inclusive em julho, recusa sábado a segunda e sexta a
  segunda, e devolve 410,00 quando é pago e cancelado;
- a estadia longa dá 1.574,37 em março e 1.754,30 com noites de temporada, recusa 6 noites e devolve tudo;
- o `companyCode` é ignorado no STANDARD, e o NON_REFUNDABLE continua com 15%.

A parte comum da base continua igual, os dados sobrevivem ao reinício e só uma de duas reservas simultâneas é criada.
O código da empresa não gravado (coluna Banco) não aparece em nenhuma chamada que o texto descreve, por isso não muda
esta nota.

## Dúvidas

- O pedido não diz se o código da empresa deve aparecer na reserva ou ser guardado. Contei a perda dele como defeito do
  banco porque é um dado que o hóspede informou e que a pousada provavelmente vai querer; outro revisor pode achar que,
  sem pedido explícito, isso não pesa.
- O pedido não diz se o código da empresa deve aceitar minúsculas ("tecnosul"). O agente aceita só maiúsculas
  (`ReservationService.java:76`), o que é coerente com "o código é o nome dela em maiúsculas".
