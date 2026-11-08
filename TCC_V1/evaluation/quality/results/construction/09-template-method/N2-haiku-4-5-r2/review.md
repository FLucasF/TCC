# Revisão: construção, cenário 09

## Visão geral

O projeto tem seis pacotes. Os handlers HTTP ficam soltos em `com.pousada` (`ReservationsHandler`, `ReportsHandler`, `RoomsHandler`, `MessagesHandler`). Há também `application` (o `ReservationService`), `domain` (as entidades e enums, o `SeasonCalculator` e o `Rounding`), `infrastructure` (JDBC puro com `DriverManager`, um repositório por tabela e o `Database`, que cria as tabelas com `CREATE TABLE IF NOT EXISTS`), `reports` (uma classe abstrata `Report` e quatro subclasses) e `api.dto`. Uma requisição passa por `Application` → handler (que lê o corpo com Gson) → `ReservationService` → repositório → PostgreSQL. Os relatórios vão de `ReportsHandler` direto para a subclasse de `Report`, que lê os repositórios. Os problemas mais sérios: **o sistema não sobe.** `Database.initialize` chama `conn.commit()` com o autocommit ligado (`src/main/java/com/pousada/infrastructure/Database.java:70`), o PgJDBC recusa com "Cannot commit when autoCommit is enabled" e o `main` morre (`measures/database.md`, `measures/behavior.md`). Mesmo com isso corrigido, sobram duas falhas. Primeiro, o jar não tem Jackson (o pom só traz Gson, `pom.xml:38-42`), mas os handlers respondem com `ctx.json(...)`; testei o Javalin com o classpath do próprio jar, e `ctx.json` devolve 500 "It looks like you don't have an object mapper configured". Segundo, o comprovante sai como texto, e não como o JSON que o texto pede. Não há nenhum teste. Dinheiro é `double` em todo o código.

## Arquitetura

**Números:** 6 pacotes, 11 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece em `com.pousada` e o JDBC só em `com.pousada.infrastructure`. A maior classe é `ReservationService`, com 208 linhas.
**Nota:** com problemas

As dependências vão na direção certa: os handlers dependem do serviço, o serviço dos repositórios, e os repositórios e os DTOs do domínio. Não há ciclo. O SQL fica isolado em `infrastructure/`, e o serviço não conhece o Javalin. Os defeitos:

- **Montagem espalhada.** Não há um lugar que monte as peças. Cada classe cria as suas dependências por dentro: `ReservationService.java:12-14` dá `new` nos três repositórios, os handlers dão `new` no serviço ou no repositório (`ReservationsHandler.java:18`, `RoomsHandler.java:11`, `MessagesHandler.java:8`), e cada relatório dá `new` no `ReservationRepository` (`OccupancyReport.java:11-12`, `RevenueReport.java:13`, …). `ReportsHandler` cria um relatório novo a cada chamada (`ReportsHandler.java:26,46,66,86`). `Application.java:17-23` cria sete `ReservationsHandler`, um por rota, e cada um vem com o seu próprio serviço. A conexão sai de um `static` global (`Database.java:8-14`). Com isso, não dá para trocar uma peça nem testar uma parte isolada.
- **Regra na borda.** O handler de confirmação calcula o valor pago (`ReservationsHandler.java:86-88`), em vez de deixar isso para o serviço. A validação do CPF e dos campos também ficou no handler (`ReservationsHandler.java:26-44`).
- **Os relatórios pulam a camada de regras.** As regras do dinheiro que fica com a pousada e de quais reservas contam como pagas moram dentro de cada relatório (`RevenueReport.java:69-82`, `RatePlansReport.java:77-90`), e não no domínio nem no serviço.
- O pacote raiz guarda os handlers junto com o `main`. Não é um erro, mas a borda HTTP não tem um pacote próprio, ao contrário do `api.dto`.

São problemas localizados. As camadas existem e não há ciclo, por isso a nota é `com problemas`.

## Design de baixo nível

**Números:** 4 de 4 relatórios são subclasses de `Report` e nenhum foi escrito por inteiro fora dela. A linha de título só aparece em `Report.java:31-33`. O esqueleto (`Report.java:9-23`) não é `final`. Há 3 cópias de `isPaidReservation`, 2 de `calculateAmount`, 4 classes `ErrorResponse` iguais e 11 blocos `try/catch` iguais nos handlers.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O pattern pedido é o Template Method, e está aplicado direito. `Report.generate` (`reports/Report.java:9-23`) é o método-modelo: título, cabeçalho, linhas e total. Os passos que mudam são abstratos (`Report.java:25-29`), e os quatro relatórios, os dois da parte comum inclusive, só preenchem esses passos (`OccupancyReport.java:14-64`, `RevenueReport.java:15-67`, `TourismTaxReport.java:16-79`, `RatePlansReport.java:16-90`). A linha de título está num lugar só (`Report.java:31-33`), e uma linha nova no fim de todos os relatórios mexeria só em `generate`. Dois pontos afastam o código do ideal, mas não mudam a classe:
- `generate` não é `final`, então uma subclasse pode reescrever o esqueleto;
- os passos `generateRows` e `generateTotal` são independentes e cada um busca as reservas de novo. Cada relatório faz duas consultas, e o total é recalculado em vez de vir das linhas (`RevenueReport.java:35` e `:57`, `TourismTaxReport.java:36` e `:59`, `RatePlansReport.java:36` e `:62`). A ocupação chega a fazer duas voltas de uma consulta por quarto (`OccupancyReport.java:34-35` e `:56-57`).

O valor da taxa de turismo está numa constante (`TourismTaxReport.java:13`), embora a prefeitura mude esse valor todo ano.

**O exagero.** Não há interface, fábrica, builder nem camada que só repasse. `ReceiptResponse` (`api/dto/ReceiptResponse.java`) nunca é usado, mas isso é código morto, não estrutura a mais. A nota do exagero é `contenção`.

**O resto do código.** O resto do código tem defeitos espalhados:
- **Dinheiro em `double`** em tudo: `Reservation.java:16-21`, `RoomType.java:9`, `RatePlan.java:8`, os DTOs, e `setDouble` no banco (`ReservationRepository.java:38-42`). O "meio para o par" pedido é feito com `new BigDecimal(double)` (`Rounding.java:8`), que arredonda a representação binária do valor, e não o valor decimal. Um caso exato de meio centavo pode sair para o lado errado. As somas também acumulam em `double` (`SeasonCalculator.java:30-34`, `RevenueReport.java:59-63`).
- **Duplicação:**
  - `isPaidReservation` está copiado em `RevenueReport.java:69`, `TourismTaxReport.java:71` e `RatePlansReport.java:77`;
  - `calculateAmount` está em `RevenueReport.java:75` e `RatePlansReport.java:83`;
  - a classe `ErrorResponse` aparece igual quatro vezes (`ReservationsHandler.java:164`, `ReportsHandler.java:95`, `RoomsHandler.java:25`, `MessagesHandler.java:21`);
  - o mesmo bloco `try/catch (ApiException)/catch (Exception)` se repete em cada método dos handlers (`ReservationsHandler.java:57-66, 74-80, 90-96, …`);
  - os quatro métodos de `ReportsHandler` são cópias um do outro (`ReportsHandler.java:16-93`).
- **Erros:**
  - todo `catch (Exception e)` vira 500, com `e.getMessage()` exposto ao cliente (`ReportsHandler.java:29-32`);
  - um mês ou uma data fora do formato (`YearMonth.parse` em `ReportsHandler.java:44`) devolve 500, quando o texto pede 400 INVALID_REQUEST;
  - um corpo que não é JSON faz o Gson lançar `JsonSyntaxException`, que é uma `IllegalArgumentException`, e cai na mensagem "Invalid enum value or date format" (`ReservationsHandler.java:60-62`);
  - um id que não é número também vira 500 (`ReservationsHandler.java:71`);
  - um campo ausente é detectado por `== 0` (`ReservationsHandler.java:26-28`), e o `guests: 0` vira "campo faltando" por acaso;
  - `ApiException` é checada, e por isso todos os métodos declaram `throws Exception`.
- **Estado:** `Reservation` é um saco de setters públicos (`Reservation.java:46-92`), e as transições de situação ficam todas no serviço, com `setStatus` (`ReservationService.java:75, 92, 105, 119`). Nada impede alguém de pôr qualquer situação.
- **Método que faz várias coisas:** `createReservation` valida, confere a disponibilidade noite a noite (uma consulta por noite, `ReservationService.java:38-45`), calcula o preço e grava. `generateReceipt` e `formatMoney` montam o texto à mão com `String.format("%.2f")` (`ReservationService.java:194-197`), que depende do `Locale` padrão e não põe o separador de milhar (o texto pede R$ 1.234,56).

Esses defeitos não estão num canto só: aparecem no domínio, nos handlers, nos relatórios e no acesso ao banco. Por isso a nota é `ruim`.

## Testes

**Números:** 0 testes rodaram (0 falhas, 0 erros, 0 pulados), 0 métodos de teste escritos e nenhum arquivo de teste. Não há cobertura de linhas nem de ramos: o JaCoCo não gerou relatório porque nenhum teste rodou (`measures/tests.md`). O `mvn verify` termina com BUILD SUCCESS (`measures/tests-maven.log`) só porque não há o que verificar.
**Nota:** ruim

O projeto não tem `src/test`. O `pom.xml` declara o JUnit (`pom.xml:51-56`) e o surefire (`pom.xml:92-96`), mas nenhum teste foi escrito. Nenhuma regra tem rede:
- o preço com a temporada e o arredondamento;
- o desconto do NON_REFUNDABLE;
- o cancelamento e a devolução;
- as transições de situação;
- os códigos de erro;
- toda a parte nova do cenário: a taxa de R$ 4,50 por hóspede por noite, a linha por plano mesmo sem reservas, e o título dos quatro relatórios.

Se alguém trocasse 4.50 por 5.40 (`TourismTaxReport.java:13`) ou a ordem dos campos do título (`Report.java:32`), nada falharia. Um teste qualquer que subisse o serviço teria pegado a falha de inicialização (`Database.java:70`) e a falta do mapeador JSON.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro é `numeric(10,2)` nas 5 colunas, e as datas são `date`. As conferências acusaram:
- 1 chave estrangeira sem índice (`reservations.room_number`);
- 4 colunas de situação ou tipo sem CHECK;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- nenhum pool de conexões;
- 0 transações explícitas e 0 `FOR UPDATE`;
- 1 SQL montado com `+`.

O serviço não subiu contra o banco vazio: "Cannot commit when autoCommit is enabled" em `Database.java:70` (`measures/database.md`).
**Nota:** ruim

- **O serviço não sobe contra um banco vazio.** As três tabelas são criadas e as cinco linhas de `rooms` são inseridas (cada comando com autocommit). Depois, o `conn.commit()` de `Database.java:70` lança a exceção e `Application.main` morre (`Application.java:8`). Isso basta para a nota `ruim`.
- **Reiniciar com dados quebraria de novo.** `Database.java:63` faz `DELETE FROM rooms` a cada subida. Com reservas gravadas, a chave estrangeira `reservations → rooms` (`Database.java:47`) recusa o `DELETE`, e o sistema não subiria sobre um banco com dados, mesmo sem o `commit`.
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` fica no código (`Database.java:21-60`), sem migrações, e não sabe acrescentar uma coluna numa tabela que já existe.
- **O tipo do quarto mora no código.** A capacidade e a diária vêm do enum (`RoomType.java:4-6`). A tabela `rooms` guarda só o número e o tipo, sem CHECK no tipo.
- **A regra "um quarto, uma reserva por noite" não tem proteção.** Não há restrição EXCLUDE, transação nem trava. O serviço consulta noite a noite e depois insere (`ReservationService.java:38-45` e `:60`), cada passo numa conexão nova, e duas reservas ao mesmo tempo passam as duas.
- **Duas escritas que deviam andar juntas, sem transação.** A confirmação grava a reserva (`ReservationService.java:77`) e depois a mensagem (`:80`). Se a segunda falhar, a reserva fica confirmada sem e-mail.
- **O código JDBC.** Usa `DriverManager.getConnection` a cada chamada, sem pool (`Database.java:12-14`), e o dinheiro é gravado com `setDouble` (`ReservationRepository.java:38-42, 80`). O único SQL montado com `+` (`RoomRepository.java:32`) recebe um `int` do JSON, então não há injeção, mas o código destoa do resto, que usa `?`. As conexões são devolvidas com try-with-resources.
- **A carga.** A ocupação faz uma consulta por quarto, duas vezes (`OccupancyReport.java:35, 57`). Os relatórios mensais trazem o mês inteiro duas vezes, e o filtro de situação é feito em Java (`RevenueReport.java:35-38, 57-61`).

## Comportamento

**Números:** 0 conferências passaram e 0 falharam: o roteiro não conseguiu testar nada, porque o serviço não subiu contra um banco vazio (`measures/behavior.md`; o log em `measures/behavior-service.log` termina em `Database.java:70`, "Cannot commit when autoCommit is enabled").
**Nota:** ruim

Para o dono da pousada, isso quer dizer que o sistema entregue não funciona: o `java -jar` termina com uma exceção antes de abrir a porta, e o site não consegue chamar nada. Pela leitura, mesmo que a inicialização passasse, sobrariam estas falhas.

**O que derruba quase toda a API:**
- **Nenhuma resposta JSON funcionaria.** O jar não tem Jackson (só Gson, `pom.xml:38-42`), e todos os handlers respondem com `ctx.json` (`ReservationsHandler.java:56`, `RoomsHandler.java:18`, …). Rodei um Javalin com o classpath do jar gerado, e o `ctx.json` devolveu 500 "It looks like you don't have an object mapper configured". Isso inclui as respostas de erro. Só os quatro CSV (`ctx.result`) sairiam.
- **O sistema não subiria uma segunda vez** com reservas no banco, por causa do `DELETE FROM rooms` de `Database.java:63`.

**O que fica fora do texto:**
- O comprovante do check-out e da segunda via sai como texto puro (`ReservationsHandler.java:117-118, 145-146`), sem os campos `title`, `reservationId`, … que o texto pede.
- O dinheiro no comprovante sai sem separador de milhar (`ReservationService.java:194-197`).
- A ocupação conta reservas pendentes (`ReservationRepository.java:90-94`, usado em `OccupancyReport.java:35`), mas deve contar só as pagas.
- O financeiro e o relatório por plano não contam a reserva não reembolsável cancelada (`RevenueReport.java:69-73`, `RatePlansReport.java:77-81`), embora o texto diga que ela continua contando. Os totais de dinheiro sairiam errados para o contador.
- Mês ou data fora do formato e id que não é número dariam 500 em vez de 400 (`ReportsHandler.java:44`, `ReservationsHandler.java:71`).
- Um CPF com pontuação seria recusado com 400 (`ReservationsHandler.java:40`). O texto não pede essa recusa, mas é uma escolha defensável.

## Dúvidas

- O roteiro do comportamento não conseguiu medir nada, então as falhas da seção "Comportamento", além da inicialização, vêm da leitura do código. A exceção é a falta do mapeador JSON, que confirmei rodando o Javalin com o classpath do jar gerado (fora do projeto). As falhas dos relatórios (as reservas pendentes na ocupação, a não reembolsável cancelada fora do financeiro e do relatório por plano) não foram executadas.
- O Template Method recebeu `acerto`, mas sem `final` e com o total recalculado em cada passo. Um revisor mais rígido poderia ver isso como execução fraca, ainda que todos os quatro relatórios passem pelo esqueleto.
- Os scripts de medição criaram `projeto/target/` ao empacotar o projeto. Eu não mexi em nada do código.
