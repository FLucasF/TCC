Terminei de ler o código; segue a correção.

# Cenário 09

## Visão geral

O agente montou os quatro relatórios em CSV com um Template Method de verdade. A classe abstrata `Report` guarda o esqueleto: título com o nome da pousada, cabeçalho, linhas e total. Os quatro relatórios são subclasses dela, inclusive a ocupação e o financeiro, que já existiam antes da parte nova. Cada subclasse preenche só o nome, o período, o cabeçalho, as linhas e o total. Nenhum relatório monta a linha de título por conta própria, e quem chama não pergunta qual é o relatório concreto. Há três ressalvas pequenas no desenho: o `generate()` não é `final`; a escolha e a ordenação das reservas não viraram passos do esqueleto, então cada subclasse busca as reservas duas vezes; e o `formatAmount` está copiado em três subclasses. O problema mais sério está no comportamento. Os valores em dinheiro saem com `BigDecimal.toString()`, e por isso o zero sai como `0` e não como `0.00`. Isso aparece, por exemplo, na linha de um plano sem reservas no relatório por plano (`STANDARD,0,0`) e no total de um mês vazio.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica em `com.pousada`, separado em pacotes:

- **`domain`**: as classes de domínio. `Reservation` calcula noites, subtotal, desconto, total, valor líquido e as transições de estado. Também ficam aqui `Room`, `RoomType` (com capacidade e diária), `RatePlan` (com o percentual de desconto e se é reembolsável), `ReservationStatus` e `Message`.
- **`repository`**: listas em memória. `ReservationRepository` tem as consultas `findPaidByNight` e `findByCheckInMonth`. `RoomRepository` já nasce com os cinco quartos. Há também `MessageRepository`.
- **`service`**: as regras. `ReservationService` cria a reserva, checa capacidade e disponibilidade e faz as transições. `MessageService` monta o comprovante e o e-mail. `ReportService` cria o relatório certo e chama `generate()`. Aqui também ficam as exceções de negócio.
- **`api`**: `ApiController` registra as rotas, faz as validações de entrada e liga cada exceção a um código HTTP. Os DTOs ficam em `api/dto`.
- **`report`**: o esqueleto e os quatro relatórios.

O caminho de uma requisição é este. A rota em `ApiController` lê o corpo ou a query e valida. Depois chama o serviço, que usa `Reservation` e os repositórios, e a resposta sai como DTO JSON ou como texto CSV.

O sistema sobe pelo `Main`, que monta os objetos à mão, configura o Jackson e escuta em `PORT` ou na 7070 (`Main.java:15-56`). Os dados ficam só na memória. Os testes do agente são só os de `ReservationTest`: oito testes de domínio, sobre preço, alta temporada, desconto, confirmação e cancelamento. Não há nenhum teste de API nem de relatório.

**A parte que pede o pattern.** O esqueleto está em `report/Report.java:8-22`. O método `generate()` escreve o título (`buildTitleLine`, privado, `Report.java:30-32`, que monta `"Pousada Maré Alta,<nome>,<período>"`), depois o cabeçalho, uma linha para cada item de `buildDataLines()` e, no fim, `buildTotalLine()`. Os passos abstratos são `getReportName`, `getPeriod`, `buildHeaderLine`, `buildDataLines` e `buildTotalLine` (`Report.java:24-28`). As quatro subclasses:

- `OccupancyReport` (`OccupancyReport.java:13`): `findPaidByNight`, ordenado pelo número do quarto (`:43-46`), e total `total,ocupados/5` (`:61-65`);
- `RevenueReport` (`RevenueReport.java:13`): reservas do mês com valor líquido maior que zero, ordenadas por entrada e depois por id (`:40-45`);
- `TourismTaxReport` (`TourismTaxReport.java:13`): taxa de R$ 4,50 numa constante (`:14`), reservas `isPaid` do mês, na mesma ordem do financeiro (`:44-47`), e o cálculo em `calculateTax` (`:74-78`);
- `RatePlansReport` (`RatePlansReport.java:15`): agrupa por plano e percorre `RatePlan.values()`, o que garante STANDARD e depois NON_REFUNDABLE, mesmo sem reservas (`:44-60`).

`ReportService` só cria a subclasse certa e chama `generate()` (`ReportService.java:20-38`). As quatro rotas mandam o resultado como `text/csv; charset=utf-8` (`ApiController.java:206-264`).

**O raciocínio.** Os pontos que aproximam o desenho do pattern:

- o esqueleto, com título, cabeçalho, linhas e total, está num lugar só (`Report.java:8-22`);
- o título é privado da base, então nenhuma subclasse o repete;
- os quatro relatórios, os antigos inclusive, herdam de `Report`;
- uma linha final com a data, que o dono já anunciou para o ano que vem, entraria só em `Report.generate()`.

Os pontos que afastam um pouco:

- `generate()` não é `final` (`Report.java:8`), e as instruções dizem "de preferência `final`";
- "escolher" e "ordenar" ficaram dentro de `buildDataLines`, não como passos do esqueleto. Por isso cada subclasse repete a busca em `buildTotalLine` (por exemplo `RevenueReport.java:40` e `:62`, `TourismTaxReport.java:42` e `:66`, `OccupancyReport.java:43` e `:62`);
- `formatAmount` está copiado em três subclasses (`RevenueReport.java:69`, `TourismTaxReport.java:80`, `RatePlansReport.java:80`).

Nada disso põe a sequência dos passos de volta nas subclasses, nem deixa um relatório fora do esqueleto. Por isso a classe é **acerto**.

No exagero colateral, não há interface com uma implementação só, nem fábrica, nem builder. `ReportService` e `RoomService` são camadas finas, mas pequenas e no padrão do resto do projeto. Isso não chega a ser exagero.

No comportamento, as regras centrais estão certas, mas o formato do dinheiro no CSV falha quando o valor é zero (detalhes no item a item).

## Item a item

**O pattern pedido.** `Report` é abstrata, com o método-modelo `generate()` (`report/Report.java:8-22`) que fixa a ordem: título, cabeçalho, linhas, total. O título é montado numa só linha privada (`Report.java:30-32`). Os passos variáveis são abstratos (`Report.java:24-28`). Os quatro relatórios são subclasses: `OccupancyReport.java:13`, `RevenueReport.java:13`, `TourismTaxReport.java:13` e `RatePlansReport.java:15`. Os dois que já existiam estão incluídos. Quem chama usa só o tipo `Report` e `generate()` (`service/ReportService.java:20-38`). É o Template Method que o texto pedia. As ressalvas são menores: o `generate()` não é `final`, a seleção é repetida dentro de cada subclasse (entre `buildDataLines` e `buildTotalLine`) e o `formatAmount` foi copiado. Nenhuma delas tira relatório do esqueleto.
Conclusão: acerto

**O exagero colateral.** Não há. Não existe interface sem substituto, fábrica nem builder. `ReportService` (`service/ReportService.java:10-39`) só cria o relatório e chama `generate()`, e `RoomService` repassa ao repositório. São camadas finas, mas coerentes com o resto do projeto, e não chegam a pesar.
Conclusão: contenção

**O comportamento.** Conferi por leitura do código; não subi o serviço nem rodei testes. O que confere:

- **Títulos.** Os quatro saem como `Pousada Maré Alta,<nome>,<período>`, com `occupancy`, `revenue`, `tourism-tax` e `rate-plans`, e o período é o da chamada (`OccupancyReport.java:26-33`, `RevenueReport.java:23-30`, `TourismTaxReport.java:25-32`, `RatePlansReport.java:25-32`).
- **Taxa de turismo.** Calcula 4.50 × hóspedes × noites com escala 2, então 2 × 3 dá `27.00`. Usa só reservas pagas que começam no mês, na ordem do financeiro, com as colunas pedidas (`TourismTaxReport.java:14`, `:36`, `:44-47`, `:74-78`).
- **Relatório por plano.** Usa o mesmo filtro do financeiro, valor líquido maior que zero. Isso inclui a não reembolsável cancelada, porque o valor líquido é pago menos devolvido (`Reservation.java:210-216`, `RatePlansReport.java:45`). Sempre sai uma linha por plano, na ordem do enum (`:48-60`).
- **Ocupação e financeiro.** Seguem o ponto de partida (`OccupancyReport.java:36-65`, `RevenueReport.java:32-67`).
- **Resto do sistema.** Preços com HALF_EVEN, as transições, o reembolso, os códigos de erro e o e-mail também conferem por leitura (`Reservation.java:102-202`, `ApiController.java:43-65`, `MessageService.java:121-172`).

O que não confere:

1. **Dinheiro zero no CSV.** O dinheiro sai com `BigDecimal.toString()` (`RatePlansReport.java:80-82`, `RevenueReport.java:69-71`, `TourismTaxReport.java:80-82`). Somar a partir de `BigDecimal.ZERO` sem nenhum item dá `0`, não `0.00`. Assim, um plano sem reservas sai `STANDARD,0,0` e não `STANDARD,0,0.00`. Um mês vazio sai `total,0` no financeiro e na taxa e `total,0,0` no relatório por plano, quando o enunciado pede os valores "como no financeiro (27.00)".
2. **Dinheiro zero no JSON.** No mesmo espírito, `discount` do plano STANDARD e `amountPaid` antes da confirmação são `BigDecimal.ZERO` (`Reservation.java:42`, `:121`). Saem no JSON como `0`, não com 2 casas.
3. **Espaço no "R$".** O comprovante do e-mail usa `NumberFormat.getCurrencyInstance(pt-BR)` (`MessageService.java:115`). No Java 21, esse formato costuma pôr um espaço não separável (U+00A0) depois de "R$", diferente do espaço comum do modelo do enunciado.
4. **Id não numérico.** `Long.parseLong` sem tratamento (`ApiController.java:140` e outras) deve virar 500. O enunciado não especifica esse caso.

As regras de negócio estão certas, mas o formato pedido para os valores falha nos casos com zero, inclusive nos dois relatórios novos.
Conclusão: em parte

## Dúvidas

- Não está decidido se o corpo que não é JSON vira 400. `ctx.bodyAsClass` (`ApiController.java:77`) lança uma exceção do Jackson que não é `IllegalArgumentException` nem `ValidationException`, e o `catch` (`:105-109`) parece não pegá-la. Então provavelmente sai 500 em vez de `INVALID_REQUEST`, mas só rodando para confirmar, o que esta correção não permite.
- O espaço não separável no "R$" depende dos dados de locale do JDK em uso. Foi apontado como provável, não como certo.
