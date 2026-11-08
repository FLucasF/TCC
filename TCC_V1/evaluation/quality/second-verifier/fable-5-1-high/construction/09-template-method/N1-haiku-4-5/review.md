# Revisão: construção, cenário 09

## Visão geral

O projeto tem sete pacotes: `com.pousada` (o `main`, que cria e liga as peças e semeia os quartos), `api` (uma classe `ApiHandlers` com todas as rotas e a conversão para JSON), `business` (`ReservationService` e `ApiException`), `db` (`Database`, que cria as tabelas com `CREATE TABLE IF NOT EXISTS`, e três repositórios JDBC), `models` (entidades e enums), `reports` (uma classe abstrata `Report` e quatro subclasses) e `utils` (`PricingUtils`). Uma requisição entra pelo Javalin em `Application.java:35-47`, cai num método de `ApiHandlers`, que chama o `ReservationService` (ou um repositório direto, nas leituras e nos relatórios), e o serviço fala com o PostgreSQL pelos repositórios; os dados ficam em três tabelas (`rooms`, `reservations`, `messages`). O defeito mais sério: o `pom.xml` não traz o Jackson e o código não configura nenhum `JsonMapper`, mas todos os handlers respondem com `ctx.json(...)` (`api/ApiHandlers.java:110,140,142,...`); na prática, **toda rota JSON, inclusive as de erro, responde 500** (`measures/behavior-service.log`: "It looks like you don't have an object mapper configured", repetido a cada chamada). Só os quatro relatórios CSV funcionam, e, como nenhuma reserva pode ser criada nem confirmada, saem vazios. O `mvn verify` passa, mas o sistema entregue não cria uma reserva sequer.

## Arquitetura

**Números:** 7 pacotes, 14 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Maior classe: `api/ApiHandlers.java` com 316 linhas; depois `business/ReservationService.java` com 191 e `db/ReservationRepository.java` com 176.
**Nota:** com problemas

O que está bem: as dependências apontam da borda para as regras e das regras para os dados (`api -> business -> db -> models`), sem ciclo; o Javalin só aparece em `com.pousada` e `com.pousada.api`, e o JDBC só em `db` e `reports` (tabela de bibliotecas em `measures/dependencies.md`); o `ReservationService` não conhece `Context` nem SQL (`business/ReservationService.java:12-21`); a montagem é toda no `main` (`Application.java:20-30`), com os repositórios injetados por construtor; o acesso ao banco mora nos repositórios, com try-with-resources.

O que pesa:

- A borda pula o serviço: `ApiHandlers` recebe `ReservationRepository` e `MessageRepository` além do serviço (`api/ApiHandlers.java:99-105`) e vai direto ao banco em `getReservation` (`:151`), `getDocument` (`:222`) e nos relatórios (`:259,275,291,307`), que ela mesma instancia. A regra "segunda via só para reserva paga" ficou no handler, e testada por `amountPaid > 0` em vez da situação (`api/ApiHandlers.java:229-231`).
- SQL fora da camada de dados: `Application.initializeRooms` abre `Statement` e executa `SELECT COUNT(*)` à mão (`Application.java:52-67`), e os cinco quartos estão codificados no `main` em vez de num lugar de dados.
- Os relatórios (`reports/*`) dependem dos repositórios e do `SQLException` (`reports/OccupancyReport.java:30,89-91`), ou seja, uma camada de apresentação de CSV que também é camada de consulta.
- `ApiHandlers` concentra tudo (treze rotas, quatro conversores JSON e o tratamento de erro em cada método, `api/ApiHandlers.java:107-312`); é a classe que vai doer quando o site pedir mais uma rota.

Nada disso é ausência de camadas; é mistura localizada, que dá `com problemas`.

## Design de baixo nível

**Números:** 4 relatórios como subclasses de `Report` (`reports/Report.java:3`), mas o esqueleto (título → cabeçalho → linhas → total) está escrito inteiro 4 vezes, uma em cada `generate()` (`reports/OccupancyReport.java:46-92`, `reports/RevenueReport.java:116-152`, `reports/TourismTaxReport.java:178-215`, `reports/RatePlanReport.java:241-297`); 0 passos abstratos além do próprio `generate()`.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O que aproxima do Template Method: existe uma classe abstrata `Report` (`reports/Report.java:3-20`) e os quatro relatórios, inclusive a ocupação e o financeiro, herdam dela. O que afasta: o único método abstrato é `generate()` (`reports/Report.java:6`), e cada subclasse escreve a sequência completa por conta própria: monta o período, chama `generateTitle`, escreve o cabeçalho, varre as reservas, escreve a linha de total e devolve a string (por exemplo, `reports/TourismTaxReport.java:180-211`). A base só oferece dois utilitários, `generateTitle` e `generateCsvRow` (`reports/Report.java:8-19`). Isso é exatamente o caso que a referência chama de malfeito: "o esqueleto é só uma função utilitária de CSV e cada relatório ainda repete a sequência dos passos". O pedido do dono ("no ano que vem todos os relatórios vão ter que terminar com uma linha com a data; quando o jeito mudar, quero que mude em todos de uma vez") não é atendido: acrescentar o rodapé exige mexer nos quatro `generate()`. A repetição também aparece no cálculo do dinheiro que ficou com a pousada, copiado em `reports/RevenueReport.java:129-132` e `reports/RatePlanReport.java:264-267`, e na construção do período `String.format("%04d-%02d", year, month)` em três classes (`RevenueReport.java:119`, `TourismTaxReport.java:181`, `RatePlanReport.java:244`).

**O exagero.** Não há interface com uma implementação só, fábrica, builder nem camada que só repassa; `contenção`.

**O resto do código.**

- Tipos: `BigDecimal` com `HALF_EVEN` a cada passo (`utils/PricingUtils.java:243-273`), `LocalDate`, enums para plano, situação e tipo de quarto; isso está certo. Mas para o JSON todo valor passa por `Double.parseDouble(String.format("%.2f", ...))`, repetido nove vezes (`api/ApiHandlers.java:319,335-341,373-375`): dinheiro vira `double` na saída, e `String.format` sem `Locale` depende do locale da JVM (em pt_BR sai "599,76" e o `parseDouble` quebra); o mesmo `String.format("%.2f")` sem locale está nos CSVs (`reports/RevenueReport.java:140,146` e irmãos).
- `formatCurrency` não põe o separador de milhar que o texto pede ("R$ 1.234,56"): `"R$ " + String.format("%.2f", value).replace(".", ",")` dá "R$ 1159,80" para uma SUITE de duas noites (`utils/PricingUtils.java:275-280`).
- `isHighSeason` considera fevereiro até o dia 28 (`utils/PricingUtils.java:234-235`): 29/02/2028 fica fora da alta temporada, embora o texto diga "até o fim de fevereiro".
- Erros: todo handler termina em `catch (Exception e)` que devolve 400 `INVALID_REQUEST` com `e.getMessage()` para qualquer falha, inclusive de banco (`api/ApiHandlers.java:143-145,178-179,190-191,202-203`); em `checkOut` o mesmo `catch` devolve 400 com código `INTERNAL_ERROR` (`:214-215`); `comprovanteToJson` engole a exceção e devolve `roomType` vazio (`:365-369`); os relatórios embrulham `SQLException` em `RuntimeException` (`reports/OccupancyReport.java:89-91`).
- Estado: `Database.RoomData` com campos públicos mutáveis (`db/Database.java:93-97`); `Reservation` sem `final` nos campos, embora use `withX` para mudar (`models/Reservation.java:7-22`).
- Métodos: `createReservation` do handler lê, valida e converte oito campos num bloco só (`api/ApiHandlers.java:118-134`); os quatro `generate()` fazem consulta, cálculo e formatação juntos.

Defeitos reais, mas localizados num código pequeno e legível: `com problemas`.

## Testes

**Números:** 5 testes rodaram, 0 falharam, 0 pulados; 5 métodos de teste escritos; cobertura de linhas 15 de 809 (1,9%) e de ramos 9 de 200 (4,5%); 0 testes pela API, 0 contra o banco, 0 com mocks (`measures/tests.md`). O `mvn verify` termina em `BUILD SUCCESS` (`measures/tests-maven.log:339`).
**Nota:** ruim

Há um único arquivo, `src/test/java/com/pousada/PricingUtilsTest.java`, com cinco testes de unidade sobre `PricingUtils`: alta temporada em julho e dezembro, baixa em maio (`:14-29`), o preço da noite 249,90 × 1,2 = 299,88 (`:32-37`) e o arredondamento meio para o par de 299,885 (`:40-44`). Esses conferem valores de verdade, mas é tudo o que existe: nada sobre o desconto de 15%, a soma das noites, o total, as situações da reserva, os cancelamentos e devoluções, os códigos de erro, o comprovante, o e-mail, e nada sobre a parte nova do cenário (linha de título, taxa de turismo, relatório por plano). A pergunta da mutação: trocar `4.50` em `reports/TourismTaxReport.java:165`, trocar a ordem dos planos em `reports/RatePlanReport.java:274`, apagar a linha de título em qualquer relatório ou trocar `0.15` em `utils/PricingUtils.java:220` não faria nenhum teste falhar. A cobertura dos pacotes `api`, `business`, `db` e `reports` é 0 de 635 linhas. Um teste pela API, ou mesmo um que chamasse `ctx.json`, teria revelado o defeito que derruba o sistema inteiro (o mapper de JSON ausente). Quase nenhum teste: `ruim`.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)` nas seis colunas (ok); datas em `date`; 1 chave estrangeira (`reservations.room_number -> rooms`) sem índice; 0 CHECK, 0 UNIQUE/EXCLUDE; esquema criado por `CREATE TABLE IF NOT EXISTS` no código, sem migrações; 0 pool, 0 transações explícitas, 0 `FOR UPDATE`; nenhum SQL montado com `+` (`measures/database.md`).
**Nota:** com problemas

O que está certo: o serviço sobe contra um banco vazio e cria as três tabelas (`measures/database-schema.sql`); os tipos são os certos (`db/Database.java:27-62`); todo SQL usa `?` (`db/ReservationRepository.java:125-148,164-172,184-196`); as conexões são fechadas por try-with-resources; a reserva aponta para o quarto por chave estrangeira (`db/Database.java:52`); as semeaduras dos quartos só acontecem se a tabela estiver vazia (`Application.java:55-57`).

O que pesa:

- Nada impede duas reservas do mesmo quarto na mesma noite, nem no banco nem no processo: a verificação é "consulta e depois insere", em duas conexões diferentes, sem transação, sem trava e sem `EXCLUDE` (`business/ReservationService.java:42-58`; `db/ReservationRepository.java:200-226` e `:122-161`). Dois pedidos simultâneos passam os dois pela checagem. (O roteiro do comportamento não conseguiu medir isso porque toda chamada já responde 500; o código deixa claro.)
- O esquema vive em `CREATE TABLE IF NOT EXISTS` dentro de `db/Database.java:24-71`: quando o contador pedir o relatório de hóspedes por estado e for preciso uma coluna nova, esse código não sabe acrescentá-la numa tabela que já existe.
- `status`, `rate_plan`, `type` e `channel` são `varchar` sem CHECK (`db/Database.java:29,44,49,57`), e `check_out > check_in`, `guests >= 1` e `capacity` não têm restrição no banco.
- Chave estrangeira sem índice (`measures/database.md`), e a ocupação faz uma consulta por quarto em vez de uma só (`reports/OccupancyReport.java:57-62`).
- Sem pool: `DriverManager.getConnection` a cada chamada (`db/Database.java:73-75`).
- `findByMonthStarting` filtra `status IN (CONFIRMED, CHECKED_IN, CHECKED_OUT)` (`db/ReservationRepository.java:237,246-248`) e é a consulta do financeiro e do relatório por plano: uma NON_REFUNDABLE cancelada, que o texto manda contar, fica de fora. Isso é regra errada na consulta; pesa aqui como defeito de dado e aparece no comportamento.

Nada em ponto flutuante, nada montado com dado de fora, nada que suma ao reiniciar: `com problemas`.

## Comportamento

**Números:** 12 de 64 conferências passaram (52 falharam): parte comum 4 de 46; dados depois de reiniciar 1 de 3; cenário 09 6 de 13; duas reservas ao mesmo tempo 1 de 2 (`measures/behavior.md`).
**Nota:** ruim

A causa de quase tudo é uma só: o Javalin 6 precisa do Jackson (ou de um `JsonMapper` configurado) para `ctx.json(...)`, e o `pom.xml` só traz o Gson (`pom.xml:36-41`), sem `jackson-databind` e sem `JavalinConfig.jsonMapper` em `Application.java:32-33`; o jar empacotado não tem nenhuma classe `com/fasterxml/jackson` (conferido com `jar tf projeto/target/mare-alta.jar`). Toda resposta JSON, inclusive a de erro (`api/ApiHandlers.java:379-384` também passa por `ctx.json`), termina em 500 (`measures/behavior-service.log`, "It looks like you don't have an object mapper configured", a cada requisição). Daí:

- `GET /rooms` responde 500; `POST /reservations` responde 500 e nenhuma reserva é criada; confirmar, cancelar, check-in, check-out, segunda via e todos os códigos de erro (400, 404, 409, 422) viram 500 ou 404 porque a reserva não existe. Para o dono: o site não consegue criar uma reserva.
- As únicas conferências que passaram são as de CSV (`ctx.result`, não `ctx.json`): os quatro relatórios respondem 200 em `text/csv`, com a linha de título "Pousada Maré Alta,<nome>,<período>" e o cabeçalho certos (`measures/behavior.md`: "ocupação: a linha de título vem primeiro", "taxa de turismo de um mês vazio: título, cabeçalho e total 0.00", "planos: responde 200"). Mas como não há reservas, as linhas e os totais vêm vazios ou zerados, e o resto do cenário não pôde ser conferido.
- "As mensagens continuam lá" passou só porque `GET /messages` respondeu a mesma coisa (500) antes e depois de reiniciar; não é prova de persistência.
- Nas dez rodadas simultâneas as duas chamadas deram 500; o "OK: nenhuma com as duas criadas" é vazio.
- "Mês fora do formato: 400" veio 500 pelo mesmo motivo (o erro também é JSON).

Defeitos que o roteiro não alcançou mas o código mostra e que falhariam mesmo com o JSON consertado: a confirmação exige `amountPaid` no corpo (`api/ApiHandlers.java:166-173`), enquanto o texto diz que o pago é o total da reserva e o roteiro chama `POST .../confirmation` sem corpo (`gson.fromJson("")` dá `null` e cai no 400); a segunda via de reserva não paga devolve 404 `RESERVATION_NOT_FOUND` em vez de 409 `OPERATION_NOT_ALLOWED` (`api/ApiHandlers.java:229-231`); o financeiro e o relatório por plano excluem a NON_REFUNDABLE cancelada (`db/ReservationRepository.java:237`); o comprovante escreve "R$ 1159,80" sem o ponto de milhar (`utils/PricingUtils.java:279`); 29 de fevereiro fica fora da alta temporada (`utils/PricingUtils.java:234-235`); e a reserva simultânea não tem proteção (coluna do banco).

O serviço sobe, mas a parte comum quebra inteira: `ruim`.

## Dúvidas

- A nota do banco: a duplicação de reserva não tem proteção nenhuma (nem no processo). A skill coloca "protegida só dentro do processo" em `com problemas`; dei `com problemas` porque o que o sistema grava, grava certo, mas um revisor que pese o risco de dado errado poderia dar `ruim`.
- O `String.format("%.2f")` sem `Locale` só quebra se a JVM rodar com locale que usa vírgula; no ambiente desta medição não quebrou, e por isso contou como defeito de design, não de comportamento.
- O enunciado não diz o código da segunda via de reserva não paga; o roteiro espera 409 `OPERATION_NOT_ALLOWED` ("uma operação que a situação da reserva não permite"), que me parece a leitura certa, mas o 404 do agente é uma escolha, não só um erro.
