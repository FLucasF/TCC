# Revisão: manutenção, cenário 07

## Visão geral

A base é um Javalin em sete pacotes: `hotel.web` (rotas, `HttpApi`) chama `hotel.reservations` (`ReservationService`,
`ReservationStore` com JDBC), que chama `hotel.messages` (`EmailSender` → `SentMessages`, que grava na tabela
`messages`). Os dados ficam no PostgreSQL, com o esquema em migrações do Flyway, e a montagem é a classe `Hotel`. O
agente mexeu só em `hotel.messages`, em `Hotel` e nos testes. Criou `MailSettings` (lê o ambiente), `RuleScope`,
`MessageStatus` e a migração `V2__message_status.sql`, e pôs todas as regras de envio dentro de `EmailSender.send`
(`src/main/java/hotel/messages/EmailSender.java:36-63`). O sistema faz o que o dono pediu: o roteiro passou 65 de 65 e
os testes, 33 de 33. O mais sério está no design. O texto pede regras que se empilham em volta do envio e mudam por
modo e por instalação, e elas ficaram como uma série de `if` num método só, com o modo de teste consultado dentro de
cada regra. A próxima regra vai entrar no mesmo método.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Um ciclo, `hotel.documents` ↔
`hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). `hotel.messages` passou de 3 para 6
classes (de 75 para 295 linhas) e continua sem depender de nenhum outro pacote do projeto.
**Nota:** bom

A mudança ficou toda no pacote das mensagens, sem Javalin. A borda HTTP só passou a mostrar o campo novo: a rota
continua `ctx.json(sentMessages.all())` (`src/main/java/hotel/web/HttpApi.java:81`), e o `status` vem do record
`Message` (`src/main/java/hotel/messages/Message.java:3`). O SQL novo ficou isolado em `SentMessages`
(`src/main/java/hotel/messages/SentMessages.java:22-67`). A configuração é lida num lugar só, na montagem: `Hotel`
recebe o ambiente e entrega `MailSettings.fromEnvironment(environment)` ao `EmailSender`
(`src/main/java/hotel/Hotel.java:27-36`). Os testes podem passar outro ambiente (`Map.of()` em
`src/test/java/hotel/web/HttpApiTest.java:35`). A classe nova mais longa, `EmailSender`, tem 105 linhas, menos que as
maiores da base. O agente não criou nenhum ciclo.

## Design de baixo nível

**Números:** cinco regras de envio (o endereço inválido, a cópia, o modo de teste, a madrugada e o limite por dia), e
nenhuma delas virou uma peça própria: as cinco ficaram em `if` dentro de `EmailSender.send` e dos métodos privados do
`EmailSender`. Nenhum embrulho e nenhuma interface de envio.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O pedido é o caso típico do Decorator: regras em volta do mesmo envio, que se combinam conforme o modo e conforme a
instalação ("a segunda pousada não quer a cópia"). O agente fez o contrário do que a referência descreve como acerto.
`send` testa o endereço, a madrugada e o limite numa condição só (`EmailSender.java:38`). Depois pergunta pelo modo de
teste e, nele, desvia a mensagem (`EmailSender.java:41-50`). Por fim, grava a original e pergunta se há cópia
(`EmailSender.java:51-61`). Quem decide as combinações por modo são as próprias regras: a madrugada e o limite consultam
`settings.testMode()` por dentro (`EmailSender.java:80` e `:95`), e a regra "sem cópia no modo de teste" é só o
`return` antecipado da linha 49. A montagem (`Hotel.java:36`) não empilha nada, só entrega um record de opções. A
estrutura que existe (`MailSettings`, `RuleScope`) organiza as opções, não as regras. Cada regra nova pede um campo a
mais no record (`src/main/java/hotel/messages/MailSettings.java:19-27`), mais um trecho em `fromEnvironment` e mais um
`||` na linha 38. Pelos critérios da referência, isso é omissão, não malfeito: não há embrulho nenhum convivendo com o
jeito antigo.

Exagero: não há. `RuleScope` (`src/main/java/hotel/messages/RuleScope.java:4-23`) e `MessageStatus` são enums pequenos
e com uso. `MailSettings` é um record de configuração. Os três construtores de `EmailSender`
(`EmailSender.java:22-34`) só dão valores padrão.

O resto do código novo tem defeitos localizados:
- As opções ausentes viram `null`: `quietFrom`, `quietUntil` e um `Integer dailyLimit` anulável
  (`MailSettings.java:22-25`), conferidos com `== null` em `EmailSender.java:80` e `:95`, em vez de um tipo que diga
  "sem regra".
- `SentMessages.add` recebe sete parâmetros posicionais, quatro deles `String` seguidos, e o `originalRecipient` vai
  `null` na maior parte das chamadas (`SentMessages.java:22-29`, `EmailSender.java:39`, `:51` e `:53-60`).
- Uma mensagem `BLOCKED` não diz o motivo (endereço, madrugada ou limite), embora a recepção precise ligar para o
  hóspede (`EmailSender.java:38-39`).
- `isValidAddress` tem uma condição redundante: `dot < domain.length() - 1` já decorre das outras duas
  (`EmailSender.java:76`).
- `to == null ? "" : to` grava um texto vazio e devolve `null` no objeto (`SentMessages.java:35` e `:43`).

Os tipos de domínio estão certos (`Instant`, `LocalTime`, `ZoneId` e um `Clock` injetável para testar,
`EmailSender.java:30-37`).

## Testes

**Números:** 33 testes rodaram, 0 falharam, 0 pulados, 33 métodos escritos (a base tinha 26, todos mantidos). As
linhas cobertas foram de 93,8% na base para 93,5%, e os ramos, de 87,1% para 82,9%. Em `hotel.messages`, 114 de 124
linhas e 56 de 73 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** bom

O agente escreveu `EmailSenderTest` com 7 testes contra o banco de verdade, que conferem as regras com valores:
- A validação do endereço, com os exemplos do dono e outros casos de borda: `a@@b.com`, `a@.com`, `a@gmail.` e `null`
  (`src/test/java/hotel/messages/EmailSenderTest.java:36-45`).
- O endereço inválido: uma mensagem só, `BLOCKED`, com o endereço original (`EmailSenderTest.java:48-55`).
- A cópia logo depois da original, com `[Cópia] ` e o mesmo texto (`EmailSenderTest.java:58-67`), e a cópia
  desligada (`:70-74`).
- O modo de teste: o endereço de testes, o assunto, o texto exato com `Destinatário original: ...\n`, nenhuma cópia,
  e o endereço inválido bloqueado com o endereço original (`EmailSenderTest.java:77-90`).
- A madrugada que passa da meia-noite e o escopo `TEST_ONLY` (`:93-104`).
- O limite por dia, contado por hóspede, também no modo de teste, e zerado no dia seguinte (`:107-123`).

Os testes da base foram reforçados, não enfraquecidos: a contagem passou de 1 para 2 e eles passaram a conferir a
cópia (`src/test/java/hotel/reservations/ReservationServiceTest.java:92-96`, `:110`) e o `status` na API
(`src/test/java/hotel/web/HttpApiTest.java:81-83`).

A pergunta da mutação:
- Trocar `>=` por `>` no limite (`EmailSender.java:103`), tirar o `return` do modo de teste (que passaria a fazer
  cópia) ou a cópia em si: algum teste falha.
- Trocar `!time.isBefore(from)` por `time.isAfter(from)` (`EmailSender.java:90`), ou seja, liberar a mensagem às 22:00
  em ponto: nenhum teste falha.
- Inverter `NORMAL_ONLY` em `RuleScope.java:14`: nenhum teste falha.

Também falta um teste que ligue o modo de teste pela variável de ambiente, de ponta a ponta. São lacunas nas regras
extras, que o texto só anunciava. As três regras pedidas estão bem conferidas.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, nenhuma chave estrangeira
está sem índice e nenhuma data é guardada como texto. Há 2 migrações: `V1` igual à da base e `V2` nova. A conferência
acusou `messages.status` em `text` sem CHECK, assim como as outras colunas de situação, que já eram assim na base. Não
há nenhuma transação explícita (`measures/database.md`).
**Nota:** com problemas

A evolução foi feita do jeito certo. A migração nova é aditiva e tem valores padrão para as linhas que já existiam
(`src/main/resources/db/migration/V2__message_status.sql:3-6`), e a `V1` da base não foi tocada. Todo o SQL novo usa
parâmetros, e o que entra por `+` é constante (`SentMessages.java:31-33`, `:52-56`). As datas e horas são
`timestamptz` gravadas em UTC (`SentMessages.java:40`).

Os defeitos:
- **A original e a cópia não andam juntas.** São dois `sentMessages.add` seguidos, cada um com a sua conexão e sem
  transação (`EmailSender.java:51-61`, `SentMessages.java:30`). Se a segunda gravação falhar, a mensagem fica `SENT`
  sem a cópia que o contador exige, e a exceção volta para `confirm` depois de a reserva já ter sido gravada como
  confirmada (`src/main/java/hotel/reservations/ReservationService.java:62-68`). A falta de transação no `confirm` vem
  da base. A cópia solta é do agente.
- **`status` aceita qualquer texto** (`V2__message_status.sql:4`). Um CHECK com `SENT`/`BLOCKED` custaria uma linha.
  Isso segue o costume da base, então pesa pouco.
- **O limite por dia depende de um `synchronized` que só vale para um processo** (`EmailSender.java:36`). A consulta
  de contagem usa `lower(coalesce(original_recipient, recipient))` sem índice (`SentMessages.java:54-56`). Com o volume
  de uma pousada, isso não pesa.

## Comportamento

**Números:** 65 de 65 conferências passaram, nenhuma falhou (`measures/behavior.md`):
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário no modo normal: 9 de 9;
- o cenário no modo de teste: 5 de 5;
- as reservas simultâneas: 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo funciona como ele pediu. Os endereços "ana@gmail", "ana.gmail.com", "ana@@gmail.com" e
"@gmail.com" aparecem na lista como `BLOCKED`, com o endereço original e sem cópia. Toda mensagem enviada ganha logo
depois uma cópia para `arquivo@pousadamarealta.com.br` com `[Cópia] ` e o mesmo texto. Com `HOTEL_TEST_MODE=true`,
nada sai para o hóspede: a mensagem vai para `testes@...` com `[TESTE] ` e a linha "Destinatário original", sem cópia,
e o endereço inválido continua bloqueado com o endereço original. Nada do que já funcionava quebrou, e os dados
continuam lá depois de reiniciar.

## Dúvidas

- O agente decidiu que a mensagem da madrugada vira `BLOCKED` e nunca sai (`EmailSender.java:38`, README). O texto só
  dizia "não mandar mensagem de madrugada", e segurar a mensagem para mandar de manhã seria tão ou mais sensato. Como
  é uma regra só anunciada, o roteiro não a confere, e não pesei isso na nota.
- O limite por dia conta só as mensagens `SENT` para o hóspede (`SentMessages.java:53-56`). Daria para contar também
  as bloqueadas, e o texto não decide.
