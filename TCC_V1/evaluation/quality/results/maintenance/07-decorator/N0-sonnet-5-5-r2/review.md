# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (as regras e o
acesso às reservas), `hotel.rooms`, `hotel.documents`, `hotel.reports`, `hotel.messages` e `hotel`, que monta as peças
(`Hotel`). Uma confirmação entra por `HttpApi`, passa por `ReservationService.confirm`, que grava a reserva por
`ReservationStore` e chama `EmailSender.send` (`src/main/java/hotel/reservations/ReservationService.java:65`). Os dados
ficam no PostgreSQL, com o esquema criado por migrações do Flyway. A mudança ficou toda em `hotel.messages`, e as regras
de envio viraram peças próprias. Há três tipos de regra (`Block`, `Rewrite`, `Copy`,
`src/main/java/hotel/messages/MessageRules.java:15-27`), cada uma registrada numa `MessagePolicy` com o escopo em que
vale (`Scope.ALWAYS/TEST_ONLY/LIVE_ONLY`). O `EmailSender` aplica a política sem saber em que modo está. O status é um
`enum` e a coluna nova veio numa migração nova (`V2__message_status.sql`). O roteiro passou inteiro (65/65) e os 33
testes passam. Não achei nenhum defeito sério. Os pontos menores são: uma política mutável, a mensagem original e a
cópia gravadas sem transação, e um teste da API que depende da variável de ambiente.

## Arquitetura

**Números:** 7 pacotes, 17 dependências, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), o mesmo da base
(`measures/dependencies-base.md`). O `hotel.messages` passou de 3 para 12 classes (75 → 269 linhas). Nenhuma
dependência nova entre pacotes.
**Nota:** bom

- A mudança não criou dependência nova entre pacotes. O `hotel.messages` continua sem depender de nenhum outro pacote
  do projeto (0 dependências), e o único JDBC dele fica em `SentMessages`
  (`src/main/java/hotel/messages/SentMessages.java:20-31`). As regras (`MessageRules`, `MessagePolicy`, `Scope`) não
  conhecem nem o JDBC nem o Javalin.
- A montagem continua num lugar só. `Hotel` lê o modo pela variável de ambiente e passa a política para o `EmailSender`
  (`src/main/java/hotel/Hotel.java:21-30`). O construtor `Hotel(DataSource, MessagePolicy)` deixa uma instalação montar
  outra combinação sem mexer no envio.
- A borda HTTP não mudou: `/messages` continua sendo `ctx.json(sentMessages.all())`
  (`src/main/java/hotel/web/HttpApi.java:81`). O campo `status` aparece porque entrou no record `Message`.
- O ciclo `documents` ↔ `reservations` veio da base, e o agente não o piorou.

## Design de baixo nível

**Números:** as três regras pedidas (o endereço inválido, o desvio do modo de teste, a cópia para o arquivo) estão na
estrutura, cada uma uma peça própria (`MessageRules.java:30`, `:50`, `:58`). Nenhuma fica fora dela. O envio não
pergunta pelo modo (`EmailSender.java:24-33`), e nenhuma regra confere o modo por dentro.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

**O pattern.** O agente não empilhou embrulhos com a mesma interface do envio, como faria o Decorator. Fez uma lista de
filtros, separados em três tipos, que o envio aplica nesta ordem fixa: bloqueio sobre o e-mail original, reescrita,
cópias (`EmailSender.java:26-31`). A referência aceita essa estrutura como alternativa equivalente: "uma lista de
filtros aplicada pelo envio, desde que cada regra seja uma peça própria e a combinação por modo seja decidida na
montagem". Aqui as duas condições se cumprem:
- cada regra é uma fábrica separada em `MessageRules` (`invalidAddress`, `redirectTo`, `archiveTo`);
- o escopo de cada regra é declarado na hora de montar a política (`MessagePolicy.java:31-36`:
  `block(ALWAYS, …)`, `rewrite(TEST_ONLY, …)`, `copy(LIVE_ONLY, …)`);
- o `MessagePolicy.applies` só compara o escopo com o modo (`MessagePolicy.java:77-78`, `Scope.java`). Nenhuma regra
  sabe do modo de teste: a cópia não pergunta se está em teste.

A instalação sem cópia é só uma política sem a regra `copy` (o teste em `EmailSenderTest.java:81-86` faz isso). Uma regra
nova, como o horário da madrugada ou o limite por dia, é mais um `Block` com o escopo que quiser.

O que afasta a estrutura do acerto:
- a ordem das fases é fixa no `EmailSender`, e uma regra que não caiba em bloquear, reescrever ou copiar exige mexer no
  envio;
- a combinação padrão mora num `static` dentro de `hotel.messages` (`MessagePolicy.standard`), e não na classe de
  montagem. Mas `Hotel` pode passar outra.

**O exagero.** Contenção. As peças novas servem à variação: `Email`, `Scope`, as três interfaces, `MessagePolicy`. As
interfaces `Rewrite` e `Copy` têm uma implementação cada, mas o próprio texto anuncia regras novas, e elas fazem parte
da estrutura do pattern. O `Optional` em `Copy.copyOf` (`MessageRules.java:26`) ainda não tem uso: toda implementação
devolve `Optional.of`. É pequeno.

**O resto do código.** Está limpo. Pontos pequenos que eu apontaria, sem bloquear a aceitação:
- `MessagePolicy` é mutável depois de montada: `block`, `rewrite` e `copy` acrescentam numa `ArrayList` e devolvem
  `this` (`MessagePolicy.java:17-19`, `:38-51`). Quem tiver a referência pode trocar as regras com o sistema rodando.
  Um builder que gerasse listas imutáveis seria mais seguro.
- Em `isValidAddress`, a conferência de espaço no domínio repete a do endereço inteiro (`MessageRules.java:45-46`).
- O construtor `EmailSender(SentMessages)` assume `standard(false)` sem dizer nada (`EmailSender.java:14-16`). Hoje só
  os testes o usam.

O tipo do status é um `enum` (`MessageStatus`), gravado com `status.name()` e lido com `valueOf`
(`SentMessages.java:28`, `:51`). Os nomes são claros.

## Testes

**Números:** 33 testes rodaram, 0 falhas, 0 erros, 0 pulados. Há 33 métodos escritos: os 26 da base mais 7 novos em
`EmailSenderTest`. Cobertura de linhas: 94,4% (base: 93,8%). Cobertura de ramos: 87,2% (base: 87,1%). No
`hotel.messages`: 99 de 104 linhas e 31 de 35 ramos.
**Nota:** bom

- A parte nova está testada com valores, contra o banco:
  - a validação do endereço, com os exemplos do texto e as bordas (`EmailSenderTest.java:27-35`: `ana@gmail`,
    `ana.gmail.com`, `@gmail.com`, `a@b@gmail.com`, `ana@.com`, `ana@gmail.`);
  - a cópia logo depois da original, com o assunto `[Cópia] Oi` e o mesmo texto (`:38-48`);
  - o bloqueio sem cópia (`:51-56`);
  - o desvio no modo de teste, com o endereço, o assunto e a primeira linha exatos, e sem cópia (`:59-67`);
  - o bloqueio no modo de teste, com o endereço original (`:70-78`);
  - a instalação sem arquivo (`:81-86`);
  - o modo lido do ambiente (`:89-94`).
- A pergunta da mutação:
  - se alguém trocar `LIVE_ONLY` por `ALWAYS` na cópia, `testModeRedirectsWithoutCopy` falha (espera 1 mensagem);
  - se o bloqueio passar a julgar o e-mail já reescrito (o `testes@` é válido), `testModeStillBlocksWithTheOriginalAddress`
    falha;
  - se mudar o prefixo `[TESTE] ` ou o `\n`, a asserção do corpo falha.
- Ficaram sem rede:
  - o `status` SENT da mensagem desviada e o da cópia, que nenhum teste confere;
  - o `BLOCKED` pela API (o `HttpApiTest` só confere `SENT`, `HttpApiTest.java:82`);
  - o caso sem a variável ou com outro valor em `fromEnvironment`;
  - os ramos de espaço em `isValidAddress`.
- Os testes da base foram ajustados, não enfraquecidos: as contagens subiram de 1 para 2 mensagens, por causa da cópia
  (`ReservationServiceTest.java:91`, `:105`, `HttpApiTest.java:81`).
- Um risco: o `HttpApiTest` monta o serviço com `new Hotel(database.dataSource())` (`HttpApiTest.java:35`), que lê
  `HOTEL_TEST_MODE` do ambiente de verdade (`Hotel.java:22`). Numa máquina com a variável ligada, a asserção de 2
  mensagens falha. Bastava usar o construtor com a política explícita.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`, nenhuma chave estrangeira está
sem índice e nenhuma data está em texto. A conferência acusou `messages.status` em `text` sem CHECK. A
`V1__create_tables.sql` está igual à da base, e a `V2__message_status.sql` é nova.
**Nota:** bom

- A evolução está certa: uma migração nova, `ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT'`
  (`src/main/resources/db/migration/V2__message_status.sql:2`). A migração da base não foi editada, e as mensagens que
  já existiam ficam como `SENT`, que é o que elas foram.
- O SQL novo usa parâmetros (`SentMessages.java:23-28`), e as conexões são abertas em try-with-resources.
- `messages.status` sem CHECK segue a convenção da base, que também não tem CHECK em `reservations.status`, `rate_plan`
  e `channel`. O `enum` no Java limita os valores na escrita, mas um valor estranho gravado direto no banco quebraria
  o `MessageStatus.valueOf` (`SentMessages.java:51`). É menor.
- A original e a cópia são dois `INSERT` em conexões separadas, sem transação (`EmailSender.java:30-31`). Se o segundo
  falhar, fica uma mensagem enviada sem cópia. O `synchronized` em `send` (`EmailSender.java:24`) só garante a cópia
  "logo depois" dentro de um processo. Para o tamanho da pousada, é uma nota.
- A proteção contra reserva duplicada (o `EXCLUDE USING gist`) veio da base e continua lá.

## Comportamento

**Números:** 65 de 65 conferências passaram:
- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- o cenário, no modo normal: 9/9;
- o cenário, no modo de teste: 5/5;
- as reservas simultâneas: 2/2.
**Nota:** bom

Para o dono da pousada, faz o que ele pediu:
- endereço inválido não sai e aparece como `BLOCKED`, com o endereço que o hóspede deu e sem cópia (`ana@gmail`,
  `ana.gmail.com`, `ana@@gmail.com`, `@gmail.com`);
- toda mensagem enviada ganha a cópia para `arquivo@pousadamarealta.com.br`, logo depois dela, com `[Cópia] ` no assunto
  e o mesmo texto;
- com `HOTEL_TEST_MODE=true`, nada vai para o hóspede: tudo vai para `testes@pousadamarealta.com.br`, com `[TESTE] ` no
  assunto e a linha `Destinatário original: …`, sem cópia, e o bloqueio continua valendo.

Tudo o que já funcionava continua funcionando: preços, recusas, ciclo da reserva, relatórios, dados depois de reiniciar,
reservas simultâneas.

## Dúvidas

- Classe: fiquei entre `alternativa equivalente` e `acerto`. Não há embrulhos com a interface do envio, mas há uma lista
  de filtros com peças próprias e o escopo declarado na montagem, que é exatamente o que a referência aceita como
  equivalente. A combinação padrão mora em `MessagePolicy.standard`, não em `Hotel`. Eu não li isso como combinação
  "dentro das regras", porque nenhuma regra confere o modo.
- As regras anunciadas e ainda não feitas (madrugada, limite por dia) caberiam como `Block`. Mas se a madrugada pedir
  "adiar" em vez de "bloquear", as três fases fixas do `EmailSender` não bastam. O texto não diz qual das duas.
