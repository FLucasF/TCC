# Revisão: manutenção, cenário 09

## Visão geral

A base tem sete pacotes: `hotel` (a montagem e o banco), `hotel.web` (a borda Javalin), `hotel.reservations` (regras e
`ReservationStore` com JDBC), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`. Uma requisição de
relatório entra em `HttpApi` (`src/main/java/hotel/web/HttpApi.java:91-106`), que lê o parâmetro e chama o relatório.
O relatório lê as reservas do `ReservationStore` (PostgreSQL, esquema criado pelo Flyway). O agente mexeu só em
`hotel.reports`, na montagem e nas rotas. Criou uma classe abstrata `CsvReport` com o método-modelo `final`
(`src/main/java/hotel/reports/CsvReport.java:22-32`) e passou para ela os dois relatórios que já existiam e os dois
novos. Juntou a seleção mensal num ajudante, `MonthlyReservations`, e acrescentou as duas rotas novas. Não achei nada
sério: o roteiro passou inteiro (64 de 64), os 30 testes passam e nada piorou em relação à base. Só há detalhes
pequenos, como um import repetido.

## Arquitetura

**Números:** 7 pacotes e 17 dependências, os mesmos da base. Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`,
que já existia na base (`measures/dependencies.md` e `measures/dependencies-base.md`). `hotel.reports` cresceu de 2
para 7 classes (de 95 para 292 linhas) e continua sem Javalin e sem JDBC.
**Nota:** bom

- **Borda HTTP fina.** As duas rotas novas só leem o mês com o `readMonth` que já existia e devolvem o CSV
  (`src/main/java/hotel/web/HttpApi.java:99-106`).
- **Regras fora da borda.** As regras dos relatórios ficam em `hotel.reports`, que só depende de `hotel.reservations` e
  `hotel.rooms`.
- **Montagem num lugar só.** Tudo é ligado em `src/main/java/hotel/Hotel.java:26-40`, inclusive o valor da taxa, que
  entra no construtor do relatório (`Hotel.java:22`, `Hotel.java:38`).
- **Ciclo da base.** O ciclo vem de `src/main/java/hotel/reservations/ReservationService.java:3`, que importa
  `hotel.documents`; o agente não o criou nem o piorou.
- **Tamanho.** `HttpApi` passou de 170 para 186 linhas só pelos dois campos e pelas duas rotas novas, o que não
  preocupa.

## Design de baixo nível

**Números:** os 4 relatórios estão no esqueleto (`OccupancyReport`, `RevenueReport`, `TourismTaxReport`,
`RatePlanReport`) e nenhum ficou fora. A linha de título fica num lugar só (`CsvReport.java:25`), sem cópia nos
relatórios. Há 4 passos abstratos: `header`, `rows`, `line`, `total`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** É um Template Method de manual:

- O esqueleto (título, cabeçalho, uma linha por item, total) é um método `public final`
  (`src/main/java/hotel/reports/CsvReport.java:22-32`), e os passos que mudam são abstratos (`CsvReport.java:35-43`).
- Os dois relatórios da base foram convertidos, não copiados: `src/main/java/hotel/reports/OccupancyReport.java:13`
  e `src/main/java/hotel/reports/RevenueReport.java:13`. O `generate` deles sumiu, e cada um só diz o seu cabeçalho,
  a sua seleção, a sua linha e o seu total.
- Os dois novos são subclasses do mesmo jeito (`TourismTaxReport.java:13`, `RatePlanReport.java:12`).
- O parâmetro de tipo `R` deixa o relatório por plano ter linhas que não são reservas (`RatePlanReport.java:15`).
- A linha de data pedida para o ano que vem entra num lugar só, `CsvReport.generate`, e um relatório novo só preenche
  os quatro passos.

Nada no código pergunta qual é o relatório.

**O exagero.** `MonthlyReservations` (`src/main/java/hotel/reports/MonthlyReservations.java:11-36`) tem uso real: tira
dos três relatórios mensais a seleção e a ordem (data de entrada e depois id) que antes moravam só no financeiro. Assim
o "na mesma ordem do relatório financeiro" e o "as mesmas reservas que o financeiro conta" ficam garantidos por
construção (`MonthlyReservations.java:20-27`). Não há interface, fábrica nem camada que só repassa.

**O resto do código.** O design está bom:

- `BigDecimal` com escala 2 para o dinheiro (`TourismTaxReport.java:54-59`).
- O valor da taxa entra pelo construtor, e não fica fixo na fórmula (`TourismTaxReport.java:18-22`).
- `enum` para os planos, iterado na ordem da declaração (`RatePlanReport.java:32`).

Só há detalhes pequenos, nenhum que exija mudança antes de aceitar:

- `import java.math.RoundingMode;` está repetido (`TourismTaxReport.java:6-7`). Compila, mas é descuido.
- A soma de `BigDecimal` se repete em `RevenueReport.java:41-44`, `TourismTaxReport.java:43-48` e
  `RatePlanReport.java:34-36,49`.
- A taxa, que muda todo ano, é uma constante no código (`Hotel.java:22`), e não uma configuração. Trocá-la exige
  recompilar, mas fica num lugar só e com nome.

## Testes

**Números:** 30 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 30 métodos escritos (a base tinha 26, todos
mantidos). Cobertura total: linhas 522/553 (94,4%) e ramos 99/114 (86,8%). Em `hotel.reports`, a cobertura é de 88/88
linhas e 20/20 ramos (`measures/tests.md`; na base, 93,8% e 87,1%).
**Nota:** bom

- **Testes da base atualizados, não enfraquecidos.** Os cinco testes de relatório da base ganharam a linha de título
  no texto esperado (`src/test/java/hotel/reports/ReportsTest.java:62`, `:75`, `:88`, `:105`, `:117`). O CSV
  continua conferido inteiro, com `assertEquals` sobre o texto todo.
- **Quatro testes novos, com valores:**
  - A taxa, com o exemplo do pedido (2 hóspedes × 3 noites = 27,00), fora a não paga, a cancelada e a de março, e o
    total 45,00 (`ReportsTest.java:123-140`).
  - A taxa de um mês vazio (`ReportsTest.java:143-151`).
  - Os planos com `NON_REFUNDABLE` em zero (`ReportsTest.java:154-165`).
  - A não reembolsável cancelada contando nos planos (`ReportsTest.java:168-182`).
- **Pela API.** As rotas novas, os totais e os 400 estão conferidos (`src/test/java/hotel/web/HttpApiTest.java:119-124`).
  Como esse teste usa o `Hotel` de verdade, trocar o 4,50 em `Hotel.java:22` derrubaria o total 18,00.
- **Pergunta da mutação.** Trocar `reservations.paid(month)` por `reservations.withRevenue(month)` em
  `TourismTaxReport.java:31` não derrubaria nenhum teste. O caso que distingue os dois filtros, uma não reembolsável
  cancelada (dinheiro retido, mas não "paga"), não está no teste da taxa. Uma reserva com check-out na taxa também não
  é conferida.
- **Planos misturados.** Nenhum teste tem os dois planos com reservas no mesmo mês. Mesmo assim, apagar o filtro por
  plano (`RatePlanReport.java:33`) derrubaria o teste de abril, que espera `NON_REFUNDABLE,0`.

São lacunas pequenas, diante de uma parte nova conferida com valores.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. Não há
chave estrangeira sem índice. Um `EXCLUDE USING gist` impede duas reservas do mesmo quarto nas mesmas noites. Há 1
migração do Flyway, `V1__create_tables.sql`, igual à da base (`measures/database.md`). O agente não mexeu no banco.
**Nota:** bom

- **Sem migração nova.** O pedido não precisava de coluna nova: tudo o que os relatórios usam (hóspedes, datas, plano,
  valor retido) já estava na reserva, e a migração da base ficou intacta.
- **SQL da base.** Os SQL montados com `+` que o script lista (`src/main/java/hotel/reservations/ReservationStore.java:33,83,98,111`)
  são da base e só concatenam constantes (`COLUMNS`).
- **Carga, herdada da base.** Os relatórios novos leem a tabela inteira (`store.all()`) e filtram em Java
  (`src/main/java/hotel/reports/MonthlyReservations.java:31`). É o mesmo jeito que a base já usava no financeiro e na
  ocupação (`OccupancyReport.java:32`), não piora.
- **CHECK ausente, herdado da base.** As colunas `status` e `rate_plan` sem CHECK também vêm da base.

## Comportamento

**Números:** 64 de 64 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 46 de 46 |
| dados depois de reiniciar | 3 de 3 |
| cenário 09 | 13 de 13 |
| reservas simultâneas | 2 de 2 |

**Nota:** bom

Nada falhou. Para o dono da pousada, isso quer dizer:

- Os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>".
- A ocupação e o financeiro continuam iguais depois do título.
- A taxa de turismo cobra R$ 4,50 por hóspede por noite das reservas pagas, na ordem do financeiro.
- O relatório por plano traz STANDARD e NON_REFUNDABLE mesmo sem reserva, com o total no fim.
- O mês fora do formato é recusado com 400.
- O resto do sistema (preços, cancelamentos, e-mails, dados depois de reiniciar, reservas simultâneas) continua
  funcionando.

## Dúvidas

- O texto não diz o que fazer com uma não reembolsável cancelada na taxa de turismo: o dinheiro ficou com a pousada,
  mas a reserva não é "paga". O agente seguiu a definição literal do pedido ("as confirmadas, as que já fizeram
  check-in e as que já fizeram check-out") e a deixou fora (`MonthlyReservations.java:25-27`, via
  `Reservation.isPaid`). O roteiro aceitou essa leitura, mas nenhum teste do agente a fixa.
- Deixar a taxa como constante no código (`Hotel.java:22`), e não como configuração, é uma escolha defensável. Mesmo
  assim, a troca anual pedida vai exigir uma nova versão do sistema.
