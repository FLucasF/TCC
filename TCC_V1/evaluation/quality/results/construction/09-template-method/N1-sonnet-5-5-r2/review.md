# Revisão: construção, cenário 09

## Visão geral

O projeto tem seis pacotes: `pousada` (a montagem em `App`), `web` (as rotas do Javalin e as views), `service`
(a reserva, o comprovante e o e-mail), `report` (os quatro relatórios e o gerador comum), `domain` (a reserva, o preço,
a temporada, os planos e os quartos, sem Javalin nem JDBC) e `persistence` (o pool Hikari, a transação `Tx` e os
repositórios com JDBC puro). Uma requisição entra numa rota curta de `web/Api.java`, que chama
`ReservationService` ou `ReportService`. O serviço abre uma transação em `Database.inTransaction`, trava o que precisa
(trava consultiva por quarto, `FOR UPDATE` na reserva), aplica a regra no objeto `Reservation` e grava. Os dados ficam
em duas tabelas do PostgreSQL (`reservations` e `messages`), criadas por `CREATE TABLE IF NOT EXISTS` no código. Os
quatro relatórios passam todos pelo mesmo esqueleto (título, cabeçalho, linhas, total). Não achei defeito sério. O
ponto mais fraco é o banco: não há migrações, não há tabela de quartos nem chave estrangeira, e nenhuma restrição
`CHECK`.

## Arquitetura

**Números:** 6 pacotes, 13 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece em `pousada` e `pousada.web`, e o JDBC só em `pousada.persistence`. A maior classe é `ReservationRepository`, com 144 linhas.
**Nota:** bom

- **A borda HTTP é fina.** Cada rota lê a requisição, chama o serviço e escreve a resposta em uma linha
  (`src/main/java/pousada/web/Api.java:46-58`). As rotas dos relatórios são registradas num laço sobre a lista de
  relatórios (`Api.java:60-64`). A validação do corpo fica separada em `web/ReservationRequestParser.java:22-43`.
- **As regras não conhecem Javalin nem SQL.** A mudança de situação, o pagamento e a devolução estão em
  `domain/Reservation.java:56-76`. O preço está em `domain/Quote.java:9-18`, `domain/Season.java:17-26` e
  `domain/RatePlan.java:17-23`. Os relatórios recebem uma `ReportSource` (`report/ReportSource.java:9-28`) e nunca
  veem o JDBC.
- **O acesso ao banco fica isolado** em `persistence/ReservationRepository.java` e `MessageRepository.java`, atrás
  de `Tx` (`persistence/Tx.java:16-28`).
- **A montagem acontece num lugar só,** `App.main` (`App.java:21-35`), que cria o banco, o catálogo, os relatórios e
  os serviços e os liga.
- **Os pontos menores:**
  - `domain/ErrorCode.java:4-9` guarda o status HTTP dentro do domínio. Isso não chega a ser uma dependência do
    Javalin, mas é um detalhe da borda morando nas regras.
  - `ReportService` (`report/ReportService.java:16-30`) fica no mesmo pacote dos relatórios e conhece o `Database`.
    Por isso existe a dependência `report -> persistence`. Isso não gera ciclo.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo esqueleto comum (`CsvWriter.write`, chamado por `ReportService.generate`); nenhum relatório escreve o título, nem o CSV, por conta própria.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

**O pattern.** O agente não fez uma classe abstrata com método-modelo. Fez a forma por composição, que a referência
aceita como equivalente:

- O esqueleto mora num lugar só, `report/CsvWriter.java:13-24`, e a ordem é: título
  `Establishment.NAME, report.name(), period.label()` (linha 15), cabeçalho (16), linhas (17) e total (18).
- `report/ReportService.java:16-30` conduz o processo: lê o período, abre a transação, pede a tabela ao relatório e
  escreve o CSV.
- Cada relatório só implementa o que varia (`report/Report.java:3-10`): o nome, o tipo de período e
  `build(period, source)`, que devolve uma `ReportTable` com o cabeçalho, as linhas e o total. Ninguém pergunta qual
  é o relatório.
- Os quatro relatórios, os dois da parte comum e os dois novos, passam por ele: `OccupancyReport.java:26-38`,
  `RevenueReport.java:21-35`, `TourismTaxReport.java:27-42` e `RatePlanReport.java:22-32`.
- A linha com a data que o dono pede para o ano que vem entra numa linha só, em `CsvWriter.java:18-19`.
- Um relatório novo custa uma classe e uma linha em `App.java:27-31`.
- A taxa da prefeitura vem pelo construtor (`TourismTaxReport.java:12-14`), com o padrão 4.50 lido de variável de
  ambiente (`App.java:30`).
- O tipo de período (noite ou mês) também está fora dos relatórios: `report/PeriodKind.java:11-53` faz a leitura e a
  validação do parâmetro.

O que afasta do Template Method clássico: o esqueleto não é um método `final` de uma superclasse. Mesmo assim, a
sequência dos passos não se repete em nenhum relatório, e por isso a classe é alternativa equivalente.

**O exagero.** Não há estrutura sem uso:

- `ReportSource` tem duas implementações: a de produção, em `ReportService.java:18-28`, e a falsa dos testes, em
  `ReportsTest.java:31-43`.
- `PeriodKind` tem dois casos de verdade.
- `Period` e `ReportTable` são registros simples que carregam dados.
- `Tx` agrupa a conexão e os repositórios de uma transação.

**O resto do código.** O código está limpo:

- O dinheiro é `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:13-15`, `Season.java:24-26`,
  `RatePlan.java:17-19`, `Quote.java:14-17`).
- As datas são `LocalDate`, e a situação e o plano são `enum` com o comportamento dentro (`ReservationStatus.isPaid`,
  `RatePlan.refundFor`).
- As transições protegem a situação em um lugar só (`Reservation.java:94-99`).
- O serviço repete a sequência travar, mudar, gravar e completar uma vez só, em
  `service/ReservationService.java:83-90`.

Há só detalhes:

- A mesma soma do valor retido aparece em `RevenueReport.java:23` e `RatePlanReport.java:35`.
- O bloco `catch (SQLException) -> IllegalStateException` se repete em cada método dos repositórios.
- `Reservation` tem um construtor público de 16 parâmetros (`Reservation.java:25-28`).
- `Reservation.occupies` (`Reservation.java:90-92`) só é usado pelos testes.

## Testes

**Números:** 16 testes rodaram, 0 falharam, 0 foram pulados; 16 métodos de teste escritos. A cobertura é de 92,1% das linhas (515 de 559) e 76,7% dos ramos (66 de 86). Fora `pousada.App`, que não tem cobertura, os pacotes ficam entre 87% e 100% das linhas (`measures/tests.md`).
**Nota:** bom

São três níveis: unidade do domínio, os relatórios contra uma fonte falsa, e a API de verdade contra o PostgreSQL de
`DB_URL`, num schema próprio (`ApiTest.java:45-59`). As regras são conferidas com valores:

- **A temporada:** 599.76 em `DomainTest.java:36-42`, e as fronteiras de 14/12, 15/12, 29/02, 01/03, 31/07 e 01/08
  em `DomainTest.java:44-52`.
- **O arredondamento meio para o par:** desconto de 116.97, e 86.98 sobre 86.985 (`DomainTest.java:54-61, 114-128`).
- **A ordem das situações e a devolução por plano,** inclusive a pendente cancelada (`DomainTest.java:63-96`).
- **O comprovante com o texto exato** (`DomainTest.java:98-112`).
- **Os códigos de erro pela API** (`ApiTest.java:160-175`).

A parte nova do cenário tem o CSV inteiro comparado, título incluído:

- a ocupação, com quem sai no dia e a pendente fora (`ReportsTest.java:60-73`);
- o financeiro, com a NON_REFUNDABLE cancelada dentro e a STANDARD devolvida fora (`ReportsTest.java:75-92`);
- a taxa, com 2 hóspedes e 3 noites dando 27.00 e a cancelada fora (`ReportsTest.java:94-106`);
- os planos, com o NON_REFUNDABLE zerado aparecendo (`ReportsTest.java:108-118`).

Pela API também: a taxa dá 18.00 (`ApiTest.java:135`) e os planos dão `STANDARD,0,0.00` e `NON_REFUNDABLE,1,212.42`
(`ApiTest.java:155-156`).

A pergunta da mutação:

- Trocar a fórmula da taxa ou a ordem do título derruba `ReportsTest`.
- Trocar o filtro `retained() > 0` por `>= 0` faria a STANDARD devolvida aparecer, e isso é pego em
  `ReportsTest.java:85-91` e `ApiTest.java:155-156`.

O que fica sem rede:

- o valor padrão de 4.50 em `App.java:30`, porque `App` tem 0% de cobertura e os testes passam o 4.50 à mão;
- uma reserva pendente no relatório da taxa (só a cancelada é testada);
- a disputa de duas reservas ao mesmo tempo.

## Banco

**Números:** 2 tabelas (`reservations` com 16 colunas, `messages` com 5), as duas com chave primária. O dinheiro está todo em `numeric(12,2)` e as datas em `date`. As conferências acusaram 0 chaves estrangeiras, 0 restrições (CHECK, UNIQUE ou EXCLUDE), `status`, `rate_plan` e `channel` em `text` sem CHECK, e o esquema criado por `CREATE TABLE` no código, sem migração (`measures/database.md`).
**Nota:** com problemas

O que está certo:

- os tipos;
- o SQL sempre com `?`: o único texto concatenado é a constante `COLUMNS`, em `ReservationRepository.java:19-20, 75, 80, 98, 104`;
- o pool Hikari (`persistence/Database.java:13-21`);
- a transação com `rollback` em qualquer erro (`Database.java:33-47`);
- a confirmação e o e-mail gravados na mesma transação (`ReservationService.java:53-56`).

A dupla reserva é barrada entre processos:

- uma trava consultiva por quarto, `pg_advisory_xact_lock`, em `Tx.java:35-51` e `ReservationService.java:39`;
- antes da conferência de conflito, em `ReservationRepository.java:29-42`;
- e cada mudança de situação trava a linha com `FOR UPDATE` (`ReservationRepository.java:79-81`).

Os defeitos:

- **O esquema não evolui.** Ele nasce de `CREATE TABLE IF NOT EXISTS` em `persistence/Schema.java:13-41`, sem
  migrações versionadas. A primeira coluna nova que o dono pedir (os hóspedes por estado, por exemplo) não chega a
  um banco que já tem a tabela.
- **Não há tabela de quartos.** `room_number INT NOT NULL` (`Schema.java:16`) não tem chave estrangeira, e os quartos
  vivem só no código (`RoomCatalog.java:13-20`).
- **O banco não protege nenhuma regra.** Não há `CHECK` na situação, no plano, em `guests >= 1` ou em
  `check_out > check_in`, nem uma restrição `EXCLUDE` contra a sobreposição de noites. A regra de não haver duas
  reservas no mesmo quarto depende de todo código que grava passar pela trava.

## Comportamento

**Números:** 64 de 64 conferências passaram, e nenhuma falhou (`measures/behavior.md`):

- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 09: 13 de 13;
- as reservas simultâneas: 2 de 2, nas 10 rodadas.

**Nota:** bom

Tudo o que o texto pede funcionou pela API, a partir de um banco vazio:

- o preço da alta temporada (599.76) e o desconto meio para o par (37.48, total 212.42);
- as recusas, com os códigos e status certos;
- a confirmação com o e-mail e o comprovante;
- o check-in e o check-out;
- as devoluções por plano;
- os dados que continuam depois de reiniciar;
- uma só reserva criada quando duas chegam juntas.

Nos relatórios, os quatro começam com a linha `Pousada Maré Alta,<nome>,<período>`:

- a taxa de turismo lista só as pagas, na ordem do financeiro, e o total de um mês vazio dá 0.00;
- o de planos mostra o STANDARD e o NON_REFUNDABLE mesmo sem reserva;
- um mês fora do formato dá 400.

Para o dono da pousada, o sistema faz o que ele descreveu.

## Dúvidas

- Na classe do pattern, fiquei entre `acerto` e `alternativa equivalente`. O esqueleto está num lugar só e cobre os
  quatro relatórios, mas por composição: `CsvWriter` mais `ReportService`, com a interface `Report` devolvendo uma
  `ReportTable`. Não é uma superclasse abstrata com método-modelo. Pelo critério da referência, isso é alternativa
  equivalente.
- O código não garante o comportamento com uma `TOURISM_TAX_PER_GUEST_NIGHT` com mais de duas casas: o valor é
  arredondado por reserva em `TourismTaxReport.java:45`, e o texto não fala disso.
