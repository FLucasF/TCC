# Revisão: construção, cenário 03

## Visão geral

O projeto é um serviço Javalin em sete pacotes: `api` (handlers estáticos e DTOs), `service` (preço, comprovante, relatórios e as transições da reserva), `notification` (os ouvintes da confirmação e do cancelamento), `repository` (JDBC puro com `PreparedStatement`), `persistence` (pool Hikari e um `Migration` com `CREATE TABLE IF NOT EXISTS`) e `domain`. Uma requisição entra numa rota de `App.java:39-53`, passa por um método estático de `ReservationController`, que chama `ReservationService` (também estático), que lê e grava pelos repositórios estáticos em três tabelas do PostgreSQL (`rooms`, `reservations`, `messages`). A parte do cenário (avisar a governança e o financeiro) foi feita com um Observer de verdade: uma interface e um ouvinte por interessado. O problema mais sério está no banco: `guest_cpf` é `VARCHAR(11)` (`src/main/java/com/pousada/marealta/persistence/Migration.java:22`). Com o CPF escrito como no enunciado (`111.222.333-44`), toda criação de reserva dá 500, e o roteiro de comportamento passou só 19 de 66 conferências. Com o CPF só em dígitos, o sistema anda, mas ainda erra regras do texto: dezembro inteiro é cobrado como alta temporada, o check-out não devolve o comprovante, o relatório de ocupação conta reservas pendentes, o financeiro ignora a NON_REFUNDABLE cancelada e o dinheiro sai sem separador de milhar. Fora isso, nada impede duas reservas do mesmo quarto ao mesmo tempo, e só há 6 testes, todos do cálculo de preço.

## Arquitetura

**Números:** 7 pacotes, 13 dependências entre eles, 1 ciclo (`service` ↔ `notification`), segundo `measures/dependencies.md`. O Javalin só aparece em `com.pousada.marealta` e `api`; o JDBC em `persistence`, `repository`, `service` e `notification` (nos dois últimos só pelo tipo `SQLException`). A maior classe é `ReservationController`, com 212 linhas.
**Nota:** com problemas

As camadas existem e as regras não conhecem o Javalin: `ReservationService` não recebe `Context`, e o SQL fica todo em `repository/`. Mas há misturas localizadas:

- **Ciclo `service` ↔ `notification`.** O serviço chama `NotificationService` (`service/ReservationService.java:65`, `:128`), e os ouvintes voltam a `service.ReceiptFormatter` para formatar dinheiro e datas (`notification/FinanceNotificationObserver.java:6`, `:20`; `notification/GovernanceNotificationObserver.java:5`, `:18`). O formatador está no pacote errado; o certo seria um pacote neutro ou o próprio `notification`.
- **A borda HTTP vai direto aos dados e decide regras.** `ReservationController` chama `ReservationRepository.findById` (`api/ReservationController.java:64`, `:81`, `:151`) e `RoomRepository.findByNumber` (`:163`). O controller decide quem tem segunda via (`:158`, pelo `amountPaid > 0`, e não pela situação da reserva) e monta o comprovante por conta própria (`:165-178`). Também é ele que escolhe o valor pago na confirmação: passa `reservation.getTotal()` ao serviço (`:89`).
- **Não há montagem.** Tudo é `static`: repositórios, serviços, controllers e o `Database` global (`persistence/Database.java:15`, `App.java:23`). Os ouvintes são criados num bloco `static` dentro de `NotificationService` (`notification/NotificationService.java:11-15`), e não no `main`. Nenhuma peça recebe as suas dependências de fora.

Num sistema deste tamanho isso ainda é aceitável, mas são mais que um ou dois deslizes. Por isso a nota não fica em `bom`.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança, financeiro) e 2 eventos (confirmação, cancelamento). Todos os 3 estão na estrutura de ouvintes, e não há nenhum e-mail montado fora dela. 1 interface (`ReservationObserver`) e 3 implementações. 2 classes mortas: `domain/Receipt.java` e `RoomRepository.findAllAsMap`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `ReservationObserver` (`notification/ReservationObserver.java:5-8`) tem `onReservationConfirmed` e `onReservationCancelled`, e cada interessado é uma classe: `GuestNotificationObserver`, `GovernanceNotificationObserver` e `FinanceNotificationObserver`. O e-mail do hóspede também é um ouvinte (`notification/GuestNotificationObserver.java:16-31`), e não uma chamada direta. `ReservationService` só anuncia o evento (`service/ReservationService.java:65`, `:128`) e não sabe quem é avisado nem monta texto de e-mail. A regra "sem devolução, o financeiro não é avisado" fica dentro do próprio ouvinte (`notification/FinanceNotificationObserver.java:35-38`). Para pôr o restaurante ou o transfer, basta uma classe nova, e `registerObserver`/`unregisterObserver` existem (`notification/NotificationService.java:29-35`).

O que afasta do ideal da referência: a lista não é recebida de fora nem registrada na montagem. Ela é uma `static ArrayList` mutável, preenchida num bloco `static` (`notification/NotificationService.java:9-15`), e o serviço chama esse sujeito global por método estático. Trocar quem é avisado ainda exige editar `NotificationService`, e a lista não é segura entre threads se alguém registrar com o sistema rodando. Também falta proteção entre ouvintes: cada um engole a própria `SQLException` (`FinanceNotificationObserver.java:26`), então uma falha não derruba os outros, mas o aviso some sem ninguém saber, e o e-mail não é gravado na mesma transação que a confirmação (`ReservationService.java:63-65`). Mesmo assim, a estrutura cobre todos os casos e o serviço não pergunta qual é o caso. Por isso: acerto.

**Exagero:** não há estrutura sem necessidade. `Receipt` (`domain/Receipt.java:19`) e `findAllAsMap` (`repository/RoomRepository.java:52`) são código morto, não camadas. Esse é um defeito de limpeza.

**O resto do código:**

- **Dinheiro como `double` no JSON.** O `BigDecimal` vira `double` na resposta (`api/ReservationConverter.java:24-29`, `api/ReservationController.java:175-177`), e por isso 779,80 sai `779.8` e 0,00 sai `0.0`. O desconto também é um `double` no enum (`domain/RatePlan.java:4-5`), convertido por `BigDecimal.valueOf(double)` (`service/PricingCalculator.java:48`).
- **Formato do dinheiro.** O padrão `"0.00"` (`service/ReceiptFormatter.java:20`) não tem separador de milhar: sai `R$ 1739,70`, e não `R$ 1.739,70` como o texto pede.
- **Regra da temporada.** `isPeakSeason` trata dezembro inteiro como alta temporada (`service/PricingCalculator.java:17`); o texto diz "de 15 de dezembro". O parâmetro `dayOfMonth` é calculado e nunca usado (`:14`).
- **Erros.** Os erros são texto: códigos passados na mensagem de `IllegalArgumentException` e decodificados num `switch` de strings (`api/ReservationController.java:195-211`). Há `catch (Exception)` → 500 em todos os handlers (`:55`, `:72`, `:94`...), e isso esconde um corpo que não é JSON (vira 500) e um CPF que não cabe na coluna (vira 500).
- **Estado.** Há `static` mutável global (`Database.dataSource`, a lista de ouvintes), e `Reservation` tem setters livres para situação, valor pago e devolução (`domain/Reservation.java:50-74`), sem que a transição more no objeto.
- **Duplicação.** São 6 `DateTimeFormatter.ofPattern` espalhados, o `parseDate` repetido em dois controllers, a lista de colunas do `SELECT` repetida em `ReservationRepository` (`:41`, `:76`, `:102`) e o tipo do quarto como `String` em vez de enum.

## Testes

**Números:** 6 testes rodaram, 0 falharam; 6 métodos escritos, num só arquivo. Cobertura de 25 de 829 linhas (3,0%) e 14 de 191 ramos (7,3%), segundo `measures/tests.md`. Só o pacote `service` tem cobertura (25 de 177 linhas). Não há teste pela API nem contra o banco.
**Nota:** ruim

O único arquivo, `src/test/java/com/pousada/marealta/service/PricingCalculatorTest.java`, confere com valores o preço da noite na alta temporada (`:28-29`, 299,88), a soma (`:40`, 599,76), o desconto de 15% (`:51`, 89,96) e o total (`:60`). É pouco, e não pega o erro que existe:

- **A temporada.** Testa 20/12 (`:16`), mas nenhum dia entre 1 e 14 de dezembro, e por isso não percebe que `PricingCalculator.java:17` cobra 20% a mais no mês inteiro.
- **A parte nova do cenário não tem teste nenhum.** Nenhum teste confere os ouvintes, os assuntos, os destinatários ou a regra "sem devolução, sem aviso ao financeiro". Se alguém apagasse `observers.add(new FinanceNotificationObserver())` (`NotificationService.java:14`), ou trocasse `<= 0` por `< 0` em `FinanceNotificationObserver.java:35`, nada falharia.
- **Nada de situações da reserva, cancelamento e devolução, códigos de erro, relatórios, comprovante ou formato do dinheiro.** Um teste qualquer de criação pela API teria mostrado que o CPF do enunciado derruba o sistema.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` nas 6 colunas e as datas em `date`. As conferências de `measures/database.md` acusaram:

- 1 chave estrangeira sem índice: `reservations(room_number)`;
- 4 colunas de situação ou tipo sem CHECK;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- 0 transações e 0 `FOR UPDATE`;
- 1 SQL montado com `+`.

O esquema é criado no código Java, sem migrações.
**Nota:** ruim

- **A coluna do CPF quebra o sistema.** `guest_cpf VARCHAR(11)` (`src/main/java/com/pousada/marealta/persistence/Migration.java:22`) só aceita o CPF sem pontuação, e nada valida ou normaliza a entrada antes. Com o CPF escrito como o dono escreve no enunciado (`111.222.333-44`), o `INSERT` falha com "value too long for type character varying(11)" (`measures/behavior-service.log`, linha 25, a partir de `ReservationRepository.java:36`). Nenhuma reserva é gravada.
- **Nada impede a reserva dupla.** A disponibilidade é conferida noite a noite, com um `SELECT` por noite (`service/ReservationService.java:134-144`), e depois vem o `INSERT` (`:48`). Isso acontece sem transação, sem trava e sem restrição no banco (nenhum EXCLUDE ou UNIQUE). Dois pedidos simultâneos podem passar os dois pela conferência.
- **Escritas que deviam andar juntas, separadas.** A confirmação grava a reserva (`ReservationService.java:63`), e as mensagens são gravadas depois, cada uma na sua conexão e com a falha engolida. Uma reserva pode ficar confirmada sem os avisos.
- **O esquema não evolui.** É `CREATE TABLE IF NOT EXISTS` em `Migration.java:10`, `:17`, `:36`: a primeira coluna nova vai exigir mexer no banco à mão.
- **Faltam índice e restrições.** A chave estrangeira não tem índice (`Migration.java:19`), e não há CHECK para situação, plano ou quantidade de hóspedes.
- **SQL montado com `+`.** `repository/RoomRepository.java:39` concatena o número do quarto. Como ele é um `int` lido pelo Jackson, não há injeção, mas o resto do código usa `?`, e essa linha devia usar também.
- **O que está certo:** o pool Hikari (`persistence/Database.java:20-30`), try-with-resources em todas as conexões e `PreparedStatement` com parâmetros no resto.

## Comportamento

**Números:** 19 de 66 conferências passaram (`measures/behavior.md`):

- parte comum: 15 de 46;
- depois de reiniciar: 1 de 3;
- cenário 03: 2 de 15;
- reservas simultâneas: 1 de 2 (nas 10 rodadas, as duas chamadas deram 500).
**Nota:** ruim

**A falha que derruba quase tudo:** `POST /reservations` com o CPF `111.222.333-44` responde 500 INTERNAL_ERROR, por causa da coluna `VARCHAR(11)` (veja a seção Banco). Daí em diante, confirmação, e-mails, check-in, check-out, cancelamentos, relatórios e os avisos do cenário falham em cascata, com 404 ou vazio. As poucas conferências que passaram não dependem de haver uma reserva: quartos, recusas por capacidade, quarto inexistente, datas, campos faltando, mês inválido, e "cancelar pendente não manda nada". Para o dono da pousada, o site não consegue criar nenhuma reserva com o CPF no formato em que ele mesmo escreveu.

Para separar esse defeito do resto, subi o jar à mão com o CPF só em dígitos (`11122233344`). A parte do cenário funciona: na confirmação saem os três e-mails, para o hóspede, para `governanca@...` ("Reserva confirmada - quarto 201", com 10/03/2027, 12/03/2027 e 2 hóspedes) e para `financeiro@...` ("Pagamento recebido - reserva 1", R$ 779,80). No cancelamento da confirmada STANDARD saem a governança e o "Reembolso - reserva 1"; na NON_REFUNDABLE, só a governança. Mesmo assim, encontrei estes erros contra o texto:

- **Preço.** Uma noite de 01/12/2027 no STANDARD custou 299,88, e não 249,90: dezembro inteiro é cobrado como alta temporada (`service/PricingCalculator.java:17`). O hóspede paga 20% a mais de 1 a 14 de dezembro.
- **Check-out.** Devolve a reserva, e não o comprovante (`api/ReservationController.java:121-122`).
- **Segunda via.** Sai para uma reserva cancelada que tinha sido paga, porque a regra é `amountPaid > 0` (`:158`). Para uma pendente, a resposta é 404 RECEIPT_NOT_FOUND, um código que o texto não prevê (o roteiro esperava 409 OPERATION_NOT_ALLOWED).
- **Ocupação.** O relatório conta reservas PENDING: a pendente de 01/12 apareceu com `total,1/5`. A consulta só exclui CANCELLED (`repository/ReservationRepository.java:79`, `service/ReportService.java:31`, `:41`).
- **Financeiro.** O relatório ignora a NON_REFUNDABLE cancelada, que deveria continuar contando: em março, deu `total,,,,0.00` com R$ 1.478,74 retidos. A consulta pega só CONFIRMED, CHECKED_IN e CHECKED_OUT (`repository/ReservationRepository.java:105`). A linha de total também sai `total,,,,0.00`, e não `total,599.76`.
- **Formato do dinheiro.** No JSON sai `779.8` e `0.0`, sem 2 casas. Nos e-mails sai `R$ 1739,70`, sem separador de milhar.
- **Corpo que não é JSON.** Responde 500, e não 400 INVALID_REQUEST.

Depois de reiniciar, as mensagens continuam (o banco guarda), mas as reservas não puderam ser conferidas porque nunca foram criadas. As reservas simultâneas também não puderam ser medidas, pelo mesmo motivo. Pela leitura do código, nada as impede (veja a seção Banco).

## Dúvidas

- **A classe do pattern.** Entre `acerto` e `malfeito`: os ouvintes estão completos e o serviço não conhece os interessados, mas a lista é um `static` preenchido dentro de `NotificationService` (`notification/NotificationService.java:11-15`), e não recebida de fora nem registrada no `main`, como a referência descreve. Dei `acerto` porque nenhum dos casos de `malfeito` da referência acontece (todos os interessados e os dois eventos estão na estrutura, sem e-mail montado à mão). Ainda assim, a ligação não está na montagem.
- **O formato do CPF.** O texto não diz em que formato o CPF chega no `POST`; só mostra `111.222.333-44` no comprovante. Guardar só 11 dígitos é uma escolha defensável, mas sem validar nem normalizar a entrada ela vira 500. Por isso tratei como erro do agente.
- **Reservas simultâneas.** O roteiro não conseguiu medi-las, porque toda criação deu 500. A falta de proteção vem só da leitura do código.
