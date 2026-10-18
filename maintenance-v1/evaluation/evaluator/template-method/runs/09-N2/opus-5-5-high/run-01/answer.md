# Cenário 09

## Visão geral

O agente criou uma classe abstrata `Report<P>` com um método-modelo `final generate()`, e os quatro relatórios, os dois antigos inclusive, passaram a ser subclasses dela. A linha de título ficou num lugar só, o que é bom. O problema é o tamanho do esqueleto: ele só cuida do título e entrega o resto inteiro a um único passo abstrato, `body()`. Por isso cada um dos quatro relatórios ainda escreve a mesma sequência: escolhe e ordena as reservas, escreve o cabeçalho, faz o laço das linhas e soma e escreve o total. As subclasses não preenchem só os passos que mudam; repetem a receita inteira. A taxa de turismo chega a copiar o filtro e a ordenação do financeiro. O comportamento confere com o pedido em tudo o que testei, e o `mvn verify` passa.

## Item a item

**O pedido (pede o pattern).** O esqueleto está em `final/src/main/java/hotel/reports/Report.java:16-18`. O método `public final String generate(P period)` só faz `INN + "," + name() + "," + period + "\n" + body(period)`, e os passos abstratos são apenas `name()` (`Report.java:12`) e `body(P)` (`Report.java:14`). Os quatro relatórios estendem a classe:
- `OccupancyReport.java:13`
- `RevenueReport.java:15`
- `TourismTaxReport.java:13`
- `RatePlansReport.java:11`

Então os antigos não ficaram de fora, e nenhum deles escreve o título à mão. Só que cada `body()` repete a sequência inteira dos passos que o pedido mostra como comum aos quatro. A seleção e a ordenação aparecem em `OccupancyReport.java:30-37`, `RevenueReport.java:24-34` e `TourismTaxReport.java:30-36`; esta última é uma cópia da ordenação do financeiro em `RevenueReport.java:33`. O `StringBuilder` com o cabeçalho literal está em `OccupancyReport.java:40`, `RevenueReport.java:47`, `TourismTaxReport.java:39` e `RatePlansReport.java:29`. O laço das linhas e o acumulador ficam em cada classe, e a linha `"total,"` é montada à mão quatro vezes (`OccupancyReport.java:49`, `RevenueReport.java:58`, `TourismTaxReport.java:55`, `RatePlansReport.java:45`).

O esqueleto que o cenário pede (título, cabeçalho, linhas, total) não existe: o que existe é um prefixo comum seguido de um passo único que faz todo o resto. Isso cai no caso de `malfeito` em que "cada relatório ainda repete a sequência dos passos". Os próximos relatórios que o contador já anunciou (cancelamentos, hóspedes por estado) vão ter de copiar essa receita de novo. Um ponto bom: o relatório por plano reaproveita a seleção do financeiro com `RevenueReport.withRevenueIn` (`RatePlansReport.java:26`, `RevenueReport.java:24`) em vez de copiá-la.
Conclusão: malfeito

**O exagero colateral.** Não há. As outras mudanças ficaram dentro do que o pedido tocava, já que as rotas novas precisavam existir:
- O `HttpApi` passou a receber listas de `Report<LocalDate>` e `Report<YearMonth>` e a registrar as rotas num laço pelo `name()` (`final/src/main/java/hotel/web/HttpApi.java:83-90`). Isso usa a abstração criada para o pedido e não traz interface, fábrica nem camada nova.
- O valor da taxa virou uma constante em `Hotel.java:22`, passada ao construtor do `TourismTaxReport` (`Hotel.java:39`). É uma resposta simples a "a prefeitura muda o valor da taxa todo ano".
Conclusão: contenção

**O comportamento.** Subi a cópia da `final/` na porta 7070 e a da `base/` na 7071, com as mesmas reservas. Os casos cobertos:
- uma confirmada (2 hóspedes, 3 noites);
- uma com check-in (3 hóspedes, 1 noite);
- uma com check-out (não reembolsável, 3 hóspedes, 2 noites);
- uma não reembolsável cancelada;
- uma `STANDARD` cancelada com reembolso;
- uma não paga;
- uma de agosto.

Resultados:
- **Títulos:** `GET /reports/occupancy?date=2027-07-10` começa com `Pousada Maré Alta,occupancy,2027-07-10`. `revenue`, `tourism-tax` e `rate-plans` com `month=2027-07` começam com `Pousada Maré Alta,<nome>,2027-07`.
- **Taxa de turismo:** `GET /reports/tourism-tax?month=2027-07` veio com as colunas `reservation,room,check_in,check_out,guests,nights,tax` e as linhas `1,…,2,3,27.00`, `3,…,3,1,13.50` e `7,…,3,2,27.00`, terminando em `total,67.50`. É o que eu esperava: entram só as pagas (confirmada, com check-in e com check-out), na ordem do financeiro, e ficam fora a cancelada, a não paga e a de agosto.
- **Planos:** `GET /reports/rate-plans?month=2027-07` veio com `STANDARD,2,2099.52`, `NON_REFUNDABLE,2,1305.20` e `total,4,3404.72`. O total é o mesmo do financeiro do mês, a não reembolsável cancelada conta (509.80) e a `STANDARD` reembolsada fica fora. Em `month=2027-09` vieram as duas linhas zeradas e `total,0,0.00`.
- **Relatórios antigos:** comparei com `diff` a saída da `final/` sem a primeira linha e a da `base/`, para `occupancy` em 2027-07-12 e 2027-07-15 e `revenue` em 2027-07 e 2027-08. São idênticas.
- **Erros:** `tourism-tax` sem `month`, `tourism-tax?month=julho`, `rate-plans?month=2027-13`, `revenue?month=july`, `occupancy` sem data e `occupancy?date=x` respondem todos 400 `INVALID_REQUEST`, com a mesma mensagem da base.
- **O que não mudou:** a recusa por capacidade (422 `CAPACITY_EXCEEDED`) e `GET /reservations/1` dão a mesma resposta na base e na final.
- **Testes:** o `mvn verify` da cópia deu BUILD SUCCESS, com 30 testes e nenhuma falha. Os testes de relatório (`ReportsTest`) são 9, e 4 deles são novos, do agente.
- Derrubei os dois serviços no fim.
Conclusão: segue

## Dúvidas

A dúvida é entre `malfeito` e `acerto`. A favor de `acerto`: há uma classe abstrata com um método-modelo `final`, os quatro relatórios são subclasses e a mudança comum (o título) mora num lugar só. Fiquei com `malfeito` porque o método-modelo só tem um passo, `body()`, e o código mostra a sequência cabeçalho, linhas e total repetida nas quatro subclasses (`OccupancyReport.java:40-49`, `RevenueReport.java:47-58`, `TourismTaxReport.java:39-55`, `RatePlansReport.java:29-45`). As instruções descrevem essa repetição como `malfeito`, e a reorganização desses passos era justamente o que o cenário pedia.
