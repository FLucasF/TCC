# Revisão: construção, cenário 09

## Visão geral

O projeto foi construído do zero em 9 pacotes. `pousada.controller` é a borda HTTP do Javalin, `pousada.service` e `pousada.pricing` têm as regras, `pousada.repository` tem o JDBC com Hikari, e há ainda `pousada.domain`, `pousada.dto`, `pousada.report` e `pousada.config`. Tudo é montado em `Main.java:26-69`. Uma requisição entra num método do controller, que lê o corpo ou o parâmetro e chama o `ReservationService`. O serviço valida, calcula o preço com `PricingCalculator` e grava pelo repositório. Os relatórios seguem outro caminho: o `ReportController` cria um `Report` que consulta o repositório no construtor, e o `CsvReportWriter` escreve título, cabeçalho, linhas e total. Os dados ficam em três tabelas que `Database.java:45-80` cria com `CREATE TABLE IF NOT EXISTS`. O mais sério é que nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos: o serviço confere e depois grava, sem transação, trava ou restrição. O roteiro criou as duas reservas em 9 de 10 rodadas. Também pesam três coisas: os testes de integração dependem de Docker e não rodaram, o que deixou a cobertura em 6%; corpo que não é JSON dá 500; e os relatórios escrevem `0` em vez de `0.00` quando o valor é zero.

## Arquitetura

**Números:** 9 pacotes, 17 dependências entre eles, nenhum ciclo. O Javalin aparece só em `pousada` e `pousada.controller`, e o JDBC só em `pousada.config` e `pousada.repository`. As maiores classes são `ReservationRepository` (247 linhas), `ReservationController` (230) e `ReservationService` (195).
**Nota:** com problemas

As camadas estão no lugar e apontam na direção certa: controller, depois service, depois repository, sem ciclo. As regras de preço e de situação da reserva não conhecem nem o Javalin nem o SQL (`ReservationService.java:27-135`, `PricingCalculator.java:45-73`). O SQL fica todo nos repositórios.

As misturas são localizadas:
- **Regra dentro do handler.** A regra "segunda via só para reserva paga" está em `ReservationController.java:165`, e não no serviço. O check-out e a segunda via também vão direto ao `RoomRepository` (`ReservationController.java:144`, `:169`).
- **Relatórios sem camada de regras.** O `ReportController` recebe os repositórios (`Main.java:44`) e cria relatórios que fazem a consulta no próprio construtor (`OccupancyReport.java:19`, `RevenueReport.java:17`). A seleção dos dados de relatório não passa por nenhuma camada de regras.
- **Montagem só em parte num lugar.** O banco é um `static` global (`Database.java:12`), e todos os repositórios o chamam por `Database.getConnection()` (por exemplo, `ReservationRepository.java:27`), sem injeção. O serviço cria o próprio `PricingCalculator` (`ReservationService.java:23`), e o controller cria o próprio `CsvReportWriter` (`ReportController.java:22`).

Nada disso é grave num sistema deste tamanho, mas um revisor pediria para corrigir.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo mesmo esqueleto (`CsvReportWriter.write`, `CsvReportWriter.java:8-21`). Nenhum relatório escreve a própria linha de título. A interface `Report` tem 4 implementações.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O esqueleto (título, cabeçalho, linhas, total) está num lugar só, `CsvReportWriter.java:11-18`. O título com o nome da pousada está escrito uma vez, em `CsvReportWriter.java:24`. Cada relatório só preenche as partes que variam: `getName`, `getPeriod`, `getColumns`, `getDataRows` e `getTotalRow` (`Report.java:5-9`). Os quatro relatórios, ocupação e financeiro inclusive, passam por ele (`ReportController.java:35`, `:55`, `:75`, `:95`). Não há classe abstrata com um método-modelo `final`: é a composição de um escritor genérico com objetos que trazem as partes que mudam. A referência aceita isso de antemão como alternativa equivalente. A mudança anunciada (a linha com a data no fim de todos os relatórios) se faz num só ponto. Um relatório novo é uma classe nova, sem copiar a sequência.

**O exagero.** Não há estrutura sobrando. A única interface, `Report`, tem quatro implementações. `MessageService` só repassa ao repositório (`MessageService.java:16-23`), mas é fino e tem um papel claro.

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` a cada passo (`PricingCalculator.java:56`, `:66`, `:94`), `LocalDate`, e `enum` para a situação, o plano e o tipo de quarto. Mas há defeitos localizados:
- **Duplicação nos relatórios.** `formatAmount` está copiado em três relatórios (`RevenueReport.java:63`, `RatePlanReport.java:80`, `TourismTaxReport.java:67`), e `getNetAmount` em dois (`RevenueReport.java:58`, `RatePlanReport.java:75`). A cópia de `formatAmount` é só `amount.toString()`, sem `setScale(2)`, e é dela que vem o `0` no lugar de `0.00` (ver Comportamento).
- **Dado de domínio repetido.** `OccupancyReport.java:56-63` repete num `switch` fixo qual número de quarto é de qual tipo (`"101", "102" -> "STANDARD"`…), em vez de usar o `Room` ou o `RoomType`. Se um quarto mudar, o relatório mente.
- **Tratamento de erros copiado.** Os mesmos blocos `catch` que traduzem exceção em resposta estão repetidos nos sete handlers de `ReservationController.java` (por exemplo, `:99`, `:116`, `:133`, `:153`, `:178`) e nos quatro de `ReportController.java:39-99`, em vez de um `app.exception` só. Mesmo assim falta o caso do JSON inválido, que vira 500 (`ReservationController.java:30` só é protegido por `catch (IllegalArgumentException)` em `:54`).
- **Lógica redundante no cancelamento.** `ReservationService.java:96-105` põe o reembolso em zero nos dois ramos do mesmo jeito.
- **Zero sem 2 casas.** `amountPaid` e `discount` começam como `BigDecimal.ZERO` sem escala (`Reservation.java:26`, `PricingCalculator.java:63`), e por isso o JSON da criação traz `0` em vez de `0.00`.
- **Exceções aninhadas no serviço.** Todas as exceções de negócio são classes aninhadas do serviço (`ReservationService.java:160-195`), o que obriga o controller a conhecê-las por `ReservationService.X`.

## Testes

**Números:** 6 testes rodaram, com 0 falhas, 1 erro (a classe `IntegrationTest` inteira) e 0 pulados. Há 21 métodos de teste escritos. Cobertura de linhas: 58 de 936 (6,2%). Cobertura de ramos: 14 de 140 (10,0%). Só `pousada.pricing` tem cobertura de verdade (41 de 42 linhas). `report`, `service`, `controller` e `repository` estão em 0.
**Nota:** ruim

**Os testes que rodam.** São só os cinco de `PricingCalculatorTest.java`. Eles conferem com valores a alta temporada (599.76, em `:29-31`), o desconto não reembolsável com arredondamento (116.97 e 662.83, em `:48-49`), a baixa temporada (`:65`) e o limite de 14 e 15 de dezembro (`:83`, `:100`). Essa parte está bem testada.

**Os que não rodam.** Os outros 16 métodos estão em `IntegrationTest.java`, que sobe um PostgreSQL por Testcontainers (`IntegrationTest.java:34-38`) e ignora o banco que chega em `DB_URL`. Sem Docker, a classe inteira falha antes de começar: "Could not find a valid Docker environment" (`measures/tests-maven.log`). A falha é de ambiente, mas a dependência de Docker foi escolha do agente, e com ela o `mvn verify` que o texto exige não passa aqui. Esses testes até conferem valores: a taxa de turismo `18.00` (`IntegrationTest.java:303-304`), os planos (`:317-319`), as devoluções (`:332`, `:363`). Mas nenhum deles rodou, e por isso a parte nova do cenário (os quatro relatórios e o título) não tem nenhum teste executado.

**A pergunta da mutação.** Trocar `4.50` por `5.40` (`TourismTaxReport.java:12`), tirar a linha de título (`CsvReportWriter.java:11`) ou trocar a seleção das reservas que começam no mês não faria falhar nenhum teste que roda. Pior: `IntegrationTest.java:318` espera `NON_REFUNDABLE,0,0`, ou seja, trava o defeito do `0` sem 2 casas como se fosse o certo.

**Outros defeitos.** Os testes de integração dependem da ordem (`@TestMethodOrder` e ids fixos 1, 2 e 3) e de uma porta fixa (7071). Eles também copiam a montagem do `Main` (`IntegrationTest.java:43-87`) em vez de usá-la, e assim não testam as rotas reais.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram:
- a chave estrangeira `reservations(room_number)` sem índice;
- nenhum CHECK nas colunas de situação, plano, tipo e canal;
- nenhuma restrição UNIQUE ou EXCLUDE;
- 0 transações explícitas e 0 `FOR UPDATE`.

Não há SQL montado com `+`.
**Nota:** ruim

**O que está certo.** Os tipos estão certos (`Database.java:52-70`), todo o SQL usa `?`, as conexões vêm de um pool Hikari e são devolvidas com try-with-resources (`ReservationRepository.java:27`, `:72`). Os dados sobrevivem a um reinício (3 de 3 no roteiro).

**O defeito sério.** A regra principal do negócio, "um quarto não pode ter duas reservas na mesma noite", não é protegida por nada. `ReservationService.java:45` consulta `hasConflict` (`ReservationRepository.java:116-147`, em autocommit) e só depois grava em `ReservationRepository.java:27`, outra conexão e outra transação. Não há restrição de exclusão no esquema, nem transação, nem trava, nem mesmo um `synchronized`. Com dois pedidos simultâneos, o roteiro criou as duas reservas em 9 de 10 rodadas: é a pousada vendendo o mesmo quarto duas vezes.

**Os outros defeitos.**
- O esquema é criado no código com `CREATE TABLE IF NOT EXISTS` (`Database.java:45-80`), sem migrações versionadas. A primeira coluna nova que o próximo pedido trouxer não vai entrar num banco que já existe.
- A chave estrangeira `room_number` (`Database.java:69`) não tem índice.
- Nenhum `CHECK` protege `status`, `rate_plan`, `guests >= 1` ou `check_out > check_in`.
- A confirmação grava a reserva (`ReservationService.java:78`) e depois o e-mail (`:80`) em transações separadas. Uma falha entre as duas deixa a reserva confirmada sem mensagem.

## Comportamento

**Números:** 57 de 64 conferências passaram:
- a parte comum: 42 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 09: 11 de 13;
- as duas reservas ao mesmo tempo: 1 de 2.
**Nota:** com problemas

**O que funciona.** A parte comum funciona: o preço com a temporada e o arredondamento, os planos, as recusas com os códigos certos, a confirmação com o e-mail e o comprovante, o check-in e o check-out, as devoluções, os dois relatórios e os dados depois de reiniciar. Os quatro relatórios começam com o título certo, e a taxa de turismo e os planos dão os valores certos quando há reservas.

**As falhas, uma a uma:**
- **"O 201 comporta 3" e "a diária do 201".** O roteiro procura `number` como número, e o projeto devolve `"201"` como texto (`RoomController.java:28`). O texto não fixa o tipo, e "201" como texto é uma escolha razoável. Não pesa.
- **`amountPaid` como `0` e não `0.00` na criação.** O texto pede o dinheiro no JSON com 2 casas. Vem de `Reservation.java:26`. O defeito é pequeno: depois de ler do banco, o valor já vem com 2 casas.
- **Corpo que não é JSON dá 500, em vez de 400 INVALID_REQUEST.** O site recebe um erro de servidor sem o formato `{error, message}` combinado.
- **Taxa de turismo de um mês vazio termina em `total,0`, e o plano sem reserva sai `NON_REFUNDABLE,0,0`.** O texto pede os valores "como no financeiro (27.00)". O contador e a prefeitura recebem um CSV com o formato do dinheiro diferente de um mês para outro. O mesmo `toString()` faz o financeiro de um mês vazio sair `total,0`.
- **Duas reservas simultâneas do mesmo quarto foram criadas em 9 de 10 rodadas.** O defeito é do banco e pesa naquela coluna. Para o dono, quer dizer que, com dois hóspedes reservando pelo site ao mesmo tempo, o quarto pode ser vendido duas vezes.

## Dúvidas

- Na coluna Banco, a skill não cita "nenhuma proteção contra a reserva duplicada" entre os exemplos de `ruim`. Dei `ruim` pelo critério geral (risco de dado errado), porque a regra não tem proteção nenhuma e o roteiro mostrou a duplicação em 9 de 10 rodadas. Um revisor mais brando daria `com problemas`.
- Os testes de integração podem passar numa máquina com Docker. Aqui não foi possível conferir, e a nota de Testes reflete o que rodou contra o `DB_URL` dado.
