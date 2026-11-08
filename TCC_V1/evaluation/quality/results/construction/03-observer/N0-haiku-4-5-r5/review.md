# Revisão: construção, cenário 03

## Visão geral

O projeto tem 8 pacotes: `controller`, `service`, `notification`, `repository`, `domain`, `util`, `config` e o pacote raiz com o `main`. Uma requisição entra num handler do Javalin registrado por um método estático (`controller/ReservationController.java:27`). O handler chama o `ReservationService` (`service/ReservationService.java:26`), e o serviço lê e grava no PostgreSQL pelos repositórios em JDBC puro, cada um abrindo a própria conexão com `DriverManager` (`config/DatabaseConfig.java:30`). As tabelas são criadas no código, com `CREATE TABLE IF NOT EXISTS` (`config/DatabaseConfig.java:34-80`). Os avisos de confirmação e de cancelamento passam por uma lista de ouvintes (`notification/NotificationService.java:32-42`). O defeito mais sério é que **o sistema não funciona pela API**. O `pom.xml` não tem o Jackson; ele traz o Gson (`pom.xml:35-39`), que o Javalin não usa. Por isso todo `ctx.json`/`bodyAsClass` dá 500 com "It looks like you don't have an object mapper configured": 74 ocorrências em `measures/behavior-service.log`, e só 6 de 66 conferências passaram. O único teste que o agente escreveu é de preço, e o `mvn verify` passa sem ver nada disso. O README e o ENTREGA.md dizem que tudo funciona (`ENTREGA.md:53`), mas o código não confirma isso. Além disso há erros de regra que o 500 esconde:
- a alta temporada é aplicada à estadia inteira quando qualquer noite cai nela;
- o aviso de cancelamento da governança nunca sai;
- a segunda via sai para reserva cancelada.

## Arquitetura

**Números:** 8 pacotes, 20 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O `controller` depende de 5 pacotes. O `util` depende de `repository` e usa JDBC. A maior classe é `ReservationController`, com 288 linhas.
**Nota:** com problemas

As camadas existem e as dependências apontam na direção certa, sem ciclo. O `ReservationService` não conhece o Javalin nem monta SQL (`service/ReservationService.java:1-11`), e o SQL fica nos repositórios. Mas há misturas localizadas:

- **A borda HTTP tem regra e acesso direto aos dados.**
  - `GET /reservations/{id}` e a segunda via vão direto ao repositório (`controller/ReservationController.java:76`, `:178`).
  - A regra de quando existe segunda via mora no handler (`controller/ReservationController.java:186-190`), e está errada: veja Comportamento.
  - Os dois relatórios inteiros (filtragem, cálculo do valor que fica com a pousada, total) estão no controller, que cria repositórios com `new` dentro do handler (`controller/ReportController.java:64-119`, `:65-66`, `:93`, `:101-113`).
  - `RoomController` e `MessageController` também dão `new` no repositório dentro da rota (`controller/RoomController.java:12`, `controller/MessageController.java:12`).
- **A montagem está espalhada.** O `main` cria só o `NotificationService` (`Application.java:16-17`). O `ReservationController` monta o serviço e os repositórios em campos `static` (`controller/ReservationController.java:20-25`). O `NotificationService` é um singleton global (`notification/NotificationService.java:9-20`). Os repositórios pegam conexão num `static` global (`repository/ReservationRepository.java:23`).
- **Um utilitário de formatação consulta o banco.** `DocumentFormatter` guarda um `RoomRepository` estático e faz SELECT para descobrir o tipo do quarto (`util/DocumentFormatter.java:11`, `:15`). Isso cria a dependência `util -> repository` e põe JDBC num pacote de utilitários.

Não chega a `ruim`: não há SQL nas rotas nem Javalin nas regras, e não há ciclo. Mas o próximo relatório ou a próxima regra de documento vai para o controller.

## Design de baixo nível

**Números:** a interface `NotificationListener` (`notification/NotificationListener.java:5-8`) tem 3 implementações, uma por interessado (hóspede, governança, financeiro), e cobre os dois eventos (confirmação e cancelamento). O serviço manda nos dois casos pelos ouvintes (`service/ReservationService.java:92`, `:136`). Nenhum e-mail é montado fora de um ouvinte. A lista é montada dentro do próprio `NotificationService` (`notification/NotificationService.java:22-26`), e o `addListener` (`:28`) nunca é chamado.
**Classe:** acerto
**Exagero:** exagero
**Nota:** ruim

**O pattern.** Isto é um Observer. O `ReservationService` só anuncia `notifyReservationConfirmed`/`notifyReservationCancelled` e não sabe quem é avisado. Cada interessado é uma classe separada que monta o próprio e-mail:
- `GuestNotificationListener.java:19-27`;
- `GovernanceNotificationListener.java:19-58`;
- `FinanceNotificationListener.java:19-42`.

O hóspede também é ouvinte, e o cancelamento também passa pela lista. Para pôr o restaurante ou o transfer basta uma classe nova.

Duas coisas afastam do ideal:
- **O registro não fica na montagem do sistema.** O próprio despachante dá `new` em cada ouvinte concreto (`notification/NotificationService.java:22-26`), em vez de recebê-los do `main`. Quem entra e sai conforme os contratos é registrado editando a classe que dispara os avisos.
- **O evento é só a reserva no estado novo.** Com isso o ouvinte da governança tenta descobrir se ela "estava confirmada" olhando o status (`notification/GovernanceNotificationListener.java:39-43`). Como a reserva chega sempre `CANCELLED`, o aviso nunca sai. A falta de um evento com o estado anterior (ou de um evento próprio para "confirmada e cancelada") gerou um erro de regra.

**O exagero.**
- O `Reservation.Builder` (`domain/Reservation.java:61-99`) só serve para copiar os 16 campos a cada mudança de situação.
- O singleton `NotificationService` tem estado mutável estático e um `getInstance` sem sincronização (`notification/NotificationService.java:9-20`).
- Há estrutura que nada usa: `addListener` (`notification/NotificationService.java:28`) e `MathUtil.roundHalfEven(double)` (`util/MathUtil.java:11`).

**O resto do código.** Os defeitos estão espalhados:

- **Duplicação.** O mesmo bloco de 17 linhas do builder aparece copiado 4 vezes (`service/ReservationService.java:72-89`, `:116-133`, `:151-168`, `:184-201`). Os mesmos 4 `catch` estão repetidos nas 6 rotas (`controller/ReservationController.java:49-70`, `:99-111`, `:119-131`, …). `error()` está duplicado (`controller/ReservationController.java:269`, `controller/ReportController.java:125`). O mapeamento da linha de `rooms` aparece duas vezes (`repository/RoomRepository.java:18-23`, `:38-43`).
- **Regra errada no preço.** `isHighSeason` devolve `true` se *qualquer* noite cai na temporada, e aí a diária com 20% vale para todas (`service/PricingService.java:20-21`, `:36-53`). Uma estadia de 28/02 a 02/03 cobra a noite de 01/03 como alta temporada.
- **Erros.**
  - Captura `JsonSyntaxException` do Gson num Javalin que usaria o Jackson, então o tratamento é morto (`controller/ReservationController.java:64`).
  - `RatePlan.valueOf` com valor inválido lança `IllegalArgumentException`, que nada captura e vira 500 (`controller/ReservationController.java:44`).
  - Os ouvintes embrulham `SQLException` em `RuntimeException` (`notification/FinanceNotificationListener.java:24-26`). Isso estoura depois de a reserva já estar gravada como confirmada (`service/ReservationService.java:91-92`) e a resposta vira 500.
  - Faltar `roomNumber` ou `guests` é detectado comparando com 0 (`controller/ReservationController.java:205-207`).
- **Estado e formatação.**
  - Um `NumberFormat` estático, que não é seguro entre threads, é compartilhado pelas requisições (`util/MoneyFormatter.java:8`).
  - Ele também gera "R$" seguido de espaço não separável (U+00A0, conferido rodando a mesma chamada), não o "R$ 779,80" do texto.
  - O CSV usa `String.format("%.2f")`, que depende da localidade da máquina (`controller/ReportController.java:121-123`).

O que está certo: `BigDecimal` com `HALF_EVEN` a cada passo (`util/MathUtil.java:7-9`), `LocalDate` para datas e `enum` para situação, plano e tipo (`domain/Reservation.java:101-107`).

## Testes

**Números:** 6 testes rodaram, 0 falharam; 6 métodos de teste escritos, todos em um arquivo. Cobertura: 39 de 917 linhas (4,3%) e 14 de 178 ramos (7,9%). `notification`, `controller`, `repository`, `config` e o `util` (fora uma linha) têm 0% (`measures/tests.md`). Não há nenhum teste pela API nem contra o banco.
**Nota:** ruim

O único arquivo, `src/test/java/com/pousada/marealta/service/PricingServiceTest.java`, confere o preço com valores e isso está bem feito:
- fora de temporada (`:15-26`);
- a alta temporada com o exemplo do texto, 599,76 (`:29-40`);
- o desconto NON_REFUNDABLE com meio para o par (`:43-54`, `:57-68`);
- a fronteira de 15/12 (`:71-96`).

Todo o resto ficou sem rede:
- a parte nova do cenário (os ouvintes, os destinatários, os assuntos, o "não avisa o financeiro sem devolução");
- as transições de situação, as devoluções, as recusas e seus códigos HTTP, os relatórios, o comprovante.

A pergunta da mutação mostra o mesmo:
- apagar a linha que registra o `FinanceNotificationListener` (`notification/NotificationService.java:25`) não derruba teste nenhum;
- trocar o endereço da governança também não;
- o ouvinte de cancelamento da governança, que nunca dispara, passa despercebido;
- a estadia que cruza o fim da temporada não é testada, então a regra errada do `isHighSeason` passa.

Sobretudo, nenhum teste sobe a aplicação. Por isso o `mvn verify` passa com um sistema em que toda rota JSON responde 500.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em `numeric(10,2)` e as datas em `date`. As conferências acusaram:
- a chave estrangeira `reservations(room_number) -> rooms` sem índice;
- nenhum CHECK nas colunas de situação, plano, tipo e canal;
- nenhuma restrição UNIQUE/EXCLUDE;
- nenhum pool, nenhuma transação explícita, nenhum `FOR UPDATE`.

Nenhum SQL é montado com `+` (`measures/database.md`). O serviço subiu e criou o esquema num banco vazio.
**Nota:** com problemas

Os tipos estão certos e o SQL usa parâmetros (`repository/ReservationRepository.java:16-39`, `:91-103`). As conexões são devolvidas com try-with-resources.

Os defeitos:
- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código (`config/DatabaseConfig.java:34-80`), sem migração. A primeira coluna nova não chega num banco que já existe.
- **Nada impede duas reservas na mesma noite.** A disponibilidade é um `SELECT COUNT(*)` seguido de um `INSERT` em outra conexão (`service/ReservationService.java:38`, `:59`; `repository/ReservationRepository.java:90-112`). Não há restrição de exclusão, transação, trava nem sequer `synchronized`. Dois pedidos simultâneos podem passar os dois.
- **Duas escritas não andam juntas.** A confirmação grava a situação e depois cada ouvinte grava sua mensagem numa conexão própria (`service/ReservationService.java:91-92`). Se um falha, a reserva fica confirmada com parte dos avisos.
- **Detalhes menores.**
  - Uma conexão nova por consulta com `DriverManager`, sem pool (`config/DatabaseConfig.java:30`).
  - O relatório de ocupação faz uma consulta por quarto (`controller/ReportController.java:73-74`).
  - `discount` aceita nulo, mas sempre é gravado (`config/DatabaseConfig.java:57`).

## Comportamento

**Números:** 6 de 66 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 3 de 46 |
| os dados depois de reiniciar | 1 de 3 |
| o cenário 03 | 1 de 15 |
| as reservas simultâneas | 1 de 2 |

**Nota:** ruim

O serviço sobe, mas toda resposta JSON e toda leitura de corpo JSON dá 500, porque não há mapeador de JSON (`pom.xml:35-39` traz o Gson, e o Javalin 6 precisa do `jackson-databind`). O log repete o aviso do Javalin 74 vezes.

Para o dono da pousada isso significa:
- o site não lista os quartos (`GET /rooms` 500);
- não cria reserva (`POST /reservations` 500 em todos os casos, inclusive nas recusas, que deviam ser 400/404/409/422);
- não confirma, não faz check-in nem check-out;
- não manda e-mail a ninguém.

As 6 que passaram são vazias:
- os cabeçalhos dos CSV, que saem sem nenhuma reserva (`total,0/5`, `total,0.00`);
- "as mensagens continuam", com a lista vazia;
- "cancelar pendente não manda nada", porque nada foi criado;
- "nenhuma rodada com duas reservas criadas", porque as duas deram 500 nas dez rodadas.

Só a validação do mês inválido nem chegou a ser 400: deu 500, porque a resposta de erro também é JSON (`controller/ReportController.java:56`).

Mesmo com o Jackson, a leitura do código mostra falhas que o roteiro pegaria:
- **A governança nunca é avisada de um cancelamento.** O ouvinte só segue se a reserva estiver CONFIRMED, CHECKED_IN ou CHECKED_OUT, mas recebe a reserva já CANCELLED (`notification/GovernanceNotificationListener.java:39-43`, `service/ReservationService.java:130`, `:136`).
- **Os valores nos e-mails não batem com o texto.** Eles saem com espaço não separável depois de "R$" (`util/MoneyFormatter.java:8`), então não seriam o "R$ 779,80" do texto.
- **A segunda via sai para reserva cancelada.** Só a PENDING é recusada (`controller/ReservationController.java:186`).
- **A estadia que cruza o fim da alta temporada é cobrada toda com 20%** (`service/PricingService.java:20`, `:36-53`).
- **As reservas simultâneas não estão protegidas** (ver Banco).

## Dúvidas

- **A classe do pattern.** Dei `acerto` porque a estrutura está toda lá: interface, um ouvinte por interessado com o hóspede incluído, os dois eventos pela lista, e o serviço sem conhecer os destinos. Mas a lista é criada dentro do próprio `NotificationService` (`notification/NotificationService.java:22-26`), não na montagem do sistema, e um dos ouvintes erra a regra por depender do status. Um revisor mais rígido poderia ler isso como `malfeito`.
- **O exagero.** Um builder para uma entidade de 16 campos imutável é defensável em si. Eu o contei como exagero porque aqui ele só serve para recopiar a reserva a cada transição, junto com o singleton e o `addListener` sem uso.
- **O que o roteiro não mediu.** Não dá para medir, pela API, quanto do restante funcionaria se o mapeador de JSON existisse. As falhas que listei além do 500 vêm da leitura do código e de uma execução isolada do `NumberFormat`, não do roteiro.
