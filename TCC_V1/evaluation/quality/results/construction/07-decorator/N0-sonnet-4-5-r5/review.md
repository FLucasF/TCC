# Revisão: construção, cenário 07

## Visão geral

O projeto separa as coisas em camadas: `controller` (os handlers do Javalin), `service` (as regras), `repository` (o
JDBC à mão), `domain`, `dto`, `config` (a criação do esquema e as variáveis de ambiente) e `messaging` com
`messaging.rules` (as regras de envio). Tudo é criado e ligado em `App.main` (`App.java:24-86`). Uma requisição entra
pelo handler (`ReservationController.java:26-87`), que valida a entrada e chama o `ReservationService`. O serviço
calcula o preço com o `PricingService`, grava pelo `ReservationRepository` e, na confirmação, manda o e-mail pelo
`MessageService`, que passa a mensagem por uma lista de regras (`MessagePipeline`) e grava o que sai na tabela
`messages`. Os dados ficam no PostgreSQL, em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código
(`config/Database.java:23-86`). A parte do cenário (bloqueio, cópia para o arquivo, modo de teste) funciona por
inteiro: 14 de 14 conferências. O problema mais sério está no banco. Nada impede duas reservas do mesmo quarto na
mesma noite quando os pedidos chegam juntos: a disponibilidade é conferida e a reserva gravada em passos separados,
sem trava, sem transação e sem restrição (`ReservationService.java:39-54`), e o roteiro criou as duas reservas nas
10 de 10 rodadas. Os testes cobrem só as regras de envio e o preço por noite (12,8% das linhas).

## Arquitetura

**Números:** 9 pacotes, 18 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em
`controller` e no pacote raiz (`App`); JDBC só em `repository` e `config`; `service`, `domain` e `messaging` não
usam nenhuma biblioteca de borda. As maiores classes são `ReservationRepository` (252 linhas),
`ReservationService` (233) e `Reservation` (178).
**Nota:** bom

- **A borda HTTP é fina.** Os handlers leem a requisição, chamam o serviço e traduzem as exceções em código HTTP
  (`controller/ReservationController.java:89-99`, `controller/ReportController.java:18-32`). A validação de formato
  (campos que faltam, datas, saída depois da entrada, pelo menos um hóspede) fica no handler
  (`ReservationController.java:31-65`). É uma escolha aceitável para a borda, mas com ela o
  `ReservationService.createReservation` aceita uma reserva com `guests = 0` se for chamado por outro caminho.
- **As regras não conhecem o Javalin nem o SQL.** O preço, a situação da reserva e a devolução estão no serviço e no
  domínio (`service/ReservationService.java:57-84`, `151-168`; `domain/Reservation.java:157-176`). As regras de envio
  dependem só de `domain` (`messaging/rules/*`).
- **O acesso ao banco está isolado** nos três repositórios. A montagem fica num lugar só (`App.java:33-62`), com tudo
  passado pelo construtor. O único `static` (`config/Environment.java`) só lê variáveis de ambiente.
- **Um ponto localizado:** o handler do check-out e o da segunda via fazem um trabalho que é do serviço. Chamam o
  serviço, jogam fora o texto que ele devolve e buscam de novo a reserva e o quarto para montar a resposta
  (`ReservationController.java:146-149`, `162-165`). Pesa no design mais do que na arquitetura.

## Design de baixo nível

**Números:** as três regras de envio pedidas (bloqueio, cópia, modo de teste) ficam todas na estrutura, cada uma
numa classe que implementa `MessageRule` (`messaging/rules/ValidateEmailRule.java`, `ArchiveCopyRule.java`,
`TestModeRedirectRule.java`). Nenhuma fica num `if` fora dela. A combinação por modo é decidida em
`App.java:42-47`.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Não é um Decorator: as regras não embrulham o envio. É uma lista de regras que o `MessagePipeline`
aplica uma depois da outra. Cada regra recebe uma mensagem e devolve uma lista, o que permite bloquear, mudar ou
acrescentar uma cópia (`messaging/MessagePipeline.java:24-37`, `messaging/MessageRule.java:18`). Depois o
`MessageService` grava tudo o que sai (`service/MessageService.java:22-32`). A referência aceita isso como
alternativa equivalente ("uma lista de filtros aplicada pelo envio, desde que cada regra seja uma peça própria e a
combinação por modo seja decidida na montagem"), e as duas condições se cumprem:

- cada regra é uma peça própria;
- o envio (`MessageService`) não pergunta pelo modo;
- quem decide quando cada regra vale é a montagem: `new TestModeRedirectRule(testMode)` e
  `new ArchiveCopyRule(!testMode)` (`App.java:44-45`).

Duas coisas afastam do ideal:

- **A combinação por bandeira, não por presença.** As regras são sempre postas na lista e recebem um booleano que as
  desliga por dentro (`ArchiveCopyRule.java:31-33`, `TestModeRedirectRule.java:31-33`). Bastava não pôr a regra na
  lista. Com o jeito atual, cada regra nova ("de madrugada", "limite por dia") vai pedir mais uma bandeira.
- **O bloqueio não para a mensagem.** A mensagem bloqueada continua passando pelas regras seguintes, e cada uma
  precisa lembrar de deixá-la passar (`TestModeRedirectRule.java:36-38`, `ArchiveCopyRule.java:36-38`). A próxima regra
  que esquecer essa conferência vai copiar ou desviar uma mensagem bloqueada. A ordem da lista também importa (a
  validação tem de vir primeiro), e só a montagem sabe disso.

**O exagero:** contenção. A interface `MessageRule` tem três implementações e é o que a variação pede. Não há fábrica,
builder nem interface de uma implementação só. Há duas sobras pequenas, que não chegam a ser estrutura:

- o `RoomService` só repassa ao repositório (`service/RoomService.java:15-21`);
- o parâmetro `excludeId` de `findConflictingReservations` nunca é usado: a única chamada passa `null`
  (`repository/ReservationRepository.java:124`, `134`; `ReservationService.java:44`).

**O resto do código:**

- **O mesmo bloco repetido.** Os seis handlers repetem os mesmos `catch` de `NumberFormatException`,
  `ReservationNotFoundException` e `OperationNotAllowedException` (`ReservationController.java:94-172`), quando um
  tratador de exceções do Javalin resolveria uma vez só.
- **Uma chamada que ninguém lê.** O `checkOut` e o `getDocument` do serviço montam o texto do comprovante, e o
  handler o descarta (`ReservationController.java:146`, `162`). No `getDocument` a chamada serve só para conferir a
  situação.
- **Exceção sem tratamento.** O `bodyAsClass` não é tratado (`ReservationController.java:28`): um corpo que não é JSON
  dá 500. Conferi com `curl`, e o log mostra `JsonParseException` sem tratamento. O mesmo vale para `"guests": "x"`.
- **Dinheiro com escala errada.** `BigDecimal.ZERO` tem escala 0 (`domain/Reservation.java:26`,
  `ReservationService.java:73`), então a reserva recém-criada sai com `"amountPaid":0` e `"discount":0`, sem as duas
  casas.
- **Formatação que depende da máquina.** O `formatMoney` usa o `Locale` padrão e depois troca `,` por `.` à mão
  (`ReservationService.java:223-225`). Numa máquina em pt-BR, R$ 1.234,56 sai como "R$ 1,234.56". O certo é fixar o
  `Locale`.
- **Os tipos de dinheiro e de data estão certos:** `BigDecimal` com `HALF_EVEN` (`PricingService.java:50-52`),
  `LocalDate` e `enum` para a situação e o plano. O `Reservation`, porém, é um objeto com setters para tudo, e a mudança
  de situação é feita de fora por `setStatus` (`ReservationService.java:101`, `134`, `145`, `158`). As conferências
  `canBe*` ficam no domínio, mas nada impede quem chama de pular a conferência.

## Testes

**Números:** 17 testes rodaram, 0 falhas, 0 erros, 0 pulados, e há 17 métodos escritos em 2 arquivos. Cobertura:
linhas 107 de 833 (12,8%), ramos 42 de 168 (25,0%). `messaging.rules` 51 de 53 linhas; `service` 16 de 188;
`controller`, `repository`, `dto` e `config` 0. Nenhum teste chama a API ou usa o banco (`measures/tests.md`).
**Nota:** com problemas

- **A parte do cenário está bem testada, com valores:**
  - endereços inválidos `ana.gmail.com`, `ana@@gmail.com`, `@gmail.com`, `ana@` e `ana@gmail`
    (`MessageRulesTest.java:31-53`);
  - o desvio de teste com o destinatário, o "[TESTE] " e a primeira linha (`MessageRulesTest.java:69-79`);
  - a cópia com o endereço, o "[Cópia] " e o mesmo texto (`MessageRulesTest.java:95-111`);
  - mensagem bloqueada sem cópia e sem desvio (`MessageRulesTest.java:82-92`, `125-134`);
  - a lista inteira nos dois modos (`MessageRulesTest.java:137-188`).

  Se alguém tirasse a conferência de `BLOCKED` da cópia, ou trocasse o prefixo, um teste falharia.
- **O preço por noite também tem teste com valor:** 249,90 vira 299,88 na alta temporada (`PricingServiceTest.java:229-238`),
  com as bordas da temporada (`:211-219`) e o meio para o par (`:252-259`).
- **A montagem real não é testada.** Os testes criam a própria lista de regras (`MessageRulesTest.java:138-141`). Se
  `App.java:43-45` trocasse a ordem (a cópia antes da validação) ou passasse `testMode` à cópia, nenhum teste
  falharia. Também nada testa um ponto no começo ou no fim do domínio (`ValidateEmailRule.java:65-66`, as linhas não
  cobertas).
- **O resto das regras do enunciado fica sem rede:**
  - o desconto de 15% e a soma das noites (`ReservationService.java:57-84`): trocar `0.15` por `0.51` não derruba
    nenhum teste;
  - a devolução no cancelamento (`ReservationService.java:151-168`);
  - as transições de situação (`Reservation.java:157-176`);
  - a disponibilidade;
  - o texto do comprovante e do e-mail;
  - os dois relatórios em CSV;
  - os códigos de erro da API.

  Como o enunciado não pedia testes, conta a iniciativa: ela existe e é boa na parte nova, mas cobre pouco do sistema.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. O script acusou:

- a chave estrangeira `reservations(room_number) -> rooms` sem índice;
- 5 colunas de situação e tipo sem `CHECK`;
- nenhuma restrição que proteja uma regra (UNIQUE, EXCLUDE ou CHECK);
- o esquema por `CREATE TABLE` no código, sem migração;
- conexão por `DriverManager.getConnection`, sem pool;
- 0 transações e 0 `FOR UPDATE`.

Nenhum SQL é montado com `+` (`measures/database.md`).
**Nota:** ruim

- **O defeito sério: nada protege a regra "um quarto não pode ter duas reservas na mesma noite".** A criação consulta
  os conflitos (`repository/ReservationRepository.java:124-162`) e depois insere (`ReservationService.java:39-54`),
  cada passo numa conexão própria em autocommit, sem trava, sem `synchronized` e sem restrição `EXCLUDE` no banco. Na
  medida do comportamento, os dois pedidos simultâneos foram aceitos em 10 de 10 rodadas, e a pousada fica com dois
  hóspedes no mesmo quarto. É um risco concreto de dado errado.
- **O esquema não evolui.** O esquema é criado por `CREATE TABLE IF NOT EXISTS` (`config/Database.java:28-69`), que não
  sabe acrescentar uma coluna numa tabela que já existe. O próximo pedido que mudar o banco vai precisar de
  `ALTER TABLE` à mão.
- **Sem pool:** cada chamada abre uma conexão nova pelo `DriverManager` (`config/Database.java:19-21`). Uma consulta de
  ocupação abre 2 + N conexões, uma para cada linha, porque `ReportService.java:31` busca o quarto linha a linha.
- **Escritas sem transação.** A confirmação grava a reserva e depois as mensagens, uma a uma, sem transação
  (`ReservationService.java:101-106`, `MessageService.java:29-31`). Se uma falhar no meio, a reserva fica confirmada
  sem o e-mail ou sem a cópia.
- **O que está certo:**
  - o SQL usa sempre parâmetros `?`;
  - as conexões são devolvidas por try-with-resources;
  - os quartos são inseridos com `ON CONFLICT DO NOTHING` (`Database.java:72-81`);
  - os dados continuam lá depois de reiniciar (3 de 3 no roteiro).

## Comportamento

**Números:** 60 de 65 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 42 de 46 |
| depois de reiniciar | 3 de 3 |
| cenário, no modo normal | 9 de 9 |
| cenário, no modo de teste | 5 de 5 |
| reservas ao mesmo tempo | 1 de 2 |

**Nota:** com problemas

- **O cenário passa inteiro.** No modo normal:
  - a mensagem válida sai SENT;
  - a cópia vem logo depois, com "[Cópia] ", o mesmo texto e SENT;
  - os quatro endereços inválidos ficam BLOCKED, sem cópia e com o endereço original.

  Com `HOTEL_TEST_MODE=true`:
  - uma mensagem só, para testes@, com "[TESTE] " e a primeira linha "Destinatário original: …";
  - o endereço inválido continua BLOCKED.
- **Reservas ao mesmo tempo (falhou):** nas 10 rodadas, os dois pedidos simultâneos para o 301 nas mesmas noites
  foram criados (201 e 201). Para o dono, isso quer dizer quarto vendido duas vezes quando o site recebe dois pedidos
  juntos. O defeito pesa na coluna do banco; aqui só entra como o que se vê pela API.
- **O corpo que não é JSON (falhou):** responde 500 em vez de 400 INVALID_REQUEST (`ReservationController.java:28`,
  sem `catch`). Conferi também com `curl`.
- **O dinheiro com 2 casas (falhou):** a reserva recém-criada traz `"amountPaid":0` e `"discount":0`, sem as duas
  casas que o texto pede (`domain/Reservation.java:26`, `ReservationService.java:73`). Depois de lida do banco, a
  reserva já vem com 0.00. É um erro pequeno, mas o texto era explícito.
- **"O 201 comporta 3" e "a diária do 201 é 389.90" (falharam):** o `GET /rooms` devolve `"number":"101"` como texto
  (confirmado com `curl`), e o roteiro procura o número. Capacidade e diária estão certas (3 e 389.90). O texto não diz
  o tipo do `number`, então é uma escolha razoável num ponto em aberto e não pesa.
- **O resto da parte comum passa:**
  - os preços com a temporada e o meio para o par (599,76; desconto 37,48; total 212,42);
  - as recusas e os seus códigos;
  - a confirmação com o e-mail e o comprovante;
  - o check-in e o check-out;
  - as devoluções;
  - os dois relatórios.

## Dúvidas

- **A classe do pattern.** Hesitei entre `alternativa equivalente` e `malfeito`. As regras de cópia e de modo de teste
  recebem um booleano e se desligam por dentro (`ArchiveCopyRule.java:31-33`, `TestModeRedirectRule.java:31-33`).
  Mas quem decide o valor é a montagem (`App.java:44-45`), e nenhuma regra pergunta pelo modo de teste por conta
  própria. Por isso fiquei com a alternativa equivalente.
- **O exagero.** O `RoomService` só repassa ao repositório e poderia contar como "camada que só repassa". Julguei
  pequeno demais para marcar exagero.
- **O formato do dinheiro no comprovante e no e-mail.** Saiu certo nesta máquina, mas depende do `Locale` padrão da
  JVM (`ReservationService.java:224`). Não testei com o servidor em pt-BR.
