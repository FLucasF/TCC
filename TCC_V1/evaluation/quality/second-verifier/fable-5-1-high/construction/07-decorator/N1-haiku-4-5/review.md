# Revisão: construção, cenário 07

## Visão geral

O agente montou o projeto em seis pacotes abaixo de `com.pousada.marealta`: `api` (quatro controllers Javalin), `service` (`ReservationService`, `ReportService`, `PriceCalculator`), `domain` (`Reservation`, `Room`, `Message` e os enums), `infrastructure/persistence` (`Database` com os `CREATE TABLE` e três repositórios JDBC), `infrastructure/messaging` (o `MessageSender` com uma lista de "políticas") e `util` (arredondamento, formatação, temporada, e-mail). A montagem é toda no `main` (`Application.java:32-51`): uma requisição entra num handler do controller, que chama o serviço, que usa os repositórios e, na confirmação, o `MessageSender`; os dados ficam em três tabelas PostgreSQL criadas ao subir. O mais sério: **o sistema não funciona pela API**. As rotas com parâmetro foram escritas na sintaxe `:id` (`api/ReservationController.java:68`), que o Javalin 6 recusa com `IllegalArgumentException` ao registrar; como o servidor já tinha sido iniciado (`Application.java:46`) antes do `register` (`Application.java:48-51`), o `main` morre e o Jetty fica de pé só com `GET /rooms` e `POST /reservations`; e essas duas respondem 500 porque o `pom.xml` traz Gson e nenhum Jackson, e o `ctx.json` do Javalin 6 exige um JSON mapper. Resultado: 3 de 65 conferências do roteiro passaram, e o `mvn verify` passa só porque os cinco testes existentes não tocam nem a API nem o banco.

## Arquitetura

**Números:** 7 pacotes, 16 dependências entre eles, 0 ciclos (`measures/dependencies.md`). Javalin só em `api` e no `main`; JDBC só em `infrastructure.persistence`; `domain` e `util` não dependem de nada do projeto.
**Nota:** com problemas

As camadas existem e apontam no sentido certo: os controllers chamam o serviço, o serviço chama os repositórios e o `MessageSender`, os repositórios falam JDBC (`infrastructure/persistence/ReservationRepository.java:36-37`), as regras não conhecem `Context` nem SQL (`service/ReservationService.java` inteira), e tudo é criado e ligado num só lugar (`Application.java:32-44`). Sem ciclo. O que pesa:

- **A borda HTTP não é fina.** `api` depende de `infrastructure.persistence` diretamente: `ReservationController` recebe dois repositórios além do serviço (`api/ReservationController.java:19-20`) e faz as leituras por conta própria (`:71`, `:177`), e monta o comprovante em JSON duas vezes, copiado, dentro dos handlers de check-out e de segunda via (`:145-158` e `:184-197`), consultando o quarto no repositório para isso. `RoomController` e `MessageController` nem passam pelo serviço (`api/RoomController.java:13`, `api/MessageController.java:11`). Uma regra ficou na borda por omissão: a segunda via "só existe para reserva paga", e o handler de `/document` não confere o estado (`:174-199`).
- **O contrato entre camadas é o texto da exceção.** O serviço lança `IllegalArgumentException("Room not found")`, `("Invalid guest count")`, `("Room unavailable")` (`service/ReservationService.java:30,34,38,43`), e o controller escolhe o código HTTP com `message.contains(...)` (`api/ReservationController.java:229-240`, e `e.getMessage().contains("not found")` repetido em `:92,110,128,162`). Mudar uma mensagem no serviço muda o status HTTP.
- Os métodos do serviço declaram `throws Exception` (`service/ReservationService.java:27,59,89,120,142,164`), vazando o `SQLException` do repositório para a borda, e os handlers fecham com `catch (Exception)` devolvendo `e.getMessage()` em 500 (`api/ReservationController.java:63-65,99-100`).
- A maior classe é o controller de reservas (242 linhas), justamente por carregar leitura, montagem de JSON e mapeamento de erros.

## Design de baixo nível

**Números:** das 3 regras de envio, 2 estão na estrutura (`EmailValidationPolicy`, `TestModePolicy`) e 1 fora (a cópia para o arquivo, codificada no `MessageSender`); o modo de teste é lido em 2 lugares (na política e no próprio envio); a combinação de regras é fixa no construtor do envio, não na montagem; o construtor de 16 argumentos de `Reservation` é copiado 5 vezes no serviço.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O enunciado pede, sem nomear, o Decorator: cada regra de envio um embrulho com a interface do envio, empilhados na montagem conforme o modo. O agente fez uma lista de filtros: a interface `MessagePolicy` (`infrastructure/messaging/MessagePolicy.java:3-5`), um resultado que pode transformar destinatário/assunto/texto ou bloquear (`:7-27`), e o `MessageSender.applyPolicies` que percorre a lista e para no primeiro bloqueio (`infrastructure/messaging/MessageSender.java:43-52`). Isso é a forma da "alternativa equivalente" que a referência aceita, e a validação de e-mail (`EmailValidationPolicy.java:7-12`) e o desvio de teste (`TestModePolicy.java:14-23`) são peças próprias. O que afasta do acerto, e decide a classe:

- **Uma das três regras ficou fora da estrutura.** A cópia para o arquivo não é uma política: é um `if` e um método privado dentro do envio (`MessageSender.java:38-40` e `:54-58`), com o endereço e o prefixo como constantes da classe (`:12-13`). A "segunda pousada que não quer a cópia" teria de editar o `MessageSender`.
- **O envio de dentro ainda pergunta pelo modo de teste.** `MessageSender` guarda `testModeEnabled` (`:17,21`) e decide por ele se faz a cópia (`:38`), ao mesmo tempo em que a `TestModePolicy` recebe o mesmo flag e decide por dentro se se aplica (`TestModePolicy.java:7,15`). A combinação por modo está escrita dentro das regras e do envio, exatamente o que a referência chama de malfeito.
- **A combinação não é decidida na montagem.** `Application.java:37` passa só um booleano; a lista de políticas é fixada no construtor do próprio `MessageSender` (`MessageSender.java:22-25`). Existe um `addPolicy` (`:60-62`), mas ninguém o chama. "Vale sempre, só no teste ou só fora dele" não tem onde ser expresso sem um `if` novo dentro de cada regra.

**O exagero.** Não há estrutura sem uso fora do pattern: a interface `MessagePolicy` tem duas implementações, `MessageContext` e `MessagePolicyResult` são os dois valores que a cadeia precisa, `PriceCalculator.PriceBreakdown` é um retorno simples. Contenção.

**O resto do código.** A base é razoável: domínio imutável (`domain/Reservation.java:6-21`), enums para situação, plano e tipo, `LocalDate` para datas, nenhum `static` mutável. Mas um revisor pediria para corrigir antes de aceitar:

- **Dinheiro em `double`** em todo o domínio e no cálculo: `Reservation.subtotal/discount/total/amountPaid` (`domain/Reservation.java:16-21`), as diárias (`domain/RoomType.java:4-6`), o desconto `0.15` (`domain/RatePlan.java:5`), a soma noite a noite em `double` (`service/PriceCalculator.java:27-33`), o `MoneyRounder` fazendo `new BigDecimal(double)` e voltando para `double` (`util/MoneyRounder.java:10-12`). O arredondamento "meio para o par" pedido sobre a expansão binária de um `double` só acerta por sorte (o teste em `MoneyRounderTest.java:10-11` passa porque 2.455 em binário é 2.45500000000000007…). E o JSON com "2 casas" (`0.00`) é inalcançável com `double` (sai `0.0`).
- **Duplicação pesada.** O construtor de 16 argumentos de `Reservation` é copiado por inteiro em cada transição de estado (`service/ReservationService.java:49-54, 69-76, 108-115, 130-137, 152-159`), só para trocar `status`, `amountPaid` e `refund`; acrescentar um campo à reserva mexe em cinco pontos do serviço, no `mapRow`, no `insert` e nos dois montadores de JSON do controller (`api/ReservationController.java:146-158, 185-197`, que também duplicam o comprovante em texto de `ReservationService.java:170-187`). Os quatro `fromString` são o mesmo laço copiado (`domain/RatePlan.java:17-24`, `RoomType.java:30-37`, `ReservationStatus.java:10-17`, `MessageStatus.java:7-14`) fazendo o que `valueOf` faz.
- **`null` sem cuidado.** Os `fromString` devolvem `null` para valor desconhecido; um `ratePlan` inválido atravessa o serviço e o `PriceCalculator` (`:37` compara com `NON_REFUNDABLE` e segue) e só estoura como `NullPointerException` em `ReservationRepository.java:46`, que o `catch (Exception)` do handler transforma em 400 "por acidente". `room` pode ser `null` em `api/ReservationController.java:145,152`.
- **Erros por texto e `catch (Exception)`:** o mapeamento por `contains` (`api/ReservationController.java:229-240`), um `catch (Exception)` em `ReportController.java:52-53` que transforma qualquer falha (inclusive de banco) em "Invalid month format", e `e.getMessage()` devolvido ao cliente em 500.
- Miudezas: `"/5"` fixo no total da ocupação (`service/ReportService.java:37`) em vez de contar os quartos; `gson` criado e nunca usado (`api/RoomController.java:14`, `Application.java:16`); `pom.xml` com o assembly e o shade gerando o mesmo `mare-alta.jar` (`pom.xml:55-100`, e o aviso em `measures/tests-maven.log:200-201`).

## Testes

**Números:** 5 testes rodaram, 0 falharam, 0 pulados; 5 métodos de teste (o script contou 0 porque procura `@Test` e as classes são JUnit 3, `extends TestCase`); cobertura de linhas 27 de 854 (3,2%) e de ramos 24 de 230 (10,4%), toda no pacote `util` (`measures/tests.md`). Zero testes pela API, contra o banco ou com mocks.
**Nota:** ruim

Os três arquivos testam só utilitários: `EmailValidatorTest.java:6-20` (a regra do @ e do ponto, com bons exemplos, inclusive `a@b.c` e `user@@example.com`), `MoneyRounderTest.java:6-12` (quatro valores do HALF_EVEN) e `SeasonCalculatorTest.java:7-32` (as bordas da temporada, 14/12 e 15/12, 28/02 e 01/03). Isso é tudo. Nada do que o enunciado pede como regra está conferido com valores: o preço de uma reserva (os R$ 599,76 do exemplo, o desconto de 15%), as transições de estado e os códigos 409/422/404, o cancelamento e a devolução, os dois relatórios, e **nenhuma linha da parte nova do cenário**: `MessageSender`, `EmailValidationPolicy` e `TestModePolicy` têm 0 de 53 linhas cobertas. A pergunta da mutação: se a cópia para o arquivo deixasse de ser gerada (`MessageSender.java:38`), se o prefixo "[TESTE] " virasse "[TEST] " (`TestModePolicy.java:5`), ou se a mensagem bloqueada passasse a ser gravada como SENT (`MessageSender.java:33`), nenhum teste falharia. Um teste que subisse o Javalin teria pego também o `:id` e a falta do JSON mapper, os dois defeitos que derrubam o sistema; o `mvn verify` passa porque não exercita nada disso.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; 5 colunas de dinheiro em `numeric(10,2)`; datas em `date`; 1 chave estrangeira (`reservations.room_number → rooms`) **sem índice**; 0 CHECK, 0 UNIQUE/EXCLUDE; 0 transações explícitas, 0 `FOR UPDATE`, 0 pool; SQL sempre com `?` (nenhum candidato a concatenação) (`measures/database.md`).
**Nota:** com problemas

O que está certo: o serviço sobe contra um banco vazio e cria o esquema; dinheiro em `numeric(10,2)` (`infrastructure/persistence/Database.java:81-86`), datas em `date` (`:76-77`), `NOT NULL` no obrigatório, `refund` anulável como o texto pede, reserva ligada ao quarto por FK (`:87`), ids a partir de 1 por `SERIAL`; todo SQL parametrizado e conexões em try-with-resources (`ReservationRepository.java:36-37`). O que pesa:

- **O esquema é `CREATE TABLE IF NOT EXISTS` dentro do código** (`Database.java:21-29`), sem migração versionada: o próximo pedido que acrescentar uma coluna não tem como evoluir um banco que já tem dados.
- **Nada impede duas reservas na mesma noite.** A verificação é ler e depois inserir (`service/ReservationService.java:41-44` e `:56`), em conexões diferentes, sem transação, sem trava e sem restrição no banco (nenhum EXCLUDE/UNIQUE no esquema). Dois pedidos simultâneos passam os dois pela leitura e os dois são gravados. Não há proteção nem mesmo dentro do processo.
- A chave estrangeira não tem índice, e as consultas de conflito e de ocupação filtram por `room_number`, `status` e datas sem nenhum índice (`ReservationRepository.java:120-122, 148-149`). A do financeiro usa `EXTRACT(YEAR/MONTH FROM check_in)` (`:176`), que não aproveita índice em `check_in`.
- `status`, `rate_plan`, `type` e `channel` são `varchar` sem CHECK: o banco aceita qualquer texto.
- Sem pool: cada operação abre uma conexão nova pelo `DriverManager` (`Database.java:17-19`), e `save` abre uma segunda para o `findById` logo depois do `RETURNING id` (`ReservationRepository.java:59-63`, `MessageRepository.java:34-38`).
- `rooms` guarda só número e tipo (`Database.java:33-36`); capacidade e diária vivem no enum `RoomType`. Para cinco quartos que "quase nunca mudam" é aceitável, mas um reajuste de diária é um deploy.

## Comportamento

**Números:** 3 de 65 conferências passaram (`measures/behavior.md`): parte comum 1 de 46; dados depois de reiniciar 1 de 3; cenário no modo normal 0 de 9; cenário no modo de teste 0 de 5; duas reservas ao mesmo tempo 1 de 2 (e essa "passou" porque as duas foram recusadas com 500 nas dez rodadas).
**Nota:** ruim

Dois defeitos derrubam tudo, e os dois estão no log do serviço e no código:

1. **As rotas com parâmetro usam `:id`** (`api/ReservationController.java:68,86,104,122,140,174`). O Javalin 6 lança `IllegalArgumentException: Path '/reservations/:id' invalid - Javalin 4 switched from ':param' to '{param}'` (`measures/behavior-service.log:21-31`, apontando `ReservationController.java:68` e `Application.java:49`). Como `Javalin.create().start()` vem antes dos `register` (`Application.java:46` vs `:48-51`), o `main` morre com o servidor já de pé: ficam registradas só `GET /rooms` e `POST /reservations`. Por isso confirmar, cancelar, check-in, check-out, segunda via, `/messages` e os dois relatórios respondem 404 ("Endpoint GET /reports/occupancy not found").
2. **Não há JSON mapper.** O `pom.xml` traz Gson (`pom.xml:34-38`) e nenhum Jackson, e o jar não contém `com.fasterxml.jackson` (conferido no `target/mare-alta.jar`); todo `ctx.json(...)` do Javalin 6 falha com "It looks like you don't have an object mapper configured" (`measures/behavior.md`, a conferência do `amountPaid`). Por isso `GET /rooms` e `POST /reservations` dão 500, e os handlers de erro, que também usam `ctx.json`, falham de novo dentro do `catch`.

Para o dono da pousada: nenhuma reserva pode ser criada, confirmada nem consultada, nenhum e-mail é gerado, nenhum relatório sai. As três regras novas (bloqueio de endereço inválido, cópia para o arquivo, modo de teste) nunca chegam a executar pela API. As duas únicas conferências que passaram são vazias ("refund null" porque a resposta não tinha nada; "as mensagens continuam lá" porque a lista era igual antes e depois, inacessível nas duas vezes).

Pela leitura, mesmo com os dois defeitos corrigidos, estes pontos do texto falhariam:

- **Segunda via de reserva pendente** devolveria o comprovante em vez de 409 `OPERATION_NOT_ALLOWED`: o handler não confere o estado (`api/ReservationController.java:174-199`).
- **Nenhum hóspede** daria 422 `CAPACITY_EXCEEDED` em vez de 400 `INVALID_REQUEST`: `guests < 1` e `guests > capacidade` caem na mesma exceção (`service/ReservationService.java:33-35` → `api/ReservationController.java:232-233`).
- **O comprovante escreve "R$ 599,76" com espaço não separável (U+00A0)**: `NumberFormat.getCurrencyInstance(pt-BR)` (`util/MoneyFormatter.java:7-10`), conferido no JDK desta máquina; o texto pede "R$ 1.234,56" com espaço comum, e a conferência "Total: R$ 599,76" do roteiro não bateria.
- **O financeiro inclui toda reserva `CANCELLED`** (`ReservationRepository.java:175`), inclusive a STANDARD devolvida (fica com `amount 0.00`) e a que foi cancelada ainda pendente; o texto pede só as que "deixaram dinheiro com a pousada". A linha de total sai como `total,,,,,599.76` (`service/ReportService.java:65`), e o roteiro espera `total,599.76`; o texto não fecha esse formato, então isso sozinho não pesaria.
- **Dinheiro no JSON como `double`**: `amountPaid` antes da confirmação sairia `0.0`, não `0.00`.
- **Duas reservas ao mesmo tempo**: sem transação nem restrição (ver Banco), as duas seriam criadas.

O que a leitura indica que funcionaria: o preço (duas noites de R$ 299,88 = R$ 599,76 em `PriceCalculator.java:28-43` com `SeasonCalculator.java:26-31`), as transições de estado (`domain/Reservation.java:109-123`), as devoluções (`ReservationService.java:99-106`), o texto do e-mail (`:190-196`), a validação do endereço, a cópia com "[Cópia] " logo depois da original, e o desvio para `testes@` com "[TESTE] " e a linha "Destinatário original:" (`TestModePolicy.java:19-22`), sem cópia no modo de teste (`MessageSender.java:38`).

## Dúvidas

- A classe do pattern ficou entre `alternativa equivalente` e `malfeito`: a lista de filtros tem a forma que a referência aceita, mas uma das três regras (a cópia) está fora dela e o envio pergunta pelo modo de teste; a referência trata os dois pontos como `malfeito`, e foi o que dei.
- A nota do design ficou entre `com problemas` e `ruim`: o dinheiro em `double` e a duplicação do construtor aparecem em vários pacotes, mas a estrutura (domínio imutável, enums, serviço pequeno) é sã; dei `com problemas`.
- A linha de total do financeiro (`total,,,,,X` ou `total,X`) é um ponto que o texto não fecha; não contei como erro.
