# Revisão: manutenção, cenário 10

## Visão geral

A base é um serviço Javalin em 7 pacotes: `hotel.web` (as rotas, `HttpApi`) recebe a requisição e chama as regras em
`hotel.reservations` (`ReservationService`, `PriceCalculator`), que guardam os dados no PostgreSQL por classes de
acesso (`ReservationStore`, `Rooms`, `SentMessages`), com o esquema criado por uma migração do Flyway. Os relatórios
moram em `hotel.reports`. O pedido era uma coluna `guest` no relatório de ocupação, logo depois de `reservation`, com
a vírgula do nome trocada por espaço. O agente fez exatamente isso: uma linha no cabeçalho e uma linha na montagem da
linha do CSV (`src/main/java/hotel/reports/OccupancyReport.java:34` e `:40`), os dois testes de ocupação ajustados, um
teste novo para a vírgula e uma frase no README. Não criou estrutura nenhuma para o "telefone, talvez um dia". Não há
nada sério: os 27 testes passam e as 55 conferências do roteiro também. O único ciclo entre pacotes já vinha da base.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais na base e na final. Um ciclo (`hotel.documents` ↔
`hotel.reservations`), que já existia na base. A única diferença em `measures/dependencies.md` é `hotel.reports`, que
passou de 95 para 96 linhas.
**Nota:** bom

A mudança fica toda no lugar onde a coisa já mora (`src/main/java/hotel/reports/OccupancyReport.java:34,40`), e o
relatório já recebia a `Reservation`, que tem o `Guest`. Por isso não surgiu nenhuma dependência nova. A borda HTTP
continua fina: a rota só lê a data e escreve o CSV (`src/main/java/hotel/web/HttpApi.java:83-85`). O ciclo vem da
base: `src/main/java/hotel/reservations/ReservationService.java:3` importa `ReservationReceipt`, e
`src/main/java/hotel/documents/ReservationReceipt.java:3` importa `Reservation`. O agente não o criou nem o piorou.

## Design de baixo nível

**Números:** 1 classe de produção mudada (`OccupancyReport`), com 1 linha a mais e 1 linha alterada. Nenhuma classe,
interface ou tipo novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É um controle negativo, e a frase que tenta é "talvez um dia a recepção peça o telefone do hóspede também". A resposta
esperada pelos critérios é "uma coluna a mais no `OccupancyReport`, e a troca da vírgula por espaço", e foi isso o que
o agente fez. O cabeçalho ganhou `guest` depois de `reservation` (`OccupancyReport.java:34`), e a linha ganhou
`reservation.guest().name().replace(',', ' ')` na mesma cadeia de `append` que já existia (`OccupancyReport.java:40`).
Não há esqueleto de relatórios, colunas configuráveis nem campo de telefone, e o `RevenueReport` ficou intocado.

Quanto ao resto do código (da base): o dinheiro é `BigDecimal` com arredondamento HALF_EVEN num lugar só
(`src/main/java/hotel/reservations/PriceCalculator.java:50-52`), as datas são `LocalDate`, e os planos e as situações
são `enum` com `switch` exaustivo (`ReservationService.java:82-85`). Os erros têm código próprio (`HotelException`
com `ErrorCode`), e o nome do hóspede chega validado como obrigatório (`HttpApi.java:116-125`), então o `replace` não
corre risco de `null`. Não há nada que um revisor pediria para mudar.

## Testes

**Números:** 27 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 27 métodos escritos (a base tinha 26, todos
mantidos). Cobertura de linhas de 93,8% (472 de 503) e de ramos de 87,1% (101 de 116). `hotel.reports` tem 49 de 49
linhas e 22 de 22 ramos cobertos.
**Nota:** bom

O agente atualizou, com valores exatos, os dois testes da base que conferem o CSV inteiro com `assertEquals`, incluindo
a coluna nova (`src/test/java/hotel/reports/ReportsTest.java:61-67` e `:85-90`), e não enfraqueceu nenhum. Ele também
acrescentou `commasInTheGuestNameBecomeSpaces` (`ReportsTest.java:69-79`), com o nome "Lima, Bruno" e a conferência
de `",Lima  Bruno,"`. A pergunta da mutação:

- tirar o `replace` deixaria `",Lima, Bruno,"` no CSV, e o teste novo falharia;
- trocar a vírgula por nada também o faria falhar;
- tirar a coluna, ou pô-la em outra posição, faria falhar os dois `assertEquals`.

Há um detalhe pequeno: todas as reservas da preparação têm o mesmo hóspede, "Bruno Lima" (`ReportsTest.java:32`).
Assim, um erro que pusesse o nome de outra reserva na linha passaria sem ser pego. Isso não chega a pedir mudança.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. A chave
estrangeira tem índice e há um `EXCLUDE` contra duas reservas no mesmo quarto. A única migração
(`V1__create_tables.sql`) está igual à da base. Das conferências, só as colunas de situação e de tipo sem CHECK
(`status`, `rate_plan`, `type`, `channel`) acusaram algo.
**Nota:** bom

O pedido não mexe no banco, e o agente não mexeu: o nome já estava em `reservations.guest_name` e já era lido em
`src/main/java/hotel/reservations/ReservationStore.java:124`. A falta de CHECK nas colunas de texto de situação e de
tipo vem da base. Também vem da base o relatório carregar todas as reservas (`ReservationStore.all()`, usada em
`OccupancyReport.java:25`) para filtrar em Java. Nenhum desses pontos é do agente.

## Comportamento

**Números:** 55 de 55 conferências passaram: 46 de 46 na parte comum, 3 de 3 depois de reiniciar, 4 de 4 no cenário 10
e 2 de 2 nas reservas simultâneas.
**Nota:** bom

O relatório de ocupação traz a coluna `guest` logo depois de `reservation`, e a vírgula do nome vira espaço. O total e
o relatório financeiro continuam iguais. Todo o resto do sistema segue funcionando, inclusive depois de reiniciar e
com dois pedidos ao mesmo tempo. Para o dono da pousada, a recepção já vê o nome do hóspede, e nada do que funcionava
deixou de funcionar.

## Dúvidas

O texto só pede tratar a vírgula, e o agente fez só isso. Um nome com aspas ou com quebra de linha (a API aceita
qualquer texto não vazio, `HttpApi.java:116-125`) ainda bagunçaria o CSV. Também "Lima, Bruno" vira "Lima  Bruno",
com dois espaços, que é a leitura literal do pedido. Nenhum dos dois pontos pesou na nota.
