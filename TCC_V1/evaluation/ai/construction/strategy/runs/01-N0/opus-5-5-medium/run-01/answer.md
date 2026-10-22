# Cenário 01

## Visão geral

O agente resolveu os planos de tarifa com um Strategy limpo. Há uma interface `RatePlan` com três responsabilidades: código, cotação (que inclui a validação) e devolução. Cada um dos cinco planos tem a sua implementação, inclusive os dois que já existiam, `StandardPlan` e `NonRefundablePlan`. A escolha passa por um único mapa, o `RatePlans`. O serviço e a reserva só pedem ao plano o preço e a devolução, sem nomear nenhum plano. Não achei `switch` nem `if` sobre o plano fora das implementações. As regras e os exemplos do enunciado conferem na leitura do código. Não há problema sério. O único ponto discutível é a ordem das checagens: a regra do plano é avaliada antes da capacidade e da disponibilidade do quarto, o que é uma escolha razoável.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está em quatro pacotes:
- `domain`: `Room`, `RoomType`, `Reservation`, `ReservationStatus`, `Money` (arredondamento `HALF_EVEN` e formatação), `Quote` (noites, subtotal, desconto e total) e `Season` (alta temporada).
- `plan`: a interface `RatePlan`, as cinco implementações, o registro `RatePlans`, o auxiliar `NightlyPricing`, o objeto de parâmetros `BookingRequest` e a `PlanRuleException`.
- `service`: `ReservationService`, com as regras e o armazenamento em memória em `LinkedHashMap`; `Receipt`, que monta o texto do comprovante e do e-mail; `Message` e `ApiException`.
- `web`: `Api`, com as rotas Javalin, o parse do JSON, as visões e o tratamento de erro.

`Main` lê `PORT` e usa 7070 se ela não vier (`Main.java:10-11`). O caminho de uma criação de reserva é este:
1. `POST /reservations` chega em `Api.parseNew`, que resolve o plano por `RatePlans.find(planCode)`. Plano desconhecido vira 400 (`web/Api.java:95-96`).
2. `ReservationService.create` valida as datas e o número de hóspedes, procura o quarto e chama `n.ratePlan().quote(new BookingRequest(...))` (`service/ReservationService.java:64`). Uma `PlanRuleException` vira 400 `INVALID_REQUEST` (`service/ReservationService.java:65-66`).
3. Depois vêm as checagens de capacidade (422) e de conflito de datas (409), e a `Reservation` guarda o `RatePlan` e a `Quote`.

O cancelamento chama `ratePlan.refund(amountPaid)` (`domain/Reservation.java:58`).

Os testes do agente ficam num único `ApiTest`, de ponta a ponta via HTTP, com quatro casos:
- a alta temporada no plano normal;
- as contas do financeiro (convênio, pacote com cancelamento e estadia longa);
- as recusas por regra de plano;
- o ciclo de vida completo, com e-mail, relatórios e erros 409, 422 e 400.

**A parte do pattern.** A interface `RatePlan` declara `code()`, `quote(BookingRequest)` e `refund(BigDecimal)` (`plan/RatePlan.java:11-19`). Cada plano mora num lugar só dele:
- `StandardPlan`: desconto 0 e devolve tudo (`plan/StandardPlan.java:10-12`).
- `NonRefundablePlan`: 15% de desconto e devolução zero (`plan/NonRefundablePlan.java:10-12`).
- `CorporatePlan`: mapa de empresas com 12, 18 e 25%, recusa sem código ou com código inválido, devolve tudo (`plan/CorporatePlan.java:10-25`).
- `WeekendPackagePlan`: exige entrada na sexta e 2 noites, preço fechado por tipo de quarto, devolve metade (`plan/WeekendPackagePlan.java:12-26`).
- `LongStayPlan`: exige 7 noites ou mais, 10% de desconto, devolve tudo (`plan/LongStayPlan.java:13-20`).

O cálculo por diária com temporada é compartilhado em `NightlyPricing` (`plan/NightlyPricing.java:15-30`). Esse auxiliar não decide nada pelo plano: recebe o percentual de quem o chama. A seleção é um único mapa montado em `RatePlans` (`plan/RatePlans.java:12-17`). O campo `ratePlan` da resposta sai de `r.ratePlan().code()` (`web/Api.java:127`).

**O julgamento.**
- O que aproxima do pattern: os cinco planos, os antigos inclusive, estão na estrutura. Preço, validação e devolução ficam dentro de cada plano. Quem usa o plano (`ReservationService.create` e `Reservation.cancel`) só chama métodos da interface. Um plano novo significa uma classe nova e uma linha no registro.
- O que afasta: não achei nada. Não há `switch`, `instanceof` nem comparação de plano fora das implementações.
- O que fica fora do plano: o `companyCode` é extraído para todos os planos em `web/Api.java:101-102` e só o convênio o usa. Isso não é regra de plano, é transporte de dado.
- Exagero colateral: `BookingRequest`, `NightlyPricing`, `PlanRuleException` e `RatePlans` têm uso real. Não vi interface de implementação única nem camada que só repassa.
- Comportamento: as contas conferem na leitura (detalhe no item abaixo).

## Item a item

**O pattern pedido.** O agente criou a interface `RatePlan` (`plan/RatePlan.java:11-19`) com uma implementação para cada um dos cinco planos (`plan/StandardPlan.java:7`, `plan/NonRefundablePlan.java:7`, `plan/CorporatePlan.java:9`, `plan/WeekendPackagePlan.java:11`, `plan/LongStayPlan.java:8`), escolhida por um mapa (`plan/RatePlans.java:10-23`).
- **Validação:** cada regra de quem pode usar está no plano: o código da empresa em `plan/CorporatePlan.java:18-21`, sexta e 2 noites em `plan/WeekendPackagePlan.java:20-22` e o mínimo de 7 noites em `plan/LongStayPlan.java:14-16`.
- **Preço:** é pedido ao plano em `service/ReservationService.java:64`.
- **Devolução:** é pedida ao plano em `domain/Reservation.java:58`.
- **Planos antigos:** o normal e o não reembolsável também são estratégias. Não ficou nenhum `switch` antigo.

É o Strategy que o texto pedia.
Conclusão: acerto

**O exagero colateral.** Não há. `NightlyPricing` (`plan/NightlyPricing.java:10`) é reuso real entre quatro planos. `BookingRequest` (`plan/BookingRequest.java:7`) agrupa os dados que o plano precisa. `PlanRuleException` (`plan/PlanRuleException.java:4`) separa a recusa do plano do erro HTTP. `RatePlans` é o ponto de seleção do próprio pattern. Não há fábricas, builders, repositórios abstratos nem interfaces com implementação única fora do pattern.
Conclusão: contenção

**O comportamento.** A conferência foi feita só por leitura do código: não subi o serviço, não rodei o Maven e não chamei a API.

*Os exemplos do texto:*
- **Alta temporada no plano normal (STANDARD, 10/07 a 12/07/2027):** cada noite sai 249,90 × 1,20 = 299,88 (`plan/NightlyPricing.java:16`), e as duas somam 599,76.
- **Convênio Tecnosul, deluxe de 10/03 a 12/03/2027:** subtotal de 779,80. O desconto de 12% dá 93,576, que arredonda para 93,58 (`domain/Money.java:19-20`), e o total fica 686,22 (`domain/Quote.java:8`). Confere.
- **Convênio sem código ou com código fora da lista:** dá `PlanRuleException`, que vira 400 `INVALID_REQUEST` (`plan/CorporatePlan.java:18-20`, `service/ReservationService.java:65-66`). Agrovale com 18% e Farmavida com 25% estão no mapa (`plan/CorporatePlan.java:10-13`).
- **Pacote no deluxe:** sai 820,00 com desconto zero e sem temporada (`plan/WeekendPackagePlan.java:23`). Pago e cancelado, devolve 820,00 × 50% = 410,00 (`plan/WeekendPackagePlan.java:26`). Confere. Os preços de 520,00 e 1.200,00 estão no mapa. Fora de sexta a domingo, ou com outra quantidade de noites, a reserva é recusada com 400.
- **Estadia longa, standard de 01/03 a 08/03/2027:** 7 × 249,90 = 1.749,30, com desconto de 174,93 e total de 1.574,37. Confere. Com menos de 7 noites dá 400, e a temporada continua valendo porque o cálculo usa `NightlyPricing`.

*O normal e o não reembolsável:*
- O não reembolsável dá 15% de desconto e devolve zero.
- O normal, o convênio e a estadia longa devolvem tudo.
- Uma reserva pendente tem `amountPaid` zero, então a devolução é zero em qualquer plano (`domain/Reservation.java:22`, `domain/Reservation.java:58`).

*O resto do sistema:*
- O ciclo de estados está em `service/ReservationService.java:92-117` e a segunda via em `service/ReservationService.java:120-126`.
- O comprovante tem a linha de desconto condicional e a forma "1 diária" (`service/Receipt.java:22-27`).
- Os relatórios usam pago menos devolvido (`domain/Reservation.java:63-64`, `service/ReservationService.java:145-161`).
- Os códigos de erro estão no lugar, e o `companyCode` é ignorado nos outros planos.

Tudo o que conferi segue o pedido.
Conclusão: segue

## Dúvidas

- A regra do plano é checada antes da capacidade e da disponibilidade (`service/ReservationService.java:62-77`). Um pedido que fere as duas regras recebe 400, e não 422 ou 409. O enunciado não define a precedência, então não descontei nada por isso.
- O código da empresa só é aceito exatamente em maiúsculas (`plan/CorporatePlan.java:18`), o que é compatível com "o código é o nome dela em maiúsculas".
- O `companyCode` é lido para todos os planos em `web/Api.java:101-102`, mas só o convênio o usa. É transporte de dado, não regra de plano, e não muda a classe.
