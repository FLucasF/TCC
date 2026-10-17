# Cenário 09

## Visão geral

O agente entregou o que o pedido descreve, mas deixou o desenho como estava. Criou duas classes novas, `TourismTaxReport` e `RatePlanReport`, cada uma escrita inteira no mesmo molde das duas antigas: escolher as reservas, ordenar, título, cabeçalho, uma linha por reserva e total. A linha de título, que vale para todos os relatórios, foi copiada à mão nos quatro, com o nome da pousada repetido em cada um. Não há esqueleto comum, nem classe abstrata, nem escritor genérico. O pedido avisa que vêm mais dois relatórios e que a taxa muda todo ano, e mesmo assim o desenho continua sendo "copiar o relatório inteiro". Esse é o problema mais sério. A parte boa é o comportamento: todas as regras que testei conferem, os relatórios antigos não mudaram depois do título e o `mvn verify` passa.

## Item a item

**O pedido (pede o pattern).** São quatro relatórios com os mesmos passos e uma mudança que vale para todos, e o pedido diz que outros virão. Isso pede um Template Method ou um escritor genérico, e o agente não fez nenhum dos dois:
- `final/src/main/java/hotel/reports/TourismTaxReport.java:22-50` e `final/src/main/java/hotel/reports/RatePlanReport.java:20-48` são classes `final` independentes. Cada uma refaz a sequência inteira.
- O filtro de `RatePlanReport.java:21-28` é a mesma cópia do filtro de `RevenueReport.java:24-31`.
- A linha de título está copiada em quatro lugares: `OccupancyReport.java:34`, `RevenueReport.java:35`, `TourismTaxReport.java:33` e `RatePlanReport.java:31`. As quatro repetem a string `"Pousada Maré Alta,"`.
- O cabeçalho e o total também são montados à mão em cada classe: `OccupancyReport.java:35,44`, `RevenueReport.java:36,47`, `TourismTaxReport.java:34,48` e `RatePlanReport.java:32,46`.

Não existe nem uma função utilitária para o título. Os dois relatórios antigos só ganharam uma linha cada (base `OccupancyReport.java:33` e `RevenueReport.java:34`, comparados com a final). As instruções descrevem exatamente isso como omissão: "os quatro relatórios ficam escritos inteiros, cada um com a sua linha de título copiada".

Conclusão: omissão

**O exagero colateral.** Não há. A única mudança fora dos relatórios é passar os dois relatórios novos pelo construtor do `HttpApi` (`final/src/main/java/hotel/web/HttpApi.java:42-51`) e criá-los em `final/src/main/java/hotel/Hotel.java:27-29`. As rotas novas (`HttpApi.java:99-106`) seguem o mesmo jeito das que já existiam. Não surgiu interface, fábrica nem camada nova.

Conclusão: contenção

**O comportamento.** Subi uma cópia da `final/` na porta 7070 e criei sete reservas pela API. Em julho de 2027 ficaram assim:
- a nº 1 é STANDARD, 2 hóspedes, 3 noites, confirmada;
- a nº 2 é NON_REFUNDABLE, confirmada e depois cancelada;
- a nº 3 é STANDARD, 3 hóspedes, 2 noites, com check-in;
- a nº 4 ficou pendente;
- a nº 5 é STANDARD, confirmada e cancelada (com devolução);
- a nº 6 tem 1 hóspede, 5 noites, de 28/07 a 02/08, com check-out.

Fiz também a nº 7, em agosto. O que veio:
- **`GET /reports/tourism-tax?month=2027-07`** veio com `Content-Type: text/csv; charset=utf-8`. O título é `Pousada Maré Alta,tourism-tax,2027-07` e as colunas são `reservation,room,check_in,check_out,guests,nights,tax`. Vieram as linhas `1,…,2,3,27.00`, `3,…,3,2,27.00` e `6,…,1,5,22.50`, e o total `total,76.50`. Era o que eu esperava: 4,50 × hóspedes × noites, só reservas pagas (a 2 cancelada, a 4 pendente e a 5 ficaram de fora), a ordem do financeiro (data de entrada, depois id) e a reserva que entra em julho e sai em agosto contada em julho.
- **`GET /reports/rate-plans?month=2027-07`** trouxe `STANDARD,3,5056.82`, `NON_REFUNDABLE,1,509.80` e `total,4,5566.62`. A cancelada não reembolsável entra, e o total bate com o do financeiro. Em setembro, sem reservas, vieram as duas linhas zeradas e `total,0,0.00`, como pedido.
- **Os relatórios antigos.** O financeiro e a ocupação de 10/07 começam com `Pousada Maré Alta,revenue,2027-07` e `Pousada Maré Alta,occupancy,2027-07-10`, e o resto ficou igual ao formato da base: na ocupação, `201,DELUXE,1,…` e `301,SUITE,3,…` e `total,2/5`.
- **O que não mudou.** O mês inválido nos relatórios novos dá o mesmo erro `INVALID_REQUEST` do financeiro. A data inválida na ocupação e o `CAPACITY_EXCEEDED` ao criar reserva continuam iguais.
- **Os testes.** O `mvn verify` da cópia passou: 28 testes, sem falhas. O agente acrescentou dois testes, um para cada relatório novo, e ajustou os quatro que já existiam para o título.

Derrubei o serviço no fim.

Conclusão: segue

## Dúvidas

Nenhuma.
