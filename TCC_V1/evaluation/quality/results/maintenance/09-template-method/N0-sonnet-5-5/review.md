# Revisão: manutenção, cenário 09

## Visão geral

A base já era um serviço Javalin em pacotes por assunto: `hotel.web` (as rotas, em `HttpApi`), `hotel.reservations`
(as regras em `ReservationService` e o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages`,
`hotel.documents` e `hotel.reports`, tudo montado em `hotel.Hotel`, com o PostgreSQL criado por uma migração do
Flyway. Uma requisição de relatório passa por uma rota fina (`src/main/java/hotel/web/HttpApi.java:91-106`), que chama
o `generate` do relatório, que lê `store.all()` e monta o CSV. O agente mexeu só nos relatórios: criou
`CsvLayout.render` (`src/main/java/hotel/reports/CsvLayout.java:16-25`), uma função estática que escreve título,
cabeçalho, linhas e total; passou os dois relatórios da base a usá-la; criou `TourismTaxReport` e `RatePlanReport`;
ligou as duas rotas novas; e deixou a taxa configurável por variável de ambiente (`src/main/java/hotel/Hotel.java:55-57`).
Os 64 pontos do roteiro passam, e os 28 testes também. O ponto mais fraco é o design: o "esqueleto" comum é só a
escrita do texto. Cada um dos quatro relatórios ainda repete a sequência inteira (escolher, ordenar, montar as
linhas, somar o total), e a seleção e a ordem do financeiro foram copiadas no relatório da taxa. Além disso, os
testes novos não pegam os erros mais prováveis nas regras novas.

## Arquitetura

**Números:** 7 pacotes (eram 7 na base), 17 dependências entre eles (as mesmas da base), 1 ciclo
(`hotel.documents` ↔ `hotel.reservations`), que veio da base. `hotel.reports` passou de 2 classes e 95 linhas para
5 classes e 211 linhas, e continua sem Javalin e sem JDBC. `HttpApi` cresceu de 170 para 186 linhas.
**Nota:** bom

As rotas novas só leem o mês e devolvem o CSV (`src/main/java/hotel/web/HttpApi.java:99-106`), como as da base. Os
relatórios novos não conhecem o Javalin nem o SQL: leem as reservas por `ReservationStore.all()`
(`src/main/java/hotel/reports/TourismTaxReport.java:28`, e `RatePlanReport` pelo financeiro). A montagem continua
num lugar só (`src/main/java/hotel/Hotel.java:28-39`). A taxa entra pelo construtor
(`src/main/java/hotel/reports/TourismTaxReport.java:21-24`) e a lê o `main` (`src/main/java/hotel/Hotel.java:55-57`),
que é o lugar certo. O agente não acrescentou nenhuma dependência nova entre pacotes nem nenhum ciclo novo. O único
ciclo (`documents` ↔ `reservations`) já existia na base, e o agente não o tocou.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam por `CsvLayout.render` para escrever título, cabeçalho, linhas e total. Os
4 de 4 ainda fazem por conta própria a sequência dos passos: escolher as reservas, ordenar, montar as linhas num
laço e somar o total. A seleção e a ordem do financeiro aparecem duas vezes (`RevenueReport.java:24-35` e
`TourismTaxReport.java:27-33`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

O que o texto pede é o Template Method. Os quatro relatórios seguem os mesmos passos, o contador vai pedir mais, e "quando o
jeito dos relatórios mudar, quero que mude em todos de uma vez". O que aproxima: a forma da saída ficou num lugar
só. `CsvLayout.render` (`src/main/java/hotel/reports/CsvLayout.java:16-25`) escreve título, cabeçalho, linhas e total,
os quatro relatórios a chamam (`OccupancyReport.java:39-40`, `RevenueReport.java:46-47`,
`TourismTaxReport.java:45-46`, `RatePlanReport.java:39-40`) e os dois da base também foram migrados. A linha de
data do ano que vem entraria ali, uma vez só. O que afasta: isso não é um método-modelo nem um escritor genérico
que recebe as partes que variam (a seleção, a linha, o total) como funções. É uma função utilitária que recebe o
texto pronto, e cada relatório continua dono do algoritmo inteiro. Veja `OccupancyReport.java:23-40`,
`RevenueReport.java:37-47`, `TourismTaxReport.java:26-46` e `RatePlanReport.java:20-40`: os quatro têm o mesmo laço
de seleção sobre `store.all()`, o `sort`, o laço que monta as linhas e o acumulador do total. A referência do pattern
chama isso de malfeito: "o 'esqueleto' é só uma função utilitária de CSV e cada relatório ainda repete a sequência
dos passos". O relatório de cancelamentos e o de hóspedes por estado vão nascer copiando um desses quatro métodos.

Exagero: nenhum. Não há interface sem uso, fábrica nem camada que só repassa. O segundo construtor de `Hotel`
(`src/main/java/hotel/Hotel.java:24-26`) só mantém a assinatura antiga para os testes.

O resto do código novo:
- **Duplicação.** A seleção das reservas pagas do mês e a ordem do financeiro foram copiadas em
  `TourismTaxReport.java:27-33`. O comparador é o mesmo de `RevenueReport.java:33`, embora o texto diga que a taxa
  segue "a mesma ordem do relatório financeiro". Se essa ordem mudar num lugar, os dois relatórios se separam.
- **Acoplamento entre relatórios.** `RatePlanReport` reaproveita as reservas do financeiro por um método `static`
  de outro relatório (`RatePlanReport.java:21` → `RevenueReport.java:24`). Isso garante que os dois contem as mesmas
  reservas, mas deixa a regra presa dentro de uma classe irmã, e não num lugar próprio.
- **Planos fixos à mão.** `RatePlanReport.java:26` lista `List.of(RatePlan.STANDARD, RatePlan.NON_REFUNDABLE)` em vez
  de `RatePlan.values()`. Um plano novo sumiria do relatório e do total sem aviso, e o total deixaria de bater com o
  financeiro.
- **O que está bem feito.** O dinheiro é `BigDecimal` com 2 casas (`TourismTaxReport.java:39`, `:49-51`). A taxa é
  um parâmetro, e não uma constante enterrada (`TourismTaxReport.java:16`, `:21-24`). Os métodos são curtos e os nomes
  são claros. As quatro rotas quase iguais de `HttpApi.java:91-106` são uma repetição pequena.

## Testes

**Números:** 28 testes rodaram, 0 falharam, 0 foram pulados, e há 28 métodos escritos. A base tinha 26, e o agente
acrescentou 2. Cobertura: 92.9% das linhas (523 de 563) e 88.6% dos ramos (117 de 132); na base, 93.8% e 87.1%.
`hotel.reports` tem 94 de 94 linhas e 38 de 38 ramos.
**Nota:** com problemas

O agente manteve os 26 testes da base passando e os ajustou para a linha de título, sem afrouxar nenhum. Cada
asserção de relatório continua comparando o CSV inteiro (`src/test/java/hotel/reports/ReportsTest.java:60-147`). Os
dois testes novos também comparam o CSV inteiro, mas os dados escolhidos deixam regras novas sem rede:

- **Taxa de turismo** (`ReportsTest.java:123-135`): uma reserva só, com **1 hóspede** e 3 noites. Se alguém tirasse
  os hóspedes da conta (`TourismTaxReport.java:39`), o teste continuaria dando 13.50. O mês de maio não tem reserva
  pendente nem cancelada. Por isso, apagar o filtro `isPaid()` (`TourismTaxReport.java:29`) também não seria pego, e
  a ordem não é conferida, porque há uma linha só. O exemplo do próprio texto (2 hóspedes, 3 noites, 27.00) não está
  em nenhum teste.
- **Planos** (`ReportsTest.java:138-148`): abril não tem nenhuma reserva NON_REFUNDABLE que comece no mês. A linha
  NON_REFUNDABLE só é conferida zerada, e a regra "contar as mesmas reservas do financeiro, inclusive a não
  reembolsável cancelada" fica sem teste.
- **Rotas novas:** nenhum teste chama `/reports/tourism-tax` nem `/reports/rate-plans`. O `HttpApiTest` só chama as
  duas rotas da base (`src/test/java/hotel/web/HttpApiTest.java:113-120`), e a leitura da variável
  `TOURISM_TAX_PER_GUEST_NIGHT` também não é testada.

A cobertura de 100% de `hotel.reports` mostra que o código roda, não que as regras estão conferidas.

## Banco

**Números:** 3 tabelas, 1 migração (`V1__create_tables.sql`, igual à da base), e nenhuma migração nova. Nenhuma
tabela sem chave primária. O dinheiro está todo em `numeric(10,2)`. Não há chave estrangeira sem índice nem data em
texto. Há um `EXCLUDE USING gist` contra duas reservas do mesmo quarto. As conferências apontaram só as colunas
`status`, `rate_plan`, `type` e `channel` em `text` sem CHECK, o que veio da base.
**Nota:** bom

A mudança não precisava mexer no banco, e o agente não mexeu: a migração da base está intacta, e o SQL continua com
parâmetros (`src/main/java/hotel/reservations/ReservationStore.java`, que o agente não tocou). A única observação nova
é de carga. Os dois relatórios novos seguem o padrão da base de trazer a tabela inteira (`ReservationStore.java:108`)
e filtrar o mês em Java (`TourismTaxReport.java:28`, e `RatePlanReport` pelo financeiro). Isso já acontecia nos
relatórios da base e não pesa na nota.

## Comportamento

**Números:** 64 de 64 conferências passaram: a parte comum 46 de 46, os dados depois de reiniciar 3 de 3, o cenário
09 (os relatórios) 13 de 13 e as duas reservas ao mesmo tempo 2 de 2.
**Nota:** bom

Para o dono da pousada, os quatro relatórios saem como ele pediu. Os quatro começam com
"Pousada Maré Alta,<nome>,<período>", e a ocupação e o financeiro continuam iguais depois do título. A taxa de turismo
cobra R$ 4,50 por hóspede por noite das reservas pagas, na ordem do financeiro, e um mês vazio dá total 0.00. O
relatório de planos mostra STANDARD e NON_REFUNDABLE, mesmo zerado. Um mês fora do formato dá 400. Nada do que já
funcionava quebrou.

## Dúvidas

- A classe ficou entre `malfeito` e `alternativa equivalente`. `CsvLayout.render` cumpre o pedido concreto do dono:
  a linha de data no fim de todos os relatórios mudaria num lugar só. Mas ela recebe o texto pronto, e não as partes
  que variam como funções, e cada relatório repete a sequência dos passos. Dei `malfeito` porque a referência descreve
  esse caso quase com essas palavras.
- O texto não diz como arredondar a taxa quando o valor mudar para algo com mais casas. `HALF_EVEN`
  (`TourismTaxReport.java:50`) é uma escolha razoável, e nenhum valor do roteiro a põe à prova.
