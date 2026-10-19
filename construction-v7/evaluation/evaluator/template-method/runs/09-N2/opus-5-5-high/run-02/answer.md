# Cenário 01

## Visão geral

O agente montou os quatro relatórios com Template Method. A classe abstrata `CsvReport` tem o método-modelo `final`, que escreve o título, o cabeçalho, as linhas e o total. Uma base intermediária, `MonthlyCsvReport`, guarda o que os três relatórios mensais têm em comum. Ocupação, financeiro, taxa de turismo e planos são subclasses que só preenchem o conteúdo. A rota de relatórios não pergunta qual é o relatório: busca num registro e chama o mesmo método para todos. A taxa de turismo fica num objeto próprio, então a mudança anual do valor não mexe no relatório. Todo o comportamento que testei confere, e o `mvn verify` passa. O único ponto fraco está fora dos relatórios: duas interfaces com uma implementação só (`ReservationRepository` e `MessageSender`), que nada substitui hoje.

## Como foi implementado, e por que o julgamento

**O projeto.** São quatro pacotes:

- `domain`: a regra de negócio.
  - `Reservation` calcula noites, subtotal, desconto e total no construtor e controla as transições de estado.
  - `RatePlan` é um enum com o desconto e o reembolso de cada plano.
  - `Season` dá o preço da noite com a alta temporada.
  - `Money` cuida do arredondamento meio para o par e dos formatos `R$ 1.234,56` e `599.76`.
  - `ErrorCode` liga cada código de erro ao seu status HTTP.
- `service`:
  - `ReservationService` faz a orquestração: criação com as checagens de quarto, capacidade e conflito, confirmação com envio do e-mail, cancelamento, check-in, check-out e segunda via.
  - `RoomCatalog` tem os cinco quartos fixos.
  - `InMemoryReservationRepository` e `InMemoryOutbox` guardam os dados em memória.
  - `ReceiptFormatter` monta o texto do comprovante e `Receipt` é o comprovante em JSON.
- `report`: os relatórios.
- `web`:
  - `App` registra as rotas e trata as exceções.
  - `BookingParser` valida o corpo do POST.
  - `ReservationView` é o JSON da reserva.
  - `Main` lê `PORT` e usa 7070 por padrão (`web/Main.java:7-8`).

Uma requisição segue este caminho: a rota em `App` (`web/App.java:34-45`) chama o `ReservationService`, que busca no repositório e delega a regra para `Reservation` (por exemplo, `domain/Reservation.java:56-77`). As exceções `BusinessException` viram `{error, message}` com o status do `ErrorCode` (`web/App.java:29-30`, `web/App.java:73-75`). Tudo é montado à mão em `App.create()` (`web/App.java:21-27`), sem contêiner. O jar é gerado pelo shade com `Main` no manifesto (`pom.xml:56-58`). Os testes do agente são um único teste de ponta a ponta (`AppTest.java:39-79`). Ele sobe o servidor, cria e confirma uma reserva, confere o e-mail e as recusas, e compara o CSV inteiro da ocupação, da taxa e dos planos.

**Os relatórios.** As classes são estas:

- `CsvReport<P>` (`report/CsvReport.java:9`) tem o método-modelo `generate`, declarado `final` (`report/CsvReport.java:20`). Ele valida a presença do parâmetro, chama o passo abstrato `parse` (`:15`) e depois o passo abstrato `build` (`:17`), que devolve um `Table(header, rows, total)` (`:13`). Em seguida escreve, nesta ordem, a linha de título `Pousada Maré Alta,<name()>,<período>`, o cabeçalho, as linhas e o total (`:25-28`), e junta tudo com vírgulas (`:30`). A linha de título existe só nesse lugar.
- `MonthlyCsvReport` (`report/MonthlyCsvReport.java:11`) fica no meio da hierarquia. Ela implementa o `parse` do mês (`:16-23`) e oferece duas seleções: `startingIn`, que pega as reservas que começam no mês, ordenadas por entrada e depois por número (`:26-31`), e `retainingMoney`, que pega as que deixaram dinheiro com a pousada (`:34-36`).
- `OccupancyReport` (`report/OccupancyReport.java:11`) analisa a data e monta a tabela das reservas pagas que ocupam a noite, em ordem de quarto, com o total `n/5` (`:30-41`).
- `RevenueReport` (`report/RevenueReport.java:10-23`) usa `retainingMoney` e soma `retained()`.
- `TourismTaxReport` (`report/TourismTaxReport.java:10-28`) filtra as reservas pagas de `startingIn` (`:19`) e calcula a taxa com `TourismTaxPolicy`. Essa política guarda o valor por hóspede por noite (`report/TourismTaxPolicy.java:10-20`) e é injetada pelo registro.
- `RatePlanReport` (`report/RatePlanReport.java:12-30`) percorre `RatePlan.values()`, que dá STANDARD e depois NON_REFUNDABLE, mesmo sem reservas (`:21-27`), sobre as mesmas reservas do financeiro (`:17`).
- `ReportRegistry` (`report/ReportRegistry.java:18-24`) é o único lugar que lista os quatro.
- A rota única `GET /reports/{name}` (`web/App.java:47-55`) acha o relatório pelo nome, lê o parâmetro que ele declara (`report.parameter()`) e chama `generate`. Não há `if` nem `switch` por relatório em nenhum ponto.

**O raciocínio do julgamento.** As instruções do pattern pedem uma classe abstrata com o método-modelo, de preferência `final`, e os quatro relatórios como subclasses, a ocupação e o financeiro inclusive. É o que existe aqui (`report/CsvReport.java:20`, mais as quatro subclasses). O esqueleto (título, cabeçalho, linhas, total) e a formatação CSV estão num lugar só. Nenhuma subclasse escreve a linha de título nem repete a sequência dos passos: elas só devolvem os dados da tabela. Uma diferença em relação à forma clássica é que o passo `build` é grosso. O cabeçalho, as linhas e o total saem juntos num `Table`, e não de três ganchos separados. Mesmo assim, a ordem e o formato continuam só no método-modelo, e a seleção e a ordenação comuns aos mensais foram para a base intermediária. Isso aproxima o desenho do pattern, não o afasta. Criar o relatório de cancelamentos ou o de hóspedes por estado seria uma subclasse nova e uma linha no registro.

No exagero colateral pesaram `ReservationRepository` (`service/ReservationRepository.java:8`) e `MessageSender` (`service/MessageSender.java:4`). As duas são interfaces com uma implementação só, e nada as substitui. No caso da segunda, a rota `/messages` usa direto a classe concreta `InMemoryOutbox` (`web/App.java:23`, `:45`). A interface `Report` (`report/Report.java:8`) e o `ReportRegistry` ficam na parte dos relatórios e por isso não entram nessa conta.

No comportamento, tudo o que testei confere.

## Item a item

**O enunciado (pede o pattern).** `CsvReport` tem o método-modelo `final generate` (`report/CsvReport.java:20-32`). Ele escreve o título, o cabeçalho, as linhas e o total (`:25-28`) e chama os passos abstratos `parse` e `build` (`:15`, `:17`). Os quatro relatórios são subclasses: `OccupancyReport` (`report/OccupancyReport.java:11`) diretamente, e `RevenueReport`, `TourismTaxReport` e `RatePlanReport` (`report/RevenueReport.java:10`, `report/TourismTaxReport.java:10`, `report/RatePlanReport.java:12`) por meio de `MonthlyCsvReport` (`report/MonthlyCsvReport.java:11`). A rota usa só a abstração, sem perguntar qual é o relatório concreto (`web/App.java:47-55`). Isso é o Template Method que o enunciado precisava, aplicado aos quatro casos, incluindo os dois da parte comum.
Conclusão: acerto

**O exagero colateral.** Há duas interfaces com uma implementação só, que nada substitui:
- `ReservationRepository` (`service/ReservationRepository.java:8`), implementada só por `InMemoryReservationRepository` (`service/InMemoryReservationRepository.java:11`);
- `MessageSender` (`service/MessageSender.java:4`), implementada só por `InMemoryOutbox` (`service/InMemoryOutbox.java:6`).

O próprio `App` não usa a segunda interface para ler as mensagens: depende da classe concreta (`web/App.java:23`, `:45`). O resto é enxuto: `RoomCatalog` é concreto, o comprovante é uma função estática e não há fábricas nem builders.
Conclusão: exagero

**O comportamento.** Fiz o build de uma cópia, subi o jar na porta 7070 e criei oito reservas em julho, agosto e março de 2027, nos dois planos. Depois confirmei, cancelei uma não reembolsável confirmada, uma STANDARD confirmada e uma pendente, e fiz check-in e check-out de outra.

- **Preços.** STANDARD de 10/07 a 12/07 deu `subtotal 599.76`, como no exemplo do enunciado. DELUXE não reembolsável de 3 noites em julho deu `1403.64`, desconto `210.55` e total `1193.09`, que confere com o meio para o par.
- **Recusas.** Conflito de noite deu 409 ROOM_UNAVAILABLE. 3 pessoas no 101 deu 422 CAPACITY_EXCEEDED. Quarto 999 deu 404 ROOM_NOT_FOUND. Saída antes da entrada e campo faltando deram 400 INVALID_REQUEST. Check-out repetido deu 409 OPERATION_NOT_ALLOWED. Segunda via de reserva cancelada sem pagamento deu 409. Reembolso: STANDARD confirmada devolve tudo, não reembolsável devolve `0.00`, pendente devolve `0.00`.
- **Comprovante e e-mail.** O texto do e-mail tem o assunto, o canal `EMAIL` e o comprovante exatamente como no enunciado, com "1 diária" e a linha "Desconto: R$ 104,38" quando é o caso. O check-out devolve o JSON do comprovante com `title`.
- **`occupancy?date=2027-07-10`.** Começa com `Pousada Maré Alta,occupancy,2027-07-10`, segue o cabeçalho, traz as linhas em ordem de quarto e fecha com `total,3/5`. A STANDARD cancelada fica de fora. Em 2027-07-12, quem sai no dia não conta: `total,0/5`.
- **`revenue?month=2027-07`.** O título vem certo, a ordem é por entrada e depois por número, e a não reembolsável cancelada aparece com `1193.09`. A STANDARD cancelada fica de fora e o total é `3320.11`.
- **`tourism-tax?month=2027-07`.** Traz o título e as colunas pedidas, só as reservas pagas, na ordem do financeiro: `2,2,18.00`, `3,1,13.50` e `3,2,27.00`, com `total,58.50`.
- **`rate-plans?month=2027-07`.** Dá `STANDARD,2,1535.52`, `NON_REFUNDABLE,2,1784.59` e `total,4,3320.11`, batendo com o financeiro. Num mês vazio as duas linhas aparecem com `0,0.00`.
- **Formato e erros dos relatórios.** O `Content-Type` é `text/csv; charset=utf-8`. Mês `2027-7`, parâmetro ausente e data `10/07/2027` dão 400 INVALID_REQUEST.

O `mvn -q verify` da cópia passou, com 1 teste rodado, 0 falhas e 0 erros. Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

No exagero colateral, o enunciado diz "por enquanto" tanto para os dados em memória quanto para os e-mails que não saem de verdade. Dá para ler as duas interfaces como preparação para uma troca anunciada. Mesmo assim, hoje nenhuma das duas tem uma segunda implementação nem é trocada em lugar nenhum, e o próprio `App` ignora `MessageSender` para ler a lista. Por isso fiquei com `exagero`, que é o que a regra das instruções descreve.

No pattern, o gancho único `build`, que devolve cabeçalho, linhas e total juntos, é mais grosso que três passos separados. Mesmo assim, não muda a classe: a sequência e a formatação só existem no método-modelo.
