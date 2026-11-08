# Revisão: manutenção, cenário 09

## Visão geral

A base já tinha os pacotes `hotel` (montagem e banco), `hotel.web` (rotas Javalin), `hotel.reservations` (regras e
`ReservationStore` com JDBC), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`. Uma requisição de
relatório entra numa rota de `hotel/web/HttpApi.java`, que lê o parâmetro e chama `generate` de um relatório. O
relatório lê as reservas com `ReservationStore.all()`, e os dados ficam no PostgreSQL, num esquema criado por uma
migração Flyway. O agente mudou só o pacote `hotel.reports`, as rotas e a montagem. Ele criou a classe abstrata
`Report<P>` com o método-modelo `final generate` (título, cabeçalho, linhas, total) e pôs os quatro relatórios como
subclasses: os dois que já existiam e os dois novos. A seleção "o que conta no financeiro" foi para
`MonthlyRevenue`, e o relatório por plano usa essa mesma seleção. Os 64 pontos do roteiro passaram, e os 28 testes
também. Não há defeito sério. Os pontos fracos são dois: o teste do relatório por plano não confere a separação por
plano, e o cálculo da taxa usa `setScale(2)` sem modo de arredondamento.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), igual à
base (`measures/dependencies-base.md` tem os mesmos pacotes, as mesmas dependências e o mesmo ciclo). `hotel.reports`
passou de 2 classes e 95 linhas para 7 classes e 285 linhas, e não usa Javalin nem JDBC. `HttpApi` passou de 170 para
186 linhas.
**Nota:** bom

As mudanças respeitam as camadas da base. As rotas novas só leem o mês e chamam o relatório
(`src/main/java/hotel/web/HttpApi.java:99-106`). Os relatórios não conhecem Javalin nem SQL e leem os dados pelo
`ReservationStore` (`src/main/java/hotel/reports/TourismTaxReport.java:34`,
`src/main/java/hotel/reports/MonthlyRevenue.java:17`). As peças continuam montadas num lugar só, no `Hotel`
(`src/main/java/hotel/Hotel.java:30-44`). O valor da taxa entra pelo construtor e pode vir de uma variável de ambiente
(`src/main/java/hotel/Hotel.java:60-62`), o que atende ao "a prefeitura muda o valor todo ano" sem mexer no código. O
ciclo `documents` ↔ `reservations` veio da base, e o agente não o tocou. Uma observação que não pesa na nota: cada
relatório novo acrescenta um parâmetro ao construtor do `HttpApi` (agora são 7, `HttpApi.java:37-44`) e uma rota
quase igual às outras. Com os relatórios que o contador ainda vai pedir, esse é o ponto que vai crescer.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo esqueleto (`OccupancyReport`, `RevenueReport`, `TourismTaxReport` e
`RatePlanReport` estendem `Report`). Nenhum relatório escreve o título ou o CSV à mão. Há 3 passos abstratos:
`name`, `header` e `table`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern pedido é o Template Method, e foi aplicado direito. `Report.generate` é `final`
(`src/main/java/hotel/reports/Report.java:16-26`). O método escreve o título com o nome da pousada, o nome do
relatório e o período (`Report.java:19`), depois o cabeçalho, as linhas e o total, e cada subclasse só fornece os
passos abstratos (`Report.java:29-33`). Os dois relatórios da base foram convertidos e perderam o `StringBuilder`
próprio (`OccupancyReport.java:23-55`, `RevenueReport.java:22-47`), e os dois novos seguem a mesma forma
(`TourismTaxReport.java:21-58`, `RatePlanReport.java:20-50`). A linha de data que virá no ano que vem entra num lugar
só, depois de `Report.java:24`. Uma ressalva pequena: o passo `table` junta seleção, ordenação, linhas e total num
método só. O esqueleto não separa "escolher e ordenar" de "uma linha por reserva", então cada relatório ainda escreve
o seu laço. A referência pede o esqueleto com título, cabeçalho, linhas e total, e isso está lá.

Não há exagero. O `record Table` (`Report.java:14`) e o `MonthlyRevenue` (`MonthlyRevenue.java:11-33`) têm uso real.
O `MonthlyRevenue` evita copiar no relatório por plano o critério do financeiro (`RatePlanReport.java:32`), e a taxa
reaproveita a ordem e o filtro de mês desse critério (`TourismTaxReport.java:35,39`). Não há interface nem fábrica
sem necessidade.

No resto, o dinheiro está em `BigDecimal` com 2 casas, os períodos em `LocalDate`/`YearMonth` e os planos vêm do enum,
na ordem da declaração (`RatePlanReport.java:36`). Há um defeito latente pequeno:
`TourismTaxReport.java:45-46` usa `setScale(2)` sem `RoundingMode`. Se a prefeitura fixar um valor com 3 casas na
variável `TOURISM_TAX_PER_GUEST_NIGHT` (por exemplo 4.505), o relatório lança `ArithmeticException` e a rota responde
500. Com valores em centavos, como hoje, ele funciona. Um detalhe de estilo: em `MonthlyRevenue.java:27` a constante
`ORDER` foi declarada depois do método que a usa.

## Testes

**Números:** 28 testes rodaram, 0 falharam e 0 foram pulados, com 28 métodos escritos (a base tinha 26, e nenhum foi
apagado ou enfraquecido). A cobertura de linhas subiu de 93,8% para 94,4% (552 de 585) e a de ramos de 87,1% para
88,6% (117 de 132). O pacote `hotel.reports` tem 116 de 116 linhas e 38 de 38 ramos cobertos.
**Nota:** com problemas

Os testes da base foram atualizados para conferir o CSV inteiro com a linha de título
(`src/test/java/hotel/reports/ReportsTest.java:62,75,88,105,117`). Há um teste da taxa com o exemplo do texto: 2
hóspedes por 3 noites, R$ 27,00, e o total 45.00 (`ReportsTest.java:124-141`), e outro dos planos com a linha vazia de
`NON_REFUNDABLE` (`ReportsTest.java:144-154`). Pela API, as duas rotas novas são conferidas com o total e com o 400
(`src/test/java/hotel/web/HttpApiTest.java:119-122`).

A pergunta da mutação mostra regras novas sem rede:
- **A separação por plano:** em todo teste do relatório de planos, só o `STANDARD` tem reservas (`ReportsTest.java:150-152`,
  `HttpApiTest.java:121`). Se `RatePlanReport.java:41` jogasse todas as reservas no `STANDARD`, nenhum teste falharia.
- **A não reembolsável cancelada:** o texto pede que ela conte no relatório por plano, como no financeiro, e isso não
  é testado ali. Se `RatePlanReport.java:32` usasse as reservas pagas (`isPaid`) em vez de `MonthlyRevenue`, os
  testes passariam.
- **A seleção da taxa:** nenhum teste tem, no mês, uma não reembolsável cancelada nem uma reserva com check-out. Se
  `TourismTaxReport.java:35` trocasse `isPaid()` por "ficou dinheiro", ou deixasse de fora o `CHECKED_OUT`, os testes
  também passariam.

O roteiro de comportamento confere esses pontos, e eles funcionam. Faltam testes do projeto que os segurem.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`, as datas em `date`, a chave
estrangeira tem índice e um `EXCLUDE` com gist impede duas reservas do mesmo quarto nas mesmas noites. A conferência
acusou só as colunas de situação e tipo em `text` sem `CHECK` (`reservations.status`, `reservations.rate_plan`,
`rooms.type`, `messages.channel`). A migração `V1__create_tables.sql` está igual à da base.
**Nota:** bom

O agente não mexeu no banco, e o pedido não precisava: a taxa e o relatório por plano usam colunas que já existiam
(`guests`, `rate_plan`, os valores). Ele não editou a migração da base nem criou SQL novo. As colunas de texto sem
`CHECK` vieram da base. Uma observação de carga, que também veio da base e não pesa na nota: todos os relatórios leem
a tabela inteira (`src/main/java/hotel/reservations/ReservationStore.java:108-111`) e filtram em Java
(`MonthlyRevenue.java:17`, `TourismTaxReport.java:34`). Os dois relatórios novos repetem o padrão dos dois antigos.

## Comportamento

**Números:** 64 de 64 conferências passaram. Parte comum: 46 de 46. Dados depois de reiniciar: 3 de 3. Cenário 09:
13 de 13. Reservas simultâneas: 2 de 2.
**Nota:** bom

Os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>". A ocupação e o financeiro continuam iguais
depois do título. A taxa de turismo cobra R$ 4,50 por hóspede por noite das reservas pagas do mês, e um mês vazio sai
com o total 0.00. O relatório por plano traz `STANDARD` e `NON_REFUNDABLE`, inclusive um plano sem reservas, com o
total. Nada do que já funcionava quebrou. Para o dono da pousada, os relatórios novos saem como ele pediu, e os
antigos só ganharam a linha de título.

## Dúvidas

O texto não diz como a taxa deve ser arredondada quando a prefeitura mudar o valor. Com valores em centavos, o
resultado é exato. Um valor com mais de 2 casas derruba o relatório (`TourismTaxReport.java:46`), e não sei se isso
chega a acontecer na prática; por isso não pesou na nota do design.
