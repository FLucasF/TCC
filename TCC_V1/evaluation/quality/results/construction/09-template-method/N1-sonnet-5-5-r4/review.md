# Revisão: construção, cenário 09

## Visão geral

O projeto tem seis pacotes: `web` (as rotas do Javalin e a leitura do corpo), `service` (o `ReservationService`, que
cuida das mudanças de situação dentro de transações), `domain` (reserva, preço, temporada, planos, comprovante,
mensagem, sem Javalin e sem JDBC), `infra` (o pool Hikari, a criação do esquema e os dois repositórios com SQL com
parâmetros) e `report` (os quatro relatórios em CSV). Uma requisição entra em `Api` (`web/Api.java:51-59`), o corpo é
validado em `CreateReservationRequest`, o serviço abre a transação com `Db.inTransaction`, trava o quarto com
`pg_advisory_xact_lock` (`infra/ReservationRepository.java:21-26`) e grava em PostgreSQL (tabelas `reservations` e
`messages`). Os relatórios seguem um Template Method limpo: `Report.render` é `final` e monta título, cabeçalho,
linhas e total para os quatro (`report/Report.java:38-50`). O roteiro do comportamento passou inteiro (64 de 64). O
que há de mais sério é pouco: o esquema é criado com `CREATE TABLE IF NOT EXISTS` no código, sem migrações nem
restrições (`infra/Db.java:17-45`), e os testes, que são poucos e longos, não protegem os filtros por situação dos
relatórios.

## Arquitetura

**Números:** 6 pacotes, 11 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). `domain` não depende
de nenhum pacote do projeto e é usado por 4. O Javalin aparece só em `web` e no `Main`. A maior classe é o
`ReservationRepository`, com 132 linhas.
**Nota:** bom

- A borda HTTP é fina: cada rota lê o id ou o corpo, chama um método do serviço e serializa
  (`web/Api.java:48-59`). A validação do corpo fica separada em `web/CreateReservationRequest.java:23-45`, que entrega
  um `NewReservation` sem tipos HTTP.
- As regras ficam no domínio, sem Javalin e sem SQL: as transições em `domain/ReservationStatus.java:15-23` e
  `domain/Reservation.java:46-75`, o preço em `domain/Quote.java:8-18` e `domain/Season.java:22-25`, a devolução em
  `domain/RatePlan.java:22-24`.
- O SQL fica isolado em `infra/ReservationRepository.java` e `infra/MessageRepository.java`. O serviço só coordena a
  transação (`service/ReservationService.java:96-104`) e não escreve SQL.
- Pontos menores, que não chegam a ser defeito neste tamanho: a montagem fica no construtor da `Api`
  (`web/Api.java:29-34`) e não no `Main`, e a `Api` cria um segundo `ReservationRepository` para os relatórios
  (`web/Api.java:61`). A rota dos relatórios abre a transação ela mesma e passa o `Connection` para o relatório
  (`web/Api.java:63`), sem passar pelo serviço; por isso `report` usa JDBC (`report/Report.java:36`). Como os
  repositórios não têm estado, isso não causa erro, e as dependências continuam indo da borda para os dados.

## Design de baixo nível

**Números:** os 4 relatórios dentro da estrutura e nenhum fora dela. `Report<P>` tem o método-modelo `final render`
e 4 passos abstratos (`name`, `parameter`, `parse`, `build`). `MonthlyReport` cuida do parâmetro `month`, comum a 3
relatórios, e `Reports.all` registra os 4, de onde saem as rotas.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

- **O pattern:** `Report.render` (`report/Report.java:38-50`) é `final` e é o único lugar que escreve a linha de
  título (`Report.java:45`), o cabeçalho, as linhas, o total e o escape do CSV (`Report.java:56-63`). Cada relatório só
  diz o nome e monta o miolo numa `Table` (`report/Table.java:6`): `OccupancyReport.java:41-48`,
  `RevenueReport.java:34-44`, `TourismTaxReport.java:27-39` e `RatePlanReport.java:28-38`. Os dois relatórios da parte
  comum também estão nessa estrutura. A linha final com a data, que o dono vai pedir no ano que vem, entra só em
  `render`. Um relatório novo é uma subclasse e uma linha em `Reports.all` (`report/Reports.java:12-18`), e a rota
  sai sozinha (`web/Api.java:61-66`). A taxa de turismo também ficou num lugar só (`domain/TourismTax.java:7`).
- **O exagero:** não há. `MonthlyReport` (`report/MonthlyReport.java:7-22`) evita que três relatórios repitam a mesma
  leitura do mês. `Table` é um record de três campos. A única interface pequena é privada e serve para os efeitos
  depois de uma mudança de situação (`service/ReservationService.java:90-93`). Não há interface com uma implementação
  só, nem fábrica, nem camada que só repassa.
- **O resto do código:** o dinheiro é `BigDecimal` com `HALF_EVEN` num lugar só (`domain/Money.java:20-22`), as datas
  são `LocalDate` com parse estrito (`web/CreateReservationRequest.java:17-18`), situação, plano, tipo e canal são
  `enum`. As mudanças de situação passam todas por `change` (`service/ReservationService.java:96-104`) e por `moveTo`
  (`domain/Reservation.java:69-75`). Há algumas miudezas: `RatePlanReport` reaproveita a seleção do financeiro por um
  método `static` de pacote (`report/RevenueReport.java:26-31`, `report/RatePlanReport.java:29`); a soma do dinheiro
  retido aparece duas vezes (`RevenueReport.java:36`, `RatePlanReport.java:40-42`); a taxa é calculada duas vezes por
  linha (`TourismTaxReport.java:31` e `:35`); o construtor de `Reservation` tem 16 parâmetros
  (`domain/Reservation.java:24-27`). Nada disso torna o código difícil de mudar.

## Testes

**Números:** 8 testes rodaram, 0 falharam, 0 foram pulados. São 8 métodos escritos em 2 arquivos (6 de unidade em
`DomainTest`, 2 pela API contra o PostgreSQL em `ApiIntegrationTest`). Cobertura de 94,6% das linhas (441 de 466) e
73,3% dos ramos (74 de 101); no `report`, 84 de 87 linhas e 8 de 14 ramos (`measures/tests.md`).
**Nota:** com problemas

- **O que está conferido com valores:** a alta temporada e o total de 599,76
  (`src/test/java/br/com/mare/pousada/DomainTest.java:13-17`); as bordas da temporada, inclusive 14 e 15/12 e
  29/02 (`DomainTest.java:20-28`); o desconto de 116,97 (`DomainTest.java:31-36`); o texto inteiro do comprovante
  (`DomainTest.java:39-51`); a taxa de 27,00 (`DomainTest.java:54-59`); o não reembolsável cancelado
  (`DomainTest.java:62-71`). Pela API: os códigos 409, 422, 404 e 400
  (`src/test/java/br/com/mare/pousada/ApiIntegrationTest.java:77-82`), e os quatro CSVs comparados por inteiro, com
  título, cabeçalho, linhas e total (`ApiIntegrationTest.java:94-95`, `:104-112`), inclusive
  `NON_REFUNDABLE,0,0.00` no relatório de planos. Trocar 4,50 por outro valor, o nome de um relatório ou a ordem dos
  planos derruba algum teste.
- **O que fica sem rede (a pergunta da mutação):** nos meses que os testes consultam, nunca há uma reserva pendente
  ou cancelada. Por isso apagar o `.filter(r -> r.getStatus().isPaid())` da taxa de turismo
  (`report/TourismTaxReport.java:29`) ou da ocupação (`report/OccupancyReport.java:43`), ou o `signum() > 0` do
  financeiro (`report/RevenueReport.java:29`), não derruba nenhum teste. O não reembolsável cancelado só é conferido
  no financeiro (`ApiIntegrationTest.java:122`), e não no relatório de planos. A devolução do STANDARD e a do
  cancelamento pendente também não têm teste.
- **A forma:** quase todo o fluxo da API está num único método de cerca de 45 linhas (`ApiIntegrationTest.java:69-113`).
  A primeira asserção que falha esconde as outras. O teste de integração é pulado em silêncio quando falta `DB_URL`
  (`ApiIntegrationTest.java:36`): um `mvn verify` sem banco passa sem testar a API.

## Banco

**Números:** 2 tabelas (`reservations`, 16 colunas; `messages`, 5), as duas com chave primária. As 5 colunas de
dinheiro são `numeric(12,2)` e as datas são `date`. Nenhuma chave estrangeira, nenhum CHECK, nenhum UNIQUE ou
EXCLUDE. O esquema é criado por `CREATE TABLE` no código, sem arquivo de migração. Não há SQL montado com `+`
(`measures/database.md`).
**Nota:** com problemas

- **O esquema não evolui:** ele é um texto com `CREATE TABLE IF NOT EXISTS` (`infra/Db.java:17-45`), executado ao
  subir (`Db.java:60-68`). O contador já anunciou relatórios novos (por exemplo, hóspedes por estado). Uma coluna
  nova numa tabela que já existe não entra por esse caminho, e não há Flyway nem migrações versionadas.
- **O banco não protege as regras:** `status`, `rate_plan` e `channel` são `text` sem CHECK (`Db.java:27`, `:32`,
  `:40`); `guests` não tem `CHECK (guests >= 1)` (`Db.java:26`). Os quartos ficam só no código
  (`domain/RoomCatalog.java:7-12`), então `room_number` não tem chave estrangeira. Como o dono diz que os quartos
  quase nunca mudam, isso é uma escolha defensável.
- **A sobreposição de reservas** é impedida por uma trava de transação por quarto (`pg_advisory_xact_lock`,
  `infra/ReservationRepository.java:21-26`), seguida da consulta de sobreposição (`ReservationRepository.java:28-41`)
  e do insert, na mesma transação (`service/ReservationService.java:44-51`). A trava vale entre processos, e o roteiro
  confirmou isso nas 10 rodadas simultâneas. Ainda assim, sem um `EXCLUDE` o banco aceita a sobreposição de quem
  escrever sem passar pelo serviço.
- **O código:** o SQL usa sempre parâmetros. As conexões vêm do Hikari e são fechadas com try-with-resources
  (`Db.java:49-57`, `:70-84`). As mudanças de situação usam `SELECT ... FOR UPDATE` (`ReservationRepository.java:71-78`),
  e a confirmação grava a reserva e a mensagem na mesma transação (`ReservationService.java:58-62`). Os relatórios
  filtram por situação em Java depois de buscar pelo índice de `check_in` (`Db.java:37`,
  `ReservationRepository.java:92-101`). Com o volume de uma pousada, isso não pesa.

## Comportamento

**Números:** 64 de 64 conferências passaram. Parte comum: 46 de 46. Dados depois de reiniciar: 3 de 3. Cenário 09
(os relatórios): 13 de 13. Reservas simultâneas: 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Nada falhou. Para o dono da pousada, isto quer dizer:

- os preços, inclusive a alta temporada, o desconto com arredondamento "meio para o par" e o total de 599,76, saem
  como no texto;
- as recusas trazem os códigos combinados com o site;
- a confirmação grava o e-mail com o comprovante;
- as devoluções seguem o plano;
- os quatro relatórios começam com `Pousada Maré Alta,<nome>,<período>`, a taxa de turismo conta só as reservas
  pagas do mês, o relatório de planos mostra também o plano sem reservas, e o não reembolsável cancelado continua no
  financeiro;
- as reservas e as mensagens continuam lá depois de reiniciar;
- duas reservas simultâneas do mesmo quarto nunca entram as duas.

## Dúvidas

- Nos testes, hesitei entre `bom` e `com problemas`. Os quatro relatórios são conferidos por inteiro com valores, e a
  cobertura é alta. Mas o filtro "só as pagas" da taxa de turismo, que é uma regra da parte nova, e os filtros da
  ocupação e do financeiro não têm nenhum teste que falhe se forem apagados. Por isso fiquei com `com problemas`.
