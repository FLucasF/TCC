# Revisão: construção, cenário 07

## Visão geral

O projeto tem cinco pacotes. `web` guarda as rotas Javalin (`Routes`). O pacote raiz guarda a montagem (`Main`), o serviço (`ReservationService`) e os relatórios (`Reports`). `domain` tem as regras puras (preço, temporada, situações, comprovante). `messaging` tem a cadeia de envio das mensagens, e `persistence` tem JDBC puro sobre PostgreSQL. Uma requisição entra em `Routes`, que valida o corpo e chama o serviço. O serviço abre uma transação com `Database.inTransaction`, trava a linha com `FOR UPDATE`, aplica a regra do domínio e grava pelos repositórios. As mensagens passam por uma pilha de embrulhos de `MessageSink` (validação do endereço, cópia para o arquivo, desvio de teste), montada em `MessagePipeline.assemble` conforme o modo, e terminam na tabela `messages`. Os dados ficam em três tabelas, criadas com `CREATE TABLE IF NOT EXISTS` no código. Não há defeito grave: as 65 conferências do roteiro passaram e os 12 testes passam. Os pontos fracos são três: um ciclo pequeno entre o pacote raiz e `web`, um esquema sem migrações e sem restrições, e nenhum pool de conexões.

## Arquitetura

**Números:** 5 pacotes, 9 dependências entre eles, 1 ciclo (`br.com.marealta` ↔ `br.com.marealta.web`). O pacote raiz usa Javalin, JDBC e Jackson. `domain` e `messaging` não usam nenhuma biblioteca. A maior classe é `ReservationRepository`, com 143 linhas.
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota é uma linha que chama o serviço (`web/Routes.java:42-54`). O handler só faz a leitura e a validação do formato do corpo (`web/Routes.java:70-97`), sem regra de negócio e sem SQL. Os erros saem pelo `ErrorCode`, que já sabe o status HTTP (`domain/ErrorCode.java:3-9`, `web/Routes.java:36`).
- **As regras estão separadas.** `domain` não conhece nem Javalin nem JDBC. A máquina de situações está em `domain/Operation.java:7-10`, o preço em `domain/Quote.java:8-16` e `domain/HighSeason.java:20-23`, e a devolução em `domain/RatePlan.java:19-21`.
- **O acesso ao banco está isolado** em `persistence`. O serviço não monta SQL, mas conhece `java.sql.Connection` e `SQLException` (`ReservationService.java:17-18`, `:117-123`), porque é ele que delimita a transação passando a conexão aos repositórios. Para um sistema deste tamanho é uma unidade de trabalho aceitável.
- **A montagem fica num lugar só:** `Main.createApp` (`Main.java:31-44`) cria e liga todas as peças, e a escolha das regras de envio fica em `Installation.mareAlta()`.
- **O ciclo** existe porque `Main`, que importa `Routes` (`Main.java:10`, `:42`), mora no mesmo pacote que `ReservationService` e `Reports`, que `Routes` importa (`web/Routes.java:3-5`). É pequeno, entre dois pacotes vizinhos, e se resolve tirando a montagem do pacote do serviço. Mesmo assim, é um ciclo criado pelo agente, e o serviço e os relatórios ficaram soltos no pacote raiz em vez de num pacote de aplicação.
- **Uma dependência estranha:** `persistence` depende de `messaging`, porque `StoredMessages` implementa `MessageSink` (`persistence/StoredMessages.java:14`). Ela aponta no sentido certo (dados → contrato), mas mostra que `messaging` faz ao mesmo tempo o papel de domínio e de porta.

## Design de baixo nível

**Números:** as 3 regras pedidas (endereço inválido, cópia para o arquivo, desvio de teste) estão na estrutura, e nenhuma ficou fora dela. As 3 escolhas de modo (sempre, só no teste, só fora do teste) ficam na montagem, e nenhuma está dentro de um embrulho.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** É um Decorator de livro:

- **Uma interface de envio:** `messaging/MessageSink.java:4`. O envio real é `persistence/StoredMessages.java:22`.
- **Um embrulho por regra, cada um com o mesmo contrato e chamando o próximo:**
  - `AddressValidation` (`messaging/AddressValidation.java:14-20`) bloqueia o endereço inválido e grava a mensagem como `BLOCKED` direto no destino final, sem passar pelo resto da pilha.
  - `ArchiveCopy` (`messaging/ArchiveCopy.java:16-19`) entrega a original e depois a cópia com "[Cópia] ".
  - `TestRedirect` (`messaging/TestRedirect.java:16-18`) troca o destinatário, o assunto e a primeira linha do texto.
- **Nenhum embrulho pergunta pelo modo.** A combinação é decidida na montagem: `MessagePipeline.assemble` (`messaging/MessagePipeline.java:11-18`) empilha só as regras cujo `RuleScope` vale para o modo atual (`messaging/RuleScope.java:9-14`), a partir da lista da instalação (`messaging/Installation.java:8-12`).
- **As regras anunciadas cabem sem mexer no que existe.** A madrugada e o limite por dia seriam mais um embrulho com escopo `ALWAYS`, `TEST_ONLY` ou `LIVE_ONLY`. A segunda pousada seria outra lista sem `ArchiveCopy`, e o teste `MessagePipelineTest.java:52-56` faz exatamente isso.
- **O serviço não sabe de nada disso:** ele só chama `messages.deliver` (`ReservationService.java:72`).

**O exagero: contenção.** `MessageRule`, `ScopedRule`, `RuleScope` e `Installation` servem diretamente à variação que o texto anuncia (cada regra com o seu escopo e cada instalação com a sua combinação), então pertencem ao pattern. Fora dele não há interface com implementação única, fábrica nem camada que só repassa. A interface `Database.Work` (`persistence/Database.java:9-12`) é a forma funcional da transação, e o `enum Operation` substitui o que seriam `if`s de situação. O segundo parâmetro de `MessageRule.wrap`, o destino das bloqueadas (`messaging/MessageRule.java:10`), só é usado por `AddressValidation`, o que é um detalhe pequeno.

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:9-11`, `domain/Quote.java:12`, `:16`), `LocalDate`, `enum` para situação, plano, tipo, canal e status, e `record`s imutáveis. Os métodos são curtos, e a sequência travar → mudar → gravar está escrita uma vez só (`ReservationService.java:109-115`). Restam detalhes pequenos:

- `Reservation.confirmed()` repete o construtor de 16 argumentos em vez de usar `withStatus` (`domain/Reservation.java:20-23` contra `:43-46`).
- A definição de "reserva paga" está duas vezes, em SQL e em Java (`persistence/ReservationRepository.java:18` e `domain/ReservationStatus.java:11-13`).
- O e-mail é enviado depois do commit da confirmação, numa transação à parte (`ReservationService.java:70-72`, `persistence/StoredMessages.java:23`). Se a gravação da mensagem falhar, a reserva fica CONFIRMED, o cliente recebe 500 e não há e-mail.
- `catch (Exception)` com `printStackTrace` (`web/Routes.java:37-40`) não esconde a causa, mas não usa o logger.

Nada disso faria um revisor exigente recusar o código.

## Testes

**Números:** 12 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 12 métodos escritos em 3 classes: `DomainTest` (6), `MessagePipelineTest` (4) e `ApiTest` (2, pela API e contra o PostgreSQL de `DB_URL`, num esquema próprio). A cobertura é de 93,1% das linhas (434 de 466) e 77,7% dos ramos (80 de 103). Em `domain` ela é de 94 de 94 linhas e, em `messaging`, de 66 de 67.
**Nota:** bom

**O que os testes conferem, com valores:**

- **A parte nova, nos dois modos.** No modo normal: a original e depois a cópia, com assunto "[Cópia] Oi" e o mesmo texto (`MessagePipelineTest.java:18-25`). No modo de teste: um só destinatário de testes, "[TESTE] Oi", a primeira linha "Destinatário original: …" e nenhuma cópia (`:28-35`). Seis endereços inválidos ficam `BLOCKED` com o endereço original, nos dois modos (`:38-49`). Uma instalação sem a cópia também é testada (`:52-56`).
- **A pergunta da mutação**, na parte nova:
  - trocar o escopo de `ArchiveCopy` para `ALWAYS` derruba `:30`;
  - trocar `at <= 0` por `at < 0` derruba o caso "@b.com" (`:41`);
  - tirar o "\n" do desvio derruba `:33`.
- **A parte comum:**
  - o preço da alta temporada, 599,76 (`DomainTest.java:16-20`);
  - os limites da temporada, inclusive 29/02 (`:23-28`);
  - o desconto de 74,97 (`:31-36`);
  - o texto exato do comprovante (`:39-51`) e o formato R$ 1.234,56 (`:54-56`);
  - a devolução zero do não reembolsável e as transições recusadas (`:59-68`);
  - o fluxo inteiro pela API, com o CSV de ocupação e o financeiro com a cancelada não reembolsável contando (`ApiTest.java:82-147`).

**O que ficou sem rede:**

- A devolução do plano STANDARD (`RatePlan.STANDARD` com `refundable=false` não derrubaria nenhum teste, porque `DomainTest.java:59-68` só usa NON_REFUNDABLE).
- A linha "Desconto:" e o "1 diária" do comprovante.
- Um empate de arredondamento "meio para o par".
- Os códigos de erro pela API: só o status é conferido, nunca o campo `error` (`ApiTest.java:95-105`).
- A API só sobe em modo de teste (`ApiTest.java:45`), então a cópia para o arquivo nunca é vista de ponta a ponta. Ela está coberta só na unidade.

São lacunas pontuais. A iniciativa (o texto não pedia testes) e a qualidade das asserções são boas.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária, e as 6 colunas de dinheiro em `numeric(12,2)`. As conferências acusaram:

- 1 chave estrangeira sem índice (`reservations.room_number` → `rooms`);
- 5 colunas de situação ou tipo sem CHECK;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- o esquema criado por `CREATE TABLE` no código (0 arquivos de migração).

O script contou "1 arquivo com pool", mas não há pool no `pom.xml` (só `postgresql`). O `PGSimpleDataSource` abre uma conexão nova por transação (`persistence/Database.java:14`).
**Nota:** com problemas

**O que está certo:**

- Datas em `date`, `NOT NULL` em todo campo obrigatório (`refund` aceita null de propósito), e a reserva ligada ao quarto por chave estrangeira (`persistence/Database.java:63-94`, FK em `:72`).
- Todo SQL usa `?`. Os três candidatos de SQL montado com `+` só concatenam constantes (`persistence/ReservationRepository.java:18`, `:88`, `:96`, e o sufixo `" FOR UPDATE"` em `persistence/RoomRepository.java:27-36`).
- Conexões em try-with-resources, e uma transação com rollback que envolve cada operação (`persistence/Database.java:23-37`).
- A sobreposição de reservas é protegida por uma trava de linha no quarto, `SELECT … FOR UPDATE` (`ReservationService.java:47`, `persistence/RoomRepository.java:27`), antes da consulta de sobreposição (`persistence/ReservationRepository.java:70-84`). A trava vale entre processos, e o roteiro confirmou 10 rodadas sem duplicação.
- A criação do esquema usa uma advisory lock contra duas instâncias subindo juntas (`persistence/Database.java:61`).

**Os defeitos:**

- O esquema não evolui. `CREATE TABLE IF NOT EXISTS` (`persistence/Database.java:63-94`) não sabe acrescentar uma coluna numa tabela que já existe, e não há Flyway nem migração versionada.
- A regra "uma noite, um quarto" não tem restrição no banco (nenhum EXCLUDE). Ela depende de todo escritor passar pela trava do serviço.
- Nenhum CHECK em `status`, `rate_plan`, `guests` ou `check_out > check_in`.
- A FK `room_number` não tem índice.
- Não há pool de conexões.
- A mensagem original e a sua cópia são gravadas em transações separadas (`persistence/StoredMessages.java:23`), e a ordem "logo depois" depende de um `synchronized` que só vale dentro de um processo (`messaging/MessagePipeline.java:20-23`).

## Comportamento

**Números:** 65 de 65 conferências passaram, e nenhuma falhou:

- parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- cenário 07 no modo normal: 9 de 9;
- cenário 07 no modo de teste: 5 de 5;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede:

- O preço com a temporada e o "meio para o par" saem certos, inclusive o desconto de 37,48 numa noite.
- As recusas vêm com os códigos combinados.
- O e-mail de confirmação traz o comprovante.
- As devoluções seguem o plano, e os dois relatórios em CSV batem.
- Reservas e mensagens sobrevivem a um reinício.
- Duas reservas simultâneas do mesmo quarto nunca passam juntas.

Na parte do cenário:

- Um endereço inválido ("ana@gmail", "ana.gmail.com", "ana@@gmail.com", "@gmail.com") fica BLOCKED, sem cópia e com o endereço original.
- A mensagem válida sai SENT e é seguida da cópia "[Cópia] " para o arquivo.
- Com `HOTEL_TEST_MODE=true`, tudo vai para a caixa de testes com "[TESTE] " e "Destinatário original: …", sem cópia, e o bloqueio continua valendo.

Nenhuma falha pesou.

## Dúvidas

- A nota da arquitetura ficou entre `bom` e `com problemas`. O ciclo é pequeno e fácil de desfazer (mover `Main`), mas a regra da skill põe um ciclo novo entre dois pacotes em `com problemas`, e foi isso que pesou.
- Nos testes, a devolução do plano STANDARD e os códigos de erro pela API ficaram sem asserção. Achei que ficavam dentro de `bom` pela cobertura e pela qualidade do resto, mas um revisor mais rígido poderia dar `com problemas`.
