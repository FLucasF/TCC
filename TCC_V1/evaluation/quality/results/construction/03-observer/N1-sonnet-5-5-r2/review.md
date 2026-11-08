# Revisão: construção, cenário 03

## Visão geral

O agente montou o projeto do zero em seis pacotes. Uma requisição entra por `web.Routes`, que lê o corpo com o `BookingParser` e chama `app.ReservationService` ou `app.ReportService`. Os serviços abrem a transação com `persistence.Database.transaction` e usam os repositórios JDBC (`ReservationRepository`, `MessageRepository`). As regras (preço, temporada, situações da reserva, devolução, comprovante) ficam em `domain`, que não depende de nenhum outro pacote. Os dados ficam no PostgreSQL, em duas tabelas que o próprio código cria ao subir. Os quartos não estão no banco: são uma lista fixa em `Room.ALL`. A parte que o texto pede desde o primeiro dia virou um Observer. O serviço publica `ReservationConfirmed` ou `ConfirmedReservationCancelled` num `EventBus`, e cada interessado (hóspede, governança, financeiro) é um `Notifier` separado, ligado na montagem do sistema. Os números são bons: 66 de 66 conferências de comportamento passaram, inclusive as reservas simultâneas, e os 12 testes passam com 91,6% de cobertura de linhas. Não há defeito sério. Os pontos fracos são do banco (o esquema é criado no código sem migrações, não há tabela de quartos nem chave estrangeira, e nenhuma coluna tem CHECK) e alguns buracos nos testes da parte nova.

## Arquitetura

**Números:** 6 pacotes (`pousada`, `app`, `domain`, `notification`, `persistence`, `web`), 14 dependências entre eles, nenhum ciclo. `domain` não depende de ninguém e é usado por 4 pacotes. O Javalin aparece só em `web` e no `Main`; o JDBC e o pool só em `persistence`. A maior classe tem 143 linhas (`ReservationRepository`).
**Nota:** bom

- **A borda HTTP é fina.** Cada rota lê a requisição, chama o serviço e escreve a resposta (`web/Routes.java:42-57`). A validação do corpo fica no `BookingParser` (`web/BookingParser.java:19-40`), e os erros saem por um tratador só, a partir do `ErrorCode` (`web/Routes.java:40`, `domain/ErrorCode.java:3-9`).
- **As regras não conhecem Javalin nem SQL.** A máquina de estados e a devolução estão em `domain/Reservation.java:58-78`, e o preço em `domain/Quote.java:13-32`. Os serviços só coordenam a transação, a regra e a publicação do evento (`app/ReservationService.java:29-89`).
- **O acesso ao banco está isolado em `persistence`.** O SQL é todo parametrizado, e a conexão da transação em andamento fica num `ThreadLocal` (`persistence/Database.java:44,78-98`).
- **A montagem fica num lugar só**, em `Main.start` (`Main.java:23-33`). A lista dos interessados fica em `Notifiers.wire` (`notification/Notifiers.java:11-19`), chamado de lá.
- **`persistence -> notification` não é um problema.** `MessageRepository` implementa a interface `MessageSender`, que é definida em `notification` (`persistence/MessageRepository.java:14`). É a dependência no sentido certo.
- **Ressalva pequena:** a rota `/messages` recebe o `Database` e abre a transação ela mesma (`web/Routes.java:30,54`), em vez de passar por um serviço. Daí vem a seta `web -> persistence`. É um trecho só e não pesa na nota.

## Design de baixo nível

**Números:** os 3 interessados (hóspede, governança, financeiro) e os 2 acontecimentos (confirmação e cancelamento de reserva confirmada) estão todos na estrutura; nenhum e-mail é montado fora dela. O serviço publica 2 eventos e não conhece nenhum destinatário.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** O serviço anuncia o acontecimento e não sabe quem é avisado:
- na confirmação, `events.publish(event.apply(r))` com `ReservationConfirmed::new` (`app/ReservationService.java:49,84-86`);
- no cancelamento, publica `ConfirmedReservationCancelled` só quando a reserva estava confirmada (`app/ReservationService.java:59-64`).

O `EventBus` guarda inscrições por tipo de evento (`notification/EventBus.java:10-26`). O serviço recebe o `EventBus` de fora, no construtor (`app/ReservationService.java:23-27`).

Cada interessado é uma classe que implementa `Notifier`:
- `GuestNotifier` (`notification/GuestNotifier.java:15-28`): o hóspede também é um ouvinte, sem chamada direta;
- `HousekeepingNotifier` (`notification/HousekeepingNotifier.java:19-37`);
- `FinanceNotifier` (`notification/FinanceNotifier.java:19-38`).

A regra "sem nada a devolver, o financeiro não é avisado" fica dentro do ouvinte do financeiro (`notification/FinanceNotifier.java:32-34`), não no serviço. Para incluir o restaurante ou o transfer, basta uma classe nova e uma linha em `notification/Notifiers.java:13-16`. É o que a referência chama de acerto: uma interface de ouvinte, uma implementação por interessado, e a lista montada fora do serviço.

Só uma coisa afasta um pouco: a lista de interessados é fixa no código de `Notifiers.wire`. "Entrar e sair conforme os contratos" pede um novo build, mas isso é aceitável neste tamanho de sistema.

**O exagero.** Não há estrutura sem uso:
- a interface `MessageSender` tem uma única implementação de produção (`persistence/MessageRepository.java:14`), mas o teste a substitui por um lambda (`src/test/.../NotifiersTest.java:24`), e ela é o que mantém `notification` independente do JDBC;
- `Notifier` e `EventBus` são o próprio pattern.

**O resto do código.**
- Dinheiro em `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:17-19`, `domain/Quote.java:25`, `domain/RatePlan.java:17-19`).
- Situação, plano e tipo de quarto são `enum` (`domain/ReservationStatus.java`, `domain/RatePlan.java`, `domain/RoomType.java`), e as datas são `LocalDate`.
- O estado só muda por transições validadas (`domain/Reservation.java:94-99`).

Pontos menores, que não mudam a nota:
- `change` recebe uma `Function<Reservation, Object>` que pode ser `null` e usa o nome completo do tipo (`app/ReservationService.java:53,70,78-86`).
- A regra de "reserva paga" está escrita duas vezes, no enum (`domain/ReservationStatus.java:7-9`) e com os nomes das situações dentro do SQL (`persistence/ReservationRepository.java:105`). O mesmo vale para "dinheiro retido", em `domain/Reservation.java:90-92` e em `persistence/ReservationRepository.java:112`.
- `catch (Exception e)` na leitura do JSON (`web/BookingParser.java:23`): aceitável, porque vira 400.

## Testes

**Números:** 12 testes rodaram, 0 falharam, 0 pulados; 12 métodos de teste escritos. Cobertura de 91,6% das linhas (468 de 511) e 77,9% dos ramos (67 de 86). `domain` tem 134 de 134 linhas cobertas e `notification` 59 de 59. Há 3 classes de teste: unidade (`DomainTest`, `NotifiersTest`) e ponta a ponta pela API contra o PostgreSQL (`ApiTest`).
**Nota:** com problemas

**O que está testado com valores:**
- a temporada e as suas bordas, 14/12 e 15/12, 28/02 e 01/03 (`src/test/.../DomainTest.java:32-42`);
- o exemplo do texto, 599,76 (`DomainTest.java:25-29`);
- o desconto com "meio para o par" (`DomainTest.java:45-52`);
- o formato R$ 1.234,56 (`DomainTest.java:55-58`);
- a capacidade (`DomainTest.java:61-65`);
- a máquina de estados e as devoluções dos dois planos (`DomainTest.java:68-94`);
- os códigos de erro pela API (`src/test/.../ApiTest.java:99-108`).

**A parte nova:**
- o e-mail do hóspede é conferido inteiro, linha a linha (`NotifiersTest.java:36-49`);
- os assuntos da governança e do financeiro são conferidos, assim como o valor pago no corpo do financeiro (`NotifiersTest.java:50-53`);
- há testes para "NON_REFUNDABLE cancelada não avisa o financeiro" e "STANDARD avisa" (`NotifiersTest.java:57-74`).

**O que fica sem rede (a pergunta da mutação):**
- **O cancelamento de reserva pendente não avisar ninguém.** Se alguém apagasse o `if (wasConfirmed)` de `app/ReservationService.java:62`, nenhum teste falharia: o único cancelamento que passa pelo serviço é de uma reserva confirmada (`ApiTest.java:84-96`).
- **O corpo dos e-mails da governança.** As datas e o número de hóspedes na confirmação (`notification/HousekeepingNotifier.java:26-28`) e as datas liberadas no cancelamento nunca são conferidos; só os assuntos.
- **O valor a devolver no corpo do "Reembolso".** Também não é conferido.
- **Os relatórios.** A linha `total,N/5` da ocupação, a exclusão do dia de saída e a NON_REFUNDABLE cancelada contando no financeiro não são testadas: o `ApiTest` só confere o cabeçalho, uma linha e um total (`ApiTest.java:70-80`).

**Asserções e ambiente:**
- Algumas asserções comparam dinheiro como `double` (`ApiTest.java:59,68,90`) e não conferem as 2 casas no JSON.
- O `ApiTest` é pulado quando não há `DB_URL` (`ApiTest.java:39`). Aqui rodou.

## Banco

**Números:** 2 tabelas (`reservations` com 16 colunas, `messages` com 5), as duas com chave primária. Todo o dinheiro está em `numeric(12,2)` e as datas em `date`. 0 chaves estrangeiras, 0 CHECK, 0 UNIQUE/EXCLUDE. O esquema é criado com `CREATE TABLE` no código Java, sem migrações. Pool HikariCP, 1 transação explícita, 1 `FOR UPDATE`.
**Nota:** com problemas

**O que está certo:**
- Tipos certos, `NOT NULL` em tudo o que é obrigatório (`persistence/Database.java:15-40`), e um índice em `(room_number, check_in, check_out)` para a busca de conflito (`persistence/Database.java:33`).
- SQL com `?`; os trechos montados com `+` juntam só constantes (`COLUMNS`, `persistence/ReservationRepository.java:20-21,100-112`).
- Conexões em try-with-resources.
- **A reserva dupla é protegida entre processos, não só dentro de um:** um `pg_advisory_xact_lock` pelo número do quarto dentro da transação, antes da verificação e do insert (`persistence/ReservationRepository.java:30-37`, `app/ReservationService.java:34-41`). O roteiro confirmou com 10 rodadas simultâneas.
- As mensagens são gravadas na mesma transação da mudança de situação: uma confirmação nunca fica sem os seus e-mails.

**Os defeitos:**
- **O esquema não evolui.** É `CREATE TABLE IF NOT EXISTS` dentro do código (`persistence/Database.java:14-41,57-68`). O próximo pedido que acrescentar uma coluna não tem por onde entrar num banco que já tem dados.
- **Não há tabela de quartos.** `room_number` é um `INT` solto, sem chave estrangeira (`persistence/Database.java:17`), e os quartos vivem só em `domain/Room.java:9-14`. Um quarto que sair dessa lista quebra a leitura das reservas antigas (`orElseThrow` em `persistence/ReservationRepository.java:133`).
- **Nenhuma restrição no banco.** `status`, `rate_plan` e `channel` são `text` sem CHECK. Também não há CHECK em `guests >= 1` nem em `check_out > check_in`. Todas as regras dependem só do código.
- **Menor:** `findById` sempre trava a linha com `FOR UPDATE` (`persistence/ReservationRepository.java:99-101`), inclusive nas leituras `GET /reservations/{id}` e `/document` (`app/ReservationService.java:44-46,73-75`).

## Comportamento

**Números:** 66 de 66 conferências passaram, 0 falharam:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 03: 15 de 15;
- duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Tudo o que o texto descreve funciona pela API.
- **A parte comum:** os preços com temporada e desconto (599,76; desconto de 37,48), as recusas com os códigos certos, a confirmação com o e-mail e o comprovante, check-in, check-out, as devoluções dos dois planos e os dois relatórios em CSV.
- **O cenário:** na confirmação, a governança recebe "Reserva confirmada - quarto 201" com 10/03/2027, 12/03/2027 e 2 hóspedes, e o financeiro recebe "Pagamento recebido - reserva 1" com R$ 779,80. No cancelamento de uma reserva confirmada, a governança e o financeiro são avisados, este com "Reembolso" e o valor. Na NON_REFUNDABLE cancelada, o financeiro não é avisado. Na pendente cancelada, ninguém é avisado.
- **Os dados e a concorrência:** os dados continuam depois de reiniciar, e nenhuma das 10 rodadas simultâneas criou duas reservas para o mesmo quarto.

Para o dono da pousada: o sistema faz o que ele pediu, e os avisos para a governança e o financeiro saem certos.

## Dúvidas

A lista de interessados é fixa em `Notifiers.wire` (`notification/Notifiers.java:13-16`), e não é passada pelo `Main`. Contei como "registrada na montagem do sistema", porque é uma classe de montagem que o `Main` chama e o serviço não a conhece. Um leitor mais estrito poderia pedir que a lista ficasse no próprio `Main`, mas isso não muda a classe.
