# Revisão: construção, cenário 07

## Visão geral

O projeto tem sete pacotes: `web` (as rotas do Javalin, numa classe `Routes`), `service` (`ReservationService`),
`domain` (reserva, preço, planos, quartos, comprovante), `messaging` (o envio das mensagens e as regras em volta dele),
`persistence` (JDBC puro sobre o PostgreSQL), `report` (os dois CSV) e a raiz com o `Main`, que monta as peças. Uma
requisição entra em `Routes`, que lê e valida o corpo e chama o serviço. O serviço usa o domínio para calcular o preço
e mudar a situação da reserva, e o repositório grava a mudança numa transação com `FOR UPDATE`. Na confirmação, o
serviço manda o e-mail por um `MessageSender`. Esse `MessageSender` é uma pilha de embrulhos (bloqueio, cópia para o
arquivo, desvio para a caixa de testes) montada no `Main` conforme o modo. Os dados ficam em duas tabelas
(`reservations` e `messages`), criadas por `CREATE TABLE IF NOT EXISTS` no código. Nada sério: as 65 conferências do
comportamento passam, os 11 testes passam com 90,6% de linhas cobertas e não há ciclo entre pacotes. O ponto mais
fraco é o banco: o esquema é criado no código e não tem migrações, nem restrições, nem índices, nem pool de conexões.

## Arquitetura

**Números:** 7 pacotes, 16 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só
em `web` e na raiz, e o JDBC só em `persistence` e na raiz. A maior classe é `Routes`, com 190 linhas, e logo depois
vem `ReservationRepository`, com 161.
**Nota:** bom

- A borda HTTP é fina. Cada rota é uma linha que lê, chama o serviço e escreve
  (`src/main/java/br/com/marealta/web/Routes.java:51-58`), e a validação do corpo fica em `parseNewReservation`
  (`Routes.java:106-131`). Nenhuma regra de preço ou de situação fica no handler.
- As regras não conhecem nem o Javalin nem o JDBC. As transições de situação ficam em
  `domain/Reservation.java:47-67`, o preço em `domain/Pricing.java:14-35` e o desconto e a devolução no próprio enum
  do plano (`domain/RatePlan.java:6-23`). O serviço só orquestra (`service/ReservationService.java:22-57`).
- O SQL fica todo em `persistence` (`ReservationRepository.java`, `PostgresMessageLog.java`, `Database.java`).
- A montagem fica num lugar só: `Main.java:15-39` cria o `DataSource`, o repositório, o log e a pilha de envio, e
  liga tudo por construtor.
- Pequenos vazamentos, que não pedem mudança imediata:
  - `Routes` recebe o repositório e monta os relatórios direto sobre ele, sem passar pelo serviço
    (`Routes.java:34-38` e `Routes.java:69-72`). Os relatórios também consultam o banco no construtor
    (`report/OccupancyReport.java:13-15`, `report/RevenueReport.java:14-18`).
  - O domínio carrega o código HTTP nas exceções (`domain/ApiException.java:16-38`) e uma anotação do Jackson
    (`domain/Receipt.java:20`).

## Design de baixo nível

**Números:** as 3 regras de envio pedidas estão na estrutura, cada uma num embrulho próprio (`BlockInvalidAddress`,
`ArchiveCopy`, `RedirectToTest`), e nenhuma ficou fora dela. Nenhum `if` sobre o modo de teste existe dentro dos
embrulhos nem do envio final. A combinação por modo fica numa lista só, em `Main.java:31-39`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** É o Decorator, do jeito que a referência descreve:

- Há uma interface de envio, `messaging/MessageSender.java:4-6`, com o envio final `messaging/LogSender.java:4-14`,
  que só grava como SENT.
- Cada regra implementa a mesma interface e chama o próximo passo:
  - o bloqueio grava BLOCKED com o endereço original e não segue adiante (`messaging/BlockInvalidAddress.java:23-29`);
  - a cópia só sai se a original saiu (`messaging/ArchiveCopy.java:12-18`);
  - o desvio troca o destinatário, o assunto e a primeira linha do texto (`messaging/RedirectToTest.java:12-17`).
- Quem decide em que modo cada regra vale é a montagem. Cada `MessageRule` traz um `RuleScope` (`ALWAYS`,
  `TEST_MODE_ONLY`, `OUTSIDE_TEST_MODE_ONLY`; `messaging/RuleScope.java:4-13`), e `MessagePipeline.build` empilha só
  as regras do modo atual (`messaging/MessagePipeline.java:9-18`). A ordem dos embrulhos é a da lista (bloqueio por
  fora, depois a cópia, depois o desvio).
- As variações anunciadas pelo dono cabem sem mudar nenhum embrulho. Uma regra nova (madrugada, limite por dia) é uma
  classe a mais e uma linha em `Main.java:33-37`, com o escopo em que vale. A instalação sem cópia é uma variável de
  ambiente (`Main.java:18` e `Main.java:34`).
- O serviço não sabe de nada disso: só chama `sender.send(...)` (`ReservationService.java:37`).

**O exagero.** Não há estrutura sem uso:

- `MessageLog` tem uma implementação de produção, mas o teste a substitui por uma lista em memória
  (`src/test/java/br/com/marealta/MessageRulesTest.java:12-18`).
- `MessageRule`, `RuleScope` e `MessagePipeline` são a montagem que o próprio pattern pede (regras que valem sempre,
  só no teste ou só fora dele).
- A classe abstrata `CsvReport` (`report/CsvReport.java:7-21`) tem duas subclasses e evita repetir a sequência
  cabeçalho, linhas e total. É pouca coisa e tem uso.

**O resto do código.**

- Os tipos certos:
  - `BigDecimal` com `HALF_EVEN` em cada passo (`domain/Money.java:15-17`, `Pricing.java:22` e `Pricing.java:27`,
    `RatePlan.java:15`);
  - `LocalDate` e `YearMonth` para datas e meses;
  - enums para a situação, o plano, o tipo do quarto, o canal e o status da mensagem.
- O estado mutável da reserva só muda pelos métodos de transição, que conferem a situação antes
  (`Reservation.java:47-75`).
- Defeitos pequenos e localizados:
  - A regra de "reserva paga" aparece duas vezes, em `ReservationStatus.isPaid` (`domain/ReservationStatus.java:7-9`)
    e na lista de situações dentro do SQL (`persistence/ReservationRepository.java:110`).
  - Um id não numérico vira "Reserva 0 não existe" (`Routes.java:167-173`).
  - O `catch (Exception)` geral só faz `printStackTrace` (`Routes.java:43-46`).

  Nenhum desses pede mudança antes de aceitar.

## Testes

**Números:** 11 testes rodaram, nenhum falhou nem foi pulado, e há 11 métodos escritos. A cobertura é de 441 de 487
linhas (90,6%) e de 82 de 109 ramos (75,2%). O pacote `messaging` tem 44 de 44 linhas cobertas e o `domain` 128 de 129
(`measures/tests.md`). São três arquivos: um de unidade do domínio, um de unidade das regras de envio e um pela API
contra o PostgreSQL de verdade.
**Nota:** bom

**As regras pedidas, conferidas com valores:**

- a alta temporada com os 599,76 do enunciado e as bordas de 14/12, 15/12 e 29/02→01/03
  (`src/test/java/br/com/marealta/PricingAndReceiptTest.java:15-29`);
- o desconto do NON_REFUNDABLE com meio para o par, 116,97 sobre 779,80 (`PricingAndReceiptTest.java:31-37`);
- o texto inteiro do comprovante do enunciado e o formato R$ 1.234,56 (`PricingAndReceiptTest.java:39-52`);
- a devolução zero do NON_REFUNDABLE (`PricingAndReceiptTest.java:54-64`);
- pela API: o fluxo completo, as recusas com os códigos, os dois relatórios e a NON_REFUNDABLE cancelada contando no
  financeiro (`src/test/java/br/com/marealta/ApiIntegrationTest.java:68-134`).

**A parte nova, coberta com valores (`MessageRulesTest.java:26-63`):**

- o destinatário, o assunto "[Cópia] " e o texto da cópia;
- a instalação sem cópia;
- seis endereços inválidos, nos dois modos, sem cópia e com o endereço original;
- o desvio de teste, com o endereço, o "[TESTE] " e a linha exata "Destinatário original: ...", sem cópia.

**A pergunta da mutação.** Trocar o prefixo "[Cópia] " ou o endereço do arquivo, tirar o `if` da cópia em
`ArchiveCopy.java:14`, inverter o escopo da cópia ou relaxar a expressão do endereço (por exemplo, aceitar
"ana@gmail") faria algum teste falhar.

**O que fica sem rede:**

- a devolução integral do plano STANDARD (`RatePlan.java:9`) e a devolução zero de uma reserva pendente;
- o texto do e-mail de confirmação em volta do comprovante (`ReservationService.java:63-69`); o teste só confere o
  assunto (`ApiIntegrationTest.java:88-90`);
- as linhas "Desconto:" e "1 diária" do comprovante.

São lacunas pequenas perto do que está coberto.

## Banco

**Números:** 2 tabelas (`messages` e `reservations`), as duas com chave primária e todo o dinheiro em
`numeric(12,2)`. Nenhuma data fica guardada como texto. Não há nenhuma restrição (CHECK, UNIQUE ou EXCLUDE), e as
colunas `status`, `rate_plan` e `channel` são `text` sem CHECK. Há 2 transações explícitas e 1 `FOR UPDATE`
(`measures/database.md`). O esquema despejado (`measures/database-schema.sql`) não tem nenhum índice além das chaves
primárias.
**Nota:** com problemas

**O que está certo:**

- Os tipos estão certos, com `NOT NULL` em tudo o que é obrigatório; só `refund` aceita nulo, de propósito
  (`persistence/Database.java:27-54`).
- Todo SQL usa parâmetros. Os quatro candidatos de concatenação que o script listou só juntam constantes, a lista
  `COLUMNS` e cláusulas fixas (`ReservationRepository.java:14-15`, `:64`, `:81`, `:110-111`, `:121`).
- As conexões são devolvidas com try-with-resources.
- A duplicação de reserva é protegida no banco, não no processo. `pg_advisory_xact_lock(room)`, a checagem de
  sobreposição e o `INSERT` rodam numa transação só (`ReservationRepository.java:22-59`), e isso vale também entre
  processos: as 10 rodadas simultâneas do roteiro passaram.
- As mudanças de situação travam a linha com `FOR UPDATE` dentro de uma transação (`ReservationRepository.java:75-106`).

**Os defeitos:**

- O esquema nasce de `CREATE TABLE IF NOT EXISTS` no código, sem migração versionada (`Database.java:27-54`). O
  próximo pedido que acrescentar uma coluna não tem como evoluir um banco que já está rodando.
- Nenhuma restrição protege as regras no banco:
  - nenhum CHECK em `status`, `rate_plan` ou `guests`;
  - nenhum `check_out > check_in`;
  - nenhuma exclusão de sobreposição; a regra depende só da trava no código.
- Nenhum índice em `room_number` e `check_in`, que são as colunas da checagem de sobreposição e dos dois relatórios
  (`ReservationRepository.java:31-32`, `:110`, `:116`).
- Não há pool. O script contou "1 arquivo" com pool, mas o que existe é um `PGSimpleDataSource`
  (`Database.java:14`), que abre uma conexão física nova a cada chamada. Uma consulta, por exemplo
  `insert` → `find`, abre duas.
- Os quartos não são tabela e a reserva não tem chave estrangeira. O catálogo fixo no código
  (`domain/RoomCatalog.java:7-12`) é aceitável porque "os quartos quase nunca mudam".

## Comportamento

**Números:** 65 de 65 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário no modo normal | 9 de 9 |
| o cenário no modo de teste | 5 de 5 |
| as duas reservas simultâneas | 2 de 2 |

**Nota:** bom

Nada falhou. Para o dono da pousada, isso quer dizer:

- Os preços batem com os do texto (599,76, e o desconto de 37,48 com meio para o par).
- As recusas vêm com os códigos combinados.
- O e-mail de confirmação traz o comprovante.
- A devolução do cancelamento segue o plano.
- Os dois relatórios saem como pedido.
- Os dados sobrevivem a um reinício.
- Duas reservas simultâneas do mesmo quarto nunca entram juntas.
- As três regras de e-mail funcionam:
  - no modo normal, o endereço inválido fica BLOCKED, sem cópia e com o endereço original, e a cópia "[Cópia] " vem
    logo depois da original;
  - com `HOTEL_TEST_MODE=true`, tudo vai para a caixa de testes com "[TESTE] " e a linha do destinatário original,
    sem cópia, e o inválido continua bloqueado.

## Dúvidas

- A instalação sem cópia para o arquivo é ligada por uma variável que o texto não pediu, `HOTEL_ARCHIVE_COPY=false`
  (`Main.java:18`). É uma decisão razoável num ponto que o texto deixou em aberto, mas nenhum roteiro a confere pela
  API; só o teste de unidade `MessageRulesTest.java:36-39` a exercita.
- Se a gravação da mensagem falhar depois do `commit` da confirmação (`ReservationService.java:36-37`), a reserva
  fica CONFIRMED e o cliente recebe 500. O texto não diz o que fazer nesse caso, e o roteiro não o provoca.
