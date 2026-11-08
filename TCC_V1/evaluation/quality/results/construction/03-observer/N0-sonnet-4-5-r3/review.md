# Revisão: construção, cenário 03

## Visão geral

O projeto é um Javalin com montagem manual em `Main.java`, separado em `controller` (handlers HTTP), `service` (regras: preço, situação da reserva, relatórios), `repository` (JDBC puro com `PreparedStatement`), `domain` (POJOs e enums), `event` (o publicador e os eventos) e `notification` (um ouvinte por interessado). Uma requisição entra pelo handler (`ReservationController`), que lê o corpo como `Map`, chama `ReservationService`, e este fala com os repositórios e publica `ReservationConfirmedEvent`/`ReservationCancelledEvent` no `EventPublisher`. Os ouvintes gravam as mensagens na tabela `messages`. O esquema é criado com `CREATE TABLE IF NOT EXISTS` em `config/Database.java`, que também guarda o pool num `static`. O Observer pedido está bem feito, e com a convenção do próprio agente (número do quarto como texto, CPF só com dígitos) os cinco avisos saem certos. O mais sério está fora do pattern. A coluna `guest_cpf` é `VARCHAR(11)` (`Database.java:49`), então o CPF no formato que o próprio texto mostra ("111.222.333-44") não cabe e a criação da reserva cai num `400 INVALID_REQUEST "Failed to insert reservation"`. Um `roomNumber` numérico dá `ClassCastException`, que também vira 400 (`ReservationController.java:25`). Por isso o roteiro de comportamento passou só em 16 de 66 conferências. Nada impede duas reservas do mesmo quarto ao mesmo tempo: nas minhas 10 rodadas, as 10 criaram as duas. O único teste roda com Testcontainers e precisa de Docker, então nenhum teste rodou aqui (cobertura 0%).

## Arquitetura

**Números:** 9 pacotes, 21 dependências entre eles, sem ciclo (`measures/dependencies.md`). Javalin só em `controller` e na raiz, JDBC só em `config` e `repository`. A maior classe é `ReservationController` (267 linhas), seguida de `ReservationRepository` (234).
**Nota:** com problemas

As camadas existem e apontam na direção certa: borda → `service` → `repository`, sem Javalin nem SQL nas regras (`service/ReservationService.java:31-153` não conhece `Context` nem `java.sql`). A montagem é feita num lugar só (`Main.java:34-62`), inclusive o registro dos ouvintes (`Main.java:44-54`). Os defeitos:

- **Regra de negócio no handler.** Quem decide se a segunda via existe é o controller, pelo valor pago (`controller/ReservationController.java:210`), e não o serviço pela situação. Isso gera o defeito de comportamento da segunda via de reserva cancelada (ver Comportamento). O tipo do quarto é deduzido do primeiro dígito do número dentro do handler (`ReservationController.java:262-266`), em vez de vir do `Room` que o repositório já tem.
- **Estado global estático.** O pool fica num `static HikariDataSource` (`config/Database.java:11`), e os repositórios o buscam por `Database.getConnection()` estático (`repository/ReservationRepository.java:34`, `MessageRepository.java:22`, `RoomRepository.java:21`), em vez de receberem o `DataSource` na montagem. Só existe um banco por processo, e o teste precisa chamar o `Database.init` global (`SystemIT.java:56`).
- **Borda que pula o serviço.** `RoomController` e `MessageController` vão direto ao repositório (`RoomController.java:15`, `MessageController.java:15`). Num sistema deste tamanho isso é aceitável, mas `NotificationService` mora em `service` e é usado por `notification`, que é usado pela raiz. Isso cria uma dependência de `notification` para `service` (`notification/GuestNotificationListener.java:6`) só para um repasse.

## Design de baixo nível

**Números:** 2 eventos (confirmação e cancelamento), 5 ouvintes, 5 de 5 avisos dentro da estrutura (hóspede, governança e financeiro na confirmação; governança e financeiro no cancelamento) e nenhum fora dela. O serviço só chama `eventPublisher.publish` (`ReservationService.java:98`, `:126`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `EventListener<T extends Event>` (`event/EventListener.java:3-5`) tem uma implementação por interessado e por evento, entre elas a do hóspede (`notification/GuestNotificationListener.java:9`). O `EventPublisher` (`event/EventPublisher.java:8-24`) guarda a lista de ouvintes por tipo de evento e a percorre. O serviço recebe o publicador de fora (`ReservationService.java:24`) e não sabe quem é avisado. Os ouvintes são registrados em `Main.java:44-54`. Para pôr o restaurante ou o transfer basta uma classe nova e uma linha na montagem, e para tirar basta apagar a linha. As regras de cada destino ficam no ouvinte: "só confirmada avisa" (`HousekeepingCancellationListener.java:21`, `FinanceCancellationListener.java:23`) e "sem devolução, sem aviso ao financeiro" (`FinanceCancellationListener.java:29`). O serviço não pergunta qual é o caso. Há um ponto fraco no pattern. A publicação acontece depois do `save` e sem transação, e o publicador não isola os ouvintes (`EventPublisher.java:19-21`). Se a gravação de uma mensagem falhar, a reserva já está confirmada, os ouvintes seguintes não rodam e o handler devolve 500, porque `confirmReservation` só trata as exceções do domínio (`ReservationController.java:118-133`).

**O exagero.** Não há estrutura sem necessidade fora do pattern. A interface marcadora `Event` faz parte do publicador tipado. `NotificationService` é um repasse de uma linha para o repositório (`service/NotificationService.java:13-16`), mas fica no ponto onde entraria outro canal, e não chega a pesar.

**O resto do código:**
- **Duplicação.**
  - `getRoomType` pelo primeiro dígito está copiado três vezes: `ReservationController.java:262`, `service/ReportService.java:77` e `util/FormatUtil.java:59`. Um quarto novo, como o 401, sairia como SUITE.
  - O mapa do comprovante está copiado em `checkOut` e `getDocument` (`ReservationController.java:177-191` e `:218-230`).
  - O JSON de erro se repete umas 15 vezes no controller.
  - O total de quartos está fixo no código, como `"total,%d/5"` (`ReportService.java:38`).
- **Erros.**
  - O `catch (Exception e)` na criação (`ReservationController.java:97-102`) transforma qualquer falha em `400 INVALID_REQUEST`, inclusive a falha do banco, e expõe a mensagem interna. Foi assim que a recusa do CPF formatado e o `ClassCastException` do número do quarto apareceram como erro de quem chamou.
  - O `catch (IllegalArgumentException)` diz sempre "Invalid rate plan" (`:77-81`).
  - Os campos são lidos com cast cego (`(String) body.get(...)`, `((Number) body.get("guests")).intValue()`, `:25-31`).
  - Um id que não é número dá 500, porque não há tratamento para o `NumberFormatException` de `Long.parseLong` (`:107` e seguintes). Eu conferi: `GET /reservations/abc` → `500 Server Error`.
- **Tipos.**
  - O dinheiro é `BigDecimal` com `HALF_EVEN` (`util/MoneyUtil.java:8`), as datas são `LocalDate` e as situações e os planos são `enum`, tudo certo.
  - `BigDecimal.ZERO` sai sem escala, e por isso a reserva recém-criada mostra `"amountPaid":0` e `"discount":0` em vez de `0.00` (`ReservationService.java:75`, `PricingService.java:42`).
- **Estado.** `Reservation` é um saco de setters (`domain/Reservation.java`), e as transições ficam em `if`s no serviço, o que é legível. Mas a segunda via decide pelo valor pago, e não pela situação (ver Arquitetura).

## Testes

**Números:** 1 classe rodou, com 0 falhas, 1 erro e 0 pulados, e há 6 métodos de teste escritos. Cobertura de linhas 0 de 802 (0,0%) e de ramos 0 de 146 (0,0%) (`measures/tests.md`). O erro foi `SystemIT » IllegalState Could not find a valid Docker environment`.
**Nota:** ruim

O único arquivo de teste é `SystemIT.java`, que sobe um PostgreSQL por Testcontainers (`SystemIT.java:43-47`) e ignora o `DB_URL` que o texto diz que o sistema recebe. Sem Docker, a classe inteira cai antes do primeiro teste. Nenhuma regra foi executada aqui, e o `mvn verify` que o texto exige não passa num ambiente que só tem o banco das variáveis (o failsafe acusa o erro no `verify`, `measures/tests-maven.log:436-442`). Mesmo lidos, os testes cobrem pouco:

- **Têm valores:** o preço de alta temporada (599,76, `SystemIT.java:236`), o desconto NON_REFUNDABLE (116,97 e 662,83, `:261-263`), os três avisos da confirmação com assunto e destino (`:187-211`) e a devolução de um STANDARD cancelado (`:306`). O financeiro não ser avisado sem devolução também está testado (`:377-384`).
- **Asserções fracas:** do e-mail do hóspede só se confere que contém o nome e "Comprovante de reserva" (`:195-196`), sem o texto. Da governança, só "Entrada:" e "Hóspedes: 2" (`:203-204`), sem as datas. Do cancelamento, só que existem assuntos com "Reserva cancelada" e "Reembolso" (`:316-326`), sem o valor nem as datas.
- **Sem teste:** os códigos de erro (404, 409, 422, 400), as transições de situação (confirmar duas vezes, check-in, check-out, cancelar depois do check-in), os dois relatórios, a segunda via, o cancelamento de reserva pendente que não avisa ninguém e as reservas simultâneas.
- **A pergunta da mutação:** se alguém apagasse o `if (previousStatus != CONFIRMED) return` de `HousekeepingCancellationListener.java:21`, nenhum teste falharia. Trocar as datas do corpo da governança também passaria. O teste copia a montagem do `Main` (`SystemIT.java:58-109`), em vez de usá-la, e por isso tirar um ouvinte do `Main` também passaria.
- **Frágil:** os testes dependem de estado compartilhado, com a contagem de mensagens entre métodos (`:295`, `:358`), e usam uma porta fixa (`:52`).

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` nas 6 colunas e as datas em `date`. Acusações: a chave estrangeira `reservations(room_number) -> rooms` sem índice, nenhuma CHECK sobre situação, plano, tipo ou canal, nenhuma restrição UNIQUE/EXCLUDE, 0 transações explícitas e 0 `FOR UPDATE`. O esquema é criado por `CREATE TABLE` no código, sem migrações (`measures/database.md`).
**Nota:** ruim

- **A coluna do CPF é estreita demais.** `guest_cpf VARCHAR(11)` (`config/Database.java:49`) só aceita o CPF sem pontuação, e o texto mostra o CPF como "111.222.333-44". Esse CPF, enviado pela API, faz o `INSERT` falhar. Conferi à mão: `POST /reservations` com `"guestCpf":"111.222.333-44"` → `400 {"error":"INVALID_REQUEST","message":"Failed to insert reservation"}`, e com `"11122233344"` → 201. É um tipo errado que recusa dado válido e derruba a criação de reservas no roteiro (ver Comportamento).
- **Nada protege a regra do quarto.** "Um quarto não pode ter duas reservas na mesma noite" é conferido com um `SELECT COUNT(*)` (`repository/ReservationRepository.java:118-154`) seguido de um `INSERT` em outra conexão (`ReservationService.java:48-78`). Não há transação, trava nem restrição `EXCLUDE`, nem sequer um `synchronized`. Conferi à mão, com o formato do agente: 10 rodadas de dois pedidos simultâneos para o 301, e as 10 criaram as duas reservas (20 linhas para 10 datas). Depois de confirmar as duas, o relatório de ocupação mostrou o 301 duas vezes e `total,2/5`. Isso é dado errado no banco.
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` (`Database.java:34-72`) não sabe acrescentar uma coluna a uma tabela que já existe, e o próximo pedido vai precisar de uma migração feita à mão.
- **Escritas que deviam andar juntas.** A confirmação grava a reserva (`ReservationService.java:96`) e depois cada mensagem numa conexão própria (`MessageRepository.java:22`), sem transação. Uma falha no meio deixa a reserva confirmada com só parte dos avisos.
- **O que está certo:** o SQL é todo com `?`, sem SQL montado com `+`. As conexões são devolvidas por try-with-resources, sobre um pool Hikari (`Database.java:14-20`). Os dados sobrevivem ao reinício (o roteiro confirmou as mensagens, e eu confirmei a reserva e as mensagens depois de reiniciar).

## Comportamento

**Números:** 16 de 66 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 12 de 46 |
| depois de reiniciar | 1 de 3 |
| cenário 03 | 2 de 15 |
| reservas simultâneas | 1 de 2 (nenhuma criação nas 10 rodadas: 400 400) |

**Nota:** ruim

As falhas vêm quase todas de um ponto: o roteiro não conseguiu criar nenhuma reserva. Há duas causas, somadas.

1. **O número do quarto.** O roteiro manda `"roomNumber": 201` como número, e o handler faz `(String) body.get("roomNumber")` (`ReservationController.java:25`). O `ClassCastException` vira `400 INVALID_REQUEST` com a mensagem da JVM. O agente também devolve `number` como texto em `GET /rooms` ("101"), e por isso "o 201 comporta 3" falhou. O texto não diz o tipo, e sozinho este ponto seria uma escolha discutível. Mas recusar um número com um erro interno não é razoável.
2. **O CPF formatado.** O roteiro manda o CPF como o texto mostra ("111.222.333-44"), e isso falha sozinho, mesmo com o quarto como texto (conferi à mão, ver Banco). Para o dono da pousada, um site que mande o CPF com pontos não consegue fazer reserva nenhuma, e recebe "requisição inválida".

Com essas duas falhas, caem em cascata a confirmação, os e-mails, o check-in e o check-out, os cancelamentos, os relatórios e quase todo o cenário 03. As recusas que passaram (saída igual à entrada, nenhum hóspede, corpo que não é JSON, campos faltando, reserva inexistente, mês fora do formato) não dependem de criar reserva. "Cancelar pendente não manda nada" e "NON_REFUNDABLE não avisa o financeiro" passam sozinhas, porque nada foi confirmado.

Chamei a API à mão com o formato do agente (quarto como texto, CPF só com dígitos):

- **O que funciona:** os preços certos (779,80, e 37,48 com 212,42 no NON_REFUNDABLE de uma noite) e as recusas 404, 409 e 422. Os cinco avisos saem certos: o e-mail do hóspede igual ao do texto, a governança com "Reserva confirmada - quarto 201", "10/03/2027", "12/03/2027" e "Hóspedes: 2", o financeiro com "Pagamento recebido - reserva 1" e "R$ 779,80", e no cancelamento a governança com as datas liberadas e o financeiro com "Reembolso - reserva 1" e "R$ 779,80". O NON_REFUNDABLE cancelado conta no financeiro (`2,101,2029-03-10,2029-03-11,CANCELLED,212.42`).
- **O que não funciona:**
  - As duas reservas simultâneas são criadas, como relatado em Banco.
  - A segunda via de uma reserva **cancelada** com devolução responde 200 com o comprovante. O texto diz que ela só existe para reserva confirmada, com check-in ou com check-out (`ReservationController.java:210`).
  - `amountPaid`, `discount` e `refund` saem como `0` em vez de `0.00`.
  - Um id não numérico dá 500.

## Dúvidas

- O tipo do número do quarto. O texto não diz se `number` e `roomNumber` são número ou texto. Mesmo que se aceite o texto, a falha do CPF formatado basta para a criação cair no roteiro, e a nota de comportamento não muda.
- Os testes. A falha é do ambiente (falta Docker), e com Docker os seis testes talvez passassem. Dei `ruim` porque nada rodou, o projeto ignora o `DB_URL` que o texto manda usar, e mesmo os testes lidos deixam de fora a maior parte das regras (erros, transições, relatórios, segunda via, cancelamento pendente). Se o critério for só a qualidade do que está escrito, seria `com problemas`.
