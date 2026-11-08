# Revisão: manutenção, cenário 09

## Visão geral

A base é um serviço Javalin com sete pacotes: `hotel.web` (as rotas em `HttpApi`), `hotel.reservations` (as regras e o
`ReservationStore` com JDBC), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports` (os relatórios em
CSV). A montagem fica em `hotel.Hotel`, e os dados ficam no PostgreSQL, num esquema criado por uma migração do Flyway.
Uma requisição de relatório entra por `HttpApi` (`src/main/java/hotel/web/HttpApi.java:91-106`), chega ao `generate`
do relatório, que lê `store.all()` e filtra, ordena e escreve o CSV em Java. O agente fez quatro coisas: acrescentou
uma classe abstrata `CsvReport` (`src/main/java/hotel/reports/CsvReport.java:3-14`), pôs os quatro relatórios para
estendê-la, criou `TourismsTaxReport` e `RatePlansReport` e ligou as duas rotas novas. Tudo funciona: 64 de 64
conferências do roteiro passaram, 32 testes passaram, e o banco não mudou. O defeito mais sério está no design. O
"esqueleto" é só um método utilitário, `formatCsv`, que cola o título, o cabeçalho, as linhas e o total. Cada um dos
quatro relatórios continua repetindo por inteiro a sequência de escolher, ordenar, escrever as linhas e somar o total,
e a seleção do financeiro foi copiada no relatório por plano.

## Arquitetura

**Números:** 7 pacotes, 17 dependências e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais à base
(`measures/dependencies-base.md`: 7 pacotes, 17 dependências e o mesmo ciclo). `hotel.reports` passou de 2 para 5
classes (de 95 para 211 linhas) e não usa Javalin nem JDBC. `HttpApi` passou de 170 para 186 linhas.
**Nota:** bom

A mudança respeitou as camadas da base. As rotas novas só leem o mês, chamam o relatório e escrevem o CSV
(`src/main/java/hotel/web/HttpApi.java:99-106`) e reaproveitam o `readMonth` que já existia (`HttpApi.java:166-173`).
As regras da taxa e dos planos ficam em `hotel.reports`, sem Javalin e sem SQL
(`src/main/java/hotel/reports/TourismsTaxReport.java:21-48`, `src/main/java/hotel/reports/RatePlansReport.java:19-52`).
O acesso ao banco continua só no `ReservationStore`. As peças novas são criadas na montagem, em `src/main/java/hotel/Hotel.java:28-30`.
O ciclo `documents`/`reservations` veio da base, e o agente não criou dependência nova. O construtor de `HttpApi`
ganhou mais dois relatórios e já recebe sete parâmetros (`HttpApi.java:37-44`). A cada relatório novo ele cresce, mas
isso ainda é aceitável e pesa pouco.

## Design de baixo nível

**Números:** 4 de 4 relatórios estendem `CsvReport` e passam pelo `formatCsv`. Nenhum passo é abstrato: o "esqueleto"
só monta o texto final, e os 4 relatórios repetem cada um a sequência de escolher, ordenar, escrever as linhas e
calcular o total.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O que aproxima do Template Method:
- Há uma classe abstrata comum, e os quatro relatórios passam por ela, os dois da base inclusive
  (`OccupancyReport.java:13`, `RevenueReport.java:15`).
- A linha de título fica num lugar só (`CsvReport.java:4` e `CsvReport.java:8`). Uma linha final com a data, que o
  dono avisou que virá no ano que vem, entraria só em `formatCsv`.

O que afasta:
- `CsvReport` não tem método-modelo nem passos abstratos. `formatCsv(reportName, period, columnHeaders, rows, total)`
  (`CsvReport.java:6-13`) é uma função utilitária que recebe pedaços de texto já prontos.
- Cada `generate` refaz a sequência inteira: filtra `store.all()` num laço, ordena, abre um `StringBuilder`, escreve as
  linhas, soma o total e monta a string do total (`RevenueReport.java:23-47`, `OccupancyReport.java:22-44`,
  `TourismsTaxReport.java:21-47`, `RatePlansReport.java:19-51`).
- O formato da linha do total (`"total," + ... + "\n"`) é repetido nos quatro (`RevenueReport.java:45`,
  `OccupancyReport.java:43`, `TourismsTaxReport.java:45`, `RatePlansReport.java:49`).
- A herança serve só para alcançar um método `protected`. Nada impede um relatório novo de montar o CSV sem passar por
  ele.

É exatamente o caso que a referência chama de malfeito: "o 'esqueleto' é só uma função utilitária de CSV e cada
relatório ainda repete a sequência dos passos".

**O exagero.** Não há estrutura sem necessidade: nenhuma interface, fábrica ou camada nova além de `CsvReport`.

**O resto do código:**
- **Duplicação.** A seleção das reservas do relatório por plano foi copiada do financeiro
  (`RatePlansReport.java:20-27` é igual a `RevenueReport.java:24-31`). O texto pede "as mesmas reservas que o relatório
  financeiro conta". Se a regra do financeiro mudar, os dois podem divergir sem que ninguém perceba.
- **Data e dinheiro à mão.** As noites são calculadas à mão com `toEpochDay()` (`TourismsTaxReport.java:34`). O cálculo
  da taxa ocupa uma linha longa, com `java.math.RoundingMode` escrito por extenso (`TourismsTaxReport.java:35`).
- **Laço aninhado.** `RatePlansReport.java:33-47` percorre todas as reservas uma vez para cada plano. Com dois planos
  funciona, mas um agrupamento seria mais claro.
- **Nome com erro.** A classe se chama `TourismsTaxReport`, com o "s" a mais, e o erro se espalha pelo campo e pelos
  imports (`HttpApi.java:34`, `Hotel.java:8`).
- **O que está certo.** O dinheiro fica em `BigDecimal` com 2 casas (`TourismsTaxReport.java:13`,
  `TourismsTaxReport.java:32`), e os planos saem do `enum` na ordem certa (`RatePlansReport.java:33`).
- **A taxa no código.** O valor de R$ 4,50 é uma constante (`TourismsTaxReport.java:13`). O texto diz que ele muda todo
  ano, então trocá-lo exige mudar o código. É aceitável, mas vale registrar.

## Testes

**Números:** 32 testes rodaram, 0 falharam e 0 foram pulados; há 32 métodos escritos (a base tinha 26). A cobertura de
linhas é de 93,5% (535 de 572) e a de ramos de 89,1% (123 de 138). Em `hotel.reports`, 108 de 108 linhas e 44 de 44
ramos foram executados. A base tinha 93,8% de linhas e 87,1% de ramos.
**Nota:** com problemas

**O que está bom:**
- Os 26 testes da base continuam passando. Os cinco de relatório foram atualizados só com a linha de título
  (`src/test/java/hotel/reports/ReportsTest.java:67`, `:80`, `:93`, `:110`, `:122`), sem ficar mais fracos.
- Os relatórios novos têm testes que comparam o CSV inteiro: a taxa de abril com 9.00 + 9.00 = 18.00
  (`ReportsTest.java:129-139`), os planos de abril com STANDARD 2 / 1939.60 e NON_REFUNDABLE 0 / 0.00
  (`ReportsTest.java:150-160`) e um mês vazio com os dois planos zerados (`ReportsTest.java:163-173`).

**As lacunas na parte nova:**
- **A multiplicação pelos hóspedes.** Todas as reservas da fixture têm 1 hóspede (`ReportsTest.java:33`). O auxiliar
  `bookMultiple`, que aceitaria mais hóspedes, foi escrito e nunca usado (`ReportsTest.java:36-40`). Pela pergunta da
  mutação: se alguém trocasse `reservation.guests() * nights` por `nights` em `TourismsTaxReport.java:35`, nenhum
  teste falharia. O exemplo do próprio texto (2 hóspedes, 3 noites, 27,00) não está testado.
- **O plano NON_REFUNDABLE com valor.** Ele nunca aparece diferente de zero, e nenhum teste de plano conta uma
  NON_REFUNDABLE cancelada, que o texto manda contar. Se alguém trocasse o filtro `amountKept().signum() > 0` por
  `isPaid()` em `RatePlansReport.java:22-24`, os testes de plano continuariam passando.
- **As rotas novas.** Nenhum teste chama `/reports/tourism-tax` nem `/reports/rate-plans` pela API: `HttpApiTest` não
  mudou (`src/test/java/hotel/web/HttpApiTest.java:113-120` só chama a ocupação e o financeiro).
- **Testes repetidos.** Os três testes "HasHeader" (`ReportsTest.java:142-147`, `:176-181` e `:184-189`) conferem a
  primeira linha, que os testes de CSV inteiro já conferem.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` em todas as colunas. Nenhuma chave
estrangeira está sem índice e nenhuma data está guardada como texto. A sobreposição de reservas é barrada por
`EXCLUDE USING gist`. Há 1 migração (`V1__create_tables.sql`), igual à da base. As únicas conferências que acusaram
algo foram `status`, `rate_plan`, `type` e `channel` em `text` sem `CHECK`, e isso veio da base.
**Nota:** bom

O pedido não mexia no esquema, e o agente não mexeu: a migração da base está intacta (`measures/database.md`, "igual"),
e nenhum SQL novo foi escrito. Os relatórios novos leem as reservas pelo `ReservationStore.all()` que já existia
(`src/main/java/hotel/reservations/ReservationStore.java:111`) e filtram por mês em Java
(`TourismsTaxReport.java:23-28`, `RatePlansReport.java:21-27`). Esse padrão de trazer a tabela inteira veio da base
(`RevenueReport` já fazia assim). O agente só o repetiu em mais dois relatórios: com o crescimento da tabela isso vai
pesar, mas não é um defeito novo. O SQL usa parâmetros, e os trechos montados com `+` concatenam só constantes
(`ReservationStore.java:33`, `:83`, `:98`, `:111`).

## Comportamento

**Números:** 64 de 64 conferências passaram:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 09: 13 de 13;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>", e a ocupação e o financeiro continuam iguais
aos de antes depois do título. A taxa de turismo lista as reservas pagas do mês na ordem do financeiro, com 4,50 por
hóspede por noite e o total. O relatório por plano traz STANDARD e NON_REFUNDABLE (mesmo sem reserva), com a quantidade,
o valor e o total. Um mês em formato errado responde 400. Para o dono da pousada, os dois relatórios novos saem como
o contador e a prefeitura pediram, e nada do que já funcionava quebrou.

## Dúvidas

O design ficou entre malfeito e alternativa equivalente:
- **A favor de alternativa equivalente:** os quatro relatórios passam pela mesma montagem final, e a linha final com a
  data entraria num lugar só.
- **A favor de malfeito:** a referência descreve a alternativa equivalente como um escritor que recebe as partes que
  variam (a seleção, a linha, o total) e conduz os passos. Aqui ninguém conduz os passos: cada relatório repete a
  sequência inteira.

Escolhi malfeito porque a referência descreve esse caso de forma explícita.
