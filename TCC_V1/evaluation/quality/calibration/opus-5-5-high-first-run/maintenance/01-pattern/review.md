# Revisão: manutenção, cenário 01

## Visão geral

O sistema continua com a forma da base: `hotel.web` (Javalin, `HttpApi`) lê a requisição e chama
`hotel.reservations.ReservationService`, que valida, calcula o preço (`PriceCalculator`) e grava pelo
`ReservationStore` (JDBC com parâmetros, migrações do Flyway). A montagem continua num lugar só, em
`hotel/Hotel.java:22-27`. Para os três planos novos, o agente tirou os dois `switch` de comportamento da base (o
desconto em `PriceCalculator.calculate` e a devolução em `ReservationService.cancel`) e criou uma interface
`RatePlanPolicy` com `validate`, `price` e `refund`. Cada um dos cinco planos tem a sua classe, inclusive os dois que já
existiam, e um `EnumMap` em `RatePlans` escolhe a classe certa. O `companyCode` chega pelo `CreateReservationRequest`
e é guardado numa coluna nova, criada por uma migração nova (`V2__reservation_company_code.sql`); a V1 não foi tocada.
O `mvn verify` passa (30 testes) e o roteiro de comportamento passa inteiro (72 de 72). Não há defeito sério. Os pontos
fracos são pequenos: os testes novos só conferem os três exemplos do texto e não verificam o código de erro; um plano que
alguém esquecer de registrar em `RatePlans` só aparece em produção, como `NullPointerException`; e o README não foi
atualizado (ainda descreve só dois planos, `README.md:32-38`).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais na base e na final. Há um ciclo
(`hotel.documents` ↔ `hotel.reservations`), que já existia na base (`measures/dependencies-base.md`). O pacote
`hotel.reservations` passou de 13 classes e 508 linhas para 20 classes e 731 linhas. As maiores classes continuam as da
base: `HttpApi` (170 linhas), `ReservationStore` (145) e `ReservationService` (137).
**Nota:** bom

A mudança não altera nenhuma dependência entre pacotes, e as classes novas ficam todas em `hotel.reservations`, sem
Javalin nem JDBC (a tabela de bibliotecas é a mesma da base). A borda HTTP só ganhou o campo novo:
`web/CreateReservationRequest.java:16` e `web/HttpApi.java:128-129`, que repassa `body.companyCode()` sem nenhuma regra
de plano. As regras de plano ficam nas classes de plano. O SQL novo fica no `ReservationStore`
(`reservations/ReservationStore.java:31-33,50`). `RatePlans` é criado em `Hotel.java:25` e entregue ao `PriceCalculator`
e ao `ReservationService` (`Hotel.java:26-27`), sem `new` escondido nem `static` global. O ciclo entre `documents` e
`reservations` veio da base, e o agente não o piorou.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (uma implementação de `RatePlanPolicy` por plano). Fora dela ficam 0 decisões
de comportamento: nenhum `switch` ou `if` sobre o plano em `src/main`, e o único ponto que escolhe o plano é o
`EnumMap` de `RatePlans`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern pedido, o Strategy, está aplicado direito:
- A interface `reservations/RatePlanPolicy.java:7-15` junta as três coisas que variam por plano: quem pode usar
  (`validate`), o preço (`price`) e a devolução (`refund`).
- Os dois planos da base foram para a estrutura: `StandardPlan.java:13-19` e `NonRefundablePlan.java:8-21`.
- Cada plano novo tem a sua classe, e as suas regras moram nela:
  - `CorporatePlan.java:10-13,17` (a tabela de descontos por empresa e a recusa sem código ou com código desconhecido);
  - `WeekendPackagePlan.java:12-16,19-37` (sexta ao domingo, preço fechado por tipo de quarto, devolução da metade);
  - `LongStayPlan.java:10-27` (mínimo de 7 noites, 10% de desconto).
- O cálculo e o cancelamento só pedem ao plano, sem dizer qual é: `PriceCalculator.java:26-28` e
  `ReservationService.java:84`.
- A seleção é um único `EnumMap` (`RatePlans.java:9-21`), o que a referência aceita.
- Os `switch` da base sumiram: o patch os remove de `PriceCalculator.calculate` e de `ReservationService.cancel`.
- O que é comum aos planos não foi copiado: as diárias com a temporada continuam no `PriceCalculator`
  (`nightlyCharges` e `nightPrice`) e chegam ao plano como `NightlyCharges`. O desconto percentual arredondado ficou num
  ajudante só, `PercentOff.java:11-12`, usado por quatro planos.

Não há exagero. A única interface nova tem cinco implementações e é o próprio pattern. `RatePlans` é o ponto de
seleção, `NightlyCharges` é um `record` de dois campos com uso real e `PercentOff` evita copiar a conta quatro vezes. Não
há fábrica, builder nem camada que só repasse.

No resto do código, os tipos estão certos: `BigDecimal` com `HALF_EVEN` (`PriceCalculator.cents`), `LocalDate` e
`enum` para o plano. Não há estado mutável novo, e o agente não deixou métodos longos. Pontos menores, que não derrubam
a nota:
- `RatePlans.policyFor` devolve `null` para um plano não registrado (`RatePlans.java:19-20`). Um sexto valor no `enum`
  sem a linha correspondente só quebraria em tempo de execução, com `NullPointerException` (resposta 500), em vez de
  falhar ao subir. O mesmo vale para `PRICE_BY_ROOM_TYPE.get` (`WeekendPackagePlan.java:30`) se surgir um tipo de quarto
  novo.
- O `companyCode` é gravado em qualquer plano (`ReservationStore.java:50`), embora o texto diga que fora do convênio ele
  pode ser ignorado. É inofensivo, porque não muda o preço nem aparece na resposta.

## Testes

**Números:** 30 testes rodaram, com 0 falhas, 0 erros e 0 pulados, em 30 métodos (a base tinha 26). Cobertura de
linhas: 94,0% (521 de 554; a base tinha 93,8%). Cobertura de ramos: 87,1% (108 de 124; igual à base). No pacote
`hotel.reservations`: 258 de 273 linhas e 56 de 60 ramos.
**Nota:** com problemas

Os 26 testes da base continuam lá e passam. O patch só ajustou as assinaturas novas (`PriceCalculator(RatePlans)` e o
`companyCode` no `NewReservation`) e não enfraqueceu nenhuma asserção. O agente acrescentou 4 testes, em
`reservations/NewRatePlansTest.java`, que rodam contra o banco e conferem com valores os três exemplos do financeiro:
- o convênio Tecnosul, com desconto de 93,58 e total de 686,22 (`:36-39`);
- o pacote deluxe, com total de 820,00 e devolução de 410,00 depois de pago e cancelado (`:50-54`);
- a estadia longa, com desconto de 174,93 e total de 1.574,37 (`:60-63`).

Também conferem as recusas: o convênio sem código e com o código "ACME" (`:44-45`), o pacote começando num sábado
(`:55`) e a estadia longa de 6 noites (`:64`). O teste de 7 noites aceitas contra 6 recusadas fecha o limite `<`.

Mas parte das regras novas ficou sem rede:
- Ninguém confere os descontos da Agrovale (18%) e da Farmavida (25%): trocar `0.18` por `0.81` em
  `CorporatePlan.java:12` não derruba nenhum teste.
- Nenhum teste confere os preços do pacote no standard (520,00) e na suíte (1.200,00), o pacote na alta temporada, o
  pacote de sexta a segunda, nem a estadia longa com noites de temporada.
- Nenhum teste confere a devolução integral do convênio e da estadia longa: trocar `amountPaid()` por zero em
  `CorporatePlan.java:29-30` ou `LongStayPlan.java:26-27` passa.
- As recusas usam só `assertThrows(HotelException.class, …)` (`NewRatePlansTest.java:44-45,55,64`) e não conferem o
  `INVALID_REQUEST`, que o texto pede. Os outros testes da base fazem isso com `errorOf`.
- Não há teste pela API com o campo `companyCode`, e nenhum teste lê de volta do banco uma reserva do convênio.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` (6 de 6 colunas), nenhuma chave
estrangeira sem índice, nenhuma data em texto. `rate_plan` e `status` são texto sem CHECK, como já eram na base. A V1 está
igual à da base e a V2 é nova. Todo o SQL usa parâmetros: os `+` que o script lista só juntam constantes.
**Nota:** bom

A mudança do esquema foi feita do jeito certo: uma migração nova,
`src/main/resources/db/migration/V2__reservation_company_code.sql:2`
(`ALTER TABLE reservations ADD COLUMN company_code text`), sem editar a V1. Assim, um banco que já roda a base sobe sem
problema. A coluna aceita nulo, o que está certo, porque só o convênio a usa. O `INSERT` e a leitura ganharam a coluna
com parâmetro (`reservations/ReservationStore.java:31-33,50` e `:132-133`). As regras que o banco protege vieram da base
e continuam lá: o `EXCLUDE USING gist` contra duas reservas do mesmo quarto na mesma noite e os `CHECK` de datas e de
hóspedes (`V1__create_tables.sql:36-40`). A falta de `CHECK` sobre `rate_plan` também veio da base. Fica um detalhe: o
`company_code` é gravado mesmo nos planos que não o usam (`ReservationStore.java:50`). Isso não altera nenhum valor de
dinheiro.

## Comportamento

**Números:** 72 de 72 conferências passaram, com 0 falhas: 46 de 46 na parte comum, 3 de 3 depois de reiniciar, 21 de
21 no cenário 01 e 2 de 2 nas reservas simultâneas.
**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- Os três convênios, com os descontos de 93,58, 140,36 e 194,95; a recusa sem código ou com código desconhecido
  (400 `INVALID_REQUEST`); e a devolução integral.
- O pacote: 520,00, 820,00 e 1.200,00, igual em julho; a recusa de sábado a segunda e de sexta a segunda; e a devolução
  de 410,00.
- A estadia longa: 1.749,30, 174,93 e 1.574,37; com noites de temporada, 1.754,30; a recusa com 6 noites; e a devolução
  integral.
- O `companyCode` é ignorado no plano normal, e o não reembolsável continua com 15%.

A parte comum continua igual (preços, recusas, e-mail, relatórios, cancelamentos). Os dados continuam lá depois de
reiniciar, e as duas reservas simultâneas nunca são criadas juntas. Para o dono da pousada, os três planos novos estão
prontos para usar, com os valores que o financeiro conferiu, e nada do que já funcionava quebrou.

## Dúvidas

- Gravar o `companyCode` também nos planos que não são o convênio (`ReservationStore.java:50`) é uma regra de plano
  pequena fora do plano. A referência manda registrá-la aqui, sem mudar a classe. Não aparece na resposta nem muda
  valores.
- O código da empresa é comparado exatamente como chega: "tecnosul", em minúsculas, é recusado
  (`CorporatePlan.java:17`). O texto diz que o código vem em maiúsculas, e por isso não contei como defeito.
