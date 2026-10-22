# Revisão: construção, cenário 07

## Visão geral

O agente montou um serviço Javalin em 9 pacotes. O `App.main` liga todas as peças (`App.java:25-75`). O
`controller.ApiController` lê a requisição e chama `service.ReservationService` e `service.ReportService`. As regras de
preço e de situação ficam no domínio (`domain.PriceCalculator`, `domain.Reservation`). O SQL fica nos `repository.Jdbc*`,
e o esquema é criado com `CREATE TABLE IF NOT EXISTS` em `infrastructure.Database`. Os dados ficam no PostgreSQL, em
três tabelas (`rooms`, `reservations`, `messages`). A parte do cenário (bloqueio, cópia para o arquivo, modo de teste)
virou uma lista de regras (`MessageRule`) que o `MessageProcessor` aplica. A combinação de regras de cada modo é
decidida no `main` (`App.java:77-89`). O cenário passou inteiro pela API (14 de 14).

O problema mais sério é a reserva dupla. Nada impede que duas reservas fiquem com o mesmo quarto na mesma noite: o
serviço consulta e depois insere, sem trava e sem restrição no banco. Nas reservas simultâneas, **10 de 10 rodadas
criaram as duas reservas**. Há também outros defeitos:

- respostas 500 para corpo que não é JSON e para campo que falta;
- a segunda via sai para reserva cancelada;
- os testes de integração dependem de Docker, ignoram o `DB_URL` e não rodaram aqui.

## Arquitetura

**Números:** 9 pacotes, 21 dependências entre eles, 1 ciclo (`messaging` ↔ `repository`). Só o pacote raiz e o
`controller` usam Javalin. Só o `repository` e o `infrastructure` usam JDBC.
**Nota:** com problemas

O que está bem:

- **A montagem fica num lugar só.** É `App.java:25-75`, e nenhuma classe cria as próprias dependências.
- **As regras não conhecem Javalin nem SQL.** É o caso de `domain/Reservation.java:60-94` (transições e devolução),
  `domain/PriceCalculator.java` e `service/ReservationService.java`.
- **O SQL está todo isolado nos repositórios**, sempre com parâmetros.

Os defeitos:

- **Ciclo entre `messaging` e `repository`.** O `MessageService` usa o `MessageRepository`
  (`messaging/MessageService.java:3`), e a interface do repositório usa tipos de `messaging`: `OutgoingMessage`,
  `SentMessage` e `MessageStatus` (`repository/MessageRepository.java:3-5`). A interface do repositório está no pacote
  errado, ou os tipos da mensagem estão.
- **Uma regra de negócio no handler.** Quem decide se existe segunda via é o controller, e decide por
  `amountPaid == 0` (`controller/ApiController.java:159`), não pela situação da reserva. Por isso a reserva cancelada
  com devolução recebe segunda via (ver Comportamento).
- **O controller pula o serviço e fala direto com o repositório.** Isso acontece em `ApiController.java:34`, `:142` e
  `:165`. O check-out e a segunda via montam o comprovante na borda HTTP.
- **As exceções de negócio ficam aninhadas no serviço** (`service/ReservationService.java:132-151`). Cada handler
  repete a tradução para HTTP (`ApiController.java:67-82`, `:90-107`, …), e não existe um tratador de exceções comum.

Nenhuma classe é desproporcional. A maior é `JdbcReservationRepository` (291 linhas), e o tamanho vem de repetição
(ver Design).

## Design de baixo nível

**Números:** as 3 regras do cenário estão na estrutura, uma classe por regra, e nenhuma fica fora dela. A combinação de
cada modo fica num só lugar (`App.java:77-89`). Nenhuma regra pergunta pelo modo de teste. Há 3 interfaces de
repositório com uma implementação cada, e nada as substitui (nem os testes).
**Classe:** alternativa equivalente
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** O texto pede um Decorator, e o agente fez uma lista de filtros. A referência aceita isso de antemão:

- cada regra é uma peça própria que implementa `MessageRule` (`messaging/rules/EmailValidationRule.java`,
  `ArchiveCopyRule.java` e `TestModeRule.java`);
- o `MessageProcessor` aplica as regras em sequência e para no bloqueio (`messaging/MessageProcessor.java:17-28`);
- a combinação por modo é decidida na montagem (`App.java:80-86`: validação sempre, depois teste **ou** cópia);
- nenhuma regra conhece o modo; o `TestModeRule` só reescreve a mensagem (`TestModeRule.java:13-23`);
- o `MessageService` só grava o resultado: a original, depois as cópias (`MessageService.java:20-28`).

Uma regra nova (madrugada, limite por dia) entra como outra classe na lista, sem mexer nas outras. Não é um Decorator
de fato: a cópia sai como "mensagem adicional" (`ProcessingResult.java:25-27`) que as regras seguintes não veem, e não
como um envio embrulhado. A ordem da lista passa a ter significado implícito (pôr a cópia antes do teste mudaria o
resultado). Por isso a classe é "alternativa equivalente", e não "acerto".

**O exagero.** São três itens:

- **Interfaces sem substituto.** `RoomRepository`, `ReservationRepository` e `MessageRepository` têm uma implementação
  JDBC cada, e nada as troca: os testes usam as `Jdbc*` com Testcontainers (`IntegrationTest.java:53-55`).
- **Dois tipos de resultado quase iguais.** `ProcessingResult` (`messaging/ProcessingResult.java`) e o record
  `MessageProcessor.ProcessedMessages` (`MessageProcessor.java:31-44`) carregam a mesma informação.
- **Uma classe escrita à mão onde caberia um record:** `SentMessage` (`messaging/SentMessage.java`).

**O resto do código.** O domínio está bem tipado: `BigDecimal` com `HALF_EVEN` a cada passo
(`PriceCalculator.java:19,25,30,36`), `LocalDate`, enums para situação, plano e tipo. As transições ficam protegidas
dentro de `Reservation` (`Reservation.java:60-94`). Os defeitos:

- **Duplicação.** O mapeamento da linha para `Reservation` está copiado quatro vezes
  (`repository/JdbcReservationRepository.java:120-137`, `:169-186`, `:217-234`, `:264-281`). A tradução de exceção para
  HTTP se repete em sete handlers.
- **`null` sem cuidado.** `parseDate(null)` e `RatePlan.valueOf(null)` lançam `NullPointerException`
  (`ApiController.java:221-226`, `:239`), que vira 500. `Long.parseLong` do id também não tem tratamento
  (`ApiController.java:87`, …). O `int guests` do request (`controller/dto/CreateReservationRequest.java:10`)
  transforma a falta do campo em 0.
- **Escala do dinheiro.** O desconto do plano STANDARD é `BigDecimal.ZERO`, sem escala 2
  (`PriceCalculator.java:32`), e por isso o JSON da criação traz `"discount":0` e `"amountPaid":0`
  (`Reservation.java:57`). O total vazio do financeiro sai como `total,0` (`service/ReportService.java:53,82`).
- **Consultas repetidas.** O relatório de ocupação busca o quarto uma vez por linha (`ReportService.java:32`).
- **Escritas soltas.** `confirmReservation` grava a reserva e depois as mensagens em escritas separadas, sem transação
  (`ReservationService.java:65-66`).

## Testes

**Números:** 16 testes rodaram, com 0 falhas e 1 erro. O erro é a classe `IntegrationTest` inteira, que precisa de
Docker. Há 29 métodos de teste escritos; os 14 de integração não chegaram a rodar. A cobertura é de 15,0% das linhas
(127 de 848) e 24,0% dos ramos (36 de 150). `controller`, `service`, `repository` e `infrastructure` ficaram em 0%.
**Nota:** com problemas

**Os testes de unidade conferem valores.**

- **Preço:** temporada (599.76) e desconto NON_REFUNDABLE (74.97 e 424.83), em `PriceCalculatorTest.java:23-55`.
- **Situações e devolução:** confirmar, confirmar de novo, cancelar STANDARD e NON_REFUNDABLE, ocupação na noite de
  saída, em `ReservationTest.java:15-141`.
- **As três regras do cenário, uma a uma** (`MessageRulesTest.java:14-77`): o assunto "[TESTE] ", o destinatário de
  testes, a linha "Destinatário original", a cópia com "[Cópia] " e o mesmo texto.

**Os defeitos.**

- **A integração depende de Docker.** `IntegrationTest` sobe um PostgreSQL com Testcontainers
  (`IntegrationTest.java:36-40`) e ignora o `DB_URL` que o texto garante. Sem Docker, como aqui, ela não roda. Um
  `mvn verify` comum falharia: o script só termina em BUILD SUCCESS porque passa `-Dmaven.test.failure.ignore=true`.
- **A combinação do cenário só é conferida nessa integração que não rodou, e de leve.** O teste do bloqueio
  (`IntegrationTest.java:285-306`) só confere que a última mensagem está BLOCKED com o endereço `carlos.gmail.com`,
  que também não tem @. A falta da cópia só se vê de modo indireto. Mesmo lá, as regras são montadas à mão (`IntegrationTest.java:57-59`), copiando o
  `App`. A montagem real dos modos (`App.java:77-89`) e o modo de teste completo não têm teste.
- **Regras sem rede, pela pergunta da mutação:**
  - quem apagasse a exigência de ponto no domínio (`EmailValidationRule.java:31-39`) não seria pego por nenhum teste de
    unidade: o único endereço inválido testado é `user.example.com`, que já cai pela contagem de @
    (`MessageRulesTest.java:33`);
  - o mesmo vale para quem trocasse `atIndex == 0` (`EmailValidationRule.java:27`);
  - quem invertesse a ordem da lista em `App.java:80-86` também não seria pego.
- **Erros e relatórios pela API:** só pela integração que não rodou. Faltam testes para 400 e 404, para a
  concorrência e para o relatório financeiro.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` (5 colunas, todas certas) e as
datas em `date`. As conferências acusaram uma chave estrangeira sem índice (`reservations.room_number -> rooms`).
Nenhum CHECK nas colunas de situação, plano, tipo e canal. Nenhuma restrição UNIQUE ou EXCLUDE. 0 transações
explícitas, 0 `FOR UPDATE`. Nenhum SQL montado com `+`.
**Nota:** ruim

**O defeito sério: nada protege a regra de um quarto por noite.**

- `ReservationService.createReservation` consulta os conflitos (`service/ReservationService.java:40`) e insere depois
  (`:48`), sem transação e sem trava. O método nem é `synchronized`.
- O banco também não tem restrição que impeça a sobreposição (`infrastructure/Database.java:43-63`; uma `EXCLUDE` com
  `daterange` resolveria).
- O roteiro do comportamento confirmou o efeito: **10 de 10** pares simultâneos criaram as duas reservas.
- Para a pousada, isso é dado errado: o mesmo quarto vendido duas vezes para a mesma noite.

**Os outros defeitos são menores.**

- **O esquema não evolui.** Ele nasce de `CREATE TABLE IF NOT EXISTS` no código (`Database.java:36-74`), sem
  migrações. O próximo pedido que acrescente uma coluna não terá como alterar um banco que já roda.
- **Faltam índice e restrições.** A chave estrangeira não tem índice (`Database.java:61`). Situação, plano e tipo são
  `VARCHAR` sem CHECK (`Database.java:39,53,58`). Também não há CHECK para `guests >= 1` nem para
  `check_out > check_in`.
- **O que está certo:** os quartos são semeados só quando a tabela está vazia (`Database.java:83-98`). O SQL usa `?`.
  As conexões vêm de um pool Hikari (`Database.java:15-20`) e são devolvidas por try-with-resources.
- **Consulta do financeiro.** Ela filtra o mês com `EXTRACT(...)` (`repository/JdbcReservationRepository.java:249-250`),
  o que impede usar um índice em `check_in`. Isso pesa pouco neste tamanho.

## Comportamento

**Números:** 58 de 65 conferências passaram (7 falharam):

| parte | passaram |
|---|---|
| parte comum | 40 de 46 |
| dados depois de reiniciar | 3 de 3 |
| cenário, modo normal | 9 de 9 |
| cenário, modo de teste | 5 de 5 |
| duas reservas ao mesmo tempo | 1 de 2 |

**Nota:** com problemas

**O cenário está todo certo.** Endereços inválidos ("ana@gmail", "ana.gmail.com", "ana@@gmail.com", "@gmail.com") ficam
BLOCKED, com o endereço original e sem cópia. A cópia para o arquivo vem logo depois da original, com "[Cópia] " no
assunto. No modo de teste a mensagem vai para testes@ com "[TESTE] " e "Destinatário original", sem cópia, e o
inválido continua bloqueado. Os dados sobrevivem ao reinício.

As falhas:

- **Reservas simultâneas, a que pesa.** Em 10 de 10 rodadas, dois pedidos ao mesmo tempo criaram duas reservas do
  mesmo quarto para a mesma noite. O dono pode vender o 301 duas vezes. O defeito está contado na coluna do banco.
- **Corpo que não é JSON e campo que falta devolvem 500 em vez de 400 INVALID_REQUEST.** Conferi também à mão: falta
  de `guestName` e id não numérico (`/reservations/abc`) também dão 500. O site recebe "Server Error" em vez de saber
  o que corrigir.
- **Segunda via de reserva pendente devolve 400 INVALID_REQUEST** em vez de 409 OPERATION_NOT_ALLOWED
  (`ApiController.java:159-163`). Fora do roteiro, uma reserva STANDARD confirmada e depois cancelada **recebe** a
  segunda via (200). O texto diz que ela só existe para reserva paga (confirmada, com check-in ou com check-out).
- **Dinheiro sem duas casas na criação:** `"discount":0` e `"amountPaid":0`, em vez de `0.00`. O texto pede número
  com 2 casas. O efeito é pequeno: depois de gravar, o valor volta com 2 casas.
- **"O 201 comporta 3" e "a diária do 201 é 389.90" vieram vazios.** Não é erro: os valores estão certos
  (`"capacity":3`, `"nightlyRate":389.90`). O agente devolve `number` como texto (`"201"`), e o roteiro procura o
  número 201. O texto não diz o tipo de `number`, então foi uma escolha razoável num ponto em aberto e não pesa.

## Dúvidas

- **Testes.** A integração (14 métodos) não rodou aqui por falta de Docker, então não sei se ela passa. A nota conta o
  que rodou e a dependência de um ambiente que o texto não dá (o texto dá um `DB_URL`).
- **Banco.** O `ruim` vem da falta total de proteção contra a reserva dupla, comprovada em 10 de 10 rodadas. A skill
  põe a proteção "só dentro do processo" em `com problemas`, e aqui nem isso existe. Mesmo assim, alguém poderia ler o
  caso como `com problemas`.
- **Design.** A diferença entre "alternativa equivalente" e "acerto" está na leitura de que a lista de filtros não é
  um embrulho do envio. A referência aceita a lista, e foi por ela que decidi.
