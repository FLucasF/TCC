# Revisão: manutenção, cenário 09

## Visão geral

A base é um serviço Javalin em sete pacotes. `hotel.web` (as rotas) chama as regras de `hotel.reservations` e os
relatórios de `hotel.reports`. Os dados ficam no PostgreSQL, atrás de `ReservationStore`, `Rooms` e `SentMessages`, e
o esquema é criado por uma migração Flyway. O agente acrescentou ao pacote `hotel.reports` dois relatórios novos
(`TourismTaxReport` e `RatePlanReport`) e uma classe abstrata `Report`, de onde os quatro relatórios herdam. Também
registrou duas rotas em `HttpApi` e passou as peças novas em `Hotel`. Pela API, tudo funciona: 64 de 64 conferências
passaram. O ponto mais sério está no design. `Report` só põe a linha de título na frente de um texto que cada
relatório monta inteiro sozinho. A sequência título → cabeçalho → linhas → total não ficou num lugar só, e foi isso
que o dono pediu ("quando o jeito dos relatórios mudar, quero que mude em todos de uma vez").

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), o mesmo da
base (`measures/dependencies-base.md`). As dependências são as mesmas da base. Só cresceu `hotel.reports`, de 2 para 5
classes e de 95 para 216 linhas, e `HttpApi` foi de 170 para 186 linhas.
**Nota:** bom

As rotas novas são finas: leem o mês, chamam o relatório e escrevem o CSV (`src/main/java/hotel/web/HttpApi.java:99-106`).
Os relatórios não conhecem o Javalin nem o JDBC e leem os dados só por `ReservationStore.all()`
(`src/main/java/hotel/reports/TourismTaxReport.java:23`, `src/main/java/hotel/reports/RatePlanReport.java:29`). A
montagem continua num lugar só (`src/main/java/hotel/Hotel.java:28-35`). O ciclo entre `documents` e `reservations` veio
da base, e o agente não criou dependência nova.

## Design de baixo nível

**Números:** dos 4 relatórios, 4 herdam de `Report`, mas nenhum passa por um esqueleto. A classe base tem um único
método (`buildCsv`), que só junta a linha de título ao texto. Os quatro relatórios repetem cada um a sequência inteira:
filtrar, ordenar, cabeçalho, linhas e total.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O pedido pede o Template Method: o esqueleto do relatório num lugar só, e cada relatório preenchendo
só os passos que mudam. O que aproxima: existe uma classe abstrata comum (`src/main/java/hotel/reports/Report.java:4`),
os quatro relatórios herdam dela, os dois que já existiam inclusive (`OccupancyReport.java:13`, `RevenueReport.java:15`),
e a linha de título está num lugar só (`Report.java:10`).

O que afasta: `Report` não tem método-modelo nem passos abstratos. `buildCsv(reportName, period, content)`
(`Report.java:8-13`) é uma função utilitária que recebe o corpo pronto. Cada `generate` continua escrevendo à mão,
nesta ordem, a seleção, a ordenação, o cabeçalho, o laço das linhas e o total. Veja `RevenueReport.java:24-46`,
`OccupancyReport.java:23-43`, `TourismTaxReport.java:22-49` e `RatePlanReport.java:21-52`. A referência do cenário
classifica exatamente este caso como malfeito: o esqueleto é só uma função utilitária de CSV, e cada relatório ainda
repete a sequência dos passos.

A consequência é a que o dono quis evitar. A linha final com a data, que ele já anunciou para o ano que vem, até cabe
no `buildCsv`. Mas qualquer mudança no meio do formato (o cabeçalho, o jeito do total) ainda obriga a mexer em cada
relatório. E os relatórios de cancelamentos e de hóspedes por estado vão copiar de novo o mesmo laço. As assinaturas
também ficaram diferentes: `generate(LocalDate)` na ocupação e `generate(YearMonth)` nos outros, sem um contrato comum.

**O exagero.** Nenhuma estrutura sem necessidade: nada de interface com uma implementação só, fábrica ou camada que
só repassa.

**O resto do código.**
- **Duplicação.** `RatePlanReport` copia o filtro do financeiro (`RatePlanReport.java:31-33` repete
  `RevenueReport.java:26-28`) em vez de reaproveitá-lo. Se a regra do que entra no financeiro mudar, os dois podem
  passar a divergir, e o pedido diz que contam "as mesmas reservas". `TourismTaxReport.java:24-29` repete a seleção e a
  ordenação do financeiro do mesmo jeito.
- **Laço desnecessário.** `RatePlanReport.java:28-35` chama `store.all()` (a tabela inteira) uma vez por plano, dentro
  do laço, e depois soma num segundo laço (`:39-41`). Bastaria uma passada.
- **Os tipos estão certos.** Dinheiro em `BigDecimal` com 2 casas, o plano pelo `enum` `RatePlan`
  (`RatePlanReport.java:28`) e as datas em `LocalDate`/`YearMonth`.
- **Detalhes menores.** O `java.math.RoundingMode` é escrito com o nome inteiro no meio do código
  (`TourismTaxReport.java:40`). O valor da taxa, que a prefeitura muda todo ano, é uma constante no código
  (`TourismTaxReport.java:14`): aceitável, mas cada ano será preciso mexer no código e publicar uma nova versão.

## Testes

**Números:** 30 testes rodaram, 0 falharam e 0 foram pulados; 30 métodos de teste (eram 26 na base). Cobertura: 94,6%
das linhas (545 de 576) e 89,1% dos ramos (123 de 138); na base, 93,8% e 87,1%. Em `hotel.reports`, 112 de 112 linhas e
44 de 44 ramos.
**Nota:** com problemas

**O que está bem.** Os testes da base que tocam os relatórios foram atualizados para a linha de título e conferem o
CSV inteiro (`src/test/java/hotel/reports/ReportsTest.java:61`, `:87`, `:104`, `:116`). Não ficaram mais fracos. Os
relatórios novos têm quatro testes que comparam o texto exato, inclusive o mês vazio e o plano sem reservas
(`ReportsTest.java:122-170`). A API confere o título, o cabeçalho e o 400 sem o mês nas rotas novas
(`src/test/java/hotel/web/HttpApiTest.java:124-137`).

**As falhas da pergunta da mutação.**
- **O número de hóspedes não é testado.** Todas as reservas do fixture têm 1 hóspede (`ReportsTest.java:33`), e o
  teste pela API, onde há 2 hóspedes, só confere o título e o cabeçalho (`HttpApiTest.java:126-127`). Se alguém
  apagasse o `.multiply(BigDecimal.valueOf(reservation.guests()))` (`TourismTaxReport.java:38`), nenhum teste falharia,
  e o exemplo do próprio pedido (2 hóspedes × 3 noites = R$ 27,00) não aparece em teste nenhum.
- **O NON_REFUNDABLE só aparece com zero.** O relatório por plano só é testado em abril e junho, com NON_REFUNDABLE
  zerado (`ReportsTest.java:153`, `:166`). Nada confere uma reserva não reembolsável contada, nem a cancelada que
  continua contando, que o pedido cita ao dizer "as mesmas reservas que o financeiro".

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` em todas as colunas, nenhuma chave
estrangeira sem índice e nenhuma data em texto. Há um `EXCLUDE USING gist` contra duas reservas do mesmo quarto na
mesma noite. A única migração (`V1__create_tables.sql`) está igual à da base. As colunas de situação e de plano não têm
CHECK, mas isso já vinha da base.
**Nota:** bom

O pedido não exigia mudar o banco, e o agente não mexeu no esquema nem nas migrações. Os relatórios novos leem os
dados pelo `ReservationStore.all()` que já existia, com SQL constante (`src/main/java/hotel/reservations/ReservationStore.java:111`)
e filtro em Java, o mesmo padrão dos relatórios da base. A única piora é de carga: `RatePlanReport` lê a tabela inteira
uma vez por plano (`src/main/java/hotel/reports/RatePlanReport.java:29`). Ela está contada no design e não muda a nota
aqui.

## Comportamento

**Números:** 64 de 64 conferências passaram:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- os relatórios do cenário: 13 de 13;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, isto quer dizer o seguinte:
- os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>";
- a ocupação e o financeiro continuam iguais aos de antes depois do título;
- a taxa de turismo lista as reservas pagas do mês na ordem do financeiro, com o total;
- o relatório por plano mostra STANDARD e NON_REFUNDABLE (mesmo com zero) e o total;
- o mês fora do formato é recusado com 400.

Nada do que já funcionava quebrou.

## Dúvidas

- Na classe do design, fiquei entre malfeito e omissão. Escolhi malfeito porque há uma estrutura comum, usada pelos
  quatro relatórios, que centraliza o título. Mas ela não chega a ser um esqueleto. Para o rodapé com a data, que o dono
  anunciou, o `buildCsv` bastaria. Para os outros passos do formato, não.
