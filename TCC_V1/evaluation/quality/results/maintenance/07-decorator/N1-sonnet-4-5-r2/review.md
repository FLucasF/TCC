# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em 7 pacotes: `hotel.web` (as rotas em `HttpApi`), `hotel.reservations` (o
`ReservationService` com as regras e o `ReservationStore` com o SQL), `hotel.rooms`, `hotel.reports`,
`hotel.documents` e `hotel.messages`. As peças são ligadas em `hotel.Hotel`, e os dados ficam no PostgreSQL com
migrações do Flyway. Uma confirmação segue este caminho: `HttpApi` chama `ReservationService.confirm`
(`src/main/java/hotel/reservations/ReservationService.java:56`), que grava a reserva e chama `EmailSender.send`
(`:64`), e o envio guarda a mensagem em `SentMessages`. O agente mexeu quase só em `hotel.messages`. Criou
`EmailValidator`, `MessageStatus` e `OutgoingEmail`, acrescentou a coluna `status` numa migração nova (V2), e passou
`HOTEL_TEST_MODE` de `main` até o `EmailSender` (`src/main/java/hotel/Hotel.java:47`). O comportamento pedido funciona
todo: 65 de 65 conferências passaram. O ponto mais sério é o design. As três regras (bloqueio, cópia e modo de teste)
ficaram num `if / else if / else` dentro de `EmailSender.send` (`src/main/java/hotel/messages/EmailSender.java:26-39`).
A combinação por modo está escrita lá dentro, e não na montagem. Por isso, as regras que o dono já anunciou (não
mandar de madrugada, limite por dia, a segunda pousada sem a cópia) vão exigir mais ramos e mais flags nesse mesmo
método.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números
da base são os mesmos (`measures/dependencies-base.md`): só `hotel.messages` cresceu, de 3 para 6 classes (de 75 para
146 linhas), e `hotel` foi de 85 para 90 linhas.
**Nota:** bom

O agente não criou nenhuma dependência nem nenhum ciclo. O ciclo `hotel.documents` ↔ `hotel.reservations` já existia
na base. A mudança ficou toda dentro de `hotel.messages`. O JDBC continua só em `SentMessages`
(`src/main/java/hotel/messages/SentMessages.java:20-28`), e o validador e o envio não conhecem nem o Javalin nem o
JDBC. A rota `GET /messages` continua fina (`src/main/java/hotel/web/HttpApi.java:81`): ela devolve o `Message`, que
agora traz o `status` (`src/main/java/hotel/messages/Message.java:3`). A variável de ambiente é lida num lugar só, no
`main` (`src/main/java/hotel/Hotel.java:47`), e o valor desce pelo construtor (`Hotel.java:24-29`), sem nada
`static` nem global. Os construtores antigos foram mantidos e repassam `false` (`Hotel.java:20-22`,
`EmailSender.java:13-15`), o que não quebra quem já os usava. Fica uma ressalva, que pesa no design e não aqui: a
montagem recebe só um `boolean`, e quem decide a combinação das regras é o `EmailSender`.

## Design de baixo nível

**Números:** 3 regras pedidas (bloqueio, cópia, modo de teste). Nenhuma está numa peça própria: as 3 são ramos do
mesmo `if / else if / else` em `EmailSender.send` (`EmailSender.java:26-39`). A estrutura de envio tem 0 embrulhos e 0
interfaces.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O pedido descreve comportamentos em volta do mesmo envio, que se combinam conforme o modo e conforme a
instalação, e anuncia outros. Pela referência, esse é o caso de embrulhos com a mesma interface do envio, empilhados
na montagem. O agente fez o caso que a referência chama de omissão: os três `if` dentro do envio. São eles
`!validator.isValid(to)` (`EmailSender.java:26`), `else if (testMode)` (`:28`) e o `else`, que manda o original e a
cópia (`:33-38`). A regra "no modo de teste não há cópia" não é uma combinação montada: ela nasce da ordem dos ramos.
A montagem só passa um `boolean` (`Hotel.java:29`). A validação ficou numa classe à parte (`EmailValidator`), e as
mensagens a gravar viraram uma lista de `OutgoingEmail` (`EmailSender.java:24`, `:41-47`), mas isso não forma uma
estrutura de regras. Nada substitui o `EmailSender`, e nenhuma regra embrulha outra. Com isso, cada regra anunciada
pede mexer nesse método:

- a madrugada e o limite por dia, cada uma valendo "sempre, só no teste ou só fora dele", viram mais ramos e mais
  flags;
- a segunda pousada sem a cópia vira outro `boolean` no construtor.

**O exagero.** Não há. `OutgoingEmail`, com duas fábricas estáticas (`src/main/java/hotel/messages/OutgoingEmail.java:5-11`),
é um registro pequeno que de fato carrega o que vai ser gravado. `MessageStatus` é o tipo certo para o campo
(`src/main/java/hotel/messages/MessageStatus.java:3-6`). Não há interface com uma implementação só, nem fábrica, nem
camada que só repassa.

**O resto do código.**
- **O validador aceita mais do que o dono definiu.** O dono pediu "um nome com pelo menos um ponto no meio", mas
  `EmailValidator.java:14` só confere `afterAt.contains(".")`. Com isso, `ana@gmail.` e `ana@.com` passam como
  válidos, e espaços no endereço também passam (`ana silva@gmail.com`).
- **Os endereços estão soltos no código.** `testes@...`, `arquivo@...` e os prefixos `[TESTE] ` e `[Cópia] ` são
  textos literais no meio do método (`EmailSender.java:30-31`, `:36-37`), e não constantes nem configuração. Isso pesa
  justamente porque o pedido diz que cada instalação pode querer outra combinação.
- **O valor de retorno é desajeitado.** O laço com `Message first = null` (`EmailSender.java:41-48`) existe só para
  devolver a primeira mensagem, e quem chama nem usa esse valor (`ReservationService.java:65`).

O resto não muda em relação à base: `BigDecimal` para dinheiro, `enum` para as situações, SQL com parâmetros.

## Testes

**Números:** 31 testes rodaram, com 0 falhas, 0 erros e 0 pulados. Há 31 métodos de teste escritos; a base tinha 26,
então o agente acrescentou 5 e não apagou nenhum. A cobertura de linhas foi de 93,8% para 94,1% (506 de 538), e a de
ramos de 87,1% para 89,0% (121 de 136). `hotel.messages` tem 62 de 66 linhas e 22 de 22 ramos cobertos.
**Nota:** bom

As regras novas estão conferidas com valores:
- **O validador:** `src/test/java/hotel/messages/EmailValidatorTest.java:13-29` testa os exemplos do dono
  (`ana@gmail` e `ana.gmail.com`), o `@` duplicado, a falta do que vem antes ou depois do `@`, o texto vazio e o
  `null`.
- **O bloqueio:** status `BLOCKED`, o endereço original e nenhuma cópia (`ReservationServiceTest.java:167-178`).
- **O modo de teste:** o endereço de testes, o `[TESTE] `, a primeira linha com `\n` e a mensagem sem cópia
  (`ReservationServiceTest.java:181-199`).
- **O bloqueio no modo de teste:** o endereço original é mantido (`ReservationServiceTest.java:202-220`).
- **A cópia:** vem logo depois da original, com o assunto `[Cópia] ` e status `SENT`, tanto pelo serviço
  (`ReservationServiceTest.java:92-99`) quanto pela API (`src/test/java/hotel/web/HttpApiTest.java:80-85`).

Os testes da base que mudaram foram ajustados ao comportamento novo, de 1 para 2 mensagens
(`ReservationServiceTest.java:92`, `:108`; `HttpApiTest.java:81`), sem ficarem mais fracos.

Pela pergunta da mutação, três coisas ficariam sem rede:
- **O texto da cópia.** Se ela saísse com o corpo vazio, nenhum teste falharia, porque só se conferem `to` e `subject`
  (`ReservationServiceTest.java:98-99`).
- **O ponto "no meio" do domínio.** Não há caso como `ana@gmail.` nem `ana@.com`, e por isso o defeito do validador
  passa despercebido.
- **A leitura de `HOTEL_TEST_MODE` no `main`.** Ela não é testada (`hotel` tem 2 de 4 ramos, igual à base).

Há também um detalhe nos testes do modo de teste: eles abrem um `EmptyDatabase` próprio e o fecham no fim do
método, fora de um `finally` (`ReservationServiceTest.java:198`, `:219`). Se uma asserção falhar, o banco não é
fechado.

## Banco

**Números:** 3 tabelas, todas com chave primária. Há 2 migrações do Flyway: `V1__create_tables.sql` está igual à da
base e `V2__add_message_status.sql` é nova. Dinheiro está todo em `numeric(10,2)`, nenhuma chave estrangeira está sem
índice e nenhuma data está como texto. A única coisa que as conferências acusaram: `messages.status` é `text` sem
`CHECK`, como as outras colunas de situação da base (`reservations.status`, `messages.channel`).
**Nota:** bom

O agente fez o que se espera numa manutenção. Não editou a V1 e acrescentou uma V2 com
`ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT'`
(`src/main/resources/db/migration/V2__add_message_status.sql:2`). Num banco que já roda, as mensagens antigas ficam
como `SENT`, que é o que elas foram. O `INSERT` e o `SELECT` usam parâmetros e incluem a coluna nova
(`SentMessages.java:23-28`, `:41`). A falta de `CHECK` em `status` segue a convenção da base e não perde dado, porque
quem grava é sempre `status.name()` de um `enum`. Há um ponto menor: o original e a cópia são dois `INSERT` em
conexões separadas, sem transação (`EmailSender.java:42-43`), e uma falha no segundo deixaria o original sem a cópia.
A confirmação na base também não tinha transação em volta do envio (`ReservationService.java:61-67`).

## Comportamento

**Números:** 65 de 65 conferências passaram, e nenhuma falhou:

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| depois de reiniciar | 3 de 3 |
| o cenário no modo normal | 9 de 9 |
| o cenário no modo de teste | 5 de 5 |
| as reservas simultâneas | 2 de 2 |

**Nota:** bom

Para o dono da pousada, o pedido funciona:

- Uma mensagem para um endereço válido sai com `SENT`, seguida da cópia para `arquivo@pousadamarealta.com.br`, com o
  mesmo texto e o assunto começando com `[Cópia] `.
- `ana@gmail`, `ana.gmail.com`, `ana@@gmail.com` e `@gmail.com` aparecem como uma mensagem só, `BLOCKED`, com o
  endereço original e sem cópia.
- Com `HOTEL_TEST_MODE=true`, tudo vai para `testes@pousadamarealta.com.br` com `[TESTE] ` e a linha
  "Destinatário original: …", sem cópia, e o endereço inválido continua bloqueado.

O que já funcionava (preços, recusas, cancelamentos, relatórios, os dados depois de reiniciar, as reservas
simultâneas) continua funcionando. Achei um defeito fora do roteiro, pela leitura do código, e ele pesa no design:
endereços como `ana@gmail.` ou `ana@.com` seriam aceitos e enviados (`EmailValidator.java:14`), embora fujam da
definição do dono ("um ponto no meio").

## Dúvidas

- Classe `omissão` ou `malfeito`: escolhi `omissão`. O agente separou a validação numa classe e montou uma lista de
  mensagens a gravar, mas nenhuma das três regras virou um embrulho ou uma peça da estrutura de envio. As três
  continuam como ramos de um `if` dentro de `send`, que é o caso que a referência chama de omissão.
- Não testei pela API o defeito de `ana@gmail.`. A conclusão vem só da leitura de `EmailValidator.java:14`, que é
  inequívoca.
