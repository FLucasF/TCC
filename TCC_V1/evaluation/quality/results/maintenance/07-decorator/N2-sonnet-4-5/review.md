# Revisão: manutenção, cenário 07

## Visão geral

A base já era organizada em pacotes por assunto (`hotel.web` com as rotas do Javalin, `hotel.reservations` com o
serviço, as regras e o acesso ao banco das reservas, `hotel.messages` com o envio e a lista de mensagens,
`hotel.rooms`, `hotel.reports`, `hotel.documents`), montados em `Hotel`. Uma confirmação segue este caminho: `HttpApi`,
depois `ReservationService.confirm` (`src/main/java/hotel/reservations/ReservationService.java:56`), depois
`EmailSender.send`, e no fim `SentMessages.add`, que grava na tabela `messages` do PostgreSQL. O agente mexeu só em
`hotel.messages`, em `Hotel` e numa migração nova. O `EmailSender` agora passa cada mensagem por um `MessageProcessor`,
que aplica uma lista de `MessageHandler` (validação, modo de teste, cópia) e grava o resultado com um `status` novo.
O serviço funciona e passa nas 65 conferências do roteiro. O mais sério está no design da parte pedida: a combinação
por modo foi escrita dentro dos próprios embrulhos (`ArchiveCopyHandler` e `TestModeHandler` recebem o `testMode` e
perguntam por ele), e a montagem põe sempre os três. Fora do roteiro, achei dois defeitos de regra. Endereços como
`ana@.com` e `ana@gmail.` são aceitos. A mensagem bloqueada é gravada com o assunto e o texto vazios.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base
eram os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. `hotel.messages` passou de 4 para 12 classes
(285 linhas) e continua sem depender de nenhum pacote do projeto.
**Nota:** bom

A mudança ficou toda no pacote certo. As regras novas (`src/main/java/hotel/messages/EmailValidationHandler.java`,
`TestModeHandler.java`, `ArchiveCopyHandler.java`) não conhecem nem o Javalin nem o JDBC. O SQL continua só em
`SentMessages` (`src/main/java/hotel/messages/SentMessages.java:20-31`). A leitura de `HOTEL_TEST_MODE` e a montagem
da corrente ficam num lugar só, a classe de montagem (`src/main/java/hotel/Hotel.java:26` e `:32-36`). A borda HTTP
não mudou: `GET /messages` só serializa a lista (`src/main/java/hotel/web/HttpApi.java:81`). O único ciclo
(`ReservationReceipt` importa `Reservation`, `src/main/java/hotel/documents/ReservationReceipt.java:3`, e o serviço
importa o recibo, `src/main/java/hotel/reservations/ReservationService.java:3`) veio da base, e o agente não o piorou.

## Design de baixo nível

**Números:** 3 regras, as 3 numa peça própria (`MessageHandler`). Mas 2 das 3 decidem por dentro se valem conforme o
modo (`ArchiveCopyHandler.java:18`, `TestModeHandler.java:15`), e a montagem liga sempre as 3, sem olhar o modo
(`Hotel.java:32-36`). São 6 tipos novos para a parte pedida: a interface, o processador, 2 records de mensagem, o
envelope de resultado e o enum.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O agente fez uma lista de filtros aplicada pelo envio. A referência aceita isso como alternativa ao
Decorator, desde que a combinação por modo seja decidida na montagem e não dentro das regras.

- O que aproxima: cada regra é uma classe própria com a mesma interface
  (`src/main/java/hotel/messages/MessageHandler.java:13`). O `EmailSender` não pergunta pelo modo de teste
  (`src/main/java/hotel/messages/EmailSender.java:16-28`). A ordem e a escolha das peças estão em `Hotel`.
- O que afasta: a combinação está escrita dentro das regras. `ArchiveCopyHandler` recebe o `testMode` e, se ele
  estiver ligado, só repassa a mensagem (`src/main/java/hotel/messages/ArchiveCopyHandler.java:12-20`). É o exemplo
  de malfeito que a referência dá ("a cópia que verifica se está em modo de teste"). `TestModeHandler` faz o mesmo ao
  contrário (`src/main/java/hotel/messages/TestModeHandler.java:15-17`). A montagem põe as três peças em qualquer
  modo (`src/main/java/hotel/Hotel.java:32-36`), em vez de deixar de fora a que não vale.
- A consequência: a segunda pousada, que não quer a cópia, até pode tirar a peça na montagem. Mas uma regra nova "só
  no modo de teste" ou "só fora dele" (a madrugada, o limite por dia) vai precisar de mais um `boolean testMode`
  dentro dela. É justamente o eixo que o texto anuncia.

**O exagero.** Não há estrutura fora da parte do pattern: nenhuma fábrica, nenhum builder, nenhuma camada que só
repassa. Os tipos novos servem à corrente. Por isso, contenção. Eles pesam na nota, logo abaixo.

**O resto do código.**
- O protocolo entre as peças é implícito e frágil. `ProcessedMessage` é uma lista em que a posição 0 é "a" mensagem e
  o resto são extras, e o `MessageProcessor` depende dessa convenção: lê `get(0)` quatro vezes, guarda o estado
  `blocked`, limpa a lista acumulada e no fim remonta tudo (`src/main/java/hotel/messages/MessageProcessor.java:15-54`).
  `ArchiveCopyHandler` devolve a original mais a cópia (`ArchiveCopyHandler.java:22-30`), só que a original é
  descartada e reconstruída pelo processador (`MessageProcessor.java:32-36`, `:48-51`). As cópias também não passam
  pelas peças seguintes, então um limite por dia posto depois da cópia não a veria.
- Há 3 records para a mesma mensagem: `OutgoingMessage`, `ProcessedMessage.MessageToStore` e `Message`.
- A mensagem bloqueada perde o conteúdo. `ProcessedMessage.blocked(to)` grava o assunto e o texto como `""`
  (`src/main/java/hotel/messages/ProcessedMessage.java:12-14`), e a recepção vê na lista uma mensagem vazia, sem
  saber qual e-mail não saiu.
- A validação não segue a regra do texto ("um nome com pelo menos um ponto no meio"): basta conter um ponto
  (`src/main/java/hotel/messages/EmailValidationHandler.java:34`).
- `EmailSender.send` começa com `firstMessage = null` e o devolve assim se a lista vier vazia
  (`EmailSender.java:21-28`).
- O que está bem: o `status` virou um `enum` (`src/main/java/hotel/messages/MessageStatus.java`), e os campos são
  `final`, sem `static` mutável.

## Testes

**Números:** 32 testes rodaram, 0 falhas, 0 erros, 0 pulados; 32 métodos escritos (a base tinha 26, todos mantidos,
e 6 são novos). Cobertura de linhas: 94,5% (554 de 586; na base, 93,8%). Cobertura de ramos: 87,8% (130 de 148; na
base, 87,1%). `hotel.messages`: 110 de 115 linhas e 31 de 34 ramos.
**Nota:** com problemas

**O que está bem.** Os testes novos conferem valores, não só o status 200
(`src/test/java/hotel/messages/MessageProcessingTest.java`):
- os 4 endereços inválidos ficam `BLOCKED` (`:21-40`);
- a cópia vem logo depois da original, com "[Cópia] " no assunto (`:108-133`);
- no modo de teste, a mensagem vai para `testes@…`, com "[TESTE] " e com a primeira linha "Destinatário original: …"
  (`:60-78`);
- no modo de teste não há cópia (`:80-92`), e a bloqueada também não gera cópia (`:94-106`).

Os testes da base não foram enfraquecidos. As contagens passaram de 1 para 2 mensagens, e o agente acrescentou
asserções sobre a cópia (`src/test/java/hotel/reservations/ReservationServiceTest.java:101-110`,
`src/test/java/hotel/web/HttpApiTest.java:81-83`).

**O que fica sem rede.**
- O bloqueio no modo de teste, com o endereço original, não tem teste nenhum. Se alguém pusesse o `TestModeHandler`
  antes do `EmailValidationHandler` em `Hotel.java:32-36`, todo endereço inválido sairia para `testes@…` no modo de
  teste, e nenhum teste falharia: os testes montam a própria lista (repetida em 3 arquivos) e não a de `Hotel`.
  `HttpApiTest` usa `Hotel` só no modo normal.
- O "ponto no meio" do domínio não é testado. `contains(".")` passaria em todos os testes, e por isso o defeito de
  `ana@.com` e `ana@gmail.` passou.
- Nenhum teste confere o campo `status` no JSON de `GET /messages`, nem o assunto e o texto da mensagem bloqueada.
- Nenhum teste confere a leitura de `HOTEL_TEST_MODE`.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`. Nenhuma chave estrangeira sem índice
e nenhuma data em texto. Uma migração nova (`V2__add_message_status.sql`), e `V1__create_tables.sql` ficou igual à da
base. As colunas de situação (`messages.status` e as da base) são `text` sem CHECK. Nenhuma transação explícita (já
era assim na base).
**Nota:** bom

O agente evoluiu o esquema do jeito certo. Fez uma migração nova, com `ALTER TABLE messages ADD COLUMN status TEXT
NOT NULL DEFAULT 'SENT'` (`src/main/resources/db/migration/V2__add_message_status.sql:2`), sem editar a V1. O
`DEFAULT` mantém as mensagens que já estavam no banco válidas como enviadas. O SQL novo usa parâmetros (`?`) e
try-with-resources (`src/main/java/hotel/messages/SentMessages.java:20-31`). O que o script lista como SQL montado com
`+` é tudo constante e veio da base (`ReservationStore.java:33`, `:83`, `:98`, `:111`). Faltou um
`CHECK (status IN ('SENT','BLOCKED'))` em `messages.status`, mas a base também não protege as suas colunas de
situação. A proteção contra duas reservas na mesma noite (o `EXCLUDE` com `gist`) veio da base e continua lá. O
assunto e o texto vazios da mensagem bloqueada são um defeito do código (veja o design), não do esquema.

## Comportamento

**Números:** 65 de 65 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário no modo normal: 9 de 9;
- o cenário no modo de teste: 5 de 5;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** com problemas

O roteiro inteiro passa. A parte comum continua igual, o `status` aparece em cada mensagem, a cópia vem logo depois
da original, e o modo de teste desvia, marca o assunto, acrescenta a primeira linha e não faz cópia. Os endereços
inválidos do roteiro ficam `BLOCKED`, com o endereço original, também no modo de teste.

Fora do roteiro, chamei o `MessageProcessor` compilado com a montagem do modo normal e achei dois defeitos que o dono
vai notar.

1. **Endereços que deviam ser bloqueados saem.** `ana@.com`, `ana@gmail.` e `ana@gmail..com` saem como `SENT` e
   ainda geram a cópia para o arquivo. O texto pede "um nome com pelo menos um ponto no meio", e nesses casos o ponto
   está na ponta. Para o dono: um hóspede que digitar `ana@gmail.` não recebe nada, e a recepção não fica sabendo
   que precisa ligar. É o problema que a primeira regra devia resolver.
2. **A mensagem bloqueada aparece vazia.** Em `GET /messages`, ela vem com o endereço certo, mas com o assunto e o
   texto vazios (`ProcessedMessage.java:12-14`). A recepção sabe para quem ligar, mas não sabe o que o hóspede
   deixou de receber (a confirmação, o comprovante).

## Dúvidas

- A mensagem bloqueada sem assunto e sem texto: o texto só pede que ela "apareça na lista como bloqueada, para a
  recepção ligar", então guardar só o endereço pode ser uma leitura possível. Contei como defeito porque a recepção
  perde o conteúdo do que não saiu.
- A classe: a montagem até escolhe as peças, e daria para tirar a cópia na segunda pousada. Mas a referência dá como
  malfeito justamente "a cópia que verifica se está em modo de teste", e é o que `ArchiveCopyHandler.java:18` faz.
  Por isso, malfeito, e não alternativa equivalente.
- O README não foi atualizado com `HOTEL_TEST_MODE` nem com o campo `status`. O patch não toca nele, e o texto não
  pedia isso, então não pesou em nenhuma nota.
