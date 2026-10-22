# Revisão: construção, cenário 09

## Visão geral

O agente montou um projeto Maven com sete pacotes: `api` (um `ApiHandlers` com todas as rotas), `business` (o `ReservationService` e o `ApiException`), `db` (o `Database`, que cria as tabelas com `CREATE TABLE IF NOT EXISTS` e abre conexões por `DriverManager`, e três repositórios JDBC), `models`, `reports` (uma classe abstrata `Report` e os quatro relatórios), `utils` (o `PricingUtils`) e a classe `Application`, que liga as peças e registra as rotas. Uma requisição entra no `ApiHandlers`, que lê o corpo com Gson, chama o serviço (ou direto um repositório) e responde com `ctx.json(...)`. Os dados ficam no PostgreSQL, em três tabelas.

O defeito mais sério é que o sistema não funciona pela API. O projeto usa Gson para ler o JSON, mas responde com `ctx.json`, e esse método usa o mapeador do Javalin, que precisa do Jackson. O Jackson não está no `pom.xml`. Por isso toda resposta JSON vira 500, inclusive as de erro: só 12 de 64 conferências passaram. Há mais um problema por baixo disso: as reservas são gravadas antes de a resposta quebrar, e não há nada que impeça duas reservas do mesmo quarto na mesma noite. Nas 10 rodadas simultâneas, o banco ficou com as 10 duplicadas.

Mesmo com o Jackson, outros defeitos continuariam:
- a confirmação exige um `amountPaid` no corpo;
- o financeiro e o relatório de planos deixam de fora a reserva não reembolsável cancelada;
- o comprovante escreve o dinheiro sem separador de milhar;
- o dia 29 de fevereiro fica fora da alta temporada.

O pattern pedido (Template Method) ficou malfeito: a classe base só tem funções de CSV, e cada relatório repete a sequência inteira dos passos.

## Arquitetura

**Números:** 7 pacotes, 14 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). A maior classe é `ApiHandlers` (316 linhas), seguida de `ReservationService` (191) e `ReservationRepository` (176). Usam JDBC os pacotes `com.pousada`, `db` e `reports` (este só pelo `SQLException`). O Javalin aparece só em `com.pousada` e `api`.
**Nota:** com problemas

As camadas existem e apontam na direção certa. As regras de preço e de situação ficam em `business/ReservationService.java` e `utils/PricingUtils.java`, sem Javalin e sem SQL. O SQL fica nos repositórios (`db/ReservationRepository.java:19-176`), e a montagem fica no `main` (`Application.java:20-30`).

Há misturas localizadas:
- A borda HTTP pula o serviço e fala com os repositórios: `api -> db` no jdeps, e `ApiHandlers` recebe três repositórios (`api/ApiHandlers.java:30-35`).
- O handler da segunda via decide sozinho a regra de "só para reserva paga" (`api/ApiHandlers.java:153-163`). O mesmo handler busca o tipo do quarto no repositório dentro da conversão para JSON (`api/ApiHandlers.java:297-299`).
- O `main` executa SQL direto para semear os quartos (`Application.java:52-67`), fora da camada de dados.
- Os relatórios são criados pelo handler a cada chamada com os repositórios (`api/ApiHandlers.java:190`, `206`, `222`, `238`). Cada um faz a sua consulta, sem passar por um serviço.

`ApiHandlers` concentra validação, conversão para JSON, tratamento de erro e leitura de parâmetros de todas as 13 rotas. Cada novo relatório ou rota vai crescer ali.

## Design de baixo nível

**Números:** 4 relatórios, todos subclasses de `Report`. O esqueleto (título, cabeçalho, linhas, total) está em 0 deles: só o título (`generateTitle`) e a montagem de uma linha CSV (`generateCsvRow`) estão na base. A sequência de passos aparece repetida 4 vezes.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O que aproxima do Template Method:
- existe uma classe abstrata `Report` (`reports/Report.java:3`);
- os quatro relatórios, inclusive os dois da parte comum, são subclasses dela (`OccupancyReport.java:14`, `RevenueReport.java:11`, `TourismTaxReport.java:11`, `RatePlanReport.java:13`);
- o nome da pousada e o formato do título estão num lugar só (`Report.java:4`, `Report.java:8-10`).

Mas o único método abstrato é o próprio `generate()` (`Report.java:6`). Não há método-modelo: cada subclasse escreve a sequência inteira à mão (título, `\n`, cabeçalho, laço das linhas, linha do total, `\n`):
- `OccupancyReport.java:26-71`
- `RevenueReport.java:23-58`
- `TourismTaxReport.java:25-61`
- `RatePlanReport.java:24-80`

É exatamente o caso "o esqueleto é só uma função utilitária de CSV e cada relatório ainda repete a sequência dos passos". O pedido do dono, de pôr a data no fim de todos os relatórios de uma vez, exigiria mexer nas quatro classes. O mesmo vale para:
- o formato do período, copiado em três (`RevenueReport.java:26`, `TourismTaxReport.java:28`, `RatePlanReport.java:28`);
- o `catch (SQLException) -> RuntimeException`, repetido quatro vezes.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface com uma só implementação, nenhuma fábrica, nenhuma camada que só repassa. Os `with...` do `Reservation` (`models/Reservation.java:111-127`) são simples.

**O resto do código.** Os defeitos são localizados, mas vários:
- **Erros:**
  - todo handler tem `catch (Exception e)` que transforma qualquer falha em 400 `INVALID_REQUEST` (`api/ApiHandlers.java:74-76`, `110`, `122`, `134`, `192`) ou em 500 com a mensagem da exceção (`api/ApiHandlers.java:43`, `90`, `168`). O check-out responde 400 com o código `INTERNAL_ERROR` (`api/ApiHandlers.java:145-146`);
  - `comprovanteToJson` engole a exceção e deixa o `roomType` de fora (`api/ApiHandlers.java:296-300`);
  - todos os métodos do serviço declaram `throws Exception` (`business/ReservationService.java:24`, `62`, `82`, `109`, `126`), e o comprovante lança um `Exception` genérico (`business/ReservationService.java:163`).
- **Dinheiro:**
  - no JSON, o dinheiro passa por `Double.parseDouble(String.format("%.2f", ...))` (`api/ApiHandlers.java:250`, `266-272`, `304-306`). Isso perde as duas casas (0.00 vira 0.0) e depende do locale da máquina;
  - no comprovante, `formatCurrency` não põe o separador de milhar que o texto pede, R$ 1.234,56 (`utils/PricingUtils.java:64-69`).
- **Datas:** a alta temporada fixa o fim de fevereiro no dia 28 (`utils/PricingUtils.java:23-24`), então o dia 29 de um ano bissexto sai como baixa temporada.
- **Duplicação:**
  - "buscar a reserva ou 404" aparece quatro vezes no serviço (`business/ReservationService.java:63-66`, `83-86`, `110-113`, `127-130`);
  - o "pago menos devolvido" aparece duas vezes (`RevenueReport.java:36-39`, `RatePlanReport.java:48-51`);
  - a leitura do parâmetro `month` aparece três vezes no handler (`api/ApiHandlers.java:199-206`, `215-222`, `231-238`).
- **Estado:** `Database.RoomData` tem campos públicos mutáveis (`db/Database.java:93-97`).

Os tipos de domínio estão certos: `BigDecimal` com `HALF_EVEN` (`utils/PricingUtils.java:60-62`), `LocalDate`, e `enum` para plano, situação e tipo de quarto.

## Testes

**Números:** 5 testes rodaram, 0 falharam, 0 pulados; 5 métodos escritos, num arquivo só. A cobertura de linhas é 15 de 809 (1,9%) e a de ramos é 9 de 200 (4,5%). Os pacotes `api`, `business`, `db`, `models` e `reports` têm 0% (`measures/tests.md`).
**Nota:** ruim

O único arquivo de teste (`src/test/java/com/pousada/PricingUtilsTest.java`) confere três datas de temporada (`:14-29`), o preço de uma noite de alta temporada, 299,88 (`:32-37`), e o arredondamento meio para o par (`:40-44`).

Nada mais é testado:
- o desconto do não reembolsável e o total;
- as situações da reserva e as recusas;
- o cancelamento e a devolução;
- o comprovante e o e-mail;
- os dois relatórios da parte comum;
- a parte nova do cenário: o título dos relatórios, a taxa de turismo e o relatório por plano.

A pergunta da mutação:
- Trocar `4.50` por `5.40` (`TourismTaxReport.java:12`) não derruba nenhum teste.
- Apagar o `CANCELLED` que falta no filtro, ou trocar a ordem dos planos, também não.
- Trocar `day <= 28` por `day <= 29` (`PricingUtils.java:24`) não derruba nenhum teste.

Não há teste pela API nem contra o banco. Por isso o defeito que derruba o sistema inteiro (o `ctx.json` sem Jackson) passou pelo `mvn verify` sem ser visto.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Os 6 campos de dinheiro estão em `numeric(10,2)` e as datas em `date`. As conferências acusaram:
- 1 chave estrangeira sem índice (`reservations.room_number -> rooms`);
- as colunas de situação, plano, tipo e canal em `varchar` sem CHECK;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- nenhum pool, 1 `DriverManager.getConnection`, 0 transações, 0 `FOR UPDATE`;
- nenhum SQL montado com `+` (`measures/database.md`).

**Nota:** ruim

Os tipos estão certos, o SQL usa parâmetros (`?`) em todos os repositórios, e as conexões são fechadas com try-with-resources (por exemplo, `db/ReservationRepository.java:27-28`). O problema é o que o banco deixa passar e como ele é mantido:

- **Nada impede a reserva em dobro.** O serviço consulta os conflitos (`business/ReservationService.java:42`, que usa `db/ReservationRepository.java:94-120`) e depois insere (`business/ReservationService.java:58`), cada passo numa conexão própria. Não há transação, trava nem restrição de exclusão. Depois do roteiro, o banco `review_behavior_2` tinha **10 pares de reservas não canceladas do mesmo quarto (301) nas mesmas noites**, um para cada rodada simultânea. A API respondeu 500 às duas chamadas de cada rodada, mas as duas foram gravadas. É dado errado de verdade: o mesmo quarto vendido duas vezes.
- **O esquema não evolui.** Ele é criado no código com `CREATE TABLE IF NOT EXISTS` (`db/Database.java:24-71`), sem migrações. O próximo pedido que precise de uma coluna nova não tem como chegar a um banco que já existe.
- **Faltam índice e conexões reutilizáveis.** A chave estrangeira `room_number` não tem índice (`db/Database.java:52`), e é justamente a coluna da consulta de conflito. Toda operação abre uma conexão nova por `DriverManager` (`db/Database.java:73-75`), sem pool.
- **A confirmação não é atômica.** A atualização da reserva (`business/ReservationService.java:75`) e a gravação do e-mail (`business/ReservationService.java:157`) não estão numa transação: uma falha entre as duas deixa a reserva confirmada sem mensagem.
- **O relatório de ocupação faz uma consulta por quarto** (`reports/OccupancyReport.java:37-42`). Com cinco quartos, isso pesa pouco.

## Comportamento

**Números:** 12 de 64 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 4 de 46 |
| os dados depois de reiniciar | 1 de 3 |
| o cenário 09 | 6 de 13 |
| as reservas simultâneas | 1 de 2 |

**Nota:** ruim

O serviço sobe, mas quase nenhuma chamada que devolve JSON funciona. `GET /rooms`, `POST /reservations` e todas as respostas de erro (409, 422, 404, 400) voltam 500 com a mensagem do Javalin "It looks like you don't have an object mapper configured" (`measures/behavior-service.log`). A causa: o código responde com `ctx.json` (por exemplo, `api/ApiHandlers.java:41`, `71`, `73`), e o `pom.xml` só tem Gson, sem `jackson-databind`.

Para o dono da pousada, o site não consegue:
- listar os quartos;
- criar uma reserva e saber o número dela;
- confirmar, fazer check-in ou check-out;
- receber um código de erro legível.

E a reserva é gravada mesmo quando a resposta falha: um hóspede que tenta de novo cria outra reserva, e duas pessoas podem ficar com o mesmo quarto (as 10 rodadas simultâneas gravaram em dobro, como na coluna do banco). O "OK" da conferência simultânea ("nenhuma com as duas criadas") só passou porque nenhuma chamada respondeu 201.

As conferências que passaram são as que não dependem de JSON: os relatórios em CSV respondem 200 com o título e o cabeçalho certos (`Pousada Maré Alta,occupancy,...`, `...,tourism-tax,...`), e a taxa de um mês vazio sai com o total 0.00. As linhas dos relatórios vieram vazias porque nenhuma reserva chegou a ser confirmada.

Mesmo se o mapeador de JSON fosse corrigido, a leitura do código mostra falhas que continuariam:
- A confirmação exige um corpo com `amountPaid` (`api/ApiHandlers.java:97-104`), e o texto diz que o pago é o total da reserva. Uma confirmação sem corpo daria `NullPointerException` e 400.
- O financeiro e o relatório de planos usam `findByMonthStarting`, que só traz CONFIRMED, CHECKED_IN e CHECKED_OUT (`db/ReservationRepository.java:130-131`). Com isso, a não reembolsável cancelada, que o texto manda contar, some dos dois relatórios.
- A segunda via de uma reserva pendente responde 404 `RESERVATION_NOT_FOUND` em vez de uma recusa da situação (`api/ApiHandlers.java:160-163`).
- O dinheiro sai no JSON sem as duas casas (`api/ApiHandlers.java:266-272`).
- O comprovante escreve R$ 1234,56 sem o ponto do milhar (`utils/PricingUtils.java:68`).

## Dúvidas

1. O texto não diz que código a segunda via de uma reserva não paga deve devolver. Tratei o 404 como desvio menor, porque o roteiro espera 409 `OPERATION_NOT_ALLOWED`.
2. No design de baixo nível, a nota fica entre `com problemas` e `ruim`: o `catch (Exception)` aparece em todos os handlers e o dinheiro é formatado de três jeitos. Dei `com problemas` porque os defeitos se concentram no `ApiHandlers` e no `PricingUtils`, e o resto é legível.
3. No banco, a skill põe a duplicação "protegida só dentro do processo" em `com problemas`. Aqui não há proteção nenhuma, e a duplicação aconteceu de fato nas 10 rodadas, por isso dei `ruim`.
