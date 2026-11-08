# Revisão: manutenção, cenário 09

## Visão geral

O projeto é o da base com o mesmo desenho: `hotel.web.HttpApi` recebe a requisição, lê o parâmetro (`readMonth`,
`src/main/java/hotel/web/HttpApi.java:166`) e chama uma classe de relatório em `hotel.reports`, que lê as reservas do
`ReservationStore` (JDBC sobre PostgreSQL, esquema pela migração Flyway `V1__create_tables.sql`) e monta o CSV. O
agente acrescentou dois relatórios novos (`TourismTaxReport`, `RatePlanReport`), duas rotas
(`HttpApi.java:99-106`), a ligação em `Hotel.java` e a linha de título copiada nos quatro relatórios. O mais sério é
de comportamento e de design ao mesmo tempo: em vez de um esqueleto comum, cada relatório foi escrito inteiro, e a
taxa de turismo copiou o filtro do financeiro (`amountKept() > 0`, `TourismTaxReport.java:24-26`), por isso cobra a
taxa de uma reserva NON_REFUNDABLE **cancelada**, quando o pedido diz "as confirmadas, as que já fizeram check-in e as
que já fizeram check-out". O roteiro mostrou um total de 72.00 onde o certo é 45.00.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais na base e na final (`measures/dependencies.md` e
`measures/dependencies-base.md`); 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base.
`hotel.reports` passou de 2 classes/95 linhas para 4 classes/207 linhas; nenhuma dependência nova.
**Nota:** bom

As rotas novas seguem o padrão das que já existiam: só leem o mês, chamam o relatório e escrevem o CSV
(`src/main/java/hotel/web/HttpApi.java:99-106`). Os relatórios não conhecem o Javalin nem o JDBC: usam só
`ReservationStore.all()` (`TourismTaxReport.java:23`, `RatePlanReport.java:21`), e a tabela de bibliotecas mostra
`hotel.reports` sem Javalin e sem JDBC. A montagem continua num lugar só, `src/main/java/hotel/Hotel.java:28-35`, que
cria os relatórios e os passa ao `HttpApi`. O ciclo `documents`/`reservations` vem da base e o agente não o piorou.
O único sinal de crescimento é o construtor do `HttpApi`, que ganha um parâmetro por relatório
(`HttpApi.java:37-45`, agora 7), consequência direta da falta de uma abstração comum de relatório (que conta no
design, não aqui).

## Design de baixo nível

**Números:** 4 relatórios, 0 dentro de uma estrutura comum; a linha de título está escrita à mão 4 vezes
(`OccupancyReport.java:34`, `RevenueReport.java:35`, `TourismTaxReport.java:33`, `RatePlanReport.java:45`); o filtro
"pagas que começam no mês" está copiado 3 vezes (`RevenueReport.java:24-31`, `TourismTaxReport.java:22-29`,
`RatePlanReport.java:20-27`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O pedido descreve quatro relatórios com os mesmos passos (escolher, ordenar, título, cabeçalho, linhas, total), avisa
que vêm mais dois e que "quando o jeito dos relatórios mudar, quero que mude em todos de uma vez" (a linha de data no
ano que vem). Isso pede o Template Method (ou um escritor genérico que receba as partes que variam). O agente não fez
nem um nem outro: as quatro classes continuam `final`, independentes, cada uma com o seu `StringBuilder`, o seu
título, o seu cabeçalho e o seu total (`RevenueReport.java:34-48`, `OccupancyReport.java:83-95`,
`TourismTaxReport.java:32-52`, `RatePlanReport.java:44-54`). A mudança da linha de data, quando vier, vai precisar ser
copiada em cada relatório — é exatamente o caso que a referência chama de omissão ("os quatro relatórios ficam
escritos inteiros, cada um com a sua linha de título").

Exagero: nenhum; não há interface, fábrica nem camada sem necessidade.

O resto do código novo tem defeitos localizados:
- **Duplicação que já produziu um erro:** o filtro do financeiro foi copiado para a taxa de turismo
  (`TourismTaxReport.java:22-29`), mas a regra da taxa é outra (situação paga, não "ficou com dinheiro"); o resultado
  é a reserva cancelada sem devolução entrando na taxa (ver Comportamento).
- **O nome da pousada repetido como literal** nos quatro relatórios, quando a base já tinha a constante
  `ReservationService.HOTEL_NAME` (`src/main/java/hotel/reservations/ReservationService.java:12`).
- **O plano de tarifa à mão:** `RatePlanReport.java:29-52` tem duas variáveis e um `if/else if` para cada valor do
  `enum` e escreve os nomes como texto (`"STANDARD,"`, `"NON_REFUNDABLE,"`, linhas 47-48). Um plano novo no
  `RatePlan` seria ignorado em silêncio (o `else if` não tem `else`); um laço sobre `RatePlan.values()` com um
  `EnumMap` resolveria.
- **Noites recalculadas** com `toEpochDay` (`TourismTaxReport.java:37`), quando a reserva já guarda
  `price().nights()` (`src/main/java/hotel/reservations/Price.java:5`, usado em `ReservationReceipt.java:34`).
- Detalhes: `java.math.RoundingMode` com o nome completo no meio do código (`TourismTaxReport.java:41`).

Os tipos estão certos: `BigDecimal` com 2 casas para a taxa e os valores, `YearMonth` para o mês, o valor da taxa
numa constante (`TourismTaxReport.java:13`).

## Testes

**Números:** final: 29 testes rodaram, 0 falhas, 0 erros, 0 pulados; 29 métodos escritos (base: 26, todos passando).
Cobertura de linhas 94.1% (544 de 578) e de ramos 87.1% (122 de 140); base 93.8% e 87.1%. `hotel.reports`: 111 de
114 linhas, 43 de 46 ramos (`measures/tests.md`).
**Nota:** com problemas

O agente manteve os 26 testes da base, atualizou os cinco de relatório com a linha de título
(`src/test/java/hotel/reports/ReportsTest.java:61,74,87,104,116`) sem enfraquecê-los, e acrescentou três testes com o
CSV inteiro: a taxa de turismo (`ReportsTest.java:122-133`), os planos (`ReportsTest.java:135-146`) e os planos num
mês vazio (`ReportsTest.java:148-159`); o teste da API confere status, `Content-Type`, título e total das duas rotas
novas (`src/test/java/hotel/web/HttpApiTest.java:123-134`).

O que fica sem rede, pela pergunta da mutação:
- **A regra de quais reservas pagam a taxa:** o cenário de teste (`ReportsTest.java:42-54`) não tem nenhuma reserva
  NON_REFUNDABLE cancelada que comece no mês (a NON_REFUNDABLE começa em março e a cancelada é STANDARD, com
  devolução total). Por isso trocar o filtro por `isPaid()` (o certo) ou deixá-lo em `amountKept() > 0` (o errado)
  passa nos mesmos testes — e o erro que o roteiro achou passou despercebido.
- **O dinheiro do NON_REFUNDABLE no relatório de planos:** em todos os testes essa linha é `0,0.00`
  (`ReportsTest.java:143,156`) e o teste da API só tem reserva STANDARD (`HttpApiTest.java:133-134`); apagar
  `RatePlanReport.java:39-40` não faria nenhum teste falhar.
- O fator "por hóspede" só é conferido pela API (2 hóspedes, 2 noites, 18.00, `HttpApiTest.java:24,127`); os testes
  de unidade usam sempre 1 hóspede (`ReportsTest.java:33`).

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice;
nenhuma data em texto; `EXCLUDE USING gist` contra duas reservas do mesmo quarto; colunas de situação/plano em `text`
sem `CHECK`. Uma migração Flyway, `V1__create_tables.sql`, igual à da base (`measures/database.md`).
**Nota:** bom

O agente não mexeu no banco: a migração da base ficou intacta e nenhuma foi acrescentada, o que é o certo, porque os
relatórios novos só leem colunas que já existiam (`guests`, `rate_plan`, `check_in`, `check_out`). O SQL continua com
parâmetros em `ReservationStore` (os `+` listados pelo script só concatenam a constante `COLUMNS`). As colunas
`status` e `rate_plan` sem `CHECK` vêm da base. Os relatórios novos repetem o padrão da base de trazer a tabela
inteira (`store.all()`, `TourismTaxReport.java:23`, `RatePlanReport.java:21`) e filtrar o mês em Java; com o volume de
uma pousada isso não pesa, e o padrão veio da base.

## Comportamento

**Números:** 63 de 64 conferências passaram: a parte comum 46 de 46; depois de reiniciar 3 de 3; o cenário 09 12 de
13; duas reservas ao mesmo tempo 2 de 2 (`measures/behavior.md`).
**Nota:** com problemas

Passam: a linha de título nos quatro relatórios, a ocupação e o financeiro iguais aos da base depois do título, o
relatório de planos inteiro (STANDARD e depois NON_REFUNDABLE, o plano sem reserva com 0, a NON_REFUNDABLE cancelada
contando como no financeiro), a taxa de um mês vazio e o 400 para mês fora do formato.

Falhou a taxa de turismo do mês com reservas: o esperado era `1,101,...,2,3,27.00`, `3,301,...,4,1,18.00` e
`total,45.00`; veio também a reserva 2 (`2,201,2027-07-05,2027-07-07,3,2,27.00`) e o total `72.00`. A reserva 2 é a
NON_REFUNDABLE cancelada: o pedido diz que a taxa conta só "as confirmadas, as que já fizeram check-in e as que já
fizeram check-out", e o código usa o critério do financeiro ("ficou dinheiro com a pousada",
`TourismTaxReport.java:24-26`). Para o dono da pousada, isso quer dizer declarar e pagar à prefeitura a taxa de
hóspedes que nunca vieram: neste exemplo, R$ 27,00 a mais num mês; o erro aparece toda vez que alguém cancela uma
reserva não reembolsável. O resto (a parte comum, os dados depois de reiniciar, as reservas simultâneas) continua
funcionando como na base.

## Dúvidas

Na taxa de turismo, alguém poderia ler "reservas pagas" como "as que pagaram" e incluir a NON_REFUNDABLE cancelada
(que pagou e não recebeu nada de volta); mas o próprio pedido explica entre parênteses quais são (confirmadas, com
check-in e com check-out), e uma reserva cancelada não tem hóspede para a taxa por hóspede por noite. Por isso contei
como erro, e não como escolha num ponto em aberto.
