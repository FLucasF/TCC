# Cenário 01

## Visão geral

O agente resolveu os planos de tarifa com um Strategy completo. Há uma interface `RatePlanStrategy` com três operações: validar, calcular o preço e calcular a devolução. Cada um dos cinco planos tem a sua implementação, inclusive o normal e o não reembolsável, que já existiam. A escolha acontece num único ponto, um `EnumMap` no `RatePlanRegistry`. A criação e o cancelamento da reserva pedem tudo ao plano e não nomeiam nenhum. Não há `switch` nem `if` sobre o plano fora do registro. A classe abstrata `BaseRatePlanStrategy` guarda o que é comum: a temporada, o arredondamento e a soma das diárias. Todas as regras dos planos conferem, inclusive os exemplos do enunciado. Os problemas mais sérios estão fora dos planos. A segunda via sai também para reserva cancelada. Um corpo que não é JSON (ou com campo de tipo errado) cai, pela leitura, no tratador genérico e vira 500, e não 400.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está em `com.pousada`, dividido em cinco pacotes:

- `controller`: um controlador por recurso (quartos, reservas, mensagens, relatórios). Leem a requisição, validam o formato e montam a resposta em `Map`.
- `service`: `RoomService` tem os cinco quartos fixos num `Map`. `ReservationService` cuida do ciclo de vida da reserva, do comprovante e do e-mail. `MessageService` guarda as mensagens numa lista em memória. `ReportService` gera os CSV.
- `domain`: as entidades e os enums. `RoomType` traz a capacidade e a diária.
- `rateplan`: as estratégias de tarifa.
- `exception`: `BusinessException`, com o código do erro e o status HTTP.

O `App` monta tudo à mão, configura o Jackson, lê a porta de `PORT` (7070 se ela não vier) e registra as rotas e os tratadores de exceção (`src/main/java/com/pousada/App.java:22-76`). O caminho de uma criação é este:

1. `ReservationController.createReservation` lê o corpo, confere os campos obrigatórios, as datas, os hóspedes e o `ratePlan` (`src/main/java/com/pousada/controller/ReservationController.java:26-76`).
2. `ReservationService.createReservation` confere a capacidade, monta um `PricingContext`, pede a estratégia ao registro, valida, calcula o preço, confere a disponibilidade e guarda a reserva numa lista em memória (`src/main/java/com/pousada/service/ReservationService.java:29-74`).

Os testes do agente ficam em `RatePlanTest`, um teste por plano, inclusive os exemplos do enunciado, e em `ReservationServiceTest`, que cobre criação, capacidade, conflito, confirmação, cancelamento não reembolsável, check-in e check-out.

**A parte que pede o pattern.** Os planos ficam assim:

- **A interface.** `RatePlanStrategy` declara `validate`, `calculatePrice` e `calculateRefund` (`src/main/java/com/pousada/rateplan/RatePlanStrategy.java:5-9`).
- **A base comum.** `BaseRatePlanStrategy` traz o arredondamento meio-para-o-par, a regra da alta temporada e a soma das noites (`src/main/java/com/pousada/rateplan/BaseRatePlanStrategy.java:10-49`).
- **`StandardRatePlan`.** Sem validação, sem desconto, devolve tudo (`src/main/java/com/pousada/rateplan/StandardRatePlan.java:8-23`).
- **`NonRefundableRatePlan`.** 15% de desconto, devolução zero (`src/main/java/com/pousada/rateplan/NonRefundableRatePlan.java:14,22`).
- **`CorporateRatePlan`.** Guarda num mapa próprio as empresas e seus descontos, recusa código ausente ou desconhecido e devolve tudo (`src/main/java/com/pousada/rateplan/CorporateRatePlan.java:9-41`).
- **`WeekendPackageRatePlan`.** Exige 2 noites, entrada na sexta e saída no domingo. O preço fechado vem de um mapa por tipo de quarto, e a devolução é metade (`src/main/java/com/pousada/rateplan/WeekendPackageRatePlan.java:11-46`).
- **`LongStayRatePlan`.** Exige 7 noites ou mais, dá 10% sobre as diárias com a temporada valendo e devolve tudo (`src/main/java/com/pousada/rateplan/LongStayRatePlan.java:9-28`).

A escolha é um `EnumMap` preenchido uma vez (`src/main/java/com/pousada/rateplan/RatePlanRegistry.java:9-21`). O serviço só chama `strategy.validate` e `strategy.calculatePrice` na criação (`src/main/java/com/pousada/service/ReservationService.java:48-51`) e `strategy.calculateRefund` no cancelamento (`src/main/java/com/pousada/service/ReservationService.java:133-134`). O enum `RatePlan` é só uma lista de constantes (`src/main/java/com/pousada/domain/RatePlan.java:3-9`).

**O raciocínio.** O que as instruções pedem está aqui:

- cada um dos cinco planos tem preço, validação e devolução numa classe só dele;
- os dois planos antigos também viraram estratégias;
- a seleção acontece num ponto único;
- o serviço não pergunta qual é o plano.

Procurei `switch` e `if` sobre `RatePlan` e não achei nenhum fora do registro. As regras de elegibilidade (sexta a domingo, 7 noites, código da empresa) estão dentro das estratégias, e não no serviço. Um plano novo pede uma classe nova e uma linha no registro. Por isso a classe é **acerto**.

**Exagero colateral.** Não achei estrutura sem necessidade:

- a classe base tem uso real (temporada e soma das noites);
- `PricingContext` e `PricingResult` são objetos de dados simples;
- o registro é o próprio ponto de seleção;
- não há fábricas, builders nem camadas que só repassam.

O campo `guests` do `PricingContext` não é usado (`src/main/java/com/pousada/rateplan/PricingContext.java:11`). É um detalhe, não uma estrutura a mais.

**Comportamento.** As regras dos planos conferem. Pesaram contra, fora dos planos:

- **Segunda via de reserva cancelada.** Ela sai, quando o enunciado a restringe às reservas pagas.
- **Erros de entrada.** Pela leitura, viram 500 em vez de 400.
- **Zeros no JSON.** Saem sem duas casas.

## Item a item

**O pattern pedido.** O agente criou a interface `RatePlanStrategy` (`src/main/java/com/pousada/rateplan/RatePlanStrategy.java:5-9`) com cinco implementações, uma por plano, e uma base abstrata com o cálculo comum. A escolha é um único `EnumMap` (`src/main/java/com/pousada/rateplan/RatePlanRegistry.java:11-17`). A criação usa só a estratégia para validar e precificar (`src/main/java/com/pousada/service/ReservationService.java:48-51`), e o cancelamento só para a devolução (`src/main/java/com/pousada/service/ReservationService.java:133-134`). Os planos que já existiam também estão na estrutura (`src/main/java/com/pousada/rateplan/StandardRatePlan.java`, `src/main/java/com/pousada/rateplan/NonRefundableRatePlan.java`). Não há `switch` de comportamento, e as validações de elegibilidade ficaram dentro de cada plano. É o Strategy que o texto pedia.
Conclusão: acerto

**O exagero colateral.** Não há:

- a interface tem cinco implementações;
- a classe base concentra lógica realmente compartilhada (`src/main/java/com/pousada/rateplan/BaseRatePlanStrategy.java:14-49`);
- `PricingContext` e `PricingResult` são portadores de dados simples;
- não há fábrica, builder nem camada de repasse.

O único resto é o campo `guests` do `PricingContext`, que não é usado (`src/main/java/com/pousada/rateplan/PricingContext.java:11`).
Conclusão: contenção

**O comportamento.** Conferi por leitura do código, sem subir o serviço nem rodar o Maven.

O que confere:

- **Alta temporada.** 249,90 × 1,20 = 299,88, arredondado meio-para-o-par a cada noite (`src/main/java/com/pousada/rateplan/BaseRatePlanStrategy.java:31-36`). O exemplo de julho dá R$ 599,76.
- **Plano normal.** Devolve o que foi pago.
- **Não reembolsável.** 15% de desconto e devolução 0.
- **Convênio.** 12%, 18% e 25%. Código ausente ou fora da lista dá 400 `INVALID_REQUEST` (`src/main/java/com/pousada/rateplan/CorporateRatePlan.java:17-25`). O exemplo da Tecnosul dá 779,80 → desconto 93,58 → total 686,22.
- **Pacote de fim de semana.** Exige 2 noites, de sexta a domingo, com 520, 820 e 1.200 sem temporada. A metade devolvida é 410,00 no exemplo do deluxe (`src/main/java/com/pousada/rateplan/WeekendPackageRatePlan.java:19-45`).
- **Estadia longa.** Recusa menos de 7 noites e dá 10% com temporada. O exemplo dá 1.749,30 → 174,93 → 1.574,37.
- **Cancelamento de pendente.** Devolve 0, porque `amountPaid` é zero.
- **Transições de estado.** Estão como pedido.
- **Conflito de quarto.** Ignora as reservas canceladas.
- **Relatórios.** Seguem o filtro e a ordem pedidos.

O que não confere:

1. **Segunda via de reserva cancelada.** `generateDocument` só recusa a reserva `PENDING`, então a segunda via sai também para reserva `CANCELLED` (`src/main/java/com/pousada/service/ReservationService.java:145`). O enunciado diz que a segunda via só existe para reserva confirmada, com check-in ou com check-out.
2. **Erros de entrada viram 500.** O corpo é lido com `ctx.bodyAsClass(Map.class)` (`src/main/java/com/pousada/controller/ReservationController.java:27`). Os campos são convertidos com casts diretos para `String` (`src/main/java/com/pousada/controller/ReservationController.java:38-42`, `48-49`, `68`). Um corpo que não é JSON, ou um campo de tipo errado, lança uma exceção que não é `BusinessException`. Ela cai no tratador genérico, que responde 500 `INTERNAL_ERROR` (`src/main/java/com/pousada/App.java:51-57`), e não 400 `INVALID_REQUEST`.
3. **Zeros sem duas casas.** Os valores zero saem como `BigDecimal.ZERO`, que no JSON vira `0`, e não `0.00`. Isso acontece em `amountPaid` (`src/main/java/com/pousada/domain/Reservation.java:44`), no desconto do normal e do pacote, e na devolução do não reembolsável.
4. **Formato do dinheiro depende do locale.** `formatMoney` usa `String.format` com o locale padrão da JVM (`src/main/java/com/pousada/service/ReservationService.java:219-223`). Num locale pt-BR, a troca de pontos e vírgulas inverteria o formato.
Conclusão: em parte

## Dúvidas

- **Erro com corpo inválido.** Que o corpo inválido vira 500 é inferência pela leitura. Não confirmei por execução que o Javalin 6.7.0 não traduz para 400 a falha do Jackson em `bodyAsClass`.
- **Peso dos desvios.** Dei "em parte" por desvios fora dos planos, já que todas as regras dos planos conferem. Quem pese só as regras de tarifa poderia ler como "segue".
- **`companyCode` guardado sempre.** O `companyCode` é guardado na reserva para qualquer plano (`src/main/java/com/pousada/domain/Reservation.java:38`), mas só o convênio o usa. Não muda a classe.
