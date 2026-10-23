# Revisão: construção, cenário 07

## Visão geral

O agente montou um serviço Javalin em 8 pacotes: `api` (handlers que leem a requisição com Gson e escrevem a resposta),
`service` (reserva, preço, relatórios, mensagens), `messaging` + `messaging.processors` (uma lista de "processadores"
aplicada a cada e-mail), `repository` (JDBC puro sobre uma única `Connection` estática de `config.DatabaseConnection`)
e `domain`. Uma requisição vai de `App.java:33-46` para o handler, que chama o serviço, que chama o repositório; o
esquema é criado com `CREATE TABLE IF NOT EXISTS` no código (`config/DatabaseMigration.java:9-50`). O mais sério: **o
serviço praticamente não funciona pela API**. Todas as respostas JSON usam `ctx.json(...)` (por exemplo
`api/Handler.java:18`, `api/ReservationsHandler.java:52`), mas o `pom.xml` só traz o Gson, sem o Jackson que o
Javalin usa por padrão; toda resposta JSON, inclusive as de erro, vira 500 ("It looks like you don't have an object
mapper configured"), e o roteiro passou em 5 de 63 conferências. Além disso, o serviço **não sobe de novo** quando já
há uma reserva no banco (`DatabaseMigration.java:57-59` apaga os quartos a cada partida e esbarra na chave
estrangeira), a verificação de conflito deixa passar reservas sobrepostas, a confirmação grava o valor que o cliente
manda em vez do total, e dá para cancelar depois do check-in.

## Arquitetura

**Números:** 8 pacotes, 16 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em `api` e
no pacote raiz; JDBC só em `config` e `repository`; `service`, `domain` e `messaging` sem Javalin e sem JDBC. A maior
classe é `service/ReservationService.java` (212 linhas).
**Nota:** com problemas

As camadas existem e as dependências apontam no sentido certo, sem ciclo: as regras não conhecem o `Context` nem
montam SQL, e o SQL está todo em `repository/`. Os defeitos:

- **A montagem está espalhada.** `App.java:18-29` cria os repositórios e os handlers, mas os handlers criam os seus
  serviços por dentro (`api/ReservationsHandler.java:23`, `api/ReportsHandler.java:17`), o `ReservationService` cria
  o `MessageService` (`service/ReservationService.java:29`) e o `MessageService` monta a corrente de regras lendo a
  configuração global (`service/MessageService.java:44-50`, com `Config.isTestMode()` estático). Os repositórios
  pegam a conexão de um `static` global (`config/DatabaseConnection.java:7-18`), sem nada injetado. Não há como
  montar a combinação de regras de outra instalação sem editar o serviço.
- **Regras na borda HTTP.** O tipo do quarto é um `switch` com os números dos quartos dentro do handler
  (`api/ReservationsHandler.java:187-194`), em vez de vir do quarto; a regra "segunda via só para reserva paga" mora no
  handler e está incompleta (`api/ReservationsHandler.java:124`, só recusa PENDING, aceita CANCELLED); o comprovante
  é montado duas vezes, campo a campo, nos handlers (`api/ReservationsHandler.java:99-111` e `130-142`).
- `service` depende de `config` (`service/MessageService.java:3`) para ler variável de ambiente no meio da regra.

## Design de baixo nível

**Números:** 3 regras pedidas (bloqueio, cópia, modo de teste); 2 delas fazem trabalho dentro da estrutura
(`EmailValidationProcessor`, `TestModeProcessor`); a cópia tem uma classe vazia na estrutura
(`ArchiveCopyProcessor.process` não faz nada) e é feita à mão no serviço (`MessageService.java:33-41`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** ruim

**O pattern.** A estrutura escolhida é uma lista de filtros (`messaging/MessageProcessor.java:3-5`,
`messaging/MessageProcessorChain.java:13-17`) que alteram um `MessageData` mutável — o tipo de estrutura que a
referência aceita como alternativa equivalente, *se* cada regra for uma peça própria e a combinação por modo for
decidida na montagem. Não é o caso:

- a cópia para o arquivo está fora da estrutura: `messaging/processors/ArchiveCopyProcessor.java:15-21` tem o corpo
  vazio ("The chain will create a copy message after all processors run"), e quem cria a cópia é o próprio envio, com
  um `if` (`service/MessageService.java:33-41`);
- a combinação por modo está escrita dentro de uma regra: o desvio de teste desliga a cópia
  (`messaging/processors/TestModeProcessor.java:26`), e o bloqueio também (`EmailValidationProcessor.java:22`), por
  um sinalizador `shouldGenerateCopy` que o envio consulta;
- todas as regras são sempre empilhadas, ligadas ou desligadas por um booleano no construtor
  (`MessageService.java:46-48`), e cada regra pergunta "estou ligada? a mensagem já foi bloqueada?"
  (`EmailValidationProcessor.java:16`, `TestModeProcessor.java:16`, `ArchiveCopyProcessor.java:16`). A ordem das
  peças é um acoplamento escondido (o desvio de teste só funciona se o bloqueio vier antes).

Acrescentar "não mandar de madrugada" ou o limite por hóspede caberia numa peça nova, mas a cópia, que é uma das três
regras pedidas, ficou no jeito sem estrutura: malfeito.

**O exagero.** Não há estrutura sobrando fora do pattern (nenhuma interface com uma implementação só, fábrica ou
camada que só repassa); a classe vazia `ArchiveCopyProcessor` é defeito do pattern, contado acima. Contenção.

**O resto do código** tem defeitos espalhados:

- erros por texto: o serviço lança `IllegalArgumentException("ROOM_NOT_FOUND")` etc.
  (`service/ReservationService.java:44,48,52,58,88,97`) e o handler compara a mensagem com `equals`
  (`api/Handler.java:56-73`); qualquer outra `IllegalArgumentException` (plano desconhecido em
  `domain/RatePlan.java:23`), um JSON inválido (`ReservationsHandler.java:32`, Gson) ou um corpo vazio
  (`ReservationsHandler.java:71-72`, `NullPointerException`) vira 500. Todo handler tem `catch (Exception e)`;
- `guests < 1` vira `CAPACITY_EXCEEDED` (`service/ReservationService.java:47`), não o 400 pedido;
- estado global mutável: uma `Connection` `static` compartilhada por todas as threads do Jetty
  (`config/DatabaseConnection.java:7-18`);
- a regra da temporada está errada: dezembro inteiro conta como alta (`service/PriceCalculator.java:50`), quando o
  texto diz "de 15 de dezembro"; `YearMonth` é importado e não usado;
- os quartos estão escritos em três lugares (`domain/RoomType.java:6-8`, `config/DatabaseMigration.java:64-68`,
  `api/ReservationsHandler.java:188-192`), e o `Room` ignora a capacidade e a diária gravadas no banco
  (`repository/RoomRepository.java:20-23` lê só o tipo);
- duplicação: o comprovante em JSON (`ReservationsHandler.java:99-111` e `130-142`), `formatDate`
  (`service/ReportService.java:86` e `service/ReservationService.java:196`);
- código morto: o comprovante calculado e descartado no check-out (`ReservationsHandler.java:97`), os métodos
  `confirm/checkIn/checkOut/cancel` do domínio que ninguém chama (`domain/Reservation.java:124-140`),
  `getRequiredDouble` (`api/Handler.java:43`);
- dinheiro no JSON como `double` (`ReservationsHandler.java:172-176`), o que perde as 2 casas (100.00 sai 100.0), e
  `String.format("%.2f")` dependente do locale da JVM no CSV (`service/ReportService.java:95`).

## Testes

**Números:** 10 testes rodaram, 0 falhas, 0 erros, 0 pulados; 10 métodos escritos em 2 arquivos; cobertura de linhas
10.8% (85 de 787) e de ramos 15.8% (31 de 196). `api`, `repository`, `config` e `App` com 0%. Nenhum teste pela API ou
contra o banco (`measures/tests.md`).
**Nota:** ruim

O `mvn verify` passa, mas os testes quase não existem. `service/PriceCalculatorTest.java` confere com valores o preço
de julho (299.88), a soma de duas noites (599.76), o desconto (89.96) e o total — é a parte boa. Mas não há nenhum
caso nas bordas da temporada (14/15 de dezembro, fim de fevereiro), e por isso o erro de dezembro inteiro
(`PriceCalculator.java:50`) passa sem ninguém ver; também não há caso de "meio para o par" de verdade.
`messaging/MessageProcessorChainTest.java` testa o bloqueio com um endereço só de cada tipo (linhas 15 e 53), o desvio
de teste com `startsWith("[TESTE]")` e `contains("Destinatário original")` (linhas 42-43, não confere que é a primeira
linha) e não testa a cópia para o arquivo em lugar nenhum: se alguém apagasse o `if` de `MessageService.java:33-41`,
ou trocasse `dotIndex > 0` por `>= 0` em `EmailValidationProcessor.java:47`, nenhum teste falharia. Nada testa as
situações da reserva, o cancelamento e a devolução, os conflitos, os códigos de erro, os relatórios nem a API — e um
único teste pela API teria acusado que toda resposta JSON dá 500.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`
e datas em `date` (ok); 1 chave estrangeira sem índice (`reservations.room_number`); nenhuma CHECK, UNIQUE ou EXCLUDE;
5 colunas de situação/tipo sem CHECK; 0 pool, 1 `DriverManager.getConnection`, 0 transações, 0 `FOR UPDATE`; 1 SQL
montado com `+` (`measures/database.md`).
**Nota:** ruim

- **O serviço não sobe de novo depois que há reservas.** A cada partida, `config/DatabaseMigration.java:57-59` faz
  `DELETE FROM rooms` e reinsere os quartos; com uma reserva gravada, a chave estrangeira recusa
  (`ERROR: update or delete on table "rooms" violates foreign key constraint "reservations_room_number_fkey"`,
  reproduzido à mão) e o `main` morre. O roteiro confirma: "o serviço sobe de novo sobre o mesmo banco: não subiu".
  Os dados ficam no banco, mas a pousada fica sem sistema no primeiro reinício.
- **Nada impede a reserva dupla.** A consulta de conflito procura só reservas que cobrem a noite de entrada
  (`repository/ReservationRepository.java:117-125`, chamada com `checkIn` em `ReservationService.java:55`), então uma
  reserva de 10 a 13/03 passa por cima de outra de 12 a 14/03 no mesmo quarto (reproduzido: as duas ficaram gravadas).
  Não há restrição no banco (nenhuma EXCLUDE), nem transação, nem trava: duas reservas simultâneas iguais foram
  gravadas as duas (reproduzido à mão, ids 3/4 e 5/6).
- O esquema nasce de `CREATE TABLE IF NOT EXISTS` no código (`DatabaseMigration.java:10-50`), sem migração
  versionada: não sabe acrescentar uma coluna numa tabela existente.
- Uma `Connection` única e `static` para todas as requisições (`config/DatabaseConnection.java:7-18`), sem pool e em
  autocommit: confirmar grava a reserva e depois a mensagem sem transação (`ReservationService.java:100-104`), então
  uma falha no meio deixa a reserva confirmada sem e-mail.
- `repository/RoomRepository.java:32` concatena o número do quarto no SQL; é um `int`, não há injeção, mas foge do
  padrão de parâmetros do resto.
- Sem CHECK em `status`, `rate_plan`, `type`; `amount_paid` aceita nulo (`DatabaseMigration.java:34`).

## Comportamento

**Números:** 5 de 63 conferências passaram (`measures/behavior.md`): parte comum 4 de 46; dados depois de reiniciar
0 de 1; cenário no modo normal 0 de 9; cenário no modo de teste 0 de 5; reservas simultâneas 1 de 2.
**Nota:** ruim

A causa principal das falhas é uma só e é do agente: toda resposta JSON passa por `ctx.json(...)` sem um mapeador
configurado (o `pom.xml` não tem Jackson; o projeto usa Gson só para ler), então `GET /rooms`, `POST /reservations`,
`GET /messages` e até as respostas de erro (`api/Handler.java:18`) saem 500. Como a criação devolve 500, o roteiro
não recebe o id e as chamadas seguintes dão 404; só passaram o cabeçalho dos dois CSV, o content-type da ocupação e
`refund` nulo. Para o dono da pousada: o site não consegue criar nem consultar reserva, e a recepção não consegue ver a
lista de mensagens.

O serviço também não volta depois de reiniciar (ver Banco). As duas reservas simultâneas não "foram criadas" só porque
as duas respostas foram 500; à mão, as duas ficaram gravadas no banco.

Confirmei à mão, direto no banco (a API não mostra), outros erros que o roteiro não chegou a ver:

- reservas sobrepostas aceitas (10–13/03 sobre 12–14/03 no quarto 101);
- 02/12 cobrado como alta temporada (299.88 em vez de 249.90);
- a confirmação sem corpo dá 500; com `{"amount":"10.00"}` a reserva de R$ 499,80 fica confirmada com 10.00 pago
  (`ReservationsHandler.java:71-72`), quando o texto diz que o pago é o total;
- cancelar depois do check-in é aceito e devolve o pago (`ReservationService.java:134-136`), quando deveria dar 409;
- o relatório de ocupação escreve as datas como 12/03/2027 (`ReportService.java:37-38`), não 2027-03-12, e o
  financeiro fecha com `total,,,,0.00` (`ReportService.java:72`) em vez de `total,<soma>`; o financeiro também lista
  uma reserva cancelada com tudo devolvido (valor 0.00), que não deixou dinheiro.

As regras de envio do cenário funcionam no banco: com endereço válido, a confirmação gravou a mensagem SENT e logo
depois a cópia para `arquivo@pousadamarealta.com.br` com "[Cópia] ". Mas nada disso aparece pela API.

## Dúvidas

- Classe do pattern: a lista de filtros seria uma alternativa equivalente se a cópia fosse uma peça de verdade. Dei
  `malfeito` porque `ArchiveCopyProcessor` é vazio, a cópia é feita à mão no envio (`MessageService.java:33-41`) e o
  desvio de teste desliga a cópia por dentro (`TestModeProcessor.java:26`).
- O modo de teste põe uma linha em branco depois de "Destinatário original: …" (`TestModeProcessor.java:25`). O texto
  pede "uma primeira linha a mais" e não fecha se a linha em branco é aceitável; não pesou.
- Nenhuma conferência do cenário pôde ser medida pela API por causa do 500 em todo JSON; o que digo das regras de
  envio vem da leitura do código e de uma consulta direta à tabela `messages` (só no modo normal).
