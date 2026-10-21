# Revisão: manutenção, cenário 07

## Visão geral

O sistema da base é dividido em sete pacotes: `hotel` (a montagem e o `main`), `web` (as rotas do Javalin),
`reservations` (as regras e o `ReservationStore` com o JDBC), `rooms`, `messages`, `documents` e `reports`. Uma
requisição entra em `HttpApi`, chama o `ReservationService`, que grava pelo `ReservationStore` e, na confirmação, manda
o e-mail pelo `EmailSender`, que guarda a mensagem na tabela `messages` pelo `SentMessages`. O esquema é criado pelas
migrações do Flyway. O agente mexeu em pouca coisa. Acrescentou a coluna `status` numa migração nova
(`V2__message_status.sql`) e o enum `MessageStatus`. Leu `HOTEL_TEST_MODE` na montagem e colocou as três regras do pedido
como três `if` dentro de `EmailSender.send` (`src/main/java/hotel/messages/EmailSender.java:27-38`), com um `boolean
testMode`. O comportamento está todo certo: 65 de 65 conferências passaram. O mais sério tem dois lados. O primeiro é o
design: o texto avisa que vêm outras regras (madrugada, limite por dia), cada uma valendo sempre, só no teste ou só fora
dele, e que cada instalação vai querer uma combinação. Mesmo assim, a variação ficou num método só, com a combinação
escrita no código, e a segunda pousada, que não quer a cópia, não tem como desligá-la. O segundo é que nenhum teste novo
foi escrito: o agente só trocou `1` por `2` em três asserções que já existiam.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem os
mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. A única diferença é que `hotel.messages` passou de 3 para 4
classes e de 75 para 110 linhas (`measures/dependencies.md` comparado com `measures/dependencies-base.md`).
**Nota:** bom

A mudança ficou toda em `hotel.messages` e na montagem, e nenhuma dependência nova foi criada. As rotas continuam finas:
`GET /messages` só serializa `sentMessages.all()` (`src/main/java/hotel/web/HttpApi.java:81`). O SQL novo ficou na
classe de dados (`src/main/java/hotel/messages/SentMessages.java:20-24` e `:42`). O modo de teste é decidido na classe
que monta as peças (`src/main/java/hotel/Hotel.java:26`). O ciclo `documents` ↔ `reservations` vem da base, e o agente
não o tocou. Um detalhe: a variável de ambiente é lida dentro do construtor de `Hotel` (`Hotel.java:26`), e não no `main`,
onde as outras (`PORT` e as do banco) são lidas (`Hotel.java:43-44`). É uma inconsistência pequena, que fica para o
design.

## Design de baixo nível

**Números:** 3 regras pedidas (bloqueio, cópia, desvio para a caixa de testes) e nenhuma delas numa estrutura própria.
As três são `if` dentro de `EmailSender.send` (`EmailSender.java:28`, `:31`, `:35-36`). A combinação por modo (sem
cópia no teste) está escrita no próprio fluxo do método, e não na montagem.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern pedido (Decorator).** A referência diz que é omissão "quando os três `if` (o endereço, o modo de teste, a
cópia) ficam dentro do envio", e é exatamente o que aconteceu. Não há uma interface de envio nem embrulhos. Em
`EmailSender.java:27-38`:

- `if (!VALID_ADDRESS...)` grava como BLOCKED;
- `if (testMode)` desvia para a caixa de testes;
- senão, grava a original e a cópia.

A regra "sem cópia no modo de teste" não está escrita em nenhum lugar: ela sai do `return` antecipado da linha 32-33, e
isso amarra a ordem das regras ao método. Para pôr a regra da madrugada "só fora do teste" ou o limite diário "sempre",
alguém vai ter de mexer de novo nesse método e criar mais `boolean`. A segunda pousada sem a cópia não tem como ser
montada: o construtor só aceita `testMode` (`EmailSender.java:22`). Nada aproxima do pattern: não há peça por regra, e
a montagem (`Hotel.java:26`) só passa uma flag.

**Exagero.** Nenhum. O agente não criou interface, fábrica nem camada que só repassa. A única peça nova é o enum
`MessageStatus` (`src/main/java/hotel/messages/MessageStatus.java`), e ela é necessária.

**O resto do código.**

- O que está bem:
  - o `status` é um `enum`, e não texto solto (`MessageStatus.java:3-7`);
  - ele é gravado e lido pelo nome (`SentMessages.java:28`, `:51`);
  - o registro `Message` ganhou o campo sem quebrar quem o usa (`Message.java:3-4`).
- O que merece correção:
  - um argumento `boolean` que muda o comportamento inteiro do método (`EmailSender.java:22`, `:31`);
  - o construtor antigo de um argumento ficou só para os testes (`EmailSender.java:18-20`), e com ele o modo de teste
    nunca é exercitado;
  - a leitura de `System.getenv` dentro do construtor de `Hotel` (`Hotel.java:26`) faz o modo depender do ambiente de
    quem constrói `Hotel`, inclusive nos testes, em vez de entrar pelo `main` como as outras configurações;
  - os endereços `ARCHIVE` e `TEST_INBOX` são constantes da classe (`EmailSender.java:11-12`), e não configuração.

Nada disso é grave, mas um revisor pediria a mudança. O resto do código vem da base sem alteração.

## Testes

**Números:**

- `final`: 26 testes rodaram, 0 falhas, 0 erros, 0 pulados, 26 métodos de teste escritos;
- `base`: os mesmos 26 testes, então nenhum teste novo;
- linhas cobertas: 93,6% (485 de 518) na `final`, contra 93,8% na base;
- ramos cobertos: 85,8% (103 de 120) na `final`, contra 87,1% na base;
- `hotel.messages`: 4 de 6 ramos cobertos (`measures/tests.md`, `measures/tests-base.md`).

**Nota:** ruim

O agente não testou o que mudou. A diferença nos testes é só trocar o tamanho esperado da lista de 1 para 2 em
`src/test/java/hotel/reservations/ReservationServiceTest.java:91` e `:105` e em
`src/test/java/hotel/web/HttpApiTest.java:81`. Nenhum teste confere:

- o bloqueio de endereço inválido (os dois ramos descobertos de `hotel.messages` são o `BLOCKED` e o modo de teste);
- o campo `status`;
- o assunto "[Cópia] " ou o texto da cópia;
- o desvio para `testes@...` com "[TESTE] " e a linha "Destinatário original: ".

A pergunta da mutação:

- trocar a expressão regular (`EmailSender.java:13`) por uma que aceite `ana@gmail` não faz nenhum teste falhar;
- apagar o `"[Cópia] "` ou o `"[TESTE] "` também não;
- apagar o bloco do modo de teste inteiro também não;
- só apagar a linha da cópia (`EmailSender.java:36`) faria falhar as contagens.

O agente também não enfraqueceu os testes que já existiam, e eles continuam passando. Mas a parte nova do cenário está,
na prática, sem rede, e por isso a nota é `ruim` (ver "Dúvidas").

## Banco

**Números:** 3 tabelas e 2 migrações do Flyway. A `V1__create_tables.sql` está igual à da base; a
`V2__message_status.sql` é nova. Nada foi acusado:

- toda tabela tem chave primária;
- o dinheiro está em `numeric(10,2)`;
- não há chave estrangeira sem índice nem data guardada como texto;
- `messages.status` tem CHECK;
- não há SQL montado com dado de fora.

Os números são de `measures/database.md`.
**Nota:** bom

O agente fez a mudança do jeito certo para um sistema que já roda: uma migração nova, sem editar a antiga
(`src/main/resources/db/migration/V2__message_status.sql:2-3`). A coluna é `NOT NULL DEFAULT 'SENT'`, então as
mensagens antigas, que saíram de fato, viram `SENT`, e há um `CHECK` com os dois valores. O SQL novo usa parâmetros
(`SentMessages.java:20-28`). Um ponto pequeno: a original e a cópia são dois `INSERT` em conexões separadas, sem
transação (`EmailSender.java:35-36`). Se o segundo falhar, a original fica gravada sem a cópia e a confirmação devolve
erro. Mas a base já gravava a reserva e a mensagem do mesmo jeito, separadas
(`src/main/java/hotel/reservations/ReservationService.java:63-68`). A proteção contra reserva duplicada (o `EXCLUDE
USING gist`) vem da base e continua lá.

## Comportamento

**Números:** 65 de 65 conferências passaram e nenhuma falhou (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 07 no modo normal | 9 de 9 |
| o cenário 07 no modo de teste | 5 de 5 |
| duas reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

Tudo o que o dono pediu funciona como ele descreveu:

- "ana@gmail", "ana.gmail.com", "ana@@gmail.com" e "@gmail.com" ficam BLOCKED, com o endereço original e sem cópia;
- a mensagem válida sai SENT e é seguida da cópia para o arquivo, com "[Cópia] " e o mesmo texto;
- com `HOTEL_TEST_MODE=true`, só uma mensagem sai, para `testes@pousadamarealta.com.br`, com "[TESTE] " e a primeira
  linha "Destinatário original: ";
- no modo de teste, o endereço inválido continua bloqueado com o endereço original.

O que já funcionava continua funcionando, e os dados resistem a reiniciar o serviço. Para o dono da pousada, hoje está
tudo certo. O custo aparece no próximo pedido (as regras da madrugada e do limite, e a segunda pousada sem cópia), e
isso pesa no design e nos testes, não aqui.

## Dúvidas

- Nos testes, fiquei entre `ruim` e `com problemas`. Os 26 testes da base passam e conferem as regras antigas com
  valores, mas o agente não escreveu nenhum teste para a parte nova: só ajustou três contagens. Como nenhuma das regras
  novas (o bloqueio, o status, os assuntos, o modo de teste) é conferida, tratei a coluna como quase ausente para a
  mudança.
- No design, fiquei entre `com problemas` e `bom`. Fora do pattern, os defeitos do código do agente são pequenos e
  localizados: o argumento `boolean`, o `getenv` no construtor, o construtor que sobrou para os testes.
