# Revisão: construção, cenário 07

## Visão geral

O projeto (os caminhos abaixo são relativos a `src/main/java/com/pousadamarealta/`) tem seis pacotes. `api` traz uma classe de rotas por recurso, que registra lambdas do Javalin. `service` tem só `PricingService` e `MessageService`. `repository` tem três classes estáticas com JDBC. `database` cria as tabelas com `CREATE TABLE IF NOT EXISTS` e abre uma conexão por chamada com `DriverManager`. `model` tem classes de campos públicos. O caminho de uma requisição é: a lambda em `api/ReservationsEndpoints.java` lê o corpo com Gson, aplica ela mesma as regras da situação da reserva, chama os repositórios estáticos e responde com `ctx.json(...)`. Os dados ficam no PostgreSQL.

O mais sério é que o sistema não funciona. O `main` cai ao registrar a rota `/reservations/:id` (`Application.java:18` → `api/ReservationsEndpoints.java:88`), porque o Javalin 6 rejeita a sintaxe `:param`. A mensagem no log é "Path '/reservations/:id' invalid - Javalin 4 switched from ':param' to '{param}'" (`measures/behavior-service.log:19`). Por isso as rotas de reserva por id, de mensagens e de relatórios nunca são registradas. As duas rotas que sobram respondem 500 porque o projeto não tem mapeador JSON para o `ctx.json` (falta o `jackson-databind` no `pom.xml`). E o `POST /reservations` grava a reserva antes de falhar: o cliente recebe 500, mas a reserva fica no banco.

O roteiro passou 3 de 65 conferências. A parte do cenário, as regras de envio, está toda em `if` dentro de um único método (`service/MessageService.java:27,40,53`).

## Arquitetura

**Números:** 6 pacotes, 9 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). A maior classe é `api/ReservationsEndpoints.java`, com 351 linhas, o triplo da segunda (`ReservationRepository`, 171). `api` usa JDBC (`SQLException`) e fala direto com `repository`. `service` também usa JDBC.
**Nota:** com problemas

O que está bem: o SQL fica isolado em `repository/` e as dependências não formam ciclo.

O que está mal é a camada de regras, que praticamente não existe para a reserva. Não há serviço de reservas: as regras moram nos handlers do Javalin.
- As transições de situação estão em `api/ReservationsEndpoints.java:116`, `:145`, `:177` e `:203`.
- A devolução do cancelamento está em `api/ReservationsEndpoints.java:150-156`.
- A regra de "reserva paga" da segunda via está em `api/ReservationsEndpoints.java:229`.
- O texto do e-mail e do comprovante é montado em `api/ReservationsEndpoints.java:265-307`.
- O valor líquido do relatório financeiro é calculado em `api/ReportsEndpoints.java:101-106`.

A borda HTTP chama os repositórios direto, por exemplo em `api/ReservationsEndpoints.java:54,65,79`.

Não há montagem. Tudo é `static`: `Database.getConnection()` é chamado de dentro de cada repositório (`repository/ReservationRepository.java:12`), e o modo de teste é lido do ambiente numa constante estática (`service/MessageService.java:11`). Por isso não dá para trocar nenhuma peça nem testar sem o banco real.

As camadas existem só pela metade: os dados estão separados, as regras não. Esse ponto pesa entre `com problemas` e `ruim` (ver "Dúvidas").

## Design de baixo nível

**Números:** 3 regras de envio pedidas (bloqueio de endereço inválido, cópia para o arquivo, desvio do modo de teste). Nenhuma está numa estrutura: as 3 são `if` dentro de `MessageService.sendMessage`. Não há interface de envio, nem embrulhos, nem uma combinação decidida na montagem.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede regras que se combinam por modo e anuncia outras ("não mandar de madrugada", "limite por dia"), que podem valer sempre, só no teste ou só fora dele, e cada instalação pode querer outra combinação. O agente pôs as três regras como `if` encadeados num único método estático:
- o bloqueio está em `service/MessageService.java:27-34`;
- o desvio do modo de teste está em `service/MessageService.java:40-44`;
- a cópia condicionada ao modo está em `service/MessageService.java:52-61`.

A combinação por modo está fixa no código (`if (!TEST_MODE)` em `:53`). Para a segunda pousada, que não quer a cópia, ou para uma regra nova, é preciso editar o mesmo método. Isso é a omissão que a referência descreve.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface com uma implementação só, nem fábrica. Contenção.

**O resto do código.** Os defeitos estão espalhados:
- **Texto solto no lugar de tipos do domínio:** situação e plano são `String`, como em `api/ReservationsEndpoints.java:116,150` e `model/Reservation.java:184`. O modelo tem campos públicos mutáveis (`model/Reservation.java:7-22`, `model/Message.java:4-9`).
- **Duplicação:** a classe `ErrorResponse` foi copiada em quatro arquivos (`api/MessagesEndpoints.java:18`, `api/ReportsEndpoints.java:17`, `api/ReservationsEndpoints.java:20`, `api/RoomsEndpoints.java:18`). O bloco "ler id, buscar, 404, conferir a situação" se repete em cinco handlers (`api/ReservationsEndpoints.java:106-240`). A lista de colunas do `SELECT` aparece quatro vezes (`repository/ReservationRepository.java:46,91,114,134`). A constante `ARCHIVE_EMAIL` está duplicada e não é usada (`api/ReservationsEndpoints.java:18`).
- **Código morto:** `findByRoomAndMonth` (`repository/ReservationRepository.java:87`) e os imports que sobraram (`service/MessageService.java:5-6`).
- **Erros escondidos:** o `catch (Exception e)` transforma qualquer falha, inclusive de banco, em 400 INVALID_REQUEST, com `printStackTrace` (`api/ReservationsEndpoints.java:82-85`).
- **Formato do dinheiro:** `formatMoney` não põe o separador de milhar que o texto pede, "R$ 1.234,56" (`api/ReservationsEndpoints.java:298-300`). O CSV usa `String.format("%.2f")` sem `Locale` (`api/ReportsEndpoints.java:113,116`): num servidor em pt-BR, a vírgula decimal quebraria as colunas.
- **Validação do endereço:** `ana@.com` é aceito (`service/MessageService.java:76`), embora o ponto não esteja "no meio" do nome.
- **Relatório financeiro:** a consulta traz reservas que não deixaram dinheiro, como a pendente cancelada e a STANDARD cancelada com devolução, com valor 0,00 (`repository/ReservationRepository.java:138`).
- **JSON:** o código mistura Gson (`JsonObject`) com o `ctx.json` do Javalin (`api/ReservationsEndpoints.java:80`), sem mapeador configurado. Mesmo com o Jackson, um `JsonObject` do Gson não sairia como o JSON esperado.

O cálculo de preço está correto: `BigDecimal`, `HALF_EVEN` a cada passo e `LocalDate` (`service/PricingService.java:16-68`).

## Testes

**Números:** 9 testes rodaram, nenhum falhou nem foi pulado; 9 métodos escritos. A cobertura é de 36 de 608 linhas (5,9%) e 20 de 148 ramos (13,5%). Só o pacote `service` é tocado; `api`, `repository` e `database` ficam em 0 (`measures/tests.md`). Nenhum teste passa pela API ou pelo banco.
**Nota:** ruim

**O que os testes conferem.** `PricingServiceTest` confere, com valores, o preço normal, o de julho e o desconto NON_REFUNDABLE (`src/test/java/com/pousadamarealta/service/PricingServiceTest.java:19-21,33,44`). `MessageServiceTest` testa só `isValidEmail`, por reflexão sobre um método privado (`src/test/java/com/pousadamarealta/service/MessageServiceTest.java:11-12`).

**O que fica sem teste:**
- **O envio:** o bloqueio que grava `BLOCKED`, a cópia "[Cópia] " e o desvio "[TESTE] ".
- **O resto das regras:** as situações da reserva, o cancelamento e a devolução, as recusas com os códigos de erro, o comprovante e os relatórios.

**A pergunta da mutação:** quase nada seria pego.
- Se alguém apagasse a cópia (`service/MessageService.java:53-61`), trocasse "[TESTE] " ou removesse o `if (TEST_MODE)`, nenhum teste falharia.
- Também passaria a troca de `day >= 15` por `day > 15` na alta temporada de dezembro (`service/PricingService.java:28`): não há teste de dezembro.
- O mesmo vale para trocar `HALF_EVEN` por `HALF_UP`: nenhum valor testado cai num empate de arredondamento.

Os testes passam, mas não perceberam que o serviço não sobe nem responde JSON, que é o defeito que derruba o sistema inteiro.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram:
- a chave estrangeira `reservations(room_number)` sem índice;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- o esquema criado no código, sem migração;
- nenhum pool;
- nenhuma transação explícita;
- nenhum `FOR UPDATE` (`measures/database.md`).

**Nota:** ruim

**Tipos e SQL.** Os tipos estão certos e todo o SQL usa parâmetros `?` (`repository/*.java`).

**Reserva duplicada.** Nada no sistema impede duas reservas do mesmo quarto na mesma noite. A conferência (`repository/ReservationRepository.java:71-85`) e a gravação (`:11-41`) acontecem em conexões separadas, sem transação nem trava (`api/ReservationsEndpoints.java:65,79`), e o banco não tem restrição de exclusão. Testei à mão: subi o jar sobre um banco vazio e mandei dez rodadas de dois `POST /reservations` simultâneos para o quarto 102 na mesma noite. Nas dez, as duas reservas ficaram gravadas (`select check_in, count(*) ... group by check_in` deu 2 em todas). Isso é dado errado no banco.

O roteiro de comportamento registrou essa parte como "nenhuma com as duas criadas" só porque as duas respostas foram 500. A reserva é gravada mesmo quando a resposta é 500.

**Outros defeitos:**
- O esquema nasce de `CREATE TABLE IF NOT EXISTS` no código (`database/Database.java:46-99`), que não sabe evoluir uma tabela que já existe.
- Cada chamada abre uma conexão nova com `DriverManager`, sem pool (`database/Database.java:32-34`).
- `status` e `rate_plan` são `varchar` sem CHECK (`database/Database.java:69,74`).

## Comportamento

**Números:** 3 de 65 conferências passaram:
- a parte comum: 1 de 46;
- os dados depois de reiniciar: 1 de 3;
- o cenário 07 no modo normal: 0 de 9;
- o cenário 07 no modo de teste: 0 de 5;
- as reservas simultâneas: 1 de 2 (`measures/behavior.md`).

**Nota:** ruim

As três conferências que passaram não valem nada: "refund null", "as mensagens continuam lá" (as duas chamadas deram o mesmo 404) e "nenhuma com as duas criadas" (as duas deram 500).

A causa das falhas são os dois defeitos da visão geral:
- **O `main` cai na rota `/reservations/:id`** (`api/ReservationsEndpoints.java:88`). Toda rota por id, `GET /messages` e os relatórios respondem 404 "Endpoint not found".
- **O `ctx.json` não tem mapeador JSON.** `GET /rooms` e `POST /reservations` respondem 500 "It looks like you don't have an object mapper configured".

Para o dono da pousada, isso quer dizer que o site não consegue listar quartos, criar, confirmar nem cancelar reservas. A recepção não faz check-in, check-out nem segunda via, e não vê a lista de mensagens. O contador não tira nenhum relatório.

O pior é que cada tentativa de reserva que "dá erro" fica gravada no banco como PENDING e ocupa o quarto. O hóspede tenta de novo e recebe outro erro, porque agora o quarto está ocupado pela tentativa anterior.

Nenhuma das regras de e-mail do cenário pôde ser vista funcionando: nem o bloqueio, nem a cópia, nem o modo de teste.

## Dúvidas

- **A nota da arquitetura** fica entre `com problemas` e `ruim`. O acesso a dados está isolado e não há ciclo, mas toda a regra do ciclo de vida da reserva mora nos handlers e não há montagem nenhuma (tudo `static`). Dei `com problemas` porque a camada de dados existe; um revisor mais rígido daria `ruim`.
- **O comportamento das regras de envio** pela API não pôde ser observado, porque as rotas não sobem. O julgamento delas vem só da leitura de `service/MessageService.java`.
