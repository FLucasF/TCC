# Revisão: manutenção, cenário 01

## Visão geral

A base é um serviço Javalin com sete pacotes: `hotel.web` (rotas e JSON), `hotel.reservations` (regras e acesso ao
banco da reserva), `hotel.rooms`, `hotel.messages`, `hotel.documents`, `hotel.reports` e `hotel` (montagem e
Flyway). Uma requisição `POST /reservations` passa por `HttpApi`, que monta um `NewReservation`
(`src/main/java/hotel/web/HttpApi.java:128-130`). Depois vai para `ReservationService.create`, que valida, calcula o
preço e grava pelo `ReservationStore` (JDBC com parâmetros) no PostgreSQL. O esquema vem de migrações do Flyway. O
agente trocou os dois `switch` da base (desconto em `PriceCalculator.calculate`, devolução em
`ReservationService.cancel`) por um `enum RatePlan` com um corpo por constante. Os cinco planos ficaram lá, cada um
com o seu preço, a sua validação e a sua devolução (`src/main/java/hotel/reservations/RatePlan.java:15-100`).
Acrescentou também uma tabela de convênios (`CorporateAgreements.java`), o campo `companyCode` e uma migração nova
(`V2__add_company_code.sql`). Não há defeito sério. Os 33 testes passam e as 72 conferências do roteiro também. Os
pontos menores: uma regra de plano escorregou para o acesso ao banco (`ReservationStore.java:44`), e a assinatura de
`price` carrega o `companyCode` para todos os planos (`RatePlan.java:92`).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Um ciclo, `hotel.documents` ↔
`hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). `hotel.reservations` passou de 13
para 17 classes e de 508 para 639 linhas. A maior classe continua sendo `HttpApi` (171 linhas, 170 na base).
**Nota:** bom

O agente não mexeu na forma das camadas. A borda HTTP só ganhou a passagem de um campo
(`src/main/java/hotel/web/CreateReservationRequest.java:16`, `src/main/java/hotel/web/HttpApi.java:130`). Toda a
regra nova mora em `hotel.reservations`, sem Javalin e sem SQL: `RatePlan.java` só usa `Room`, `RoomType`,
`PriceCalculator` e `HotelException`. O SQL novo ficou no `ReservationStore` (`ReservationStore.java:31-44`,
`:132-133`). O serviço continua pedindo ao plano sem conhecer os planos (`ReservationService.java:42`, `:83`). O
único ciclo veio da base (`ReservationService` usa `ReservationReceipt`, que usa `Reservation`), e o agente não o
piorou. Há um único deslize de responsabilidade: a camada de dados decide pelo plano ao gravar o código da empresa
(`ReservationStore.java:44`, `request.ratePlan() == RatePlan.CORPORATE ? ... : null`). É pequeno e não muda a nota.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (os 2 da base e os 3 novos), 0 fora dela. Não sobrou `switch` nem `if` de
comportamento sobre o plano no cálculo nem no cancelamento. Uma regra de plano isolada ficou fora da estrutura
(`ReservationStore.java:44`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** O `enum RatePlan` virou a estratégia, com dois métodos abstratos: `price`, que calcula o preço e
recusa com `INVALID_REQUEST` quando o plano não pode ser usado, e `refund`, que calcula a devolução
(`RatePlan.java:92`, `:95`). Cada constante tem o seu corpo:

- `STANDARD` (`:17-27`) e `NON_REFUNDABLE` (`:29-39`), os dois que já existiam, também foram para a estrutura.
- `CORPORATE` valida o código e aplica o desconto do convênio (`:41-53`).
- `WEEKEND_PACKAGE` valida sexta + 2 noites, tem a tabela de preço fechado por tipo de quarto e devolve metade
  (`:55-74`).
- `LONG_STAY` exige 7 noites e dá 10% (`:76-89`).

Quem usa a estrutura não pergunta qual é o plano. `PriceCalculator.calculate` só delega
(`PriceCalculator.java:23-25`), e o cancelamento faz `reservation.ratePlan().refund(...)`
(`ReservationService.java:83`). O cálculo das noites com temporada virou um passo reaproveitável,
`nightlyPrice(..., discountRate)` (`PriceCalculator.java:28-37`), sem decidir pelo plano. É o "enum como estratégia"
que a referência aceita como acerto. O que afasta um pouco:

- (a) `ReservationStore.java:44` ainda nomeia `CORPORATE`. É a regra pequena "guardar o código só no convênio", que
  a referência manda contar nas dúvidas sem mudar a classe.
- (b) O contrato `price(calculator, room, checkIn, checkOut, companyCode)` (`RatePlan.java:92`) leva um dado que só o
  convênio usa. Os planos que o dono já anunciou (melhor idade, 60 dias de antecedência) vão precisar de outros dados
  (a idade, a data da reserva), e isso vai obrigar a mudar a assinatura dos cinco. Passar o `NewReservation`
  resolveria.

**O exagero.** Não há. `CorporateAgreements` (`CorporateAgreements.java:8-21`) é só um mapa com uma consulta, no
tamanho certo. Não há interface com uma só implementação, nem fábrica, nem camada de repasse. As sobrecargas de
compatibilidade (`PriceCalculator.java:18-20`, `NewReservation.java:14-17`) existem para não mexer nos testes e nas
chamadas da base. São pequenas.

**O resto.** O dinheiro continua em `BigDecimal`, arredondado por `PriceCalculator.cents` (half-even), e as datas em
`LocalDate`. Os nomes são claros, e os métodos são curtos. Ficam detalhes que um revisor citaria sem bloquear:

- o vaivém `PriceCalculator.calculate` → `RatePlan.price(this, ...)` → `calculator.nightlyPrice`
  (`PriceCalculator.java:24`, `RatePlan.java:20`);
- `company_code` é lido de volta (`ReservationStore.java:133`) para um `NewReservation` cujo código o `Reservation`
  descarta (`Reservation.java:31-41` não guarda o campo). A leitura não serve para nada;
- `WEEKEND_PACKAGE` devolve `null` em `prices.get(room.type())` se surgir um tipo de quarto novo
  (`RatePlan.java:66`). Hoje os três tipos estão cobertos.

## Testes

**Números:** 33 testes rodaram, 0 falhas, 0 erros, 0 pulados, e há 33 métodos escritos. A base tinha 26, todos
mantidos sem alteração (o patch não toca nos testes da base). A cobertura de linhas foi de 93,8% para 94,2%
(503/534), e a de ramos de 87,1% para 87,5% (105/120). Em `hotel.reservations`: 240/253 linhas e 53/56 ramos.
**Nota:** bom

O agente escreveu `src/test/java/hotel/reservations/RatePlansTest.java`, com 7 testes contra o banco real, todos com
valores:

- **Os exemplos do texto.** Tecnosul 779,80/93,58/686,22 (`:37-45`), pacote 820,00 e devolução 410,00 (`:62-68`) e
  estadia longa 1.749,30/174,93/1.574,37 (`:85-91`).
- **Os outros convênios.** Agrovale aparece pela devolução 639,44 (`:55-58`) e Farmavida pelo desconto 187,42
  (`:44`).
- **Os preços fechados e a temporada.** O pacote fechado na suíte em julho e no standard (`:71-74`), e a temporada
  dentro da estadia longa (`:97-101`).
- **As recusas.** Sem código, código desconhecido e código em minúsculas (`:48-51`). Pacote em quinta, de sexta a
  segunda e de uma noite só (`:78-81`). Estadia de 6 noites (`:93`).

Pela pergunta da mutação, trocar 0.12 por 0.21 quebra `:41`. Trocar `< 7` por `<= 7` quebra `:86`, que cria
exatamente 7 noites. Tirar a conferência das 2 noites quebra `:80`, e trocar 0.5 por outro valor quebra `:68`. Ficam
sem rede:

- o caminho HTTP do `companyCode`: nenhum teste em `HttpApiTest` manda o campo nem confere o 400 de uma regra de
  plano. O roteiro do comportamento cobre isso, mas os testes do projeto não;
- a coluna `company_code` gravada só no convênio (`ReservationStore.java:44`): nada confere o que vai para o banco;
- o pacote que começa no sábado: só quinta é testada como dia errado de entrada, mas a regra é a mesma conferência
  `getDayOfWeek() != FRIDAY`.

São lacunas pequenas perto do que foi coberto.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`. Nenhuma chave estrangeira
sem índice e nenhuma data em texto. O `EXCLUDE USING gist` contra noites sobrepostas veio da base. São 2 migrações
do Flyway: `V1__create_tables.sql` igual à da base e `V2__add_company_code.sql` nova. O script acusou `rate_plan`,
`status`, `type` e `channel` em `text` sem `CHECK` (já era assim na base). Os 3 candidatos a SQL montado com `+`
juntam só constantes (`ReservationStore.java:84`, `:99`, `:112`).
**Nota:** bom

O agente evoluiu o esquema do jeito certo: uma migração nova, sem editar a da base
(`src/main/resources/db/migration/V2__add_company_code.sql:2`, `ALTER TABLE reservations ADD COLUMN company_code
text`). Um banco que já roda o sistema sobe sem problema, e as reservas antigas ficam com `null`. O `INSERT` e o
`SELECT` usam parâmetros (`ReservationStore.java:31-44`). A coluna `company_code` não tem `CHECK` ligando-a ao plano
(o código garante isso em `ReservationStore.java:44`) e, como dito no design, é lida mas não chega ao `Reservation`.
Não perde nem corrompe dado. A ausência de `CHECK` em `rate_plan` vem da base, e os três planos novos não precisaram
de mudança ali, porque o valor é o nome do `enum`.

## Comportamento

**Números:** 72 de 72 conferências passaram:

- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- o cenário 01: 21/21;
- as reservas simultâneas: 2/2.
**Nota:** bom

Pela API, a partir de um banco vazio, tudo o que o dono pediu funciona com os valores do financeiro:

- Tecnosul 686,22, Agrovale 639,44 e Farmavida 584,85;
- convênio sem código ou com código desconhecido recusado com 400 `INVALID_REQUEST`;
- pacote a R$ 520/820/1.200 o ano todo, inclusive em julho, recusado fora de sexta a domingo e devolvendo R$ 410,00
  no cancelamento;
- estadia longa com 1.574,37, temporada mantida, recusa abaixo de 7 noites e devolução integral;
- `companyCode` ignorado nos outros planos;
- o `NON_REFUNDABLE` continua com 15%.

O que já funcionava continua igual, inclusive a recusa de duas reservas simultâneas. Uma mudança de ordem, que o
texto não fecha: o preço (e, com ele, a validação do plano) agora é calculado antes de conferir se o quarto está
livre (`ReservationService.java:42-44`). Assim, um pedido que quebra a regra do plano e pega um quarto ocupado recebe
400 em vez de 409. É uma escolha razoável e não pesa.

## Dúvidas

- `ReservationStore.java:44` nomeia `RatePlan.CORPORATE` para decidir se grava o código da empresa. É uma regra de
  plano fora do plano. A referência do Strategy manda contá-la aqui e não mudar a classe por ela, e por isso a classe
  ficou `acerto`. Se cada plano tivesse de decidir o que guarda, o lugar certo seria o próprio `RatePlan`.
- A assinatura de `RatePlan.price` com `companyCode` (`RatePlan.java:92`) funciona para os cinco planos de hoje. A
  evidência não decide se os próximos planos anunciados vão obrigar a mudá-la, mas é provável, porque precisam de
  dados que hoje não chegam ao plano.
