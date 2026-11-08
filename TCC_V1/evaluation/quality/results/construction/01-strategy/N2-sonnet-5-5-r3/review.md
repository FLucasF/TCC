# Revisão: construção, cenário 01

## Visão geral

O projeto tem 6 pacotes e 31 classes, cerca de 1.040 linhas em `src/main`. `web` (`Routes`, `RequestParser`) recebe a requisição e transforma o JSON em `ReservationRequest`. `service` (`ReservationService`, `ReportService`) aplica as regras dentro de `Database.inTransaction`. `domain` guarda os tipos e as transições de situação (`Reservation`, `Stay`, `Money`, `Season`, `Rooms`). `plan` tem os cinco planos de tarifa. `infra` faz o JDBC (`ReservationRepository`, `MessageRepository`, `Database`). Tudo é montado em `App.main` (`App.java:25-48`). As reservas e as mensagens ficam no PostgreSQL, em duas tabelas criadas por `CREATE TABLE IF NOT EXISTS`. Os quartos são uma lista fixa no código (`domain/Rooms.java:7-12`). O pattern pedido está bem resolvido: uma interface `RatePlan` com uma classe por plano, escolhida por mapa. O roteiro de comportamento passou inteiro (72 de 72). Os pontos fracos são três:
- os testes cobrem pouco: 22,5% das linhas, só os planos e o comprovante;
- o banco é criado sem migrações versionadas e sem nenhuma restrição;
- há um ciclo real entre `domain` e `plan`, além dos ciclos que vêm da `DomainException` no pacote raiz.

## Arquitetura

**Números:** 6 pacotes, 16 dependências entre eles, 5 ciclos no `tsort`. Quatro passam pelo pacote raiz `br.com.marealta`, onde ficam juntos `App` e `DomainException`. O quinto é `domain` ↔ `plan`. Só `web` e o raiz usam Javalin. JDBC aparece em `infra` e em `service`.
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota chama um método do serviço e escreve o resultado (`web/Routes.java:32-45`). A leitura e a validação do corpo ficam em `web/RequestParser.java:16-41`. O mapeamento de erro para HTTP está num lugar só (`web/Routes.java:25-30`).
- **As regras não conhecem Javalin nem SQL.**
  - O preço e a validação de cada plano ficam em `plan/`.
  - As transições ficam em `domain/Reservation.java:27-49`.
  - A capacidade e o conflito são decididos no serviço (`service/ReservationService.java:39-51`).
  - Todo o SQL está em `infra/ReservationRepository.java` e `infra/MessageRepository.java`.
- **A montagem é feita num lugar só** (`App.java:25-48`), com injeção pelo construtor.

Os defeitos:
- **Ciclo entre `domain` e `plan`, o único com causa de responsabilidade.** `domain/Reservation.java:4,43` recebe um `RatePlan` em `cancel(...)` e chama `plan.refund(...)`. Ao mesmo tempo, `plan` depende de `domain` (`Stay`, `Money`, `RoomType`). O cálculo da devolução poderia ficar no serviço, que já tem o plano em mãos (`service/ReservationService.java:70`), e o ciclo sumiria.
- **Os outros ciclos vêm de onde está a `DomainException`.** Ela fica no pacote raiz (`DomainException.java:1`), junto com o `App`, que depende de todos (`App.java:3-14`). Todo pacote que lança erro (`domain`, `plan`, `service`, `web`) passa a depender do pacote do `main`. Isso é mais um problema de organização do que de responsabilidade: mover a exceção para `domain` desfaria esses ciclos.
- **O serviço vê `java.sql.Connection`.** Ele recebe a conexão nas lambdas de `inTransaction` e a repassa aos repositórios (`service/ReservationService.java:47-53,60-66,93-98`). Não há SQL no serviço, mas o tipo JDBC vaza para a camada de regras.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (`StandardPlan`, `NonRefundablePlan`, `CorporatePlan`, `WeekendPackagePlan`, `LongStayPlan`), 0 fora dela. Nenhum `switch` ou `if` sobre o nome do plano fora das classes de plano. Há um único ponto de seleção, o mapa de `RatePlans`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.**
- A interface `RatePlan` (`plan/RatePlan.java:8-17`) reúne as três variações que o texto pede: `quote` (preço e validação, que lança `INVALID_REQUEST`), `refund` e `code`.
- Cada plano tem o seu comportamento num lugar só:
  - o desconto por empresa e a exigência do código: `plan/CorporatePlan.java:10-25`;
  - sexta a domingo, o preço fechado por tipo de quarto e a devolução de metade: `plan/WeekendPackagePlan.java:13-33`;
  - o mínimo de 7 noites e os 10%: `plan/LongStayPlan.java:9-21`;
  - os dois planos que já existiam: `plan/StandardPlan.java:7-11` e `plan/NonRefundablePlan.java:8-12`.
- A escolha é feita por mapa (`plan/RatePlans.java:11-21`). O registro fica no `main` (`App.java:29-34`).
- Quem usa a estrutura não pergunta qual é o plano. A criação chama `plan.quote(...)` (`service/ReservationService.java:42-43`), e o cancelamento chama `plan.refund(...)` (`domain/Reservation.java:48`, `service/ReservationService.java:70`).
- Um plano novo é uma classe e uma linha no `main`.

**O exagero.** Não há. A classe abstrata `NightlyPlan` (`plan/NightlyPlan.java:9-23`) junta a sequência que quatro dos cinco planos dividem: validar, somar as noites, aplicar o percentual e arredondar. Ela evita copiar o cálculo e não é estrutura sem uso. `Stay` e `Quote` são records pequenos, que levam e trazem dados entre o serviço e o plano. Não há fábrica, builder nem camada que só repassa.

**O resto do código.**
- Os tipos estão certos:
  - `BigDecimal` com `HALF_EVEN` e 2 casas em todo passo (`domain/Money.java:14-16`; noite a noite em `domain/Stay.java:16-25`; desconto em `plan/NightlyPlan.java:15`);
  - `LocalDate` e `YearMonth` para datas e meses;
  - `enum` para a situação da reserva e o tipo de quarto.
- `Reservation` é um record imutável, e as transições devolvem uma cópia (`domain/Reservation.java:27-59`).
- Há detalhes pequenos, que não mudam a nota:
  - o construtor de 16 argumentos é repetido quatro vezes (`domain/Reservation.java:29,46,52,57`);
  - o plano fica guardado como `String` (`domain/Reservation.java:18`), embora todo uso passe por `RatePlans.get`;
  - `NullPointerException` é capturada para tratar parâmetro ausente (`web/RequestParser.java:46,54`), quando um teste de `null` resolveria;
  - o `catch (Exception)` geral só faz `printStackTrace` (`web/Routes.java:27-30`), mas não esconde a causa: ela vai para o log e a resposta é 500.

## Testes

**Números:** 6 testes rodaram, 0 falharam, 0 pulados. São 6 métodos escritos, todos em `PlansTest`. Cobertura: 84 de 374 linhas (22,5%) e 23 de 108 ramos (21,3%). Por pacote: `plan` 33/48, `domain` 46/62, `service` 0/77, `infra` 0/105, `web` 0/60. Nenhum teste passa pela API nem pelo banco.
**Nota:** com problemas

**O que os testes conferem bem, com valores.** A parte nova do cenário está coberta com os números do texto:
- Tecnosul: 779,80, 93,58 e 686,22 (`src/test/java/br/com/marealta/PlansTest.java:35-42`);
- o pacote: 820,00 e a devolução de 410,00 (`PlansTest.java:45-53`);
- a estadia longa: 1.749,30, 174,93 e 1.574,37 (`PlansTest.java:56-62`);
- as recusas: convênio sem código ou com código inválido, pacote fora de sexta a domingo, 6 noites (`PlansTest.java:40-41,51-52,61`);
- a alta temporada de julho (`PlansTest.java:22-25`) e o desconto do não reembolsável (`PlansTest.java:28-32`);
- o texto do comprovante, linha a linha (`PlansTest.java:65-77`).

**A pergunta da mutação.** Trocar 0.12 por 0.21 ou `< 7` por `<= 7` faria algum teste falhar. Estas mudanças passariam sem ninguém ver:
- os percentuais da Agrovale e da Farmavida (`plan/CorporatePlan.java:12-13`);
- a devolução integral do convênio e da estadia longa (`plan/CorporatePlan.java:25`, `plan/LongStayPlan.java:21`). Trocar `amountPaid` por zero não quebra nenhum teste;
- os preços do pacote no standard e na suíte (`plan/WeekendPackagePlan.java:15,17`);
- o limite de 15 de dezembro da temporada (`domain/Season.java:13`). Trocar `>= 15` por `> 15` passa, porque só julho é testado.

**O que não tem teste nenhum.**
- A máquina de situações: confirmar, check-in, check-out, cancelar e as recusas 409 (`domain/Reservation.java:27-49`).
- O conflito de noites, a capacidade, o e-mail de confirmação e a segunda via (`service/ReservationService.java`, 0 de 77 linhas).
- Os dois relatórios (`service/ReportService.java`).
- A leitura do corpo e os códigos de erro (`web/`).
- O SQL (`infra/`).

Os testes que existem são bons. O que falta é rede para quase tudo além dos planos.

## Banco

**Números:** 2 tabelas (`reservations` e `messages`), ambas com chave primária. O dinheiro está todo em `numeric(12,2)`, e as datas em `date`. Não há chave estrangeira, nem CHECK, nem UNIQUE ou EXCLUDE. `status`, `rate_plan` e `channel` são `text` sem CHECK. O esquema é criado por `CREATE TABLE` no código, sem migrações. Não há pool: a conexão vem de `DriverManager`, uma por operação. Há 1 transação explícita (reutilizada por todas as operações) e 1 `FOR UPDATE`.
**Nota:** com problemas

O que está certo:
- **Os tipos estão certos**, e o `NOT NULL` está em tudo que é obrigatório (`infra/Database.java:28-52`). Só `refund` aceita nulo, como o texto pede.
- **O SQL usa sempre parâmetros.** As quatro concatenações que o script listou juntam só constantes do código: `COLUMNS`, o `where` fixo e o `" FOR UPDATE"` (`infra/ReservationRepository.java:31-32,44-46,82,92,97,102`).
- **As conexões são fechadas**, por try-with-resources e com rollback em erro (`infra/Database.java:66-80`).
- **A confirmação grava a reserva e o e-mail na mesma transação** (`service/ReservationService.java:60-66`).
- **A reserva dupla é barrada em qualquer número de processos.** `pg_advisory_xact_lock` por quarto (`infra/ReservationRepository.java:22-27`) e, em seguida, a consulta de sobreposição na mesma transação (`service/ReservationService.java:47-53`). A trava vale no banco, não só dentro do processo.

Os defeitos:
- **O esquema não tem como evoluir.** Ele é criado por `CREATE TABLE IF NOT EXISTS` dentro de `Database.migrate` (`infra/Database.java:22-64`), sem versão. O próximo plano que precisar de uma coluna nova, por exemplo para guardar o `companyCode`, que hoje não é gravado, não terá como alterar uma tabela que já existe.
- **O banco não protege nenhuma regra.**
  - Não há EXCLUDE para noites sobrepostas: a proteção depende de todo código de escrita passar pela trava.
  - Não há CHECK em `status` ou `guests`.
  - Não há tabela de quartos nem chave estrangeira (os quartos estão em `domain/Rooms.java`, o que o texto tolera porque eles "quase nunca mudam").
- **A consulta mais usada não tem índice.** A consulta de conflito filtra por `room_number`, `check_in` e `check_out` (`infra/ReservationRepository.java:31-32`), e os relatórios filtram por datas (`infra/ReservationRepository.java:92,97`). Com poucos dados isso não pesa.
- **Não há pool de conexões** (`infra/Database.java:67`).

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam. Por parte: comum 46/46, depois de reiniciar 3/3, cenário 01 21/21, reservas simultâneas 2/2.
**Nota:** bom

Para o dono da pousada, tudo o que o texto descreve funciona pela API como pedido:
- o preço com a alta temporada e o arredondamento "meio para o par";
- os códigos de erro;
- a confirmação com o e-mail e o comprovante, o check-in e o check-out;
- as devoluções e os dois relatórios em CSV.

Os dados continuam no banco depois de reiniciar. Os três planos novos dão exatamente os valores do texto:
- Tecnosul: 686,22;
- Agrovale: 639,44, e Farmavida: 584,85;
- o pacote: 820,00, com devolução de 410,00 e o mesmo preço em julho;
- a estadia longa: 1.574,37, e 1.754,30 com noites de temporada.

As recusas por regra de plano voltam 400 `INVALID_REQUEST`. Nas 10 rodadas de duas reservas ao mesmo tempo, sempre uma foi criada e a outra recusada com 409.

## Dúvidas

- O `companyCode` não é gravado na reserva. Ele só entra no cálculo (`domain/Stay.java:8`, `service/ReservationService.java:43`), e a tabela não tem a coluna (`infra/Database.java:28-45`). O texto não pede que a reserva mostre ou guarde o código, então isso não pesou como erro. Mas o contador não consegue saber depois qual empresa teve o desconto.
- A arquitetura ficou em `com problemas` por causa do ciclo `domain` ↔ `plan`, que é pequeno, e dos ciclos que vêm da `DomainException` no pacote raiz. As camadas em si estão bem separadas. Um revisor menos rígido com ciclos daria `bom`.
