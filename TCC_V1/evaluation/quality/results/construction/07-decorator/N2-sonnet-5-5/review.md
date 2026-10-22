# Revisão: construção, cenário 07

## Visão geral

O agente montou o projeto do zero em 7 pacotes: `web` (Javalin + Jackson), `service` (o `ReservationService`), `domain`
(reserva, preço, planos, quartos, comprovante, sem Javalin nem JDBC), `messaging` (as regras de envio), `persistence`
(JDBC puro), `report` (os dois CSV) e a montagem em `App`. Uma requisição entra em `WebApp` (`web/WebApp.java:40-47`),
o corpo é lido por `ReservationRequest`, o serviço chama o domínio (`Reservation.confirm/cancel/...`) e o
`ReservationRepository`, e a resposta sai pelos mapas de `Views`. Os dados ficam em duas tabelas do PostgreSQL,
`reservations` e `messages`, criadas com `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:28-55`).
Os quartos não ficam no banco: estão fixos no código (`domain/RoomCatalog.java:11-16`). A parte do cenário, as regras de
envio, é um Decorator limpo: `MessageSender` com o envio real (`OutboxSender`) e um embrulho por regra, empilhados
na montagem conforme o modo (`messaging/Installation.java:16-20`). Os 65 itens do roteiro passaram e os 17 testes
também. Os pontos mais fracos estão no banco: o esquema sem migrações, nenhuma restrição (CHECK, FK) e as mudanças de
situação da reserva protegidas só por `synchronized`.

## Arquitetura

**Números:** 7 pacotes, 15 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em `web` e
na montagem (`br.com.marealta`); JDBC só em `persistence`; `domain` e `messaging` não dependem de nenhum outro pacote
do projeto. A maior classe é `ReservationRepository` (148 linhas).
**Nota:** bom

- A borda HTTP é fina: cada rota lê a requisição, chama o serviço e escreve a resposta (`web/WebApp.java:38-59`). O
  parse do corpo fica separado em `web/ReservationRequest.java:17-36`, e o JSON de saída em `web/Views.java`.
- As regras ficam no domínio, sem Javalin e sem SQL. As transições e a devolução estão em
  `domain/Reservation.java:46-72`, o preço em `domain/Pricing.java:24-38` e o plano em `domain/RatePlan.java:17-23`.
- O acesso ao banco fica isolado em `persistence/` (`ReservationRepository`, `JdbcMessageStore`). O pacote
  `messaging` só conhece a interface `MessageStore` (`messaging/MessageStore.java`), e a implementação JDBC aponta
  para ela (persistence → messaging), na direção certa.
- A montagem fica num lugar só, `App.build` (`App.java:25-32`), que liga banco, repositório, envio, serviço e web.
- Ressalvas pequenas, que não mudam a nota:
  - os relatórios e a lista de mensagens vão da web direto ao repositório e ao `MessageStore`, sem passar pelo serviço
    (`web/WebApp.java:26-27,49,53,57-58`). São leituras sem regra;
  - `ApiException`, no domínio, carrega o código HTTP (`domain/ApiException.java:16-38`), um detalhe da borda dentro
    das regras;
  - `RoomCatalog` é um `static` global, mas imutável (`domain/RoomCatalog.java:11`).

## Design de baixo nível

**Números:** as 3 regras do texto (endereço inválido, cópia para o arquivo, modo de teste) são 3 embrulhos de
`MessageSender`, e nenhuma ficou fora da estrutura. A combinação por modo fica na montagem, em 1 lugar
(`Installation.java:16-19`). Nenhum `if (testMode)` dentro das regras.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** `MessageSender` (`messaging/MessageSender.java:7-9`) é a interface do envio, e `OutboxSender`
(`messaging/OutboxSender.java:10-12`) é o envio real (guardar na lista). Cada regra é um embrulho com a mesma
interface, que recebe o próximo e o chama:
- `ValidatingSender` (`messaging/ValidatingSender.java:14-19`) bloqueia o endereço inválido e grava a mensagem como
  BLOCKED, com o endereço original;
- `TestModeSender` (`messaging/TestModeSender.java:14-19`) desvia para a caixa de testes, com "[TESTE] " e a linha
  "Destinatário original";
- `ArchiveCopySender` (`messaging/ArchiveCopySender.java:14-20`) manda a cópia logo depois, só se a original saiu
  SENT.

Nenhum embrulho pergunta pelo modo. O "onde vale" de cada regra (sempre, só no teste, só fora dele) é um dado da
montagem: `Scope` (`messaging/Scope.java:4-13`) e `SendingRule` (`messaging/SendingRule.java:6`), e
`MessagePipeline.assemble` empilha só as que valem no modo atual (`messaging/MessagePipeline.java:12-19`). A ordem
também resolve as combinações do texto: a validação vê o endereço original antes do desvio de teste, e a cópia
fica por fora da validação, então a mensagem bloqueada não gera cópia.

`Scope`, `SendingRule` e `MessagePipeline` não são estrutura sem necessidade: o texto pede, para as próximas regras
(madrugada, limite por dia), que cada uma possa valer "sempre, só no modo de teste ou só fora dele" e que cada
instalação escolha a sua combinação. O teste `MessagingTest.java:83-88` monta outra combinação. A interface
`MessageStore` tem uma implementação de produção, mas é substituída por uma em memória nos testes
(`MessagingTest.java:12-22`). Por isso, contenção.

Um detalhe: para gravar a mensagem bloqueada, `ValidatingSender` fala direto com o `MessageStore`
(`messaging/ValidatingSender.java:16`), sem passar pelo `next`. Um embrulho futuro posto por dentro dele (um log, por
exemplo) não veria as bloqueadas. Hoje isso não muda nada, e é a escolha que mantém o endereço original.

**O resto do código.**
- Os tipos são os certos para o domínio:
  - `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:15-17`, `domain/Pricing.java:26,33,36`);
  - `LocalDate` para as datas;
  - enums para situação, plano e tipo de quarto (`domain/ReservationStatus.java`, `domain/RatePlan.java`,
    `domain/RoomType.java`), com o desconto e a devolução no próprio enum (`RatePlan.java:17-23`), sem `switch`
    espalhado.
- As transições ficam num só `require` (`domain/Reservation.java:74-79`).
- Os métodos são curtos e não há duplicação que pese.
- Pontos menores:
  - o construtor de `Reservation` tem 13 parâmetros (`domain/Reservation.java:21-23`);
  - `Reservation` é mutável e tem `assignId` público (`Reservation.java:87`);
  - o texto do e-mail está montado à mão no serviço (`service/ReservationService.java:81-88`).

Um revisor exigente aceitaria o código como está.

## Testes

**Números:** 17 testes rodaram, 0 falhas, 0 erros, 0 pulados; 17 métodos escritos em 3 classes. Cobertura de 91,1% das
linhas (489/537) e 80,2% dos ramos (89/111). Em `messaging`: 59/59 linhas e 22/23 ramos; em `domain`: 139/140 linhas
(`measures/tests.md`).
**Nota:** bom

As regras são conferidas com valores, em três níveis.

**O domínio** (`DomainTest`):
- o exemplo de 599,76 em alta temporada (`DomainTest.java:24-27`);
- as bordas da temporada: 14/12, 15/12, 29/02, 01/03, 30/06 e 01/08 (`DomainTest.java:31-37`);
- o desconto com meio para o par: 116,97, 662,83, e 0,125 → 0,12 (`DomainTest.java:41-47`);
- o comprovante inteiro, com "1 diária" e a linha de desconto (`DomainTest.java:51-70`);
- as transições e as devoluções nos três casos (`DomainTest.java:73-103`);
- os dois CSV linha a linha (`DomainTest.java:120-144`).

**A parte do cenário** (`MessagingTest`), com valores exatos:
- a validação do endereço, com 9 casos (`MessagingTest.java:25-35`);
- a cópia: destinatário, "[Cópia] Oi" e o texto (`MessagingTest.java:38-48`);
- o bloqueio sem cópia (`MessagingTest.java:51-57`);
- o modo de teste, com o corpo exato "Destinatário original: ana@gmail.com\nTexto" e sem cópia
  (`MessagingTest.java:60-69`);
- o bloqueio no modo de teste, com o endereço e o assunto originais (`MessagingTest.java:72-80`).

A pergunta da mutação tem resposta em todas as regras novas:
- tirar o `"[Cópia] "` faz falhar `MessagingTest.java:46`;
- trocar `at < 1` por `at < 0` faz falhar `MessagingTest.java:30`;
- trocar o `Scope` da cópia para `ALWAYS` faz falhar `MessagingTest.java:63`;
- inverter a ordem validação/teste faz falhar `MessagingTest.java:78`.

**A API** (`ApiTest`), de ponta a ponta contra o PostgreSQL de `DB_URL`, num schema descartável: os códigos 409, 422,
404 e 400, a confirmação, as mensagens com a cópia, os relatórios e a segunda via (`ApiTest.java:71-114`).

Defeitos menores:
- os dois testes da API dividem o mesmo banco, e `fullFlow` espera exatamente 2 mensagens (`ApiTest.java:96`). Se o
  JUnit rodar `invalidEmailBlockedAndRefundOnCancel` primeiro, o teste quebra. Hoje passa só pela ordem que o JUnit
  escolhe;
- o texto completo do e-mail de confirmação não é conferido;
- o modo de teste não é testado pela API;
- não há teste de reservas simultâneas;
- sem banco, `ApiTest` é pulado em silêncio (`ApiTest.java:36,41`).

## Banco

**Números:** 2 tabelas (`messages`, `reservations`), ambas com chave primária. Dinheiro em `numeric(12,2)` nas 5 colunas,
datas em `date`. 0 chaves estrangeiras, 0 CHECK, 0 UNIQUE/EXCLUDE. As 4 colunas de situação, plano e canal são `text`
sem CHECK. Esquema criado por `CREATE TABLE` no Java, sem migrações. Nenhum pool (`DriverManager`); 1 transação
explícita (`measures/database.md`).
**Nota:** com problemas

O que está certo:
- os tipos (`persistence/Database.java:29-55`);
- `NOT NULL` em tudo o que é obrigatório, só `refund` nulo, como pede o texto;
- todo o SQL com parâmetros: os candidatos com `+` só concatenam a constante `COLUMNS`
  (`persistence/ReservationRepository.java:20,87,107,116`);
- conexões fechadas com try-with-resources;
- a regra "um quarto, uma reserva por noite", protegida na criação por uma transação com `pg_advisory_xact_lock` pelo
  número do quarto, seguida da consulta de sobreposição e do insert (`persistence/ReservationRepository.java:28-84`).
  Isso vale também entre processos, e o roteiro confirmou 10 de 10 rodadas simultâneas.

Os defeitos:
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:28,47`) não sabe
  acrescentar uma coluna numa tabela que já existe. O próximo pedido que mude o banco (os limites por hóspede, por
  exemplo) vai exigir mexer à mão no banco de quem já roda o sistema.
- **Nenhuma restrição.** Os quartos não estão no banco (`domain/RoomCatalog.java:11-16`), então `room_number` não
  tem chave estrangeira, e `status`, `rate_plan` e `channel` aceitam qualquer texto. A sobreposição de reservas
  depende só do código; não há `EXCLUDE` nem índice em `(room_number, check_in)` para a consulta de
  `ReservationRepository.java:37-38`.
- **As mudanças de situação não têm trava no banco.** Confirmar, cancelar e fazer o check-in leem a reserva e depois
  gravam em conexões separadas, sem `FOR UPDATE` e sem transação (`service/ReservationService.java:41-68`,
  `ReservationRepository.java:91-103`). Quem protege é o `synchronized` do serviço, que só vale dentro de um
  processo.
- **A confirmação e o e-mail não andam juntos.** Na confirmação, a reserva é gravada e só depois a mensagem
  (`ReservationService.java:44-45`). Se a gravação da mensagem falhar, a reserva fica confirmada sem e-mail.
- **Uma conexão nova por consulta, sem pool** (`persistence/Database.java:19-21`).

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam (`measures/behavior.md`):
- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- as regras de envio no modo normal: 9/9;
- as regras de envio no modo de teste: 5/5;
- duas reservas ao mesmo tempo: 2/2.

**Nota:** bom

Nenhuma falha. Para o dono da pousada, isto quer dizer:
- os preços batem com o texto, inclusive na alta temporada e com o arredondamento meio para o par (599,76; desconto
  37,48 e total 212,42 no não reembolsável);
- as recusas vêm com os códigos combinados com o site;
- o e-mail de confirmação traz o comprovante;
- cancelamentos e devoluções seguem os planos, e os dois relatórios saem como pedido;
- as reservas e as mensagens continuam lá depois de reiniciar.

Na parte nova:
- no modo normal, o e-mail válido sai e gera a cópia para o arquivo logo depois;
- os quatro endereços inválidos ficam BLOCKED, sem cópia;
- com `HOTEL_TEST_MODE=true`, tudo vai para a caixa de testes com "[TESTE] " e o destinatário original, sem cópia, e
  o endereço inválido continua bloqueado com o endereço original.

Duas reservas simultâneas para o mesmo quarto nunca foram criadas juntas. A falta de trava nas mudanças de situação
(coluna do banco) não aparece no roteiro, que roda um processo só.

## Dúvidas

- Na arquitetura, a web chama o repositório direto para os relatórios e as mensagens (`web/WebApp.java:49,53,57-58`),
  e o domínio carrega o código HTTP em `ApiException`. Li isso como ressalva, não como mistura de camadas que derrube
  a nota. Um revisor mais rígido poderia dar `com problemas`.
- Nos testes, a dependência de ordem em `ApiTest.java:96` não falhou nesta execução. Não verifiquei se outra ordem do
  JUnit a faria falhar, mas pela leitura faria.
