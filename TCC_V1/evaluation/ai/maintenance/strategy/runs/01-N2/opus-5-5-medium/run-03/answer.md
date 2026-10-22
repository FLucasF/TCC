# Cenário 01

## Visão geral

O agente aplicou o Strategy direito. Criou a interface `RatePlanPolicy`, com três métodos: validação, preço e devolução. Cada um dos cinco planos ganhou uma implementação própria, inclusive o `STANDARD` e o `NON_REFUNDABLE`, que já existiam. O `enum RatePlan` guarda a estratégia de cada constante no construtor. Os dois `switch` de comportamento da base sumiram: o cálculo do preço, a validação e o cancelamento pedem ao plano, sem nomear nenhum. Não achei problema sério. Nada indica estrutura sobrando. Pela leitura do código, as regras e os exemplos do pedido conferem.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin. O pacote `hotel.web` tem a `HttpApi`, com as rotas, a conversão do corpo JSON (`CreateReservationRequest`) para `NewReservation` e o mapeamento de `ErrorCode` para status HTTP (`final/src/main/java/hotel/web/HttpApi.java:101-102`, com `INVALID_REQUEST -> 400`). O pacote `hotel.reservations` tem o `ReservationService` (criar, confirmar, cancelar, check-in e check-out), o `PriceCalculator`, a entidade `Reservation` e o `ReservationStore`, que guarda os dados em memória. Os outros pacotes são `hotel.rooms` (quartos e tipos), `hotel.messages` (e-mails guardados), `hotel.documents` (comprovante) e `hotel.reports` (CSV). Uma criação de reserva segue este caminho: a rota `POST /reservations` monta o `NewReservation`, agora com `companyCode` (`HttpApi.java:128-130`). O `ReservationService.create` valida as datas e os hóspedes e depois chama a validação do plano (`ReservationService.java:34`). Em seguida busca o quarto, confere a capacidade e a disponibilidade, calcula o preço (`ReservationService.java:50`) e salva. O agente escreveu testes de unidade para o cálculo dos três planos novos, com os exemplos do financeiro (`PriceCalculatorTest.java:87`, `:97`, `:111`). Escreveu também testes de serviço para as recusas e as devoluções (`ReservationServiceTest.java:155`, `:173`, `:187`) e um teste HTTP do convênio sem código, que dá 400 com `INVALID_REQUEST`, e com código válido, que dá 201 (`HttpApiTest.java:96-99`).

**A parte do pattern.**
- **A interface.** `RatePlanPolicy` (`final/src/main/java/hotel/reservations/RatePlanPolicy.java:7-16`) tem um `validate` com implementação padrão vazia, um `price(room, request, calculator)` e um `refund(amountPaid)`.
- **A escolha da estratégia.** Cada constante do `enum` recebe a sua no construtor (`RatePlan.java:6-14`) e a expõe por `policy()` (`RatePlan.java:22-24`).
- **`StandardPlan`.** Preço cheio, com desconto zero (`StandardPlan.java:10`), e devolve tudo (`:15`).
- **`NonRefundablePlan`.** 15% de desconto (`NonRefundablePlan.java:12`) e devolução `0.00` (`:17`).
- **`CorporatePlan`.** Um mapa de empresa para desconto, a recusa sem código ou com código desconhecido (`CorporatePlan.java:17`) e a devolução total (`:29`).
- **`WeekendPackagePlan`.** A regra de sexta a domingo (`WeekendPackagePlan.java:20-21`), o preço fechado por tipo de quarto, que não passa pela temporada (`:28-30`), e a devolução de metade (`:35`).
- **`LongStayPlan`.** O mínimo de 7 noites (`LongStayPlan.java:15`), 10% de desconto pelo cálculo noite a noite, que mantém a temporada (`:22`), e a devolução total (`:27`).

O `PriceCalculator` perdeu o `switch` e a constante do não reembolsável. O `calculate` agora só delega ao plano (`PriceCalculator.java:18-19`). Ficou um auxiliar genérico, `discounted(room, request, taxa)`, que soma as noites com a temporada e aplica a fração recebida (`PriceCalculator.java:23-31`). Esse auxiliar não decide nada pelo plano. O cancelamento virou `reservation.ratePlan().policy().refund(...)` (`ReservationService.java:87`). Em `final/src/main`, não sobrou nenhum `switch` ou `if` sobre o plano. O único `switch` restante é o do código de erro na `HttpApi`.

**O julgamento.** As instruções aceitam como acerto uma interface com uma implementação por plano, escolhida por um campo do `enum`. É exatamente o caso aqui. Os cinco planos estão dentro da estrutura, inclusive os dois antigos. Preço, validação e devolução moram no plano. Quem usa a estrutura (o calculador e o serviço) não pergunta qual é o plano concreto. Um plano novo, como o da melhor idade, seria uma classe nova e uma constante nova, sem mexer no calculador nem no serviço. Ao `discounted` o plano chama por conta própria, passando a sua taxa; ele não volta a decidir pelo plano, então não é o caso de "malfeito" previsto nas instruções.

Quanto ao exagero, a interface tem cinco implementações e existe justamente para resolver a variação. Não há fábrica, builder nem camada que só repasse. O construtor auxiliar de `NewReservation` (`NewReservation.java:15-18`) só mantém compatíveis as chamadas antigas, sem o `companyCode`. Não é estrutura nova.

Quanto ao comportamento, conferi as contas e as regras pela leitura do código, e elas batem com o pedido.

## Item a item

**O pattern pedido.** `RatePlanPolicy` (`final/src/main/java/hotel/reservations/RatePlanPolicy.java:7`) tem cinco implementações, uma por plano: `StandardPlan`, `NonRefundablePlan`, `CorporatePlan`, `WeekendPackagePlan` e `LongStayPlan`. A estratégia de cada constante é escolhida no próprio `enum` (`RatePlan.java:6-14`, `:22`). O preço é delegado em `PriceCalculator.java:19`, a validação em `ReservationService.java:34` e a devolução em `ReservationService.java:87`. Os `switch` da base (`base/src/main/java/hotel/reservations/PriceCalculator.java:26` e `base/src/main/java/hotel/reservations/ReservationService.java:86`) foram removidos. Não sobrou decisão por plano fora das classes de plano. É o Strategy que o texto pedia, cobrindo também os dois planos que já existiam.
Conclusão: acerto

**O exagero colateral.** Não há. A interface tem cinco implementações reais. O auxiliar `PriceCalculator.discounted` (`PriceCalculator.java:23`) é reuso que três planos compartilham, não uma camada que só repassa. Não há fábrica nem builder.
Conclusão: contenção

**O comportamento.** A conferência foi feita só pela leitura do código e das contas, sem subir o serviço nem rodar os testes.
- **Convênio.** O mapa tem TECNOSUL 0.12, AGROVALE 0.18 e FARMAVIDA 0.25 (`CorporatePlan.java:10-13`). Código nulo ou fora da lista dá `INVALID_REQUEST` (`CorporatePlan.java:17-19`), que vira 400 (`HttpApi.java:102`). No exemplo do deluxe (389,90 × 2 = 779,80), 12% dá 93,576, arredondado para 93,58, e o total fica R$ 686,22, como pedido.
- **Pacote.** Exige entrada na sexta e saída dois dias depois (`WeekendPackagePlan.java:20-21`). O preço é 520/820/1.200 por tipo de quarto, sem temporada e com desconto 0,00 (`WeekendPackagePlan.java:13-16`, `:28-30`). A devolução é metade do pago (`:35`). Para o exemplo, dá R$ 820,00 e R$ 410,00; uma reserva pendente devolve 0,00.
- **Estadia longa.** Recusa menos de 7 noites (`LongStayPlan.java:15`). Aplica 10% sobre as noites calculadas com a temporada (`PriceCalculator.java:26-30`). No exemplo, 249,90 × 7 = 1.749,30, o desconto é 174,93 e o total R$ 1.574,37. A devolução é total.
- **Normal e não reembolsável.** Funcionam como na base. O normal tem desconto 0,00 e devolve tudo; o não reembolsável tem 15% com arredondamento half-even e devolve 0,00 (`NonRefundablePlan.java:9`, `:17-18`).
- **`companyCode`.** Chega pelo JSON (`CreateReservationRequest.java:16`, `HttpApi.java:130`) e os outros planos o ignoram.
Conclusão: segue

## Dúvidas

Nenhuma que mude a classe. Um detalhe: a validação do plano roda antes da busca do quarto (`ReservationService.java:34-37`). Por isso, um convênio sem código num quarto que não existe responde 400 em vez de 404. O pedido não fala dessa ordem, e as duas respostas são defensáveis.
