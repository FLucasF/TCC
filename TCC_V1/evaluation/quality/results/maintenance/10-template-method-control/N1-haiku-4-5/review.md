# Revisão: manutenção, cenário 10

## Visão geral

O sistema (Javalin + PostgreSQL) tem 7 pacotes: `hotel` (a montagem e o `main`), `hotel.web` (as rotas e os DTOs),
`hotel.reservations` (as regras, o preço e o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages`,
`hotel.documents` (o comprovante) e `hotel.reports` (os dois CSV). Uma requisição entra em `HttpApi` e passa para o
serviço ou o relatório. O relatório de ocupação sai por `web/HttpApi.java:83-86` → `OccupancyReport.generate` →
`ReservationStore.all()`. Os dados ficam em 3 tabelas criadas por uma migração Flyway. O pedido era uma coluna `guest`
logo depois de `reservation`, com a vírgula do nome trocada por espaço. O agente mudou só dois arquivos: 3 linhas em
`src/main/java/hotel/reports/OccupancyReport.java:34,37,41` e as duas asserções do relatório de ocupação em
`ReportsTest`. Não criou nenhuma estrutura nova por causa da frase sobre o telefone. A única falha de verdade é que
nenhum teste confere a troca da vírgula.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles. Há um ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números são iguais aos da base (`measures/dependencies-base.md`); só `hotel.reports` passou de 95 para 97 linhas.
**Nota:** bom

A mudança fica toda em `hotel.reports`, onde o relatório já estava, e não cria nenhuma dependência nova:
`OccupancyReport` continua importando só `hotel.reservations` e `hotel.rooms` (`src/main/java/hotel/reports/OccupancyReport.java:3-6`).
O ciclo veio da base e o agente não mexeu nele: `reservations/ReservationService.java:3` importa
`documents.ReservationReceipt`, e `documents/ReservationReceipt.java:3` importa `reservations.Reservation`. As outras
camadas estão bem separadas. A borda HTTP é fina e só lê os parâmetros e chama o relatório (`web/HttpApi.java:83-86`).
O JDBC fica nos pacotes de dados, e o Javalin só em `hotel` e `hotel.web` (tabela de bibliotecas em
`measures/dependencies.md`).

## Design de baixo nível

**Números:** um arquivo de produção mudou (+3 linhas e 1 alterada). Não há classe, interface nem método novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O texto tenta o agente com "talvez um dia a recepção peça o telefone do hóspede também". O agente fez a mudança
pequena, no lugar onde o relatório já mora. Mudou o cabeçalho (`src/main/java/hotel/reports/OccupancyReport.java:34`),
trocou a vírgula numa variável local (`:37`, `reservation.guest().name().replace(',', ' ')`) e acrescentou a coluna
na ordem pedida (`:41`). Não há esqueleto de relatório, sistema de colunas configuráveis nem campo de telefone, e é
exatamente o que `references/controls.md` chama de contenção.

No resto do código não há o que um revisor exigente pediria para mudar. O dinheiro é `BigDecimal` com centavos
(`reservations/PriceCalculator.java:16-17,50`), as situações são `enum` (`ReservationStatus`, `RatePlan`), e as
recusas passam por `HotelException` com `ErrorCode` e um único mapeamento para HTTP (`web/HttpApi.java:100-107`). Os
métodos do serviço são curtos (`reservations/ReservationService.java:27-119`). A montagem do CSV continua com a mesma
forma que já tinha na base e que o `RevenueReport` também usa (`reports/RevenueReport.java:34-46`). A semelhança entre
os dois relatórios veio da base e não pede abstração.

## Testes

**Números:** 26 testes rodaram, sem falhas, erros ou pulados, e há 26 métodos de teste (os mesmos 26 da base). A cobertura de linhas é 93,8% (473 de 504) e a de ramos 87,1% (101 de 116); `hotel.reports` tem 50 de 50 linhas e 22 de 22 ramos (`measures/tests.md`).
**Nota:** com problemas

O agente manteve os testes da base passando e atualizou as duas asserções do relatório de ocupação para o cabeçalho
novo e o nome na posição certa (`src/test/java/hotel/reports/ReportsTest.java:61-63,73-74`). As asserções comparam o
CSV inteiro, e não só um pedaço. Mesmo assim, a regra nova que tem lógica ficou sem rede. Todas as reservas do teste
usam o mesmo hóspede, "Bruno Lima", que não tem vírgula (`ReportsTest.java:31`). Pela pergunta da mutação:

- se alguém apagasse o `.replace(',', ' ')` de `OccupancyReport.java:37`, nenhum teste falharia;
- se o relatório pusesse o nome do hóspede de outra reserva, nenhum teste falharia, porque os nomes são todos iguais.

O teste pela API só confere o total (`src/test/java/hotel/web/HttpApiTest.java:113-116`). Falta um teste com um nome
com vírgula, e de preferência com hóspedes diferentes.

## Banco

**Números:** 3 tabelas, criadas por 1 migração Flyway (`V1__create_tables.sql`, igual à da base). Nenhuma tabela sem chave primária, dinheiro todo em `numeric(10,2)`, nenhuma chave estrangeira sem índice e nenhuma data em texto. Há um `EXCLUDE USING gist` que impede duas reservas no mesmo quarto e nas mesmas noites. As colunas `status`, `rate_plan`, `type` e `channel` não têm CHECK (`measures/database.md`).
**Nota:** bom

O pedido não precisava de coluna nova no banco, porque o nome do hóspede já é gravado, e o agente não mexeu no
esquema nem nas migrações. A falta de CHECK nas colunas de situação e de tipo veio da base. Também veio da base o
relatório que traz a tabela inteira para filtrar em Java (`src/main/java/hotel/reports/OccupancyReport.java:25`,
`store.all()`). O SQL usa parâmetros: os `+` em `reservations/ReservationStore.java:33,83,98,111` só juntam
constantes.

## Comportamento

**Números:** 55 de 55 conferências passaram: a parte comum 46 de 46, depois de reiniciar 3 de 3, o cenário 10 4 de 4 e as reservas simultâneas 2 de 2 (`measures/behavior.md`).
**Nota:** bom

O relatório de ocupação sai com a coluna `guest` logo depois de `reservation`, a vírgula do nome vira espaço, o total
continua igual e o relatório financeiro não mudou. Tudo o que já funcionava continua funcionando, inclusive a recusa
de duas reservas ao mesmo tempo para o mesmo quarto. Para o dono da pousada, a recepção já vê o nome do hóspede
sem perder nada do que tinha.

## Dúvidas

O nome só tem a vírgula trocada. Um nome com aspas ou com quebra de linha ainda bagunçaria o CSV. O pedido falou só da
vírgula, e o cadastro pela API só exige que o nome não esteja em branco (`src/main/java/hotel/web/HttpApi.java:118`).
Por isso não contei como defeito, mas a evidência não decide se o dono esperaria essa proteção.
