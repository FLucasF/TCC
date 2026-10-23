# Revisão: manutenção, cenário 07

## Visão geral

O sistema da base tem sete pacotes: `hotel` (montagem e banco), `hotel.web` (rotas Javalin), `hotel.reservations`
(regras e acesso aos dados das reservas), `hotel.rooms`, `hotel.documents`, `hotel.reports` e `hotel.messages`. Uma
requisição entra em `HttpApi` e chama o `ReservationService`. O serviço grava pelo `ReservationStore`, e na
confirmação manda o e-mail por `EmailSender.send` (`src/main/java/hotel/reservations/ReservationService.java:65`), que
guarda a mensagem na tabela `messages` pelo `SentMessages`. Todos os dados ficam no PostgreSQL, com o esquema criado
por migrações do Flyway. O agente mexeu só em `hotel.messages`, na montagem (`Hotel`) e numa migração nova (`V2`).
Agora, cada regra de envio é uma classe que implementa `MessageRule`. `ValidAddress`, `TestRedirect` e `ArchiveCopy`
são as três regras. O `ScopedRule` embrulha cada uma com o seu `Scope` (sempre, só no teste, só fora dele). A lista
para a instalação é montada em `MessageRules.forInstallation(testMode)`, chamada por `Hotel`. O `EmailSender` aplica
essa lista. Os 65 itens do roteiro passaram, os 30 testes passaram, e o agente não trouxe ciclo novo nem defeito
sério. Os pontos fracos são pequenos. A combinação de regras de cada instalação está escrita no código e não vem de
configuração. A ordem das regras importa sem que nada a declare. A mensagem original e a cópia são gravadas fora de
uma transação.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base.
`hotel.messages` passou de 3 para 12 classes (de 75 para 260 linhas) e continua sem depender de nenhum pacote do projeto.
**Nota:** bom

A mudança ficou toda dentro de `hotel.messages`, e as ligações entre os pacotes não mudaram. O modo de teste é lido
uma vez só, no `main` (`src/main/java/hotel/Hotel.java:50`). Ele entra na montagem por `Hotel(DataSource, boolean)`
(`src/main/java/hotel/Hotel.java:25-31`), que entrega a lista de regras ao `EmailSender`. Nenhuma regra lê variável
de ambiente nem conhece o Javalin. Só o `SentMessages` acessa o banco (`src/main/java/hotel/messages/SentMessages.java:20-37`).
A rota `/messages` continua fina (`src/main/java/hotel/web/HttpApi.java:81`), e o campo `status` aparece no JSON
porque o agente o acrescentou ao record `Message` (`src/main/java/hotel/messages/Message.java:3`). O ciclo
`documents` ↔ `reservations` veio da base. Nenhuma classe nova é grande: a maior é `SentMessages`, com 59 linhas.

## Design de baixo nível

**Números:** 3 de 3 regras pedidas estão na estrutura (`ValidAddress`, `TestRedirect`, `ArchiveCopy`), cada uma
numa classe. 0 regras ficaram em `if` dentro do envio. A combinação por modo fica num só lugar,
`MessageRules.forInstallation`, com 3 `ScopedRule`. Não havia casos antigos para migrar: na base, o envio só gravava.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

O que o agente fez não são embrulhos do envio, em que cada um chama o próximo. É uma lista de filtros que o
`EmailSender` aplica (`src/main/java/hotel/messages/EmailSender.java:23-33`): primeiro o `apply` de cada regra, que
pode mudar ou bloquear o `Draft`, e depois, se a mensagem não foi bloqueada, o `extrasFor` de cada regra, que pede as
mensagens extras. A referência aceita essa forma de antemão, porque as duas condições valem:

- cada regra é uma peça própria: `ValidAddress.java:6-16`, `TestRedirect.java:8-16` e `ArchiveCopy.java:10-13`;
- a combinação por modo é decidida fora das regras. Nenhuma regra pergunta pelo modo de teste. Quem decide é o
  `ScopedRule` (`src/main/java/hotel/messages/ScopedRule.java:19-26`, um Decorator de verdade sobre `MessageRule`),
  com o `Scope` (`src/main/java/hotel/messages/Scope.java:9-15`). Ele é montado em `MessageRules.java:12-16` e chamado
  por `Hotel.java:31`.

As regras anunciadas, de madrugada e do limite diário, cabem nisso: cada uma vira uma classe com o seu `Scope`.

O que afasta do ideal:

1. A ordem da lista importa e nada a declara. `ValidAddress` precisa vir antes de `TestRedirect`. Se a ordem fosse
   trocada, o endereço `testes@...` passaria pela validação. Por isso `TestRedirect` olha se a mensagem já foi
   bloqueada por outra regra (`TestRedirect.java:10-12`), um acoplamento entre regras.
2. "Cada instalação pode querer outra combinação" ficou escrito no código. `MessageRules.forInstallation` só recebe
   `testMode`, e a segunda pousada, sem a cópia, exige mudar o código ou criar outro método. Isso ainda é decidido na
   montagem, então não muda a classe.
3. A interface `MessageRule` tem dois papéis, cada um com um default vazio (`MessageRule.java:11-18`). Cada regra só
   implementa um deles.

Não há exagero. `Scope` e `ScopedRule` respondem ao que o texto pede ("pode valer sempre, só no modo de teste ou só
fora dele"). `Draft` é o objeto que atravessa as regras, e `MessageStatus` é o tipo do campo pedido. Os construtores
curtos (`EmailSender.java:12-14`, `Hotel.java:21-23`) mantêm os testes da base sem mudança. Não há fábrica, builder
nem camada que só repassa.

No resto do código mudado: `Draft` é um record imutável e devolve uma cópia com `blocked()` (`Draft.java:4-16`); o
status é um `enum` (`MessageStatus.java:3-6`) e não um texto solto. `SentMessages.add` recebe o `Draft` e passou a
gravar também o status (`SentMessages.java:20-37`). A validação do endereço está numa função pequena e testável
(`ValidAddress.java:11-18`). Ela tem uma conferência redundante: `indexOf('.', 1) > 0` junto com
`!startsWith(".")`. Também aceita espaços, que o texto não proíbe. Não vi duplicação, estado mutável nem exceção
engolida.

## Testes

**Números:** 30 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 30 métodos escritos; a base tinha 26, todos
mantidos, e o agente escreveu 4 em `EmailSenderTest`. A cobertura de linhas é de 94,4% (535/567) e a de ramos, de
89,1% (131/147); na base eram 93,8% e 87,1%. Em `hotel.messages`, são 90/94 linhas e 32/33 ramos
(`measures/tests.md`).
**Nota:** bom

Os testes novos conferem as regras com valores, contra o banco de verdade:

- a cópia vem logo depois da original, com o endereço do arquivo, o assunto `[Cópia] Olá`, o mesmo texto e SENT
  (`src/test/java/hotel/messages/EmailSenderTest.java:27-38`);
- sete endereços inválidos e um válido passam por `isValid`, e o envio para "ana@gmail" fica BLOCKED, com o endereço
  original e sem cópia (`EmailSenderTest.java:41-52`);
- no modo de teste, a mensagem vai para o endereço de testes, com `[TESTE] Olá`, o corpo exato
  `"Destinatário original: ana@gmail.com\nTexto"` e sem cópia (`EmailSenderTest.java:55-63`);
- no modo de teste, o endereço inválido continua bloqueado e com o endereço original (`EmailSenderTest.java:66-73`).

O único teste antigo que o agente mudou foi o da API. A mudança está certa: a confirmação agora gera 2 mensagens, e o
teste confere o `status` no JSON e a cópia para o arquivo (`src/test/java/hotel/web/HttpApiTest.java:81-84`).

A pergunta da mutação:

- trocar o prefixo `[Cópia] ` ou `[TESTE] ` derruba um teste;
- inverter um `Scope` derruba a contagem de mensagens;
- trocar `at < 1` por `at < 0` falha no caso `@gmail.com`;
- apagar o `if (!draft.isBlocked())` de `EmailSender.java:28` falha na contagem de mensagens.

Ficam sem rede:

- a leitura de `HOTEL_TEST_MODE` no `main` (`Hotel.java:50`), que o roteiro de comportamento cobre;
- o assunto e o corpo da mensagem bloqueada fora do modo de teste.

## Banco

**Números:** 3 tabelas, todas com chave primária, criadas por 2 migrações do Flyway. A V1 está igual à da base, e a
V2 é nova. O dinheiro está em `numeric(10,2)`, nenhuma chave estrangeira está sem índice e nenhuma data está em
texto. As conferências acusaram só colunas de situação sem CHECK, entre elas a nova `messages.status`
(`measures/database.md`).
**Nota:** bom

O agente fez a mudança de esquema do jeito certo. Criou uma migração nova
(`src/main/resources/db/migration/V2__message_status.sql:2`, `ALTER TABLE messages ADD COLUMN status text NOT NULL
DEFAULT 'SENT'`) e não editou a V1. As mensagens que já existem continuam válidas, como SENT. O INSERT e o SELECT
usam parâmetros (`src/main/java/hotel/messages/SentMessages.java:23-29`), e o status é lido de volta com
`MessageStatus.valueOf` (`SentMessages.java:52`).

Ressalvas pequenas:

- `messages.status` não tem `CHECK (status IN ('SENT','BLOCKED'))`. Isso segue o estilo da base: nenhuma coluna de
  situação tem CHECK.
- A original e a cópia são dois INSERTs, cada um na sua conexão, sem transação (`EmailSender.java:27-31`). Se a cópia
  falhar, a original fica gravada sem a cópia, e a confirmação, já gravada (`ReservationService.java:62-65`), responde
  com erro. Gravar a reserva e mandar o e-mail fora de uma transação já vinha da base.

## Comportamento

**Números:** 65 de 65 itens passaram (`measures/behavior.md`):

- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário, modo normal: 9/9;
- cenário, modo de teste (`HOTEL_TEST_MODE=true`): 5/5;
- duas reservas ao mesmo tempo: 2/2.

**Nota:** bom

Para o dono da pousada, as três regras funcionam pela API do jeito que ele pediu:

- **e-mail digitado errado:** "ana@gmail", "ana.gmail.com", "ana@@gmail.com" e "@gmail.com" ficam na lista como
  BLOCKED, com o endereço original e sem cópia;
- **cópia para o contador:** toda mensagem enviada gera, logo depois, uma cópia para `arquivo@pousadamarealta.com.br`,
  com `[Cópia] ` no assunto e o mesmo texto;
- **modo de teste:** com a variável ligada, nada sai para o hóspede. A mensagem vai para
  `testes@pousadamarealta.com.br`, com `[TESTE] ` no assunto e a primeira linha "Destinatário original: …", sem a
  cópia, e o endereço inválido continua bloqueado.

O que já funcionava continua funcionando, inclusive depois de reiniciar e com duas reservas ao mesmo tempo.

## Dúvidas

- A classe ficou entre `acerto` e `alternativa equivalente`. O envio não é embrulhado (é uma lista de filtros com
  duas fases, aplicada pelo `EmailSender`). O Decorator aparece só no `ScopedRule`, que embrulha a regra e não o
  envio. Por isso dei `alternativa equivalente`.
- O texto diz que "cada instalação do sistema pode querer outra combinação", mas não diz como essa escolha deve ser
  feita. A solução não deixa a segunda pousada desligar a cópia sem mudar `MessageRules.java:12-16`. Não contei isso
  como defeito do pattern, mas a próxima instalação vai exigir uma mudança aí.
