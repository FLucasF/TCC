# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em sete pacotes. `hotel.Hotel` monta as peças, `hotel.web.HttpApi` é a borda HTTP,
`hotel.reservations` guarda as regras e o acesso às reservas, `hotel.rooms` cuida dos quartos e `hotel.messages` dos
e-mails, que ficam gravados na tabela `messages`. Os esquemas vêm das migrações do Flyway. O agente mexeu só no envio de
e-mail. `EmailSender.send` (`src/main/java/hotel/messages/EmailSender.java:23-34`) passa a mensagem por uma lista de
regras. Cada regra é uma classe própria (`ValidAddressRule`, `ArchiveCopyRule`, `TestRedirectRule`), tem um escopo
(`RuleScope`: sempre, só no teste, só fora dele) e devolve as mensagens que saem no lugar da que recebeu. A lista com os
escopos fica em `MessageRules.ofTheInn()` (`MessageRules.java:14-19`). O modo de teste é lido de `HOTEL_TEST_MODE` no
`main` (`Hotel.java:50`). O status entrou por uma migração nova (`V2__message_status.sql:2`), e as migrações da base não
foram tocadas. Os 31 testes passam e as 65 conferências do roteiro também. Não achei nada sério. Os pontos menores são
estes: a mensagem e a cópia são gravadas sem uma transação que as una, e `messages.status` não tem CHECK, o mesmo
costume da base para as outras colunas de situação.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, tanto na `base` quanto na `final`. Há um ciclo
(`hotel.documents` ↔ `hotel.reservations`), que já existia na base. `hotel.messages` passou de 3 para 12 classes e de
75 para 247 linhas, e continua sem depender de nenhum pacote do projeto (depende de 0, é usado por 3).
**Nota:** bom

O agente não criou nenhuma dependência nova nem nenhum ciclo novo. O ciclo `documents`↔`reservations` veio da base, e o
patch não o toca. A parte nova ficou toda em `hotel.messages`, que não usa Javalin; o único JDBC do pacote continua em
`SentMessages` (`SentMessages.java:21-23`, `:45`). As regras não conhecem nem o banco nem o HTTP: elas transformam um
`OutgoingMessage` em memória (`ArchiveCopyRule.java:12-13`, `TestRedirectRule.java:12-15`,
`ValidAddressRule.java:10`). A montagem continua num lugar só. `Hotel.java:31` cria o `EmailSender` com as regras e o
modo, e `Hotel.java:50` lê a variável de ambiente. `ReservationService` não foi alterado e continua chamando só
`emailSender.send(...)` (`ReservationService.java:65`). A borda HTTP também não mudou: `GET /messages` serializa o
registro, que agora tem `status` (`HttpApi.java:81`). Nenhuma classe ficou desproporcional; a maior do pacote novo é
`SentMessages`, com 64 linhas.

## Design de baixo nível

**Números:** as 3 regras pedidas estão na estrutura, e nenhuma ficou fora dela. Há 3 peças `MessageRule`, cada uma numa
classe. Nenhuma regra consulta o modo de teste. A combinação por modo está declarada em 1 lugar
(`MessageRules.java:16-18`). Não sobrou nenhum `if` sobre o endereço, o modo ou a cópia em quem chama o envio.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

O texto pede, na prática, um Decorator. O agente fez outra coisa: uma lista de filtros que o envio aplica em sequência
(`EmailSender.java:25-31`). A referência aceita essa saída como alternativa equivalente, desde que cada regra seja uma
peça própria e que a combinação por modo fique fora das regras. As duas condições valem aqui:

- cada regra é uma classe com a mesma interface (`MessageRule.java:11`);
- o bloqueio, a cópia e o desvio não sabem em que modo o sistema está (`ValidAddressRule.java:10`,
  `ArchiveCopyRule.java:11-14`, `TestRedirectRule.java:11-16`);
- o escopo de cada regra (`ALWAYS`, `OUTSIDE_TEST_MODE_ONLY`, `TEST_MODE_ONLY`) é declarado na lista da instalação
  (`MessageRules.java:16-18`).

Para acrescentar a regra da madrugada ou o limite diário, basta uma classe nova e uma linha nessa lista. Para a segunda
pousada, sem a cópia, basta outra lista; o teste `EmailSenderTest.java:75-80` mostra exatamente isso.

O que afasta do Decorator propriamente dito: não há embrulhos encadeados. Quem filtra pelo escopo é o `EmailSender`
(`EmailSender.java:26`), que recebe o `testMode`. Ele faz isso de forma genérica, sem perguntar por nenhuma regra em
particular, e por isso não é o caso de "o envio de dentro pergunta pelo modo de teste". Também é o `EmailSender` que
decide que uma mensagem bloqueada não passa pelas regras seguintes (`EmailSender.java:28`). Isso deixa a política
"bloqueada não gera cópia" no laço, e não numa regra, mas trata todas as regras da mesma maneira.

Não há exagero. `RuleScope` (`RuleScope.java:4-15`) e `ScopedRule` (`ScopedRule.java:3`) servem diretamente ao pedido
"sempre, só no teste ou só fora". `OutgoingMessage` é um record imutável com métodos `with...`
(`OutgoingMessage.java:4-28`), sem builder nem fábrica. `MessageRule` tem três implementações.

No resto do código, o status é um `enum` (`MessageStatus.java:3-6`), e não texto solto. Os records são imutáveis, não
há `static` mutável e os erros de SQL continuam saindo com a causa (`SentMessages.java:40`). A validação do endereço
(`ValidAddressRule.java:13-23`) segue a definição do dono sem regex e está correta nos casos dados. Os detalhes que um
revisor apontaria são pequenos. `Hotel.java:31` ficou com uma linha longa demais. `EmailSender.send` devolve
`kept.get(0)` (`EmailSender.java:33`), o que supõe que a primeira mensagem é sempre a original; isso vale para as três
regras atuais, mas é implícito. Nenhum dos dois pede mudança antes de aceitar.

## Testes

**Números:** na `final`, 31 testes rodaram, com 0 falhas, 0 erros e 0 pulados; são 31 métodos escritos. Na `base` eram
26. A cobertura de linhas foi de 93,8% na base para 94,1% (528 de 561), e a de ramos de 87,1% para 87,9% (124 de 141).
Em `hotel.messages`, são 83 de 88 linhas e 25 de 27 ramos.
**Nota:** bom

O agente escreveu `EmailSenderTest` (5 testes) contra o banco real, e cada teste confere a regra nova com valores:

- a cópia vem logo depois da original, para `arquivo@pousadamarealta.com.br`, com `[Cópia] Oi`, o mesmo texto e
  `SENT` (`EmailSenderTest.java:26-37`);
- sete endereços inválidos ficam `BLOCKED` e não geram cópia (`:40-49`);
- no modo de teste, a mensagem vai para `testes@…`, com `[TESTE] Oi` e o corpo
  `"Destinatário original: ana@gmail.com\nTexto"` (`:52-61`);
- no modo de teste, o inválido continua bloqueado e com o endereço original (`:64-72`);
- uma instalação pode ficar sem o arquivo (`:75-80`).

`HttpApiTest.java:81-83` confere pela API o campo `status` e a cópia logo depois da original. Nos testes da base, o
agente trocou duas contagens de 1 para 2 mensagens (`ReservationServiceTest`), por causa da cópia que o texto pede.
Essa troca não enfraquece nada: as asserções sobre a primeira mensagem continuam lá.

A pergunta da mutação dá os seguintes resultados. Trocar o prefixo "[Cópia] " ou "[TESTE] " derruba um teste. Tirar
o `isBlocked()` de `EmailSender.java:28` também, porque a cópia apareceria em `:46`. Mudar `at < 1` para `at < 0`
derruba `@gmail.com`. Ficariam sem rede três coisas:

- a leitura de `HOTEL_TEST_MODE` no `main` (`Hotel.java:50`; o pacote `hotel` tem 28 de 34 linhas cobertas);
- o `BLOCKED` visto pela API;
- um limite como `domain.length() > 2`, porque nenhum endereço válido curto (como `a@b.c`) é testado.

São lacunas pequenas, e as regras centrais estão conferidas.

## Banco

**Números:** 3 tabelas e 2 migrações (`V1` igual à base, `V2` nova). Nenhuma tabela está sem chave primária, nenhum
dinheiro está fora de `numeric(10,2)`, nenhuma chave estrangeira está sem índice e nenhuma data está em texto. As
colunas de situação estão sem CHECK, inclusive a nova `messages.status`. Não há transação explícita (0 chamadas a
`setAutoCommit(false)`).
**Nota:** bom

O agente evoluiu o esquema do jeito certo. Fez uma migração nova, `V2__message_status.sql:2`, que é um
`ALTER TABLE ... ADD COLUMN status text NOT NULL DEFAULT 'SENT'`, e deixou a `V1` intacta. Num banco que já roda o
sistema, as mensagens antigas recebem `SENT`, o que é o certo, porque todas foram de fato enviadas. O SQL novo usa
parâmetros (`SentMessages.java:23`), e a conexão é devolvida por try-with-resources (`:21`). O status é gravado pelo
`name()` do enum e lido de volta com `valueOf` (`:57`).

Faltam duas coisas, ambas pequenas:

- `messages.status` não tem `CHECK (status IN ('SENT','BLOCKED'))`. É o mesmo costume da base, que também não tem
  CHECK em `reservations.status` (`V1__create_tables.sql:33`).
- A original e a cópia são gravadas em duas conexões separadas (`EmailSender.java:32` chama `SentMessages.add` uma vez
  por mensagem). Se a segunda gravação falhar, a original fica sem a cópia do contador. A base também não usava
  transações.

Nenhum dos dois põe dado em risco nos casos normais. A proteção contra a reserva duplicada (o `EXCLUDE USING gist`)
veio da base e continua no lugar.

## Comportamento

**Números:** 65 de 65 conferências passaram. Por parte: a parte comum, 46 de 46; os dados depois de reiniciar, 3 de 3;
o cenário no modo normal, 9 de 9; o cenário com `HOTEL_TEST_MODE=true`, 5 de 5; as duas reservas ao mesmo tempo, 2 de
2.
**Nota:** bom

Nenhuma conferência falhou. Para o dono da pousada, isso quer dizer o seguinte:

- no modo normal, o hóspede com endereço válido recebe a mensagem (`SENT`) e o contador recebe logo em seguida a cópia
  com "[Cópia] " e o mesmo texto;
- os endereços "ana@gmail", "ana.gmail.com", "ana@@gmail.com" e "@gmail.com" ficam `BLOCKED`, com o endereço original
  e sem cópia, para a recepção ligar;
- no modo de teste, nada sai para o hóspede: tudo vai para `testes@…`, com "[TESTE] " e a linha "Destinatário original:
  …", sem cópia, e o endereço inválido continua bloqueado com o endereço original.

Tudo o que já funcionava continua funcionando: preços, recusas, cancelamentos, relatórios, os dados depois de reiniciar
e a recusa da reserva duplicada.

## Dúvidas

- **A classe do pattern.** Dei "alternativa equivalente", e não "acerto", porque a estrutura é uma lista de filtros
  aplicada pelo `EmailSender` (`EmailSender.java:25-31`), e não embrulhos encadeados com a interface do envio. Quem
  avalia o escopo contra o `testMode` é o próprio `EmailSender` (`:26`). A referência aceita essa forma, porque a
  combinação por modo fica declarada fora das regras, em `MessageRules.java:16-18`. Um leitor mais estrito poderia
  achar que essa decisão deveria estar na montagem (`Hotel`), e não no envio.
- **A combinação de cada instalação.** Ela está fixa no código (`MessageRules.ofTheInn()`, usada em `Hotel.java:31`).
  Para a segunda pousada, alguém precisa mudar o código, e não uma configuração. O texto não diz como a instalação
  escolhe as regras, então não contei isso como defeito.
