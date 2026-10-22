# Cenário 01

## Visão geral

O agente montou um Strategy completo para os planos de tarifa. A interface `RatePlanStrategy` tem três métodos: validar, calcular o preço e calcular a devolução. Cada um dos cinco planos tem a sua classe, os dois que já existiam inclusive. Uma fábrica com um único `switch` escolhe a implementação, e tanto a criação quanto o cancelamento da reserva pedem ao plano o que precisam, sem nomear nenhum. Não há outro `switch` ou `if` sobre o plano em nenhum lugar do código, e as regras dos planos (valores, recusas, devoluções e os exemplos do texto) conferem na leitura. O problema mais sério está fora do pattern, no resto do sistema. A confirmação exige do cliente um `amountPaid` no corpo, em vez de usar o total da reserva. A capacidade excedida volta 400 em vez de 422. O dinheiro no comprovante e no e-mail sai como "R$ 779.80". E o relatório financeiro inclui reservas pendentes e mostra o valor pago, não o que ficou com a pousada.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `com.pousadamareaita` tem estas partes:

- `Main`: monta os repositórios, os serviços e os controllers à mão e sobe o Javalin na porta de `PORT`, ou na 7070 (`Main.java:13`, `Main.java:38`).
- `http`: quatro controllers (quartos, reservas, mensagens e relatórios) e os DTOs de saída.
- `service`: `ReservationService` (ciclo de vida da reserva, e-mail e comprovante) e `ReportService` (os CSV).
- `storage`: repositórios em memória com mapas `static`. É por serem `static` que funciona o `ReservationController` criar o seu próprio `RoomRepository` (`ReservationController.java:25`).
- `model`: as entidades e os `enum`s.
- `pricing`: os planos.
- `exception`: as exceções com status e código.

Uma requisição de criação segue este caminho:

1. O controller lê o corpo como `Map` e extrai os campos, inclusive o opcional `companyCode` (`ReservationController.java:40-50`).
2. O serviço busca o quarto e obtém a estratégia na fábrica (`ReservationService.java:31`).
3. O serviço chama `validate` e converte o `IllegalArgumentException` em `InvalidRequestException` (`ReservationService.java:33-37`).
4. O serviço confere a disponibilidade do quarto e chama `calculatePrice` (`ReservationService.java:41`).
5. A reserva é gravada com noites, subtotal, desconto e total.

O cancelamento pede de novo a estratégia à fábrica e chama `calculateRefund(amountPaid)` (`ReservationService.java:114-121`).

Há dois arquivos de teste, ambos de unidade sobre o pricing. `RatePlanStrategyTest` cobre os cinco cálculos de preço com os exemplos do texto, e `PricingUtilsTest` cobre os utilitários. Não há teste de HTTP.

**A parte que pede o pattern.**

- **A interface.** `RatePlanStrategy` (`pricing/RatePlanStrategy.java:6-12`) declara `validate`, `calculatePrice` e `calculateRefund`.
- **Normal.** `StandardPlanStrategy` soma as noites com a temporada, sem desconto, e devolve tudo (`StandardPlanStrategy.java:30-39`).
- **Não reembolsável.** `NonRefundablePlanStrategy` aplica 15% e devolve zero (`NonRefundablePlanStrategy.java:31`, `:39`).
- **Convênio.** `CorporatePlanStrategy` guarda o mapa das empresas (`CorporatePlanStrategy.java:8-12`) e recusa código ausente ou desconhecido (`:22-24`). Aplica a taxa da empresa (`:47-48`) e devolve tudo (`:56`).
- **Pacote de fim de semana.** `WeekendPackagePlanStrategy` exige entrada na sexta, saída no domingo e exatamente 2 noites (`WeekendPackagePlanStrategy.java:24-33`). Usa o preço fechado por tipo de quarto (`:10-14`, `:45-48`) e devolve metade (`:55`).
- **Estadia longa.** `LongStayPlanStrategy` exige 7 noites ou mais (`LongStayPlanStrategy.java:10-12`), dá 10% com a temporada valendo (`:29-35`) e devolve tudo (`:43`).
- **A escolha.** O único ponto de seleção é `RatePlanStrategyFactory.create`, com um `switch` que só instancia (`RatePlanStrategyFactory.java:8-14`).
- **Fora da estrutura.** Uma busca por `switch` e por `RatePlan.` no código principal só encontra esse `switch`. O resto é a leitura do campo e o DTO.

**O raciocínio.** O que as instruções pedem está todo aqui: cada um dos cinco planos tem preço, validação e devolução numa classe só dele, há um único `switch` de seleção, e quem usa a estrutura não pergunta qual é o plano. Isso é acerto.

Há dois defeitos de acabamento, que não mudam a classe:

- **A regra geral copiada.** As regras gerais (hóspedes ≥ 1, capacidade, saída depois da entrada) foram copiadas para dentro de cada estratégia (por exemplo, `StandardPlanStrategy.java:9-17`). Isso duplica código e causa um erro de comportamento: capacidade vira 400.
- **O código da empresa em dois lugares.** O código chega tanto no construtor quanto no parâmetro de `validate` (`CorporatePlanStrategy.java:16`, `:21`). A interface ficou com um parâmetro que só serve a um plano.

**O exagero colateral.** A fábrica é o ponto de seleção que o pattern aceita, e `PriceCalculation` é só um valor de retorno. Não pesei nenhum dos dois como exagero, e não há interface de implementação única, builder nem camada que só repasse.

**O comportamento.** Pesou o resto do sistema, que tem falhas sérias, enquanto as regras dos planos conferem.

## Item a item

**O pattern pedido.** Há uma interface `RatePlanStrategy` (`pricing/RatePlanStrategy.java:6-12`) e cinco implementações, uma por plano. Os dois planos que já existiam também estão nela (`StandardPlanStrategy.java`, `NonRefundablePlanStrategy.java`), ao lado dos três novos (`CorporatePlanStrategy.java`, `WeekendPackagePlanStrategy.java`, `LongStayPlanStrategy.java`). Cada uma tem a sua validação, o seu preço e a sua devolução. O único `switch` sobre o plano só escolhe a implementação (`RatePlanStrategyFactory.java:8-14`). A criação (`ReservationService.java:31-41`) e o cancelamento (`ReservationService.java:114-120`) só conversam com a interface. Não sobrou `switch` ou `if` de comportamento sobre o plano, e o próximo plano exige uma classe nova mais uma linha na fábrica. É o Strategy que o texto pedia.
Conclusão: acerto

**O exagero colateral.** Não há. `RatePlanStrategyFactory` é o ponto de seleção único, que as instruções aceitam (`RatePlanStrategyFactory.java:7-15`). `PriceCalculation` é um objeto de valor com quatro campos (`PriceCalculation.java:5-33`). Não há interface com uma implementação só, builder nem camada que só repasse.
Conclusão: contenção

**O comportamento.** Conferi por leitura do código, sem subir o serviço nem rodar testes.

Regras dos planos, todas conferem:
- Convênio: 12%, 18% e 25% (`CorporatePlanStrategy.java:8-12`). Código ausente ou fora da lista dá 400 `INVALID_REQUEST` (`CorporatePlanStrategy.java:22-24`, `ReservationService.java:35-36`). O exemplo da Tecnosul dá 779,80, depois 93,58 (93,576 arredondado meio para o par) e 686,22.
- Pacote: só sexta → domingo com 2 noites. O preço é fechado (520, 820 e 1.200), sem temporada. A devolução é metade do pago, então o exemplo dá 820,00 e depois 410,00.
- Estadia longa: 7 noites ou mais e 10% com temporada. O exemplo dá 1.749,30, depois 174,93 e 1.574,37.
- Normal e não reembolsável: a temporada de 20% com arredondamento por noite está em `PricingUtils.java:13-37`, e o exemplo de julho dá 299,88 × 2 = 599,76.
- Devoluções: convênio e estadia longa devolvem tudo. Uma reserva pendente devolve 0, porque `amountPaid` começa em zero.

Problemas no resto do sistema:
- **Confirmação.** Exige `amountPaid` no corpo (`ReservationController.java:79-80`) e grava o que o cliente mandou (`Reservation.java:115-118`). Pelo texto, o valor pago é o total da reserva. Sem esse corpo, a confirmação dá 400, e com ela o resto do ciclo para.
- **Capacidade.** Capacidade excedida vira 400 `INVALID_REQUEST`, porque a checagem está nas estratégias e lança `IllegalArgumentException` (`StandardPlanStrategy.java:12-13`). `CapacityExceededException` nunca é usada, e o 422 `CAPACITY_EXCEEDED` não existe na prática.
- **Formato do dinheiro.** `formatCurrency` usa `String.format("R$ %,.2f")` e depois troca toda vírgula por ponto (`ReservationService.java:196`). O comprovante e o e-mail saem "R$ 779.80" ou "R$ 1.234.56", e não "R$ 779,80".
- **Relatório financeiro.**
  - Não exclui as reservas pendentes (`ReportService.java:80-93`).
  - A coluna `amount` mostra o valor pago bruto, e não o que ficou com a pousada (`ReportService.java:99`). Um pacote cancelado aparece com 820.00, embora o total some 410.00.
  - O formato `%.2f` depende do locale (`ReportService.java:113`).
- **Segunda via.** Decide por `amountPaid > 0` e não pelo status (`ReservationController.java:128`), então emite segunda via para reserva paga e cancelada.
- **Dinheiro no JSON.** Sai como `double`, e por isso um valor como 520,00 vai como `520.0` (`ReservationDto.java:20-22`).

Conclusão: em parte

## Dúvidas

1. A regra geral (capacidade, hóspedes, datas) foi repetida dentro de cada estratégia. Isso não tira nenhuma regra de plano do plano, mas mistura regra geral com regra de plano. Não pesei como malfeito, mas é a origem do 400 no lugar do 422.
2. Não consigo decidir pela leitura se o site manda `amountPaid` na confirmação. O texto não prevê esse campo. Se o site não manda, a confirmação falha sempre e o comportamento estaria mais perto de "não segue".
