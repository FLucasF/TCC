# Revisão: construção, cenário 05

## Visão geral

O agente montou um serviço Javalin com sete pacotes: `hotel` (montagem e banco), `hotel.web` (rotas e JSON),
`hotel.reservations` (serviço, cálculo de preço, entidade e o `ReservationStore` com JDBC), `hotel.rooms`,
`hotel.documents` (o texto dos três documentos), `hotel.messages` (as mensagens enviadas) e `hotel.reports` (os dois
CSV). Uma requisição entra em `HttpApi` (`src/main/java/hotel/web/HttpApi.java:61-91`), que valida o corpo e chama o
`ReservationService`. O serviço usa o `PriceCalculator` e o `ReservationStore`, e os dados ficam no PostgreSQL, num
esquema criado por duas migrações do Flyway e protegido por uma restrição `EXCLUDE` contra reserva dupla. O
comportamento está todo certo (83 de 83 conferências) e o banco é sólido. O defeito mais sério é o do pattern que o
texto pedia: a escolha de qual documento emitir (comprovante, fatura ou *booking receipt*) é um `if/else if/else`
sobre o tipo de hóspede copiado igual nos três lugares que emitem o documento
(`src/main/java/hotel/reservations/ReservationService.java:67-76`, `:123-132` e `:144-153`). Quando chegar o documento
das agências, vai ser preciso mexer nos três.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A maior classe é `HttpApi` (184 linhas), depois `ReservationService` (174) e `ReservationStore` (150).
**Nota:** com problemas

As camadas existem e quase sempre respeitam o seu papel:
- **A borda HTTP é fina.** Cada rota lê a requisição, chama o serviço e escreve a resposta (`HttpApi.java:62-90`). Os
  códigos de erro viram status num lugar só (`HttpApi.java:93-107`).
- **As regras não conhecem Javalin nem SQL.** O preço, as situações da reserva e o cancelamento ficam em
  `ReservationService` e `PriceCalculator`. Pela tabela do jdeps, `hotel.reservations` não usa Javalin.
- **O SQL fica isolado** nas classes de acesso a dados: `ReservationStore`, `Rooms` e `SentMessages`.
- **A montagem está num lugar só**, o construtor de `Hotel` (`src/main/java/hotel/Hotel.java:20-28`), sem `static`
  global.

Os defeitos:
- **O ciclo entre `hotel.documents` e `hotel.reservations`.** Os textos dos documentos leem a `Reservation`
  (`src/main/java/hotel/documents/DocumentTexts.java:3`, `ReservationReceipt.java:3`), e o serviço de reservas importa
  e monta os documentos (`ReservationService.java:3-4`). A decisão de qual documento emitir mora no serviço e não no
  pacote dos documentos, e por isso a dependência vai nos dois sentidos.
- **Uma regra de domínio ficou na borda HTTP.** A regra "exatamente um jeito de identificar o hóspede" só existe em
  `HttpApi.java:126-134` (e numa `CHECK` do banco). O `Guest` (`src/main/java/hotel/reservations/Guest.java:7-14`)
  aceita qualquer combinação.

São defeitos localizados, que não quebram a separação.

## Design de baixo nível

**Números:** 3 tipos de documento; nenhum está numa estrutura de criação. A escolha do documento é um `if/else if/else` sobre `isCompany()`/`isForeigner()` repetido 3 vezes (confirmação, check-out e segunda via). Não há fábrica, criador nem método polimórfico no hóspede.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto anuncia que vem um quarto documento (o das agências), e a resposta esperada era concentrar num
lugar só a decisão de qual documento criar. O agente separou o texto de cada documento em funções auxiliares
(`DocumentTexts.personReceipt`, `companyInvoice` e `foreignReceipt`, `DocumentTexts.java:15`, `:33` e `:55`). Mas a
decisão de qual delas chamar, e com que título, aparece copiada em três lugares:
- `confirm`, em `ReservationService.java:67-76`;
- `checkOut`, em `ReservationService.java:123-132`;
- `receiptCopy`, em `ReservationService.java:144-153`.

A referência do pattern descreve exatamente isso como omissão: "o `if` ou o `switch` sobre o tipo de hóspede aparece
repetido nos três lugares (mesmo que o texto de cada documento esteja em funções auxiliares separadas)". Além disso, o
título de cada documento ("Fatura de hospedagem", "Booking receipt", "Comprovante de reserva") é um literal repetido
nas três cópias, separado da função que gera o texto, e pode divergir dele.

**O exagero.** Não há estrutura sem necessidade: nada de interface com uma implementação só, fábrica vazia ou camada
que só repassa. A classe `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:4-15`) é fina, mas tem um papel
(fixa o canal `EMAIL`).

**O resto do código** é cuidadoso:
- os tipos estão certos: `BigDecimal` com `HALF_EVEN` a cada passo (`PriceCalculator.java:50-52`), `LocalDate`, e
  `enum` para status, plano e tipo de quarto;
- os erros têm código próprio (`HotelException` + `ErrorCode`), e a violação da `EXCLUDE` vira `ROOM_UNAVAILABLE`
  (`ReservationStore.java:60-63`);
- as transições de situação são simples de seguir (`ReservationService.java:58-134`).

Os defeitos, fora a classe do pattern:
- **Os textos dos documentos estão duplicados.** O comprovante e a fatura repetem as mesmas oito linhas: quarto,
  datas, "diária/diárias", diárias, desconto e total (`DocumentTexts.java:20-29` e `:39-48`). O *booking receipt*
  repete a mesma sequência em inglês (`:61-70`).
- **O ISS é calculado dentro da montagem do texto** (`DocumentTexts.java:49-50`). É uma regra de dinheiro escondida na
  formatação, que nenhum outro lugar (o JSON do documento, um relatório) consegue usar.
- **O `Guest` guarda os três jeitos de identificação como sete campos `String` que podem ser `null`**
  (`Guest.java:7-14`), e o tipo é deduzido pelo que está preenchido (`isCompany()`/`isForeigner()`, `:20-26`). Nada no
  tipo garante que só um jeito está preenchido, e o caso "CPF" é simplesmente o que sobra no `else`.
- **`Reservation.changeStatus` aceita qualquer situação** (`src/main/java/hotel/reservations/Reservation.java:102-104`):
  quem garante as transições é só o serviço.

## Testes

**Números:** 29 testes rodaram, 0 falharam, 0 pulados; 29 métodos de teste escritos. Cobertura de 93,6% das linhas (553 de 591) e 81,2% dos ramos (130 de 160). O pacote `hotel.web` tem 29 de 48 ramos cobertos. Os testes rodam contra o banco real (5 arquivos) e pela API (1); não há mocks.
**Nota:** com problemas

A parte comum está bem testada, com valores:
- **o preço:** a temporada que começa em 15/12 e termina em fevereiro, julho, e o arredondamento "meio para o par"
  (37,48 e 212,42) (`src/test/java/hotel/reservations/PriceCalculatorTest.java:31-74`);
- **o cancelamento e a devolução por plano**, inclusive o valor que fica com a pousada (`ReservationServiceTest.java:109-121`);
- **as recusas por código de erro** (`ReservationServiceTest.java:53-68`, `HttpApiTest.java:161-176`);
- **o e-mail**, com assunto e valores (`ReservationServiceTest.java:84-96`);
- **os dois relatórios**, comparando o CSV inteiro (`src/test/java/hotel/reports/ReportsTest.java:57-115`).

A parte nova do cenário ficou testada só em parte. `DocumentsByGuestTest` confere a fatura (cabeçalho da empresa e
ISS de R$ 38,99, `src/test/java/hotel/documents/DocumentsByGuestTest.java:47-49`) e o *booking receipt* (datas e
`BRL 779.80`, `:59-61`). Ficaram sem teste:
- **A regra "exatamente um jeito de identificar".** Nenhum teste manda duas identificações, nenhuma, um CNPJ sem razão
  social ou um passaporte sem país. Os nomes `companyCnpj` e `guestPassport` não aparecem em `src/test`.
  - Pergunta da mutação: se alguém apagasse `|| companyIncomplete || passportIncomplete` em `HttpApi.java:131`, nenhum
    teste falharia. O CNPJ sem razão social passaria a cair na `CHECK` do banco e virar 500.
- **"1 night", a linha "Discount" e o milhar com vírgula do *booking receipt*.** Trocar `nights == 1` em
  `DocumentTexts.java:65` não derruba nenhum teste.
- **O documento da empresa e o do estrangeiro dentro do e-mail de confirmação e no check-out.** Os testes só pegam a
  segunda via.
- **Os cinco campos de identificação, com `null`, no JSON da reserva e do documento.**

Para a pessoa com CPF, o comprovante novo só tem o título conferido (`DocumentsByGuestTest.java:68`). Um teste
(`ReservationServiceTest.java:76-81`) não tem asserção: só espera que nada lance exceção. A iniciativa é boa e os
testes passam, mas metade das regras novas está sem rede.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; 2 migrações do Flyway. As conferências não acusaram nenhum dinheiro fora de `numeric(10,2)`, nenhuma chave estrangeira sem índice e nenhuma data em texto. Só `status`, `rate_plan`, `type` e `channel` são `text` sem `CHECK`. Não há transação explícita nem `FOR UPDATE`.
**Nota:** bom

**O esquema** nasce e evolui por migrações versionadas. A identificação do hóspede entrou numa migração nova
(`src/main/resources/db/migration/V2__guest_identification.sql:2-11`), sem editar a V1. Os tipos estão certos:
- dinheiro em `numeric(10, 2)` e datas em `date` (`V1__create_tables.sql:25-35`);
- a reserva ligada ao quarto por chave estrangeira (`:21`);
- `CHECK` nas regras: hóspedes ≥ 1, saída depois da entrada, e exatamente uma identificação (`V2__...sql:8-11`).

**A regra mais importante** (um quarto não pode ter duas reservas na mesma noite) está no banco, com
`EXCLUDE USING gist (room_number WITH =, daterange(check_in, check_out) WITH &&) WHERE (status <> 'CANCELLED')`
(`V1__create_tables.sql:38-40`). A regra vale mesmo com vários processos, e a violação vira 409
(`ReservationStore.java:60-63`).

**O código:** o SQL sempre usa `?`. Os `+` que o script listou só juntam constantes (`ReservationStore.java:18-20`,
`:32-34`). As conexões vêm de um pool Hikari (`src/main/java/hotel/Database.java:23-27`) e voltam por
try-with-resources.

Ressalvas pequenas, que não mudam a nota:
- **A confirmação faz duas escritas sem transação:** grava a reserva (`ReservationService.java:64`) e depois a
  mensagem (`:77-80`). Uma falha entre as duas deixa a reserva confirmada sem e-mail registrado.
- **Os dois relatórios trazem a tabela inteira de reservas para filtrar em Java**
  (`src/main/java/hotel/reports/OccupancyReport.java:25`, `RevenueReport.java:25`). O de ocupação ainda faz uma
  consulta de quarto por linha (`OccupancyReport.java:36`).
- **As colunas de situação e de plano não têm `CHECK`.**

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- as reservas simultâneas: 2 de 2 (10 rodadas, sempre uma criada e outra recusada com 409).

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede:
- os preços com temporada e desconto saem com os valores do texto (599,76; 37,48; 212,42);
- as situações e as devoluções seguem as regras, e os relatórios saem no formato combinado;
- nada se perde ao reiniciar, e duas reservas simultâneas do mesmo quarto nunca passam juntas.

Na parte nova, também está tudo certo:
- a empresa recebe a fatura com o ISS de R$ 38,99, no e-mail (em português), no check-out e na segunda via;
- o estrangeiro recebe o *booking receipt* em inglês, com datas `2027-03-10`, `BRL 2,783.52`, "1 night" e a linha
  "Discount";
- a pessoa com CPF continua recebendo o comprovante;
- pedidos sem identificação, com duas, ou com um jeito incompleto voltam com 400 `INVALID_REQUEST`.

## Dúvidas

Na classe do pattern, fiquei entre `omissão` e `malfeito`. Existe um começo de estrutura: as três funções de texto em
`DocumentTexts` e um único tipo de resultado, `ReservationReceipt`. Mesmo assim, não há nenhum lugar que concentre a
escolha do documento: a decisão está copiada nos três pontos de emissão. A referência classifica esse caso
expressamente como omissão, e foi essa a classe que a evidência sustentou.
