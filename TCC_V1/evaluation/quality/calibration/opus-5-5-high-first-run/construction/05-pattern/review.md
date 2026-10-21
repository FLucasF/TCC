# Revisão: construção, cenário 05

## Visão geral

O projeto tem 7 pacotes (`hotel`, `hotel.web`, `hotel.reservations`, `hotel.documents`, `hotel.messages`,
`hotel.reports`, `hotel.rooms`), com cerca de 1.350 linhas em `src/main`. Uma requisição entra pelas rotas do
Javalin em `hotel.web.HttpApi`. Elas transformam o JSON em `NewReservation` e chamam `ReservationService`, que aplica
as regras (situação, capacidade, preço via `PriceCalculator`) e grava pelos repositórios JDBC (`ReservationStore`,
`Rooms`, `SentMessages`). Os repositórios usam um pool Hikari, e o esquema é criado e evoluído por duas migrações do
Flyway. Toda a montagem acontece em `Hotel` (`Hotel.java:20-28`). O documento de cada tipo de hóspede sai de uma
fábrica concentrada (`DocumentFactory`), e os três lugares que entregam o documento pedem a ela. O comportamento
passou inteiro (83 de 83), inclusive as reservas simultâneas, que estão protegidas por uma restrição `EXCLUDE` no
banco. Não há nada sério. Os defeitos são menores:
- um ciclo entre `hotel.documents` e `hotel.reservations`;
- a regra "um só jeito de identificar o hóspede" mora no handler HTTP;
- a confirmação e o e-mail são gravados sem transação;
- há texto duplicado entre os escritores de documento;
- os testes da parte nova são rasos.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), conforme
`measures/dependencies.md`. Só `hotel` (a montagem) e `hotel.web` usam Javalin. Usam JDBC `hotel` (o pool e o
Flyway), `hotel.reservations`, `hotel.rooms` e `hotel.messages`. A maior classe é `HttpApi`, com 184 linhas.
**Nota:** com problemas

- **A borda é fina na maior parte.** As rotas de `web/HttpApi.java:62-90` só leem a requisição, chamam o serviço e
  escrevem a resposta. Os códigos de erro viram status HTTP num lugar só (`web/HttpApi.java:93-107`).
- **As regras não conhecem Javalin.** `reservations/ReservationService.java` não usa Javalin nem SQL; o SQL fica em
  `ReservationStore`, `Rooms` e `SentMessages`. A montagem está concentrada em `Hotel.java:20-28`, e a configuração
  do banco e das migrações fica em `Database.java:18-29`.
- **Há um ciclo entre dois pacotes vizinhos.**
  - Num sentido, `hotel.documents` depende de `hotel.reservations`: os escritores recebem `Reservation`
    (`documents/DocumentWriter.java:11`) e a fábrica recebe `Guest` (`documents/DocumentFactory.java:19`).
  - No outro, `hotel.reservations` depende de `hotel.documents`: `ReservationService` importa `DocumentFactory` e
    `ReservationReceipt` (`reservations/ReservationService.java:3-4`).
  - O ciclo é pequeno, mas mostra que o serviço de reservas e os documentos não têm uma direção clara.
- **Uma regra de negócio ficou na borda HTTP.** A exigência de exatamente um jeito de identificar o hóspede (CPF; ou
  CNPJ com razão social; ou passaporte com país) está no handler, em `web/HttpApi.java:126-134`, e não no domínio. O
  `Guest` aceita qualquer combinação (`reservations/Guest.java:7-14`). Quem criar uma reserva por outro caminho que
  não o HTTP (os testes, por exemplo) passa sem essa validação; só a restrição do banco segura.
- **Uma dependência é criada por dentro.** `ReservationService` faz `new DocumentFactory()` no próprio campo
  (`reservations/ReservationService.java:19`), em vez de receber a fábrica na montagem. É pequeno, mas foge do padrão
  de montagem do resto.

## Design de baixo nível

**Números:**
- Há 3 tipos de documento, um escritor para cada (`PersonReceiptWriter`, `CompanyInvoiceWriter`,
  `ForeignReceiptWriter`). Os 3 estão na estrutura e nenhum fica fora dela.
- A escolha de qual documento criar acontece num lugar só (`documents/DocumentFactory.java:19-27`).
- Os 3 lugares que entregam o documento pedem à fábrica: a confirmação com e-mail
  (`reservations/ReservationService.java:66`), o check-out (`:111`) e a segunda via (`:120`). Nenhum cria o documento
  à mão.

**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A forma usada é a fábrica concentrada:
- `DocumentFactory.issue` (`documents/DocumentFactory.java:14-17`) escolhe o `DocumentWriter` pelo tipo de hóspede
  (`:19-27`) e devolve o `ReservationReceipt` pronto, com o título e o texto do escritor certo.
- O serviço não pergunta qual é o tipo de hóspede em nenhum ponto.
- O e-mail de confirmação embute o `receipt.text()` sem saber qual documento é
  (`reservations/ReservationService.java:127-133`).
- Para a agência de turismo bastaria um escritor novo e um ramo novo em `writerFor`.

Duas coisas afastam o código da forma ideal, sem mudar a classe:
- A decisão em `writerFor` usa `isCompany()`/`isForeigner()` sobre um `Guest` com campos anuláveis
  (`reservations/Guest.java:20-26`). O tipo de hóspede não é um tipo próprio. O CPF é o caso "o que sobra"
  (`DocumentFactory.java:26`), o que vale enquanto a validação da borda garante que só um jeito vem preenchido.
- O mesmo `ReservationReceipt` (`documents/ReservationReceipt.java:10-27`) serve aos três documentos, mudando só o
  título e o texto. É o que o texto pede para o JSON.

**O exagero.** Não encontrei estrutura sem necessidade fora do pattern:
- A interface `DocumentWriter` (`documents/DocumentWriter.java:7`) tem três implementações e é o próprio pattern.
- Não há builders nem camadas que só repassam.
- `EmailSender` (`messages/EmailSender.java:12-14`) é fino, mas separa enviar de guardar, e isso o texto sugere.

**O design do resto.** É bom no geral:
- os tipos são certos: `BigDecimal` com `HALF_EVEN` a cada passo (`reservations/PriceCalculator.java:33-52`),
  `LocalDate`, e `enum` para situação, plano e tipo de quarto;
- os métodos são curtos e os nomes claros;
- o estado da `Reservation` só muda por métodos de pacote (`reservations/Reservation.java:102-114`).

Os defeitos são localizados:
- **Texto duplicado.** `PersonReceiptWriter` e `CompanyInvoiceWriter` repetem, linha a linha, o quarto, as datas, as
  diárias, o desconto e o total (`documents/PersonReceiptWriter.java:20-29` e
  `documents/CompanyInvoiceWriter.java:23-32`). O `ForeignReceiptWriter` repete a mesma sequência em inglês
  (`documents/ForeignReceiptWriter.java:23-32`). Uma mudança no corpo comum precisa ser feita em três lugares.
- **Arredondamento repetido à mão.** O ISS repete o arredondamento com `setScale(2, RoundingMode.HALF_EVEN)` e um
  literal `0.05` (`documents/CompanyInvoiceWriter.java:33-34`), em vez de uma constante e do `cents` que já existe
  (`reservations/PriceCalculator.java:50`).
- **Um `catch` largo demais.** `catch (Exception)` ao ler o corpo (`web/HttpApi.java:113`) transforma qualquer falha
  em `INVALID_REQUEST` e descarta a causa.

## Testes

**Números:** 29 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 29 métodos escritos. A cobertura é de 94,2% das
linhas (549 de 583) e 81,6% dos ramos (124 de 152). Em `hotel.web` os ramos ficam em 29 de 48 (`measures/tests.md`).
Os testes rodam contra o PostgreSQL de verdade (5 arquivos), há um pela API e nenhum usa mocks.
**Nota:** com problemas

**A parte comum está bem testada, com valores:**
- **Preço:** a temporada nas bordas de 15/12, fim de fevereiro e julho, e o arredondamento meio para o par, com o
  caso 37,485 → 37,48 (`reservations/PriceCalculatorTest.java:30-75`).
- **Situações e recusas:** `reservations/ReservationServiceTest.java:52-161`.
- **Devoluções e o dinheiro que fica:** `reservations/ReservationServiceTest.java:108-122`.
- **Relatórios:** os dois têm o CSV inteiro comparado (`reports/ReportsTest.java:56-115`).

**A parte nova do cenário está rasa:**
- `documents/DocumentsByGuestTest.java` tem só 3 testes, que conferem o título e um ou dois trechos com `contains`.
- **A fatura** confere a linha da empresa e o ISS de R$ 38,99 (`:48-49`). Trocar 0.05 por outro valor seria pego.
- **O booking receipt** confere as datas e o total (`:60-61`). Não confere a linha do hóspede com passaporte e país,
  "1 night", a linha "Discount" nem o separador de milhar em BRL. Apagar ou trocar qualquer uma dessas passaria.
- **O comprovante com CPF** só tem o título conferido (`:68`). Nenhum teste compara o texto dele com o modelo do
  enunciado.
- **Sem nenhum teste:**
  - a validação "exatamente uma identificação", inclusive nenhuma, duas, CNPJ sem razão social e passaporte sem
    país (`web/HttpApi.java:126-134`);
  - os cinco campos de identificação e o `text` no JSON da reserva e do documento;
  - o e-mail de uma empresa ou de um estrangeiro levando o documento certo.

  Inverter `!=` por `==` em `web/HttpApi.java:131` não derrubaria nenhum teste.
- **Um teste pela API é frágil:** `web/HttpApiTest.java:77` compara `total` como `double` (`935.76`) e não confere as
  duas casas decimais do JSON.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), criadas por 2 migrações do Flyway, todas com chave
primária (`measures/database.md`):
- o dinheiro está todo em `numeric(10,2)` e as datas em `date`;
- a chave estrangeira `reservations.room_number` → `rooms` tem índice (o da `EXCLUDE`);
- há 6 restrições de regra: a `EXCLUDE` contra reserva dupla, a identificação única e os `CHECK` de datas, hóspedes,
  capacidade e diária;
- a única coisa acusada é a falta de `CHECK` em `status`, `rate_plan`, `rooms.type` e `messages.channel`;
- não há transação explícita (0) nem `FOR UPDATE` (0);
- os 4 candidatos de SQL montado com `+` concatenam só constantes (`reservations/ReservationStore.java:18-20,34,88`).

**Nota:** com problemas

**O que está bem:**
- **A reserva dupla é barrada no banco**, pela restrição
  `EXCLUDE USING gist (room_number WITH =, daterange(check_in, check_out) WITH &&) WHERE (status <> 'CANCELLED')`
  (`src/main/resources/db/migration/V1__create_tables.sql:38-40`). A violação vira `ROOM_UNAVAILABLE`
  (`reservations/ReservationStore.java:60-63`), e por isso a proteção vale entre processos, não só no `synchronized`.
- **A identificação única tem um `CHECK`** no banco (`V2__guest_identification.sql:8-11`).
- **As conexões** são devolvidas com try-with-resources num pool Hikari (`Database.java:23-27`).
- **Os quartos** vêm na própria migração (`V1__create_tables.sql:9-14`).

**O que um revisor pediria para mudar:**
- **A confirmação e o e-mail não andam juntos.** `confirm` grava a nova situação e o valor pago
  (`reservations/ReservationService.java:64-65`) e depois insere a mensagem em outra conexão (`:67-70`, via
  `messages/SentMessages.java:20-35`), sem transação. Se o insert da mensagem falhar, a reserva fica CONFIRMED sem
  e-mail, e um novo pedido de confirmação recebe 409.
- **Os relatórios lêem a tabela inteira** (`ReservationStore.all()`) e filtram em Java
  (`reports/OccupancyReport.java:25-30`, `reports/RevenueReport.java:25-31`). Com o tempo, a tabela de reservas só
  cresce.
- **`status` e `rate_plan` são texto sem `CHECK`** (`V1__create_tables.sql:28,33`). Um valor fora do `enum` só é
  descoberto na leitura (`reservations/ReservationStore.java:138,146`).
- **O `CHECK` de identificação deixa passar um caso.** Ele conta "CNPJ e razão social" juntos, então aceita uma linha
  com CPF e um CNPJ sem razão social (`V2__guest_identification.sql:9-11`). Hoje só a validação HTTP impede isso.

## Comportamento

**Números:** 83 de 83 conferências passaram, nenhuma falhou (`measures/behavior.md`):
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que o texto pede funciona como foi pedido:
- **Preços e operação:** os preços com temporada e desconto (599,76 e 37,48), as recusas com os códigos certos, o
  e-mail de confirmação e os dois relatórios.
- **Os dados** continuam lá depois de reiniciar.
- **Os três documentos:**
  - a pessoa com CPF continua recebendo o comprovante;
  - a empresa recebe a fatura com o ISS de R$ 38,99, dentro de um e-mail em português, e também no check-out;
  - o estrangeiro recebe o booking receipt com `BRL 2,783.52`, datas `2027-03-10`, "1 night" e a linha "Discount".
- **As identificações erradas** são recusadas com 400: nenhuma, CPF junto com CNPJ, CNPJ sem razão social e
  passaporte sem país.
- **Reservas simultâneas:** em 10 rodadas de dois pedidos ao mesmo tempo, nunca saíram duas reservas do mesmo quarto.

## Dúvidas

- A forma do pattern é uma fábrica concentrada: uma classe que escolhe o escritor pelo tipo de hóspede, com uma
  interface e três implementações. Não é o Factory Method clássico, com um criador sobrescrito por subclasse. A
  referência aceita as duas formas como acerto.
- Pesei a falta de transação entre confirmar e gravar o e-mail como defeito de banco. Num caso real, ela só aparece
  se o insert da mensagem falhar depois do update, e nenhuma conferência provocou essa falha.
