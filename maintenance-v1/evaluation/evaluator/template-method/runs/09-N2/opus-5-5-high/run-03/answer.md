# Cenário 09

## Visão geral

O agente não fez nada com o desenho. Os dois relatórios novos, `TourismTaxReport` e `RatePlanReport`, são classes escritas inteiras, copiadas do `RevenueReport`: a mesma seleção, o mesmo `StringBuilder`, o mesmo cabeçalho, as mesmas linhas e o mesmo total. A linha de título foi colada à mão nos quatro relatórios, cada um com o nome da pousada escrito outra vez. Não existe esqueleto comum, nem classe abstrata, nem escritor genérico. Por isso cada relatório que o contador ainda vai pedir vai exigir mais uma cópia. O comportamento está bom: os valores, as linhas, a ordem e os títulos conferem com o pedido, e os dois relatórios antigos só mudaram na linha de título. O problema sério é só o de desenho: é uma omissão clara, num pedido que diz com todas as letras que vai crescer.

## Item a item

**O pedido (pede o pattern).** Com o pedido, passam a ser quatro relatórios com os mesmos passos (escolher as reservas, ordenar, título, cabeçalho, linhas, total), e há uma mudança que vale para todos. O agente escreveu cada relatório inteiro:
- `src/main/java/hotel/reports/TourismTaxReport.java:22-54` repete a seleção e a ordenação do financeiro (`:23-31`, cópia de `src/main/java/hotel/reports/RevenueReport.java:24-32`). Depois escreve o título (`:34`), o cabeçalho (`:35`), as linhas (`:37-51`) e o total (`:52`).
- `src/main/java/hotel/reports/RatePlanReport.java:20-56` copia a mesma seleção do `RevenueReport` (`:21-28`) e escreve à mão o título (`:31`), o cabeçalho (`:32`) e o total (`:53-54`).
- A linha de título está copiada em quatro lugares, cada um com a string "Pousada Maré Alta" literal: `src/main/java/hotel/reports/OccupancyReport.java:34`, `src/main/java/hotel/reports/RevenueReport.java:35`, `src/main/java/hotel/reports/TourismTaxReport.java:34` e `src/main/java/hotel/reports/RatePlanReport.java:31`. O nome da pousada já existia como constante em `src/main/java/hotel/reservations/ReservationService.java:12`, e o agente nem a reaproveitou.
- As quatro classes continuam `final` e independentes. Também não há tipo comum: `src/main/java/hotel/web/HttpApi.java:91-106` registra uma rota por relatório, cada uma chamando o `generate` concreto.

Não há método-modelo nem composição com as partes que variam. É exatamente o caso que as instruções chamam de omissão: os quatro relatórios escritos inteiros, cada um com a sua linha de título copiada.
Conclusão: omissão

**O exagero colateral.** Não há. A mudança fora dos relatórios se limita ao necessário para ligar os dois relatórios novos: dois parâmetros a mais no construtor do `HttpApi` (`src/main/java/hotel/web/HttpApi.java:37-52`), as duas rotas (`:99-106`) e a montagem em `src/main/java/hotel/Hotel.java:27-29`. Não foi criada interface, fábrica nem camada nova.
Conclusão: contenção

**O comportamento.** O `mvn -q verify` passa na cópia: 31 testes, nenhuma falha. São 8 em `ReportsTest`, 3 deles novos do agente, e 6 em `HttpApiTest`, 2 deles novos. Subi o serviço na porta 7070 e criei pela API, para julho de 2027, as reservas abaixo, na ordem em que foram criadas:
- id 1: STANDARD, quarto 201, 10→13, 2 hóspedes, confirmada;
- id 2: NON_REFUNDABLE, quarto 101, 05→07, 1 hóspede, confirmada e cancelada;
- id 3: STANDARD, quarto 301, 20→22, 3 hóspedes, pendente;
- id 4: STANDARD, quarto 102, 01→04, 2 hóspedes, com check-out;
- id 5: STANDARD, quarto 202, 15→16, 2 hóspedes, confirmada e cancelada;
- id 6: NON_REFUNDABLE, quarto 201, 25→27, 1 hóspede, com check-in.

O que testei:
- `GET /reports/tourism-tax?month=2027-07` devolveu, depois do título `Pousada Maré Alta,tourism-tax,2027-07` e do cabeçalho `reservation,room,check_in,check_out,guests,nights,tax`, as linhas `4,...,2,3,27.00`, `1,...,2,3,27.00` e `6,...,1,2,9.00`, e no fim `total,63.00`. É o que se esperava: 4,50 × hóspedes × noites, só as reservas pagas (a pendente e as duas canceladas ficam de fora) e na ordem do financeiro.
- `GET /reports/rate-plans?month=2027-07` devolveu `STANDARD,2,2303.28`, `NON_REFUNDABLE,2,1305.20` e `total,4,3608.48`. A cancelada não reembolsável (509,80) entra, a cancelada STANDARD não, e o total bate com o do `/reports/revenue` do mesmo mês (3608.48).
- Em `2027-08`, o relatório de planos traz as duas linhas zeradas na ordem `STANDARD`, `NON_REFUNDABLE` e `total,0,0.00`. O da taxa traz só o cabeçalho e `total,0.00`.
- O occupancy de `2027-07-01` começa com `Pousada Maré Alta,occupancy,2027-07-01`, ou seja, o título leva a data da chamada.
- `month=julho` e a falta do `month` dão 400 `INVALID_REQUEST`.
- Comparei a `base/` com a `final/` repetindo a mesma sequência de chamadas: o comprovante do check-out, o financeiro, a ocupação e a recusa `ROOM_UNAVAILABLE`. A única diferença é a linha de título no começo dos dois relatórios. No fim derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma.
