# Cenário 01

## Visão geral

O agente montou os cinco planos de tarifa como um Strategy: uma interface `PricingStrategy`, uma classe por plano (o normal e o não reembolsável também) e um único `switch` que só escolhe a classe. Cada classe guarda as próprias regras: o preço, quem pode usar (código da empresa, sexta a domingo, 7 noites) e quanto devolve. O serviço de reservas não cita nenhum plano pelo nome, nem ao calcular o preço nem ao cancelar. Esse é o lado bom. O lado fraco: a regra de devolução não tem um método próprio. Ela é um campo `refundPercentage` que volta junto com o preço, e para descobrir quanto devolver o cancelamento calcula o preço inteiro de novo (`service/pricing/PricingStrategy.java:15`, `service/ReservationService.java:124-135`). O laço que soma as noites também está copiado em quatro classes. Nos testes, as regras dos cinco planos conferem todas. Os problemas estão na parte comum: o dinheiro sai como `R$ 820.00` e `R$ 1749.30` no comprovante e no e-mail, e o relatório financeiro lista reservas que devolveram tudo, com valor 0.00.

## Como foi implementado, e por que o julgamento

**O projeto.** `Application` (`Application.java:13-68`) monta tudo à mão: cria os serviços (`RoomService`, `MessageService`, `ReservationService`, `ReportService`), passa cada um ao seu controller, configura um `JsonMapper` com Gson (`Application.java:26-49`) e registra as rotas (`Application.java:52-65`). A porta vem de `PORT`, e é 7070 quando ela falta (`Application.java:70-80`). O pacote `api` tem os controllers. Eles leem o corpo como `JsonObject`, conferem os campos obrigatórios, convertem datas e plano (`api/ReservationController.java:22-89`), chamam o serviço e montam DTOs de resposta. O pacote `service` tem as regras: `ReservationService` cuida da criação, das transições de estado, da disponibilidade, do comprovante e do e-mail, e `ReportService` gera os dois CSVs à mão. `RoomService` tem os cinco quartos fixos (`service/RoomService.java:16-22`) e `MessageService` guarda os e-mails numa lista. Todos os dados ficam em listas na memória, dentro dos serviços (`service/ReservationService.java:14`). O caminho de uma criação é este: rota `POST /reservations`, depois `ReservationController.createReservation`, depois `ReservationService.createReservation`. O serviço confere hóspedes, datas, quarto, capacidade e disponibilidade, e então pede o preço à estratégia do plano (`service/ReservationService.java:45-46`). O `pom.xml` usa o shade para gerar um jar que sobe sozinho. Os testes do agente são 14 testes unitários em JUnit 4: 9 em `PricingStrategyTest`, que chamam cada estratégia direto e incluem os três exemplos do enunciado, e 5 em `MoneyRounderTest`. Não há teste da API.

**A parte dos planos.** `RatePlan` é um `enum` sem comportamento (`domain/RatePlan.java:3-9`). A interface `PricingStrategy` tem um método só, `calculatePrice(room, checkIn, checkOut, nights, companyCode)`, que devolve um `PricingResult` com subtotal, desconto, total e `refundPercentage` (`service/pricing/PricingStrategy.java:8-23`). A escolha acontece num único `switch` em `PricingStrategyFactory.getStrategy` (`service/pricing/PricingStrategyFactory.java:7-13`), e esse `switch` só cria a classe. Cada regra fica dentro da estratégia do seu plano:

- `StandardPricingStrategy`: soma as noites com a temporada, sem desconto, devolução 100 (`StandardPricingStrategy.java:17-31`);
- `NonRefundablePricingStrategy`: 15% de desconto, devolução 0 (`NonRefundablePricingStrategy.java:28-31`);
- `CorporatePricingStrategy`: tem o mapa das empresas (`CorporatePricingStrategy.java:13-19`), recusa código ausente ou desconhecido com 400 `INVALID_REQUEST` (`:23-25`), aplica o desconto da empresa e devolve 100 (`:41-45`);
- `WeekendPackagePricingStrategy`: exige 2 noites, entrada na sexta e saída no domingo (`WeekendPackagePricingStrategy.java:22-32`), usa o preço fechado por tipo de quarto (`:12-18`, `:34`), ignora a temporada e devolve 50 (`:42`);
- `LongStayPricingStrategy`: exige 7 noites ou mais (`LongStayPricingStrategy.java:14-16`), dá 10% sobre as diárias com a temporada e devolve 100 (`:32-35`).

No cancelamento, o serviço pega a estratégia pela fábrica, chama `calculatePrice` de novo só para ler o `refundPercentage` e multiplica pelo que foi pago (`service/ReservationService.java:124-140`). Se a estratégia lançar exceção, a devolução vira 0 sem aviso (`:133-135`).

**O raciocínio.** As instruções dizem que é acerto uma interface com uma implementação por plano, escolhida por um único ponto de seleção, desde que preço, validação e devolução fiquem no plano e o cálculo e o cancelamento não citem planos. É o que este código faz. Os cinco planos estão na estrutura, sem nenhum de fora num `switch`. A validação de cada plano está dentro da estratégia, não no serviço. O único `switch` sobre `RatePlan` no projeto é o da fábrica, que só escolhe a classe. O código tem pontos que o afastam do desenho ideal, mas nenhum deles bate com a definição de malfeito:

- a devolução é um dado que vem junto com o resultado do preço, e não um método. Por isso o cancelamento refaz o cálculo inteiro e engole exceções. Um plano futuro cuja devolução dependa da data (o de 60 dias de antecedência, por exemplo) obrigaria a mudar a interface;
- o `companyCode` faz parte da assinatura de todas as estratégias, mas só o convênio o usa;
- o laço das noites com a temporada está copiado em quatro estratégias (`StandardPricingStrategy.java:17-26`, `NonRefundablePricingStrategy.java:17-26`, `CorporatePricingStrategy.java:30-39`, `LongStayPricingStrategy.java:21-30`). Isso é duplicação, não decisão por plano.

Quanto ao exagero colateral: fora da parte dos planos, as camadas fazem trabalho de verdade. Não há interface com uma implementação só, nem fábrica ou builder sobrando. A `PricingStrategyFactory` pertence à parte dos planos e é o ponto de seleção que as instruções aceitam. Quanto ao comportamento, todas as regras dos planos conferiram nos testes. Os desvios estão na parte comum: o formato do dinheiro no texto do comprovante (`service/ReservationService.java:250-252`), o filtro do relatório financeiro (`service/ReportService.java:68-70`) e o JSON com números sem as 2 casas e sem o `refund: null` (`Application.java:27-28`).

## Item a item

**O enunciado (pede o pattern).** Há uma interface `PricingStrategy` (`service/pricing/PricingStrategy.java:8-9`) com cinco implementações, uma por plano, incluindo `StandardPricingStrategy` e `NonRefundablePricingStrategy`. A escolha acontece num único `switch` que só cria a classe (`service/pricing/PricingStrategyFactory.java:7-13`). Preço, validação (`CorporatePricingStrategy.java:23-25`, `WeekendPackagePricingStrategy.java:22-32`, `LongStayPricingStrategy.java:14-16`) e percentual de devolução (`WeekendPackagePricingStrategy.java:42`, `NonRefundablePricingStrategy.java:31`) ficam cada um na classe do seu plano. A criação e o cancelamento só pedem à estratégia (`service/ReservationService.java:45-46`, `:124-132`) e não citam nenhum plano. Para criar um plano novo, basta uma classe e uma linha na fábrica. Isso é o Strategy que o enunciado pede. A forma da devolução (um campo no resultado do preço, obtido refazendo o cálculo) é desajeitada, mas continua dentro do plano.
Conclusão: acerto

**O exagero colateral.** Não há. Os serviços são classes concretas sem interfaces sobrando (`service/RoomService.java:9`, `service/MessageService.java:8`). Os controllers convertem dados de verdade (`api/ReservationController.java:175-211`) e a montagem em `Application.java:16-24` é direta. A única fábrica do projeto é o ponto de seleção do Strategy, dentro da parte dos planos.
Conclusão: contenção

**O comportamento.** Usei uma cópia do projeto, rodei `mvn -q package -DskipTests` e subi `target/mare-alta-1.0.0.jar` na porta 7070. Chamei a API com `curl`.

Parte dos planos, tudo conferiu:
- convênio TECNOSUL no 201 de 10 a 12/03/2027: subtotal 779.8, desconto 93.58, total 686.22. AGROVALE no 202: 779.8, 140.36, 639.44. FARMAVIDA na 301: 1159.8, 289.95, 869.85;
- convênio sem código, com `ACME` ou com `tecnosul`: 400 `INVALID_REQUEST`;
- pacote no 201 de 12 a 14/03: total 820.0. Confirmado e depois cancelado: refund 410.0;
- pacote no 101 em julho: 520.0, sem temporada. Na 301: 1200.0;
- pacote de quinta a sábado, ou de sexta a segunda: 400 `INVALID_REQUEST`;
- estadia longa no 101 de 01 a 08/03: 1749.3, 174.93, 1574.37. Com 6 noites: 400. De 28/06 a 05/07 a temporada vale: subtotal 1949.22, desconto 194.92, total 1754.3;
- cancelamento depois de pagar: convênio e estadia longa devolvem tudo (686.22 e 1574.37), o normal devolve 599.76, o não reembolsável devolve 0.0. Um pacote ainda pendente cancelado devolve 0.0.

Parte comum:
- conferiu: STANDARD no 101 de 12 a 14/07 dá 599.76; NON_REFUNDABLE em julho dá 509.8; 409 `ROOM_UNAVAILABLE`, 422 `CAPACITY_EXCEEDED`, 404 `ROOM_NOT_FOUND` e `RESERVATION_NOT_FOUND`, 400 para JSON inválido, campo faltando e mês inválido; o cancelamento depois do check-in dá 409; o relatório de ocupação (`total,2/5`, `text/csv;charset=utf-8`) está certo;
- falhou: o texto do comprovante no e-mail sai `Diárias: R$ 1749.30` e `Total: R$ 820.00`, em vez de `R$ 1.749,30` e `R$ 820,00`. A causa é `String.format("R$ %.2f")`, que depende do locale e nunca põe o separador de milhar (`service/ReservationService.java:250-252`);
- falhou: o financeiro de 2027-03 lista as reservas 7 e 1, canceladas com devolução total, com `0.00`. A de 2027-07 lista a 9 do mesmo jeito. Pelo enunciado, elas não deixaram dinheiro com a pousada e não deveriam aparecer (`service/ReportService.java:68-70`). O pacote cancelado com 410.00 e o não reembolsável cancelado com 509.80 aparecem certos;
- desvios menores: o JSON sai com 779.8 e 820.0 em vez de 2 casas, porque o Gson serializa via `doubleValue()` (`Application.java:27-28`); o campo `refund` some em vez de vir `null`; a segunda via de uma reserva pendente responde 400 `INVALID_REQUEST` em vez de 409 `OPERATION_NOT_ALLOWED` (`service/ReservationService.java:154`).

O `mvn verify` da cópia passou: 14 testes, 0 falhas, `BUILD SUCCESS`. Derrubei o serviço no fim.
Conclusão: em parte

## Dúvidas

- A devolução não é um método da estratégia. Ela é o campo `refundPercentage` no `PricingResult` (`service/pricing/PricingStrategy.java:15`), e o cancelamento só chega nele chamando `calculatePrice` de novo (`service/ReservationService.java:124-135`). Se a estratégia lançar exceção nesse segundo cálculo, a devolução vira 0 sem aviso. A regra continua no plano, por isso mantive `acerto`, mas o encaixe é frágil e não acomoda devoluções que dependam da data do cancelamento.
- O `companyCode` faz parte da assinatura comum de todas as estratégias (`service/pricing/PricingStrategy.java:9`), e a reserva o guarda em qualquer plano (`service/ReservationService.java:58`). É uma regra do convênio vazando para a interface. É pequena e não é de preço nem de devolução, então não mudou a classe.
