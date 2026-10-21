# Revisão: manutenção, cenário 01

## Visão geral

O sistema continua com a forma da base: `hotel.web.HttpApi` (Javalin) lê o JSON, monta um `NewReservation` e chama o
`ReservationService` (`hotel.reservations`), que confere quarto, capacidade e disponibilidade, pede o preço ao
`PriceCalculator` e grava pelo `ReservationStore` (JDBC com parâmetros, pool, migrações do Flyway). A montagem continua
toda em `Hotel.java:25-27`. O agente transformou o plano de tarifa num Strategy: uma interface `RatePlanPolicy`
(validar, calcular o preço, devolver no cancelamento) com uma classe por plano, os dois antigos inclusive, escolhida por
um `EnumMap` em `RatePlans`. Os dois `switch` da base sumiram. O código da empresa entrou por uma migração nova (`V2`),
sem mexer na `V1`. Os 34 testes passam e as 72 conferências do roteiro também. Não achei nenhum defeito sério. Ficam
só pontos pequenos: o `companyCode` é gravado mesmo fora do convênio, o README não foi atualizado, e há um ciclo entre
pacotes que já vinha da base.

## Arquitetura

**Números:** 7 pacotes, 17 dependências, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`); a base tinha os mesmos 7
pacotes, as mesmas 17 dependências e o mesmo ciclo (`measures/dependencies-base.md`). `hotel.reservations` passou de
13 classes e 508 linhas para 20 classes e 731 linhas; a maior classe continua sendo `HttpApi` (170 linhas).
**Nota:** bom

O agente não criou nenhuma dependência nova entre pacotes. As classes novas ficaram todas em `hotel.reservations`, sem
Javalin e sem JDBC (`CorporatePlan.java`, `LongStayPlan.java`, `WeekendPackagePlan.java`, `StandardPlan.java`,
`NonRefundablePlan.java`, `RatePlans.java`, `PercentOff.java`, `NightlyCharges.java`). A borda HTTP continua fina: a
única mudança é repassar o campo novo (`web/HttpApi.java:128-129`, `web/CreateReservationRequest.java:16`). Todo o SQL
novo fica no `ReservationStore` (`reservations/ReservationStore.java:31-50`, `:132`). As peças novas são criadas e
ligadas no mesmo lugar de antes (`Hotel.java:25-27`), e a mesma instância de `RatePlans` vai para o calculador e para o
serviço. O ciclo `documents` ↔ `reservations` já existia na base, e o agente não o piorou.

## Design de baixo nível

**Números:** os 5 planos estão na estrutura (5 implementações de `RatePlanPolicy`) e nenhum fica fora. Sobram 0
`switch`/`if` de comportamento sobre o plano: os dois da base (o desconto em `PriceCalculator.calculate` e a devolução
em `ReservationService.cancel`) foram removidos. A seleção acontece num único ponto, `RatePlans.java:19-21`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** `RatePlanPolicy.java:6-15` junta as três coisas que, segundo o pedido, variam por plano: quem pode
usar (`validate`), o preço (`price`) e a devolução (`refund`). Cada plano tem a sua classe, e os dois que já existiam
também foram migrados (`StandardPlan.java`, `NonRefundablePlan.java:8`, onde a constante dos 15% saiu do
`PriceCalculator`). O cálculo pede tudo ao plano sem dizer qual é (`PriceCalculator.java:25-29`), e o cancelamento
também (`ReservationService.java:84`). As regras de cada plano ficam dentro do plano: a lista de empresas e a recusa
estão em `CorporatePlan.java:10-21`, a regra de sexta a domingo em `WeekendPackagePlan.java:19-25`, as 7 noites em
`LongStayPlan.java:13-17` e a metade devolvida em `WeekendPackagePlan.java:36`. Nenhuma validação de plano ficou no
serviço. O cálculo das diárias com a temporada, que é comum a todos, foi separado em `NightlyCharges`
(`PriceCalculator.java:31-39`). Assim, o pacote recebe as diárias e pode trocá-las pelo preço fechado
(`WeekendPackagePlan.java:29-32`) sem que o calculador pergunte qual é o plano. Para um plano novo, basta uma constante
no `enum`, uma classe e uma linha em `RatePlans`.

**O exagero.** Não vi estrutura sem necessidade. `RatePlans` é o ponto único de seleção que a referência aceita.
`PercentOff` evita copiar o mesmo cálculo de desconto em quatro planos. `NightlyCharges` é um record pequeno que o
pattern usa. Não há fábrica, builder nem camada que só repassa.

**O resto do código.** O dinheiro continua em `BigDecimal` com arredondamento para centavos (`PercentOff.java:9-12`,
`WeekendPackagePlan.java:36`). Os campos são `final`, os mapas são imutáveis (`Map.of`), e o `EnumMap` é preenchido só
no construtor. Os métodos são curtos e os nomes dizem o que fazem. Pontos pequenos, que não mudam a nota:

- `ReservationStore.java:50` grava o `companyCode` de qualquer plano. O pedido diz que fora do convênio o campo "pode
  ignorar". O preço o ignora, mas o código fica gravado na reserva STANDARD.
- Quatro dos cinco `validate` não usam o parâmetro `Room` (`RatePlanPolicy.java:10`).
- `PriceCalculator.calculate` passou também a validar (`PriceCalculator.java:24-27`), o que o nome "calculadora" não
  diz.
- O `README.md` ficou igual ao da base e ainda descreve só os dois planos antigos (`README.md:32-38`).

## Testes

**Números:** 34 testes rodaram, 0 falharam, 0 foram pulados; 34 métodos escritos (a base tinha 26, todos mantidos).
Cobertura de linhas de 94,4% (523 de 554; na base, 93,8%) e de ramos de 87,9% (109 de 124; na base, 87,1%).
`hotel.reservations` tem 260 de 273 linhas e 57 de 60 ramos cobertos.
**Nota:** bom

O agente escreveu `NewRatePlansTest` com 7 testes e mais um teste na API, todos contra o banco de verdade e com os
valores do pedido:

- as três empresas com os valores exatos, incluindo 779,80 / 93,58 / 686,22
  (`reservations/NewRatePlansTest.java:44-57`);
- a recusa sem código e com código desconhecido, e a devolução integral do convênio (`:59-67`);
- o pacote nos três tipos de quarto, em julho e com a devolução de 410,00 (`:70-83`);
- as três formas de data errada no pacote: começar no sábado, ter 3 noites, ter 1 noite (`:86-93`);
- a estadia longa com o exemplo do texto, com a temporada e com a devolução (`:96-107`), e a recusa com 6 noites
  (`:110-113`);
- o `companyCode` ignorado no STANDARD (`:116-121`);
- pela API, o 201 com o total de 686,22 e o 400 `INVALID_REQUEST` (`web/HttpApiTest.java:109-120`).

A pergunta da mutação: trocar 0.12 por 0.21, `< 7` por `<= 7` (a reserva de 7 noites em `:97` passaria a ser recusada),
`FRIDAY` por outro dia, ou 0.5 por 1, faz algum teste falhar em todos os casos. Os testes da base só foram adaptados à
assinatura nova (`PriceCalculatorTest.java:63-66`, mais um `null` no `NewReservation`), sem perder nenhuma asserção. O
que fica sem teste é pouco: o cancelamento de um pacote ainda pendente (o 0,00 da metade de nada) e um código de
empresa em minúsculas, que o texto não define.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`, sem chave estrangeira sem índice e sem
data guardada como texto. 2 migrações: a `V1__create_tables.sql` igual à da base e a `V2__reservation_company_code.sql`
nova. As colunas `rate_plan`, `status`, `type` e `channel` estão sem CHECK, como já era na base. 0 SQL montado com dado
de fora: os 4 candidatos são constantes.
**Nota:** bom

O agente fez a mudança do esquema do jeito certo: uma migração nova, em vez de editar a que já existia
(`src/main/resources/db/migration/V2__reservation_company_code.sql:2`, `ALTER TABLE reservations ADD COLUMN
company_code text`). A coluna aceita nulo, o que serve para os planos sem empresa. O `INSERT` e a leitura usam
parâmetros (`ReservationStore.java:31-50`, `:132-133`). A proteção contra reserva dupla continua sendo o `EXCLUDE USING
gist` da base. A falta de CHECK em `rate_plan` vem da base; o agente não piorou nada, porque os planos novos são valores
do mesmo `enum` gravados por `name()` (`ReservationStore.java:43`). O único ponto é o já citado no design: o
`company_code` fica gravado também nas reservas fora do convênio (`ReservationStore.java:50`).

## Comportamento

**Números:** 72 de 72 conferências passaram:

- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 01: 21 de 21;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Os três exemplos do financeiro conferem pela API: o Tecnosul com 779,80 / 93,58 / 686,22, o pacote deluxe com 820,00 e
devolução de 410,00, e a estadia longa com 1.749,30 / 174,93 / 1.574,37. Também conferem:

- Agrovale e Farmavida;
- as recusas com 400 `INVALID_REQUEST`: convênio sem código ou com código desconhecido, pacote fora de sexta a domingo,
  estadia longa com 6 noites;
- o pacote com o mesmo preço em julho;
- a estadia longa com a temporada;
- o `companyCode` ignorado no STANDARD;
- o NON_REFUNDABLE com os 15% de antes.

A parte que já existia continua igual. Para o dono da pousada, os três planos funcionam como o comercial combinou, e o
que funcionava antes continua funcionando.

## Dúvidas

O texto não decide se o código da empresa deve aceitar minúsculas (`tecnosul`); o agente recusa, pois exige o código
exato (`CorporatePlan.java:17`), o que segue a letra do pedido. Também fica em aberto se gravar o `companyCode` nos
planos que não são de convênio (`ReservationStore.java:50`) contraria o "pode ignorar" do pedido. Como o preço e a
resposta não mudam, tratei isso como ponto pequeno.
