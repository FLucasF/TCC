# Revisão: manutenção, cenário 01

## Visão geral

A base tem sete pacotes: `hotel` (a montagem), `web` (Javalin), `reservations` (regras e o acesso ao banco em `ReservationStore`), `rooms`, `documents`, `messages` e `reports`. Uma requisição entra em `HttpApi`, vira um `NewReservation` e segue para `ReservationService`, que valida, consulta a disponibilidade, calcula o preço e grava pelo `ReservationStore` (JDBC com parâmetros, PostgreSQL com migração do Flyway). O agente acrescentou ao `enum RatePlan` uma estratégia por constante, passada no construtor (`RatePlan.java:4-8`). São cinco classes, uma por plano (`StandardPricing`, `NonRefundablePricing`, `CorporatePricing`, `WeekendPackagePricing`, `LongStayPricing`), cada uma com `validate`, `calculatePrice` e `calculateRefund`. Os dois `switch` da base saíram do cálculo e do cancelamento. Também criou o campo `companyCode` na requisição e em `NewReservation`. As 72 conferências do roteiro passam, e os 36 testes também. Não há defeito sério. Os pontos mais fracos são três. O laço que soma as noites foi copiado em quatro estratégias. O `companyCode` não é gravado no banco: a reserva do convênio não registra qual empresa deu o desconto. E os testes deixam de fora parte das regras novas (Agrovale, Farmavida, dois dos três preços do pacote, a temporada na estadia longa).

## Arquitetura

**Números:** 7 pacotes, 17 dependências, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo (`measures/dependencies-base.md`). `hotel.reservations` passou de 13 para 17 classes e de 508 para 710 linhas.
**Nota:** bom

As novas classes ficam em `hotel.reservations` e não usam nem Javalin nem JDBC (a tabela de bibliotecas de `measures/dependencies.md` não mudou). A borda HTTP continua fina: `HttpApi.java:128-135` só repassa `body.companyCode()` para o `NewReservation`. A regra do plano é chamada pelo serviço: `ReservationService.java:42` para a validação e `ReservationService.java:84` para a devolução. O SQL continua só em `ReservationStore`. O ciclo `documents` ↔ `reservations` já existia na base, e o agente não o piorou. Nenhuma classe cresceu demais: a maior continua sendo `HttpApi` (176 linhas).

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (os 2 da base e os 3 novos), 0 `switch` ou `if` sobre o plano fora dela. São 3 comportamentos por plano (preço, validação, devolução), todos nas estratégias. O laço que soma as noites aparece copiado em 4 das 5 estratégias.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O `enum` recebe a estratégia no construtor (`RatePlan.java:4-17`), e a interface `RatePlanPricing` (`RatePlanPricing.java:7-13`) reúne as três regras de cada plano. Os dois `switch` da base saíram: o do desconto em `PriceCalculator.calculate` (hoje `PriceCalculator.java:10-13`) e o da devolução em `ReservationService.cancel` (hoje `ReservationService.java:84`). Os planos que já existiam também entraram na estrutura (`StandardPricing.java`, `NonRefundablePricing.java`). As validações de cada plano moram no próprio plano: o código da empresa em `CorporatePricing.java:16-26`, sexta a domingo e 2 noites em `WeekendPackagePricing.java:18-32`, 7 noites em `LongStayPricing.java:13-20`. O serviço não nomeia nenhum plano. É o acerto da referência.

**O exagero.** O agente não criou fábrica, registro nem camada nova. `PriceCalculator` virou um repasse de uma linha (`PriceCalculator.java:10-13`), mas a classe já existia na base e o agente só a esvaziou. Isso deixa uma inconsistência: o serviço vai direto ao plano para validar e devolver (`ReservationService.java:42,84`) e passa pelo `PriceCalculator` para o preço. Não chega a ser estrutura nova sem necessidade.

**O resto do código.**
- **Duplicação:** o laço que soma as diárias foi copiado em quatro classes: `StandardPricing.java:18-23`, `NonRefundablePricing.java:17-22`, `CorporatePricing.java:30-35` e `LongStayPricing.java:24-29`. Cada nova estratégia "com desconto percentual" repete o laço e o `new Price(...)`. O cálculo da temporada mora em `StandardPricing` como `static` (`StandardPricing.java:33-47`) e é chamado pelas outras estratégias (`CorporatePricing.java:33`, `LongStayPricing.java:27`, `NonRefundablePricing.java:20`): a regra comum ficou dentro de um dos casos. O `ChronoUnit` é escrito com o nome completo e repetido (`LongStayPricing.java:14`, `WeekendPackagePricing.java:27`).
- **A interface carrega um dado de um plano só:** `calculatePrice(..., String companyCode)` (`RatePlanPricing.java:11`) obriga os cinco planos a receber o código da empresa, que só o convênio usa. E o próximo plano que precisar de outro dado (a idade, a antecedência) vai mudar a assinatura de todos. Passar o `NewReservation`, como já faz o `validate` (`RatePlanPricing.java:9`), evitaria isso.
- **`null` sem cuidado:** `CorporatePricing.calculatePrice` faz `COMPANY_DISCOUNTS.get(companyCode)` e multiplica (`CorporatePricing.java:36-37`). Isso confia que o `validate` rodou antes; chamado sozinho com um código inválido, o método dá `NullPointerException`. O pacote também fixa `2` noites em `WeekendPackagePricing.java:37` em vez de contar.
- **Documentação:** o agente apagou os comentários que explicavam os planos e o preço (`RatePlan`, `PriceCalculator`, `CreateReservationRequest`, ver o `changes.patch`) e não atualizou o `README.md` (idêntico ao da base), que ainda descreve só dois planos e não fala do `companyCode`.

## Testes

**Números:** 36 testes rodaram, 0 falhas, 0 erros, 0 pulados; 36 métodos escritos (a base tinha 26, todos mantidos). Cobertura de linhas: 94,4% (538 de 570); de ramos: 87,9% (116 de 132). Na base, eram 93,8% e 87,1%. Em `hotel.reservations`: 271 de 285 linhas e 64 de 68 ramos.
**Nota:** com problemas

**O que está testado, com valores.** Os três exemplos do financeiro:
- Tecnosul 779,80 / 93,58 / 686,22 em `PriceCalculatorTest.java:76-86` e pela API em `HttpApiTest.java:124-135`;
- o pacote deluxe por 820,00 em `PriceCalculatorTest.java:88-98`, e a devolução de 410,00 em `ReservationServiceTest.java:208-214`;
- a estadia longa 1.749,30 / 174,93 / 1.574,37 em `PriceCalculatorTest.java:100-109`.

Também estão testadas as recusas: o convênio sem código, com código vazio ou com código inválido (`ReservationServiceTest.java:168-181`); o pacote de quinta, de sexta a segunda e de uma noite (`ReservationServiceTest.java:193-206`); a estadia longa de 6 noites, e a de 7 aceita (`ReservationServiceTest.java:216-225`). Por fim, a devolução integral do convênio e da estadia longa (`ReservationServiceTest.java:184-190`, `227-233`). Os 26 testes da base só mudaram para o novo parâmetro, nenhum foi enfraquecido.

**A pergunta da mutação.** Algumas regras novas ficam sem rede:
- trocar 0.18 (Agrovale) ou 0.25 (Farmavida) em `CorporatePricing.java:11-12` não derruba nenhum teste;
- o preço do pacote no standard (520,00) e na suíte (1.200,00), em `WeekendPackagePricing.java:13,15`, também não tem teste;
- nenhum teste confere que a estadia longa mantém a alta temporada nem que o pacote a ignora em julho;
- nenhum teste confere que o `companyCode` é ignorado nos outros planos.

Trocar `<` por `<=` em `LongStayPricing.java:15` seria pego (6 noites recusadas, 7 aceitas). Os testes da API em `HttpApiTest.java:122-160` usam `asDouble()` para comparar o dinheiro, uma asserção mais frouxa que a dos testes de serviço.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`. A chave estrangeira tem índice e as datas estão em `date`. O `EXCLUDE` contra a reserva dupla está lá. A migração `V1__create_tables.sql` está igual à da base, e não há migração nova. As conferências só apontam `rate_plan`, `status`, `type` e `channel` em `text` sem `CHECK`, e isso já vinha da base.
**Nota:** com problemas

O agente não mexeu no esquema e não editou a migração da base: o banco de quem já roda o sistema sobe sem problema. O SQL continua com parâmetros (`ReservationStore.java:31-49`). O defeito é que o `companyCode` não é guardado. O `INSERT` não tem a coluna (`ReservationStore.java:31-33`), e a leitura monta a reserva com `null` (`ReservationStore.java:132`). Por isso, numa reserva `CORPORATE`, fica gravado o desconto, mas não a empresa que o justificou. A pousada não tem como saber, depois, a qual convênio cobrar ou conferir aquele desconto. Uma migração `V2` com a coluna `company_code` resolveria. A falta de `CHECK` em `rate_plan` vem da base. Agora são cinco valores possíveis, e o banco continua aceitando qualquer texto.

## Comportamento

**Números:** 72 de 72 conferências passaram:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário 01: 21 de 21;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- os três descontos do convênio (686,22, 639,44 e 584,85) e a recusa sem código ou com código desconhecido (400 `INVALID_REQUEST`);
- o pacote nos três quartos (520,00, 820,00 e 1.200,00), inclusive em julho, a recusa fora de sexta a domingo e a devolução de 410,00;
- a estadia longa com e sem temporada (1.574,37 e 1.754,30), a recusa com 6 noites e a devolução integral;
- o `companyCode` ignorado num `STANDARD`;
- o normal e o não reembolsável como antes.

Para o dono da pousada, os três planos novos funcionam como o comercial definiu, e o que já existia continua igual. A única coisa que não aparece pela API é a empresa do convênio, que não fica gravada (contado no Banco).

## Dúvidas

- O código da empresa é comparado exatamente (`CorporatePricing.java:21`): "tecnosul" em minúsculas é recusado. O texto diz que o código é o nome em maiúsculas, então a escolha é defensável, mas o site pode mandar em minúsculas.
- Não gravar o `companyCode` pesou como "com problemas" no Banco, e não como "ruim". Nada do que o sistema faz depois (devolução, relatórios) precisa dele, mas é um dado informado pelo hóspede que se perde em silêncio.
