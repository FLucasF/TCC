# Revisão: manutenção, cenário 09

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel` (montagem), `hotel.web` (HTTP), `hotel.reservations` (regras e acesso ao banco pelo `ReservationStore`), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`. Os dados ficam no PostgreSQL, com o esquema criado por uma migração Flyway. Uma requisição de relatório vai da rota em `HttpApi` (`src/main/java/hotel/web/HttpApi.java:91-106`) para a classe do relatório, que lê `store.all()`, filtra e monta o CSV em Java. O agente criou dois relatórios novos (`TourismTaxReport` e `RatePlansReport`), as duas rotas, e uma função estática `Report.withTitle` que põe a linha de título na frente do texto de cada relatório. Os quatro relatórios saem certos pela API (64 de 64 conferências) e o `mvn verify` passa (31 testes). O ponto mais sério é o design. O pedido diz que vêm mais relatórios e que "quando o jeito dos relatórios mudar, quero que mude em todos de uma vez". Mesmo assim, cada relatório continua escrevendo inteiro a sua sequência de passos (escolher, ordenar, cabeçalho, linhas, total). Só o título foi para um lugar comum, e a regra de seleção do financeiro foi copiada no relatório por plano.

## Arquitetura

**Números:** 7 pacotes (iguais aos da base), 17 dependências entre pacotes (iguais às da base). Um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. `hotel.reports` passou de 2 para 6 classes (95 → 233 linhas). A maior classe continua sendo `HttpApi`, que foi de 170 para 186 linhas.
**Nota:** bom

As peças novas entram no padrão da base. As rotas novas só leem o mês, chamam o relatório e escrevem o CSV (`src/main/java/hotel/web/HttpApi.java:99-106`). Os relatórios não conhecem o Javalin nem o JDBC: leem pelo `ReservationStore` (`src/main/java/hotel/reports/TourismTaxReport.java:28`, `src/main/java/hotel/reports/RatePlansReport.java:27`). A montagem continua num lugar só, em `src/main/java/hotel/Hotel.java:28-35`, onde os dois relatórios novos são criados e passados ao `HttpApi`. Não apareceu dependência nova nem ciclo novo. O ciclo documents/reservations vem da base. O construtor de `HttpApi` cresce um parâmetro por relatório (`HttpApi.java:37-45`), mas isso continua o jeito da base e não chega a ser defeito neste tamanho.

## Design de baixo nível

**Números:** 4 relatórios, e os 4 chamam `Report.withTitle`. Nenhum dos 4 passa por um esqueleto comum: cada um ainda escreve a sua sequência de seleção, ordenação, cabeçalho, linhas e total (`OccupancyReport.java:23-44`, `RevenueReport.java:23-48`, `TourismTaxReport.java:26-57`, `RatePlansReport.java:22-57`). A regra de seleção do financeiro está copiada em 1 lugar e a ordenação do financeiro em 1 lugar.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O pedido descreve quatro relatórios com os mesmos passos e anuncia mais dois. Também pede que uma mudança de formato, como o rodapé com a data do ano que vem, "mude em todos de uma vez". A estrutura criada é só uma função utilitária: `Report.withTitle(reportName, period, content)` (`src/main/java/hotel/reports/Report.java:11-16`), que concatena a linha de título com um texto que cada relatório já montou inteiro. Não existe método-modelo nem passos abstratos, e não existe um escritor genérico que receba as partes que variam. Cada classe repete o mesmo roteiro com o seu `StringBuilder`: cabeçalho, laço de linhas e `content.append("total,")` (`RevenueReport.java:34-46`, `TourismTaxReport.java:37-54`, `RatePlansReport.java:37-54`, `OccupancyReport.java:33-43`). A referência descreve exatamente este caso como malfeito: o "esqueleto" é só uma função utilitária de CSV e cada relatório ainda repete a sequência dos passos. O que aproxima: os quatro relatórios passam pela mesma função de título, os dois antigos inclusive, e o nome da pousada fica num lugar só (`Report.java:9`). O que afasta: o rodapé do ano que vem não tem onde morar. Ele vai exigir outro método utilitário e quatro chamadas novas, ou um `withTitle` que muda de sentido. O relatório de cancelamentos e o de hóspedes por estado vão nascer copiando a sequência de novo.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface, fábrica ou camada a mais. `PlanStats` (`RatePlansReport.java:59-62`) é um acumulador privado e pequeno.

**O resto do código.**
- A regra "as mesmas reservas que o relatório financeiro conta" foi copiada, não reaproveitada. `RatePlansReport.java:28-30` repete `amountKept().signum() > 0 && startsInMonth` de `RevenueReport.java:26-28`. Se a regra do financeiro mudar, os dois relatórios vão divergir em silêncio.
- A ordenação do financeiro também foi copiada: `TourismTaxReport.java:34` repete `RevenueReport.java:32`.
- O relatório por plano escreve cada plano à mão (`RatePlansReport.java:24-25` e `40-48`) e soma o total plano a plano (`50-51`). Bastava percorrer `RatePlan.values()`. Um terceiro plano no enum ficaria fora do relatório, e o `stats.get(...)` da linha 31 daria `NullPointerException` para ele.
- O que está certo: `BigDecimal` com 2 casas para a taxa e os valores (`TourismTaxReport.java:18`, `41-44`), `YearMonth` e `LocalDate` para o período, e a taxa numa constante com nome. Pelo pedido ("a prefeitura muda o valor da taxa todo ano"), seria melhor tirá-la do código, mas a constante já deixa a mudança num ponto só.

## Testes

**Números:** 31 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 31 métodos escritos; a base tinha 26, e o agente acrescentou 5 em `ReportsTest` sem apagar nenhum. Cobertura de linhas: 94,5% (549 de 581; na base, 93,8%). Cobertura de ramos: 88,6% (117 de 132; na base, 87,1%). `hotel.reports`: 116 de 117 linhas e 38 de 38 ramos.
**Nota:** com problemas

O que está bom: os testes antigos dos relatórios foram atualizados com o título e continuam conferindo o CSV inteiro por igualdade (`src/test/java/hotel/reports/ReportsTest.java:60-118`). Os novos também conferem o CSV inteiro com valores, contra o banco de verdade:
- a taxa só conta as reservas pagas que começam no mês (`ReportsTest.java:137-153`);
- o relatório por plano mostra os dois planos mesmo com um deles vazio (`ReportsTest.java:169-183`);
- a NON_REFUNDABLE cancelada continua contando e a STANDARD reembolsada sai (`ReportsTest.java:186-205`).

O que falta, pela pergunta da mutação:
- **A multiplicação por hóspedes da taxa de turismo não tem rede.** Todas as reservas que entram na taxa nos testes têm 1 hóspede: o `book` usa `guests = 1` (`ReportsTest.java:33`). A única com 3 hóspedes fica pendente e não entra no relatório (`ReportsTest.java:140-141`). Se alguém apagasse `.multiply(BigDecimal.valueOf(reservation.guests()))` (`TourismTaxReport.java:42`), nenhum teste falharia. O comentário "2 guests x 2 nights = 18.00" (`ReportsTest.java:126`) não corresponde ao que o teste confere (1 hóspede, 9.00). Isso deixa sem teste justamente o exemplo que o dono deu (2 hóspedes por 3 noites dão R$ 27,00).
- Pela API, os relatórios novos só são conferidos por `startsWith` e `contains` do título e do cabeçalho, sem nenhum valor (`src/test/java/hotel/web/HttpApiTest.java:123-136`). Os valores ficam só nos testes de unidade, o que é aceitável.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`. Nenhuma chave estrangeira sem índice e nenhuma data em texto. Há um `EXCLUDE` que impede duas reservas do mesmo quarto na mesma noite. A migração `V1__create_tables.sql` está igual à da base, e não há migração nova. As conferências acusaram as colunas de situação e de tipo sem `CHECK` (`reservations.status`, `reservations.rate_plan`, `rooms.type`, `messages.channel`), que vêm da base.
**Nota:** bom

O pedido não mexe no esquema, e o agente não mexeu no banco: nenhuma migração editada e nenhum SQL novo. Os relatórios novos leem pelo `ReservationStore` que já existia. As colunas sem `CHECK` são da base. Os candidatos de SQL montado com `+` (`src/main/java/hotel/reservations/ReservationStore.java:33,83,98,111`) concatenam só constantes, e também são da base. Uma observação de carga: os dois relatórios novos fazem como os da base e trazem a tabela inteira de reservas (`store.all()`, `ReservationStore.java:111`) para filtrar o mês em Java (`TourismTaxReport.java:28`, `RatePlansReport.java:27`). O agente repetiu um padrão da base. Isso não estraga dado e não muda a nota.

## Comportamento

**Números:** 64 de 64 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 09: 13 de 13;
- as reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>";
- a ocupação e o financeiro continuam iguais depois do título;
- a taxa de turismo lista as reservas pagas do mês, na ordem do financeiro, com o total (também num mês vazio);
- o relatório por plano mostra STANDARD e NON_REFUNDABLE (mesmo com 0) e o total;
- o mês fora do formato é recusado com 400.

Tudo o que já funcionava continua funcionando, inclusive os dados depois de reiniciar e a recusa da segunda reserva simultânea.

## Dúvidas

- A classe do pattern ficou entre `malfeito` e `omissão`. Os quatro relatórios passam por uma estrutura comum, mas ela é só a linha de título; o resto da sequência está escrito inteiro em cada um. A referência põe o caso da "função utilitária de CSV com cada relatório repetindo a sequência" em malfeito, e foi essa a classe dada.
- O roteiro do comportamento não deixa claro se ele confere a taxa de uma reserva com mais de um hóspede. Pela leitura, `TourismTaxReport.java:41-44` multiplica certo (2 hóspedes × 3 noites × 4,50 = 27,00), mas os testes do agente não conferem isso.
