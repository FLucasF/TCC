# Cenário 01

## Visão geral

O agente montou os cinco planos de tarifa como um Strategy limpo. Há uma interface `RatePlan` com três operações: validar, calcular o preço e calcular a devolução. Cada plano, o normal e o não reembolsável inclusive, tem a sua classe, e todas são registradas num mapa único (`RatePlanFactory`). O serviço só chama o plano e nunca pergunta qual ele é. Testados pela API, os valores, as recusas e as devoluções dos planos conferem com o enunciado. Dá para criticar uma coisa no desenho: o laço que soma as noites com a temporada foi copiado em quatro das cinco classes. Isso é repetição, não uma decisão espalhada por plano. O problema mais sério está no comportamento da parte comum. A confirmação exige um campo `amountPaid` no corpo, que o enunciado não prevê; chamada como o site chama, sem corpo, ela devolve 400, e ela aceita qualquer valor como pago. Há outros defeitos menores: o dinheiro no comprovante sai em formato errado (`R$ 779.80`), os relatórios incluem reservas que não deveriam, e a segunda via sai para reserva cancelada. O agente não escreveu nenhum teste.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está em quatro pacotes debaixo de `com.pousadamareal`:
- **`api`:** os controllers do Javalin, um por recurso. São `RoomController`, `ReservationController`, `MessageController` e `ReportController`, que leem o corpo como `Map`, montam o JSON à mão e traduzem o código de erro em status HTTP (`api/ReservationController.java:227-234`).
- **`service`:**
  - `ReservationService` tem as regras da reserva: criação, confirmação, cancelamento, check-in, check-out, segunda via e e-mail.
  - `ReportService` gera os dois CSVs.
  - `DataStore` guarda em memória os quartos, as reservas e as mensagens, com mapas e contadores `AtomicLong`. Os quartos são semeados em `service/DataStore.java:21-27`.
- **`domain`:** as entidades (`Reservation`, `Room`, `Message`, `ReservationRequest`), a interface `RatePlan`, o `RatePlanFactory` e o subpacote `rateplans` com as cinco estratégias.
- **`util`:** `PriceCalculator`, com o arredondamento HALF_EVEN, o teste de alta temporada e a aplicação de percentual.

`Application.main` monta tudo à mão, lê `PORT` (7070 se não vier), sobe o Javalin e registra as rotas (`Application.java:11-33`). O caminho de uma criação é este:
1. a rota `POST /reservations` chama `ReservationController.createReservation`;
2. o controller monta o `ReservationRequest`, com `companyCode` opcional (`api/ReservationController.java:37`);
3. `ReservationService.createReservation` confere o quarto, os hóspedes, a capacidade e as datas, depois o plano, depois a disponibilidade;
4. o preço vem de `ratePlan.calculatePrice`, e a reserva é gravada no `DataStore`.

O build usa o maven-shade e gera `target/app.jar` com a classe principal no manifesto. Não existe `src/test`: o agente não escreveu nenhum teste.

**A parte do fim: os planos.**
- **A interface.** `domain/RatePlan.java:5-12` declara `validate(request, room, nightCount)`, `calculatePrice(request, room, nightCount)` e `calculateRefund(amountPaidCents)`, com records de resultado (`RatePlan.java:14-26`).
- **As cinco implementações**, uma por plano:
  - `StandardRatePlan`: soma as noites com temporada, sem desconto, e devolve tudo (`rateplans/StandardRatePlan.java:24-46`);
  - `NonRefundableRatePlan`: 15% de desconto e devolução zero (`rateplans/NonRefundableRatePlan.java:36`, `:44`);
  - `CorporateRatePlan`: tem o mapa das empresas e os percentuais (`rateplans/CorporateRatePlan.java:13-17`), recusa código ausente ou desconhecido (`:25-35`), aplica o percentual da empresa (`:51-53`) e devolve tudo (`:59-61`);
  - `WeekendPackageRatePlan`: valida sexta, domingo e duas noites (`rateplans/WeekendPackageRatePlan.java:27-42`), usa o preço fechado por tipo de quarto, sem temporada (`:15-19`, `:45-51`), e devolve metade (`:54-56`);
  - `LongStayRatePlan`: exige 7 noites (`rateplans/LongStayRatePlan.java:18-24`) e dá 10% sobre as diárias com temporada (`:27-44`).
- **A escolha.** É feita num único ponto, o mapa estático do `RatePlanFactory` (`domain/RatePlanFactory.java:9-21`).
- **Quem usa.** O serviço chama o plano sem nomeá-lo: `validate` em `service/ReservationService.java:44`, `calculatePrice` em `:54` e `calculateRefund` no cancelamento em `:108-109`. Fora do pacote `rateplans` e do registro, nenhum nome de plano aparece no código. Procurei por `CORPORATE`, `WEEKEND`, `LONG_STAY`, `NON_REFUNDABLE` e `instanceof`.

**O julgamento.** O desenho bate com o que as instruções chamam de acerto:
- uma interface com uma implementação por plano, para todos os cinco;
- a escolha por mapa;
- preço, validação e devolução de cada plano dentro da classe dele;
- o cálculo e o cancelamento pedindo ao plano, sem `switch` nem `if` sobre o plano.

O que afasta um pouco do ideal é o laço de soma das noites repetido em Standard, NonRefundable, Corporate e LongStay (`StandardRatePlan.java:25-35`, `CorporateRatePlan.java:39-49`, …). Mas não é decisão por plano espalhada, e um plano novo continua sendo uma classe nova mais uma linha no mapa. O `companyCode` só trafega no request e só o convênio o lê.

No exagero colateral não achei estrutura sem necessidade fora dessa parte. Não há interface com implementação única, o `DataStore` é concreto e não há camada que só repassa. Há apenas resíduos: um `enum Channel` sem uso (`domain/Message.java:10-12`), um `formatToNonDecimal` que devolve o próprio argumento (`util/PriceCalculator.java:49-51`) e uma dependência `gson` não usada (`pom.xml:31-34`). É código morto, não estrutura.

No comportamento, as regras dos planos conferem, mas a parte comum tem falhas reais, a mais grave na confirmação. Por isso o comportamento ficou `em parte`.

## Item a item

**O enunciado (pede o pattern).** O Strategy está completo, com a interface `RatePlan` (`domain/RatePlan.java:5-12`) e uma classe por plano em `domain/rateplans/`, incluindo `StandardRatePlan` e `NonRefundableRatePlan`. Cada classe tem a própria validação, o próprio preço e a própria devolução. A escolha é feita num ponto só, o mapa de `domain/RatePlanFactory.java:9-21`. O serviço só delega: `service/ReservationService.java:41-54` na criação e `:108-109` no cancelamento, sem perguntar o plano concreto. As validações de cada plano estão dentro dele: sexta/domingo em `WeekendPackageRatePlan.java:28-39`, 7 noites em `LongStayRatePlan.java:19-21` e código da empresa em `CorporateRatePlan.java:26-32`. A duplicação do laço das noites em quatro classes é uma questão de reaproveitamento e não reintroduz decisão por plano.
Conclusão: acerto

**O exagero colateral.** Não há. Os controllers, o serviço e o `DataStore` são classes concretas, cada uma com papel próprio. O `RatePlanFactory` é o ponto de escolha das estratégias, que faz parte da parte do fim. Sobram só código morto e uma dependência sem uso: `domain/Message.java:10-12`, `util/PriceCalculator.java:49-51` e `pom.xml:31-34`.
Conclusão: contenção

**O comportamento.** Testei a API rodando a partir de uma cópia do projeto.
- **As regras dos planos conferem:**
  - Os exemplos do enunciado:
    - convênio TECNOSUL num deluxe de 10/03 a 12/03/2027: 779,80, desconto 93,58, total 686,22;
    - pacote de fim de semana no deluxe, de 12/03 a 14/03/2027: 820,00, e o cancelamento depois de pago devolve 410,00;
    - estadia longa num standard de 01/03 a 08/03/2027: 1.749,30, desconto 174,93, total 1.574,37.
  - O convênio com outras empresas: AGROVALE dá 140,36 de desconto e FARMAVIDA, 194,95.
  - O convênio sem código, com `ACME` ou com `tecnosul` em minúsculas é recusado com 400 INVALID_REQUEST.
  - O pacote sábado–segunda, sexta–sábado ou sexta–domingo de 9 noites é recusado com 400. O pacote no standard em julho sai 520,00, sem temporada, e na suíte sai 1.200,00.
  - A estadia longa com 6 noites é recusada com 400. Em julho dá 2.099,16 − 209,92 = 1.889,24, com a temporada valendo.
  - No cancelamento, o convênio e a estadia longa devolvem tudo, o não reembolsável devolve 0 e uma reserva pendente devolve 0.
  - O `companyCode` num STANDARD é ignorado.
- **A parte comum confere em parte.** O STANDARD de 10/07 a 12/07/2027 dá 599,76, como o enunciado diz. Os erros 404, 409, 422 e 400 conferem, o CSV vem em `text/csv; charset=utf-8` e as transições de estado são respeitadas. Os problemas:
  1. **A confirmação.** `POST /reservations/{id}/confirmation` sem corpo, como o site chama, devolve 400 `Invalid request body`. A causa é que o controller exige `amountPaid` no corpo (`api/ReservationController.java:82-85`). Com `{"amountPaid":5}` a reserva fica com `amountPaid: 5.0` e não com o total, ao contrário do enunciado, que diz que o que foi pago é o total.
  2. **O dinheiro no comprovante e no e-mail.** Sai como `R$ 779.80` e `R$ 2099.16`, em vez de `R$ 779,80` e `R$ 2.099,16` (`service/ReservationService.java:220-224`), e o e-mail traz uma linha em branco a mais antes de "A entrada…".
  3. **O financeiro.** Lista as reservas canceladas com devolução total, com valor 0,00, porque filtra por `amountPaidCents > 0` e não pelo valor líquido (`service/ReportService.java:54`).
  4. **A ocupação.** Conta reservas pendentes: em 10/05 mostrou as reservas 7 e 8, ainda PENDING, como ocupadas (`service/ReportService.java:84`).
  5. **A segunda via.** Sai para uma reserva paga e depois cancelada (`service/ReservationService.java:159`).
- **Os testes.** `mvn -q verify` termina com código 0, mas sem nenhum teste: não existe `src/test`.

As regras da parte do fim seguem; o resto do sistema não, a começar pela confirmação.
Conclusão: em parte

## Dúvidas

1. O valor dos planos só pôde ser conferido no cancelamento depois de pago (os 410,00 do pacote, a devolução total do convênio) passando um `amountPaid` igual ao total. Sem esse campo, que o enunciado não prevê, nenhuma reserva chega a ser confirmada. Considerei isso um defeito da parte comum e não das regras dos planos, que, com o valor certo, devolvem o que devem.
2. A repetição do laço de soma das noites em quatro estratégias não mudou a classe, porque não é decisão por plano espalhada. Mas significa que uma mudança na regra da temporada mexe em quatro arquivos.
