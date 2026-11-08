# Revisão: construção, cenário 03

## Visão geral

O agente montou um serviço Javalin em camadas: `api` (quatro controllers e os DTOs em `api/dto`), `domain.service`
(as regras: `ReservationService`, `PriceCalculator`, `ReportService`), `domain.event` (um `EventBus` e os eventos de
confirmação e de cancelamento), `domain.event.handlers` (um ouvinte por interessado: hóspede, governança, financeiro),
`repository` (JDBC puro) e `config.Database` (pool Hikari e `CREATE TABLE IF NOT EXISTS`). Uma requisição vai da rota
(`Main.java:79-89`) ao controller, que chama o serviço, que lê e grava pelo repositório e, na confirmação e no
cancelamento, publica um evento que os ouvintes transformam em linhas da tabela `messages`. Os dados ficam em três
tabelas no PostgreSQL. A parte do cenário (o Observer) está bem feita. Os problemas sérios estão no banco e no
comportamento. Todas as requisições usam **uma única `Connection` JDBC** (`Main.java:46-49`), sem transação nenhuma
e sem restrição contra reserva dupla: numa rodada do roteiro com CPF só em dígitos, **9 de 10 pares de pedidos
simultâneos criaram as duas reservas** para o mesmo quarto e as mesmas noites. Além disso, `guest_cpf` é
`VARCHAR(11)` (`config/Database.java:45`) e nada valida o CPF antes de gravar. Um CPF escrito com pontos e traço,
como `111.222.333-44` (o mesmo formato do comprovante do enunciado), derruba a criação da reserva com 500. É isso que
faz o roteiro oficial passar só em 16 das 66 conferências.

## Arquitetura

**Números:** 10 pacotes, 22 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece
em `api` e no pacote raiz; o JDBC aparece em `repository`, `config`, no pacote raiz e em `domain.service` (que
captura `SQLException`). As maiores classes têm 188 linhas (`ReservationRepository`) e 176 (`ReservationService`).
**Nota:** com problemas

A direção geral está certa. Os controllers leem a requisição, chamam o serviço e escrevem a resposta
(`api/ReservationController.java:22-68`). As regras de preço e de situação ficam em `domain/service`, sem Javalin e
sem SQL (`domain/service/ReservationService.java:27-169`, `domain/service/PriceCalculator.java`). O SQL fica nos
repositórios, e a montagem está num lugar só (`Main.java:36-89`). Não há ciclo. O que afasta do `bom`:

- **Uma regra na borda HTTP:** a regra "segunda via só para reserva paga" está no handler
  (`api/ReservationController.java:143-147`), e não no serviço.
- **O catálogo dos quartos como `static` global da camada de dados:** `RoomRepository.getRoomType`
  (`repository/RoomRepository.java:15-21, 82-84`) é chamado pelo DTO (`api/dto/DocumentResponse.java:4, 30`), pelo
  formatador do domínio (`domain/util/DocumentFormatter.java:5, 17`) e pelo serviço de relatórios
  (`domain/service/ReportService.java:29`). Isso cria as dependências `api.dto -> repository` e
  `domain.util -> repository` que o jdeps mostra: o DTO e um utilitário do domínio dependem da camada de dados.
- **A tradução de exceção em resposta HTTP copiada em cada handler** (`api/ReservationController.java:59-67, 75-79,
  87-93, 101-107, 115-121, 129-135, 150-154`), em vez de um `app.exception(...)` central. Por causa disso, o que
  escapa vira 500 (veja o comportamento).
- **A montagem repetida no teste:** `ReservationSystemIT.java:44-104` copia `Main.java:36-89` linha a linha. Por
  isso `Main` tem 0 de 53 linhas cobertas.

## Design de baixo nível

**Números:** os 5 avisos pedidos (o hóspede, a governança e o financeiro na confirmação; a governança e o financeiro
no cancelamento) estão todos na estrutura de ouvintes: 5 inscrições em `Main.java:60-64`. Nenhum aviso é montado ou
mandado fora dela. `ReservationService` só publica (`domain/service/ReservationService.java:101, 130`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern:** é um Observer de verdade. `ReservationService` recebe o `EventBus` de fora (construtor,
`ReservationService.java:21-25`) e publica `ReservationConfirmedEvent` e `ReservationCancelledEvent` sem saber quem
escuta (`ReservationService.java:101, 130`). O `EventBus` percorre a lista de ouvintes do tipo do evento
(`domain/event/EventBus.java:16-23`). Cada interessado é uma classe separada, e isso inclui o e-mail do hóspede
(`domain/event/handlers/GuestNotificationHandler.java:10`, `HousekeepingNotificationHandler.java:18-62`,
`FinanceNotificationHandler.java:20-66`). A inscrição fica na montagem (`Main.java:60-64`). Para avisar o restaurante
ou o transfer, basta uma classe nova e uma linha de `subscribe`, sem tocar no serviço. As regras "pendente cancelada
não avisa ninguém" e "sem devolução não avisa o financeiro" ficam dentro de cada ouvinte
(`HousekeepingNotificationHandler.java:42`, `FinanceNotificationHandler.java:41-50`), apoiadas no campo
`wasConfirmed` do evento. Isso é aceitável. Há um detalhe a corrigir: os ouvintes rodam em sequência dentro da
requisição, depois do `UPDATE` e fora de transação. Se um deles falhar, a reserva fica confirmada, só parte dos
e-mails é gravada e o cliente recebe 500.

**O exagero:** não há. A interface marcadora `DomainEvent` e o `EventHandler<T>` genérico são o próprio pattern.
`RoomService` e `MessageService` quase só repassam ao repositório (`domain/service/RoomService.java:16-30`,
`domain/service/MessageService.java:16-30`), mas servem para traduzir a `SQLException` e são finos demais para contar
como camada sem necessidade.

**O resto do código** tem defeitos localizados, mas reais:

- **Estado `static` compartilhado e não seguro entre threads:** `MoneyFormatter` guarda um único `DecimalFormat`
  estático (`domain/util/MoneyFormatter.java:9-19`). Ele é usado por várias threads do Jetty ao mesmo tempo, na
  confirmação e no cancelamento.
- **O catálogo dos quartos em três lugares:** o `Map` estático (`repository/RoomRepository.java:15-21`), o enum
  `RoomType` com capacidade e diária (`domain/model/RoomType.java:6-8`) e a tabela `rooms`, cujas colunas `capacity`
  e `nightly_rate` são gravadas e nunca lidas (`RoomRepository.java:52, 67`). O total de quartos está escrito à mão
  no relatório (`domain/service/ReportService.java:36`, `"total,%d/5"`).
- **Dinheiro sem escala fixa:** `BigDecimal.ZERO` sem `setScale(2)` em `domain/model/Reservation.java:26`,
  `PriceCalculator.java:40`, `ReservationService.java:116` e `ReportService.java:49`. O JSON sai com
  `"amountPaid":0`, `"discount":0` e `"refund":0`, e o relatório com `total,0`, em vez de 2 casas.
- **Os erros:** o mesmo `try { ... } catch (SQLException e) { throw new RuntimeException(...) }` aparece sete vezes em
  `ReservationService` (`:37-48, 66-70, 74-82, 95-99, 124-128, 144-148, 162-166`). `createReservation` só captura
  `IllegalArgumentException` (`api/ReservationController.java:59`). Uma data malformada (`DateTimeParseException`,
  `:38-39`) ou um corpo que não é JSON escapam e viram 500. Exceções de domínio sem mensagem também ficam aninhadas
  no serviço (`ReservationService.java:171-175`).
- **Um modelo anêmico:** `Reservation` é uma sacola de getters e setters (`domain/model/Reservation.java`), e as
  transições de situação ficam espalhadas em `if`s no serviço.

## Testes

**Números:** 11 testes rodaram, 0 falhas, 0 erros, 0 pulados; 11 métodos escritos, todos numa classe
(`ReservationSystemIT`). Cobertura de 75,3% das linhas (615/817) e 62,0% dos ramos (88/142) (`measures/tests.md`).
**Nota:** com problemas

Os testes passam pela API de verdade e conferem alguns valores do enunciado: o preço na alta temporada, 599,76
(`ReservationSystemIT.java:212-213`); o desconto meio para o par, 37,48 e 212,42 (`:240-241`); a devolução de 779,80
(`:309`); as recusas 409 e 422 (`:267-268, 294-295`); a linha da ocupação (`:365-366`). Também conferem os assuntos
e os destinatários dos avisos novos (`:180-185, 319-320`) e que o NON_REFUNDABLE cancelado só manda um aviso
(`:344`). Mas:

- **Rodam contra H2** (`ReservationSystemIT.java:46`), não contra o PostgreSQL de `DB_URL`. Por isso não pegam o
  `VARCHAR(11)` do CPF nem a falta de proteção contra a reserva dupla, que são os dois defeitos sérios do sistema.
- **São encadeados por `@Order` e dependem do estado deixado pelo teste anterior** (`:36`, ids fixos em `:164, 302,
  327, 351`). Um teste que falha derruba os seguintes.
- **Não conferem o corpo dos avisos novos,** só o assunto. A pergunta da mutação: trocar `getAmountPaid()` por outro
  valor no aviso do financeiro (`FinanceNotificationHandler.java:25`), ou tirar as datas e os hóspedes do aviso da
  governança (`HousekeepingNotificationHandler.java:21-27`), não derruba nenhum teste. Apagar o
  `if (!event.wasConfirmed()) return;` de `HousekeepingNotificationHandler.java:42` também não, porque nenhum teste
  cancela uma reserva pendente.
- **Ficam sem teste:** check-in, check-out, segunda via, `OPERATION_NOT_ALLOWED`, os erros 400 e 404 e o corpo do
  e-mail do hóspede. O relatório financeiro só confere que existe uma linha `total,` (`:381`). Não há teste de
  unidade de `PriceCalculator`, por exemplo nas fronteiras de 15/12 e de fevereiro.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. Uma chave estrangeira (`reservations.room_number -> rooms`) sem índice. Nenhum
CHECK, UNIQUE ou EXCLUDE. 0 transações explícitas, 0 `FOR UPDATE`. O esquema é criado com `CREATE TABLE` no código,
sem migrações. Nenhum SQL montado com `+` (`measures/database.md`).
**Nota:** ruim

Os tipos estão certos e todo o SQL usa parâmetros. Os problemas:

- **Reserva dupla sem nenhuma proteção:** a disponibilidade é conferida com um `SELECT COUNT(*)`
  (`repository/ReservationRepository.java:89-118`) e depois vem o `INSERT` (`domain/service/ReservationService.java:38-67`),
  sem transação, sem trava e sem restrição `EXCLUDE` no banco. Numa rodada do roteiro com o CPF só em dígitos, 9 das
  10 rodadas de dois pedidos simultâneos criaram as duas reservas. Isso é dado errado: o mesmo quarto vendido duas
  vezes.
- **Uma única `Connection` para o serviço inteiro:** `Main.java:46-49` tira uma conexão do pool e a entrega aos três
  repositórios para sempre. Todas as threads do Jetty usam essa conexão ao mesmo tempo. O pool de 10
  (`config/Database.java:18`) fica sem uso, não há isolamento entre requisições, e se essa conexão cair o serviço
  inteiro passa a falhar, porque nada pega outra.
- **Nenhuma transação onde as escritas têm de andar juntas:** a confirmação faz um `UPDATE` e três `INSERT` em
  `messages` em autocommit separado (`ReservationService.java:95-101`, `repository/MessageRepository.java:16-35`).
  Uma falha no meio deixa a reserva confirmada com parte dos avisos.
- **`guest_cpf VARCHAR(11)`** (`config/Database.java:45`), sem validar nem normalizar o CPF antes de gravar. O CPF
  com pontuação estoura a coluna (`value too long for type character varying(11)` em
  `measures/behavior-service.log`) e a criação responde 500.
- **O esquema não evolui:** `CREATE TABLE IF NOT EXISTS` (`config/Database.java:30-69`) não sabe acrescentar uma
  coluna numa tabela que já existe, e não há Flyway ou Liquibase. `status`, `rate_plan`, `type` e `channel` não têm
  CHECK. A chave estrangeira não tem índice.

## Comportamento

**Números:** o roteiro oficial passou em 16 das 66 conferências. Parte comum: 12 de 46. Depois de reiniciar: 1 de 3.
Cenário 03: 2 de 15. Duas reservas ao mesmo tempo: 1 de 2 (`measures/behavior.md`). Rodei uma cópia do roteiro
trocando só o CPF `111.222.333-44` por `11122233344`, e o resultado subiu para 61 de 66 (parte comum 42/46,
reinício 3/3, cenário 15/15, simultâneas 1/2).
**Nota:** ruim

- **Quase tudo falha por uma causa só:** o roteiro manda o CPF formatado, `111.222.333-44`, o mesmo formato do
  comprovante do enunciado. `POST /reservations` responde 500 porque o CPF não cabe em `VARCHAR(11)` e nada o valida
  antes. Sem reserva criada, todas as conferências seguintes falham (confirmação, e-mails, check-in, relatórios e os
  15 avisos do cenário). Para o dono da pousada, isso quer dizer que, se o site mandar o CPF com pontuação, nenhuma
  reserva entra, e o hóspede vê um erro de servidor em vez de um 400 que diga o que está errado.
- **Com o CPF em dígitos, a parte do cenário funciona inteira (15/15):** os avisos à governança com as datas e os
  hóspedes, ao financeiro com o valor pago, os avisos de cancelamento, nenhum aviso ao financeiro quando não há
  devolução e nenhum aviso quando a reserva cancelada estava pendente. Conferi também à mão: `GET /messages` traz
  "Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 hóspedes" e "Valor pago: R$ 779,80".
- **Falhas que continuam mesmo com o CPF em dígitos:**
  - Reserva dupla em 9 de 10 rodadas simultâneas. O mesmo quarto pode ser vendido duas vezes. O defeito pesa no
    banco e está citado aqui.
  - Corpo que não é JSON responde 500, não 400 `INVALID_REQUEST`. Uma data inválida no corpo também
    (`api/ReservationController.java:38-39, 59`).
  - `"amountPaid":0` e `"discount":0`, sem as 2 casas que o texto pede.
- **Não pesa:** as duas falhas de `GET /rooms` (capacidade e diária do 201). O roteiro procura `number == 201` como
  número, e o agente devolve `"201"` como texto. O enunciado não diz o tipo, e `capacity: 3` e
  `nightlyRate: 389.90` estão certos na resposta.

## Dúvidas

- O enunciado não diz em que formato o site manda o `guestCpf`. Ele só mostra o CPF formatado no comprovante. Se o
  site mandasse sempre os 11 dígitos, o comportamento ficaria em 61 de 66, e as falhas que restam (a reserva dupla, o
  500 com corpo inválido, o dinheiro sem 2 casas) dariam `com problemas` nessa coluna. Dei `ruim` porque o roteiro
  oficial usa o formato do próprio texto e porque estourar com 500, sem validação, não é uma escolha razoável para
  um ponto em aberto.
- A falha das reservas simultâneas no roteiro oficial (500 nos dois pedidos) vem do CPF. Quem mostra a reserva dupla
  é a rodada que fiz com o CPF em dígitos, numa cópia do roteiro fora da pasta das skills. O resultado oficial em
  `measures/behavior.md` foi restaurado depois dessa rodada.
