# Revisão: construção, cenário 09

## Visão geral

O agente fez um serviço Javalin com seis pacotes: `pousada` (o `Main`), `web` (a montagem em `App` e as rotas),
`domain` (registros imutáveis `Reservation`, `Room` e `Receipt`, os enums, `Pricing`, `Money` e o `ReservationService`),
`report` (os quatro relatórios e o escritor de CSV), `db` (o pool Hikari, as transações e os repositórios com JDBC) e
`error` (a `ApiException`). Uma requisição entra numa rota de uma linha (`web/ReservationRoutes.java:14-20`), passa
pelo `ReservationService`, que abre a transação, trava a linha do quarto ou da reserva com `FOR UPDATE` e chama a
transição do registro (`domain/ReservationService.java:23-76`, `domain/Reservation.java:33-67`). Os dados ficam em três
tabelas do PostgreSQL, criadas por `CREATE TABLE IF NOT EXISTS` no código (`db/Schema.java:12-48`). Os quatro
relatórios passam por um mesmo esqueleto (`report/CsvReportWriter.java:12-20`). As 64 conferências do comportamento
passaram, e os 14 testes também. Nada no projeto quebra o sistema. O que pesa: dois ciclos entre pacotes (o domínio lê
JSON pelo `web.Json` e depende das classes concretas do `db`), o esquema sem migrações e sem `CHECK`, e testes que
deixam sem conferir parte das regras novas dos relatórios.

## Arquitetura

**Números:** 6 pacotes e 13 dependências entre eles, com 2 ciclos: `domain ↔ db` e `domain → web → report → domain`
(de `measures/dependencies.md`). A maior classe é `ReservationRepository`, com 145 linhas. O Javalin só aparece em
`pousada` e `web`, e o JDBC só em `db`.
**Nota:** com problemas

- A borda HTTP é fina. Cada rota lê a requisição, chama o serviço e escreve a resposta
  (`web/ReservationRoutes.java:14-20`). As rotas dos relatórios são um laço genérico (`web/ReportRoutes.java:13-19`).
  Nenhum handler tem regra de negócio nem SQL. Duas rotas chamam o repositório direto (`web/App.java:49-50`), o que é
  aceitável para leituras sem regra.
- As regras não conhecem o Javalin nem o JDBC: preço em `domain/Pricing.java:16-38`, plano em
  `domain/RatePlan.java:18-24`, situação em `domain/Reservation.java:33-67`. O SQL fica todo em `db/`.
- A montagem fica num lugar só (`web/App.java:29-53`), e o `Main` só lê as variáveis de ambiente
  (`Main.java:11-16`). A taxa de turismo entra pelo construtor (`web/App.java:24,38`).
- O primeiro ciclo (`domain → web`) vem de um único ponto: o registro de domínio `NewReservation` lê o corpo HTTP em
  JSON com o `ObjectMapper` da camada web (`domain/NewReservation.java:6-10,23-31`). Ler o corpo é trabalho da borda,
  não do domínio.
- O segundo ciclo (`domain ↔ db`) existe porque o `ReservationService` recebe e usa as classes concretas
  `Database`, `RoomRepository` e `ReservationRepository` (`domain/ReservationService.java:5-7,11-21`), enquanto os
  repositórios devolvem os tipos do domínio. Para o tamanho do sistema, esse acoplamento é tolerável, mas o ciclo
  existe.
- A `ApiException` guarda o status HTTP (`error/ApiException.java:7,22-44`), e o domínio a lança
  (`domain/Reservation.java:7,63`). Isso leva números do protocolo HTTP para dentro das regras. É pequeno.

As camadas existem e estão bem separadas. Os dois ciclos são localizados e fáceis de desfazer, e por isso a nota é
"com problemas", não "ruim".

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo mesmo esqueleto (`CsvReportWriter.write`), e 0 escrevem a linha de título
por conta própria. Os 3 relatórios mensais herdam um método-modelo `final` de `MonthlyReport`; a ocupação implementa
`Report` direto.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

**O pattern.** O esqueleto dos relatórios fica num lugar só: `CsvReportWriter.write`
(`report/CsvReportWriter.java:12-20`) escreve, nesta ordem, o título (`Pousada.NAME`, o nome e o período), o
cabeçalho, as linhas e o total. O comentário da linha 18 marca onde entraria a linha de rodapé com a data que o dono
anunciou. Cada relatório só preenche as partes que mudam: o nome, o parâmetro e um `ReportContent` com período,
cabeçalho, linhas e total (`report/Report.java:4-13`, `report/ReportContent.java:5`). Os quatro relatórios, inclusive
a ocupação e o financeiro, passam pelo escritor no laço de `web/ReportRoutes.java:14-18`, e nenhum repete a sequência
de passos. Isso é a composição que a referência aceita como alternativa equivalente.

Por cima disso, há um Template Method de verdade para os relatórios mensais: `MonthlyReport.generate(String)` é
`final`, lê o mês, responde 400 se o formato estiver errado e chama o passo abstrato `generate(YearMonth)`
(`report/MonthlyReport.java:24-35`). A base comum do financeiro e dos planos também fica ali
(`report/MonthlyReport.java:38-40`). Os três mensais são subclasses (`RevenueReport`, `TourismTaxReport` e
`RatePlanReport`). A ocupação implementa `Report` direto (`report/OccupancyReport.java:16`). Por isso a classe é
alternativa equivalente, e não acerto: o esqueleto do CSV está no escritor, por composição, e não numa classe
abstrata com os quatro como subclasses.

**O exagero.** Não achei estrutura sobrando. `Report`, `MonthlyReport`, `ReportContent` e `CsvReportWriter` fazem
parte da solução do pattern. A interface `Notifier` tem uma implementação só (`domain/Notifier.java:4`,
`db/MessageRepository.java:11`), mas o texto diz que os e-mails "por enquanto" não saem de verdade, e a interface
também mantém o serviço longe do repositório de mensagens. `Database.SqlWork` (`db/Database.java:25-27`) só existe
para permitir lambdas.

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` em cada passo (`domain/Money.java:16-18`,
`domain/Pricing.java:20-28`, `domain/RatePlan.java:18-20`), `LocalDate` e `YearMonth` para datas, enums para
situação, plano, tipo e canal. Os registros são imutáveis, e as transições validam a situação de origem num único
`moveTo` (`domain/Reservation.java:61-67`). Também não há `static` mutável. Os defeitos são pequenos e localizados:

- duas regras repetidas: a soma do que ficou com a pousada (`report/RevenueReport.java:28` e
  `report/RatePlanReport.java:36-38`) e a leitura do período com `try/catch` (`report/OccupancyReport.java:38-42` e
  `report/MonthlyReport.java:26-30`);
- `NullPointerException` capturada como controle de fluxo quando falta o parâmetro
  (`report/MonthlyReport.java:29`, `report/OccupancyReport.java:40`);
- a leitura do JSON dentro do domínio (`domain/NewReservation.java:23-31`), já contada na arquitetura.

Nada disso dificulta mudar o código.

## Testes

**Números:** 14 testes rodaram, com 0 falhas, 0 erros e 0 pulados; 14 métodos escritos em 3 classes. Cobertura: 93,7%
das linhas (415 de 443) e 82,6% dos ramos (71 de 86); `report` tem 80 de 81 linhas (de `measures/tests.md`). Há
testes de unidade e um teste pela API contra o PostgreSQL de verdade, num schema próprio
(`ApiIntegrationTest.java:27-58`).
**Nota:** com problemas

O que está conferido com valores:

- **Preço:** a alta temporada (`PricingTest.java:20-24`, 599.76), as bordas 14/12, 15/12, 29/02 e 01/03
  (`PricingTest.java:26-33`), o desconto com arredondamento (`PricingTest.java:36-41`, 89.96 e 509.80), o "meio para o
  par" (`PricingTest.java:44-47`) e o formato R$ (`PricingTest.java:50-53`).
- **Texto:** o comprovante e o e-mail, texto inteiro, inclusive "1 diária" e a linha do desconto
  (`ReceiptAndReportFormatTest.java:32-56`).
- **Ciclo da reserva pela API:** as recusas 409, o e-mail e os quatro relatórios com o CSV inteiro, título incluído
  (`ApiIntegrationTest.java:86-133`).
- **Cancelamento:** a devolução por plano e o financeiro com a não reembolsável cancelada
  (`ApiIntegrationTest.java:136-155`).
- **Erros e conflitos:** todos os códigos de erro (`ApiIntegrationTest.java:162-175`) e a reserva emendada
  (`ApiIntegrationTest.java:178-182`).

Onde a rede falha é na parte nova do cenário:

- **Taxa de turismo:** só é conferida com uma única reserva paga (`ApiIntegrationTest.java:127-129`). Se alguém
  apagasse o filtro `r.status().paid()` (`report/TourismTaxReport.java:26`), nenhum teste falharia, porque nenhum mês
  testado tem uma reserva pendente ou cancelada junto com a taxa.
- **Planos:** só são conferidos com STANDARD 1 e NON_REFUNDABLE 0 (`ApiIntegrationTest.java:130-132`). A regra
  "contar as mesmas reservas do financeiro, inclusive a não reembolsável cancelada" não é conferida nesse relatório:
  se `RatePlanReport` usasse só as reservas pagas, o teste continuaria passando.
- **Ordem:** nenhum relatório é testado com mais de uma reserva, então a ordem (data de entrada e número) fica sem
  conferência.
- **Asserções fracas:** o comprovante do check-out só confere o título e o tipo (`ApiIntegrationTest.java:120-121`), e
  a segunda via só confere o status 200 (`ApiIntegrationTest.java:122`).
- **Concorrência:** as reservas simultâneas não são testadas.

A trava em si funciona: o comportamento confirmou.

Um detalhe: `roomsAreListed` confere o dinheiro com `asDouble()` (`ApiIntegrationTest.java:82`), o que não prova as
2 casas.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. As 6 colunas de dinheiro são
`numeric(12,2)`, as datas são `date`, e a chave estrangeira `reservations.room_number` tem índice. As conferências
acusaram: 0 restrições que protegem uma regra (nenhum `UNIQUE`, `EXCLUDE` ou `CHECK`), 4 colunas de situação ou tipo
sem `CHECK`, e o esquema criado por `CREATE TABLE` no código, com 0 arquivos de migração (de `measures/database.md`).
**Nota:** com problemas

- **Tipos e chaves:** estão certos (`db/Schema.java:14-48`). `NOT NULL` em tudo o que é obrigatório; `refund` aceita
  nulo de propósito, porque é `null` antes do cancelamento (`db/Schema.java:37`). Os índices servem às consultas de
  conflito e de relatório mensal (`db/Schema.java:39-40`).
- **Evolução:** o esquema não evolui. São só `CREATE TABLE IF NOT EXISTS` (`db/Schema.java:12-48`), sem migrações
  versionadas. O próximo pedido que acrescentar uma coluna não terá como alterar um banco que já existe. A trava
  `pg_advisory_xact_lock` impede só a corrida de duas instâncias subindo juntas (`db/Schema.java:63-65`).
- **Restrições:** o banco não protege nenhuma regra. Não há `CHECK` em `status`, `rate_plan`, `type`, `channel`,
  `guests >= 1` nem `check_out > check_in`, nem um `EXCLUDE` para o conflito de noites.
- **Reserva dupla:** é impedida por uma trava no banco, e não dentro do processo. O serviço trava a linha do quarto com
  `SELECT ... FOR UPDATE` dentro da transação (`db/RoomRepository.java:39-41`,
  `domain/ReservationService.java:24-34`). Isso serializa as reservas do mesmo quarto mesmo entre processos. O
  comportamento confirmou: 10 de 10 rodadas sem duplicação.
- **Código de acesso:**
  - Todo o SQL usa `?`. Os `+` que o script listou só juntam constantes, como `SELECT` e `COLUMNS` com
    `" FOR UPDATE"` (`db/ReservationRepository.java:76,81,110`, `db/RoomRepository.java:23,45`).
  - Conexões em try-with-resources, com pool Hikari (`db/Database.java:15-23,34`).
  - Uma transação por operação, com a conexão presa à thread (`db/Database.java:41-61`). A confirmação e o e-mail são
    gravados juntos (`domain/ReservationService.java:41-48`).
  - Se o `rollback` falhar, a exceção original se perde (`db/Database.java:52-54`). É um detalhe.

## Comportamento

**Números:** 64 de 64 conferências passaram, 0 falharam (de `measures/behavior.md`):

- parte comum do enunciado: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 09, os relatórios: 13 de 13;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Tudo o que o roteiro conferiu funciona:

- o preço com a temporada e o "meio para o par";
- as recusas com os códigos certos e as transições;
- o e-mail com o comprovante;
- as devoluções;
- a ocupação e o financeiro;
- os dados depois de reiniciar;
- a trava contra a reserva dupla.

Na parte nova, os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>". A taxa de turismo conta só as
reservas pagas do mês, e o mês vazio sai com total 0.00. O relatório de planos traz STANDARD e NON_REFUNDABLE mesmo
sem reserva, e um mês fora do formato dá 400. Para o dono da pousada, os quatro relatórios saem como ele pediu, e o
sistema não deixa vender duas vezes o mesmo quarto.

## Dúvidas

- **Classe do pattern:** dei "alternativa equivalente" porque o esqueleto do CSV está num escritor que recebe as
  partes que variam, e não numa classe abstrata com os quatro relatórios como subclasses. Ao mesmo tempo, há um
  Template Method legítimo (`MonthlyReport`, com método `final`) para três deles. Um leitor que valorize mais essa
  parte poderia dar "acerto".
- **Nota dos testes:** fica entre "bom" e "com problemas". A cobertura é alta e as regras comuns estão conferidas com
  valores, mas dois filtros da parte nova ficam sem conferência: as pagas na taxa e a não reembolsável cancelada nos
  planos.
