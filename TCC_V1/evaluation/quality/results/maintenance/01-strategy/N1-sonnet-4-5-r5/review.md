# Revisão: manutenção, cenário 01

## Visão geral

O projeto (vindo da base) tem sete pacotes: `hotel` monta as peças (Javalin, pool, Flyway), `hotel.web` é a borda HTTP
(`HttpApi` lê o JSON e chama o serviço), `hotel.reservations` guarda as regras (`ReservationService`,
`PriceCalculator`) e o acesso ao banco (`ReservationStore`, JDBC com `?`), e os dados ficam no PostgreSQL, com o
esquema criado pelo Flyway. Uma requisição `POST /reservations` passa por `HttpApi` → `ReservationService.create` →
`PriceCalculator.calculate` → `ReservationStore.add`. O agente pôs os três planos novos (CORPORATE, WEEKEND_PACKAGE,
LONG_STAY) do jeito mais curto: acrescentou casos aos dois `switch` que a base já tinha (preço e devolução) e criou
uma cadeia de `if (ratePlan == …)` no serviço para as regras de quem pode usar. O sistema faz tudo o que o dono pediu
(72 de 72 conferências do roteiro passaram, 41 testes passam) e a migração nova está certa. O ponto mais sério é o
design: a variação que o texto anuncia que vai crescer ("o comercial vive inventando plano") ficou espalhada em três
lugares e duas classes, e o próximo plano vai mexer em todos eles.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), igual à base
(`measures/dependencies-base.md`); `hotel.reservations` passou de 508 para 589 linhas, e `ReservationService` passou
de 138 para 176 linhas e virou a maior classe.
**Nota:** bom

O agente não mexeu na forma das camadas. A borda continua fina: `HttpApi` só repassa o campo novo
(`src/main/java/hotel/web/HttpApi.java:128-130`, `src/main/java/hotel/web/CreateReservationRequest.java:16`). As
regras novas ficaram em `hotel.reservations`, sem Javalin e sem SQL (`ReservationService.java:54-83`,
`PriceCalculator.java:33-54`), e o SQL novo ficou só em `ReservationStore` (`ReservationStore.java:44`). A montagem
não mudou. O único ciclo, `hotel.documents` ↔ `hotel.reservations`, já existia na base. Há um acoplamento novo entre
as duas classes de regra: o serviço lê a tabela de descontos do calculador (`ReservationService.java:59` usa
`PriceCalculator.CORPORATE_DISCOUNTS`), mas isso é um problema de design (a regra de um plano dividida em duas classes)
e entra na coluna seguinte, não na de arquitetura.

## Design de baixo nível

**Números:** 5 planos; 0 têm o comportamento num lugar só dele. Três pontos decidem pelo plano: o `if` e o `switch` de
5 casos do preço (`PriceCalculator.java:34` e `:46-52`), o `switch` de 5 casos da devolução
(`ReservationService.java:117-123`) e 3 `if (ratePlan == …)` de validação (`ReservationService.java:55`, `:64`,
`:77`). `RatePlan` continua um `enum` sem comportamento (`RatePlan.java`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern (Strategy).** O texto diz que cada plano chega "com o seu jeito de calcular o preço, com as suas regras de
quem pode usar e com a sua regra de devolução" e que vêm mais planos. A referência chama de omissão exatamente o que
aconteceu: "os `switch` que já existiam crescendo para os cinco casos" e "`if (ratePlan == …)` no serviço ou no
cálculo". O desconto cresceu de 2 para 5 casos (`PriceCalculator.java:46-52`), a devolução também
(`ReservationService.java:117-123`), o preço fechado do pacote é um `if` antes do laço (`PriceCalculator.java:34-37`),
e a validação de cada plano é um `if` no serviço (`ReservationService.java:54-83`). Não há nenhuma estrutura que
isole o plano: nem interface, nem `enum` com corpo por constante. O plano para a melhor idade, ou para quem reserva com
60 dias, vai exigir mudar `RatePlan`, os dois `switch` e `validateRatePlan`, em duas classes. Nada aproxima o código do
pattern.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface, fábrica ou camada nova. Contenção.

**O resto do código (o que o agente acrescentou).** São defeitos pequenos e localizados:
- A regra do convênio está dividida em duas classes: a tabela de descontos mora em `PriceCalculator.java:23-26`, e o
  serviço a consulta por um campo `static` de pacote para validar (`ReservationService.java:59`). O desconto também
  depende de o serviço ter validado antes: `CORPORATE_DISCOUNTS.get(companyCode)` (`PriceCalculator.java:49`) devolve
  `null` e lança `NullPointerException` se alguém chamar o calculador sem passar pelo serviço.
- O caso `WEEKEND_PACKAGE -> cents(BigDecimal.ZERO)` no `switch` (`PriceCalculator.java:51`) nunca roda, porque o
  `return` da linha 36 vem antes. Ele só existe para o `switch` compilar, e é sinal de que a variação não cabe ali.
- O número de noites do pacote está fixo em `2` (`PriceCalculator.java:36`). Só está certo porque o serviço validou
  antes.
- A conta de noites com `ChronoUnit.DAYS.between` está repetida (`ReservationService.java:65` e `:78`), e o laço do
  calculador já conta as noites.
- O fator `new BigDecimal("0.5")` está solto no meio da devolução (`ReservationService.java:121`), enquanto os outros
  fatores são constantes nomeadas. O import `DayOfWeek` em `PriceCalculator.java:7` não é usado.
- `companyCode` é gravado em qualquer plano (`ReservationStore.java:44`, `Reservation.java:39`). O texto diz "pode
  ignorar", e o código guarda o valor, embora não o use.

Os tipos estão certos: `BigDecimal` com `HALF_EVEN` (`PriceCalculator.java:73-75`) e `LocalDate`. O código da empresa
é um `String` solto, o que é aceitável aqui. Nenhum desses defeitos é grave sozinho, mas juntos, e somados à variação
espalhada, pedem uma correção antes de aceitar.

## Testes

**Números:** 41 testes rodaram, 0 falharam, 0 pulados; 41 métodos escritos (a base tinha 26, todos mantidos; 15
novos). Cobertura de linhas: 93,9% (509/542), contra 93,8% na base. Cobertura de ramos: 88,2% (127/144), contra 87,1%
na base. `hotel.reservations` tem 246/261 linhas e 75/80 ramos cobertos.
**Nota:** bom

Os testes da base não foram enfraquecidos: o patch só acrescenta o argumento `null` às chamadas
(`PriceCalculatorTest.java`, `ReportsTest.java`, `ReservationServiceTest.java:33-42`). As regras novas são conferidas
com valores, em três níveis:
- preço: Tecnosul com o exemplo do texto, 779,80 / 93,58 / 686,22 (`PriceCalculatorTest.java:78`); Agrovale 18%
  (`:91`); Farmavida 25% (`:103`); o pacote nos três quartos, 520/820/1200 (`:115`); a estadia longa com o exemplo
  1.749,30 / 174,93 / 1.574,37 (`:133`) e com temporada (`:146`);
- regras e devolução: convênio sem código, com código vazio e com código inválido recusados (`ReservationServiceTest.java:169`);
  devolução integral do convênio (`:183`); pacote recusado começando no sábado, com 3 noites e com 1 noite (`:192`);
  devolução de 410,00 (`:205`); estadia longa recusada com 6 noites e aceita com 7 (`:213`), e devolução integral
  (`:222`);
- pela API, com 400 `INVALID_REQUEST` e os totais (`HttpApiTest.java:124`, `:156`, `:179`).

A pergunta da mutação: trocar 0,12 por 0,21 quebra `PriceCalculatorTest.java:78`; trocar `nights < 7` por `<= 7` ou
por `< 6` quebra `ReservationServiceTest.java:213`; trocar 0,5 por 1 quebra `:205`. Ficam sem rede: o pacote em alta
temporada (nenhum teste do agente marca uma sexta de julho; o roteiro de comportamento confere isso), o `companyCode`
ignorado nos outros planos, e o cancelamento do pacote ainda pendente. Os testes pela API comparam dinheiro com
`asDouble()` (`HttpApiTest.java:144`), o que é frágil, mas a base já fazia assim (`HttpApiTest.java:77`). São lacunas
pequenas.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; chave estrangeira com índice; datas em
`date`. As conferências acusaram só `rate_plan`, `status`, `type` e `channel` sem `CHECK`, o que já vinha da base.
Migrações: `V1__create_tables.sql` igual à da base, `V2__add_company_code.sql` nova. 0 transações explícitas, igual
à base.
**Nota:** bom

O agente fez a mudança do esquema do jeito certo: uma migração nova, sem editar a que já existia
(`src/main/resources/db/migration/V2__add_company_code.sql:2`, `ALTER TABLE reservations ADD COLUMN company_code text`).
Ela roda sobre um banco que já tem dados, e a coluna pode ser nula, o que está certo, porque só o convênio a usa. O SQL
novo usa parâmetros (`ReservationStore.java:31-44`), e a leitura devolve a coluna (`ReservationStore.java` em
`COLUMNS` e no mapeamento da linha). Os candidatos de SQL montado com `+` que o script lista só concatenam a constante
`COLUMNS`. A proteção contra reserva dupla é a restrição `EXCLUDE` da base, que não mudou. Um detalhe: o código da
empresa é gravado também nos planos que não o usam (`ReservationStore.java:44`). Não corrompe nada, e no design isso
já está anotado.

## Comportamento

**Números:** 72 de 72 conferências passaram: a parte comum, 46 de 46; os dados depois de reiniciar, 3 de 3; o
cenário 01, 21 de 21; as reservas simultâneas, 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API: os três descontos do convênio (686,22 no exemplo da Tecnosul), a recusa sem
código ou com código desconhecido (400 `INVALID_REQUEST`), o pacote com preço fechado (820,00 no deluxe, 520,00 e
1.200,00), mesmo em julho, a devolução de metade (410,00), a recusa fora de sexta a domingo, a estadia longa
(1.574,37, com a temporada valendo) e a recusa com 6 noites, a devolução integral do convênio e da estadia longa, o
`companyCode` ignorado no plano normal, e o normal e o não reembolsável como antes. Para o dono da pousada, o sistema
está pronto para os três planos. O custo aparece no próximo plano, como diz a coluna de design.

## Dúvidas

- O texto diz que o código da empresa "é o nome dela em maiúsculas". O agente aceita só o código exatamente em
  maiúsculas (`ReservationService.java:59`), e um `tecnosul` em minúsculas é recusado. O texto deixa isso em aberto, e
  a escolha é razoável.
- Gravar o `companyCode` nos planos que não o usam (`ReservationStore.java:44`) pode ser lido como "não ignorar" o
  campo. A resposta da API não o mostra, e o roteiro não acusou nada, por isso não pesou em nenhuma nota.
