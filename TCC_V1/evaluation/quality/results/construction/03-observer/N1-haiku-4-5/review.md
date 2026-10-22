# Revisão: construção, cenário 03

## Visão geral

O projeto tem 7 pacotes sob `com.pousada`: `api` (os handlers do Javalin, um por recurso, todos `static`), `service`
(`ReservationService`, `PricingService`, `FormattingService`), `database` (um `HikariDataSource` estático e três
repositórios JDBC estáticos), `model` (classes com campos públicos), `event` (a interface de ouvinte, os dois eventos e
um `EventDispatcher` estático) e `event.listener` (um ouvinte por interessado: hóspede, governança, financeiro). Uma
requisição entra em `Application.java:25-35`, vai para um método estático de `ReservationsEndpoint`, que lê o corpo
com Gson e chama `ReservationService`, que chama os repositórios e depois `EventDispatcher`. Os dados ficam no
PostgreSQL, em tabelas criadas com `CREATE TABLE IF NOT EXISTS` em `Database.java:37-78`. O mais sério: **o sistema
não sobe**. `Database.java:74` chama `conn.commit()` numa conexão com autoCommit ligado, e o PostgreSQL recusa com
"Cannot commit when autoCommit is enabled". Os scripts do banco e do comportamento pararam aí. Numa cópia descartável,
fora de `projeto/`, tirei esse `commit` e apareceram mais dois defeitos que também derrubam o sistema. As rotas usam
`/:id` (`Application.java:27-32`), sintaxe que o Javalin 6 rejeita na subida ("Javalin 4 switched from ':param' to
'{param}'"). E o `pom.xml` não traz Jackson (`pom.xml:18-61`): com as rotas corrigidas, toda resposta com `ctx.json`
(inclusive `GET /rooms`) volta 500 "you don't have an object mapper configured". O Observer pedido está bem
estruturado, mas nunca chega a rodar.

## Arquitetura

**Números:** 7 pacotes, 16 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em
`com.pousada` e `com.pousada.api`, JDBC só em `com.pousada.database`. A maior classe é `ReservationRepository`, com 226
linhas.
**Nota:** com problemas

A direção das dependências está certa (api → service → database → model) e as regras não conhecem o Javalin nem o
JDBC. A separação, porém, vaza em vários pontos:

- A borda HTTP passa por cima do serviço: `ReservationsEndpoint.java:68` e `:153` chamam `ReservationRepository.findById`
  direto, `:122` e `:167` chamam `RoomRepository`. A regra "segunda via só para reserva paga" mora no handler
  (`ReservationsEndpoint.java:161`), e o comprovante é montado ali, duas vezes (`:125-139` e `:170-184`).
- Os relatórios são regra de negócio feita dentro do handler. O valor que fica com a pousada (pago menos devolvido)
  está em `ReportsEndpoint.java:77-80`; o casamento quarto × reserva e a contagem de ocupação, em `:32-51`. Não existe
  serviço de relatório.
- O serviço conhece HTTP: `ReservationService.ApiException` leva o código de status (`ReservationService.java:30`, `:38`,
  `:149-157`).
- Não há montagem. Tudo é `static`: o `DataSource` é um campo estático global (`Database.java:10`), e os serviços chamam
  os repositórios por métodos estáticos (`ReservationService.java:28`, `:46`, `:55`). Só os ouvintes são ligados no
  `main` (`Application.java:20-22`). Trocar uma peça, ou testar uma sem banco, exige mexer no código de quem a usa.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança, financeiro) × 2 eventos (confirmação, cancelamento). Os três são
ouvintes de `ReservationEventListener`, todos registrados no `main`. Nenhum e-mail é montado ou enviado pelo serviço.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** `ReservationService` anuncia o fato e não sabe quem é avisado:
`EventDispatcher.dispatchConfirmed(...)` em `ReservationService.java:84` e `dispatchCancelled(...)` em `:116`. O
`EventDispatcher` percorre a lista de ouvintes (`EventDispatcher.java:13-23`). Cada interessado é uma classe:
`GuestNotificationListener` (o e-mail do hóspede também virou ouvinte, `GuestNotificationListener.java:15-31`),
`HousekeepingNotificationListener` e `FinancialNotificationListener`. Os três são registrados em
`Application.java:20-22`. As condições do cancelamento ficam em cada ouvinte: a governança só se a reserva estava
confirmada (`HousekeepingNotificationListener.java:32`), o financeiro só se há algo a devolver
(`FinancialNotificationListener.java:27`). Para pôr o restaurante ou o transfer, basta uma classe nova e uma linha no
`main`. O que afasta um pouco da referência:

- a lista não é recebida de fora pelo serviço. É um `static` mutável global (`EventDispatcher.java:7`), preenchido
  pelo `main`;
- a interface obriga todo ouvinte a tratar os dois eventos, e o hóspede fica com um método vazio
  (`GuestNotificationListener.java:33-35`).

Nenhum dos dois põe a variação de volta num `if` do serviço.

**Exagero.** Não há. A classe-base `ReservationEvent` (`ReservationEvent.java:5-15`) e os dois eventos são parte do
pattern. Fora dele não há interfaces de uma implementação só, fábricas nem camadas de repasse.

**O resto do código.** Os defeitos se espalham pelo projeto:

- **Regra errada no preço.** `isHighSeason` trata dezembro inteiro como alta temporada (`PricingService.java:67-69`); o
  texto diz "de 15 de dezembro". A variável `dayOfMonth` é lida e nunca usada (`:61`). Uma reserva de 01/12 a 14/12
  sai 20% mais cara.
- **Formato do dinheiro.** `formatCurrency` não põe separador de milhar (`FormattingService.java:18`): sai "R$ 1234,56",
  e não "R$ 1.234,56" como o comprovante pede. `formatCsv` usa `String.format("%.2f")` com a localidade da máquina
  (`FormattingService.java:22`): num servidor em pt-BR, o CSV sai com "599,76". `CURRENCY_FORMAT` é declarado e nunca
  usado (`:11`).
- **Texto solto no lugar de tipos.** Situação e plano são `String` (`ReservationService.java:76`, `:96`, `:106-110`;
  `ReservationsEndpoint.java:161`), e o plano nunca é validado. Um `ratePlan` desconhecido é aceito com preço normal
  (`PricingService.java:49`). Se essa reserva for confirmada e depois cancelada, fica com `refund` nulo
  (`ReservationService.java:105-110`).
- **Duplicação.** O comprovante JSON aparece duas vezes, igual (`ReservationsEndpoint.java:125-139` e `:170-184`), e o
  texto do comprovante tem uma terceira versão em `GuestNotificationListener.java:37-73`. A busca seguida do 404
  repete-se quatro vezes (`ReservationService.java:71-74`, `:90-93`, `:122-125`, `:136-139`). A lista de colunas do
  `SELECT` aparece quatro vezes em `ReservationRepository.java`. A conversão do id inválido para 404 também se repete
  seis vezes no endpoint.
- **Estado.** Todo o modelo tem campos públicos (`Reservation.java:7-22`, `Room.java:6-9`), e a requisição
  `CreateReservationRequest` tem campos públicos e mutáveis (`ReservationService.java:16-25`).
- **Erros.** `catch (Exception e)` transforma qualquer falha, inclusive do banco, em 400 `INVALID_REQUEST`
  (`ReservationsEndpoint.java:60-61`). Os repositórios embrulham `SQLException` num `RuntimeException` genérico
  (`ReservationRepository.java:58-59` e seguintes).
- **Código morto.** `total` é calculado e descartado em `PricingService.java:54`.

## Testes

**Números:** 4 testes rodaram, 0 falhas, 0 erros, 0 pulados. São 4 métodos escritos, num arquivo só. Cobertura de
linhas: 33 de 721 (4.6%); de ramos: 8 de 162 (4.9%). Só `com.pousada.service` tem alguma cobertura (33 de 108 linhas);
`api`, `database`, `event` e `event.listener` ficam com 0 (`measures/tests.md`). Nenhum teste passa pela API nem pelo
banco.
**Nota:** ruim

Os quatro testes conferem só o `PricingService`, com valores: R$ 599,76 em julho (`ReservationTest.java:17-29`), o
desconto de R$ 89,96 e o total de R$ 509,80 (`:32-44`), e o arredondamento meio para o par (`:60-68`). Fora isso, não
há teste algum:

- **A parte nova do cenário.** Nada confere os e-mails da governança e do financeiro, nem o silêncio no cancelamento de
  uma reserva pendente. Se alguém apagasse o `return` de `FinancialNotificationListener.java:27-29` ou de
  `HousekeepingNotificationListener.java:32-34`, ou trocasse o assunto, nenhum teste falharia.
- **As regras de negócio.** Também sem teste: transições de situação, devolução, conflito de datas, capacidade, códigos
  de erro, comprovante, relatórios.
- **A temporada.** O teste de alta temporada usa 20/12 (`ReservationTest.java:48`) e não pega o erro de 01/12 a 14/12
  (`PricingService.java:67-69`).

O `mvn verify` passa com um sistema que não sobe: os testes não exercitam nem a inicialização do banco, nem as rotas,
nem a serialização JSON.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(10,2)`,
datas em `date`. Conferências que acusaram algo: a chave estrangeira `reservations(room_number) -> rooms` sem índice;
`status`, `rate_plan`, `type` e `channel` sem CHECK; nenhuma restrição UNIQUE, EXCLUDE ou CHECK; 0 transações
explícitas; 0 `FOR UPDATE`. O serviço não subiu contra um banco vazio (`measures/database.md`).
**Nota:** ruim

- **O serviço não sobe contra um banco vazio.** `createTables` executa os `CREATE TABLE` e depois chama
  `conn.commit()` com autoCommit ligado (o padrão do Hikari). O driver lança `PSQLException: Cannot commit when
  autoCommit is enabled` em `Database.java:74`, e a inicialização aborta (`Database.java:76`, `Application.java:18`).
  Como os `CREATE` já tinham sido executados em autocommit, as três tabelas ficam, mas os quartos nunca são inseridos.
  O mesmo `commit()` está em todas as escritas: `Database.java:103`, `ReservationRepository.java:91`, `:106`, `:121` e
  `MessageRepository.java:24`. Mesmo depois de subir, confirmar, cancelar, fazer check-in ou check-out e gravar
  mensagem dariam erro depois de o `UPDATE` já ter sido aplicado.
- **Nada impede reserva dupla.** O conflito é checado com um `SELECT` (`ReservationService.java:45-50`) e a reserva é
  inserida noutra conexão (`:55`), sem transação, sem trava e sem uma restrição EXCLUDE. Dois pedidos simultâneos
  podem criar duas reservas para a mesma noite.
- **Escritas sem transação.** A confirmação grava a situação (`ReservationService.java:81`) e depois as mensagens, cada
  uma noutra conexão (`MessageRepository.java:17-24`). Uma falha no meio deixa a reserva confirmada sem os avisos.
- **O esquema não evolui.** Ele é criado no código com `CREATE TABLE IF NOT EXISTS` (`Database.java:39-72`), sem
  migração versionada, e não sabe acrescentar uma coluna a uma tabela que já existe.
- **Situação e plano sem CHECK** (`Database.java:56`, `:61`).
- **O que está certo:** o SQL usa parâmetros em todas as consultas com dado de fora (os `+` listados pelo script são
  constantes), as conexões vêm do pool e são fechadas com try-with-resources, e os tipos de dinheiro e de data estão
  certos.

## Comportamento

**Números:** 0 conferências rodaram. O roteiro `check_03.sh` registrou "não deu para testar": o serviço não subiu
contra um banco vazio, com a pilha em `Database.java:74` (`measures/behavior.md`).
**Nota:** ruim

Para o dono da pousada, o sistema entregue não atende nenhuma chamada: o `java -jar` termina com erro antes de abrir a
porta. Numa cópia descartável, fora de `projeto/`, contornei esse primeiro erro e apareceram outros dois, cada um
suficiente para derrubar o sistema:

- as rotas `/reservations/:id...` (`Application.java:27-32`) fazem o Javalin 6.7.0 abortar na subida, com
  `IllegalArgumentException: Path '/reservations/:id' invalid`;
- com as rotas também corrigidas, toda resposta JSON volta 500 "you don't have an object mapper configured" (sem
  Jackson no `pom.xml:18-61`): `GET /rooms`, `POST /reservations` e até o corpo não-JSON, que devia dar 400.

Pela leitura, há ainda erros que o roteiro pegaria se o sistema rodasse:

- o preço de 1 a 14 de dezembro com 20% a mais (`PricingService.java:67-69`);
- valores acima de mil reais sem o ponto de milhar no comprovante e nos e-mails (`FormattingService.java:18`);
- o comprovante JSON sem o campo `discount` quando ele é zero (`ReservationsEndpoint.java:136-138`), embora o texto o
  liste entre os campos;
- datas do comprovante JSON em `dd/MM/yyyy` (`ReservationsEndpoint.java:132-133`), enquanto o resto da API usa
  `2027-07-10`;
- reservas simultâneas sem proteção (ver Banco).

## Dúvidas

- **A classe do pattern.** Dei `acerto`: a estrutura cobre os três interessados e os dois eventos, e o serviço não
  conhece nenhum. A referência, porém, fala de um serviço que recebe a lista de ouvintes de fora, e aqui o serviço
  chama um despachante estático global (`EventDispatcher.java:7`, `ReservationService.java:84`, `:116`). Um revisor
  mais estrito poderia ler isso como `malfeito`. Fiquei com `acerto` porque a variação está toda nos ouvintes, e
  acrescentar ou tirar um interessado não toca o serviço.
- **As conferências do comportamento.** Os itens da parte comum e do cenário não foram medidos pelo roteiro, porque o
  serviço não subiu. Os erros que listei além dos três defeitos de subida vêm da leitura do código, não de chamadas à
  API.
