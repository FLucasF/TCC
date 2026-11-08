# Revisão: manutenção, cenário 09

## Visão geral

A base é um serviço Javalin em 7 pacotes. `hotel.web.HttpApi` recebe a requisição e chama o serviço
(`hotel.reservations.ReservationService`) ou um relatório (`hotel.reports`). Os relatórios leem as reservas por
`ReservationStore.all()`, e os dados ficam no PostgreSQL, num esquema criado por uma migração Flyway (`V1__create_tables.sql`).
O agente fez três coisas. Criou `TouristTaxReport` e `RatePlanReport`, duas classes novas escritas inteiras, cada uma
copiando os passos dos relatórios que já existiam. Criou um utilitário estático, `ReportFormatter.withTitle`, que põe a
linha de título na frente do CSV. Ligou as rotas novas em `HttpApi` e em `Hotel`. Os valores calculados estão certos.
O mais sério tem duas partes. Primeiro, o pedido era deixar o esqueleto dos relatórios num lugar só, para que "quando o
jeito dos relatórios mudar, mude em todos de uma vez", e isso não foi feito: os quatro relatórios repetem a sequência
seleção → ordem → cabeçalho → linhas → total. Segundo, a linha de total da taxa de turismo saiu `total,,,,,45.00` em
vez de `total,45.00`. Isso quebrou 2 das 13 conferências do cenário, e o próprio preenchimento ficou desalinhado: são
6 campos para 7 colunas.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. `hotel.reports`
passou de 2 para 5 classes (de 95 para 220 linhas) e `HttpApi` de 170 para 186 linhas.
**Nota:** bom

O agente não mexeu na estrutura, e as peças novas ficaram nos lugares certos:
- **Os relatórios novos** estão em `hotel.reports`. Não usam Javalin nem JDBC (a tabela de bibliotecas não marca nada para o
  pacote) e dependem só de `ReservationStore` (`src/main/java/hotel/reports/TouristTaxReport.java:14`,
  `src/main/java/hotel/reports/RatePlanReport.java:14`).
- **As rotas novas são finas:** leem o mês, chamam `generate` e escrevem o CSV
  (`src/main/java/hotel/web/HttpApi.java:99-106`).
- **A montagem** continua num lugar só (`src/main/java/hotel/Hotel.java:28-30`).

O agente não criou nenhuma dependência nem ciclo novo. O ciclo `documents`/`reservations` veio da base. A única coisa a notar é
que o construtor de `HttpApi` cresce em um parâmetro a cada relatório (`src/main/java/hotel/web/HttpApi.java:37-44`), o que
acompanha a falta de um tipo comum para os relatórios, descrita na próxima coluna.

## Design de baixo nível

**Números:** há 4 relatórios e nenhum deles passa por um esqueleto comum (0 dentro da estrutura, 4 fora). A única peça
compartilhada é a função estática que põe o título (`ReportFormatter.withTitle`), chamada à mão no fim de cada um dos 4.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O pedido descreve quatro relatórios com os mesmos passos, avisa que virão mais e diz que uma mudança no
formato (a linha da data no fim, no ano que vem) tem que valer para todos de uma vez. A resposta esperada é o Template
Method: o esqueleto (título, cabeçalho, linhas, total) num lugar só, e cada relatório preenchendo só os passos que mudam.
Não há classe abstrata nem escritor genérico.
- **O que aproxima:** a linha de título tem um dono só, `src/main/java/hotel/reports/ReportFormatter.java:13-21`, e os quatro
  relatórios o usam (`OccupancyReport.java:44`, `RevenueReport.java:93`, `TouristTaxReport.java:50`, `RatePlanReport.java:50`).
- **O que afasta:** é exatamente o caso que a referência chama de malfeito, um "esqueleto" que é só uma função
  utilitária de CSV enquanto cada relatório repete a sequência dos passos. Cada `generate` refaz o laço de seleção sobre
  `store.all()`, a ordenação, o `StringBuilder` com o cabeçalho, as linhas e o total. Compare
  `RevenueReport.java:23-46` com `TouristTaxReport.java:21-50`: a seleção e a ordenação são as mesmas linhas copiadas
  (`RevenueReport.java:32` e `TouristTaxReport.java:29`). Um relatório novo vai copiar tudo de novo, e quem esquecer de
  chamar `withTitle` sai sem título.
- **A linha da data no fim** até caberia em `withTitle`, mas só por acaso do nome. Nada no código obriga um relatório a
  passar por ali.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface, fábrica ou camada nova. A classe é contenção.

**O resto do código** tem defeitos localizados:
- **Duplicação de regra.** A seleção "dinheiro que ficou e começa no mês" foi copiada de `RevenueReport.java:26-28` para
  `RatePlanReport.java:27-29`, em vez de ser compartilhada. O pedido diz que os dois relatórios contam as mesmas reservas.
- **Plano fixo no código.** `RatePlanReport` amarra os dois planos num `if/else` com dois pares de contadores
  (`RatePlanReport.java:21-24`, `:30-36`, `:42-45`) em vez de percorrer `RatePlan.values()`. Um terceiro plano cairia
  calado no `else` e seria somado como NON_REFUNDABLE.
- **Cálculo repetido.** As noites são recalculadas à mão com `toEpochDay` (`TouristTaxReport.java:35`), embora a reserva
  já tenha `price().nights()` (usado em `src/main/java/hotel/documents/ReservationReceipt.java:34`).
- **A taxa** é uma constante no código (`TouristTaxReport.java:14`). Aceitável, mas o pedido avisa que o valor muda
  todo ano.
- **Sujeira miúda.** Há imports sem uso (`RatePlanReport.java:8-9`), `java.math.RoundingMode` com o nome completo
  (`TouristTaxReport.java:39`) e duas sobrecargas de `withTitle` iguais, que só mudam o tipo do período
  (`ReportFormatter.java:13-21`).
- **Os tipos estão certos:** `BigDecimal` com 2 casas e `HALF_UP`, `YearMonth` e `LocalDate`.

## Testes

**Números:** 31 testes rodaram, com 0 falhas, 0 erros e 0 pulados. A base tinha 26 (`measures/tests-base.md`). Há 31
métodos escritos. A cobertura de linhas é 93,6% (537/574, contra 93,8% na base) e a de ramos 88,8% (119/134, contra
87,1%). `hotel.reports` está 100% coberto (110/110 linhas, 40/40 ramos). Os 26 testes da base continuam passando.
**Nota:** com problemas

**O que o agente fez bem:**
- Atualizou os testes da base para o título, sem enfraquecer nenhum: comparam o CSV inteiro
  (`src/test/java/hotel/reports/ReportsTest.java:59-121`).
- Escreveu 5 testes novos, que comparam o CSV inteiro com valores (`ReportsTest.java:125-190`).
- Cobriu as duas linhas de plano, inclusive o plano com zero (`:150`, `:180`).
- Cobriu a NON_REFUNDABLE cancelada contando no relatório de planos (`:163`).
- Cobriu a reserva de março que entra só no mês em que começa (`:138`).

**O que deixa a desejar:**
- **A pergunta da mutação na taxa de turismo:** todas as reservas do cenário têm 1 hóspede
  (`ReportsTest.java:35`). Quem apagasse o `.multiply(guests)` (`TouristTaxReport.java:37`) não seria pego por nenhum
  teste. O exemplo do próprio pedido (2 hóspedes × 3 noites = 27,00) não está testado.
- **O formato errado ficou travado:** os testes fixam a linha de total `total,,,,,18.00` e `total,,,,,13.50`
  (`ReportsTest.java:133`, `:145`), quando o pedido diz "total" e o valor.
- **As rotas novas não têm teste pela API:** `/reports/tourism-tax` e `/reports/rate-plans` não aparecem em
  `src/test/java/hotel/web/HttpApiTest.java`. Lá só se conferiu o título dos dois relatórios antigos (`:116`, `:120`).

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`. Nenhuma chave estrangeira sem
índice e nenhuma data em texto. Há `EXCLUDE` com gist contra duas reservas na mesma noite. As colunas de situação e de
plano estão em `text` sem `CHECK`. Há uma migração Flyway, `V1__create_tables.sql`, "igual" à da base
(`measures/database.md`).
**Nota:** bom

O pedido não exigia mudar o banco, e o agente não o tocou: a migração da base ficou intacta e não há SQL novo.
- **Leitura da tabela inteira:** os dois relatórios novos leem as reservas por `store.all()` e filtram em Java
  (`TouristTaxReport.java:23`, `RatePlanReport.java:26`). Repetem o jeito dos relatórios da base
  (`RevenueReport.java:25`, `ReservationStore.java:111`), o que pesa à medida que a tabela cresce. É uma nota de carga,
  que vem da base.
- **Colunas sem `CHECK`:** a falta de `CHECK` em `status` e `rate_plan` também vem da base.

Nada do que o agente fez perde ou corrompe dado.

## Comportamento

**Números:** 62 de 64 conferências passaram (`measures/behavior.md`):
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 09: 11/13;
- reservas simultâneas: 2/2.

**Nota:** com problemas

**O que funciona:** os quatro relatórios começam com o título certo, por exemplo `Pousada Maré Alta,tourism-tax,2027-07`.
A ocupação e o financeiro continuam iguais depois do título. O relatório de planos sai certo, com a linha zerada do plano
sem reserva.

**As duas falhas** são a mesma: a linha de total da taxa de turismo, no mês com reservas e no mês vazio.
- **O que veio e o que se esperava:** o serviço devolve `total,,,,,45.00` e `total,,,,,0.00`; o esperado é `total,45.00`
  e `total,0.00` (`TouristTaxReport.java:49`).
- **O texto não deixava em aberto:** ele diz "a última linha é 'total' e o valor", e o financeiro, que o pedido manda
  imitar, usa `total,935.76`.
- **O preenchimento não alinha:** são 5 vírgulas para 7 colunas, então numa planilha o 45.00 cai na coluna `nights`, e não
  em `tax`.

Os valores da taxa estão certos (27,00 para 2 hóspedes × 3 noites e 18,00 para 4 × 1). Para o dono da pousada, isso quer
dizer que o relatório que vai para a prefeitura tem o total certo no lugar errado. Quem o ler numa planilha, ou com um
programa que procure "total" e o valor, vai ler errado ou não vai achar.

## Dúvidas

- **Malfeito ou omissão.** A referência define omissão como "os quatro relatórios escritos inteiros, cada um com a sua
  linha de título", e malfeito como "o esqueleto é só uma função utilitária de CSV e cada relatório ainda repete a
  sequência". Aqui os quatro estão escritos inteiros, mas a linha de título tem um dono só, `ReportFormatter`. Dei
  malfeito pela segunda definição. Uma leitura mais dura daria omissão, porque o esqueleto em si não existe.
- **O README** não foi atualizado com as duas rotas novas (`README.md:56-57` só lista as antigas). O pedido não exigia
  isso, e não pesei em nenhuma coluna.
