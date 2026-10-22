# Cenário 09

## Visão geral

O agente usou o Template Method para os relatórios. A classe abstrata `CsvReport` tem o método-modelo `final generate()`, que monta título, cabeçalho, linhas e total. Os quatro relatórios são subclasses e preenchem só o que muda: os dois que já existiam (ocupação e financeiro) e os dois novos (taxa de turismo e planos de tarifa). A linha de título aparece em um único lugar, a classe-base, e nenhum relatório monta CSV fora dela. As regras dos relatórios conferem com o enunciado. O problema mais sério está fora dos relatórios: pela leitura, um corpo que não é JSON, ou uma data fora do formato no corpo do `POST /reservations`, não é capturado e cai no 500 padrão do Javalin, quando deveria ser 400 `INVALID_REQUEST`. Há também uma questão menor de desenho: o `totalLine()` de cada relatório depende de um campo que o `dataLines()` preenche antes.

## Como foi implementado, e por que o julgamento

**O projeto.** Ele está dividido em pacotes:
- `domain`: `Reservation`, `Room`, os enums `RoomType`, `RatePlan` e `ReservationStatus`, `PriceCalculator` e `Message`.
- `repository`: repositórios em memória feitos com `LinkedHashMap`.
- `service`: `ReservationService`, `NotificationService`, `ReceiptFormatter` e `ReportService`.
- `report`: o `CsvReport` e as quatro subclasses.
- `api/dto`: records para o JSON.

As rotas ficam todas em `Application.createApp()` (`Application.java:39-255`), que monta o Javalin com um `ObjectMapper` configurado com o módulo de datas (`Application.java:40-46`). O construtor faz a montagem de tudo à mão, sem framework (`Application.java:28-37`). O `main` lê `PORT` e usa 7070 quando ela não vem (`Application.java:270-285`).

Uma requisição segue este caminho: a rota valida os parâmetros, chama o serviço, o serviço usa o repositório e o domínio, e as exceções do serviço viram códigos HTTP com `try/catch` em cada rota (por exemplo, `Application.java:75-85`). As regras de preço e de estado ficam em `Reservation`:
- cálculo do preço: `Reservation.java:43-58`;
- transições de estado: `Reservation.java:60-88`;
- desconto e reembolso: um método abstrato por constante do enum `RatePlan` (`RatePlan.java:7-32`).

Os testes do agente são unitários: preço, reserva, serviço e relatórios. O `ReportTest` confere o título de cada um dos quatro relatórios e a conta de 2 hóspedes × 3 noites = 27.00 (`src/test/java/com/pousadamarealta/report/ReportTest.java:25-98`).

**A parte que pede o pattern.**
- `CsvReport.generate()` é `final` (`report/CsvReport.java:8-17`) e define a ordem dos passos: título, cabeçalho, uma linha para cada item de `dataLines()` e a linha do total.
- O título é escrito uma única vez, em `titleLine()` (`report/CsvReport.java:19-21`), com o nome da pousada guardado numa constante (`report/CsvReport.java:6`).
- Os passos abstratos são `reportName`, `period`, `headerLine`, `dataLines` e `totalLine` (`report/CsvReport.java:23-27`).

Cada subclasse implementa só esses passos:
- `OccupancyReport` (`report/OccupancyReport.java:12`): usa `findPaidByNight`, ordenado por quarto (`repository/ReservationRepository.java:33-38`), e o total `total,%d/%d` (`report/OccupancyReport.java:59-64`).
- `RevenueReport` (`report/RevenueReport.java:12`): usa as reservas do mês com receita líquida maior que zero (`report/RevenueReport.java:41-43`).
- `TourismTaxReport` (`report/TourismTaxReport.java:12`): a taxa de 4.50 fica numa constante da própria classe (`report/TourismTaxReport.java:15`).
- `RatePlansReport` (`report/RatePlansReport.java:12`): os dois planos são inicializados sempre, em ordem (`report/RatePlansReport.java:44-46`).

O `ReportService` só instancia o relatório certo e chama `generate()` (`service/ReportService.java:19-37`). Cada rota de relatório chama o método correspondente do serviço (`Application.java:190-252`).

**O julgamento.** O desenho bate com o que as instruções chamam de acerto:
- há uma classe abstrata com o método-modelo `final`;
- os passos variáveis são abstratos;
- os quatro relatórios são subclasses, os dois antigos inclusive;
- nenhum relatório repete a sequência dos passos nem escreve a linha de título.

Quando vier a "linha com a data em que foram tirados", basta mudar `generate()`. Há imperfeições, mas nenhuma reabre a variação:
- a seleção e a ordenação ficam dentro de `dataLines()` em vez de serem passos próprios;
- o `totalLine()` lê um campo preenchido como efeito colateral de `dataLines()` (`report/RevenueReport.java:17,41,62`; `report/RatePlansReport.java:16,44,68`), o que é um acoplamento temporal;
- `formatAmount` e o formatador de data estão copiados em três subclasses (`report/RevenueReport.java:69-71`, `report/TourismTaxReport.java:78-80`, `report/RatePlansReport.java:78-80`).

Quanto ao exagero colateral, o `ReportService` é uma camada fina, mas tem um papel: ele guarda os repositórios e escolhe qual relatório montar. Fora isso não há interfaces de implementação única, fábricas nem builders. Quanto ao comportamento, os relatórios conferem com o pedido, mas o tratamento de corpo inválido no POST não confere.

## Item a item

**O pattern pedido.** `CsvReport` (`report/CsvReport.java:5-28`) tem o esqueleto `final generate()` (`report/CsvReport.java:8-17`) com título, cabeçalho, linhas e total, e o título é montado só ali (`report/CsvReport.java:19-21`). Os quatro relatórios estendem essa classe: `report/OccupancyReport.java:12`, `report/RevenueReport.java:12`, `report/TourismTaxReport.java:12` e `report/RatePlansReport.java:12`. Cada um implementa só nome, período, cabeçalho, linhas e total. Quem usa os relatórios trata todos como `CsvReport` e chama `generate()` (`service/ReportService.java:20-36`), sem perguntar qual é o caso concreto. É o Template Method aplicado a todos os casos, inclusive aos dois que já existiam. As ressalvas são de detalhe:
- o estado é passado entre `dataLines()` e `totalLine()` por um campo (`report/OccupancyReport.java:43,61`);
- `formatAmount` está copiado nas subclasses.

Conclusão: acerto

**O exagero colateral.** Não há estrutura sem necessidade. O `ReportService` (`service/ReportService.java:10-38`) é fino, mas escolhe e monta o relatório com as suas dependências; não é só repasse puro. Os repositórios são classes concretas, sem interfaces de implementação única. `RatePlan` com métodos por constante (`domain/RatePlan.java:6-33`) é uma solução enxuta para uma variação real.

Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura, sem subir o serviço nem chamar a API.

Relatórios:
- **Título:** os quatro começam com `Pousada Maré Alta,<nome>,<período>` (`report/CsvReport.java:20`), com os nomes `occupancy`, `revenue`, `tourism-tax` e `rate-plans`. O período é a data ou o mês da chamada, nos formatos `yyyy-MM-dd` e `yyyy-MM`.
- **Taxa de turismo:** lista as reservas pagas que começam no mês (`repository/ReservationRepository.java:48-52`), na mesma ordem do financeiro, por entrada e depois id (`repository/ReservationRepository.java:43-44`). A taxa é 4.50 × hóspedes × noites (`report/TourismTaxReport.java:71-76`), as colunas são as pedidas (`report/TourismTaxReport.java:38`) e o total fica no fim.
- **Planos de tarifa:** sempre tem `STANDARD` e depois `NON_REFUNDABLE`, com contagem e valor. Conta as mesmas reservas do financeiro, ou seja, receita líquida maior que zero, o que inclui a não reembolsável cancelada, porque o reembolso dela é 0 (`domain/RatePlan.java:26-28`). O total é `total,<qtd>,<valor>` (`report/RatePlansReport.java:66-76`).
- **Ocupação:** reservas pagas que ocupam a noite (quem sai no dia não conta, `domain/Reservation.java:90-92`), em ordem de quarto, com o total `total,n/5`.
- **Financeiro:** colunas, ordem e total como o enunciado pede.
- **Tipo de conteúdo:** `text/csv; charset=utf-8` em todas as rotas de relatório.

Resto do sistema:
- Preço, desconto, transições de estado, reembolso, comprovante e e-mail conferem pela leitura.
- Problema: no `POST /reservations`, a rota só captura `IllegalArgumentException` e as exceções do serviço (`Application.java:75-85`). Um erro de parse do Jackson, como corpo que não é JSON ou data fora do formato no corpo, não é `IllegalArgumentException`, e não há tratador global de exceções. A resposta seria o 500 do Javalin em vez de 400 `INVALID_REQUEST`.
- Detalhe menor: a capacidade é checada antes das datas (`service/ReservationService.java:25-35`), então um pedido inválido nos dois pontos recebe 422 em vez de 400.

Conclusão: em parte

## Dúvidas

O 500 para corpo malformado é uma inferência de leitura. Ele depende de o `bodyAsClass` do Javalin 6.7.0 com `JavalinJackson` repassar a exceção do Jackson (`JsonProcessingException`, que não é `IllegalArgumentException`) sem convertê-la. Não rodei a API para confirmar. Se o Javalin converter essa exceção num `BadRequestResponse` próprio, a resposta seria 400, mas com o corpo padrão do Javalin e não com `{ "error": "INVALID_REQUEST", ... }`. O comportamento continuaria fora do pedido nos dois casos.
