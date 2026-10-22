# Revisão: construção, cenário 09

## Visão geral

O agente montou um serviço Javalin em oito pacotes: `web` (um `ApiController` com as 13 rotas, a leitura do corpo e o
mapeamento das exceções para os códigos de erro), `service` (`ReservationService` com as situações da reserva,
`PricingService` com a temporada e o arredondamento, `NotificationService` com o comprovante e o e-mail,
`ReportService` com os quatro CSV), `repository` (JDBC puro com `?`), `db` (conexão por `DriverManager` e
`CREATE TABLE IF NOT EXISTS`), `model`, `dto` e `exception`. Tudo é montado no `App.main`. Uma requisição vai do
handler (`web/ApiController.java:115`) para o serviço (`service/ReservationService.java:30`), que valida, calcula o
preço e grava pelo repositório (`repository/ReservationRepository.java:21`). Os dados ficam em três tabelas no
PostgreSQL. O problema mais sério está em `web/ApiController.java:245`: `Map.entry("refund", r.getRefund())` lança
`NullPointerException` sempre que `refund` é `null`, ou seja, em toda reserva que não foi cancelada. Por isso criar,
consultar, confirmar e fazer o check-in de uma reserva respondem 500, **depois** de a mudança já ter sido gravada no
banco. O `mvn verify` do próprio projeto tem 4 testes falhando por esse motivo. Nada no projeto resolve o pedido do
fim do texto, o esqueleto comum dos relatórios: os quatro relatórios foram escritos inteiros, um ao lado do outro.

## Arquitetura

**Números:** 8 pacotes, 15 dependências entre eles, nenhum ciclo. O Javalin aparece só em `web` e no pacote raiz
(`App`), o JDBC só em `db` e `repository`. A maior classe é `ApiController`, com 317 linhas.
**Nota:** bom

As camadas existem e as dependências vão na direção certa: `web → service → repository → db`. Os serviços não conhecem
nem o Javalin nem o SQL (`service/ReservationService.java:1-11`, `service/ReportService.java:1-11`), e todo o SQL fica
nos três repositórios. As peças são criadas e ligadas num lugar só, `App.java:21-41`, com injeção pelo construtor e
sem `static` global. Os handlers só leem, chamam e respondem (`web/ApiController.java:144-172`); as regras de
situação, de preço e de cancelamento ficam no serviço (`service/ReservationService.java:98-139`).

Há pequenos desvios, nenhum grave:
- `ApiController` fala direto com `RoomRepository` e `MessageRepository` em duas leituras
  (`web/ApiController.java:102-113` e `:174-186`), pulando o serviço;
- o código de produção tem um ramo só para o H2 dos testes (`db/Database.java:27` e `:36-67`), com o esquema escrito
  duas vezes;
- `ApiController` junta rotas, leitura do corpo, mapeamento de DTO e tratamento de erro (`web/ApiController.java:44-316`).
  Ainda dá para ler, mas é a classe que mais vai crescer.

## Design de baixo nível

**Números:** 4 relatórios, nenhum dentro de uma estrutura comum. A linha de título está copiada 4 vezes, o cabeçalho
4 vezes, a linha de total 4 vezes, e o filtro de reservas pagas aparece 3 vezes, de dois jeitos diferentes.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede que o "jeito dos relatórios" mude "em todos de uma vez" e avisa que vêm mais relatórios
e uma linha de rodapé com a data. O esperado era o esqueleto (título, cabeçalho, linhas, total) num lugar só, ou seja,
um Template Method ou um escritor genérico. Em vez disso, `service/ReportService.java` tem quatro métodos que repetem
a sequência inteira cada um:
- o título está em `:36`, `:64`, `:95` e `:128`, cada um com o literal `"Pousada Maré Alta,..."`;
- o cabeçalho está em `:37`, `:65`, `:96` e `:129`;
- o laço de linhas está em `:39-50`, `:69-79`, `:100-111` e `:134-147`;
- o total está em `:52`, `:81`, `:113` e `:149`.

Não há classe abstrata nem interface de relatório, nem um escritor que receba as partes que variam. A linha com a
data, que o dono já anunciou, teria de ser acrescentada em quatro lugares. A referência chama isso de omissão.

**O exagero.** Não há estrutura sobrando: nenhuma interface com uma implementação só, nenhuma fábrica, nenhuma camada
que só repasse. A conclusão é contenção.

**O resto do código.**
- `null` sem cuidado, e com consequência grave: `Map.ofEntries`/`Map.entry` não aceitam `null`, e `refund` é `null`
  até o cancelamento (`web/ApiController.java:245`). Isso derruba a maior parte da API (ver Comportamento).
- Duplicação de regra. O desconto de 15% está no literal `new BigDecimal("0.15")` em
  `service/ReservationService.java:90`, enquanto a constante `NON_REFUNDABLE_DISCOUNT` de
  `service/PricingService.java:9` não é usada. O conjunto "reserva paga" está escrito à mão em
  `service/ReportService.java:26-30`, `:89-91` e `service/ReservationService.java:172-174`, e o filtro "dinheiro
  que ficou" (`!= PENDING`, em `service/ReportService.java:60` e `:121`) é diferente dele e está errado (ver
  Comportamento).
- O modelo é anêmico e mutável. `model/Reservation.java` só tem setters públicos, inclusive `setStatus` e
  `setAmountPaid` (`:137`, `:145`), e todas as transições de situação ficam espalhadas em `if`s no serviço
  (`service/ReservationService.java:102`, `:123-126`, `:145`, `:158`).
- Os valores em dinheiro nascem com escala 0: `BigDecimal.ZERO` em `model/Reservation.java:26` e em
  `service/ReservationService.java:88` e `:134`. Antes de passar pelo banco, eles iriam para o JSON como `0`, e não
  como `0.00`.
- O formato do dinheiro usa `NumberFormat.getCurrencyInstance(new Locale("pt","BR"))`
  (`service/NotificationService.java:15`). Esse formato põe um espaço não separável (U+00A0) depois de "R$", e por
  isso o e-mail não traz exatamente "R$ 779,80"; conferi a saída com `od` (bytes `302 240`). O construtor de
  `Locale` usado também está obsoleto.
- Os erros são tratados de modo estreito: `createReservation` só captura `IllegalArgumentException` e
  `DateTimeParseException` (`web/ApiController.java:133`), e o corpo que não é JSON escapa como 500.

Os tipos certos estão no lugar: `BigDecimal` com `HALF_EVEN` em cada passo (`service/PricingService.java:34-36`,
`service/ReservationService.java:85-94`), `LocalDate` para as datas e `enum` para situação, plano e tipo de quarto.

## Testes

**Números:** 7 testes rodaram, 4 falharam (0 erros, 0 pulados); 7 métodos de teste escritos. Cobertura de 55,0% das
linhas (441/802) e 39,9% dos ramos (59/148); em `service`, 123 de 255 linhas e 26 de 74 ramos.
**Nota:** ruim

A maioria dos testes falha, e a falha é do código, não do ambiente:
`IntegrationTest.testCreateAndConfirmReservation:117`, `testHighSeasonPricing:161`, `testNonRefundableDiscount:189`
e `testRoomConflict:217` recebem 500 no lugar de 201, por causa do `NullPointerException` em
`ApiController.toReservationDto` (`measures/tests-maven.log`, `ApiController.java:245`). O texto exigia que o
`mvn verify` passasse; sem `-Dmaven.test.failure.ignore`, como o script roda, ele não passa. O agente entregou os
testes sem tê-los visto passar.

Mesmo os testes que passam conferem pouco:
- `testReports` (`src/test/java/com/pousadamarealta/IntegrationTest.java:244-278`) roda sobre um banco sem nenhuma
  reserva e só confere com `contains` a linha de título e as palavras `STANDARD,` e `NON_REFUNDABLE,`. A taxa de
  R$ 4,50 por hóspede por noite, a ordem das linhas, a contagem por plano e o total não são conferidos. Trocar
  `4.50` por `5.40` (`service/ReportService.java:15`), ou trocar o filtro do relatório por plano, não derrubaria
  nenhum teste.
- `SimpleTest` (`src/test/java/com/pousadamarealta/SimpleTest.java:293-306`) só conta os quartos.
- Ficam sem nenhum teste: o cancelamento e a devolução nos dois planos, as transições proibidas (409), o check-in, o
  check-out e o comprovante, o e-mail, a ocupação com dados e o relatório financeiro com dados.

Todos os testes rodam sobre H2 em modo PostgreSQL (`IntegrationTest.java:36`), e não sobre o PostgreSQL que o sistema
usa, o que levou a um ramo próprio no código de produção (`db/Database.java:27`). Os valores esperados que existem
estão certos (599,76; 116,97 e 662,83, em `IntegrationTest.java:165-195`), mas esses testes não chegam a conferi-los,
porque falham antes.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`;
datas em `date`. As conferências acusaram: a chave estrangeira `reservations(room_number)` sem índice; nenhum CHECK,
UNIQUE nem EXCLUDE; 0 transações explícitas; 0 `FOR UPDATE`; nenhum pool (1 `DriverManager.getConnection`); esquema
criado no código, sem migrações.
**Nota:** com problemas

O que está certo: os tipos, a chave estrangeira para o quarto (`db/Database.java:87`), `NOT NULL` no que é
obrigatório, SQL sempre com `?`, e conexões fechadas por try-with-resources em todos os repositórios. O `%s` de
`repository/ReservationRepository.java:164` só recebe `?` gerados no código, então não há injeção.

Os defeitos:
- Nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos. O serviço consulta
  (`repository/ReservationRepository.java:120-153`) e depois grava (`service/ReservationService.java:55` e `:71`), sem
  transação, sem trava e sem restrição de exclusão no banco. Eu mesmo mandei 10 pares de pedidos simultâneos para o
  quarto 301 contra um banco vazio, e as 10 rodadas gravaram **duas** reservas para as mesmas noites
  (`count = 2` em todas).
- O esquema é criado com `CREATE TABLE IF NOT EXISTS` (`db/Database.java:29-99`), sem migrações versionadas, e
  escrito duas vezes, uma para o H2 e outra para o PostgreSQL. Não há como acrescentar uma coluna depois.
- Não há CHECK em `status`, `rate_plan`, `type` nem `guests`.
- Cada chamada abre uma conexão nova, sem pool (`db/Database.java:20`).

Os relatórios mensais filtram no banco por data (`repository/ReservationRepository.java:199`), e o filtro de
situação é feito em Java. Com o tamanho da pousada, isso não pesa.

## Comportamento

**Números:** 28 de 64 conferências passaram:
- a parte comum: 14 de 46;
- os dados depois de reiniciar: 2 de 3;
- o cenário 09: 11 de 13;
- duas reservas ao mesmo tempo: 1 de 2.

**Nota:** ruim

A parte comum está quebrada. Toda chamada que devolve uma reserva ainda não cancelada responde 500 (`POST
/reservations`, `GET /reservations/{id}`, a confirmação e o check-in), por causa de
`web/ApiController.java:245`. Isso vale para o roteiro e para a minha chamada manual com o exemplo do texto. O site
não recebe o número da reserva, e daí vêm em cascata as falhas da confirmação, do e-mail, da segunda via, do check-in,
do check-out, dos cancelamentos e da reserva "que continua lá" depois de reiniciar.

Pior, a gravação acontece antes do erro. Pelo `curl`:
- a reserva criada com 500 existe;
- a confirmação que respondeu 500 confirmou a reserva, registrou o pagamento e mandou o e-mail;
- a segunda via e o cancelamento funcionam, porque o comprovante e a reserva cancelada não têm `refund` nulo.

Para o dono da pousada, isso quer dizer reservas e pagamentos que o site acha que falharam, mas que estão gravados.
Também há um corpo que não é JSON respondendo 500 em vez de 400 (`web/ApiController.java:117`, `:133`).

O cenário falhou em 2 de 13, e as duas falhas têm a mesma causa. O relatório financeiro e o relatório por plano
contam toda reserva que não é `PENDING` (`service/ReportService.java:60` e `:121`), então entram também as
canceladas que não deixaram dinheiro. A reserva 5, uma STANDARD cancelada com devolução, aparece como
`5,202,...,CANCELLED,0.00`, e o relatório por plano diz `STANDARD,3` quando são 2 (`total,4` quando são 3). Os
totais em dinheiro saem certos. A linha de título dos quatro relatórios, a taxa de turismo (inclusive a do mês vazio),
a ocupação e as linhas zeradas por plano passaram.

A conferência "nenhuma rodada com as duas reservas criadas" passou só porque as duas respostas foram 500. No banco,
as duas foram gravadas em todas as rodadas (ver Banco). O e-mail usa um espaço não separável depois de "R$" (ver
Design de baixo nível).

## Dúvidas

- A nota do banco ficou em `com problemas` porque a skill põe a duplicação de reserva sem proteção no banco nessa
  faixa. Mas a duplicação se reproduziu em 10 de 10 rodadas, o que poderia justificar `ruim` pelo critério geral de
  risco de dado errado.
- Na arquitetura, as leituras que pulam o serviço (`web/ApiController.java:102`, `:174`) e o ramo para o H2 em
  `db/Database.java` são pequenos; um revisor mais rígido poderia dar `com problemas`.
