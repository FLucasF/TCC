# Cenário 01

## Visão geral

O agente montou um sistema em camadas (controllers, services, domínio, repositórios em memória) que funciona bem no essencial. Os quatro relatórios saem com o título certo e os valores certos quando há dados. O desenho da parte do fim, porém, não tem Template Method nem composição equivalente. Existe uma interface `ReportGenerator` com um único método `generate`, e cada uma das quatro implementações escreve o relatório inteiro: monta o `StringBuilder`, copia a linha de título, o cabeçalho, o laço das linhas e a linha do total. A sequência de passos está repetida quatro vezes, e a linha "Pousada Maré Alta,…" aparece copiada em cada classe. Esse é o problema mais sério. No comportamento há dois defeitos. Com valor zero, os relatórios escrevem `0` em vez de `0.00`, por exemplo `NON_REFUNDABLE,0,0` e `total,0`. E na parte comum, um corpo que não é JSON ou uma data fora do formato dá 500 em vez de 400.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está em `com.pousadamarealta`:
- `domain`: `Reservation` com o cálculo das noites, do subtotal, do desconto e do total, e a máquina de estados; `RatePlan` com `StandardRatePlan` e `NonRefundableRatePlan`, criados pela `RatePlanFactory`; `SeasonCalculator`; `RoomType` com capacidade e diária; `Message`.
- `repository`: mapas em memória com contador de id. `RoomRepository` já nasce com os cinco quartos (`repository/RoomRepository.java:11-17`).
- `service`: `ReservationService` (validações e transições, traduzidas em exceções próprias), `DocumentService` (o comprovante e o texto do e-mail), `MessageService` (guarda o e-mail) e `ReportService`, junto com o subpacote `service/report`.
- `api`: controllers, DTOs em `record` e mappers estáticos.

`App.java:16-59` monta tudo à mão, configura o Jackson, lê `PORT` e registra as rotas. Uma requisição vai da rota ao método do controller, que valida o corpo (`ReservationController.java:152-174`), chama o service, que aplica a regra no `Reservation`, e devolve o DTO montado pelo mapper. O jar sobe com `java -jar` porque o manifesto aponta `lib/` e o `maven-dependency-plugin` copia as dependências para lá (`pom.xml`). O agente escreveu 11 testes de unidade, só de domínio (`RatePlanTest`, `ReservationTest`, `SeasonCalculatorTest`). Nenhum teste cobre relatórios nem a API.

**A parte do fim (os quatro relatórios).**
- **O contrato.** `ReportGenerator` (`service/report/ReportGenerator.java:8-10`) só declara `String generate(List<Reservation>, List<Room>, String)`.
- **A escolha do relatório.** `ReportService` guarda as quatro implementações num `Map` por nome (`ReportService.java:20-23`) e escolhe pelo nome (`ReportService.java:27`). O controller tem um método por relatório, e esses métodos são cópias entre si (`ReportController.java:18-84`), cada um passando o nome fixo como string.
- **O corpo de cada relatório.** Cada implementação escreve o relatório inteiro:
  - `OccupancyReportGenerator.java:15-40`: filtra, ordena e cria o `StringBuilder`; o título está em `:22`, o cabeçalho em `:23`, o laço em `:25-36` e o total em `:38`.
  - `RevenueReportGenerator.java:17-46`: título em `:28`, cabeçalho em `:29`, laço em `:32-42`, total em `:44`.
  - `TourismTaxReportGenerator.java:19-52`: título em `:30`, taxa fixa em constante em `:13`, cálculo em `:35-37`.
  - `RatePlansReportGenerator.java:18-53`: título em `:30`, laço por `RatePlanType.values()` em `:36-49`, total em `:51`.
- **O que mais se repete.** A seleção do financeiro está copiada no de planos (`RevenueReportGenerator.java:17-22` e `RatePlansReportGenerator.java:18-23`). A ordem do financeiro está copiada no da taxa (`TourismTaxReportGenerator.java:25-26`). O `formatMoney` existe em três cópias (`RevenueReportGenerator.java:49-51`, `TourismTaxReportGenerator.java:55-57`, `RatePlansReportGenerator.java:56-58`).

**O raciocínio.** O que as instruções do pattern pedem é o esqueleto num lugar só: título, cabeçalho, linhas e total, com cada relatório preenchendo só os passos que mudam. Aqui não existe esqueleto. A interface separa os relatórios e permite escolher um pelo nome, o que lembra um Strategy, mas não fatora a sequência de passos. Também não há classe abstrata com método-modelo, nem escritor genérico que receba as partes variáveis. Os quatro relatórios estão escritos inteiros, cada um com a sua linha de título, que é exatamente a definição de `omissão`. Não é `malfeito`, porque nenhum relatório passa por um esqueleto: falta a estrutura do pattern em si, não só parte dela. Os próximos relatórios (cancelamentos, hóspedes por estado) vão exigir mais uma cópia da sequência e do título.

**O exagero colateral.** Pesei a `RatePlanFactory` (um `switch` estático sobre o enum, `domain/RatePlanFactory.java:4-8`), os mappers e o `MessageService`. A interface `RatePlan` tem duas implementações reais, com regras diferentes de desconto e reembolso, então se justifica. A fábrica é só o mapeamento do enum para a estratégia, que precisa existir em algum lugar. Os DTOs dão a forma do JSON, que o domínio sozinho não daria. Julguei como contenção.

**O comportamento.** Com dados, os quatro relatórios saem certos: título, colunas, ordem, filtros, a não reembolsável cancelada contando e a taxa de R$ 4,50. Falham na formatação do zero e em dois casos de 400 da parte comum, então o resultado é `em parte`.

## Item a item

**O enunciado (pede o pattern).** Os quatro relatórios implementam `ReportGenerator` (`service/report/ReportGenerator.java:8-10`), uma interface de um método só, sem esqueleto. Cada classe repete a sequência completa (filtrar, ordenar, `StringBuilder`, título, cabeçalho, laço, total) e escreve a própria linha de título:
- `OccupancyReportGenerator.java:22`
- `RevenueReportGenerator.java:28`
- `TourismTaxReportGenerator.java:30`
- `RatePlansReportGenerator.java:30`

Também há cópias da seleção (`RatePlansReportGenerator.java:18-23` contra `RevenueReportGenerator.java:17-22`) e do `formatMoney`. O `Map` de geradores em `ReportService.java:20-27` só escolhe qual relatório roda: nenhum passo fica num lugar comum. Isso não é Template Method nem a composição aceita como alternativa, porque não há método-modelo nem escritor genérico pelo qual os relatórios passem. São quatro relatórios escritos inteiros.
Conclusão: omissão

**O exagero colateral.** A `RatePlanFactory` (`domain/RatePlanFactory.java:3-9`) é um `switch` sobre o enum, usado num lugar só (`domain/Reservation.java:37`). Ela serve a uma estratégia real com duas implementações (`StandardRatePlan`, `NonRefundableRatePlan`). Os DTOs e mappers (`api/mapper/ReservationMapper.java:7-26`) dão a forma do JSON. O `MessageService` (`service/MessageService.java:14-18`) é fino, mas cria a mensagem com o canal. Não há interface de implementação única, builder ou camada que só repasse sem motivo.
Conclusão: contenção

**O comportamento.** Criei seis reservas em julho de 2027 e uma em agosto, e passei algumas por confirmação, cancelamento, check-in e check-out.

Os relatórios de julho saíram certos:
- **`/reports/occupancy?date=2027-07-10`**: título `Pousada Maré Alta,occupancy,2027-07-10`, linhas 101, 201 e 301 em ordem de quarto, e `total,3/5`. Só entram as pagas (a pendente da 202 ficou fora).
- **`/reports/revenue?month=2027-07`**: a não reembolsável cancelada (1193.09) entra, a STANDARD cancelada com reembolso fica fora, a ordem é por entrada e depois por número, e veio `total,3284.13`.
- **`/reports/tourism-tax?month=2027-07`**: cabeçalho `reservation,room,check_in,check_out,guests,nights,tax`, três linhas de `18.00` (2×2, 4×1 e 2×2 a 4,50), `total,54.00` e a cancelada fora.
- **`/reports/rate-plans?month=2027-07`**: `STANDARD,2,1295.64`, `NON_REFUNDABLE,2,1988.49` e `total,4,3284.13`, com a não reembolsável cancelada contada.
- **Em todos**: `Content-Type: text/csv; charset=utf-8`, e um mês mal formado (`2027-7`, `x`) ou um parâmetro faltando dá 400 `INVALID_REQUEST`.

Defeitos nos relatórios:
- **Zero sem duas casas.** Em agosto (só uma STANDARD), o relatório de planos trouxe `NON_REFUNDABLE,0,0`. Em março (sem reservas), vieram `total,0` no financeiro e na taxa e `total,0,0` nos planos, quando o pedido é escrever como no financeiro (`0.00`). A causa é `BigDecimal.ZERO.toPlainString()` (`RatePlansReportGenerator.java:56-58`, idem nos outros).

Parte comum, o que confere:
- O STANDARD de 10/07 a 12/07 dá `subtotal 599.76`.
- O não reembolsável de 3 noites na DELUXE dá desconto `210.55` e total `1193.09`.
- Aparecem as recusas 409 `ROOM_UNAVAILABLE`, 404 `ROOM_NOT_FOUND`, 422 `CAPACITY_EXCEEDED`, 400 para saída igual à entrada e para 0 hóspedes, 409 para check-in de pendente e 409 para a segunda via de pendente.
- O check-out devolve o comprovante.
- O e-mail tem o assunto e o texto pedidos, com a linha de desconto e o CPF formatado.

Parte comum, os defeitos:
- **500 em vez de 400.** Um corpo que não é JSON dá `500 Server Error`, e uma data `11/09/2027` também, porque só `IllegalArgumentException` é capturada (`ReservationController.java:24,42`).
- **Espaço do dinheiro.** O "R$" do comprovante vem com espaço não separável (U+00A0, de `NumberFormat` pt-BR em `DocumentService.java:12`).
- **Zeros no JSON.** `discount` e `amountPaid` saem como `0`, não `0.00`.

`mvn -q verify` na cópia passou: 11 testes, 0 falhas, todos de domínio. O serviço foi derrubado no fim.
Conclusão: em parte

## Dúvidas

A `RatePlanFactory` poderia ser lida como uma fábrica desnecessária, já que o enum poderia guardar a estratégia. Mantive `contenção` porque é um mapeamento mínimo de um enum para estratégias que existem de fato, e não uma camada sem propósito.
