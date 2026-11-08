# Revisão: construção, cenário 03

## Visão geral

O projeto tem sete pacotes (`controller`, `service`, `repository`, `model`, `config`, `util` e a raiz, com o `Main`).
Uma requisição entra num handler estático do Javalin (`controller/*Controller.java`), que chama o `ReservationService`
ou vai direto a um repositório. Os repositórios usam JDBC sobre um `HikariDataSource` estático
(`config/DatabaseConfig.java:14`). O esquema é criado com `CREATE TABLE IF NOT EXISTS` dentro do código
(`DatabaseConfig.java:49-89`). O defeito mais sério é que o sistema não funciona. O `Main` sobe o Javalin
(`Main.java:18-19`) e só depois registra as rotas. O registro de `GET /reservations/:id`
(`ReservationController.java:63`) lança `IllegalArgumentException`, porque o Javalin 6 usa `{id}` e não aceita mais `:id`
(`measures/behavior-service.log:23`). A thread `main` morre ali: nenhuma rota de reserva com id, nem `/messages`, nem
`/reports/*`, nem o tratador de `ApiException` (`ReservationController.java:184`) chega a ser registrada. Sobram
`GET /rooms` e `POST /reservations`, e este último grava a reserva no banco mas responde 400. O motivo é que falta o
módulo `jackson-datatype-jsr310` no `pom.xml` para serializar o `LocalDate`, e o `catch (Exception)` da linha 56
transforma o erro em `INVALID_REQUEST`. O resultado são reservas gravadas que o site acha que falharam, e reservas
duplicadas no mesmo quarto e na mesma noite (veja Banco). Os avisos à governança e ao financeiro, que são a parte nova do
cenário, foram escritos como chamadas em sequência, sem Observer, e nunca rodam pela API.

## Arquitetura

**Números:** 7 pacotes, 11 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só em
`controller` e na raiz, o JDBC só em `repository` e `config`, e `service` não usa nenhuma das duas bibliotecas.
**Nota:** com problemas

As camadas existem e as dependências vão no sentido certo: controller → service → repository → config. As regras
(`service/ReservationService.java`) não conhecem nem o Javalin nem o SQL. Há três defeitos:

- **A borda HTTP nem sempre é fina.** O relatório financeiro calcula o valor que fica com a pousada dentro do handler
  (`ReportController.java:74-87`, `amountPaid - refund` em `double`) e monta o CSV ali mesmo. Os dois relatórios e as
  mensagens vão direto ao repositório, sem passar por um serviço (`ReportController.java:20-21`,
  `MessageController.java:7`). O comprovante também é montado no controller (`ReservationController.java:130-142` e
  `162-174`), e a regra de que a segunda via só vale para reserva paga não está em lugar nenhum.
- **A montagem não fica num lugar só.** Cada classe cria as próprias dependências com `new`: `ReservationService.java:17-19`,
  `NotificationService.java:13` e os campos `static final` dos controllers (`ReservationController.java:21`,
  `ReportController.java:20-21`). Os repositórios pegam a conexão de um `static` global
  (`DatabaseConfig.getConnection()`, `ReservationRepository.java:36`). Nada pode ser trocado, nem num teste.
- **O tipo do quarto vem de um `switch` fixo por número**, copiado na borda e no serviço
  (`ReservationController.java:200-207`, `ReportController.java:109-116`, `NotificationService.java:107-114`). O código
  ignora o `Room.type` que já está no banco: um quarto novo vira "UNKNOWN".

## Design de baixo nível

**Números:** dos 5 avisos pedidos (o hóspede, a governança e o financeiro na confirmação; a governança e o financeiro no
cancelamento), nenhum está numa estrutura de ouvintes. Os 5 são chamadas diretas, em sequência, a métodos de uma única
classe notificadora (`ReservationService.java:113-115` e `151-156`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto diz que haverá cada vez mais interessados (o restaurante, o transfer) e que eles entram e saem
com o tempo, ou seja, pede um Observer. O que o agente entregou é o caso de omissão que a referência descreve: o
`ReservationService` conhece cada interessado e chama, um depois do outro, `sendGuestConfirmationEmail`,
`sendHousekeepingNotification` e `sendFinanceNotificationConfirmation` (`ReservationService.java:113-115`). No
cancelamento, ele mesmo decide quem é avisado (`ReservationService.java:151-156`). O `NotificationService` é um
notificador único que conhece todos os destinos, com os endereços fixos no código (`NotificationService.java:41`, `50`,
`61`, `70`). Não há interface de ouvinte, nem lista, nem registro na montagem. Para pôr o restaurante, é preciso mexer
no serviço de reservas e no notificador.

**O exagero.** Nenhuma estrutura sobra: não há interfaces com uma implementação só, fábricas nem camadas que só
repassam.

**O resto do código** tem defeitos espalhados:

- **Dinheiro em `double` em todo o domínio.** Isso vale para `Reservation.java:38-53`, `Room.java:16` e
  `ReservationDocument.java:35-41`. O arredondamento "meio para o par" é feito com `new BigDecimal(double)`
  (`MoneyUtil.java:12`, `17-18`, `23-24`), que parte da expansão binária do número, e a soma do relatório financeiro é
  feita em `double` (`ReportController.java:74-84`). O texto pede arredondamento exato em cada passo.
- **Exceções engolidas ou mal classificadas.** O `catch (Exception)` de `ReservationController.java:56` e `96` captura
  também a `ApiException`, de modo que `ROOM_NOT_FOUND`, `ROOM_UNAVAILABLE` e `CAPACITY_EXCEEDED` saem como 400
  `INVALID_REQUEST`. Os repositórios engolem a `SQLException` e devolvem vazio (`ReservationRepository.java:98-101`,
  `124-127`, `149-152`, `176-179`; `RoomRepository.java:27-30`). Uma falha de banco na conferência de disponibilidade
  passa por "quarto livre".
- **Regras erradas no código** (o roteiro nem chega a exercitar estas):
  - o cancelamento é aceito depois do check-in, porque só recusa `CHECKED_OUT` e `CANCELLED` (`ReservationService.java:127-130`);
  - a confirmação exige `amountPaid` no corpo e grava o valor que vier (`ReservationController.java:84-91`), mas o texto
    diz que o pago é o total;
  - a segunda via sai para qualquer reserva, inclusive pendente (`ReservationController.java:151-176`);
  - a conferência de conflito usa `check_in <= ?` (`ReservationRepository.java:111`), o que recusa uma reserva que
    termina no dia em que outra começa;
  - a alta temporada termina em 28/02 fixo (`DateUtil.java:38`), e o 29/02 dos anos bissextos fica fora.
- **Duplicação.** A classe `ErrorResponse` está repetida três vezes (`Main.java:35`, `ReservationController.java:190`,
  `ReportController.java:99`). O `getQuartoType` também aparece três vezes, e o comprovante é montado duas vezes em
  `ReservationController`.
- **Estado aberto.** Os modelos têm todos os campos `public` e mutáveis (`Reservation.java:7-53`). O serviço muda o
  objeto e depois o salva (`ReservationService.java:108-110`).

## Testes

**Números:** 7 testes rodaram, 0 falharam, 0 pulados; 7 métodos escritos. Cobertura de linhas: 27 de 696 (3,9%).
Cobertura de ramos: 7 de 150 (4,7%). Os pacotes `service`, `repository` e `controller` têm 0% (`measures/tests.md`).
**Nota:** ruim

Os testes passam e o `mvn verify` passa, mas quase não conferem nada:

- `ReservationServiceTest.java:13` (o arquivo `src/test/java/br/com/pousada/service/ReservationServiceTest.java`) é só
  `assertTrue(true)`.
- O que existe testa só os utilitários. A alta temporada é conferida com datas (`DateUtilTest.java:28-42`, sem o
  29/02). O 249,90 com 20% dá 299,88 e o desconto de 15% sobre 599,76 dá 89,96 (`MoneyUtilTest.java:75-83`). O teste de
  formatação só confere que o texto contém "R$" e "1" (`MoneyUtilTest.java:89-90`).
- Nada testa o preço de uma reserva inteira, as situações da reserva, o cancelamento e a devolução, os códigos de
  erro, o comprovante, os relatórios ou a parte nova do cenário (os e-mails da governança e do financeiro).
- Não há nenhum teste pela API nem contra o banco. Por isso as duas falhas que derrubam o sistema (a rota `:id` e o
  `LocalDate` sem módulo) passaram em branco.
- A pergunta da mutação: apagar a linha `ReservationService.java:115` (o aviso ao financeiro), trocar `refundAmount > 0`
  por `>= 0` em `ReservationService.java:153`, ou mudar o assunto em `NotificationService.java:35` não faria nenhum teste
  falhar.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. As 6 colunas de dinheiro são
`numeric(10,2)`, as datas são `date`, a chave estrangeira `reservations.room_number` tem índice, não há CHECK, UNIQUE
nem EXCLUDE, e não há nenhuma transação explícita nem `FOR UPDATE` (`measures/database.md`). O esquema é criado no
código, sem migração.
**Nota:** ruim

O que está certo: os tipos das colunas, as chaves, o `NOT NULL`, o SQL sempre com parâmetros `?`, o pool HikariCP e as
conexões em try-with-resources (`ReservationRepository.java:36-37`).

O que está errado:

- **Nada impede duas reservas do mesmo quarto na mesma noite.** A disponibilidade é conferida e a reserva é gravada em
  dois passos soltos, sem transação, sem trava e sem restrição (`ReservationService.java:42-45` e `90`;
  `ReservationRepository.java:104-128`). Isso foi observado de fato: depois do roteiro, o banco
  `review_behavior_2` guarda 9 pares de reservas do quarto 301 nas mesmas noites, ids 5/6, 7/8 … 21/22, de
  2030-02-04 a 2030-02-21, todas `PENDING`. Em 9 das 10 rodadas simultâneas, as duas reservas foram gravadas, embora
  ambas tenham recebido 400. Além disso, como a `SQLException` da conferência é engolida e vira lista vazia
  (`ReservationRepository.java:124-127`), uma falha de banco também libera o quarto.
- **O que se escreve não anda junto.** A confirmação grava a situação e depois insere três mensagens, cada uma na sua
  conexão e no seu autocommit (`ReservationService.java:110-115`, `MessageRepository.java:21`). Uma falha no meio deixa
  a reserva confirmada sem avisos.
- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` (`DatabaseConfig.java:49-89`), que não sabe
  acrescentar uma coluna numa tabela que já existe. As colunas `status`, `rate_plan` e `type` não têm CHECK.
- **O dinheiro passa por `double` no caminho.** É gravado com `new BigDecimal(double)` (`ReservationRepository.java:47-52`)
  e lido com `rs.getDouble` (`ReservationRepository.java:195-199`). O Postgres arredonda para 2 casas na gravação, mas o
  valor que o código usa é o `double`.

## Comportamento

**Números:** passaram 12 de 66 conferências:

- parte comum: 9 de 46;
- dados depois de reiniciar: 1 de 3;
- cenário 03: 1 de 15;
- reservas simultâneas: 1 de 2 (`measures/behavior.md`).

**Nota:** ruim

O serviço sobe, mas a thread `main` morre ao registrar as rotas (`measures/behavior-service.log:23`, em
`ReservationController.java:63`). Só funcionam `GET /rooms` (os 4 OKs dos quartos) e as recusas de `POST /reservations`
que acontecem antes do serviço: campo faltando, JSON inválido, saída igual à entrada e nenhum hóspede. Todo o resto
falha:

- criar uma reserva válida responde 400 em vez de 201, por causa do `LocalDate` sem o módulo jsr310 (log, linha 35),
  mas a reserva fica gravada;
- quarto inexistente, quarto ocupado e capacidade excedida saem como 400 `INVALID_REQUEST` em vez de 404, 409 e 422
  (`ReservationController.java:56`);
- `GET /reservations/{id}`, a confirmação, o check-in, o check-out, o cancelamento, a segunda via, `/messages` e os dois
  relatórios respondem 404 "Endpoint not found", porque nunca foram registrados.

Os únicos OKs do cenário e das reservas simultâneas passam por acaso. O "cancelar pendente não manda nada" passa porque
nada é mandado nunca. O "nenhuma rodada com as duas criadas" passa porque ambas responderam 400, embora o banco tenha
gravado as duas (veja Banco).

Para o dono da pousada, o resultado é que o site não consegue reservar nem confirmar. O hóspede não recebe e-mail, a
governança e o financeiro também não, e a recepção e o contador não têm relatório. Pior: cada tentativa do site deixa
uma reserva pendente gravada que ninguém vê e que bloqueia o quarto.

## Dúvidas

- **A nota do Banco.** Os tipos e as chaves estão certos, e a lista de `ruim` da skill trata de outros defeitos. A nota
  `ruim` se apoia na duplicação de reservas observada no banco (9 pares no `review_behavior_2`), que é dado errado
  gravado, e na ausência de qualquer proteção contra ela, nem mesmo dentro do processo. Se o critério for só o
  esquema, a nota seria `com problemas`.
- **Os defeitos de regra apontados no Design** (cancelar depois do check-in, `amountPaid` vindo do corpo, segunda via
  de reserva pendente, conflito com `<=`, o 29/02) foram lidos no código. O roteiro não chegou a exercitá-los, porque as
  rotas não existiam.
