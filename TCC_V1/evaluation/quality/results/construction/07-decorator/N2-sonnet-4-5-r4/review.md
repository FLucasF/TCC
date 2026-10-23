# Revisão: construção, cenário 07

## Visão geral

O projeto tem 8 pacotes: `controller` (handlers do Javalin), `service` (regras e relatórios), `domain` (a `Reservation`
com preço e transições, as enums), `repository` (JDBC puro com HikariCP), `messaging` (as regras de envio), `dto`,
`util` e o `App`, onde tudo é montado. Uma requisição entra pelo handler (`ReservationController`), que lê o DTO e
chama o `ReservationService`. O serviço usa a `Reservation` para as regras e o `ReservationRepository` para o SQL. Os
dados ficam em três tabelas no PostgreSQL, criadas com `CREATE TABLE IF NOT EXISTS` no código. A separação em
camadas é boa, mas o sistema não funciona. A consulta de conflito de quarto,
`AND (? IS NULL OR id != ?)` com `setObject(null)` (`src/main/java/com/pousadamarealta/repository/ReservationRepository.java:137`
e `:146-147`), faz o PostgreSQL recusar a consulta ("could not determine data type of parameter $4",
`measures/behavior-service.log:63`). Por isso **todo POST /reservations responde 500**. Nenhuma reserva é criada, e
nada que vem depois (confirmação, e-mail, as regras de envio do cenário, check-in, check-out, relatórios com dados)
chega a rodar. O único teste do agente depende de Docker (Testcontainers) e não rodou. Assim, o erro não foi pego.

## Arquitetura

**Números:** 8 pacotes, 16 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin fica só em
`controller` e no `App`, e o JDBC só em `repository` e no `App`. `service`, `domain` e `messaging` não usam nenhuma
biblioteca. A maior classe é `ReservationRepository` (243 linhas).
**Nota:** com problemas

A direção das dependências está certa: borda → serviço → repositório → domínio. A montagem fica num lugar só: os
`new` estão todos em `App.java:23-53`, e o modo de envio é escolhido em `App.java:97-111`. Os handlers são quase
todos finos (`controller/ReservationController.java:25-81`). Há misturas localizadas:

- A regra da segunda via mora no handler (`controller/ReservationController.java:88-90`, "só se `amountPaid` ≠ 0").
  O serviço tem um `generateDocument` com outra regra (`service/ReservationService.java:108-117`), que ninguém chama.
  São duas versões da mesma regra, e a que vale está na borda.
- A regra do relatório financeiro ("deixou dinheiro") está escrita no SQL
  (`repository/ReservationRepository.java:203`), e não no serviço do relatório.
- `OccupancyReportService.java:11` fixa `TOTAL_ROOMS = 5`, em vez de contar os quartos que já estão no
  `RoomService` que ele recebe.
- A tradução de exceções é global e larga: qualquer `IllegalStateException` vira 409 `OPERATION_NOT_ALLOWED`, e
  qualquer `IllegalArgumentException` vira 400 (`controller/ExceptionHandler.java:26-28` e `:50-52`). Um erro de
  programação pode sair como erro de negócio.

## Design de baixo nível

**Números:** as três regras pedidas (validação do endereço, cópia para o arquivo, desvio no modo de teste) estão
todas na estrutura, cada uma numa classe que implementa `MessageRule`, e nenhuma ficou fora dela. A combinação por
modo é decidida na montagem (`App.java:100-108`). Três `if` sobram dentro das regras: a checagem de `BLOCKED` em duas
delas e uma flag morta.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O agente não fez um Decorator (embrulhos do envio que chamam o próximo). Fez uma lista de filtros:
`MessageProcessor.process` aplica cada `MessageRule` em ordem sobre um `MessageContext`
(`messaging/MessageProcessor.java:15-23`). A referência aceita isso como alternativa equivalente: cada regra é uma
peça própria (`EmailValidationRule`, `ArchiveCopyRule`, `TestModeRule`), e a montagem decide a combinação, com a
cópia só fora do teste e a validação sempre (`App.java:100-108`). Uma regra nova (madrugada, limite diário) entra
como mais uma classe na lista, no ramo do modo certo. O que afasta do ideal:

- As regras dependem da ordem e conferem o estado umas das outras. A cópia e o desvio perguntam se a mensagem já foi
  bloqueada (`messaging/ArchiveCopyRule.java:14`, `messaging/TestModeRule.java:24`). Sem um "parar a corrente", cada
  regra nova também terá de lembrar disso.
- `TestModeRule` recebe uma flag `testModeEnabled` e a testa (`messaging/TestModeRule.java:10-20`), mas a montagem
  sempre passa `true` (`App.java:105`). O código é morto, mas não chega a decidir o modo dentro da regra.
- O "envio" de fato, que é gravar na lista, fica fora da estrutura, no serviço
  (`service/ReservationService.java:140-144`). Para uma lista de filtros isso é aceitável.
- A regra do endereço está incompleta: `domain.contains(".")` (`messaging/EmailValidationRule.java:37`) aceita
  `ana@gmail.` e `ana@.com`. O texto pede um ponto *no meio* do nome.

**O exagero:** contenção. Não há interface com uma implementação só, nem fábrica, nem builder. O `MessageService`
só repassa ao repositório (`service/MessageService.java:15-17`), mas é uma classe de um método só e segue o padrão
das outras camadas.

**O resto do código:**

- Dinheiro sai como `double` nos DTOs (`dto/ReservationDTO.java:21-26`, `dto/DocumentDTO.java:20-22`,
  `dto/RoomDTO.java:9`), e por isso o JSON perde as 2 casas: `nightlyRate` vem 249.9 (conferido com `curl` em GET
  /rooms).
- `@JsonInclude(NON_NULL)` em `dto/ReservationDTO.java:9` tira o `refund` do JSON, quando o texto pede `refund: null`.
- Subtotal, desconto e total nunca são guardados. São recalculados a partir da diária atual da enum em três lugares
  (`dto/ReservationDTO.java:31-33`, `dto/DocumentDTO.java:25-27`, `service/ReservationService.java:149-151`).
- A formatação depende do locale padrão da JVM. `formatMoney` faz `%,.2f` seguido de troca de caracteres
  (`service/ReservationService.java:186`), e o CSV usa `%.2f` (`service/RevenueReportService.java:30` e `:42`). Numa
  JVM em pt_BR, o CSV sairia com vírgula.
- Todo repositório faz `catch (Exception)` e relança uma `RuntimeException` genérica
  (`repository/ReservationRepository.java:75`, `:99` e outros), que vira 500 sem código de erro.
- Há código morto (`ReservationService.generateDocument`) e a `Message` é mutável por setters
  (`domain/Message.java:44-62`), alterada no lugar pelas regras.
- O que está bem feito: `BigDecimal` com `HALF_EVEN` em cada passo (`domain/Reservation.java:58-87`), `LocalDate`, as
  enums, e as transições validadas na própria entidade (`domain/Reservation.java:89-123`).

## Testes

**Números:** 1 classe rodou, com 0 falhas, 1 erro e 0 pulados. São 8 métodos de teste escritos, e nenhum executou. A
cobertura é de 0 de 832 linhas (0,0%) e 0 de 176 ramos (0,0%) (`measures/tests.md`). O erro foi "Could not find a
valid Docker environment".
**Nota:** ruim

- Todos os testes estão numa classe só, `IntegrationTest`. Ela sobe um PostgreSQL por Testcontainers
  (`src/test/java/com/pousadamarealta/IntegrationTest.java:19-23`) e ignora o `DB_URL` que o texto dá. Sem Docker,
  nenhum teste roda, e o `mvn verify` não confere nada.
- Se tivessem rodado, `testCreateReservation` (`IntegrationTest.java:77-107`) e quase todos os outros falhariam,
  porque todo POST /reservations dá 500. Isso indica que o agente nunca viu os testes passarem.
- O serviço sobe numa thread, com `Thread.sleep(3000)` (`IntegrationTest.java:34-44`), e nunca é parado.
- Os valores conferidos são bons onde existem: a alta temporada com 599,76 (`:165-166`), o NON_REFUNDABLE com
  173,97 e 985,83 (`:191-193`), e os códigos 409 e 422 (`:238-239`, `:263-264`).
- Ficam sem nenhum teste o cancelamento e a devolução, as transições recusadas, o comprovante e o e-mail, os dois
  relatórios e quase toda a parte nova do cenário. O único teste do cenário (`:268-305`) confere só que a última
  mensagem é `BLOCKED` para `invalid.email`.
- A pergunta da mutação, na parte nova: apagar a `ArchiveCopyRule`, apagar a `TestModeRule`, trocar o prefixo
  "[Cópia] " ou "[TESTE] ", ou trocar `domain.contains(".")` por `true`. Nenhum teste falharia em nenhum desses
  casos.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As
conferências acusaram uma chave estrangeira sem índice (`reservations.room_number`), nenhuma restrição que proteja
uma regra (CHECK, UNIQUE ou EXCLUDE), nenhuma transação explícita e nenhum `FOR UPDATE` (`measures/database.md`).
**Nota:** ruim

- **O SQL que derruba a criação de reservas.** `hasConflict` usa `(? IS NULL OR id != ?)` com `setObject(4, null)`
  (`repository/ReservationRepository.java:137` e `:146-147`). O driver manda o parâmetro sem tipo, e o PostgreSQL
  recusa a consulta com "could not determine data type of parameter $4" (`measures/behavior-service.log:63`). Como
  `createReservation` chama essa consulta antes de todo insert (`service/ReservationService.java:39`), nenhuma
  reserva chega ao banco.
- **Nada impede a reserva dupla.** O código lê se há conflito e depois insere, sem transação, sem trava e sem
  restrição (`service/ReservationService.java:39-48`). Mesmo sem o erro acima, duas reservas simultâneas do mesmo
  quarto passariam. Um `EXCLUDE` com `daterange` faria o banco proteger a regra.
- O esquema é criado com `CREATE TABLE IF NOT EXISTS` no código (`repository/DatabaseInitializer.java:18-48`), sem
  migrações. Esse comando não sabe acrescentar uma coluna numa tabela que já existe.
- A chave estrangeira `room_number` não tem índice (`DatabaseInitializer.java:27`). As colunas de situação e de plano
  são `varchar` sem CHECK.
- A confirmação grava a reserva e depois cada mensagem, em conexões separadas e sem transação
  (`service/ReservationService.java:66-67`, `:142-144`). Uma falha no meio deixa a reserva confirmada sem o e-mail.
- O resto está certo: o SQL usa só parâmetros `?`, as conexões vêm do HikariCP e são devolvidas por
  try-with-resources, e os quartos são semeados com `ON CONFLICT DO NOTHING` (`repository/RoomRepository.java:22-30`).

## Comportamento

**Números:** passaram 16 de 65 conferências (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 13 de 46 |
| depois de reiniciar | 1 de 3 |
| cenário, modo normal | 1 de 9 |
| cenário, modo de teste | 0 de 5 |
| reservas simultâneas | 1 de 2 |

Nas 10 rodadas simultâneas, as duas chamadas deram 500 em todas.
**Nota:** ruim

O sistema sobe e lista os quartos. As recusas que não dependem do banco também respondem certo: 400 para campo
faltando, JSON inválido, datas e hóspedes; 404 para quarto ou reserva que não existe; 422 para capacidade. Mas
**criar uma reserva sempre dá 500**, pelo erro de SQL descrito na coluna do banco. Por isso falha tudo o que vem
depois:

- o preço com a temporada e o desconto, pela API;
- a confirmação, o e-mail e o comprovante;
- o check-in, o check-out e os cancelamentos com devolução;
- os relatórios com dados;
- as reservas depois de reiniciar;
- todas as regras de envio do cenário, nos dois modos. Nenhuma mensagem chega a ser gerada.

Para o dono da pousada, isso quer dizer que o site não consegue fazer nenhuma reserva.

Pontos que o texto deixava em aberto, e que não pesam:

- O roteiro procura o quarto pelo número inteiro (`select(.number == 201)`). O agente devolve o número como texto
  ("201"), e o texto não fecha isso.

Outros defeitos, vistos na leitura e conferidos com `curl`, que a falha principal esconde:

- O dinheiro sai sem as 2 casas no JSON (249.9).
- O `refund` some do JSON em vez de vir `null`.
- A segunda via sairia para uma reserva STANDARD confirmada e depois cancelada, porque `amountPaid` ≠ 0
  (`controller/ReservationController.java:88`).
- O financeiro listaria uma STANDARD cancelada e devolvida, com 0.00 (`repository/ReservationRepository.java:203`).
- Os relatórios sem `date` ou `month` dão 500, e não 400 (`controller/ReportController.java:22`).

## Dúvidas

- A classe do pattern ficou entre `alternativa equivalente` e `malfeito`. Pesam contra as regras que conferem o
  estado `BLOCKED` umas das outras e a flag `testModeEnabled` dentro da `TestModeRule`. Mas a combinação por modo é
  decidida só na montagem, e as três regras estão na estrutura. Como o envio nunca roda pela API (nenhuma reserva é
  criada), o funcionamento das regras não pôde ser conferido.
- O erro de SQL está classificado como defeito do banco, porque está no código que fala com o banco. Ele também
  derruba o comportamento: as duas notas `ruim` vêm em parte da mesma causa. Mas a coluna do banco também seria
  ruim sem ele, já que nada impede a reserva dupla.
