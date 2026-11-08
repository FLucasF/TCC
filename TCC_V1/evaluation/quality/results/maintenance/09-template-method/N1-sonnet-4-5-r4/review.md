# Revisão: manutenção, cenário 09

## Visão geral

O agente mexeu só no que o pedido tocava. Criou uma classe abstrata `hotel.reports.Report` com o esqueleto do CSV (título, cabeçalho, linhas, total) e fez os quatro relatórios herdarem dela: os dois que já existiam (`OccupancyReport`, `RevenueReport`) e os dois novos (`TourismTaxReport`, `RatePlansReport`). Também ligou as duas rotas novas em `HttpApi` e as duas classes novas em `Hotel`. Uma requisição segue como na base: a rota Javalin lê o parâmetro (`web/HttpApi.java:99-106`), chama `generate(month)` do relatório, e o relatório lê todas as reservas por `ReservationStore.all()` (JDBC/PostgreSQL) e filtra em Java. Esquema, migração e camadas ficaram como estavam. O roteiro passa inteiro (64/64) e o `mvn verify` passa (30 testes). O problema mais sério vem do jeito como o período chega ao esqueleto: cada relatório guarda o mês ou a noite num campo mutável (`RevenueReport.java:18`, `:24-26`, e o mesmo nos outros três). Cada relatório é um objeto único que todas as requisições dividem (`Hotel.java:32-35`). Na base os relatórios não guardavam estado. Com o serviço de pé, pedidos simultâneos de `/reports/revenue` para julho e agosto devolveram o relatório errado em 454 de 900 respostas (seis por vez) e em 42 de 100 (três por vez). Em 9 dessas o título dizia um mês e as linhas eram de outro. O defeito pega também os dois relatórios que já funcionavam. Há ainda duplicação nova em todos os relatórios: a seleção das reservas roda duas vezes, uma para as linhas e outra para o total.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, igual à base. Há 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base. O pacote `hotel.reports` passou de 2 para 6 classes e de 95 para 376 linhas; `HttpApi` passou de 170 para 186 linhas. `hotel.reports` continua sem Javalin e sem JDBC.
**Nota:** bom

- **Camadas:** a mudança respeitou as camadas da base. As rotas novas só leem o mês, chamam o relatório e escrevem o CSV (`web/HttpApi.java:99-106`). Os relatórios dependem só de `ReservationStore` e `Rooms` (`reports/TourismTaxReport.java:3-4`, `reports/RatePlansReport.java:3-5`), sem SQL e sem `Context`.
- **Montagem:** continua num lugar só (`Hotel.java:28-35`).
- **O que não é do agente:** o ciclo `documents`/`reservations` veio da base, e o agente não o piorou.
- **Tamanho:** nenhuma classe ficou desproporcional. O que cresceu foi o construtor de `HttpApi`, que já recebe sete dependências (`web/HttpApi.java:37-44`), um relatório por parâmetro. O próximo relatório vai mexer em `Hotel` e `HttpApi` de novo, mas isso é aceitável neste tamanho.

## Design de baixo nível

**Números:** 4 de 4 relatórios estão na estrutura (os 2 da base e os 2 novos); nenhum monta a linha de título por conta própria. O esqueleto fica num lugar só (`reports/Report.java:22-29`), com 5 passos abstratos (`Report.java:12-20`). Os 4 relatórios guardam o período num campo mutável de um objeto compartilhado. No teste de concorrência, 454 de 900 e 42 de 100 respostas vieram erradas. Há 4 trechos de lógica duplicados dentro dos relatórios.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** É o Template Method que a referência descreve:

- `Report` tem o método-modelo `generate()`, que escreve título, cabeçalho, linhas e total (`Report.java:22-29`). O nome da pousada está numa constante só (`Report.java:10`).
- As quatro subclasses preenchem só o que muda: `reportName`, `period`, `header`, `dataLines` e `totalLine`.
- Os dois relatórios antigos foram levados para a estrutura (`OccupancyReport.java:13`, `RevenueReport.java:15`). A linha de data que o dono quer para o ano que vem entra em um lugar só.

O que afasta um pouco do ideal: o método-modelo não é `final` e é `public` sem argumento (`Report.java:22`). Uma subclasse pode reescrever o esqueleto, e quem chamar `generate()` direto, sem ter passado pelo `generate(month)`, recebe um `NullPointerException` em `period()` (`RevenueReport.java:36`).

**O exagero.** Não há estrutura sobrando: nenhuma interface de uma implementação só, nenhuma fábrica, nenhum builder. `PlanStats` (`RatePlansReport.java:92-95`) é um acumulador local, não exagero.

**Por que a nota é `ruim`.**

1. **Estado mutável compartilhado, e é o mais sério.** Para levar o período até os passos, cada relatório grava o parâmetro num campo e depois chama o esqueleto:
   - `OccupancyReport.java:17` e `:24-27`;
   - `RevenueReport.java:18` e `:24-26`;
   - `TourismTaxReport.java:18` e `:24-26`;
   - `RatePlansReport.java:17` e `:23-26`.

   Como há uma instância de cada relatório para o servidor inteiro (`Hotel.java:32-35`, usadas em `HttpApi.java:91-106`), duas requisições ao mesmo tempo sobrescrevem o mês uma da outra. Conferi com o serviço de pé, contra um banco só meu, com uma reserva paga em julho e outra em agosto:
   - em 150 rodadas de seis pedidos simultâneos (`revenue` de julho e de agosto), vieram 454 respostas erradas em 900;
   - em 50 rodadas de três pedidos, vieram 42 erradas em 100, e 9 com o título de um mês e as linhas de outro (por exemplo, o título `Pousada Maré Alta,revenue,2027-08` em cima da reserva de julho).

   Na base, `generate(month)` usava só variáveis locais (`base/src/main/java/hotel/reports/RevenueReport.java:23`). O defeito é do agente e quebra os relatórios que já funcionavam, justamente os que vão para o contador e para a prefeitura. O jeito sem estado seria passar o período como argumento dos passos, ou criar um objeto de relatório por pedido.

2. **A seleção roda duas vezes.** Em cada relatório, `dataLines()` e `totalLine()` filtram as reservas de novo:
   - `OccupancyReport.java:46-52` e `:69-75` são o mesmo laço copiado;
   - `RevenueReport.java:46` e `:62`;
   - `RatePlansReport.java:45` e `:59` calculam `calculateStats()` duas vezes;
   - o cálculo da taxa está copiado em `TourismTaxReport.java:50-53` e `:70-73`.

   Além da cópia, são duas leituras da tabela por relatório. Se uma reserva mudar entre as duas, o total deixa de bater com as linhas. O esqueleto deveria escolher as reservas uma vez e passá-las para as linhas e para o total.

3. **A regra do financeiro está copiada.** "As mesmas reservas que o relatório financeiro conta" virou uma cópia do filtro: `RatePlansReport.java:74-81` repete `RevenueReport.java:71-78`. Se a regra do financeiro mudar, o relatório por plano fica para trás sem ninguém notar.

4. **Os planos estão escritos à mão.** As duas linhas são montadas uma a uma (`RatePlansReport.java:48-53`), em vez de percorrer `RatePlan.values()`. Um terceiro plano não apareceria no relatório.

O valor da taxa ficou numa constante `BigDecimal` (`TourismTaxReport.java:15`), um lugar só para a troca anual. Isso está bem.

## Testes

**Números:** 30 testes rodaram, sem falhas, erros nem pulados. Na base eram 26: o agente acrescentou 4 e não apagou nenhum. Há 30 métodos de teste no código. A cobertura total é de 94,1% das linhas (588/625) e 90,0% dos ramos (135/150); na base era 93,8% e 87,1%. O pacote `hotel.reports` tem 161/161 linhas e 56/56 ramos.
**Nota:** com problemas

**O que está bem.**

- Os 5 testes antigos de relatório foram atualizados só com a linha de título (`ReportsTest.java:57-120`), sem enfraquecer nenhuma asserção.
- Os 4 testes novos comparam o CSV inteiro, com valores:
  - a taxa de abril tira a reserva que começou em março, a nunca paga e a cancelada, e mantém a ordem do financeiro (`ReportsTest.java:123-133`);
  - o plano sem reserva aparece com 0 (`ReportsTest.java:152-162`);
  - os dois planos com valores (`ReportsTest.java:165-180`).

**As lacunas.** Fazendo a pergunta da mutação nas regras novas:

- **Hóspedes da taxa.** O `book` de teste cria toda reserva com 1 hóspede (`ReportsTest.java:33`), e o teste que se chama `tourismTaxCountsGuestsAndNights` também usa 1 hóspede (`ReportsTest.java:136-149`). Se alguém tirar a multiplicação por hóspedes em `TourismTaxReport.java:52`, todos os testes continuam passando. É justamente o exemplo do pedido (2 hóspedes por 3 noites dão R$ 27,00).
- **A não reembolsável cancelada.** Nenhum teste do relatório por plano tem uma `NON_REFUNDABLE` cancelada. Trocar o filtro `amountKept() > 0` por `isPaid()` em `RatePlansReport.java:76` passaria em todos os testes, e o relatório deixaria de contar "as mesmas reservas que o financeiro".
- **As rotas novas.** Nenhum teste chama `/reports/tourism-tax` nem `/reports/rate-plans` pela API. `HttpApiTest.java:108-121` ficou como na base e só confere o fim do CSV (`endsWith`), então também não vê o título.
- **Concorrência.** Nenhum teste pega o estado compartilhado dos relatórios, porque todos criam um relatório novo por chamada.

## Banco

**Números:** 3 tabelas, 1 migração Flyway (`V1__create_tables.sql`), igual à da base. Nenhuma tabela está sem chave primária. O dinheiro está todo em `numeric(10,2)`. Nenhuma chave estrangeira está sem índice. Não há datas em texto. Há um `EXCLUDE` contra duas reservas no mesmo quarto e noite. As colunas de situação, plano, tipo e canal estão em `text` sem `CHECK` (vem da base). O SQL montado com `+` usa só constantes (`ReservationStore.java:33`, `:83`, `:98`, `:111`).
**Nota:** bom

- **O que mudou:** nada no banco. A mudança não pedia esquema novo, e o agente não criou nem editou migração.
- **Tipos:** o dinheiro dos relatórios novos é somado em `BigDecimal` com 2 casas (`TourismTaxReport.java:68`, `RatePlansReport.java:61`). Os valores lidos do banco vêm de colunas `numeric(10,2)`.
- **Carga:** os relatórios trazem a tabela inteira por `store.all()` (`ReservationStore.java:111`) e filtram em Java. Isso vem da base. Mas o agente passou a chamar `store.all()` duas vezes por relatório (`RevenueReport.java:46` e `:62`, e o mesmo nos outros três), dobrando a leitura. O defeito está contado no design; aqui é só uma observação sobre a carga, que não derruba a coluna.

## Comportamento

**Números:** 64 de 64 conferências passaram: 46/46 na parte comum, 3/3 depois de reiniciar, 13/13 no cenário 09 (os relatórios) e 2/2 nas reservas simultâneas.
**Nota:** bom

Pela API, o sistema faz o que o dono pediu:

- os quatro relatórios começam com `Pousada Maré Alta,<nome>,<período>`;
- a ocupação e o financeiro continuam iguais depois do título;
- a taxa de turismo dá R$ 27,00 para 2 hóspedes em 3 noites (conferi também à mão: `1,101,2027-07-10,2027-07-13,2,3,27.00`);
- o relatório por plano mostra `STANDARD` e `NON_REFUNDABLE` mesmo sem reserva;
- o mês fora do formato dá 400.

Fora do roteiro, achei que pedidos simultâneos de relatório devolvem o mês errado. A causa é o estado mutável compartilhado, um defeito de design: ele pesa na coluna do design e não muda esta nota. Para o dono, isso quer dizer que, se duas pessoas tirarem relatórios ao mesmo tempo (o contador e a gerência, ou o site e a recepção), uma delas pode receber o relatório de outro mês, ou um com o título de um mês e os números de outro. Pedidos feitos um de cada vez saem certos.

## Dúvidas

- **A classe do pattern.** Dei `acerto` porque os quatro relatórios passam pelo esqueleto e nenhum repete a sequência dos passos. Mas o período chega aos passos por um campo mutável, e o esqueleto não é `final`. Um revisor mais estrito poderia ver isso como estrutura malfeita; pelos critérios da referência, que falam de casos fora da estrutura, continua `acerto`.
- **Onde pesa a corrida.** O defeito de concorrência aparece em chamadas que o texto descreve, mas o roteiro não testa relatórios simultâneos. Contei a corrida só no design, como a regra de uma coluna por defeito pede. Se ela pesasse no comportamento, a nota de lá seria `com problemas`.
