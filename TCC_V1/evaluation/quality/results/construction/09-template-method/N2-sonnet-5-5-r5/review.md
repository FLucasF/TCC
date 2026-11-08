# Revisão: construção, cenário 09

## Visão geral

O projeto tem cinco pacotes: `web` (Javalin), `domain` (regras), `report` (os quatro CSV), `persistence` (JDBC com
HikariCP) e a raiz, onde fica `Main`. Uma requisição entra numa rota de `web/Api.java`. A rota lê o corpo com
`RequestParser`, chama o `ReservationService`, que usa `Reservation`, `ReservationStatus` e `RatePlan` para preço e
situação, e grava pelo `JdbcReservationRepository`. Os dados ficam em duas tabelas do PostgreSQL (`reservations` e
`messages`), criadas por `CREATE TABLE IF NOT EXISTS` no código. Os quartos ficam fixos no código (`domain/Hotel.java:8`).

Os relatórios seguem um Template Method limpo: `Report.render` é `final` e as quatro subclasses preenchem os passos.
O roteiro de comportamento passou inteiro, 64 de 64. Não há nada grave. Os defeitos são estes:
- um ciclo entre `domain` e `persistence`, porque o serviço depende das classes JDBC concretas;
- o banco sem migrações e sem restrições: a reserva duplicada só é barrada por um `synchronized`, que vale para um
  processo só;
- poucos testes (5), e eles rodam contra o H2 em vez do PostgreSQL.

## Arquitetura

**Números:** 5 pacotes, 11 dependências entre eles, 1 ciclo (`br.com.mare.domain` ↔ `br.com.mare.persistence`). O
Javalin aparece só em `web` e na raiz, o JDBC só em `persistence` e na raiz. `domain` e `report` não usam nenhuma
biblioteca de borda. A maior classe é `JdbcReservationRepository`, com 125 linhas.
**Nota:** com problemas

O que está bem:
- **Borda fina.** Cada rota só lê a requisição, chama o serviço e escreve a resposta (`web/Api.java:57-68`). Os
  relatórios são registrados num laço genérico (`web/Api.java:72-75`).
- **Regras sem Javalin e sem SQL.** As transições ficam em `domain/ReservationStatus.java:6-78`, o desconto e a
  devolução em `domain/RatePlan.java:6-33`, a temporada em `domain/Season.java:14-24`.
- **SQL isolado** em `persistence/` (`JdbcReservationRepository.java`, `JdbcMessageRepository.java`).
- **Montagem num lugar só,** `Main.build` (`Main.java:37-47`).

Os defeitos:
- **O ciclo `domain` ↔ `persistence`.** `ReservationService` importa e guarda as classes concretas
  `JdbcReservationRepository` e `JdbcMessageRepository` (`domain/ReservationService.java:3-4,10-13`). Ao mesmo tempo,
  `persistence` depende de `domain` para as entidades (`persistence/JdbcReservationRepository.java:3-5`). Uma
  interface de repositório no domínio, ou o serviço num pacote próprio, desfaria o ciclo.
- **A borda vai direto à persistência.** `Api` recebe o `JdbcMessageRepository` e o chama na rota `/messages`
  (`web/Api.java:8,44,70`). `ReservationQueries` também está ligado ao repositório JDBC concreto
  (`report/ReservationQueries.java:4,15`).
- **Um pedaço de HTTP no serviço.** O serviço recebe o id como `String` e o converte, devolvendo
  RESERVATION_NOT_FOUND quando não é número (`domain/ReservationService.java:38-46`). Isso é trabalho da borda.

São misturas localizadas, num ciclo pequeno entre dois pacotes vizinhos.

## Design de baixo nível

**Números:** 4 de 4 relatórios são subclasses do esqueleto `Report` (ocupação, financeiro, taxa de turismo, planos).
0 relatórios escrevem o título, o cabeçalho ou o total por conta própria. A linha de título está num lugar só
(`report/Report.java:37`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** `Report<P>` é uma classe abstrata com o método-modelo `render` declarado `final`
(`report/Report.java:33-43`). Ele faz, nesta ordem: lê o período, monta o corpo, escreve o título
"Pousada Maré Alta,nome,período", o cabeçalho, as linhas e o total. Os passos que mudam são abstratos: `name`,
`parameter`, `parse`, `periodLabel`, `header` e `body` (`report/Report.java:19-31`). Os quatro relatórios só preenchem
esses passos:
- `OccupancyReport` (`report/OccupancyReport.java:12`);
- `RevenueReport` (`report/RevenueReport.java:10`);
- `TourismTaxReport` (`report/TourismTaxReport.java:11`);
- `RatePlanReport` (`report/RatePlanReport.java:13`).

A linha de data que o dono quer para o ano que vem entraria só em `render`. Um relatório novo é uma subclasse a mais
e uma linha em `Main.build` (`Main.java:41-45`), e a rota sai sozinha (`web/Api.java:72-75`). A taxa da prefeitura
não está fixa no código: chega pelo construtor e pode vir da variável `TOURISM_TAX` (`Main.java:23,32`;
`report/TourismTaxReport.java:15-18`). O que poderia afastar do acerto, e não afasta: o passo `body` devolve as linhas
já formatadas (`report/Report.java:15`), mas a sequência dos passos não se repete em nenhuma subclasse.

**O exagero.** A estrutura a mais se justifica:
- `MonthlyReport` (`report/MonthlyReport.java:10-33`) é um nível intermediário, mas guarda o `parse` do mês e o
  rótulo, que três relatórios usam.
- `ReservationQueries` (`report/ReservationQueries.java:12-46`) concentra as seleções que os relatórios compartilham
  (pagas no mês, com dinheiro retido, ocupando a noite). Assim, o financeiro e o de planos contam as mesmas reservas
  pela mesma função (`retainedStartingIn`).

Não há interfaces de uma implementação só, fábricas ou builders.

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` em cada passo (`domain/Money.java:16-18`,
`domain/Reservation.java:52-57`, `domain/Season.java:23`), `LocalDate`, e `enum` com comportamento para a situação e
o plano. Isso evita `switch` espalhado (`domain/ReservationStatus.java:53-77`, `domain/RatePlan.java:30-32`). O
estado só muda por métodos de domínio (`domain/Reservation.java:63-79`). Há só detalhes menores:
- `catch (... | NullPointerException e)` para parâmetro ausente (`report/OccupancyReport.java:33`);
- uma exceção lançada só para ser pega logo abaixo (`report/MonthlyReport.java:20-21`);
- a soma do retido repetida (`report/RevenueReport.java:34` e `report/RatePlanReport.java:42`);
- um construtor de 16 parâmetros com `assignId` mutável (`domain/Reservation.java:26,100`);
- as falhas do banco embrulhadas em `IllegalStateException` sem tratamento na borda: viram 500 sem o corpo
  `{error, message}` (`persistence/JdbcReservationRepository.java:55-56`, `web/Api.java:51`).

Nada disso atrapalha mudar o código com segurança.

## Testes

**Números:** 5 testes rodaram, 0 falhas, 0 erros, 0 pulados. São 5 métodos de teste, todos em
`ApiTest`. A cobertura é de 89,6% das linhas (446 de 498) e 63,3% dos ramos (57 de 90). Por pacote, `report` tem
88 de 90 linhas e `domain` 186 de 194. Os testes passam pela API, contra o H2 em memória.
**Nota:** com problemas

O que os testes conferem bem, com valores:
- o preço da alta temporada, 599.76 (`ApiTest.java:57-63`);
- o e-mail e o comprovante, com o texto exato (`ApiTest.java:73-76`);
- as devoluções, 0.00 e 249.90 (`ApiTest.java:106-107`).

Os quatro relatórios são comparados por inteiro, com título, cabeçalho, linhas e total (`ApiTest.java:121-145`). Isso
inclui o desconto arredondado meio para o par (134.946 → 764.69) e a taxa de 27.00. A pergunta da mutação na parte
nova:
- **Tirar o filtro de reserva paga da taxa:** o teste quebra, porque a reserva 3, pendente, entraria.
- **Trocar `> 0` por `>= 0` no retido:** o teste quebra.
- **Inverter a ordem dos planos:** o teste quebra.

O que fica sem rede:
- **Ordem com várias linhas.** Os relatórios financeiro e de taxa têm uma linha só no teste
  (`ApiTest.java:130,136`). Trocar a ordem por data e número passaria.
- **Não reembolsável cancelada.** Não há teste de que ela continua contando no financeiro e no de planos.
- **O valor de 4,50 em produção.** O teste injeta 4.50 direto (`ApiTest.java:29`), então trocar
  `DEFAULT_TOURISM_TAX` (`Main.java:23`) passaria.
- **O comprovante.** A linha "Desconto:" e o "1 diária" não são conferidos.
- **As bordas da temporada,** como 14/12 contra 15/12 e 01/03.
- **Os códigos de erro.** Quase todas as recusas conferem só o status HTTP e não o código
  (`ApiTest.java:85-95`); só ROOM_UNAVAILABLE é conferido (`ApiTest.java:92`).

Os testes também rodam contra o H2 em modo PostgreSQL (`ApiTest.java:28`), não contra o banco de `DB_URL`, então o
esquema real nunca passa pelos testes. São poucos testes, cada um grande e com vários cenários num só
(`ApiTest.java:65-98`).

## Banco

**Números:** 2 tabelas (`reservations`, `messages`), as duas com chave primária. O dinheiro está em
`numeric(12,2)` (5 colunas ok) e as datas em `date`. A conferência acusou:
- 0 chaves estrangeiras;
- 0 CHECK e 0 UNIQUE/EXCLUDE;
- `status`, `rate_plan` e `channel` em `varchar` sem CHECK;
- 0 transações explícitas e 0 `FOR UPDATE`;
- esquema por `CREATE TABLE` no código, sem migrações;
- nenhum SQL montado com `+`.
**Nota:** com problemas

O que guarda certo: os tipos, o `NOT NULL` em tudo que é obrigatório (`persistence/Database.java:39-64`), o SQL todo
com `?` (`persistence/JdbcReservationRepository.java:28-31,61,84`), as conexões devolvidas por try-with-resources e o
pool HikariCP (`persistence/Database.java:20-29`). Os defeitos:
- **O esquema não evolui.** É criado por `CREATE TABLE IF NOT EXISTS` dentro do Java
  (`persistence/Database.java:38-64`), sem migrações versionadas. A primeira coluna nova, como o estado do hóspede
  para o relatório que o contador já anunciou, não chega a um banco que já existe.
- **A regra "um quarto, uma reserva por noite" não está no banco.** Não há restrição EXCLUDE nem trava. Só o
  `synchronized` do serviço impede a duplicação (`domain/ReservationService.java:19,26-34`), e ele vale para um
  processo só. Duas instâncias do serviço gravariam duas reservas na mesma noite.
- **Sem chave estrangeira para o quarto.** Os quartos não estão no banco (`domain/Hotel.java:8-13`), então
  `room_number` não tem chave estrangeira nem índice. Também não há CHECK em `status`, `rate_plan` e `guests`.
- **A confirmação faz duas escritas sem transação.** Primeiro o `UPDATE` da reserva, depois o `INSERT` da mensagem
  (`domain/ReservationService.java:48-50`). Se a segunda falhar, a reserva fica confirmada sem o e-mail registrado.
- **Consultas pesadas nos relatórios.** Todos trazem a tabela inteira para filtrar em Java
  (`report/ReservationQueries.java:24,42` → `persistence/JdbcReservationRepository.java:78-80`), e essa tabela cresce
  sem limite.

## Comportamento

**Números:** 64 de 64 conferências passaram, 0 falharam:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 09: 13 de 13;
- as duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo o que o texto pede funciona pela API a partir de um banco vazio:
- os preços com temporada e arredondamento meio para o par (599.76, desconto 37.48);
- as recusas com os códigos certos;
- a confirmação com o e-mail e o comprovante;
- o check-in e o check-out;
- as devoluções por plano;
- os quatro relatórios com a linha de título, a taxa de turismo e o relatório por plano, inclusive o plano sem
  reserva com 0 e a não reembolsável cancelada contando.

Os dados continuam depois de reiniciar. Nas dez rodadas de reservas simultâneas, nenhum quarto ficou com duas
reservas. Isso vale com um processo só; o limite está descrito na coluna do banco e não muda a nota aqui.

## Dúvidas

Na arquitetura, fiquei entre `bom` e `com problemas`. O ciclo `domain` ↔ `persistence` é pequeno e vem só do serviço
depender das classes JDBC concretas. Dei `com problemas` porque a skill conta um ciclo novo entre dois pacotes nessa
faixa. No design, a nota `bom` deixa de lado os detalhes menores listados (o `catch` de `NullPointerException`, a soma
repetida). Um revisor mais rigoroso poderia pedir que fossem corrigidos antes de aceitar.
