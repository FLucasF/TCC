# Revisão: construção, cenário 03

## Visão geral

O projeto tem 7 pacotes. `api` traz quatro controladores com rotas estáticas do Javalin, que montam o JSON com o
`JsonObject` do Gson. `services` reúne os serviços e a classe `Database`, que concentra todo o SQL por JDBC com
HikariCP. `domain` tem as classes e os enums do domínio. `events` e `listeners` fazem o Observer pedido, e `utils`
cuida do preço e da formatação. Uma requisição entra pelo controlador, que lê o corpo e chama o `ReservationService`
(ou a `Database` direto, em algumas rotas). O serviço aplica a regra, grava pela `Database` e avisa o
`ReservationEventDispatcher`, que chama os três ouvintes. As mensagens ficam na tabela `messages`. O problema mais
sério é que **o sistema não funciona pela API**. O `pom.xml` não traz o Jackson (nem outro mapeador JSON do Javalin),
e todo `ctx.json(...)` e `ctx.bodyAsClass(...)` dá 500 com "It looks like you don't have an object mapper configured"
(`measures/behavior-service.log`). Só 7 das 66 conferências do roteiro passaram, e só as que não dependem de JSON ou
que passam sem nada acontecer. Além disso, a checagem de disponibilidade ignora as reservas pendentes, e a
confirmação não confere de novo se o quarto está livre. Um programa que chamei direto sobre os serviços confirmou
duas reservas para o 201 nas mesmas noites. Os testes são 5 e cobrem só o cálculo de preço (3,2% das linhas), por
isso o `mvn verify` passa com o sistema quebrado. Já o Observer pedido pelo cenário está bem desenhado.

## Arquitetura

**Números:** 7 pacotes, 15 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O JDBC aparece só em
`services` e o Javalin em `api` e no `main`. A maior classe é `services/Database.java` (416 linhas), seguida de
`api/ReservationsController.java` (272).
**Nota:** com problemas

O que está bom:

- As dependências apontam da borda para as regras e das regras para os dados.
- A montagem fica toda num lugar só, `Main.java:22-40`: o banco, o dispatcher, os serviços e o registro dos ouvintes.
- As regras não conhecem o Javalin, e o SQL está isolado numa classe só, com consultas parametrizadas.

As misturas, localizadas:

- **Controladores que pulam o serviço:** `RoomsController.java:10` e `ReservationsController.java:15`, `:63` e `:159`
  recebem a `Database` e a consultam direto.
- **Regra de negócio dentro do handler:** quem pode tirar a segunda via (só reserva paga) é decidido em
  `ReservationsController.java:169` (`getAmountPaid().signum() == 0`), não num serviço.
- **Regra repetida no SQL:** a lista de situações que ocupam o quarto aparece literal em `Database.java:309`, `:337`
  e `:367`.
- **Classe que faz o acesso a dados de tudo:** a `Database` cria o esquema, insere os quartos e responde pelos
  quartos, pelas reservas, pelas mensagens e pelos relatórios. É o lugar onde o próximo pedido vai doer.
- **Pacote errado:** a `Database` fica no pacote `services`, misturada aos serviços.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança, financeiro), todos ouvintes de uma interface com dois eventos
(confirmada e cancelada). Nenhum e-mail é montado fora dos ouvintes, e os três ouvintes são registrados no `Main`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern pedido (acerto).**

- `ReservationEventListener` (`events/ReservationEventListener.java:5-8`) é a interface de ouvinte, com
  `onReservationConfirmed` e `onReservationCancelled`.
- Há uma implementação por interessado, e o e-mail do hóspede também é um ouvinte:
  - `GuestNotificationListener.java:19-39`, o hóspede;
  - `GovernanceNotificationListener.java:18-40`, a governança;
  - `FinanceNotificationListener.java:19-34`, o financeiro.
- O `ReservationService` recebe o dispatcher de fora (`ReservationService.java:15`). Ele só anuncia a confirmação
  (`:99`) e o cancelamento de uma reserva que estava confirmada (`:133-135`), sem conhecer quem é avisado.
- O dispatcher só percorre a lista (`ReservationEventDispatcher.java:14-24`), e os ouvintes são registrados na montagem
  (`Main.java:30-32`).
- Para avisar o restaurante ou o transfer, basta uma classe nova e uma linha no `Main`.

Dois detalhes pequenos, que não mudam a classe:

- A interface tem dois métodos obrigatórios, e por isso o ouvinte do hóspede carrega um `onReservationCancelled`
  vazio (`GuestNotificationListener.java:42-43`). Quem entrar só para um dos eventos vai ter o mesmo método vazio.
- O e-mail do financeiro mostra o `getTotal()` em vez do `getAmountPaid()` (`FinanceNotificationListener.java:21`).
  Hoje os dois valores são iguais.

**O exagero (contenção).** Não há interface com uma implementação só, nem fábrica, nem camada que só repassa. O
dispatcher é uma classe concreta, simples.

**O resto do código (com problemas):**

- **A biblioteca de JSON errada.** O código usa o `JsonObject` do Gson (`ReservationsController.java:3`) e o entrega
  ao `ctx.json`, mas o `ctx.json` do Javalin usa o mapeador JSON do Javalin, e o `pom.xml` não traz nenhum. É a causa
  dos 500. E acrescentar o Jackson não basta: o Jackson não sabe serializar o `JsonObject` do Gson.
- **O dinheiro vai como `double` no JSON.** Exemplos: `ReservationsController.java:213-217` e `RoomsController.java:15`.
  O `0.00` sairia `0.0`, e o texto pede 2 casas.
- **Os erros são texto.** O serviço lança `IllegalArgumentException("ROOM_NOT_FOUND")` e semelhantes
  (`ReservationService.java:24`, `:32`, `:40`, `:92`), e o controlador traduz a mensagem num `switch` de strings
  (`ReservationsController.java:233-270`). Qualquer outra `IllegalArgumentException`, como a de `RatePlan.fromValue`
  (`RatePlan.java:23`), cai no `default` com a mensagem interna. Há também um `catch (Exception e)` que transforma
  qualquer falha em 400 (`ReservationsController.java:52-57`).
- **Duplicação:**
  - o JSON de erro é montado à mão cerca de 15 vezes;
  - o JSON do comprovante está copiado inteiro em `ReservationsController.java:131-145` e `:177-191`;
  - `formatCpf` existe duas vezes (`ReservationsController.java:226` e `DocumentService.java:54`);
  - o serviço monta a `Reservation` duas vezes seguidas (`ReservationService.java:48-79`).
- **Regras erradas ou frágeis:**
  - a alta temporada termina em 28 de fevereiro (`PricingCalculator.java:18`), e o 29/02/2028 sai como baixa
    temporada (confirmado chamando a classe);
  - o tipo do quarto no relatório de ocupação é deduzido do número (`ReportService.java:33`), quando o quarto já
    está na lista ao lado;
  - `Formatting.formatMoney` depende do idioma da máquina (`Formatting.java:21`): com o padrão pt_BR, R$ 1.234,56 sai
    "R$ 1,234.56" (confirmado).
- **Campo enganoso:** o `messageIdCounter` de `MessageService.java:10` gera um id que o banco descarta.

Os tipos de domínio estão certos (`BigDecimal` com HALF_EVEN, `LocalDate`, enums), e o serviço é curto e legível.

## Testes

**Números:** 5 testes rodaram, 0 falhas; 5 métodos escritos, num arquivo só. A cobertura é de 3,2% das linhas
(29 de 895) e 7,4% dos ramos (14 de 190). `api`, `services`, `events` e `listeners` têm 0%
(`measures/tests.md`).
**Nota:** ruim

Os cinco testes (`ReservationSystemTest.java:13-57`) conferem só o `PricingCalculator`: a alta temporada, a diária com
20%, o subtotal de R$ 599,76 (`:38`), o desconto de 15% e o número de noites. Eles têm valores, mas não cobrem nada
além disso:

- as situações da reserva, o cancelamento e a devolução;
- os códigos de erro e os relatórios;
- a persistência;
- e, sobretudo, a parte nova do cenário: os avisos à governança e ao financeiro.

A pergunta da mutação: apagar a linha `Main.java:31` (o registro da governança), trocar `signum() > 0` por `>= 0` em
`FinanceNotificationListener.java:28` ou remover o `if (wasConfirmed)` de `ReservationService.java:133` não faria
nenhum teste falhar. O 29 de fevereiro, que está errado, também não é testado.

O pior é que não há nenhum teste pela API. Por isso o `mvn verify` passa com um sistema em que toda rota JSON responde
500.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária, e o dinheiro em
`numeric(10,2)` (`measures/database.md`). As conferências acusaram:

- a chave estrangeira `reservations(room_number)` sem índice;
- as colunas de situação, plano, tipo e canal sem CHECK;
- nenhuma restrição que proteja a regra do quarto (UNIQUE, EXCLUDE ou CHECK);
- o esquema criado com `CREATE TABLE IF NOT EXISTS` no código Java, sem migrações;
- 1 transação explícita (só na criação do esquema) e 0 `FOR UPDATE`.

**Nota:** ruim

O que está certo:

- os tipos: dinheiro em `numeric`, datas em `date` (`Database.java:52-62`), a chave estrangeira para o quarto
  (`:63`);
- o SQL sempre com `?`;
- o pool HikariCP, com as conexões em try-with-resources.

O defeito sério é que nada impede um quarto de ter duas reservas na mesma noite:

- **As pendentes não contam.** A checagem de disponibilidade só considera CONFIRMED, CHECKED_IN e CHECKED_OUT
  (`Database.java:309`), mas o texto manda contar todas as que não foram canceladas.
- **A confirmação não confere de novo.** `ReservationService.confirmReservation` (`ReservationService.java:84-101`)
  não olha a disponibilidade outra vez.
- **O resultado, confirmado:** um programa que chamei direto sobre os serviços criou duas reservas para o 201 de
  10 a 12/03/2027 e confirmou as duas (`r1=CONFIRMED r2=CONFIRMED`). Isso grava no banco um quarto vendido duas
  vezes, sem precisar de concorrência nenhuma.
- **Nem com concorrência haveria proteção.** A checagem e o `INSERT` (`ReservationService.java:39` e `:64`) correm
  sem transação, sem trava e sem restrição de exclusão.

Além disso:

- **Sem atomicidade.** A mudança de situação (`Database.java:240-259`) e as mensagens dos ouvintes são gravadas em
  conexões separadas, sem transação. Uma falha num ouvinte deixa a reserva confirmada sem os avisos.
- **O esquema não evolui.** Com `CREATE TABLE IF NOT EXISTS` (`Database.java:36-75`), o próximo pedido não consegue
  acrescentar uma coluna numa tabela que já existe.

## Comportamento

**Números:** 7 de 66 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum do enunciado | 4 de 46 |
| os dados depois de reiniciar | 1 de 3 |
| o cenário 03 | 1 de 15 |
| as reservas simultâneas | 1 de 2 |

**Nota:** ruim

O serviço sobe e cria o banco, mas toda rota que lê ou devolve JSON responde 500: GET /rooms, POST /reservations, as
recusas, a confirmação, o cancelamento, o check-in e o check-out. A causa é a falta do mapeador JSON (o
`behavior-service.log` repete "It looks like you don't have an object mapper configured").

Só passaram:

- os relatórios em CSV, que não usam JSON, mas vêm vazios porque nenhuma reserva pôde ser criada (`total,0/5`);
- conferências que passam sem que nada aconteça: "refund null", "as mensagens continuam lá", "cancelar pendente não
  manda nada" e "nenhuma rodada com as duas reservas criadas" (todas deram 500).

Para o dono da pousada, o site não consegue listar quartos nem criar, confirmar ou cancelar reservas, e nenhum aviso
à governança ou ao financeiro sai, porque nenhuma confirmação acontece. O sistema não é usável.

Lendo o código, há outros erros que apareceriam mesmo com o JSON consertado:

- o quarto vendido duas vezes, pesado na coluna do banco;
- o 29/02 cobrado como baixa temporada (`PricingCalculator.java:18`);
- o dinheiro no JSON sem as 2 casas.

## Dúvidas

- Não dá para medir pela API se os avisos do cenário saem certos, porque nenhuma confirmação funciona. Pela leitura,
  os assuntos e os destinatários batem com o texto (`GovernanceNotificationListener.java:19` e `:32`,
  `FinanceNotificationListener.java:20` e `:29`), e o financeiro só é avisado no cancelamento quando há devolução
  (`:28`). Mas isso não foi conferido rodando.
- A nota de design ficou entre `com problemas` e `ruim`. O uso de uma biblioteca de JSON que o Javalin não usa é o que
  derruba o sistema, mas o defeito está concentrado na borda, e o domínio e o serviço estão razoáveis. Escolhi
  `com problemas`.
