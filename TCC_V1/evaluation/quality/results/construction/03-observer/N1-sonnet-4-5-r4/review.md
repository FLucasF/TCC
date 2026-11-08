# Revisão: construção, cenário 03

## Visão geral

O projeto está em camadas, em pacotes separados por responsabilidade: `api.controller` (os handlers do Javalin), `api.dto`, `domain.model` (a `Reservation` com o preço e as transições), `domain.service`, `domain.event` (o `EventPublisher` e os eventos), `domain.event.listeners` (um ouvinte por interessado) e `repository` (JDBC puro, com `CREATE TABLE IF NOT EXISTS` no código). Tudo é montado em `Main.java`. Uma requisição segue este caminho: rota em `Main.java:77-87` → controller → `ReservationService` → `ReservationRepository` → PostgreSQL. Uma conexão JDBC só (`Main.java:39`) é compartilhada por todos os repositórios e por todas as threads do Jetty. O pedido do cenário, avisar mais gente quando a reserva é confirmada ou cancelada, virou um Observer bem feito. Os problemas mais sérios são estes:
- nada impede duas reservas do mesmo quarto feitas ao mesmo tempo: o roteiro criou as duas em 7 de 10 rodadas, e não há restrição no banco, nem transação, nem trava;
- os testes são quase ausentes (9,6% das linhas, nada da parte nova);
- o dinheiro nos e-mails sai com um espaço não separável (`R$ 779,80`), diferente do formato pedido;
- o preço é recalculado em três lugares, dois deles na borda HTTP.

## Arquitetura

**Números:** 10 pacotes, 19 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). A maior classe é `ReservationRepository` (230 linhas). Pelo jdeps, o JDBC aparece em `api.controller` e em `domain.service`.
**Nota:** com problemas

O que está bom:
- As dependências vão da borda para as regras e das regras para os dados, sem ciclo.
- Todas as peças são montadas em `Main.java:38-67`: repositórios, serviços, publicador, ouvintes e controllers, nenhum `new` escondido dentro de uma classe.
- O SQL fica só em `repository/`.
- Os serviços não conhecem o `Context` do Javalin.
- Os controllers quase só leem a requisição e escrevem a resposta (`RoomController.java:18-23`, `MessageController.java:18-23`).

O que mistura camadas, de forma localizada:
- **A regra de preço também mora na borda HTTP.** `ReservationController.java:89-93` calcula subtotal, desconto e total para montar o comprovante, e o próprio DTO calcula o preço no construtor (`ReservationResponse.java:69-71`). O cálculo existe ainda em `Reservation.calculateTotal` (`Reservation.java:77-81`) e em `ReservationService.java:134-136`. Uma mudança de regra de preço precisa ser lembrada em vários lugares, dois deles na camada HTTP.
- **O JDBC vaza para as regras e para a borda.** Os serviços e os controllers declaram `throws SQLException` (`ReservationService.java:10,34`, `ReservationController.java:14,25`), e é por isso que o jdeps vê JDBC nesses pacotes.
- **O serviço formata texto de apresentação.** `ReservationService` monta o comprovante, com formatação de moeda, de CPF e de data (`ReservationService.java:132-169`).
- **Os controllers montam o que o serviço poderia devolver.** Depois de cada operação, eles buscam o quarto de novo para montar a resposta (`ReservationController.java:43,50,57,...`).
- **Uma conexão só para tudo.** `Main.java:39-43` entrega a mesma `Connection` aos três repositórios. Se a conexão cair, o `Database.getConnection` que reconecta (`config/Database.java:19-24`) nunca é chamado de novo, e o serviço fica quebrado até ser reiniciado.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança, financeiro) e 2 eventos (confirmação, cancelamento). Os 5 avisos estão na estrutura de ouvintes e nenhum fica fora dela. São 3 classes de ouvinte, registradas em `Main.java:54-58`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O Observer está aplicado direito, nos dois eventos.
- `EventPublisher` (`domain/event/EventPublisher.java:6-37`) guarda uma lista de ouvintes de confirmação e outra de cancelamento, percorre cada uma ao publicar (`:18-28`) e define as duas interfaces de ouvinte (`:30-36`).
- `ReservationService` só anuncia: `publishConfirmed` em `ReservationService.java:84` e `publishCancelled` em `:101`. Ele não sabe quem é avisado e não monta nenhum e-mail da governança ou do financeiro.
- Cada interessado é uma classe: `GuestNotificationListener`, `HousekeepingNotificationListener` e `FinanceNotificationListener`. O e-mail do hóspede também é um ouvinte (`GuestNotificationListener.java:17-34`).
- Os ouvintes são registrados na montagem (`Main.java:53-58`). Para pôr o restaurante ou o transfer, basta uma classe nova e uma linha em `Main`.
- As regras ficam nos ouvintes: "só avisa se estava confirmada" (`HousekeepingNotificationListener.java:45`, `FinanceNotificationListener.java:43`) e "só avisa o financeiro se há o que devolver" (`FinanceNotificationListener.java:49`).

O que afasta um pouco do ideal, sem mudar a classe:
- O evento de confirmação já carrega o texto do comprovante montado pelo serviço (`ReservationService.java:83-84`, `ReservationConfirmedEvent.java:7`). Assim, o serviço faz um trabalho que só o ouvinte do hóspede usa.
- O publicador não isola um ouvinte do outro (`EventPublisher.java:19-21`). Se um deles lança exceção (o `RuntimeException` de `MessageService.java:19-21`), os seguintes não rodam. A reserva já foi gravada como CONFIRMED (`ReservationService.java:81`) e o cliente recebe 500.

**O exagero.** Não há. Não existem interfaces com uma implementação só fora do pattern, nem fábricas, nem camadas que só repassam. `RoomService` e `MessageService` são finos, mas pequenos.

**O resto do código**, com defeitos localizados:
- **Código morto que quebra.** `Reservation.confirm()` chama `calculateTotal(null)` (`Reservation.java:87-93`), o que daria `NullPointerException` em `calculateSubtotal` (`:44-46`). Ninguém chama esse método hoje, mas ele é público e está errado.
- **Duplicação.**
  - O cálculo subtotal → desconto → total aparece em 4 lugares (`Reservation.java:77-81`, `ReservationService.java:134-136`, `ReservationController.java:90-92`, `ReservationResponse.java:69-71`).
  - `formatMoney` e `LOCALE_BR` estão copiados em `ReservationService.java:17,158-161` e `FinanceNotificationListener.java:19,63-66`.
- **Formatação de dinheiro errada.** A formatação usa `NumberFormat.getCurrencyInstance(pt-BR)`, que no Java 21 põe um espaço não separável (U+00A0) depois de `R$`. O texto pede `R$ 1.234,56` com espaço comum, e o roteiro confirmou a diferença.
- **A verificação de situação está feita duas vezes.** O serviço confere `canConfirm()`/`canCancel()` (`ReservationService.java:73,92,109,122`), e o modelo confere de novo e lança `IllegalStateException` (`Reservation.java:88,130`). Essa exceção não tem tratamento em `Main` e viraria 500.
- **A reidratação repete as transições.** `ReservationRepository.mapRow` (`:212-226`) reconstrói a situação chamando `confirmWithTotal`/`checkIn`/`cancel`. O `refund` gravado é lido (`:210`) e nunca usado: a devolução é recalculada a cada leitura.
- **Os tipos.**
  - As datas entram no serviço como `String` e são convertidas lá dentro (`ReservationService.java:33,37-38`).
  - `amountPaid` começa como `BigDecimal.ZERO`, com escala 0 (`Reservation.java:33`), e por isso o JSON traz `"amountPaid":0` em vez de `0.00`.
  - O lado bom: `BigDecimal` com `HALF_EVEN`, `LocalDate` e enums para situação, plano e tipo de quarto.
- **Trabalho repetido.** `generateReceiptText` recebe o `RoomType` e mesmo assim busca o quarto de novo (`ReservationService.java:132-133`).

## Testes

**Números:** 11 testes rodaram, 0 falharam, 0 pulados; 11 métodos escritos, em 2 classes. A cobertura é de 9,6% das linhas (79 de 825) e 12,3% dos ramos (19 de 154). Só `domain.model` tem cobertura (79 de 126 linhas). Os pacotes `domain.event`, `domain.event.listeners`, `domain.service`, `repository` e `api` ficam todos em 0 (`measures/tests.md`). Não há teste pela API, nem contra o banco, nem com mocks.
**Nota:** ruim

O que os testes conferem, com valores:
- o preço fora de temporada (`ReservationTest.java:13-25`, 499.80);
- a alta temporada em julho (`:28-40`, 599.76, o exemplo do texto);
- o desconto do NON_REFUNDABLE (`:43-55`, 74.97);
- a devolução no cancelamento, nos dois planos e com a reserva pendente (`:89-133`);
- as constantes dos quartos (`RoomTest.java`).

O que fica sem nenhum teste:
- a parte nova do cenário inteira: o publicador, os três ouvintes, os assuntos, os destinatários, a regra "pendente cancelada não avisa ninguém" e a regra "sem devolução não avisa o financeiro";
- o comprovante e o e-mail do hóspede;
- os relatórios;
- as recusas e os códigos de erro;
- o conflito de datas;
- a alta temporada de dezembro e fevereiro.

A pergunta da mutação, na parte nova: nenhum teste falharia se alguém
- apagasse o `if (!event.wasConfirmed()) return;` de `HousekeepingNotificationListener.java:45`;
- trocasse o assunto "Pagamento recebido - reserva %d" (`FinanceNotificationListener.java:31`);
- tirasse o registro de um ouvinte em `Main.java:54-58`.

Os testes rodam e passam no `mvn verify`, mas cobrem menos de um décimo do código e nada do que o cenário pede.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. Não há chave estrangeira, nem CHECK, nem UNIQUE/EXCLUDE. O esquema é criado por `CREATE TABLE IF NOT EXISTS` no código, sem migrações. Não há pool, há 1 `DriverManager.getConnection`, 0 transações explícitas e 0 `FOR UPDATE` (`measures/database.md`).
**Nota:** com problemas

O que está bom:
- Os tipos estão certos: `amount_paid`/`refund` em `DECIMAL(10,2)` (`ReservationRepository.java:35-36`), `check_in`/`check_out` em `DATE` (`:30-31`) e `NOT NULL` no que é obrigatório.
- Todo o SQL usa `?` com parâmetros e try-with-resources nos statements.
- Os dados ficam no banco depois de reiniciar (o roteiro confirmou, 3 de 3).

Os defeitos:
- **A duplicação de reserva não é protegida.** A verificação de conflito é um `SELECT` (`ReservationRepository.java:123-145`) seguido de um `INSERT` separado (`ReservationService.java:52-62`), sem transação, sem trava, sem restrição de exclusão e sem nem mesmo um `synchronized`. O roteiro criou as duas reservas simultâneas em 7 de 10 rodadas.
- **Falta a chave estrangeira.** `reservations.room_number` não tem chave estrangeira para `rooms.number` (`ReservationRepository.java:26`).
- **Não há CHECK** nas colunas de situação, de plano e de tipo.
- **O esquema não sabe evoluir.** Ele é criado no código (`RoomRepository.java:19-23`, `ReservationRepository.java:23-37`, `MessageRepository.java:17-25`), e o `CREATE TABLE IF NOT EXISTS` não consegue acrescentar uma coluna numa tabela que já existe.
- **Uma conexão sem pool para todas as threads.** `Main.java:39-43` entrega a mesma `Connection` a todos os repositórios, e ela nunca é refeita se cair.
- **A confirmação não é atômica.** Ela grava a reserva (`ReservationService.java:81`) e depois as mensagens (`MessageRepository.java:32-52`) sem transação. Uma falha no meio deixa a reserva confirmada sem os avisos.

Não pus `ruim` porque nada se perde e os tipos estão certos. Mesmo assim, a nota fica no limite: a falta de qualquer proteção contra a reserva dupla é o defeito mais sério do projeto.

## Comportamento

**Números:** 57 de 66 conferências passaram (`measures/behavior.md`):
- a parte comum: 40 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 03: 13 de 15;
- as reservas simultâneas: 1 de 2.

**Nota:** com problemas

Dos 9 FALHOU:
- **O espaço não separável no dinheiro (3 falhas).** O e-mail do hóspede (o comprovante) e os e-mails do financeiro ("Pagamento recebido" e "Reembolso") trazem `R$ 779,80` em vez de `R$ 779,80`. Conferi chamando a API, e o `od` mostra `R $ 302 240 7`. Os valores estão certos e, para quem lê, o e-mail parece igual. Mesmo assim, o texto dá o formato exato, e qualquer programa que leia a mensagem vai falhar. A causa é o `NumberFormat` do pt-BR em `ReservationService.java:158-161` e `FinanceNotificationListener.java:63-66`. Fora isso, o cenário funciona inteiro:
  - a governança e o financeiro recebem os assuntos certos, com as datas, os hóspedes e o valor;
  - no cancelamento de uma confirmada, os dois são avisados;
  - numa NON_REFUNDABLE cancelada, o financeiro não é avisado;
  - uma pendente cancelada não gera nenhuma mensagem.
- **Duas reservas ao mesmo tempo.** Em 7 de 10 rodadas, as duas reservas do mesmo quarto e das mesmas noites foram criadas. Para o dono, isso é o quarto vendido duas vezes quando dois hóspedes reservam pelo site no mesmo instante. A causa está na coluna do banco.
- **Corpo que não é JSON responde 500** ("Server Error"), e não 400 INVALID_REQUEST. A `JsonParseException` do `ctx.bodyAsClass` (`ReservationController.java:26`) não tem tratamento em `Main.java:89-127`.
- **A segunda via de uma reserva pendente responde 200** com o comprovante, e não 409 OPERATION_NOT_ALLOWED. `ReservationController.getDocument` (`:82-87`) não confere a situação, mas o texto diz que a segunda via "só existe para reserva paga".
- **`amountPaid` sai como `0` antes da confirmação**, e não `0.00`, por causa de `BigDecimal.ZERO` (`Reservation.java:33`). O texto pede número com 2 casas.
- **"o 201 comporta 3" e "a diária do 201" (2 falhas)** não pesam. O `GET /rooms` devolve os dados certos (`"capacity":3,"nightlyRate":389.90`), mas com `number` como texto (`"201"`), e o roteiro procura o número 201. O texto não fixa o tipo, então foi uma escolha razoável.

A parte comum funciona no essencial: os preços com a temporada e o arredondamento, os códigos de erro, as transições, as devoluções e os dois relatórios. Ainda assim, há essas falhas reais, além da reserva dupla.

## Dúvidas

- O espaço não separável em `R$`: para quem lê, o e-mail é igual, e por isso pesei pouco esse ponto. Um avaliador mais literal poderia contar o comprovante como fora do formato pedido.
- A nota do banco ficou entre `com problemas` e `ruim`. A skill põe a "duplicação protegida só dentro do processo" em `com problemas`, e aqui não há proteção nenhuma (7 de 10 duplicadas). Escolhi `com problemas` porque os tipos, as chaves primárias e a persistência estão certos.
