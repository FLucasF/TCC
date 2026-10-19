# Cenário 09

## Visão geral

O agente aplicou o Template Method nos quatro relatórios. Ele criou uma classe abstrata `CsvReport<P>` com um método-modelo `final` que monta, sempre na mesma ordem, o título, o cabeçalho, as linhas e o total. A ocupação e o financeiro, que já existiam, foram reescritos como subclasses, e os dois relatórios novos também. Cada subclasse preenche só os passos que mudam: o nome, o cabeçalho, a seleção com a ordenação, as linhas e o total. Nenhuma subclasse monta o CSV por conta própria, e ninguém pergunta qual é o relatório concreto. O comportamento confere em tudo o que testei. A ocupação e o financeiro saem byte a byte iguais aos da base depois da linha de título. Não achei problema sério. Um detalhe pequeno: as rotas novas não ganharam teste HTTP, só teste de unidade.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin dividido por pacotes:
- `hotel.rooms`: os quartos.
- `hotel.reservations`: reserva, preço, regras de estado e o `ReservationStore`, que guarda tudo em memória.
- `hotel.messages`: os e-mails guardados.
- `hotel.documents`: os comprovantes.
- `hotel.reports`: os relatórios CSV.
- `hotel.web.HttpApi`: as rotas.

A classe `Hotel` monta as peças à mão e sobe o servidor. A mudança só passou a construir os dois relatórios novos e a entregá-los ao `HttpApi` (`final/src/main/java/hotel/Hotel.java:27-29`). Uma requisição de relatório chega à rota em `HttpApi`, que lê o parâmetro `month` ou `date` e chama `generate(period)` no relatório. Por exemplo, `final/src/main/java/hotel/web/HttpApi.java:99-106` para as rotas novas. O relatório lê `store.all()` e devolve o texto CSV. Os testes do agente ficam em `ReportsTest`. Ele acrescentou a linha de título nos cinco testes que já existiam e escreveu dois novos, `tourismTaxIsChargedPerGuestPerNight` e `ratePlansSplitTheRevenue` (`final/src/test/java/hotel/reports/ReportsTest.java:103-127`). Não há teste HTTP para `/reports/tourism-tax` nem para `/reports/rate-plans`.

**A parte do pattern.** `final/src/main/java/hotel/reports/CsvReport.java:15-25` é o método-modelo `public final String generate(P period)`. Ele chama `select(period)`, escreve `Pousada Maré Alta,<name()>,<period>`, depois `header()`, uma linha para cada item de `rows(selected)` e `total,` seguido de `total(selected)`. Os cinco passos são abstratos (`CsvReport.java:27-37`). O parâmetro genérico `P` separa o período da ocupação, que é um `LocalDate`, do período dos relatórios mensais, que é um `YearMonth`. Os quatro relatórios estendem a classe:
- `OccupancyReport extends CsvReport<LocalDate>` (`final/src/main/java/hotel/reports/OccupancyReport.java:12`): seleção e ordenação por quarto em `:33-39`, total `n/5` em `:51-53`.
- `RevenueReport extends CsvReport<YearMonth>` (`final/src/main/java/hotel/reports/RevenueReport.java:14`): a seleção das reservas que deixaram dinheiro com a pousada fica num método estático, `keptMoneyIn`, em `:23-29`.
- `TourismTaxReport` (`final/src/main/java/hotel/reports/TourismTaxReport.java:11`): a taxa de R$ 4,50 é uma constante (`:13`), o cálculo por reserva está em `:21-24` e a seleção das reservas pagas, na ordem do financeiro, em `:37-43`.
- `RatePlanReport` (`final/src/main/java/hotel/reports/RatePlanReport.java:12`): reaproveita a seleção do financeiro (`:36`) e gera uma linha para cada valor de `RatePlan.values()` (`:41-44`), o que faz aparecer também o plano sem reservas.

**O raciocínio.** Isso é o que as instruções chamam de acerto: uma classe abstrata com o método-modelo `final` e os passos abstratos, e os quatro relatórios como subclasses, os dois antigos inclusive. A sequência dos passos aparece uma vez só (`CsvReport.java:15-25`). Nenhuma subclasse escreve título, cabeçalho ou a linha `total,` por conta própria. O nome da pousada também mora num lugar só (`CsvReport.java:13`). Um relatório novo, como o de cancelamentos ou o de hóspedes por estado, seria só mais uma subclasse.

Sobre o exagero colateral, não há interface com uma implementação só, fábrica, builder nem camada de repasse. O `HttpApi` recebe cada relatório pelo tipo concreto, como já fazia na base. O genérico `<P>` existe porque os períodos têm tipos diferentes. O método estático `keptMoneyIn` serve para dividir com o relatório por plano a regra "as mesmas reservas do financeiro".

Sobre o comportamento, tudo o que testei bateu: títulos, colunas, valores, ordem, linhas com zero, totais e erros de parâmetro. A ocupação e o financeiro ficaram idênticos aos da base depois do título.

## Item a item

**O pattern pedido.** `final/src/main/java/hotel/reports/CsvReport.java:11` é a classe abstrata. O método-modelo `final` está em `CsvReport.java:15-25` e os passos abstratos `name`, `header`, `select`, `rows` e `total` em `CsvReport.java:27-37`. `OccupancyReport.java:12`, `RevenueReport.java:14`, `TourismTaxReport.java:11` e `RatePlanReport.java:12` estendem a classe, e cada um só implementa os passos. O código da base, onde cada relatório montava o CSV inteiro (`base/src/main/java/hotel/reports/OccupancyReport.java:23-46` e `base/src/main/java/hotel/reports/RevenueReport.java:23-48`), foi substituído e não sobrou. Todos os quatro passam pelo esqueleto, e nenhum usuário pergunta qual é o caso concreto. É o Template Method que o texto pedia.
Conclusão: acerto

**O exagero colateral.** Não há. O que existe a mais tem uso: o genérico `<P>` resolve os dois tipos de período (`CsvReport.java:11`), e `keptMoneyIn` divide a seleção entre o financeiro e o relatório por plano (`RevenueReport.java:23-29`, `RatePlanReport.java:36`). O resto da montagem continua como na base (`Hotel.java:27-29`, `HttpApi.java:37-51`).
Conclusão: contenção

**O comportamento.** Fiz o build de uma cópia da `final/` e de uma cópia da `base/` e subi as duas, nas portas 7070 e 7071. Criei a mesma massa de dados nas duas, em julho de 2027:
- uma reserva STANDARD confirmada, com 2 hóspedes e 3 noites;
- uma NON_REFUNDABLE confirmada e depois cancelada;
- uma STANDARD que foi até o check-out, com 3 hóspedes e 2 noites;
- uma pendente;
- uma STANDARD cancelada com reembolso;
- uma NON_REFUNDABLE confirmada, com 1 hóspede e 1 noite;
- uma em check-in, com 3 hóspedes e 2 noites;
- uma em agosto.

O que veio:
- `GET /reports/tourism-tax?month=2027-07`: título `Pousada Maré Alta,tourism-tax,2027-07` e as colunas `reservation,room,check_in,check_out,guests,nights,tax`. Vieram só as quatro reservas pagas, na ordem do financeiro, com `27.00` (2×3), `27.00` (3×2), `27.00` e `4.50`, e `total,85.50`. As reservas pendente, cancelada com reembolso e não reembolsável cancelada ficaram de fora, como deviam.
- `GET /reports/rate-plans?month=2027-07`: `STANDARD,3,3731.16`, `NON_REFUNDABLE,2,764.70` e `total,5,4495.86`. Isso conta a não reembolsável cancelada e bate com o total do financeiro.
- Em agosto veio `NON_REFUNDABLE,0,0.00`. Em junho vieram as duas linhas com zero e `total,0,0.00`.
- A taxa de junho veio só com o título, o cabeçalho e `total,0.00`.
- A ocupação de `2027-07-10` e o financeiro de `2027-07` vieram com o título `Pousada Maré Alta,occupancy,2027-07-10` e `Pousada Maré Alta,revenue,2027-07`. Depois do título, saíram idênticos aos da base.
- Coisas que o pedido não mudou continuaram iguais nas duas versões: `month=xx` e a falta de `month` ou de `date` dão 400 com a mesma mensagem, e as rotas novas também dão 400 nesses casos. A segunda via do comprovante e `/messages` vieram iguais.
- O `content-type` das rotas novas é `text/csv; charset=utf-8`.
- O `mvn -q verify` da cópia passou com 28 testes, 0 falhas e 0 erros: 7 em `ReportsTest`, 2 deles novos; 11 em `ReservationServiceTest`; 6 em `PriceCalculatorTest`; 4 em `HttpApiTest`.

Os serviços foram derrubados no fim.
Conclusão: segue

## Dúvidas

Nenhuma.
