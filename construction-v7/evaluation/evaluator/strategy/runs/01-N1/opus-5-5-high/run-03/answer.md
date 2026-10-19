# Cenário 01

## Visão geral

O agente resolveu os planos de tarifa com um Strategy limpo. Há uma interface `RatePlan` com três operações (validar, calcular o preço, calcular a devolução) e uma classe para cada um dos cinco planos, incluindo o normal e o não reembolsável. Um `EnumMap` é o único ponto que escolhe a classe. A criação e o cancelamento da reserva pedem tudo ao plano e não nomeiam nenhum deles. Fora dessa parte o desenho é contido. Todas as regras e todos os exemplos dos planos conferiram no teste. Os problemas estão na parte comum do enunciado:
- o comprovante e o e-mail escrevem o dinheiro no formato do idioma da máquina (`R$ 1,749.30` em vez de `R$ 1.749,30`);
- corpo que não é JSON, campo que falta e data fora do formato dão 500, ou a reserva é aceita com `null`;
- o relatório financeiro lista reservas que não deixaram dinheiro, com valor 0,00.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está em `com.pousadamarealta`, dividido em pacotes:
- `controller` recebe as requisições do Javalin, traduz o corpo e devolve o JSON;
- `service` tem as regras e os dados: `ReservationService`, `RoomService`, `MessageService` e `ReportService`;
- `model` tem `Reservation`, `Room`, os enums `RoomType`, `RatePlanType` e `ReservationStatus`, e `Message`;
- `dto` tem os records de entrada e de saída;
- `rateplan` tem os planos de tarifa;
- `util` tem dinheiro, datas e CPF.

O `App` monta tudo à mão (`App.java:16-24`), registra as rotas (`App.java:36-49`) e converte as exceções do serviço nos códigos de erro (`App.java:51-89`). Ele escuta na porta de `PORT` ou na 7070 (`App.java:94-104`).

O caminho de uma criação de reserva é este:
1. `ReservationController.createReservation` lê o corpo como `CreateReservationRequest` (`controller/ReservationController.java:20`).
2. Ele chama `ReservationService.createReservation`, que confere o quarto, os hóspedes e as datas (`service/ReservationService.java:31-44`).
3. O serviço pega o plano em `RatePlanFactory` (`:46`), manda o plano validar (`:49`) e confere se o quarto está livre (`:54`).
4. O plano calcula o preço (`:59`), e a reserva é guardada num `HashMap` em memória (`:18`, `:78`).

Os quartos ficam fixos no `RoomService` (`service/RoomService.java:15-19`). A diária e a capacidade ficam no enum `RoomType`. O agente escreveu um único teste, `PricingTest`, com 5 casos de preço e devolução que chamam as classes de plano direto.

**Os planos de tarifa.**
- **A interface.** `RatePlan` (`rateplan/RatePlan.java:7-13`) declara `validate`, `calculatePrice` e `calculateRefund`.
- **O normal.** `StandardRatePlan` (`rateplan/StandardRatePlan.java:10-48`) não valida nada, devolve o que foi pago (`:25-27`) e soma as noites com a temporada em `calculateSubtotal` (`:29-47`), que os outros planos reaproveitam.
- **O não reembolsável.** `NonRefundableRatePlan` dá 15% de desconto (`rateplan/NonRefundableRatePlan.java:11-17`) e devolve zero (`:20-22`).
- **O convênio.** `CorporateRatePlan` guarda o mapa de empresas e percentuais (`rateplan/CorporateRatePlan.java:11-15`), recusa código ausente ou desconhecido (`:18-22`) e aplica o desconto da empresa (`:25-32`). A devolução total vem do normal.
- **O pacote.** `WeekendPackageRatePlan` guarda o preço fechado por tipo de quarto (`rateplan/WeekendPackageRatePlan.java:14-18`), exige duas noites de sexta a domingo (`:21-35`), devolve o preço sem desconto e sem temporada (`:38-41`) e devolve metade no cancelamento (`:44-46`).
- **A estadia longa.** `LongStayRatePlan` exige 7 noites (`rateplan/LongStayRatePlan.java:12-18`) e dá 10% sobre as diárias com a temporada (`:21-27`).
- **A escolha.** `RatePlanFactory` tem um `EnumMap` com as cinco instâncias (`rateplan/RatePlanFactory.java:9-17`).
- **O cancelamento.** Só pede a devolução ao plano (`service/ReservationService.java:132-133`).

Procurei `switch`, `case`, `instanceof` e `RatePlanType.` no código-fonte. As únicas ocorrências estão no próprio mapa de seleção.

**O raciocínio.** Cada plano tem preço, validação e devolução numa classe só dele, e os cinco estão na estrutura. A seleção acontece num único lugar, um mapa. Quem usa o plano (criação e cancelamento) não pergunta qual é o plano concreto. Um plano novo pede uma classe nova e uma linha no mapa. Isso é o que as instruções chamam de acerto.

O que afasta um pouco do ideal é a assinatura da interface: ela carrega o `companyCode` para todos os planos (`rateplan/RatePlan.java:8,10`), um parâmetro que só serve ao convênio. Um plano futuro que precise de outro dado (por exemplo, a data em que a reserva foi feita, para a antecedência de 60 dias) vai obrigar a mudar a assinatura de todos. É uma fragilidade da interface, mas não há decisão por plano fora da estrutura.

No exagero colateral, a divisão controller/service/dto é comum e cada camada faz algo além de repassar. Não há interface com uma implementação só, nem builder. A `RatePlanFactory` faz parte do próprio pattern.

No comportamento, os planos estão corretos, e os erros que encontrei são todos da parte comum.

## Item a item

**O enunciado (pede o pattern).** Há uma interface `RatePlan` com `validate`, `calculatePrice` e `calculateRefund` (`rateplan/RatePlan.java:7-13`). Os cinco planos têm uma implementação cada: `StandardRatePlan`, `NonRefundableRatePlan`, `CorporateRatePlan`, `WeekendPackageRatePlan` e `LongStayRatePlan`. A escolha é feita por um `EnumMap` único (`rateplan/RatePlanFactory.java:9-17`). Cada classe guarda as suas regras:
- as empresas e os percentuais do convênio: `rateplan/CorporateRatePlan.java:11-22`;
- a regra de sexta a domingo e os preços fechados do pacote: `rateplan/WeekendPackageRatePlan.java:14-35`;
- as 7 noites da estadia longa: `rateplan/LongStayRatePlan.java:12-18`;
- a devolução zero do não reembolsável: `rateplan/NonRefundableRatePlan.java:20-22`;
- a devolução de metade do pacote: `rateplan/WeekendPackageRatePlan.java:44-46`.

O serviço só delega a validação (`service/ReservationService.java:46-52`), o preço (`:59`) e a devolução (`:132-133`), sem nenhum `if` ou `switch` sobre o plano. O normal e o não reembolsável também estão na estrutura. Esse é o Strategy que o enunciado precisava.
Conclusão: acerto

**O exagero colateral.** Não há. A divisão em controller, service e dto é comum. Os controllers convertem o corpo e os DTOs, e os services guardam dados e regras. Não há interface com uma implementação só, nem builder, nem fábrica fora da seleção de planos. `PricingResult` (`rateplan/PricingResult.java:5-27`) é só um valor de retorno.
Conclusão: contenção

**O comportamento.** Testei com `curl` contra o jar gerado, `target/reservas-1.0-SNAPSHOT.jar`.

Os planos conferiram todos:
- **Convênio.**
  - Tecnosul, deluxe de 10 a 12/03/2027: 779,80, 93,58 de desconto e 686,22 de total.
  - Agrovale: 140,36 de desconto e 639,44 de total.
  - Farmavida, na suíte: 289,95 de desconto e 869,85 de total.
  - Sem código, e com o código `ACME`: 400 `INVALID_REQUEST`.
- **Pacote.**
  - Deluxe de 12 a 14/03/2027: 820,00.
  - Standard de 9 a 11/07/2027, em plena temporada: 520,00, sem acréscimo, e o `companyCode` enviado foi ignorado.
  - Suíte: 1.200,00.
  - Sábado a segunda, sexta a sábado e quinta a domingo: os três deram 400.
  - Pago e cancelado: devolveu 410,00.
- **Estadia longa.**
  - Standard de 01 a 08/03/2027: 1.749,30, 174,93 de desconto e 1.574,37 de total.
  - De 28/06 a 05/07/2027: 1.949,22, com as noites de julho a 299,88.
  - 6 noites: 400.
- **Devoluções.**
  - Convênio e estadia longa, pagos e cancelados: devolveram tudo.
  - Não reembolsável pago: devolução 0.
  - Normal: devolveu tudo.
  - Reserva ainda pendente: devolução 0.

Na parte comum:
- **Preços.** O normal deluxe em julho deu 935,76, e o não reembolsável 795,40.
- **Recusas e fluxo.**
  - Quarto inexistente: 404.
  - Capacidade excedida: 422.
  - Quarto ocupado: 409.
  - Cancelar duas vezes: 409.
  - Segunda via de reserva pendente: 409.
  - Check-in, check-out e segunda via funcionaram.
  - Ocupação: `total,1/5`, com `text/csv; charset=utf-8`.

Os erros da parte comum foram estes:
- **O dinheiro no comprovante e no e-mail.** O valor saiu como `R$ 1,749.30` e `R$ 779.80`. `MoneyUtils.formatMoney` usa `String.format("R$ %,.2f")` com o idioma padrão da JVM (`util/MoneyUtils.java:12`), que nesta máquina é en_US. Num idioma pt-BR o comprovante sairia certo, mas o `%.2f` do CSV passaria a usar vírgula (`service/ReportService.java:61,70`).
- **O relatório financeiro.** Lista as reservas canceladas com devolução total como linhas de valor 0,00: as reservas 1, 7 e 9. O filtro aceita qualquer reserva com pagamento ou devolução maior que zero (`service/ReservationService.java:165-166`), e não só as que deixaram dinheiro com a pousada. O total ficou certo: 1.610,00 em março e 795,40 em julho.
- **Erros de dados.**
  - Corpo que não é JSON: 500.
  - Plano desconhecido: 500.
  - `ratePlan` ausente: 500, por um `NullPointerException` em `service/ReservationService.java:49`.
  - Data fora do formato: 500.
  - `checkIn` ausente: 500.
  - `guestName` ausente: a reserva foi criada (201) com `null`.
  - O enunciado pede 400 `INVALID_REQUEST` em todos esses casos.

Os testes do agente: `mvn -q verify` passou, com 5 testes, 0 falhas e 0 erros. O serviço foi derrubado no fim.
Conclusão: em parte

## Dúvidas

O `companyCode` faz parte da assinatura de `validate` e `calculatePrice` para todos os planos (`rateplan/RatePlan.java:8,10`), e a reserva guarda o código seja qual for o plano (`service/ReservationService.java:71`). Nenhuma regra de plano ficou fora da estrutura por isso, então a classe não muda. Mas é um parâmetro de um plano só, vazando para a interface comum, e pode exigir mexer em todas as implementações quando chegar um plano que precise de outro dado.
