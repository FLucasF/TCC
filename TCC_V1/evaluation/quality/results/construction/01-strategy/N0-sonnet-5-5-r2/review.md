# Revisão: construção, cenário 01

## Visão geral

O projeto tem dois pacotes: `pousada` (14 classes, 745 linhas) e `pousada.plan` (9 classes, 218 linhas). Uma requisição
entra pelas rotas do Javalin em `Api` (`src/main/java/pousada/Api.java:32-43`), que lê e valida o corpo e chama o
`ReservationService`. O serviço guarda num lugar só as regras (situações da reserva, capacidade, disponibilidade) e o
SQL em JDBC puro, com uma transação por operação (`src/main/java/pousada/ReservationService.java:42-56`). O preço, a
validação e a devolução de cada plano de tarifa ficam no pacote `plan`, numa interface `RatePlan` com uma classe por
plano. Os dados ficam no PostgreSQL, num esquema que `Db.migrate` cria com `CREATE TABLE IF NOT EXISTS` toda vez que o
serviço sobe (`src/main/java/pousada/Db.java:35-82`). As peças são ligadas no `Main`. O sistema passou nas 72
conferências do roteiro, e os 18 testes passam. Não há defeito sério. Os pontos fracos são estes: o serviço mistura
regras e SQL, há um ciclo entre os dois pacotes porque os planos lançam a exceção HTTP do pacote raiz, a situação da
reserva é guardada como texto solto e o esquema não tem migrações versionadas.

## Arquitetura

**Números:** 2 pacotes (`pousada` com 14 classes e 745 linhas, `pousada.plan` com 9 classes e 218 linhas), 2 dependências entre eles (`pousada -> pousada.plan` e `pousada.plan -> pousada`), 1 ciclo (`pousada` ↔ `pousada.plan`). As maiores classes são `ReservationService` (293 linhas) e `Api` (215).
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota lê o caminho, chama o serviço e converte o resultado para JSON
  (`src/main/java/pousada/Api.java:32-43`). O `create` (`Api.java:80-114`) também valida o formato do corpo, a
  ordem das datas e o mínimo de hóspedes (`Api.java:105`, `Api.java:107`). Isso é validação de requisição e
  pode ficar na borda. Os erros são traduzidos num lugar só (`Api.java:45-49`).
- **A montagem está num lugar só.** O `Main` cria o pool, roda o `migrate` e entrega o `ReservationService` à `Api`
  (`src/main/java/pousada/Main.java:9-13`). O serviço recebe o `DataSource` pelo construtor
  (`ReservationService.java:38-40`).
- **O serviço mistura regras e acesso ao banco.** `ReservationService` tem as regras de negócio (as transições em
  `ReservationService.java:158`, `175`, `189` e `197`, a capacidade em `110-113`, o conflito de datas em `115-124`) e,
  no mesmo método, o SQL, o mapeamento do `ResultSet` (`280-292`) e a montagem dos CSV (`205-253`). Não existe uma
  classe de acesso a dados. Com 293 linhas, é a classe onde o próximo pedido vai pesar. O serviço também não conhece o
  Javalin, e isso é bom.
- **Há um ciclo entre os dois pacotes.** `pousada.plan` depende de `pousada` porque usa `ApiException`, `Money`,
  `Room`, `RoomType` e `Season` (`plan/CorporatePlan.java:3`, `plan/BookingContext.java:3-5`). `pousada` depende de
  `plan` pelo serviço e pela `Api`. O pior desses laços é a regra de negócio lançar uma exceção que carrega o status
  HTTP: `ApiException.invalid` devolve 400 (`src/main/java/pousada/ApiException.java:14-16`) e é chamada dentro dos
  planos (`plan/CorporatePlan.java:20`, `plan/LongStayPlan.java:17`, `plan/WeekendPackagePlan.java:24`). Assim as
  regras sabem qual é o protocolo da borda. O ciclo é pequeno, entre dois pacotes vizinhos, mas mostra
  responsabilidades fora do lugar.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (`STANDARD`, `NON_REFUNDABLE`, `CORPORATE`, `WEEKEND_PACKAGE` e `LONG_STAY`, cada um numa classe que implementa `RatePlan`) e 0 fora dela. Nenhum `switch` ou `if` sobre o plano no preço nem na devolução. Há 1 regra pequena de plano fora do plano (`ReservationService.java:139`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O pedido é um Strategy, e foi aplicado direito:

- A interface `RatePlan` tem `code()`, `validate(ctx)`, `quote(ctx)` e `refund(amountPaid)`
  (`src/main/java/pousada/plan/RatePlan.java:10-21`). Cada um dos cinco planos tem a sua implementação, e os dois
  planos da parte comum também estão nela (`plan/StandardPlan.java:5-13`, `plan/NonRefundablePlan.java:7-17`).
- As regras de cada plano ficam dentro da própria classe:
  - o código da empresa e as taxas, em `plan/CorporatePlan.java:10-26`;
  - a regra de sexta a domingo e o preço fechado por tipo de quarto, em `plan/WeekendPackagePlan.java:13-32`;
  - o mínimo de 7 noites, em `plan/LongStayPlan.java:16-18`.
- A escolha do plano é feita num único ponto, um mapa pelo código (`plan/RatePlans.java:11-29`).
- Quem usa a estrutura nunca pergunta qual é o plano. A criação chama `n.plan().validate(ctx)` e `n.plan().quote(ctx)`
  (`ReservationService.java:109` e `125`). O cancelamento chama `RatePlans.get(r.ratePlan()).refund(...)`
  (`ReservationService.java:176`).
- O cálculo comum (as noites com a temporada) fica no `BookingContext` (`plan/BookingContext.java:20-27`). O desconto
  percentual fica em `Quote.withDiscountRate` (`plan/Quote.java:14-16`). Por isso, um plano novo é uma classe a mais e
  uma linha no registro.
- Só uma regra de plano ficou fora do plano: guardar o `companyCode` apenas quando o plano é `"CORPORATE"`
  (`ReservationService.java:139`). Ela é pequena, não trata de preço nem de devolução e não muda a classe. Está anotada
  nas dúvidas.

**O exagero.** Não há estrutura sem necessidade. `BookingContext` e `Quote` são records com papel real, e `RatePlans`
é o ponto de seleção que o Strategy pede. Não há fábricas, builders nem interfaces com uma única implementação fora da
parte do pattern.

**O resto do código.** Os tipos do dinheiro e das datas estão certos: o dinheiro é `BigDecimal` com `HALF_EVEN` num
lugar só (`src/main/java/pousada/Money.java:15-22`) e as datas são `LocalDate`. Mesmo assim, há defeitos que um
revisor pediria para mudar:

- **A situação da reserva é texto solto.** `Reservation.status` é `String`
  (`src/main/java/pousada/Reservation.java:8`). As comparações também usam texto: em `Reservation.java:12`, em
  `ReservationService.java:158`, `175`, `189` e `197`, e nos literais SQL de `159`, `190`, `198` e `210`. Não existe
  um `enum` de situação, e um erro de digitação só aparece em tempo de execução. O plano também é guardado como texto
  na `Reservation` (`Reservation.java:7`) e precisa ser procurado de novo no registro para cancelar
  (`ReservationService.java:176`).
- **`ApiException` tem campos públicos** (`ApiException.java:5-6`) e é usada como exceção de domínio dentro dos planos.
  É o mesmo acoplamento com o HTTP descrito na arquitetura.
- **Os métodos do serviço são longos.** `ReservationService.create` (`ReservationService.java:98-151`) faz numa só
  sequência a trava do quarto, a validação, a capacidade, o conflito de datas, o cálculo e o INSERT. `confirm` grava o
  e-mail com SQL no meio da transição (`ReservationService.java:161-167`).
- **Detalhes menores:** a anotação `@Override` aparece só em alguns métodos que implementam a interface (está em
  `validate`, falta em `code`, `quote` e `refund`), e `revenueCsv` usa o nome completo de `java.time.YearMonth`
  (`ReservationService.java:232`).

São defeitos localizados. O código é legível e não há duplicação relevante.

## Testes

**Números:** 18 testes rodaram, 0 falhas, 0 erros, 0 pulados; 18 métodos de teste escritos (11 em `PlansTest` e 7 em `ApiIntegrationTest`). Cobertura: 94,4% das linhas (386 de 409) e 75,9% dos ramos (85 de 112). No pacote `pousada.plan`: 53 de 54 linhas e 14 de 14 ramos.
**Nota:** bom

Há dois níveis de teste e os dois conferem valores:

- **Unidade dos planos e da temporada** (`src/test/java/pousada/PlansTest.java`):
  - o exemplo da alta temporada, R$ 599,76 (`PlansTest.java:31-35`);
  - os limites da temporada, de 14/12 a 15/12, 29/02, 30/06 a 01/07 e 31/07 a 01/08 (`PlansTest.java:38-46`);
  - o não reembolsável, com a devolução zero (`PlansTest.java:49-55`);
  - os três exemplos do texto: Tecnosul com R$ 93,58 e R$ 686,22 (`PlansTest.java:58-64`), o pacote com R$ 820,00 e
    R$ 410,00 (`PlansTest.java:75-83`) e a estadia longa com R$ 174,93 e R$ 1.574,37 (`PlansTest.java:100-107`);
  - as recusas do convênio, inclusive o código em minúsculas (`PlansTest.java:67-72`), e as do pacote fora de sexta a
    domingo (`PlansTest.java:86-91`);
  - a estadia longa com noites de temporada (`PlansTest.java:110-114`);
  - o arredondamento meio para o par na devolução (`PlansTest.java:94-97`).
- **Integração pela API, contra o PostgreSQL de `DB_URL`, num esquema descartável**
  (`src/test/java/pousada/ApiIntegrationTest.java:155-169`):
  - o ciclo de vida inteiro, com o texto exato do e-mail e do comprovante (`ApiIntegrationTest.java:219-258`);
  - a disponibilidade e a liberação do quarto ao cancelar (`ApiIntegrationTest.java:261-275`);
  - os códigos de erro (`ApiIntegrationTest.java:286-300`);
  - as regras dos planos pela API (`ApiIntegrationTest.java:303-328`);
  - os dois CSV comparados byte a byte (`ApiIntegrationTest.java:331-352`).

**A pergunta da mutação:**

- Trocar 0,12 por 0,21 na Tecnosul falharia (`PlansTest.java:61`).
- Trocar `< 7` por `<= 7` no `LongStayPlan` falharia, porque o teste aceita 7 noites (`PlansTest.java:101`) e recusa 6
  (`PlansTest.java:105-106`).
- Trocar a metade da devolução do pacote falharia (`PlansTest.java:80`).
- Ficam sem rede:
  - as taxas da Agrovale (18%) e da Farmavida (25%), que nenhum teste usa;
  - o preço do pacote na suíte (R$ 1.200,00);
  - a devolução integral da estadia longa;
  - as duas reservas simultâneas.

São lacunas pequenas diante do resto. O texto não pedia testes, e o que o agente escreveu é de boa qualidade.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations` e `messages`), todas com chave primária. Todo o dinheiro está em `numeric(12,2)` e as datas em `date`. A única chave estrangeira (`reservations.room_number`) tem índice. Nenhuma restrição CHECK, UNIQUE ou EXCLUDE, e as colunas `status`, `rate_plan`, `type` e `channel` não têm CHECK. O esquema é criado por `CREATE TABLE` no código, sem arquivo de migração. O único SQL montado com `+` só junta constantes (`ReservationService.java:271`).
**Nota:** com problemas

- **O que está certo:**
  - Os tipos estão certos e há `NOT NULL` no que é obrigatório (`src/main/java/pousada/Db.java:46-63`).
  - Há um pool HikariCP (`Db.java:15-24`).
  - Todas as consultas usam `?` como parâmetro.
  - As conexões são devolvidas com try-with-resources.
  - Cada operação roda numa transação (`ReservationService.java:42-56`), de modo que a confirmação e o e-mail são
    gravados juntos (`ReservationService.java:159-167`).
  - A dupla reserva é barrada no banco, não só dentro do processo: o `SELECT ... FOR UPDATE` na linha do quarto põe na
    fila duas criações simultâneas do mesmo quarto (`ReservationService.java:101`). A verificação de conflito vem
    depois (`ReservationService.java:115-124`). As transições travam a linha da reserva (`ReservationService.java:271`).
  - A criação do esquema roda sob uma trava consultiva (`Db.java:38`) e semeia os quartos de forma idempotente
    (`Db.java:73-79`).
- **Os defeitos:**
  - O esquema nasce de `CREATE TABLE IF NOT EXISTS` no código (`Db.java:40`, `46` e `66`), sem migrações versionadas.
    Quando o próximo pedido acrescentar uma coluna, isso não alcança um banco que já existe.
  - Nenhuma regra é protegida por restrição. Não há CHECK em `status`, `rate_plan`, `guests > 0` nem
    `check_out > check_in`. A ausência de sobreposição depende só da trava na aplicação, porque não há uma restrição
    `EXCLUDE`. A trava funciona, e o roteiro confirmou isso.

## Comportamento

**Números:** 72 de 72 conferências passaram e 0 falharam. A parte comum passou em 46 de 46, os dados depois de reiniciar em 3 de 3, o cenário 01 em 21 de 21 e as duas reservas ao mesmo tempo em 2 de 2.
**Nota:** bom

Tudo o que o texto pede funciona pela API, a partir de um banco vazio:

- **A parte comum:** o preço com a temporada e o meio para o par (R$ 599,76 e o desconto de R$ 37,48), os códigos de
  erro, a confirmação com o e-mail, o check-in e o check-out com o comprovante, as devoluções e os dois relatórios em
  CSV.
- **Os planos novos:**
  - o convênio com os três descontos (R$ 686,22, R$ 639,44 e R$ 584,85), com recusa sem o código ou com um código
    inválido;
  - o pacote com o preço fechado (R$ 520,00, R$ 820,00 e R$ 1.200,00), também em julho, recusado fora de sexta a
    domingo e com devolução de R$ 410,00;
  - a estadia longa com R$ 1.574,37, a temporada contando nas noites de julho, a recusa com 6 noites e a devolução
    integral.
- **Os dados e a concorrência:** os dados continuam lá depois de reiniciar o serviço, e em 10 rodadas de duas
  reservas simultâneas só uma foi criada em cada uma.

Para o dono da pousada, os cinco planos cobram e devolvem o que ele descreveu. Não falhou nenhuma conferência.

## Dúvidas

- `ReservationService.java:139` guarda o `companyCode` só quando `n.plan().code().equals("CORPORATE")`. É uma regra
  de plano fora do plano, pequena e que não trata de preço nem de devolução, e por isso não mudou a classe `acerto`. Se
  vier um segundo plano que precise de um dado extra, esse `if` vai crescer.
- A validação do plano roda antes da verificação de capacidade (`ReservationService.java:109-113`). Uma reserva que
  quebra as duas regras recebe 400, e não 422. O texto não define essa ordem.
