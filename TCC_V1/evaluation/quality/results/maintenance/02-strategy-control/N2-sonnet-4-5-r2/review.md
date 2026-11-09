# Revisão: manutenção, cenário 02

## Visão geral

O projeto é um serviço Javalin em sete pacotes: `hotel.web` (as rotas em `HttpApi`), `hotel.reservations` (as regras em
`ReservationService` e `PriceCalculator`, o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages`,
`hotel.documents`, `hotel.reports` e `hotel` (a montagem em `Hotel.java:21-27` e o pool em `Database`). Uma requisição
entra em `HttpApi`, passa ao `ReservationService`, que calcula o preço no `PriceCalculator` e grava pelo
`ReservationStore` no PostgreSQL, com o esquema criado por uma migração Flyway (`V1__create_tables.sql`). O pedido era
pontual: o acréscimo da alta temporada sobe de 20% para 25%, e a semana da Páscoa de 2027 e de 2028 passa a ser alta
temporada. O agente mudou só três arquivos (`diff -rq` com a base): a constante em
`src/main/java/hotel/reservations/PriceCalculator.java:16`, duas condições a mais em `isHighSeason`
(`PriceCalculator.java:50-63`) e os testes. Não criou nenhuma estrutura nova. Os 28 testes passam, e as 72
conferências do roteiro também. Nada é sério. O único descuido do agente fica fora do código: o README continua dizendo
"a noite custa 20% a mais" e não cita a Páscoa (`README.md:31-32`).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais na base e na final (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já vinha da base. Só o
tamanho mudou: `hotel.reservations` passa de 508 para 525 linhas, e `PriceCalculator` de 53 para 70.
**Nota:** bom

A mudança ficou toda no lugar onde a regra de preço já morava, `PriceCalculator.java:42-65`, numa classe sem Javalin e
sem JDBC. O agente não acrescentou nenhuma dependência, nenhum pacote e nenhuma classe. O ciclo vem da base:
`documents/ReservationReceipt.java:3` importa `Reservation`, e `reservations/ReservationService.java:3` importa
`ReservationReceipt`. O agente não o piorou. A borda HTTP (`web/HttpApi.java`) e a montagem num lugar só
(`Hotel.java:21-27`) também ficaram como estavam.

## Design de baixo nível

**Números:** 1 constante mudada (`PriceCalculator.java:16`) e 2 condições acrescentadas em `isHighSeason`
(`PriceCalculator.java:50-63`). Nenhuma classe, interface ou enum nova; 0 arquivos novos em `src/main`.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O texto tenta o agente com "um acréscimo diferente para cada temporada", mas o agente não caiu nessa. A constante
`HIGH_SEASON_SURCHARGE` passou para `0.25` (`PriceCalculator.java:16`) e continua uma só para todas as temporadas, que
é o que o dono pediu. As duas Páscoas entraram como duas condições a mais no `isHighSeason`
(`PriceCalculator.java:50-56` e `57-63`), e a referência dos controles (`controls.md`, cenário 02) cita essa forma
como contenção. Não há classe por temporada, interface de regra, nem acréscimo parametrizado. O resto da base ficou
intocado.

Há dois detalhes pequenos, que não chegam a pedir mudança:
- As linhas 52 e 59 chamam `date.getMonth()` de novo, embora a variável local `month` (linha 43) já tenha o mesmo
  valor.
- Os anos e os dias da Páscoa estão escritos direto nas condições. Uma lista de períodos ficaria mais fácil de
  aumentar quando o dono mandar os próximos anos. Mesmo assim, as duas formas são aceitas, e o dono disse que os
  próximos anos viriam depois.

O resto do código veio da base e está correto:
- `BigDecimal` com `HALF_EVEN` em `cents` (`PriceCalculator.java:67-69`);
- `LocalDate` para as datas;
- um `switch` exaustivo sobre o enum `RatePlan` (`PriceCalculator.java:26-29`).

## Testes

**Números:**
- Final: 28 testes rodaram, 0 falhas, 0 erros, 0 pulados, 28 métodos escritos. Cobertura de 94,0% das linhas
  (483/514) e 88,6% dos ramos (117/132).
- Base: 26 testes, 93,8% das linhas e 87,1% dos ramos.
- Em `hotel.reservations`, os ramos cobertos passam de 49/52 para 65/68 (`measures/tests.md` e
  `measures/tests-base.md`).

**Nota:** bom

O agente atualizou os valores esperados dos testes da base para o acréscimo novo, com a conta escrita ao lado, sem
afrouxar nenhuma asserção:
- `PriceCalculatorTest.java:36` e `:45` esperam 562,28;
- `PriceCalculatorTest.java:72-74` esperam 2899,52, 434,93 e 2464,59 no não reembolsável sobre a suíte;
- `HttpApiTest.java:77` e `:118` esperam 974,76.

Escreveu também dois testes novos para a Páscoa, que conferem as duas pontas e o dia fora de cada lado:
`easterWeek2027IsHighSeason` (`PriceCalculatorTest.java:78-85`) e `easterWeek2028IsHighSeason` (`:87-94`).

A pergunta da mutação:
- Voltar 0.25 para 0.20 derruba vários testes.
- Trocar `>=` por `>` ou `<=` por `<` nos dias da Páscoa derruba os testes das pontas.
- Apagar a condição `getYear() == 2027` (ou `2028`) não derruba nenhum teste, porque nenhum confere que 21 a 28/03 de
  outro ano continua fora da alta.

Esse é o único buraco, e ele é pequeno. Nenhum teste da base foi apagado ou enfraquecido.

## Banco

**Números:**
- 3 tabelas, todas com chave primária.
- O dinheiro está todo em `numeric(10,2)`.
- Nenhuma chave estrangeira sem índice, nenhuma data guardada como texto.
- Um `EXCLUDE USING gist` impede duas reservas do mesmo quarto na mesma noite.
- A única migração (`V1__create_tables.sql`) está igual à da base.
- Só as colunas de situação e de tipo aparecem sem CHECK: `status`, `rate_plan`, `type` e `channel`
  (`measures/database.md`).

**Nota:** bom

O pedido não mexia no banco, e o agente não mexeu: a migração da base continua igual, e o código JDBC não mudou. O
preço é calculado em Java e gravado já com 2 casas. A falta de CHECK nas colunas de situação e de tipo vem da base. Os
SQL montados com `+` em `ReservationStore.java:33`, `:83`, `:98` e `:111` só juntam constantes (`COLUMNS`), e os
valores vão por `?`.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o acréscimo novo | 5 de 5 |
| a semana da Páscoa | 10 de 10 |
| o resto dos períodos | 6 de 6 |
| as reservas simultâneas | 2 de 2 |

**Nota:** bom

Para o dono da pousada, o resultado é o que ele pediu:
- O standard numa noite de alta sai R$ 312,38, o valor dado no pedido. O deluxe sai R$ 487,38 e a suíte R$ 724,88.
- O não reembolsável aplica os 15% sobre o valor novo: desconto de R$ 46,86 e total de R$ 265,52.
- As noites de 21 a 28/03/2027 e de 09 a 16/04/2028 custam o preço de alta, e as noites vizinhas, de 20/03, 29/03,
  08/04 e 17/04, o preço normal.
- 15/12 a fevereiro e julho continuam alta temporada.
- Todo o resto do sistema continua funcionando, inclusive depois de reiniciar e com duas reservas ao mesmo tempo.

## Dúvidas

O README (`README.md:31-32`) ainda descreve o acréscimo de 20% e não fala da Páscoa. Quem ler a documentação vai achar
a regra antiga. Isso não é código e não entrou em nenhuma nota, mas um revisor exigente pediria a correção.
