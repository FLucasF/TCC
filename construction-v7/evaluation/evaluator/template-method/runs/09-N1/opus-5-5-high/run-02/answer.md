# Cenário 01

## Visão geral

O agente montou um sistema enxuto e correto. São controllers finos, serviços com regra, repositórios em memória e uma classe de preço com funções estáticas. Para os relatórios ele criou uma interface `ReportGenerator` com quatro implementações, guardadas num mapa pelo nome. Essa interface só deixa todos os relatórios com a mesma assinatura (`getName` e `generate`). Ela não tem esqueleto nenhum: cada um dos quatro relatórios escreve de novo, por conta própria, a sequência inteira de título, cabeçalho, laço de linhas e total. A linha "Pousada Maré Alta,…" aparece copiada nas quatro classes, e o `formatDecimal` aparece copiado em três. É o caso que as instruções chamam de omissão. O comportamento confere em tudo o que testei: os quatro relatórios, os preços, as recusas, o comprovante e o e-mail.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `com.pousada` tem o `Main`, que monta tudo à mão e sobe o Javalin. A porta vem de `PORT`, com 7070 se ela não vier (`Main.java:47`, `Main.java:81-91`). As rotas estão em `Main.java:57-72` e os tratadores de erro em `Main.java:74-75`.

- `api.controller`: um controller por recurso. Eles leem a requisição, validam campos e formatos e chamam o serviço. `api.dto` tem os records de entrada e de saída, e `api.ErrorHandler` traduz o código de erro em status HTTP com um `switch` (`ErrorHandler.java:12-17`).
- `domain.service`: `ReservationService` tem a criação e as transições (`ReservationService.java:22-87`). `DocumentService` monta o comprovante e o e-mail (`DocumentService.java:26-69`). Os outros são `RoomService`, `MessageService` e `ReportService`.
- `domain.validation.ReservationValidator`: as recusas, em métodos estáticos.
- `domain.pricing`: `PricingCalculator` e `SeasonCalculator`, também estáticos.
- `domain.model.Reservation`: guarda o estado e calcula o reembolso e o dinheiro que fica com a pousada (`Reservation.java:55-62`, `Reservation.java:78-81`).
- `infrastructure.repository`: listas em memória com `AtomicLong` para numerar as reservas e as mensagens a partir de 1 (`ReservationRepository.java:11-12`). Os quartos são fixos no construtor (`RoomRepository.java:13-20`).

O caminho de uma requisição, tomando a criação: `ReservationController.createReservation` valida os campos que faltam (`ReservationController.java:106-113`) e chama `ReservationService.createReservation`. O serviço busca o quarto (é dele que vem o 404), valida as datas, os hóspedes e a disponibilidade noite a noite, calcula o preço com `PricingCalculator` e grava no repositório.

Testes do agente: 25, em `PricingCalculatorTest`, `SeasonCalculatorTest`, `ReservationServiceTest` e `ErrorHandlingTest`. Nenhum testa os relatórios.

**Os relatórios.** São estas as peças:

- A interface `ReportGenerator`, com `getName()` e `generate(period, reservations, roomRepository)` (`report/ReportGenerator.java:9-12`).
- Quatro classes que a implementam: `OccupancyReportGenerator`, `RevenueReportGenerator`, `TourismTaxReportGenerator` e `RatePlansReportGenerator`.
- `ReportService`, que guarda um `Map<String, ReportGenerator>` preenchido no `Main` (`Main.java:35-39`) e despacha pelo nome (`ReportService.java:24-31`).
- `ReportController`, com um método por rota. Cada método valida o parâmetro, chama `generateReport("<nome>", período)` e põe o content-type (`ReportController.java:18-48`).

Cada `generate` faz tudo sozinho. Ele interpreta o período, filtra e ordena as reservas, abre um `StringBuilder`, escreve o título, escreve o cabeçalho, percorre as reservas escrevendo as linhas, acumula e escreve o total.

- Título: `OccupancyReportGenerator.java:29`, `RevenueReportGenerator.java:34`, `TourismTaxReportGenerator.java:35`, `RatePlansReportGenerator.java:37`. As quatro linhas são idênticas.
- Cabeçalho: `OccupancyReportGenerator.java:30`, `RevenueReportGenerator.java:35`, `TourismTaxReportGenerator.java:36`, `RatePlansReportGenerator.java:38`.
- Total: `OccupancyReportGenerator.java:41-42`, `RevenueReportGenerator.java:49`, `TourismTaxReportGenerator.java:56`, `RatePlansReportGenerator.java:58`.
- `formatDecimal`, igual nas três classes que o usam: `RevenueReportGenerator.java:54-56`, `TourismTaxReportGenerator.java:61-63`, `RatePlansReportGenerator.java:63-65`.
- O filtro "começa no mês e deixou dinheiro" aparece duas vezes, em `RevenueReportGenerator.java:23-28` e em `RatePlansReportGenerator.java:25-30`.

Onde moram as regras de cada caso:

- A taxa de R$ 4,50 está fixa em `PricingCalculator.calculateTourismTax` (`PricingCalculator.java:48-52`).
- A seleção da taxa de turismo é `isPaid` e o mês, com a ordem do financeiro (`TourismTaxReportGenerator.java:24-32`).
- O relatório por plano percorre `RatePlan.values()` para trazer os dois planos mesmo sem reserva (`RatePlansReportGenerator.java:43-56`).

**O raciocínio.** O enunciado pede o Template Method: o esqueleto (título, cabeçalho, linhas, total) num lugar só e os passos que variam em cada relatório. Também aceitaria um escritor genérico que recebesse as partes que variam.

O que existe aqui é polimorfismo só na entrada. A interface permite escolher o relatório pelo nome, o que é um Strategy ou registro, mas ela não fixa a ordem dos passos nem a linha de título. Cada implementação é o relatório "escrito inteiro, com a sua linha de título", que é exatamente a definição de omissão nas instruções. O próximo relatório (cancelamentos, hóspedes por estado) seria mais uma cópia do mesmo laço.

O que o aproxima do pattern é pouco: há uma abstração comum e um ponto único de despacho. Isso não o põe em malfeito, porque malfeito pede um esqueleto que exista ao menos para uma parte dos relatórios, e aqui não existe esqueleto para nenhum.

Exagero colateral: fora dos relatórios não há interface com uma implementação só, nem fábrica, nem builder. As camadas de serviço fazem alguma coisa. `RoomService` transforma o quarto ausente em 404 (`RoomService.java:18-22`) e `MessageService` dá o número da mensagem (`MessageService.java:15-19`). Só o `RoomService.getAllRooms` é um repasse puro (`RoomService.java:14-16`), o que é pouco para contar como estrutura sem necessidade.

Comportamento: tudo o que testei confere. Há um detalhe pequeno de JSON: os zeros saem como `0`, e não `0.00` (`discount`, `amountPaid`, `refund`), por causa do `BigDecimal.ZERO` (`PricingCalculator.java:41`, `Reservation.java:38`).

## Item a item

**O enunciado (pede o pattern).** Os quatro relatórios implementam a interface `ReportGenerator` (`report/ReportGenerator.java:9-12`) e são escolhidos pelo nome em `ReportService.java:24-31`. A interface não tem método-modelo, e não há classe-base nem escritor genérico. Cada relatório escreve sozinho a linha de título (`OccupancyReportGenerator.java:29`, `RevenueReportGenerator.java:34`, `TourismTaxReportGenerator.java:35`, `RatePlansReportGenerator.java:37`), o cabeçalho, o laço das linhas e o total, e três deles repetem o `formatDecimal`. O esqueleto que o enunciado pedia num lugar só está copiado quatro vezes.
Conclusão: omissão

**O exagero colateral.** Não há. Os serviços, repositórios e validadores são classes concretas usadas por um único chamador e fazem trabalho de verdade (`RoomService.java:18-22`, `MessageService.java:15-19`, `ReservationService.java:22-51`). A interface `ReportGenerator`, que tem quatro implementações, fica dentro da parte do fim do enunciado.
Conclusão: contenção

**O comportamento.** Testei numa cópia do projeto, com o jar `target/mare-alta-1.0.0.jar`. Criei 7 reservas: 1 a 5 confirmadas, a 3 com check-in e check-out, a 4 (não reembolsável) e a 5 (normal) canceladas depois de pagas, a 6 cancelada ainda pendente e a 7 em março.

- **Preço e cancelamento.** STANDARD de 10 a 12/07 no quarto 101: 599.76. Não reembolsável de 3 noites na DELUXE: 1403.64 − 210.55 = 1193.09. O cancelamento devolve 599.76 na normal, 0 na não reembolsável e 0 na que estava pendente.
- **`occupancy?date=2027-07-10`.** Veio `Pousada Maré Alta,occupancy,2027-07-10`, depois o cabeçalho, as linhas 101 e 301 e `total,2/5`, com `text/csv; charset=utf-8`.
- **`revenue?month=2027-07`.** Título, depois as reservas 2, 1, 3 e 4 (CANCELLED, 795.40) e `total,3284.13`. A 5 ficou de fora, como devia, porque foi reembolsada.
- **`tourism-tax?month=2027-07`.** Colunas `reservation,room,check_in,check_out,guests,nights,tax`. A reserva de 2 hóspedes por 3 noites deu 27.00, as outras duas deram 18.00 e 18.00, e o total foi 63.00. A não reembolsável cancelada ficou de fora, que é o certo, porque não está paga.
- **`rate-plans?month=2027-07`.** Veio `STANDARD,2,1295.64`, `NON_REFUNDABLE,2,1988.49` (com a cancelada contando) e `total,4,3284.13`. Em 2027-08 os dois planos aparecem com `0,0.00`, e o total é `total,0,0.00`.
- **Comprovante e e-mail.** A segunda via e o e-mail da reserva 7 batem com o modelo do enunciado (R$ 779,80, CPF formatado, "2 diárias").
- **Recusas.** 409 ROOM_UNAVAILABLE, 422 CAPACITY_EXCEEDED, 404 ROOM_NOT_FOUND e RESERVATION_NOT_FOUND, 400 INVALID_REQUEST (saída igual à entrada, 0 hóspedes, data fora do formato, corpo que não é JSON, campo que falta, mês 2027-13 e "jul", parâmetro ausente) e 409 OPERATION_NOT_ALLOWED (check-out sem check-in, segunda via de reserva não paga).
- **`mvn verify`.** 25 testes, 0 falhas, BUILD SUCCESS.

O único desvio é cosmético: os zeros saem no JSON como `0`, e não `0.00`.
Conclusão: segue

## Dúvidas

A interface `ReportGenerator` com o registro em `ReportService` é uma estrutura polimórfica de verdade, e isso podia sugerir malfeito em vez de omissão. Fiquei com omissão porque essa estrutura escolhe o relatório, mas não guarda nenhum passo comum. Não existe esqueleto, nem parcial, e os quatro relatórios estão escritos inteiros com a linha de título copiada, que é a definição de omissão nas instruções.
