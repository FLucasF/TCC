# Revisão: manutenção, cenário 01

## Visão geral

A base é um serviço Javalin em sete pacotes. Uma requisição entra por `hotel.web` (`HttpApi`), que monta um `NewReservation` e chama `hotel.reservations.ReservationService`. O serviço valida, pede o preço a `PriceCalculator` e grava por `ReservationStore`, que faz JDBC num PostgreSQL com o esquema criado por migrações do Flyway. Para criar os três planos novos (CORPORATE, WEEKEND_PACKAGE e LONG_STAY), o agente aumentou os `switch` sobre o `RatePlan` que já existiam e criou mais um. Ficaram quatro pontos de decisão por plano: `PriceCalculator.java:22` (pacote ou não), `PriceCalculator.java:48` (desconto), `ReservationService.java:54` (validação nova, `validateRatePlan`) e `ReservationService.java:125` (devolução). Também criou o campo `companyCode` da API até o banco, com uma migração nova (`V2__add_company_code.sql`). Tudo funciona: os 72 itens do roteiro passaram, os 40 testes passaram e a migração da base ficou intacta. O mais sério é o design. O texto avisa que "o comercial vive inventando plano", e o próximo plano vai exigir mexer em quatro `switch` de duas classes. A lista de empresas conveniadas ficou repetida em dois lugares. O README não foi atualizado: continua dizendo que todos os campos do corpo são obrigatórios e só fala de STANDARD e NON_REFUNDABLE.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base. Há 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base (`measures/dependencies-base.md`). `hotel.reservations` passou de 508 para 595 linhas. `ReservationService` passou de 138 para 182 linhas e hoje é a maior classe.
**Nota:** bom

O agente não mexeu na estrutura. A borda HTTP continua fina: só repassa o campo novo (`src/main/java/hotel/web/HttpApi.java:129-130`, `src/main/java/hotel/web/CreateReservationRequest.java:16`). As regras novas ficaram em `hotel.reservations`, sem Javalin e sem SQL (`ReservationService.java:53-91`, `PriceCalculator.java:21-65`). O SQL novo ficou só no `ReservationStore` (`ReservationStore.java:19`, `:32-33`, `:50`, `:133`), e a montagem não mudou. O ciclo entre `documents` e `reservations` veio da base. O único ponto de atenção é o tamanho do `ReservationService`, que agora também guarda as regras de elegibilidade de cada plano. Esse problema é de design (próxima seção) e não de camadas.

## Design de baixo nível

**Números:** dos 5 planos, nenhum tem comportamento num lugar só dele. São 4 `switch` sobre o plano: `PriceCalculator.java:22` (escolhe o cálculo de pacote), `PriceCalculator.java:48-54` (desconto, 5 casos), `ReservationService.java:54-73` (validação, 3 casos e um `default`) e `ReservationService.java:125-129` (devolução, 5 casos). Há também 1 `switch` sobre o código da empresa (`PriceCalculator.java:59-64`). Nenhuma interface nem classe por plano. `RatePlan` continua um `enum` sem comportamento (`RatePlan.java:3-14`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern pedido (Strategy):** o texto diz que cada plano chega "com o seu jeito de calcular o preço, com as suas regras de quem pode usar e com a sua regra de devolução", e que outros planos estão a caminho. A resposta esperada era tirar o comportamento dos `switch` e dar a cada um dos cinco planos o seu objeto, ou um corpo próprio em cada constante do `enum`. O agente fez o contrário: os dois `switch` da base cresceram para cinco casos (`PriceCalculator.java:48-54`, `ReservationService.java:125-129`) e apareceram mais dois (`PriceCalculator.java:22-25`, `ReservationService.java:54-73`). Pela referência, isso é omissão: o comportamento de cada plano é decidido por `switch` no preço, no cancelamento e na validação. Um plano novo, como o "melhor idade", vai exigir mexer em `RatePlan` e em quatro `switch` de duas classes. O caso `WEEKEND_PACKAGE -> cents(BigDecimal.ZERO); // not reachable but needed for completeness` (`PriceCalculator.java:53`) mostra o custo disso: um ramo morto só para o `switch` compilar.

**Exagero:** nenhum. O agente não criou interface, fábrica nem camada sem uso.

**O resto do código:**
- A lista de empresas aparece duas vezes: em `ReservationService.java:77` (validação) e em `PriceCalculator.java:60-62` (desconto). Uma empresa nova incluída só na validação seria aceita com 0% de desconto, pelo `default -> BigDecimal.ZERO` (`PriceCalculator.java:63`), sem nenhum erro. Além disso, `switch (companyCode)` lança `NullPointerException` com código nulo (`PriceCalculator.java:59`). O cálculo só é seguro porque o serviço valida antes.
- `calculateWeekendPackage` grava 2 noites fixas (`PriceCalculator.java:30`) e recebe `checkIn`/`checkOut` sem usar (`:28`). A regra de que o pacote tem exatamente 2 noites fica no serviço (`ReservationService.java:82`), e o preço depende de que ela tenha sido conferida antes.
- `countNights` (`ReservationService.java:85-91`) conta as noites num laço dia a dia, repetindo o laço de `PriceCalculator.java:44-47`. `ChronoUnit.DAYS.between` resolveria em uma linha.
- A metade da devolução do pacote ficou como `new BigDecimal("0.5")` solto em `ReservationService.java:128`, enquanto os outros percentuais são constantes nomeadas (`PriceCalculator.java:17-19`).
- O que está certo: dinheiro em `BigDecimal` com o arredondamento da base (`cents`), datas em `LocalDate` e erros pela `HotelException` com `INVALID_REQUEST`, como o texto pede.

## Testes

**Números:** 40 testes rodaram, com 0 falhas, 0 erros e 0 pulados. Há 40 métodos escritos; a base tinha 26, e o agente acrescentou 14 sem apagar nenhum. Cobertura de linhas: 94,1% (513 de 545); na base era 93,8%. Cobertura de ramos: 87,6% (134 de 153); na base era 87,1%. No pacote `hotel.reservations`: 250 de 264 linhas e 82 de 89 ramos.
**Nota:** bom

Os testes novos conferem com valores todas as regras novas, e os exemplos do financeiro estão neles:
- **Convênio:** as três empresas (`src/test/java/hotel/reservations/PriceCalculatorTest.java:78`, `:90`, `:102`), com os R$ 779,80 / 93,58 / 686,22 do texto. A recusa sem código e com código inválido está em `ReservationServiceTest.java:164-185`, e a devolução integral em `:187`.
- **Pacote:** os três preços fechados (`PriceCalculatorTest.java:114`, `:126`, `:138`). As recusas de quarta a sexta e de sexta a segunda estão em `ReservationServiceTest.java:198-214`. A devolução de metade (R$ 260,00, no standard) está em `ReservationServiceTest.java:216`.
- **Estadia longa:** os R$ 1.749,30 / 174,93 / 1.574,37 do texto (`PriceCalculatorTest.java:150`) e a alta temporada (`:163`). O limite de 6 noites recusadas e 7 aceitas está em `ReservationServiceTest.java:227-239`, e a devolução integral em `:241`.

Pela pergunta da mutação, trocar 0,12 por 0,21, `< 7` por `<= 7`, ou 0,5 por outro valor faz algum teste falhar. Há poucas lacunas, e nenhuma pesa na nota. Nenhum teste chama a API com o campo `companyCode` nem confere o 400 pela HTTP: `HttpApiTest` não mudou, e o roteiro do comportamento cobre isso. Nenhum teste confere que o pacote ignora a alta temporada, nem que um `companyCode` enviado num plano que não é o convênio é ignorado.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, nenhuma chave estrangeira está sem índice e nenhuma data está em texto. Há 2 migrações: `V1__create_tables.sql` igual à da base e `V2__add_company_code.sql` nova. A conferência acusou `rate_plan`, `status`, `type` e `channel` em `text`, sem CHECK, mas isso veio da base.
**Nota:** bom

O agente fez a mudança do esquema do jeito certo: uma migração nova (`src/main/resources/db/migration/V2__add_company_code.sql:1`, `ALTER TABLE reservations ADD COLUMN company_code text`), sem editar a V1. Um banco que já roda sobe sem problema. O código novo usa parâmetro (`ReservationStore.java:33`, `:50`) e lê a coluna de volta (`ReservationStore.java:133`). A proteção contra reserva duplicada continua sendo o `EXCLUDE` da base. Há um detalhe pequeno: o código da empresa é gravado em qualquer plano (`ReservationStore.java:50`), e não só no convênio. Uma reserva STANDARD que chegue com `companyCode` guarda esse texto sem uso. Não corrompe nada, porque o preço ignora o código fora do convênio, e a referência do pattern trata isso como regra menor.

## Comportamento

**Números:** 72 de 72 conferências passaram: 46 de 46 na parte comum, 3 de 3 nos dados depois de reiniciar, 21 de 21 no cenário e 2 de 2 nas reservas simultâneas.
**Nota:** bom

Nada falhou. Os valores do financeiro conferem pela API: o convênio Tecnosul dá R$ 686,22; o pacote deluxe sai por R$ 820,00 e devolve R$ 410,00 se for cancelado; a estadia longa dá R$ 1.574,37. As três empresas têm os descontos certos. Convênio sem código, ou com código inválido, recebe 400 `INVALID_REQUEST`. O pacote fora de sexta a domingo e a estadia longa com 6 noites são recusados. O pacote continua com o mesmo preço em julho. O `companyCode` é ignorado no STANDARD, e o NON_REFUNDABLE continua com 15%. Para o dono da pousada, os três planos funcionam como ele descreveu, e o que já existia continua igual.

## Dúvidas

- A classe do design fica entre omissão e malfeito. Escolhi omissão porque não há estrutura nenhuma: não existe interface nem classe por plano, e os métodos privados `calculateWeekendPackage`, `validateRatePlan` e `getCompanyDiscount` são escolhidos por `switch` sobre o plano.
- A coluna `company_code` é gravada mesmo fora do convênio (`ReservationStore.java:50`). A referência manda contar esse ponto aqui, sem mudar a classe.
- O README ficou sem os planos novos e sem o campo `companyCode`, e ainda diz que todos os campos são obrigatórios. Não pesou em nenhuma coluna, mas o próximo a mexer no sistema vai ler um contrato desatualizado.
