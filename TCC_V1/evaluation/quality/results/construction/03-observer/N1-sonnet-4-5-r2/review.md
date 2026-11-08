# Revisão: construção, cenário 03

## Visão geral

O agente montou um projeto Maven com Javalin em 9 pacotes: `infrastructure.http` (um controller e os DTOs),
`application.service` (`ReservationService`, `ReportService` e as exceções), `application.notification` (os ouvintes
dos avisos), `domain.model`, `domain.event` (o publicador de eventos), `domain.repository` (as interfaces) e
`infrastructure.persistence` (JDBC puro com HikariCP). Uma requisição passa pelo `ReservationController`, que lê o
corpo, chama o `ReservationService`, e este usa o modelo `Reservation` (preço, situação, devolução) e o repositório
JDBC. Na confirmação e no cancelamento, o serviço publica um evento que cinco ouvintes registrados no `Main` recebem.
As reservas e as mensagens ficam no PostgreSQL, em duas tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código. Os
quartos ficam numa lista fixa em memória. A parte que o texto pede (os avisos) ficou bem feita: um Observer de
verdade, e as 15 conferências do cenário passaram. O mais sério é que nada protege contra a dupla reserva: o serviço
consulta se há conflito e depois insere, cada passo numa conexão própria, sem transação nem restrição no banco. Com
dois pedidos simultâneos, 9 de 10 rodadas criaram as duas reservas. Os testes cobrem só o modelo (15,1% das linhas),
e o teste de integração foi desligado, renomeado para `.skip`.

## Arquitetura

**Números:** 9 pacotes, 21 dependências entre eles, nenhum ciclo. Javalin só em `infrastructure.http` (e no `Main`);
JDBC só em `infrastructure.persistence` (e no `Main`). A maior classe tem 202 linhas
(`PostgresReservationRepository`).
**Nota:** com problemas

As dependências vão no sentido certo, da borda para as regras e das regras para os dados. As regras não conhecem nem
o Javalin nem o JDBC (`application.service` e `application.notification` não usam nenhuma das duas bibliotecas,
segundo `measures/dependencies.md`). A montagem está toda num lugar só, em `Main.java:29-67`: o pool, os repositórios,
o publicador, o registro dos ouvintes, os serviços e o controller. O acesso ao banco está isolado em
`infrastructure/persistence/`. As misturas são poucas e localizadas:

- uma regra de negócio mora no handler: a segunda via "só existe para reserva paga" é decidida em
  `infrastructure/http/ReservationController.java:133`, e não no serviço;
- o controller vai direto aos repositórios, sem passar pelo serviço: `RoomRepository` em
  `ReservationController.java:60` e `MessageRepository` em `ReservationController.java:142`. Além disso, todos os
  handlers buscam o quarto de novo só para montar a resposta (`ReservationController.java:87, 97, 104, 111, 118, 125,
  137`);
- os valores (subtotal, desconto, total) são recalculados nos DTOs a cada resposta
  (`infrastructure/http/dto/ReservationResponse.java:27-29`, `ReceiptResponse.java:23-25`) em vez de virem do
  serviço ou de ficar guardados com a reserva.

Nada disso chega a misturar camadas por inteiro, mas a regra da segunda via e o acesso direto aos repositórios a
partir da borda são o que alguém pediria para mudar.

## Design de baixo nível

**Números:** 5 interessados (hóspede, governança e financeiro na confirmação; governança e financeiro no
cancelamento), os 5 como ouvintes separados registrados no `Main`; nenhum aviso montado ou enviado fora da estrutura.
O serviço só publica 2 eventos (`ReservationService.java:61` e `:101`).
**Classe:** acerto
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** `domain/event/EventPublisher.java:8-25` guarda uma lista de ouvintes por tipo de evento e percorre essa
lista no `publish`. `EventListener<T>` (`domain/event/EventListener.java:3-5`) é a interface do ouvinte. O
`ReservationService` recebe o publicador de fora (`ReservationService.java:17-23`) e não conhece nenhum destinatário:
na confirmação ele publica `ReservationConfirmedEvent` (`:61`) e no cancelamento `ReservationCancelledEvent` com o
indicador `wasConfirmed` (`:96`, `:101`). Cada interessado é uma classe: `GuestNotificationListener` (o e-mail do
hóspede também é um ouvinte), `HousekeepingNotificationListener`, `FinanceNotificationListener`,
`HousekeepingCancellationListener` e `FinanceCancellationListener`. Todas estão registradas em `Main.java:39-48`. As
regras de "só avisa se estava confirmada" e "o financeiro só é avisado quando há o que devolver" ficam dentro de cada
ouvinte (`HousekeepingCancellationListener.java:20`, `FinanceCancellationListener.java:21-30`). Para o restaurante ou
o transfer, basta acrescentar uma classe e uma linha no `Main`. Isso é o acerto que a referência descreve. Um detalhe
que afasta um pouco: o publicador é chamado dentro do `try` que traduz `IllegalStateException` em
`OPERATION_NOT_ALLOWED` (`ReservationService.java:58-65`, `:98-105`). Se um ouvinte falhar, a reserva já foi gravada,
os avisos ficam pela metade e o cliente recebe um erro de uma operação que na verdade aconteceu.

**O exagero.** Há estrutura que nada usa:

- três interfaces de repositório com uma implementação cada (`domain/repository/RoomRepository.java`,
  `ReservationRepository.java`, `MessageRepository.java`), que nenhum teste nem outra implementação substitui;
- `NotificationService` (`application/notification/NotificationService.java:13-16`), uma camada que só repassa:
  monta a `Message` e chama o `save`;
- `DomainEvent` (`domain/event/DomainEvent.java:3-4`), uma interface vazia. Esta pesa menos, porque faz parte do
  pattern.

O exagero é pequeno, mas existe.

**O resto do código.** Os tipos estão certos na maior parte: `BigDecimal` com `HALF_EVEN` a cada passo
(`domain/model/Reservation.java:109-110, 116, 121-122, 130`), `LocalDate`, e `enum` para a situação e o plano. Os
defeitos:

- **Duplicação.** O `formatMoney` com `new Locale("pt","BR")` está copiado em três ouvintes
  (`FinanceCancellationListener.java:41-43`, `FinanceNotificationListener.java:31-33`,
  `GuestNotificationListener.java:74-76`). O `DATE_FORMATTER` está repetido em três
  (`GuestNotificationListener.java:15`, `HousekeepingCancellationListener.java:12`,
  `HousekeepingNotificationListener.java:14`). Os endereços da governança e do financeiro aparecem duas vezes cada.
  O trio subtotal/desconto/total é recalculado em três lugares (`ReservationResponse.java:27-29`,
  `ReceiptResponse.java:23-25`, `GuestNotificationListener.java:55-57`), e `calculateTotal` calcula o subtotal de novo
  (`Reservation.java:127-128`).
- **Dinheiro sem as 2 casas.** O zero sai sem casas: `BigDecimal.ZERO` em `Reservation.java:43` (o `amountPaid` da
  reserva nova) e em `Reservation.java:124` (o desconto do plano normal). No JSON eles aparecem como `0`, não `0.00`.
- **Os erros.** O `catch (IllegalArgumentException)` em `ReservationController.java:89` transforma em 400 qualquer
  exceção desse tipo que venha do serviço. O `Long.parseLong` do id não é tratado (`ReservationController.java:95`,
  `102`, ...), e `/reservations/abc` responde 500. O JSON inválido também cai em 500.
- **Miudezas.** `cancel()` devolve um valor que ninguém usa (`Reservation.java:174-188`). Há um import não usado em
  `HousekeepingNotificationListener.java:4`. O construtor `new Locale(...)` está obsoleto no Java 21.

São defeitos localizados, que não impedem mudar o código com segurança.

## Testes

**Números:** 14 testes rodaram, 0 falhas, 0 erros, 0 pulados; 14 métodos escritos (11 em `ReservationTest`, 3 em
`EventPublisherTest`). Cobertura de linhas: 103 de 680 (15,1%). Cobertura de ramos: 27 de 116 (23,3%). Cobertura 0
em `application.notification` (0/98), `application.service` (0/103), `infrastructure.http` (0/115) e
`infrastructure.persistence` (0/143). Nenhum teste pela API nem contra o banco.
**Nota:** com problemas

O que está testado, com valores, é só o modelo:

- o preço na baixa temporada (499,80, `src/test/java/com/pousadamarealta/ReservationTest.java:22`);
- julho (599,76, `:37`);
- o plano não reembolsável, com desconto de 116,97 e total de 662,83 (`:72-74`);
- a devolução do plano normal (`:107-108`) e a do não reembolsável (`:124`);
- as transições proibidas (`:155-156`, `:167`, `:181`).

O teste de dezembro calcula o esperado com a mesma fórmula do código (`ReservationTest.java:51-56`), e por isso não
confere nada que o código não diga. O 15 de dezembro e o fim de fevereiro, onde a temporada começa e termina, não têm
teste.

A parte nova do cenário está praticamente sem rede. `EventPublisherTest.java:14-84` testa o mecanismo do publicador
com lambdas, mas nenhum ouvinte de verdade é exercitado: a cobertura de `application.notification` é zero. Pela
pergunta da mutação:

- se alguém apagasse o `if (!event.wasConfirmed())` de `HousekeepingCancellationListener.java:20`, nenhum teste
  falharia;
- o mesmo vale para a checagem de devolução zero em `FinanceCancellationListener.java:28`;
- e também para uma troca do endereço `governanca@...` ou do assunto "Pagamento recebido - reserva N".

As recusas da API (os códigos de erro), os relatórios e o SQL também não têm teste. O agente escreveu um teste de
integração com Testcontainers, mas desligou o arquivo, renomeando-o para `src/test/java/com/pousadamarealta/IntegrationTest.java.skip`.
Os testes que existem passam e conferem valores, mas cobrem pouco do sistema e quase nada da parte nova.

## Banco

**Números:** 2 tabelas (`reservations`, `messages`), as duas com chave primária. O dinheiro está em `numeric(10,2)`
(`amount_paid`, `refund`) e as datas em `date`. 0 chaves estrangeiras, 0 CHECK, 0 UNIQUE/EXCLUDE, 0 transações
explícitas, 0 `FOR UPDATE`, nenhum SQL montado com `+`. O esquema é criado por `CREATE TABLE` no código, sem migração.
Pool HikariCP.
**Nota:** ruim

O que está certo: os tipos (`infrastructure/persistence/DatabaseInitializer.java:14-27`), o SQL todo com parâmetros
(`PostgresReservationRepository.java:31-36, 65-69, 107-113`) e as conexões devolvidas com try-with-resources a partir
de um pool (`Main.java:81-87`).

O defeito sério é a dupla reserva. A regra "um quarto não pode ter duas reservas na mesma noite" não tem proteção
nenhuma, nem no banco nem no processo. `ReservationService.java:42-49` consulta os conflitos
(`PostgresReservationRepository.java:106-131`) e depois insere (`:30-62`), cada passo numa conexão própria, sem
transação, sem trava e sem uma restrição `EXCLUDE`. O roteiro do comportamento mediu o efeito: em 9 de 10 rodadas de
dois pedidos simultâneos, as duas reservas foram criadas. Para a pousada, isso quer dizer dois hóspedes pagos no
mesmo quarto, na mesma noite.

Os outros defeitos:

- **Esquema sem migração.** O esquema nasce de `CREATE TABLE IF NOT EXISTS` (`DatabaseInitializer.java:13-38`), que
  não sabe acrescentar uma coluna numa tabela que já existe.
- **Sem tabela de quartos.** Os quartos não estão no banco (`InMemoryRoomRepository.java:10-16`), e por isso a
  reserva não tem chave estrangeira para o quarto. Ela guarda o número como texto solto (`room_number VARCHAR(10)`,
  `DatabaseInitializer.java:16`).
- **Escritas que deviam andar juntas.** A confirmação grava a reserva e depois cada mensagem, cada uma em sua conexão
  (`PostgresMessageRepository.java:26`). Uma falha no meio deixa a reserva confirmada sem os avisos.
- **Sem CHECK.** As colunas `status`, `rate_plan` e `channel` não têm CHECK.
- **Consulta sem índice.** O financeiro filtra com `EXTRACT(YEAR/MONTH FROM check_in)`
  (`PostgresReservationRepository.java:164-165`), o que impede o uso de um índice em `check_in` (e também não há
  índice nenhum além das chaves primárias).

## Comportamento

**Números:** 61 de 66 conferências passaram:

- a parte comum: 42 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 03: 15 de 15;
- duas reservas ao mesmo tempo: 1 de 2.

**Nota:** com problemas

O cenário passou inteiro: o hóspede continua recebendo o e-mail com o comprovante, a governança e o financeiro
recebem os avisos da confirmação e do cancelamento com as datas, os hóspedes e os valores, o financeiro não é avisado
quando não há o que devolver, e o cancelamento de uma reserva pendente não avisa ninguém. A parte comum quase toda
funciona: os preços com temporada e arredondamento, as recusas, as transições, as devoluções e os dois relatórios. Os
dados sobrevivem ao reinício.

As falhas:

- **"o 201 comporta 3" e "a diária do 201 é 389.90".** Chamei `GET /rooms` à mão e os valores estão certos
  (`"capacity":3,"nightlyRate":389.90`). O roteiro procura `number` como número, e o projeto devolve o número do
  quarto como texto (`"201"`). O texto não fixa esse tipo, e por isso estas duas falhas não pesam.
- **O dinheiro no JSON com 2 casas.** A reserva recém-criada devolve `"amountPaid":0` e, no plano normal,
  `"discount":0` (veio do `BigDecimal.ZERO` sem escala, `Reservation.java:43, 124`). O texto pede número com 2 casas.
  É um erro pequeno, mas real.
- **Corpo que não é JSON.** Veio 500 em vez de 400 `INVALID_REQUEST`. O log mostra a `JsonParseException` sem
  tratamento. Também fora do roteiro, `GET /reservations/abc` responde 500 (`NumberFormatException`). O site recebe
  um erro genérico onde o combinado era um código.
- **Duas reservas ao mesmo tempo.** Em 9 de 10 rodadas, as duas foram criadas. O defeito é do banco e já pesou
  naquela coluna. Aqui, o que ele quer dizer para o dono é que um quarto pode ser vendido duas vezes se dois hóspedes
  reservarem no mesmo instante.

## Dúvidas

- **A nota do banco.** A skill põe "a duplicação protegida só dentro do processo" em `com problemas`. Aqui não há
  proteção nenhuma, e a duplicação foi medida (9 de 10). Somando a falta de migração, de chave estrangeira e de
  transação, dei `ruim`. Um leitor mais brando daria `com problemas`.
- **A nota dos testes.** Ficou no limite entre `com problemas` e `ruim`. Os 14 testes passam e conferem valores do
  modelo, mas a parte nova do cenário (os ouvintes) tem cobertura zero, e o teste de integração está desligado.
- **O exagero.** Ficou no limite: três interfaces de repositório que nada substitui e um `NotificationService` que só
  repassa. A estrutura do Observer em si não é exagero.
