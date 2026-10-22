# Cenário 01

## Visão geral

O agente trocou os dois `switch` da base por um Strategy completo. Há uma interface `RatePlanStrategy` com três operações: validar, calcular o preço e calcular a devolução. Cada um dos cinco planos tem a sua classe, inclusive o STANDARD e o NON_REFUNDABLE, que já existiam. A escolha passa por um único `EnumMap`. O cálculo do preço, a validação e o cancelamento pedem ao plano o que fazer e não nomeiam nenhum plano. Não sobrou `switch` nem `if` sobre o plano no código de produção. As regras pedidas conferem na leitura, e os quatro exemplos do financeiro batem. Não há problema sério. Os pontos fracos são pequenos: o `companyCode`, que só serve ao convênio, entra na assinatura de todos os planos, e `PriceCalculator` virou uma fachada fina que só repassa.

## Como foi implementado, e por que o julgamento

**O projeto.** O sistema tem estes pacotes:
- `hotel.rooms`: os quartos, com tipo, capacidade e diária. São fixos em `Rooms`, com diárias de 249,90, 389,90 e 579,90.
- `hotel.reservations`: o domínio. Tem `ReservationService`, `Reservation`, `ReservationStore` (memória), `PriceCalculator`, `Price`, `RatePlan` e as exceções `HotelException`/`ErrorCode`.
- `hotel.web`: a API em Javalin (`HttpApi`) e os DTOs.
- `hotel.documents`, `hotel.messages` e `hotel.reports`: o recibo, os e-mails guardados em memória e os relatórios em CSV.

`Hotel` monta tudo à mão e sobe o servidor (`final/src/main/java/hotel/Hotel.java:19-28`). Uma criação de reserva segue este caminho:
1. `POST /reservations` chega em `HttpApi.readNewReservation`, que monta o `NewReservation`, agora com o `companyCode` (`final/src/main/java/hotel/web/HttpApi.java:128-135`).
2. `ReservationService.create` faz as validações gerais: datas, hóspedes, quarto, capacidade e disponibilidade.
3. Em seguida chama `priceCalculator.validate` e `priceCalculator.calculate` (`final/src/main/java/hotel/reservations/ReservationService.java:49-51`).
4. A reserva é gravada no `ReservationStore`.

Os testes do agente cobrem os três planos novos em `PriceCalculatorTest`, `ReservationServiceTest` e `HttpApiTest`: preço, recusas e devolução. Os testes antigos foram ajustados só para a nova assinatura, que recebe um `null` como `companyCode`.

**A parte do pattern.**
- A interface é `RatePlanStrategy`, com `validate`, `calculate` e `refundAmount` (`final/src/main/java/hotel/reservations/RatePlanStrategy.java:11-21`).
- Há cinco implementações: `StandardRatePlanStrategy`, `NonRefundableRatePlanStrategy`, `CorporateRatePlanStrategy`, `WeekendPackageRatePlanStrategy` e `LongStayRatePlanStrategy`.
- A seleção acontece num único ponto, o `EnumMap` de `RatePlanStrategies` (`final/src/main/java/hotel/reservations/RatePlanStrategies.java:9-21`). O `enum` `RatePlan` continua sem comportamento e funciona só como chave (`final/src/main/java/hotel/reservations/RatePlan.java:3-14`).
- `PriceCalculator` ficou com o que é comum a todos os planos: a soma das noites com a temporada (`sumNights`, `nightPrice`, `isHighSeason`), a contagem de noites e o arredondamento. `calculate` e `validate` só repassam ao plano (`final/src/main/java/hotel/reservations/PriceCalculator.java:18-24`).
- O cancelamento pede a devolução ao plano (`final/src/main/java/hotel/reservations/ReservationService.java:88`).

Cada regra mora no seu plano:
- **Convênio:** a tabela de descontos e a recusa ficam em `CorporateRatePlanStrategy.java:11-26`.
- **Pacote de fim de semana:** sexta, domingo e duas noites são exigidos em `WeekendPackageRatePlanStrategy.java:19-32`. O preço fechado por tipo de quarto está em `:13-16` e `:35-38`, e a devolução de metade em `:41-43`.
- **Estadia longa:** o mínimo de 7 noites e os 10% ficam em `LongStayRatePlanStrategy.java:10-28`.
- **Normal e não reembolsável:** continuam fazendo o que a base fazia (`StandardRatePlanStrategy.java:16-26`, `NonRefundableRatePlanStrategy.java:10-28`).

**O raciocínio.**
- **O pattern:** o desenho bate com o acerto das instruções. Há uma interface com uma implementação por plano, os cinco planos estão nela, a escolha passa por um mapa e não há `switch` de comportamento. Procurei `switch` e `== RatePlan` em `final/src/main/java`. O único `switch` que restou é o de `ErrorCode` para o status HTTP (`final/src/main/java/hotel/web/HttpApi.java:101`), que não tem relação com os planos. A validação de cada plano está dentro do plano, e não no serviço. O que afasta o desenho do ideal é pouco. O parâmetro `companyCode` está na interface de todos os planos, quando só o convênio o usa. E `Reservation` guarda o código sempre (`final/src/main/java/hotel/reservations/Reservation.java:29`), mas faz isso sem `if` por plano. Nenhum dos dois pontos é decisão de comportamento fora do plano.
- **O exagero:** o mapa de seleção faz parte do pattern. Os métodos `PriceCalculator.calculate`/`validate`, que só repassam, são uma camada fina, mas aproveitam uma classe e um ponto de injeção que já existiam. Não é estrutura nova criada sem necessidade.
- **O comportamento:** conferido por leitura e com as contas refeitas à mão. Tudo confere.

## Item a item

**O pattern pedido.**
- `RatePlanStrategy` (`final/src/main/java/hotel/reservations/RatePlanStrategy.java:11-21`) reúne preço, validação e devolução.
- Os cinco planos têm classe própria, os dois antigos inclusive (`StandardRatePlanStrategy.java:8`, `NonRefundableRatePlanStrategy.java:8`, `CorporateRatePlanStrategy.java:9`, `WeekendPackageRatePlanStrategy.java:11`, `LongStayRatePlanStrategy.java:8`).
- A seleção é um único `EnumMap` (`RatePlanStrategies.java:11-17`).
- Os dois `switch` da base sumiram. O de preço estava em `base/src/main/java/hotel/reservations/PriceCalculator.java:26-29` e foi trocado pela delegação em `final/src/main/java/hotel/reservations/PriceCalculator.java:18-24`. O de devolução estava em `base/src/main/java/hotel/reservations/ReservationService.java:86-89` e foi trocado por `final/src/main/java/hotel/reservations/ReservationService.java:88`.
- O serviço e o cálculo não nomeiam nenhum plano. Um plano novo pede uma classe e uma linha no mapa.

Conclusão: acerto

**O exagero colateral.** Não há estrutura sem necessidade fora do pattern. `RatePlanStrategies` é o ponto de seleção do próprio pattern. Os métodos de `PriceCalculator` que só repassam (`final/src/main/java/hotel/reservations/PriceCalculator.java:18-24`) reaproveitam uma classe que já existia e já era injetada no serviço (`final/src/main/java/hotel/Hotel.java:24`). Não foi criada nenhuma interface com implementação única, nenhuma fábrica e nenhum builder.

Conclusão: contenção

**O comportamento.** A conferência foi só por leitura: não subi o serviço nem rodei testes.
- **Convênio:** 12%, 18% e 25% (`CorporateRatePlanStrategy.java:11-14`). O código ausente, em branco ou fora da lista dá `INVALID_REQUEST` (`:18-25`), que vira 400 (`final/src/main/java/hotel/web/HttpApi.java:102`). Exemplo: um deluxe de 10 a 12/03/2027 dá 2 × 389,90 = 779,80. O desconto é 93,576, arredondado para 93,58, e o total fica 686,22. Confere.
- **Pacote:** exige entrada na sexta, saída no domingo e exatamente 2 noites, o que garante o domingo logo depois (`WeekendPackageRatePlanStrategy.java:20-31`). O preço é 520,00, 820,00 ou 1.200,00 conforme o quarto, sem temporada e sem desconto (`:36-37`). Metade de 820,00 é 410,00 (`:42`). Confere.
- **Estadia longa:** recusa menos de 7 noites (`LongStayRatePlanStrategy.java:15-19`) e dá 10% sobre `sumNights`, que já aplica a temporada (`:24-25`). Exemplo: 7 × 249,90 = 1.749,30, com 174,93 de desconto e 1.574,37 de total. Confere.
- **Devolução:** o convênio e a estadia longa devolvem tudo (`CorporateRatePlanStrategy.java:38-40`, `LongStayRatePlanStrategy.java:31-33`). Uma reserva pendente devolve zero, porque `amountPaid` ainda é zero.
- **Normal e não reembolsável:** iguais à base. O não reembolsável dá 15% de desconto com arredondamento half-even e devolve zero.
- **Campo `companyCode`:** é opcional na API (`final/src/main/java/hotel/web/HttpApi.java:116-126`) e os outros planos o ignoram.

Conclusão: segue

## Dúvidas

Nenhuma que mude a classe. Há duas observações menores:
- O `companyCode` está na assinatura de todos os planos (`final/src/main/java/hotel/reservations/RatePlanStrategy.java:14,17`) e `Reservation` guarda o código para qualquer plano (`final/src/main/java/hotel/reservations/Reservation.java:29`). Isso é dado específico do convênio vazando para a interface comum, mas não é decisão por plano fora do plano.
- O código da empresa só é aceito em maiúsculas exatas (`CorporateRatePlanStrategy.java:22`). Isso combina com o texto do pedido.
