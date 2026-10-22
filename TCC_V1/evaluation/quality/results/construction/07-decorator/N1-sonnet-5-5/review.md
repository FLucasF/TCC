# Revisão: construção, cenário 07

## Visão geral

O projeto tem seis pacotes. `web` traz as rotas do Javalin e a leitura da requisição. Na raiz ficam `App`, que monta as peças, `Settings` e `ReservationService`. `domain` guarda as regras puras (preço, temporada, planos, situações da reserva, comprovante). `mail` cuida das regras de envio. `persistence` é o JDBC puro, com repositórios e uma classe `Db` que abre uma transação por operação. `report` faz os dois CSV. Uma requisição segue este caminho: `Routes` → `RequestParser` → `ReservationService`, que abre a transação, trava o quarto com `FOR UPDATE`, aplica a regra do `domain` e grava pelo repositório → `ReservationResponse` em JSON. Os dados ficam no PostgreSQL, em três tabelas criadas pelo próprio código ao subir. O comportamento está completo: 65 de 65 conferências passaram, inclusive as regras de e-mail nos dois modos e as reservas simultâneas. A parte de e-mail foi feita como uma lista de regras, uma classe por regra, cada uma marcada com o modo em que vale. Os pontos mais fracos não quebram nada:
- dois ciclos pequenos entre pacotes;
- o esquema criado com `CREATE TABLE IF NOT EXISTS`, sem migrações, sem pool de conexões e sem `CHECK`;
- testes só de unidade, que deixam o banco, a API e os relatórios sem nenhuma cobertura.

## Arquitetura

**Números:** 6 pacotes, 15 dependências entre eles, 2 ciclos (`br.com.marealta` ↔ `web` e `persistence` ↔ `mail`), segundo `measures/dependencies.md`. Os pacotes de regra não usam o Javalin: só `web` e a raiz (`App`) o usam. `domain` não depende de nenhum outro pacote.
**Nota:** com problemas

O que está bem:
- **A borda HTTP é fina.** Cada rota lê, chama e escreve em uma linha (`src/main/java/br/com/marealta/web/Routes.java:50-66`). A validação fica em `RequestParser`, que traduz todo problema em `INVALID_REQUEST` (`web/RequestParser.java:23-45`). Os códigos de erro saem de um único `app.exception` (`Routes.java:68`), com o status HTTP guardado no próprio `ErrorCode` (`domain/ErrorCode.java:4-9`).
- **As regras não conhecem nem o Javalin nem o JDBC.** As transições ficam em `domain/Reservation.java:42-66` e `domain/ReservationStatus.java:22-30`. O preço fica em `domain/Quote.java:8-17`, `domain/Season.java:14-24` e `domain/RatePlan.java:18-24`.
- **O SQL está todo em `persistence`.**
- **A montagem acontece num lugar só,** `App.main` (`App.java:21-36`), com injeção pelo construtor.

Os defeitos:
- **Ciclo raiz ↔ `web`.** `ReservationService` mora no pacote raiz, junto com `App`. `App` importa `web.Routes` (`App.java:13`) e `Routes` importa `br.com.marealta.ReservationService` (`web/Routes.java:3`). O serviço da aplicação ficou sem pacote próprio, e por isso a borda depende do pacote de montagem.
- **Ciclo `mail` ↔ `persistence`.** `mail.Mailer` grava direto pelo `persistence.MessageRepository` (`mail/Mailer.java:3,18-21`), e o repositório depende do `mail.Message` (`persistence/MessageRepository.java:3-5`).
- **A borda conhece o banco em duas rotas.** `Routes` recebe `Db` e repositórios e abre transação por conta própria em `/rooms` e `/messages` (`Routes.java:50`, `Routes.java:60`), sem passar por um serviço.
- **Os relatórios abrem a própria transação** (`report/OccupancyReport.java:29`, `report/RevenueReport.java:25`). Para um relatório de leitura isso é aceitável.

São misturas localizadas e ciclos curtos entre pacotes vizinhos, sem regra de negócio na borda.

## Design de baixo nível

**Números:** três regras de envio pedidas, as três na estrutura e cada uma numa classe (`InvalidAddressRule`, `TestRedirectRule`, `ArchiveCopyRule`). Nenhum `if` sobre o modo de teste dentro das regras nem no envio. A combinação por modo está declarada num lugar só (`mail/MailRules.java:14-19`).
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

**O pattern.** Não é um Decorator: nenhuma regra embrulha o envio nem chama o próximo. É uma lista de filtros aplicada pelo envio, e a referência aceita isso de antemão:
- **Cada regra é uma peça própria** que implementa `MailRule.apply(Message) → List<Message>` (`mail/MailRule.java:9-11`). Devolver uma lista permite as três formas pedidas: alterar (o desvio para teste, `mail/TestRedirectRule.java:14-19`), bloquear (`mail/InvalidAddressRule.java:8-10`) e acrescentar uma mensagem logo depois (a cópia, `mail/ArchiveCopyRule.java:14-17`).
- **`MailPipeline` aplica as regras na ordem** e deixa passar sem mexer o que já foi bloqueado (`mail/MailPipeline.java:17-31`). Assim, mensagem bloqueada não gera cópia sem que a cópia precise perguntar nada.
- **A combinação por modo fica fora das regras.** Cada regra vem num `ScopedRule` com um `Scope` (`ALWAYS`, `TEST_ONLY`, `OUTSIDE_TEST_ONLY`, `mail/Scope.java:4-15`). O pipeline filtra pelo modo ao ser montado (`MailPipeline.java:10-15`), e `App` entrega o modo e a lista (`App.java:28`).
- **As regras anunciadas cabem sem mexer no resto.** A de madrugada e o limite por dia, cada uma valendo sempre, só no teste ou só fora dele, entram como uma classe nova mais uma linha em `MailRules`. Uma instalação sem a cópia é uma lista sem aquela linha, e o teste `MailTest.java:191-195` mostra isso.

O que afasta um pouco do ideal:
- A lista da instalação é um método estático com as regras fixas no código (`MailRules.java:14`). A "segunda pousada" pediria outro método ou uma configuração, mas a escolha continua num lugar só e fora das regras.
- A ordem importa (o bloqueio antes da cópia), e isso está documentado em `MailRules.java:5-9`.

**Exagero:** contenção. `Scope` e `ScopedRule` existem para a combinação por modo que o texto pede. Fora do pattern não há interface com uma só implementação, fábrica nem camada que só repassa. `Db.Work` (`persistence/Db.java:8-11`) é só a interface funcional da transação.

**O resto do código** é limpo:
- **Os tipos estão certos.** Dinheiro usa `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:16-18`, `Quote.java:12-16`). Datas usam `LocalDate`. Planos, situações, canal e status são enums.
- **O plano carrega desconto e reembolso** (`RatePlan.java:7-8`), e a máquina de estados fica num `switch` só (`ReservationStatus.java:22-30`), sem duplicação.
- **O estado mutável da `Reservation` só muda por métodos que conferem a transição** (`Reservation.java:60-66`).

Detalhes menores, que não mudam a nota:
- Os getters `arrival()`/`departure()` têm nomes diferentes dos campos `checkIn`/`checkOut` (`Reservation.java:76-77`).
- Há nomes totalmente qualificados no meio do código (`Routes.java:83`, `ReservationService.java:97`).
- Os quartos estão semeados como `String[][]` convertido à mão (`persistence/Schema.java:50-56,68-75`).
- O tratador genérico usa `printStackTrace` (`Routes.java:70`).

## Testes

**Números:** 14 testes rodaram, 0 falharam (14 métodos escritos, em 2 classes). Cobertura de 32,6% das linhas (173/530) e 50,8% dos ramos (61/120), segundo `measures/tests.md`. Por pacote: `domain` 113/120 linhas, `mail` 60/72, e 0% em `persistence`, `report`, `web` e na raiz. Nenhum teste pela API nem contra o banco.
**Nota:** com problemas

O que está bem testado:
- **As regras de domínio, com valores do texto:**
  - a alta temporada dá 599,76 (`src/test/java/br/com/marealta/DomainTest.java:31-35`);
  - as bordas de 14→15/12 e de 28/02→01/03 (`DomainTest.java:38-45`);
  - o NON_REFUNDABLE com 116,97 de desconto (`DomainTest.java:48-53`);
  - o meio para o par (`DomainTest.java:56-59`);
  - o texto exato do comprovante, com desconto, milhar e "1 diária" (`DomainTest.java:62-87`);
  - as transições e as devoluções (`DomainTest.java:90-121`).
- **A parte nova do cenário:**
  - a validação do endereço com os exemplos do texto (`MailTest.java:147-156`);
  - a cópia logo depois, com o assunto "[Cópia] " e o mesmo texto (`MailTest.java:159-167`);
  - o bloqueio sem cópia nos dois modos, com o endereço original (`MailTest.java:170-179`);
  - o desvio de teste com o assunto e a primeira linha exatos (`MailTest.java:182-188`).
- **A pergunta da mutação, na parte nova:** trocar "[Cópia] " ou "[TESTE] ", tirar o `isSent()` do pipeline ou mudar o `Scope` da cópia faria algum teste falhar.

O que ficou sem rede:
- **A persistência das mensagens,** inclusive a ordem de inserção que põe a cópia logo depois da original (`Mailer.java:18-21`).
- **O e-mail de confirmação** (`ConfirmationEmail.java:9-16`, 0 linhas cobertas).
- **A sobreposição de datas** (`ReservationRepository.java:85-98`): trocar `check_out > ?` por `>=` não seria pego.
- **Os dois relatórios,** inclusive a regra "quem sai naquele dia não conta" (`ReservationRepository.java:105`).
- **Os códigos de erro HTTP e a validação do corpo** (`RequestParser`).
- **A segunda via só para reserva paga** (`ReservationService.java:85-95`).

Os testes conferem regras de verdade, mas metade das regras pedidas, as que passam pelo banco e pela API, não tem teste.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(12,2)` nas 6 colunas e datas em `date`. Conferências que acusaram algo, em `measures/database.md`:
- 1 chave estrangeira sem índice (`reservations.room_number`);
- 5 colunas de situação ou tipo sem `CHECK`;
- 0 restrições UNIQUE, EXCLUDE ou CHECK;
- o esquema criado por `CREATE TABLE` no código, sem migrações;
- nenhum pool de conexões.

**Nota:** com problemas

O que está certo:
- **Os tipos e as chaves** (`src/main/java/br/com/marealta/persistence/Schema.java:10-44`). A reserva se liga ao quarto por chave estrangeira (`Schema.java:20`), e o obrigatório tem `NOT NULL`.
- **Todo SQL usa `?`.** Os trechos concatenados que o script listou só juntam constantes: `SELECT`, `COLUMNS` e os sufixos `" FOR UPDATE"`/`""` (`ReservationRepository.java:76`, `RoomRepository.java:39`).
- **As conexões são fechadas** com try-with-resources (`Db.java:24`).
- **Cada operação roda numa transação com rollback** (`Db.java:23-37`). A confirmação grava a reserva e as mensagens na mesma transação (`ReservationService.java:55-64`).
- **A dupla reserva é impedida no banco, não no processo.** A criação trava a linha do quarto com `SELECT … FOR UPDATE` antes de conferir a sobreposição (`RoomRepository.java:29-31`, `ReservationService.java:34-43`). Isso vale entre processos, e o roteiro confirmou nas 10 rodadas simultâneas.

Os defeitos:
- **O esquema não evolui.** Ele é criado por `CREATE TABLE IF NOT EXISTS` em código (`Schema.java:11,18,37`), sem migrações versionadas. A próxima coluna nova não chega a um banco que já existe.
- **A chave estrangeira não tem índice** (`Schema.java:20`). A consulta de sobreposição filtra justamente por `room_number` (`ReservationRepository.java:89`).
- **Nenhuma `CHECK` protege uma regra.** Faltam em `status`, `rate_plan`, `guests > 0`, `check_out > check_in` e no `status` das mensagens (`Schema.java:24-32,43`). Também nenhuma restrição (EXCLUDE) impede a sobreposição se outro caminho gravar no banco.
- **Não há pool.** Cada transação abre uma conexão nova com `DriverManager.getConnection` (`Db.java:24`).
- **O relatório financeiro traz as reservas do mês e filtra em Java** o que deixou dinheiro (`RevenueReport.java:26-29`). Com a consulta limitada ao mês, isso pesa pouco.

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam (`measures/behavior.md`):
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 07 no modo normal: 9/9;
- cenário 07 no modo de teste: 5/5;
- duas reservas ao mesmo tempo: 2/2.

**Nota:** bom

Tudo o que o roteiro conferiu funciona como o texto descreve:
- **Preços:** os valores pedidos (599,76; o desconto de 37,48 pelo meio para o par).
- **Recusas:** todos os códigos de erro com o status certo.
- **Ciclo da reserva:** confirmação com e-mail e comprovante, check-in, check-out, segunda via, cancelamentos com e sem devolução.
- **Relatórios:** os dois CSV, inclusive a não reembolsável cancelada que continua contando.
- **Persistência:** os dados continuam depois de reiniciar.
- **Regras de e-mail:** os quatro endereços inválidos bloqueados sem cópia e com o endereço original; a cópia para o arquivo logo depois, com "[Cópia] "; no modo de teste, o desvio para testes@ com "[TESTE] " e a linha "Destinatário original", sem cópia.
- **Reservas simultâneas:** nas 10 rodadas, só uma reserva foi criada e a outra levou 409.

Para o dono da pousada: o sistema faz o que ele pediu, inclusive as três regras dos e-mails nos dois modos.

## Dúvidas

- **A classe do pattern.** Ficou entre `alternativa equivalente` e `acerto`. Não há embrulhos encadeados com a interface do envio (o Decorator clássico), e sim uma lista de regras que o `MailPipeline` aplica. Pela referência, isso é exatamente a "lista de filtros aplicada pelo envio" com a combinação por modo decidida fora das regras. A combinação está em `MailRules` (pacote `mail`), não no `main`. O `main` só escolhe o modo e a lista, e considerei que isso basta como montagem.
- **A validação do endereço** aceita domínios como `gmail..com` (`EmailAddress.java:16-18` só olha o primeiro ponto e o fim). O texto ("um nome com pelo menos um ponto no meio") não decide esse caso, e o roteiro não o testa.
