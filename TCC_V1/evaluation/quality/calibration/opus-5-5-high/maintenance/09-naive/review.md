# Revisão: manutenção, cenário 09

## Visão geral

O projeto é o da base com sete pacotes: `hotel` (montagem, `Hotel.java`, e o `DataSource`), `hotel.web` (as rotas
Javalin em `HttpApi`), `hotel.reservations` (regras em `ReservationService`/`PriceCalculator`, acesso a dados em
`ReservationStore`), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`. Uma requisição de relatório
entra em `HttpApi` (`src/main/java/hotel/web/HttpApi.java:91-106`), lê o período, chama o `generate` do relatório, que
busca todas as reservas por `ReservationStore.all()` (`src/main/java/hotel/reservations/ReservationStore.java:108-111`)
e monta o CSV em Java; os dados ficam no PostgreSQL, com o esquema criado pelo Flyway. O agente acrescentou dois
relatórios novos (`TourismTaxReport`, `RatePlanReport`), duas rotas, a linha de título nos quatro relatórios e dois
testes. Tudo funciona (64 de 64 conferências do comportamento, 28 de 28 testes), mas o ponto central do pedido ficou
de fora: o dono avisou que viriam mais relatórios e que "quando o jeito dos relatórios mudar, quero que mude em todos
de uma vez", e o agente escreveu os quatro relatórios inteiros, cada um com a sua cópia da linha de título e da
sequência de passos. É uma omissão do Template Method.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais na
base e na final (`measures/dependencies.md`, `measures/dependencies-base.md`); `hotel.reports` passou de 2 classes e
95 linhas para 4 classes e 197 linhas; `HttpApi` de 170 para 186 linhas.
**Nota:** bom

A mudança respeitou as camadas da base: os relatórios novos dependem só de `hotel.reservations` e não usam Javalin nem
JDBC (tabela de bibliotecas em `measures/dependencies.md`); as rotas novas só leem o mês e escrevem o CSV
(`src/main/java/hotel/web/HttpApi.java:99-106`); a montagem continua num lugar só
(`src/main/java/hotel/Hotel.java:28-30`). O ciclo `hotel.documents` ↔ `hotel.reservations` veio da base, e o agente
não o tocou. O único ponto que vai doer no crescimento é que cada relatório novo pede mais um campo, mais um parâmetro
no construtor e mais uma rota em `HttpApi` (`src/main/java/hotel/web/HttpApi.java:37-51`); com dois relatórios
anunciados, o construtor passaria de nove parâmetros. Isso é consequência do design e é avaliado lá, não é mistura de
camadas.

## Design de baixo nível

**Números:** 4 relatórios, 0 numa estrutura comum, 4 escritos inteiros; a linha de título copiada 4 vezes
(`OccupancyReport.java:34`, `RevenueReport.java:35`, `TourismTaxReport.java:33`, `RatePlanReport.java:31`); o filtro do
financeiro copiado inteiro em `RatePlanReport` (9 linhas).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O pattern: o texto traz quatro relatórios com os mesmos passos (título, cabeçalho, uma linha por reserva, total),
avisa que o contador vai pedir mais dois e diz que, quando o formato mudar (a linha de data no fim, no ano que vem),
deve mudar em todos de uma vez. A resposta esperada era o esqueleto num lugar só e cada relatório preenchendo os passos
que mudam. O agente não criou classe-base, nem escritor genérico, nem sequer uma função comum para o título: cada
relatório tem o seu `StringBuilder`, monta o título à mão com o nome da pousada em texto
(`src/main/java/hotel/reports/OccupancyReport.java:33-45`, `RevenueReport.java:34-47`, `TourismTaxReport.java:32-49`,
`RatePlanReport.java:30-47`). A linha de data do ano que vem vai exigir mexer nos quatro (e nos que vierem), que é
justamente o que o dono pediu para evitar. Pela referência, isso é omissão.

O exagero: o agente não acrescentou interface, fábrica nem camada sem necessidade. Contenção.

O resto do código (o que o agente escreveu):
- duplicação: `RatePlanReport.java:21-29` repete linha a linha a seleção de `RevenueReport.java:24-31` (`keptMoney &&
  startsInMonth`); se a regra do financeiro mudar, as duas cópias podem divergir, e o pedido exige que os dois contem
  as mesmas reservas. O nome da pousada já existia como constante (`src/main/java/hotel/reservations/ReservationService.java:12`,
  `HOTEL_NAME`), e o agente escreveu o texto literal quatro vezes em vez de reutilizá-la;
- as quatro rotas de relatório são quase idênticas (`src/main/java/hotel/web/HttpApi.java:91-106`);
- a taxa de turismo é uma constante no código (`src/main/java/hotel/reports/TourismTaxReport.java:14`), e o texto avisa
  que a prefeitura muda o valor todo ano. Pelo menos está num lugar só e com nome, mas cada mudança de valor exige
  compilar e publicar de novo. É aceitável, não ideal;
- os tipos estão certos: dinheiro em `BigDecimal` com 2 casas (`TourismTaxReport.java:35-38`, `RatePlanReport.java:33-36`),
  `YearMonth` para o mês e o `enum RatePlan` percorrido em ordem para garantir as duas linhas (`RatePlanReport.java:34`).
  Os métodos são curtos e os nomes dizem o que fazem.

Os defeitos ficam todos no pacote de relatórios, mas são duplicações que o próprio pedido mostrava que iam se
multiplicar.

## Testes

**Números:** 28 testes rodaram, 0 falhas, 0 erros, 0 pulados; 28 métodos (a base tinha 26: os 26 continuam e passam,
5 asserções da base foram atualizadas só com a linha de título e 2 testes são novos); cobertura de linhas 93,5%
(532/569) e de ramos 89,1% (123/138); `hotel.reports` 105/105 linhas e 44/44 ramos (`measures/tests.md`; na base,
93,8% e 87,1%, `measures/tests-base.md`).
**Nota:** com problemas

O que está bom: os testes de relatório comparam o CSV inteiro, com valores exatos, inclusive o título
(`src/test/java/hotel/reports/ReportsTest.java:57-145`), e nenhum teste da base foi apagado nem enfraquecido; as
mudanças em `ReportsTest` são só a linha de título acrescentada. O teste da taxa
(`src/test/java/hotel/reports/ReportsTest.java:112-122`) confere título, colunas, ordem e total; o dos planos
(`ReportsTest.java:125-135`) confere a linha `NON_REFUNDABLE,0,0.00` de um plano sem reserva.

O que fica sem rede, pela pergunta da mutação:
- todas as reservas da fixture têm 1 hóspede (`ReportsTest.java:33`, o `1` fixo em `book`), e a taxa esperada é
  `9.00` = 4,50 × 1 × 2 (`ReportsTest.java:118-119`). Se alguém apagasse o fator `guests()` da conta
  (`TourismTaxReport.java:37-38`), o teste continuaria passando. O "por hóspede" do pedido não está testado;
- o relatório por plano nunca vê uma reserva `NON_REFUNDABLE` com dinheiro no mês testado (abril só tem STANDARD), nem
  a não reembolsável cancelada que o pedido manda contar (ela existe em maio na fixture, `ReportsTest.java:96-107`, mas
  só o financeiro é testado com ela). Uma troca do filtro de `amountKept() > 0` por `isPaid()` em
  `RatePlanReport.java:23-25` passaria nos testes;
- as rotas novas `/reports/tourism-tax` e `/reports/rate-plans` não têm teste pela API (`src/test/java/hotel/web/HttpApiTest.java:113-120`
  só chama as duas rotas da base), então um erro de rota ou de validação do mês só apareceria em produção.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` (6 colunas, todas ok); nenhuma chave
estrangeira sem índice; nenhuma data em texto; 4 colunas de situação/tipo sem CHECK; `EXCLUDE USING gist` contra
reserva sobreposta; 1 migração Flyway, `V1__create_tables.sql` igual à da base (`measures/database.md`).
**Nota:** bom

O agente não mexeu no banco: o pedido não precisava de coluna nova (hóspedes, noites, plano e valor guardado já
existiam), e a migração da base ficou intacta, o que não quebra quem já roda o sistema. As colunas `status`,
`rate_plan`, `type` e `channel` sem CHECK vêm da base. O único reparo que cabe ao agente é de carga: os dois
relatórios novos repetem o padrão da base de trazer a tabela inteira (`ReservationStore.all()`,
`src/main/java/hotel/reservations/ReservationStore.java:108-111`) e filtrar o mês em Java
(`src/main/java/hotel/reports/TourismTaxReport.java:24-29`, `RatePlanReport.java:22-28`). Agora são quatro relatórios
que leem tudo a cada chamada. Numa pousada de cinco quartos isso não pesa, e o padrão veio da base.

## Comportamento

**Números:** 64 de 64 conferências passaram: parte comum 46/46, dados depois de reiniciar 3/3, cenário 09 13/13,
reservas simultâneas 2/2 (`measures/behavior.md`).
**Nota:** bom

Os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>" e, depois do título, a ocupação e o financeiro
seguem iguais aos da base. A taxa de turismo lista as reservas pagas do mês na ordem do financeiro, com a taxa e o
total, e responde também num mês vazio e com 400 para mês mal escrito. O relatório por plano traz STANDARD e
NON_REFUNDABLE (com 0 quando não há reserva) e o total. Nada do que funcionava antes quebrou. Para o dono da pousada,
os relatórios saem como ele pediu. O problema está em mudar depois, não no que funciona hoje.

## Dúvidas

O texto diz "as reservas pagas" para a taxa de turismo e lista as situações (confirmada, check-in, check-out); o
agente usou `isPaid()` (`src/main/java/hotel/reservations/Reservation.java:91-95`), que exclui a não reembolsável
cancelada, embora a pousada tenha ficado com o dinheiro dela. A leitura do agente segue a lista do texto e o roteiro
aceitou. Fora isso, nenhuma.
