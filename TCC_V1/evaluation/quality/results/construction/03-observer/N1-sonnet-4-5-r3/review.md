# Revisão: construção, cenário 03

## Visão geral

O projeto (`projeto/`) é um Javalin 6.7.0 com HikariCP e JDBC puro, em 9 pacotes: `api` (controladores e DTOs),
`service` (as regras de uso e os relatórios), `service.listeners` (os cinco avisos), `domain` (reserva, preço,
formatação), `domain.events` (o publicador de eventos), `repository` (interfaces e implementações JDBC) e
`infrastructure` (o `CREATE TABLE IF NOT EXISTS` ao subir). Uma requisição entra pela rota em
`Application.java:63-76`, vai ao controlador (`ReservationController`), que chama o `ReservationService`. Este usa os
repositórios e, ao confirmar ou cancelar, publica um evento (`ReservationService.java:73,118`) que os ouvintes
registrados no `main` (`Application.java:38-43`) transformam em mensagens gravadas na tabela `messages`. O desenho do
Observer está certo. O problema mais sério é que o sistema não aguenta o primeiro uso real. A coluna `guest_cpf` é
`VARCHAR(11)` (`DatabaseInitializer.java:26`), e o CPF no formato que o texto mostra (`111.222.333-44`) faz toda criação
de reserva cair em 500. Por isso o roteiro do comportamento passou em só 17 de 66 conferências. Além disso:
- nada impede duas reservas do mesmo quarto feitas ao mesmo tempo; eu reproduzi isso com CPF só de dígitos;
- o dinheiro sai nos e-mails com espaço não separável (U+00A0) depois de "R$";
- um teste do próprio agente falha, então o `mvn verify` que o texto exige não passa.

## Arquitetura

**Números:** 9 pacotes, 20 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só em
`com.pousadamarealta` e `api`. O JDBC aparece só em `com.pousadamarealta` (o pool), `repository` e `infrastructure`.
A maior classe é `JdbcReservationRepository` (302 linhas); depois vêm `Reservation` (187) e `ReservationService` (171).
**Nota:** com problemas

As camadas existem e apontam para o lado certo. As regras de preço e de situação moram no domínio
(`Reservation.java:71-108`, `PriceCalculator.java:12-37`), sem Javalin nem SQL. O SQL fica todo nos repositórios. A
montagem é feita num lugar só, o `main` (`Application.java:28-51`). Mas há misturas localizadas:

- **Uma regra no controlador:** a segunda via só para reserva paga é decidida em `ReservationController.java:85-87`,
  e não no serviço.
- **Controladores que pulam o serviço e vão direto ao repositório:** `RoomController.java:20`,
  `MessageController.java:20` e `ReservationController.java:114`, que busca o quarto para montar o comprovante. Daí a
  dependência `api -> repository` no jdeps.
- **Exceções dentro do serviço:** as exceções de negócio são classes internas de `ReservationService`
  (`ReservationService.java:136-170`). Os controladores e o `ReportController` (`ReportController.java:21,28,38,45`)
  lançam `ReservationService.InvalidRequestException`, ou seja, a borda depende de um detalhe interno do serviço.
- **Ouvinte que vai ao banco:** o ouvinte do hóspede consulta o repositório de quartos
  (`GuestConfirmationListener.java:23`), o que gera a dependência `service.listeners -> repository`.

Nada disso é grave, mas são pontos que um revisor pediria para acertar.

## Design de baixo nível

**Números:** 5 interessados pedidos (hóspede, governança e financeiro na confirmação; governança e financeiro no
cancelamento). Os 5 são ouvintes separados, e nenhum aviso é mandado à mão fora da estrutura. Há 3 interfaces de
repositório, cada uma com uma implementação só.
**Classe:** acerto
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** É o Observer como a referência pede:

- uma interface de ouvinte (`EventListener.java:3-5`) e um publicador que guarda listas por tipo de evento
  (`EventPublisher.java:9-22`);
- uma classe por interessado em `service/listeners/`, inclusive o e-mail do hóspede (`GuestConfirmationListener.java:11`);
- todos registrados na montagem (`Application.java:38-43`);
- o serviço só anuncia `ReservationConfirmed` e `ReservationCancelled` (`ReservationService.java:73,118`) e não
  conhece quem é avisado.

Para pôr o restaurante ou o transfer, basta escrever uma classe e acrescentar uma linha no `main`. Dois pontos afastam
um pouco do ideal:

- O evento de cancelamento sai também para reserva pendente, e cada ouvinte repete o filtro `wasConfirmed`
  (`HousekeepingCancellationListener.java:20`, `FinanceCancellationListener.java:22`).
- O evento leva a entidade mutável `Reservation` (`ReservationConfirmed.java:5`).
- Os ouvintes rodam de forma síncrona depois do `save`, sem transação. Se um deles falhar, a reserva fica confirmada
  no banco e a resposta é 500 (`ReservationService.java:71-73`).

**O exagero.** Há três interfaces de repositório com uma implementação só (`RoomRepository.java`,
`ReservationRepository.java`, `MessageRepository.java`), e nada as substitui: o teste usa as próprias classes JDBC
(`ReservationFlowIT.java:35-37`). O `NotificationService` também quase só repassa ao repositório
(`NotificationService.java:13-16`).

**O resto do código:**

- **Dinheiro formatado errado e de forma insegura.** `DocumentFormatter.java:12` usa um
  `NumberFormat.getCurrencyInstance` estático e compartilhado entre as threads do Jetty, mas `NumberFormat` não é
  seguro entre threads. Ele também produz "R$ 779,80" (espaço não separável, que confirmei no corpo de
  `/messages`) em vez do "R$ 779,80" do texto.
- **Escala do dinheiro inconsistente.** `BigDecimal.ZERO` sem escala vai para o JSON e para o CSV como `0` em vez de
  `0.00`: `Reservation.java:45` (amountPaid), `Reservation.java:106` (refund) e `ReportService.java:64` (`total,0`).
- **Duplicação:**
  - o `IllegalStateException` traduzido para `OperationNotAllowedException` 4 vezes (`ReservationService.java:65-69,
    82-86, 95-99, 110-114`);
  - a lista de colunas do `SELECT` repetida 4 vezes (`JdbcReservationRepository.java:96,125,161,209`);
  - os endereços da governança e do financeiro repetidos em cada ouvinte (`FinanceConfirmationListener.java:10`,
    `FinanceCancellationListener.java:12`, `HousekeepingConfirmationListener.java:10`,
    `HousekeepingCancellationListener.java:10`).
- **Tipos fracos e lógica frágil:**
  - o plano de tarifa entra no serviço como `String` (`ReservationService.java:29,128-134`);
  - o `EventPublisher` usa tipo cru (`EventPublisher.java:18`);
  - o `rs.wasNull()` de `JdbcReservationRepository.java:299` testa a coluna errada (`amount_paid`) e só funciona por
    acaso;
  - a ordem das validações (`ReservationService.java:34-44`) faz a capacidade ser conferida antes da data inválida.

## Testes

**Números:** 11 testes rodaram e 1 falhou (`ReservationFlowIT.shouldCancelNonRefundableWithoutRefund:168`); 11
métodos escritos (7 de unidade do preço, 4 de integração contra o banco). A cobertura é de 50,7% das linhas (409 de
807) e 37,2% dos ramos (61 de 164). `api` e `Application` têm 0%, `service.listeners` 100% das linhas
(`measures/tests.md`).
**Nota:** com problemas

**O teste que falha.** A falha é do próprio teste, não do código. Os testes de integração compartilham o banco e não
limpam nada. O `noneMatch(... "Reembolso")` de `ReservationFlowIT.java:168-170` olha todas as mensagens e encontra o
reembolso criado pelo teste anterior (`ReservationFlowIT.java:132-135`). As datas são fixas e não há limpeza, então
uma segunda execução sobre o mesmo banco também falharia com `ROOM_UNAVAILABLE`. A consequência é que o
`mvn verify`, que o texto exige que passe, falha: a medida só termina porque o script usa
`-Dmaven.test.failure.ignore`.

**O que está bem conferido.** O preço tem testes com valores: temporada, arredondamento "meio para o par", desconto
de 89,96 e total de 509,80 (`PriceCalculatorTest.java:21-81`).

**O que fica sem rede.** Na parte nova, os testes conferem só o destinatário e um pedaço do assunto, e nunca o corpo
(`ReservationFlowIT.java:86-99, 127-135`). Por isso:
- apagar as datas ou o número de hóspedes do e-mail da governança não faria nenhum teste falhar;
- o mesmo vale para trocar o valor pago pelo total, ou o reembolso por zero, no e-mail do financeiro;
- nada confere o texto do e-mail do hóspede nem o "R$" com espaço comum, e é exatamente aí que está o espaço não
  separável.

**O que não tem teste nenhum:** a API, os códigos de erro, as recusas de transição, os relatórios e o CPF. Os testes
usam só CPF de dígitos (`ReservationFlowIT.java:65`) e não pegam a quebra com o CPF formatado.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. As conferências acusaram uma chave estrangeira sem índice
(`reservations.room_number`), nenhum CHECK nas colunas de situação, plano e tipo, nenhuma restrição UNIQUE ou EXCLUDE,
0 transações explícitas e 0 `FOR UPDATE` (`measures/database.md`).
**Nota:** ruim

**Reservas duplicadas.** Nada protege a regra "um quarto, uma reserva por noite". O serviço consulta
(`ReservationService.java:46-49`) e depois insere (`JdbcReservationRepository.java:43-55`), em conexões e comandos
separados, sem transação, trava ou restrição. Como o roteiro não conseguiu criar reservas por causa do CPF, testei à
mão com CPF de dígitos. Em 8 rodadas de dois pedidos simultâneos para o quarto 202, 7 criaram as duas reservas
(201/201) e o banco ficou com 2 reservas ativas para as mesmas noites. Isso é dado errado no coração do negócio.

**O CPF.** `guest_cpf VARCHAR(11)` (`DatabaseInitializer.java:26`) não comporta o CPF no formato que o texto usa, e o
erro do PostgreSQL ("value too long for type character varying(11)", em `measures/behavior-service.log:68`) vira 500.

**O resto:**

- **O esquema não evolui.** Ele é criado por `CREATE TABLE IF NOT EXISTS` no código
  (`DatabaseInitializer.java:13-50`), sem migrações, e não sabe acrescentar uma coluna.
- **Faltam restrições e índice.** Não há índice na chave estrangeira (`DatabaseInitializer.java:38`) nem CHECK em
  `status` e `rate_plan`.
- **Escritas que deveriam andar juntas não andam.** A confirmação e as mensagens não ficam na mesma transação
  (`ReservationService.java:71-73`), e o `UPDATE` não confere a situação anterior (`JdbcReservationRepository.java:63-69`).

**O que está certo.** O SQL usa parâmetros (o `StringBuilder` de `JdbcReservationRepository.java:160-179` só junta
`?` constantes), as conexões são devolvidas por try-with-resources e há um pool.

## Comportamento

**Números:** 17 de 66 conferências passaram (`measures/behavior.md`):
- a parte comum: 13 de 46;
- os dados depois de reiniciar: 1 de 3;
- o cenário 03: 2 de 15;
- as reservas simultâneas: 1 de 2 (todas as rodadas deram 500 500).
**Nota:** ruim

**A causa principal.** Quase todas as falhas vêm de um defeito só. `POST /reservations` com o CPF
`111.222.333-44`, o formato que o texto mostra, responde 500 `INTERNAL_ERROR` ("Failed to insert reservation"). A
partir daí não há reserva para confirmar, fazer check-in, cancelar ou pôr nos relatórios, nem e-mail para conferir.
Para o dono da pousada, isso quer dizer que o site não consegue criar reservas quando o CPF vem com pontuação.

**O que mais está errado.** Subi o serviço à mão com CPF só de dígitos. O fluxo funciona: preço, confirmação, os três
e-mails da confirmação, os dois do cancelamento, nenhum aviso para a pendente, check-out e relatórios corretos. Mesmo
assim continuam errados:

- **O dinheiro nos e-mails.** O comprovante e os avisos trazem "R$" seguido de espaço não separável. Quem procurar
  "Total: R$ 599,76" num texto não acha.
- **O formato dos números no JSON.** `amountPaid` sai `0`, e não `0.00`; `refund` de não reembolsável sai `0`.
- **Corpo que não é JSON** dá 500 em vez de 400 `INVALID_REQUEST`. A causa é `ctx.bodyAsClass` em
  `ReservationController.java:26` sem tratamento, que cai no `Exception` genérico de `Application.java:108-111`. Esse
  tratamento genérico ainda devolve ao cliente a mensagem interna do Jackson.
- **Reservas simultâneas.** Duas ao mesmo tempo para o mesmo quarto são criadas as duas (o defeito pesa na coluna do
  banco).

**Um ponto em aberto que não pesa.** O `number` do quarto sai como texto (`"201"`), e por isso falham "o 201 comporta 3"
e "a diária do 201 é 389.90". O texto não fixava esse tipo.

## Dúvidas

- **O CPF.** O texto não diz em que formato o site manda o CPF. O roteiro manda `111.222.333-44`, o mesmo do
  comprovante, e o agente supôs 11 dígitos. Mas a escolha do agente não valida nada e responde 500 em vez de recusar
  ou normalizar. Por isso a falha conta como erro dele, qualquer que seja a leitura.
- **O exagero.** As três interfaces de repositório com uma implementação só são um exagero leve. Marquei `exagero`
  porque a skill define assim, mas é um caso de fronteira.
