# Revisão: manutenção, cenário 01

## Visão geral

O agente mudou um projeto que já existia. Os pacotes ficaram os mesmos sete da base: `hotel` (montagem e Flyway), `hotel.web` (Javalin), `hotel.reservations` (regras e `ReservationStore` com o JDBC), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`. Uma reserva entra por `HttpApi` (`src/main/java/hotel/web/HttpApi.java:64`), passa por `ReservationService.create` (`src/main/java/hotel/reservations/ReservationService.java:29`), que pede o preço a `PriceCalculator` (`PriceCalculator.java:26-28`). O `PriceCalculator` escolhe o plano em `RatePlans` e o plano valida e calcula. Os dados vão para o PostgreSQL pelo `ReservationStore`. A parte de negócio está bem feita: os cinco planos são estratégias, os testes conferem os valores e o roteiro de comportamento passa inteiro (72 de 72). O problema mais sério está no banco. O agente reescreveu a migração `V1__create_tables.sql`, que a base já tinha, e com isso o dinheiro passou a ser `double precision`, as datas viraram `text`, a chave estrangeira, os `NOT NULL`, os `CHECK` e a restrição `EXCLUDE` contra reserva dupla sumiram, e `messages` ficou sem chave primária. Testei subir a versão nova num banco onde a base já tinha rodado, e o serviço não sobe: o Flyway acusa `Migration checksum mismatch for migration version 1`. Ou seja, a pousada não consegue instalar a versão nova sobre o banco que usa hoje.

## Arquitetura

**Números:** 7 pacotes e 17 dependências, nos dois lados. Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). O pacote `hotel.reservations` passou de 13 classes e 508 linhas para 20 classes e 731 linhas. A maior classe continua sendo `HttpApi` (170 linhas, igual à base).
**Nota:** bom

O agente manteve a separação de camadas da base. O handler continua fino: a mudança em `hotel.web` é só repassar o `companyCode` (`src/main/java/hotel/web/HttpApi.java:127-128`). As regras novas ficam em `hotel.reservations`, sem Javalin e sem JDBC (por exemplo `CorporatePlan.java:16-31` e `WeekendPackagePlan.java:19-37`). O acesso ao banco continua isolado em `ReservationStore`. A montagem continua num lugar só: `RatePlans` é criada uma vez em `src/main/java/hotel/Hotel.java:25` e passada ao `PriceCalculator` e ao `ReservationService`. O agente não criou dependência nova nem ciclo novo. O único ciclo veio da base.

## Design de baixo nível

**Números:** 5 de 5 planos ficaram na estrutura, cada um numa classe que implementa `RatePlanPolicy` com `validate`, `price` e `refund`. Fora dela ficaram 0 casos. Nenhum `switch` sobre o plano sobrou no código: o único `switch` de `src/main` é o de `HttpApi.java:101`, que trata códigos de erro. Os dois `switch` da base saíram de `PriceCalculator` e de `ReservationService.cancel`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

É um Strategy completo. A interface `RatePlanPolicy` (`src/main/java/hotel/reservations/RatePlanPolicy.java:7-15`) reúne as três coisas que variam por plano. Cada plano, inclusive os dois que já existiam (`StandardPlan.java:6-21`, `NonRefundablePlan.java:6-23`), tem a sua classe. Um único ponto escolhe a implementação, o `EnumMap` de `RatePlans.java:12-16`. O cálculo e o cancelamento pedem ao plano sem saber qual ele é: `PriceCalculator.java:26-28` e `ReservationService.java:84`. As regras de quem pode usar cada plano (o código da empresa, sexta a domingo, 7 noites) estão dentro do próprio plano (`CorporatePlan.java:16-21`, `WeekendPackagePlan.java:19-26`, `LongStayPlan.java:14-18`), e não no serviço. A única coisa de plano que ficou fora da estrutura é pequena: o `companyCode` é gravado em qualquer plano (`ReservationStore.java:50`), e não só no convênio. O preço não é afetado.

Não há exagero. `PercentOff` (`PercentOff.java:10-13`) tira a duplicação do desconto percentual, que se repete em quatro planos. `NightlyCharges` é um record de dois campos que leva as diárias ao plano. As duas peças são usadas por várias classes.

Fora do pattern, a nota cai por causa do código que o agente mexeu no acesso ao banco, que acompanha o esquema degradado:
- as datas viajam como texto (`ReservationStore.java:40`, `:82`, `:130-131`, com `setString` e `LocalDate.parse`), quando antes eram `Date`;
- `money()` (`ReservationStore.java:120-123`) e `Rooms.java:54` acertam a escala de um valor que agora sai de um `double`. É um remendo para um tipo errado;
- sobrou código morto: a constante `EXCLUSION_VIOLATION` (`ReservationStore.java:21`) não é mais usada desde que o tratamento do erro 23P01 saiu de `add`, e o `import java.sql.Date` (`ReservationStore.java:5`) também não.

Há ainda um ponto pequeno: `ReservationService` recebe `PriceCalculator` e `RatePlans` (`ReservationService.java:20-25`), ou seja, dois caminhos até o plano. O resto do código novo é curto e tem nomes claros.

## Testes

**Números:** final: 34 testes rodaram, 0 falhas, 0 erros, 0 pulados; 34 métodos escritos. Cobertura de 94,9% das linhas (524 de 552) e 89,5% dos ramos (111 de 124). Base: 26 testes, 93,8% das linhas e 87,1% dos ramos. Em `hotel.reservations`, a final cobre 261 de 271 linhas e 59 de 60 ramos.
**Nota:** bom

O agente escreveu 8 testes novos e manteve os 26 da base passando. Nos testes da base, ele só ajustou as chamadas à assinatura nova (`PriceCalculatorTest.java:19-22`, `ReservationServiceTest.java:34-37`, `ReportsTest.java:32-36`) e não enfraqueceu nenhuma asserção. `NewRatePlansTest` confere com valores as contas do texto e outras:
- os três descontos do convênio (`NewRatePlansTest.java:44-56`);
- a recusa sem código e com código desconhecido, e a devolução integral (`:59-67`);
- os três preços do pacote, o pacote em julho e a devolução de R$ 410,00 (`:70-83`);
- as datas recusadas do pacote: sábado a segunda, 3 noites e 1 noite (`:86-93`);
- a estadia longa com e sem temporada e a devolução (`:96-107`);
- a recusa com 6 noites (`:110-113`);
- o `companyCode` ignorado fora do convênio, lido de volta do banco (`:116-121`).

`HttpApiTest.java:108-120` confere o campo `companyCode` pela API e o 400 `INVALID_REQUEST`.

Fiz a pergunta da mutação. Trocar 0.12 por 0.21, `< 7` por `<= 7`, ou 0.5 por 1 faria algum teste falhar. Nenhum teste, nem da base nem novo, confere o esquema, nem a reserva dupla entre processos. Por isso a degradação do banco passou sem que nenhum teste acusasse, mas essa lacuna já vinha da base.

## Banco

**Números:** 3 tabelas, 2 migrações. A `V1__create_tables.sql` foi editada (é uma migração da base) e a `V2__reservation_company_code.sql` é nova. As conferências acusaram:
- 6 colunas de dinheiro em `double precision`;
- `check_in` e `check_out` em `text`;
- `messages` sem chave primária;
- 0 chaves estrangeiras;
- 0 `CHECK`, `UNIQUE` ou `EXCLUDE`;
- 1 `NOT NULL` por tabela (só a chave).

**Nota:** ruim

A V2 estava certa: a coluna nova veio numa migração nova (`src/main/resources/db/migration/V2__reservation_company_code.sql:2`). O que pesa é que o agente também reescreveu a V1, e só piorou o esquema. Compare `src/main/resources/db/migration/V1__create_tables.sql:1-40` com a base: lá havia `numeric(10, 2) NOT NULL` (`base/.../V1__create_tables.sql:6`, `:30-35`), `date NOT NULL` (`:25-26`), `REFERENCES rooms (number)` (`:21`) e a restrição `EXCLUDE USING gist` contra reserva dupla (`:39`). Na final:
- dinheiro em `double precision` (`V1__create_tables.sql:5`, `:26-31`);
- datas em `text` (`:21-22`);
- nenhuma restrição;
- `messages.id bigserial` sem `PRIMARY KEY` (`:35`).

São três defeitos que a skill classifica como `ruim`: uma migração da base editada, dinheiro em ponto flutuante e uma tabela sem chave. Rodei o teste do primeiro: subi o jar da base num banco vazio e depois o da final no mesmo banco. A final não subiu: `FlywayValidateException: Migration checksum mismatch for migration version 1`. Também deixou de existir a proteção contra reserva dupla no banco. O agente apagou a `EXCLUDE` e o tratamento do 23P01 (`ReservationStore.java:55-57`, onde agora qualquer `SQLException` vira `IllegalStateException`). O que impede a duplicação passou a ser só o `synchronized` de `ReservationService.java:29`, que vale para um processo só. Isso vale tanto para o banco que a pousada já usa quanto para um banco novo.

O SQL continua com parâmetros `?`. Os candidatos que o script listou só concatenam constantes (`ReservationStore.java:33`, `:80`, `:95`, `:108`). As conexões usam try-with-resources e um pool.

## Comportamento

**Números:** 72 de 72 conferências passaram:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 01: 21 de 21;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Partindo de um banco vazio, o sistema faz tudo o que o dono pediu:
- R$ 686,22 no convênio Tecnosul, R$ 140,36 e R$ 194,95 de desconto na Agrovale e na Farmavida;
- recusa 400 `INVALID_REQUEST` sem código da empresa ou com código desconhecido;
- pacote de R$ 520,00, R$ 820,00 e R$ 1.200,00, também em julho, recusado fora de sexta a domingo e com devolução de R$ 410,00;
- estadia longa de R$ 1.574,37, com a temporada valendo, e recusa com 6 noites;
- o código ignorado nos outros planos;
- o normal e o não reembolsável funcionando como antes.

As reservas simultâneas passaram graças ao `synchronized` num processo só. Fora do roteiro, como contei na coluna do banco, a versão nova não sobe sobre o banco que a pousada já tem (checksum da V1). Para o dono, isso quer dizer que a atualização não roda em produção sem apagar ou consertar o banco à mão. Esse defeito é do banco e pesa naquela coluna, não nesta.

## Dúvidas

O texto não diz se o `companyCode` deve ser gravado nos planos que não são o convênio. O agente grava sempre (`ReservationStore.java:50`) e ignora o valor no preço. Não muda a classe do pattern. A evidência também não mostra por que o agente reescreveu a V1. O efeito está medido (checksum que não bate, tipos e restrições perdidos), mas o motivo não aparece no código.
