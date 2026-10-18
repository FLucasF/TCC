# Cenário 09

## Visão geral

O pedido trazia dois relatórios novos, o que faz quatro relatórios com os mesmos passos, e uma linha de título que vale para todos. O agente não criou um esqueleto comum. Escreveu os dois relatórios novos inteiros, cada um repetindo a sequência inteira: seleção, ordenação, `StringBuilder`, título, cabeçalho, linhas e total. Nos dois antigos, só colou mais uma linha de `append` com o título. A linha "Pousada Maré Alta,…" está copiada em quatro lugares, e o laço de seleção do financeiro está copiado em mais dois relatórios. O comportamento está certo em tudo o que testei. O problema sério é de desenho: o pedido diz que o contador vai pedir mais relatórios, e o próximo vai ser mais uma cópia.

## Item a item

**O pedido (pede o pattern).** Não há classe abstrata, método-modelo nem escritor genérico que receba as partes que variam. Os quatro relatórios continuam sendo classes `final` soltas, cada uma com o seu `generate` completo:

- **A linha de título está copiada à mão nos quatro:** `src/main/java/hotel/reports/OccupancyReport.java:34`, `src/main/java/hotel/reports/RevenueReport.java:35`, `src/main/java/hotel/reports/TouristTaxReport.java:34` e `src/main/java/hotel/reports/RatePlanReport.java:41`.
- **Cada um repete a sequência dos passos:**
  - cria o seu `StringBuilder` (`OccupancyReport.java:33`, `RevenueReport.java:34`, `TouristTaxReport.java:33`, `RatePlanReport.java:40`);
  - escreve o cabeçalho, as linhas e o `total,` (`OccupancyReport.java:44`, `RevenueReport.java:47`, `TouristTaxReport.java:54`, `RatePlanReport.java:48`).
- **A seleção também foi copiada:** o filtro "ficou dinheiro e começa no mês" do financeiro (`RevenueReport.java:26-28`) reaparece em `RatePlanReport.java:27-29`. A seleção com `isPaid` e a ordenação por `checkIn` e `id` (`RevenueReport.java:32`) reaparecem em `TouristTaxReport.java:25-31`.
- **O relatório por plano trata cada plano à mão:** `RatePlanReport.java:30-36` usa um `if (ratePlan() == RatePlan.STANDARD) … else`, com dois pares de variáveis.
- **O `HttpApi` ganhou duas rotas copiadas:** `src/main/java/hotel/web/HttpApi.java:99-106` repete as anteriores.

Os quatro relatórios ficaram escritos inteiros, cada um com a sua linha de título copiada. É exatamente o caso que as instruções chamam de omissão.

Conclusão: omissão

**O exagero colateral.** Não há. A mudança fora de `reports/` se resume a passar os dois relatórios novos pelo construtor (`src/main/java/hotel/Hotel.java:27-34`, `src/main/java/hotel/web/HttpApi.java:34-51`) e a registrar as duas rotas. Nenhuma interface, fábrica ou camada nova.

Conclusão: contenção

**O comportamento.** Subi o jar de uma cópia da `final/` na porta 7070 e criei seis reservas, sempre com uma chamada `POST /reservations` seguida de `confirmation`, `cancellation`, `check-in` ou `check-out`:

| Reserva | Quarto | Período | Hóspedes | Plano | Situação final |
|---|---|---|---|---|---|
| 1 | 201 | 10–13/07 | 2 | STANDARD | confirmada |
| 2 | 101 | 05–07/07 | 1 | NON_REFUNDABLE | confirmada e cancelada |
| 3 | 301 | 10–12/07 | 3 | STANDARD | pendente |
| 4 | 102 | 20–22/07 | 2 | STANDARD | confirmada e cancelada (reembolsada) |
| 5 | 202 | 01–03/07 | 3 | NON_REFUNDABLE | check-in e check-out |
| 6 | 202 | 01–03/08 | 2 | STANDARD | confirmada |

Os resultados:

- **`GET /reports/tourism-tax?month=2027-07`:**
  - título `Pousada Maré Alta,tourism-tax,2027-07`, depois `reservation,room,check_in,check_out,guests,nights,tax`;
  - linhas `5,…,3,2,27.00` e `1,…,2,3,27.00`, na ordem do financeiro;
  - `total,54.00`.

  É o esperado: 3×2×4,50 e 2×3×4,50. Ficaram de fora a pendente, a não reembolsável cancelada e a reembolsada.
- **`GET /reports/rate-plans?month=2027-07`:**
  - título `Pousada Maré Alta,rate-plans,2027-07`, depois `rate_plan,reservations,amount`;
  - `STANDARD,1,1403.64` e `NON_REFUNDABLE,2,1305.20`, que conta a não reembolsável cancelada (509.80 + 795.40);
  - `total,3,2708.84`, o mesmo total do financeiro.
- **Mês de agosto em `rate-plans`:** traz `NON_REFUNDABLE,0,0.00`, ou seja, a linha do plano aparece mesmo sem reserva.
- **Mês vazio:** `tourism-tax` de 2027-09 traz só o título, o cabeçalho e `total,0.00`.
- **Relatórios antigos:**
  - `GET /reports/occupancy?date=2027-07-10` começa com `Pousada Maré Alta,occupancy,2027-07-10`, seguido do cabeçalho de antes, da linha do quarto 201 e de `total,1/5`;
  - `GET /reports/revenue?month=2027-07` começa com `Pousada Maré Alta,revenue,2027-07`, seguido de três linhas (5, 2, 1) e `total,2708.84`.

  Depois do título, os dois estão iguais aos de antes.
- **O que o pedido não mudou:** `GET /reports/rate-plans` sem `month` continua dando 400 `INVALID_REQUEST`, e o ciclo de vida das reservas responde como antes.

O `mvn -q verify` da cópia passou: 32 testes, nenhuma falha. São 11 em `ReportsTest`, 7 deles novos do agente para os dois relatórios novos, e os testes antigos só ganharam a linha de título. O serviço foi derrubado no fim.

Conclusão: segue

## Dúvidas

Nenhuma.
