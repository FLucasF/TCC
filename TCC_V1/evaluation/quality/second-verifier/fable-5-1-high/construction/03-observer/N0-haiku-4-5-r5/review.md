# Revisão: construção, cenário 03

## Visão geral

O projeto está em `com.pousada.marealta`, em oito pacotes: `controller` (os handlers do Javalin, um por recurso),
`service` (`ReservationService` e `PricingService`), `repository` (JDBC puro, uma conexão nova por chamada),
`notification` (o Observer do cenário: `NotificationService`, a interface `NotificationListener` e três ouvintes),
`domain`, `util` e `config` (`DatabaseConfig`, que cria as tabelas com `CREATE TABLE IF NOT EXISTS` ao subir). Uma
requisição entra num handler estático (`ReservationController.register`), vai para `ReservationService`, que valida,
calcula o preço e chama o repositório; a confirmação e o cancelamento avisam `NotificationService`, que percorre os
ouvintes, e cada ouvinte grava a mensagem na tabela `messages`. O mais sério: **o sistema não funciona pela API**.
O `pom.xml` traz o Gson (`pom.xml:34-38`), mas o Javalin 6.7.0 só usa o Jackson, e nem o `jackson-databind` está no
classpath nem um `jsonMapper` foi configurado em `Javalin.create()` (`Application.java:19`). Com isso, todo
`ctx.json(...)` e todo `ctx.bodyAsClass(...)` falham com 500 ("It looks like you don't have an object mapper
configured", `measures/behavior-service.log:21`): nenhuma reserva é criada, nenhum quarto é listado, nenhum e-mail
sai. Só os dois relatórios em CSV respondem. O roteiro do comportamento passou em 6 de 66 conferências, e o único
teste do agente (6 métodos sobre `PricingService`) não toca o HTTP, por isso o `mvn verify` passa sem acusar nada.

## Arquitetura

**Números:** 8 pacotes, 20 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Maior classe:
`ReservationController`, 288 linhas; depois `ReservationService`, 254.
**Nota:** com problemas

O que está certo: o Javalin só aparece em `Application` e em `controller`; `ReservationService` não conhece `Context`
nem SQL (`service/ReservationService.java:1-11`); todo SQL mora em `repository`; os handlers de reserva só leem a
requisição, chamam o serviço e traduzem exceções em códigos (`controller/ReservationController.java:28-71`); sem
ciclos.

O que mistura camadas ou espalha a montagem:

- A montagem não está num lugar só. `Application.main` só chama `DatabaseConfig.initialize()` e
  `NotificationService.getInstance().initialize(...)` (`Application.java:14-17`); as peças de verdade são criadas em
  campos `static` do controller (`controller/ReservationController.java:20-25`) e com `new` dentro dos handlers
  (`controller/RoomController.java:12`, `controller/MessageController.java:12`,
  `controller/ReportController.java:65-66,93`). `NotificationService` é um singleton global com `getInstance()`
  (`notification/NotificationService.java:9-20`), e `DocumentFormatter`, em `util`, cria o seu próprio
  `RoomRepository` estático e vai ao banco (`util/DocumentFormatter.java:11-15`), o que dá a dependência
  `util -> repository` que o jdeps mostra: um pacote de utilidades que consulta o banco está na camada errada.
- Regra de negócio na borda HTTP: os dois relatórios inteiros (quais reservas contam, `pago − devolvido`, a linha
  `total`) estão em `controller/ReportController.java:64-119`; a regra de que a segunda via só existe para reserva
  paga está no handler (`controller/ReservationController.java:186-190`); a montagem do comprovante também
  (`controller/ReservationController.java:250-267`).
- O JDBC vaza para a borda: todo handler captura `SQLException` e devolve `e.getMessage()` ao cliente
  (`controller/ReservationController.java:67-70` e os demais); o jdeps marca `controller`, `notification`, `service`
  e `util` como usuários de JDBC.

Nenhum ciclo e nenhuma classe desproporcional; as misturas são localizadas, mas são várias. Daí `com problemas`.

## Design de baixo nível

**Números:** 1 interface de ouvinte com 2 eventos, 3 ouvintes (hóspede, governança, financeiro), 0 e-mails
montados fora dos ouvintes; o serviço chama o assunto em 2 pontos (confirmação e cancelamento). Quatro cópias de uma
cadeia de 15 chamadas do `Builder` no serviço.
**Classe:** acerto
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** A variação ficou num Observer: `NotificationListener` com `onReservationConfirmed` e
`onReservationCancelled` (`notification/NotificationListener.java:5-8`); um ouvinte por interessado, inclusive o
hóspede (`GuestNotificationListener.java:19-27`, `GovernanceNotificationListener.java:19-58`,
`FinanceNotificationListener.java:19-42`); o assunto guarda a lista, tem `addListener` e percorre a lista nos dois
eventos (`notification/NotificationService.java:10,28-42`); `ReservationService` recebe o assunto pelo construtor e só
anuncia o evento, sem saber quem ouve (`service/ReservationService.java:18-24,92,136`). Nenhum e-mail é montado no
serviço, e o cancelamento também passa pelos ouvintes. O que afasta do acerto pleno: os três ouvintes não são
registrados na montagem, e sim dentro do próprio assunto, em `NotificationService.initialize`
(`notification/NotificationService.java:22-26`), que conhece as três classes concretas; o restaurante e o transfer
entrariam ou editando esse método ou chamando o `addListener` que existe mas ninguém usa. A estrutura é a do Observer
e a dependência do serviço está do lado certo, por isso `acerto`, com essa ressalva (ver "Dúvidas").

Um defeito lógico dentro do pattern: `GovernanceNotificationListener.onReservationCancelled` só avisa se a reserva
estiver `CONFIRMED`, `CHECKED_IN` ou `CHECKED_OUT` (`GovernanceNotificationListener.java:39-43`), mas o serviço
entrega ao assunto a reserva **já** com `CANCELLED` (`service/ReservationService.java:130,136`), então o aviso
"Reserva cancelada - quarto N" nunca sai. O financeiro acerta por outro caminho (olha o `refund`,
`FinanceNotificationListener.java:31`). A informação de "estava confirmada" não chega aos ouvintes.

**O exagero.** `Reservation` é imutável com um `Builder` de 15 campos (`domain/Reservation.java:61-99`), e o preço
disso é que cada mudança de situação copia os 15 campos à mão, quatro vezes (`service/ReservationService.java:72-89,
116-133, 151-168, 184-201`); um objeto com `withStatus(...)` ou três setters controlados resolveria. O singleton
`NotificationService.getInstance()` (`notification/NotificationService.java:9-20`) também é estrutura sem necessidade
(e sem segurança entre threads) num sistema que já recebe o serviço pelo construtor.

**O resto do código.** Tipos certos: `BigDecimal` com `HALF_EVEN` em cada passo (`util/MathUtil.java:7-9`,
`service/PricingService.java:21-31`), `LocalDate`, enums para plano, situação, tipo e canal. Os defeitos:

- Duplicação: as quatro cópias do builder acima; `error(...)` repetido em dois controllers
  (`ReservationController.java:269-276`, `ReportController.java:125-132`); o `catch (SQLException)` com 500 copiado
  em todos os sete handlers; o mapeamento de linha repetido em `repository/RoomRepository.java:18-23,38-43`.
- Regras erradas na leitura: a alta temporada é decidida para a estadia inteira, se **alguma** noite cair nela todas
  as noites sobem 20% (`service/PricingService.java:19-24,36-54`), quando o texto diz que cada noite tem o seu
  preço (uma estadia de 13/12 a 17/12 cobra as quatro noites com acréscimo); o `refund` volta a `null` quando é
  zero (`service/ReservationService.java:132`), e o texto diz que ele é `null` só enquanto não cancelada; o dinheiro
  nos e-mails sai com espaço não quebrável (`NumberFormat` pt-BR, `util/MoneyFormatter.java:8`, gera `R$ 599,76`,
  conferido com `java`), não "R$ 599,76" como o texto pede.
- Erros: `Reservation.RatePlan.valueOf(req.ratePlan)` sem proteção (`ReservationController.java:44`) vira 500 para
  um plano inválido; a mensagem interna do `SQLException` vai para o cliente; os ouvintes embrulham `SQLException`
  em `RuntimeException` sem tratamento acima (`FinanceNotificationListener.java:24-26`).
- Nomes fully-qualified no meio do código (`java.math.BigDecimal` em `ReservationService.java:56,109,113` e
  `ReportController.java:99-102`) em vez de `import`.

## Testes

**Números:** 6 testes rodaram, 0 falharam, 0 pulados; 6 métodos de teste, num arquivo; cobertura de 4,3% das linhas
(39 de 917) e 7,9% dos ramos (14 de 178) (`measures/tests.md`). `BUILD SUCCESS` no `mvn verify`.
**Nota:** ruim

O que existe é bom no que confere: `PricingServiceTest` testa com valores o exemplo do texto (599,76 em alta
temporada, `service/PricingServiceTest.java:29-40`), o desconto com arredondamento meio para o par (74,97 e 424,83,
`:42-54`), o combinado dos dois (`:56-68`) e a fronteira de 15 de dezembro dos dois lados (`:70-96`). Trocar 0.15 por
outro valor ou `>= 15` por `> 15` derruba um teste.

Mas é só isso. Nada testa: a parte nova do cenário (nenhum teste instancia um ouvinte, nem `NotificationService`;
`notification` tem 0 de 79 linhas cobertas), as transições de situação e as recusas (409, 422, 404), o cancelamento
e a devolução, os relatórios, o HTTP, o banco. Pela pergunta da mutação: apagar o `GovernanceNotificationListener` da
lista, trocar o assunto "Pagamento recebido" por outro, ou devolver o pago numa NON_REFUNDABLE não derruba teste
nenhum. E a ausência de qualquer teste pela API é exatamente o que deixou passar o defeito que quebra tudo (o
Javalin sem mapper de JSON): um único teste com `JavalinTest` ou `HttpClient` teria falhado. A estadia que cruza o
dia 15 de dezembro (o caso em que a regra de temporada está errada) também não tem teste. Com 4% de cobertura e a
parte nova sem rede, a coluna está quase ausente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`
(6 colunas, ok); datas em `date`; 1 chave estrangeira (`reservations.room_number -> rooms`) **sem índice**; 0 CHECK,
0 UNIQUE/EXCLUDE; 0 arquivos de migração; 0 pool; 0 transações; 0 `FOR UPDATE`; nenhum SQL montado com `+`
(`measures/database.md`).
**Nota:** com problemas

O serviço sobe contra um banco vazio e cria o esquema (`config/DatabaseConfig.java:34-90`), com os tipos certos
(`numeric(10,2)` para todo dinheiro, `date` para as datas, `bigserial` numerando a partir de 1), `NOT NULL` no
obrigatório, a FK para `rooms` e os cinco quartos semeados com `ON CONFLICT DO NOTHING`
(`config/DatabaseConfig.java:73-81`). Todo SQL usa `?` e toda conexão fecha em try-with-resources
(`repository/ReservationRepository.java:23-24,60-61`).

Os defeitos:

- Esquema por `CREATE TABLE IF NOT EXISTS` no código, sem migração: o próximo pedido que acrescentar uma coluna não
  tem como chegar a um banco que já existe.
- Nada protege a regra "um quarto não tem duas reservas na mesma noite": a conferência `isRoomAvailable`
  (`repository/ReservationRepository.java:90-112`) e o `INSERT` (`:15-51`) rodam em conexões diferentes, sem
  transação, sem trava e sem `EXCLUDE`; duas requisições simultâneas passam as duas (nem um `synchronized` há). O
  roteiro não conseguiu provar isso porque o POST já quebra antes (ver Comportamento), mas a leitura é clara.
- A confirmação faz `UPDATE` e depois três `INSERT` em `messages`, cada um na sua conexão, sem transação
  (`service/ReservationService.java:91-92`, `repository/MessageRepository.java:19-26`): se um ouvinte falhar, a
  reserva fica confirmada com parte dos avisos.
- Sem pool: `DriverManager.getConnection` a cada chamada (`config/DatabaseConfig.java:30-32`); o relatório de
  ocupação abre uma conexão por quarto (`controller/ReportController.java:73-74`).
- FK sem índice; `status`, `rate_plan`, `type` e `channel` em `varchar` sem CHECK; `discount` aceita nulo
  (`config/DatabaseConfig.java:57`) embora seja sempre calculado.

## Comportamento

**Números:** 6 de 66 conferências passaram (`measures/behavior.md`): parte comum 3 de 46; dados depois de reiniciar
1 de 3; cenário 03 1 de 15; duas reservas ao mesmo tempo 1 de 2.
**Nota:** ruim

O serviço sobe, mas toda rota com JSON responde 500. A causa está no `pom.xml:34-38` (Gson em vez de
`jackson-databind`) e em `Application.java:19` (`Javalin.create()` sem `jsonMapper`): o jar não contém
`ObjectMapper` e o Javalin avisa "It looks like you don't have an object mapper configured"
(`measures/behavior-service.log:21-33`) a cada chamada. `GET /rooms` dá 500; `POST /reservations` dá 500 antes de
tocar o serviço (`bodyAsClass` já falha, e os bancos `review_behavior_1/2` ficaram com 0 reservas); por consequência
todas as conferências de preço, situações, recusas, confirmação, e-mail, check-in, check-out, cancelamento,
devolução e persistência da reserva falharam, e o relatório financeiro e o de ocupação, os únicos que respondem
(CSV), vêm vazios. Para o dono da pousada: o site não consegue nem listar os quartos nem criar uma reserva.

Os seis "OK" não provam nada: os dois cabeçalhos CSV e o `text/csv` saem de um relatório vazio; "as mensagens
continuam lá" e "cancelar pendente não manda nada" passaram com 500 dos dois lados; "nenhuma rodada com as duas
reservas criadas" passou porque nenhuma rodada criou nada (10 rodadas de `500 500`).

Com o mapper corrigido, pela leitura do código ainda falhariam: o aviso "Reserva cancelada - quarto N" à governança,
que nunca sai (`GovernanceNotificationListener.java:39-43` testa a situação da reserva já cancelada); o valor nos
e-mails com espaço não quebrável (`util/MoneyFormatter.java:8`), que não bate com "R$ 779,80"; o `refund` 0.00 que
volta como `null` (`service/ReservationService.java:132`); corpo que não é JSON, que o `catch (JsonSyntaxException)`
do Gson (`ReservationController.java:64`) não pegaria do Jackson; e uma estadia que cruza 15/12 cobrada inteira com
acréscimo (`service/PricingService.java:36-54`). Os dois últimos pontos pesam no design; cito aqui porque aparecem
em chamadas que o texto descreve.

## Dúvidas

- A classe do pattern: a lista de ouvintes é preenchida dentro do próprio assunto (`NotificationService.initialize`,
  `notification/NotificationService.java:22-26`), e não na montagem, como a referência pede. Dei `acerto` porque a
  interface, os três ouvintes separados e o serviço que só anuncia estão todos lá, e `addListener` existe; um revisor
  mais rigoroso poderia ler o registro embutido como `malfeito`.
- O comportamento foi medido com tudo em 500, então os defeitos que listei "com o mapper corrigido" vêm da leitura
  do código e de uma conferência local do `NumberFormat`, não de chamadas à API.
