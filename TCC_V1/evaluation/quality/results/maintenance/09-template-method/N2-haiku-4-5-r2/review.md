# Revisão: manutenção, cenário 09

## Visão geral

O projeto (base e final) tem sete pacotes: `hotel.web` (as rotas do Javalin em `HttpApi`), `hotel.reservations` (as
regras em `ReservationService`, o preço em `PriceCalculator` e o JDBC em `ReservationStore`), `hotel.rooms`,
`hotel.messages`, `hotel.documents`, `hotel.reports` (os relatórios CSV) e `hotel` (a montagem em `Hotel.java` e o pool
em `Database.java`). Uma requisição de relatório entra por `HttpApi`, que lê o parâmetro e chama `generate(...)` do
relatório; o relatório lê todas as reservas com `ReservationStore.all()`, filtra e monta o CSV. Os dados ficam no
PostgreSQL, com o esquema criado pela migração Flyway `V1__create_tables.sql`. O agente acrescentou
`TouristTaxReport` e `RatePlanReport`, duas rotas e a linha de título nos quatro relatórios. Tudo funciona: os 64
conferências do roteiro passaram e os 30 testes passam. O mais sério é o design: o texto pede de forma explícita que
"quando o jeito dos relatórios mudar, [...] mude em todos de uma vez", e o agente fez o contrário. Os quatro relatórios
ficaram escritos cada um inteiro, com a linha de título copiada em cada um (o Template Method foi omitido). A próxima
mudança pedida, a linha com a data no fim, vai ter de ser feita à mão nos quatro arquivos. Os testes novos também
deixam sem conferência a multiplicação pelo número de hóspedes e o plano NON_REFUNDABLE com reservas.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, antes e depois. Há um ciclo, `hotel.documents` ↔
`hotel.reservations`, que já vinha da base (`measures/dependencies-base.md` mostra o mesmo ciclo). `hotel.reports`
foi de 2 classes e 95 linhas para 4 classes e 209 linhas, e `HttpApi` de 170 para 186 linhas. A mudança não criou
nenhuma dependência nova entre pacotes.
**Nota:** bom

As duas classes novas estão no pacote certo (`src/main/java/hotel/reports/TouristTaxReport.java`,
`src/main/java/hotel/reports/RatePlanReport.java`) e dependem só de `hotel.reservations`, como os relatórios que já
existiam. Elas não usam Javalin nem JDBC (a tabela de bibliotecas em `measures/dependencies.md` deixa `hotel.reports`
sem nenhuma). As rotas novas são finas: só leem o mês e chamam o relatório (`src/main/java/hotel/web/HttpApi.java:99-106`),
e reaproveitam o `readMonth` que já existia (`HttpApi.java:166`). A montagem continua num lugar só
(`src/main/java/hotel/Hotel.java:28-35`). O ciclo `documents`↔`reservations` veio da base, e o agente não o piorou.
`HttpApi` ganha mais um campo e um parâmetro de construtor a cada relatório (`HttpApi.java:34-51`). Isso pesa pouco
com quatro relatórios, mas é mais um sinal de que não existe um tipo comum de relatório (ver o design).

## Design de baixo nível

**Números:** 4 relatórios, 0 dentro de uma estrutura comum e 4 escritos inteiros. A linha de título
"Pousada Maré Alta,…" está copiada nos 4 (`OccupancyReport.java:34`, `RevenueReport.java:35`,
`TouristTaxReport.java:35`, `RatePlanReport.java:31`). Não há classe abstrata, esqueleto nem escritor comum.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto descreve quatro relatórios que seguem os mesmos passos (título, cabeçalho, linhas, total) e
avisa que virão mais dois. Também diz que "quando o jeito dos relatórios mudar, quero que mude em todos de uma vez"
(a linha com a data no fim, no ano que vem). Isso pede o esqueleto num lugar só. O agente não criou estrutura nenhuma.
Os dois relatórios novos são cópias da forma dos antigos: `RatePlanReport.java:21-29` repete a seleção de
`RevenueReport.java:24-31` linha por linha, e `TouristTaxReport.java:24-32` repete a seleção e a ordenação. A linha de
título foi colada à mão em cada um dos quatro arquivos (linhas acima). A próxima mudança de formato vai ter de tocar os
quatro arquivos, e os dois que o contador já anunciou vão ser a quinta e a sexta cópia. É o caso que a referência
chama de omissão.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface, fábrica ou camada nova. Contenção.

**O resto do código.** Os tipos estão certos: `BigDecimal` com a taxa como constante e escala 2
(`TouristTaxReport.java:15`, `:40-43`), `YearMonth` para o mês, e o `enum RatePlan` percorrido em ordem, o que garante
uma linha por plano mesmo sem reserva (`RatePlanReport.java:37`). Mas há duplicação localizada, além da do pattern:
- o nome da pousada já existe como constante (`src/main/java/hotel/reservations/ReservationService.java:12`,
  `HOTEL_NAME`), e o agente repetiu o texto literal quatro vezes;
- `TouristTaxReport.java:39` recalcula as noites com `toEpochDay`, quando a reserva já guarda esse número
  (`reservation.price().nights()`, usado em `src/main/java/hotel/documents/ReservationReceipt.java:34`);
- `RatePlanReport.java:37-51` percorre a lista inteira uma vez por plano. Com dois planos isso é inofensivo, mas um
  agrupamento seria mais direto.

Nada disso é grave, mas um revisor pediria para mudar antes de aceitar.

## Testes

**Números:** 30 testes rodaram, com 0 falhas, 0 erros e 0 pulados (30 métodos escritos). A base tinha 26, todos
mantidos e passando. A cobertura de linhas foi de 93,8% para 93,6% (540 de 577) e a de ramos de 87,1% para 89,1%
(123 de 138). `hotel.reports` tem 113 de 113 linhas e 44 de 44 ramos cobertos.
**Nota:** com problemas

Pontos bons: o agente atualizou os 5 testes dos relatórios antigos para a linha de título, com o CSV inteiro
comparado (`src/test/java/hotel/reports/ReportsTest.java:67-73`, `:93-99`). Também escreveu 4 testes novos, que
comparam o CSV inteiro com valores (`ReportsTest.java:129-143`, `:145-154`, `:156-172`, `:174-185`). Mudou o preparo
dos dados para dentro do `@BeforeEach` (`ReportsTest.java:37-56`), o que não muda o que se testa.

A pergunta da mutação mostra três buracos na parte nova, apesar da cobertura de 100%:
- **Os hóspedes na taxa.** Todas as reservas do teste têm 1 hóspede (`ReportsTest.java:34`). Se alguém apagasse
  `.multiply(new BigDecimal(reservation.guests()))` (`TouristTaxReport.java:41`), nenhum teste falharia. O exemplo
  do próprio dono ("2 hóspedes por 3 noites dão R$ 27,00") não está testado. Os comentários do teste ainda falam em
  "2 guests" e "total: 27.00" (`ReportsTest.java:133-135`), enquanto a asserção espera 1 hóspede e 18.00
  (`:139-141`): o comentário desmente o próprio teste.
- **O plano NON_REFUNDABLE com reservas.** Nos testes, a linha NON_REFUNDABLE é sempre `0,0.00` (`ReportsTest.java:169`,
  `:182`). Uma troca de `amountKept().signum() > 0` por `isPaid()` em `RatePlanReport.java:24` deixaria de contar a
  NON_REFUNDABLE cancelada, que o texto manda contar ("as mesmas reservas que o relatório financeiro conta"), e
  passaria. Uma troca de STANDARD por NON_REFUNDABLE na soma também passaria. Os comentários em `:160-164` estão
  confusos ("amount unknown, but we can calculate").
- **As rotas novas.** `src/test/java/hotel/web/HttpApiTest.java:108-121` não chama `/reports/tourism-tax` nem
  `/reports/rate-plans`. O código das rotas é coberto só pelo roteiro de comportamento, não pelos testes do agente.

Detalhe menor: os imports de `hotel.reports.RatePlanReport` e `TouristTaxReport` em `ReportsTest.java:8-9` são
desnecessários, porque as classes estão no mesmo pacote.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)`, e não há chave estrangeira sem índice nem data em texto. As colunas de situação, plano e tipo são
`text` sem CHECK. Há o EXCLUDE contra duas reservas na mesma noite. A única migração, `V1__create_tables.sql`, está
"igual" à da base. O SQL é montado com `+` só com constantes (`ReservationStore.java:33`, `:83`, `:98`, `:111`).
**Nota:** bom

O agente não tocou no banco, e o pedido não exigia: os relatórios novos usam dados que já existiam (`guests`,
`rate_plan`, `amount_paid`/`refund`). As observações das conferências (status, plano e tipo em `text` sem CHECK) vieram
da base. A única coisa a notar é a carga: os dois relatórios novos seguem o jeito da base e trazem a tabela inteira de
reservas para filtrar em Java (`TouristTaxReport.java:25`, `RatePlanReport.java:22`, os dois com `store.all()`). Isso
dá quatro varreduras completas em vez de duas. É um defeito herdado (`RevenueReport.java:25`, `OccupancyReport.java:25`)
que o agente repetiu, e não muda a nota.

## Comportamento

**Números:** 64 de 64 conferências passaram, 0 falharam: a parte comum, 46 de 46; os dados depois de reiniciar, 3 de
3; o cenário 09, 13 de 13; as duas reservas ao mesmo tempo, 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API. Os quatro relatórios começam com
"Pousada Maré Alta,<nome>,<período>", com os nomes `occupancy`, `revenue`, `tourism-tax` e `rate-plans`. A ocupação e o
financeiro continuam iguais depois do título. A taxa de turismo cobra R$ 4,50 por hóspede por noite das reservas pagas
do mês, na ordem do financeiro, e um mês vazio dá `total,0.00`. O relatório por plano traz STANDARD e depois
NON_REFUNDABLE, mesmo sem reservas, com o total. Um mês fora do formato é recusado com 400. O que já funcionava continua
funcionando, e os dados sobrevivem ao reinício. Uma lacuna fora do roteiro: o README não foi atualizado com as duas
rotas novas (`final/README.md` é igual ao da base, e a tabela da API para em `/reports/revenue`). Isso não muda a nota.

## Dúvidas

Nenhuma sobre as notas. A classe "omissão" é clara: não há esqueleto nem escritor comum, e os quatro relatórios têm a
linha de título copiada.
