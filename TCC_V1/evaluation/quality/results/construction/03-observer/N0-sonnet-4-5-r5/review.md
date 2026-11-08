# Revisão: construção, cenário 03

## Visão geral

O agente montou um serviço Javalin em camadas: `controller` (os handlers) → `service` (as regras) → `repository` (JDBC puro com `DriverManager`) → PostgreSQL. O esquema é criado com `CREATE TABLE IF NOT EXISTS` em `database/Database.java`, e tudo é ligado à mão em `Main.java`. Os avisos da confirmação e do cancelamento foram feitos como Observer: há um `EventPublisher`, dois eventos e cinco ouvintes registrados em `Main.java:48-58`. A estrutura é boa, mas o sistema não funciona. **Nenhuma reserva pode ser criada.** A consulta de disponibilidade usa `? IS NULL` com `setObject(4, null)` (`ReservationRepository.java:115,124-125`), e o PostgreSQL recusa com "could not determine data type of parameter $4". Toda chamada a `POST /reservations` responde 500 (`measures/behavior-service.log:20-65`). Mesmo sem esse erro, a coluna `guest_cpf VARCHAR(11)` (`Database.java:42`) recusa o CPF no formato `111.222.333-44` que o texto mostra ("value too long for type character varying(11)", conferido com `psql` no esquema criado). Os testes não chegam a pegar nada disso: são 6 testes de preço e um placeholder `assertTrue(true)`, com 4,8% de cobertura de linhas.

## Arquitetura

**Números:** 9 pacotes, 21 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece em `com.pousadamarealta` e em `controller`, e o JDBC só em `database` e `repository`. A maior classe é `ReservationRepository`, com 205 linhas.
**Nota:** com problemas

As dependências seguem o sentido certo: a borda chama as regras, e as regras chamam os dados. Os serviços não conhecem nem `Context` nem SQL (`service/ReservationService.java`), e a montagem fica num lugar só (`Main.java:35-71`). As misturas são localizadas, mas existem:
- **Regra no handler:** a regra "a segunda via só existe para reserva paga" está no controller, decidida por `amountPaid == 0` (`controller/ReservationController.java:85-87`), e não no serviço. A regra também está errada: uma STANDARD confirmada e depois cancelada fica com `amountPaid > 0` e ganharia segunda via.
- **Comprovante no handler:** o comprovante da API é montado no controller (`ReservationController.java:113-128`), e o tipo do quarto é deduzido ali pelo primeiro dígito do número (`ReservationController.java:130-134`). O `Room` já tem o tipo.
- **Handler sem serviço:** `MessageController` vai direto ao `MessageRepository` (`controller/MessageController.java:12,19`); é o único handler que pula o serviço.
- **Tratamento de erro genérico demais:** `Main.java:93-95` transforma qualquer `NumberFormatException` em `RESERVATION_NOT_FOUND`, venha de onde vier.

## Design de baixo nível

**Números:** os 5 casos de aviso estão na estrutura e nenhum ficou fora dela. Na confirmação são 3 ouvintes: hóspede, governança e financeiro (`Main.java:48-53`). No cancelamento são 2: governança e financeiro (`Main.java:55-58`). O `ReservationService` só chama `eventPublisher.publish` (`ReservationService.java:83,108`) e não monta nenhum e-mail.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A interface de ouvinte é `event/EventListener.java:3-5`. O publicador guarda as listas por tipo de evento (`event/EventPublisher.java:9-23`) e o serviço o recebe de fora (`ReservationService.java:24-31`). Cada interessado é uma classe própria em `event/listeners/`, inclusive o e-mail do hóspede (`GuestNotificationListener.java`). O ouvinte decide se o evento lhe interessa: o cancelamento de reserva pendente é ignorado em `HousekeepingCancellationListener.java:21` e `FinanceCancellationListener.java:28`, e o reembolso zero em `FinanceCancellationListener.java:34`. Um restaurante ou um transfer entra com mais uma classe e uma linha no `Main`, sem mexer no serviço. A única coisa que afasta um pouco: o evento de cancelamento carrega a flag `wasConfirmed` (`ReservationCancelledEvent.java:6`) e cada ouvinte a confere, em vez de haver um evento só para o cancelamento de reserva confirmada. Não chega a ser defeito.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface com uma implementação só, nem fábrica, nem builder. `RoomService` e `NotificationService` são finos, mas têm papel (o 404 do quarto, a mensagem com o canal). O enum `domain/RoomNumber.java` não é usado em lugar nenhum. É código morto, não estrutura.

**O resto do código:**
- **O tipo do quarto deduzido do número:** o mesmo `startsWith("1") ? STANDARD : startsWith("2") ? DELUXE : SUITE` aparece em três lugares (`ReservationController.java:130-134`, `service/ReportService.java:73-77`, `GuestNotificationListener.java:52-53`), quando `Room.getType()` já existe. Um quarto novo, um 103 DELUXE por exemplo, sairia com o tipo errado em todos eles.
- **O formatador de dinheiro:** é recriado igual em três ouvintes (`FinanceCancellationListener.java:20-23`, `FinanceNotificationListener.java:19-22`, `GuestNotificationListener.java:21-24`).
- **O comprovante:** existe em duas versões que podem divergir, a do e-mail (`GuestNotificationListener.java:46-66`) e a da API (`ReservationController.java:113-128`).
- **`formatCpf`:** recorta com `substring` fixo (`GuestNotificationListener.java:68-71`). Com um CPF já formatado ele embaralha o texto, e com um CPF de menos de 11 caracteres lança `StringIndexOutOfBoundsException` depois de a confirmação já ter sido gravada (`ReservationService.java:81-83`).
- **O lado bom:** `BigDecimal` com `HALF_EVEN` em cada passo (`PricingService.java:20-21,27,32-33`, `ReservationService.java:61`), `LocalDate` e `enum` para situação e plano.
- **`Reservation`:** é um saco de setters (`domain/Reservation.java`). As transições ficam no serviço, o que é aceitável neste tamanho.

## Testes

**Números:** 7 testes rodaram, com 0 falhas e 0 pulados, e há 7 métodos de teste no código. A cobertura é de 38 de 785 linhas (4,8%) e 14 de 120 ramos (11,7%). Os pacotes `event`, `event.listeners`, `controller`, `repository` e `database` ficaram com 0% (`measures/tests.md`).
**Nota:** ruim

Só o preço é testado, e com valores. O teste da alta temporada espera 599.76 (`src/test/java/com/pousadamarealta/PricingServiceTest.java:36`), a borda de 15/12 espera 467.88 (`PricingServiceTest.java:65`), janeiro espera 695.88 (`PricingServiceTest.java:78`) e o desconto de 15% espera 75.00 (`PricingServiceTest.java:44`). Três desses testes calculam uma variável `expected` que nunca usam (`PricingServiceTest.java:33-34,62-63,75-76`). O único teste de integração é um placeholder `assertTrue(true, "Placeholder test")` (`src/test/java/com/pousadamarealta/ReservationIT.java:9-11`).

Ficou sem nenhum teste:
- a máquina de situações e as recusas (os códigos de erro);
- o cancelamento e a devolução;
- os relatórios e o comprovante;
- a parte nova do cenário, os avisos.

A pergunta da mutação na parte nova: apagar a linha `if (!event.wasConfirmed()) return;` de `HousekeepingCancellationListener.java:21`, trocar o destinatário do financeiro, ou tirar um `subscribe` de `Main.java:48-58` não faria nenhum teste falhar. Nenhum teste chama a API nem o banco, por isso o erro que impede criar qualquer reserva passou pelo `mvn verify`.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram (`measures/database.md`):
- uma chave estrangeira sem índice, `reservations(room_number) -> rooms`;
- nenhum CHECK nas colunas de situação, plano, tipo e canal;
- nenhuma restrição UNIQUE ou EXCLUDE;
- nenhum pool de conexões e nenhuma transação explícita.

O esquema é criado no código Java, sem migrações.
**Nota:** ruim

- **O erro que impede gravar:** a consulta de disponibilidade usa `AND (? IS NULL OR id != ?)` com `stmt.setObject(4, excludeId)` e `excludeId` nulo (`repository/ReservationRepository.java:115,124-125`). O driver manda o parâmetro sem tipo, e o PostgreSQL recusa ("could not determine data type of parameter $4", `measures/behavior-service.log:65`). É a consulta que roda antes de todo `INSERT` (`ReservationService.java:149`), então nenhuma reserva chega ao banco.
- **O CPF não cabe:** `guest_cpf VARCHAR(11)` (`database/Database.java:42`) não guarda o CPF no formato `111.222.333-44`, o que o texto usa. Conferi com um `INSERT` direto no esquema criado: "value too long for type character varying(11)".
- **O esquema não evolui:** `CREATE TABLE IF NOT EXISTS` em `Database.java:27-65` não sabe acrescentar uma coluna numa tabela que já existe.
- **Nenhuma proteção contra duas reservas na mesma noite:** a verificação e o `INSERT` usam conexões separadas e sem transação (`ReservationService.java:149`, `ReservationRepository.java:40-46`), e não há restrição `EXCLUDE` nem `FOR UPDATE`.
- **O lado bom:** o SQL usa sempre parâmetros `?`, e as conexões são fechadas com try-with-resources. Fogem disso o `Statement` e o `ResultSet` de `Database.java:75`, que ficam abertos até a conexão fechar.

## Comportamento

**Números:** 16 de 66 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 12 de 46 |
| depois de reiniciar | 1 de 3 |
| cenário 03 | 2 de 15 |
| reservas simultâneas | 1 de 2 |

**Nota:** ruim

A primeira criação de reserva já dá 500 ("criar uma reserva responde 201: veio 500"). A causa é o erro de SQL descrito no Banco, conferido no log e repetido à mão com `curl` usando um CPF só com dígitos. Quase todo o resto falha em cascata, porque não existe reserva nenhuma:
- confirmar, check-in, check-out e segunda via dão 404;
- os cancelamentos e as devoluções não acontecem;
- os relatórios vêm vazios (`total,0/5` e `total,0`);
- nenhum e-mail sai, nem o do hóspede, nem os da governança e do financeiro;
- as 10 rodadas simultâneas dão 500 nos dois pedidos.

As poucas conferências que passam não dependem de uma reserva criada. São as recusas que acontecem antes da consulta quebrada (capacidade 422, quarto inexistente 404, datas e hóspedes 400) e os dois "não manda nada" do cenário, que passam porque nada é mandado.

Falha que vem do código e não da cascata: um corpo que não é JSON dá 500 em vez de 400. O `ctx.bodyAsClass` não é tratado (`ReservationController.java:30,46`).

Falha que não pesa: "o 201 comporta 3" e "a diária do 201" falham porque o número do quarto sai como texto (`"number":"201"`). O texto não fixa o tipo, e capacidade e diária estão certas no JSON.

Para o dono da pousada: o site não consegue fazer nenhuma reserva. Os avisos à equipe, que estão bem desenhados no código, nunca chegam a ser mandados.

## Dúvidas

- **O formato do CPF:** o texto não diz se o CPF chega com pontos e traço ou só com dígitos. A coluna `VARCHAR(11)` e o `formatCpf` mostram que o agente supôs só dígitos. Com o CPF formatado, que é o do exemplo do comprovante, o `INSERT` também falharia. Isso pesa pouco perto do erro de SQL, que quebra a criação em qualquer formato.
- **O conteúdo dos avisos:** como nenhuma reserva foi criada, o roteiro não pôde conferir os e-mails da governança e do financeiro. Pela leitura, assuntos, destinatários e corpos batem com o texto (`HousekeepingNotificationListener.java:25-32`, `FinanceNotificationListener.java:27-30`, `HousekeepingCancellationListener.java:27-32`, `FinanceCancellationListener.java:38-41`), mas não vi isso funcionar.
