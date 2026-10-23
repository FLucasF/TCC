# Revisão: construção, cenário 07

## Visão geral

O projeto é um serviço Javalin em camadas, com 7 pacotes. `Main` liga as peças à mão e registra as rotas
(`Main.java:30-65`). Os `controller`s leem a requisição e chamam os `service`s, que aplicam as regras e usam os
`repository`s, e estes falam JDBC com o PostgreSQL por um pool Hikari guardado num `static` (`DatabaseManager.java:12`).
As reservas e as mensagens ficam em duas tabelas, criadas com `CREATE TABLE IF NOT EXISTS` no código. Os quartos não
estão no banco: ficam numa lista fixa em `repository/RoomRepository.java:11-17`. As regras de envio do cenário (bloqueio,
cópia, modo de teste) são peças `MessageRule` aplicadas em sequência por um `MessageRulePipeline`, e quem escolhe as
peças conforme o modo é `MessageRulePipelineFactory`. Quatro problemas pesam mais:
- **Reservas duplicadas:** nada impede duas reservas do mesmo quarto feitas ao mesmo tempo. No roteiro, das 10
  rodadas, as 10 criaram as duas reservas.
- **Erros 500:** a segunda via de reserva pendente ou cancelada dá erro 500, quando devia dar 409.
- **Número do quarto:** a API só aceita `roomNumber` como texto. Por isso o roteiro oficial, que manda o número como
  número, passa em só 15 de 65 conferências. Com o número em texto, passa em 62 de 65.
- **Testes fracos:** os testes não conferem cancelamento, devolução, relatórios nem o comprovante.

## Arquitetura

**Números:** 7 pacotes, 13 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Só `controller` e
`Main` usam Javalin. Só `database` e `repository` usam JDBC. A maior classe é `ReservationController`, com 243 linhas.
**Nota:** com problemas

As dependências seguem o sentido certo: `controller` → `service` → `repository` → `database`, e todos usam `model`.
Os serviços não conhecem o `Context` do Javalin nem o SQL, e todo o SQL está nos três repositórios. Os problemas são
pontuais:
- **Regra de preço no handler:** o mapeamento da resposta, no controller, calcula de novo subtotal, desconto e total
  (`controller/ReservationController.java:201-205`). Por isso o controller depende do `RoomRepository`
  (`controller/ReservationController.java:19-23`) e pula a camada de serviço.
- **Montagem só em parte no `main`:** os repositórios não recebem a conexão. Eles chamam o `static`
  `DatabaseManager.getConnection()` (`repository/ReservationRepository.java:33`, `:73`, `:105`…). Além disso, o
  serviço monta a sequência de regras por dentro (`service/MessageService.java:16`) em vez de recebê-la do `Main`.
- **Controller grande e repetitivo:** `ReservationController` repete o mesmo bloco `try/catch` com o mesmo JSON de
  erro em cada handler (`controller/ReservationController.java:79-98`, `:105-130`, `:139-184`). Não existe um
  tratador de exceções central do Javalin.

## Design de baixo nível

**Números:** as três regras do texto (endereço inválido, cópia para o arquivo, modo de teste) são três classes
`MessageRule` próprias, e nenhuma ficou fora da estrutura. A combinação por modo é um único `if` na fábrica
(`messaging/MessageRulePipelineFactory.java:13-17`). Nenhuma regra pergunta pelo modo.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O agente não fez embrulhos (Decorator). Fez uma lista de filtros que o envio aplica, e a referência
aceita isso:
- cada regra é uma peça: `EmailValidationRule`, `ArchiveCopyRule` e `TestModeRule`, todas com a interface
  `MessageRule.apply(context)`;
- o `MessageRulePipeline` aplica as regras em ordem (`messaging/MessageRulePipeline.java:15-25`);
- o envio (`service/MessageService.java:19-25`) não pergunta nada, só grava o que a sequência devolve;
- a escolha de quais regras valem em cada modo fica fora das regras (`messaging/MessageRulePipelineFactory.java:10-19`).

Para incluir a regra da madrugada ou o limite diário, basta uma classe nova e uma linha na fábrica.

O que afasta do acerto pleno:
- **A combinação não está no `Main`:** a fábrica é chamada de dentro do `MessageService` (`service/MessageService.java:16`),
  que recebe só um `boolean`. Uma instalação sem a cópia para o arquivo (a segunda pousada do texto) exige mudar o
  código da fábrica, porque não dá para montar a combinação de fora.
- **As regras dependem de estado compartilhado e da ordem:** elas se comunicam por um contexto mutável. A validação
  tem de vir primeiro, porque marca `blocked` e muda o status da mensagem (`messaging/EmailValidationRule.java:11-14`).
  A cópia e o desvio consultam esse `blocked` (`messaging/ArchiveCopyRule.java:9`, `messaging/TestModeRule.java:9`).
  Isso é a regra "bloqueada não gera cópia", não uma consulta ao modo. Por isso não é malfeito.

**O exagero.** Não há estrutura sem necessidade fora do pattern. A fábrica faz parte da montagem do pattern. Sobram
só métodos que nada usa: `getOriginalMessage` e `setCurrentMessage` (`messaging/MessageRuleContext.java:25`, `:33`) e
`DatabaseManager.getDataSource` (`database/DatabaseManager.java:25`).

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` em cada passo
(`model/Reservation.java:126-150`), `LocalDate` e `enum`s, com o desconto no próprio `RatePlan`. Os defeitos estão
no fluxo de erros e na duplicação:
- **O mesmo cálculo em três lugares:** subtotal, desconto e total são calculados em
  `controller/ReservationController.java:202-205`, em `service/ReservationService.java:57-60` e em
  `service/ReservationService.java:124-127`.
- **Erros escondidos:** `catch (Exception e)` transforma qualquer falha em 400 com a mensagem interna
  (`controller/ReservationController.java:94-99`). Uma falha do banco vira "Failed to insert reservation", e um
  `roomNumber` numérico vira o texto da `ClassCastException`.
- **`null` como resposta de erro:** `getDocument` devolve `null` quando a reserva não está paga
  (`service/ReservationService.java:118-122`), e o controller serializa isso, o que dá 500.
- **Id que não é número:** `Long.parseLong` não é tratado em nenhum handler (`controller/ReservationController.java:104`,
  `:117`…), e `/reservations/abc` dá 500.
- **Exceções sem mensagem:** as exceções de domínio são classes aninhadas vazias
  (`service/ReservationService.java:212-216`).
- **Variável sem uso:** `reservation` não é usada em `controller/ReservationController.java:172`.
- **Validação do e-mail mais frouxa que o texto:** basta haver um ponto em qualquer lugar depois do @
  (`messaging/EmailValidationRule.java:39`), então `ana@gmail.` é aceito. Conferi à mão: a mensagem saiu como `SENT`,
  com cópia. O texto pede "um ponto no meio".

## Testes

**Números:** 16 testes rodaram, 0 falharam e 0 foram pulados, de 16 métodos escritos. Cobertura de linhas de
58,4% (471 de 807) e de ramos de 46,0% (57 de 124). Por pacote: `messaging` 69 de 78 linhas, `model` 157 de 166,
`service` 75 de 156 e `controller` 59 de 194 (`measures/tests.md`).
**Nota:** com problemas

O que está testado, com valores:
- o preço com a temporada e o arredondamento: 599,76 em julho, 15/12 e janeiro, e o desconto de 74,97
  (`PricingTest.java:16-37`, `:41-53`);
- a capacidade, pela API: 422 com `CAPACITY_EXCEEDED` (`ReservationIT.java:175-200`);
- as três regras de envio, isoladas e pelas duas combinações da fábrica (`MessageRulesTest.java:15-105`).

O que ficou sem teste:
- o cancelamento e a devolução;
- as transições proibidas (409);
- o conflito de datas (409);
- os dois relatórios CSV;
- o comprovante e o texto do e-mail;
- a segunda via.

O fluxo pela API só confere `amountPaid > 0` (`ReservationIT.java:141`), sem conferir o valor.

A pergunta da mutação, na parte nova:
- se alguém apagar a exigência do ponto depois do @ (`messaging/EmailValidationRule.java:39`), nenhum teste falha. Os
  endereços inválidos dos testes são `invalid-email` e `invalid`, que não têm @ (`MessageRulesTest.java:17`, `:71`), e
  `ana@gmail` nunca é testado;
- se o prefixo `"[Cópia] "` perder o espaço, o `startsWith("[Cópia]")` continua passando (`MessageRulesTest.java:66`).
  O mesmo vale para `"[TESTE]"` (`:49`);
- se a cópia levar outro texto, nada confere (o corpo da cópia não é comparado);
- se uma mensagem bloqueada no modo de teste perder o endereço original, nenhum teste vê, porque esse caso não é
  testado.

Por fim, `ReservationIT` não usa o `Main`: refaz a montagem e as rotas à mão (`ReservationIT.java:49-80`), sem os
relatórios. Se a montagem real estiver errada, o teste não acusa.

## Banco

**Números:** 2 tabelas (`reservations` com 12 colunas e `messages` com 6), as duas com chave primária. O dinheiro
está em `numeric(10,2)` e as datas em `date`. Não há nenhuma chave estrangeira, nenhum CHECK e nenhum UNIQUE ou
EXCLUDE. Também não há transação explícita nem `FOR UPDATE`. O esquema é criado por `CREATE TABLE` no código, sem
migrações, e não há SQL montado com `+` (`measures/database.md`).
**Nota:** ruim

O que está certo: o SQL usa parâmetros, as conexões voltam ao pool por try-with-resources, e os dados resistem ao
reinício (3 de 3 na execução com o número do quarto em texto, `measures/behavior-roomtexto.md`). O que é sério:
- **Nenhuma proteção contra duas reservas na mesma noite:** o serviço consulta os conflitos e depois insere, em duas
  conexões separadas e sem transação (`service/ReservationService.java:32-42`,
  `repository/ReservationRepository.java:121-148`). O banco não tem uma restrição de exclusão sobre quarto e datas,
  nem uma trava. Medido: nas 10 rodadas de dois pedidos simultâneos, as 10 criaram as duas reservas do mesmo quarto
  nas mesmas noites (`measures/behavior-roomtexto.md`). É dado errado, e quebra a regra mais básica da pousada.
- **Valores calculados na hora, não gravados:** o subtotal, o desconto e o total não são gravados. Eles são
  recalculados a cada leitura a partir das diárias fixas no código (`controller/ReservationController.java:202-205`,
  `model/RoomType.java:6-8`). Se a diária mudar, o total e o comprovante das reservas antigas mudam junto. Só o
  `amount_paid` fica gravado.
- **Esquema sem evolução:** `CREATE TABLE IF NOT EXISTS` (`database/DatabaseManager.java:35`, `:52`) não sabe
  acrescentar uma coluna numa tabela que já existe.
- **Quarto sem chave estrangeira:** os quartos não estão no banco, e `room_number` é um texto livre.
- **Situação, plano e canal sem CHECK.**
- **Números pulados:** um INSERT que falha (por exemplo, sem `guestName`, recusado pelo NOT NULL) consome o número do
  `SERIAL`. Conferi à mão: a reserva seguinte recebeu o número 2.

## Comportamento

**Números:** o roteiro oficial passou em 15 de 65 conferências:
- parte comum: 12 de 46;
- reinício: 1 de 3;
- cenário no modo normal: 1 de 9;
- cenário no modo de teste: 0 de 5;
- concorrência: 1 de 2.

Quase todas as falhas vêm de uma só causa. O roteiro manda `"roomNumber": 101` como número, e o controller faz
`(String) body.get("roomNumber")` (`controller/ReservationController.java:31`), que dá `ClassCastException` e vira
400. O `GET /rooms` também devolve `number` como texto, e por isso o roteiro não acha o quarto 201.

Rodei uma cópia do mesmo roteiro com o número do quarto como texto (`measures/behavior-roomtexto.md`). Ela passou em
62 de 65 conferências:
- parte comum: 44 de 46;
- reinício: 3 de 3;
- cenário no modo normal: 9 de 9;
- cenário no modo de teste: 5 de 5;
- concorrência: 1 de 2.
**Nota:** com problemas

Na versão com o número em texto, o cenário funciona inteiro:
- o endereço válido sai como `SENT`, com a cópia `[Cópia] ` logo depois e o mesmo texto;
- `ana@gmail`, `ana.gmail.com`, `ana@@gmail.com` e `@gmail.com` ficam `BLOCKED`, com o endereço original e sem cópia;
- no modo de teste, a mensagem vai para `testes@…` com `[TESTE] ` e a linha "Destinatário original", sem a cópia, e
  o endereço inválido continua bloqueado.

O que falhou, e o que isso quer dizer para o dono:
- **A segunda via de uma reserva pendente dá erro 500, não 409:** a recepção vê uma falha do servidor em vez de "não
  é permitido". Conferi à mão que acontece o mesmo com uma reserva cancelada. A causa está em
  `service/ReservationService.java:121`.
- **O dinheiro zerado sai sem as 2 casas:** a reserva nova mostra `"amountPaid":0` e `"discount":0`, e o texto pede
  número com 2 casas. O cancelamento sem devolução sai como `"refund":0`. O mesmo vale para a linha `total,0` do
  financeiro de um mês sem receita (`service/ReportService.java:48`, `:75`).
- **Duas reservas simultâneas do mesmo quarto são as duas aceitas:** aconteceu nas 10 rodadas. O defeito é do banco e
  pesa naquela coluna, mas para o dono quer dizer quarto vendido duas vezes.

Achados fora do roteiro, conferidos à mão:
- `GET /reservations/abc` dá 500;
- `month=2027-13` devolve 200 com um relatório vazio, em vez de 400;
- `ana@gmail.` é aceito como endereço válido.

O número do quarto como texto é uma escolha que o texto não fechou: ele não diz o tipo. Por isso as falhas do roteiro
oficial que vêm só dela não pesam como erro. Mesmo assim, a API recusa com uma mensagem interna do Java um pedido do
site que mande o número como número.

## Dúvidas

- **O tipo do número do quarto:** o enunciado não diz se `roomNumber` e `number` são número ou texto. Tratei o texto
  como escolha aceitável e julguei o comportamento pela execução com o número em texto (62 de 65), mas registrei os
  15 de 65 do roteiro oficial. Se o combinado com o site for número, a parte comum do sistema não funciona pela API, e
  a nota de comportamento seria `ruim`.
- **A classe do pattern:** dei `alternativa equivalente`. A estrutura é uma lista de filtros com cada regra numa peça,
  e a combinação fica fora das regras. Mas essa combinação está numa fábrica fixa chamada de dentro do serviço, não no
  `main`, e não deixa outra instalação montar a sua sem mudar código. Um leitor mais rígido poderia ver nisso a
  combinação "fora da montagem".
- **A nota do banco:** dei `ruim` porque não há nenhuma proteção contra a reserva dupla e ela foi medida nas 10 de 10
  rodadas. A skill do banco põe a proteção "só dentro do processo" em `com problemas`; aqui não há nem isso.
