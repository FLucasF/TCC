# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em sete pacotes (`hotel.web` com as rotas, `hotel.reservations` com o serviço e o acesso
às reservas, `hotel.messages` com o envio e a lista de mensagens, `hotel.rooms`, `hotel.reports`, `hotel.documents`
e a montagem em `hotel.Hotel`), com os dados no PostgreSQL criados por migrações do Flyway. Uma confirmação passa por
`HttpApi` → `ReservationService.confirm` → `EmailSender.send` → `SentMessages.add` (INSERT em `messages`). O agente
mexeu só no pacote `hotel.messages`, na montagem (`Hotel.java`) e numa migração nova. As três regras ficaram numa
lista de peças `MessageRule` que o `EmailSender` aplica em ordem. Quem decide a combinação por modo é a montagem
(`MessageRules.standard(testMode)`, chamada em `Hotel.java:26`). O modo de teste vem de `HOTEL_TEST_MODE`
(`Hotel.java:45`), e o `status` foi para o banco por uma `V2` nova. Nada de sério: o roteiro passou inteiro (65 de 65)
e os 33 testes passam. Os defeitos são pequenos:
- `send` pode devolver `null` se uma regra futura tirar a mensagem;
- a original e a cópia são gravadas sem transação;
- o README não fala do modo de teste nem do `status`.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), igual ao da
base (`measures/dependencies-base.md`). Do lado do agente, o `hotel.messages` passou de 3 para 8 classes (de 75 para
213 linhas) e continua sem depender de nenhum outro pacote do projeto.
**Nota:** bom

- As regras novas ficam todas em `hotel.messages` e não conhecem o Javalin nem o JDBC
  (`src/main/java/hotel/messages/MessageRules.java:18-49`, `MessageRule.java:10-17`).
- O SQL continua isolado em `SentMessages` (`SentMessages.java:20-58`).
- A borda HTTP não mudou: `/messages` só serializa `sentMessages.all()` (`src/main/java/hotel/web/HttpApi.java:81`), e
  o campo `status` sai do record `Message` (`Message.java:3`).
- A leitura da variável de ambiente fica no `main` (`Hotel.java:45`) e chega às peças como parâmetro
  (`Hotel.java:21,26`). Nenhuma regra lê `System.getenv`.
- O agente não criou nenhuma dependência nova entre pacotes. O ciclo `documents`/`reservations` já vinha da base.

## Design de baixo nível

**Números:** 3 regras pedidas, as 3 dentro da estrutura (`MessageRules.java:25-39`), nenhuma fora dela. Nenhum `if`
sobre o modo de teste no envio nem nas regras. O modo só aparece em `RuleMode.applies` (`RuleMode.java:9-15`), usado
na montagem (`MessageRules.java:19-22`).
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

**O pattern.** Não é o Decorator clássico: não há uma interface de envio com embrulhos que chamam o próximo. É uma
lista de filtros que o envio aplica, o que a referência aceita como alternativa equivalente:
- cada regra é uma peça própria (`MessageRules.java:25-27`, `29-32`, `34-39`) com a mesma interface
  `List<Outgoing> apply(Outgoing)` (`MessageRule.java:12`);
- o `EmailSender` só percorre a lista, sem saber quais regras existem nem qual é o modo (`EmailSender.java:21-42`);
- a combinação por modo fica fora das regras: `MessageRule.in(RuleMode, testMode, regra)` (`MessageRule.java:15-17`)
  troca a regra por uma passagem direta quando o modo não vale, e a lista é montada uma vez
  (`MessageRules.java:18-23`), a partir de `Hotel.java:26`;
- a cópia não pergunta se está em modo de teste (`MessageRules.java:29-32`), e o desvio não sabe da cópia.

A interface `List<Outgoing>` cobre os três casos: mudar, bloquear e acrescentar. As regras anunciadas (madrugada,
limite por dia) entram como peças novas na lista, cada uma com um `RuleMode`. Uma instalação sem a cópia é outra lista
(o teste disso está em `EmailSenderTest.java:88-92`). A troca é feita no código, o que faz sentido, já que o texto não
pede configuração.

O que afasta do Decorator: a regra não controla a chamada do "próximo". Quem trata a mensagem bloqueada é o laço do
envio, que a deixa passar sem chamar as regras seguintes (`EmailSender.java:26-27`). A regra de bloqueio não interrompe
a corrente por conta própria. É uma convenção da lista, e está documentada (`MessageRule.java:8`).

**O exagero.** Não há. `Outgoing` (`Outgoing.java:4`) separa a mensagem ainda sem `id` da `Message` gravada. `RuleMode`
responde ao pedido de cada regra valer sempre, só no teste ou só fora dele. `MessageStatus` é o enum do campo pedido.
Não há fábrica, builder nem camada que só repassa. `MessageRules` é só o catálogo das regras e da combinação padrão.

**O resto do código** está bem: tipos próprios (`enum` para o status e o modo), records imutáveis, nenhum `static`
mutável, e o erro de SQL embrulhado com a causa (`SentMessages.java:34-35`). Três detalhes:
- `EmailSender.send` devolve `null` se as regras não deixarem nenhuma mensagem (`EmailSender.java:34-41`). Hoje isso
  não acontece, e quem chama ignora o retorno (`ReservationService.java:65`), mas uma regra de "não mandar de
  madrugada" que descarte a mensagem cairia nesse caso.
- `"true".equals(System.getenv("HOTEL_TEST_MODE"))` (`Hotel.java:45`) não aceita `TRUE`. É uma escolha aceitável.
- Linha longa em `Hotel.java:26`.

Nenhum desses é de mudar antes de aceitar.

## Testes

**Números:** 33 testes rodaram, 0 falhas, 0 erros, 0 pulados, 33 métodos escritos. Na base eram 26, todos mantidos;
o agente acrescentou 7. Cobertura de linhas 94.2% (523 de 555), de ramos 89.0% (129 de 145). No `hotel.messages`,
80 de 84 linhas e 30 de 31 ramos (na base, 29 de 33 e 2 de 2).
**Nota:** bom

A parte nova tem testes com valores, contra o banco de verdade, no `src/test/java/hotel/messages/EmailSenderTest.java`:
- a cópia logo depois da original, com o endereço, o assunto `"[Cópia] Olá"`, o mesmo texto e `SENT` (`:27-38`);
- o endereço inválido `BLOCKED` sem cópia (`:42-48`);
- o desvio no modo de teste, com o endereço de testes, `"[TESTE] Olá"` e a primeira linha
  `"Destinatário original: ana@gmail.com\ntexto"` (`:52-60`);
- o bloqueio no modo de teste com o endereço e o assunto originais (`:64-71`);
- nove casos da validação do endereço (`:75-84`);
- uma regra fora do seu modo que não faz nada (`:96-100`).

Os testes antigos foram adaptados, não enfraquecidos: o `HttpApiTest.java:81-84` passou a esperar 2 mensagens e
confere o `status` e a cópia pela API. Os testes do serviço montam o envio sem regras (`ReservationServiceTest.java:26`,
`ReportsTest.java:30`), e as contagens de e-mail deles continuam valendo.

A pergunta da mutação:
- tirar `"[Cópia] "` ou o `"\n"` do desvio quebra `:36` ou `:59`;
- trocar `at < 1` por `at < 0` quebra `:80`;
- tirar o teste do segundo `@` quebra `:82`.

O que fica sem rede:
- a leitura de `HOTEL_TEST_MODE` no `main` (`Hotel.java:45`), que nenhum teste sobe;
- o `BLOCKED` pela API;
- o `aRuleCanBeLeftOutOfAnInstallation` (`:88-92`), que só confere a contagem.

São lacunas pequenas, porque a combinação inteira de cada modo está testada pelo `MessageRules.standard`.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`. Não há chave estrangeira sem
índice nem data em texto. 2 migrações: a `V1` igual à da base, a `V2__message_status.sql` nova. `messages.status` é
`text NOT NULL` sem CHECK (as outras colunas de situação da base também não têm). Nenhum SQL montado com dado de fora:
os 4 candidatos do script, em `ReservationStore`, juntam só constantes, e vêm da base.
**Nota:** bom

- O agente evoluiu o esquema do jeito certo: uma migração nova, sem tocar na `V1`.
  `ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT'`
  (`src/main/resources/db/migration/V2__message_status.sql:2`) preenche as mensagens que já existiam com `SENT`. Isso
  é verdade para elas, já que antes nada era bloqueado.
- O INSERT e o SELECT usam parâmetros e incluem o `status` (`SentMessages.java:23-28`, `42`, `52`).
- `messages.status` não tem CHECK limitando a `SENT`/`BLOCKED`. Segue o costume da base (`reservations.status` e
  `rate_plan` também não têm), e o enum no Java já barra outros valores (`SentMessages.java:28,52`).
- A original e a cópia são gravadas em INSERTs separados, cada um com a sua conexão e sem transação
  (`EmailSender.java:35-36`). Se o segundo falhar, a original fica sem a cópia do contador. A base também não usava
  transação na confirmação (`ReservationService.java:63-68`), e o risco é pequeno.

## Comportamento

**Números:** 65 de 65 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário no modo normal: 9 de 9;
- o cenário no modo de teste: 5 de 5;
- as reservas simultâneas: 2 de 2.
**Nota:** bom

Nada falhou. Para o dono da pousada:
- a mensagem para endereço válido sai e gera, logo depois, a cópia para `arquivo@pousadamarealta.com.br` com
  "[Cópia] " e o mesmo texto;
- "ana@gmail", "ana.gmail.com", "ana@@gmail.com" e "@gmail.com" ficam `BLOCKED`, sem cópia, com o endereço original;
- com `HOTEL_TEST_MODE=true`, nada vai para o hóspede: a mensagem vai para `testes@pousadamarealta.com.br` com
  "[TESTE] " e a linha do destinatário original, sem cópia, e o endereço inválido continua bloqueado com o endereço
  original;
- tudo o que já funcionava continua funcionando.

Fora do roteiro, a validação do endereço é tão frouxa quanto o texto (`MessageRules.java:42-49`): aceita espaços e
"ana@gmail..com". Isso segue a definição do dono e não pesa.

## Dúvidas

- A classe ficou entre `acerto` e `alternativa equivalente`. Dei a equivalente porque a estrutura é uma lista de
  filtros percorrida pelo envio (`EmailSender.java:23-33`), não embrulhos com a interface do envio, e porque a parada
  na mensagem bloqueada é decidida pelo laço, não pela regra.
- O README não foi atualizado. Ele não fala do modo de teste nem do campo `status` (`README.md`, seção "Mensagens"). O
  texto não pedia isso, e não pesei em nenhuma coluna.
