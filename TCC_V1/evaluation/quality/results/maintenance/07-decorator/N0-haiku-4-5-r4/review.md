# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel` monta as peças (`Hotel.java:20-29`), `hotel.web` tem as rotas
(`HttpApi`), `hotel.reservations` tem as regras e o acesso às reservas, `hotel.messages` guarda os e-mails "enviados"
no PostgreSQL, e o esquema vem das migrações do Flyway. Na confirmação, uma requisição passa por `HttpApi` →
`ReservationService.confirm` → `EmailSender.send` → `SentMessages.add`, que grava na tabela `messages`. O agente fez
uma mudança pequena e que funciona: a migração nova `V2__add_message_status.sql`, o campo `status` no `Message`, um
`EmailValidator`, um `MessageRules` que lê `HOTEL_TEST_MODE` e as três regras de envio. As 65 conferências do roteiro
passaram e os 34 testes também. O mais sério é o design: as três regras ficaram como `if` dentro de
`SentMessages.addProcessed` (`final/src/main/java/hotel/messages/SentMessages.java:30-42`), que é a classe de acesso
ao banco. Não há embrulho do envio e a combinação por modo não é decidida na montagem. Por isso as regras que o dono já
anunciou (não mandar de madrugada, limite por dia, cada uma valendo sempre, só no teste ou só fora dele) e a pousada
sem cópia vão obrigar a mexer de novo nesse mesmo método.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base são
os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. Só `hotel.messages` mudou, de 3 classes e 75 linhas
para 5 classes e 140 linhas, e `SentMessages` passou de 56 para 91 linhas (`measures/dependencies.md`,
`measures/dependencies-base.md`).
**Nota:** com problemas

Pelos números, o agente não criou nenhuma dependência nova e não piorou o grafo. O ciclo `documents`↔`reservations`
vem da base. A borda HTTP continua fina: `/messages` só serializa a lista (`HttpApi.java:81`).

O problema está em onde as regras novas foram parar:

- **Regra de negócio dentro da classe de dados.** O bloqueio, o desvio para testes e a cópia para o arquivo moram em
  `SentMessages` (`SentMessages.java:30-51`), a mesma classe que monta o `INSERT` (`SentMessages.java:53-69`). O
  ponto natural era `EmailSender.send`, o envio, mas ele continua só repassando a chamada
  (`EmailSender.java:12-14`). Assim, "guardar a mensagem" e "decidir para onde e se ela sai" ficaram misturados.
- **A montagem saiu do lugar dela.** `Hotel.java:23` continua a chamar `new SentMessages(dataSource)`, e esse
  construtor cria por dentro um `new MessageRules()` (`SentMessages.java:17-19`), que lê `System.getenv` no próprio
  construtor (`MessageRules.java:7-9`). O modo de teste, que decide o comportamento do sistema inteiro, não passa pela
  classe de montagem: é uma dependência escondida.

É uma mistura localizada, num pacote só, e não quebra as camadas do resto.

## Design de baixo nível

**Números:** 3 regras de envio pedidas (bloqueio, cópia, desvio de teste), as 3 fora de qualquer estrutura: são 2 `if`
e uma sequência fixa em `SentMessages.addProcessed` (`SentMessages.java:31-41`). Há 0 embrulhos e 0 interfaces de
envio. A combinação por modo está escrita dentro do envio, não na montagem.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern (Decorator).** O pedido descreve três comportamentos em volta do mesmo envio, que se combinam conforme o
modo, e anuncia outros que podem valer "sempre, só no modo de teste ou só fora dele", com instalações diferentes
querendo combinações diferentes. O agente resolveu isso com o caso exato que a referência chama de omissão: os três
`if` dentro do envio.

- `SentMessages.java:31` testa o endereço e bloqueia.
- `SentMessages.java:35` testa `rules.isTestMode()` e desvia.
- `SentMessages.java:39-40` grava a original e a cópia sem condição.

A combinação "cópia só fora do teste" sai da ordem dos `return`, não de uma decisão de montagem. Para atender a
segunda pousada (sem cópia), seria preciso mais um `if` com mais uma flag em `MessageRules`. As regras de madrugada e
de limite diário, cada uma com três modos, fariam esse método crescer em combinações. `EmailSender`
(`EmailSender.java:3-15`) não ganhou interface nem embrulho, e `Hotel.java:20-29` não escolhe nada. Nada aproxima
o resultado do pattern: `MessageRules` é só um booleano (`MessageRules.java:3-14`), não uma lista de regras.

**Exagero.** Não há estrutura sem necessidade. `MessageRules` e `EmailValidator` são classes pequenas que fazem uma
coisa cada.

**O resto do código mudado.** Os defeitos são localizados:

- O status é texto solto, `"SENT"` e `"BLOCKED"` (`SentMessages.java:39-40,45,50`, `Message.java:3`), em vez de um
  `enum`. O banco também não tem `CHECK` nele (`V2__add_message_status.sql:1`).
- Os endereços `arquivo@…` e `testes@…` e os prefixos estão em literais dentro dos métodos
  (`SentMessages.java:40,49-50`), sem nome nem configuração. Isso pesa porque a "outra pousada" vai precisar mudar
  justamente isso.
- `add` só repassa para `addProcessed` (`SentMessages.java:26-28`): um método que não acrescenta nada.
- `MessageRules` não é `final` e lê o ambiente no construtor (`MessageRules.java:7-9`). Por isso os testes precisam
  sobrescrevê-la com uma classe anônima (`SentMessagesTest.java:51-56`).
- O `EmailValidator` segue a regra do dono (`EmailValidator.java:9-14`). Detalhe: ele aceita espaços no endereço
  (`"ana silva@gmail.com"`), o que o texto não mencionou.

## Testes

**Números:** final com 34 testes rodados, 0 falhas, 0 erros, 0 pulados, e 34 métodos escritos. Base com 26 rodados,
0 falhas. Cobertura de linhas de 94,0% (498/530; na base, 93,8%) e de ramos de 88,8% (119/134; na base, 87,1%). Em
`hotel.messages`: 56/61 linhas e 20/20 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

O agente testou a parte nova com valores:

- Os testes do validador cobrem exatamente os exemplos do dono, `ana@gmail` e `ana.gmail.com`, além de `@`, `@@` e
  ponto na ponta (`EmailValidatorTest.java:19-30`).
- `SentMessagesTest` confere o bloqueio com o endereço original e sem cópia (`SentMessagesTest.java:20-30`); a cópia
  logo depois da original, com `[Cópia] ` e `SENT` (`:33-47`); o modo de teste com o destinatário, o `[TESTE] ` e o
  corpo exato com "Destinatário original:" (`:50-67`); a falta da cópia no teste (`:70-83`) e o bloqueio no teste
  (`:86-101`).
- Pela API, `HttpApiTest.java:80-85` confere o `status` no JSON, e `HttpApiTest.java:127-136` confere o `BLOCKED`.

O agente ajustou os testes da base que mudaram de verdade, de 1 para 2 mensagens, e não os enfraqueceu: acrescentou
asserções (`ReservationServiceTest.java:91-101`, `HttpApiTest.java:81-85`). Os 26 testes da base continuam passando.

Ficaram sem rede:

- **"Com o mesmo texto" da cópia.** Nenhum teste confere o `body()` da cópia (`SentMessagesTest.java:44-46`,
  `ReservationServiceTest.java:98-101`). Se alguém trocar `body` por `""` em `SentMessages.java:40`, nada falha.
- **A leitura de `HOTEL_TEST_MODE`.** O modo de teste só é testado com a classe anônima. Não há teste de que a
  variável liga o modo (`MessageRules.java:8`), nem pela API.
- **Testes que dependem do ambiente.** `SentMessagesTest.java:21,34`, `ReservationServiceTest` e `HttpApiTest` usam
  `new MessageRules()`, que lê o ambiente real. Rodar `mvn verify` numa máquina com `HOTEL_TEST_MODE=true` (o caso do
  pessoal do site) faria falhar os testes que esperam a cópia para o arquivo, como
  `SentMessagesTest.sendsValidEmailsAndCreatesArchiveCopy`, `ReservationServiceTest.confirmingSendsTheReceiptByEmail`
  e `HttpApiTest.booksConfirmsAndChecksOut`.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` (6 colunas, todas ok). Nenhuma chave
estrangeira sem índice e nenhuma data em texto. Há uma restrição `EXCLUDE` contra reserva dupla. Migrações: V1 igual à
base, V2 nova. As conferências acusam só as colunas de situação sem `CHECK`, entre elas a nova `messages.status`. Não
há nenhuma transação explícita (`measures/database.md`).
**Nota:** com problemas

O agente fez o certo no essencial:

- O esquema evoluiu por uma migração nova, sem editar a da base (`V2__add_message_status.sql:1`).
- A coluna é `NOT NULL DEFAULT 'SENT'`, então as mensagens que já existiam num banco em uso ficam com um valor
  coerente.
- O SQL novo usa parâmetros e try-with-resources (`SentMessages.java:54-68,72-75`).

Há dois defeitos menores:

- **`status` sem `CHECK`.** A coluna é `text` sem `CHECK (status IN ('SENT','BLOCKED'))`
  (`V2__add_message_status.sql:1`), e o banco aceita qualquer texto. As outras colunas de situação da base também não
  têm `CHECK`, então o agente seguiu o costume, mas a coluna é dele.
- **A original e a cópia não andam juntas.** São gravadas em dois `insertMessage` independentes, cada um com a sua
  conexão e em autocommit (`SentMessages.java:39-40`). Se o segundo falhar, fica a original sem a cópia que o contador
  exige, e a confirmação estoura uma exceção depois de a reserva já estar paga.

A duplicação de reserva continua protegida pela `EXCLUDE` que vem da base.

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam.

- Parte comum: 46 de 46.
- Dados depois de reiniciar: 3 de 3.
- Cenário no modo normal: 9 de 9.
- Cenário no modo de teste (`HOTEL_TEST_MODE=true`): 5 de 5.
- Reservas simultâneas: 2 de 2.

(`measures/behavior.md`)
**Nota:** bom

Tudo o que o dono pediu funciona pela API:

- **Endereço válido:** o hóspede recebe com `SENT`, e a cópia para `arquivo@pousadamarealta.com.br` vem logo depois,
  com `[Cópia] `, o mesmo texto e `SENT`.
- **Endereços inválidos** (`ana@gmail`, `ana.gmail.com`, `ana@@gmail.com`, `@gmail.com`): aparecem uma vez só, como
  `BLOCKED`, com o endereço original e sem cópia.
- **Modo de teste:** a mensagem vai para `testes@…` com `[TESTE] ` e a linha "Destinatário original:", sem cópia, e o
  endereço inválido continua bloqueado com o endereço original.

Nada do que já funcionava quebrou: a parte comum, os dados depois de reiniciar e as reservas simultâneas passaram.
Para o dono, o sistema faz hoje o que ele pediu. O custo aparece no próximo pedido (ver Design).

## Dúvidas

- A falta de transação entre a original e a cópia (`SentMessages.java:39-40`) não aparece em nenhuma chamada do
  roteiro. Contei-a no banco como defeito menor, sem medir o efeito de uma falha real.
- A quebra dos testes com `HOTEL_TEST_MODE=true` no ambiente foi deduzida da leitura (`MessageRules.java:8` e os
  `new MessageRules()` dos testes), não executada.
