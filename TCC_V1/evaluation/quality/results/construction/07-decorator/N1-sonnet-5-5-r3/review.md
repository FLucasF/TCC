# Revisão: construção, cenário 07

## Visão geral

O projeto tem cinco pacotes: `web` (Javalin, as rotas e os DTOs de resposta), `domain` (reserva, preço, temporada, planos, serviços de reserva e de relatórios), `mail` (o envio e as regras de e-mail), `persistence` (o esquema e os repositórios JDBC com HikariCP) e a raiz com `Main`, que monta tudo. Uma requisição entra em `web/Api.java:45-56`, que só converte a entrada (`NewReservationParser`), chama `ReservationService`/`ReportService` e serializa a resposta. As regras de estado ficam em `Reservation` (`domain/Reservation.java:48-75`), e os dados ficam no PostgreSQL, em duas tabelas (`reservations` e `messages`) que o próprio código cria com `CREATE TABLE IF NOT EXISTS` (`persistence/Database.java:45-72`). As regras de e-mail são uma lista de regras, cada uma numa classe, com escopo por modo (`mail/MailRules.java:13-18`), e o `MailService` passa a mensagem por elas (`mail/MailService.java:17-31`). O comportamento passou em todas as 65 conferências. O que há de mais sério é estrutural:
- os dois ciclos entre pacotes, porque as interfaces dos repositórios moram em `persistence`;
- o esquema sem migrações e sem restrições: a proteção contra a reserva dupla vale só dentro do processo (`synchronized`);
- a falta de transação entre as escritas que deveriam andar juntas.

## Arquitetura

**Números:** 5 pacotes, 11 dependências entre eles e 2 ciclos: `domain` ↔ `persistence` e `mail` ↔ `persistence` (`measures/dependencies.md`). Só `web` e a raiz usam o Javalin. Só `persistence` usa o JDBC, além do `DataSource` em `Main`. A maior classe é `JdbcReservationRepository`, com 128 linhas.
**Nota:** com problemas

A borda HTTP é fina: cada rota em `web/Api.java:45-56` lê, chama o serviço e responde. Os erros viram JSON num lugar só (`web/Api.java:38-43`), a partir do código HTTP que fica no `ErrorCode` (`domain/ErrorCode.java:3-9`). As regras não conhecem o Javalin nem o SQL:
- as transições ficam em `domain/Reservation.java:48-75`;
- o preço fica em `domain/Quote.java:9-24`;
- a disponibilidade e a capacidade ficam em `domain/ReservationService.java:17-37`.

O SQL fica isolado nos repositórios JDBC, e a montagem acontece num lugar só (`Main.java:17-28`), sem `new` de acesso a banco dentro dos serviços.

O defeito são os ciclos. As interfaces `ReservationRepository` e `MessageRepository` ficam no pacote `persistence` (`persistence/ReservationRepository.java:3`, `persistence/MessageRepository.java:3`) e dependem dos tipos de `domain` e de `mail`. Ao mesmo tempo, `domain` e `mail` dependem delas (`domain/ReservationService.java:4`, `domain/ReportService.java:3`, `mail/MailService.java:3`). Com as interfaces ao lado de quem as usa, os dois ciclos sumiriam. Um ponto menor: o formato CSV dos relatórios é montado dentro de `domain` (`domain/ReportService.java:28-29,41-42`, `domain/Csv.java`). Isso é apresentação dentro da camada de regras, mas o tamanho é pequeno. Os ciclos são pequenos e entre vizinhos, por isso a nota fica em `com problemas`, e não em `ruim`.

## Design de baixo nível

**Números:** as 3 regras pedidas (endereço inválido, cópia para o arquivo, desvio no modo de teste) estão, todas, na estrutura: `ValidAddressRule`, `ArchiveCopyRule` e `TestRedirectRule`, que implementam `MailRule`. Nenhum caso ficou fora dela. Nenhum `if` de modo de teste aparece no envio nem nas regras.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

Não é um Decorator: as regras não embrulham um envio com a mesma interface. É uma lista de filtros aplicada pelo envio, e a referência aceita isso como equivalente:
- cada regra é uma peça própria com `List<Message> apply(Message)` (`mail/MailRule.java:9-11`);
- o `MailService` passa a mensagem por todas, em ordem, e as mensagens bloqueadas não seguem adiante (`mail/MailService.java:17-31`);
- a combinação por modo é declarada fora das regras, com um `Scope` por regra (`mail/MailRules.java:13-18`, `mail/Scope.java:6-12`), e o `MailService` só filtra a lista pelo modo na construção (`mail/MailService.java:14`);
- nenhuma regra pergunta se está em modo de teste.

As regras anunciadas (madrugada, limite por dia) entram como uma classe nova e uma linha com o escopo, e a segunda pousada monta outra lista. O que afasta do acerto: a lista e os escopos ficam num método estático do pacote `mail` (`MailRules.marealta()`), e não no `Main`, embora seja o `Main` quem escolhe a lista (`Main.java:26`). Também não há uma interface de envio que se possa embrulhar.

Não há exagero. As duas interfaces de repositório têm uma segunda implementação de verdade, em memória, nos testes (`src/test/.../InMemory.java:15,46`). `ScopedRule` e `Scope` respondem ao texto ("pode valer sempre, só no modo de teste ou só fora dele").

O resto do código está limpo:
- dinheiro em `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:16-18`, `domain/Quote.java:16-23`, `domain/RatePlan.java:17-19`);
- `LocalDate`/`YearMonth` para datas;
- enums para o quarto, o plano, a situação, o canal e o status;
- as transições centralizadas num `require` (`domain/Reservation.java:70-75`);
- a devolução polimórfica no `RatePlan` (`domain/RatePlan.java:21-23`).

Detalhes menores:
- a `Reservation` nasce com id 0 e recebe o id depois por `assignId` (`domain/Reservation.java:43,86-88`);
- a cópia do arquivo reaproveita um método chamado `redirectedTo` (`mail/ArchiveCopyRule.java:15`), um nome que não diz o que acontece ali;
- o relatório de ocupação filtra em Java a mesma sobreposição que a consulta já fez (`domain/ReportService.java:21-22`).

Nada disso pede mudança antes de aceitar.

## Testes

**Números:** 10 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 10 métodos escritos, em 3 classes: 5 pela API, 4 de unidade e 1 de repositório em H2. A cobertura é de 89,1% das linhas (417/468) e 78,2% dos ramos (79/101). O `domain` tem 100% das linhas cobertas, e o `mail` tem 53/54 linhas e 23/27 ramos (`measures/tests.md`).
**Nota:** com problemas

Os testes conferem valores de verdade:
- o preço da alta temporada é 599,76 (`PricingTest.java:15-19`);
- o meio para o par vale no desconto (86,985 vira 86,98, em `PricingTest.java:22-27`);
- os limites da temporada são conferidos (`PricingTest.java:30-35`);
- o texto inteiro do e-mail de confirmação é comparado (`ApiTest.java:74-87`);
- os dois CSVs são comparados linha a linha (`ApiTest.java:98-102,146-147`);
- o cancelamento NON_REFUNDABLE devolve 0,00 e continua no financeiro (`ApiTest.java:141-147`);
- os códigos de erro são conferidos (`ApiTest.java:151-164`).

Da parte nova, estão testados:
- a mensagem bloqueada, sem cópia e com o endereço original (`ApiTest.java:106-114`);
- a cópia logo depois da original, com o assunto e o destinatário (`ApiTest.java:69-73`);
- o modo de teste: o desvio, o "[TESTE] ", a primeira linha e o bloqueio com o endereço original (`ApiTest.java:117-132`).

Ficam sem rede:
- **A validação do endereço.** Só "ana@gmail" e "ana.gmail.com" são testados. Se alguém apagasse `at <= 0` (o endereço "@gmail.com"), o `lastIndexOf` (o "a@@b.com") ou o recorte do domínio que exige o ponto "no meio" (`mail/ValidAddressRule.java:17,21`), nenhum teste falharia.
- **O texto da cópia.** Nenhum teste confere que ela leva o mesmo texto da original.
- **O reembolso STANDARD.** Nenhum teste confere que ele devolve o que foi pago: trocar `amountPaid` por `ZERO` em `domain/RatePlan.java:22` passaria.
- **As linhas variáveis do comprovante.** Nenhum teste cobre a linha "Desconto:" nem o "1 diária" (`domain/ReservationDocument.java:26-30`).

Os testes de API usam repositórios em memória cujo `update` não faz nada (`InMemory.java:24-25`), e o teste JDBC roda em H2, não em PostgreSQL (`JdbcRepositoryTest.java:17`). Os testes passam e cobrem bem o código, mas parte das regras novas fica sem conferir.

## Banco

**Números:** 2 tabelas (`reservations` e `messages`), ambas com chave primária. O dinheiro está todo em `numeric(12,2)` e as datas em `date`. Não há chave estrangeira, `CHECK`, `UNIQUE` nem `EXCLUDE`, e as 4 colunas de situação, plano e canal não têm `CHECK`. O esquema vem de `CREATE TABLE` no código Java, sem migração, com 0 transações explícitas e 0 `FOR UPDATE` (`measures/database.md`).
**Nota:** com problemas

O que está certo:
- os tipos (`persistence/Database.java:46-71`);
- o SQL sempre com `?`: os dois candidatos do script juntam só constantes, `COLUMNS` e os `where` fixos (`persistence/JdbcReservationRepository.java:24-26,76,85,93,105`);
- o pool HikariCP;
- try-with-resources em toda conexão.

Os defeitos:
- **O esquema não evolui.** É `CREATE TABLE IF NOT EXISTS` em `persistence/Database.java:45-72`, que não sabe acrescentar uma coluna numa tabela que já existe.
- **A reserva dupla é barrada só pelo `synchronized`** de `domain/ReservationService.java:17`, que vale para um processo só. O banco não tem restrição de exclusão nem trava, e nem um índice em `room_number`/`check_in`.
- **Não há tabela de quartos**, então o `room_number` não é chave estrangeira.
- **Não há transação onde duas escritas deveriam andar juntas.** A confirmação grava a reserva e depois as mensagens, cada uma na sua conexão (`domain/ReservationService.java:47-48`, `mail/MailService.java:30`). Se a gravação da mensagem falhar, a reserva fica confirmada sem o e-mail registrado, ou a original fica sem a cópia.

Nada perde dinheiro nem dado na operação normal, e os dados sobrevivem ao reinício.

## Comportamento

**Números:** 65 de 65 conferências passaram, com 0 falhas:
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 07, modo normal: 9/9;
- cenário 07, modo de teste: 5/5;
- reservas simultâneas: 2/2 (`measures/behavior.md`).

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto descreve:
- os preços, inclusive com a alta temporada e o meio para o par;
- as situações da reserva e as devoluções;
- o e-mail com o comprovante;
- os dois relatórios em CSV;
- os códigos de erro.

Nas regras novas:
- "ana@gmail", "ana.gmail.com", "ana@@gmail.com" e "@gmail.com" ficam bloqueados, sem cópia;
- a cópia para o arquivo vem logo depois da original, com "[Cópia] ";
- com `HOTEL_TEST_MODE=true`, tudo vai para a caixa de testes, com "[TESTE] " e a linha do destinatário original, sem cópia, e o inválido continua bloqueado com o endereço original.

As duas reservas simultâneas passam porque há uma instância só, servida pelo `synchronized`. Com duas instâncias, isso não valeria; o defeito está contado na coluna do banco.

## Dúvidas

Na classe do pattern, fiquei entre `alternativa equivalente` e `acerto`. A estrutura é uma lista de filtros com escopo por modo, e não embrulhos de um envio; a referência aceita isso como equivalente. A combinação está em `MailRules.marealta()`, chamada pelo `Main`, e não escrita no próprio `Main`. Considerei isso montagem, e não decisão tomada dentro das regras.
