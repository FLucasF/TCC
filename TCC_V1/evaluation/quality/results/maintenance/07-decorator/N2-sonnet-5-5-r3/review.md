# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas em `HttpApi`), `hotel.reservations` (as regras em
`ReservationService` e o SQL em `ReservationStore`), `hotel.rooms`, `hotel.reports`, `hotel.documents`,
`hotel.messages` (o envio e a lista de mensagens em `SentMessages`) e `hotel` (a montagem em `Hotel` e o pool em
`Database`). Uma requisição entra pelo `HttpApi`, chama o `ReservationService`, que grava pelo `ReservationStore` no
PostgreSQL (o esquema vem das migrações do Flyway) e, na confirmação, manda o e-mail pelo envio. O agente fez as três
regras de e-mail como embrulhos de uma interface nova, `MessageSender`, (`ValidAddressOnly`, `RedirectToTests`,
`ArchiveCopy`, em volta do `EmailSender`). A combinação por modo fica em `MessageSenders.create`, chamado pela
montagem em `Hotel`. O `status` virou uma coluna nova, acrescentada por uma migração nova (`V2__message_status.sql`).
Não achei nada sério: os 65 pontos do roteiro passam, os 31 testes passam e não apareceu nenhum ciclo novo. Os reparos
são pequenos: o canal `"EMAIL"` está repetido, o `true` que liga a cópia está solto na montagem e a coluna `status` não
tem `CHECK`.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos números da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base.
`hotel.messages` passou de 3 para 9 classes (de 75 para 191 linhas) e continua sem depender de nenhum outro pacote do
projeto.
**Nota:** bom

- A mudança ficou dentro de `hotel.messages`, mais duas linhas na montagem. `ReservationService` agora depende da
  interface, e não da classe concreta (`src/main/java/hotel/reservations/ReservationService.java:4,17,20`), e não sabe
  nada de modo de teste nem de cópia.
- O modo de teste é lido do ambiente só no `main` (`src/main/java/hotel/Hotel.java:48`) e passado pelo construtor
  (`Hotel.java:24,29`). Nenhuma classe consulta `System.getenv` por dentro.
- O SQL novo ficou na classe de dados que já existia (`src/main/java/hotel/messages/SentMessages.java:20-56`). A rota
  `/messages` continua fina (`src/main/java/hotel/web/HttpApi.java:81`), e o campo `status` sai sozinho pelo record
  `Message`.
- O ciclo `documents`↔`reservations` veio da base, e o agente não o piorou.

## Design de baixo nível

**Números:** as 3 regras estão na estrutura, uma classe por regra, todas implementando `MessageSender`, e nenhuma
ficou fora dela. Nenhum envio pergunta pelo modo de teste. A combinação por modo fica num único lugar
(`MessageSenders.java:17-24`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

- **O pattern:** é um Decorator de livro. A interface é `src/main/java/hotel/messages/MessageSender.java:4-7`, e o
  envio real é o `EmailSender` (`EmailSender.java:4,13-15`). Cada embrulho guarda o `next` e o chama:
  - `ValidAddressOnly.java:15-20` bloqueia o endereço inválido;
  - `RedirectToTests.java:15-17` reescreve o destinatário, o assunto e o texto;
  - `ArchiveCopy.java:15-21` manda a cópia só se o original saiu `SENT`.

  A combinação é decidida na montagem, em `MessageSenders.java:17-24`. No modo de teste, o bloqueio fica por fora do
  desvio, e por isso a mensagem bloqueada guarda o endereço original. Fora dele, a cópia embrulha o bloqueio, e a
  instalação sem cópia escolhe isso pelo parâmetro `archive`. Nenhum embrulho conhece o modo de teste: a regra "sem
  cópia no teste" mora na montagem, não dentro de `ArchiveCopy`. Uma regra nova (madrugada, limite por dia) é uma
  classe nova e uma linha no ramo do modo em que ela vale.
- **O que afasta um pouco:**
  - `ValidAddressOnly` grava a mensagem bloqueada direto no `SentMessages` (`ValidAddressOnly.java:17`), sem passar
    pelo envio de dentro, e repete o canal `"EMAIL"`, que também está em `EmailSender.java:14`. Faz sentido que a
    bloqueada não atravesse o desvio, mas a regra de como guardar uma mensagem ficou em dois lugares.
  - Em `Hotel.java:29`, o `true` que liga a cópia para o arquivo é um literal sem nome. Para a segunda pousada, que
    não quer a cópia, é preciso mudar o código, porque não há uma configuração. O texto não pedia uma variável para
    isso, então não pesa como defeito do pattern.
- **O exagero:** não há. `MessageSender` tem quatro implementações de verdade, e `MessageSenders` é a montagem que o
  pattern pede. Não há builder, nem uma camada que só repassa.
- **O resto:** o status é um `enum` (`MessageStatus.java:3-6`), e não texto solto, e o record `Message` ganhou o campo
  (`Message.java:3`). A validação do endereço é curta e fiel ao texto (`ValidAddressOnly.java:23-34`), com cuidado
  com `null`. O resto do código veio da base e não foi tocado. Os dois reparos acima são pequenos e localizados.

## Testes

**Números:** 31 testes rodaram, 0 falharam e 0 foram pulados. Há 31 métodos escritos: os 26 da base e 5 novos, em
`MessageSendersTest`. A cobertura é de 93,9% das linhas (511 de 544) e de 88,1% dos ramos (118 de 134); na base, era de
93,8% e 87,1%. Em `hotel.messages`, ficou em 66 de 71 linhas e 19 de 20 ramos (`measures/tests.md`).
**Nota:** bom

- Os testes novos rodam contra o banco de verdade e conferem as regras com valores:
  - a cópia, com o endereço, o assunto `"[Cópia] Oi"` e o mesmo texto, logo depois da original
    (`src/test/java/hotel/messages/MessageSendersTest.java:22-31`);
  - a instalação sem cópia (`:34-37`);
  - oito endereços inválidos bloqueados, sem cópia e com o endereço original (`:41-51`);
  - o modo de teste, com o destinatário, o assunto e o texto exatos, sem cópia (`:54-62`);
  - o bloqueio no modo de teste, com o endereço e o assunto originais (`:65-71`).
- **A pergunta da mutação:**
  - Trocar o prefixo `"[Cópia] "` faz falhar `:30`.
  - Tirar o `if` do `SENT` em `ArchiveCopy` (a cópia de uma bloqueada) faz falhar a contagem de 8 em `:49`.
  - Trocar `at < 1` por `at < 0` faz falhar o caso `"@gmail.com"`.
  - Trocar a ordem dos embrulhos no modo de teste faz falhar `:69`.

  O que fica sem rede: a leitura de `HOTEL_TEST_MODE` no `main` (`Hotel.java:48`, sem teste; `hotel` tem 2 de 4
  ramos) e o modo de teste pela API.
- O único teste da base que mudou foi `src/test/java/hotel/web/HttpApiTest.java:81-84`. Ele passou de 1 para 2
  mensagens, que é a mudança pedida (a cópia), e ganhou asserções do `status` e do endereço do arquivo. Nenhum foi
  enfraquecido.

## Banco

**Números:** são 3 tabelas e 2 migrações aplicadas: a `V1` é igual à da base, e a `V2__message_status.sql` é nova.
Nenhuma tabela está sem chave primária, o dinheiro está todo em `numeric(10,2)`, não há chave estrangeira sem índice
nem datas em texto. A conferência acusou colunas de situação sem `CHECK`; entre elas está a nova `messages.status`, e
as outras já eram assim na base (`measures/database.md`).
**Nota:** bom

- A mudança no esquema é uma migração nova, e a da base ficou intacta
  (`src/main/resources/db/migration/V2__message_status.sql:2`). O `NOT NULL DEFAULT 'SENT'` dá às mensagens que já
  existiam o valor certo, porque elas foram enviadas.
- O SQL novo usa parâmetros e try-with-resources (`src/main/java/hotel/messages/SentMessages.java:21-28,40-51`).
- O que poderia melhorar:
  - `status` é `text` sem `CHECK (status IN ('SENT','BLOCKED'))`. Segue a convenção da base (`reservations.status`
    também não tem), mas o banco aceitaria um valor que `MessageStatus.valueOf` (`SentMessages.java:51`) não sabe
    ler.
  - A original e a cópia são dois `INSERT` sem transação (`ArchiveCopy.java:16-18`). Se o segundo falhar, a
    original fica sem cópia. O risco é pequeno.
  - `all()` traz a tabela inteira, mas isso já era assim na base.

## Comportamento

**Números:** passaram 65 de 65 conferências e nenhuma falhou (`measures/behavior.md`):
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário no modo normal: 9 de 9;
- o cenário no modo de teste: 5 de 5;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- o endereço inválido (`ana@gmail`, `ana.gmail.com`, `ana@@gmail.com`, `@gmail.com`) aparece como `BLOCKED`, com o
  endereço original e sem cópia;
- a mensagem válida sai `SENT`, seguida da cópia `[Cópia] ...` para o arquivo, com o mesmo texto;
- com `HOTEL_TEST_MODE=true`, só uma mensagem vai para `testes@...`, com `[TESTE] ` no assunto e
  `Destinatário original: ...` na primeira linha, e o inválido continua bloqueado com o endereço original.

O que já funcionava continua funcionando, os dados resistem ao reinício e o quarto não é reservado duas vezes.

## Dúvidas

O texto diz que "cada instalação pode querer outra combinação", e a solução atende isso com um parâmetro no código
(`Hotel.java:29`), não com uma configuração. Li que isso basta, porque o texto não pede uma variável, mas um leitor
mais exigente poderia querer a escolha fora do código.
