# Revisão: manutenção, cenário 01

## Visão geral

O agente partiu da base (Javalin + JDBC + Flyway, pacotes `hotel`, `hotel.web`, `hotel.reservations`, `hotel.rooms`,
`hotel.messages`, `hotel.documents` e `hotel.reports`). Uma reserva entra por `HttpApi` (`hotel/web/HttpApi.java:64`),
vira um `NewReservation` (`HttpApi.java:128-135`, agora com `companyCode` opcional) e vai para
`ReservationService.create`, que valida, pede o preço e grava em `ReservationStore` (SQL com parâmetros, tabela
`reservations` criada pela migração `V1__create_tables.sql`, que o agente não mexeu). A mudança foi transformar o
plano de tarifa num Strategy: uma interface `RatePlanStrategy` (validar, calcular o preço, calcular a devolução), uma
classe por plano (os cinco) e o `enum RatePlan` levando a sua estratégia no construtor (`RatePlan.java:5-13`); o
serviço pede ao plano sem nomeá-lo (`ReservationService.java:47-49` e `85-86`). Os valores pedidos estão certos
(roteiro 72 de 72, 41 testes passando). O que há de mais sério: **cancelar um pacote de fim de semana ainda pendente
(nunca pago) grava uma devolução de R$ 410,00 com `amount_paid` 0,00** (`WeekendPackagePlanStrategy.java:42-44`),
o que quebra a regra da base ("reserva pendente não devolve nada") e o texto ("recebe de volta metade do que pagou").
Também ficou um `switch` sobre o plano em `PriceCalculator.java:26-30`, ao qual os planos normal e não reembolsável
ainda delegam o preço.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), igual à base
(`measures/dependencies-base.md`: 7 pacotes, 17 dependências, o mesmo ciclo). `hotel.reservations` passou de 13
classes/508 linhas para 18 classes/741 linhas; `hotel.web` de 236 para 243 linhas.
**Nota:** bom

O agente não mudou a forma do sistema: as cinco estratégias e a interface moram em `hotel.reservations`, sem Javalin
nem JDBC (só `Room`, `BigDecimal`, `LocalDate`); a borda HTTP só ganhou a leitura do campo novo
(`hotel/web/CreateReservationRequest.java:16`, `HttpApi.java:135`); nenhuma dependência nova entre pacotes, e o
ciclo `documents` ↔ `reservations` veio da base. As classes maiores continuam as da base (`HttpApi` 176 linhas,
`ReservationStore` 144, `ReservationService` 139), sem crescer de verdade. Um ponto pequeno de montagem: as
estratégias são criadas dentro do `enum` com `new PriceCalculator()` (`RatePlan.java:5-13`), à margem da montagem em
`Hotel.java:25`, que continua injetando um `PriceCalculator` no serviço que agora ninguém usa
(`ReservationService.java:16,23`). Como o `PriceCalculator` não tem estado, isso não muda a nota; conta no design.

## Design de baixo nível

**Números:** 5 de 5 planos têm uma classe de estratégia com validação, preço e devolução; o serviço não nomeia
nenhum plano. Mas 2 dos 5 (STANDARD e NON_REFUNDABLE) calculam o preço chamando `calculateForRatePlan`, que ainda
decide pelo plano num `switch` de 5 casos (`PriceCalculator.java:26-30`); 1 regra comum (pendente não devolve nada)
copiada em 3 estratégias e esquecida em 1.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

O pattern: o que aproxima do acerto é a interface `RatePlanStrategy` (`RatePlanStrategy.java:8-14`) com uma
implementação por plano, escolhida por um campo do `enum` (`RatePlan.java:15-23`), e o serviço usando só a interface
na criação (`ReservationService.java:47-49`) e no cancelamento (`ReservationService.java:85-86`). As regras novas
estão cada uma no seu plano: o código da empresa em `CorporatePlanStrategy.java:27-33`, sexta a domingo em
`WeekendPackagePlanStrategy.java:23-33`, 7 noites em `LongStayPlanStrategy.java:19-23`. O que afasta, e decide a
classe: o `switch` de comportamento da base não sumiu, cresceu. `PriceCalculator.calculateForRatePlan` ainda decide o
desconto por `switch (ratePlan)` (`PriceCalculator.java:26-30`), agora com um caso morto para os três planos novos
(`PriceCalculator.java:29`), e as estratégias dos dois planos que já existiam só repassam para ele com o próprio nome
(`StandardPlanStrategy.java:22`, `NonRefundablePlanStrategy.java:22`). O desconto de 15% do não reembolsável continua
fora da classe dele (`PriceCalculator.java:17,28`). É exatamente o caso que a referência chama de malfeito: "uma
implementação delega o preço para um cálculo que ainda decide pelo plano num `switch`". O próximo plano obriga a mexer
nesse `switch` (por ser exaustivo sobre o `enum`), mesmo sem precisar dele.

Exagero: nenhum. A interface tem cinco implementações que de fato variam; não há fábrica, registro nem camada que só
repassa.

O resto do código:
- duplicação: `calculateWithDiscount` (`PriceCalculator.java:34-43`) copia o laço das noites de
  `calculateForRatePlan` (`PriceCalculator.java:20-25`) em vez de os dois usarem um subtotal comum (que tornaria o
  `switch` desnecessário); a devolução "pendente não devolve nada, senão o total" está copiada igual em
  `StandardPlanStrategy.java:27`, `CorporatePlanStrategy.java:43` e `LongStayPlanStrategy.java:32`;
- essa regra é comum a todos os planos e na base morava no serviço (a devolução era `reservation.amountPaid()`, que
  é zero antes do pagamento). Ao trocar a assinatura para `calculateRefund(Price, ReservationStatus)`
  (`RatePlanStrategy.java:13`), o agente passou para cada plano a obrigação de lembrar disso, e o pacote esqueceu:
  `WeekendPackagePlanStrategy.java:42-44` devolve metade do total mesmo de uma reserva pendente (confirmado pela API,
  ver Comportamento);
- acoplamento temporal: `CorporatePlanStrategy.calculatePrice` faz `companyCode.get()` (`CorporatePlanStrategy.java:37`)
  confiando que `validate` foi chamado antes; nada na interface garante a ordem;
- a interface recebe `Optional<String> companyCode` em todos os métodos de todos os planos
  (`RatePlanStrategy.java:9-11`), um dado de um plano só vazando para os cinco; `Optional` como campo de record
  (`NewReservation.java:13`);
- código morto: o campo `priceCalculator` do serviço não é mais usado (`ReservationService.java:16,23`);
- detalhes: nomes qualificados no meio do código (`ReservationService.java:46`, `HttpApi.java:135`); a mensagem de
  erro do convênio lista as empresas na ordem de um `HashMap` (`CorporatePlanStrategy.java:12,31`).

Os tipos estão certos (`BigDecimal` com `cents` meio-para-o-par, `LocalDate`, `DayOfWeek`, `enum`), sem estado
mutável novo além dos mapas estáticos preenchidos uma vez.

## Testes

**Números:** 41 testes rodaram, 0 falhas, 0 erros, 0 pulados; 41 métodos escritos (a base tinha 26, todos mantidos).
Cobertura de linhas 94,3% (544 de 577; base 93,8%) e de ramos 85,2% (115 de 135; base 87,1%); `hotel.reservations`
277 de 292 linhas e 63 de 71 ramos.
**Nota:** com problemas

O agente acrescentou 10 testes de serviço e 5 pela API, e só ajustou os da base à nova assinatura (nenhum apagado nem
enfraquecido). Os exemplos do texto estão conferidos com valores: Tecnosul 779,80/93,58/686,22 e Agrovale
140,36/639,44 (`ReservationServiceTest.java:172-182`), os três preços do pacote (`ReservationServiceTest.java:201-210`),
a devolução de 410,00 (`ReservationServiceTest.java:212-218`), a estadia longa 1.749,30/174,93/1.574,37
(`ReservationServiceTest.java:227-234`) e as recusas (sem código e código inválido, quarta a sexta e sábado a
segunda, 6 noites: `ReservationServiceTest.java:164-170,193-199,221-225`; pela API com 400 e `INVALID_REQUEST`,
`HttpApiTest.java:110-123`). A pergunta da mutação: trocar 0.12, 0.18, 0.10 ou o divisor do pacote quebra testes;
mas trocar 0.25 da Farmavida não quebra nada (nenhum teste usa FARMAVIDA); apagar a checagem de domingo
(`WeekendPackagePlanStrategy.java:30-32`) também não, porque os casos testados já caem nas outras; e a estadia longa
com noites de temporada não é testada. Sobretudo, nenhum teste cancela uma reserva pendente dos planos novos, e é
aí que está o defeito real do pacote (`WeekendPackagePlanStrategy.java:42-44`), que passa sem rede. Menores:
`userExamplesAreCorrect` (`ReservationServiceTest.java:245-262`) repete os testes anteriores; os testes pela API
comparam dinheiro como `double` com tolerância de 0,01 (`HttpApiTest.java:135,158`).

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` (6 de 6 colunas); nenhuma chave
estrangeira sem índice; nenhuma data em texto; a migração `V1__create_tables.sql` igual à da base; `rate_plan`,
`status`, `type` e `channel` em `text` sem CHECK (já na base); EXCLUDE contra reserva dupla no mesmo quarto (da base).
**Nota:** bom

O agente não precisou mudar o esquema e não mudou: os planos novos cabem em `rate_plan text` sem restrição
(`V1__create_tables.sql`), e preço, desconto, total e devolução continuam gravados como antes
(`ReservationStore.java:44-49,64-72`), com SQL parametrizado e try-with-resources. A única mudança no acesso a dados
é a leitura devolver `Optional.empty()` para o código da empresa (`ReservationStore.java:132`): o código não é
guardado, então uma reserva de convênio relida do banco não diz de qual empresa veio o desconto. O texto não pede
isso; fica nas dúvidas. A devolução errada do pacote pendente acaba gravada no banco (`refund` 410,00 com
`amount_paid` 0,00), mas a causa é a regra do plano e pesa nas colunas de design e de comportamento, não aqui.

## Comportamento

**Números:** 72 de 72 conferências passaram: parte comum 46 de 46, dados depois de reiniciar 3 de 3, cenário 01 21
de 21, reservas simultâneas 2 de 2.
**Nota:** com problemas

Pelo roteiro, tudo o que o texto pede funciona: os três descontos do convênio (686,22, 639,44, 584,85), as recusas
com 400 `INVALID_REQUEST`, o pacote com preço fechado nos três quartos e em julho, a devolução de 410,00, a estadia
longa com e sem temporada, `companyCode` ignorado nos outros planos, e o normal e o não reembolsável como antes.
Mas o roteiro só cancela o pacote depois de pago. Chamando a API à mão (criar um WEEKEND_PACKAGE deluxe de
12/03/2027 a 14/03/2027 e chamar `POST /reservations/1/cancellation` sem confirmar), a resposta é `status` CANCELLED,
`amountPaid` 0.00 e `refund` 410.00, e é isso que fica gravado. Na base, e para os outros quatro planos, cancelar uma
reserva pendente devolve 0,00. Para o dono da pousada: um hóspede que reservou o pacote, não pagou e desistiu aparece
no sistema com R$ 410,00 (ou R$ 260,00, ou R$ 600,00) a receber de volta, dinheiro que nunca entrou. O relatório
financeiro não chega a mostrar um valor negativo, porque ignora reservas com valor retido não positivo
(`RevenueReport.java:26`), mas o dado da reserva está errado e a recepção pode acabar fazendo a devolução. O defeito
está fora do roteiro, mas atinge uma regra do texto ("recebe de volta metade do que pagou") e uma regra da base que
devia continuar valendo.

## Dúvidas

- A classe do pattern: a estrutura cobre os cinco planos na validação e na devolução, e o serviço não nomeia nenhum
  plano, o que fica perto do acerto. Dei `malfeito` porque o preço dos dois planos que já existiam ainda é decidido
  pelo `switch` em `PriceCalculator.java:26-30`, que a referência cita como malfeito.
- O código da empresa não é guardado (`ReservationStore.java:132`). O texto não pede para guardar; o financeiro talvez
  queira saber, depois, a qual empresa cada desconto se refere. Não pesou na nota.
- O plano é validado depois da checagem de quarto ocupado (`ReservationService.java:42-48`): um convênio sem código
  num quarto ocupado recebe 409 `ROOM_UNAVAILABLE`, e não 400. O texto não decide a ordem; não pesou.
