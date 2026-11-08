# Revisão: construção, cenário 03

## Visão geral

O projeto tem 8 pacotes e 26 classes (cerca de 1.390 linhas em `src/main`): `routes` (os handlers do Javalin), `service` (`ReservationService`, `ReportService`), `notifications` (uma interface `NotificationHandler` e um ouvinte para cada interessado: hóspede, governança e financeiro), `repository` (JDBC com `PreparedStatement`), `database` (um pool HikariCP `static` e um `SchemaInitializer` que roda `CREATE TABLE IF NOT EXISTS`), `models` e `utils` (preço e formatação). Uma requisição entra pela rota, que lê o JSON à mão com `JsonNode`, chama o serviço, e o serviço chama os repositórios. Tudo fica no PostgreSQL, inclusive as mensagens enviadas. O mais sério: **o sistema não sobe**. As rotas usam a sintaxe `/reservations/:id` (`routes/ReservationRoutes.java:55`, `:66`, `:76`, `:86`, `:111`, `:137`), que o Javalin 6 recusa (`IllegalArgumentException: Path '/reservations/:id' invalid - Javalin 4 switched from ':param' to '{param}'`). O `Main` pega a exceção e chama `System.exit(1)` (`Main.java:34-36`). Por isso nem o script do banco nem o do comportamento conseguiram testar nada pela API. Mesmo com essa linha corrigida, há outros defeitos que quebram o sistema: o `DELETE FROM rooms` roda a cada partida e falha quando já existem reservas, e a criação de reserva responde 500 (veja "Dúvidas"). Os testes cobrem 5,8% das linhas.

## Arquitetura

**Números:** 8 pacotes, 16 dependências entre eles, 1 ciclo (`com.pousadamarealta` ↔ `com.pousadamarealta.routes`). O Javalin aparece só em `routes` e na raiz, o JDBC só em `repository` e `database`. `service` e `notifications` não usam Javalin nem JDBC. A maior classe é `ReservationService` (182 linhas).
**Nota:** com problemas

As camadas existem e apontam na direção certa: as rotas chamam o serviço (`routes/ReservationRoutes.java:43-46`), o serviço guarda as regras de situação, cancelamento e devolução sem `Context` e sem SQL (`service/ReservationService.java:84-158`), e o SQL fica nos repositórios (`repository/ReservationRepository.java`). Os defeitos:

- **Não há um lugar de montagem.** Cada classe de rotas cria os seus próprios repositórios e serviços (`routes/ReservationRoutes.java:21-28`, `routes/ReportRoutes.java:14-16`, `routes/MessageRoutes.java:9`, `routes/RoomRoutes.java:9`), e `ReservationService` cria o `NotificationService` por dentro (`service/ReservationService.java:30`). O resultado é duas instâncias de `ReservationRepository` e de `RoomRepository`.
- **O acesso ao banco passa por um `static` global** (`database/Database.java:14`, `:47-49`), que todos os repositórios chamam direto (`repository/ReservationRepository.java:25`).
- **O ciclo entre a raiz e `routes`.** `Main` depende de `routes` (`Main.java:22-25`), e as rotas dependem de `ErrorResponse`, que mora na raiz (`ErrorResponse.java:1`, `routes/ReservationRoutes.java:13`). É pequeno, mas mostra uma classe no pacote errado.
- **A borda HTTP monta o comprovante.** O mapeamento de reserva para comprovante, com a mesma chamada de construtor, aparece copiado em dois handlers (`routes/ReservationRoutes.java:91-103` e `:117-129`). E a regra "segunda via só para reserva paga" não existe em lugar nenhum: o `GET /document` devolve o comprovante de qualquer reserva (`routes/ReservationRoutes.java:111-131`).

## Design de baixo nível

**Números:** 3 interessados pedidos (hóspede, governança, financeiro) e os 3 ficam em ouvintes separados (`GuestNotificationHandler`, `GovernanceNotificationHandler`, `FinanceNotificationHandler`), tanto para a confirmação quanto para o cancelamento. Nenhum e-mail é montado no serviço. A lista de ouvintes, porém, é preenchida dentro do próprio `NotificationService`, e não na montagem.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O que aproxima do Observer:

- há uma interface de ouvinte com um método por evento (`notifications/NotificationHandler.java:6-9`), com uma implementação por interessado, e o hóspede também é ouvinte (`notifications/GuestNotificationHandler.java:10-19`);
- o `NotificationService` percorre a lista sem saber quem está nela (`notifications/NotificationService.java:24-34`);
- o `ReservationService` só anuncia o evento (`service/ReservationService.java:96`, `:153-155`) e não conhece destinatário, assunto nem texto;
- a regra "cancelamento de reserva pendente não avisa ninguém" fica no serviço (`:153`), e a regra "sem devolução, o financeiro não é avisado" fica no ouvinte do financeiro (`notifications/FinanceNotificationHandler.java:21`). As duas estão no lugar certo.

O que afasta: os ouvintes não são registrados na montagem do sistema. `NotificationService` registra os três no próprio construtor (`notifications/NotificationService.java:15-22`), e `ReservationService` faz `new NotificationService(...)` (`service/ReservationService.java:30`). Para pôr o restaurante ou o transfer, é preciso editar `NotificationService`. O `registerHandler` existe (`notifications/NotificationService.java:36-38`), mas ninguém o chama, e o `getNotificationService()` (`service/ReservationService.java:179-181`) também não é usado. Os ouvintes também recebem o `MessageRepository` como parâmetro de cada evento (`notifications/NotificationHandler.java:7-8`), o que amarra o evento ao lugar onde a mensagem é guardada. O ouvinte do hóspede implementa o cancelamento com um método vazio (`notifications/GuestNotificationHandler.java:22-24`), custo de uma interface com dois eventos. Mesmo assim, todos os casos estão na estrutura e nenhum é feito à mão, e por isso a classe é acerto.

**O exagero.** Não há estrutura sem necessidade fora do pattern: nem fábricas, nem builders, nem interfaces com uma implementação só. Os dois métodos sem uso citados acima são código morto, não camada.

**O resto do código** tem defeitos espalhados:

- **Erros codificados em texto.** O serviço lança `new Exception("ROOM_NOT_FOUND")`, `new Exception("CAPACITY_EXCEEDED")` e assim por diante (`service/ReservationService.java:46`, `:49`, `:56`, `:89`, `:171`, `:175`), e as rotas descobrem o código comparando `e.getMessage()` com textos (`routes/ReservationRoutes.java:149-172`). Quando a exceção não tem mensagem, `message.equals` dá `NullPointerException` dentro do próprio tratador (`:151`). Qualquer outra exceção, como um campo faltando (`body.get("roomNumber").asInt()` com `null`, `:34-41`), um JSON inválido, um `ratePlan` desconhecido ou uma data mal escrita, vira 500 em vez de 400 `INVALID_REQUEST`. A causa é engolida sem log (`:50-51`, `:169-171`). Nos relatórios é o contrário: qualquer exceção, até uma falha do banco, vira 400 (`routes/ReportRoutes.java:32-34`, `:52-54`).
- **O estado.** Todos os modelos têm campos públicos mutáveis (`models/Reservation.java:7-23`, `models/Room.java:6-9`, `models/Message.java:4-8`), e o serviço muda a reserva campo a campo (`service/ReservationService.java:64-79`, `:92-93`). O pool de conexões é `static` mutável (`database/Database.java:14`).
- **Os tipos.** O dinheiro é `BigDecimal` com `HALF_EVEN` a cada passo (`utils/PricingUtils.java:29-31`, `:51`, `:56`, `:62`), o que está certo. O tipo do quarto, porém, é `String` solto (`models/Room.java:7`, `models/Reservation.java:9`), e `amountPaid` e `discount` são `BigDecimal.ZERO` com escala 0 (`service/ReservationService.java:79`, `utils/PricingUtils.java:58`), o que sai no JSON como `0` e não `0.00`.
- **A formatação errada para o que o texto pede.** O `NumberFormat` de moeda pt-BR escreve `R$` seguido de um espaço não separável (U+00A0), não de um espaço comum (`utils/FormatterUtils.java:14`). O número do CSV usa `NumberFormat` de `Locale.US` com separador de milhar (`utils/FormatterUtils.java:15`, `:36-38`): um valor de 1.234,56 sai `1,234.56` e parte a coluna do CSV. O comprovante escreve `Quarto: 201` sem o tipo, `(DELUXE)` (`utils/FormatterUtils.java:53`).
- **A duplicação.** O mesmo `try { long id = Long.parseLong(...); ... } catch (Exception e) { handleError }` aparece em seis handlers (`routes/ReservationRoutes.java:55-145`), e a montagem do comprovante está copiada (`:91-103` e `:117-129`).

## Testes

**Números:** 9 testes rodaram, 0 falharam, 0 pulados; 9 métodos de teste escritos, em 2 arquivos. A cobertura é de 36 de 622 linhas (5,8%) e de 15 de 136 ramos (11,0%). Só o pacote `utils` tem cobertura (36 de 57 linhas); `service`, `notifications`, `repository` e `routes` têm 0. Não há nenhum teste pela API nem contra o banco.
**Nota:** ruim

O que está testado, com valores: a temporada alta e as bordas de 14/12 e 15/12 (`src/test/java/com/pousadamarealta/PricingUtilsTest.java:13-35`), o preço da noite com o arredondamento (299,88, `:37-45`), a soma das noites do exemplo do texto (599,76, `:47-56`) e a formatação de data e de CPF (`FormatterUtilsTest.java:10-29`).

O que não está testado:

- o desconto do NON_REFUNDABLE e o total (`utils/PricingUtils.java:54-63`);
- as situações da reserva e as transições recusadas;
- o cancelamento e a devolução;
- os códigos de erro;
- o comprovante e o e-mail do hóspede;
- os relatórios;
- toda a parte do cenário: nenhum teste confere que a confirmação gera os e-mails da governança e do financeiro, que o cancelamento de reserva confirmada avisa os dois, que o financeiro não é avisado sem devolução, nem que a reserva pendente cancelada não avisa ninguém.

A pergunta da mutação na parte nova não tem saída: trocar `> 0` por `>= 0` em `notifications/FinanceNotificationHandler.java:21`, apagar o `if (wasConfirmed)` em `service/ReservationService.java:153`, ou trocar o assunto em `notifications/GovernanceNotificationHandler.java:11`, e nada falharia. Os testes também não pegariam o defeito que derruba o sistema (as rotas com `:id`), porque nenhum sobe o Javalin.

## Banco

**Números:** o serviço **não subiu** contra o banco vazio (o mesmo `IllegalArgumentException` de `routes/ReservationRoutes.java:55`), mas as 3 tabelas já tinham sido criadas antes da queda (`messages`, `reservations`, `rooms`). As conferências encontraram:

- chave primária em todas as tabelas;
- dinheiro em `numeric(10,2)` em todas as 6 colunas;
- a chave estrangeira `reservations.room_number` coberta por índice;
- datas em `date`;
- 0 restrições que protegem regra (nenhum CHECK, UNIQUE ou EXCLUDE);
- 0 transações explícitas e 0 `FOR UPDATE`;
- 1 SQL montado com `+` (`repository/RoomRepository.java:30`).

**Nota:** ruim

Os tipos estão certos e o SQL dos repositórios usa parâmetros (`repository/ReservationRepository.java:25-41`). O que pesa:

- **O serviço não sobe contra um banco vazio**, que é o que o texto pede e o que o script mediu.
- **A segunda partida também falharia, mesmo com as rotas corrigidas.** `SchemaInitializer` roda `DELETE FROM rooms` e reinsere os quartos toda vez que o sistema sobe (`database/SchemaInitializer.java:61-73`). Como `reservations.room_number` referencia `rooms(number)` sem `ON DELETE` (`:22`), o `DELETE` é recusado assim que existe uma reserva. A partida cai em `System.exit(1)` (`Main.java:34-36`), e o sistema fica fora do ar depois do primeiro reinício com dados.
- **Nada impede duas reservas na mesma noite.** O serviço consulta os conflitos e depois insere (`service/ReservationService.java:52-57` e `:81`), em autocommit (`database/Database.java:33`), sem trava, sem transação e sem restrição `EXCLUDE`. O índice parcial (`database/SchemaInitializer.java:51-55`) só acelera a consulta. Dois pedidos simultâneos podem criar as duas reservas.
- **As escritas que deviam andar juntas estão separadas.** A confirmação grava a reserva (`service/ReservationService.java:95`) e depois cada mensagem numa conexão separada (`repository/MessageRepository.java:18`). Uma falha no meio deixa a reserva confirmada sem os avisos.
- **O esquema é criado no código**, com `CREATE TABLE IF NOT EXISTS` e sem migrações (`database/SchemaInitializer.java:10-49`). Não há como acrescentar uma coluna numa tabela que já existe.
- **Não há CHECK** em `status`, `rate_plan` nem `guests`.
- **O SQL concatenado** em `repository/RoomRepository.java:30` recebe um `int` e não abre injeção, mas foge do padrão do resto.

## Comportamento

**Números:** 0 conferências passaram e 0 falharam: o roteiro não conseguiu testar, porque o serviço não subiu (`measures/behavior.md`: `Path '/reservations/:id' invalid`, em `routes/ReservationRoutes.java:55`, chamado de `Main.java:23`).
**Nota:** ruim

Para o dono da pousada, o resultado é um sistema que não abre. O `java -jar` termina com erro antes de atender qualquer pedido. O site não cria reservas, a recepção não faz check-in, o contador não tira relatório, e nenhum aviso chega à governança ou ao financeiro. O defeito é a sintaxe de rota de uma versão antiga do Javalin, num projeto em que o texto fixou o Javalin 6.7.0. Ele apareceria na primeira vez que alguém subisse o jar.

## Dúvidas

- **O que viria depois da queda.** Para saber o que viria em seguida, subi uma cópia descartável fora de `projeto/`, trocando só `:id` por `{id}`. Nela, `GET /rooms` respondeu certo. `POST /reservations` com um corpo válido respondeu 500, mas gravou a reserva; a causa provável é o `LocalDate` sem o módulo `jackson-datatype-jsr310` no `pom.xml`. A confirmação também respondeu 500, mas gravou os três e-mails, com assuntos e destinatários certos. O e-mail do hóspede saiu com `Quarto: 201` sem `(DELUXE)` e com `R$` seguido de espaço não separável. Um corpo sem campos ou que não é JSON deu 500, e não 400. O log não mostrou a causa dos 500, porque `handleError` engole a exceção. Isso não entrou nas notas além do que o código já mostra, e não confirmei a causa do 500 da criação.
- **A classe do pattern.** Ficou entre `acerto` e `malfeito`. Todos os interessados são ouvintes e o serviço não monta e-mail nenhum, mas a lista é fixa dentro de `NotificationService` (`notifications/NotificationService.java:18-22`) e não é recebida da montagem. Dei `acerto` porque nenhum caso fica fora da estrutura. Quem ler o critério "a lista que ele recebe de fora" com rigor pode preferir `malfeito`.
