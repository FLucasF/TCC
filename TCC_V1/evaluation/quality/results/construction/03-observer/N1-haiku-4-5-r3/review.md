# Revisão: construção, cenário 03

## Visão geral

O projeto é um Maven com Javalin 6.7.0 e JDBC puro. Tem 9 pacotes: `route` para o HTTP, `service` para as regras,
`repository` para o SQL, `database` com uma `Connection` estática única, mais `model`, `dto`, `notification` e `util`.
Tudo é montado em `Main.java:34-54`. Uma requisição entra numa rota estática (`ReservationRoutes.register`), passa
pelo `ReservationService`, que usa os repositórios, e os repositórios pegam a conexão global com
`Database.getConnection()`. Os dados ficam em três tabelas criadas com `CREATE TABLE IF NOT EXISTS`
(`Database.java:21-73`). O `mvn verify` passa, mas o sistema não funciona pela API, por três defeitos que se somam:

1. As rotas usam a sintaxe `:id` do Javalin 3. O Javalin 6 recusa essa sintaxe e o `main` morre em
   `ReservationRoutes.java:24` (`IllegalArgumentException: Path '/reservations/:id' invalid`, em
   `measures/behavior-service.log`). Por isso as rotas de mensagens e de relatórios nunca são registradas.
2. O `pom.xml` não traz o `jackson-databind` (traz um Gson que nada usa). Assim, todo `ctx.json` e todo
   `ctx.bodyAsClass` dão 500 ("It looks like you don't have an object mapper configured").
3. O `Database.java:77` faz `DELETE FROM rooms` a cada vez que o serviço sobe, mas `reservations.room_number` tem
   chave estrangeira para `rooms`. Com uma reserva gravada, o serviço não sobe mais (eu conferi: veja Banco).

O roteiro do comportamento passou em 4 de 66 conferências.

## Arquitetura

**Números:** 9 pacotes, 19 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só
em `route` e no pacote raiz; o JDBC aparece em `database`, `repository` e também em `service`, por causa do
`SQLException`. A maior classe é `ReservationService`, com 265 linhas.
**Nota:** com problemas

As camadas existem e as dependências vão no sentido certo: rota → serviço → repositório → banco, sem ciclo. As regras
de preço, de situação da reserva e de cancelamento estão no serviço, sem Javalin (`ReservationService.java:32-155`).
A montagem fica num lugar só (`Main.java:36-54`). Os defeitos:

- **Estado global no acesso ao banco:** os repositórios não recebem a conexão. Todos pegam a mesma `static
  Connection` com `Database.getConnection()` (`Database.java:9,17`; `ReservationRepository.java:24,61,73`;
  `RoomRepository.java:19,32`; `MessageRepository.java:17`). Isso amarra tudo a uma única conexão, compartilhada
  entre as threads do Jetty, e impede trocar a peça na montagem.
- **Apresentação misturada nas rotas e no serviço:** `ReservationRoutes.java:133-195` monta o comprovante e tem
  cópias próprias de `formatCpf`, `formatDate` e `getRoomType`. O `ReservationService.java:157-215` escreve o texto
  do e-mail e do comprovante.
- **Rotas que pulam o serviço:** `RoomRoutes.java:14` e `MessageRoutes.java:15` chamam o repositório direto. Isso pesa
  pouco num sistema deste tamanho.
- **O serviço depende do JDBC:** os métodos do serviço declaram `throws SQLException`
  (`ReservationService.java:77,99,112`).
- **Os quartos fixos no código:** `ReportService.java:28` percorre `new int[]{101, 102, 201, 202, 301}` e
  `ReportService.java:43` fixa `/5`, em vez de usar o `RoomRepository`, que o próprio `ReportService` recebe
  (`ReportService.java:16`) e não usa.

## Design de baixo nível

**Números:** há dois interessados no fluxo de ouvintes (`GovernanceNotifier` e `FinanceNotifier`, registrados em
`Main.java:41-42`). O e-mail do hóspede fica fora dele, numa chamada direta (`ReservationService.java:93`). A
confirmação e o cancelamento passam pela lista (`ReservationService.java:94,144`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** ruim

**O que aproxima do Observer:**

- Existe uma interface de ouvinte com os dois eventos (`ReservationNotifier.java:5-8`), com uma implementação por
  interessado (`GovernanceNotifier.java:8`, `FinanceNotifier.java:8`).
- O `NotificationService` guarda a lista e a percorre (`NotificationService.java:8-24`). Os ouvintes são registrados
  na montagem (`Main.java:40-42`), e o serviço de reservas recebe esse notificador de fora
  (`ReservationService.java:24-29`).
- Cada ouvinte decide se reage: o financeiro não avisa quando o reembolso é zero (`FinanceNotifier.java:30`), e o
  serviço só anuncia o cancelamento quando a reserva estava confirmada (`ReservationService.java:143-145`).
- Para pôr o restaurante ou o transfer basta escrever uma classe e registrá-la.

**O que afasta:** o hóspede, que a referência manda tratar como ouvinte também, continua com chamada direta no
serviço. `confirmReservation` chama `sendConfirmationEmail(reservation)` (`ReservationService.java:93`), e o
`ReservationService.java:157-174` monta o assunto e o corpo e grava a mensagem. A referência dá esse caso como
malfeito: "o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes".

**Exagero:** não há. A interface tem duas implementações que se trocam de verdade, os DTOs são simples, e não há
fábrica nem camada que só repassa.

**O resto do código** tem defeitos espalhados, que tornam qualquer mudança arriscada:

- **Os dados dos quartos copiados em vários lugares:** o `getRoomType` fixo existe três vezes
  (`ReservationService.java:199-206`, `ReservationRoutes.java:188-195`, `ReportService.java:78-85`), além da
  lista de números em `ReportService.java:28` e do `INSERT` em `Database.java:78-82`. O `Room.getType()` do banco
  não é usado para isso. Também estão duplicados o `formatCpf` (`ReservationService.java:208`,
  `ReservationRoutes.java:179`) e a formatação de data (`DateUtils.java:28`, `ReservationRoutes.java:175`).
- **Os erros como texto:** os códigos viajam como mensagem de `IllegalArgumentException`
  (`ReservationService.java:38,41,47,86`), e a rota os compara com `equals` em uma cadeia de `if`
  (`ReservationRoutes.java:150-173`). O `validateCreateRequest` pega a própria exceção e decide pelo conteúdo da
  mensagem (`ReservationService.java:258-262`). Um `ratePlan` inválido faz o `RatePlan.valueOf`
  (`ReservationService.java:52`) cair no `else` e virar 500 `INTERNAL_ERROR`, quando deveria ser 400.
- **O `catch (Exception)` que esconde a causa:** qualquer falha no `POST /reservations`, inclusive uma do banco,
  vira 400 "Invalid request body" (`ReservationRoutes.java:18-20`). O mesmo vale para o relatório financeiro
  (`ReportRoutes.java:47-49`). As outras rotas devolvem `e.getMessage()` cru com 500.
- **O estado:** uma `static Connection` mutável (`Database.java:9`), campos públicos nos DTOs
  (`CreateReservationRequest.java:4-11`) e setters livres na `Reservation`, que o serviço muda à mão
  (`ReservationService.java:89-90`).
- **Os tipos:** o dinheiro é `BigDecimal` com `HALF_EVEN` (`MoneyUtils.java:9-19`), o que está certo. Mas o
  `ratePlan` chega como `String` e o tipo do quarto é `String` (`Room.java:7`). O `CreateReservationRequest` usa
  `int` para `roomNumber` e `guests` (`CreateReservationRequest.java:4,10`), e por isso a falta do campo vira `0`
  e é tratada por `roomNumber == 0` (`ReservationService.java:241`).
- **Uso de API errada:** a sintaxe de rota `:id` (`ReservationRoutes.java:24,37,50,63,76,90`) não existe no Javalin
  pedido, e o mapeador JSON não foi configurado.

## Testes

**Números:** 8 testes rodaram (0 falhas, 0 erros, 0 pulados) e há 8 métodos de teste escritos. A cobertura é de
11 de 724 linhas (1,5%) e de 8 de 174 ramos (4,6%). Fora do `util`, a cobertura é 0% em todos os pacotes
(`measures/tests.md`). Há só testes de unidade de utilitários: nenhum pela API e nenhum contra o banco.
**Nota:** ruim

Há só dois arquivos, e ambos testam utilitários:

- `DateUtilsTest.java` confere com valores os limites da alta temporada (14/12 e 15/12, julho, janeiro e fevereiro;
  linhas 11-28), a contagem de noites e o formato da data.
- `MoneyUtilsTest.java` confere o arredondamento "meio para o par" (linhas 55-59), a diária de alta temporada de
  299.88 (linha 65) e o desconto de 15% de 89.96 (linha 72).

Os seguintes pontos não têm nenhum teste:

- o cálculo da reserva (`ReservationService.calculateNightsCost`);
- as transições de situação, o cancelamento e a devolução;
- os códigos de erro;
- os relatórios;
- o comprovante e o e-mail;
- toda a parte nova do cenário, os ouvintes (`notification`, com 0 de 49 linhas cobertas).

Pela pergunta da mutação: se alguém apagasse o `if` de `FinanceNotifier.java:30`, ou trocasse o endereço de
`GovernanceNotifier.java:9`, ou removesse o `notificationService.notifyConfirmed` de
`ReservationService.java:94`, nenhum teste falharia. A falta de testes da API também deixou passar o que mais pesa:
o serviço morre ao subir (`ReservationRoutes.java:24`) e não serializa JSON, e mesmo assim o `mvn verify` passa.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)`, as datas em `date`, e a chave estrangeira `reservations.room_number` tem índice. Nenhuma restrição
CHECK, UNIQUE ou EXCLUDE. O esquema é criado com `CREATE TABLE` no código, sem migrações. Há uma conexão por
`DriverManager`, sem pool, nenhuma transação explícita e nenhum `FOR UPDATE` (`measures/database.md`).
**Nota:** ruim

- **O serviço não sobe de novo depois da primeira reserva:** a cada início, `Database.java:77` apaga todos os
  quartos (`DELETE FROM rooms`) para inseri-los de novo. Mas `reservations.room_number` referencia
  `rooms(number)` (`Database.java:35`). Eu conferi à parte: subi o jar num banco vazio, gravei uma reserva direto
  na tabela e subi de novo. O `main` caiu com `PSQLException: update or delete on table "rooms" violates foreign key
  constraint "reservations_room_number_fkey"`. Na prática, o primeiro reinício com dados deixa a pousada sem sistema.
  O roteiro do comportamento não chegou a ver isso, porque nenhuma reserva foi criada pela API.
- **Uma conexão para tudo:** uma única `static Connection`, compartilhada por todas as threads do Jetty
  (`Database.java:9,12`), sem pool e sem reconexão. Se a conexão cair, o sistema para.
- **A regra "um quarto, uma reserva por noite" sem proteção:** só há o `SELECT` de conflito
  (`ReservationRepository.java:85-109`) seguido do `INSERT` (`ReservationService.java:44-74`), sem transação, sem
  trava e sem restrição `EXCLUDE`. Dois pedidos ao mesmo tempo podem gravar as duas reservas.
- **Um vazamento de recurso:** `RoomRepository.java:32` cria um `PreparedStatement` fora do try-with-resources e
  nunca o fecha.
- **O esquema não evolui:** o `CREATE TABLE IF NOT EXISTS` (`Database.java:23-61`) não sabe acrescentar uma coluna
  numa tabela que já existe.
- **O que está certo:** todo o SQL usa parâmetros `?` e as colunas têm os tipos certos.

## Comportamento

**Números:** 4 de 66 conferências passaram (62 falharam), por parte:

- a parte comum: 1 de 46;
- depois de reiniciar: 1 de 3;
- o cenário 03: 1 de 15;
- duas reservas ao mesmo tempo: 1 de 2.

**Nota:** ruim

O serviço sobe a porta, mas o `main` morre logo depois, ao registrar `/reservations/:id`, uma sintaxe que o
Javalin 6 recusa (`ReservationRoutes.java:24`; o erro está em `measures/behavior-service.log`). Ficam registradas
só as rotas `GET /rooms` e `POST /reservations`, e as duas dão 500 porque não há mapeador JSON: o `pom.xml` não tem
Jackson. Todas as outras chamadas dão 404 "Endpoint not found": consultar, confirmar, cancelar, check-in, check-out,
segunda via, mensagens e relatórios.

As quatro conferências que passaram não provam nenhuma regra:

- "refund null" passou porque o campo veio ausente;
- "as mensagens continuam" e "cancelar pendente não manda nada" passaram porque não existe mensagem alguma;
- "nenhuma rodada com as duas reservas criadas" passou porque todas as tentativas deram 500.

Para o dono da pousada, isso quer dizer que o site não consegue listar quartos nem criar reservas, a recepção não
faz check-in nem check-out, e não saem e-mails para o hóspede, a governança ou o financeiro. Os relatórios também
não existem pela API. Além disso, mesmo que esses dois defeitos fossem corrigidos, o primeiro reinício com uma
reserva gravada derrubaria o serviço (`Database.java:77`, contado na coluna do banco).

## Dúvidas

- As regras do cenário não puderam ser vistas funcionando pela API. Pela leitura, os assuntos, os destinatários e
  as condições dos avisos parecem corretos (`GovernanceNotifier.java:18-44`, `FinanceNotifier.java:18-41`), assim
  como o cálculo de preço (`ReservationService.java:217-238`). Mas isso não foi confirmado em execução.
- A nota do design de baixo nível ficou entre `com problemas` e `ruim`. Escolhi `ruim` pela soma de três coisas: os
  dados dos quartos repetidos em quatro lugares, os erros tratados como texto e o `catch (Exception)` que esconde a
  causa. Cada defeito, sozinho, é localizado.
