```markdown
# Cenário 01

## Visão geral

O agente montou os quatro relatórios como um Template Method: uma classe abstrata `CsvReport` tem o esqueleto (título, cabeçalho, linhas, total) em `render`, e a ocupação, o financeiro, a taxa de turismo e os planos são subclasses que só preenchem o que muda. Entre a base e os três relatórios mensais há uma classe intermediária, `MonthReport`, com o que eles têm em comum: a leitura do mês e a seleção das reservas. A linha de título é escrita num único lugar e a rota HTTP é registrada por um laço sobre a lista de relatórios, sem nenhum `if` por tipo. O valor da taxa vem da configuração. Os quatro relatórios e a parte comum que testei se comportam como o enunciado pede. Não há problema sério. As ressalvas são pequenas: o método-modelo não é `final`, e o passo variável é largo, porque cada subclasse monta numa só chamada o cabeçalho, as linhas e o total.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote raiz `br.com.marealta` tem o domínio:
- `Room` e `RoomType`: os quartos fixos, com capacidade e diária (`Room.java:8-13`, `RoomType.java:6-8`);
- `Season`: a alta temporada e o preço da noite (`Season.java:12-21`);
- `RatePlan`: o desconto e a regra de devolução, guardados no próprio enum (`RatePlan.java:6-23`);
- `Reservation`: calcula noites, subtotal, desconto e total no construtor (`Reservation.java:36-43`) e diz quanto dinheiro ficou com a pousada (`Reservation.java:65-67`);
- `ReservationStatus`: as transições de estado e `isPaid` (`ReservationStatus.java:7-9`);
- `Receipt`: o comprovante e o seu texto (`Receipt.java:20-34`);
- `Outbox`: a lista de mensagens enviadas;
- `Money`: o arredondamento meio para o par e a formatação do dinheiro;
- `ReservationService`: as regras de criação, confirmação, cancelamento, check-in, check-out e segunda via, com o e-mail de confirmação (`ReservationService.java:22-104`).

O pacote `http` tem só `Api`, que registra as rotas Javalin, lê o JSON e converte para o domínio as respostas e os erros, que viram `ApiException` (`Api.java:43-64`). Uma requisição segue este caminho: a rota em `Api` chama o `ReservationService`, que valida, muda a `Reservation` e devolve o resultado, e `Api` o transforma num mapa JSON. Os dados ficam em memória, numa `ArrayList` dentro do serviço (`ReservationService.java:11`). `Main` monta tudo à mão: lê `PORT` e `TOURISM_TAX_RATE`, cria `Outbox`, o serviço e a lista dos quatro relatórios, e sobe a `Api` (`Main.java:15-26`). O jar é sombreado pelo shade com a classe principal no manifesto (`pom.xml:56-58`). Os testes ficam todos em `ApiTest`: quatro testes de ponta a ponta sobre HTTP, que cobrem o preço na alta temporada, o e-mail, os erros e os quatro relatórios com o CSV inteiro comparado.

**A parte do fim.** `Report` é a interface que a `Api` conhece: nome, nome do parâmetro e `render` (`Report.java:4-11`). `CsvReport<P>` é a classe abstrata:
- `render` interpreta o período, pede a tabela à subclasse e escreve, nesta ordem, a linha `Pousada Maré Alta,<nome>,<período>`, o cabeçalho, as linhas e o total (`reports/CsvReport.java:29-39`);
- os passos abstratos são `parse`, `label` e `table` (`reports/CsvReport.java:23-27`);
- `table` devolve um `record Table(header, rows, total)` (`reports/CsvReport.java:9`).

`MonthReport` especializa a base para relatórios mensais. Ela interpreta e valida `month` (`reports/MonthReport.java:20-30`) e oferece duas seleções: `startingIn`, as reservas do mês em ordem de entrada e depois de número (`reports/MonthReport.java:38-43`), e `retainingMoneyIn`, as que deixaram dinheiro com a pousada (`reports/MonthReport.java:46-48`).

Cada um dos quatro relatórios só implementa `table`:
- `OccupancyReport` estende `CsvReport<LocalDate>` direto e tem a sua própria interpretação da data (`reports/OccupancyReport.java:13-50`);
- `RevenueReport` usa `retainingMoneyIn` (`reports/RevenueReport.java:18-29`);
- `TourismTaxReport` recebe o valor por hóspede por noite no construtor e filtra as reservas pagas de `startingIn` (`reports/TourismTaxReport.java:15-35`);
- `RatePlanReport` percorre `RatePlan.values()`, o que dá uma linha por plano mesmo sem reserva (`reports/RatePlanReport.java:20-33`).

O caso concreto é escolhido na montagem, pela lista em `Main.java:21-25`. A `Api` registra uma rota por relatório num laço, sem perguntar qual é qual (`Api.java:59-63`). Um relatório novo é uma subclasse a mais e uma linha a mais nessa lista.

**O raciocínio.** O que as instruções pedem como acerto está aqui:
- uma classe abstrata com o método-modelo e os passos abstratos;
- os quatro relatórios como subclasses, a ocupação e o financeiro inclusive;
- a sequência dos passos e a linha de título só no esqueleto: nenhuma subclasse escreve CSV nem título.

Duas coisas afastam o desenho do ideal, sem mudar a classe. A primeira é que `render` não é `final` (`reports/CsvReport.java:30`), então uma subclasse poderia reescrever o esqueleto. A segunda é que o passo `table` é largo: cada subclasse monta numa só chamada a seleção, o cabeçalho, as linhas e o total, em vez de passos separados. A ordem de escrita, porém, continua toda na base, e o cabeçalho e o total voltam como dados, não como texto. Por isso não é o caso descrito como malfeito, de uma "função utilitária de CSV com cada relatório repetindo a sequência". No exagero colateral pesei o resto do sistema, que é direto: o serviço, o domínio e a `Api`, sem interfaces de uma implementação só, sem fábricas e sem camadas que só repassam. A interface `Report` e a classe intermediária `MonthReport` ficam dentro da parte do fim e têm uso real. No comportamento, todas as regras testadas conferiram.

## Item a item

**O enunciado (pede o pattern).** `CsvReport.render` é o método-modelo e escreve título, cabeçalho, linhas e total (`reports/CsvReport.java:29-39`). Os quatro relatórios são subclasses que só implementam `table`, e os três mensais também reaproveitam a interpretação do mês e a seleção de `MonthReport`: `reports/OccupancyReport.java:37`, `reports/RevenueReport.java:18`, `reports/TourismTaxReport.java:25` e `reports/RatePlanReport.java:20`. A `Api` trata todos de modo uniforme, sem `if` nem `switch` por relatório (`Api.java:59-63`). É o Template Method aplicado a todos os casos, inclusive os dois da parte comum. O único detalhe é o esqueleto não ser `final`.
Conclusão: acerto

**O exagero colateral.** Não há. Fora dos relatórios, `ReservationService` é uma classe concreta usada diretamente (`ReservationService.java:8`). `Season` e `Money` são utilitários estáticos pequenos. `Receipt.of` é um método de fábrica estático de uma linha, não uma fábrica (`Receipt.java:14-18`). `Main` monta tudo sem contêiner nem builder (`Main.java:19-26`).
Conclusão: contenção

**O comportamento.** Rodei numa cópia: `mvn -q package -DskipTests` gerou `reservas-1.0.0.jar`, e o serviço subiu na 7070. Criei seis reservas pelo `POST /reservations`, todas confirmadas menos a 4. A 2 é não reembolsável e foi cancelada depois da confirmação. A 5 é STANDARD e foi cancelada depois da confirmação, com devolução integral. A 3 passou pelo check-in e pelo check-out. A 4 ficou pendente.
- **Título:** os quatro relatórios começam com `Pousada Maré Alta,occupancy,2027-07-10`, `...,revenue,2027-07`, `...,tourism-tax,2027-07` e `...,rate-plans,2027-07`, todos em `text/csv; charset=utf-8`.
- **Taxa de turismo, julho:** o relatório trouxe a 1 com `2,2,18.00` e a 3 com `3,3,40.50`, e `total,58.50`. Ficaram de fora a cancelada e a pendente, como pede o enunciado, e a ordem é a do financeiro. Em agosto, sem reservas, veio o cabeçalho e `total,0.00`.
- **Planos, julho:** `STANDARD,1,599.76`, `NON_REFUNDABLE,2,2386.18` (a não reembolsável cancelada conta) e `total,3,2985.94`, igual ao total do financeiro. Em março, sem reserva não reembolsável, veio `NON_REFUNDABLE,0,0.00`.
- **Financeiro, julho:** saíram a 2 (CANCELLED, 1193.09), a 1 e a 3, nessa ordem, com `total,2985.94`. A 5, com devolução integral, e a 4, pendente, ficaram fora.
- **Ocupação:** em 2027-07-10 veio `101` e `202` com `total,2/5`. Em 2027-07-12 a 101 sai e fica `total,1/5`.
- **Erros dos relatórios:** `month=2027-3`, `date=x` e um parâmetro ausente dão 400 INVALID_REQUEST.
- **Parte comum:** o STANDARD de 10 a 12/07/2027 deu `subtotal 599.76`. O NON_REFUNDABLE de 3 noites deu desconto de 210.55 e total de 1193.09. O e-mail saiu com o canal EMAIL, o assunto certo e o comprovante de março com "2 diárias" e "R$ 779,80". A segunda via da pendente deu 409 OPERATION_NOT_ALLOWED. Também conferiram: ROOM_UNAVAILABLE com 409, CAPACITY_EXCEEDED com 422, um corpo que não é JSON com 400, o cancelamento de uma reserva já com check-out com 409, uma reserva inexistente com 404, e `nightlyRate` como 249.90.
- **Testes do agente:** o `mvn -q verify` passou, com `Tests run: 4, Failures: 0, Errors: 0`. Depois derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma que mude a classe. O ponto mais discutível é o passo `table`, que é largo: cada subclasse monta a seleção, o cabeçalho, as linhas e o total, e só a escrita e a linha de título ficam no esqueleto. Mesmo assim a sequência dos passos não se repete em nenhuma subclasse, e por isso fiquei com `acerto`. O fato de `render` não ser `final` é uma imperfeição, não uma convivência com o jeito sem o pattern.
```
