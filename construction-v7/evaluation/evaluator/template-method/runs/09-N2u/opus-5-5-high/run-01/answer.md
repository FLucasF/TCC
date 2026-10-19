# Cenário 01

## Visão geral

O agente escreveu os quatro relatórios (ocupação, financeiro, taxa de turismo e planos de tarifa) como quatro classes independentes. Elas implementam uma interface `ReportStrategy` que só declara `generate` e `getReportName`. Cada classe monta o próprio CSV do começo ao fim: a linha de título, o cabeçalho, o laço das linhas e a linha do total. O esqueleto comum, que é o que o enunciado pedia, não existe em lugar nenhum, então o desenho é uma **omissão** do Template Method, mesmo com uma interface por cima. O comportamento dos relatórios está certo em tudo que testei: títulos, colunas, ordem, taxa, plano sem reserva, não reembolsável cancelada. O problema mais sério de comportamento está na parte comum: a segunda via do comprovante sai também para reserva cancelada. Fora dos relatórios há estrutura sem necessidade: uma Strategy de plano de tarifa paralela ao enum, criada por um `switch`, e um State com cinco classes.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `com.pousada` tem o `Application` (`Application.java:24-102`). Ele monta tudo à mão: repositórios, serviços e controllers. Também registra as rotas do Javalin (`Application.java:53-68`) e traduz exceções em respostas de erro: `IllegalArgumentException` com o código na mensagem, mapeado por `switch` (`Application.java:70-90`), e `IllegalStateException` para 409 (`Application.java:92-95`). A porta vem de `PORT`, e a 7070 é o padrão (`Application.java:104-114`). Os pacotes são estes:
- `domain`: `Reservation`, `Room`, os enums `RoomType`, `RatePlan` e `ReservationStatus`;
- `repository`: listas em memória, `RoomRepository` com os cinco quartos fixos (`RoomRepository.java:13-20`) e `ReservationRepository` com o gerador de id e a checagem de conflito (`ReservationRepository.java:40-46`);
- `service`: `ReservationService`, `ReportService` e `NotificationService`, que é a lista de mensagens;
- `controller`: um controller por recurso;
- `state`: o ciclo de vida da reserva;
- `strategy`: os planos de tarifa e os relatórios.

O caminho de uma requisição é este: a rota chama o `ReservationController`, que valida o corpo (`ReservationController.java:163-186`) e chama o `ReservationService`. O serviço checa quarto, hóspedes, capacidade e conflito (`ReservationService.java:25-51`). As transições passam pela `Reservation`, que delega a um objeto `ReservationState` (`Reservation.java:104-118`). O preço é recalculado a cada leitura a partir da diária do quarto (`Reservation.java:56-101`). O comprovante JSON é montado duas vezes no controller, com o mesmo código copiado: no check-out (`ReservationController.java:80-104`) e na segunda via (`ReservationController.java:106-135`). O comprovante em texto do e-mail fica em `ReservationService.java:92-115`.

O build usa o shade plugin e gera `target/mare-alta.jar`, que sobe sozinho. Os testes do agente são 9 testes unitários em `ReservationTest.java`: criação, preço normal, não reembolsável, alta temporada, confirmação, cancelamento, check-in/check-out, conflito e capacidade. Nenhum teste cobre os relatórios ou a camada HTTP.

**A parte do fim do enunciado.** As classes são `OccupancyReportStrategy`, `RevenueReportStrategy`, `TourismTaxReportStrategy` e `RatePlansReportStrategy`. Todas implementam `ReportStrategy` (`ReportStrategy.java:10-13`), que só tem `generate(List<Reservation>, List<Room>, Object)` e `getReportName()`. O `ReportService` registra as quatro num mapa (`ReportService.java:25-33`). Depois tem quatro métodos quase idênticos, e cada um busca a sua estratégia por uma string fixa (`ReportService.java:35-61`). O `ReportController` repete quatro vezes a validação do parâmetro e o envio do CSV (`ReportController.java:20-86`). Cada relatório escreve sozinho a sequência inteira dos passos:
- ocupação: título em `OccupancyReportStrategy.java:19`, cabeçalho em `:20`, seleção e ordem em `:22-26`, linhas em `:28-39`, total em `:41`;
- financeiro: título em `RevenueReportStrategy.java:20`, cabeçalho em `:21`, seleção e ordem em `:25-30`, linhas em `:32-42`, total em `:44`;
- taxa de turismo: título em `TourismTaxReportStrategy.java:23`, cabeçalho em `:24`, seleção e ordem em `:28-33`, linhas em `:35-46`, total em `:48`. A taxa é a constante `TAX_PER_GUEST_PER_NIGHT` (`:14`) multiplicada por hóspedes × noites (`:64-67`);
- planos: título em `RatePlansReportStrategy.java:21`, cabeçalho em `:22`, agregação por plano em `:24-37`, uma linha por `RatePlan.values()` em `:42-50`, total em `:52`.

Além da sequência, os auxiliares também estão copiados. `formatCsvNumber` aparece três vezes (`RevenueReportStrategy.java:60`, `TourismTaxReportStrategy.java:69`, `RatePlansReportStrategy.java:68`). `isPaid` aparece duas vezes (`OccupancyReportStrategy.java:51`, `TourismTaxReportStrategy.java:58`), e `getNetRevenue` também duas (`RevenueReportStrategy.java:54`, `RatePlansReportStrategy.java:62`).

**O raciocínio.** O Template Method pede o esqueleto (título → cabeçalho → linhas → total) num lugar só, com cada relatório preenchendo os passos que mudam. A alternativa aceita é um escritor genérico que recebe as partes. Aqui não há nenhum dos dois. A interface `ReportStrategy` dá aos relatórios uma assinatura comum, mas não guarda nenhum passo. O nome "Strategy" e o mapa de registro não mudam isso, porque o código mostra quatro relatórios inteiros, cada um com a sua linha de título: é o caso que as instruções chamam de omissão. Também não é malfeito, porque não existe esqueleto parcial nem utilitário de CSV compartilhado: os quatro repetem tudo.

No exagero colateral pesou o plano de tarifa. A `Reservation` guarda o enum `RatePlan` e, em paralelo, um `RatePlanStrategy` criado por um `switch` sobre esse enum (`Reservation.java:39`, `:45-50`). São uma interface e duas classes (`RatePlanStrategy.java:5-8`, `StandardRatePlan.java:5-14`, `NonRefundableRatePlan.java:6-17`) para dois comportamentos constantes, um desconto e um booleano, que o próprio enum poderia carregar, e o enunciado não diz que os planos vão crescer. O ciclo de vida também virou um State com cinco classes e uma interface (`state/*.java`), em que quase todos os métodos só lançam `OPERATION_NOT_ALLOWED`.

No comportamento, os quatro relatórios conferem com o pedido. O desvio que encontrei na parte comum é o comprovante de reserva cancelada.

## Item a item

**O enunciado (pede o pattern).** Cada relatório escreve inteiro o título, o cabeçalho, as linhas e o total (`OccupancyReportStrategy.java:18-43`, `RevenueReportStrategy.java:19-46`, `TourismTaxReportStrategy.java:22-50`, `RatePlansReportStrategy.java:20-54`). A linha "Pousada Maré Alta,..." está copiada nos quatro (`:19`, `:20`, `:23`, `:21`). A interface comum `ReportStrategy.java:10-13` não tem método-modelo nem passos: é só uma assinatura. O `ReportService` ainda escolhe cada relatório por uma string fixa num método próprio (`ReportService.java:35-61`). Não há esqueleto em lugar nenhum, nem classe abstrata nem escritor genérico com as partes injetadas, então isso não é o Template Method nem a composição equivalente.
Conclusão: omissão

**O exagero colateral.** O plano de tarifa tem duas representações paralelas: o enum `RatePlan` e uma hierarquia `RatePlanStrategy`/`StandardRatePlan`/`NonRefundableRatePlan`. A segunda é criada por uma fábrica `switch` sobre o primeiro (`Reservation.java:45-50`), para dois valores fixos (desconto de 15% e "reembolsável") que caberiam no enum. O ciclo de vida virou um State de cinco classes (`state/PendingState.java`, `ConfirmedState.java`, `CheckedInState.java`, `CheckedOutState.java`, `CancelledState.java`) mais `ReservationState.java:8-14`, em que a maior parte dos métodos só lança exceção. O mesmo conteúdo cabe numa tabela de transições.
Conclusão: exagero

**O comportamento.** Criei 8 reservas em julho, agosto e junho/julho de 2027. Confirmei sete. Cancelei uma não reembolsável e uma normal, e deixei uma pendente. Fiz check-in e check-out da 1 e check-in da 6. Resultados:
- `/reports/occupancy?date=2027-07-10`: título `Pousada Maré Alta,occupancy,2027-07-10`, linhas 101 e 102 em ordem de quarto, `total,2/5`, `text/csv; charset=utf-8`. Confere.
- `/reports/revenue?month=2027-07`: a não reembolsável cancelada (1193.09) entra, a normal cancelada e a pendente ficam de fora, a ordem é por entrada e depois por número, e o total é `total,3585.70`. A reserva que começa em junho fica de fora. Confere.
- `/reports/tourism-tax?month=2027-07`: as colunas são as pedidas, só entram as reservas pagas (sem as canceladas) na ordem do financeiro, 2 hóspedes × 3 noites dão `27.00`, 1×2 dão `9.00`, e o total é `total,54.00`. Confere.
- `/reports/rate-plans?month=2027-07`: `STANDARD,2,1199.52`, `NON_REFUNDABLE,2,2386.18` (a cancelada conta), `total,4,3585.70`. Num mês vazio (2027-09) aparecem as duas linhas zeradas e `total,0,0.00`. Confere.
- Mês inválido (`2027-13`), parâmetro ausente e data fora do formato dão 400 `INVALID_REQUEST`.

Na parte comum, conferem:
- o preço: `STANDARD` de 10/07 a 12/07 dá 599.76, e o desconto de 15% sobre 1403.64 dá 210.55;
- o e-mail, com o comprovante, a linha "Desconto" e o dinheiro como `R$ 1.403,64`;
- o comprovante JSON do check-out;
- as recusas: 409 `ROOM_UNAVAILABLE`, 422 `CAPACITY_EXCEEDED`, 409 `OPERATION_NOT_ALLOWED` ao cancelar depois do check-out, 404 de quarto e de reserva, 400 para corpo que não é JSON;
- a segunda via de reserva pendente, que é recusada com 409.

Não conferem:
- a segunda via de reserva **cancelada** responde 200, tanto para a não reembolsável quanto para a normal já reembolsada. A checagem olha só `amountPaid` (`ReservationController.java:110`), quando o enunciado só aceita reserva confirmada, com check-in ou com check-out;
- o JSON manda zeros como `0`, e não com 2 casas (`discount`, `amountPaid` e `refund`, que vêm de `BigDecimal.ZERO`).

O `mvn -q verify` da cópia passou: 9 testes, 0 falhas, 0 erros. O serviço foi derrubado no fim.
Conclusão: em parte

## Dúvidas

O exagero colateral era a questão mais aberta. Um State para o ciclo de vida da reserva costuma ser defensável. O que decidiu foi a Strategy de plano de tarifa, que duplica o enum e é escolhida por `switch` (`Reservation.java:45-50`). Se esse par fosse aceito, o resultado ficaria no limite entre `contenção` e `exagero`. Na classe do enunciado não houve dúvida real: a interface `ReportStrategy` poderia parecer uma estrutura comum, mas não guarda nenhum passo do relatório.
