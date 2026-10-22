# Revisão: construção, cenário 01

## Visão geral

O agente montou um serviço Javalin em oito pacotes: `api` (rotas, `ApiMapper`, DTOs), `service` (reservas, relatórios, mensagens, quartos), `pricing` (um plano de tarifa por classe), `repository` (JDBC puro), `model` e `exception`. Tudo é ligado no `App.main`. Uma requisição entra em `ApiController`, que lê o corpo, converte datas e o plano e chama `ReservationService`. O serviço pede ao plano a validação e o preço (`RatePlanFactory.getStrategy`), confere conflito pelo `ReservationRepository` e grava. Os dados ficam em duas tabelas do PostgreSQL (`reservations` e `messages`), criadas com `CREATE TABLE IF NOT EXISTS` no construtor de `Database`. Os quartos não estão no banco: ficam em `enum`s. Os cinco planos ficaram num Strategy limpo, e o cenário passou inteiro no roteiro (21 de 21). Há três problemas sérios:
- **Reservas duplicadas:** nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos. Não há restrição no banco, nem transação, nem trava, e o roteiro criou as duas em 10 de 10 rodadas.
- **`mvn verify` falha:** os 16 testes de integração do próprio agente não chegam a conectar, então o `mvn verify` que o texto pede não passa.
- **Erros que viram 500:** um corpo que não é JSON e um corpo com campos faltando dão 500, quando deviam dar 400.

## Arquitetura

**Números:** 8 pacotes, 14 dependências entre eles, nenhum ciclo. O Javalin só aparece em `com.pousadamareaalta` e `api`, o JDBC só em `repository`. A maior classe é `ReservationRepository`, com 235 linhas.
**Nota:** bom

- **Dependências:** apontam numa direção só, `api` → `service` → `pricing`/`repository` → `model`, sem ciclo (`measures/dependencies.md`).
- **Montagem:** num lugar só. `App.java:25-45` cria o `Database`, os repositórios, os serviços e o controlador, e passa cada um pelo construtor.
- **Borda HTTP:** fina. `api/ApiController.java:83-107` lê o corpo, converte e chama `reservationService.createReservation`. Os demais handlers (`:109-143`) só leem o `id`, chamam o serviço e mapeiam. Os erros são traduzidos num lugar só (`:51-74`).
- **Regras:** não conhecem o Javalin nem o SQL. `service/ReservationService.java:29-78` valida, pede ao plano e grava pelo repositório. O SQL fica todo em `repository/ReservationRepository.java` e `repository/MessageRepository.java`.
- **Pontos menores:**
  - o `ApiMapper` consulta um serviço para montar o comprovante (`api/ApiMapper.java:51`);
  - a regra de quando existe comprovante fica num caminho só, o do e-mail (`service/ReservationService.java:161`). A rota da segunda via passa por fora dela (`api/ApiController.java:139-142`). Isso é um erro de comportamento, contado lá.

Nenhum dos dois pede outra forma de dividir as camadas.

## Design de baixo nível

**Números:** os 5 planos estão na estrutura e nenhum fora dela. A interface `RatePlanStrategy` tem 3 operações: validar, preço e devolução. Há 1 `switch`, só de seleção (`pricing/RatePlanFactory.java:8`), e nenhum `if` sobre o plano no serviço.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `pricing/RatePlanStrategy.java:8-14` define `validate`, `calculatePrice` e `calculateRefund`. Cada plano tem a sua classe com as três regras dele, inclusive os dois que já existiam:
- `StandardRatePlan`;
- `NonRefundableRatePlan`, com 15% e sem devolução;
- `CorporateRatePlan`, com o mapa das empresas e a recusa do código (`:11-25`);
- `WeekendPackageRatePlan`, com sexta a domingo, preço fechado e metade de devolução (`:12-42`);
- `LongStayRatePlan`, com o mínimo de 7 noites e 10% (`:10-32`).

Quem usa pede ao plano sem perguntar qual ele é. A criação usa `strategy.validate` e `strategy.calculatePrice` (`service/ReservationService.java:49-57`), e o cancelamento usa `strategy.calculateRefund` (`:110-111`). O único `switch` só escolhe a implementação (`pricing/RatePlanFactory.java:8-14`), o que a referência aceita.

O que afasta um pouco:
- **Herança para reaproveitar código:** três planos estendem `StandardRatePlan` só para usar `calculateSubtotal` (`pricing/CorporateRatePlan.java:10`, `LongStayRatePlan.java:9`, `NonRefundableRatePlan.java:8`). Uma mudança no plano normal pode vazar para eles.
- **`companyCode` em todos os planos:** o parâmetro, que só o convênio usa, está na assinatura de todos (`pricing/RatePlanStrategy.java:9,11`).
- **Código da empresa gravado sempre:** é guardado em qualquer plano (`service/ReservationService.java:68`). É pequeno e não é regra de preço; está nas dúvidas.

**O exagero.** Não há estrutura fora do que o pattern pede. `PricingResult` é só o retorno do cálculo, e a fábrica é o ponto de seleção do próprio Strategy. Não há interfaces de uma implementação só, nem camadas que só repassam.

**O resto do código** tem defeitos localizados:
- **Dinheiro formatado pela localidade da JVM:** `String.format("%,.2f")` seguido de troca de caracteres (`service/ReservationService.java:189`) e `String.format("%.2f")` no CSV (`service/ReportService.java:78`). Numa JVM em `pt_BR`, o CSV sairia `599,76` e o comprovante `R$ 1,234.56`. Falta `Locale` explícito.
- **Erros mal tratados:**
  - o `catch` da criação só pega `DateTimeParseException | IllegalArgumentException` (`api/ApiController.java:104`). Um `null` em `LocalDate.parse` ou em `RatePlan.valueOf` (`:87-89`) e uma falha do Jackson escapam como 500;
  - o `Long.parseLong` do `id` (`:110-140`) também vira 500 com um `id` que não é número;
  - os repositórios embrulham toda `SQLException` em `RuntimeException` genérica (`repository/ReservationRepository.java:51` e seguintes).
- **Campos obrigatórios sem validação:** `guestName`, `guestEmail` e `guestCpf` nulos só são barrados pelo `NOT NULL` do banco, como erro 500.
- **Modelo anêmico:** `model/Reservation.java` é só getters e setters. O serviço muda a situação com `setStatus` em quatro métodos, cada um com o seu `if`, sem um lugar que concentre as transições.
- **Zero sem duas casas:** o zero é criado com escala 0 (`service/ReservationService.java:74`, `pricing/StandardRatePlan.java:17`), e o JSON sai `0` em vez de `0.00`.
- **Formato de data repetido:** o `DateTimeFormatter` `yyyy-MM-dd` está repetido (`api/ApiMapper.java:13`, `service/ReportService.java:23,46`, `api/ApiController.java`).

## Testes

**Números:**
- **Execução:** 22 testes rodaram: 6 passaram e 16 deram erro, sem falhas de asserção e sem pulados. Há 22 métodos de teste escritos.
- **Cobertura:** 18,0% das linhas (128 de 712) e 17,5% dos ramos (17 de 97). Por pacote, `pricing` tem 57 de 87 linhas e `service` tem 13 de 148.
**Nota:** ruim

**Por que os 16 dão erro.** Todos os testes de `ReservationSystemIT` deram `ConnectException`. O servidor de teste é subido numa thread com uma espera fixa de 2 s (`src/test/java/com/pousadamareaalta/TestAppRunner.java:46`). O jar leva cerca de 2,1 s para começar a ouvir (medi subindo-o à mão), mais ainda com o agente do JaCoCo. Além disso, `TestAppRunner.java:29-32` configura o banco e a porta com `System.setProperty`, mas `App.java:20-23` lê `System.getenv`: a configuração do teste é ignorada. A falha é do código de teste, não do ambiente. Com ela, o `mvn verify` que o texto exige não passa.

**O que se confere de verdade.** Só os 6 testes de unidade de `pricing/PricingTest.java`, e eles conferem com valores:
- Tecnosul: 779,80, 93,58 e 686,22 (`:14-24`);
- pacote: 820,00 (`:27-38`) e a devolução de 410,00 (`:41-48`);
- estadia longa: 1.749,30, 174,93 e 1.574,37 (`:51-61`);
- alta temporada: 599,76 (`:64-74`) e as bordas da temporada (`:77-86`).

**O que fica sem rede.** Fazendo a pergunta da mutação:
- trocar 0.18 da Agrovale ou 0.25 da Farmavida não derruba nenhum teste que rode;
- trocar `nights < 7` por `nights <= 7` também não (`LongStayRatePlan.java:15`);
- nem tirar a conferência de sexta e domingo do pacote, ou a recusa do código da empresa;
- nem as devoluções do convênio, da estadia longa e do não reembolsável.

Essas regras só aparecem nos testes de integração, que não rodam.

**A qualidade dos testes de integração.** Mesmo que rodassem, são frágeis:
- dependem da ordem (`@TestMethodOrder`) e de ids fixos num banco compartilhado (`reservationId` igual a 1);
- os relatórios só conferem o cabeçalho e a palavra `total,`.

## Banco

**Números:**
- **Tabelas:** 2 (`reservations` e `messages`), ambas com chave primária. Não há tabela de quartos.
- **Restrições:** nenhuma chave estrangeira, nenhum `CHECK` e nenhum `UNIQUE`/`EXCLUDE`. `status` e `rate_plan` são `varchar` sem `CHECK`.
- **Tipos:** o dinheiro está todo em `numeric(10,2)` e as datas em `date`.
- **Acesso:** sem pool (`DriverManager` direto), 0 transações explícitas, 0 `FOR UPDATE` e nenhum SQL montado com `+`.
- **Esquema:** criado com `CREATE TABLE` no código.

**Nota:** ruim

- **Reservas duplicadas.** Nada impede duas reservas do mesmo quarto na mesma noite. A criação lê os conflitos (`repository/ReservationRepository.java:121-156`, chamado em `service/ReservationService.java:52`) e depois insere (`:77`), em duas conexões separadas e em autocommit. Não há trava nem transação, e no banco não há `EXCLUDE` sobre o quarto e as datas. No roteiro, as duas reservas foram criadas nas 10 de 10 rodadas de pedidos simultâneos. É a regra central da pousada, e o resultado é dado errado gravado, não só um risco teórico.
- **O esquema não evolui.** `repository/Database.java:29-58` cria as tabelas com `CREATE TABLE IF NOT EXISTS` no construtor. Não há migração versionada: a próxima coluna (a de um plano novo, por exemplo) não chega a um banco que já existe.
- **A reserva não se liga a nada.** `room_number` não é chave estrangeira (`repository/Database.java:31`): os quartos só existem num `enum` (`model/RoomNumber.java`).
- **Nenhum `CHECK`** protege `guests >= 1`, `check_out > check_in` ou os valores de `status` e `rate_plan`.
- **Duas escritas soltas.** A confirmação grava a reserva (`service/ReservationService.java:95`) e depois a mensagem (`:97`), sem transação. Se a segunda escrita falhar, a reserva fica confirmada sem e-mail.
- **O que está certo:** o SQL usa sempre parâmetros `?`, as conexões são fechadas com try-with-resources e os tipos de dinheiro e de data estão corretos.
- **Consulta do financeiro:** usa `EXTRACT` sobre `check_in` (`repository/ReservationRepository.java:189-190`), o que impede o uso de um índice. Num sistema deste tamanho, pesa pouco.

## Comportamento

**Números:** 67 de 72 conferências passaram, 5 falharam.

| parte | passaram |
|---|---|
| parte comum | 42 de 46 |
| depois de reiniciar | 3 de 3 |
| cenário 01 | 21 de 21 |
| reservas simultâneas | 1 de 2 |

**Nota:** com problemas

**O cenário funciona inteiro.** Os três convênios dão 12%, 18% e 25%. A recusa sem código, ou com código inválido, dá 400. O pacote sai a 520, 820 e 1.200, inclusive em julho, devolve 410,00 e recusa datas erradas. A estadia longa dá 1.574,37, respeita a temporada e recusa 6 noites. As devoluções do convênio e da estadia longa estão certas. Os planos normal e não reembolsável continuam certos, e os dados sobrevivem ao reinício.

O que falhou, e o que significa para o dono da pousada:
- **Duas reservas ao mesmo tempo:** as duas foram criadas em 10 de 10 rodadas. Se dois hóspedes reservarem o mesmo quarto quase juntos pelo site, os dois ficam com a reserva. O defeito é do banco e já pesou lá.
- **Corpo que não é JSON e campos faltando:** dão 500, não 400 `INVALID_REQUEST` (`api/ApiController.java:85-104`). O site recebe um erro genérico em vez de saber que mandou dados errados.
- **Segunda via de reserva pendente:** responde 200 com o comprovante, quando devia dar 409. A rota não confere a situação (`api/ApiController.java:139-142`). A recepção pode tirar comprovante de reserva não paga, e pela leitura do código também de reserva cancelada.
- **`amountPaid` de reserva recém-criada:** sai `0`, não `0.00` (`service/ReservationService.java:74`). O mesmo vale para `discount` nos planos sem desconto. O texto pede 2 casas: é um erro pequeno, de formato.

## Dúvidas

- **A nota do banco.** A skill põe em `com problemas` a duplicação protegida só dentro do processo. Aqui não há proteção nenhuma, e a duplicação aconteceu em todas as rodadas. Por isso dei `ruim`, mas uma leitura mais branda daria `com problemas`.
- **O código da empresa.** É gravado em qualquer plano (`service/ReservationService.java:68`), não só no convênio. É uma regra de plano pequena fora do plano; não mudou a classe.
- **Por que o corpo inválido dá 500.** O log do serviço não mostra a exceção. Pela leitura, é a exceção do Jackson, que não é `IllegalArgumentException`, ou o `NullPointerException` de `LocalDate.parse(null)`, que escapam do `catch` em `api/ApiController.java:104`.
