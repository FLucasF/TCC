# Revisão: manutenção, cenário 07

## Visão geral

Os pacotes da base continuam os mesmos: `hotel` (a montagem), `hotel.web` (o Javalin), `hotel.reservations`,
`hotel.rooms`, `hotel.documents`, `hotel.reports` e `hotel.messages`. Uma requisição entra pela `HttpApi`, passa
pelo `ReservationService` e chega aos stores JDBC. Os dados ficam no PostgreSQL, com o esquema criado pelas migrações
do Flyway. O agente pôs a mudança toda em `hotel.messages`. Criou uma interface `MessageRule`
(`List<OutgoingMessage> apply(OutgoingMessage)`) e três regras: `EmailValidationRule`, `TestModeRule` e
`ArchiveCopyRule`. O `EmailSender.send` aplica as regras em sequência e grava o que sobra. A lista é montada em
`Hotel`, e o modo de teste vem de `HOTEL_TEST_MODE`. A status da mensagem é um `enum`, guardada numa migração nova
(`V2__add_message_status.sql`). O comportamento pedido funciona: o roteiro passou em 65 de 65 conferências, e os 37
testes passam. O que há de mais sério está no design. A escolha por modo ficou dentro das regras: `TestModeRule` e
`ArchiveCopyRule` recebem o `boolean` do modo e se desligam sozinhas, e a montagem empilha sempre as mesmas três. É
justamente o que a referência chama de malfeito. Há também um defeito pequeno de regra: o validador aceita
`ana@gmail.`, embora o texto peça o ponto "no meio".

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, as mesmas da base. Há 1 ciclo (`hotel.documents` ↔
`hotel.reservations`), que já existia na base. `hotel.messages` passou de 3 classes e 75 linhas para 10 classes e
255 linhas, continua sem depender de nenhum pacote do projeto e só usa JDBC (em `SentMessages`).
**Nota:** bom

As peças novas ficam todas em `hotel.messages`, sem Javalin e sem SQL fora de `SentMessages`
(`src/main/java/hotel/messages/SentMessages.java:20-35`). A montagem fica num lugar só: a lista de regras é criada e
ligada ao `EmailSender` em `src/main/java/hotel/Hotel.java:36-42`, e a variável de ambiente é lida também na
montagem (`src/main/java/hotel/Hotel.java:48-51`), não dentro das regras. A borda HTTP não mudou: `GET /messages`
continua sendo `ctx.json(sentMessages.all())` (`src/main/java/hotel/web/HttpApi.java:81`). O `ReservationService`
continua chamando só `emailSender.send(...)` (`src/main/java/hotel/reservations/ReservationService.java:65`). O
agente não criou dependência nova nem ciclo novo. O ciclo `documents`/`reservations` veio da base.

## Design de baixo nível

**Números:** 3 regras do texto e as 3 estão na estrutura (validação, desvio para testes, cópia para o arquivo). 2
das 3 trazem dentro delas a escolha por modo (`isTestMode`). A montagem não muda conforme o modo: põe sempre as
mesmas três regras.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A estrutura é a de uma lista de filtros aplicada pelo envio (`src/main/java/hotel/messages/EmailSender.java:21-27`),
com uma peça para cada regra. Isso é o que a referência aceita como alternativa equivalente, e as três regras do
texto estão nela. O que afasta é onde ficou a combinação por modo:

- `ArchiveCopyRule` pergunta se está em modo de teste:
  `if (isTestMode || message.status() == MessageStatus.BLOCKED)` (`src/main/java/hotel/messages/ArchiveCopyRule.java:19`).
  É o exemplo exato de malfeito da referência ("a cópia que verifica se está em modo de teste").
- `TestModeRule` também recebe o modo e se desliga sozinha (`src/main/java/hotel/messages/TestModeRule.java:18`).
- A montagem sempre põe as três (`src/main/java/hotel/Hotel.java:37-39`) e só repassa o `boolean` do modo. A decisão
  de qual regra vale em qual modo não está na montagem.

A próxima regra anunciada no pedido ("só no modo de teste ou só fora dele", e "a segunda pousada não quer a cópia")
vai exigir mais um `boolean`/`if` dentro de cada regra, ou uma mudança no `Hotel`, em vez de só empilhar peças
diferentes. As regras também dependem da ordem e do estado deixado pela anterior. A cópia e o desvio olham
`status() == BLOCKED`, que só a `EmailValidationRule` preenche (`src/main/java/hotel/messages/TestModeRule.java:18`,
`src/main/java/hotel/messages/ArchiveCopyRule.java:19`). Se alguém trocar a ordem da lista, o comportamento muda sem
erro nenhum.

**O exagero.** Contenção. A interface `MessageRule` tem três implementações, e o `EmailValidator`, separado da regra,
é uma classe concreta pequena (`src/main/java/hotel/messages/EmailValidator.java`), sem interface nem fábrica. Os três
arquivos de documentação novos (`EXEMPLO_USO.md`, `MUDANCAS.md`, `SUMARIO_IMPLEMENTACAO.md`) são ruído, mas não são
estrutura de código. O README, por outro lado, não foi atualizado com `HOTEL_TEST_MODE` nem com o `status`.

**O resto do código.**
- `OutgoingMessage` é uma classe mutável, com setters para todos os campos
  (`src/main/java/hotel/messages/OutgoingMessage.java:20,28,36,44`). As regras mudam a mensagem que recebem e a
  devolvem (`src/main/java/hotel/messages/EmailValidationRule.java:16`,
  `src/main/java/hotel/messages/TestModeRule.java:20-24`), mas a `ArchiveCopyRule` cria uma mensagem nova
  (`src/main/java/hotel/messages/ArchiveCopyRule.java:24`). As duas formas convivem, e um `record` com cópias
  (`withTo`, `blocked()`) seria o natural.
- `EmailSender.send` guarda a primeira mensagem gravada com um `null` inicial e um `if` dentro do laço
  (`src/main/java/hotel/messages/EmailSender.java:29-36`).
- A regra do texto ("um nome com pelo menos um ponto no meio") virou `domainPart.contains(".")`
  (`src/main/java/hotel/messages/EmailValidator.java:18`), que aceita o ponto na ponta (o efeito está na coluna
  Comportamento).

Do lado bom, a status é um `enum` (`MessageStatus`), e as constantes de endereço e prefixo têm nome.

## Testes

**Números:** 37 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 37 métodos escritos, 11 deles novos, e os 26
da base foram mantidos. A cobertura de linhas foi de 93,8% na base para 94,0% (551 de 586), e a de ramos de 87,1%
para 89,0% (130 de 146). Em `hotel.messages`, são 105 de 109 linhas e 31 de 32 ramos. `hotel` cobre 2 de 4 ramos.
**Nota:** bom

As regras novas são conferidas com valores, nos dois níveis:
- **Com o serviço e o banco:** `src/test/java/hotel/messages/MessageRulesTest.java`. O bloqueio é conferido com
  status `BLOCKED` e o endereço original (`:40-51`). O desvio no modo de teste confere o endereço `testes@...`, o
  prefixo `[TESTE] ` e a primeira linha `Destinatário original: original@email.com\n` (`:68-84`). A cópia confere o
  endereço do arquivo, o prefixo `[Cópia] `, o mesmo corpo e a posição logo depois da original (`:86-108`). Também
  há testes para a falta de cópia no modo de teste (`:110-124`), para a falta de cópia na mensagem bloqueada
  (`:126-141`) e para o bloqueio no modo de teste com o endereço original (`:143-158`).
- **Pela API, com o `Hotel` montado:** `src/test/java/hotel/messages/ProductionModeTest.java:58-97` cobre o modo
  normal. `src/test/java/hotel/web/HttpApiTest.java:79-84` cobre o modo de teste.
- **O validador:** os exemplos do texto e mais alguns (`src/test/java/hotel/messages/EmailValidatorTest.java:12-28`).

A asserção da base em `HttpApiTest` (o e-mail para `carla@example.com`) foi trocada por asserções do modo de teste.
Isso não enfraquece o teste, porque o modo normal ficou coberto em `ProductionModeTest`.

A pergunta da mutação deixa algumas regras sem rede:
- trocar `"true".equalsIgnoreCase` por outra leitura da variável `HOTEL_TEST_MODE` (`src/main/java/hotel/Hotel.java:48-51`).
  Nenhum teste passa pelo construtor que lê o ambiente.
- o ponto "no meio" do domínio. Nenhum teste tem `ana@gmail.` ou `ana@.com`, e por isso o defeito do validador passou.

São lacunas pequenas perto do que está conferido.

## Banco

**Números:** 3 tabelas e 2 migrações (`V1` igual à da base, `V2__add_message_status.sql` nova). Nenhuma tabela sem
chave primária, o dinheiro está todo em `numeric(10,2)`, não há chave estrangeira sem índice nem data em texto, e a
restrição `EXCLUDE` contra reserva dupla vem da base. As conferências apontam colunas de status/tipo sem `CHECK`,
entre elas a nova `messages.status`.
**Nota:** bom

O agente fez a mudança de esquema do jeito certo, com uma migração nova e sem editar a V1:
`ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT'`
(`src/main/resources/db/migration/V2__add_message_status.sql:2`). Num banco que já tem mensagens, as antigas ficam
`SENT`, o que faz sentido. O SQL novo usa parâmetros (`src/main/java/hotel/messages/SentMessages.java:23-29`). A
leitura converte a coluna para o `enum` (`src/main/java/hotel/messages/SentMessages.java:51`).

`messages.status` não tem `CHECK (status IN ('SENT','BLOCKED'))`, mas segue a convenção da base (`rooms.type`,
`reservations.status` e `rate_plan` também não têm). Um ponto menor: a original e a cópia são gravadas em duas
conexões, sem transação (`src/main/java/hotel/messages/EmailSender.java:30-35`). Uma falha entre as duas deixaria a
original sem cópia. Na base, a confirmação da reserva e a gravação do e-mail também não andam juntas.

## Comportamento

**Números:** 65 de 65 conferências passaram. Foram 46 de 46 na parte comum, 3 de 3 depois de reiniciar, 9 de 9 no
cenário em modo normal, 5 de 5 no cenário em modo de teste e 2 de 2 nas reservas simultâneas.
**Nota:** bom

Para o dono da pousada, tudo o que o roteiro confere funciona:
- O e-mail válido sai com status `SENT`, e a cópia para `arquivo@pousadamarealta.com.br` vem logo depois, com
  `[Cópia] ` no assunto e o mesmo texto.
- `ana@gmail`, `ana.gmail.com`, `ana@@gmail.com` e `@gmail.com` ficam `BLOCKED`, com o endereço original e sem cópia.
- Com `HOTEL_TEST_MODE=true`, tudo vai para `testes@...`, com `[TESTE] ` no assunto e a linha
  `Destinatário original: ...`, sem cópia, e o endereço inválido continua bloqueado.
- A parte que já existia não quebrou.

Há uma falha fora do roteiro. Rodei o `EmailValidator` compilado com outros endereços, e ele dá válido para
`ana@gmail.`, `ana@.com` e `ana@gmail..`, porque só confere se há um ponto em algum lugar depois do @
(`src/main/java/hotel/messages/EmailValidator.java:18`). O texto pede "um nome com pelo menos um ponto no meio".
Na prática, um hóspede que digite `ana@gmail.` teria a mensagem "enviada" para lugar nenhum, em vez de bloqueada para
a recepção ligar. É um caso de borda da regra, não do caminho principal. Por isso a nota fica em `bom`, e a ressalva
fica registrada em Dúvidas.

## Dúvidas

- **A classe do pattern.** A forma é a de uma alternativa equivalente (uma lista de filtros, uma peça por regra), mas
  a combinação por modo ficou dentro de duas regras (`ArchiveCopyRule.java:19`, `TestModeRule.java:18`), e a montagem
  não varia com o modo. Como a referência dá exatamente esse caso como exemplo de malfeito, fiquei com `malfeito`.
- **A nota do comportamento.** O validador aceita o ponto na ponta do domínio (`ana@gmail.`), contra o "no meio" do
  texto. Como o roteiro inteiro passou e o caso é de borda, dei `bom`. Um revisor mais rígido poderia dar
  `com problemas` por ser um desvio explícito do que o texto diz.
