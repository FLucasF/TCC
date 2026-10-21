# Revisão: manutenção, cenário 07

## Visão geral

O agente partiu de uma base organizada por pacotes de domínio: `hotel.web` (Javalin, `HttpApi`), `hotel.reservations`
(o serviço, as regras e o `ReservationStore` com JDBC), `hotel.rooms`, `hotel.reports`, `hotel.documents` e
`hotel.messages`. A montagem fica em `hotel.Hotel`, e os dados ficam no PostgreSQL, com migrações do Flyway. Uma
confirmação entra por `HttpApi`, passa por `ReservationService.confirm` e chega a `send` de um `MessageSender`
(`reservations/ReservationService.java:65`). A mudança é pequena e bem localizada. Agora há uma interface
`MessageSender` (`messages/MessageSender.java:4`), o envio real (`EmailSender`) e três embrulhos, um para cada regra:
`BlockInvalidAddress`, `ArchiveCopy` e `RedirectToTestInbox`. Os embrulhos são empilhados conforme o modo num lugar
só, `Hotel.messageSender` (`Hotel.java:39-44`). O status foi para o `Message` e para a tabela por uma migração nova
(`V2__message_status.sql`), sem editar a V1. O roteiro de comportamento passou inteiro (65 de 65) e os 29 testes
passam. Não há defeito sério. O que falta é pouco: os testes não conferem o bloqueio no modo de teste nem a montagem
pelo `HOTEL_TEST_MODE`.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, um ciclo (`hotel.documents` ↔ `hotel.reservations`), tudo igual à
base. Só o tamanho mudou: `hotel.messages` foi de 3 classes e 75 linhas para 8 classes e 158 linhas, e `hotel` foi de
85 para 100 linhas (`measures/dependencies.md` comparado com `measures/dependencies-base.md`).
**Nota:** bom

- **O ciclo vem da base.** O agente não acrescentou nenhuma dependência entre pacotes.
- **As regras novas ficaram todas em `hotel.messages`.** Esse pacote não depende de nenhum outro pacote do projeto e
  só usa JDBC dentro de `SentMessages`.
- **O serviço depende da interface, não da classe concreta.** `ReservationService` agora recebe um `MessageSender`
  (`reservations/ReservationService.java:4,17,20`).
- **A montagem continua num lugar só.** É na classe `Hotel` que o modo é lido e a pilha de embrulhos é montada
  (`Hotel.java:29,39-44`).
- **A borda HTTP continua fina.** `GET /messages` só serializa a lista (`web/HttpApi.java:81`), e o campo `status` sai
  do próprio record `Message` (`messages/Message.java:3-4`).
- **Nenhuma classe cresceu demais.** A maior continua sendo `HttpApi`, com 170 linhas, que veio da base.

## Design de baixo nível

**Números:** as 3 regras pedidas estão na estrutura, cada uma num embrulho com a mesma interface do envio, e nenhuma
ficou fora dela. Há 0 `if` de modo dentro dos embrulhos ou do envio real. A combinação por modo é decidida numa única
linha da montagem (`Hotel.java:42`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern (Decorator).** É o desenho que a referência descreve:

- **Uma interface de envio.** `MessageSender.send(to, subject, body)` (`messages/MessageSender.java:4-6`).
- **O envio real.** `EmailSender` só grava a mensagem como `SENT` (`messages/EmailSender.java:13-14`).
- **Um embrulho por regra, cada um chamando o próximo:**
  - o bloqueio só passa adiante quando o endereço é válido, e grava `BLOCKED` com o endereço original
    (`messages/BlockInvalidAddress.java:20-24`);
  - a cópia manda a original e depois a cópia, pelo mesmo `next` (`messages/ArchiveCopy.java:15-18`);
  - o desvio troca o destinatário, o assunto e a primeira linha do texto (`messages/RedirectToTestInbox.java:15-16`).
- **Nenhum embrulho sabe do modo de teste.** É a ordem da pilha que garante as regras: o bloqueio fica por fora, e por
  isso a mensagem bloqueada não gera cópia e, no modo de teste, fica com o endereço original. A escolha entre a cópia
  e o desvio fica na montagem (`Hotel.java:41-43`).
- **As próximas regras cabem do mesmo jeito.** Madrugada e limite por dia seriam mais um embrulho cada um, empilhado
  em `Hotel.messageSender`.
- **A segunda pousada, sem a cópia,** pede mudar só a montagem.

A expressão regular segue a definição do dono: um único `@`, algo antes dele e um domínio com ponto no meio
(`messages/BlockInvalidAddress.java:9`).

**O exagero.** Não há:

- a interface `MessageSender` tem quatro implementações de verdade;
- o `enum MessageStatus` (`messages/MessageStatus.java:3-7`) é o tipo certo para `SENT`/`BLOCKED`;
- não há fábrica, builder nem camada que só repasse.

**O resto do código.** Está limpo, com tipos certos e nada de estado mutável. Há três detalhes pequenos, que não
pedem mudança antes de aceitar:

- **O bloqueio grava direto em `SentMessages`, sem passar pela pilha.** Com isso, o literal `"EMAIL"` aparece duas
  vezes (`messages/BlockInvalidAddress.java:22` e `messages/EmailSender.java:14`).
- **`HOTEL_TEST_MODE` é lido dentro do construtor de `Hotel` (`Hotel.java:41`).** O `PORT` e o banco são lidos no
  `main` (`Hotel.java:57-58`), e ler o modo no construtor deixa a classe presa ao ambiente do processo.
- **O campo do serviço ainda se chama `emailSender`** (`reservations/ReservationService.java:17`), mas hoje guarda a
  pilha inteira.

## Testes

**Números:** 29 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 29 métodos escritos. A base tinha 26, e os 3
novos estão em `SendingRulesTest`. A cobertura de linhas é de 94,2% (499 de 530; na base, 93,8%) e a de ramos, de
86,7% (104 de 120; na base, 87,1%). Em `hotel.messages`, 52 de 56 linhas e 4 de 4 ramos estão cobertos; em `hotel`,
só 3 de 6 ramos (`measures/tests.md` e `measures/tests-base.md`).
**Nota:** com problemas

**O que está conferido, com valores.** Os testes novos rodam contra o banco de verdade e conferem as regras, não só a
execução:

- **A cópia fora do teste.** A cópia vem depois da original, para `arquivo@…`, com `[Cópia] ` no assunto e status
  `SENT` (`messages/SendingRulesTest.java:28-32`).
- **O bloqueio.** Quatro endereços inválidos ficam `BLOCKED`, com o endereço original e sem cópia
  (`messages/SendingRulesTest.java:36-43`).
- **O desvio no modo de teste.** Vai para `testes@…` com `[TESTE] `, e o texto ganha a linha "Destinatário original"
  (`messages/SendingRulesTest.java:47-51`).

**O que mudou nos testes da base.** Nenhum foi apagado. Só um mudou: `HttpApiTest` passou a esperar 2 mensagens em
vez de 1 depois da confirmação (`web/HttpApiTest.java:81`), o que é a mudança pedida e não um enfraquecimento.

**O que ficou sem rede:**

- **O bloqueio no modo de teste com o endereço original não tem teste,** e o dono pede essa regra com todas as letras.
  Se alguém invertesse a ordem em `Hotel.java:43`, pondo o desvio por fora do bloqueio, o endereço inválido passaria a
  sair para `testes@…` como `SENT`, e nenhum teste falharia. Os testes montam a pilha à mão
  (`messages/SendingRulesTest.java:29,37,48`), e nenhum passa pela montagem em modo de teste. Por isso só 3 de 6 ramos
  de `hotel` estão cobertos.
- **O texto da cópia não é conferido.** Se `ArchiveCopy` mandasse um corpo vazio, os testes continuariam passando,
  porque o resumo só olha destinatário, assunto e status (`messages/SendingRulesTest.java:22-25`).
- **O campo `status` no JSON de `GET /messages` não é conferido.** O teste da API também não confere que a segunda
  mensagem é a cópia (`web/HttpApiTest.java:81-82`).
- **`HttpApiTest` depende do ambiente.** Ele sobe `new Hotel(...)` (`web/HttpApiTest.java:35`), que lê
  `HOTEL_TEST_MODE` do processo. Com a variável ligada na máquina, o teste falharia.

## Banco

**Números:** 3 tabelas e 2 migrações do Flyway. A V1 está igual à da base, e a `V2__message_status.sql` é nova. Não há
tabela sem chave primária, dinheiro fora de `numeric(10,2)`, chave estrangeira sem índice nem data em texto. A coluna
nova `messages.status` tem `CHECK` (`measures/database.md`).
**Nota:** bom

- **A mudança foi feita como deve.** Uma migração nova acrescenta a coluna `status text NOT NULL DEFAULT 'SENT'` com
  `CHECK (status IN ('SENT','BLOCKED'))` (`src/main/resources/db/migration/V2__message_status.sql:2-3`). As mensagens
  antigas ficam `SENT`, e a migração da base não foi tocada.
- **O SQL novo usa parâmetros.** O status entra como `?` (`messages/SentMessages.java:23-29`), e a leitura converte o
  valor para o `enum` (`messages/SentMessages.java:42,52`). Os candidatos de SQL montado com `+` que o script listou
  ficam em `ReservationStore` e só juntam constantes; vêm da base.
- **A reserva duplicada é barrada pelo banco.** O `EXCLUDE USING gist` contra noites sobrepostas veio da base.
- **Dois detalhes pequenos, que não derrubam a nota:**
  - a original e a cópia são dois `INSERT` em conexões separadas, sem transação (`messages/ArchiveCopy.java:16-17`),
    então uma falha entre os dois deixaria a original sem cópia;
  - `messages.channel` continua sem `CHECK`, como na base.

## Comportamento

**Números:** 65 de 65 conferências passaram e nenhuma falhou (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário, no modo normal | 9 de 9 |
| o cenário, no modo de teste | 5 de 5 |
| as duas reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

Para o dono da pousada, as três regras funcionam como ele pediu:

- **O endereço inválido é bloqueado.** "ana@gmail", "ana.gmail.com", "ana@@gmail.com" e "@gmail.com" aparecem uma vez
  só, como `BLOCKED`, com o endereço original e sem cópia.
- **Toda mensagem enviada gera a cópia para o arquivo.** A cópia vem logo depois da original, com `[Cópia] ` e o
  mesmo texto.
- **No modo de teste (`HOTEL_TEST_MODE=true`), nada sai para o hóspede.** Tudo vai para `testes@…` com `[TESTE] ` e a
  linha "Destinatário original", sem cópia. O endereço inválido continua bloqueado com o endereço original.

Cada mensagem tem o `status`. O resto do sistema continua igual ao de antes, as mensagens sobrevivem a um reinício e
duas reservas simultâneas do mesmo quarto nunca passam juntas.

## Dúvidas

- **A nota dos testes ficou entre `bom` e `com problemas`.** As regras novas estão conferidas com valores, mas o
  bloqueio no modo de teste e a montagem pelo `HOTEL_TEST_MODE` não têm teste, e o texto pede essa regra
  explicitamente. Dei `com problemas` por isso.
- **O texto diz que "cada instalação pode querer outra combinação",** e hoje mudar a combinação pede mudar
  `Hotel.java:42`. Li isso como decisão de montagem, que a referência aceita, e não como um pedido de configuração por
  arquivo.
