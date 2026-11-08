# Revisão: manutenção, cenário 09

## Visão geral

A base é um serviço Javalin em 7 pacotes. A rota (`hotel.web.HttpApi`) lê a requisição e chama um serviço ou um relatório. As regras ficam em `hotel.reservations`, e o acesso ao PostgreSQL fica isolado em `ReservationStore`, `Rooms` e `SentMessages`, com o esquema criado pelo Flyway. Tudo é montado em `hotel.Hotel`. O agente criou a classe abstrata `ReportTemplate`, com o método-modelo `final` que escreve o título, o cabeçalho, as linhas e o total. Passou os dois relatórios que já existiam para dentro dela, escreveu os dois novos (`TourismTaxReport` e `RatePlansReport`), ligou as duas rotas novas e acrescentou 4 testes de unidade e asserções no teste da API. O roteiro de comportamento passa inteiro (64 de 64). O defeito mais sério está no jeito como o total chega à última linha. Cada relatório calcula o total em `dataLines` e o guarda num campo da própria instância, que `totalLine` lê logo depois. As instâncias são únicas e servidas por várias threads do Jetty, então dois pedidos ao mesmo tempo misturam os totais. Eu reproduzi isso pela API: em 600 pedidos simultâneos do financeiro de julho e de agosto, 12 respostas saíram com o total do outro mês. Na base, que usava variáveis locais, foram 0 de 600.

## Arquitetura

**Números:** 7 pacotes (os mesmos da base), 17 dependências entre eles (as mesmas da base), 1 ciclo (`hotel.documents` ↔ `hotel.reservations`, que já existia na base, `measures/dependencies-base.md`). `hotel.reports` passou de 2 classes e 95 linhas para 6 classes e 311 linhas; `HttpApi` passou de 170 para 186 linhas.
**Nota:** bom

As mudanças respeitam as camadas da base:
- **Relatórios:** os novos ficam em `hotel.reports`, usam só `ReservationStore` e `Reservation` e não conhecem Javalin nem JDBC (`measures/dependencies.md`, tabela de bibliotecas: `hotel.reports` sem nenhuma).
- **Rotas:** as duas novas são finas. Leem o mês com o `readMonth` que já existia e chamam `generate` (`src/main/java/hotel/web/HttpApi.java:99-106`).
- **Montagem:** continua num lugar só (`src/main/java/hotel/Hotel.java:28-35`).

O agente não criou dependência nova nem ciclo novo. O único ciclo veio da base. O construtor de `HttpApi` cresce com um parâmetro por relatório (`HttpApi.java:37-44`): com mais dois relatórios já anunciados, isso vai continuar crescendo, mas não é defeito neste tamanho.

## Design de baixo nível

**Números:** 4 de 4 relatórios na estrutura (os 2 da base e os 2 novos); 0 relatórios escritos inteiros fora dela; a linha de título num lugar só. Os 4 relatórios guardam o total num campo da instância entre dois passos do modelo. Medido pela API: 12 de 600 respostas simultâneas com o total errado (na base, 0 de 600).
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O agente aplicou o Template Method como a referência descreve:
- `ReportTemplate` é abstrata, com o método-modelo `public final String generate(T period)`, que fixa a ordem título → cabeçalho → linhas → total (`src/main/java/hotel/reports/ReportTemplate.java:11-18`).
- Os passos que variam são abstratos (`ReportTemplate.java:20-26`), e o título, com o nome da pousada, está num lugar só (`ReportTemplate.java:9`, `:28-30`).
- Os quatro relatórios são subclasses, inclusive os dois que já existiam: `OccupancyReport.java:13`, `RevenueReport.java:15`, `TourismTaxReport.java:13` e `RatePlansReport.java:14`.

Uma linha de rodapé com a data, como a que o dono anuncia para o ano que vem, entraria só no `generate`. O que afasta um pouco da referência: o passo `dataLines` junta escolher, ordenar e escrever as linhas. A parte "escolher as reservas e ordenar" não é um passo do esqueleto, e por isso o filtro e a ordenação ficam repetidos nas subclasses.

**O exagero.** Não há estrutura sem necessidade: nada de interface com uma implementação só, fábrica ou camada que só repassa. O tipo genérico `<T>` é usado de verdade (`LocalDate` na ocupação, `YearMonth` nos outros).

**O resto do design.** A nota vem do estado:
- **O defeito sério.** As assinaturas dos passos não deixam `dataLines` entregar o total para `totalLine`. Por isso cada subclasse guarda o total num campo mutável e lê esse campo no passo seguinte:
  - `OccupancyReport.java:53` e `:58-62`;
  - `RevenueReport.java:56` e `:61-65`;
  - `TourismTaxReport.java:60` e `:65-69`;
  - `RatePlansReport.java:64-65` e `:70-75`.

  Cada relatório é uma instância única criada em `Hotel.java:32-35` e compartilhada por todas as threads que atendem os pedidos. Quando dois pedidos se cruzam, um escreve o campo entre o `dataLines` e o `totalLine` do outro. O relatório sai com as linhas de um mês e o total de outro: medi `julho … 935.76 … total,249.90` e `agosto … 249.90 … total,935.76`. É dado errado justamente no relatório do contador e no da prefeitura. Se ninguém chamar `dataLines` antes, o campo também fica `null` (`RevenueReport.java:62` faria `NullPointerException`). Na base esse defeito não existia: o total era uma variável local.
- **Duplicação.** A regra "as mesmas reservas que o financeiro conta" foi copiada, não reaproveitada: `RatePlansReport.java:34-41` repete `RevenueReport.java:35-42`. Se a regra do financeiro mudar, o relatório por plano fica diferente sem ninguém perceber. A ordenação do financeiro também foi copiada (`TourismTaxReport.java:42` = `RevenueReport.java:43`).
- **Planos fixos à mão.** `RatePlansReport.java:44-45` põe os planos no mapa um por um. Um plano novo no `enum` faria o `summaries.get` devolver `null` e o relatório quebrar com `NullPointerException` (`RatePlansReport.java:48-49`), em vez de percorrer `RatePlan.values()`.
- **Menor.** O valor da taxa, que muda todo ano, é uma constante no código (`TourismTaxReport.java:15`): fica num lugar só, mas mudar o valor exige um novo build.

`BigDecimal` com 2 casas e `YearMonth` e `LocalDate` estão certos.

## Testes

**Números:**
- **Final:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados; 30 métodos escritos.
- **Base:** 26 testes, todos passando.
- **Cobertura:** linhas 94,8% (563 de 594) e ramos 89,0% (121 de 136), contra 93,8% e 87,1% na base. `hotel.reports` está com 130 de 130 linhas e 42 de 42 ramos (`measures/tests.md`).

**Nota:** com problemas

**O que está bom.** O agente manteve os 26 testes da base e acrescentou a linha de título em todos os que conferem relatório, sem enfraquecer nenhum (`src/test/java/hotel/reports/ReportsTest.java:63`, `:76`, `:89`, `:106`, `:118`). Os testes novos conferem o CSV inteiro, com valores:
- a taxa de turismo com o filtro das pagas (a pendente e a cancelada ficam de fora, `ReportsTest.java:125-136`);
- o relatório por plano com um plano em zero (`ReportsTest.java:155-165`) e com os dois planos (`ReportsTest.java:168-185`).

Pela API, os quatro títulos e o total da taxa com 2 hóspedes × 2 noites = 18.00 (`src/test/java/hotel/web/HttpApiTest.java:116-129`).

**O que falta:**
- **A não reembolsável cancelada no relatório por plano.** Nenhum teste confere que ela entra nesse relatório. Se alguém trocasse o filtro de `RatePlansReport.java:36` por `isPaid()`, todos os testes continuariam passando. Essa regra é justamente a do "contando as mesmas reservas que o relatório financeiro conta".
- **Hóspedes nos testes de unidade.** O teste `tourismTaxMultipliesGuestsAndNights` usa 1 hóspede (o `book` fixa 1 em `ReportsTest.java:35`), e o comentário de `ReportsTest.java:128` não bate com o nome do teste. A multiplicação por hóspedes só é conferida pelo total da API (`HttpApiTest.java:125`).
- **A ordem do relatório da taxa.** Os ids e as datas de entrada vêm na mesma ordem, então a ordenação de `TourismTaxReport.java:42` não é realmente testada.
- **Pedidos simultâneos.** Nenhum teste chama os relatórios ao mesmo tempo, por isso o defeito do estado compartilhado passou.

## Banco

**Números:**
- **Tabelas:** 3 (`messages`, `reservations`, `rooms`), todas com chave primária.
- **Migração:** 1 do Flyway (`V1__create_tables.sql`), igual à da base.
- **Conferências:** dinheiro todo em `numeric(10,2)`; nenhuma chave estrangeira sem índice; nenhuma data em texto. As colunas de situação e de tipo estão em `text` sem `CHECK`, como já estavam na base. O `EXCLUDE USING gist` impede duas reservas no mesmo quarto e nas mesmas noites (`measures/database.md`).

**Nota:** bom

O pedido não mexe no banco, e o agente não tocou no esquema nem no acesso a dados (`changes.patch` não muda nada em `hotel.reservations` nem em `db/migration`). O SQL montado com `+` só concatena constantes (`src/main/java/hotel/reservations/ReservationStore.java:33`, `:83`, `:98`, `:111`) e veio da base.

Os dois relatórios novos repetem o jeito da base de trazer a tabela inteira (`ReservationStore.java:111`, `SELECT … FROM reservations ORDER BY id`) e filtrar em Java (`TourismTaxReport.java:36`, `RatePlansReport.java:35`). Com a tabela crescendo, isso pesa, mas o padrão veio da base e, num sistema deste tamanho, não pede mudança agora.

## Comportamento

**Números:** 64 de 64 conferências passaram (`measures/behavior.md`):
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 09: 13 de 13;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Pelo roteiro, tudo o que o dono pediu funciona:
- **Título:** os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>".
- **Ocupação e financeiro:** depois do título, continuam como eram.
- **Taxa de turismo:** cobra R$ 4,50 por hóspede por noite das reservas pagas que começam no mês, na ordem do financeiro, inclusive num mês vazio, que sai com o total 0.00.
- **Por plano:** traz STANDARD e depois NON_REFUNDABLE, mesmo sem reserva no mês.
- **Mês inválido:** um mês fora do formato é recusado com 400.

Fora do roteiro, achei o defeito do estado compartilhado, contado no design de baixo nível, e por isso ele não muda esta nota. Para o dono, ele quer dizer o seguinte: se duas pessoas (o contador e a gerência, ou a prefeitura) tirarem relatórios no mesmo instante, um deles pode sair com o total errado, e o total errado é o de outro período. Na medição, isso aconteceu em cerca de 2% dos pedidos simultâneos.

## Dúvidas

Não sei quanto o defeito de estado pesa na prática. Ele só aparece com pedidos simultâneos, que numa pousada pequena são raros. Mesmo assim, ele produz um número errado sem nenhum aviso, nos relatórios que vão para o contador e para a prefeitura, e eu o reproduzi pela API. Por isso dei `ruim` no design, e não `com problemas`. Ele é a consequência do jeito como o esqueleto foi desenhado: os passos não passam o total de um para o outro. Mesmo assim, mantive a classe `acerto`, porque a referência mede se os quatro relatórios passam pelo esqueleto, e passam.
