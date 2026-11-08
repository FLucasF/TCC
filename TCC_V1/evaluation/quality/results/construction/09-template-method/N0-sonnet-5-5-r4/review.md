# Revisão: construção, cenário 09

## Visão geral

O projeto tem seis pacotes: `web`, `service`, `report`, `domain`, `store` e a raiz com o `Main`. Uma requisição entra pelas rotas do Javalin em `web/App.java:46-66`. O corpo é lido e validado em `web/RequestParser.java:20-41`, e a rota chama `ReservationService` (para as reservas) ou `ReportService` (para os relatórios). Essas classes rodam a operação dentro de uma transação, por `Store.inTransaction` (`store/PostgresStore.java:81-95`). As regras de situação, preço e devolução ficam no domínio, em `Reservation`, `Quote` e `RatePlan`. Os dados vão para o PostgreSQL, em três tabelas criadas pelo próprio código ao subir (`store/PostgresStore.java:52-78`). Os quatro relatórios produzem um `ReportTable`, e um único `CsvFormat.render` escreve título, cabeçalho, linhas e total (`report/CsvFormat.java:16-23`). O roteiro do comportamento passou inteiro (64 de 64 conferências), inclusive as reservas simultâneas. Os pontos fracos são estes:
- os dois relatórios novos (taxa de turismo e planos) não têm teste nenhum;
- nenhum teste passa pelo banco (o `store` tem 0% de cobertura);
- o esquema é criado com `CREATE TABLE IF NOT EXISTS` no código, sem migração e sem `CHECK`.

## Arquitetura

**Números:** 6 pacotes, 12 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece em `web` e na raiz, e o JDBC só em `store`. A maior classe é `PostgresStore`, com 269 linhas, e as outras têm 101 linhas ou menos.
**Nota:** bom

- **Borda HTTP fina:** cada rota só lê a requisição, chama o serviço e escreve a resposta (`web/App.java:46-55`). As rotas dos relatórios são geradas a partir da lista de relatórios (`web/App.java:57-66`), então um relatório novo não exige rota nova. O mapeamento de erro para status HTTP fica num lugar só (`web/App.java:35`, `domain/ErrorCode.java:3-9`).
- **Regras sem Javalin e sem SQL:** a máquina de estados fica em `domain/Reservation.java:41-69`, o preço em `domain/Quote.java:9-26` e o desconto e a devolução por plano em `domain/RatePlan.java:18-25`.
- **Acesso ao banco isolado:** todo o SQL está em `store/PostgresStore.java`. Os serviços e os relatórios só conhecem a interface `Tx` (`store/Tx.java:11-41`). Os relatórios dependem de `store`, mas só dessa interface (`report/MonthlyReport.java:34-38`), o que segue o sentido regras → dados.
- **Montagem num lugar só:** tudo é criado e ligado em `Main.java:15-25`, sem `static` mutável.

Nada que um revisor exigente pediria para mudar.

## Design de baixo nível

**Números:** os 4 relatórios passam pela mesma estrutura, e nenhum fica fora dela.
- `Report` (`report/Report.java:6-15`) tem duas classes abstratas com método-modelo `final`: `DailyReport` (`report/DailyReport.java:16-27`) e `MonthlyReport` (`report/MonthlyReport.java:20-31`).
- As quatro subclasses (`OccupancyReport`, `RevenueReport`, `TourismTaxReport`, `RatePlanReport`) preenchem só o passo `build`.
- A linha de título aparece em 1 lugar (`report/CsvFormat.java:18`), e em 0 relatórios.

**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

**O pattern.** O esqueleto que o texto pede (título, cabeçalho, linhas, total) está num lugar só, `CsvFormat.render` (`report/CsvFormat.java:16-23`). Os quatro relatórios passam por ele, por `ReportService.csv` (`report/ReportService.java:26-28`). Cada relatório só entrega as partes que variam, como dados: o nome, o cabeçalho, as linhas e o total, num `ReportTable` (`report/ReportTable.java:6-7`). Isso é a composição que a referência aceita: um escritor genérico que recebe as partes que variam, com os quatro relatórios passando por ele.

Há também um Template Method de verdade: `generate` é `final` em `DailyReport` e `MonthlyReport`, valida o período e chama o `build` abstrato. A seleção comum das reservas do mês também está na classe-mãe (`report/MonthlyReport.java:34-48`). Mas esse método-modelo cobre a leitura do período, não a escrita do CSV.

A linha com a data de emissão, que o dono anuncia para o ano que vem, entraria só em `CsvFormat.render`, e um relatório novo é só mais uma subclasse na lista de `report/ReportService.java:17-20`.

**O que afasta um pouco.** Cada `build` ainda monta à mão as suas linhas e o seu total. E a soma do "dinheiro que ficou" aparece duas vezes: `report/RevenueReport.java:23` e `report/RatePlanReport.java:31-33`.

**O exagero.**
- `Store` e `Tx` têm duas implementações (`PostgresStore` e o `InMemoryStore` dos testes), então se justificam.
- `TourismTax` (`report/TourismTax.java:7-13`) é uma interface funcional com uma implementação só, e o parâmetro `month` é ignorado. É pequena e responde ao "a prefeitura muda o valor da taxa todo ano", mas sozinha a variável de ambiente lida em `Main.java:21` já bastaria. Não chega a pesar como exagero.

**O resto do código** é limpo:
- dinheiro em `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:16-18`, `domain/Quote.java:10-17`);
- datas em `LocalDate` e `YearMonth`;
- `enum` para situação, plano, tipo de quarto e canal;
- records imutáveis, com transição de estado que devolve um novo objeto (`domain/Reservation.java:71-74`);
- exceções que não somem: o `catch (Throwable)` de `store/PostgresStore.java:88-91` desfaz a transação e relança.

Restam pontos pequenos:
- o construtor de `Reservation`, com 16 argumentos posicionais (vários do mesmo tipo `BigDecimal`), é chamado em três lugares (`service/ReservationService.java:39-41`, `store/PostgresStore.java:161-163` e `264-267`);
- o tipo do quarto na ocupação é buscado numa lista e, se não for achado, cai em `""` sem aviso (`report/OccupancyReport.java:26-27`).

## Testes

**Números:** 9 testes rodaram, nenhum falhou e nenhum foi pulado; 9 métodos escritos (`measures/tests.md`). Cobertura total: 59,8% das linhas e 64,3% dos ramos.

| pacote | linhas | ramos |
|---|---|---|
| `domain` | 87/87 | 32/34 |
| `service` | 45/46 | 6/6 |
| `web` | 66/76 | 19/30 |
| `report` | 65/90 | 6/14 |
| `store` | 0/127 | 0/6 |

**Nota:** com problemas

**O que os testes conferem bem, com valores:**
- o exemplo de alta temporada, 599,76 (`PricingTest.java:15-19`);
- as fronteiras da temporada (`PricingTest.java:22-30`);
- o desconto arredondado meio para o par, 37,485 → 37,48 (`PricingTest.java:33-38`);
- o ciclo de vida inteiro, com o texto exato do e-mail e a segunda via igual ao comprovante do check-out (`ApiTest.java:75-121`);
- as devoluções nos três casos (`ApiTest.java:124-136`);
- os códigos de erro (`ApiTest.java:145-159`);
- a ocupação e o financeiro com a linha de título, inclusive a não reembolsável cancelada que continua contando (`ApiTest.java:162-189`).

**O que falta:**
- **Os dois relatórios novos, a parte central do cenário, não têm teste nenhum.** Nenhum teste chama `/reports/tourism-tax` ou `/reports/rate-plans`. Pela pergunta da mutação, nada falharia:
  - se a taxa trocasse 4,50 por outro valor;
  - se `taxOf` (`report/TourismTaxReport.java:37-39`) multiplicasse só pelas noites;
  - se `paidStartingIn` virasse `retainedStartingIn` (`report/TourismTaxReport.java:26`), e as não reembolsáveis canceladas entrassem na taxa;
  - se o relatório de planos deixasse de listar um plano sem reserva.
- **Nenhum teste passa pelo banco.** Os testes da API rodam sobre um `InMemoryStore` (`InMemoryStore.java:13-83`), que reimplementa à mão o SQL das consultas (`InMemoryStore.java:39-42`, `64-72`). Por isso o `store` tem 0% de cobertura, e um erro no SQL de sobreposição ou de período (`store/PostgresStore.java:129-137`, `193-210`) passaria despercebido. A trava contra reservas simultâneas também não tem teste.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária (`measures/database.md`).
- O dinheiro está todo em `numeric(12,2)`.
- A chave estrangeira `reservations.room_number` tem índice.
- As datas estão em `date`.
- Não há nenhum `CHECK`, `UNIQUE` ou `EXCLUDE`, e as quatro colunas de situação, plano, tipo e canal ficam sem `CHECK`.
- Os candidatos a SQL montado com `+` só concatenam constantes (`RESERVATION_COLUMNS`, `store/PostgresStore.java:16-18`).

**Nota:** com problemas

**O que está certo:**
- os tipos, as chaves, `NOT NULL` em tudo o que é obrigatório (`store/PostgresStore.java:56-71`);
- um pool Hikari (`store/PostgresStore.java:22-30`), conexões em try-with-resources e uma transação por operação (`store/PostgresStore.java:81-95`);
- o e-mail gravado na mesma transação da confirmação (`service/ReservationService.java:50-57`);
- a criação do esquema serializada por `pg_advisory_xact_lock` (`store/PostgresStore.java:55`).

A regra "um quarto, uma reserva por noite" é protegida por um `SELECT ... FOR UPDATE` na linha do quarto antes da conferência de sobreposição (`service/ReservationService.java:33-34`, `store/PostgresStore.java:123-137`). A trava vale entre processos, e o roteiro confirmou isso.

**Os defeitos:**
- **O esquema é criado no código, com `CREATE TABLE IF NOT EXISTS`** (`store/PostgresStore.java:56-71`), sem migração. O próximo pedido que precisar de uma coluna nova não terá como acrescentá-la num banco que já existe.
- **O banco não protege nenhuma regra por conta própria:** não há `CHECK` em `status`, `rate_plan` ou `check_out > check_in`, e não há restrição de exclusão para a sobreposição. Tudo depende de o código passar pela trava.

## Comportamento

**Números:** 64 de 64 conferências passaram (`measures/behavior.md`):
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 09: 13 de 13;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Nada falhou. Para o dono da pousada:
- os preços com temporada e desconto saem com os centavos certos;
- as recusas trazem os códigos combinados com o site;
- o e-mail e o comprovante saem no texto pedido;
- os dados sobrevivem a um reinício;
- duas reservas do mesmo quarto ao mesmo tempo nunca são as duas aceitas;
- os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>";
- a taxa de turismo conta só as reservas pagas e dá 0.00 num mês vazio;
- o relatório de planos mostra os dois planos mesmo quando um deles não tem reserva.

## Dúvidas

- **A classe do pattern.** Ficou entre `acerto` e `alternativa equivalente`. Existe uma hierarquia de Template Method (`DailyReport` e `MonthlyReport`, com `generate` `final` e `build` abstrato) que cobre os quatro relatórios. Mas o esqueleto que o texto descreve, título, cabeçalho, linhas e total, fica num escritor genérico que recebe os dados (`CsvFormat.render`), e não no método-modelo. Por isso dei `alternativa equivalente`.
- **O exagero.** A interface `TourismTax`, com um parâmetro de mês que ninguém usa, é um caso de fronteira. Contei como contenção por ser mínima e por responder a uma mudança que o texto anuncia.
