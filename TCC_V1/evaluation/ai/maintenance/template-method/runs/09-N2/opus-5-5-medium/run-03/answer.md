# Cenário 09

## Visão geral

O agente não fez o Template Method. Nenhuma estrutura reúne o que os relatórios têm em comum. A linha de título foi copiada à mão nos dois relatórios que já existiam, e os dois novos foram escritos inteiros, cada um com a sua própria sequência: escolher as reservas, ordenar, título, cabeçalho, linhas e total. O resultado são quatro classes soltas que repetem o mesmo esqueleto, que é o caso de omissão. Não há estrutura sobrando. O comportamento está quase todo certo, mas a taxa de turismo usa o filtro do financeiro ("a pousada ficou com dinheiro") em vez de "reserva paga". Por isso uma reserva não reembolsável cancelada entra na taxa, e o pedido não a inclui.

## Como foi implementado, e por que o julgamento

O projeto é um serviço Javalin. Os pacotes são estes:

- `hotel.reservations`: o domínio, com `Reservation`, `ReservationService`, `ReservationStore` (os dados ficam em memória), `PriceCalculator` e `RatePlan`.
- `hotel.rooms`: os quartos.
- `hotel.messages`: os e-mails.
- `hotel.documents`: o recibo.
- `hotel.reports`: os relatórios CSV.
- `hotel.web.HttpApi`: as rotas.

A classe `hotel.Hotel` monta tudo à mão e sobe o servidor. A mudança acrescentou dois relatórios a essa montagem (`final/src/main/java/hotel/Hotel.java:28-34`). Uma requisição como `GET /reports/tourism-tax?month=...` cai numa lambda em `HttpApi` (`final/src/main/java/hotel/web/HttpApi.java:99-106`), que lê o mês e chama `generate(month)` no relatório concreto. O relatório percorre `store.all()` e monta o CSV. O `HttpApi` recebe cada relatório como um parâmetro próprio no construtor (`HttpApi.java:39-51`). Os testes do agente ficam todos em `final/src/test/java/hotel/reports/ReportsTest.java`. Ele acrescentou o título às expectativas que já existiam e escreveu três testes novos: a taxa de abril (`:117`), os planos de abril (`:130`) e os planos de maio com uma não reembolsável (`:143`). Não há teste HTTP para as rotas novas.

**A parte que pede o pattern.** São quatro classes `final`, sem tipo comum entre elas:

- `OccupancyReport`: a única mudança foi a linha de título, colada (`final/src/main/java/hotel/reports/OccupancyReport.java:34`).
- `RevenueReport`: também só ganhou o título colado (`final/src/main/java/hotel/reports/RevenueReport.java:35`).
- `TourismTaxReport`: é nova e repete a seleção e a ordenação do financeiro, linha por linha (`final/src/main/java/hotel/reports/TourismTaxReport.java:22-30`). Depois vêm o título (`:33`), o cabeçalho (`:34`), o laço das linhas (`:36-47`) e o total (`:49`).
- `RatePlanReport`: é nova, com a mesma sequência montada à mão (`final/src/main/java/hotel/reports/RatePlanReport.java:25-49`).

A string "Pousada Maré Alta," aparece literal nas quatro classes. Não existe classe abstrata com um método-modelo, nem um escritor genérico que receba as partes que variam. Para cada relatório, quem escolhe o caso é o `HttpApi`, com uma rota e um campo por relatório.

**O raciocínio.** As instruções dizem que é omissão quando os quatro relatórios ficam escritos inteiros, cada um com a sua linha de título, e é exatamente o que está no código. O pedido avisa que vêm mais relatórios e que um dia todos terão uma linha final com a data, "mudar em todos de uma vez". Hoje essa mudança precisaria ser feita em quatro lugares, e a regra de seleção do financeiro já está copiada num segundo arquivo. Também não é malfeito, porque nem uma parte do esqueleto foi para uma estrutura comum. Não há nem uma função utilitária compartilhada. No exagero colateral, não acho nada: a única estrutura nova é a classe interna `ReservationCount` (`RatePlanReport.java:52-55`), um acumulador privado e necessário. No comportamento pesou a seleção da taxa de turismo, explicada no item abaixo.

## Item a item

**O pattern pedido.** Não há esqueleto comum. A linha de título foi colada em `OccupancyReport.java:34` e em `RevenueReport.java:35`. `TourismTaxReport.java:21-51` e `RatePlanReport.java:20-50` repetem a sequência inteira (seleção, ordenação, título, cabeçalho, linhas, total), e a seleção de `TourismTaxReport.java:22-30` é cópia de `RevenueReport.java:24-32`. Não existe classe abstrata, interface nem compositor que reúna os quatro relatórios. Pelas instruções, quatro relatórios inteiros, cada um com o seu título, são omissão.
Conclusão: omissão

**O exagero colateral.** Não há. As classes novas seguem o estilo das que já existiam, e a única peça a mais é o acumulador privado `ReservationCount` (`RatePlanReport.java:52-55`), que tem função de verdade.
Conclusão: contenção

**O comportamento.** Conferi por leitura do código, sem rodar nada.

- Os quatro títulos estão certos: `occupancy` com a noite, `revenue`, `tourism-tax` e `rate-plans` com o mês (`OccupancyReport.java:34`, `RevenueReport.java:35`, `TourismTaxReport.java:33`, `RatePlanReport.java:36`). Fora o título, a ocupação e o financeiro ficaram iguais aos da base.
- Na taxa de turismo, as colunas estão certas (`:34`), o valor é 4,50 × hóspedes × noites (`:14`, `:37-39`), a ordem é a do financeiro (`:30`) e o total vem no fim (`:49`).
- O problema está no filtro da taxa: ele é `amountKept() > 0` (`TourismTaxReport.java:24-27`), e não `isPaid()` (que inclui confirmadas, check-in e check-out, `final/src/main/java/hotel/reservations/Reservation.java:80-84`), como o pedido define. Assim, uma reserva NON_REFUNDABLE cancelada, que fica com o dinheiro (`ReservationService.java:86-90`), entra na taxa. No cenário do teste `ReportsTest.java:90`, a taxa de 2027-05 listaria a reserva 6, que está cancelada, cobrando 4.50, quando deveria vir só o total 0.00.
- O relatório por plano está certo. Ele usa a mesma seleção do financeiro, então inclui a não reembolsável cancelada (`RatePlanReport.java:25-33`). Tem uma linha por plano na ordem do enum, STANDARD e depois NON_REFUNDABLE, mesmo sem reservas (`:22-23`, `:41`, `final/src/main/java/hotel/reservations/RatePlan.java:3-8`). O total vem no formato "total,qtd,valor" (`:48`).
- As rotas novas existem (`HttpApi.java:99-106`).

Por causa do filtro da taxa, o comportamento segue em parte.
Conclusão: em parte

## Dúvidas

Uma só, sobre o filtro da taxa de turismo. O pedido diz "reservas pagas (...) as confirmadas, as que já fizeram check-in e as que já fizeram check-out", e com isso li que uma não reembolsável cancelada fica de fora. A frase "na mesma ordem do relatório financeiro" fala só da ordem. O agente pode ter lido que a seleção também deveria ser a do financeiro, mas o texto define a seleção com todas as letras. Por isso mantive "em parte".
