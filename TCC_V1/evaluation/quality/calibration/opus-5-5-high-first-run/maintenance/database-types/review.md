# Revisão: manutenção, cenário 01

## Visão geral

A base é um serviço Javalin em sete pacotes (`hotel`, `web`, `reservations`, `rooms`, `messages`, `documents`,
`reports`). Uma requisição entra por `web/HttpApi`, que monta um `NewReservation` (`final/src/main/java/hotel/web/HttpApi.java:124-129`)
e chama `ReservationService`. O serviço calcula o preço em `PriceCalculator` e grava por JDBC em `ReservationStore`. O
esquema vem de migrações Flyway (`Database.java:28`). O agente resolveu bem os planos de tarifa: criou a interface
`RatePlanPolicy`, uma classe para cada um dos cinco planos e um mapa em `RatePlans`. Todo o roteiro de comportamento
passa (72 de 72). O que há de mais sério está no banco: o agente **reescreveu a migração `V1__create_tables.sql` que a
base já tinha**. Tirou todas as restrições (as chaves estrangeiras, os `NOT NULL`, os `CHECK`), tirou a restrição
`EXCLUDE` que impedia duas reservas do mesmo quarto na mesma noite e a chave primária de `messages`. Também trocou o
dinheiro de `numeric(10,2)` para `double precision` e as datas de `date` para `text`. Em seguida adaptou o Java a esse
esquema pior. Testei a atualização: subi o jar da base num banco, depois o jar final no mesmo banco. **O serviço final
não sobe** (`FlywayValidateException: Migration checksum mismatch for migration version 1`). Para o dono da pousada,
isso quer dizer que instalar a nova versão sobre o banco de produção derruba o sistema.

## Arquitetura

**Números:** 7 pacotes e 17 dependências, os mesmos da base. Há 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que
já existia na base (`measures/dependencies-base.md`). `hotel.reservations` passou de 13 para 20 classes (508 → 731
linhas). Nenhuma classe nova é grande: a maior continua `HttpApi`, com 170 linhas.
**Nota:** bom

O agente não mexeu na forma do sistema. A borda HTTP só ganhou o repasse do `companyCode`
(`web/CreateReservationRequest.java:16`, `web/HttpApi.java:128-129`). A regra dos planos ficou no pacote de regras:
nenhum plano conhece Javalin nem JDBC (`reservations/CorporatePlan.java`, `WeekendPackagePlan.java` etc.). O SQL
continua só em `ReservationStore`. A montagem continua num lugar só, `Hotel.java:25-27`, onde `RatePlans` é criado e
injetado. O ciclo `documents` ↔ `reservations` vem da base, e o agente não o piorou.

## Design de baixo nível

**Números:** os 5 planos estão na estrutura (5 implementações de `RatePlanPolicy`, todas registradas em
`RatePlans.java:13-17`). Nenhum plano ficou fora dela. Os dois `switch` de comportamento da base (o desconto em
`PriceCalculator` e a devolução em `ReservationService.cancel`) foram removidos, e não sobra nenhum `switch` ou `if`
sobre o plano em `src/main`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `RatePlanPolicy` (`reservations/RatePlanPolicy.java:7-15`) junta as três coisas que o texto diz que
cada plano traz: quem pode usar (`validate`), o preço (`price`) e a devolução (`refund`). Cada plano mora numa classe
só dele, inclusive os dois que já existiam (`StandardPlan.java`, `NonRefundablePlan.java`). Os novos ficam em
`CorporatePlan.java:14-31`, `WeekendPackagePlan.java:18-37` e `LongStayPlan.java:14-28`. O cálculo pede ao plano sem
nomeá-lo (`PriceCalculator.java:25-29`), e o cancelamento também (`ReservationService.java:84`). A escolha da estratégia
é um único `EnumMap` (`RatePlans.java:10-21`). Um plano novo é uma constante no `enum`, uma classe e uma linha no mapa.
A parte comum, as diárias com a temporada, ficou fora dos planos: `NightlyCharges` (`PriceCalculator.java:31-39`) é
entregue a cada plano. O pacote ignora esse valor e usa o preço fechado, e a estadia longa aplica o desconto sobre ele.
Isso é a divisão certa.

**O exagero.** Não há. `PercentOff` (`PercentOff.java:5-13`) tira a duplicação do desconto percentual de quatro
planos, e `NightlyCharges` é um record de dois campos que o pattern usa. Não há fábrica, builder nem camada de repasse.

**O resto do código.** O que pesa na nota é a adaptação ao esquema degradado, toda do agente:
- as datas vão ao banco e voltam como texto (`ReservationStore.java:40-41`, `:82-83`, `:130-131`), quando a base usava
  `Date.valueOf` e `getDate`;
- o dinheiro lido de `double precision` é remendado com `setScale` em dois lugares (`ReservationStore.java:120-123` e
  `rooms/Rooms.java:54`), com `java.math.RoundingMode` escrito por extenso no meio do código. Isso duplica
  `PriceCalculator.cents`;
- o tratamento do `23P01` foi removido, mas a constante `EXCLUSION_VIOLATION` ficou como código morto
  (`ReservationStore.java:21`);
- o `companyCode` é gravado em qualquer plano (`ReservationStore.java:50`), embora o texto mande ignorá-lo fora do
  convênio. É um detalhe pequeno.

Também há duas portas para o mesmo registro: `ReservationService` recebe `PriceCalculator` e também `RatePlans`
(`ReservationService.java:20-21`). Isso é tolerável.

## Testes

**Números:** final com 30 testes rodados, 0 falhas, 0 erros e 0 pulados (30 métodos). Linhas 522/552 (94,6%), ramos
110/124 (88,7%), `hotel.reservations` com 259/271 linhas e 58/60 ramos. Base com 26 testes, 93,8% de linhas e 87,1%
de ramos. O agente acrescentou 4 testes (`NewRatePlansTest`) e adaptou os 26 da base só na assinatura, sem
enfraquecer nenhum.
**Nota:** com problemas

Os testes novos conferem com valores os três exemplos do texto: Tecnosul 93,58 e 686,22 (`NewRatePlansTest.java:35-40`),
o pacote 820,00 com devolução de 410,00 (`:49-54`) e a estadia longa 174,93 e 1.574,37 (`:59-63`). Pela pergunta da
mutação, várias regras novas ficam sem rede:
- trocar 0.18 da Agrovale ou 0.25 da Farmavida por outro número não quebra nenhum teste (só a Tecnosul é testada);
- os preços do pacote no standard (520,00) e na suíte (1.200,00) não são testados, nem o pacote em alta temporada;
- a devolução integral do convênio e da estadia longa (`CorporatePlan.java:30`, `LongStayPlan.java:27`) não
  é testada;
- trocar `< MINIMUM_NIGHTS` por `<=` em `LongStayPlan.java:16` derruba o teste de 7 noites, mas não há teste com
  alta temporada na estadia longa;
- a recusa do pacote só testa a entrada no sábado (`:55`), não sexta a segunda;
- as recusas usam `assertThrows(HotelException.class, …)` (`:44-45`, `:55`, `:64`) sem conferir o código
  `INVALID_REQUEST`, e nenhum teste passa pela API com o campo `companyCode`, nem confere o 400.

Nenhum teste percebeu que o esquema perdeu a restrição contra reserva dupla, porque o da base
(`ReservationServiceTest.java:66`) passa pela checagem em Java.

## Banco

**Números:** 3 tabelas e 2 migrações Flyway. `V1__create_tables.sql` foi **EDITADA** e `V2__reservation_company_code.sql`
é nova. As conferências acusaram:
- `messages` sem chave primária;
- 6 colunas de dinheiro em `double precision` (`subtotal`, `discount`, `total`, `amount_paid`, `refund`, `nightly_rate`);
- `check_in` e `check_out` em `text`;
- nenhuma chave estrangeira;
- 1 `NOT NULL` por tabela (só a chave);
- nenhuma restrição `CHECK`, `UNIQUE` ou `EXCLUDE`.

Na base, todas essas conferências estavam em ordem.
**Nota:** ruim

A coluna nova do convênio foi feita do jeito certo, numa migração nova (`V2__reservation_company_code.sql:2`). Mas o
agente também reescreveu a V1 que a base já tinha (`changes.patch`, linhas 577-653, e `V1__create_tables.sql:1-40`
na final):
- **migração aplicada editada:** subi o jar da base num banco vazio e depois o jar final sobre o mesmo banco. O Flyway
  recusou com `Migration checksum mismatch for migration version 1`, e o serviço terminou com código 1. Quem já roda
  o sistema não consegue atualizar;
- **dinheiro em ponto flutuante:** `V1__create_tables.sql:5` e `:26-31`. Antes era `numeric(10, 2)`;
- **datas como texto:** `V1__create_tables.sql:21-22`. A comparação de sobreposição (`ReservationStore.java:79-83`) só
  funciona porque o ISO-8601 de quatro dígitos ordena como texto;
- **sem integridade:** sumiram `REFERENCES rooms (number)`, os `NOT NULL`, `CHECK (check_out > check_in)`,
  `CHECK (guests >= 1)` e a chave primária de `messages` (`V1__create_tables.sql:15-40`);
- **reserva dupla protegida só no processo:** sumiram a restrição `reservations_no_double_booking`
  (`EXCLUDE USING gist`) e a extensão `btree_gist`. O tratamento do `23P01` também saiu de `ReservationStore.add`.
  Sobra só o `synchronized` de `ReservationService.java:29`, que vale para uma única instância do serviço.

O SQL continua com parâmetros (os `+` que o script listou só juntam constantes), com try-with-resources e pool.

## Comportamento

**Números:** 72 de 72 conferências passaram:
- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- o cenário 01: 21/21;
- as reservas simultâneas: 2/2.
**Nota:** bom

Num banco vazio, o sistema faz tudo o que o texto pede:
- os três convênios: 686,22, 639,44 e 584,85;
- as recusas do convênio sem código ou com código desconhecido, com 400 `INVALID_REQUEST`;
- o pacote a 520,00, 820,00 e 1.200,00, inclusive em julho, com devolução da metade e recusa fora de sexta a domingo;
- a estadia longa com a temporada (1.754,30) e a recusa com 6 noites;
- o `companyCode` ignorado no plano normal;
- o normal e o não reembolsável como antes.

As reservas simultâneas passam por causa do `synchronized` em `ReservationService.create`, que já estava na base. Fora
do roteiro, porém, a nova versão não sobe sobre um banco criado pela versão anterior (ver "Banco"). Para o dono da
pousada, isso significa que a atualização não pode ser instalada como está sem perder o banco atual ou consertá-lo à
mão.

## Dúvidas

- A falha ao atualizar um banco existente está avaliada em "Banco", como a skill dessa coluna pede. Em "Comportamento",
  a nota segue o roteiro, que roda num banco vazio e passa inteiro. Se a atualização fosse contada aqui, a nota seria
  `ruim` (o serviço não sobe sobre o banco de produção).
- Não está claro por que o agente rebaixou o esquema. Os ajustes de leitura (`setScale`, datas em texto) mostram que
  foi uma mudança deliberada, mas o pedido não justificava nada disso.
