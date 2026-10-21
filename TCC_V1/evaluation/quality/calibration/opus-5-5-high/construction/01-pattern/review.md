# Revisão: construção, cenário 01

## Visão geral

O projeto tem sete pacotes. `hotel` monta as peças e sobe o servidor, `hotel.web` é a borda HTTP do Javalin,
`hotel.reservations` guarda as regras, os planos de tarifa e o `ReservationStore` (JDBC), `hotel.rooms` cuida dos
quartos e do acesso a eles, `hotel.messages` guarda as mensagens enviadas, `hotel.documents` gera o comprovante e
`hotel.reports` gera os dois CSV. Uma requisição entra numa lambda de `HttpApi` (`src/main/java/hotel/web/HttpApi.java:61-91`),
que lê o corpo e chama o `ReservationService`. O serviço valida, pede o preço ao `PriceCalculator`, que por sua vez pede ao
plano (`RatePlanPolicy`), e grava pelo `ReservationStore`. Os dados ficam no PostgreSQL, num esquema que o Flyway cria com duas
migrações. O choque de reservas é barrado também por uma restrição `EXCLUDE` no banco. Os planos de tarifa seguem um Strategy
limpo, com os cinco planos em classes próprias. As 72 conferências de comportamento passaram e os 34 testes passam, com 94,4%
das linhas cobertas. Não achei defeito sério. Os pontos a corrigir são menores: um ciclo pequeno entre `documents` e
`reservations`, a confirmação que grava a reserva e o e-mail sem transação, e os relatórios que trazem a tabela inteira de
reservas para filtrar em Java.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles. Há 1 ciclo, entre `hotel.documents` e `hotel.reservations`
(`measures/dependencies.md`). O Javalin só aparece em `hotel` e `hotel.web`. O JDBC aparece em `hotel.reservations`,
`hotel.rooms` e `hotel.messages`, sempre numa classe de acesso a dados. A maior classe é `HttpApi`, com 170 linhas.
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota lê a requisição, chama o serviço e escreve a resposta
  (`src/main/java/hotel/web/HttpApi.java:62-90`). A conversão de `ErrorCode` em status HTTP fica num ponto só
  (`HttpApi.java:93-107`), e a validação dos campos obrigatórios fica na borda (`HttpApi.java:109-130`), que é o lugar dela.
- **As regras não conhecem o Javalin nem o SQL.** `ReservationService`, `PriceCalculator` e os planos não importam
  `io.javalin` nem `java.sql`. O SQL está isolado em `ReservationStore`, `Rooms` e `SentMessages`, todos com
  `PreparedStatement`.
- **A montagem fica num lugar só,** em `src/main/java/hotel/Hotel.java:21-31`. Ninguém dá `new` no acesso a dados por
  dentro, e não há `static` mutável.
- **O ciclo:** `ReservationService` cria o `ReservationReceipt` (`src/main/java/hotel/reservations/ReservationService.java:3`,
  `:66`, `:108`, `:117`), e o `ReservationReceipt` recebe a `Reservation` no construtor
  (`src/main/java/hotel/documents/ReservationReceipt.java:3`, `:24`). Ligado a isso, o texto do e-mail de confirmação é
  montado dentro do serviço de regras (`ReservationService.java:124-130`), e não junto do comprovante em `documents`. O
  ciclo é pequeno e entre pacotes vizinhos, mas foi criado pelo agente. Seria fácil desfazê-lo, por exemplo levando a
  montagem do comprovante e do e-mail para quem chama, ou fazendo `documents` receber só os dados.
- **Um detalhe:** `hotel.reservations` mistura as regras com a classe JDBC (`ReservationStore`) no mesmo pacote. Como a
  classe está separada, isso não pesa na nota.

## Design de baixo nível

**Números:** 5 de 5 planos estão na estrutura (`RatePlanPolicy` com 5 implementações) e 0 ficam fora dela. Não há
nenhum `switch` nem `if` sobre o plano fora do mapa de seleção. O único `switch` do código principal converte o
`ErrorCode` em status HTTP (`HttpApi.java:101`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A interface `RatePlanPolicy` (`src/main/java/hotel/reservations/RatePlanPolicy.java:7-15`) reúne as três
decisões de um plano: quem pode usar (`validate`), o preço (`price`) e a devolução (`refund`). Os cinco planos, inclusive
os dois do ponto de partida, têm cada um a sua classe:

- `StandardPlan.java:6-21`;
- `NonRefundablePlan.java:6-23`;
- `CorporatePlan.java:8-32`, com o mapa das empresas e a recusa do código desconhecido;
- `WeekendPackagePlan.java:10-38`, com sexta a domingo, o preço fechado por tipo de quarto e a devolução de metade;
- `LongStayPlan.java:8-29`, com o mínimo de 7 noites.

Os planos são escolhidos num ponto só, por um `EnumMap` (`RatePlans.java:9-21`). O cálculo pede ao plano sem nomeá-lo
(`PriceCalculator.java:25-29`), e o cancelamento também (`ReservationService.java:84`). A alta temporada continua comum a
todos, em `PriceCalculator.nightlyCharges`/`nightPrice` (`PriceCalculator.java:31-56`), e o plano recebe as diárias
prontas. Um plano novo é uma classe, uma constante no `enum` e uma linha no mapa.

Duas observações, que não mudam a classe:

- O código da empresa é gravado em todos os planos (`ReservationStore.java:50`), e não só no convênio. Para uma reserva
  STANDARD que mande `companyCode`, o banco guarda o código, que não serve para nada.
- O `validate` do plano roda depois da conferência de disponibilidade (`ReservationService.java:44-48`). Um pacote numa
  data errada e num quarto já ocupado responde 409, e não 400. O texto não fecha essa ordem.

**O exagero.** Não há estrutura sem necessidade. `PercentOff` (`PercentOff.java:10-13`) evita repetir o mesmo cálculo de
desconto em quatro planos. `NightlyCharges` e `Price` são `record`s simples. `EmailSender`
(`src/main/java/hotel/messages/EmailSender.java:12-14`) quase só repassa para `SentMessages`, mas fixa o canal EMAIL e
deixa o ponto onde um envio de verdade vai entrar, o que o texto anuncia. Ele está no limite, sem chegar a ser cerimônia.

**O resto do código.** Dinheiro está em `BigDecimal`, com `HALF_EVEN` em cada passo (`PriceCalculator.java:58-60`), as
datas em `LocalDate`, e a situação e o plano em `enum`. As exceções são de domínio e têm código (`HotelException`). Os
métodos são curtos e os nomes dizem o que fazem. Há pequenos senões, que um revisor só comentaria:

- `PriceCalculator.cents` virou um utilitário de dinheiro usado em `Reservation.java:21`, `ReservationStore.java:49`,
  `NonRefundablePlan.java:21` e `WeekendPackagePlan.java:31,36`. Ele merecia um lugar próprio.
- `ReservationStore.read` reconstrói a reserva passando por um `NewReservation`, que é o tipo do pedido
  (`ReservationStore.java:126-133`). Nesse caminho o `companyCode` lido do banco se perde, porque `Reservation` não o
  guarda (`Reservation.java:31-41`).
- `Reservation.changeStatus` aceita qualquer situação (`Reservation.java:102-104`). As transições são vigiadas só no serviço.
- `HttpApi.java:113` usa `catch (Exception)`, mas só em volta da leitura do corpo, para devolver 400. Isso é aceitável.

## Testes

**Números:** 34 testes rodaram, com 0 falhas, 0 erros e 0 pulados, e há 34 métodos de teste escritos. A cobertura é de
523 de 554 linhas (94,4%) e de 109 de 124 ramos (87,9%). Em `hotel.reservations` são 260 de 273 linhas e 57 de 60 ramos,
e em `hotel.reports`, 48 de 48 linhas e 22 de 22 ramos (`measures/tests.md`). Os testes rodam contra o PostgreSQL real
(5 arquivos) e pela API HTTP (1 arquivo), sem mocks.
**Nota:** bom

As regras são conferidas com valores.

**A parte nova** (`src/test/java/hotel/reservations/NewRatePlansTest.java`):

- os três descontos de empresa, com o desconto e o total exatos (`:44-56`: 93,58/686,22, 140,36/639,44 e 194,95/584,85);
- a recusa do convênio sem código e com código desconhecido, e a devolução integral (`:59-67`);
- o pacote nos três tipos de quarto e em julho, mais a devolução de 410,00 (`:70-83`);
- as três recusas do pacote (`:86-93`);
- a estadia longa com o exemplo do texto e com noites de temporada (`:96-107`), e a recusa com 6 noites (`:110-113`);
- o `companyCode` ignorado fora do convênio (`:116-121`).

**A pergunta da mutação.** Trocar 0.12 por 0.21 quebra `:50`. Trocar a metade devolvida quebra `:82`. Trocar `< 7` por
`<= 7` quebra `:97`, e por `< 6` quebra `:112`. Tirar a conferência da sexta-feira quebra `:88`.

**A parte comum** também está coberta:

- a temporada e as suas bordas (`PriceCalculatorTest`);
- o meio para o par (37,48/212,42);
- as transições e as recusas (`ReservationServiceTest.java:54-160`);
- as devoluções por plano (`ReservationServiceTest.java:110-124`);
- os relatórios, comparando o CSV inteiro (`ReportsTest`).

**O que ficou fraco:**

- O texto do comprovante e o do e-mail só são conferidos com `contains` em duas linhas
  (`ReservationServiceTest.java:96-97`). O formato "1 diária", as datas `dd/MM/yyyy` e o separador de milhar de
  `R$ 1.234,56` não têm teste.
- Pela API, o dinheiro é conferido com `asDouble` (`HttpApiTest.java:77`, `:115`), o que não verifica as duas casas
  decimais pedidas no JSON. `HttpApiTest.java:66` chega a afirmar `"249.9"`.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations` e `messages`), criadas por 2 migrações do Flyway. As conferências não
acusaram tabela sem chave primária, dinheiro fora de `numeric(10,2)` (as 6 colunas estão certas), chave estrangeira sem
índice nem data em texto. Acusaram 4 colunas de situação ou tipo sem CHECK: `reservations.status`,
`reservations.rate_plan`, `rooms.type` e `messages.channel`. Há 5 restrições de regra, entre elas um `EXCLUDE USING gist`
contra a reserva dupla. O código tem 0 transações explícitas e 0 `FOR UPDATE` (`measures/database.md`).
**Nota:** com problemas

**O esquema está bem feito.** Ele é criado e evoluído por migrações versionadas
(`src/main/resources/db/migration/V1__create_tables.sql` e `V2__reservation_company_code.sql`). Tem os tipos certos, a
chave estrangeira do quarto (`V1__create_tables.sql:21`), as datas em `date`, `CHECK (check_out > check_in)` e
`CHECK (guests >= 1)` (`:27`, `:36`). A regra "um quarto, uma reserva por noite" está protegida no próprio banco, pela
restrição de exclusão com `daterange` que ignora as canceladas (`:38-40`). O código traduz a violação em 409
(`ReservationStore.java:56-59`), o que vale também com vários processos. Todo o SQL usa `?`: as concatenações que o script
lista juntam só a constante `COLUMNS` (`ReservationStore.java:18-19`). As conexões vêm de um pool Hikari
(`Database.java:23-27`) e são devolvidas com try-with-resources.

**Os defeitos:**

- **A confirmação faz duas escritas sem transação.** `store.update` grava a reserva como CONFIRMED e paga, e depois
  `emailSender.send` insere a mensagem, cada um com a sua conexão (`ReservationService.java:64-70`,
  `ReservationStore.java:65-77`, `SentMessages.java:20-35`). Se a segunda escrita falhar, a reserva fica confirmada sem o
  e-mail, o site recebe 500, e a nova tentativa responde 409.
- **Os dois relatórios trazem a tabela inteira de reservas para filtrar em Java.** Eles chamam `store.all()`
  (`ReservationStore.java:109-122`, usado em `OccupancyReport.java:25` e `RevenueReport.java:25`). A data e o mês
  poderiam ir no `WHERE`. Isso cresce sem limite.
- **Faltam CHECKs.** `status` e `rate_plan` não têm CHECK. O `company_code` é gravado também nos planos que não são
  convênio (`ReservationStore.java:50`).

## Comportamento

**Números:** 72 de 72 conferências passaram e nenhuma falhou (`measures/behavior.md`):

- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 01, os planos novos: 21 de 21;
- as duas reservas ao mesmo tempo: 2 de 2 (em 10 rodadas, sempre uma criada e a outra recusada com 409).

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede:

- os preços com a alta temporada e o arredondamento meio para o par;
- os três convênios com os valores do financeiro (686,22, por exemplo);
- o pacote com o preço fechado, que continua 820,00 em julho e devolve 410,00 quando cancelado;
- a estadia longa com 1.574,37 e a recusa abaixo de 7 noites;
- a recusa com 400 INVALID_REQUEST nas regras dos planos;
- o e-mail, a segunda via e os dois relatórios;
- as reservas que continuam lá depois de reiniciar o sistema;
- a reserva dupla do mesmo quarto barrada mesmo quando os pedidos chegam juntos.

Fora do roteiro, uma observação de leitura: quando uma reserva viola a regra do plano e também cai num quarto ocupado, a
resposta é 409 ROOM_UNAVAILABLE, e não 400 (`ReservationService.java:44-48`). O texto não define essa ordem, e por isso
ela não pesa na nota.

## Dúvidas

- O `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:12-14`) quase só repassa para `SentMessages`. Contei
  como contenção, porque fixa o canal e prepara o envio real que o texto anuncia, mas um revisor mais rígido poderia vê-lo
  como uma camada a mais.
- Na coluna do banco, a falta de transação na confirmação e a leitura da tabela inteira nos relatórios não perdem dado
  hoje. Dei `com problemas` pela soma dos dois, mas o esquema em si (migrações, tipos, `EXCLUDE`) seria de nota `bom`.
