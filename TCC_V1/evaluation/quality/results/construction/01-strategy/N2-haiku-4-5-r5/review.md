# Revisão: construção, cenário 01

## Visão geral

Projeto Javalin + JDBC puro em 8 pacotes: `api` (as rotas em `Routes`, `ApiException`, DTOs em `api.request` e `api.response`), `service` (`ReservationService` e `ReportService`), `pricing` (a interface `RatePlan`, uma classe por plano, `RatePlanFactory`, `Season`, `MoneyRounder`), `database` (DAOs estáticos com `DriverManager`) e `domain`. Uma requisição entra em `Routes` (métodos estáticos), chama um método estático do serviço, que lê e escreve pelos DAOs estáticos. Os dados ficam em 3 tabelas criadas por `CREATE TABLE IF NOT EXISTS` em `Database.java`. Os planos de tarifa seguem bem o Strategy. Mas o sistema, na prática, não funciona, e há dois defeitos graves. Primeiro: o `pom.xml` não traz o Jackson e as rotas respondem com `ctx.json(...)`. Então toda resposta JSON, inclusive as de erro, sai como 500 "object mapper not configured": o roteiro passou em 5 de 70 conferências. Pior, `POST /reservations` grava a reserva antes de falhar. Segundo: a cada subida, `Database.java:83` roda `DELETE FROM rooms`. Assim que existe uma reserva, a chave estrangeira impede esse `DELETE`, e o serviço não sobe mais sobre o próprio banco. Os testes são só de unidade do preço (12,5% das linhas) e não pegaram nenhum dos dois problemas.

## Arquitetura

**Números:** 8 pacotes, 15 dependências entre eles, 2 ciclos (`api ↔ service` e `api → service → pricing → api`, de `measures/dependencies.md`). Maiores classes: `ReservationService` (279 linhas), `Routes` (205) e `ReservationDao` (204).
**Nota:** com problemas

- As camadas existem: o SQL fica só no pacote `database` (o JDBC aparece em `service` só pelo tipo `SQLException`), as regras de preço ficam em `pricing` sem Javalin, e `domain` não depende de ninguém.
- Os ciclos vêm de uma causa só. `ApiException`, que carrega status HTTP, mora em `api` e é lançada pelo serviço (`service/ReservationService.java:27,36,40,84…`) e pelos planos (`pricing/StandardPlan.java:12`, `pricing/CorporatePlan.java:25`, `pricing/WeekendPackagePlan.java:20`). Assim, regra de negócio conhece código e status HTTP.
- O serviço também recebe o DTO da borda, `CreateReservationRequest` (`service/ReservationService.java:7,18`).
- A borda não é só fina:
  - `Routes` acessa os DAOs direto, sem passar pelo serviço (`api/Routes.java:52`, `:108`, `:134`, `:153`);
  - a regra "segunda via só para reserva paga" está no handler (`api/Routes.java:130-132`);
  - a montagem do comprovante está copiada em dois handlers (`api/Routes.java:109-121` e `:135-147`).
- Não há montagem: tudo é `static`. A configuração do banco fica em campos estáticos mutáveis (`database/Database.java:9-11`), os serviços chamam DAOs estáticos, e `Main` só inicializa o estático (`Main.java:18,21`). Para trocar uma peça ou testar o serviço isolado, seria preciso reescrever.
- Uma mistura localizada mais um ciclo com causa única dão `com problemas`, não `ruim`.

## Design de baixo nível

**Números:** 5 planos de tarifa, os 5 na estrutura (`RatePlan` + `StandardPlan`, `NonRefundablePlan`, `CorporatePlan`, `WeekendPackagePlan`, `LongStayPlan`); 0 fora dela. Um único `switch`, que só escolhe a implementação (`pricing/RatePlanFactory.java:7-13`). Nenhum `if` sobre o plano no serviço.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `RatePlan` (`pricing/RatePlan.java:7-13`) junta os três comportamentos que variam: `validate`, `calculatePrice` e `calculateRefund`.

- Cada plano, inclusive os dois do ponto de partida, tem o seu preço, a sua validação e a sua devolução:
  - sexta a domingo e 2 noites: `pricing/WeekendPackagePlan.java:19-30`;
  - 7 noites: `pricing/LongStayPlan.java:16-18`;
  - código da empresa: `pricing/CorporatePlan.java:24-26`;
  - devolução da metade: `pricing/WeekendPackagePlan.java:45`;
  - devolução zero: `pricing/NonRefundablePlan.java:42`.
- O serviço pede ao plano sem nomeá-lo, na criação (`service/ReservationService.java:43-48`) e no cancelamento (`service/ReservationService.java:154-158`).
- A única seleção é o `switch` da fábrica, que a referência aceita. Um plano novo é uma classe e um `case`.
- O que afasta um pouco:
  - `CorporatePlan` guarda o `companyCode` num campo que `validate` preenche e `calculatePrice` lê (`pricing/CorporatePlan.java:16,28,49`). É acoplamento temporal: chamar o preço sem validar antes dá `NullPointerException`.
  - O `companyCode` é gravado para qualquer plano (`service/ReservationService.java:61`), mas é ignorado no preço (ver Dúvidas).

**O exagero.** Não há estrutura sem necessidade: a fábrica é o ponto de seleção do próprio pattern, e os DTOs servem à borda.

**O resto do código.**

- Duplicação:
  - o laço das noites com a temporada está copiado, idêntico, em quatro planos (`pricing/StandardPlan.java:21-31`, `pricing/NonRefundablePlan.java:21-31`, `pricing/CorporatePlan.java:36-46`, `pricing/LongStayPlan.java:26-36`);
  - a conferência "saída depois da entrada" se repete em quatro `validate` (`pricing/StandardPlan.java:11`, `pricing/CorporatePlan.java:20`…), embora o serviço já a faça (`service/ReservationService.java:26`).
- Regra de domínio errada: `Season.isHighSeason` só considera alta temporada até o dia 28 em janeiro e fevereiro (`pricing/Season.java:19-21`). As noites de 29, 30 e 31 de janeiro, e 29/02 nos anos bissextos, ficam sem os 20%.
- Erros:
  - `catch (Exception)` transforma qualquer falha (até erro de banco ou de serialização) em 400 `INVALID_REQUEST` (`api/Routes.java:75-76`, `service/ReportService.java:49-50`);
  - os blocos `catch (ApiException e) { throw e; }` em todos os métodos do serviço são ruído (`service/ReservationService.java:71-73`…).
- Formatação:
  - dinheiro e CSV são formatados com `String.format` no locale padrão da JVM (`service/ReservationService.java:246`, `service/ReportService.java:103`); numa máquina em pt_BR, o CSV sairia com vírgula;
  - o e-mail sai com uma linha em branco a mais, porque o comprovante termina em `\n` (`service/ReservationService.java:236`) e o corpo ainda acrescenta `"\n\n"` (`:207`).
- O que está certo: os tipos (`BigDecimal` com `HALF_EVEN` a cada passo, em `pricing/MoneyRounder.java:8`; `LocalDate`; `enum` para status e plano).

## Testes

**Números:** 9 testes rodaram, 0 falharam (9 métodos escritos). Cobertura: 12,5% das linhas (101 de 811) e 17,3% dos ramos (33 de 191). `pricing` tem 95 de 125 linhas cobertas; `service`, `api` e `database` têm 0. Nenhum teste chama a API ou fala com o banco (`measures/tests.md`).
**Nota:** com problemas

- Os testes de `PricingTest` conferem com valores os exemplos do texto:
  - TECNOSUL 779,80 / 93,58 / 686,22 (`src/test/java/com/pousadamarealta/pricing/PricingTest.java:114-116`);
  - pacote 820,00 e devolução de 410,00 (`:130-135`);
  - estadia longa 1.749,30 / 174,93 / 1.574,37 (`:149-151`);
  - temporada de julho a 599,76 (`:83-85`) e NON_REFUNDABLE (`:98-100`).
- Fica todo o resto sem rede:
  - as recusas dos planos novos (convênio sem código ou com código errado, pacote fora de sexta a domingo, estadia com menos de 7 noites): `validate` só é chamado com entradas válidas (`:107`, `:127`, `:146`);
  - AGROVALE e FARMAVIDA;
  - a devolução do convênio e da estadia longa;
  - a máquina de estados, os códigos de erro, o e-mail e os relatórios.
- A pergunta da mutação:
  - trocar 0.18 por 0.81 (`pricing/CorporatePlan.java:12`) não derruba nenhum teste;
  - trocar `nights < 7` por `nights < 8` (`pricing/LongStayPlan.java:16`) também não;
  - apagar a conferência de sexta-feira (`pricing/WeekendPackagePlan.java:19-21`) também não.
- `RoutesTest` não testa rotas: confere o Gson, um arredondamento e a temporada (`src/test/java/com/pousadamarealta/api/RoutesTest.java:15-42`). A temporada é conferida em 15 de janeiro (`:37`), por isso o erro de 29 a 31 de janeiro passou.
- Como nenhum teste sobe o servidor, o `mvn verify` passa com um sistema que responde 500 a tudo.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária, e o dinheiro em `numeric(10,2)`. As conferências acusaram (`measures/database.md`):

- a chave estrangeira `reservations(room_number)` sem índice;
- nenhum CHECK em status, plano, tipo ou canal;
- nenhuma restrição UNIQUE/EXCLUDE;
- 0 transações, 0 `FOR UPDATE`, nenhum pool (`DriverManager` a cada chamada);
- o esquema criado em `CREATE TABLE IF NOT EXISTS` no código.

**Nota:** ruim

- O defeito sério: `Database.initialize` roda `DELETE FROM rooms` e reinsere os quartos a cada subida (`database/Database.java:83-93`). Reproduzi à mão: depois de criada uma reserva, a segunda subida morre com `update or delete on table "rooms" violates foreign key constraint "reservations_room_number_fkey"`. O serviço não sobe mais sobre o próprio banco, e o roteiro do comportamento confirmou ("o serviço sobe de novo sobre o mesmo banco: não subiu"). Os dados não somem, mas ficam inacessíveis até alguém mexer no banco à mão.
- O esquema não evolui: não há migrações, e `CREATE TABLE IF NOT EXISTS` (`database/Database.java:35-75`) não acrescenta coluna numa tabela que já existe. É o que o próximo plano de tarifa com dado novo vai precisar.
- Nada impede duas reservas na mesma noite: a consulta de disponibilidade (`service/ReservationService.java:46`, `database/ReservationDao.java:101-106`) e o `INSERT` (`service/ReservationService.java:52`) rodam em conexões separadas, sem transação, trava nem restrição.
- Escritas que deviam andar juntas vão em conexões separadas e sem transação: status e valor pago na confirmação (`service/ReservationService.java:87-88`), status e devolução no cancelamento (`:161-162`).
- O que está certo: o SQL é todo com parâmetros (`?`), as conexões são fechadas por try-with-resources, as datas estão em `date`, há `NOT NULL` no que é obrigatório e a chave estrangeira para o quarto existe.

## Comportamento

**Números:** 5 de 70 conferências passaram (`measures/behavior.md`):

- a parte comum: 4 de 46;
- os dados depois de reiniciar: 0 de 1;
- o cenário 01: 0 de 21;
- reservas simultâneas: 1 de 2.

**Nota:** ruim

- O serviço sobe, mas qualquer rota que responde JSON devolve 500 com "It looks like you don't have an object mapper configured". O `pom.xml` não tem `jackson-databind`, e `api/Routes.java` responde com `ctx.json` (`api/Routes.java:64` nos quartos, `:72` na criação…). Até o tratador de erros usa `ctx.json` (`api/Routes.java:41`), então os 400, 404, 409 e 422 também viram 500.
- Para o dono da pousada, isso quer dizer:
  - o site não consegue listar quartos, criar, confirmar, cancelar nem consultar reservas;
  - nenhum e-mail é gerado;
  - nenhum dos planos novos pode ser usado pela API.
- Pior: `POST /reservations` grava a reserva no banco antes de falhar (conferido à mão: o 500 voltou e a linha `1 | PENDING | 499.80` ficou em `reservations`). O site acha que não reservou, mas o quarto fica preso por uma reserva pendente.
- Só passam o que não usa JSON (o cabeçalho dos dois CSV, com o `Content-Type` certo) e "nenhuma rodada com as duas reservas criadas", que passou porque ambas responderam 500.
- Também pesam:
  - o serviço não sobe de novo depois de haver uma reserva (explicado em Banco);
  - fora do roteiro, pela leitura do código, há o erro da temporada nas noites de 29 a 31 de janeiro (`pricing/Season.java:19-21`);
  - o financeiro lista com 0,00 as reservas canceladas com devolução total (`database/ReservationDao.java:160`), quando o texto pede só as que deixaram dinheiro;
  - a linha de total do financeiro sai como `total,,,,0.00` (`service/ReportService.java:88`), e não no formato `total,<soma>` do exemplo de ocupação.

## Dúvidas

- Testes: a nota ficou entre `com problemas` e `ruim`. Os 9 testes conferem com valores os três exemplos do texto, mas cobrem só o preço (12,5% das linhas) e nenhuma recusa, situação da reserva ou relatório. Ficou `com problemas` porque o que existe confere regra de verdade.
- Gravar o `companyCode` em qualquer plano (`service/ReservationService.java:61`) é uma regra de plano pequena fora do plano. Não muda a classe: o preço o ignora fora do convênio.
- O formato da linha de total do financeiro não estava fechado no texto. Só pesa ao lado das outras falhas do relatório.
