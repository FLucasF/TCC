# Revisão: manutenção, cenário 09

## Visão geral

O sistema da base tem sete pacotes: `hotel` (a montagem, em `Hotel.java`), `hotel.web` (as rotas do Javalin em
`HttpApi.java`), `hotel.reservations` (as regras em `ReservationService` e o JDBC em `ReservationStore`), `hotel.rooms`,
`hotel.messages`, `hotel.documents` e `hotel.reports`. Uma requisição de relatório chega a `HttpApi`, que lê o mês ou a
noite e chama `generate` do relatório. O relatório lê as reservas por `ReservationStore.all()` e monta o CSV. Os dados
ficam no PostgreSQL, num esquema criado por uma migração do Flyway. O agente mexeu só em `hotel.reports`, em `Hotel` e
em `HttpApi`. Criou a classe abstrata `Report<P>`, com o método-modelo `final generate` (título, cabeçalho, linhas e
total), passou os dois relatórios que já existiam para ela e escreveu os dois novos como subclasses. As 64 conferências
do roteiro passaram, e os 28 testes também. Não há defeito sério. O que há são três pontos pequenos, todos no código
novo: o filtro do financeiro foi copiado no relatório por plano, as noites são recalculadas em vez de lidas da reserva,
e os testes deixam sem conferir a diferença entre "paga" (taxa de turismo) e "ficou com dinheiro" (planos) numa
reserva não reembolsável cancelada.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base: os
mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo (`measures/dependencies-base.md`). `hotel.reports` passou
de 2 para 6 classes (95 → 258 linhas). `HttpApi` passou de 170 para 186 linhas.
**Nota:** bom

O agente não criou dependência nova nem ciclo novo. O ciclo `documents` ↔ `reservations` já existia na base
(`src/main/java/hotel/reservations/ReservationService.java:3` importa `ReservationReceipt`, que importa `Reservation`).
As duas rotas novas são finas, do mesmo jeito que as da base: leem o mês e chamam o relatório
(`src/main/java/hotel/web/HttpApi.java:99-106`). Os relatórios novos não conhecem o Javalin nem o JDBC: leem pelo
`ReservationStore` (`src/main/java/hotel/reports/TourismTaxReport.java:36`, `RatePlanReport.java:41`). As peças novas
são criadas no lugar de montagem que já existia (`src/main/java/hotel/Hotel.java:32-39`). O valor da taxa, que a
prefeitura muda todo ano, fica numa constante da montagem (`Hotel.java:22`) e é injetado no relatório. Assim, a regra
não guarda o número dentro dela. O preço disso é que a cada ano é preciso mudar o código, e não uma configuração; para
o tamanho do sistema, é aceitável. O construtor de `HttpApi` passou a receber sete parâmetros, um por relatório
(`HttpApi.java:36-43`). Cada relatório novo vai alongar essa lista, mas ainda não é um defeito.

## Design de baixo nível

**Números:** 4 relatórios, 4 na estrutura (`OccupancyReport`, `RevenueReport`, `TourismTaxReport` e `RatePlanReport`
estendem `Report`), 0 fora dela. A linha de título só aparece em um lugar (`Report.java:26`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O agente aplicou o Template Method como a referência descreve. `src/main/java/hotel/reports/Report.java:23-33`
é o método-modelo, e ele é `final`: título com o nome da pousada, o nome e o período, depois o cabeçalho, as linhas e o
total. Os passos que variam são abstratos: `name()`, `header()` e `content(P)` (`Report.java:36-40`). Os dois relatórios
que já existiam também foram para a estrutura (`OccupancyReport.java:13`, `RevenueReport.java:14`). Nenhuma subclasse
monta título nem quebra de linha, e nenhuma pergunta qual é o caso. Quando o rodapé com a data chegar no ano que vem,
ele vai entrar só em `generate`, e isso atende ao pedido de "mudar em todos de uma vez". A ordenação comum
(`BY_CHECK_IN_THEN_ID`, `Report.java:17-18`) foi tirada do financeiro e é usada pela taxa de turismo, que o texto manda
seguir "na mesma ordem do relatório financeiro" (`TourismTaxReport.java:41`).

**O exagero.** Não há. O tipo genérico `P` (noite ou mês) e o record `Content` (`Report.java:21`) servem ao pattern e
não são camadas que só repassam. Não há fábrica, nem interface com uma implementação só.

**O resto do código** (o que pesa na nota):
- `RatePlanReport.java:42-44` copia o filtro do financeiro (`RevenueReport.java:36-38`): `amountKept().signum() > 0` e
  o mês do check-in. O texto pede "as mesmas reservas que o relatório financeiro conta". Se a regra do financeiro mudar,
  o relatório por plano não acompanha. Uma regra assim deveria morar num lugar só, por exemplo um método na reserva ou
  na classe de relatórios.
- `RatePlanReport.java:41` chama `store.all()` dentro do laço dos planos e lê a tabela inteira uma vez por plano. São
  duas leituras completas a cada pedido hoje, e mais uma a cada plano novo.
- `TourismTaxReport.java:46` recalcula as noites com `ChronoUnit.DAYS.between`, mas a reserva já guarda esse número
  (`reservation.price().nights()`, `src/main/java/hotel/reservations/Price.java:5`), e o comprovante usa esse valor
  (`documents/ReservationReceipt.java:34`). É uma duplicação pequena.
- O que está certo: o dinheiro é `BigDecimal` com 2 casas e arredondamento explícito (`TourismTaxReport.java:47-48`), os
  planos vêm do `enum` (`RatePlanReport.java:38`) e o "pago" usa o `isPaid()` que a base já tinha
  (`TourismTaxReport.java:37`, `reservations/Reservation.java:91-95`). As linhas são concatenadas sem escapar vírgula,
  mas isso vem da base, e os campos são números, datas e enums.

## Testes

**Números:** 28 testes rodaram, 0 falhas, 0 erros, 0 pulados. São 28 métodos escritos, contra 26 na base, e nenhum foi
apagado. Cobertura de linhas: 94,5% (535/566), contra 93,8% na base. Cobertura de ramos: 89,1% (123/138), contra 87,1%.
`hotel.reports` tem 101/101 linhas e 44/44 ramos cobertos.
**Nota:** com problemas

O agente atualizou os cinco testes de relatório da base para a linha de título, comparando o CSV inteiro
(`src/test/java/hotel/reports/ReportsTest.java:62`, `75`, `88`, `105`, `117`) e sem enfraquecer nenhum. O teste novo da
taxa de turismo confere com valores o exemplo do texto: 2 hóspedes × 3 noites = 27,00 (`ReportsTest.java:142`). Ele
também confere que a pendente, a cancelada e a que começa em março ficam fora, e confere o total 45,00
(`ReportsTest.java:124-145`). O teste dos planos confere a linha com 0 de um plano sem reserva, nos dois sentidos
(`ReportsTest.java:148-163`). Pela API, as duas rotas novas são conferidas só pelo total
(`src/test/java/hotel/web/HttpApiTest.java:121-122`).

A pergunta da mutação deixa duas regras novas sem rede, justamente as que o texto diferencia:
- se `RatePlanReport` passasse a usar `isPaid()` em vez de `amountKept() > 0`, nenhum teste falharia. Os dados de abril
  e março não têm uma não reembolsável cancelada (`ReportsTest.java:43-54`), que deve contar no relatório por plano como
  conta no financeiro;
- se `TourismTaxReport` passasse a usar `amountKept() > 0` em vez de `isPaid()`, nenhum teste falharia, pelo mesmo
  motivo: falta uma não reembolsável cancelada no mês, que deve ficar fora da taxa. Também não há uma reserva com
  check-out nos dados da taxa.

O roteiro do comportamento confere esses casos e eles passam, mas os testes do agente não os protegem.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` (6 colunas ok). Nenhuma chave
estrangeira sem índice e nenhuma data como texto. Há um `EXCLUDE` contra duas reservas do mesmo quarto. A migração
`V1__create_tables.sql` está igual à da base, e não há migração nova.
**Nota:** bom

O pedido não mexe no banco, e o agente não mexeu: os dois relatórios novos usam só colunas que já existiam (`guests`,
`rate_plan`, as datas e o dinheiro). O esquema, a migração, as restrições (`EXCLUDE USING gist` com
`daterange(check_in, check_out)`) e o SQL com parâmetros vêm da base
(`src/main/java/hotel/reservations/ReservationStore.java:83`, `98`, `111`). Os candidatos a "SQL montado com `+`"
apontados pelo script juntam só constantes (`COLUMNS`). A única observação de carga é do código novo: os relatórios
leem a tabela inteira (`ReservationStore.java:111`) e filtram em Java. Esse padrão já vinha da base, e
`RatePlanReport.java:41` faz essa leitura uma vez por plano. Num volume de pousada pequena, isso não pesa a ponto de
mudar a nota.

## Comportamento

**Números:** 64 de 64 conferências passaram: parte comum 46/46, depois de reiniciar 3/3, cenário 09 13/13, reservas
simultâneas 2/2.
**Nota:** bom

Nada falhou. Para o dono da pousada, isso quer dizer o seguinte:
- os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>";
- a ocupação e o financeiro continuam iguais depois do título;
- a taxa de turismo lista as reservas pagas do mês na ordem do financeiro e soma R$ 4,50 por hóspede por noite, e
  devolve só título, cabeçalho e total 0.00 num mês vazio;
- o relatório por plano traz STANDARD e NON_REFUNDABLE (com 0 quando não há reserva) e conta a não reembolsável
  cancelada como o financeiro conta;
- um mês fora do formato é recusado com 400;
- o resto do sistema (preços, recusas, cancelamentos, e-mails, os dados depois de reiniciar e as reservas simultâneas)
  continua funcionando.

## Dúvidas

- O texto diz que "a prefeitura muda o valor da taxa todo ano". O agente pôs o valor numa constante da montagem
  (`Hotel.java:22`), e não numa configuração ou no banco. Isso cumpre o pedido e isola o valor, mas a evidência não
  decide se o dono esperava poder mudá-lo sem um novo pacote do sistema. Não contei isso como defeito.
- A nota `com problemas` no design se apoia em defeitos pequenos e localizados: o filtro copiado, a leitura repetida e
  as noites recalculadas. Um revisor mais tolerante poderia aceitar o código como está.
