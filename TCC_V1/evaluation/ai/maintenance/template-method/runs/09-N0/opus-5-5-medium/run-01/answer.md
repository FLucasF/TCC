# Cenário 09

## Visão geral

O agente pôs a linha de título nos dois relatórios que já existiam e criou os dois novos (`TourismTaxReport` e `RatePlanReport`). Cada um dos quatro foi escrito inteiro, como classe solta: cada um filtra, ordena, monta o cabeçalho, as linhas e o total no seu próprio `generate`, e cada um tem a sua cópia da linha de título. Não há esqueleto comum, nem classe abstrata, nem escritor genérico. Na parte nova o agente seguiu o caminho da cópia por caso, e isso é a omissão do pattern. O comportamento quase todo confere, mas o relatório da taxa de turismo escolhe as reservas pelo critério do financeiro (`amountKept() > 0`) em vez de "pagas" (confirmada, check-in, check-out). Por isso ele também cobra a taxa de uma reserva não reembolsável cancelada.

## Como foi implementado, e por que o julgamento

**O projeto.** O sistema é um serviço Javalin. `hotel.Hotel` monta tudo: `Rooms`, `ReservationStore` (em memória), `ReservationService` (com `PriceCalculator` e `EmailSender`) e os relatórios, que são entregues ao `HttpApi` (`final/src/main/java/hotel/Hotel.java:25-29`). O `HttpApi` registra as rotas. As de relatório leem `date` ou `month` da query e devolvem o texto CSV que o `generate` do relatório produz (`final/src/main/java/hotel/web/HttpApi.java:91-106`). O pacote `hotel.reports` agora tem quatro classes `final`, independentes, sem nenhum tipo em comum. O agente acrescentou testes em `ReportsTest`: título nos dois relatórios antigos, valores da taxa de turismo, mês vazio, os dois planos sempre presentes. No `HttpApiTest`, conferiu o título da ocupação e do financeiro.

**A parte que pede o pattern.** Os quatro relatórios repetem a mesma sequência à mão:
- `OccupancyReport`: filtra (`final/src/main/java/hotel/reports/OccupancyReport.java:24-30`), ordena (`:31`), escreve o título (`:34`), o cabeçalho (`:35`), as linhas (`:36-43`) e o total (`:44`).
- `RevenueReport`: filtra (`final/src/main/java/hotel/reports/RevenueReport.java:24-31`), ordena (`:32`), escreve o título (`:35`), o cabeçalho (`:36`), as linhas e a soma (`:38-46`) e o total (`:47`).
- `TourismTaxReport`: copia o filtro e a ordenação do financeiro (`final/src/main/java/hotel/reports/TourismTaxReport.java:22-31`), depois o título (`:34`), o cabeçalho (`:35`), as linhas (`:37-51`) e o total (`:52`).
- `RatePlanReport`: copia o mesmo filtro (`final/src/main/java/hotel/reports/RatePlanReport.java:20-28`), conta os planos com um `if/else if` por plano (`:35-43`) e escreve título, cabeçalho, uma linha por plano e total (`:49-53`).

A string `"Pousada Maré Alta,<nome>,"` aparece quatro vezes, uma em cada classe. Cada caso é "escolhido" pela rota que chama a classe concreta. Nenhum ponto do código diz quais são os passos de um relatório.

**O raciocínio.** As instruções pedem o esqueleto (título, cabeçalho, linhas, total) num lugar só, com cada relatório preenchendo só os passos que mudam. Elas aceitam uma classe abstrata com subclasses ou um escritor genérico composto. Aqui não existe nem uma coisa nem outra: os quatro ficaram escritos inteiros, cada um com a sua linha de título, que é exatamente a definição de omissão das instruções. O pedido avisava que todos vão ganhar uma linha de data no fim. Com esse desenho, essa mudança terá de ser repetida em cada classe. Não pesa nenhum exagero: o agente não criou interfaces, fábricas nem camadas. Pesa no comportamento o filtro da taxa de turismo, explicado abaixo.

## Item a item

**O pattern pedido.** Os quatro relatórios são classes independentes, sem superclasse nem escritor comum (`final/src/main/java/hotel/reports/OccupancyReport.java:13`, `RevenueReport.java:15`, `TourismTaxReport.java:11`, `RatePlanReport.java:11`). Cada `generate` refaz a sequência inteira e tem a sua cópia do título (`OccupancyReport.java:34`, `RevenueReport.java:35`, `TourismTaxReport.java:34`, `RatePlanReport.java:49`). O filtro "começa no mês e ficou dinheiro" foi copiado três vezes (`RevenueReport.java:24-31`, `TourismTaxReport.java:22-30`, `RatePlanReport.java:20-28`). Não há esqueleto em lugar nenhum. A variação foi resolvida com uma cópia por caso.
Conclusão: omissão

**O exagero colateral.** Não há. As mudanças fora dos relatórios só passam as duas classes novas ao `HttpApi` e registram as rotas (`final/src/main/java/hotel/Hotel.java:28-29`, `final/src/main/java/hotel/web/HttpApi.java:99-106`).
Conclusão: contenção

**O comportamento.** A conferência foi feita só pela leitura do código: não subi o serviço nem rodei testes.
- **Títulos:** os quatro começam com `Pousada Maré Alta,<nome>,<período>`, com os nomes pedidos e o período da chamada (`OccupancyReport.java:34`, `RevenueReport.java:35`, `TourismTaxReport.java:34`, `RatePlanReport.java:49`). Depois do título, a ocupação e o financeiro estão iguais aos da base.
- **Taxa de turismo:** as colunas, a fórmula de R$ 4,50 × hóspedes × noites com duas casas decimais, a ordem do financeiro e a linha `total,<valor>` estão certas (`TourismTaxReport.java:13`, `:31`, `:35-52`). A seleção, porém, usa `amountKept().signum() > 0` (`TourismTaxReport.java:24`) em vez de `isPaid()` (confirmada, check-in ou check-out, `final/src/main/java/hotel/reservations/Reservation.java:80-84`). No cancelamento de uma reserva não reembolsável o reembolso é zero (`final/src/main/java/hotel/reservations/ReservationService.java:86-90`), então ela fica com `amountKept > 0` e entra no relatório como se fosse paga, com taxa cobrada. O pedido restringe às pagas. Os testes do agente não cobrem esse caso.
- **Planos de tarifa:** sai uma linha para `STANDARD` e depois uma para `NON_REFUNDABLE`, mesmo sem reserva. A seleção é a mesma do financeiro, então inclui a não reembolsável cancelada. A linha `total,<quantidade>,<valor>` está certa (`RatePlanReport.java:20-53`).
- **Rotas:** `/reports/tourism-tax` e `/reports/rate-plans` usam o mesmo `readMonth`, com o mesmo 400 quando o mês é inválido (`HttpApi.java:99-106`).

Fica um erro de regra num dos relatórios novos.
Conclusão: em parte

## Dúvidas

Na taxa de turismo, o desvio só aparece quando há uma reserva não reembolsável cancelada no mês. É possível que o agente tenha entendido "pagas" como "das quais ficou dinheiro". O pedido, porém, enumera os estados (confirmadas, com check-in e com check-out), e por isso contei o desvio como erro de comportamento.
