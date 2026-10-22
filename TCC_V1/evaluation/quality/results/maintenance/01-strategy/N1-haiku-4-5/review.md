# Revisão: manutenção, cenário 01

## Visão geral

O projeto mantém a estrutura da base: `hotel.web` (Javalin, `HttpApi`) lê o JSON, monta um `NewReservation` e chama `ReservationService` (`hotel.reservations`), que valida, pede o preço ao `PriceCalculator` e grava pelo `ReservationStore` (JDBC com pool, PostgreSQL, esquema por migrações Flyway). O agente acrescentou os três planos (`CORPORATE`, `WEEKEND_PACKAGE`, `LONG_STAY`) ao `enum` `RatePlan`, o campo `companyCode` da requisição até o banco (nova migração `V2__add_company_code_to_reservations.sql`) e 17 testes. Pela API tudo funciona: 72 de 72 conferências passaram, e os valores do financeiro (R$ 686,22, R$ 820,00 / R$ 410,00, R$ 1.574,37) conferem. O ponto mais sério é o design: o texto avisa que "o comercial vive inventando plano", e o agente fez o contrário do que isso pede. O comportamento de cada plano ficou espalhado em `switch`/`if` sobre o plano em quatro lugares, dois no cálculo e dois no serviço. Com isso, o próximo plano obriga a mexer em todos eles de novo.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números são os mesmos da base (`measures/dependencies-base.md`: 7 pacotes, 17 dependências, o mesmo ciclo). `hotel.reservations` passou de 508 para 624 linhas, e `ReservationService` de 138 para 182.
**Nota:** bom

O agente não mexeu nas camadas. A borda HTTP só passou a repassar um campo a mais (`src/main/java/hotel/web/HttpApi.java:128-135`, `src/main/java/hotel/web/CreateReservationRequest.java:16`). As regras novas ficaram em `hotel.reservations`, sem Javalin e sem SQL (`src/main/java/hotel/reservations/ReservationService.java:54-91`, `src/main/java/hotel/reservations/PriceCalculator.java:29-83`). O SQL novo ficou só no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:19,32,50,133`). O agente não criou dependência nova nem ciclo novo. O único ciclo, `documents` ↔ `reservations` (o `ReservationService` cria o `ReservationReceipt`), já existia na base. O crescimento do `ReservationService` com regras de plano é um problema de design de baixo nível, e é avaliado na próxima seção.

## Design de baixo nível

**Números:** 5 planos, nenhum com estrutura própria. O comportamento dos planos é decidido em 4 lugares que perguntam qual é o plano: `PriceCalculator.java:31-36` (dois `if (ratePlan == …)`), `PriceCalculator.java:43-48` (`switch` do desconto, com os 5 casos), `ReservationService.java:55-78` (`switch` da validação) e `ReservationService.java:125-129` (`switch` da devolução, com os 5 casos). A lista das empresas aparece duas vezes (`ReservationService.java:81-85` e `PriceCalculator.java:68-75`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern pedido (Strategy).** A referência classifica como omissão exatamente o que o agente fez: os dois `switch` da base cresceram para os cinco casos (o desconto em `PriceCalculator.java:43-48` e a devolução em `ReservationService.java:125-129`), e ainda surgiram mais dois pontos de decisão. Um são os `if (ratePlan == RatePlan.WEEKEND_PACKAGE)` e `if (ratePlan == RatePlan.LONG_STAY)` em `PriceCalculator.java:31-36`. O outro é o `switch` de validação no serviço, em `ReservationService.java:54-79`. `RatePlan` continua um `enum` sem comportamento, com 3 constantes novas e só comentários (`src/main/java/hotel/reservations/RatePlan.java:8-13`). Nada põe o preço, a validação e a devolução de um plano num lugar só dele, nem para os dois planos antigos nem para os três novos. O plano "melhor idade" ou o "60 dias de antecedência" que o texto anuncia vão exigir mudanças em `RatePlan`, nos dois métodos de `PriceCalculator` e nos dois de `ReservationService`.

**O exagero.** O agente não criou interface, fábrica nem camada sem necessidade, por isso a avaliação é contenção.

**O resto do código** tem defeitos localizados:

- **A mesma regra em dois lugares que podem se separar.** Os códigos das empresas são validados em `ReservationService.java:81-85` e traduzidos em desconto em `PriceCalculator.java:68-75`. O `default -> BigDecimal.ZERO` de `PriceCalculator.java:73` aceita em silêncio um código que o serviço deixou passar. Se alguém acrescentar uma empresa num lugar e esquecer o outro, a reserva sai com 0% de desconto, sem erro.
- **Código duplicado e código morto.** O laço das diárias foi copiado inteiro em `calculateLongStay` (`PriceCalculator.java:57-66` repete `:37-42`). O `case WEEKEND_PACKAGE, LONG_STAY -> cents(BigDecimal.ZERO)` em `PriceCalculator.java:47` nunca é executado, porque os dois planos já saíram antes, em `:31-36`. `calculateWeekendPackage` recebe `checkIn` e `checkOut` e não os usa (`PriceCalculator.java:52`), e fixa as noites em `2` (`:54`).
- **Um nome que engana.** `WEEKEND_PACKAGE_DISCOUNT = 0.50` (`PriceCalculator.java:23`) não é desconto: é a fração devolvida no cancelamento, e é usada assim em `ReservationService.java:128`. Além disso, essa regra de devolução mora como constante na classe do preço.
- **`null` sem cuidado.** A sobrecarga de 4 argumentos repassa `companyCode = null` (`PriceCalculator.java:25-27`). Se alguém chamar `calculate(..., RatePlan.CORPORATE)` por ela, o `switch (companyCode)` de `PriceCalculator.java:69` lança `NullPointerException`. Pela API isso não acontece, porque o serviço valida antes (`ReservationService.java:35`), mas a classe pública deixa a armadilha montada.
- **Detalhes.** Há um `import java.time.DayOfWeek` sem uso (`PriceCalculator.java:6`), enquanto o serviço escreve `java.time.DayOfWeek` por extenso (`ReservationService.java:88-89`). As noites são contadas em dois lugares, com `toEpochDay` (`ReservationService.java:70` e `:90`).

## Testes

**Números:** 43 testes rodaram, 0 falharam, 0 pulados, 43 métodos escritos. Cobertura de linhas de 94,3% (532 de 564) e de ramos de 87,7% (135 de 154); `hotel.reservations` tem 265 de 279 linhas e 83 de 90 ramos cobertos. Na base eram 26 testes, com 93,8% das linhas e 87,1% dos ramos. Os 26 testes da base continuam passando: o agente só acrescentou `null` às chamadas de `NewReservation` (`src/test/java/hotel/reservations/ReservationServiceTest.java:36`, `src/test/java/hotel/reports/ReportsTest.java:33`) e não enfraqueceu nenhum deles.
**Nota:** bom

Os 17 testes novos conferem valores nas regras novas:

- **Preço.** As três empresas aparecem com diárias, desconto e total (`src/test/java/hotel/reservations/PriceCalculatorTest.java:78`, `:94`, `:109`; o exemplo do financeiro, R$ 686,22, está em `:78-91`). O pacote é testado nos três tipos de quarto (`:124`, `:135`, `:149`). A estadia longa é testada com o exemplo do financeiro (`:160`) e com noites de alta temporada (`:175`).
- **Validação, contra o banco.** O convênio sem código e com código inexistente é recusado (`ReservationServiceTest.java:164-179`). O pacote é recusado de quinta a sábado e de sexta a segunda (`:193-208`). A estadia longa é recusada com 6 noites e aceita com 7 (`:221-232`).
- **Devolução.** São conferidos o convênio (`:181`), o pacote, com R$ 410,00 (`:210`), e a estadia longa (`:234`).

Na pergunta da mutação, trocar 0.12 por 0.21, `< 7` por `<= 7` ou 0.50 por outra fração faz algum teste falhar. Ficam sem rede:

- o campo `companyCode` pela API e a resposta 400 pela API (o `src/test/java/hotel/web/HttpApiTest.java` não foi tocado);
- a leitura de `company_code` de volta do banco: `valid.companyCode()` em `ReservationServiceTest.java:178` vem do objeto em memória, não do `SELECT`;
- a regra de que o código é ignorado nos outros planos;
- a condição "exatamente duas noites" do pacote, de forma isolada: uma sexta seguida de um domingo nove dias depois não é testada.

Três testes repetem outros quase linha a linha: `verifiedExamples*` (`PriceCalculatorTest.java:190`, `:204`, `:218`) duplica `:78`, `:135` e `:160`. Isso é ruído, não um erro.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, nenhuma chave estrangeira está sem índice e nenhuma data está em texto. As colunas `rate_plan`, `status`, `type` e `channel` são `text` sem CHECK, como na base. A `V1__create_tables.sql` ficou igual à da base, e a `V2__add_company_code_to_reservations.sql` é nova. O EXCLUDE que impede reservas sobrepostas continua no lugar.
**Nota:** bom

O agente evoluiu o esquema do jeito certo: criou uma migração nova (`src/main/resources/db/migration/V2__add_company_code_to_reservations.sql:1`, `ALTER TABLE reservations ADD COLUMN company_code text`) sem editar a V1, então um banco que já roda o sistema sobe sem problema. A coluna aceita `NULL`, o que é coerente com os planos que não usam o código. O SQL novo usa parâmetros (`src/main/java/hotel/reservations/ReservationStore.java:31-33`, `:50`) e a leitura mapeia a coluna (`:19`, `:133`).

Há dois pontos pequenos, nenhum dos quais perde ou corrompe dado:

- O código da empresa é gravado em qualquer plano (`ReservationStore.java:50` grava `request.companyCode()` sem olhar o plano). Uma reserva `STANDARD` que chegue com `"companyCode": "XYZ"` fica com esse texto no banco, embora o pedido diga que nesses planos o código deve ser ignorado.
- `rate_plan` continua sem CHECK, o que vem da base.

## Comportamento

**Números:** 72 de 72 conferências passaram e nenhuma falhou. Por parte: a parte comum, 46 de 46; os dados depois de reiniciar, 3 de 3; o cenário 01, 21 de 21; as duas reservas ao mesmo tempo, 2 de 2.
**Nota:** bom

Para o dono da pousada, os três planos funcionam como ele descreveu:

- **O convênio.** Tecnosul, Agrovale e Farmavida dão 12%, 18% e 25%. O convênio sem código ou com um código desconhecido é recusado com 400 `INVALID_REQUEST`, e o cancelamento devolve tudo.
- **O pacote.** Custa R$ 520,00, R$ 820,00 e R$ 1.200,00, inclusive em julho. É recusado de sábado a segunda e de sexta a segunda, e o cancelamento devolve a metade (R$ 410,00).
- **A estadia longa.** Dá 10% de desconto, com a alta temporada valendo nas noites de temporada (R$ 1.754,30). É recusada com 6 noites, e o cancelamento devolve tudo.
- **O resto.** O `companyCode` é ignorado no `STANDARD`, e o `NON_REFUNDABLE` continua com 15%. A parte que já existia e os dados depois de reiniciar continuam iguais.

## Dúvidas

- O README não foi atualizado. Os planos novos, o campo `companyCode` e a migração V2 não aparecem nele (`final/README.md` é igual ao `base/README.md`). Isso não entra em nenhuma das cinco colunas, mas quem mantiver o sistema depois vai ler um documento desatualizado.
- Gravar o `companyCode` também nos planos que não são o convênio (`ReservationStore.java:50`) é uma regra pequena que ficou fora do plano. Pela referência, isso sozinho não muda a classe, que já é omissão pelos `switch`. Como a API não devolve o campo, não decidi se isso afeta algo além da limpeza dos dados.
