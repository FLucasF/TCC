# Revisão: manutenção, cenário 09

## Visão geral

O projeto é o da base com a mudança pedida: os pacotes `hotel.web` (as rotas no Javalin, `HttpApi`), `hotel.reservations`
(as regras, `ReservationService`, e o acesso ao banco, `ReservationStore`), `hotel.rooms`, `hotel.messages`,
`hotel.documents` e `hotel.reports`, com tudo montado em `hotel/Hotel.java`. Uma requisição de relatório chega numa rota
de `HttpApi` (`web/HttpApi.java:91-106`), que lê o mês e chama o `generate` do relatório. O relatório lê todas as
reservas por `ReservationStore.all()` e filtra em Java. Os dados ficam no PostgreSQL, num esquema criado pelo Flyway, e
o agente não mexeu nele. O agente criou dois relatórios (`TourismTaxReport`, `RatePlansReport`) e um `CsvReport` que
escreve o título, o cabeçalho, as linhas e o total, e mudou os dois relatórios antigos para usá-lo. Tudo funciona (64/64
no roteiro, 31 testes passando). O ponto mais fraco é o pattern: o `CsvReport` só junta textos que já vêm prontos, e cada
um dos quatro relatórios continua repetindo a sequência inteira (escolher, ordenar, escrever as linhas, somar o total).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Um ciclo, `hotel.documents` ↔
`hotel.reservations`, que já estava na base. `hotel.reports` passou de 2 para 6 classes (de 95 para 242 linhas).
`HttpApi` passou de 170 para 186 linhas.
**Nota:** bom

A mudança cabe nas camadas que a base já tinha. As duas rotas novas são finas: leem o mês, chamam o relatório e
escrevem o CSV (`web/HttpApi.java:99-106`), e reaproveitam o `readMonth` da base. Os relatórios novos não conhecem o
Javalin nem o JDBC: leem pelo `ReservationStore` (`reports/TourismTaxReport.java:27`, `reports/RatePlansReport.java:31`),
e a medida não mostra biblioteca nenhuma em `hotel.reports`. A montagem continua num lugar só, `Hotel.java:28-35`.
Nenhuma dependência ou ciclo novo; o ciclo `documents`/`reservations` veio da base. O construtor de `HttpApi` ganhou
mais dois parâmetros (`web/HttpApi.java:37-44`) e vai ganhar mais um a cada relatório, mas num sistema deste tamanho
isso ainda não pesa.

## Design de baixo nível

**Números:** os 4 relatórios passam pelo `CsvReport` (título, cabeçalho, linhas, total), e nenhum escreve o título por
conta própria. Mas os 4 continuam com os passos "escolher as reservas, ordenar, montar as linhas num `StringBuilder`,
somar o total" escritos inteiros, cada um no seu `generate`: 0 de 4 têm esses passos dentro da estrutura.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

O pattern. O que aproxima: existe um lugar só para a forma do relatório. `reports/CsvReport.java:17-24` escreve o título
`Pousada Maré Alta,<nome>,<período>`, o cabeçalho, as linhas e o total. Os quatro relatórios terminam chamando esse
lugar (`OccupancyReport.java:45`, `RevenueReport.java:48`, `TourismTaxReport.java:54`, `RatePlansReport.java:57`), os
dois que já existiam inclusive. A linha de data que o dono quer para o ano que vem entraria num lugar só.

O que afasta: o "esqueleto" é uma função que recebe três `String` já montadas (`header`, `dataLines`, `totalLine`). Ela
não chama passo nenhum: não pede ao relatório as reservas, a ordem, a linha de cada reserva nem o total. Por isso cada
relatório repete a sequência do jeito antigo. A seleção é um laço sobre `store.all()` com um `if`
(`RevenueReport.java:24-31`, `TourismTaxReport.java:26-32`, `RatePlansReport.java:31-39`, `OccupancyReport.java:24-30`).
A ordenação vem em seguida (`RevenueReport.java:32`, `TourismTaxReport.java:33`), depois o laço que monta as linhas e
soma o total (`RevenueReport.java:34-44`, `TourismTaxReport.java:35-50`), e por fim os textos soltos do cabeçalho e do
total. O próximo relatório (os cancelamentos, os hóspedes por estado) vai ser mais uma cópia desses passos. É o caso
que a referência chama de malfeito: "o 'esqueleto' é só uma função utilitária de CSV e cada relatório ainda repete a
sequência dos passos". Não é a alternativa por composição que a referência aceita, porque as partes que variam (a
seleção, a linha, o total) não entram como objetos ou funções: entram como texto já pronto.

O exagero: nenhum. O `CsvReport` é a peça do pattern. Ele é uma instância com dois campos que só serve para uma chamada
(`CsvReport.java:9-15`), um método estático teria bastado, mas isso não chega a ser estrutura sem necessidade. Não há
interface, fábrica nem camada que só repassa.

O resto do código:
- A regra "as mesmas reservas que o relatório financeiro conta" foi copiada, não compartilhada: o predicado de
  `RatePlansReport.java:32-34` é cópia literal de `RevenueReport.java:26-28`. Quem mudar o que conta como receita num
  dos dois precisa lembrar do outro.
- A ordem "a mesma do financeiro" também foi copiada: o comparador de `TourismTaxReport.java:33` é o de
  `RevenueReport.java:32`.
- Os tipos estão certos: `BigDecimal` com 2 casas para a taxa (`TourismTaxReport.java:17`, `:38-41`) e para os
  somatórios, `YearMonth`, e `EnumMap` com `RatePlan.values()` para garantir as duas linhas, mesmo vazias
  (`RatePlansReport.java:26-29`, `:46`).
- A taxa é uma constante num lugar só (`TourismTaxReport.java:17`), o que deixa a troca anual em uma linha.
  `PlanStats` tem campos mutáveis, mas é privado e local (`RatePlansReport.java:60-63`).

São defeitos localizados de duplicação, e por isso a nota é `com problemas`.

## Testes

**Números:** 31 testes rodaram, 0 falhas, 0 erros, 0 pulados; 31 métodos escritos (a base tinha 26, todos mantidos).
Cobertura de linhas 93,6% (545/582; na base, 93,8%) e de ramos 89,0% (121/136; na base, 87,1%). `hotel.reports` está com
118/118 linhas e 42/42 ramos.
**Nota:** bom

O agente pôs a linha de título nos 5 testes de relatório da base, sem enfraquecê-los: eles continuam comparando o CSV
inteiro (`src/test/java/hotel/reports/ReportsTest.java:58-121`). E escreveu 5 testes novos que também comparam o CSV
inteiro, com valores:
- a taxa de abril, com a reserva pendente, a cancelada e a de março de fora (`ReportsTest.java:123-137`);
- a taxa com 3 hóspedes por 3 noites, 40,50 (`ReportsTest.java:139-158`);
- os planos de abril, com `NON_REFUNDABLE,0,0.00` (`ReportsTest.java:160-171`);
- a não reembolsável cancelada contando nos planos (`ReportsTest.java:173-190`);
- um mês vazio, com as duas linhas zeradas (`ReportsTest.java:192-203`).

A pergunta da mutação: trocar 4.50 por outro valor, tirar o `isPaid()` ou o `startsInMonth` da taxa, trocar
`signum() > 0` por `>= 0` nos planos (a cancelada com devolução entraria) ou apagar uma linha de plano faria algum teste
falhar. Ficam sem rede:
- as duas rotas novas, que nenhum teste chama pela API: `HttpApiTest` só chama a ocupação e o financeiro
  (`src/test/java/hotel/web/HttpApiTest.java:113-120`), e as linhas não cobertas de `hotel.web` subiram de 5 para 11;
- a ordem da taxa quando a ordem por data difere da ordem por número (nos testes as duas coincidem);
- uma reserva `CHECKED_OUT` na taxa.

São lacunas pequenas, em código trivial.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)`, nenhuma chave estrangeira está sem índice e nenhuma data está em texto. As colunas de situação, de
plano, de tipo e de canal estão em `text` sem `CHECK`. A reserva duplicada é barrada por um `EXCLUDE USING gist`. A
migração `V1__create_tables.sql` está igual à da base, e não há migração nova.
**Nota:** bom

O pedido não mexe no esquema, e o agente não mexeu: não editou a migração da base nem criou SQL novo. Os relatórios
novos leem pelo `ReservationStore.all()` da base (`reservations/ReservationStore.java:111`, que traz a tabela inteira) e
filtram em Java (`reports/TourismTaxReport.java:27`, `reports/RatePlansReport.java:31`). É o mesmo jeito dos relatórios
da base, agora em quatro lugares. Isso pesa quando a tabela crescer, mas foi o padrão herdado. As colunas de situação
sem `CHECK` e os candidatos a SQL montado com `+` (todos com constantes, `ReservationStore.java:33,83,98,111`) também vêm
da base.

## Comportamento

**Números:** 64 de 64 conferências passaram:
- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- o cenário 09: 13/13;
- as duas reservas ao mesmo tempo: 2/2.
**Nota:** bom

Os quatro relatórios começam com `Pousada Maré Alta,<nome>,<período>`. A ocupação e o financeiro continuam iguais aos de
antes depois do título. A taxa de turismo cobra R$ 4,50 por hóspede por noite das reservas pagas do mês, na ordem do
financeiro, e um mês vazio dá total 0.00. O relatório dos planos traz STANDARD e depois NON_REFUNDABLE, mesmo zerados,
contando a não reembolsável cancelada. Um mês fora do formato é recusado com 400. Para o dono da pousada, tudo o que
foi pedido funciona hoje, e nada do que já funcionava quebrou. O custo está no próximo relatório, não neste: ver
"Design de baixo nível".

## Dúvidas

Fiquei entre `malfeito` e `alternativa equivalente`. O `CsvReport` está nos quatro relatórios e põe num lugar só a forma
da saída, que é o que o dono pediu ("quando o jeito dos relatórios mudar, quero que mude em todos de uma vez"). Uma
linha de data no fim entraria só nele. Mesmo assim, a referência define a composição aceita como um escritor que recebe
as partes que variam (a seleção, a linha, o total) como objetos ou funções. Aqui ele recebe texto pronto, e a sequência
de passos se repete em cada relatório. Fiquei com a classe que a referência descreve mais de perto.
