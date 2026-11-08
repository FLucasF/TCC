# Revisão: construção, cenário 03

## Visão geral

O projeto tem seis pacotes: `web` (Javalin), `service` (regras de aplicação e transação), `domain` (preço, temporada, situações da reserva, comprovante), `notification` (os avisos), `persistence` (JDBC puro) e a raiz com o `Main`, que monta as peças. Uma requisição entra numa rota de uma linha em `web/App.java:37-47`. O corpo é lido e validado em `web/ReservationRequestReader.java`, e daí a rota chama o `ReservationService`. O serviço abre uma transação (`persistence/Database.java:75-87`), trava a reserva com `FOR UPDATE` (ou o quarto, com `pg_advisory_xact_lock`) e pede ao objeto de domínio a mudança de situação (`domain/Reservation.java:24-46`). Depois grava a reserva e as mensagens que os ouvintes devolveram, tudo na mesma transação (`service/ReservationService.java:112-126`). Os dados ficam em duas tabelas do PostgreSQL (`reservations` e `messages`), criadas com `CREATE TABLE IF NOT EXISTS` no código. Os quartos ficam fixos no código (`domain/RoomCatalog.java`). O roteiro de comportamento passou inteiro (66/66), e o pedido do cenário, o Observer, foi bem resolvido. O que há de mais sério são defeitos menores do banco: não há migrações, a tabela de quartos não existe (e por isso não há chave estrangeira), as colunas de situação não têm CHECK e as conexões são abertas sem pool.

## Arquitetura

**Números:** 6 pacotes, 12 dependências entre eles, nenhum ciclo. Javalin só em `web` e no `Main`, JDBC em `persistence` e `service`. A maior classe é `ReservationRepository`, com 150 linhas.
**Nota:** bom

- **A borda HTTP é fina.** Cada rota lê a requisição, chama o serviço e escreve a resposta (`web/App.java:37-47`). Os erros viram JSON num só lugar: `web/App.java:49-53`, com `ServiceException` carregando o status e o código (`service/ServiceException.java:8-12`).
- **As regras estão no domínio, sem Javalin e sem JDBC.** O preço fica em `domain/Quote.java:8-17` e `domain/Season.java:15-26`, as transições em `domain/ReservationStatus.java:11-19` e a devolução em `domain/RatePlan.java:21-23`.
- **O SQL fica todo em `persistence`** (`ReservationRepository`, `MessageRepository`).
- **A montagem fica num lugar só**, `Main.java:20-40`, e a lista de ouvintes é passada de fora para o serviço.
- **Uma ressalva pequena:** os serviços conhecem `java.sql.Connection` e `SQLException`, porque recebem lambdas `Database.Work` e repassam a conexão aos repositórios (`service/ReservationService.java:59-66`, `128-134`; `service/ReportService.java:58-64`). É a fronteira da transação, sem SQL no serviço; num sistema deste tamanho não pesa.
- **Uma dependência a notar:** `persistence -> notification` existe porque `MessageRepository` grava o record `Message` (`persistence/MessageRepository.java:3-4`). É coerente e não forma ciclo.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança, financeiro), todos atrás da interface `ReservationListener`. Os 2 acontecimentos (confirmação e cancelamento de reserva paga) passam pelo mesmo sujeito, `ReservationEvents`. Nenhum e-mail é montado fora de um ouvinte.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** O Observer está inteiro:

- `notification/ReservationListener.java:13-24` declara um método por acontecimento, com resposta vazia por padrão, e cada interessado implementa só o que lhe importa.
- `GuestNotifier.java:11-19`, `HousekeepingNotifier.java:13-27` e `FinanceNotifier.java:13-28` montam cada um as suas mensagens. O hóspede também é um ouvinte, sem chamada direta.
- `ReservationEvents.java:14-28` percorre a lista recebida de fora.
- A lista é registrada na montagem (`Main.java:20-25`).
- O serviço não sabe quem é avisado: só chama `events.confirmed(...)` e `events.paidReservationCancelled(...)` (`service/ReservationService.java:76`, `84`).
- A regra "sem nada a devolver, o financeiro não é avisado" mora no próprio ouvinte do financeiro (`FinanceNotifier.java:22-24`).

O que afasta um pouco do Observer clássico:

- **Os ouvintes não mandam nada.** Eles devolvem `List<Message>`, e o serviço grava a lista na mesma transação (`ReservationService.java:122-123`). É uma escolha boa (a mensagem só existe se a mudança foi gravada), não um desvio.
- **O serviço escolhe o evento.** É ele que decide entre o evento de cancelamento de reserva paga e nenhum evento, com `r.status() == CONFIRMED` (`ReservationService.java:83-85`). Isso escolhe *o que* aconteceu, não *quem* é avisado, então não tira o acerto.
- **A lista é fixa no código** (`Main.java:20-25`). Para incluir o restaurante é preciso mudar a lista e reiniciar, o que é aceitável para "entram e saem conforme os contratos".

**O exagero.** Não há estrutura sem necessidade:

- `Channel` é um enum de um valor só, mas o texto pede o campo `channel` (`notification/Channel.java`).
- `Outcome` e `Change` são privados e pequenos, e evitam repetir o fluxo de transação em cada operação (`ReservationService.java:23-28`).
- Não há fábricas, builders nem interfaces com uma implementação só.

**O resto do código.**

- **Tipos e estado:** `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:17-19`, `Season.java:25`, `RatePlan.java:18`, `Quote.java:16`). Enums para situação, plano e tipo de quarto. Records imutáveis, e as transições validadas num lugar só (`Reservation.java:40-46`). Não há `static` mutável.
- **Repetição pequena 1:** o construtor de 16 argumentos de `Reservation` aparece em `Reservation.java:44`, `ReservationRepository.java:146` e `ReservationService.java:55`.
- **Repetição pequena 2:** `RoomCatalog.find(...).orElseThrow()` se repete em `ReservationService.java:96`, `105`, `115` e `ReportService.java:30`.
- Nada disso pede mudança antes de aceitar.

## Testes

**Números:** 10 testes rodaram, 0 falhas, 0 erros, 0 pulados; 10 métodos escritos (6 de domínio e 4 pela API contra o PostgreSQL de `DB_URL`, num schema próprio). Cobertura: 91,3% das linhas (397/435) e 78,1% dos ramos (82/105). Domínio: 85/86 linhas; `service`: 97/101 linhas e 18/18 ramos.
**Nota:** com problemas

**O que está bem testado, com valores:**

- o preço de alta temporada, 599.76 (`DomainTest.java:186-194`, numeração contínua do arquivo de teste mostrado), e as bordas da temporada (14/12 e 15/12, 28/02 e 01/03);
- o desconto meio para o par, 37.48 e 212.42 (`DomainTest.java:197-202`);
- o texto exato do comprovante (`DomainTest.java:205-215`);
- a devolução por plano e uma transição recusada (`DomainTest.java:218-228`);
- os códigos de erro e os dois relatórios pela API (`ApiIntegrationTest.java:80-162`).

**A parte nova do cenário está testada só em parte:**

- `DomainTest.java:231-244` confere os três assuntos da confirmação, o e-mail do hóspede e o valor pago no aviso do financeiro.
- `DomainTest.java:247-259` confere que o financeiro não é avisado quando não há devolução.
- Pela API, só se conta quantas mensagens surgiram (`ApiIntegrationTest.java:95`, `119`, `130`, `137`).
- **Nenhum teste confere os destinatários** `governanca@...` e `financeiro@...`.
- **Nenhum teste confere o corpo do aviso da governança** (as datas e o número de hóspedes, `HousekeepingNotifier.java:15-17`), nem as datas liberadas no aviso de cancelamento ou o valor do reembolso (`HousekeepingNotifier.java:23-25`, `FinanceNotifier.java:25-26`).

**A pergunta da mutação:**

- Trocar o endereço da governança, apagar a linha "Hóspedes" ou trocar `r.refund()` por `r.amountPaid()` no corpo do reembolso: nenhum teste falharia.
- Trocar `signum() <= 0` por `< 0` em `FinanceNotifier.java:22`: o teste falharia.

**Também ficaram sem teste:**

- a linha "Desconto" e o "1 diária" do comprovante (`Receipt.java:34-38`);
- as duas reservas simultâneas.

## Banco

**Números:** 2 tabelas (`reservations` com 16 colunas, `messages` com 5), ambas com chave primária. O dinheiro está todo em `numeric(12,2)` e as datas em `date`. Nenhuma chave estrangeira, nenhum CHECK, UNIQUE ou EXCLUDE. Status, plano e canal estão em `text` sem CHECK. O esquema é criado com `CREATE TABLE` no código. Não há pool: `DriverManager.getConnection` a cada transação.
**Nota:** com problemas

**O que está certo:**

- os tipos e os `NOT NULL` (`persistence/Database.java:32-57`);
- o SQL todo com `?`: os trechos montados com `+` só juntam constantes (`COLUMNS`, `ReservationRepository.java:20-21`);
- o try-with-resources em todo lugar;
- a reserva e as suas mensagens gravadas na mesma transação (`ReservationService.java:122-123`);
- a reserva duplicada impedida pelo banco, entre processos: `pg_advisory_xact_lock(roomNumber)` antes da consulta de sobreposição (`ReservationRepository.java:24-43`, `ReservationService.java:60-61`). As mudanças de situação usam `SELECT ... FOR UPDATE` (`ReservationRepository.java:85-87`).

**Os defeitos:**

- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` dentro de `Database.java:31-57`, sem migração versionada: o próximo pedido que acrescentar uma coluna não terá como alterá-la num banco que já roda.
- **Não há tabela de quartos.** Os quartos vivem em `RoomCatalog`, então `reservations.room_number` não tem chave estrangeira.
- **Nenhum CHECK protege `status`, `rate_plan` ou `guests`.** A regra "sem duas reservas na mesma noite" depende só da trava na aplicação, sem restrição `EXCLUDE`.
- **Falta índice** em `(room_number, check_in)` para a consulta de sobreposição e para os relatórios (`ReservationRepository.java:33-34`, `95-102`).
- **Não há pool:** cada transação abre uma conexão nova (`Database.java:76`).
- Nada disso perde ou corrompe dado.

## Comportamento

**Números:** 66 de 66 conferências passaram. Parte comum: 46/46. Dados depois de reiniciar: 3/3. Cenário 03: 15/15. Duas reservas ao mesmo tempo: 2/2 (10 rodadas, sempre uma criada e a outra recusada com 409).
**Nota:** bom

Tudo o que o texto pede funcionou pela API, a partir de um banco vazio:

- os preços com temporada e desconto (599.76, 37.48, 212.42);
- todos os códigos de erro;
- as transições;
- as devoluções;
- os dois relatórios em CSV;
- os dados que ficam depois de reiniciar.

No cenário, a confirmação manda os três e-mails:

- o hóspede recebe o comprovante;
- a governança recebe o assunto com o quarto, as datas e os hóspedes;
- o financeiro recebe o assunto com a reserva e o valor pago.

No cancelamento de uma reserva confirmada, a governança recebe as datas liberadas e o financeiro recebe o reembolso, só quando há algo a devolver. A reserva pendente cancelada não avisa ninguém. Para o dono da pousada, o sistema faz o que ele descreveu, e duas reservas ao mesmo tempo para o mesmo quarto não passam.

## Dúvidas

- Na nota dos testes, o limite entre `bom` e `com problemas` ficou apertado. A cobertura é alta e as regras de preço estão bem conferidas, mas os destinatários e o conteúdo dos avisos da governança e do reembolso ficaram sem asserção, e por isso optei por `com problemas`.
