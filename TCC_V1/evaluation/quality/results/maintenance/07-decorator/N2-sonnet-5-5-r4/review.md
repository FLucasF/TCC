# Revisão: manutenção, cenário 07

## Visão geral

A base tem sete pacotes: `hotel` (montagem e banco), `web` (as rotas do Javalin), `reservations` (as regras e o
acesso ao banco das reservas), `rooms`, `documents`, `reports` e `messages`. Uma requisição entra em
`web/HttpApi.java`, chama `ReservationService`, que usa `ReservationStore`/`Rooms` (JDBC num PostgreSQL com migrações
do Flyway) e, ao confirmar, manda o e-mail (`ReservationService.java:65`). O agente mexeu só no envio. Criou a
interface `MessageSender` com o envio real (`EmailSender`) e três embrulhos, um por regra (`AddressValidation`,
`ArchiveCopy`, `TestRedirect`). Quem monta a pilha conforme o ambiente é `MessageSenders.assemble`, chamado em
`Hotel.java:26`. O `status` da mensagem entrou como um enum, numa migração nova (`V2__message_status.sql`). Não há
defeito sério: os 31 testes passam e as 65 conferências do comportamento passam. Os pontos fracos são pequenos. O
embrulho de validação grava direto no `SentMessages`. A coluna `status` não tem CHECK. A original e a cópia são
gravadas sem transação.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base (`measures/dependencies.md` e
`measures/dependencies-base.md` têm o grafo idêntico). Há 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já
vinha da base. `hotel.messages` passou de 3 para 9 classes (de 75 para 194 linhas) e continua sem depender de nenhum
pacote do projeto.
**Nota:** bom

A mudança ficou toda dentro de `hotel.messages`, e a montagem ficou no lugar que já montava as peças.
`Hotel.java:21` recebe o ambiente e `Hotel.java:26` pede o envio montado a `MessageSenders.assemble`. O `main` passa
`System.getenv()` (`Hotel.java:45`), em vez de cada classe ler a variável por dentro, e por isso os testes podem
passar um `Map` (`MessageSendersTest.java:22`). `ReservationService` agora depende só da interface
(`ReservationService.java:17,20`), sem saber das regras. A borda HTTP não mudou: `/messages` continua serializando
`sentMessages.all()` (`HttpApi.java:81`). O SQL novo ficou no `SentMessages` (`SentMessages.java:20-31`). O ciclo
`documents`/`reservations` e o JDBC dentro de `reservations` vêm da base, e o agente não os piorou.

## Design de baixo nível

**Números:** 3 regras pedidas, as 3 em embrulhos próprios e 0 fora da estrutura. Os embrulhos e o envio real
implementam a mesma interface (`MessageSender.java:4-7`). A combinação por modo está toda na montagem
(`MessageSenders.java:15-26`), e nenhum embrulho pergunta pelo modo.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** É o Decorator como a referência descreve, e cada embrulho tem a mesma interface e chama o próximo:

- `TestRedirect.java:16` troca o destinatário, o assunto e o texto e passa adiante.
- `AddressValidation.java:16-20` bloqueia ou passa adiante.
- `ArchiveCopy.java:16-19` envia e, se saiu `SENT`, manda a cópia pelo mesmo `next`.

A montagem empilha os embrulhos na ordem certa. O redirecionamento de teste fica por dentro da validação
(`MessageSenders.java:19-22`), e por isso o bloqueio vê e guarda o endereço original. A cópia só entra fora do modo
de teste e quando a instalação não a desliga (`MessageSenders.java:23`). `HOTEL_ARCHIVE_COPY=false`
(`MessageSenders.java:16`) responde ao "a segunda pousada não quer a cópia". `ArchiveCopy` decide pela situação da
mensagem e não pelo modo, então a regra "bloqueada não gera cópia" fica dentro dela e a regra "sem cópia no teste"
fica na montagem. Para a madrugada e o limite por dia, basta um embrulho novo e uma linha na montagem. O que afasta
um pouco do ideal: o modo de cada regra é um `if` na montagem, escrito à mão, e não uma configuração por regra. Para
o tamanho atual isso é o que se espera.

**O exagero.** Não há. `MessageSenders` é a própria montagem que o pattern pede. A interface tem quatro
implementações. Não há fábrica, builder nem camada que só repassa.

**O resto do código.** Está limpo, com pequenas ressalvas que não chegam a pedir mudança antes de aceitar:

- `AddressValidation` grava a mensagem bloqueada direto em `SentMessages` (`AddressValidation.java:17`) e repete o
  canal `"EMAIL"`, que também está em `EmailSender.java:15`. São dois lugares que sabem gravar um e-mail.
- A validação tem uma condição redundante: `lastIndexOf('.') < length - 1` e `!endsWith(".")` dizem a mesma coisa
  (`AddressValidation.java:33`).
- Ela aceita espaços e dois pontos seguidos (`ana@gmail..com`). A definição do dono não proíbe isso.

O `status` é um enum (`MessageStatus.java`), e não um texto solto. O `Message` virou um record com o campo novo
(`Message.java:3`).

## Testes

**Números:** 31 testes rodaram, com 0 falhas e 0 pulados. A base tinha 26, e há 5 métodos novos em
`MessageSendersTest`. A cobertura de linhas foi de 93,8% para 94,1% (511 de 543) e a de ramos de 87,1% para 87,9%
(123 de 140). Em `hotel.messages`: 68 de 73 linhas e 24 de 26 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** bom

Os testes novos rodam contra o banco de verdade, pela montagem que o sistema usa, e conferem as regras com valores:

- A cópia: o destinatário, o assunto `"[Cópia] Oi"`, o mesmo texto, `SENT` e a posição logo depois da original
  (`MessageSendersTest.java:26-37`).
- Sete endereços inválidos, cada um `BLOCKED`, com o endereço original e sem cópia, porque o total precisa ser 7
  (`MessageSendersTest.java:40-48`).
- O modo de teste: o endereço de testes, `"[TESTE] Oi"`, o texto `"Destinatário original: ana@gmail.com\ntexto"` e
  nenhuma cópia (`MessageSendersTest.java:51-61`).
- No modo de teste, o endereço inválido continua bloqueado e com o endereço original
  (`MessageSendersTest.java:64-73`).
- A instalação que desliga a cópia (`MessageSendersTest.java:76-79`).

Pela pergunta da mutação, os testes pegariam três trocas: deixar a cópia valer no modo de teste, trocar `at < 1` por
`at < 0`, ou apagar o `dot > 0`. O teste da API mudou de 1 para 2 mensagens. Isso é consequência da regra pedida e
não enfraquece o teste, porque ele passou a conferir o `status` e o endereço da cópia (`HttpApiTest.java:82-85`).
Nenhum teste da base foi apagado. Ficam sem rede três pontos menores: o `status` `BLOCKED` pela API (o `GET /messages`
só é testado com `SENT`), um endereço válido de forma pouco comum (`a@b.c`) e um valor de `HOTEL_TEST_MODE` diferente
de `true`.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` e nenhuma data está em texto.
Nenhuma chave estrangeira está sem índice. A migração `V1` está igual à da base e a `V2` é nova. As conferências
acusaram só colunas de situação sem CHECK, entre elas a nova `messages.status` (`measures/database.md`).
**Nota:** bom

O agente evoluiu o esquema do jeito certo: uma migração nova, sem editar a `V1`.
`ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT'` (`V2__message_status.sql:2`) dá `SENT` às
mensagens que já existiam num banco em uso, e isso está correto, porque elas foram enviadas. O SQL novo usa
parâmetros (`SentMessages.java:23-28`) e devolve a conexão com try-with-resources.

Há dois defeitos pequenos:

- A coluna `status` não tem `CHECK (status IN ('SENT','BLOCKED'))`. A base também não tinha CHECK nos outros status.
  Se um valor estranho entrar, `MessageStatus.valueOf` quebra o `GET /messages` (`SentMessages.java:51`).
- A original e a cópia são dois `INSERT` independentes (`ArchiveCopy.java:16,18`). Uma falha no meio deixa a original
  sem a cópia.

A leitura da tabela inteira em `/messages` vem da base.

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| depois de reiniciar | 3 de 3 |
| as regras de envio no modo normal | 9 de 9 |
| as regras de envio no modo de teste | 5 de 5 |
| duas reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

O que funcionava antes continua funcionando, e as regras novas se comportam como o dono pediu:

- No modo normal, o e-mail sai `SENT`, com a cópia logo depois para o arquivo, com `[Cópia] ` e o mesmo texto.
- Os quatro endereços errados do roteiro (`ana@gmail`, `ana.gmail.com`, `ana@@gmail.com`, `@gmail.com`) aparecem como
  `BLOCKED`, com o endereço digitado e sem cópia.
- Com `HOTEL_TEST_MODE=true` sai uma mensagem só, para a caixa de testes, com `[TESTE] ` e a linha do destinatário
  original, e o endereço inválido continua bloqueado.

Para o dono, isso quer dizer que a recepção já vê na lista quem precisa de um telefonema, o contador recebe as cópias
e o pessoal do site não manda mais e-mail para hóspede de verdade.

## Dúvidas

A ordem dos embrulhos põe a cópia por fora da validação, e por isso a própria cópia passa pela validação de endereço.
Hoje isso não tem efeito, mas um embrulho futuro, como o limite por hóspede por dia, contaria as cópias se fosse
posto por dentro dela. Isso depende de onde o próximo agente o puser. Não há como decidir agora se é defeito.
