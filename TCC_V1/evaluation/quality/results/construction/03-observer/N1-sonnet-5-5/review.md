# Revisão: construção, cenário 03

## Visão geral

O agente montou um serviço Javalin com cinco pacotes além do `pousada.Main`: `web` (rotas e visões JSON), `app`
(serviços, eventos, o barramento de eventos), `domain` (reserva, preço, temporada, planos, comprovante, formatos),
`infra` (pool Hikari, transação, repositório JDBC, lista de mensagens no banco) e `notification` (um ouvinte por
interessado). Uma requisição entra em `web/Api.java:44-50`, que lê o corpo e chama `ReservationService`. O serviço
abre a transação em `infra/Database.java:69`, trava e lê a reserva (`ReservationRepository.java:69`, `FOR UPDATE`),
aplica a regra do domínio (`Reservation.java:54-70`) e grava. Depois do commit ele publica `ReservationConfirmed` ou
`ConfirmedReservationCancelled` no `EventBus` (`ReservationService.java:44,55`). Os dados ficam em duas tabelas
PostgreSQL criadas no próprio código (`Database.java:16-43`). Os quartos ficam num catálogo fixo em Java
(`RoomCatalog.java:7-12`). O comportamento passou inteiro (66 de 66), os 17 testes passam com 95,3% de cobertura de
linhas, e o Observer pedido está bem aplicado. Não há defeito grave. Os pontos fracos: um ciclo `app ↔ infra`, o
esquema criado com `CREATE TABLE IF NOT EXISTS` em vez de migrações, e o barramento, que engole a falha de um ouvinte
e só a imprime no stderr.

## Arquitetura

**Números:** 6 pacotes, 14 dependências entre eles, 1 ciclo (`pousada.app` ↔ `pousada.infra`). `domain` não depende
de ninguém e é usado por 5 pacotes. Javalin só em `pousada` e `pousada.web`, JDBC em `app` e `infra`. A maior classe
é `Api.java`, com 142 linhas.
**Nota:** com problemas

As camadas existem e a borda é fina. Cada rota em `web/Api.java:43-55` só converte a entrada, chama o serviço e
escreve a saída, e o mapeamento de erro para status fica em um `switch` só (`Api.java:65-72`). As regras de preço,
temporada, situação e devolução estão no `domain` sem Javalin nem JDBC (`Quote.java:9-18`, `Season.java:13-21`,
`Reservation.java:54-83`, `RatePlan.java:17-23`). O SQL está todo em `infra` (`ReservationRepository.java`,
`MessageLog.java`). A montagem fica num lugar só, `Main.build` (`Main.java:26-40`), que também inscreve os ouvintes.

Os defeitos:
- **O ciclo `app ↔ infra`.** Os serviços de `app` dependem das classes concretas `infra.Database` e
  `infra.ReservationRepository` (`ReservationService.java:4-5`, `ReportService.java:4-5`). Ao mesmo tempo,
  `infra.MessageLog` implementa `app.MessageSender` e usa `app.Message` (`MessageLog.java:3-4,12`). A inversão foi
  feita para as mensagens e não para o repositório, e o resultado é o ciclo.
- **O `java.sql.Connection` vaza para `app`.** Os serviços repassam a conexão dentro das lambdas de transação
  (`ReservationService.java:28-35,73-78`), e por isso o jdeps marca JDBC em `app`. A fronteira da transação no
  serviço é razoável, mas amarra o serviço ao JDBC.
- **A borda usa um repositório concreto.** `web.Api` depende direto de `infra.MessageLog` para listar as mensagens
  (`Api.java:13,27,51`), sem passar por um serviço.

É uma mistura localizada, num sistema que, fora isso, está bem separado.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança, financeiro), cada um num ouvinte próprio. São 5 inscrições ao todo
(confirmação: 3; cancelamento de reserva confirmada: 2). Nenhum e-mail é montado ou enviado fora dos ouvintes. O
serviço de reservas não conhece nenhum destinatário.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O serviço anuncia o que aconteceu e não sabe quem é avisado: `events.publish(new
ReservationConfirmed(...))` em `ReservationService.java:44`, e `ConfirmedReservationCancelled` só quando a reserva
estava confirmada em `ReservationService.java:54-56`. Isso cumpre "a reserva pendente cancelada não avisa ninguém"
sem que o serviço pergunte por destinatário. Cada interessado é uma classe que implementa `EventSubscriber`
(`EventSubscriber.java:4-6`) e se inscreve nos eventos que lhe interessam:
- o hóspede: `GuestNotifier.java:15-27`, também ouvinte, como a referência exige;
- a governança: `HousekeepingNotifier.java:17-34`;
- o financeiro: `FinanceNotifier.java:17-33`. A regra "sem devolução, não avisa o financeiro" mora no próprio
  ouvinte (`FinanceNotifier.java:28-30`).

A lista de ouvintes é montada fora, em `Main.java:32-36`. Para pôr o restaurante ou o transfer, basta uma classe nova
e uma linha nessa lista, sem tocar no serviço. O `EventBus` tipado por classe de evento (`EventBus.java:12-28`) é um
Observer completo. O que aproxima do acerto: nenhum caso ficou fora da estrutura, e o cancelamento também tem
ouvintes. Nada afasta.

**O exagero.** Não há estrutura sobrando:
- `EventSubscriber` tem três implementações.
- `MessageSender` (`MessageSender.java:3-5`) tem uma só, `MessageLog`, mas tem dois motivos para existir: deixa
  `notification` sem depender de `infra`, e o texto diz que os e-mails "por enquanto" não saem de verdade.
- Não há fábricas, builders nem camadas que só repassam.

**O resto do código.** No geral está limpo:
- `BigDecimal` com `HALF_EVEN` em cada passo (`Money.java:16-18`, `Season.java:20`, `Quote.java:14-17`,
  `RatePlan.java:18`);
- `LocalDate` para as datas e `enum` para situação, plano e tipo de quarto (`ReservationStatus.java`, `RatePlan.java`,
  `RoomType.java`);
- as transições de situação concentradas em `Reservation.moveTo` (`Reservation.java:77-83`);
- o comprovante montado uma vez e reaproveitado no e-mail do hóspede (`Document.java:23-36`, `GuestNotifier.java:23`).

Os defeitos, localizados:
- **A falha de um ouvinte é engolida.** `EventBus.publish` captura a `RuntimeException`, imprime no `System.err` e
  segue em frente (`EventBus.java:22-26`). Como a publicação acontece depois do commit
  (`ReservationService.java:43-44`) e cada mensagem é gravada numa transação própria (`MessageLog.java:21`), uma falha
  ao gravar o e-mail da governança deixa a reserva confirmada, a resposta em 200 e a mensagem perdida, sem nada além
  de uma linha no stderr. Não há log estruturado nem nova tentativa.
- **O cancelamento guarda a situação anterior com um `AtomicReference`** dentro de um `Consumer`
  (`ReservationService.java:49-53`). É um contorno pouco claro: daria para a própria `Reservation.cancel()` dizer se a
  reserva estava confirmada.
- **`Reservation` nasce com `id = 0`** como valor provisório e ganha outro objeto em `withId`
  (`Reservation.java:45,49-52`). Funciona, mas o `0` pode vazar, por exemplo na mensagem de erro de `moveTo`
  (`Reservation.java:80`).

## Testes

**Números:** 17 testes rodaram, 0 falhas, 0 erros, 0 pulados; 17 métodos escritos (9 em `DomainTest`, 8 em
`ApiTest`). Cobertura de 446 de 468 linhas (95,3%) e de 74 de 86 ramos (86,0%). `pousada.notification` tem 40 de 40
linhas, `pousada.app` 82 de 84 e `pousada.domain` 132 de 132.
**Nota:** bom

As regras são conferidas com valores:
- **Preço e temporada:** a alta temporada (599,76 em `DomainTest.java:19-24`) e os limites da temporada, inclusive
  14/12, 15/12, 29/02 e 01/03 (`DomainTest.java:27-35`).
- **Desconto:** o NON_REFUNDABLE com meio para o par, 116,97 e 662,83 (`DomainTest.java:38-43`).
- **Situações e devoluções:** as transições e as devoluções por plano e situação (`DomainTest.java:52-80`).
- **Comprovante:** o texto exato, a linha de desconto, "1 diária" e "R$ 1.234,56" (`DomainTest.java:83-103`).
- **Recusas:** todos os códigos de erro pela API (`ApiTest.java:222-236`).
- **Relatórios:** os dois CSV comparados por inteiro, inclusive a NON_REFUNDABLE cancelada que continua contando
  (`ApiTest.java:344-366`).

A parte nova tem testes de ponta a ponta contra o banco:
- na confirmação, as três mensagens, o e-mail do hóspede comparado por inteiro, os assuntos da governança e do
  financeiro, as datas e o valor (`ApiTest.java:248-284`);
- no cancelamento, os assuntos e o valor de reembolso (`ApiTest.java:294-307`);
- a NON_REFUNDABLE cancelada avisando só a governança, por contagem (`ApiTest.java:309-314`);
- a pendente cancelada sem aviso nenhum (`ApiTest.java:316-320`).

Pela pergunta da mutação, três pontos ficam sem rede:
- se `HousekeepingNotifier` deixasse de escrever o número de hóspedes (`HousekeepingNotifier.java:26`), nenhum teste
  falharia, porque `ApiTest.java:279-280` só confere as datas;
- se o aviso de cancelamento da governança perdesse as datas (`HousekeepingNotifier.java:32-33`), também nenhum,
  porque `ApiTest.java:303` só confere o assunto;
- o isolamento entre ouvintes, em que um que falha não impede os outros (`EventBus.java:22-26`), não tem teste.

`ApiTest` se desliga com `Assumptions` quando `DB_URL` não existe (`ApiTest.java:143-144`), mas nesta medição rodou
inteiro. São lacunas pequenas diante do que está conferido.

## Banco

**Números:** 2 tabelas (`reservations`, com 15 colunas, e `messages`, com 5), ambas com chave primária. Dinheiro em
`numeric(12,2)` nas 5 colunas, datas em `date`. Nenhuma chave estrangeira, nenhum CHECK, nenhum UNIQUE ou EXCLUDE.
Esquema criado por `CREATE TABLE` no código, sem arquivo de migração. SQL só com parâmetros, pool Hikari, transação
explícita e `FOR UPDATE`.
**Nota:** com problemas

O que está certo:
- **Tipos e restrições básicas:** os valores em `NUMERIC(12,2)` e as datas em `DATE`, com `NOT NULL` em tudo o que é
  obrigatório (`Database.java:17-42`).
- **SQL:** todo com `?`; os trechos concatenados são constantes, como `COLUMNS` e `" FOR UPDATE"`
  (`ReservationRepository.java:12-13,69`).
- **Conexões:** devolvidas por try-with-resources, com pool (`Database.java:47-55,70`).
- **Reserva duplicada:** fica impedida mesmo com vários processos, por um `pg_advisory_xact_lock` por quarto seguido
  da consulta de sobreposição na mesma transação (`ReservationRepository.java:22-40`,
  `ReservationService.java:28-35`). As 10 rodadas simultâneas do roteiro passaram.
- **Índices:** há índices para a sobreposição e para o relatório financeiro (`Database.java:34-35`).
- **Mudanças de situação:** usam `SELECT ... FOR UPDATE` (`ReservationRepository.java:69`).

Os defeitos:
- **O esquema não evolui.** Ele é um `CREATE TABLE IF NOT EXISTS` dentro do Java (`Database.java:16-43,58-66`), sem
  migração versionada. O próximo pedido que acrescentar uma coluna não terá como alterar a tabela de quem já roda o
  sistema.
- **O banco não protege as regras sozinho:**
  - não há CHECK em `status`, `rate_plan` nem `channel`;
  - não há tabela de quartos nem chave estrangeira; `room_number` é um `INT` solto (`Database.java:19`), e os quartos
    existem só em `RoomCatalog.java:7-12`;
  - a regra de "uma reserva por quarto por noite" depende de todo código que escreve passar pela trava consultiva,
    porque não há `EXCLUDE`.
- **A confirmação e os avisos não andam juntos.** As mensagens são gravadas em transações separadas, depois do commit
  da confirmação (`MessageLog.java:21`, `ReservationService.java:43-44`). Com a falha engolida no barramento, um aviso
  pode se perder sem que a confirmação seja desfeita.

Nada disso perde ou corrompe dado na operação normal, por isso a nota não é `ruim`.

## Comportamento

**Números:** 66 de 66 conferências passaram, 0 falharam: parte comum 46 de 46, dados depois de reiniciar 3 de 3,
cenário 03 15 de 15, reservas simultâneas 2 de 2.
**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede:
- **Preços:** a alta temporada e o arredondamento estão certos (599,76; desconto de 37,48; total de 212,42).
- **Recusas:** respondem com os códigos e status pedidos.
- **Reservas:** a confirmação, o check-in, o check-out, a segunda via e as devoluções por plano seguem as regras.
- **Relatórios:** os dois saem em CSV no formato pedido.
- **Persistência:** os dados sobrevivem a um reinício.
- **Reservas simultâneas:** duas reservas ao mesmo tempo para o mesmo quarto nunca passam juntas.

Na parte nova, a governança e o financeiro recebem os avisos certos na confirmação, com as datas, o número de
hóspedes e o valor pago. No cancelamento de reserva confirmada, a governança recebe as datas e o financeiro recebe o
valor a devolver. O financeiro não é avisado quando não há o que devolver, e a reserva pendente cancelada não avisa
ninguém.

Fora do roteiro, uma observação, sem mudar a nota (o defeito pesa no design e no banco): se a gravação de um aviso
falhar, a confirmação responde 200 e o aviso some sem erro visível (`EventBus.java:22-26`).

## Dúvidas

Na nota do design, entre `bom` e `com problemas`, a evidência pende para `com problemas` por pouco. O código é limpo e
o Observer está certo. O que pesou foi a exceção engolida no barramento (`EventBus.java:22-26`), somada à publicação
fora da transação. Um revisor que aceite isso como escolha consciente (está comentado em `EventBus.java:19`) daria
`bom`.
