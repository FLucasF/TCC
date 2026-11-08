# Revisão: manutenção, cenário 09

## Visão geral

O sistema é o da base: `hotel` monta tudo (`Hotel.java`), `hotel.web.HttpApi` recebe a requisição no Javalin, lê os
parâmetros e chama os serviços de `hotel.reservations` ou os relatórios de `hotel.reports`. Os dados ficam no
PostgreSQL (Flyway, uma migração), e o acesso a eles fica em `ReservationStore`/`Rooms`/`SentMessages`. O agente
criou uma classe abstrata `Report` com um método-modelo `final` (título, cabeçalho, linhas, total), fez os quatro
relatórios herdarem dela (os dois da base e os dois novos, `TourismTaxReport` e `RatePlansReport`) e acrescentou as
duas rotas. O roteiro do comportamento passou inteiro (64 de 64) e os 30 testes passam. O mais sério está no jeito
de passar dados entre os passos: como os passos do modelo não recebem parâmetros, cada relatório guarda o que
seleciona em **campos da instância** (`RevenueReport.java:18-19`, `TourismTaxReport.java:20-21`,
`RatePlansReport.java:18-21`, `OccupancyReport.java:17`). Só existe uma instância de cada relatório
(`Hotel.java:32-35`), e o Jetty atende as requisições em várias threads ao mesmo tempo. Com o serviço de pé, mandei
pedidos simultâneos de `/reports/revenue` para julho e agosto: **366 de 480 respostas vieram erradas**. Houve
relatório de julho com uma reserva de agosto e total de 123.299,76 em vez de 46.859,40, e respostas `500 Server
Error` (199 `ConcurrentModificationException` em `RevenueReport.java:34`). Com 6 pedidos do mesmo mês, 90 de 120
vieram erradas. Na taxa de turismo, 18 de 20 vieram erradas. A mesma carga na base deu 0 erradas em 480: o defeito
entrou com esta mudança.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), igual à
base (`measures/dependencies-base.md`: 7 pacotes, 17 dependências, o mesmo ciclo). `hotel.reports` passou de 2 para 5
classes (de 95 para 325 linhas), e `HttpApi` passou de 170 para 186 linhas.
**Nota:** bom

O agente não acrescentou nenhuma dependência nem ciclo. O ciclo `documents` ↔ `reservations` vem da base. As rotas
novas são finas: leem o mês, chamam o relatório e escrevem o CSV (`HttpApi.java:99-106`), como as da base. Os
relatórios novos ficam em `hotel.reports`, sem Javalin e sem SQL, e leem os dados pelo `ReservationStore`. A montagem
continua num lugar só (`Hotel.java:28-35`). O construtor de `HttpApi` passou a ter 7 parâmetros, um a mais por
relatório (`HttpApi.java:37-44`). Ainda é aceitável, mas cada relatório novo vai mexer na montagem e na rota.

## Design de baixo nível

**Números:** 4 de 4 relatórios dentro da estrutura (`OccupancyReport`, `RevenueReport`, `TourismTaxReport` e
`RatePlansReport` herdam de `Report`), nenhum fora dela. A linha de título é escrita em 1 lugar
(`Report.java:22-24`). O estado de cada execução fica em 9 campos mutáveis, espalhados pelos 4 relatórios, que são
compartilhados entre as requisições. Na carga simultânea, 366 de 480 respostas vieram erradas, contra 0 de 480 na
base.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** É o Template Method que a referência descreve. `Report` é abstrata e tem o método-modelo `final`
(`Report.java:13-20`), que fixa a sequência título → cabeçalho → linhas → total. O título fica num só lugar
(`Report.java:22-24`), e os passos que variam são abstratos (`reportName`, `header`, `dataRows` e `totalRow`, em
`Report.java:27-36`). Os dois relatórios da base passaram para a estrutura (`OccupancyReport.java:13`,
`RevenueReport.java:15`), e os dois novos também. A linha com a data que vai entrar no ano que vem se põe em
`Report.generate`, num lugar só. O que afasta um pouco é que a seleção e a ordenação ficam fora do esqueleto: cada
subclasse tem o seu `generate(...)` público, que filtra, ordena e chama `super.generate`
(`RevenueReport.java:25-37`, `TourismTaxReport.java:27-38`). Mas esses são justamente os passos que variam, e a
sequência comum não está copiada.

**Exagero:** não há. A hierarquia tem uma classe abstrata e quatro subclasses, sem interface sobrando, fábrica ou
camada que só repassa.

**A nota (o resto do design).** O defeito sério é o estado. Os passos `dataRows()`/`totalRow()` não recebem dados, e
por isso cada `generate` grava a seleção em campos (`OccupancyReport.java:25`, `RevenueReport.java:26` e `:35`,
`TourismTaxReport.java:28` e `:36`, `RatePlansReport.java:28-29` e `:45-46`), que os passos leem depois. Os quatro
objetos são únicos no processo (`Hotel.java:32-35`) e atendem requisições concorrentes. Uma requisição sobrescreve a
lista e o acumulador da outra: o relatório sai com reservas do mês errado, com o total errado, ou cai em
`ConcurrentModificationException` e vira um 500 (medido: 366 de 480 erradas). Na base esses valores eram variáveis
locais, e o mesmo teste deu 0 erros. Para o contador e para a prefeitura, isso é dado errado num relatório de
dinheiro e de imposto.

Há também um acoplamento temporal. Em `RevenueReport` e `TourismTaxReport`, o total é acumulado dentro de
`dataRows()` (`RevenueReport.java:59`, `TourismTaxReport.java:66`), e `totalRow()` só fica certo se `dataRows()` rodar
antes. Um esqueleto que mude a ordem dos passos quebra o total sem aviso.

Menores:
- `RatePlansReport.dataRows` escreve `STANDARD` e `NON_REFUNDABLE` à mão (`RatePlansReport.java:68-73`), enquanto o
  resto da classe percorre `RatePlan.values()` (`:30`, `:47`). Um plano novo entraria no total e ficaria fora das
  linhas.
- O filtro "dinheiro que ficou e começa no mês" está copiado de `RevenueReport.java:28-30` em
  `RatePlansReport.java:36-38`. A ordenação do financeiro está copiada em `TourismTaxReport.java:35`.

O dinheiro está em `BigDecimal` com 2 casas, e a taxa é uma constante (`TourismTaxReport.java:17`).

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros e 0 pulados (a base tinha 26). 30 métodos de teste escritos.
Cobertura de 93,7% das linhas (553/590) e 89,0% dos ramos (121/136). Na base, 93,8% e 87,1%. `hotel.reports` está
com 126/126 linhas e 42/42 ramos.
**Nota:** com problemas

O agente atualizou os 5 testes de relatório da base com a linha de título, sem enfraquecer nenhum
(`ReportsTest.java:61`, `:74`, `:87`, `:104`, `:116`). Também escreveu 4 testes novos, todos conferindo o CSV inteiro
com valores:
- a taxa de turismo do mês, que deixa fora a reserva não paga e a cancelada com devolução e inclui a CHECKED_IN
  (`ReportsTest.java:123`);
- o exemplo do texto, 2 hóspedes × 3 noites = 27,00 (`:138`);
- os planos, com um plano zerado (`:156` e `:171`).

As lacunas:
- **As rotas novas não são testadas.** Não há chamada a `/reports/tourism-tax` nem a `/reports/rate-plans` em
  `HttpApiTest`, que continua conferindo só o fim do financeiro (`HttpApiTest.java:118`). As linhas das rotas novas
  ficaram sem cobertura (`hotel.web`: de 105/110 para 109/120 linhas).
- **"As mesmas reservas que o financeiro" não está conferido nos planos.** O teste usa uma NON_REFUNDABLE confirmada
  (`ReportsTest.java:172-173`), não uma cancelada. Se alguém trocasse o filtro de `RatePlansReport.java:38` por
  `isPaid()`, nenhum teste falharia.
- A taxa não tem caso com uma reserva CHECKED_OUT.
- Nenhum teste chama um relatório duas vezes ao mesmo tempo. O defeito de concorrência passou sem rede.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` (6 de 6 colunas). Nenhuma chave
estrangeira sem índice, nenhuma data em texto. Há um `EXCLUDE` contra duas reservas sobrepostas do mesmo quarto. A
única migração (`V1__create_tables.sql`) está igual à da base. As colunas de situação (`status`, `rate_plan`, `type`
e `channel`) não têm CHECK.
**Nota:** bom

O agente não mexeu no banco, e nada do pedido exigia mexer: os relatórios novos só leem. Os pontos que as conferências
acusaram vêm da base: as colunas de situação sem CHECK e a leitura da tabela inteira com `ReservationStore.all()`
(`ReservationStore.java:108-111`), filtrada em Java. Os dois relatórios novos usam o mesmo `all()`
(`TourismTaxReport.java:29`, `RatePlansReport.java:35`), do mesmo jeito que os da base. Isso só vai pesar quando a
tabela crescer muito. O SQL usa parâmetros, e as concatenações listadas pelo script são constantes (`COLUMNS`).

## Comportamento

**Números:** 64 de 64 conferências passaram:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário 09: 13 de 13;
- duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>". A taxa de turismo cobra 4,50 por hóspede por
noite das reservas pagas, na ordem do financeiro, e fecha com o total. O relatório de planos traz STANDARD e
NON_REFUNDABLE, mesmo zerados, e conta a NON_REFUNDABLE cancelada como o financeiro. O mês inválido dá 400. Para o
dono da pousada, tudo o que ele pediu funciona quando um relatório é pedido por vez.

Fora do roteiro, achei o defeito de concorrência descrito no design: com pedidos simultâneos, os relatórios devolvem
dados misturados e erros 500. Ele pesa na coluna do design e não muda esta nota.

## Dúvidas

- Onde pesar o defeito de concorrência. Ele nasce do jeito de passar dados entre os passos do Template Method (estado
  em campos de objetos compartilhados) e por isso entrou na nota do design. O roteiro do comportamento não o pega,
  porque pede um relatório por vez. Numa pousada pequena, dois pedidos de relatório no mesmo instante são raros, mas
  o resultado errado sai sem nenhum aviso.
- Classe `acerto` ou `malfeito`. Fiquei com `acerto`, porque o esqueleto cobre os quatro relatórios e nada repete a
  sequência comum. A seleção e a ordenação, que o texto lista entre os passos, ficaram fora do método-modelo, em cada
  `generate` público. Um revisor mais estrito poderia ver nisso uma parte do esqueleto ainda repetida.
- O README não lista as duas rotas novas (`README.md:56-57` tem só as da base). É menor, e não pesei.
