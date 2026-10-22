# Cenário 01

## Visão geral

O agente acrescentou os três planos novos (`CORPORATE`, `WEEKEND_PACKAGE`, `LONG_STAY`) ao `enum` `RatePlan` sem dar comportamento a ele. O que cada plano faz continua sendo decidido por `switch` sobre o plano, agora em três lugares. Os dois `switch` que já existiam na base cresceram: o do preço virou um `switch` de despacho em `PriceCalculator.calculate`, que ainda guarda dentro o `switch` antigo de desconto, e o da devolução em `ReservationService.cancel` passou a ter os cinco planos. Apareceu um terceiro, de validação, em `ReservationService.validateRatePlan`. Não há Strategy. O próximo plano, seja o da melhor idade ou o de 60 dias de antecedência, vai mexer no `enum`, no cálculo, na validação e no cancelamento. O comportamento pedido confere pela leitura do código, e não há estrutura sobrando. O problema sério é só de desenho: a variação ficou espalhada em `switch`.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin pequeno. `hotel.web.HttpApi` recebe as rotas, monta um `NewReservation` a partir do `CreateReservationRequest` (`final/src/main/java/hotel/web/HttpApi.java:128-130`) e chama o `ReservationService`. Esse serviço cuida do ciclo de vida da reserva (criar, confirmar, cancelar, check-in, check-out) e usa o `PriceCalculator` para o preço. Os dados ficam em memória: os quartos fixos em `Rooms` e as reservas em `ReservationStore`. O sistema é montado em `Hotel.java`. Em `hotel.reservations`, o agente mexeu em `RatePlan`, `NewReservation` (ganhou `companyCode`), `PriceCalculator` e `ReservationService`. Em `hotel.web`, mexeu em `CreateReservationRequest` (campo `companyCode`) e `HttpApi`. Os testes novos cobrem os três planos em três níveis: o cálculo (`PriceCalculatorTest`, com Tecnosul, Agrovale, Farmavida, o pacote e a estadia longa), o serviço (recusas e devoluções em `ReservationServiceTest`) e a API (201 e 400 com `INVALID_REQUEST` em `HttpApiTest`).

**A parte que pede o pattern.** O plano continua sendo um `enum` sem comportamento (`final/src/main/java/hotel/reservations/RatePlan.java:3-14`). Cada regra mora num lugar diferente:

- **Preço.** `PriceCalculator.calculate` faz `switch (ratePlan)` para escolher um método privado por plano (`final/src/main/java/hotel/reservations/PriceCalculator.java:34-41`). O normal e o não reembolsável continuam em `calculateStandardPrice`, que ainda tem o `switch` de desconto da base, agora com `default` (`PriceCalculator.java:50-54`). Os três novos são métodos privados da mesma classe (`PriceCalculator.java:58-86`). Os descontos do convênio e os preços do pacote são mapas estáticos na própria calculadora (`PriceCalculator.java:22-32`). O laço das diárias foi copiado três vezes (`PriceCalculator.java:46-49`, `61-64` e `80-83`).
- **Validação.** Quem pode usar cada plano está num `switch` no serviço (`final/src/main/java/hotel/reservations/ReservationService.java:56-83`), com a regra do fim de semana num método auxiliar (`ReservationService.java:85-89`). Esse `switch` ainda consulta `PriceCalculator.CORPORATE_DISCOUNTS` (`ReservationService.java:59`).
- **Devolução.** O `switch` de `cancel` cresceu para os cinco planos (`ReservationService.java:122-126`). Na base era `base/src/main/java/hotel/reservations/ReservationService.java:86`.

**O raciocínio.** As instruções classificam como omissão exatamente este caso: os `switch` que já existiam crescendo para os cinco planos, e a decisão por `switch` sobre o plano no cálculo, no cancelamento e numa validação. Não há interface, classe abstrata nem `enum` com corpo por constante. Também não existe um lugar único por plano com as três regras (preço, validação e devolução) juntas. A regra do pacote, por exemplo, está repartida entre `PriceCalculator.java:70-75`, `ReservationService.java:65-71` e `ReservationService.java:125`. Não há estrutura parcial que justificasse `malfeito`. A classe é `omissão`.

Quanto ao exagero colateral, nada foi criado além de métodos privados e mapas de dados.

Quanto ao comportamento, as contas e as recusas conferem pela leitura. O único efeito lateral é pequeno: a validação do plano roda antes da busca do quarto (`ReservationService.java:34`), então um quarto inexistente com plano inválido dá 400 em vez de 404.

## Item a item

**O pattern pedido.** O `RatePlan` só ganhou três constantes, sem comportamento (`final/src/main/java/hotel/reservations/RatePlan.java:8-13`). O preço é escolhido por `switch (ratePlan)` em `final/src/main/java/hotel/reservations/PriceCalculator.java:35-40`, e o `switch` de desconto da base continua em `PriceCalculator.java:50-54`. A validação é um `switch` novo no serviço (`final/src/main/java/hotel/reservations/ReservationService.java:57-82`), e a devolução é o `switch` da base ampliado (`ReservationService.java:122-126`). As regras de cada plano estão espalhadas por três `switch` de comportamento em duas classes, e os dois planos antigos também ficaram no `switch`. É a omissão que as instruções descrevem, não o Strategy.
Conclusão: omissão

**O exagero colateral.** Não há. O agente só criou métodos privados no `PriceCalculator` (`PriceCalculator.java:58-86`), um auxiliar no serviço (`ReservationService.java:85-89`) e dois mapas de constantes (`PriceCalculator.java:22-32`). Não há interface, fábrica, builder nem camada de repasse.
Conclusão: contenção

**O comportamento.** A conferência foi por leitura do código, sem subir o serviço nem rodar testes.

- **Convênio.** Os descontos de 12%, 18% e 25% estão em `PriceCalculator.java:22-26` e são aplicados sobre as diárias com arredondamento para centavos (`PriceCalculator.java:65-66`). Sem código ou com código fora da lista dá `INVALID_REQUEST` (`ReservationService.java:59-63`), e o `HttpApiTest` novo confirma o 400. O exemplo bate: 779,80 × 0,12 = 93,576, que arredonda para 93,58, e o total fica em R$ 686,22. A devolução é integral (`ReservationService.java:123`).
- **Pacote.** Exige entrada na sexta e exatamente 2 noites (`ReservationService.java:85-89`), o que implica saída no domingo. O preço fechado é R$ 520,00, R$ 820,00 ou R$ 1.200,00 conforme o tipo do quarto, sem temporada e sem desconto (`PriceCalculator.java:28-32` e `70-75`). As chaves do mapa batem com `RoomType` (`STANDARD`, `DELUXE`, `SUITE`). A devolução é metade do pago (`ReservationService.java:125`): 820 × 0,5 dá R$ 410,00.
- **Estadia longa.** Exige 7 noites ou mais (`ReservationService.java:72-79`). Dá 10% sobre as diárias, que são calculadas por `nightPrice` e portanto com a temporada valendo (`PriceCalculator.java:77-86`). O exemplo de R$ 1.749,30, menos R$ 174,93, dá R$ 1.574,37. A devolução é integral.
- **Planos antigos.** O normal e o não reembolsável mantêm o cálculo e a devolução da base (`PriceCalculator.java:43-56` e `ReservationService.java:123-124`).
- **`companyCode`.** Fora do convênio, o campo é ignorado.

Ressalva menor: com plano inválido e quarto inexistente, a resposta passa a ser 400 em vez de 404, porque a validação do plano vem antes (`ReservationService.java:34`).
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. A única ressalva de comportamento é a ordem das validações (400 antes de 404 quando o quarto não existe), e o pedido não trata esse caso.
