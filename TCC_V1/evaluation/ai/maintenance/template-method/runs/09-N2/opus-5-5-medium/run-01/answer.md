# Cenário 09

## Visão geral

O agente criou a classe abstrata `CsvReport`, com um método-modelo `final` (título, cabeçalho, linhas, total) e cinco passos abstratos. Os quatro relatórios passaram a ser subclasses dela, inclusive a ocupação e o financeiro, que já existiam. A linha de título é escrita num lugar só, e uma linha nova no fim (a data em que o relatório foi tirado, como o dono pediu) entraria em todos de uma vez. Isso é Template Method aplicado direito. As regras pedidas conferem na leitura do código. O problema mais sério é de robustez e não de desenho: cada relatório guarda o resultado da consulta em campos mutáveis da instância antes de chamar o esqueleto. Como o servidor usa uma instância só de cada relatório, duas requisições ao mesmo tempo podem misturar os dados.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin em pacotes por assunto:
- `hotel.reservations`: o domínio (`Reservation`, `ReservationService`, `PriceCalculator`, `RatePlan`, `ReservationStore`).
- `hotel.rooms`: os quartos.
- `hotel.messages`: os e-mails.
- `hotel.documents`: o recibo.
- `hotel.reports`: os relatórios CSV.
- `hotel.web.HttpApi`: as rotas.

`Hotel` monta tudo à mão. Cria o store e o serviço e entrega ao `HttpApi` uma instância de cada relatório (`final/src/main/java/hotel/Hotel.java:28-34`). Os dados ficam em memória, no `ReservationStore`. Uma requisição de relatório segue este caminho: a rota (`final/src/main/java/hotel/web/HttpApi.java:91-106`) lê `date` ou `month` e chama `xxxReport.generate(param)`. Esse método percorre `store.all()`, filtra, ordena e calcula os totais. Depois chama o `generate()` herdado, que monta o CSV. O agente atualizou os testes de unidade dos dois relatórios antigos para incluir a linha de título e acrescentou quatro testes para os novos em `final/src/test/java/hotel/reports/ReportsTest.java`. Não acrescentou teste HTTP para as rotas novas.

**A parte do pattern.** `CsvReport` (`final/src/main/java/hotel/reports/CsvReport.java:7-29`) declara cinco passos abstratos: `reportName`, `period`, `header`, `dataRows` e `totalRow` (linhas 11-19). O método-modelo `public final String generate()` (linhas 21-28) escreve, nesta ordem:
1. `"Pousada Maré Alta,<nome>,<período>"`, com o nome da pousada numa constante só (linha 9);
2. o cabeçalho;
3. as linhas;
4. o total.

Cada caso é uma subclasse, e a escolha do caso é a rota que chama a instância certa. Não há `if` nem `switch` sobre o tipo:
- `OccupancyReport extends CsvReport` (`final/src/main/java/hotel/reports/OccupancyReport.java:13`);
- `RevenueReport extends CsvReport` (`final/src/main/java/hotel/reports/RevenueReport.java:15`);
- `TourismTaxReport extends CsvReport` (`final/src/main/java/hotel/reports/TourismTaxReport.java:13`);
- `RatePlansReport extends CsvReport` (`final/src/main/java/hotel/reports/RatePlansReport.java:14`).

Cada subclasse preenche só o que muda: o nome, o período, as colunas, o formato da linha e o total. O valor da taxa de turismo é uma constante do relatório dela (`TourismTaxReport.java:15`).

**O raciocínio.** O que aproxima do pattern:
- o esqueleto existe num lugar só e é `final`;
- os quatro relatórios passam por ele, os antigos inclusive;
- nenhum relatório escreve mais a própria linha de título nem a sequência cabeçalho→linhas→total.

Isso é exatamente o "acerto" das instruções. O que afasta um pouco:
- A seleção e a ordenação ficaram fora do esqueleto. Cada `generate(param)` ainda faz à mão o laço sobre `store.all()`, o filtro e a ordenação (`RevenueReport.java:26-43`, `TourismTaxReport.java:26-42`).
- Os passos se comunicam com o método-modelo por estado mutável da instância (`OccupancyReport.java:17-18`, `RevenueReport.java:18-20`, `TourismTaxReport.java:18-20`, `RatePlansReport.java:17-20`), em vez de receberem os dados como parâmetro.
- O `generate()` sem parâmetro fica público nas subclasses e, chamado sozinho, daria `NullPointerException`.

Nada disso faz o esqueleto conviver com um jeito sem ele: é o mesmo pattern com uma costura menos limpa. Por isso a classe é acerto. No exagero, não há nada fora do pattern. No comportamento, as regras conferem. O estado mutável compartilhado fica anotado como risco.

## Item a item

**O pattern pedido.** A classe abstrata `CsvReport` tem o método-modelo `final` `generate()` (`final/src/main/java/hotel/reports/CsvReport.java:21-28`) e os passos abstratos (`CsvReport.java:11-19`). Os quatro relatórios são subclasses que só implementam os passos: `OccupancyReport.java:13`, `RevenueReport.java:15`, `TourismTaxReport.java:13` e `RatePlansReport.java:14`. O título sai só do esqueleto (`CsvReport.java:23`). Uma linha nova no fim de todos os relatórios exigiria mudar apenas `CsvReport.generate()`. A seleção e a ordenação continuam em cada subclasse (`RevenueReport.java:26-36`), e os passos recebem os dados por campos mutáveis. São imperfeições de costura, não um caminho paralelo ao pattern.
Conclusão: acerto

**O exagero colateral.** Não há. Nenhuma interface, fábrica ou camada nova além da classe abstrata. A montagem continua manual em `Hotel.java:28-34`, e o `HttpApi` só ganhou dois parâmetros e duas rotas (`HttpApi.java:99-106`). `PlanStats` (`RatePlansReport.java:90-93`) é um acumulador privado e pequeno, que se justifica.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código, sem subir o serviço nem rodar testes.
- **Título:** `"Pousada Maré Alta,<nome>,<período>"` (`CsvReport.java:23`). Os nomes são `occupancy`, `revenue`, `tourism-tax` e `rate-plans` (`OccupancyReport.java:39-41`, `RevenueReport.java:47-49`, `TourismTaxReport.java:53-55`, `RatePlansReport.java:58-60`). O período é a data ou o mês da chamada.
- **Taxa de turismo:**
  - filtra `isPaid()`, que cobre CONFIRMED, CHECKED_IN e CHECKED_OUT (`final/src/main/java/hotel/reservations/Reservation.java:80-84`), com a reserva começando no mês (`TourismTaxReport.java:30-31`);
  - ordena como o financeiro, por check-in e depois id (`TourismTaxReport.java:35`, igual a `RevenueReport.java:36`);
  - calcula 4,50 × hóspedes × noites com escala 2 (`TourismTaxReport.java:46-49`); 2 hóspedes por 3 noites dão 27.00;
  - tem as colunas pedidas (`TourismTaxReport.java:64`) e o total `total,<valor>` (`TourismTaxReport.java:84-86`).
- **Planos:**
  - usa o mesmo critério do financeiro, `amountKept() > 0` e começo no mês (`RatePlansReport.java:34-40`), o que inclui a não reembolsável cancelada;
  - escreve uma linha por plano na ordem do enum, STANDARD e depois NON_REFUNDABLE, mesmo zerada, porque todos os planos começam com 0 (`RatePlansReport.java:29-31`, `76-81`);
  - termina com `total,<qtd>,<valor>` (`RatePlansReport.java:85-87`).
- **Ocupação e financeiro:** depois do título, cabeçalho, linhas e total são idênticos aos da base. O teste HTTP antigo, que confere só o fim do CSV (`final/src/test/java/hotel/web/HttpApiTest.java:113-115`), continua compatível.

Fica uma ressalva fora das regras pedidas. As instâncias são únicas no servidor e guardam o resultado da consulta em campos (`RevenueReport.java:18-20` e equivalentes), então requisições simultâneas ao mesmo relatório podem trocar dados entre si. Em uso sequencial, tudo confere.
Conclusão: segue

## Dúvidas

Dois pontos ficaram em aberto. O primeiro é a seleção e a ordenação, que ficaram fora do método-modelo. As instruções descrevem o esqueleto como "título, cabeçalho, linhas, total", então não tirei isso do acerto, mas a repetição do laço de seleção em cada subclasse é uma brecha. O segundo é a condição de corrida causada pelo estado mutável compartilhado entre requisições. Ela não aparece nas regras pedidas, e por isso mantive o comportamento em `segue`. Num uso com requisições concorrentes, ela poderia justificar `em parte`.
