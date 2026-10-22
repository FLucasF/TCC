Já li o código todo. Segue a correção.

# Cenário 01

## Visão geral

O agente organizou os planos de tarifa como Strategy. Há uma classe abstrata `RatePlan`, com cinco subclasses, uma para cada plano, os dois que já existiam inclusive. Cada uma tem a sua validação, o seu preço e a sua devolução. Uma fábrica com um único `switch` escolhe o plano. A criação da reserva e o cancelamento pedem tudo ao plano, sem nomeá-lo. As regras dos planos conferem com o texto, e os três exemplos do financeiro batem. Os problemas mais sérios estão fora dos planos: o check-out devolve a reserva em vez do comprovante, e o relatório de ocupação conta reservas pendentes. Também há um defeito menor: o laço de preço por noite está copiado em quatro classes.

## Como foi implementado, e por que o julgamento

**O projeto.**
- `Application` monta tudo de forma estática e sobe o Javalin na porta de `PORT`, ou na 7070 (`src/main/java/com/pousada/Application.java:31-34`). Ela também define todas as rotas e traduz para HTTP as exceções com códigos em string (`Application.java:274-288`).
- `service/ReservationService` guarda os quartos e as reservas em `HashMap` na memória. Ele cria as reservas, confere a disponibilidade e faz as transições de estado.
- `DocumentService` monta o texto do comprovante e do e-mail. `MessageService` guarda as mensagens enviadas. `ReportService` gera os dois CSV.
- `domain/` tem `Reservation`, `Room`, `RoomType` (o enum com diária e capacidade) e `ReservationStatus`. `api/dto` tem os DTOs de entrada e saída. `util/Money` arredonda com `HALF_EVEN` via `BigDecimal`, mas os valores ficam em `double`.

O caminho de uma criação de reserva é este:
1. `POST /reservations` lê o corpo com Gson e confere os campos obrigatórios.
2. Ele chama `ReservationService.createReservation` (`ReservationService.java:37`), que confere o quarto, as datas, os hóspedes e a capacidade.
3. O serviço obtém o plano por `RatePlanFactory.create` (`:57`), chama `ratePlan.validate` (`:60`, e o erro vira `INVALID_REQUEST`) e confere a disponibilidade.
4. Por fim, pede o preço a `ratePlan.calculatePrices` (`:70`).

O jar é gerado pelo `maven-shade-plugin` com `mainClass` `com.pousada.Application`. Os testes do agente são `TestPricingCalculations`, que chama os planos direto, e `TestIntegration`, que chama o serviço sem passar pelo HTTP.

**A parte que pede o pattern.**
- `RatePlan` (`domain/rateplans/RatePlan.java:6-14`) é abstrata, com `getPlanName`, `validate`, `calculatePrices` e `calculateRefund`.
- **`StandardPlan`**: sem validação; preço por noite com +20% na temporada e sem desconto; devolve tudo (`StandardPlan.java:21-44`).
- **`NonRefundablePlan`**: 15% de desconto e devolução zero (`NonRefundablePlan.java:9`, `:37`, `:45`).
- **`CorporatePlan`**: tem o mapa das empresas (`CorporatePlan.java:11-17`). Recusa código ausente ou fora da lista (`:33`), aplica o desconto da empresa (`:55`) e devolve tudo (`:63`).
- **`WeekendPackagePlan`**: exige sexta, o domingo seguinte e duas noites (`WeekendPackagePlan.java:28-40`). O preço é fechado por tipo de quarto, sem temporada e sem desconto (`:12-18`, `:44-47`), e devolve metade (`:51`).
- **`LongStayPlan`**: exige pelo menos 7 noites (`LongStayPlan.java:20-23`), dá 10% com a temporada valendo (`:31-41`) e devolve tudo (`:49`).
- A escolha é um único `switch` em `RatePlanFactory.java:5-12`. A reserva guarda o nome do plano como `String` (`Reservation.java:14`).
- No cancelamento, a rota recria o plano pela mesma fábrica (`Application.java:135`). O serviço só chama `ratePlan.calculateRefund(amountPaid)` (`ReservationService.java:159`).

**O raciocínio.**
- **O que o aproxima do pattern:** há uma implementação por plano, os dois antigos também. Preço, validação e devolução de cada plano ficam todos na classe dele. Há um só ponto de seleção, e procurei `switch` e `if` sobre o plano em serviço, rotas, relatórios e documento sem achar nenhum. Um plano novo é uma classe nova mais uma linha na fábrica. Isso é exatamente o caso de `acerto` das instruções.
- **O que o afasta um pouco, sem mudar a classe:**
  - O laço de soma das noites com temporada está copiado em quatro planos (`StandardPlan.java:25-32`, `NonRefundablePlan.java:27-34`, `CorporatePlan.java:45-52`, `LongStayPlan.java:31-38`). É duplicação, não decisão pelo plano.
  - O código da empresa entra duas vezes no `CorporatePlan`: pelo construtor, usado no preço (`CorporatePlan.java:21`, `:40`), e pelo parâmetro de `validate`, usado na validação (`:31-33`).
  - No cancelamento, o plano é recriado com `companyCode` `null` (`Application.java:135`). Isso só funciona porque a devolução do convênio não depende da empresa.
- **Exagero colateral:** a fábrica é o ponto de seleção do próprio pattern. Fora dos planos, não há interface de implementação única, builder nem camada que só repassa, a não ser o trivial `Money.formatForCsv` (`Money.java:19-21`). Conta como contenção.
- **Comportamento:** as regras dos planos estão certas. O sistema em volta tem falhas, listadas no item abaixo.

## Item a item

**O pattern pedido.** A classe abstrata `RatePlan` (`domain/rateplans/RatePlan.java:6-14`) tem cinco implementações: `StandardPlan`, `NonRefundablePlan`, `CorporatePlan`, `WeekendPackagePlan` e `LongStayPlan`. Cada uma tem a sua `validate`, o seu `calculatePrices` e o seu `calculateRefund`. A seleção é um único `switch` que só escolhe a implementação (`RatePlanFactory.java:5-12`). A criação chama `validate` e `calculatePrices` sem nomear o plano (`ReservationService.java:57-70`), e o cancelamento chama `calculateRefund` (`ReservationService.java:159`). Não sobra `switch` nem `if` de comportamento sobre o plano em nenhum outro lugar. É o Strategy que o texto pedia, com os planos antigos dentro da estrutura.
Conclusão: acerto

**O exagero colateral.** Não há. `RatePlanFactory` é o ponto único de seleção que o próprio pattern pede, e as demais classes (serviços, DTOs, `Money`, `DateUtils`) têm papel real. O único repasse é `Money.formatForCsv` (`util/Money.java:19-21`), irrelevante.
Conclusão: contenção

**O comportamento.** Conferi por leitura, sem subir o serviço nem rodar nada.

O que confere:
- **Convênio:** 12, 18 e 25% (`CorporatePlan.java:14-16`). Sem código ou com código fora da lista dá 400 `INVALID_REQUEST` (`CorporatePlan.java:33`, `ReservationService.java:61-62`). Outros planos ignoram o `companyCode`.
- **Pacote:** só de sexta ao domingo logo depois (`WeekendPackagePlan.java:28-40`). Custa 520, 820 ou 1.200, sem temporada e sem desconto (`:12-18`, `:46`), e devolve metade (`:51`).
- **Estadia longa:** exige 7 noites ou mais e dá 10% com temporada (`LongStayPlan.java:20-41`).
- **Devolução integral:** convênio e estadia longa devolvem tudo.
- **Exemplos do texto:** os três batem. No convênio, 779,80 − 93,58 = 686,22. No pacote, 820,00, que vira 410,00 de devolução. Na estadia longa, 1.749,30 − 174,93 = 1.574,37.
- **Planos antigos:** o normal e o não reembolsável seguem o ponto de partida. Uma reserva pendente devolve 0.

O que não confere no resto do sistema:
- **Check-out:** devolve a reserva, não o comprovante (`Application.java:180-182`).
- **Relatório de ocupação:** conta reservas pendentes, porque só exclui as canceladas (`ReservationService.java:166`). O texto pede só as pagas.
- **Dinheiro no JSON:** é `double`, então 820,00 sai como `820.0`, sem as 2 casas (`ReservationResponse.java:150-155`).
- **Datas do comprovante em JSON:** saem como `dd/MM/yyyy` (`DocumentResponse.java:185`). O resto da API usa `2027-07-10`.
- **Valor pago:** a confirmação aceita um `amountPaid` vindo do corpo (`Application.java:102-108`). O texto diz que o pago é o total.
Conclusão: em parte

## Dúvidas

- O cancelamento recria o plano a partir do nome guardado e passa `companyCode` `null` (`Application.java:135`). Hoje isso é inofensivo, mas quebraria se a devolução do convênio passasse a depender da empresa. Não mudei a classe por isso.
- O laço de preço copiado em quatro planos é duplicação, não seleção pelo plano. Tratei como defeito menor, não como sinal de `malfeito`.
- Os planos usam `double` arredondado a cada passo. Os exemplos do texto batem, mas um valor exatamente na metade do centavo pode cair do lado errado por erro de representação. Não consegui confirmar isso só pela leitura.
