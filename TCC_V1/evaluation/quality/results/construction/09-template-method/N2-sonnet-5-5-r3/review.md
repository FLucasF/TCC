# Revisão: construção, cenário 09

## Visão geral

O projeto tem dois pacotes. Em `com.marealta` (23 classes, 869 linhas) estão a borda HTTP (`App`), a montagem
(`Main`, `App`), o domínio (`Reservation`, `ReservationStatus`, `RatePlan`, `Quote`, `Seasons`, `Money`, `Receipt`),
o serviço (`ReservationService`) e o acesso a dados (`ReservationRepository`, `MessageRepository`, `Database`). Em
`com.marealta.report` (8 classes, 299 linhas) estão os relatórios. Uma requisição entra numa rota de uma linha em
`App.java:38-57`. A rota chama o `ReservationService`, que busca a reserva no repositório, pede a mudança de situação
à própria `Reservation` (`Reservation.java:45-67`, que consulta a tabela de transições de
`ReservationStatus.java:10-22`) e grava com `UPDATE`. Os dados ficam no PostgreSQL, em duas tabelas criadas por
`CREATE TABLE IF NOT EXISTS` (`Database.java:14-40`). Os quartos ficam em código (`RoomCatalog.java:7-12`). Os quatro
relatórios são subclasses de `Report`, que tem o método-modelo `final render()` (`report/Report.java:12-19`). As 64
conferências do comportamento passaram e os 5 testes passam. O mais sério que achei:
- há um ciclo entre os dois pacotes, porque a borda HTTP mora no mesmo pacote do domínio;
- o esquema é criado no código, sem migrações;
- o que impede duas reservas na mesma noite é só um `synchronized` dentro do processo;
- a confirmação não usa transação.

## Arquitetura

**Números:** 2 pacotes (`com.marealta`, 23 classes e 869 linhas; `com.marealta.report`, 8 classes e 299 linhas),
2 dependências entre eles, 1 ciclo (`com.marealta` ↔ `com.marealta.report`). Javalin, JDBC e Jackson ficam todos em
`com.marealta`, e `report` não usa nenhuma biblioteca.
**Nota:** com problemas

O que está bem:
- **A borda HTTP é fina.** Cada rota lê o caminho ou o corpo, chama o serviço e converte o resultado em JSON
  (`App.java:40-49`). As rotas dos relatórios são geradas a partir do catálogo (`App.java:53-57`).
- **As regras não conhecem o Javalin nem o JDBC.** Preço em `Quote.java:10-21`, temporada em `Seasons.java:9-12`,
  desconto e devolução por plano em `RatePlan.java:6-26`, transições em `ReservationStatus.java:10-22`.
- **O SQL fica só nos repositórios** (`ReservationRepository.java`, `MessageRepository.java`, `Database.java`).
- **A montagem fica num lugar só:** `App.java:22-27`, com o `DataSource` vindo do `Main.java:11-13`.

Os defeitos:
- **O ciclo entre os pacotes.** O pacote raiz mistura a borda (`App`, que importa `ReportCatalog` e `ReportData` em
  `App.java:3-4`) com o domínio e os dados que os relatórios usam (`report/ReportData.java:3-5` importa
  `Reservation`, `ReservationRepository` e `RoomCatalog`, e `report/ReportCatalog.java:3` importa `ApiException`).
  Se o HTTP, o domínio e os dados ficassem em pacotes separados, o ciclo sumiria. Do jeito que está, as 23 classes
  do pacote raiz não têm fronteira entre si.
- **O domínio conhece códigos HTTP.** A `Reservation` lança `ApiException` com status 409 (`Reservation.java:64`,
  `ApiException.java:33-35`), e o serviço recebe o id como texto e trata o `NumberFormatException` vindo do caminho da
  URL (`ReservationService.java:28-33`).

As duas misturas são localizadas. Não há classe muito maior que as outras: a maior é `ReservationRepository`, com 129
linhas.

## Design de baixo nível

**Números:** 4 de 4 relatórios (`occupancy`, `revenue`, `tourism-tax`, `rate-plans`) são subclasses de `Report` e
passam pelo esqueleto `final render()`. Nenhum relatório escreve a linha de título, o cabeçalho ou o total por fora
da estrutura, e nenhum lugar pergunta qual relatório é.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A estrutura é um Template Method como a referência descreve:
- `Report.render()` é `final` e fixa a sequência título → cabeçalho → linhas → total (`report/Report.java:12-19`).
- A linha de título com "Pousada Maré Alta" fica num lugar só (`report/Report.java:10,14`).
- Os passos que mudam são abstratos: `name`, `period`, `header`, `rows`, `total` (`report/Report.java:25-33`).
- Os dois relatórios do ponto de partida também são subclasses: `OccupancyReport.java:8` e `RevenueReport.java:10`.
  Os dois novos também: `TourismTaxReport.java:10` e `RatePlansReport.java:12`.
- A linha final com a data, que o dono diz que vem no ano que vem, entraria em `render()` uma vez só.

Nada afasta do acerto.

**O exagero.** Não há estrutura sobrando. O `ReportCatalog` (`report/ReportCatalog.java:13-27`) é um mapa de
nome → parâmetro → construtor, que serve para gerar as rotas e validar o parâmetro num lugar só. Com quatro
relatórios e mais dois anunciados, ele se paga. O `ReportData` (`report/ReportData.java:12-31`) não é só um
repasse: ele junta os filtros que os relatórios dividem (`retainedIn`, `paidIn`, `paidOccupying`). Assim, o
financeiro e o de planos contam as mesmas reservas por construção (`RevenueReport.java:16`,
`RatePlansReport.java:18`).

**O resto do código.**
- Dinheiro é `BigDecimal` com `HALF_EVEN` em cada passo: `Money.java:12-14`, a noite de alta temporada em
  `Quote.java:15`, o desconto em `RatePlan.java:18` e o total em `Quote.java:20`.
- As datas são `LocalDate`, e a situação, o plano e o tipo de quarto são `enum`.
- A variação por plano fica no próprio enum (`RatePlan.java:6-26`), sem `switch` espalhado.
- As transições estão numa tabela (`ReservationStatus.java:10-18`).
- Os nomes dizem o que fazem: `retained`, `occupies`, `startStay`.

Pontos menores, que não mudam a nota:
- **Consulta ao banco no construtor.** Cada relatório consulta o banco dentro do próprio construtor
  (`OccupancyReport.java:16`, `RevenueReport.java:16`, `TourismTaxReport.java:19`).
- **Soma duplicada.** A soma do retido aparece duas vezes (`RevenueReport.java:43`, `RatePlansReport.java:44`).
- **Taxa fixa no código.** A taxa de turismo é uma constante (`TourismTaxReport.java:12`), embora o dono avise que
  ela muda todo ano. Ela está num lugar só, mas cada mudança pede um novo build.
- **Id mutável.** A `Reservation` recebe o id depois de criada (`Reservation.java:79`).
- **Erros sem contexto.** As `SQLException` viram `IllegalStateException` sem mensagem
  (`ReservationRepository.java:50`).

## Testes

**Números:** 5 testes rodaram, 0 falharam, 0 pulados; 5 métodos de teste escritos, todos em `AppTest.java`.
Cobertura de linhas: 432 de 497 (86,9%). Cobertura de ramos: 55 de 84 (65,5%). Em `report`, 92 de 94 linhas e 4 de 4
ramos. Os testes vão pela API (HttpClient), contra H2 em modo PostgreSQL, e não usam mocks.
**Nota:** com problemas

O agente testou por iniciativa própria, e as asserções conferem valores exatos. O que os testes cobrem:
- **Preço e dinheiro no JSON.** O preço da alta temporada (599.76) e o formato `"amountPaid":0.00` estão em
  `AppTest.java:63-72`.
- **E-mail.** O corpo do e-mail de confirmação é comparado inteiro (`AppTest.java:81-94`).
- **Recusas.** As recusas com os códigos estão em `AppTest.java:99-121`.
- **A parte nova do cenário.** O teste `reports` (`AppTest.java:124-163`) compara o CSV inteiro dos quatro
  relatórios, com a linha de título, a NON_REFUNDABLE cancelada que continua contando (397.70) e a taxa de 2 × 3 ×
  4,50 = 27.00.

A pergunta da mutação na parte nova:
- **Pegos.** Trocar 4.50 por outro valor, tirar a linha de título de `render()` ou mudar o filtro `retained() > 0`
  faria o teste `reports` falhar.
- **Pego também.** Trocar `isPaid()` por "não cancelada" no relatório da taxa incluiria a reserva 3, que está
  pendente, e mudaria o total.
- **Sem rede.** Ninguém testa a linha de um plano sem nenhuma reserva, com 0: no teste, os dois planos têm uma
  reserva cada (`AppTest.java:159-160`). Um código que só listasse os planos com reserva passaria.

Ficaram sem teste:
- **Check-in e check-out.** Nenhum teste chama o `check-out` nem confere o comprovante que ele devolve; o check-in só
  aparece como recusa (`AppTest.java:102`).
- **Devolução no STANDARD.** A devolução do STANDARD confirmado não é testada, só a do não reembolsável
  (`AppTest.java:106`).
- **O comprovante.** As linhas "Desconto: ..." e "1 diária" não são conferidas.
- **A borda da ocupação.** A regra "quem sai naquele dia não conta" não fica isolada: a reserva que sai no dia 12 já
  estava cancelada (`AppTest.java:126,130`).
- **O banco de verdade.** Os testes rodam no H2, e não no PostgreSQL de `DB_URL`, então não exercitam o banco real.

## Banco

**Números:** 2 tabelas (`reservations`, com 16 colunas, e `messages`, com 5), ambas com chave primária. Todo o
dinheiro é `numeric(12,2)`, e as datas são `date`. O script não acusou nada nas conferências de chave primária,
dinheiro, chave estrangeira sem índice e datas como texto. Ele acusou:
- 0 chaves estrangeiras;
- 0 CHECK, UNIQUE ou EXCLUDE;
- `status`, `rate_plan` e `channel` como `varchar` sem CHECK;
- 0 transações explícitas;
- 0 `FOR UPDATE`;
- o esquema criado por `CREATE TABLE` no código, sem migrações.

**Nota:** com problemas

Os tipos estão certos e o SQL usa parâmetros. O único SQL montado com `+` (`ReservationRepository.java:75-76`)
concatena só texto constante. As conexões são devolvidas com try-with-resources.

Os defeitos:
- **O esquema não evolui.** Ele nasce de `CREATE TABLE IF NOT EXISTS` em `Database.java:14-40`. A primeira coluna
  nova que o próximo pedido trouxer não será aplicada num banco que já existe.
- **Nada no banco impede duas reservas na mesma noite.** Não há restrição de exclusão. A proteção é o
  `synchronized` de `ReservationService.java:14`, que só vale para um processo; a conferência de reservas
  simultâneas passa por isso.
- **A confirmação não tem transação.** `ReservationService.java:38-40` grava a reserva e a mensagem em duas conexões
  separadas, com autocommit. Se a gravação da mensagem falhar, a reserva fica confirmada e paga sem o e-mail.
- **O quarto não está no banco.** Ele vive em código (`RoomCatalog.java:7-12`), e por isso `room_number` não tem
  chave estrangeira nem índice. A consulta de sobreposição (`ReservationRepository.java:74-80`) e a de ocupação não
  têm índice.
- **Não há pool de verdade.** O `PGSimpleDataSource` (`Main.java:17`) abre uma conexão nova a cada chamada; o script
  contou um arquivo de "pool", mas esse `DataSource` não é um pool.
- **Situações sem CHECK.** As colunas de situação e de plano aceitam qualquer texto.

## Comportamento

**Números:** 64 de 64 conferências passaram, 0 falharam:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 09 (os relatórios): 13 de 13;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto descreve:
- **Preços.** O preço com a alta temporada e o meio para o par estão certos: 599.76, e o desconto de 37.48.
- **Recusas.** Todas vêm com o status e o código combinados.
- **E-mail e comprovante.** O e-mail sai com o comprovante, e a segunda via só existe para reserva paga.
- **Devoluções.** Seguem o plano.
- **Os dados.** Continuam depois de reiniciar.
- **Os quatro relatórios.** Começam com "Pousada Maré Alta,<nome>,<período>". A taxa de turismo e o de planos dão os
  valores pedidos, inclusive o plano sem reserva com 0 e o mês vazio com total 0.00.
- **Reservas simultâneas.** Nas dez rodadas, uma foi criada e a outra recusada com 409.

O único risco aqui é o da coluna do banco: com mais de uma instância do serviço, o `synchronized` deixaria de
proteger. Isso não aparece numa chamada que o texto descreve.

## Dúvidas

Na nota do design, pesei como menores a consulta ao banco nos construtores dos relatórios e a taxa de turismo fixa no
código (`TourismTaxReport.java:12`). Um revisor mais rígido poderia pedir que a taxa fosse configurável, já que o
texto diz que ela muda todo ano, e isso levaria a nota a `com problemas`.
