# Revisão: construção, cenário 03

## Visão geral

O agente montou um projeto Maven com pacotes por camada: `domain` (reserva, quarto, preço, `Money`, eventos e
interfaces de repositório), `application.reservation` (o `ReservationService`), `infrastructure.web` (Javalin, handlers
e DTOs), `infrastructure.persistence.postgres` (JDBC com HikariCP), `infrastructure.notification` (os três ouvintes) e
`infrastructure.database` (pool e Flyway). Uma requisição entra num handler (`ReservationsHandler`), que lê o corpo com
Gson e chama o `ReservationService`. O serviço aplica as regras, grava pelo repositório JDBC e, na confirmação e no
cancelamento, passa um evento a uma lista de `NotificationSubscriber`. Cada mensagem devolvida é gravada na tabela
`notifications`. O esquema sai de quatro migrações do Flyway. O defeito mais sério é que **o sistema não sobe**: o
`pom.xml` traz o `flyway-core` 10.8.1 sem o módulo `flyway-database-postgresql`, e a migração falha com
`FlywayException: Unsupported Database: PostgreSQL 18.6` (`DatabaseSetup.java:44`, chamado de `Main.java:20`).
Nenhuma conferência de comportamento rodou. Mesmo com o banco resolvido, o sistema continuaria quebrado. Testei fora
do projeto, com o jar gerado. O Gson não lê `LocalDate` no Java 21 (`JsonIOException`), então todo `POST /reservations`
cairia em 500 (`ReservationsHandler.java:33`, `:49`). O Jackson do jar não tem o módulo jsr310, então a reserva também
não sairia em JSON. E o desconto do NON_REFUNDABLE está invertido: R$ 499,80 dá desconto de R$ 424,83 e total de
R$ 74,97 (`Money.java:46-48`, `RateCalculator.java:33`). Não há nenhum teste.

## Arquitetura

**Números:** 14 pacotes, 48 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece em `infrastructure.web` e `infrastructure.web.handler`, e o JDBC só em `infrastructure.persistence.postgres`, `infrastructure.database` e `Main`. A maior classe é `PostgresReservationRepository` (291 linhas).
**Nota:** com problemas

As camadas existem e apontam para o lado certo. As regras de preço e de situação (`Reservation.java:119-167`,
`RateCalculator.java`) não conhecem Javalin nem SQL. O serviço não recebe `Context` e não monta SQL. O acesso ao banco
fica isolado nos três repositórios. Há misturas localizadas:

- **A montagem está dividida.** O `Main` cria o pool e os repositórios (`Main.java:19-28`), mas é a classe da borda
  HTTP que cria os ouvintes e o `ReservationService` (`GuesthouseApi.java:41-52`). Quem quiser acrescentar o
  restaurante mexe numa classe web.
- **Regra de negócio no handler.** O relatório financeiro calcula ali mesmo o dinheiro que fica com a pousada
  (`ReportsHandler.java:116-118`), soma em `double` (`ReportsHandler.java:102`) e monta os dois CSV. O de ocupação
  também conta os quartos no handler (`ReportsHandler.java:62-66`). Não existe serviço de relatório.
- **O repositório vaza para a borda.** O serviço expõe o repositório (`ReservationService.java:47`) e os handlers o
  usam direto (`ReservationsHandler.java:59`, `:163`). A regra "segunda via só para reserva paga" ficou sem lugar e
  não existe.
- **Os erros são decididos pelo texto da mensagem.** O handler traduz exceções genéricas comparando o texto
  (`ReservationsHandler.java:182-199`). Basta trocar a frase no serviço para o código HTTP mudar.

## Design de baixo nível

**Números:** 3 interessados, todos na estrutura: `GuestNotifier`, `GovernanceNotifier` e `FinanceNotifier`
implementam `NotificationSubscriber`. Nenhum e-mail é montado fora dela. Os 2 eventos (confirmação e cancelamento)
passam pelo mesmo `dispatchEvent`.
**Classe:** acerto
**Exagero:** exagero
**Nota:** ruim

**O pattern.** É um Observer direito. Há uma interface de ouvinte (`NotificationSubscriber.java:3-6`) e uma
implementação por interessado, inclusive o hóspede. O serviço recebe a lista de fora, no construtor
(`ReservationService.java:34-45`), e a percorre sem saber quem é avisado (`ReservationService.java:169-180`). Os
ouvintes são registrados na montagem (`GuesthouseApi.java:41-45`). O cancelamento de uma reserva pendente não
publica evento (`ReservationService.java:160`). Cada ouvinte decide sozinho o que faz, e o financeiro omite o reembolso
zero (`FinanceNotifier.java:44-46`). Acrescentar o restaurante é uma classe nova e uma linha na lista.

Pesa contra, sem mudar a classe:

- cada ouvinte separa os eventos com `instanceof` (`FinanceNotifier.java:20-24`, igual nos outros dois);
- o ouvinte devolve uma única `PersistedNotification` para o serviço gravar, em vez de mandar a mensagem. Um ouvinte
  que precise de duas mensagens, ou de outro canal, não cabe no contrato;
- a montagem fica na classe web, como dito na arquitetura.

**O exagero.** Há estrutura que nada usa:

- `DomainEvent.occurredAt` nunca é lido (`DomainEvent.java:6-13`);
- `PersistedNotification.failure` e `noop` nunca são chamados (`PersistedNotification.java:59`, `:80`), e por isso as
  colunas `success` e `error_message` são sempre `true` e `null`;
- os métodos `findByDateRange`, `findByReservationId` e `RoomRepository.save` não têm quem os chame
  (`ReservationRepository.java:15`, `NotificationRepository.java:9`);
- as interfaces de repositório têm uma implementação só, e nenhum teste as substitui;
- os enums carregam um `displayName` igual ao `name()` (`ReservationStatus.java:4-18`, `RatePlan.java`, `RoomType.java`).

**O resto do código.** Os defeitos estão espalhados:

- **Desconto invertido:** `Money.discount` devolve o valor já com o desconto, não o desconto (`Money.java:46-48`), e
  `RateCalculator.java:33` o usa como desconto.
- **Alta temporada errada:** só vai até o dia 28 em janeiro e fevereiro (`Season.java:32`). De 29 a 31 de janeiro e o
  29 de fevereiro ficam fora.
- **Dinheiro em `double`** nos DTOs (`ReservationDTO.java:17-22`, `ReceiptDTO.java:19-21`), no relatório
  (`ReportsHandler.java:116`) e na confirmação (`ReservationsHandler.java:85`).
- **Valor formatado errado:** `String.format("R$ %.2f")` não põe separador de milhar e depende do locale
  (`GuestNotifier.java:102`, `FinanceNotifier.java:63`, `Money.java:65`). O texto pede R$ 1.234,56; aqui sai
  "R$ 1234.56".
- **Erros:** `catch (Exception)` que devolve `e.getMessage()` ao cliente (`ReservationsHandler.java:49-52`, repetido em
  cada rota), e erros de dados lançados como `IllegalArgumentException`.
- **Duplicação:** a checagem de situação existe no serviço (`ReservationService.java:100`) e de novo na entidade
  (`Reservation.java:120`).
- **Estado reconstruído à mão:** depois de recarregada, a reserva cancelada perde o valor pago e o reembolso
  (`PostgresReservationRepository.java:280-287`, com o comentário "hack").
- **Código confuso e inútil:** `DateRange.overlaps` tem dupla negação e não é usado (`DateRange.java:32-34`).
- **Campos públicos mutáveis:** no pedido e nos DTOs (`CreateReservationRequest.java`).

## Testes

**Números:** 0 testes rodaram, 0 falharam, 0 métodos de teste escritos, nenhum arquivo em `src/test`. Não há
cobertura: o JaCoCo não gerou relatório porque nenhum teste rodou (`measures/tests.md`). O `mvn verify` termina em
BUILD SUCCESS só porque não há o que testar.
**Nota:** ruim

O `pom.xml` declara JUnit 5 e Mockito, mas o agente não escreveu nenhum teste. Nada confere o preço com a temporada,
os planos, as devoluções, os códigos de erro nem os avisos da governança e do financeiro. Qualquer mutação passaria
despercebida, e isso já aconteceu: o desconto invertido (`Money.java:46-48`) e o corte no dia 28 (`Season.java:32`)
seriam pegos pelo primeiro teste com o exemplo do próprio texto. Um teste que só subisse o serviço teria mostrado que
ele não sobe.

## Banco

**Números:** 0 tabelas criadas. O serviço não subiu contra o banco vazio (`FlywayException: Unsupported Database:
PostgreSQL 18.6`), e as conferências não têm o que acusar (`measures/database.md`). Pela leitura, as migrações criam 3
tabelas (`rooms`, `reservations`, `notifications`), com chave primária, chaves estrangeiras indexadas e nenhum
CHECK, UNIQUE ou EXCLUDE.
**Nota:** ruim

O esquema não chega a existir. O `pom.xml` usa Flyway 10 sem a dependência `flyway-database-postgresql`, que desde o
Flyway 10 é necessária para o PostgreSQL. A migração quebra em `DatabaseSetup.java:44` e o processo termina
(`Main.java:36`). O desenho das migrações é razoável: versionadas, com dinheiro em centavos `BIGINT` (inteiro, não
ponto flutuante), datas em `DATE`, chaves estrangeiras com índice (`V002__create_reservations_table.sql:3`, `:21`;
`V003`). Mesmo assim, se o banco subisse:

- **Reserva dupla sem proteção.** Nada no banco impede duas reservas na mesma noite: nenhuma restrição de exclusão,
  nenhuma transação, nenhum `FOR UPDATE`. A checagem é um `SELECT` seguido de um `INSERT` separado
  (`OccupancyCalculator.java:24`, `ReservationService.java:66` e `:90`).
- **Situação e plano sem CHECK.** `status` e `rate_plan` são `VARCHAR` livres.
- **Confirmação e avisos não andam juntos.** A reserva é gravada e as mensagens vão depois, cada uma na sua conexão,
  sem transação (`ReservationService.java:106-110`). Se um ouvinte falha, o erro só vai para o log
  (`ReservationService.java:176-178`).
- **Dado que existe no banco e some na leitura.** O mapeamento devolve a reserva cancelada com valor pago e reembolso
  zero (`PostgresReservationRepository.java:280-287`).
- **Coluna gravada e ignorada.** `rooms.capacity` é gravada, mas a capacidade vem do enum (`Room.java`, `capacity()`).

## Comportamento

**Números:** 0 conferências passaram e 0 falharam. O roteiro não pôde testar nada porque o serviço não subiu contra o
banco vazio (`measures/behavior.md`; a pilha termina em `DatabaseSetup.java:44` / `Main.java:20`).
**Nota:** ruim

Para o dono da pousada, o sistema entregue não funciona: o `java -jar` termina logo ao subir, e o site não consegue
listar quartos nem criar reservas. Fora do roteiro, rodei a lógica com o jar gerado e a leitura confirma que haveria
outras quebras mesmo com o banco. Elas pesam nas suas colunas e não mudam a nota:

- **Criar reserva.** O Gson não lê `LocalDate` no Java 21 (`JsonIOException`), então todo `POST /reservations` daria
  500 `INTERNAL_ERROR` (`ReservationsHandler.java:33`).
- **Devolver a reserva em JSON.** O Jackson do jar não tem o módulo jsr310, então a serialização falha.
- **Confirmar.** A confirmação exige um corpo com `amount`, que o texto não define (`ReservationsHandler.java:84-91`),
  e grava como pago o que vier, não o total.
- **Preço.** O NON_REFUNDABLE sai com o desconto e o total trocados: R$ 499,80 dá total de R$ 74,97. De 29 a 31 de
  janeiro a diária não tem os 20%.
- **Valores nos e-mails** saem como "R$ 1234.56" em vez de "R$ 1.234,56".
- **Mensagem a mais:** o hóspede recebe um e-mail de cancelamento que não foi pedido (`GuestNotifier.java:46-60`).
- **Segunda via:** sai também para reserva pendente (`ReservationsHandler.java:163-171`).
- **Relatório financeiro:** deixa de fora o NON_REFUNDABLE cancelado, que o texto manda contar
  (`PostgresReservationRepository.java:217`).
- **Erros dos relatórios:** saem como texto solto, não como `{ "error": "INVALID_REQUEST" }`
  (`ReportsHandler.java:36`, `:73`, `:82`, `:111`).

## Dúvidas

Não sei se o `Unsupported Database` vem só da falta do módulo `flyway-database-postgresql` ou também da versão
10.8.1 não reconhecer o PostgreSQL 18. As duas causas estão no `pom.xml`, e o resultado é o mesmo. As quebras citadas
fora do roteiro (Gson e Jackson com `LocalDate`, preço, formato do dinheiro) foram conferidas rodando as classes do jar
fora do projeto, não pela API, porque o serviço não sobe.
