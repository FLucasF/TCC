# Revisão: manutenção, cenário 01

## Visão geral

O projeto continua com os sete pacotes da base: `hotel` (montagem em `Hotel.java:20-29` e o `Database.java` com o
Hikari e o Flyway), `hotel.web` (o Javalin em `HttpApi.java`), `hotel.reservations` (serviço, cálculo de preço,
planos, acesso à tabela `reservations`), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`. Uma
requisição `POST /reservations` entra em `HttpApi.java:64-67`, é lida em `readNewReservation` (`HttpApi.java:109-131`),
vai para `ReservationService.create` (`ReservationService.java:27-48`), que valida, pede o preço ao
`PriceCalculator` (`PriceCalculator.java:18-27`, que por sua vez pede ao plano), confere a disponibilidade e grava via
`ReservationStore.add` (`ReservationStore.java:30-61`) num PostgreSQL criado pela migração `V1__create_tables.sql`.
O agente fez a mudança que o pedido descreve do jeito que o texto sugere: tirou os dois `switch` sobre o plano
(preço e devolução) e os substituiu por uma interface `RatePlanPolicy` com uma classe por plano, os cinco, ligada à
constante do `enum RatePlan`. Os 39 testes passam, as 72 conferências do roteiro passam e o banco não foi tocado.
Não encontrei nada sério; o único ponto que fica em aberto é que o código da empresa não é guardado na reserva.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre pacotes, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais
aos da base (`measures/dependencies.md` e `measures/dependencies-base.md`). `hotel.reservations` foi de 13 classes e
508 linhas para 17 classes e 689 linhas; `hotel.web` de 236 para 238 linhas. Maior classe: `HttpApi.java`, 171
linhas (170 na base).
**Nota:** bom

A mudança ficou toda na camada de regras. A borda HTTP só ganhou o campo novo: `CreateReservationRequest.java:15-16`
recebe `companyCode` e `HttpApi.java:128-130` o repassa ao `NewReservation`; nenhuma regra de plano entrou no handler
(a validação de campos obrigatórios em `HttpApi.java:116-126` não lista o `companyCode`, de propósito, porque ele é
opcional). As regras novas (`CorporatePlan.java`, `WeekendPackagePlan.java`, `LongStayPlan.java`, `StandardPlan.java`,
`NonRefundablePlan.java`, `RatePlanPolicy.java`) não importam Javalin nem JDBC: o `jdeps` continua mostrando JDBC em
`hotel.reservations` só por causa do `ReservationStore`, como na base. O acesso ao banco permaneceu isolado em
`ReservationStore.java` e `Rooms.java`, sem linha mudada. A montagem continua em `Hotel.java:20-29`; os planos são
instanciados nas constantes do `enum` (`RatePlan.java:8-17`), o que é razoável para objetos sem estado e é o ponto
único de seleção. O ciclo `documents` ↔ `reservations` (`ReservationService.java:3` usa `ReservationReceipt`, e
`ReservationReceipt.java:3` usa `Reservation`) já existia na base e o agente não o piorou. A única mudança de fluxo no
serviço foi mover o cálculo do preço para antes da conferência de disponibilidade (`ReservationService.java:42-46`),
para que uma violação do plano responda 400 antes de um 409; isso é testado em `ReservationServiceTest.java:127-132`.

## Design de baixo nível

**Números:** 5 planos, os 5 dentro da estrutura (uma classe cada: `StandardPlan`, `NonRefundablePlan`,
`CorporatePlan`, `WeekendPackagePlan`, `LongStayPlan`); 0 `switch`/`if` sobre o plano fora dela (o único `switch` que
restou em `src/main` é sobre `ErrorCode`, em `HttpApi.java:101-106`). Interface com 2 métodos e 2 helpers estáticos.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern pedido é o Strategy, e foi aplicado como a referência descreve. `RatePlanPolicy.java:7-17` define `price(room,
request, base)` e `refund(amountPaid)`; cada plano implementa os dois, inclusive os que já existiam: o normal
(`StandardPlan.java:9-16`, devolve a base e tudo o que foi pago) e o não reembolsável (`NonRefundablePlan.java:11-18`,
15% e devolução zero). A seleção é por campo do `enum`: cada constante recebe a sua política no construtor
(`RatePlan.java:8-23`) e o `enum` só repassa (`RatePlan.java:25-31`). Quem usa não nomeia plano: `PriceCalculator.java:25-26`
monta o preço base com a temporada e entrega ao plano; `ReservationService.java:82` pede a devolução ao plano. A
validação de cada plano está dentro do próprio plano, não no serviço: o código da empresa em `CorporatePlan.java:17-21`,
sexta a domingo com duas noites em `WeekendPackagePlan.java:21-27`, as 7 noites em `LongStayPlan.java:13-17`, todas
lançando `HotelException(INVALID_REQUEST)` como o pedido manda. O preço fechado por tipo de quarto fica num mapa
(`WeekendPackagePlan.java:12-15`) e ignora a temporada por construir um `Price` novo em vez de usar o subtotal da base
(`WeekendPackagePlan.java:28-29`). Um plano novo é uma classe e uma constante, exatamente o que o dono pediu ao avisar
que "o comercial vive inventando plano".

Não há exagero: a interface tem cinco implementações reais, não há fábrica nem builder, e o `enum` que repassa é o
próprio mecanismo de seleção que a referência aceita. Os helpers `discounted` e `percentOf` em
`RatePlanPolicy.java:19-26` são pequenos e usados por três planos.

O resto do código está bem: `BigDecimal` com `cents()` (half-even) em todo valor novo (`RatePlanPolicy.java:20`,
`WeekendPackagePlan.java:28`), `LocalDate`/`DayOfWeek` para as datas, mapas imutáveis para as tabelas de desconto e
de preço, classes `final` sem estado, mensagens de erro que dizem o motivo. O `NewReservation` ganhou o campo e manteve
o construtor antigo (`NewReservation.java:15-18`), o que poupou mudanças no `ReservationStore` e nos testes. Pontos
miúdos que um revisor apontaria sem barrar: `discounted` repete o `cents(multiply)` que `percentOf` já faz
(`RatePlanPolicy.java:20` e `:25`); `PRICE_BY_ROOM_TYPE.get(room.type())` em `WeekendPackagePlan.java:28` devolveria
`null` (e daria `NullPointerException` em `cents`) se um `RoomType` novo aparecesse sem preço, embora hoje os três
tipos estejam no mapa; e o campo `deluxe` do teste ficou no meio da classe (`PriceCalculatorTest.java:78`), com uma
linha em branco faltando entre dois testes do serviço (`ReservationServiceTest.java:132-133`).

## Testes

**Números:** 39 testes rodaram, 0 falharam, 0 pulados; 39 métodos de teste (a base tinha 26, todos mantidos; 13
novos). Cobertura: 94,3% de linhas (510 de 541) e 87,9% de ramos (109 de 124); em `hotel.reservations`, 247 de 260
linhas e 57 de 60 ramos (`measures/tests.md`; a base: 93,8% / 87,1%, `measures/tests-base.md`).
**Nota:** bom

Os testes da base foram preservados sem enfraquecer: a única alteração neles foi trocar `calculator.calculate(...)`
por um helper `price(...)` que monta o `NewReservation` (`PriceCalculatorTest.java:80-87`), e as asserções continuam
iguais. A parte nova está testada com os valores do pedido: Tecnosul em deluxe de 10 a 12/03 dá 779,80 / 93,58 /
686,22, e Agrovale e Farmavida têm o desconto conferido (140,36 e 194,95) em `PriceCalculatorTest.java:94-108`; o
código ausente, vazio, em minúsculas e desconhecido é recusado com `INVALID_REQUEST` (`:110-116`); outros planos
ignoram o código (`:118-123`); o pacote sai 820,00 / 1.200,00 / 520,00 com desconto 0,00 e o mesmo preço em janeiro
(`:126-139`); quinta a sábado, sexta a segunda, sexta a sábado e sexta ao domingo da outra semana são recusados
(`:141-153`); a estadia longa dá 1.749,30 / 174,93 / 1.574,37 (`:155-161`), mantém a temporada nas noites certas
(`:164-169`) e recusa 6 noites (`:172-175`). A devolução de cada plano novo é conferida no serviço, contra o banco:
204,92 no convênio, 410,00 no pacote, 1.574,37 na estadia longa (`ReservationServiceTest.java:110-125`), e a ordem
400-antes-de-409 em `:127-132`. Pela API, o convênio com código desconhecido e sem código responde 400
`INVALID_REQUEST` e com Farmavida responde 201 (`HttpApiTest.java:124-137`); o pacote fora de sexta a domingo responde
400 (`:145-150`).

Pergunta da mutação: trocar 0.12 por 0.21 em `CorporatePlan.java:11` derruba `:98`; trocar `<` por `<=` em
`LongStayPlan.java:13` derruba `:155` (7 noites deixaria de valer); apagar o `base.nights() != 2` em
`WeekendPackagePlan.java:23` derruba `:149` (sexta ao domingo da semana seguinte); trocar o 0.50 do pacote derruba
`:123`. Nada da regra nova fica sem rede. O que não foi coberto pela API é só a estadia longa (fica no nível de
unidade e de serviço), o que não pesa.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), 1 migração do Flyway, a `V1__create_tables.sql`
idêntica à da base; nenhuma conferência acusou algo: todo dinheiro em `numeric(10,2)`, nenhuma tabela sem chave,
nenhuma chave estrangeira sem índice, nenhuma data em texto (`measures/database.md`).
**Nota:** bom

O agente não mexeu no banco, e não precisava: `rate_plan` é `text` sem `CHECK` (`V1__create_tables.sql:28`), então as
três constantes novas são gravadas pelo `ReservationStore.add` (`ReservationStore.java:43`) e lidas por
`RatePlan.valueOf` (`ReservationStore.java:131`) sem migração; o roteiro confirmou que as reservas dos planos novos
continuam lá e com os valores certos depois de reiniciar. O que a base já tinha continua certo: `EXCLUDE USING gist`
contra a reserva dupla (`V1__create_tables.sql:38-40`), tratada pelo código `23P01` em `ReservationStore.java:55-58`,
SQL só com `?` (os `+` que o script listou concatenam constantes, `ReservationStore.java:18-19` e `:98`), conexões em
try-with-resources e pool Hikari (`Database.java:23-28`). A falta de `CHECK` nas colunas `status`, `rate_plan`, `type`
e `channel` veio da base. Uma observação, sem peso na nota porque o pedido não exigiu: o `companyCode` não é
persistido (a lista de colunas do `INSERT` em `ReservationStore.java:31-33` não o inclui, e `Reservation.java` não tem
o campo), então a reserva guarda o desconto da empresa mas não qual empresa foi; veja em Dúvidas.

## Comportamento

**Números:** 72 de 72 conferências passaram: a parte comum 46 de 46, os dados depois de reiniciar 3 de 3, o cenário 01
21 de 21, as duas reservas ao mesmo tempo 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Nada falhou. Os valores do financeiro conferem pela API: Tecnosul em deluxe de 10 a 12/03 dá R$ 779,80 de diárias,
R$ 93,58 de desconto e R$ 686,22 de total; Agrovale e Farmavida dão R$ 639,44 e R$ 584,85; o pacote deluxe de sexta
12/03 a domingo 14/03 sai R$ 820,00 e devolve R$ 410,00 depois de pago; o standard e a suíte saem R$ 520,00 e
R$ 1.200,00, e em julho o pacote continua R$ 820,00; a estadia longa de 01 a 08/03 dá R$ 1.749,30 / R$ 174,93 /
R$ 1.574,37 e devolve tudo. As recusas que o pedido descreve respondem 400 `INVALID_REQUEST`: convênio sem código ou
com código desconhecido, pacote de sábado a segunda ou de sexta a segunda, estadia longa com 6 noites. O
`companyCode` em outro plano é ignorado, e o normal e o não reembolsável funcionam como antes (37,48 de desconto,
devolução integral e zero). Para o dono da pousada: os três planos novos funcionam como foram combinados, nada do que
existia mudou, e um quarto continua não sendo reservado duas vezes para a mesma noite.

## Dúvidas

- O `companyCode` não é guardado na reserva (`ReservationStore.java:31-33`): a reserva fica com `rate_plan =
  CORPORATE` e o valor do desconto, mas sem a empresa. O pedido não disse para guardar; se o financeiro quiser depois
  saber quanto cada convênio consumiu, será preciso uma migração e uma coluna. Não pesou em nenhuma coluna.
- `CorporatePlan.java:17` compara o código exatamente como veio: `tecnosul` ou ` TECNOSUL` são recusados. O texto diz
  que o código é o nome em maiúsculas, então a escolha é defensável, mas o site pode querer normalizar antes de enviar.
