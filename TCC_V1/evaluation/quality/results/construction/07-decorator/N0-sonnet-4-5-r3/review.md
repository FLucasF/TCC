# Revisão: construção, cenário 07

## Visão geral

O projeto (`projeto/`) usa pacotes por camada: `controller` (os handlers do Javalin), `service` (as regras), `repository` (JDBC puro com HikariCP), `model`, `dto` e `mapper` (a tradução do modelo para o JSON), além de `messaging` e `messaging.rules` para a parte do cenário. Tudo é montado no `Main`. Uma requisição entra pelo `Main.java:55-65`, chega a um controller que valida e converte a entrada (`ReservationController.java:28-63`), chama o `ReservationService`, que calcula o preço com o `PricingService`, e grava pelo `ReservationRepository`. Os quartos são uma lista fixa no código (`RoomService.java:11`). As reservas e as mensagens ficam em duas tabelas que o `DatabaseInitializer` cria com `CREATE TABLE IF NOT EXISTS`. A parte do cenário ficou numa lista de regras (`MessageRule`) que o `MessagePipeline` percorre, e passou em todas as 14 conferências do roteiro. Os problemas mais sérios são estes:
- nada impede duas reservas do mesmo quarto feitas ao mesmo tempo: nas 10 rodadas do roteiro, as duas foram criadas;
- a segunda via sai para reserva pendente, porque a checagem que existe no serviço nunca é chamada;
- o dinheiro vai no JSON como `double` (`0.0` em vez de `0.00`);
- um corpo que não é JSON dá 500;
- o `mvn verify` só passa onde há Docker, e um dos testes de integração falharia mesmo com Docker.

## Arquitetura

**Números:** 11 pacotes, 21 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece em `controller` e no pacote raiz (`Main`); o JDBC só em `repository`, `database` e `Main`. A maior classe é `ReservationRepository` (234 linhas), seguida de `ReservationService` (203).
**Nota:** com problemas

As camadas existem e as dependências apontam na direção certa: `controller → service → repository → model`, sem ciclo. Os serviços não conhecem nem o `Context` nem o SQL, e todo o SQL está em `repository/`. A montagem fica quase toda no `Main.java:29-45`, com injeção pelo construtor. Há duas misturas, ambas localizadas:

- **O handler do comprovante decide sozinho.** O `ReservationController.getDocument` (`ReservationController.java:97-103`) e o `checkOut` (`:89-95`) vão direto ao `RoomService` para buscar o tipo do quarto e montam o comprovante pelo `ModelMapper`. Com isso, passam por cima da regra "segunda via só para reserva paga", que existe em `ReservationService.generateDocument` (`ReservationService.java:128-135`) e que ninguém chama (o `grep` só acha a declaração). É uma regra no lugar errado, e ela causa uma falha de comportamento (a segunda via de reserva pendente).
- **A combinação das regras de envio não está no `Main`.** O `MessageService` cria as próprias regras com `new` e decide a combinação por modo dentro de si (`MessageService.java:22-34`). O `Main` só passa um `boolean testMode` (`Main.java:37`). Uma instalação com outra combinação obriga a mexer no serviço.

Há ainda mistura de responsabilidades no `ReservationService`: além das regras da reserva, ele monta o texto do comprovante e do e-mail e formata dinheiro e CPF (`ReservationService.java:137-181`). Também repassa consultas para o `ReportService` sem acrescentar nada (`:196-202`).

## Design de baixo nível

**Números:** as 3 regras do cenário (bloqueio de endereço inválido, cópia para o arquivo, modo de teste) são todas peças próprias que implementam `MessageRule` (`messaging/rules/*.java`). Nenhuma regra pergunta pelo modo. Há um único `if (testMode)`, na montagem dentro do `MessageService` (`MessageService.java:27-31`). Nenhum caso ficou fora da estrutura.
**Classe:** alternativa equivalente
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** O texto pede o Decorator: embrulhos em volta do envio, empilhados conforme o modo. O agente fez uma lista de filtros aplicada pelo envio (`MessagePipeline.process`, `MessagePipeline.java:13-25`), e a referência aceita isso de antemão desde que cada regra seja uma peça própria e a combinação por modo fique fora das regras. As duas condições valem:
- `EmailValidationRule`, `ArchiveCopyRule` e `TestModeRule` não sabem nada de modo;
- a combinação é decidida em `MessageService.buildPipeline` (`MessageService.java:22-34`).

O que afasta do ideal:
- **A montagem não está na raiz.** Ela fica no construtor de um serviço, e não no `Main`; ver a Arquitetura.
- **A lista não acumula o resultado das regras.** Em `MessagePipeline.java:17`, cada regra substitui o resultado anterior (`result = rule.process(message)`). As mensagens extras de uma regra que não seja a última se perdem. Hoje funciona porque a `ArchiveCopyRule` é a última, mas uma regra nova posta depois dela (o limite por dia, por exemplo) apagaria a cópia sem aviso.
- **As regras mudam a mensagem recebida.** A `TestModeRule` altera o próprio `OutgoingMessage` (`TestModeRule.java:15-17`, com setters em `OutgoingMessage.java:22-48`), e a ordem entre as regras fica implícita.

**O exagero.** O `MessagePipeline.Builder` (`MessagePipeline.java:27-38`) é um builder que só faz `List.add`, e o construtor público com a lista já basta. Os repasses `ReservationService.findByCheckInMonth` e `findOccupyingRoom` (`ReservationService.java:196-202`) são uma camada que só repassa. Esse exagero é leve.

**O resto do código.**
- **Dinheiro em `double` em vários pontos.** A diária e o desconto são `double` (`RoomType.java:4-9`, `RatePlan.java:4-7`), convertidos com `BigDecimal.valueOf(double)` (`PricingService.java:34`, `:62`). Funciona com estes valores, mas é o tipo errado. Os DTOs voltam o dinheiro para `double` (`ModelMapper.java:34-39`, `:54-56`, `RoomDTO.java:7`), e por isso o JSON perde as 2 casas (`0.0`, `779.8`).
- **`formatMoney` depende do locale da máquina.** Ele faz `String.format("R$ %,.2f")` e troca vírgula e ponto (`ReservationService.java:179-181`). Numa JVM com locale pt_BR, sairia "R$ 1,234.56".
- **Código morto.** O `generateDocument` (`ReservationService.java:128-135`) não é usado, e a busca do tipo do quarto aparece duplicada em `ReservationController.java:92` e `:100`.
- **Erros.** O `catch (Exception)` global (`Main.java:91-95`) transforma o erro do Jackson num corpo inválido em 500. Não há tratamento próprio para o `bodyAsClass` (`ReservationController.java:29`).
- **Validação de e-mail mais frouxa que o texto.** Ela aceita "ana@gmail." e "ana@.com", porque só exige um ponto em qualquer lugar do domínio (`EmailValidationRule.java:39-41`), e o texto pede o ponto "no meio".

O resto é razoável: há enums para situação, plano, tipo e canal, `LocalDate` para datas, `BigDecimal` com `HALF_EVEN` a cada passo no `PricingService`, e métodos curtos.

## Testes

**Números:** 16 testes rodaram, com 0 falhas e 1 erro (`ReservationIT`, sem Docker), e há 25 métodos de teste escritos. A cobertura é de 11,4% das linhas (111 de 972) e 23,3% dos ramos (34 de 146). Ela é zero em `controller`, `repository`, `mapper` e `database`, e de 30 em 212 linhas em `service` (`measures/tests.md`).
**Nota:** com problemas

**O que é bom.** Os testes de unidade conferem regras com valores:
- a temporada nos limites (14/12 e 15/12, 28/02 e 01/03; `PricingServiceTest.java:23-30`);
- os R$ 299,88 da noite de alta temporada e os R$ 599,76 do exemplo (`:39-58`);
- o desconto de 116,97 e o total de 662,83 (`:60-80`);
- as três regras do cenário, cada uma isolada, com o endereço, o prefixo e o texto (`MessageRulesTest.java:14-71`);
- as duas combinações (`:73-106`).

**Os problemas:**
- **O teste de integração não roda sem Docker.** O `ReservationIT` usa Testcontainers e ignora o `DB_URL` (`ReservationIT.java:26-30`). Sem Docker, ele falha no `beforeAll`, e o `mvn verify` falharia, quando o texto pede que ele passe.
- **Mesmo com Docker, um teste falharia.** O `shouldConfirmReservationAndSendEmail` confere que a última mensagem vai para `carlos@example.com` (`ReservationIT.java:185-187`). Mas, fora do modo de teste, a última mensagem é a cópia para `arquivo@...`. Ou seja, o agente nunca viu esse teste passar.
- **Nada passa pela API.** Não há teste dos códigos de erro, do comprovante, da segunda via nem dos relatórios em CSV.
- **A pergunta da mutação deixa regras sem rede:**
  - Trocar `atCount != 1` por `atCount < 1` (`EmailValidationRule.java:31`) não derruba nenhum teste, porque não há caso com dois @.
  - Inverter a ordem `EmailValidationRule`/`TestModeRule` no `MessageService` não derruba nenhum teste. Os testes montam a própria lista e não testam o `MessageService`, e o bloqueio no modo de teste com o endereço original não é conferido em teste.
  - Apagar o `if (!result.isBlocked())` do `MessageService.send` (`MessageService.java:51`) só seria pego pelo teste de integração, que não roda.

## Banco

**Números:** 2 tabelas (`reservations`, `messages`), ambas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. Há 0 chaves estrangeiras, 0 CHECK e 0 UNIQUE/EXCLUDE. As colunas de situação, plano e canal estão sem CHECK. Não há nenhuma transação explícita (`setAutoCommit(false)`) nem `FOR UPDATE`. O esquema é criado por `CREATE TABLE` no código, sem migração (`measures/database.md`).
**Nota:** ruim

Os tipos estão certos, todo o SQL usa `?` (os repositórios), as conexões são devolvidas em try-with-resources e há um pool (`Main.java:98-105`). Mas a regra de dado mais importante, um quarto sem duas reservas na mesma noite, não tem nenhuma proteção:
- o `ReservationService` faz uma consulta de conflito (`ReservationService.java:188-193`, que usa `ReservationRepository.java:116-152`);
- depois faz um `INSERT` separado (`ReservationService.java:58`);
- não há trava, transação ou restrição no banco (uma `EXCLUDE` sobre o intervalo, por exemplo), nem um `synchronized`.

O roteiro do comportamento mediu o resultado: **nas 10 rodadas de dois pedidos simultâneos, as duas reservas foram criadas**. Isso é dado errado gravado, não um risco teórico.

Os outros pontos:
- **Sem migrações.** O esquema vem de `CREATE TABLE IF NOT EXISTS` (`DatabaseInitializer.java:14-44`), que não sabe acrescentar uma coluna numa tabela que já existe.
- **Reserva sem ligação a um quarto no banco.** Não há tabela de quartos, então `room_number` não tem chave estrangeira.
- **Confirmação e e-mail fora de uma transação.** A confirmação grava a reserva (`ReservationService.java:70`) e depois a mensagem (`:72`) sem transação. Se a segunda escrita falhar, a reserva fica confirmada sem o e-mail.
- **Consulta por mês sem índice.** O relatório financeiro filtra com `EXTRACT(...)` (`ReservationRepository.java:154-160`), o que impede o uso de índice. Pesa pouco.

## Comportamento

**Números:** 59 de 65 conferências passaram (`measures/behavior.md`):
- parte comum: 41 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 07, modo normal: 9 de 9;
- cenário 07, modo de teste: 5 de 5;
- reservas simultâneas: 1 de 2.

**Nota:** com problemas

A parte do cenário funciona inteira:
- o bloqueio de "ana@gmail", "ana.gmail.com", "ana@@gmail.com" e "@gmail.com", com o endereço original e sem cópia;
- a cópia logo depois da original, com "[Cópia] ";
- no modo de teste, o desvio para `testes@` com "[TESTE] " e "Destinatário original: ", sem cópia;
- o bloqueio também no modo de teste.

Os preços, a confirmação com o e-mail, os cancelamentos e as devoluções, os dois relatórios e a persistência depois de reiniciar também passam.

As falhas, e o que elas querem dizer para o dono da pousada:
- **Duas reservas simultâneas para o mesmo quarto e as mesmas noites são as duas aceitas (10 de 10).** Se dois hóspedes reservarem pelo site ao mesmo tempo, o quarto fica vendido duas vezes. Esse defeito pesa na nota do Banco.
- **A segunda via de uma reserva pendente responde 200 em vez de 409.** A recepção consegue tirar um comprovante de uma reserva que ninguém pagou (`ReservationController.java:97-103`).
- **Um corpo que não é JSON dá 500 INTERNAL_ERROR em vez de 400 INVALID_REQUEST.** O site recebe um "erro interno" quando quem errou foi ele.
- **O dinheiro no JSON não vem com 2 casas (`"amountPaid":0.0`, `"discount":0.0`).** O texto pede 599.76 com 2 casas, e o site pode mostrar "779.8".
- **"o 201 comporta 3" e "a diária do 201 é 389.90" falharam porque o `number` do quarto vem como texto ("201") e o roteiro procura o número 201.** O texto não diz o tipo do `number`, então essa escolha não pesa como erro. Mas a diária também iria como `double` (`RoomDTO.java:7`), e cairia no mesmo problema das 2 casas.

## Dúvidas

- **A classe do pattern.** Ficou entre `alternativa equivalente` e `malfeito`. A combinação por modo não está dentro das regras, mas também não está na raiz da montagem: fica num `if (testMode)` dentro do `MessageService`. E a lista descarta o resultado das regras anteriores (`MessagePipeline.java:17`), o que funciona hoje só por causa da ordem. Dei `alternativa equivalente` porque as três regras são peças próprias e nenhuma pergunta pelo modo.
- **A nota do Banco.** Ficou entre `com problemas` e `ruim`. Dei `ruim` porque a duplicação de reserva não está protegida de forma nenhuma (nem dentro do processo) e o roteiro a reproduziu nas 10 rodadas.
- **O `ReservationIT` não rodou aqui, por falta de Docker.** A conclusão de que o teste da ordem 7 falharia vem da leitura do código (a última mensagem é a cópia), não de uma execução.
