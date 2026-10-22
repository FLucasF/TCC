# Revisão: construção, cenário 09

## Visão geral

O projeto tem cinco pacotes: `br.com.mealta` (só o `Main`, que monta tudo), `web` (rotas Javalin, leitura do corpo, visões JSON), `domain` (reserva, preço, situações, comprovante, e-mail, serviço), `report` (os quatro relatórios e o escritor de CSV) e `store` (JDBC com Hikari, criação do esquema, transação). Uma requisição como `POST /reservations/{id}/confirmation` passa por `web/Api.java:48`, chega em `ReservationService.confirm` (`domain/ReservationService.java:53`). Ali, dentro de uma transação (`store/Database.java:46`), a reserva é travada com `FOR UPDATE` (`store/PgReservationRepository.java:78`), muda de situação pelo próprio objeto (`domain/Reservation.java:53`), e a reserva e o e-mail são gravados juntos. Os dados ficam no PostgreSQL, em duas tabelas (`reservations` e `messages`). Os quartos ficam num enum no código (`domain/RoomType.java:5`). Os quatro relatórios passam por um esqueleto único (`report/CsvRenderer.java:10`) e cada um preenche só as partes que mudam. O comportamento passou em todas as 64 conferências. Não achei nada grave. Os pontos fracos são três: um ciclo pequeno entre `domain` e `store`, um esquema criado com `CREATE TABLE IF NOT EXISTS` no código, sem migrações e sem restrições, e testes que não chegam ao SQL nem ao HTTP.

## Arquitetura

**Números:** 5 pacotes e 11 dependências entre eles. Há 1 ciclo, entre `br.com.mealta.domain` e `br.com.mealta.store`. O Javalin só aparece em `br.com.mealta` e `web`, e o JDBC só em `store` (`measures/dependencies.md`). A maior classe tem 153 linhas (`PgReservationRepository`).
**Nota:** com problemas

A borda HTTP é fina. Cada rota lê a requisição, chama o serviço e escreve a visão (`web/Api.java:44-58`). A leitura e a validação do corpo ficam em `web/CreateReservationParser.java:22`, e o mapeamento dos erros para o código HTTP fica num lugar só (`web/Api.java:61`, com o status em `domain/ErrorCode.java:4-9`). As regras não conhecem nem o Javalin nem o JDBC: preço (`domain/Pricing.java:10`), transições (`domain/ReservationStatus.java:17`), devolução (`domain/RatePlan.java:22`) e o que fica com a pousada (`domain/Reservation.java:84`). Todo o SQL está em `store/`. A montagem acontece num lugar só (`Main.java:17-29`).

O defeito é o ciclo. `ReservationService` (pacote `domain`) importa `store.ReservationRepository`, `store.MessageLog` e `store.Transactor` (`domain/ReservationService.java:3-5`), e o pacote `store` depende de `domain.Reservation` (`store/PgReservationRepository.java:3-5`). As interfaces que o domínio usa estão no pacote dos dados. Bastaria mudá-las para `domain` para o ciclo sumir. O ciclo é pequeno, entre dois pacotes vizinhos, mas é exatamente o caso que a skill classifica como `com problemas`. Um detalhe menor: `GET /messages` lê o `MessageLog` direto da borda (`web/Api.java:53`), sem passar pelo serviço. Num caso só de leitura, isso é aceitável.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo esqueleto (`CsvRenderer.render`). Nenhum relatório escreve título, cabeçalho ou total por conta própria. A linha de título existe em um lugar só (`report/CsvRenderer.java:12`).
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

O pattern pedido é o Template Method. O agente usou composição, que a referência aceita: um escritor genérico, `CsvRenderer.render` (`report/CsvRenderer.java:10-21`), fixa a sequência título → cabeçalho → linhas → total e recebe as partes que variam pela interface `Report`, com `name`, `period`, `columns`, `rows` e `total` (`report/Report.java:6-18`). As quatro classes, `OccupancyReport`, `RevenueReport`, `TourismTaxReport` e `RatePlanReport`, implementam só esses passos, as duas da parte comum inclusive. A seleção das reservas fica nas definições do catálogo (`report/ReportCatalog.java:27-44`) e nos filtros de cada relatório (`report/TourismTaxReport.java:19`, `report/RevenueReport.java:21`). A ordem vem das consultas (`store/PgReservationRepository.java:112`, `:119`). A linha com a data que o dono quer para o ano que vem entra uma vez só, em `CsvRenderer.render`. Um relatório novo é uma classe a mais e um item em `ReportCatalog.definitions()`. A rota é registrada num laço (`web/Api.java:55-59`). O valor da taxa é configurável (`Main.java:25`) e entra pelo construtor (`report/TourismTaxReport.java:16`). Não é uma classe abstrata com método `final`, por isso a classe é `alternativa equivalente` e não `acerto`.

Exagero: não achei. As interfaces `ReservationRepository`, `MessageLog` e `Transactor` têm uma segunda implementação de verdade, o `InMemoryStore` dos testes (`src/test/java/br/com/mealta/InMemoryStore.java:18`). O `ReportDefinition` (`report/ReportDefinition.java:9`) é um registro de três campos que serve para registrar as rotas e os parâmetros sem repetição. Não há fábricas, builders nem camadas que só repassam.

O resto do código está bem feito. O dinheiro é `BigDecimal` com `HALF_EVEN` num lugar só (`domain/Money.java:16-18`), arredondado a cada passo (`domain/Pricing.java:23`, `domain/RatePlan.java:18`). As datas são `LocalDate`, e situação, plano, tipo e canal são enums. As transições ficam numa tabela no enum (`domain/ReservationStatus.java:17-24`), e a mudança de estado só acontece pelos métodos da reserva (`domain/Reservation.java:53-77`). Os métodos são curtos. Há só miudezas, que não chegam a pedir mudança antes de aceitar:
- a regra "paga" está escrita duas vezes, em Java (`domain/ReservationStatus.java:13-15`) e em SQL (`store/PgReservationRepository.java:21`);
- a soma do que ficou com a pousada se repete (`report/RevenueReport.java:51` e `report/RatePlanReport.java:53`);
- a regra das reservas que contam como dinheiro está num método estático de um relatório concreto que outro relatório usa (`report/RevenueReport.java:21`, chamado em `report/RatePlanReport.java:19`), quando caberia no domínio;
- o `id` da reserva é mutável (`domain/Reservation.java:101`).

## Testes

**Números:** 7 testes rodaram, 0 falharam, 0 foram pulados, e há 7 métodos de teste escritos. A cobertura é de 53,4% das linhas (275 de 515) e 52,1% dos ramos (49 de 94). Por pacote: `domain` 182 de 190 linhas, `report` 93 de 93, e `store` e `web` 0 (`measures/tests.md`). Não há testes pela API nem contra o banco.
**Nota:** com problemas

O que existe confere regras com valores exatos:
- o preço da alta temporada, R$ 599,76, e as bordas da temporada, 14/12, 15/12 e 29/02 (`src/test/java/br/com/mealta/PousadaTest.java:50-58`);
- o desconto com meio para o par, 116,97 (`PousadaTest.java:62-66`);
- o texto inteiro do e-mail (`PousadaTest.java:70-88`);
- as três devoluções (`PousadaTest.java:92-102`);
- as transições, a indisponibilidade e a capacidade, pelo código de erro (`PousadaTest.java:106-119`);
- os quatro CSV comparados por inteiro, com título, a NON_REFUNDABLE cancelada contando no financeiro e no relatório dos planos, e a taxa de 18,00 (`PousadaTest.java:123-156`).

Se alguém tirasse a linha de título, trocasse a ordem dos planos ou deixasse de contar a cancelada não reembolsável, um teste falharia.

As lacunas:
- **Persistência e HTTP sem nenhum teste.** Os testes usam o `InMemoryStore`, que reimplementa em Java as consultas (`InMemoryStore.java:46-62`). As consultas reais de sobreposição, de mês e de ocupação (`store/PgReservationRepository.java:93-121`) e os códigos HTTP e as recusas 400 do parser (`web/CreateReservationParser.java`, `web/Api.java`) ficam sem rede. Um `>=` no lugar de `>` no SQL de sobreposição passaria nos testes.
- **Duas regras novas com pouca rede.** A taxa só é testada com 2 hóspedes × 2 noites (`PousadaTest.java:147`), então trocar `guests` por `nights` na fórmula (`report/TourismTaxReport.java:53`) não seria pego. O caso de um plano sem nenhuma reserva no mês, que o texto pede explicitamente, não tem teste: os dois planos têm uma reserva em `PousadaTest.java:153-154`.

## Banco

**Números:** 2 tabelas (`reservations` e `messages`), as duas com chave primária. O dinheiro está em `numeric(12,2)` em todas as colunas e as datas em `date`. As conferências acusaram: nenhum CHECK sobre `status`, `rate_plan` e `channel`, nenhuma restrição UNIQUE, EXCLUDE ou CHECK, e o esquema criado por `CREATE TABLE` no código, sem migrações (`measures/database.md`). Não há chave estrangeira, porque os quartos não estão no banco.
**Nota:** com problemas

O que está certo:
- os tipos e o `NOT NULL` (`store/Schema.java:13-29`) estão corretos, e há índices para as consultas por quarto e por entrada (`store/Schema.java:30-31`);
- o SQL usa parâmetros em tudo, e as concatenações que o script acusou juntam só constantes (`COLUMNS`, `PAID`, em `store/PgReservationRepository.java:19-21`);
- há um pool Hikari (`store/Database.java:21-27`) e as conexões são devolvidas com try-with-resources;
- a confirmação grava a reserva e o e-mail na mesma transação, porque `withConnection` reaproveita a conexão da transação em curso (`store/Database.java:31-35`, `domain/ReservationService.java:54-57`);
- a regra "um quarto, uma reserva por noite" é protegida por uma trava consultiva do PostgreSQL por quarto, dentro da transação (`store/PgReservationRepository.java:84`, chamada em `domain/ReservationService.java:39`). Essa trava vale entre processos, e o roteiro das reservas simultâneas passou.

Os defeitos:
- **O esquema não sabe evoluir.** `CREATE TABLE IF NOT EXISTS` no código (`store/Schema.java:12-39`) não acrescenta uma coluna numa tabela que já existe, e o próximo pedido vai precisar disso.
- **O banco não protege nada sozinho.** Não há CHECK nas situações e nos planos, nem em `guests >= 1` ou `check_out > check_in`, nem um `EXCLUDE` contra sobreposição. A proteção depende só do código.
- **Os quartos vivem só no código** (`domain/RoomCatalog.java:7-12`), então `reservations.room_number` não tem chave estrangeira. O texto diz que os quartos "quase nunca mudam", o que torna a escolha defensável, mas o banco aceita reserva de um quarto inexistente se alguém escrever direto nele.

## Comportamento

**Números:** 64 de 64 conferências passaram. Parte comum: 46 de 46. Dados depois de reiniciar: 3 de 3. Cenário 09 (relatórios): 13 de 13. Reservas simultâneas: 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Nada falhou. Pela API, o sistema faz o que o dono descreveu:
- os preços com temporada e meio para o par;
- as recusas com os códigos e status certos;
- a confirmação com o e-mail e o comprovante;
- check-in, check-out e as três situações de devolução;
- os dados que continuam depois de reiniciar;
- os quatro relatórios com a linha de título, a taxa de turismo, inclusive num mês vazio, e o relatório por plano com a linha zerada do plano sem reserva.

Em dez rodadas de duas reservas ao mesmo tempo, nunca saíram as duas. Para o dono da pousada, o sistema pode ir para o ar como está.

## Dúvidas

- A classe do pattern: a estrutura cumpre tudo o que a referência pede ao Template Method (esqueleto único, passos por relatório, os quatro dentro), mas por composição, com uma interface e um escritor separado, e não por herança com método-modelo. Por isso dei `alternativa equivalente`. Quem ler "acerto" de forma mais larga poderia dar `acerto`.
- Arquitetura: o único defeito é o ciclo `domain` ↔ `store`, causado só pelo pacote onde estão três interfaces. Segui a regra da skill (ciclo novo entre dois pacotes = `com problemas`), mas é um defeito de pouco peso.
