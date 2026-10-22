# Cenário 09

## Visão geral

O agente aplicou o Template Method nos relatórios. Os quatro (ocupação, financeiro, taxa de turismo e planos) são subclasses de uma classe abstrata `Report`. O esqueleto dela fica num método `final render`, que escreve o título, o cabeçalho, as linhas e o total (`src/main/java/br/com/marealta/reports/Report.java:32-41`). Cada relatório preenche só os passos que mudam: o nome, as colunas, a seleção, a linha e o total. Os dois relatórios do ponto de partida passam pela mesma estrutura que os dois novos, e nenhum deles escreve CSV por conta própria. Existe uma classe intermediária, `MonthlyReport`, mas ela tem motivo: junta a leitura do mês e a seleção por mês, que três relatórios usam. O valor da taxa vem de fora, pelo construtor. Na leitura não achei nenhum desvio das regras pedidas. Não há problema sério.

## Como foi implementado, e por que o julgamento

**O projeto.** São dois pacotes:

- **`br.com.marealta`**, com o domínio e a API:
  - `App` monta as rotas do Javalin, lê o corpo das requisições e escreve o JSON e os erros.
  - `ReservationService` guarda as reservas numa lista em memória, cria a reserva (checando quarto, capacidade e conflito de noites) e dispara as transições.
  - `Reservation` tem a máquina de estados, o cálculo de preço feito no construtor e o `retained()`, que é o pago menos o devolvido.
  - `Pricing` cuida da alta temporada.
  - `RatePlan` é um enum com o desconto e a regra de reembolso.
  - `Receipt` monta o comprovante e o texto dele.
  - `Outbox` é a lista das mensagens "enviadas".
  - `Rooms` e `RoomType` trazem os cinco quartos fixos.
  - `Money` faz o arredondamento HALF_EVEN e os formatos de dinheiro.
- **`br.com.marealta.reports`**, com os relatórios.

O caminho de uma requisição: a rota em `App.java:42-57` chama um método do `ReservationService`, que chama a regra na `Reservation`; o resultado é serializado por `json(...)`. Os erros são `ApiException`, que viram `{error, message}` em `App.java:40`. `Main` lê a variável PORT e usa 7070 se ela não vier (`Main.java:5-6`). O shade plugin gera o jar executável (`pom.xml:61-73`). Os testes são três, de integração por HTTP, em `src/test/java/br/com/marealta/AppTest.java`: preço de alta temporada, confirmação com e-mail e três dos relatórios, e erros com um não reembolsável cancelado no financeiro.

**A parte do pattern.**

- `Report<P, T>` (`reports/Report.java:14`) declara os passos abstratos: `name`, `paramName`, `parse`, `columns`, `select`, `cells` e `total` (linhas 17-30). O esqueleto é o método `public final String render` (linhas 32-41), e o título "Pousada Maré Alta,<nome>,<período>" é escrito só ali (linha 36).
- `MonthlyReport` (`reports/MonthlyReport.java:14`) fica no meio. Ela lê e valida o mês (linhas 28-35) e oferece `startingIn` e `retainingMoney`, que selecionam por mês e ordenam por data de entrada e depois por id (linhas 15-16 e 37-45).
- Os quatro relatórios concretos:
  - `OccupancyReport` estende `Report` direto (`reports/OccupancyReport.java:13`).
  - `RevenueReport` estende `MonthlyReport` (`reports/RevenueReport.java:11`).
  - `TourismTaxReport` estende `MonthlyReport` e recebe o valor da taxa no construtor (`reports/TourismTaxReport.java:11-17`).
  - `RatePlanReport` estende `MonthlyReport` e agrega por plano (`reports/RatePlanReport.java:13-36`).

  Em `App.java:31-35` eles ficam numa lista, e um laço registra todos com `/reports/{name}` (`App.java:52-57`). A rota não pergunta qual é o relatório concreto.

**O raciocínio.**

- **O pattern.** Ele está aplicado como as instruções descrevem: classe abstrata, método-modelo `final`, passos abstratos e os quatro relatórios como subclasses, os dois antigos inclusive. A futura linha com a data de emissão entraria num lugar só, o `render`. Nada afasta o desenho do pattern: nenhum relatório monta o título ou a sequência dos passos por conta própria, e ninguém faz `instanceof` ou `switch` por relatório.
- **O exagero.** A camada `MonthlyReport` tem três usuários e lógica de verdade, então não conta como exagero. Fora dos relatórios também não há interface nem fábrica sem necessidade.
- **O comportamento.** Conferi por leitura, e as regras batem.

## Item a item

**O pattern pedido.** `Report.render` é `final` e tem o esqueleto: título, cabeçalho, linhas e total (`reports/Report.java:32-41`). Os passos são abstratos (`reports/Report.java:17-30`). Os quatro relatórios são subclasses e implementam só os passos (`reports/OccupancyReport.java:13`, `reports/RevenueReport.java:11`, `reports/TourismTaxReport.java:11`, `reports/RatePlanReport.java:13`). O título existe uma vez só (`reports/Report.java:36`). As rotas são registradas de forma genérica (`App.java:52-57`). É o Template Method que o texto pedia, cobrindo também os dois relatórios do ponto de partida.
Conclusão: acerto

**O exagero colateral.** Não há. `MonthlyReport` (`reports/MonthlyReport.java:14-46`) é uma camada a mais, mas guarda lógica que três relatórios compartilham, e não só repassa chamadas. O resto do sistema não tem interfaces com uma implementação só nem fábricas.
Conclusão: contenção

**O comportamento.** Conferi por leitura, sem subir o serviço.

- **Título.** É `INN,name(),period.toString()` (`reports/Report.java:36`). `LocalDate` sai como 2027-07-10 e `YearMonth` como 2027-07. Os nomes são `occupancy`, `revenue`, `tourism-tax` e `rate-plans`.
- **Ocupação.** Entram as reservas pagas que ocupam a noite, contando a entrada e não a saída (`Reservation.java:79-81`), ordenadas por quarto. As colunas estão certas, e a linha final é `total,N/5` (`reports/OccupancyReport.java:39-59`).
- **Financeiro.** Entram as reservas que começam no mês com `retained() > 0`. Isso inclui o não reembolsável cancelado e exclui o normal cancelado com devolução integral. A ordem é por data de entrada e depois por id. As colunas e o total estão certos (`reports/RevenueReport.java:20-38`).
- **Taxa de turismo.** O valor é 4,50 × hóspedes × noites, arredondado (`reports/TourismTaxReport.java:33`, `App.java:23`). Entram as reservas pagas que começam no mês, na ordem do financeiro (`reports/TourismTaxReport.java:29`). As colunas `reservation,room,check_in,check_out,guests,nights,tax` e o total estão certos.
- **Planos.** O relatório usa `RatePlan.values()`, que vem na ordem STANDARD e depois NON_REFUNDABLE, e mostra os dois planos mesmo sem reserva. Ele conta as mesmas reservas do financeiro, por `retainingMoney` (`reports/RatePlanReport.java:30-35`), e a linha final é `total,quantidade,valor` (linhas 44-48).
- **Resto do sistema.** Também por leitura, confere: estados e recusas (`Reservation.java:40-68`), reembolso (`RatePlan.java:21-23`), preço e alta temporada (`Pricing.java:12-29`), comprovante e e-mail (`Receipt.java:19-32`, `ReservationService.java:88-94`), códigos de erro e content-type `text/csv; charset=utf-8` (`App.java:55`).

Conclusão: segue

## Dúvidas

Nenhuma.
