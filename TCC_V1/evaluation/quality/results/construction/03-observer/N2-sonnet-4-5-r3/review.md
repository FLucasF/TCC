# Revisão: construção, cenário 03

## Visão geral

O projeto tem sete pacotes: `api` (o `ApiServer` do Javalin e os DTOs), `service` (`ReservationService`, `ReportService`, `MessageService`), `domain` (a `Reservation` com o preço e as transições, os enums e os records), `notification` (a interface `ReservationEventListener` e um ouvinte para cada interessado: hóspede, governança, financeiro), `repository` (JDBC puro, com um `Database` que cria o esquema) e `util` (a formatação). Uma requisição entra no handler do `ApiServer`, que chama o serviço. O serviço usa a `Reservation` para as regras, grava pelo repositório e avisa a lista de ouvintes que recebe do `Main`. Os dados ficam no PostgreSQL, em três tabelas criadas com `CREATE TABLE IF NOT EXISTS` no código. O Observer pedido está bem feito. O problema mais sério está no banco e no comportamento. A coluna `guest_cpf` é `VARCHAR(11)` (`repository/Database.java:41`), e o `catch (Exception)` do handler troca o erro do banco por um 400 (`api/ApiServer.java:127-129`). Assim, um CPF no formato que o próprio texto mostra ("111.222.333-44") recusa a reserva: no roteiro, só 20 de 66 conferências passaram. Mesmo com um CPF só de dígitos, nada impede duas reservas do mesmo quarto ao mesmo tempo: no teste manual, 5 de 5 pares simultâneos foram os dois criados. Além disso, o dinheiro sai no JSON como `779.8` e o relatório financeiro lista cancelamentos que não deixaram dinheiro. Os testes de integração estão desligados no `pom.xml`.

## Arquitetura

**Números:** 7 pacotes, 15 dependências entre eles, 1 ciclo (`service` ↔ `notification`). O Javalin aparece só em `api` e na raiz, e o JDBC só em `repository`. As maiores classes são `PostgresReservationRepository` (279 linhas) e `ApiServer` (267 linhas).
**Nota:** com problemas

A divisão em camadas existe e quase sempre é respeitada:
- As regras do domínio não conhecem Javalin nem SQL (`domain/Reservation.java:93-155`).
- O acesso ao banco fica isolado em `repository/`.
- A montagem é feita num lugar só (`Main.java:22-51`), inclusive a lista de ouvintes (`Main.java:30-34`).

Os defeitos são localizados:
- **Um ciclo novo entre dois pacotes.** A interface do ouvinte mora em `notification` e o serviço depende dela (`service/ReservationService.java:4`). Os ouvintes, por sua vez, dependem do `MessageService`, que está em `service` (`notification/FinanceNotificationListener.java:6`).
- **Uma regra de negócio no handler.** A regra "segunda via só para reserva paga" está no `ApiServer` (`api/ApiServer.java:208-212`), e não no serviço.
- **Handlers que vão direto ao repositório.** Eles leem o `RoomRepository` sem passar pelo serviço (`api/ApiServer.java:73`, `:191`, `:214`).
- **Formato de apresentação no serviço.** O `ReportService` monta o texto CSV, que é formato de saída (`service/ReportService.java:28-44`). Isso pesa pouco.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança, financeiro), os 3 como ouvintes e nenhum fora da estrutura. Os 2 eventos (confirmação e cancelamento) passam pela lista. O serviço não monta nenhum e-mail. Há 3 interfaces de repositório com uma implementação cada, e um `MessageService` que só repassa.
**Classe:** acerto
**Exagero:** exagero
**Nota:** com problemas

**O pattern.**
- `ReservationEventListener` (`notification/ReservationEventListener.java:6-9`) tem uma implementação por interessado.
- O e-mail do hóspede também é um ouvinte (`notification/GuestNotificationListener.java:17-33`).
- O serviço recebe a lista de fora (`service/ReservationService.java:21-25`) e a percorre na confirmação (`:78-80`) e no cancelamento (`:95-97`), sem saber quem é avisado.
- As regras de "só avisar o que já estava confirmado" e "não avisar o financeiro sem devolução" ficam em cada ouvinte (`notification/HousekeepingNotificationListener.java:36-38`, `notification/FinanceNotificationListener.java:32-34`).
- Um restaurante ou um transfer entra com uma classe nova e uma linha no `Main`.
- Detalhes menores: o hóspede precisa implementar um `onReservationCancelled` vazio (`GuestNotificationListener.java:36-37`), e o evento passa um booleano solto (`wasPreviouslyConfirmed`) em vez de um objeto de evento. Nada disso afasta do acerto.

**O exagero.** Ficou fora da parte do pattern:
- `RoomRepository`, `ReservationRepository` e `MessageRepository` são interfaces com uma implementação Postgres cada, e nada as substitui: o próprio teste usa as classes Postgres (`ReservationSystemIT.java:44-46`).
- `MessageService` só repassa ao repositório (`service/MessageService.java:16-22`).

**O resto do código.**
- **Dinheiro sai como `double`.** As respostas convertem o valor para `double` (`api/ReservationResponse.java:20-25,39-44`, `api/DocumentResponse.java:20-22`) e perdem as 2 casas pedidas (sai `779.8` e `0.0`). O cálculo, porém, está certo, em `BigDecimal` com `HALF_EVEN` (`domain/Reservation.java:93-120`).
- **`catch (Exception)` que esconde a causa.** Ele transforma qualquer erro, até um do banco, em 400 INVALID_REQUEST com a mensagem interna (`api/ApiServer.java:127-129`). Nada é logado, e foi isso que escondeu a falha do CPF.
- **Id de reserva que não é número dá 500.** `Long.parseLong` sem tratamento (`api/ApiServer.java:135` e repetido) faz `/reservations/abc` responder 500.
- **`formatCpf` sem nenhuma validação.** Ele supõe 11 dígitos (`util/Formatter.java:20-26`). Um CPF curto é gravado e só estoura no ouvinte, depois que a confirmação já foi gravada (`service/ReservationService.java:76-79`): a reserva fica CONFIRMED, sem e-mail e com 500.
- **Duplicação.** O mesmo bloco de `catch` está copiado em cada handler (`api/ApiServer.java:150-156`, `164-170`, `178-184`, `193-199`). A validação de hóspedes está duplicada entre `api/ApiServer.java:89` e `service/ReservationService.java:41`, e a `InvalidRequestException` (`service/ReservationService.java:147`) nunca é mapeada de forma específica.
- **Relatório financeiro.** Ele filtra por status, e não pelo dinheiro que ficou (`service/ReservationService.java:141`, `service/ReportService.java:55-70`).

## Testes

**Números:** 10 testes rodaram, com 0 falhas. Há 24 métodos escritos: os 14 do `ReservationSystemIT` não rodam. A cobertura é de 9,0% das linhas (68 de 754) e 13,9% dos ramos (20 de 144), toda no pacote `domain`. `api`, `service`, `notification`, `repository` e `util` ficaram com 0%.
**Nota:** ruim

**O que roda é bom.** `ReservationTest` confere com valores:
- a baixa temporada (499,80);
- julho (599,76);
- o NON_REFUNDABLE (desconto 74,97, total 424,83);
- as transições e as devoluções.

As referências são `domain/ReservationTest.java:14-32`, `:35-52` e `:55-72`, e de `:164` em diante. Trocar 1.20 por 1.12, ou o 0.15, quebraria esses testes.

**A parte nova não roda.** Os avisos à governança e ao financeiro só aparecem no teste de integração, que o `pom.xml:129` desliga (`<skipITs>true</skipITs>`) e que ainda depende de Docker (Testcontainers, `ReservationSystemIT.java:30-34`). Ou seja, o `mvn verify` não confere nenhum aviso, nenhuma rota, nenhum código de erro, nenhum relatório e nada do banco.

**O teste desligado também está quebrado.** Ele espera `refund` nulo no JSON (`ReservationSystemIT.java:135`), mas o `@JsonInclude(NON_NULL)` (`api/ReservationResponse.java:8`) omite o campo. O `get("refund")` devolve `null` e o teste falharia com NullPointerException.

**Regras sem rede.** Nenhum teste que roda falharia se alguém:
- apagasse a guarda `if (!wasPreviouslyConfirmed) return;` (`notification/HousekeepingNotificationListener.java:36-38`);
- invertesse a condição de devolução zero do financeiro (`notification/FinanceNotificationListener.java:32`);
- tirasse um ouvinte da lista (`Main.java:30-34`).

## Banco

**Números:** 3 tabelas, todas com chave primária e o dinheiro em `numeric(10,2)`. As conferências acusaram:
- 0 chaves estrangeiras;
- 0 restrições que protegem uma regra (nem UNIQUE, nem EXCLUDE, nem CHECK);
- as colunas de status, plano, tipo e canal sem CHECK;
- 0 transações;
- 0 `FOR UPDATE`;
- nenhum pool (um `DriverManager.getConnection` por consulta);
- o esquema criado no código, sem migrações.

**Nota:** ruim

- **A coluna do CPF é curta demais.** `guest_cpf VARCHAR(11)` (`repository/Database.java:41`) recusa o CPF no formato que o texto mostra ("111.222.333-44"). O erro sobe como "Failed to save reservation" e o handler o devolve como 400 (`api/ApiServer.java:127-129`). Por isso o roteiro não criou nenhuma reserva.
- **Nada impede a reserva dupla.** A verificação de conflito e o `INSERT` usam conexões diferentes, sem transação, trava ou restrição (`service/ReservationService.java:49-66`, `repository/PostgresReservationRepository.java:143-169`, `:22-84`). No teste manual, com CPF só de dígitos, 5 de 5 pares simultâneos para o 301 nas mesmas noites foram os dois gravados. É dado errado: o mesmo quarto vendido duas vezes.
- **Faltam chave estrangeira e CHECK.** `reservations.room_number` não tem chave estrangeira para `rooms` (`repository/Database.java:38`), e status e plano ficam em `VARCHAR` sem CHECK (`:45`, `:50`).
- **A confirmação e os e-mails não estão numa transação.** O `update` da reserva e a gravação das mensagens são independentes (`service/ReservationService.java:76-80`). Uma falha num ouvinte deixa a reserva confirmada sem os avisos.
- **O esquema não sabe evoluir.** Ele é criado com `CREATE TABLE IF NOT EXISTS` (`repository/Database.java:26-64`), sem migração versionada.
- **O que está certo:**
  - o SQL usa parâmetros (`?`), e os `IN (...)` são montados só com marcadores constantes (`repository/PostgresReservationRepository.java:185-189`);
  - as conexões são fechadas com try-with-resources;
  - as datas ficam em `date`;
  - os dados sobrevivem ao reiniciar: as mensagens continuaram lá, conforme o roteiro.

## Comportamento

**Números:** 20 de 66 conferências passaram:
- a parte comum: 16 de 46;
- reinício: 1 de 3;
- cenário 03: 2 de 15;
- simultâneas: 1 de 2 (nenhuma rodada com as duas criadas, mas em todas veio 400 em vez de 201 e 409).

**Nota:** ruim

**A causa principal das falhas.** Quase todas vêm de um ponto só: o `POST /reservations` com o CPF "111.222.333-44" responde 400 INVALID_REQUEST "Failed to save reservation" (a coluna do banco tem 11 caracteres). A partir daí, cai tudo o que depende de uma reserva:
- confirmação, check-in, check-out e segunda via (404);
- os e-mails do hóspede, da governança e do financeiro;
- as devoluções;
- os dois relatórios;
- a reserva depois do reinício.

O texto não fixa o formato do CPF de entrada, mas mostra justamente esse formato. Um sistema que recusa esse CPF com uma mensagem de erro interna não está numa escolha razoável. Para o dono da pousada, nenhum hóspede que digite o CPF com pontos consegue reservar pelo site.

**Com CPF só de dígitos (chamadas manuais).** O fluxo anda:
- a confirmação gera os três e-mails certos (o do hóspede com o comprovante; governança com "10/03/2027 | 12/03/2027 | 2 hóspedes"; financeiro com "R$ 779,80");
- o cancelamento da reserva confirmada avisa a governança e, quando há devolução, o financeiro.

Mesmo assim, há erros que o texto fecha:
- **O dinheiro sai sem as 2 casas.** Vem `779.8`, `0.0` e `254.9` (`api/ReservationResponse.java:39-44`).
- **O financeiro lista o que não deixou dinheiro.** Ele inclui uma STANDARD cancelada com tudo devolvido, numa linha `CANCELLED,0.00`, embora o texto diga "deixaram dinheiro" (`service/ReportService.java:55-70`).
- **Reservas simultâneas passam as duas.** Duas reservas simultâneas do mesmo quarto foram as duas criadas em 5 de 5 rodadas (isso pesa na coluna do banco).
- **Id que não é número dá 500.** `GET /reservations/abc` responde 500 INTERNAL_ERROR.

## Dúvidas

- O formato do CPF de entrada não está fechado no texto. Se a intenção fosse aceitar só os 11 dígitos, a parte comum passaria em boa parte. Ainda assim, o sistema não valida nem normaliza o CPF: ele quebra no banco ou no `formatCpf` (`util/Formatter.java:20-26`). As falhas de valor (as 2 casas e o relatório financeiro) e a reserva dupla continuam. Por isso mantive `ruim` no comportamento.
- Nos testes, os 10 testes de domínio conferem valores de verdade. A nota `ruim` vem de três fatos juntos: a cobertura de 9%, nenhum teste rodando sobre a parte nova, e o teste de integração desligado e quebrado. `com problemas` seria defensável para quem pesasse mais a qualidade do que roda.
- Não deu para rodar o `ReservationSystemIT` aqui, porque o ambiente não tem Docker. A falha que aponto nele (`ReservationSystemIT.java:135`) vem da leitura do código e da resposta real da API, em que o campo `refund` não aparece.
