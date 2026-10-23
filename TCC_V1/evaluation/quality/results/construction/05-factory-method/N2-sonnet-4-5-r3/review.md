# Revisão: construção, cenário 05

## Visão geral

O agente montou do zero um serviço Javalin em 7 pacotes: `api` (os controllers e o `ErrorResponse`), `service`
(reservas, quartos, e-mail, relatórios), `repository` (JDBC puro com `PreparedStatement`), `database` (conexão por
`DriverManager` e um `CREATE TABLE IF NOT EXISTS` ao subir), `domain` (as entidades e os enums, com o preço calculado
em `Reservation`) e `document` (a interface `DocumentGenerator`, três geradores e a `DocumentGeneratorFactory`). Tudo é
montado em `App.java:29-72`. Uma requisição passa assim: `POST /reservations` → `ReservationController.createReservation`
(lê o JSON e valida) → `ReservationService.createReservation` (confere conflito e grava) → `ReservationRepository`
→ PostgreSQL (3 tabelas). A parte do cenário ficou bem resolvida: uma fábrica escolhe o documento pelo tipo de hóspede,
e os três lugares que emitem o documento pedem a ela. O roteiro do cenário passou inteiro (32 de 32). O defeito mais
sério está no banco. A conferência de conflito e a gravação acontecem sem transação, sem trava e sem restrição, e nas
10 rodadas de reservas simultâneas o mesmo quarto foi reservado duas vezes para a mesma noite em todas as 10. Há também
uma dependência da localidade (o locale) da JVM: com o locale pt_BR, o comprovante e o CSV saem errados. E os testes
cobrem só o domínio e os documentos (32% das linhas).

## Arquitetura

**Números:** 7 pacotes, 14 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). A maior classe é
`domain/Reservation.java`, com 283 linhas, e logo depois vem `api/ReservationController.java`, com 275. Pelo jdeps, JDBC
aparece em `api` e `service`, além de `repository` e `database`.
**Nota:** com problemas

As dependências vão no sentido certo: `api` → `service` → `repository` → `database`, com `domain` e `document` como
folhas. A montagem fica num lugar só (`App.java:29-52`), e cada classe recebe o que precisa pelo construtor. O SQL está
todo em `repository/` e em `database/DatabaseMigration.java`. O domínio e os geradores de documento não conhecem nem o
Javalin nem o JDBC.

Os defeitos estão concentrados na borda HTTP:
- `api/ReservationController.java:25-118` é um handler de quase 100 linhas com regra de negócio dentro. A regra
  "exatamente um jeito de identificar o hóspede" fica toda no controller (`:68-95`), e o serviço não a conhece. A
  regra de capacidade está duplicada: o controller confere em `api/ReservationController.java:103` e o serviço confere
  de novo em `service/ReservationService.java:32`. A validação de datas e de hóspedes (`:50-59`) também mora no handler.
- O controller busca o quarto e calcula preço chamando o domínio (`api/ReservationController.java:245-247`), e repete
  a busca do quarto em cada handler (`:130`, `:145`, `:164`, `:199`).
- O `SQLException` atravessa todas as camadas até os handlers (`throws SQLException` em todos os métodos de `api/` e
  `service/`). É por isso que o jdeps mostra JDBC em `api` e `service`. O vazamento é pequeno, mas amarra a borda ao
  tipo de exceção do JDBC.
- O `MessageController` fala direto com o `MessageRepository` (`App.java:51`), sem passar por um serviço. Para uma
  leitura simples isso é aceitável.

São misturas localizadas, sem ciclo. Por isso a nota fica em "com problemas".

## Design de baixo nível

**Números:** 3 casos de documento (comprovante, fatura, booking receipt), os 3 na estrutura e nenhum fora dela. Há uma
única decisão sobre o tipo de hóspede (`document/DocumentGeneratorFactory.java:10-20`), e os 3 lugares que emitem o
documento usam a fábrica: o e-mail (`service/EmailService.java:19`), o check-out (`service/ReservationService.java:96`)
e a segunda via (`service/ReservationService.java:122`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É uma fábrica concentrada, que a referência aceita como acerto. A `DocumentGeneratorFactory` decide pelo
tipo de hóspede (`hasIndividualGuest`/`hasCorporateGuest`/`hasInternationalGuest`, em
`domain/Reservation.java:144-154`) e devolve um `DocumentGenerator` (`document/DocumentGenerator.java:7`), com uma
implementação por documento. Nenhum dos três lugares cria o documento à mão ou volta a perguntar qual é o caso.
Para atender a agência de turismo, basta um gerador novo e um `if` a mais, num lugar só. Três detalhes afastam um
pouco o resultado do ideal, sem mudar a classe:
- o tipo de hóspede não é um tipo próprio, só campos anuláveis soltos na reserva. Por isso a fábrica decide pela ordem
  dos `if`, e um quarto tipo vai exigir mais campos anuláveis em `Reservation`, `Document`, no controller e na tabela;
- o "exatamente um" é conferido só no controller (`api/ReservationController.java:68-95`), fora do domínio;
- os três geradores repetem o mesmo `generate` campo a campo (`document/IndividualDocumentGenerator.java:14-40`,
  `document/CorporateDocumentGenerator.java:16-42`, `document/InternationalDocumentGenerator.java:14-40`). O
  `formatMoney` e o `formatNights` também estão copiados (`IndividualDocumentGenerator.java:63-69` é igual a
  `CorporateDocumentGenerator.java:69-75`).

**O exagero.** Não há estrutura sem necessidade: a interface `DocumentGenerator` tem três implementações reais, e a
fábrica é o próprio pattern. O único candidato é o `RoomService` (`service/RoomService.java:17-23`), que só repassa
para o `RoomRepository`. É pequeno e dá uma dependência uniforme aos outros serviços (ver Dúvidas).

**O resto do código:**
- **Formatação que depende da localidade da JVM.** `String.format("R$ %,.2f", ...)` seguido de troca de caracteres
  (`document/IndividualDocumentGenerator.java:64`, `document/CorporateDocumentGenerator.java:70`), `"BRL %,.2f"`
  (`document/InternationalDocumentGenerator.java:65`) e `"%.2f"` no CSV (`service/ReportService.java:73` e `:78`)
  não fixam o `Locale`. Subi o jar com `-Duser.language=pt -Duser.country=BR` e conferi: o comprovante saiu
  "R$ 2,783.52" e o CSV financeiro saiu `...,CONFIRMED,2783,52` e `total,2783,52`, com uma coluna a mais. No servidor
  da revisão (locale padrão) tudo passa, mas o resultado muda conforme a máquina onde o sistema roda.
- **Exceções genéricas usadas para decidir o código HTTP.** O serviço lança `IllegalArgumentException` e
  `IllegalStateException`, e o controller escolhe o código de erro comparando a mensagem
  (`api/ReservationController.java:112`, `e.getMessage().equals("Room unavailable")`). O mesmo `IllegalArgumentException`
  vira 404 RESERVATION_NOT_FOUND nos outros handlers (`:152`, `:171`, `:187`, `:206`, `:222`). Os cinco handlers repetem
  o mesmo bloco `try/catch`, e não há um `app.exception(...)` central. Por isso, um corpo que não é JSON dá 500
  (`:27`).
- **O preço é recalculado a cada leitura, com a tabela atual do enum.** `getSubtotal(Room)` e `getTotal(Room)`
  (`domain/Reservation.java:35-62`) dependem de `RoomType` (`domain/RoomType.java`). A reserva não guarda subtotal,
  desconto nem total, então se a diária mudar, as reservas antigas e os comprovantes de segunda via mudam de valor. O
  desconto é calculado duas vezes no mapeamento da resposta (`api/ReservationController.java:245-247`).
- **Estado sem controle.** `Reservation` tem as regras de transição (`domain/Reservation.java:83-131`), mas também
  `setStatus`, `setAmountPaid` e `setRefund` públicos (`:264`, `:272`, `:280`), que deixam qualquer um pular as regras.
- **Dinheiro sem escala fixa.** `amountPaid` nasce como `BigDecimal.ZERO` (`domain/Reservation.java:28`), e o JSON da
  reserva nova sai `"amountPaid":0` em vez de `0.00`. O mesmo vale para o `refund` de uma pendente cancelada
  (`:128`).

Os tipos de base estão certos (`BigDecimal` com `HALF_EVEN`, `LocalDate`, enums para situação, plano e tipo de
quarto). Os defeitos acima são localizados, então a nota é "com problemas".

## Testes

**Números:** 14 testes rodaram, 0 falhas, 0 erros, 0 pulados; 14 métodos escritos em 2 arquivos. Cobertura de linhas:
300 de 925 (32,4%); de ramos: 42 de 188 (22,3%). `document`: 134 de 136 linhas; `domain`: 166 de 216; `api`,
`service`, `repository`, `database` e `App`: 0%. Não há nenhum teste pela API nem contra o banco (`measures/tests.md`).
**Nota:** com problemas

O que está testado, com valores:
- o preço na alta temporada de julho, 599.76 (`src/test/java/com/pousadamarealta/domain/ReservationTest.java:22`), e
  em 15/12 (`:66`);
- a baixa temporada, 779.80 (`:35`);
- o NON_REFUNDABLE com desconto de 116.97 e total de 662.83 (`:51-53`);
- as transições de situação (`:70-86`);
- a devolução no cancelamento STANDARD e NON_REFUNDABLE (`:89-108`);
- nos documentos, o título, os campos nulos, "R$ 599,76", "BRL 779.80", a data 2027-03-10, "1 diária" e "1 night"
  (`src/test/java/com/pousadamarealta/document/DocumentGeneratorTest.java:28-38`, `:83-94`, `:131-132`, `:151-159`);
- a fábrica devolvendo o gerador certo por tipo de hóspede (`:98-114`).

O que fica sem rede (a pergunta da mutação):
- **O ISS retido.** O teste da fatura só confere o rótulo "ISS retido (5%)" (`DocumentGeneratorTest.java:64`), não o
  valor. Trocar `0.05` por `0.06` em `document/CorporateDocumentGenerator.java:13` não derruba nenhum teste.
- **A regra "exatamente um jeito de identificar o hóspede"** (`api/ReservationController.java:68-95`) não tem nenhum
  teste. Se alguém apagar o `if (identificationFields != 1)`, todos os testes continuam passando.
- **O separador de milhar** (R$ 1.234,56 e BRL 1,234.56) não é testado: nenhum valor passa de 1.000. Junto com a
  dependência da localidade, é justamente onde o código é frágil.
- O texto do e-mail, a segunda via só para reserva paga, os códigos de erro, o conflito de datas, a ocupação, o
  relatório financeiro e a persistência não têm teste nenhum (0% em `api`, `service` e `repository`).

Os testes que existem são bons e conferem valores. Mas as regras da parte nova ficam pela metade, e as camadas de API
e de banco não são exercitadas.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)`, e as datas em `date`. As conferências acusaram:
- uma chave estrangeira sem índice (`reservations.room_number` → `rooms`);
- as 4 colunas de situação e de tipo sem CHECK;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- 0 pool de conexões e 1 `DriverManager.getConnection`;
- 0 transações explícitas e 0 `FOR UPDATE`;
- nenhum SQL montado com `+` (`measures/database.md`).

**Nota:** ruim

O que está certo: o SQL usa só parâmetros `?`, as conexões são fechadas com try-with-resources, o dinheiro tem
2 casas, a reserva está ligada ao quarto por chave estrangeira (`database/DatabaseMigration.java:28`) e os dados
sobrevivem ao reinício (3 de 3 no roteiro).

O defeito sério: **nada impede duas reservas do mesmo quarto na mesma noite.** O serviço confere o conflito com um
`SELECT COUNT(*)` (`repository/ReservationRepository.java:118-150`) e depois grava num `INSERT` separado
(`service/ReservationService.java:36-44`). As duas operações rodam em conexões diferentes, sem transação, sem trava e
sem restrição de exclusão no banco. Nem um `synchronized` dentro do processo protege a sequência. O roteiro mostrou o
efeito: em 10 rodadas de dois pedidos simultâneos, as 10 criaram as duas reservas (`measures/behavior.md`). Para a
pousada, isso significa dado errado gravado: dois hóspedes confirmados no mesmo quarto, na mesma noite.

Os outros defeitos:
- **O esquema não evolui.** Ele é criado por `CREATE TABLE IF NOT EXISTS` no código
  (`database/DatabaseMigration.java:19`, `:26`, `:47`), sem migrações versionadas. A próxima coluna (o documento da
  agência, por exemplo) não chega a um banco que já existe.
- **Uma conexão nova por consulta.** `DriverManager.getConnection` está em `database/Database.java:19`, sem pool, e
  `confirmReservation` abre várias conexões seguidas.
- **A confirmação não é atômica.** Ela grava a reserva e depois a mensagem em duas conexões sem transação
  (`service/ReservationService.java:63-65`). Uma falha no meio deixa a reserva confirmada sem o e-mail.
- **Colunas de situação, plano e tipo sem CHECK**, e a chave estrangeira sem índice.
- **Relatórios com consultas pesadas.** O financeiro filtra com `EXTRACT(YEAR/MONTH FROM check_in)`
  (`repository/ReservationRepository.java:189-190`), o que impede o uso de índice. A ocupação faz uma consulta por
  quarto (`service/ReportService.java:32`). Com 5 quartos, isso pesa pouco.

## Comportamento

**Números:** 77 de 83 conferências passaram (`measures/behavior.md`):
- a parte comum do enunciado: 41 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- duas reservas ao mesmo tempo: 1 de 2. A que falhou mostra as duas reservas criadas em 10 de 10 rodadas; a que passou
  não acusou nenhuma rodada em que ambas foram recusadas.

**Nota:** com problemas

O cenário funciona por inteiro. As três identificações são aceitas, e nenhuma, duas ou uma incompleta dão 400. A
fatura traz a empresa e o ISS de R$ 38,99. O booking receipt sai em inglês, com BRL 2,783.52, as datas ISO e
"1 night". O documento é o mesmo no e-mail em português, no check-out e na segunda via, e o JSON traz o campo `text`.

As falhas:
- **Reservas simultâneas: o mesmo quarto foi reservado duas vezes em 10 de 10 rodadas.** É a falha que mais importa
  para o dono da pousada: se dois hóspedes reservarem o mesmo quarto quase ao mesmo tempo pelo site, os dois ficam com
  a reserva. A causa está no banco e pesa naquela coluna.
- **Um corpo que não é JSON dá 500 em vez de 400 INVALID_REQUEST.** `ctx.bodyAsClass` lança uma exceção
  (`api/ReservationController.java:27`) que nenhum handler trata. Conferi com `curl`: a resposta é `500 Server Error`
  em texto puro.
- **A segunda via de uma reserva pendente dá 404 RESERVATION_NOT_FOUND, e o esperado era 409 OPERATION_NOT_ALLOWED**
  (`api/ReservationController.java:224-225`). A reserva existe; o que a situação dela não permite é a operação. O
  texto define 409 OPERATION_NOT_ALLOWED para "uma operação que a situação da reserva não permite", então isso é erro,
  não uma escolha em aberto.
- **`amountPaid` sai como `0` e não `0.00`** na reserva recém-criada (`domain/Reservation.java:28`). O texto pede
  "número com 2 casas". O defeito é pequeno: o valor está certo, só a escala não.
- **"O 201 comporta 3" e "a diária do 201 é 389.90" falharam** porque `GET /rooms` devolve `"number":"201"` como texto,
  e o roteiro procura o número 201. Conferi com `curl`: a capacidade (3) e a diária (389.90) estão certas. O texto não
  diz o tipo de `number`, então essa é uma escolha razoável e não pesa.

Fora do roteiro (pesa no design, não aqui): com a JVM em pt_BR, o comprovante sai "R$ 2,783.52" e o CSV financeiro
sai com vírgula decimal (`2783,52`), o que quebra as colunas.

## Dúvidas

- **O exagero:** o `RoomService` (`service/RoomService.java`) só repassa para o `RoomRepository`, e a skill cita "uma
  camada que só repassa" como exagero. Dei `contenção` porque é uma classe de dois métodos que dá uma dependência
  uniforme aos outros serviços, mas um revisor mais estrito poderia marcar `exagero`.
- **A nota do banco:** dei `ruim` porque não há nenhuma proteção contra a reserva dupla (nem transação, nem trava, nem
  restrição, nem `synchronized`) e o roteiro a reproduziu em 10 de 10 rodadas: é dado errado gravado. Os outros pontos
  do banco (tipos, chaves, parâmetros, persistência) estão certos, e quem pesar só pela lista de defeitos da skill
  poderia dar `com problemas`.
- **A forma do pattern:** é uma fábrica concentrada (uma classe com `if` sobre o tipo de hóspede que devolve um gerador
  polimórfico), não a forma clássica com um criador sobrescrito. A referência aceita as duas como acerto.
