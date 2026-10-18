# Cenário 09

## Visão geral

O agente não criou nenhuma estrutura para os relatórios. Escreveu os dois novos (`TourismTaxReport` e `RatePlansReport`) inteiros, do mesmo jeito que os dois antigos. Também copiou a linha de título, à mão, em cada um dos quatro, com a string `"Pousada Maré Alta,…"` repetida quatro vezes. Agora são quatro classes que repetem a mesma sequência: escolher as reservas, ordenar, título, cabeçalho, linhas e total. O pedido avisa que o contador vai pedir mais relatórios, e mesmo assim não há um esqueleto comum, nem por herança nem por composição. O lado bom é que a mudança é pequena, não cria estrutura sobrando e o comportamento está certo em tudo o que testei. O problema mais sério é o próprio desenho: é exatamente a omissão que o pedido queria evitar.

## Item a item

**O pedido (pede o pattern).** Os quatro relatórios continuam cada um com o seu método `generate` inteiro, sem classe-base nem escritor genérico:

- **Os dois novos:** `final/src/main/java/hotel/reports/TourismTaxReport.java:21-50` e `final/src/main/java/hotel/reports/RatePlansReport.java:19-54`.
- **Os dois antigos:** `final/src/main/java/hotel/reports/OccupancyReport.java:23-46` e `final/src/main/java/hotel/reports/RevenueReport.java:23-49`.

A linha de título, que vale para todos, foi colada em cada um:

- `final/src/main/java/hotel/reports/OccupancyReport.java:34`
- `final/src/main/java/hotel/reports/RevenueReport.java:35`
- `final/src/main/java/hotel/reports/TourismTaxReport.java:32`
- `final/src/main/java/hotel/reports/RatePlansReport.java:30`

A repetição vai além do título:

- **Seleção:** a escolha das reservas do `RatePlansReport` (`RatePlansReport.java:20-27`) é uma cópia literal da do `RevenueReport` (`RevenueReport.java:24-31`).
- **Ordenação:** a do `TourismTaxReport` (`TourismTaxReport.java:29`) repete a do financeiro (`RevenueReport.java:32`).
- **Cabeçalho, linhas e total:** cada classe monta à mão com `StringBuilder`.

O `HttpApi` também ganhou duas rotas copiadas das antigas (`final/src/main/java/hotel/web/HttpApi.java:99-106`). Não existe um lugar só com o esqueleto (título, cabeçalho, linhas, total), e nenhum relatório preenche apenas os passos que mudam. Pelas instruções, quatro relatórios escritos inteiros, cada um com a sua linha de título copiada, é omissão.
Conclusão: omissão

**O exagero colateral.** Não há. Os arquivos novos são só os dois relatórios pedidos. O resto da mudança é ligação em `final/src/main/java/hotel/Hotel.java:28-34` e `final/src/main/java/hotel/web/HttpApi.java:34-51`, mais os testes. Não apareceu interface, fábrica, builder nem camada nova em outra parte do código.
Conclusão: contenção

**O comportamento.** Subi uma cópia da `final/` na porta 7070 e criei oito reservas para julho de 2027 e arredores, com todas as situações relevantes:

| Reserva | Situação |
|---|---|
| 1 | 2 hóspedes, 3 noites, STANDARD, `CONFIRMED` |
| 2 | NON_REFUNDABLE, cancelada |
| 3 | 3 hóspedes, 2 noites, `CHECKED_IN` |
| 4 | `PENDING` |
| 5 | STANDARD, cancelada com reembolso |
| 6 | NON_REFUNDABLE, `CHECKED_OUT` |
| 7 | começa em 2027-06-29 |
| 8 | começa em agosto |

- **`GET /reports/tourism-tax?month=2027-07`**
  - Veio o título `Pousada Maré Alta,tourism-tax,2027-07` e as colunas `reservation,room,check_in,check_out,guests,nights,tax`.
  - Vieram as reservas 6, 1 e 3 na ordem do financeiro, com taxas 18.00, 27.00 e 27.00. O exemplo do pedido (2 hóspedes × 3 noites = 27.00) confere.
  - O total veio `total,72.00`.
  - Ficaram de fora a pendente, as duas canceladas e as que começam fora do mês, como esperado.
  - O tipo de conteúdo é `text/csv; charset=utf-8`.
- **`GET /reports/rate-plans?month=2027-07`**
  - Veio `STANDARD,2,2795.40`, `NON_REFUNDABLE,2,1019.60` e `total,4,3815.00`.
  - Isso inclui a NON_REFUNDABLE cancelada. Bate com o financeiro do mesmo mês (`total,3815.00`).
- **Meses vazios:** em setembro, o relatório de planos traz as duas linhas zeradas (`total,0,0.00`) e o da taxa traz só `total,0.00`.
- **Erros:** `month=julho` e a falta do parâmetro dão `INVALID_REQUEST`, igual ao financeiro.
- **Relatórios antigos:** subi a `base/` na porta 7071 com as mesmas reservas.
  - A ocupação de 2027-07-01, 2027-07-04 e 2027-07-10 é idêntica à da base, depois da linha de título `Pousada Maré Alta,occupancy,<data>`.
  - O financeiro de 2027-06, 2027-07 e 2027-08 também é idêntico, depois de `Pousada Maré Alta,revenue,<mês>`.
- **O que o pedido não mudou:** `GET /reservations/1` e `GET /messages` também são iguais aos da base.
- **`mvn -q verify` na cópia:** passou, com 30 testes e nenhuma falha. São 9 em `ReportsTest`, dos quais 4 o agente acrescentou para os relatórios novos, e o agente atualizou os testes antigos para incluir o título.

No fim, derrubei os dois serviços.
Conclusão: segue

## Dúvidas

Nenhuma. O desenho não deixa margem: os quatro relatórios estão inteiros e o título está copiado em cada um. Não há esqueleto parcial que pudesse ser lido como malfeito.
