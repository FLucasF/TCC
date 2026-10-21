# Revisão: manutenção, cenário 01

## Visão geral

A base já era um serviço Javalin em camadas: `hotel.web` (rotas e JSON), `hotel.reservations` (regras e o acesso ao
banco em `ReservationStore`), `hotel.rooms`, `hotel.messages`, `hotel.reports`, `hotel.documents`, tudo montado em
`hotel.Hotel`, com os dados num PostgreSQL criado por migrações do Flyway. O caminho de uma reserva é
`HttpApi` → `ReservationService.create` → `PriceCalculator.calculate` → `ReservationStore.add`. O agente trocou os dois
`switch` sobre o plano (desconto e devolução) por um Strategy: a interface `RatePlanPolicy`, uma classe por plano (os
dois antigos e os três novos), escolhidas num `EnumMap` em `RatePlans`. Acrescentou o campo `companyCode` e uma
migração nova (`V2`). Os 72 itens do roteiro de comportamento passaram e os 30 testes também. O defeito mais sério não
tem a ver com o pedido: o agente reescreveu a rota de check-in com SQL direto no handler HTTP
(`src/main/java/hotel/web/HttpApi.java:80-92`). Com isso a rota passa por fora do `ReservationService.checkIn`, que
continua no código mas agora só os testes usam, e o check-in deixou de ser serializado com o cancelamento. O README
também não fala dos planos novos nem do `companyCode`.

## Arquitetura

**Números:** 7 pacotes e 17 dependências, como na base. Há um ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base. A diferença está em `hotel.web`, que agora usa JDBC (na base, não usava). `HttpApi` passou de 170 para 187 linhas, e `hotel.reservations` de 13 classes (508 linhas) para 20 (731).
**Nota:** com problemas

A parte que o pedido exigia está no lugar certo. As políticas de plano ficam em `hotel.reservations`, não conhecem o
Javalin nem o JDBC (`src/main/java/hotel/reservations/RatePlanPolicy.java:7-15`) e são montadas uma vez só, em
`src/main/java/hotel/Hotel.java:25-27`. O ciclo entre `documents` e `reservations` veio da base.

A mistura de camadas é nova e não foi pedida. O agente passou o `DataSource` para a borda HTTP
(`src/main/java/hotel/Hotel.java:28-29`, `src/main/java/hotel/web/HttpApi.java:30,38,44`), e a rota de check-in
monta e executa um `UPDATE reservations SET status = 'CHECKED_IN' ...` dentro do handler
(`src/main/java/hotel/web/HttpApi.java:82-90`). A regra de transição da reserva (só uma `CONFIRMED` pode fazer
check-in) agora existe em dois lugares: no SQL do handler e em `ReservationService.checkIn`
(`src/main/java/hotel/reservations/ReservationService.java:90-98`). Esse método do serviço não é mais chamado pela
API. Na base, a rota só delegava (`base/src/main/java/hotel/web/HttpApi.java:74`). A mistura é localizada, mas é
exatamente o tipo de SQL espalhado que a base evitava.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (STANDARD, NON_REFUNDABLE, CORPORATE, WEEKEND_PACKAGE, LONG_STAY). Fora dela, nenhum `switch` ou `if` de comportamento sobre o plano. Há um único ponto de seleção (`RatePlans`, um `EnumMap`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Cada plano tem o seu comportamento numa classe própria, que reúne a validação, o preço e a devolução
(`RatePlanPolicy.java:10-14`). Os dois planos antigos também foram migrados: `StandardPlan.java:13-20` e
`NonRefundablePlan.java:15-22`. Os dois `switch` da base saíram de `PriceCalculator.calculate` e de
`ReservationService.cancel`. O cálculo pede ao plano sem dizer qual é (`PriceCalculator.java:25-29`), e o
cancelamento também (`ReservationService.java:84`). As regras de quem pode usar ficaram dentro de cada plano, e não
no serviço: o código da empresa em `CorporatePlan.java:16-21`, sexta a domingo em `WeekendPackagePlan.java:19-26` e
as 7 noites em `LongStayPlan.java:14-18`. A seleção acontece num lugar só (`RatePlans.java:12-16,19-20`). Um plano
novo pede uma classe, uma constante e uma linha no mapa. `PercentOff` (`PercentOff.java:10-13`) evita repetir a
conta do desconto em quatro planos. `NightlyCharges` separa as diárias com a temporada, que são comuns a todos, do
que cada plano decide. As duas peças servem ao pattern e não são exagero. Não há fábrica, builder nem interface com
uma implementação só fora do pattern.

**O resto do código.** O handler de check-in (`HttpApi.java:80-92`) é o problema. Ele repete a regra de situação
que o serviço já tinha e põe a situação como texto solto no SQL (`'CHECKED_IN'`, `'CONFIRMED'`), em vez de usar o
`enum` `ReservationStatus`. Ele busca a reserva duas vezes e deixa `ReservationService.checkIn` como código que a
API não usa mais. Há também uma regra de plano pequena fora do plano: o `companyCode` é gravado em qualquer plano
(`ReservationStore.java:50`), embora o comentário da migração diga que ele fica nulo nos outros. Isso não pesa na
classe, mas é inconsistente. O resto está limpo: `BigDecimal` com o arredondamento da base, tipos próprios
(`RoomType` como chave do preço fechado em `WeekendPackagePlan.java:12-15`) e nomes claros.

## Testes

**Números:** 30 testes rodaram, nenhum falhou e nenhum foi pulado (a base tinha 26). São 30 métodos escritos, 4 deles novos (`NewRatePlansTest`). A cobertura de linhas é 93,8% (528 de 563), igual à da base. A de ramos é 86,5% (109 de 126), contra 87,1% na base. Em `hotel.reservations`: 258 de 273 linhas e 56 de 60 ramos.
**Nota:** com problemas

Os testes da base foram mantidos e só adaptados à nova assinatura (`PriceCalculatorTest.java:19-22`,
`ReservationServiceTest.java:24-27,34-37`). Nenhum foi apagado ou enfraquecido. Os testes novos conferem os três
exemplos do financeiro com valores: Tecnosul 93,58 e 686,22 (`NewRatePlansTest.java:38-39`), pacote 820,00 com
devolução de 410,00 (`:51,54`) e estadia longa 174,93 e 1.574,37 (`:62-63`). Eles também testam o limite das 7
noites, com 6 recusadas (`:64`).

Ficam sem rede pela pergunta da mutação:

- os descontos da Agrovale (18%) e da Farmavida (25%): trocar 0.18 por 0.81 não derruba nenhum teste;
- a devolução integral do convênio e da estadia longa: trocar por zero passaria;
- o pacote em alta temporada continuar com o preço fechado, o preço do standard (520) e o da suíte (1.200);
- o pacote de sexta a segunda: só o sábado é testado (`:55`);
- a estadia longa com noites de temporada.

As recusas são conferidas só como `HotelException` (`:44-45,55,64`), e não pelo código `INVALID_REQUEST`. Nada testa
pela API o campo `companyCode` nem a resposta 400. A rota de check-in reescrita só passa pelo status 200 do caminho
feliz (`HttpApiTest.java:84`). A recusa dela (409) e a corrida com o cancelamento não têm teste. Os testes de
check-in em `ReservationServiceTest.java:129-141` exercitam o método do serviço, que a API não usa mais.

## Banco

**Números:** 3 tabelas e 2 migrações do Flyway (V1 igual à da base, V2 nova). Nenhuma tabela sem chave primária. Todo o dinheiro está em `numeric(10,2)`, nenhuma chave estrangeira está sem índice e nenhuma data está em texto. `rate_plan` e `status` estão em `text` sem CHECK (como na base). Há 6 arquivos com JDBC (um deles, `HttpApi`, é novo) e nenhuma transação explícita.
**Nota:** com problemas

A mudança de esquema foi feita do jeito certo. A migração nova só acrescenta a coluna
(`src/main/resources/db/migration/V2__reservation_company_code.sql:2`), e a V1 não foi tocada. O `INSERT` e a leitura
usam parâmetros (`ReservationStore.java:31-50,124-133`), e a regra contra reserva dupla continua no `EXCLUDE` da base.
A falta de CHECK sobre `rate_plan` veio da base.

Os defeitos são do agente. O `UPDATE` do check-in fica fora da classe de acesso a dados
(`HttpApi.java:82-90`). Ele roda fora do `synchronized` que serializava as transições em `ReservationService`
(`ReservationService.java:78,90`). Por isso um cancelamento que leu a reserva como `CONFIRMED` pode gravar
`CANCELLED` com devolução por cima de um check-in que acabou de acontecer (`ReservationStore.java:65-73` grava a
situação inteira). Na base, os dois passavam pela mesma trava. A janela é pequena, mas é um risco novo de dado
errado. Além disso, a coluna `company_code` recebe o código mesmo nos planos que não são convênio
(`ReservationStore.java:50`), ao contrário do que o comentário da `V2` diz.

## Comportamento

**Números:** 72 de 72 conferências passaram: a parte comum 46 de 46, depois de reiniciar 3 de 3, o cenário 21 de 21 e as reservas simultâneas 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API:

- os três descontos do convênio (686,22, 639,44 e 584,85), e a recusa sem código ou com código desconhecido
  (400 `INVALID_REQUEST`);
- o pacote com o preço fechado nos três quartos, inclusive em julho, a devolução de metade (410,00) e a recusa de
  sábado a segunda e de sexta a segunda;
- a estadia longa com a temporada (1.754,30), a recusa com 6 noites e a devolução integral;
- o `companyCode` ignorado no plano normal;
- o não reembolsável continua com 15%.

O que já funcionava continua funcionando, e os dados sobrevivem a um reinício. O defeito do check-in não aparece
nesse roteiro, porque só se manifesta com check-in e cancelamento simultâneos da mesma reserva.

## Dúvidas

- Não está claro por que o agente reescreveu o check-in: o pedido não fala dele. O `UPDATE ... WHERE status =
  'CONFIRMED'` sozinho é atômico. O risco vem só de ele conviver com o `cancel` e o `checkOut`, que continuam
  fazendo ler-mudar-gravar sob uma trava que o check-in não usa. Esse defeito pesou em três colunas (arquitetura,
  design e banco), porque é ao mesmo tempo SQL na borda HTTP, regra duplicada e risco de dado.
- Gravar o `companyCode` nos planos que não são convênio (`ReservationStore.java:50`) é uma regra de plano pequena
  fora do plano. Ela não mudou a classe do pattern.
