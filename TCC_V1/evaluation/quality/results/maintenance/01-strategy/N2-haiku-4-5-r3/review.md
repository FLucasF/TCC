# Revisão: manutenção, cenário 01

## Visão geral

A base era um serviço Javalin em pacotes por assunto (`hotel.web` com as rotas, `hotel.reservations` com o serviço,
o cálculo e o acesso ao banco, `hotel.rooms`, `hotel.messages`, `hotel.documents`, `hotel.reports`). Uma requisição
entra em `HttpApi`, vira um `NewReservation` e vai para o `ReservationService`. Os dados ficam no PostgreSQL, com o
esquema criado por uma migração do Flyway. O agente pôs os três planos novos num Strategy dentro de
`hotel.reservations`. Criou a interface `RatePlanStrategy` (validar, calcular, devolver) com uma classe por plano,
inclusive os dois que já existiam, e cada constante do `enum RatePlan` guarda a sua estratégia
(`final/src/main/java/hotel/reservations/RatePlan.java:4-8`). Os dois `switch` da base saíram de `PriceCalculator` e
de `ReservationService.cancel`. O campo `companyCode` passa da borda até a validação. Os 32 testes passam, e as 72
conferências do comportamento também. Não há defeito sério. O que pesa é de acabamento: a mesma conta de desconto e
a mesma contagem de noites estão copiadas em várias classes, um teste da base (julho como alta temporada) foi
apagado, o `PriceCalculator` só repassa a chamada, e o README não fala dos planos novos.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem
os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo (`measures/dependencies-base.md`). `hotel.reservations`
passou de 13 para 19 classes (de 508 para 720 linhas).
**Nota:** bom

A mudança respeitou as camadas da base. A borda HTTP continua fina: só repassa o novo campo
(`src/main/java/hotel/web/HttpApi.java:128-135`, `src/main/java/hotel/web/CreateReservationRequest.java:16`). As
regras dos planos moram em classes sem Javalin e sem JDBC (`src/main/java/hotel/reservations/*RatePlan.java`). O
acesso ao banco ficou em `ReservationStore`, que mudou numa linha só (`ReservationStore.java:132`). A montagem não
mudou. O agente não criou nenhuma dependência nem ciclo novo; o ciclo `documents` ↔ `reservations` veio da base. A
maior classe continua sendo a `HttpApi` (176 linhas, eram 170).

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (os 2 que já existiam e os 3 novos), cada um com validação, preço e devolução
na sua classe. Sobra 0 `switch` de comportamento sobre o plano. O único `switch` novo escolhe o preço do pacote pelo
tipo de quarto, dentro do próprio plano (`WeekendPackageRatePlan.java:42-47`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É o Strategy que a referência aceita: a interface `RatePlanStrategy`
(`src/main/java/hotel/reservations/RatePlanStrategy.java:6-12`) tem uma implementação por plano, e o plano é escolhido
por um campo do `enum` (`RatePlan.java:4-18`). Quem calcula e quem cancela pergunta ao plano, sem dizer qual ele é.
Isso vale para a validação (`ReservationService.java:48`), para o preço (`PriceCalculator.java:9-11`) e para a
devolução (`ReservationService.java:88`). O teste `amountPaid == 0` em `ReservationService.java:85-90` vale para
todos os planos (uma reserva pendente não devolve nada) e não depende do plano. Os dois planos antigos também foram
para a estrutura (`StandardRatePlan.java`, `NonRefundableRatePlan.java`), e as regras de quem pode usar ficaram
dentro de cada plano. São elas o código da empresa (`CorporateRatePlan.java:15-21`), sexta a domingo
(`WeekendPackageRatePlan.java:11-28`) e 7 noites (`LongStayRatePlan.java:11-23`). Um plano novo é uma classe e uma
constante.

**O exagero.** Não há estrutura grande além do pattern. Há dois pontos pequenos, que não mudam a classe:
`ReservationData` (`ReservationData.java:5-6`) copia os campos de `NewReservation` e carrega `roomNumber` e `guests`,
que nenhum plano lê. E o `PriceCalculator` ficou só repassando a chamada para a estratégia (`PriceCalculator.java:9-11`),
enquanto o serviço chama a validação direto no `enum` (`ReservationService.java:48`). O caminho até o plano ficou
dividido em dois.

**O resto do código.**
- A conta do desconto (`subtotal × taxa`, arredondada, e um `Price` novo) está copiada em três classes:
  `NonRefundableRatePlan.java:13-19`, `CorporateRatePlan.java:24-32` e `LongStayRatePlan.java:26-34`.
- A contagem de noites com um `while` também está copiada, em `LongStayRatePlan.java:12-17` e
  `WeekendPackageRatePlan.java:12-17`. O preço já conta as noites em `StandardRatePlan.java:21-24`.
- Três planos herdam de uma classe concreta, `StandardRatePlan`, para aproveitar o preço por noite
  (`CorporateRatePlan.java:7`, `LongStayRatePlan.java:6`, `NonRefundableRatePlan.java:7`). Com isso, o "não
  reembolsável" herda uma devolução integral e precisa sobrescrevê-la. Uma peça comum de cálculo por noite separaria
  melhor o que é base e o que é plano.
- `CorporateRatePlan.java:40` usa `getOrDefault(..., ZERO)`. Isso esconde um código inválido, caso alguém chame
  `calculate` sem passar por `validate`.
- `WeekendPackageRatePlan.java:33` fixa `2` noites em vez de contá-las.
- Ficaram imports sem uso (`NonRefundableRatePlan.java:5`) e uma linha longa (`ReservationService.java:47`). O agente
  também apagou os comentários da base que explicavam a temporada e os planos.

São defeitos localizados, mas a duplicação é justamente o que o próximo plano ("melhor idade", "60 dias") vai copiar
de novo.

## Testes

**Números:** 32 testes rodaram, 0 falhas, 0 erros, 0 pulados; 32 métodos escritos (a base tinha 26, o agente
acrescentou 7 e apagou 1). Cobertura de linhas: 93,8% (542 de 578; a base também tinha 93,8%). Cobertura de ramos:
86,3% (113 de 131; a base tinha 87,1%). Em `hotel.reservations`: 275 de 293 linhas e 61 de 67 ramos.
**Nota:** com problemas

Os testes novos conferem com valores os exemplos do pedido. São eles: Tecnosul 779,80 / 93,58 / 686,22
(`src/test/java/hotel/reservations/PriceCalculatorTest.java:70-80`), o pacote deluxe por 820,00 (`:83-93`), a
estadia longa 1.749,30 / 174,93 / 1.574,37 (`:96-106`) e a devolução de 410,00 do pacote
(`ReservationServiceTest.java:207-213`). As recusas também têm teste: convênio sem código e com código desconhecido
(`ReservationServiceTest.java:169-180`), pacote de quinta e de quarta (`:183-195`), estadia longa com 6 noites
(`:198-204`). Os testes ficam no nível do serviço, contra o banco. Nenhum passa pela API com o campo `companyCode`.

Pela pergunta da mutação, algumas regras ficaram sem rede:
- trocar 0.18 ou 0.25 (Agrovale, Farmavida) não derruba nenhum teste;
- trocar o preço do pacote standard ou da suíte (520,00 e 1.200,00) também não;
- apagar a regra "exatamente 2 noites" (`WeekendPackageRatePlan.java:19-22`) não derruba nada, porque as duas datas
  recusadas no teste começam fora da sexta e caem na regra da sexta;
- a devolução integral do convênio e da estadia longa não tem teste;
- a estadia longa com noites de temporada e o pacote em alta temporada também não.

O agente também apagou um teste da base: `julyIsHighSeason`, que conferia as bordas de julho (30/06, 01/07, 31/07,
01/08). Ele sumiu porque `isHighSeason` virou privado (`StandardRatePlan.java:43`), e não entrou outro no lugar. O
início de julho ainda aparece no `HttpApiTest` (o total 935,76 em 10–12/07), mas as bordas ficaram sem conferência.
Os outros testes da base foram mantidos e continuam passando.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. A chave estrangeira tem índice. O quarto não pode ter duas reservas na mesma
noite por causa de um `EXCLUDE USING gist`. A migração `V1__create_tables.sql` está igual à da base, e não há
migração nova. As colunas de situação e de plano (`rate_plan`, `status`) são `text` sem `CHECK`, como na base.
**Nota:** bom

O pedido não exigia mudança no esquema, e o agente não mexeu nele nem nas migrações. Os planos novos cabem na coluna
`rate_plan` em texto (`src/main/resources/db/migration/V1__create_tables.sql:28`), lida por
`RatePlan.valueOf` (`ReservationStore.java:131`). O SQL continua com parâmetros, e a única mudança no código do banco
é passar `null` no lugar do `companyCode` ao ler a reserva (`ReservationStore.java:132`). Isso quer dizer que o código
da empresa não é guardado: ele serve para validar e calcular na criação, e depois some. Os valores da reserva (o
desconto) ficam salvos, então nada sai errado, mas a reserva não registra de qual empresa veio (ver Dúvidas). A falta
de `CHECK` em `rate_plan` veio da base.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam. A parte comum: 46 de 46. Os dados depois de reiniciar: 3 de
3. O cenário 01: 21 de 21. Duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Pela API, o sistema faz o que o dono pediu:
- o convênio dá 12%, 18% e 25% (686,22, 639,44, 584,85) e recusa com 400 `INVALID_REQUEST` quando falta o código ou
  o código não existe;
- o pacote custa 520,00, 820,00 e 1.200,00, também em julho, devolve 410,00 e recusa sábado a segunda e sexta a
  segunda;
- a estadia longa dá 1.574,37, aplica a temporada (1.754,30 de 28/06 a 05/07) e recusa 6 noites;
- convênio e estadia longa devolvem tudo;
- o `companyCode` é ignorado nos outros planos.

O normal e o não reembolsável continuam como antes, e os dados sobrevivem ao reinício. Uma coisa fora do roteiro:
a regra do plano só é conferida depois da disponibilidade (`ReservationService.java:42-48`). Assim, um convênio sem
código num quarto já ocupado recebe 409 `ROOM_UNAVAILABLE`, e não 400. O texto não fixa essa ordem.

## Dúvidas

- O código da empresa não fica guardado na reserva (`ReservationStore.java:132`, `Reservation.java` não tem o campo).
  O pedido não diz que precisa guardar, e os valores ficam certos. Se o financeiro precisar saber de qual empresa veio
  cada reserva (para cobrar ou conferir o convênio), isso falta, e para corrigir seria preciso uma migração nova.
- O `PriceCalculator`, que agora só repassa a chamada, e o `ReservationData`, com campos que nenhum plano usa, podem
  ser vistos como estrutura sem necessidade. Contei como `contenção` porque são pequenos, um deles é resto da base, e
  nenhum é estrutura nova em volta do pattern.
- O README (`README.md:28-35` e o exemplo do corpo de `POST /reservations`) não foi atualizado e ainda descreve só os
  dois planos antigos, sem o `companyCode`. Não pesou em nenhuma coluna.
