# Revisão: construção, cenário 01

## Visão geral

O projeto tem cinco pacotes: `br.com.mare` (só o `App`, que monta as peças), `web` (rotas Javalin, leitura do corpo e montagem do JSON), `domain` (reserva, quartos, dinheiro, serviços de reserva e de relatório), `plan` (os planos de tarifa) e `infra` (conexão JDBC, criação do esquema e repositórios). Uma requisição segue este caminho: `Routes` → `RequestParser` → `ReservationService` (abre a transação pelo `Db`, pede o preço ao `RatePlan`, muda a situação na própria `Reservation`) → `ReservationRepository`/`MessageRepository` → PostgreSQL, com duas tabelas (`reservations`, `messages`). Os quartos ficam fixos no código (`domain/Rooms.java:7-12`). O comportamento está completo: as 72 conferências do roteiro passaram, inclusive as reservas simultâneas e o reinício do serviço. Os cinco planos ficam numa estrutura Strategy limpa. Os pontos fracos são estes: o pacote `domain` depende de JDBC e de `infra` e forma dois ciclos; o esquema é criado com `CREATE TABLE IF NOT EXISTS`, sem migrações nem restrições; os testes cobrem só o cálculo de preço (17,6% das linhas). Fora do roteiro, um `GET /reports/occupancy` sem `date` responde 500.

## Arquitetura

**Números:** 5 pacotes, 10 dependências entre eles, 2 ciclos (`domain` ↔ `infra` e `domain` ↔ `plan`). O pacote `domain` usa JDBC; o Javalin fica só em `web` e no `App` (`measures/dependencies.md`).
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota lê a requisição, chama o serviço e escreve a resposta, numa linha só (`web/Routes.java:19-31`). O formato do JSON fica em `web/Views.java`, e a validação de formato em `web/RequestParser.java`. As regras de situação ficam na própria reserva (`domain/Reservation.java:47-80`). O preço e a devolução ficam nos planos (`plan/`), que não conhecem Javalin nem JDBC.
- **A montagem fica num lugar só,** em `App.java:13-20`.
- **A regra conhece o banco.** `ReservationService` importa `java.sql.Connection`, `Statement` e `infra.Db` (`domain/ReservationService.java:3-13`). Ele executa SQL direto, o `SELECT pg_advisory_xact_lock(2)` (`domain/ReservationService.java:41-43`), e recebe `Connection` nos efeitos colaterais (`domain/ReservationService.java:87-89`). `ReportService` também depende de `Db` e do repositório concreto (`domain/ReportService.java:3-4,21`). Daí vem o ciclo `domain` ↔ `infra`, porque `infra` mapeia as linhas em tipos de `domain` (`infra/ReservationRepository.java:3-6,109-127`).
- **O segundo ciclo, `domain` ↔ `plan`.** `Reservation` guarda um `RatePlan` (`domain/Reservation.java:3,65`), e os planos usam `Room`, `Money` e `ApiException` do domínio. O ciclo é pequeno e entre vizinhos, mas mostra que `plan` faz parte do domínio e ficou num pacote à parte.
- **Status HTTP no domínio.** A exceção de domínio carrega o status HTTP (`domain/ApiException.java:7-21`), então o domínio sabe que um erro vira 409 ou 422.
- **O tamanho é equilibrado:** a maior classe tem 128 linhas.

A mistura é localizada: um pedaço de SQL e a transação dentro do serviço, mais dois ciclos. Por isso a nota é "com problemas", e não "ruim".

## Design de baixo nível

**Números:** os 5 planos estão na estrutura e nenhum ficou fora dela. Nenhum `switch` ou `if` sobre o plano fora de `plan/`. Um único ponto de seleção, um mapa por código (`plan/RatePlans.java:11-17`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Todos os planos implementam a interface `RatePlan` (`plan/RatePlan.java:6-14`), que tem `code()`, `quote(Booking)` (valida e calcula o preço) e `refund(amountPaid)`. Os dois planos que já existiam também têm classe própria:
- `StandardPlan` (`plan/StandardPlan.java:3-6`) e `NonRefundablePlan` (`plan/NonRefundablePlan.java:5-14`);
- `CorporatePlan`, que valida o código e aplica o desconto da empresa (`plan/CorporatePlan.java:9-25`);
- `LongStayPlan`, que exige 7 noites e dá 10% (`plan/LongStayPlan.java:14-21`);
- `WeekendPackagePlan`, com sexta e duas noites, preço fechado por tipo de quarto e metade de devolução (`plan/WeekendPackagePlan.java:13-35`).

A classe abstrata `NightlyRatePlan` (`plan/NightlyRatePlan.java:9-34`) junta o cálculo por diária. Ela deixa ganchos para validação, taxa de desconto e taxa de devolução, e o pacote, que não é por diária, implementa a interface direto. Quem usa a estrutura não pergunta qual é o plano: o serviço só chama `plan.quote(...)` (`domain/ReservationService.java:36-38`), e o cancelamento só chama `ratePlan.refund(amountPaid)` (`domain/Reservation.java:65`). Um plano novo é uma classe a mais e uma linha em `RatePlans`. A validação de cada plano também ficou dentro dele, e não no serviço.

**O exagero.** Não há exagero. `Booking` e `Quote` são registros de dados que o pattern usa. `Db.Work` (`infra/Db.java:19-21`) e `SideEffect` (`domain/ReservationService.java:87-89`) são interfaces funcionais pequenas e usadas. Não há fábrica, builder nem camada que só repassa.

**O resto do código** é limpo: `BigDecimal` com `HALF_EVEN` num lugar só (`domain/Money.java:13`), `LocalDate` para datas e `enum` para situação e tipo de quarto. Ainda assim, há defeitos localizados:
- **Data ausente vira erro 500.** `RequestParser.date` não trata `null` (`web/RequestParser.java:50-55`). Um `GET /reports/occupancy` sem `date` dá `NullPointerException` e responde 500 `INTERNAL_ERROR`, quando o texto pede 400 `INVALID_REQUEST`. Conferi com `curl`, e o log mostra `NullPointerException: text`.
- **Construtor de 16 parâmetros.** `Reservation` tem um construtor com 16 parâmetros posicionais (`domain/Reservation.java:26-28`). O serviço o chama com um `id` falso `0` e um rascunho que só serve para o `insert` (`domain/ReservationService.java:47-51`), e é fácil trocar a ordem de dois `BigDecimal`.
- **Fórmula de devolução repetida.** A mesma fórmula aparece em dois lugares (`plan/NightlyRatePlan.java:25-27` e `plan/WeekendPackagePlan.java:33-35`).
- **Canal como texto.** O canal da mensagem é texto solto (`domain/Message.java:3-4`), e não um `enum`.
- **Texto do e-mail no serviço.** O texto do e-mail é montado por concatenação dentro do serviço (`domain/ReservationService.java:61-64`), ao lado da transação.

## Testes

**Números:** 6 testes rodaram, 0 falharam, 6 métodos escritos num único arquivo. Cobertura de 82 de 465 linhas (17,6%) e de 27 de 114 ramos (23,7%). O pacote `plan` tem 49 de 51 linhas cobertas; `infra`, `web` e o `App` ficam em 0 (`measures/tests.md`). Não há teste pela API nem contra o banco.
**Nota:** com problemas

**O que os testes conferem, com valores.** Os testes que existem conferem com valores os exemplos do texto:
- o STANDARD de alta temporada, R$ 599,76 (`src/test/java/br/com/mare/PricingTest.java:21-24`);
- a Tecnosul, com 779,80, 93,58 e 686,22 (`PricingTest.java:27-31`);
- o pacote, com 820,00 e a devolução de 410,00 (`PricingTest.java:36-39`);
- a estadia longa, com 1.749,30, 174,93 e 1.574,37 (`PricingTest.java:44-48`);
- a formatação de dinheiro (`PricingTest.java:58-61`).

**As recusas têm asserção fraca.** Os três testes de recusa só conferem `assertThrows(RuntimeException.class, ...)` (`PricingTest.java:32,40,49`). Eles não verificam o código `INVALID_REQUEST`, e qualquer `RuntimeException` os faria passar.

**A pergunta da mutação.** Estas regras ficariam sem rede:
- trocar 0.18 da Agrovale ou 0.25 da Farmavida (`plan/CorporatePlan.java:11-12`), porque só a Tecnosul é testada;
- trocar o desconto de 15% do NON_REFUNDABLE (`plan/NonRefundablePlan.java:10`), porque o teste só confere a devolução zero (`PricingTest.java:53-55`);
- tirar o `booking.nights() != 2` do pacote (`plan/WeekendPackagePlan.java:25`), porque o único caso recusado começa num sábado;
- mudar a devolução do CORPORATE ou do LONG_STAY.

**O que fica sem nenhum teste:**
- as transições de situação (`domain/Reservation.java:47-80`);
- o conflito de quarto e a capacidade;
- o comprovante e o e-mail (`domain/Receipt.java:18-31`);
- os dois relatórios (`domain/ReportService.java`);
- todos os códigos de erro da API.

O texto não pedia testes, e o que foi escrito acerta onde mira, mas deixa a maior parte das regras sem conferência.

## Banco

**Números:** 2 tabelas (`reservations`, com 16 colunas, e `messages`, com 5), ambas com chave primária. Dinheiro em `numeric(12,2)`, datas em `date`. Nenhuma chave estrangeira, nenhum CHECK, UNIQUE ou EXCLUDE. Esquema criado com `CREATE TABLE` no código, sem migrações. Sem pool: um `DriverManager.getConnection` por transação. Uma trava `FOR UPDATE` (`measures/database.md`).
**Nota:** com problemas

**O que está certo:**
- **Tipos.** Os tipos estão certos e quase tudo é `NOT NULL`; só `refund` aceita nulo, de propósito (`infra/Db.java:47-73`).
- **SQL com parâmetros.** Todo SQL usa parâmetros `?`. As concatenações que o script listou só juntam constantes (`infra/ReservationRepository.java:20-21,25,99`).
- **Transação.** Cada operação roda numa transação, com `rollback` em caso de erro (`infra/Db.java:23-37`). A confirmação grava a reserva e o e-mail na mesma transação (`domain/ReservationService.java:91-98`).
- **Duas reservas ao mesmo tempo.** O que impede a duplicação de reserva é uma trava consultiva do PostgreSQL (`domain/ReservationService.java:42`), que vale entre processos.

**Os defeitos:**
- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` dentro do Java (`infra/Db.java:47-73`), sem migração versionada. A próxima coluna, por exemplo guardar o `companyCode`, não chega a um banco que já existe.
- **O banco não protege nenhuma regra.** Não há CHECK em `status`, `rate_plan`, `guests` nem `check_out > check_in`, e não há restrição de exclusão contra sobreposição de noites. Tudo depende da trava no código. A trava tem número fixo (`2`) e serializa todas as criações de reserva, de qualquer quarto.
- **Não há índice para as consultas de conflito e de relatório.** Não existe índice em `room_number`/`check_in`, que elas filtram (`infra/ReservationRepository.java:35-36,87,94`).
- **Não há pool de conexões.** Cada transação abre uma conexão nova (`infra/Db.java:24`).
- **Os quartos não estão no banco,** e por isso `room_number` não tem chave estrangeira. Isso é aceitável, porque o texto diz que os quartos quase nunca mudam.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam (`measures/behavior.md`):
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 01: 21 de 21;
- reservas simultâneas: 2 de 2.

**Nota:** bom

**O cenário dá os valores que o financeiro passou:**
- Tecnosul com 93,58 de desconto e 686,22 de total;
- Agrovale e Farmavida;
- pacote a 520,00, 820,00 e 1.200,00, inclusive em julho, com devolução de 410,00;
- estadia longa a 1.574,37, inclusive com noites de temporada;
- as recusas de plano em 400 `INVALID_REQUEST`.

A parte comum também passou inteira: preços com temporada e arredondamento, erros, e-mail e comprovante, situações, devoluções e os dois relatórios. Os dados sobrevivem ao reinício, e nas 10 rodadas simultâneas só uma reserva foi criada. Para o dono da pousada, o sistema faz o que o texto pede.

Fora do roteiro, chamei a API com `curl` e achei um defeito: `GET /reports/occupancy` sem o parâmetro `date` responde 500 `INTERNAL_ERROR` em vez de 400 `INVALID_REQUEST` (`web/RequestParser.java:52`). O mesmo pedido sem `month` no financeiro responde 400 corretamente. O defeito já contou no design e não muda esta nota.

## Dúvidas

- **O `companyCode` não é guardado.** Ele não está na tabela nem na reserva (`infra/ReservationRepository.java:46-66`, `infra/Db.java:47-65`). O desconto fica gravado e a devolução do convênio não depende da empresa, então nada do que o texto pede quebra. Mas a pousada não fica sabendo depois de qual empresa veio cada reserva, e o texto não diz se precisa saber.
- **A nota do design ficou entre "bom" e "com problemas".** Pesou o 500 com o `date` ausente, somado ao construtor de 16 parâmetros. São defeitos pequenos e localizados.
